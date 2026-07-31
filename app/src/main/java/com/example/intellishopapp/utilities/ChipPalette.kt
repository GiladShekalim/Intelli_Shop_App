package com.example.intellishopapp.utilities

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.example.intellishopapp.R
import com.google.android.material.button.MaterialButton

/**
 * The one place that owns the pastel selection colours and the selected/unselected
 * chip look. Registration, the Preferences/Categories editor and the Search filter
 * panel + result labels all draw from here, so a chip is styled the same everywhere
 * and the colour set lives in a single spot.
 */
object ChipPalette {

    private val COLORS = listOf(
        0xFFFFCDD2.toInt(), 0xFFF8BBD0.toInt(), 0xFFE1BEE7.toInt(), 0xFFC5CAE9.toInt(),
        0xFFB3E5FC.toInt(), 0xFFB2DFDB.toInt(), 0xFFC8E6C9.toInt(), 0xFFFFF9C4.toInt(),
        0xFFFFE0B2.toInt(), 0xFFD1C4E9.toInt()
    )

    private const val ON_TEXT = 0xFF212121.toInt()

    /** A random pastel from the shared set. */
    fun random(): Int = COLORS.random()

    /**
     * Style an outlined toggle: selected gets a random pastel with dark text; unselected
     * falls back to the light card surface with brand-coloured text (the outlined look).
     */
    fun styleToggle(button: MaterialButton, selected: Boolean, brand: Int) {
        if (selected) {
            button.backgroundTintList = ColorStateList.valueOf(random())
            button.setTextColor(ON_TEXT)
        } else {
            button.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(button.context, R.color.card_background)
            )
            button.setTextColor(brand)
        }
    }
}
