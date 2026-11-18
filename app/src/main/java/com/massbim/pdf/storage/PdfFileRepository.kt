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

enum class PdfFileSort {
    NAME_ASC,
    NAME_DESC,
    NEWEST,
    LARGEST,
}

class PdfFileRepository(private val context: Context) {
    fun storageRoots(): List<PdfStorageRoot> {
        requireStorageAccess()
        return context.getExternalFilesDirs(null)
            .filterNotNull()
            .mapNotNull(::storageRootFor)
            .distinctBy { it.directory.path }
    }

    fun list(directory: File, sort: PdfFileSort = PdfFileSort.NAME_ASC): List<PdfStorageEntry> {
        requireStorageAccess()
        val root = requireSafePath(directory)
        if (!directory.isDirectory || !directory.canRead()) {
            throw IOException("Folder tidak dapat dibaca")
        }
        val children = directory.listFiles() ?: throw IOException("Folder tidak dapat dibaca")
        val directoriesWithPdf = mutableSetOf<String>()
        collectPdfs(
            directory = directory,
            root = root,
            depth = 0,
            pdfs = mutableListOf(),
            directoriesWithPdf = directoriesWithPdf,
            budget = intArrayOf(SCAN_BUDGET),
        )
        val entries = children.asSequence()
            .filterNot { PdfFileRules.isHidden(it.name) }
            .filterNot { Files.isSymbolicLink(it.toPath()) }
            .filter { PdfFileRules.isAndroidPathAllowed(relativeSegments(root, it)) }
            .filter {
                when {
                    it.isDirectory -> directoriesWithPdf.contains(it.absolutePath)
                    else -> it.isFile && PdfFileRules.isPdf(it.name)
                }
            }
            .onEach { requireWithinRoot(root, it) }
            .map {
                PdfStorageEntry(
                    file = it,
                    isDirectory = it.isDirectory,
                    sizeBytes = if (it.isFile) it.length() else 0L,
                    modifiedAt = it.lastModified(),
                )
            }
            .toList()
        return sorted(entries, sort)
    }

    /** Semua PDF di seluruh penyimpanan, dipakai filter "PDF" agar tidak perlu membuka folder. */
    fun allPdfs(sort: PdfFileSort = PdfFileSort.NAME_ASC): List<PdfStorageEntry> {
        requireStorageAccess()
        val pdfs = mutableListOf<File>()
        val directoriesWithPdf = mutableSetOf<String>()
        val budget = intArrayOf(SCAN_BUDGET)
        for (root in storageRoots()) {
            collectPdfs(
                directory = root.directory,
                root = root.directory,
                depth = 0,
                pdfs = pdfs,
                directoriesWithPdf = directoriesWithPdf,
                budget = budget,
            )
        }
        val entries = pdfs.distinctBy { it.absolutePath }.map {
            PdfStorageEntry(
                file = it,
                isDirectory = false,
                sizeBytes = it.length(),
                modifiedAt = it.lastModified(),
            )
        }
        return sorted(entries, sort)
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

    private fun collectPdfs(
        directory: File,
        root: File,
        depth: Int,
        pdfs: MutableList<File>,
        directoriesWithPdf: MutableSet<String>,
        budget: IntArray,
    ): Boolean {
        if (depth > SCAN_DEPTH || budget[0] <= 0) return false
        val children = directory.listFiles() ?: return false
        budget[0] -= children.size
        var hasPdf = false
        for (child in children) {
            if (child.isFile && PdfFileRules.isPdf(child.name)) {
                pdfs.add(child)
                hasPdf = true
            }
        }
        for (child in children) {
            if (!child.isDirectory) continue
            if (PdfFileRules.isHidden(child.name)) continue
            if (Files.isSymbolicLink(child.toPath())) continue
            if (!isWithin(root, child)) continue
            if (!PdfFileRules.isAndroidPathAllowed(relativeSegments(root, child))) continue
            if (collectPdfs(child, root, depth + 1, pdfs, directoriesWithPdf, budget)) {
                directoriesWithPdf.add(child.absolutePath)
                hasPdf = true
            }
        }
        return hasPdf
    }

    private fun sorted(
        entries: List<PdfStorageEntry>,
        sort: PdfFileSort,
    ): List<PdfStorageEntry> {
        val valueOrder: Comparator<PdfStorageEntry> = when (sort) {
            PdfFileSort.NAME_ASC -> compareBy { it.file.name.lowercase() }
            PdfFileSort.NAME_DESC -> compareByDescending { it.file.name.lowercase() }
            PdfFileSort.NEWEST -> compareByDescending { it.modifiedAt }
            PdfFileSort.LARGEST -> compareByDescending { it.sizeBytes }
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
        val rootPath = root.path.trimEnd(File.separatorChar)
        val targetPath = target.path
        return targetPath == rootPath || targetPath.startsWith(rootPath + File.separator)
    }

    private fun requireStorageAccess() {
        if (!StoragePermissionManager.hasAccess(context)) {
            throw SecurityException("Izin akses shared storage belum diberikan")
        }
    }

    private companion object {
        const val SCAN_DEPTH = 8
        const val SCAN_BUDGET = 3000
    }
}
