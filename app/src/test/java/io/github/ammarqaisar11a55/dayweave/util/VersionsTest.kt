package io.github.ammarqaisar11a55.dayweave.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {
    @Test fun `numbers compare as numbers, not text`() {
        assertTrue(isNewer("0.10.0", "0.9.0"))
        assertTrue(isNewer("1.0.0", "0.99.99"))
        assertFalse(isNewer("0.9.0", "0.10.0"))
    }

    @Test fun `a v prefix and missing parts don't matter`() {
        assertEquals(0, compareVersions("v1.2", "1.2.0"))
        assertFalse(isNewer("v0.1.0", "0.1.0"))
    }

    @Test fun `a pre-release comes before its release`() {
        assertTrue(isNewer("1.2.0", "1.2.0-beta.1"))
        assertFalse(isNewer("1.2.0-beta.1", "1.2.0"))
        assertTrue(isNewer("1.2.0-beta.2", "1.2.0-beta.1"))
    }

    @Test fun `build metadata is ignored`() {
        assertEquals(0, compareVersions("1.0.0+5", "1.0.0"))
    }
}
