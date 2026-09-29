package com.saintnico.verdlyhabits.ui.components.coins

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Vector coin drawn on Canvas — no emoji, no bitmap. The premium skin swaps the
 * mint treatment for the app's gold accent language (matches Focus plant styling).
 */
@Composable
fun CoinIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    premium: Boolean = false,
) {
    Canvas(modifier = modifier.size(size)) {
        drawCoin(premium)
    }
}

internal fun DrawScope.drawCoin(premium: Boolean, alpha: Float = 1f) {
    drawCoinAt(
        premium = premium,
        alpha = alpha,
        center = Offset(this.size.width / 2f, this.size.height / 2f),
        radius = this.size.minDimension / 2f,
    )
}

internal fun DrawScope.drawCoinAt(
    premium: Boolean,
    alpha: Float,
    center: Offset,
    radius: Float,
) {

    val (rim, face, leaf) = if (premium) {
        Triple(Color(0xFFD4890A), Color(0xFFF3B23E), Color(0xFF7A4A00))
    } else {
        Triple(Color(0xFF2D6A4F), Color(0xFF52B788), Color(0xFF10331F))
    }

    // Face
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(face.copy(alpha = alpha), rim.copy(alpha = alpha)),
            center = center.copy(x = center.x - radius * 0.25f, y = center.y - radius * 0.3f),
            radius = radius * 1.6f,
        ),
        radius = radius,
        center = center,
    )
    // Rim ring
    drawCircle(
        color = rim.copy(alpha = alpha),
        radius = radius * 0.82f,
        center = center,
        style = Stroke(width = radius * 0.10f),
    )
    // Sprout mark — small leaf curve, ties coins to the Verdly identity
    val leafPath = Path().apply {
        moveTo(center.x, center.y + radius * 0.38f)
        quadraticBezierTo(
            center.x - radius * 0.05f, center.y,
            center.x, center.y - radius * 0.36f,
        )
        quadraticBezierTo(
            center.x + radius * 0.42f, center.y - radius * 0.12f,
            center.x + radius * 0.02f, center.y + radius * 0.05f,
        )
    }
    drawPath(
        path = leafPath,
        color = leaf.copy(alpha = alpha),
        style = Stroke(width = radius * 0.12f),
    )
}
