package com.massbim.pdf.storage

object PdfFileRules {
    private val forbiddenNameCharacters = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

    fun isPdf(name: String): Boolean = name.endsWith(".pdf", ignoreCase = true)

    fun isHidden(name: String): Boolean = name.startsWith(".")

    fun isProtectedDirectory(name: String): Boolean =
        name.equals("Android", ignoreCase = true)

    /**
     * Direktori Android hanya boleh ditembus lewat Android/media (mis. PDF WhatsApp).
     * Android/data, Android/obb, dan cabang lain tetap ditutup.
     */
    fun isAndroidPathAllowed(segments: List<String>): Boolean {
        val androidIndex = segments.indexOfFirst { it.equals("Android", ignoreCase = true) }
        if (androidIndex == -1) return true
        if (androidIndex != 0) return false
        return segments.size == 1 || segments[1].equals("media", ignoreCase = true)
    }

    fun normalizedPdfName(
        input: String,
        currentName: String,
        siblingNames: Collection<String>,
    ): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty() || trimmed == "." || trimmed == "..") return null
        if (trimmed.any { it.isISOControl() || it in forbiddenNameCharacters }) return null

        val normalized = if (isPdf(trimmed)) trimmed else "$trimmed.pdf"
        if (normalized.equals(".pdf", ignoreCase = true)) return null
        val conflicts = siblingNames.any {
            it.equals(normalized, ignoreCase = true) && !it.equals(currentName, ignoreCase = true)
        }
        return normalized.takeUnless { conflicts }
    }
}
