package com.saintnico.verdlyhabits.ui.components

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.TrophyCatalog
import com.saintnico.verdlyhabits.engine.TrophyTierLevel
import kotlin.math.cos
import kotlin.math.sin

/**
 * Trophy badge: Figma-exported rank frame ([trophyFrameDrawableId]) when present in
 * `res/drawable*`, otherwise vector Canvas fallback. Center icon from [TrophyCatalog].
 */
@Composable
fun AchievementTrophy(
    icon: ImageVector,
    tier: TrophyTier,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
) {
    val context = LocalContext.current
    @DrawableRes val frameRes = remember(tier) { trophyFrameDrawableId(context, tier) }
    val palette = tierPalette(tier, unlocked)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        if (frameRes != 0) {
            val pulse by rememberInfiniteTransition(label = "trophy_asset_pulse")
                .animateFloat(
                    initialValue = 0.94f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(2200, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse,
                    ),
                    label = "trophy_asset_pulse_anim",
                )
            Image(
                painter = painterResource(frameRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = if (unlocked) {
                    null
                } else {
                    ColorFilter.colorMatrix(
                        androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0.35f) },
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (unlocked) (0.92f + (pulse - 0.94f) * 0.5f) else 0.72f),
            )
        } else {
            AchievementTrophyCanvasFallback(tier = tier, unlocked = unlocked, size = size)
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tierIconTint(tier, unlocked),
            modifier = Modifier.size(size * 0.36f),
        )
    }
}

/** Runtime lookup — add PNGs to `res/drawable-nodpi/` without changing code. */
fun trophyFrameDrawableId(context: Context, tier: TrophyTier): Int {
    val name = when (tier) {
        TrophyTier.BRONZE -> "trophy_frame_bronze"
        TrophyTier.SILVER -> "trophy_frame_silver"
        TrophyTier.GOLD -> "trophy_frame_gold"
        TrophyTier.PLATINUM -> "trophy_frame_platinum"
        TrophyTier.OBSIDIAN -> "trophy_frame_obsidian"
        TrophyTier.MYTHIC -> "trophy_frame_mythic"
    }
    return context.resources.getIdentifier(name, "drawable", context.packageName)
}

@Composable
private fun AchievementTrophyCanvasFallback(
    tier: TrophyTier,
    unlocked: Boolean,
    size: Dp,
) {
    val palette = tierPalette(tier, unlocked)

    val specular by rememberInfiniteTransition(label = "trophy_spec")
        .animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(5500, easing = LinearEasing)),
            label = "trophy_spec_rot",
        )

    val pulse by rememberInfiniteTransition(label = "trophy_pulse")
        .animateFloat(
            initialValue = 0.88f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(2200, easing = FastOutSlowInEasing),
                RepeatMode.Reverse,
            ),
            label = "trophy_pulse",
        )

    val glowAlpha = if (unlocked) (0.22f + (pulse - 0.88f) * 0.35f) else 0.10f

    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h * 0.45f
        val emblemRadius = w * 0.34f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.glow.copy(alpha = glowAlpha), Color.Transparent),
                center = Offset(cx, cy),
                radius = emblemRadius * 1.55f,
            ),
            radius = emblemRadius * 1.42f,
            center = Offset(cx, cy),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.ringHi, palette.ringLo),
                center = Offset(cx, cy - emblemRadius * 0.4f),
                radius = emblemRadius * 1.6f,
            ),
            radius = emblemRadius * 1.18f,
            center = Offset(cx, cy),
        )

        if (tier == TrophyTier.PLATINUM || tier == TrophyTier.OBSIDIAN || tier == TrophyTier.MYTHIC) {
            rotate(30f, Offset(cx, cy)) {
                drawRoundRect(
                    color = palette.faceRim.copy(alpha = if (unlocked) 0.55f else 0.28f),
                    topLeft = Offset(cx - emblemRadius * 1.08f, cy - emblemRadius * 1.08f),
                    size = Size(emblemRadius * 2.16f, emblemRadius * 2.16f),
                    cornerRadius = CornerRadius(emblemRadius * 0.22f),
                    style = Stroke(width = w * 0.018f),
                )
            }
        }

        drawRoundRect(
            color = palette.plinthShadow,
            topLeft = Offset(cx - w * 0.30f, h * 0.70f),
            size = Size(w * 0.60f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(palette.plinthHi, palette.plinthLo)),
            topLeft = Offset(cx - w * 0.28f, h * 0.66f),
            size = Size(w * 0.56f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
        )

        if (unlocked) {
            val crownY = cy - emblemRadius * 1.05f
            drawCircle(palette.crown, radius = emblemRadius * 0.18f, center = Offset(cx, crownY))
            drawCircle(
                palette.crown,
                radius = emblemRadius * 0.14f,
                center = Offset(cx - emblemRadius * 0.45f, crownY + emblemRadius * 0.08f),
            )
            drawCircle(
                palette.crown,
                radius = emblemRadius * 0.14f,
                center = Offset(cx + emblemRadius * 0.45f, crownY + emblemRadius * 0.08f),
            )
        }

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(palette.faceHi, palette.faceLo),
                start = Offset(cx - emblemRadius, cy - emblemRadius),
                end = Offset(cx + emblemRadius, cy + emblemRadius),
            ),
            radius = emblemRadius,
            center = Offset(cx, cy),
        )
        drawCircle(
            color = palette.faceRim,
            radius = emblemRadius,
            center = Offset(cx, cy),
            style = Stroke(width = w * 0.024f),
        )

        if (unlocked) {
            drawArc(
                color = Color.White.copy(alpha = 0.22f),
                startAngle = specular,
                sweepAngle = 65f,
                useCenter = false,
                topLeft = Offset(cx - emblemRadius, cy - emblemRadius),
                size = Size(emblemRadius * 2f, emblemRadius * 2f),
                style = Stroke(width = w * 0.04f),
            )
        }

        if (tier == TrophyTier.MYTHIC && unlocked) {
            val sparkR = emblemRadius * 1.28f
            for (i in 0 until 6) {
                val angle = Math.toRadians((specular + i * 60f).toDouble())
                val sx = cx + (cos(angle) * sparkR).toFloat()
                val sy = cy + (sin(angle) * sparkR).toFloat()
                drawCircle(
                    color = palette.crown.copy(alpha = 0.55f),
                    radius = w * 0.014f,
                    center = Offset(sx, sy),
                )
            }
        }
    }
}

enum class TrophyTier { BRONZE, SILVER, GOLD, PLATINUM, OBSIDIAN, MYTHIC }

fun tierLevelToVisualTier(tierLevel: TrophyTierLevel): TrophyTier = when (tierLevel) {
    TrophyTierLevel.FOUNDATION -> TrophyTier.BRONZE
    TrophyTierLevel.MOMENTUM -> TrophyTier.SILVER
    TrophyTierLevel.MASTERY -> TrophyTier.GOLD
    TrophyTierLevel.LEGEND -> TrophyTier.OBSIDIAN
    TrophyTierLevel.MYTHIC -> TrophyTier.MYTHIC
}

fun visualTierForAchievement(achievementId: String, xpReward: Int): TrophyTier {
    val def = TrophyCatalog.find(achievementId)
    return if (def != null) tierLevelToVisualTier(def.tier) else trophyTierForReward(xpReward)
}

/** High-contrast icon ink per rank — never the same as the emblem fill. */
fun tierIconTint(tier: TrophyTier, unlocked: Boolean): Color {
    if (!unlocked) {
        return tierPalette(tier, unlocked = false).faceRim.copy(alpha = 0.62f)
    }
    return when (tier) {
        TrophyTier.BRONZE -> Color(0xFF3E2723)
        TrophyTier.SILVER -> Color(0xFF1A252F)
        TrophyTier.GOLD -> Color(0xFF1A237E)
        TrophyTier.PLATINUM -> Color(0xFF004D73)
        TrophyTier.OBSIDIAN -> Color(0xFFE8E0FF)
        TrophyTier.MYTHIC -> Color(0xFFFFF59D)
    }
}

fun trophyTierForReward(xpReward: Int): TrophyTier = when {
    xpReward >= 4000 -> TrophyTier.MYTHIC
    xpReward >= 1500 -> TrophyTier.OBSIDIAN
    xpReward >= 500 -> TrophyTier.PLATINUM
    xpReward >= 200 -> TrophyTier.GOLD
    xpReward >= 100 -> TrophyTier.SILVER
    else -> TrophyTier.BRONZE
}

@Composable
fun TrophyRankChip(
    tier: TrophyTier,
    unlocked: Boolean,
    modifier: Modifier = Modifier,
) {
    val label = when (tier) {
        TrophyTier.BRONZE -> "BRONZE"
        TrophyTier.SILVER -> "SILVER"
        TrophyTier.GOLD -> "GOLD"
        TrophyTier.PLATINUM -> "PLATINUM"
        TrophyTier.OBSIDIAN -> "OBSIDIAN"
        TrophyTier.MYTHIC -> "MYTHIC"
    }
    val palette = tierPalette(tier, unlocked)
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        palette.faceLo.copy(alpha = if (unlocked) 0.35f else 0.18f),
                        palette.faceHi.copy(alpha = if (unlocked) 0.22f else 0.10f),
                    ),
                ),
                shape = shape,
            )
            .border(
                width = 1.dp,
                color = palette.faceRim.copy(alpha = if (unlocked) 0.65f else 0.35f),
                shape = shape,
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.2.sp,
            color = if (unlocked) palette.crown else palette.faceRim.copy(alpha = 0.75f),
        )
    }
}

private data class TrophyPalette(
    val faceHi: Color,
    val faceLo: Color,
    val faceRim: Color,
    val ringHi: Color,
    val ringLo: Color,
    val plinthHi: Color,
    val plinthLo: Color,
    val plinthShadow: Color,
    val crown: Color,
    val glow: Color,
)

private fun tierPalette(tier: TrophyTier, unlocked: Boolean): TrophyPalette {
    val base = when (tier) {
        TrophyTier.BRONZE -> TrophyPalette(
            faceHi = Color(0xFFFFCC80),
            faceLo = Color(0xFFB87333),
            faceRim = Color(0xFF8D5524),
            ringHi = Color(0xFFCD7F32),
            ringLo = Color(0x66B87333),
            plinthHi = Color(0xFFA1622E),
            plinthLo = Color(0xFF6D421F),
            plinthShadow = Color(0x33000000),
            crown = Color(0xFFFFC78F),
            glow = Color(0xFFCD7F32),
        )
        TrophyTier.SILVER -> TrophyPalette(
            faceHi = Color(0xFFF5F7FA),
            faceLo = Color(0xFFB0BEC5),
            faceRim = Color(0xFF78909C),
            ringHi = Color(0xFFE8EAF0),
            ringLo = Color(0x6690A4AE),
            plinthHi = Color(0xFF90A4AE),
            plinthLo = Color(0xFF546E7A),
            plinthShadow = Color(0x33000000),
            crown = Color(0xFFECEFF1),
            glow = Color(0xFFB0BEC5),
        )
        TrophyTier.GOLD -> TrophyPalette(
            faceHi = Color(0xFFFFE57F),
            faceLo = Color(0xFFFFB300),
            faceRim = Color(0xFFB8860B),
            ringHi = Color(0xFFFFD54F),
            ringLo = Color(0x66FFB300),
            plinthHi = Color(0xFFFFA000),
            plinthLo = Color(0xFFE65100),
            plinthShadow = Color(0x33000000),
            crown = Color(0xFFFFF59D),
            glow = Color(0xFFFFB300),
        )
        TrophyTier.PLATINUM -> TrophyPalette(
            faceHi = Color(0xFFE8F4FF),
            faceLo = Color(0xFF90C8E8),
            faceRim = Color(0xFF4A90B8),
            ringHi = Color(0xFFB8E4FF),
            ringLo = Color(0x6688C8E8),
            plinthHi = Color(0xFF6BAED6),
            plinthLo = Color(0xFF3D7A9E),
            plinthShadow = Color(0x44000000),
            crown = Color(0xFFE0F7FF),
            glow = Color(0xFF64B5F6),
        )
        TrophyTier.OBSIDIAN -> TrophyPalette(
            faceHi = Color(0xFF3A3A52),
            faceLo = Color(0xFF12121C),
            faceRim = Color(0xFF9E8CFF),
            ringHi = Color(0xFF7B6CF6),
            ringLo = Color(0x447B6CF6),
            plinthHi = Color(0xFF252535),
            plinthLo = Color(0xFF0A0A12),
            plinthShadow = Color(0x66000000),
            crown = Color(0xFFB388FF),
            glow = Color(0xFF7B6CF6),
        )
        TrophyTier.MYTHIC -> TrophyPalette(
            faceHi = Color(0xFF2A1A3A),
            faceLo = Color(0xFF0A0610),
            faceRim = Color(0xFFFF4081),
            ringHi = Color(0xFFE040FB),
            ringLo = Color(0x44E040FB),
            plinthHi = Color(0xFF1A1028),
            plinthLo = Color(0xFF050308),
            plinthShadow = Color(0x88000000),
            crown = Color(0xFFFF80AB),
            glow = Color(0xFFE040FB),
        )
    }
    if (unlocked) return base
    return base.copy(
        faceHi = lerp(base.faceHi, Color(0xFF37474F), 0.45f),
        faceLo = lerp(base.faceLo, Color(0xFF263238), 0.45f),
        faceRim = base.faceRim.copy(alpha = 0.55f),
        ringHi = base.ringHi.copy(alpha = 0.35f),
        ringLo = base.ringLo.copy(alpha = 0.2f),
        crown = base.crown.copy(alpha = 0.5f),
        glow = base.glow.copy(alpha = 0.25f),
    )
}

private fun lerp(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)
