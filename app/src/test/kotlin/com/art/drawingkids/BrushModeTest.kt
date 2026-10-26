package com.art.drawingkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrushModeTest {

    @Test
    fun testBrushModeValuesAndIntegrity() {
        val modes = BrushMode.values()
        assertEquals("Should have exactly 4 brush modes", 4, modes.size)
        assertTrue(modes.contains(BrushMode.PENCIL))
        assertTrue(modes.contains(BrushMode.ERASER))
        assertTrue(modes.contains(BrushMode.NEON_GLOW))
        assertTrue(modes.contains(BrushMode.RAINBOW))
    }

    @Test
    fun testBrushModeTitles() {
        for (mode in BrushMode.values()) {
            assertTrue("Brush mode string res id must be non-zero", mode.titleResId != 0)
        }
    }
}
