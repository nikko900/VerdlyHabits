package com.saintnico.verdlyhabits.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Trophy + fire leaning into each other — the unified Arena tab mark.
 * Trophy tilts slightly left; flame tilts slightly right so they read as one badge.
 */
@Composable
fun ArenaConjoinedIcon(
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 18.dp,
) {
    Box(modifier = modifier.size(26.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Rounded.EmojiEvents,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.CenterStart)
                .offset(x = (-1).dp, y = 1.dp)
                .graphicsLayer {
                    rotationZ = -14f
                    scaleX = 0.95f
                    scaleY = 0.95f
                },
        )
        Icon(
            imageVector = Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.CenterEnd)
                .offset(x = 1.dp, y = (-1).dp)
                .graphicsLayer {
                    rotationZ = 14f
                    scaleX = 0.92f
                    scaleY = 0.92f
                },
        )
    }
}
