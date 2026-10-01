package com.ehviewer.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VersionCompareTest {
    @Test
    fun newerPatchVersionIsDetected() {
        assertTrue(isNewer("1.15.1", "1.15.0"))
        assertTrue(isNewer("1.16.0", "1.15.0"))
    }

    @Test
    fun olderOrEqualIsNotNewer() {
        assertFalse(isNewer("1.14.9", "1.15.0"))
        assertFalse(isNewer("1.15.0", "1.15.0"))
    }

    @Test
    fun vPrefixAndMissingSegmentsAreNormalized() {
        assertTrue(isNewer("v1.16", "1.15.0"))
        assertTrue(isNewer("v1.16.1", "1.15"))
        assertFalse(isNewer("1.15", "1.15.0"))
    }

    @Test
    fun snapshotSuffixIsIgnored() {
        assertEquals(0, segment("1.15.0-SNAPSHOT", 2))
        assertFalse(isNewer("1.15.0", "1.15.0-SNAPSHOT"))
    }
}
