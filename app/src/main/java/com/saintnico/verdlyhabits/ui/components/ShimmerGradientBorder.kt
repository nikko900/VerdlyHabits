package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A composable container that draws an animated gradient border with a moving
 * shimmer highlight. Used around premium insight cards for that award-winning
 * visual polish.
 *
 * The border is a sweep gradient in the card's accent colors, with a bright
 * "comet" highlight that crawls around the perimeter continuously.
 *
 * @param borderWidth  Width of the stroke
 * @param cornerRadius Radius for rounded corners
 * @param gradientColors The gradient stops for the border
 * @param shimmerColor  The bright highlight that travels
 */
@Composable
fun ShimmerGradientBorder(
    modifier: Modifier = Modifier,
    borderWidth: Dp = 1.5.dp,
    cornerRadius: Dp = 18.dp,
    gradientColors: List<Color> = listOf(
        Color(0xFF7B6CF6),
        Color(0xFF2DD4BF),
        Color(0xFFF59E0B),
        Color(0xFF7B6CF6)
    ),
    shimmerColor: Color = Color.White.copy(alpha = 0.6f),
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer_border")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )

    Box(
        modifier = modifier
            .drawBehind {
                val stroke = borderWidth.toPx()
                val cr = cornerRadius.toPx()
                val inset = stroke / 2f

                // Gradient border
                drawRoundRect(
                    brush = Brush.sweepGradient(gradientColors),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(width = stroke)
                )

                // Shimmer highlight — a small bright arc that rotates
                val shimmerAngle = shimmerProgress * 360f
                val shimmerBrush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    ((shimmerAngle / 360f - 0.05f).coerceIn(0f, 1f)) to Color.Transparent,
                    (shimmerAngle / 360f) to shimmerColor,
                    ((shimmerAngle / 360f + 0.05f).coerceAtMost(1f)) to Color.Transparent,
                    1.0f to Color.Transparent
                )

                drawRoundRect(
                    brush = shimmerBrush,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(cr, cr),
                    style = Stroke(width = stroke * 2f),
                    blendMode = BlendMode.Screen
                )
            }
            .padding(borderWidth)
    ) {
        content()
    }
}
