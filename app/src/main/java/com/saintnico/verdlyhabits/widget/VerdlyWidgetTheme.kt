package com.saintnico.verdlyhabits.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

object VerdlyWidgetTheme {
    val Bg = Color(0xFF111410)
    val Surface = Color(0xFF1A221C)
    val SurfaceElevated = Color(0xFF243028)
    val Primary = Color(0xFF1D6B44)
    val PrimarySoft = Color(0xFF2A4D3C)
    val Gold = Color(0xFFD4890A)
    val TextPrimary = Color(0xFFF4F7F2)
    val TextMuted = Color(0xFF9AA89A)
    val Done = Color(0xFF66B386)
    val Divider = Color(0xFF2E3D34)

    fun color(c: Color): ColorProvider = ColorProvider(c)
}
