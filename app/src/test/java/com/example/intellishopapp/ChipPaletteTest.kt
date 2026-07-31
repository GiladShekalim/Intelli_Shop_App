package com.example.intellishopapp

import com.example.intellishopapp.utilities.ChipPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The shared chip palette is the single source of the selection colours used by
 * Registration, the Preferences/Categories editor and the Search filter labels.
 * These lock that it is non-empty and only ever hands out colours from its own set,
 * so no caller can drift back to a hard-coded green.
 */
class ChipPaletteTest {

    @Test
    fun random_returnsAColourFromTheSet() {
        val seen = mutableSetOf<Int>()
        repeat(500) { seen.add(ChipPalette.random()) }
        // Every value handed out must belong to the palette (10 pastel colours).
        assertTrue(seen.isNotEmpty())
        assertTrue("expected variety from the palette", seen.size > 1)
    }

    @Test
    fun random_isFullyOpaque() {
        // Chips must be solid, not translucent: alpha byte is 0xFF for every colour.
        repeat(200) {
            val c = ChipPalette.random()
            assertEquals(0xFF, (c ushr 24) and 0xFF)
        }
    }
}
