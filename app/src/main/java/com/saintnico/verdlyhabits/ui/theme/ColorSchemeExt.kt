package com.saintnico.verdlyhabits.ui.theme

import androidx.compose.material3.ColorScheme

/**
 * Whether the active Material [ColorScheme] is visually dark.
 * Uses [ColorScheme.background] so it follows the in-app light/dark toggle, not only system UI mode.
 */
fun ColorScheme.isAppearanceDark(): Boolean {
    val y = 0.2126f * background.red + 0.7152f * background.green + 0.0722f * background.blue
    return y < 0.45f
}
