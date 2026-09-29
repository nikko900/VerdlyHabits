package com.saintnico.verdlyhabits.ui.screens.habit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object PlantJourneyGlass {

    fun isDarkBackground(background: Color): Boolean =
        (background.red * 0.299f + background.green * 0.587f + background.blue * 0.114f) < 0.5f

    @Composable
    fun topBarColor(background: Color): Color {
        val dark = isDarkBackground(background)
        return if (dark) Color.Black.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.70f)
    }

    @Composable
    fun bottomBarColor(background: Color): Color {
        val dark = isDarkBackground(background)
        return if (dark) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.65f)
    }

    @Composable
    fun bottomBarHairline(background: Color): Color {
        val dark = isDarkBackground(background)
        return if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.40f)
    }

    @Composable
    fun inputGlassModifier(background: Color, shape: RoundedCornerShape = RoundedCornerShape(24.dp)): Modifier {
        val dark = isDarkBackground(background)
        val bg = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.60f)
        val border = if (dark) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.40f)
        return Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
    }

    @Composable
    fun summaryGlassModifier(
        background: Color,
        accent: Color,
        shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    ): Modifier {
        val dark = isDarkBackground(background)
        val bg = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.55f)
        return Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, accent.copy(alpha = if (dark) 0.22f else 0.18f), shape)
    }
}
