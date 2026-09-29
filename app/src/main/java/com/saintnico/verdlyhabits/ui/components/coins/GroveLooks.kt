package com.saintnico.verdlyhabits.ui.components.coins

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saintnico.verdlyhabits.data.model.CoinCosmetics
import com.saintnico.verdlyhabits.data.model.CosmeticKind
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

fun groveAccent(id: String?): Color {
    val item = CoinCosmetics.find(id) ?: return Color(0xFF52B788)
    return Color(item.previewArgb)
}

fun glowIsLoud(glowId: String?): Boolean =
    glowId == "glow_goldleaf" || glowId == "glow_crownfire" || glowId == "glow_night" || glowId == "glow_moonveil"

@Composable
fun pulseAlpha(glowId: String?): Float {
    if (glowId.isNullOrBlank()) return 0.5f
    val inf = rememberInfiniteTransition(label = "grove-pulse-$glowId")
    val duration = when (glowId) {
        "glow_crownfire" -> 1400
        "glow_goldleaf" -> 1800
        "glow_night" -> 2600
        "glow_moonveil" -> 3200
        else -> 2000
    }
    // High floor: the ring breathes, it never dies out. A glow that keeps sinking to
    // half-dark reads as broken, not premium.
    val a by inf.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(duration), RepeatMode.Reverse),
        label = "pulse",
    )
    return a
}

/** Jewelry-grade keyline per look — gradient metal, not a flat stroke. */
fun groveRingBrush(glowId: String?, fallback: Color): Brush = when (glowId) {
    "glow_ember" -> Brush.sweepGradient(
        listOf(Color(0xFFFFD9A0), Color(0xFFE08A2E), Color(0xFF9C5A17), Color(0xFFFFD9A0)),
    )
    "glow_moss" -> Brush.sweepGradient(
        listOf(Color(0xFFB7E4C7), Color(0xFF52B788), Color(0xFF2D6A4F), Color(0xFFB7E4C7)),
    )
    "glow_dew" -> Brush.sweepGradient(
        listOf(Color(0xFFF0FFF7), Color(0xFFB7E4C7), Color(0xFF95D5B2), Color(0xFFF0FFF7)),
    )
    "glow_mist" -> Brush.sweepGradient(
        listOf(Color(0xFFC5CAE9), Color(0xFF9FA8DA), Color(0xFF7986CB), Color(0xFFC5CAE9)),
    )
    "glow_aurora" -> Brush.sweepGradient(
        listOf(Color(0xFFE1BEE7), Color(0xFFB388FF), Color(0xFF7C4DFF), Color(0xFF64FFDA), Color(0xFFE1BEE7)),
    )
    "glow_night" -> Brush.sweepGradient(
        listOf(Color(0xFF9FA8DA), Color(0xFF5B4B8A), Color(0xFF283048), Color(0xFF9FA8DA)),
    )
    "glow_goldleaf" -> Brush.sweepGradient(
        listOf(Color(0xFFFFF3C4), Color(0xFFFFC857), Color(0xFFB8860B), Color(0xFFFFE082), Color(0xFFFFF3C4)),
    )
    "glow_crownfire" -> Brush.sweepGradient(
        listOf(Color(0xFFFFE082), Color(0xFFFF6B35), Color(0xFFB8860B), Color(0xFFFF8F5C), Color(0xFFFFE082)),
    )
    "glow_moonveil" -> Brush.sweepGradient(
        listOf(Color(0xFFFFFFFF), Color(0xFFC5CAE9), Color(0xFF7986CB), Color(0xFFE8EAF6), Color(0xFFFFFFFF)),
    )
    else -> Brush.sweepGradient(listOf(fallback, fallback.copy(alpha = 0.6f), fallback))
}

fun titleBorderWidth(glowId: String?): Dp = when (glowId) {
    null, "" -> 1.dp
    "glow_ember" -> 2.5.dp
    "glow_moss", "glow_dew" -> 3.dp
    "glow_aurora", "glow_mist" -> 3.dp
    "glow_night" -> 3.5.dp
    "glow_moonveil" -> 3.5.dp
    "glow_goldleaf" -> 4.dp
    "glow_crownfire" -> 4.5.dp
    else -> 2.5.dp
}

@Composable
fun Modifier.groveTitleAura(glowId: String?, glowColor: Color): Modifier {
    val pulse = pulseAlpha(glowId)
    val sparkle = rememberInfiniteTransition(label = "sparkle-$glowId")
    val spin by sparkle.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7200, easing = LinearEasing)),
        label = "spin",
    )
    if (glowId.isNullOrBlank()) return this
    val ringBrush = groveRingBrush(glowId, glowColor)
    return this.drawBehind {
        val pill = androidx.compose.ui.geometry.CornerRadius(size.height)

        // 1. Wide soft bloom — light pooling around the pill, not a hard slab.
        val bloomPad = 16.dp.toPx()
        drawRoundRect(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.30f * pulse), Color.Transparent),
                center = Offset(size.width / 2, size.height / 2),
                radius = size.maxDimension * 0.85f,
            ),
            topLeft = Offset(-bloomPad, -bloomPad),
            size = Size(size.width + bloomPad * 2, size.height + bloomPad * 2),
            cornerRadius = pill,
        )

        // 2. Tight core glow directly behind the title.
        val corePad = 7.dp.toPx()
        drawRoundRect(
            color = glowColor.copy(alpha = 0.16f + pulse * 0.10f),
            topLeft = Offset(-corePad / 2, -corePad / 2),
            size = Size(size.width + corePad, size.height + corePad),
            cornerRadius = pill,
        )

        // 3. Gradient metal keyline — the ring itself.
        val keyPad = 5.dp.toPx()
        drawRoundRect(
            brush = ringBrush,
            topLeft = Offset(-keyPad / 2, -keyPad / 2),
            size = Size(size.width + keyPad, size.height + keyPad),
            cornerRadius = pill,
            style = Stroke(width = 1.8.dp.toPx()),
            alpha = 0.75f + pulse * 0.25f,
        )

        // 4. Loud looks: slow-orbiting sparks with a bright core and soft halo each.
        if (glowIsLoud(glowId)) {
            rotate(spin) {
                val cx = size.width / 2
                val cy = size.height / 2
                repeat(6) { i ->
                    val ang = Math.toRadians((i * 60).toDouble())
                    val x = cx + cos(ang).toFloat() * size.width * 0.52f
                    val y = cy + sin(ang).toFloat() * size.height * 0.95f
                    drawCircle(
                        color = glowColor.copy(alpha = 0.18f * pulse),
                        radius = 4.5.dp.toPx(),
                        center = Offset(x, y),
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f + pulse * 0.4f),
                        radius = 1.4.dp.toPx(),
                        center = Offset(x, y),
                    )
                }
            }
        }

        // 5. Crownfire: a second ember ring breathing just outside the gold one.
        if (glowId == "glow_crownfire") {
            val emberPad = 9.dp.toPx()
            drawRoundRect(
                color = Color(0xFFFF6B35).copy(alpha = 0.45f * pulse),
                topLeft = Offset(-emberPad / 2, -emberPad / 2),
                size = Size(size.width + emberPad, size.height + emberPad),
                cornerRadius = pill,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }
    }
}

@Composable
fun GroveAvatarHalo(
    glowId: String?,
    frameId: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val glow = groveAccent(glowId)
    val frame = CoinCosmetics.find(frameId)
    val pulse = pulseAlpha(glowId ?: frameId)
    val outer = when (frameId) {
        "frame_goldleaf" -> 10.dp
        "frame_grove_lead" -> 9.dp
        "frame_aurora" -> 8.dp
        "frame_oak" -> 7.dp
        "frame_willow" -> 6.dp
        else -> if (glowId.isNullOrBlank()) 0.dp else 5.dp
    }
    val frameColor = frame?.let { Color(it.previewArgb) } ?: glow
    val ringBrush = when (frameId) {
        "frame_goldleaf" -> Brush.sweepGradient(
            listOf(Color(0xFFFFF3C4), Color(0xFFFFC857), Color(0xFFB8860B), Color(0xFFFFF3C4)),
        )
        "frame_grove_lead" -> Brush.sweepGradient(
            listOf(Color(0xFF95D5B2), Color(0xFF52B788), Color(0xFFFFC857), Color(0xFF2D6A4F), Color(0xFF95D5B2)),
        )
        "frame_aurora" -> Brush.sweepGradient(
            listOf(Color(0xFFB388FF), Color(0xFF7C4DFF), Color(0xFFE1BEE7), Color(0xFFB388FF)),
        )
        "frame_oak" -> Brush.linearGradient(listOf(Color(0xFFD4A373), Color(0xFF7F4F24)))
        "frame_willow" -> Brush.linearGradient(listOf(Color(0xFFD8F3DC), Color(0xFF52B788)))
        else -> Brush.radialGradient(listOf(glow.copy(alpha = 0.95f), glow.copy(alpha = 0.35f)))
    }
    Box(
        modifier = modifier
            .size(size + outer * 2)
            .drawBehind {
                val center = Offset(this.size.width / 2, this.size.height / 2)
                val haloR = size.toPx() / 2 + outer.toPx() + 8.dp.toPx()
                if (!glowId.isNullOrBlank()) {
                    // Soft radial bloom — light falls off, it doesn't stop at a hard disc edge.
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glow.copy(alpha = 0.42f * pulse),
                                glow.copy(alpha = 0.12f * pulse),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = haloR * 1.25f,
                        ),
                        radius = haloR * 1.25f,
                        center = center,
                    )
                }
                if (frameId == "frame_goldleaf") {
                    // Gilded leaf ticks around the bezel — the detail that reads "crafted".
                    val tickR = size.toPx() / 2 + outer.toPx() * 0.55f
                    repeat(12) { i ->
                        val ang = Math.toRadians((i * 30).toDouble())
                        val dir = Offset(cos(ang).toFloat(), sin(ang).toFloat())
                        drawLine(
                            color = Color(0xFFFFF3C4).copy(alpha = 0.55f + pulse * 0.35f),
                            start = center + Offset(dir.x * (tickR - 3.dp.toPx()), dir.y * (tickR - 3.dp.toPx())),
                            end = center + Offset(dir.x * (tickR + 3.dp.toPx()), dir.y * (tickR + 3.dp.toPx())),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
            .border(
                width = if (frame != null) (outer / 2).coerceAtLeast(3.dp) else if (glowId != null) 3.dp else 0.dp,
                brush = ringBrush,
                shape = CircleShape,
            )
            .border(
                width = if (frameId == "frame_goldleaf" || frameId == "frame_aurora" || frameId == "frame_grove_lead") 2.dp else 0.dp,
                color = frameColor.copy(alpha = 0.85f * pulse),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

fun Modifier.grovePlantBackdrop(plantId: String?): Modifier {
    val plant = CoinCosmetics.find(plantId) ?: return this
    val c = Color(plant.previewArgb)
    return this.drawWithContent {
        when (plant.id) {
            "plant_fern" -> {
                drawCircle(c.copy(alpha = 0.22f), radius = size.minDimension * 0.42f, center = Offset(size.width * 0.08f, size.height * 0.18f))
                drawCircle(c.copy(alpha = 0.16f), radius = size.minDimension * 0.35f, center = Offset(size.width * 0.92f, size.height * 0.22f))
            }
            "plant_ivy" -> {
                val path = Path().apply {
                    moveTo(0f, size.height * 0.12f)
                    quadraticTo(size.width * 0.2f, 0f, size.width * 0.12f, size.height * 0.55f)
                    quadraticTo(size.width * 0.05f, size.height * 0.9f, 0f, size.height)
                }
                drawPath(path, c.copy(alpha = 0.28f), style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round))
                val pathR = Path().apply {
                    moveTo(size.width, size.height * 0.08f)
                    quadraticTo(size.width * 0.8f, size.height * 0.2f, size.width * 0.92f, size.height * 0.7f)
                }
                drawPath(pathR, c.copy(alpha = 0.24f), style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round))
            }
            "plant_blossom" -> {
                drawCircle(c.copy(alpha = 0.32f), radius = size.minDimension * 0.28f, center = Offset(size.width * 0.82f, size.height * 0.16f))
                drawCircle(c.copy(alpha = 0.18f), radius = size.minDimension * 0.18f, center = Offset(size.width * 0.18f, size.height * 0.28f))
            }
            "plant_goldleaf" -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(c.copy(alpha = 0.38f), Color.Transparent, c.copy(alpha = 0.16f)),
                    ),
                )
                repeat(8) { i ->
                    val x = size.width * (0.08f + i * 0.12f)
                    drawCircle(c.copy(alpha = 0.35f), radius = 6.dp.toPx(), center = Offset(x, size.height * 0.08f))
                }
            }
        }
        drawContent()
        if (plant.id == "plant_goldleaf") {
            drawRect(Color.Black.copy(alpha = 0.04f))
        }
    }
}

@Composable
fun GrovePreviewSwatch(
    itemId: String,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val item = CoinCosmetics.find(itemId) ?: return
    val color = Color(item.previewArgb)
    val pulse = pulseAlpha(item.id)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.22f + pulse * 0.18f))
            .border(
                width = if (item.hero) 4.dp else 2.5.dp,
                color = color.copy(alpha = 0.55f + pulse * 0.4f),
                shape = CircleShape,
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            when (item.kind) {
                CosmeticKind.TITLE_GLOW -> {
                    drawCircle(color, radius = size.toPx() * 0.18f)
                    drawCircle(color.copy(alpha = 0.35f), radius = size.toPx() * 0.36f, style = Stroke(3.dp.toPx()))
                }
                CosmeticKind.PROFILE_FRAME -> {
                    drawCircle(Color.White.copy(alpha = 0.15f), radius = size.toPx() * 0.28f)
                    drawCircle(color, radius = size.toPx() * 0.38f, style = Stroke(5.dp.toPx()))
                }
                CosmeticKind.CHECK_SKIN -> {
                    repeat(5) { i ->
                        val ang = Math.toRadians((i * 72).toDouble())
                        drawCircle(
                            color,
                            radius = 3.5.dp.toPx(),
                            center = Offset(
                                size.toPx() / 2 + cos(ang).toFloat() * size.toPx() * 0.28f,
                                size.toPx() / 2 + sin(ang).toFloat() * size.toPx() * 0.28f,
                            ),
                        )
                    }
                }
                CosmeticKind.OATH_CHIP -> {
                    drawRoundRect(
                        color,
                        topLeft = Offset(size.toPx() * 0.18f, size.toPx() * 0.38f),
                        size = Size(size.toPx() * 0.64f, size.toPx() * 0.22f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()),
                    )
                }
                CosmeticKind.GROVE_PLANT -> {
                    drawCircle(color, radius = 8.dp.toPx(), center = Offset(size.toPx() * 0.5f, size.toPx() * 0.62f))
                    drawCircle(color.copy(alpha = 0.7f), radius = 6.dp.toPx(), center = Offset(size.toPx() * 0.35f, size.toPx() * 0.4f))
                    drawCircle(color.copy(alpha = 0.7f), radius = 6.dp.toPx(), center = Offset(size.toPx() * 0.65f, size.toPx() * 0.4f))
                }
            }
        }
    }
}

@Composable
fun CheckOffSpectacle(
    trigger: Any?,
    skinId: String?,
    modifier: Modifier = Modifier,
) {
    if (skinId.isNullOrBlank()) return
    var play by remember { mutableStateOf(false) }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        play = true
        delay(if (skinId == "check_mythic" || skinId == "check_goldleaf") 1800 else 1200)
        play = false
    }
    if (!play) return
    val progress by animateFloatAsState(
        targetValue = if (play) 1f else 0f,
        animationSpec = tween(900),
        label = "spectacle",
    )
    val color = groveAccent(skinId)
    Canvas(modifier.fillMaxSize()) {
        when (skinId) {
            "check_bloom" -> {
                repeat(14) { i ->
                    val ang = Math.toRadians((i * 25.7).toDouble())
                    val dist = size.minDimension * 0.12f * progress
                    drawOval(
                        color = color.copy(alpha = (1f - progress) * 0.7f),
                        topLeft = Offset(
                            size.width / 2 + cos(ang).toFloat() * dist * 2.2f - 10.dp.toPx(),
                            size.height * 0.38f + sin(ang).toFloat() * dist * 1.6f - 16.dp.toPx(),
                        ),
                        size = Size(20.dp.toPx(), 32.dp.toPx()),
                    )
                }
            }
            "check_rain" -> {
                repeat(18) { i ->
                    val x = size.width * ((i * 0.07f + 0.04f) % 1f)
                    val y = (size.height * progress * (0.4f + (i % 5) * 0.12f)) % size.height
                    drawCircle(Color(0xFFF3B23E).copy(alpha = 0.85f), radius = 5.dp.toPx(), center = Offset(x, y))
                    drawCircle(Color(0xFFFFF3C4).copy(alpha = 0.5f), radius = 2.dp.toPx(), center = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()))
                }
            }
            "check_vine" -> {
                val path = Path().apply {
                    moveTo(0f, size.height * 0.42f)
                    quadraticTo(size.width * 0.35f * progress, size.height * 0.2f, size.width * progress, size.height * 0.48f)
                }
                drawPath(path, color.copy(alpha = 0.85f), style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                val path2 = Path().apply {
                    moveTo(size.width, size.height * 0.58f)
                    quadraticTo(size.width * (1f - 0.4f * progress), size.height * 0.8f, size.width * (1f - progress), size.height * 0.55f)
                }
                drawPath(path2, color.copy(alpha = 0.7f), style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
            }
            "check_mythic" -> {
                drawRect(color.copy(alpha = (1f - progress) * 0.28f))
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(color.copy(alpha = 0.45f * (1f - progress)), Color.Transparent),
                    ),
                    radius = size.minDimension * (0.4f + progress * 0.8f),
                    center = Offset(size.width / 2, size.height * 0.4f),
                )
            }
            "check_goldleaf" -> {
                drawRect(Color(0xFFFFC857).copy(alpha = (1f - progress) * 0.22f))
                repeat(12) { i ->
                    val x = size.width * ((i * 0.09f + 0.05f) % 1f)
                    val y = size.height * progress * (0.3f + (i % 4) * 0.15f)
                    rotate(25f + i * 12f, pivot = Offset(x, y)) {
                        drawOval(
                            Color(0xFFE8C547).copy(alpha = 0.9f),
                            topLeft = Offset(x - 6.dp.toPx(), y - 12.dp.toPx()),
                            size = Size(12.dp.toPx(), 22.dp.toPx()),
                        )
                    }
                }
            }
        }
    }
}

fun checkSkinKonfetti(skinId: String?): Pair<com.saintnico.verdlyhabits.ui.components.BurstIntensity, List<Int>?> {
    val huge = com.saintnico.verdlyhabits.ui.components.BurstIntensity.HUGE
    val normal = com.saintnico.verdlyhabits.ui.components.BurstIntensity.NORMAL
    return when (skinId) {
        "check_bloom" -> normal to listOf(0xFF95D5B2.toInt(), 0xFF52B788.toInt(), 0xFFD8F3DC.toInt(), 0xFF2D6A4F.toInt())
        "check_rain" -> huge to listOf(0xFFF3B23E.toInt(), 0xFFD4890A.toInt(), 0xFFFFF3C4.toInt(), 0xFFE08A2E.toInt())
        "check_vine" -> normal to listOf(0xFF2D6A4F.toInt(), 0xFF52B788.toInt(), 0xFF95D5B2.toInt())
        "check_mythic" -> huge to listOf(0xFFFFC857.toInt(), 0xFFB388FF.toInt(), 0xFF52B788.toInt(), 0xFFFF6B35.toInt())
        "check_goldleaf" -> huge to listOf(0xFFFFC857.toInt(), 0xFFE8C547.toInt(), 0xFFFFF3C4.toInt(), 0xFFB8860B.toInt())
        else -> normal to null
    }
}

fun oathChipColors(chipId: String?, atRisk: Boolean, fallback: Color): Color {
    if (atRisk) return Color(0xFFE08A2E)
    return when (chipId) {
        "oath_moss" -> Color(0xFF52B788)
        "oath_ember" -> Color(0xFFE08A2E)
        "oath_goldleaf" -> Color(0xFFFFC857)
        else -> fallback
    }
}
