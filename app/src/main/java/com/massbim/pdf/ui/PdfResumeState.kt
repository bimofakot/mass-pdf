package com.massbim.pdf.ui

internal data class PdfResumeState(
    val savedPage: Int,
    val contentLoaded: Boolean = false,
    val applied: Boolean = false,
) {
    fun targetPage(pageCount: Int): Int? {
        if (!contentLoaded || applied || pageCount <= 0) return null
        return savedPage.coerceIn(0, pageCount - 1)
    }
}
