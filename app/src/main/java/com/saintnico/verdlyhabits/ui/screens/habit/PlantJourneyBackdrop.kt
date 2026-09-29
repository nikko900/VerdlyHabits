package com.saintnico.verdlyhabits.ui.screens.habit

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.lerp
import kotlin.math.min

private val Mint = Color(0xFF52B788)
private val Gold = Color(0xFFFFB300)
private val GoldDark = Color(0xFFD4890A)
private val Amber = Color(0xFFD4890A)
private val Forest = Color(0xFF1B4332)

internal enum class JourneyPhase {
    Dawn, Warming, Balanced, Amber, Studio, Peak,
}

internal data class GlowBlob(
    val color: Color,
    val alpha: Float,
    val centerX: Float,
    val centerY: Float,
    val radiusFactor: Float,
)

internal data class PhaseGlows(
    val baseTop: Color,
    val baseBottom: Color,
    val primary: GlowBlob,
    val secondary: GlowBlob,
    val motifScale: Float,
    val motifTint: Color,
)

private fun phaseGlows(step: Int, selectedColor: Color?, dark: Boolean): PhaseGlows {
    val phase = when (step.coerceIn(1, 6)) {
        1 -> JourneyPhase.Dawn
        2 -> JourneyPhase.Warming
        3 -> JourneyPhase.Balanced
        4 -> JourneyPhase.Amber
        5 -> JourneyPhase.Studio
        else -> JourneyPhase.Peak
    }
    val accent = selectedColor ?: Mint
    val complement = lerp(accent, Gold, 0.35f)

    return when (phase) {
        JourneyPhase.Dawn -> if (dark) {
            PhaseGlows(
                baseTop = Color(0xFF0E1210),
                baseBottom = Color(0xFF0A0F0D),
                primary = GlowBlob(Mint, 0.14f, 0.5f, 0.14f, 0.95f),
                secondary = GlowBlob(GoldDark, 0.06f, 0.82f, 0.72f, 0.75f),
                motifScale = 0.88f,
                motifTint = Mint,
            )
        } else {
            PhaseGlows(
                baseTop = Color(0xFFF4FAF7),
                baseBottom = Color(0xFFF9F6F0),
                primary = GlowBlob(Mint, 0.08f, 0.5f, 0.12f, 0.92f),
                secondary = GlowBlob(Gold, 0.04f, 0.85f, 0.70f, 0.72f),
                motifScale = 0.82f,
                motifTint = Mint,
            )
        }
        JourneyPhase.Warming -> if (dark) {
            PhaseGlows(
                baseTop = Color(0xFF141008),
                baseBottom = Color(0xFF0E100E),
                primary = GlowBlob(GoldDark, 0.18f, 0.48f, 0.13f, 1.0f),
                secondary = GlowBlob(Mint, 0.08f, 0.12f, 0.68f, 0.78f),
                motifScale = 0.92f,
                motifTint = GoldDark,
            )
        } else {
            PhaseGlows(
                baseTop = Color(0xFFFBF0DC),
                baseBottom = Color(0xFFF9F6F0),
                primary = GlowBlob(Gold, 0.10f, 0.48f, 0.12f, 0.98f),
                secondary = GlowBlob(Mint, 0.06f, 0.14f, 0.66f, 0.74f),
                motifScale = 0.86f,
                motifTint = Gold,
            )
        }
        JourneyPhase.Balanced -> if (dark) {
            PhaseGlows(
                baseTop = Color(0xFF101612),
                baseBottom = Color(0xFF0C100E),
                primary = GlowBlob(Mint, 0.16f, 0.18f, 0.15f, 0.92f),
                secondary = GlowBlob(GoldDark, 0.16f, 0.82f, 0.62f, 0.88f),
                motifScale = 0.96f,
                motifTint = Mint,
            )
        } else {
            PhaseGlows(
                baseTop = Color(0xFFF0FAF4),
                baseBottom = Color(0xFFFBF6EC),
                primary = GlowBlob(Mint, 0.10f, 0.16f, 0.14f, 0.90f),
                secondary = GlowBlob(Gold, 0.10f, 0.84f, 0.60f, 0.86f),
                motifScale = 0.90f,
                motifTint = Mint,
            )
        }
        JourneyPhase.Amber -> if (dark) {
            PhaseGlows(
                baseTop = Color(0xFF181006),
                baseBottom = Color(0xFF0A100C),
                primary = GlowBlob(Amber, 0.22f, 0.52f, 0.16f, 1.02f),
                secondary = GlowBlob(Forest, 0.10f, 0.10f, 0.78f, 0.72f),
                motifScale = 1.0f,
                motifTint = Amber,
            )
        } else {
            PhaseGlows(
                baseTop = Color(0xFFFAECD8),
                baseBottom = Color(0xFFF3F7F2),
                primary = GlowBlob(Amber, 0.14f, 0.50f, 0.15f, 1.0f),
                secondary = GlowBlob(Forest, 0.05f, 0.08f, 0.76f, 0.68f),
                motifScale = 0.94f,
                motifTint = Amber,
            )
        }
        JourneyPhase.Studio -> if (dark) {
            PhaseGlows(
                baseTop = lerp(Color(0xFF0E1210), accent, 0.12f),
                baseBottom = Color(0xFF0A0F0D),
                primary = GlowBlob(accent, 0.24f, 0.55f, 0.14f, 1.05f),
                secondary = GlowBlob(complement, 0.12f, 0.15f, 0.70f, 0.82f),
                motifScale = 1.04f,
                motifTint = accent,
            )
        } else {
            PhaseGlows(
                baseTop = lerp(Color(0xFFF6FAF8), accent, 0.08f),
                baseBottom = Color(0xFFF9F6F0),
                primary = GlowBlob(accent, 0.16f, 0.55f, 0.13f, 1.0f),
                secondary = GlowBlob(complement, 0.08f, 0.18f, 0.68f, 0.78f),
                motifScale = 1.0f,
                motifTint = accent,
            )
        }
        JourneyPhase.Peak -> if (dark) {
            PhaseGlows(
                baseTop = lerp(Color(0xFF121008), accent, 0.15f),
                baseBottom = Color(0xFF0A0F0D),
                primary = GlowBlob(accent, 0.30f, 0.50f, 0.12f, 1.12f),
                secondary = GlowBlob(GoldDark, 0.14f, 0.78f, 0.58f, 0.95f),
                motifScale = 1.08f,
                motifTint = accent,
            )
        } else {
            PhaseGlows(
                baseTop = lerp(Color(0xFFFBF4E8), accent, 0.10f),
                baseBottom = Color(0xFFF9F6F0),
                primary = GlowBlob(accent, 0.20f, 0.50f, 0.11f, 1.08f),
                secondary = GlowBlob(Gold, 0.10f, 0.80f, 0.55f, 0.92f),
                motifScale = 1.06f,
                motifTint = accent,
            )
        }
    }
}

private fun lerpPhase(from: PhaseGlows, to: PhaseGlows, t: Float): PhaseGlows {
    val f = t.coerceIn(0f, 1f)
    fun lerpBlob(a: GlowBlob, b: GlowBlob) = GlowBlob(
        color = lerp(a.color, b.color, f),
        alpha = a.alpha + (b.alpha - a.alpha) * f,
        centerX = a.centerX + (b.centerX - a.centerX) * f,
        centerY = a.centerY + (b.centerY - a.centerY) * f,
        radiusFactor = a.radiusFactor + (b.radiusFactor - a.radiusFactor) * f,
    )
    return PhaseGlows(
        baseTop = lerp(from.baseTop, to.baseTop, f),
        baseBottom = lerp(from.baseBottom, to.baseBottom, f),
        primary = lerpBlob(from.primary, to.primary),
        secondary = lerpBlob(from.secondary, to.secondary),
        motifScale = from.motifScale + (to.motifScale - from.motifScale) * f,
        motifTint = lerp(from.motifTint, to.motifTint, f),
    )
}

@Composable
fun PlantJourneyBackdrop(
    currentStep: Int,
    pagerOffset: Float = 0f,
    selectedColor: Color?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val dark = PlantJourneyGlass.isDarkBackground(background)
    val from = phaseGlows(currentStep.coerceIn(1, 6), selectedColor, dark)
    val toStep = (currentStep + 1).coerceIn(1, 6)
    val to = phaseGlows(toStep, selectedColor, dark)
    val blend by animateFloatAsState(
        targetValue = pagerOffset.coerceIn(0f, 1f),
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "phase_blend",
    )
    val phase = if (currentStep >= 6 || pagerOffset <= 0f) from else lerpPhase(from, to, blend)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.matchParentSize()) {
            drawRect(
                brush = Brush.verticalGradient(listOf(phase.baseTop, phase.baseBottom)),
            )
            fun drawBlob(blob: GlowBlob) {
                val radius = size.width * blob.radiusFactor
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(blob.color.copy(alpha = blob.alpha), Color.Transparent),
                        center = Offset(size.width * blob.centerX, size.height * blob.centerY),
                        radius = radius,
                    ),
                    radius = radius,
                    center = Offset(size.width * blob.centerX, size.height * blob.centerY),
                )
            }
            drawBlob(phase.primary)
            drawBlob(phase.secondary)

            val motifSize = min(size.width, size.height) * 0.42f * phase.motifScale
            val motifPath = Path().apply {
                moveTo(size.width * 0.92f, size.height * 0.78f)
                cubicTo(
                    size.width * 0.86f, size.height * 0.62f,
                    size.width * 0.98f, size.height * 0.52f,
                    size.width * 1.06f, size.height * 0.66f,
                )
                cubicTo(
                    size.width * 1.02f, size.height * 0.88f,
                    size.width * 0.96f, size.height * 0.96f,
                    size.width * 0.88f, size.height * 0.92f,
                )
                close()
            }
            drawPath(
                path = motifPath,
                color = phase.motifTint.copy(alpha = if (dark) 0.06f else 0.05f),
                style = Fill,
            )
        }
        content()
    }
}
