package com.massbim.pdf.storage

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException
import java.nio.file.Files

data class PdfStorageRoot(val directory: File, val label: String)

data class PdfStorageEntry(
    val file: File,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val modifiedAt: Long,
)

class PdfFileRepository(private val context: Context) {
    fun storageRoots(): List<PdfStorageRoot> {
        requireStorageAccess()
        return context.getExternalFilesDirs(null)
            .filterNotNull()
            .mapNotNull(::storageRootFor)
            .distinctBy { it.directory.path }
    }

    fun list(directory: File): List<PdfStorageEntry> {
        requireStorageAccess()
        val root = requireSafePath(directory)
        if (!directory.isDirectory || !directory.canRead()) {
            throw IOException("Folder tidak dapat dibaca")
        }
        val children = directory.listFiles() ?: throw IOException("Folder tidak dapat dibaca")
        return children.asSequence()
            .filterNot { PdfFileRules.isProtectedDirectory(it.name) }
            .filterNot { Files.isSymbolicLink(it.toPath()) }
            .filter { it.isDirectory || (it.isFile && PdfFileRules.isPdf(it.name)) }
            .onEach { requireWithinRoot(root, it) }
            .map {
                PdfStorageEntry(
                    file = it,
                    isDirectory = it.isDirectory,
                    sizeBytes = if (it.isFile) it.length() else 0L,
                    modifiedAt = it.lastModified(),
                )
            }
            .sortedWith(compareBy<PdfStorageEntry> { !it.isDirectory }.thenBy { it.file.name.lowercase() })
            .toList()
    }

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
    }

    private fun storageRootFor(appSpecificDirectory: File): PdfStorageRoot? {
        val path = appSpecificDirectory.absolutePath.replace('\\', '/')
        val marker = "/Android/data/"
        val markerIndex = path.lastIndexOf(marker, ignoreCase = true)
        if (markerIndex <= 0) return null

        val root = File(path.substring(0, markerIndex)).canonicalFile
        if (!root.isDirectory || !root.canRead()) return null
        val label = if (Environment.isExternalStorageRemovable(root)) {
            "Penyimpanan eksternal"
        } else {
            "Penyimpanan perangkat"
        }
        return PdfStorageRoot(root, label)
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
        var current: File? = normalizedTarget
        while (current != null && isWithin(root, current)) {
            if (Files.isSymbolicLink(current.toPath())) {
                throw SecurityException("Shortcut file tidak dapat dikelola")
            }
            if (current != root && PdfFileRules.isProtectedDirectory(current.name)) {
                throw SecurityException("Direktori sistem yang dilindungi tidak dapat dibuka")
            }
            if (current == root) return
            current = current.parentFile
        }
        throw SecurityException("Lokasi di luar shared storage yang diizinkan")
    }

    private fun isWithin(root: File, target: File): Boolean {
        val rootPath = root.path.trimEnd(File.separatorChar)
        val targetPath = target.path
        return targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
    }

    private fun requireStorageAccess() {
        if (!StoragePermissionManager.hasAccess(context)) {
            throw SecurityException("Izin akses shared storage belum diberikan")
        }
    }
}
