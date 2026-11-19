package com.massbim.pdf

import com.massbim.pdf.ui.PdfResumeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfResumeStateTest {
    @Test
    fun waitsForFirstContentBeforeResuming() {
        val state = PdfResumeState(savedPage = 7)

        assertNull(state.targetPage(pageCount = 12))
        assertFalse(state.applied)
        assertEquals(7, state.copy(contentLoaded = true).targetPage(pageCount = 12))
    }

    @Test
    fun preservesSavedPageWhileDocumentIsLoading() {
        val state = PdfResumeState(savedPage = 7)

        assertNull(state.targetPage(pageCount = 0))
        assertNull(state.targetPage(pageCount = 12))
        assertEquals(7, state.savedPage)
        assertEquals(7, state.copy(contentLoaded = true).targetPage(pageCount = 12))
    }

    @Test
    fun doesNotResumeAgainAfterApplyingSavedPage() {
        val state = PdfResumeState(savedPage = 7, contentLoaded = true)
        val applied = state.copy(applied = true)

        assertEquals(7, state.targetPage(pageCount = 12))
        assertTrue(applied.applied)
        assertNull(applied.targetPage(pageCount = 12))
        assertNull(applied.copy(contentLoaded = true).targetPage(pageCount = 12))
    }

    @Test
    fun clampsSavedPageToTheDocumentBounds() {
        assertEquals(2, PdfResumeState(7, contentLoaded = true).targetPage(pageCount = 3))
        assertEquals(0, PdfResumeState(-1, contentLoaded = true).targetPage(pageCount = 3))
    }

    @Test
    fun firstPageAndSinglePageDocumentsCanFinishResuming() {
        assertEquals(0, PdfResumeState(0, contentLoaded = true).targetPage(pageCount = 12))
        assertEquals(0, PdfResumeState(7, contentLoaded = true).targetPage(pageCount = 1))
    }

    @Test
    fun cannotResumeAnEmptyDocument() {
        assertNull(PdfResumeState(7, contentLoaded = true).targetPage(pageCount = 0))
    }

    @Test
    fun anotherDocumentWaitsForItsOwnFirstContent() {
        val previous = PdfResumeState(7, contentLoaded = true, applied = true)
        val next = PdfResumeState(savedPage = 2)

        assertTrue(previous.applied)
        assertFalse(next.applied)
        assertNull(next.targetPage(pageCount = 12))
        assertEquals(2, next.copy(contentLoaded = true).targetPage(pageCount = 12))
    }
}
