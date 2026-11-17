package com.massbim.pdf.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfFileRulesTest {
    @Test
    fun recognizesPdfExtensionWithoutCaseSensitivity() {
        assertTrue(PdfFileRules.isPdf("invoice.PDF"))
        assertFalse(PdfFileRules.isPdf("invoice.pdf.tmp"))
    }

    @Test
    fun appendsPdfExtensionAndPreservesAnExistingExtension() {
        assertEquals("invoice.pdf", PdfFileRules.normalizedPdfName("invoice", "old.pdf", emptyList()))
        assertEquals("invoice.PDF", PdfFileRules.normalizedPdfName("invoice.PDF", "old.pdf", emptyList()))
    }

    @Test
    fun rejectsInvalidAndCollidingNamesButAllowsKeepingCurrentName() {
        assertNull(PdfFileRules.normalizedPdfName("../invoice", "old.pdf", emptyList()))
        assertNull(PdfFileRules.normalizedPdfName("invoice", "old.pdf", listOf("invoice.PDF")))
        assertEquals("old.pdf", PdfFileRules.normalizedPdfName("old.pdf", "old.pdf", listOf("old.pdf")))
    }

    @Test
    fun identifiesProtectedAndroidDirectoryRegardlessOfCase() {
        assertTrue(PdfFileRules.isProtectedDirectory("Android"))
        assertTrue(PdfFileRules.isProtectedDirectory("android"))
        assertFalse(PdfFileRules.isProtectedDirectory("Documents"))
    }
}
