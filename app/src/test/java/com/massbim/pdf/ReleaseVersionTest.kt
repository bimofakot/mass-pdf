package com.massbim.pdf

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseVersionTest {
    @Test
    fun comparesVersionNumbersRatherThanText() {
        assertTrue(ReleaseVersion.isNewer("v0.10.0", "0.9.0"))
    }

    @Test
    fun rejectsEqualOrOlderVersions() {
        assertFalse(ReleaseVersion.isNewer("v0.1.0", "0.1.0"))
        assertFalse(ReleaseVersion.isNewer("0.9.0", "0.10.0"))
    }

    @Test
    fun rejectsVersionsOutsideTheSupportedFormat() {
        assertFalse(ReleaseVersion.isNewer("latest", "0.1.0"))
        assertFalse(ReleaseVersion.isNewer("0.2", "0.1.0"))
    }
}
