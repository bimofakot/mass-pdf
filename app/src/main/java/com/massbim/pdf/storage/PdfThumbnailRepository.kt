// Thumbnail lokal memakai dokumen terpisah dari pembaca; semua akses provider/cache di IO.
// Gunakan satu instance aplikasi agar antrean render dan invalidasi cache tetap seragam.
package com.massbim.pdf.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.LruCache
import android.util.Size
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.SandboxedPdfLoader
import com.massbim.pdf.data.PdfMetadata
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalPdfApi::class)
class PdfThumbnailRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val mutex = Mutex()
    private val mutableGeneration = MutableStateFlow(0)
    val generation: StateFlow<Int> = mutableGeneration.asStateFlow()
    private val memory = object : LruCache<String, Bitmap>(ThumbnailCachePolicy.MEMORY_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    suspend fun load(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        mutex.withLock {
            recover {
                withTimeoutOrNull(10_000L) {
                    // Jangan sajikan cache setelah akses ke dokumen dicabut.
                    if (!isReadable(uri)) return@withTimeoutOrNull null
                    val metadata = readMetadata(uri)
                    currentCoroutineContext().ensureActive()
                    val key = ThumbnailCachePolicy.key(
                        uri.toString(), metadata?.sizeBytes, metadata?.modifiedAt, System.currentTimeMillis(),
                    )
                    recover { pruneDisk() }
                    memory.get(key)?.let {
                        recover { touch(File(cacheDirectory(), "$key.png")) }
                        return@withTimeoutOrNull it
                    }
                    readDisk(key)?.let {
                        memory.put(key, it)
                        return@withTimeoutOrNull it
                    }
                    val loader = SandboxedPdfLoader(appContext, coroutineContext = Dispatchers.IO)
                    val bitmap = loader.openDocument(uri, password = null).use { document ->
                        if (document.pageCount <= 0) return@withTimeoutOrNull null
                        val info = document.getPageInfo(0)
                        val size = ThumbnailCachePolicy.dimensions(info.width, info.height)
                            ?: return@withTimeoutOrNull null
                        document.getPageBitmapSource(0).use { source ->
                            source.getBitmap(Size(size.width, size.height), null)
                        }
                    }
                    currentCoroutineContext().ensureActive()
                    memory.put(key, bitmap)
                    // Cache tidak boleh menggagalkan hasil render yang sudah berhasil.
                    recover { writeDisk(key, bitmap) }
                    bitmap
                }
            }
        }
    }

    suspend fun metadata(uri: Uri): PdfMetadata? = withContext(Dispatchers.IO) {
        recover {
            withTimeoutOrNull(10_000L) {
                if (isReadable(uri)) readMetadata(uri) else null
            }
        }
    }

    suspend fun clear(): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            memory.evictAll()
            try {
                val directory = cacheDirectory()
                if (directory.exists()) {
                    val children = directory.listFiles() ?: throw IOException("Cache tidak dapat dibaca")
                    var failed = false
                    // Tidak rekursif: jangan mengikuti direktori/link menuju berkas sumber.
                    for (child in children) {
                        if (!child.delete()) failed = true
                    }
                    if (failed) throw IOException("Sebagian thumbnail tidak dapat dihapus")
                }
            } finally {
                mutableGeneration.value += 1
            }
        }
    }

    private suspend fun isReadable(uri: Uri): Boolean {
        currentCoroutineContext().ensureActive()
        if (uri.scheme != "content" && uri.scheme != "file") return false
        return resolver.openFileDescriptor(uri, "r")?.use { true } ?: false
    }

    private suspend fun readMetadata(uri: Uri): PdfMetadata? = recover {
        currentCoroutineContext().ensureActive()
        if (uri.scheme == "file") {
            val file = uri.path?.let(::File) ?: return@recover null
            if (!file.isFile || !file.canRead()) return@recover null
            PdfMetadata(file.length(), file.lastModified().takeIf { it > 0 })
        } else if (uri.scheme == "content") {
            if (uri.authority == "${appContext.packageName}.fileprovider" &&
                uri.pathSegments.firstOrNull() == "shared_storage"
            ) {
                // FileProvider tidak menyediakan mtime; root harus sesuai file_paths.xml.
                val root = Environment.getExternalStorageDirectory().canonicalFile
                val file = File(root, uri.pathSegments.drop(1).joinToString("/")).canonicalFile
                if (!file.toPath().startsWith(root.toPath()) || !file.isFile || !file.canRead()) {
                    return@recover null
                }
                return@recover PdfMetadata(file.length(), file.lastModified().takeIf { it > 0 })
            }
            // Kolom ditanya terpisah: provider boleh menolak last_modified tetapi mendukung size.
            val size = queryLong(uri, OpenableColumns.SIZE)?.takeIf { it >= 0 }
            val modified = queryLong(uri, DocumentsContract.Document.COLUMN_LAST_MODIFIED)?.takeIf { it > 0 }
            if (size == null && modified == null) null else PdfMetadata(size, modified)
        } else null
    }

    private suspend fun queryLong(uri: Uri, column: String): Long? = recover {
        currentCoroutineContext().ensureActive()
        resolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(column)
            if (index >= 0 && cursor.moveToFirst() && !cursor.isNull(index)) cursor.getLong(index) else null
        }
    }

    private fun cacheDirectory(): File {
        val parent = appContext.cacheDir.canonicalFile
        val directory = File(parent, "pdf-thumbnails")
        if (directory.canonicalFile != directory) throw IOException("Lokasi cache tidak valid")
        return directory
    }

    private fun touch(file: File) {
        if (file.exists()) file.setLastModified(System.currentTimeMillis())
    }

    private suspend fun readDisk(key: String): Bitmap? = recover {
        val file = File(cacheDirectory(), "$key.png")
        if (!file.isFile || file.length() > ThumbnailCachePolicy.DISK_BYTES) return@recover null
        // Periksa dimensi sebelum alokasi agar cache rusak tidak memicu alokasi besar.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outMimeType != "image/png" ||
            bounds.outWidth !in 1..ThumbnailCachePolicy.LONGEST_EDGE ||
            bounds.outHeight !in 1..ThumbnailCachePolicy.LONGEST_EDGE
        ) {
            file.delete()
            return@recover null
        }
        BitmapFactory.decodeFile(file.path)?.also { touch(file) }
    }

    private fun pruneDisk(reservedBytes: Long = 0, reservedFiles: Int = 0) {
        val directory = cacheDirectory()
        if (!directory.exists()) return
        val files = directory.listFiles() ?: throw IOException("Cache tidak dapat dibaca")
        val entries = files.map { ThumbnailCachePolicy.Entry(it.name, it.length(), it.lastModified()) }
        val removed = ThumbnailCachePolicy.evictions(
            entries, ThumbnailCachePolicy.DISK_BYTES - reservedBytes,
            ThumbnailCachePolicy.DISK_FILES - reservedFiles,
        ).toSet()
        for (file in files) {
            if (file.name in removed && !file.delete()) throw IOException("Cache tidak dapat dipangkas")
        }
    }

    private suspend fun writeDisk(key: String, bitmap: Bitmap) {
        val bytes = ByteArrayOutputStream().use { output ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) return
            output.toByteArray()
        }
        if (bytes.size > ThumbnailCachePolicy.DISK_BYTES) return
        currentCoroutineContext().ensureActive()
        val directory = cacheDirectory()
        if (!directory.isDirectory && !directory.mkdirs()) return
        val file = File(directory, "$key.png")
        if (file.exists() && !file.delete()) return
        pruneDisk(bytes.size.toLong(), 1)
        try {
            file.outputStream().use { it.write(bytes) }
            touch(file)
        } catch (failure: Exception) {
            file.delete()
            throw failure
        }
    }

    private suspend fun <T> recover(block: suspend () -> T): T? = try {
        block().also { currentCoroutineContext().ensureActive() }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        currentCoroutineContext().ensureActive()
        null
    }
}
