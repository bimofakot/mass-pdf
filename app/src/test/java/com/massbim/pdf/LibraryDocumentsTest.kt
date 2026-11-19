package com.massbim.pdf

import com.massbim.pdf.data.LibraryDocuments
import com.massbim.pdf.data.PdfMetadata
import com.massbim.pdf.data.RecentPdf
import com.massbim.pdf.storage.PdfFileSort
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryDocumentsTest {
    @Test
    fun pinnedPagesCoverEmptyPartialFullAndOverflowPanels() {
        val expectedPageSizes = mapOf(
            0 to emptyList(),
            1 to listOf(1),
            4 to listOf(4),
            5 to listOf(4, 1),
            20 to listOf(4, 4, 4, 4, 4),
            25 to listOf(4, 4, 4, 4, 4),
        )
        assertEquals(4, LibraryDocuments.PAGE_SIZE)
        assertEquals(20, LibraryDocuments.PANEL_LIMIT)
        expectedPageSizes.forEach { (count, sizes) ->
            val pins = (0 until count).map { document(it, pinned = true) }
            val source = listOf(document(100)) + pins + document(101)
            val original = source.toList()
            val pages = LibraryDocuments.pinnedPages(source)
            assertEquals("pin count $count", sizes, pages.map { it.size })
            assertEquals(pins.take(20), pages.flatten())
            assertEquals(original, source)
            assertEquals(pins, source.filter { it.pinned })
            assertEquals(pins, LibraryDocuments.retain(source).filter { it.pinned })
            PdfFileSort.values().forEach { sort ->
                val allDocuments = LibraryDocuments.sorted(source, sort)
                assertEquals(source.size, allDocuments.size)
                assertEquals(pins.toSet(), allDocuments.filter { it.pinned }.toSet())
            }
        }
    }

    @Test
    fun retentionKeepsEveryOldPinAndOnlyThirtyNewestUnpinnedInOriginalOrder() {
        val source = (0 until 40).map { document(it) } +
            (40 until 65).map { document(it, pinned = true) }
        val interleaved = listOf(document(100, pinned = true)) + source.take(10) +
            document(101, pinned = true) + source.drop(10)
        val retained = LibraryDocuments.retain(interleaved)
        assertEquals(30, retained.count { !it.pinned })
        assertEquals(interleaved.filter { it.pinned }, retained.filter { it.pinned })
        assertEquals(source.take(30), retained.filterNot { it.pinned })
        assertEquals(interleaved.filter { it.pinned || it in source.take(30) }, retained)
        assertEquals(retained, LibraryDocuments.retain(retained))
        assertTrue(LibraryDocuments.retain(emptyList()).isEmpty())
    }

    @Test
    fun openingNewDocumentDoesNotEvictOldPin() {
        val oldPin = document(100, pinned = true)
        val existing = (0 until 30).map { document(it) } + oldPin
        val opened = document(31)
        val retained = LibraryDocuments.retain(listOf(opened) + existing)
        assertEquals(listOf(opened) + existing.take(29) + oldPin, retained)
    }

    @Test
    fun unpinningReappliesUnpinnedLimitWithoutLosingOtherPins() {
        val recentPin = document(100, pinned = true)
        val oldPin = document(101, pinned = true)
        val remainingPin = document(102, pinned = true)
        val source = listOf(recentPin) + (0 until 30).map { document(it) } +
            listOf(oldPin, remainingPin)
        val unpinRecent = LibraryDocuments.retain(source.map {
            if (it.uri == recentPin.uri) it.copy(pinned = false) else it
        })
        assertEquals(30, unpinRecent.count { !it.pinned })
        assertEquals(recentPin.copy(pinned = false), unpinRecent.first())
        assertEquals(listOf(oldPin, remainingPin), unpinRecent.filter { it.pinned })
        val unpinOld = LibraryDocuments.retain(source.map {
            if (it.uri == oldPin.uri) it.copy(pinned = false) else it
        })
        assertEquals(source.filterNot { it.uri == oldPin.uri }, unpinOld)
    }

    @Test
    fun lastOpenedPreservesInputRegardlessOfNamesPinsAndMetadata() {
        val source = listOf(document(9), document(1, pinned = true), document(3))
        assertEquals(source, LibraryDocuments.sorted(source, PdfFileSort.LAST_OPENED,
            mapOf(source.last().uri to PdfMetadata(999, 999))))
    }

    @Test
    fun nameSortIsCaseInsensitiveLocaleIndependentAndDeterministic() {
        val source = listOf(
            RecentPdf("z", "alpha"), RecentPdf("b", "Alpha"),
            RecentPdf("a", "Alpha"), RecentPdf("i", "I.pdf"),
            RecentPdf("h", "h.pdf"), RecentPdf("j", "j.pdf"),
        )
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val expectedUris = listOf("a", "b", "z", "h", "i", "j")
            assertEquals(expectedUris, LibraryDocuments.sorted(source, PdfFileSort.NAME_ASC).map { it.uri })
            assertEquals(expectedUris, LibraryDocuments.sorted(source.reversed(), PdfFileSort.NAME_ASC).map { it.uri })
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun dateAndSizeSortDescendingWithUnknownLastAndDeterministicTies() {
        val source = listOf(
            RecentPdf("missing", "Unknown B"), RecentPdf("null", "Unknown A"),
            RecentPdf("b", "Same"), RecentPdf("a", "Same"),
            RecentPdf("large", "Largest"), RecentPdf("new", "Newest"),
            RecentPdf("zero", "Zero"),
        )
        val metadata = mapOf(
            "null" to PdfMetadata(),
            "a" to PdfMetadata(50, 1_000), "b" to PdfMetadata(50, 1_000),
            "large" to PdfMetadata(sizeBytes = 100),
            "new" to PdfMetadata(modifiedAt = 2_000),
            "zero" to PdfMetadata(0, 0),
        )
        val expected = mapOf(
            PdfFileSort.NEWEST to listOf("new", "a", "b", "zero", "large", "null", "missing"),
            PdfFileSort.LARGEST to listOf("large", "a", "b", "zero", "new", "null", "missing"),
        )
        expected.forEach { (sort, uris) ->
            assertEquals(uris, LibraryDocuments.sorted(source, sort, metadata).map { it.uri })
            assertEquals(uris, LibraryDocuments.sorted(source.reversed(), sort, metadata).map { it.uri })
        }
        PdfFileSort.values().forEach { sort ->
            assertTrue(LibraryDocuments.sorted(emptyList(), sort).isEmpty())
        }
        listOf(PdfFileSort.NEWEST, PdfFileSort.LARGEST).forEach { sort ->
            assertEquals(
                LibraryDocuments.sorted(source, PdfFileSort.NAME_ASC),
                LibraryDocuments.sorted(source.reversed(), sort),
            )
        }
        assertEquals(null, PdfMetadata().modifiedAt)
        assertEquals(null, PdfMetadata().sizeBytes)
    }

    private fun document(index: Int, pinned: Boolean = false) =
        RecentPdf(uri = "content://pdf/$index", title = "Document $index", pinned = pinned)
}
