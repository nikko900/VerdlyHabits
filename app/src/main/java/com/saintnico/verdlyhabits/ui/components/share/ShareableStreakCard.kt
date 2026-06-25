package com.saintnico.verdlyhabits.ui.components.share

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.screens.focus.drawGrowingPlant
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import java.io.File
import java.io.FileOutputStream

/**
 * In-app share card (400×220 dp). Image export uses [shareStreakInvite] (text + link) for maximum reliability.
 */
@Composable
fun ShareableStreakCard(
    headline: String,
    habitName: String,
    referralCode: String,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF0A1610), Color(0xFF1B4332), Color(0xFF0D1F14)),
    )
    Box(
        modifier
            .width(400.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                ComposeCanvas(Modifier.fillMaxSize()) {
                    drawGrowingPlant(1f, isWilting = false, wiltAmount = 0f)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            ) {
                Text(
                    headline,
                    fontFamily = frauncesFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = Color(0xFFD8F3DC),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    habitName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(0.85f),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Built with Verdly",
                    fontSize = 11.sp,
                    color = Color.White.copy(0.45f),
                )
                Text(
                    "Join free: verdly.app/r/$referralCode",
                    fontSize = 11.sp,
                    color = Color(0xFF95D5B2),
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun referralPathCode(fullCode: String): String =
    fullCode.removePrefix("VERDLY-").uppercase()

private fun drawWrappedText(
    canvas: Canvas,
    text: String,
    paint: Paint,
    x: Float,
    y: Float,
    maxWidth: Float,
    lineHeight: Float,
    maxLines: Int = 3,
): Float {
    val words = text.split(" ")
    var line = ""
    var cursorY = y
    var lines = 0
    words.forEachIndexed { index, word ->
        val candidate = if (line.isBlank()) word else "$line $word"
        val isLastWord = index == words.lastIndex
        if (paint.measureText(candidate) <= maxWidth || line.isBlank()) {
            line = candidate
        } else {
            canvas.drawText(line, x, cursorY, paint)
            cursorY += lineHeight
            lines += 1
            line = word
        }
        if (isLastWord && line.isNotBlank() && lines < maxLines) {
            canvas.drawText(line, x, cursorY, paint)
            cursorY += lineHeight
            lines += 1
        }
    }
    return cursorY
}

private fun shareCardVariantFromSeed(seed: String): Int {
    var h = 5381
    for (c in seed) {
        h = (h * 33) xor c.code
    }
    return (h and 0x7FFF_FFFF) % 4
}

private data class ShareCardPalette(
    val grad: IntArray,
    val glow1: Int,
    val glow2: Int,
    val glowAlpha: Int,
    val cardFill: Int,
    val cardBorder: Int,
    val title: Int,
    val subtitle: Int,
    val accent: Int,
    val metricGlass: Int,
    val link: Int,
    val brand: Int,
)

private fun paletteForVariant(v: Int): ShareCardPalette = when (v % 4) {
    0 -> ShareCardPalette(
        intArrayOf(
            AndroidColor.rgb(5, 18, 12),
            AndroidColor.rgb(18, 72, 52),
            AndroidColor.rgb(4, 12, 9),
        ),
        AndroidColor.rgb(64, 145, 108),
        AndroidColor.rgb(149, 213, 178),
        52,
        AndroidColor.argb(200, 10, 28, 20),
        AndroidColor.argb(110, 190, 240, 210),
        AndroidColor.rgb(241, 255, 246),
        AndroidColor.argb(228, 255, 255, 255),
        AndroidColor.rgb(149, 213, 178),
        AndroidColor.argb(40, 255, 255, 255),
        AndroidColor.rgb(168, 230, 207),
        AndroidColor.argb(220, 216, 243, 220),
    )
    1 -> ShareCardPalette(
        intArrayOf(
            AndroidColor.rgb(18, 8, 28),
            AndroidColor.rgb(55, 20, 12),
            AndroidColor.rgb(8, 4, 14),
        ),
        AndroidColor.rgb(255, 140, 66),
        AndroidColor.rgb(255, 214, 120),
        58,
        AndroidColor.argb(205, 22, 14, 18),
        AndroidColor.argb(120, 255, 200, 120),
        AndroidColor.rgb(255, 248, 240),
        AndroidColor.argb(220, 255, 235, 220),
        AndroidColor.rgb(255, 193, 79),
        AndroidColor.argb(42, 255, 255, 255),
        AndroidColor.rgb(255, 214, 153),
        AndroidColor.argb(230, 255, 220, 200),
    )
    2 -> ShareCardPalette(
        intArrayOf(
            AndroidColor.rgb(6, 14, 32),
            AndroidColor.rgb(16, 42, 72),
            AndroidColor.rgb(4, 8, 22),
        ),
        AndroidColor.rgb(90, 160, 255),
        AndroidColor.rgb(120, 240, 220),
        48,
        AndroidColor.argb(198, 10, 22, 38),
        AndroidColor.argb(100, 140, 200, 255),
        AndroidColor.rgb(236, 246, 255),
        AndroidColor.argb(215, 220, 235, 255),
        AndroidColor.rgb(124, 196, 255),
        AndroidColor.argb(38, 255, 255, 255),
        AndroidColor.rgb(186, 230, 255),
        AndroidColor.argb(225, 210, 235, 255),
    )
    else -> ShareCardPalette(
        intArrayOf(
            AndroidColor.rgb(8, 8, 10),
            AndroidColor.rgb(28, 26, 20),
            AndroidColor.rgb(4, 4, 6),
        ),
        AndroidColor.rgb(232, 196, 90),
        AndroidColor.rgb(180, 140, 60),
        44,
        AndroidColor.argb(215, 16, 16, 18),
        AndroidColor.argb(100, 232, 200, 110),
        AndroidColor.rgb(250, 246, 235),
        AndroidColor.argb(210, 230, 225, 210),
        AndroidColor.rgb(240, 204, 96),
        AndroidColor.argb(35, 255, 255, 255),
        AndroidColor.rgb(220, 200, 150),
        AndroidColor.argb(230, 232, 220, 200),
    )
}

private fun drawInviteCodeStrip(
    canvas: Canvas,
    palette: ShareCardPalette,
    panel: RectF,
    rawCode: String,
    link: String,
) {
    val outerGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = AndroidColor.argb(55, AndroidColor.red(palette.accent), AndroidColor.green(palette.accent), AndroidColor.blue(palette.accent))
    }
    canvas.drawRoundRect(panel, 40f, 40f, outerGlow)

    val panelFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            panel.left,
            panel.top,
            panel.right,
            panel.bottom,
            intArrayOf(
                AndroidColor.argb(210, 12, 18, 16),
                AndroidColor.argb(175, 18, 26, 22),
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRoundRect(panel, 36f, 36f, panelFill)

    val innerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = AndroidColor.argb(100, 255, 255, 255)
    }
    canvas.drawRoundRect(panel, 36f, 36f, innerStroke)

    val joinLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(200, 255, 255, 255)
        textSize = 26f
        letterSpacing = 0.18f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText("JOIN FREE · INVITE CODE", panel.left + 36f, panel.top + 52f, joinLabel)

    val clean = rawCode.uppercase().filter { it.isLetterOrDigit() }.take(10)
    val contentLeft = panel.left + 32f
    val contentRight = panel.right - 32f
    val contentW = contentRight - contentLeft
    val cellTop = panel.top + 78f
    val cellH = 78f
    if (clean.isNotEmpty()) {
        val gap = 10f
        val cellW = (contentW - gap * (clean.length - 1)) / clean.length
        val cellFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(55, 255, 255, 255)
        }
        val cellStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
            color = AndroidColor.argb(
                140,
                AndroidColor.red(palette.accent),
                AndroidColor.green(palette.accent),
                AndroidColor.blue(palette.accent),
            )
        }
        val charPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(255, 255, 255)
            textSize = 46f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val fm = charPaint.fontMetrics
        for (i in clean.indices) {
            val x0 = contentLeft + i * (cellW + gap)
            val cell = RectF(x0, cellTop, x0 + cellW, cellTop + cellH)
            canvas.drawRoundRect(cell, 16f, 16f, cellFill)
            canvas.drawRoundRect(cell, 16f, 16f, cellStroke)
            val cx = cell.centerX()
            val baseline = cell.centerY() - (fm.ascent + fm.descent) / 2f - fm.descent
            canvas.drawText(clean[i].toString(), cx, baseline, charPaint)
        }
    }

    val linkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.link
        textSize = 26f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    val linkY = panel.bottom - 44f
    val linkText = if (link.length > 52) link.take(49) + "…" else link
    canvas.drawText(linkText, panel.left + 36f, linkY, linkPaint)

    val spark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent; alpha = 200 }
    canvas.drawCircle(panel.right - 48f, panel.top + 42f, 5f, spark)
    canvas.drawCircle(panel.right - 68f, panel.top + 58f, 3.5f, spark)
    canvas.drawCircle(panel.right - 38f, panel.top + 62f, 3f, spark)
}

private fun roundedCardBitmap(
    eyebrow: String,
    title: String,
    subtitle: String,
    metric: String,
    metricLabel: String,
    accentOverride: Int?,
    rawInviteCode: String,
    link: String,
    variantSeed: String,
): Bitmap {
    val variant = shareCardVariantFromSeed(variantSeed)
    val palette = paletteForVariant(variant)
    val accent = accentOverride ?: palette.accent

    val width = 1080
    val height = 1350
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            palette.grad,
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

    val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.glow1
        alpha = palette.glowAlpha
    }
    canvas.drawCircle(880f, 190f, 320f, glow)
    glow.color = palette.glow2
    canvas.drawCircle(90f, 1100f, 270f, glow)

    val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.cardFill }
    canvas.drawRoundRect(RectF(64f, 64f, 1016f, 1286f), 56f, 56f, card)
    val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = palette.cardBorder
    }
    canvas.drawRoundRect(RectF(64f, 64f, 1016f, 1286f), 56f, 56f, border)

    val eyebrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        textSize = 34f
        letterSpacing = 0.14f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText(eyebrow.uppercase(), 112f, 156f, eyebrowPaint)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.title
        textSize = 82f
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
    }
    val nextY = drawWrappedText(canvas, title, titlePaint, 112f, 280f, 820f, 92f, 3)

    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.subtitle
        textSize = 38f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    drawWrappedText(canvas, subtitle, subtitlePaint, 112f, nextY + 28f, 760f, 48f, 2)

    val metricBox = RectF(112f, 690f, 482f, 910f)
    val metricBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.metricGlass }
    canvas.drawRoundRect(metricBox, 36f, 36f, metricBg)
    val metricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        textSize = 82f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText(metric, 150f, 790f, metricPaint)
    val metricLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(185, 255, 255, 255)
        textSize = 30f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    canvas.drawText(metricLabel.uppercase(), 152f, 850f, metricLabelPaint)

    val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = when (variant % 4) {
            2 -> 14f
            3 -> 18f
            else -> 16f
        }
        color = AndroidColor.argb(
            175,
            AndroidColor.red(accent),
            AndroidColor.green(accent),
            AndroidColor.blue(accent),
        )
    }
    when (variant % 4) {
        3 -> {
            canvas.drawRoundRect(RectF(640f, 650f, 880f, 890f), 48f, 48f, markPaint)
            val ok = Paint(markPaint).apply { strokeWidth = 20f }
            canvas.drawLine(700f, 770f, 742f, 816f, ok)
            canvas.drawLine(742f, 816f, 818f, 704f, ok)
        }
        else -> {
            canvas.drawCircle(760f, 770f, 120f, markPaint)
            canvas.drawLine(700f, 770f, 742f, 816f, markPaint)
            canvas.drawLine(742f, 816f, 838f, 704f, markPaint)
        }
    }

    val invitePanel = RectF(112f, 972f, 968f, 1228f)
    drawInviteCodeStrip(canvas, palette, invitePanel, rawInviteCode, link)

    val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = palette.brand
        textSize = 34f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("Verdly", 940f, 1250f, brandPaint)
    return bitmap
}

private fun shareBitmapCard(context: Context, bitmap: Bitmap, shareText: String, chooserTitle: String): Boolean {
    val activity = context.findActivity() ?: return false
    val cachePath = File(context.cacheDir, "images").also { it.mkdirs() }
    val file = File(cachePath, "verdly_share_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    activity.startActivity(Intent.createChooser(intent, chooserTitle))
    return true
}

/**
 * Shares a premium Verdly social card with referral link. No Firebase — uses the system share sheet only.
 */
fun shareStreakCardWithImage(
    context: Context,
    headline: String,
    habitName: String,
) {
    val fullCode = ReferralManager.getReferralCode(context)
    val pathCode = referralPathCode(fullCode)
    val link = "verdly.app/r/$pathCode"
    val metric = headline.filter { it.isDigit() }.ifBlank { "PRO" }
    val metricLabel = if (headline.contains("focus", ignoreCase = true)) "focus complete" else "day streak"
    val bitmap = roundedCardBitmap(
        eyebrow = "Verdly proof",
        title = headline,
        subtitle = habitName,
        metric = metric,
        metricLabel = metricLabel,
        accentOverride = AndroidColor.rgb(149, 213, 178),
        rawInviteCode = pathCode,
        link = link,
        variantSeed = "$pathCode|$headline|$habitName",
    )
    shareBitmapCard(
        context = context,
        bitmap = bitmap,
        shareText = "$headline — $habitName\nJoin me on Verdly: $link",
        chooserTitle = "Share your Verdly card",
    )
}

fun shareChallengeInviteCard(
    context: Context,
    challengeName: String,
    stake: String,
    inviteCode: String,
    challengeId: String,
    daysRemaining: Long? = null,
    memberCount: Int? = null,
) {
    val fullCode = ReferralManager.getReferralCode(context)
    val referralCode = referralPathCode(fullCode)
    val joinCode = inviteCode.ifBlank { challengeId.take(8).uppercase() }
    val challengeLink = "https://verdlyhabits.page.link/challenge/$joinCode"
    val referralLink = "verdly.app/r/$referralCode"
    val metric = when {
        daysRemaining != null -> "${daysRemaining}D"
        memberCount != null -> "$memberCount"
        else -> "LIVE"
    }
    val metricLabel = if (daysRemaining != null) "days left" else "challenge"
    val subtitle = stake.ifBlank { "Join the challenge. Post proof. Keep the streak alive." }
    val bitmap = roundedCardBitmap(
        eyebrow = "Challenge invite",
        title = challengeName,
        subtitle = subtitle,
        metric = metric,
        metricLabel = metricLabel,
        accentOverride = AndroidColor.rgb(255, 179, 0),
        rawInviteCode = joinCode,
        link = challengeLink,
        variantSeed = "$joinCode|$challengeName",
    )
    shareBitmapCard(
        context = context,
        bitmap = bitmap,
        shareText = "Join my $challengeName challenge on Verdly.\nInvite code: $joinCode\n$challengeLink\n\nNew to Verdly? $referralLink",
        chooserTitle = "Share challenge invite",
    )
}

/** Goal completion share card with referral link baked in. */
fun shareGoalCompletionCard(
    context: Context,
    goalTitle: String,
    whyStatement: String,
    daysTaken: Long,
) {
    val fullCode = ReferralManager.getReferralCode(context)
    val pathCode = referralPathCode(fullCode)
    val link = "verdly.app/r/$pathCode"
    val shareText = buildString {
        appendLine("I just completed my goal on Verdly: \"$goalTitle\"")
        appendLine()
        appendLine("\"$whyStatement\"")
        appendLine()
        appendLine("Took me $daysTaken days. Join me with code $fullCode")
        appendLine("https://$link")
    }
    val bitmap = roundedCardBitmap(
        eyebrow = "Goal completed",
        title = goalTitle,
        subtitle = whyStatement.take(80),
        metric = "${daysTaken}D",
        metricLabel = "to finish",
        accentOverride = AndroidColor.rgb(149, 213, 178),
        rawInviteCode = pathCode,
        link = link,
        variantSeed = "$fullCode|goal|$goalTitle",
    )
    shareBitmapCard(
        context = context,
        bitmap = bitmap,
        shareText = shareText,
        chooserTitle = "Share your goal win",
    )
}

/** Branded invite card + full invite text for the referral program. */
fun shareReferralInviteCard(
    context: Context,
    fullCode: String,
    shortCode: String,
    shareLink: String,
    shareText: String,
): Boolean {
    val pathCode = referralPathCode(fullCode.ifBlank { shortCode })
    val displayLink = shareLink.removePrefix("https://").ifBlank { "verdly.app/r/$pathCode" }
    val bitmap = roundedCardBitmap(
        eyebrow = "Verdly invite",
        title = "Join me on Verdly",
        subtitle = "We both unlock Pro when you sign up",
        metric = pathCode,
        metricLabel = "your code",
        accentOverride = AndroidColor.rgb(149, 213, 178),
        rawInviteCode = pathCode,
        link = displayLink,
        variantSeed = "$fullCode|referral",
    )
    return shareBitmapCard(
        context = context,
        bitmap = bitmap,
        shareText = shareText,
        chooserTitle = "Invite friends to Verdly",
    )
}
