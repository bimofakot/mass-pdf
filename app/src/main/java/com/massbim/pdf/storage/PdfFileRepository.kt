package com.massbim.pdf.storage

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.Locale

data class PdfStorageRoot(val directory: File, val label: String, val isRemovable: Boolean)

data class PdfStorageEntry(
    val file: File,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedAt: Long,
)

enum class PdfFileSort {
    LAST_OPENED,
    NAME_ASC,
    NEWEST,
    LARGEST,
}

/**
 * Daftar PDF dan folder dibaca dari indeks MediaStore, bukan pemindaian rekursif,
 * agar folder besar tidak menghabiskan waktu setiap kali dibuka. Indeks disimpan
 * di memori sampai ada perubahan file atau layar dibuka ulang.
 */
class PdfFileRepository(private val context: Context) {
    @Volatile
    private var cachedIndex: PdfIndex? = null

    fun storageRoots(): List<PdfStorageRoot> {
        requireStorageAccess()
        return context.getExternalFilesDirs(null)
            .filterNotNull()
            .mapNotNull(::storageRootFor)
            .distinctBy { it.directory.path }
    }

    fun invalidateIndex() {
        cachedIndex = null
    }

    fun list(
        directory: File,
        sort: PdfFileSort = PdfFileSort.NAME_ASC,
        recentUris: List<String> = emptyList(),
    ): List<PdfStorageEntry> {
        requireStorageAccess()
        requireSafePath(directory)
        if (!directory.isDirectory || !directory.canRead()) {
            throw IOException("Folder tidak dapat dibaca")
        }
        val indexPath = pathKey(directory)
        val index = index()
        val entries = buildList {
            index.childFolders[indexPath].orEmpty().forEach { folder ->
                add(
                    PdfStorageEntry(
                        file = folder,
                        isDirectory = true,
                        sizeBytes = 0L,
                        modifiedAt = folder.lastModified(),
                    ),
                )
            }
            index.pdfsByParent[indexPath].orEmpty().forEach { entry ->
                add(entry)
            }
        }
        return sorted(entries, sort, recentUris)
    }

    /** Semua PDF dari seluruh penyimpanan yang diizinkan, dipakai filter "PDF". */
    fun allPdfs(
        sort: PdfFileSort = PdfFileSort.NAME_ASC,
        recentUris: List<String> = emptyList(),
    ): List<PdfStorageEntry> = sorted(index().pdfs, sort, recentUris)

    @Throws(IOException::class, SecurityException::class)
    fun renamePdf(file: File, requestedName: String): File {
        requireStorageAccess()
        val root = requireSafePath(file)
        if (Files.isSymbolicLink(file.toPath()) || !file.isFile || !PdfFileRules.isPdf(file.name)) {
            throw SecurityException("Hanya file PDF yang dapat diubah namanya")
        }
        val parent = file.parentFile ?: throw IOException("Folder induk tidak ditemukan")
        requireWithinRoot(root, parent)
        val siblings = parent.list()?.toList()
            ?: throw IOException("Folder induk tidak dapat dibaca")
        val normalizedName = PdfFileRules.normalizedPdfName(requestedName, file.name, siblings)
            ?: throw IllegalArgumentException("Nama tidak valid atau sudah digunakan")
        if (normalizedName.equals(file.name, ignoreCase = true)) return file

        val destination = File(parent, normalizedName)
        if (Files.isSymbolicLink(destination.toPath()) || destination.exists() ||
            !file.renameTo(destination)
        ) {
            throw IOException("PDF tidak dapat diubah namanya")
        }
        updateCachedIndex { pdfs ->
            pdfs.filterNot { samePath(it.file, file) } + PdfStorageEntry(
                file = destination,
                isDirectory = false,
                sizeBytes = destination.length(),
                modifiedAt = destination.lastModified(),
            )
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath, destination.absolutePath),
            null,
            null,
        )
        return destination
    }

    @Throws(IOException::class, SecurityException::class)
    fun deletePdf(file: File) {
        requireStorageAccess()
        val root = requireSafePath(file)
        if (Files.isSymbolicLink(file.toPath()) || !file.isFile || !PdfFileRules.isPdf(file.name)) {
            throw SecurityException("Hanya file PDF yang dapat dihapus")
        }
        val parent = file.parentFile ?: throw IOException("Folder induk tidak ditemukan")
        requireWithinRoot(root, parent)
        if (!file.delete()) throw IOException("PDF tidak dapat dihapus")
        updateCachedIndex { pdfs -> pdfs.filterNot { samePath(it.file, file) } }
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
    }

    private fun index(): PdfIndex {
        cachedIndex?.let { return it }
        return synchronized(this) {
            cachedIndex ?: buildIndex().also { cachedIndex = it }
        }
    }

    private fun buildIndex(): PdfIndex {
        val roots = storageRoots()
        val pdfs = mutableListOf<PdfStorageEntry>()
        val seenPaths = mutableSetOf<String>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
        )
        val baseSelection =
            "(${MediaStore.Files.FileColumns.MIME_TYPE}=? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?)"
        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            "$baseSelection AND ${MediaStore.Files.FileColumns.IS_TRASHED}=0"
        } else {
            baseSelection
        }
        val arguments = arrayOf("application/pdf", "%.pdf")
        val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)

        context.contentResolver.query(
            contentUri,
            projection,
            selection,
            arguments,
            null,
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val modifiedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn) ?: continue
                val name = cursor.getString(nameColumn) ?: continue
                if (!PdfFileRules.isPdf(name) || PdfFileRules.isHidden(name)) continue

                val file = File(path).absoluteFile.toPath().normalize().toFile()
                val root = roots.firstOrNull { isWithin(it.directory, file) } ?: continue
                val segments = relativeSegments(root.directory, file)
                val directorySegments = segments.dropLast(1)
                if (directorySegments.any { PdfFileRules.isHidden(it) }) continue
                if (!PdfFileRules.isAndroidPathAllowed(directorySegments)) continue
                if (!seenPaths.add(pathKey(file))) continue

                pdfs += PdfStorageEntry(
                    file = file,
                    isDirectory = false,
                    sizeBytes = cursor.getLong(sizeColumn),
                    modifiedAt = cursor.getLong(modifiedColumn) * 1_000L,
                )
            }
        }
        return indexFrom(pdfs, roots.map { it.directory })
    }

    private fun indexFrom(
        pdfs: List<PdfStorageEntry>,
        rootDirectories: List<File>,
    ): PdfIndex {
        val pdfsByParent = mutableMapOf<String, MutableList<PdfStorageEntry>>()
        val folders = mutableSetOf<File>()

        for (entry in pdfs) {
            val parent = entry.file.parentFile ?: continue
            pdfsByParent.getOrPut(pathKey(parent)) { mutableListOf() }.add(entry)

            val root = rootDirectories.firstOrNull { isWithin(it, entry.file) } ?: continue
            var folder = parent
            while (!samePath(folder, root) && isWithin(root, folder)) {
                folders.add(folder)
                folder = folder.parentFile ?: break
            }
        }

        val childFolders = mutableMapOf<String, MutableList<File>>()
        for (folder in folders) {
            val parent = folder.parentFile ?: continue
            childFolders.getOrPut(pathKey(parent)) { mutableListOf() }.add(folder)
        }

        return PdfIndex(
            pdfs = pdfs,
            pdfsByParent = pdfsByParent,
            childFolders = childFolders,
        )
    }

    private inline fun updateCachedIndex(transform: (List<PdfStorageEntry>) -> List<PdfStorageEntry>) {
        val current = cachedIndex ?: return
        cachedIndex = indexFrom(
            pdfs = transform(current.pdfs),
            rootDirectories = storageRoots().map { it.directory },
        )
    }

    private fun sorted(
        entries: List<PdfStorageEntry>,
        sort: PdfFileSort,
        recentUris: List<String>,
    ): List<PdfStorageEntry> {
        val nameOrder = compareBy<PdfStorageEntry> { it.file.name.lowercase(Locale.ROOT) }
            .thenBy { it.file.name }
            .thenBy { it.file.absolutePath }
        val valueOrder: Comparator<PdfStorageEntry> = when (sort) {
            PdfFileSort.LAST_OPENED -> {
                val uriRanks = mutableMapOf<String, Int>()
                recentUris.forEachIndexed { index, uri -> uriRanks.putIfAbsent(uri, index) }
                val ranks = entries.associateWith { entry ->
                    if (entry.isDirectory) {
                        Int.MAX_VALUE
                    } else {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            entry.file,
                        ).toString()
                        uriRanks[uri] ?: Int.MAX_VALUE
                    }
                }
                compareBy<PdfStorageEntry> { ranks.getValue(it) }.then(nameOrder)
            }
            PdfFileSort.NAME_ASC -> nameOrder
            PdfFileSort.NEWEST -> compareByDescending<PdfStorageEntry> { it.modifiedAt }.then(nameOrder)
            PdfFileSort.LARGEST -> compareByDescending<PdfStorageEntry> { it.sizeBytes }.then(nameOrder)
        }
        return entries.sortedWith(compareBy<PdfStorageEntry> { !it.isDirectory }.then(valueOrder))
    }

    private fun storageRootFor(appSpecificDirectory: File): PdfStorageRoot? {
        val path = appSpecificDirectory.absolutePath.replace('\\', '/')
        val marker = "/Android/data/"
        val markerIndex = path.lastIndexOf(marker, ignoreCase = true)
        if (markerIndex <= 0) return null

        val root = File(path.substring(0, markerIndex)).canonicalFile
        if (!root.isDirectory || !root.canRead()) return null
        val removable = Environment.isExternalStorageRemovable(root)
        return PdfStorageRoot(
            directory = root,
            label = if (removable) "Penyimpanan eksternal" else "Penyimpanan perangkat",
            isRemovable = removable,
        )
    }

    private fun requireSafePath(target: File): File {
        val absoluteTarget = target.absoluteFile.toPath().normalize().toFile()
        val root = storageRoots().firstOrNull { isWithin(it.directory, absoluteTarget) }?.directory
            ?: throw SecurityException("Lokasi di luar shared storage yang diizinkan")
        requireWithinRoot(root, absoluteTarget)
        return root
    }

    private fun requireWithinRoot(root: File, target: File) {
        val normalizedTarget = target.absoluteFile.toPath().normalize().toFile()
        if (!isWithin(root, normalizedTarget)) {
            throw SecurityException("Lokasi di luar shared storage yang diizinkan")
        }
        if (!PdfFileRules.isAndroidPathAllowed(relativeSegments(root, normalizedTarget))) {
            throw SecurityException("Direktori sistem yang dilindungi tidak dapat dibuka")
        }
        var current: File? = normalizedTarget
        while (current != null && isWithin(root, current)) {
            if (Files.isSymbolicLink(current.toPath())) {
                throw SecurityException("Shortcut file tidak dapat dikelola")
            }
            if (current == root) return
            current = current.parentFile
        }
        throw SecurityException("Lokasi di luar shared storage yang diizinkan")
    }

    private fun relativeSegments(root: File, target: File): List<String> {
        val rootPath = root.absoluteFile.toPath().normalize()
        val targetPath = target.absoluteFile.toPath().normalize()
        if (!targetPath.startsWith(rootPath)) return listOf("..")
        return rootPath.relativize(targetPath).map { it.toString() }
    }

    private fun isWithin(root: File, target: File): Boolean {
        val rootPath = root.absoluteFile.toPath().normalize().toString()
        val targetPath = target.absoluteFile.toPath().normalize().toString()
        return targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
    }

    private fun samePath(first: File, second: File): Boolean =
        pathKey(first) == pathKey(second)

    private fun pathKey(file: File): String =
        file.absoluteFile.toPath().normalize().toString()

    private fun requireStorageAccess() {
        if (!StoragePermissionManager.hasAccess(context)) {
            throw SecurityException("Izin akses shared storage belum diberikan")
        }
    }

    private data class PdfIndex(
        val pdfs: List<PdfStorageEntry>,
        val pdfsByParent: Map<String, List<PdfStorageEntry>>,
        val childFolders: Map<String, List<File>>,
    )
}
