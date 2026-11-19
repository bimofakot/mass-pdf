// Kebijakan cache thumbnail murni; batas, ukuran, dan masa segar dapat diuji tanpa Android.
package com.massbim.pdf.storage

import java.security.MessageDigest
import kotlin.math.roundToInt

object ThumbnailCachePolicy {
    const val LONGEST_EDGE = 320
    const val MEMORY_BYTES = 8 * 1024 * 1024
    const val DISK_BYTES = 32L * 1024 * 1024
    const val DISK_FILES = 160
    const val UNKNOWN_MTIME_TTL_MILLIS = 15L * 60 * 1000

    data class Dimensions(val width: Int, val height: Int)
    data class Entry(val key: String, val bytes: Long, val accessedAt: Long)

    fun dimensions(width: Int, height: Int): Dimensions? {
        if (width <= 0 || height <= 0) return null
        val scale = LONGEST_EDGE.toDouble() / maxOf(width, height)
        return Dimensions(
            (width * scale).roundToInt().coerceIn(1, LONGEST_EDGE),
            (height * scale).roundToInt().coerceIn(1, LONGEST_EDGE),
        )
    }

    fun timeBucket(nowMillis: Long): Long = Math.floorDiv(nowMillis, UNKNOWN_MTIME_TTL_MILLIS)

    fun key(uri: String, sizeBytes: Long?, modifiedAt: Long?, nowMillis: Long): String {
        val size = sizeBytes?.takeIf { it >= 0 }
        val modified = modifiedAt?.takeIf { it > 0 }
        // Panjang URI mencegah ambiguitas pemisah; versi mengisolasi format/ukuran cache.
        val identity = "v1:${uri.length}:$uri:$size:$modified:" +
            if (modified == null) timeBucket(nowMillis).toString() else "stable"
        return MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }

    fun evictions(
        entries: List<Entry>,
        maxBytes: Long = DISK_BYTES,
        maxFiles: Int = DISK_FILES,
    ): List<String> {
        require(maxBytes >= 0 && maxFiles >= 0)
        // Akumulasi dari yang terbaru menghindari overflow penjumlahan ukuran total.
        var remainingBytes = maxBytes
        var remainingFiles = maxFiles
        val removed = mutableListOf<String>()
        var exhausted = false
        for (entry in entries.sortedWith(compareByDescending<Entry> { it.accessedAt }.thenBy { it.key })) {
            val bytes = entry.bytes.coerceAtLeast(0)
            if (exhausted || remainingFiles == 0 || bytes > remainingBytes) {
                exhausted = true
                removed += entry.key
            } else {
                remainingBytes -= bytes
                remainingFiles--
            }
        }
        return removed
    }
}
