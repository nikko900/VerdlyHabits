package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Large animated completion ring for the dashboard.
 *
 *  - Outer ring fills smoothly from 0 → [progress].
 *  - A faint rotating "comet" highlights the arc tip for that premium feel.
 *  - Reads percentage + subtitle inside the ring.
 */
@Composable
fun DashboardProgressRing(
    progress: Float,
    title: String = "today",
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    diameter: Dp = 156.dp,
    strokeWidth: Dp = 12.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(900),
        label = "ring_progress"
    )
    val rotation by rememberInfiniteTransition(label = "ring_comet")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
            label = "ring_comet_rotation"
        )

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    val accent = androidx.compose.ui.graphics.Color(0xFFFFB300)

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val arcPad = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val arcTopLeft = Offset(arcPad, arcPad)

            // Track
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // Progress arc with gradient
            if (animated > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(primary, accent, primary)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            // Comet — only when not fully complete
            if (animated > 0f && animated < 1f) {
                rotate(rotation) {
                    drawCircle(
                        color = primary.copy(alpha = 0.55f),
                        radius = stroke * 0.45f,
                        center = Offset(center.x, arcPad + stroke / 2f)
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(animated * 100).toInt()}%",
                fontSize = (diameter.value * 0.22f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

