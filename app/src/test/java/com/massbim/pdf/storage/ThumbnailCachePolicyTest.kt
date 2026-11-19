// Uji kebijakan thumbnail tanpa Android: rasio, identitas, TTL, dan batas LRU.
package com.massbim.pdf.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThumbnailCachePolicyTest {
    @Test
    fun scalesLandscapePortraitAndSquare() {
        assertEquals(ThumbnailCachePolicy.Dimensions(320, 160), ThumbnailCachePolicy.dimensions(1200, 600))
        assertEquals(ThumbnailCachePolicy.Dimensions(160, 320), ThumbnailCachePolicy.dimensions(600, 1200))
        assertEquals(ThumbnailCachePolicy.Dimensions(320, 320), ThumbnailCachePolicy.dimensions(100, 100))
        val a4 = ThumbnailCachePolicy.dimensions(595, 842)!!
        assertEquals(320, a4.height)
        assertEquals(226, a4.width)
    }

    @Test
    fun extremeRatiosStayPositiveWithoutOverflow() {
        assertEquals(ThumbnailCachePolicy.Dimensions(320, 1), ThumbnailCachePolicy.dimensions(Int.MAX_VALUE, 1))
        assertEquals(ThumbnailCachePolicy.Dimensions(1, 320), ThumbnailCachePolicy.dimensions(1, Int.MAX_VALUE))
        assertEquals(ThumbnailCachePolicy.Dimensions(320, 320), ThumbnailCachePolicy.dimensions(Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test
    fun rejectsInvalidPageDimensions() {
        for ((width, height) in listOf(0 to 1, 1 to 0, -1 to 1, 1 to -1, Int.MIN_VALUE to Int.MAX_VALUE)) {
            assertNull(ThumbnailCachePolicy.dimensions(width, height))
        }
    }

    @Test
    fun keysTrackUriSizeAndModificationTimeWithoutExposingUri() {
        val uri = "content://example/private.pdf"
        val key = ThumbnailCachePolicy.key(uri, 100, 1234, 0)
        assertEquals(key, ThumbnailCachePolicy.key(uri, 100, 1234, Long.MAX_VALUE))
        assertNotEquals(key, ThumbnailCachePolicy.key("$uri/other", 100, 1234, 0))
        assertNotEquals(key, ThumbnailCachePolicy.key(uri, 101, 1234, 0))
        assertNotEquals(key, ThumbnailCachePolicy.key(uri, 100, 1235, 0))
        assertNotEquals(key, ThumbnailCachePolicy.key(uri, null, 1234, 0))
        assertTrue(key.matches(Regex("[0-9a-f]{64}")))
        assertFalse(key.contains("private"))
    }

    @Test
    fun unknownModificationTimeExpiresAtBucketBoundaryEvenWhenSizeIsKnown() {
        val ttl = ThumbnailCachePolicy.UNKNOWN_MTIME_TTL_MILLIS
        for (size in listOf(null, 100L)) {
            val key = ThumbnailCachePolicy.key("content://pdf/1", size, null, 0)
            assertEquals(key, ThumbnailCachePolicy.key("content://pdf/1", size, null, ttl - 1))
            assertNotEquals(key, ThumbnailCachePolicy.key("content://pdf/1", size, null, ttl))
            assertEquals(key, ThumbnailCachePolicy.key("content://pdf/1", size, 0, 0))
            assertEquals(key, ThumbnailCachePolicy.key("content://pdf/1", size, -1, 0))
        }
        assertEquals(0L, ThumbnailCachePolicy.timeBucket(ttl - 1))
        assertEquals(1L, ThumbnailCachePolicy.timeBucket(ttl))
        assertEquals(-1L, ThumbnailCachePolicy.timeBucket(-1))
        assertNotEquals(
            ThumbnailCachePolicy.key("uri", null, null, 0),
            ThumbnailCachePolicy.key("uri", 0, null, 0),
        )
    }

    @Test
    fun evictsLeastRecentlyAccessedForByteAndCountLimits() {
        val entries = listOf(entry("middle", 20, 2), entry("old", 10, 1), entry("new", 30, 3))
        assertEquals(setOf("old"), ThumbnailCachePolicy.evictions(entries, 50, 3).toSet())
        assertEquals(setOf("old", "middle"), ThumbnailCachePolicy.evictions(entries, 60, 1).toSet())
        assertEquals(setOf("old", "middle"), ThumbnailCachePolicy.evictions(entries, 30, 2).toSet())
        assertTrue(ThumbnailCachePolicy.evictions(entries, 60, 3).isEmpty())
        val touched = entries.map { if (it.key == "old") it.copy(accessedAt = 4) else it }
        assertEquals(setOf("middle"), ThumbnailCachePolicy.evictions(touched, 60, 2).toSet())
    }

    @Test
    fun enforcesProductionBoundsAndHandlesZeroAndOversizedEntries() {
        val entries = (0..200).map { entry(it.toString(), 256 * 1024L, it.toLong()) }
        val removed = ThumbnailCachePolicy.evictions(entries).toSet()
        val kept = entries.filterNot { it.key in removed }
        assertTrue(kept.size <= ThumbnailCachePolicy.DISK_FILES)
        assertTrue(kept.sumOf { it.bytes } <= ThumbnailCachePolicy.DISK_BYTES)
        assertEquals(128, kept.size)
        assertTrue(kept.any { it.key == "200" })
        assertFalse(kept.any { it.key == "0" })
        val tiny = (0..160).map { entry(it.toString(), 1, it.toLong()) }
        assertEquals(listOf("0"), ThumbnailCachePolicy.evictions(tiny))
        assertTrue(ThumbnailCachePolicy.evictions(emptyList(), 0, 0).isEmpty())
        assertEquals(listOf("a"), ThumbnailCachePolicy.evictions(listOf(entry("a", 1, 0)), 0, 1))
        assertEquals(listOf("a"), ThumbnailCachePolicy.evictions(listOf(entry("a", 0, 0)), 10, 0))
        val oversized = listOf(entry("a", Long.MAX_VALUE, 1), entry("b", Long.MAX_VALUE, 2))
        assertEquals(setOf("a", "b"), ThumbnailCachePolicy.evictions(oversized, 1, 2).toSet())
    }

    @Test
    fun tiedAccessTimesHaveDeterministicEvictionOrder() {
        val entries = listOf(entry("b", 1, 0), entry("a", 1, 0))
        assertEquals(listOf("b"), ThumbnailCachePolicy.evictions(entries, 1, 1))
        assertEquals(
            ThumbnailCachePolicy.evictions(entries, 1, 1),
            ThumbnailCachePolicy.evictions(entries.reversed(), 1, 1),
        )
    }

    private fun entry(key: String, bytes: Long, accessedAt: Long) =
        ThumbnailCachePolicy.Entry(key, bytes, accessedAt)
}
