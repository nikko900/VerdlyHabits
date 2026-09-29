package com.saintnico.verdlyhabits.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.saintnico.verdlyhabits.engine.share.ShareCardSpec
import com.saintnico.verdlyhabits.engine.share.ShareCardType
import com.saintnico.verdlyhabits.engine.share.ShareMotifCategory
import com.saintnico.verdlyhabits.ui.components.TrophyTier
import com.saintnico.verdlyhabits.ui.components.tierPalette
import kotlin.math.min
import kotlin.random.Random

object ShareCardRenderer {

    private const val MARGIN = 72f
    private const val FOOTER_Y_OFFSET = 120f

    fun render(context: Context, spec: ShareCardSpec): Bitmap {
        val w = spec.width
        val h = spec.height
        val imageBitmap = ImageBitmap(w, h)
        val canvas = Canvas(imageBitmap)
        val drawScope = CanvasDrawScope()
        val density = Density(context)

        drawScope.draw(density, LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat())) {
            val colors = spec.theme.gradient.map { Color(it) }
            drawRect(
                brush = Brush.linearGradient(colors, start = Offset.Zero, end = Offset(w.toFloat(), h.toFloat())),
                size = Size(w.toFloat(), h.toFloat()),
            )

            if (spec.theme.particleTreatment && spec.cardType != ShareCardType.PROFILE_CARD) {
                drawMythicParticles(w.toFloat(), h.toFloat())
            }

            // Premium base treatment on every card: soft light behind the hero area,
            // darkened edges, and a hairline accent frame — same finish, every surface.
            drawHeroBloom(
                w.toFloat(),
                h.toFloat(),
                Color(spec.trophyAccent ?: spec.theme.accent),
                cx = if (spec.cardType.name.startsWith("TROPHY")) w * 0.30f else w * 0.5f,
                cy = if (spec.cardType.name.startsWith("TROPHY")) h * 0.50f else h * 0.30f,
            )
            drawVignette(w.toFloat(), h.toFloat())
            drawHairlineFrame(w.toFloat(), h.toFloat(), Color(spec.theme.accent))

            when (spec.cardType) {
                ShareCardType.TROPHY_TIER_BRONZE,
                ShareCardType.TROPHY_TIER_SILVER,
                ShareCardType.TROPHY_TIER_GOLD,
                ShareCardType.TROPHY_TIER_OBSIDIAN,
                ShareCardType.TROPHY_TIER_MYTHIC -> {
                    drawTrophyEmblem(
                        tier = trophyTierForCard(spec.cardType),
                        cx = w * 0.30f,
                        cy = h * 0.50f,
                        trophySize = w * 0.54f,
                    )
                }
                ShareCardType.GOAL_MILESTONE_RITUAL -> {
                    spec.motif?.let {
                        drawProgressRing(
                            centerX = w * 0.5f,
                            centerY = h * 0.36f,
                            radius = 180f,
                            progress = it.progressPercent,
                            accent = spec.theme.accent,
                            growthMark = it.growthMarkEnabled,
                        )
                    }
                }
                ShareCardType.GOAL_DAILY_RECAP -> {
                    spec.motif?.let {
                        drawProgressRing(
                            centerX = w * 0.5f,
                            centerY = h * 0.36f,
                            radius = 180f,
                            progress = it.progressPercent,
                            accent = spec.theme.accent,
                            growthMark = it.growthMarkEnabled,
                        )
                    }
                }
                ShareCardType.GOAL_COMPLETION -> {
                    drawClosedRing(w * 0.5f, h * 0.28f, 160f, spec.theme.accent)
                }
                ShareCardType.DUO_STREAK_MILESTONE -> {
                    spec.motif?.let {
                        drawDuoRings(w * 0.5f, h * 0.38f, 200f, it.streakDays, spec.theme.accent)
                    }
                }
                ShareCardType.PROFILE_CARD -> {
                    spec.motif?.let { motif ->
                        val cx = w * 0.5f
                        val cy = h * 0.62f
                        val accent = spec.trophyAccent ?: spec.theme.accent
                        if (spec.theme.particleTreatment) {
                            drawCircle(Color(0xFF050506).copy(0.88f), 210f, Offset(cx, cy))
                            drawCircle(Color(0xFF1A0F24).copy(0.55f), 228f, Offset(cx, cy), style = Stroke(width = 3f))
                            drawLocalizedParticles(cx, cy, 220f)
                        }
                        drawMotif(motif, accent, cx, cy, 170f)
                    }
                }
                else -> Unit
            }
        }

        val native = imageBitmap.asAndroidBitmap()
        val nc = android.graphics.Canvas(native)
        drawTextLayout(nc, spec, w, h)
        drawQrIfPresent(nc, spec, w, h)
        return native
    }

    private fun drawTextLayout(nc: android.graphics.Canvas, spec: ShareCardSpec, w: Int, h: Int) {
        if (spec.cardType == ShareCardType.PROFILE_CARD) {
            drawProfileText(nc, spec, w, h)
            return
        }
        if (spec.cardType.name.startsWith("TROPHY")) {
            drawTrophyCopy(nc, spec, w, h)
            return
        }
        val cream = Color(spec.theme.textColor).toArgb()
        val accent = Color(spec.theme.accent).toArgb()
        val muted = android.graphics.Color.argb(153, 246, 243, 233)

        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }
        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = when (spec.cardType) {
                ShareCardType.GOAL_MILESTONE_STANDARD,
                ShareCardType.GOAL_MILESTONE_RITUAL,
                ShareCardType.GOAL_DAILY_RECAP,
                ShareCardType.PROFILE_CARD -> 144f
                ShareCardType.DUO_STREAK_MILESTONE -> 144f
                else -> 84f
            }
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val subheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = 30f
        }
        val xpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val quotePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 30f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }

        var y = MARGIN + 48f

        spec.chipLabel?.let { chip ->
            nc.drawText(chip.uppercase(), MARGIN, y, chipPaint)
            y += 56f
        }

        y = when (spec.cardType) {
            ShareCardType.GOAL_MILESTONE_STANDARD,
            ShareCardType.GOAL_MILESTONE_RITUAL,
            ShareCardType.GOAL_DAILY_RECAP -> h * 0.52f
            ShareCardType.GOAL_COMPLETION -> h * 0.42f
            ShareCardType.DUO_STREAK_MILESTONE -> h * 0.58f
            ShareCardType.PROFILE_CARD -> h * 0.36f
            else -> h * 0.5f
        }

        nc.drawText(truncate(spec.headline, headlinePaint, w - MARGIN * 2), MARGIN, y, headlinePaint)
        y += headlinePaint.textSize + 20f

        spec.subhead?.let { sub ->
            val lines = wrapText(truncate(sub, subheadPaint, w - MARGIN * 2), subheadPaint, w - MARGIN * 2)
            lines.take(2).forEach { line ->
                nc.drawText(line, MARGIN, y, subheadPaint)
                y += subheadPaint.textSize + 12f
            }
        }

        spec.xpChip?.let { xp ->
            if (spec.cardType == ShareCardType.GOAL_MILESTONE_STANDARD ||
                spec.cardType == ShareCardType.GOAL_MILESTONE_RITUAL ||
                spec.cardType == ShareCardType.GOAL_DAILY_RECAP
            ) {
                y += 8f
                wrapText(xp, bodyPaint, w - MARGIN * 2).take(2).forEach { line ->
                    nc.drawText(line, MARGIN, y, bodyPaint)
                    y += bodyPaint.textSize + 10f
                }
            }
        }

        spec.whyQuote?.let { quote ->
            y += 8f
            nc.drawText("\"${truncate(quote, quotePaint, w - MARGIN * 2)}\"", MARGIN, y, quotePaint)
            y += quotePaint.textSize + 16f
        }

        spec.statLine?.let { stat ->
            y += 8f
            nc.drawText(stat, MARGIN, y, bodyPaint)
        }

        spec.reclaimedChip?.let { chip ->
            y += bodyPaint.textSize + 16f
            val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                textSize = 28f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rect = RectF(MARGIN, y - 28f, MARGIN + chipPaint.measureText(chip) + 48f, y + 20f)
            val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(40, android.graphics.Color.red(accent), android.graphics.Color.green(accent), android.graphics.Color.blue(accent))
            }
            nc.drawRoundRect(rect, 24f, 24f, bg)
            nc.drawText(chip, MARGIN + 24f, y, chipPaint)
        }

        if (spec.cardType == ShareCardType.DUO_STREAK_MILESTONE) {
            spec.statLine?.let { stat ->
                nc.drawText(stat, MARGIN, h * 0.72f, bodyPaint)
            }
        }

        nc.drawText("Built with Verdly · ${spec.footerLink}", MARGIN, h - FOOTER_Y_OFFSET + 40f, footerPaint)
    }

    private fun drawTrophyCopy(nc: android.graphics.Canvas, spec: ShareCardSpec, w: Int, h: Int) {
        val cream = Color(spec.theme.textColor).toArgb()
        val accent = Color(spec.theme.accent).toArgb()
        val muted = android.graphics.Color.argb(168, 246, 243, 233)
        val colX = w * 0.56f
        val colMax = w - MARGIN - colX

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.18f
        }
        val chipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.12f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = 64f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val xpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 24f
        }

        nc.drawText("VERDLY", MARGIN, MARGIN + 28f, brandPaint)

        spec.chipLabel?.let { rank ->
            val padX = 22f
            val padY = 14f
            val chipW = chipPaint.measureText(rank) + padX * 2
            val chipH = chipPaint.textSize + padY * 2
            val left = w - MARGIN - chipW
            val top = MARGIN - 8f
            val well = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(48, android.graphics.Color.red(accent), android.graphics.Color.green(accent), android.graphics.Color.blue(accent))
            }
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
            val rect = RectF(left, top, left + chipW, top + chipH)
            nc.drawRoundRect(rect, 18f, 18f, well)
            nc.drawRoundRect(rect, 18f, 18f, stroke)
            nc.drawText(rank, left + padX, top + padY + chipPaint.textSize * 0.82f, chipPaint)
        }

        var y = h * 0.34f
        wrapText(spec.headline, titlePaint, colMax).take(3).forEach { line ->
            nc.drawText(line, colX, y, titlePaint)
            y += titlePaint.textSize + 8f
        }

        spec.subhead?.let { how ->
            y += 18f
            wrapText(how, bodyPaint, colMax).take(4).forEach { line ->
                nc.drawText(line, colX, y, bodyPaint)
                y += bodyPaint.textSize + 8f
            }
        }

        spec.xpChip?.let { xp ->
            y += 28f
            val padX = 20f
            val padY = 12f
            val xpW = xpPaint.measureText(xp) + padX * 2
            val xpH = xpPaint.textSize + padY * 2
            val well = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.argb(40, android.graphics.Color.red(accent), android.graphics.Color.green(accent), android.graphics.Color.blue(accent))
            }
            nc.drawRoundRect(RectF(colX, y, colX + xpW, y + xpH), 16f, 16f, well)
            nc.drawText(xp, colX + padX, y + padY + xpPaint.textSize * 0.82f, xpPaint)
            y += xpH + 20f
        }

        spec.statLine?.let { date ->
            nc.drawText(date, colX, y + datePaint.textSize, datePaint)
        }

        nc.drawText(
            truncate("Built with Verdly  ${spec.footerLink}", footerPaint, w - MARGIN * 2 - 200f),
            MARGIN,
            h - FOOTER_Y_OFFSET + 40f,
            footerPaint,
        )
    }

    private fun drawProfileText(nc: android.graphics.Canvas, spec: ShareCardSpec, w: Int, h: Int) {
        val cream = Color(spec.theme.textColor).toArgb()
        val accent = Color(spec.trophyAccent ?: spec.theme.accent).toArgb()
        val muted = android.graphics.Color.argb(153, 246, 243, 233)
        val avatarCx = MARGIN + 52f
        val avatarCy = MARGIN + 64f
        val avatarR = 52f

        val avatarFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb(48, 232, 196, 104)
        }
        val avatarStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        nc.drawCircle(avatarCx, avatarCy, avatarR, avatarFill)
        nc.drawCircle(avatarCx, avatarCy, avatarR, avatarStroke)

        spec.monogram?.let { letter ->
            val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = cream
                textSize = 44f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val fm = letterPaint.fontMetrics
            val textY = avatarCy - (fm.ascent + fm.descent) / 2f
            nc.drawText(letter, avatarCx, textY, letterPaint)
        }

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val memberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 28f
        }
        val nameX = avatarCx + avatarR + 28f
        spec.identityLabel?.let {
            nc.drawText(truncate(it, namePaint, w - nameX - MARGIN), nameX, avatarCy - 6f, namePaint)
        }
        spec.memberSinceLabel?.let {
            nc.drawText(it, nameX, avatarCy + 36f, memberPaint)
        }

        val headlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = 144f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val subheadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            letterSpacing = 0.04f
        }
        var y = h * 0.36f
        nc.drawText(spec.headline, MARGIN, y, headlinePaint)
        y += 52f
        spec.subhead?.let { nc.drawText(it, MARGIN, y, subheadPaint) }

        val trophyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = 36f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        spec.xpChip?.let { title ->
            nc.drawText(
                "\"${truncate(title, trophyPaint, w - MARGIN * 2)}\"",
                w / 2f,
                h * 0.76f,
                trophyPaint,
            )
        }

        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cream
            textSize = 30f
        }
        spec.statLine?.let { stat ->
            nc.drawText(stat, MARGIN, h * 0.84f, bodyPaint)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 24f
        }
        val footerMax = w - MARGIN * 2 - 200f
        nc.drawText(
            truncate("Built with Verdly · ${spec.footerLink}", footerPaint, footerMax),
            MARGIN,
            h - FOOTER_Y_OFFSET + 40f,
            footerPaint,
        )
    }

    private fun drawQrIfPresent(nc: android.graphics.Canvas, spec: ShareCardSpec, w: Int, h: Int) {
        val payload = spec.qrPayload ?: return
        val size = 144
        val qr = runCatching {
            com.saintnico.verdlyhabits.referral.ReferralQrEncoder.encodeBitmap(payload, size)
        }.getOrNull() ?: return
        val left = w - MARGIN - size
        val top = h - FOOTER_Y_OFFSET - size + 8f
        val pad = 10f
        val well = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE }
        nc.drawRoundRect(
            RectF(left - pad, top - pad, left + size + pad, top + size + pad),
            16f,
            16f,
            well,
        )
        nc.drawBitmap(qr, left, top, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        if (!qr.isRecycled) qr.recycle()
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLocalizedParticles(
        cx: Float,
        cy: Float,
        radius: Float,
    ) {
        val rng = Random(7)
        repeat(28) {
            val angle = rng.nextFloat() * (Math.PI * 2).toFloat()
            val dist = rng.nextFloat() * radius
            val x = cx + kotlin.math.cos(angle) * dist
            val y = cy + kotlin.math.sin(angle) * dist
            val c = if (it % 2 == 0) Color(0xFF7FFFD4) else Color(0xFFB388FF)
            drawCircle(c.copy(rng.nextFloat() * 0.45f + 0.18f), rng.nextFloat() * 4f + 2f, Offset(x, y))
        }
    }

    private fun trophyTierForCard(cardType: ShareCardType): TrophyTier = when (cardType) {
        ShareCardType.TROPHY_TIER_BRONZE -> TrophyTier.BRONZE
        ShareCardType.TROPHY_TIER_SILVER -> TrophyTier.SILVER
        ShareCardType.TROPHY_TIER_GOLD -> TrophyTier.GOLD
        ShareCardType.TROPHY_TIER_OBSIDIAN -> TrophyTier.OBSIDIAN
        else -> TrophyTier.MYTHIC
    }

    /**
     * Static port of the in-app trophy (AchievementTrophyCanvasFallback) — same palette,
     * same geometry: bloom, back ring, tilted facet for high tiers, plinth, crown, face,
     * rim, and a frozen specular sweep. [trophySize] is the emblem's bounding box side.
     */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrophyEmblem(
        tier: TrophyTier,
        cx: Float,
        cy: Float,
        trophySize: Float,
    ) {
        val palette = tierPalette(tier, unlocked = true)
        val w = trophySize
        val h = trophySize
        val left = cx - w / 2f
        val top = cy - h / 2f
        val ecx = left + w / 2f
        val ecy = top + h * 0.45f
        val emblemRadius = w * 0.34f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.glow.copy(alpha = 0.34f), Color.Transparent),
                center = Offset(ecx, ecy),
                radius = emblemRadius * 1.55f,
            ),
            radius = emblemRadius * 1.42f,
            center = Offset(ecx, ecy),
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.ringHi, palette.ringLo),
                center = Offset(ecx, ecy - emblemRadius * 0.4f),
                radius = emblemRadius * 1.6f,
            ),
            radius = emblemRadius * 1.18f,
            center = Offset(ecx, ecy),
        )

        if (tier == TrophyTier.PLATINUM || tier == TrophyTier.OBSIDIAN || tier == TrophyTier.MYTHIC) {
            rotate(30f, Offset(ecx, ecy)) {
                drawRoundRect(
                    color = palette.faceRim.copy(alpha = 0.55f),
                    topLeft = Offset(ecx - emblemRadius * 1.08f, ecy - emblemRadius * 1.08f),
                    size = Size(emblemRadius * 2.16f, emblemRadius * 2.16f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(emblemRadius * 0.22f),
                    style = Stroke(width = w * 0.018f),
                )
            }
        }

        drawRoundRect(
            color = palette.plinthShadow,
            topLeft = Offset(ecx - w * 0.30f, top + h * 0.70f),
            size = Size(w * 0.60f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.06f, w * 0.06f),
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(palette.plinthHi, palette.plinthLo)),
            topLeft = Offset(ecx - w * 0.28f, top + h * 0.66f),
            size = Size(w * 0.56f, h * 0.18f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.05f, w * 0.05f),
        )

        val crownY = ecy - emblemRadius * 1.05f
        drawCircle(palette.crown, radius = emblemRadius * 0.18f, center = Offset(ecx, crownY))
        drawCircle(
            palette.crown,
            radius = emblemRadius * 0.14f,
            center = Offset(ecx - emblemRadius * 0.45f, crownY + emblemRadius * 0.08f),
        )
        drawCircle(
            palette.crown,
            radius = emblemRadius * 0.14f,
            center = Offset(ecx + emblemRadius * 0.45f, crownY + emblemRadius * 0.08f),
        )

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(palette.faceHi, palette.faceLo),
                start = Offset(ecx - emblemRadius, ecy - emblemRadius),
                end = Offset(ecx + emblemRadius, ecy + emblemRadius),
            ),
            radius = emblemRadius,
            center = Offset(ecx, ecy),
        )
        drawCircle(
            color = palette.faceRim,
            radius = emblemRadius,
            center = Offset(ecx, ecy),
            style = Stroke(width = w * 0.024f),
        )

        drawArc(
            color = Color.White.copy(alpha = 0.22f),
            startAngle = 318f,
            sweepAngle = 65f,
            useCenter = false,
            topLeft = Offset(ecx - emblemRadius, ecy - emblemRadius),
            size = Size(emblemRadius * 2f, emblemRadius * 2f),
            style = Stroke(width = w * 0.04f),
        )

        if (tier == TrophyTier.MYTHIC) {
            val sparkR = emblemRadius * 1.28f
            for (i in 0 until 6) {
                val angle = Math.toRadians((318f + i * 60f).toDouble())
                val sx = ecx + (kotlin.math.cos(angle) * sparkR).toFloat()
                val sy = ecy + (kotlin.math.sin(angle) * sparkR).toFloat()
                drawCircle(
                    color = palette.crown.copy(alpha = 0.55f),
                    radius = w * 0.014f,
                    center = Offset(sx, sy),
                )
            }
        }
    }

    /** Soft light pooled behind the hero. */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeroBloom(
        w: Float,
        h: Float,
        accent: Color,
        cx: Float = w * 0.5f,
        cy: Float = h * 0.30f,
    ) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.62f,
            ),
            radius = w * 0.62f,
            center = Offset(cx, cy),
        )
    }

    /** Darkened corners so the center reads lit — the finish flat gradients lack. */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawVignette(w: Float, h: Float) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.30f)),
                center = Offset(w * 0.5f, h * 0.42f),
                radius = maxOf(w, h) * 0.78f,
            ),
            size = Size(w, h),
        )
    }

    /** Thin double keyline just inside the edge — the trading-card frame. */
    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHairlineFrame(w: Float, h: Float, accent: Color) {
        val inset = 30f
        drawRoundRect(
            color = accent.copy(alpha = 0.55f),
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2, h - inset * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f),
            style = Stroke(width = 3f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = Offset(inset + 10f, inset + 10f),
            size = Size(w - (inset + 10f) * 2, h - (inset + 10f) * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(32f),
            style = Stroke(width = 1.5f),
        )
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMotif(
        motif: com.saintnico.verdlyhabits.engine.share.ShareMotif,
        accent: Long,
        cx: Float,
        cy: Float,
        size: Float,
    ) {
        val color = Color(accent)
        when (motif.category) {
            ShareMotifCategory.STREAK -> {
                drawLine(
                    color = color.copy(0.85f),
                    start = Offset(cx - size * 0.4f, cy + size * 0.35f),
                    end = Offset(cx + size * 0.45f, cy - size * 0.45f),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round,
                )
                drawCircle(color.copy(0.6f), 12f, Offset(cx + size * 0.45f, cy - size * 0.45f))
            }
            ShareMotifCategory.TIME_OF_DAY -> {
                drawArc(
                    color = color.copy(0.7f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - size * 0.5f, cy - size * 0.25f),
                    size = Size(size, size * 0.5f),
                    style = Stroke(width = 6f),
                )
            }
            ShareMotifCategory.CONSISTENCY -> {
                for (i in 1..4) {
                    val r = size * 0.15f * i
                    drawCircle(color.copy(0.35f + i * 0.1f), r, Offset(cx, cy), style = Stroke(width = 4f))
                }
            }
            ShareMotifCategory.SOCIAL -> {
                drawCircle(color.copy(0.5f), size * 0.28f, Offset(cx - size * 0.12f, cy), style = Stroke(width = 5f))
                drawCircle(color.copy(0.5f), size * 0.28f, Offset(cx + size * 0.12f, cy), style = Stroke(width = 5f))
            }
            ShareMotifCategory.LEVEL -> {
                for (i in 0 until 4) {
                    val barW = size * 0.18f
                    val barH = size * (0.15f + i * 0.12f)
                    drawRect(
                        color = color.copy(0.45f + i * 0.12f),
                        topLeft = Offset(cx - size * 0.45f + i * (barW + 16f), cy + size * 0.3f - barH),
                        size = Size(barW, barH),
                    )
                }
            }
            ShareMotifCategory.SECRET -> {
                drawCircle(Color(0xFF1A0F24), size * 0.35f, Offset(cx, cy))
                drawCircle(color.copy(0.55f), size * 0.35f, Offset(cx + size * 0.08f, cy - size * 0.06f), style = Stroke(width = 5f))
                drawCircle(Color(0xFFFFD700).copy(0.35f), size * 0.38f, Offset(cx, cy), style = Stroke(width = 2f))
            }
            else -> Unit
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawProgressRing(
        centerX: Float,
        centerY: Float,
        radius: Float,
        progress: Int,
        accent: Long,
        growthMark: Boolean,
    ) {
        val color = Color(accent)
        drawCircle(color.copy(0.2f), radius, Offset(centerX, centerY), style = Stroke(width = 14f))
        drawArc(
            color = color.copy(0.9f),
            startAngle = -90f,
            sweepAngle = 360f * (progress / 100f),
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 14f, cap = StrokeCap.Round),
        )
        if (growthMark) {
            drawLine(color, Offset(centerX, centerY + 20f), Offset(centerX, centerY - 30f), strokeWidth = 4f)
            drawLine(color, Offset(centerX - 16f, centerY - 10f), Offset(centerX, centerY - 30f), strokeWidth = 3f)
            drawLine(color, Offset(centerX + 16f, centerY - 10f), Offset(centerX, centerY - 30f), strokeWidth = 3f)
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawClosedRing(cx: Float, cy: Float, radius: Float, accent: Long) {
        val color = Color(accent)
        drawCircle(color.copy(0.85f), radius, Offset(cx, cy), style = Stroke(width = 16f))
        drawCircle(color.copy(0.25f), radius + 24f, Offset(cx, cy), style = Stroke(width = 4f))
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDuoRings(
        cx: Float,
        cy: Float,
        baseRadius: Float,
        streakDays: Int,
        accent: Long,
    ) {
        val overlap = min(0.75f, streakDays / 365f)
        val offset = baseRadius * (0.55f - overlap * 0.35f)
        val color = Color(accent)
        val gold = Color(0xFFE8C468)
        drawCircle(color.copy(0.45f), baseRadius, Offset(cx - offset, cy), style = Stroke(width = 8f))
        drawCircle(color.copy(0.45f), baseRadius, Offset(cx + offset, cy), style = Stroke(width = 8f))
        if (overlap > 0.2f) {
            drawArc(
                color = gold.copy(0.75f),
                startAngle = 250f,
                sweepAngle = 40f,
                useCenter = false,
                topLeft = Offset(cx - baseRadius, cy - baseRadius),
                size = Size(baseRadius * 2, baseRadius * 2),
                style = Stroke(width = 10f, cap = StrokeCap.Round),
            )
        }
    }

    private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMythicParticles(w: Float, h: Float) {
        val rng = Random(42)
        repeat(48) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h * 0.55f
            val c = if (it % 2 == 0) Color(0xFF7FFFD4) else Color(0xFFB388FF)
            drawCircle(c.copy(rng.nextFloat() * 0.4f + 0.15f), rng.nextFloat() * 4f + 2f, Offset(x, y))
        }
    }

    private fun truncate(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.length > 1 && paint.measureText("$t…") > maxWidth) {
            t = t.dropLast(1)
        }
        return "$t…"
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "${current} $word"
            if (paint.measureText(candidate) <= maxWidth) {
                if (current.isNotEmpty()) current.append(' ')
                current.append(word)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
