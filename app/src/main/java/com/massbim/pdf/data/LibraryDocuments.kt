package com.massbim.pdf.data

import com.massbim.pdf.storage.PdfFileSort
import java.util.Locale

object LibraryDocuments {
    const val PAGE_SIZE = 4
    const val PANEL_LIMIT = 20
    private const val MAX_UNPINNED = 30

    fun sorted(
        documents: List<RecentPdf>,
        sort: PdfFileSort,
        metadata: Map<String, PdfMetadata> = emptyMap(),
    ): List<RecentPdf> {
        if (sort == PdfFileSort.LAST_OPENED) return documents.toList()
        val nameOrder = compareBy<RecentPdf> { it.title.lowercase(Locale.ROOT) }
            .thenBy { it.title }
            .thenBy { it.uri }
        val valueOrder = when (sort) {
            PdfFileSort.LAST_OPENED -> nameOrder
            PdfFileSort.NAME_ASC -> nameOrder
            PdfFileSort.NEWEST -> compareByDescending<RecentPdf> { metadata[it.uri]?.modifiedAt }
                .then(nameOrder)
            PdfFileSort.LARGEST -> compareByDescending<RecentPdf> { metadata[it.uri]?.sizeBytes }
                .then(nameOrder)
        }
        return documents.sortedWith(valueOrder)
    }

    fun pinnedPages(documents: List<RecentPdf>): List<List<RecentPdf>> =
        documents.filter { it.pinned }.take(PANEL_LIMIT).chunked(PAGE_SIZE)

    fun retain(documents: List<RecentPdf>): List<RecentPdf> {
        var unpinnedCount = 0
        return documents.filter { it.pinned || ++unpinnedCount <= MAX_UNPINNED }
    }
}
