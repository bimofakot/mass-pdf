package com.massbim.pdf.storage

object PdfFileRules {
    private val forbiddenNameCharacters = setOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')

    fun isPdf(name: String): Boolean = name.endsWith(".pdf", ignoreCase = true)

    fun isProtectedDirectory(name: String): Boolean =
        name.equals("Android", ignoreCase = true)

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
