package io.github.ammarqaisar11a55.dayweave.ui

import io.github.ammarqaisar11a55.dayweave.ui.components.Thread
import io.github.ammarqaisar11a55.dayweave.ui.components.layOut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeaveLayoutTest {
    private fun threads(done: Int, open: Int) = List(done) { Thread("done $it", true) } + List(open) { Thread("open $it", false) }

    @Test fun `few items are centred between faint threads`() {
        val result = layOut(threads(1, 2), minimum = 5, maximum = 8)
        assertEquals(5, result.size)
        assertEquals(listOf(true, false, false, false, true), result.map { it.ghost })
    }

    @Test fun `a fitting list is kept as it is`() {
        val items = threads(3, 3)
        assertEquals(items, layOut(items, minimum = 5, maximum = 8))
    }

    @Test fun `too many items become a proportional summary`() {
        val result = layOut(threads(10, 10), minimum = 5, maximum = 8)
        assertEquals(8, result.size)
        assertEquals(4, result.count { it.done })
        assertTrue(result.none { it.ghost })
    }

    @Test fun `an empty list still shows the loom`() {
        val result = layOut(emptyList(), minimum = 8, maximum = 14)
        assertEquals(8, result.size)
        assertTrue(result.all { it.ghost })
    }
}
