package com.saintnico.verdlyhabits.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.referral.ReferralQrEncoder
import com.saintnico.verdlyhabits.ui.theme.ProfileAccent
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Profile trading card — botanical texture over the player's [ProfileAccent] hero,
 * so a violet card shares violet (not a generic mint plaque).
 * Artboard source: [R.drawable.share_profile_card_bg] (576×1024) → 1080×1920.
 */
object ProfileShareCardExporter {

    private const val FILE_PROVIDER_SUFFIX = "fileprovider"

    private const val SRC_W = 576f
    private const val BOX_LEFT_SRC = 52f
    private const val BOX_TOP_SRC = 400f
    private const val BOX_RIGHT_SRC = 524f
    private const val BOX_BOTTOM_SRC = 632f

    data class ProfileShareData(
        val username: String,
        val displayName: String,
        val bestStreak: Int,
        val secondaryLine: String?,
        val isOwnCard: Boolean,
        val referralCode: String,
        val accentKey: String? = null,
        val photoUri: String? = null,
        val title: String = "",
        val titleColorArgb: Int? = null,
        val level: Int = 0,
        val levelTitle: String = "",
        val memberStoryLine: String = "",
        val flairs: List<String> = emptyList(),
        val membershipLabel: String? = null,
        val totalCompletions: Int = 0,
        val duoStreakDays: Int = 0,
        val bio: String = "",
        val proclaimedLabel: String? = null,
    )

    private data class Palette(
        val themeName: String,
        val base: Int,
        val highlight: Int,
        val ink: Int,
        val heroTop: Int,
        val heroMid: Int,
        val heroBottom: Int,
        val glow: Int,
        val muted: Int,
        val plaque: Int,
    ) {
        companion object {
            fun from(accent: ProfileAccent): Palette {
                val ink = accent.ink.toArgb()
                val mid = accent.heroMid.toArgb()
                return Palette(
                    themeName = accent.displayName.uppercase(),
                    base = accent.base.toArgb(),
                    highlight = accent.highlight.toArgb(),
                    ink = ink,
                    heroTop = accent.heroTop.toArgb(),
                    heroMid = mid,
                    heroBottom = accent.heroBottom.toArgb(),
                    glow = accent.glow.toArgb(),
                    muted = ColorUtils.setAlphaComponent(ink, 190),
                    plaque = ColorUtils.blendARGB(mid, 0xFFF4EDE0.toInt(), 0.18f),
                )
            }
        }
    }

    fun exportAndShare(
        context: Context,
        data: ProfileShareData,
        chooserTitle: String = "Share Verdly card",
        innerCard: Bitmap? = null,
    ): Boolean {
        val host = context
        Thread {
            try {
                val bitmap = generateBitmap(host.applicationContext, data, innerCard)
                Handler(Looper.getMainLooper()).post {
                    runCatching {
                        shareBitmap(host, bitmap, buildCaption(data), chooserTitle)
                    }
                }
            } catch (_: Exception) {
            }
        }.start()
        return true
    }

    fun exportFramed(
        context: Context,
        innerCard: Bitmap,
        caption: String,
        chooserTitle: String,
        accentKey: String?,
        referralCode: String,
    ): Boolean {
        val data = ProfileShareData(
            username = "",
            displayName = "",
            bestStreak = 0,
            secondaryLine = null,
            isOwnCard = true,
            referralCode = referralCode,
            accentKey = accentKey,
        )
        return try {
            val bitmap = generateBitmap(context, data, innerCard)
            shareBitmap(context, bitmap, caption, chooserTitle)
        } catch (_: Exception) {
            false
        }
    }

    private fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        caption: String,
        chooserTitle: String,
    ): Boolean {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "share_profile_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        if (!bitmap.isRecycled) bitmap.recycle()
        val shareUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.$FILE_PROVIDER_SUFFIX",
            file,
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, shareUri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        return true
    }

    fun generateBitmap(
        context: Context,
        data: ProfileShareData,
        innerCard: Bitmap? = null,
    ): Bitmap {
        val width = 1080
        val height = 1920
        val scale = width / SRC_W
        val output = createBitmap(width, height)
        val canvas = android.graphics.Canvas(output)
        val palette = Palette.from(ProfileAccents.byKey(data.accentKey))

        paintThemedStage(canvas, width, height, palette, context)

        val box = RectF(
            BOX_LEFT_SRC * scale,
            BOX_TOP_SRC * scale,
            BOX_RIGHT_SRC * scale,
            BOX_BOTTOM_SRC * scale,
        )
        paintPlaque(canvas, box, palette)
        val photo = if (innerCard == null) loadPhoto(context, data.photoUri) else null
        if (innerCard != null && !innerCard.isRecycled) {
            drawInnerCard(canvas, box, innerCard)
        } else {
            drawPlaqueContent(canvas, box, data, palette, photo)
        }
        if (photo != null && !photo.isRecycled) photo.recycle()
        drawBelowPlaque(canvas, width, height, box.bottom, data, palette)
        return output
    }

    /**
     * Places the real profile / trophy / goal screenshot inside the botanical
     * plate. Inset keeps the brown stroke from [R.drawable.share_profile_card_bg].
     */
    private fun drawInnerCard(canvas: android.graphics.Canvas, box: RectF, portrait: Bitmap) {
        val inset = 14f
        val inner = RectF(
            box.left + inset,
            box.top + inset,
            box.right - inset,
            box.bottom - inset,
        )
        val save = canvas.save()
        val clip = android.graphics.Path().apply {
            addRoundRect(inner, 28f, 28f, android.graphics.Path.Direction.CW)
        }
        canvas.clipPath(clip)
        val scale = minOf(inner.width() / portrait.width, inner.height() / portrait.height)
        val dw = portrait.width * scale
        val dh = portrait.height * scale
        val dest = RectF(
            inner.centerX() - dw / 2f,
            inner.centerY() - dh / 2f,
            inner.centerX() + dw / 2f,
            inner.centerY() + dh / 2f,
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(portrait, null, dest, paint)
        canvas.restoreToCount(save)
    }

    fun buildCaption(data: ProfileShareData): String {
        val link = ReferralManager.shareLinkLabel(
            ReferralManager.shortCode(data.referralCode),
        )
        val handle = identityHandle(data)
        return if (data.isOwnCard) {
            "This is my Verdly card. Come plant with me. $link"
        } else {
            "$handle on Verdly. $link"
        }
    }

    private fun identityHandle(data: ProfileShareData): String {
        val handle = data.username.trim().removePrefix("@")
        return when {
            handle.isNotBlank() && handle != "UnknownRival" -> "@$handle"
            data.displayName.isNotBlank() -> data.displayName
            else -> "Verdly"
        }
    }

    private fun paintThemedStage(
        canvas: android.graphics.Canvas,
        width: Int,
        height: Int,
        palette: Palette,
        context: Context,
    ) {
        val w = width.toFloat()
        val h = height.toFloat()
        val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                intArrayOf(palette.heroTop, palette.heroMid, palette.heroBottom),
                floatArrayOf(0f, 0.48f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, heroPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.85f,
                h * 0.12f,
                minOf(w, h) * 0.55f,
                intArrayOf(ColorUtils.setAlphaComponent(palette.glow, 110), 0x00000000),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, glowPaint)

        val liftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.14f,
                h * 0.88f,
                minOf(w, h) * 0.42f,
                intArrayOf(ColorUtils.setAlphaComponent(palette.highlight, 48), 0x00000000),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, liftPaint)

        val bg = BitmapFactory.decodeResource(context.resources, R.drawable.share_profile_card_bg)
        if (bg != null) {
            val artPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = PorterDuffColorFilter(palette.base, PorterDuff.Mode.MULTIPLY)
                alpha = 168
            }
            canvas.drawBitmap(bg, null, RectF(0f, 0f, w, h), artPaint)
            if (!bg.isRecycled) bg.recycle()
        }

        // Vignette: darkened edges so the plaque reads lit, same finish as trophy cards.
        val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f,
                h * 0.45f,
                maxOf(w, h) * 0.78f,
                intArrayOf(0x00000000, ColorUtils.setAlphaComponent(0xFF000000.toInt(), 82)),
                floatArrayOf(0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, vignette)

        // Themed hairline frame — the trading-card border in the card's own metal.
        val inset = 30f
        val keyline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = ColorUtils.setAlphaComponent(palette.highlight, 150)
        }
        canvas.drawRoundRect(RectF(inset, inset, w - inset, h - inset), 44f, 44f, keyline)
        val innerKeyline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), 26)
        }
        canvas.drawRoundRect(RectF(inset + 12f, inset + 12f, w - inset - 12f, h - inset - 12f), 36f, 36f, innerKeyline)
    }

    private fun paintPlaque(canvas: android.graphics.Canvas, box: RectF, palette: Palette) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(palette.plaque, 230)
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 5f
            color = ColorUtils.setAlphaComponent(palette.highlight, 170)
        }
        canvas.drawRoundRect(box, 36f, 36f, fill)
        canvas.drawRoundRect(box, 36f, 36f, stroke)
    }

    private fun drawPlaqueContent(
        canvas: android.graphics.Canvas,
        box: RectF,
        data: ProfileShareData,
        palette: Palette,
        photo: Bitmap?,
    ) {
        val pad = 36f
        val photoSize = 148f
        val photoLeft = box.right - pad - photoSize
        val photoTop = box.top + 38f
        val textLeft = box.left + pad
        val textRight = photoLeft - 22f
        val textWidth = (textRight - textLeft).coerceAtLeast(200f)

        drawAvatar(
            canvas = canvas,
            photo = photo,
            letter = (data.displayName.ifBlank { data.username }.ifBlank { "V" }).first().uppercaseChar().toString(),
            dest = RectF(photoLeft, photoTop, photoLeft + photoSize, photoTop + photoSize),
            palette = palette,
            premium = !data.membershipLabel.isNullOrBlank(),
        )

        val handle = identityHandle(data)
        val name = data.displayName.ifBlank { handle.removePrefix("@") }.ifBlank { "Verdly" }

        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.ink
            textSize = 48f
            typeface = Typeface.create("serif", Typeface.BOLD)
        }
        val handlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.muted
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = data.titleColorArgb ?: palette.highlight
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.ink
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val storyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.muted
            textSize = 20f
        }

        var y = box.top + 62f
        canvas.drawText(truncate(name, namePaint, textWidth), textLeft, y, namePaint)
        y += 32f
        canvas.drawText(truncate(handle, handlePaint, textWidth), textLeft, y, handlePaint)

        val title = data.title.trim()
        if (title.isNotBlank()) {
            y += 36f
            canvas.drawText(truncate(title, titlePaint, textWidth), textLeft, y, titlePaint)
        }

        val levelBits = buildList {
            if (data.level > 0) add("Level ${data.level}")
            if (data.levelTitle.isNotBlank()) add(data.levelTitle)
            data.membershipLabel?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        if (levelBits.isNotEmpty()) {
            y += 30f
            canvas.drawText(truncate(levelBits.joinToString(" · "), metaPaint, textWidth), textLeft, y, metaPaint)
        }

        val story = data.memberStoryLine.ifBlank {
            buildList {
                if (data.bestStreak > 0) add("${data.bestStreak}-day best streak")
                if (data.totalCompletions > 0) add("${data.totalCompletions} planted")
            }.joinToString(" · ")
        }
        if (story.isNotBlank()) {
            y += 16f
            val layout = StaticLayout.Builder
                .obtain(story, 0, story.length, storyPaint, textWidth.toInt())
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setIncludePad(false)
                .build()
            canvas.save()
            canvas.translate(textLeft, y)
            layout.draw(canvas)
            canvas.restore()
            y += layout.height
        }

        var chipX = textLeft
        val chipY = (box.bottom - 58f).coerceAtLeast(y + 18f)
        data.proclaimedLabel?.takeIf { it.isNotBlank() }?.let { label ->
            chipX = drawChip(canvas, label, chipX, chipY, data.titleColorArgb ?: palette.highlight)
        }
        data.flairs.take(2).forEach { label ->
            chipX = drawChip(canvas, label, chipX, chipY, palette.highlight)
        }

        val stat = buildList {
            if (data.bestStreak > 0) add("${data.bestStreak}d streak")
            if (data.duoStreakDays > 0) add("${data.duoStreakDays}d duo")
        }.joinToString(" · ")
        if (stat.isNotBlank()) {
            val statPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.ink
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(stat, box.right - pad, box.bottom - 32f, statPaint)
        }
    }

    private fun drawChip(
        canvas: android.graphics.Canvas,
        label: String,
        x: Float,
        y: Float,
        accent: Int,
    ): Float {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val text = truncate(label, paint, 220f)
        val w = paint.measureText(text) + 22f
        val h = 30f
        val rect = RectF(x, y, x + w, y + h)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ColorUtils.setAlphaComponent(accent, 36)
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = ColorUtils.setAlphaComponent(accent, 150)
        }
        canvas.drawRoundRect(rect, 15f, 15f, fill)
        canvas.drawRoundRect(rect, 15f, 15f, stroke)
        canvas.drawText(text, x + 11f, y + 21f, paint)
        return x + w + 8f
    }

    private fun drawAvatar(
        canvas: android.graphics.Canvas,
        photo: Bitmap?,
        letter: String,
        dest: RectF,
        palette: Palette,
        premium: Boolean,
    ) {
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = if (premium) 8f else 5f
            color = if (premium) 0xFFFFC857.toInt() else ColorUtils.setAlphaComponent(palette.highlight, 200)
        }
        canvas.drawOval(dest, ring)
        val inner = RectF(dest).apply { inset(if (premium) 8f else 6f, if (premium) 8f else 6f) }
        if (photo != null && !photo.isRecycled) {
            val cropped = circleCrop(photo, inner.width().toInt().coerceAtLeast(8))
            canvas.drawBitmap(cropped, null, inner, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            if (cropped !== photo && !cropped.isRecycled) cropped.recycle()
        } else {
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ColorUtils.setAlphaComponent(palette.highlight, 48)
            }
            canvas.drawOval(inner, fill)
            val letterPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.ink
                textSize = inner.width() * 0.42f
                typeface = Typeface.create("serif", Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val fm = letterPaint.fontMetrics
            canvas.drawText(
                letter,
                inner.centerX(),
                inner.centerY() - (fm.ascent + fm.descent) / 2f,
                letterPaint,
            )
        }
    }

    private fun circleCrop(src: Bitmap, size: Int): Bitmap {
        val out = createBitmap(size, size)
        val canvas = android.graphics.Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, null, RectF(0f, 0f, size.toFloat(), size.toFloat()), paint)
        return out
    }

    private fun loadPhoto(context: Context, uri: String?): Bitmap? {
        if (uri.isNullOrBlank()) return null
        return runCatching {
            if (uri.startsWith("http", ignoreCase = true)) {
                val conn = URL(uri).openConnection().apply {
                    connectTimeout = 4000
                    readTimeout = 4000
                }
                conn.getInputStream().use { BitmapFactory.decodeStream(it) }
            } else {
                context.contentResolver.openInputStream(Uri.parse(uri))?.use { BitmapFactory.decodeStream(it) }
            }
        }.getOrNull()
    }

    private fun drawBelowPlaque(
        canvas: android.graphics.Canvas,
        width: Int,
        height: Int,
        boxBottom: Float,
        data: ProfileShareData,
        palette: Palette,
    ) {
        val cx = width / 2f
        val link = ReferralManager.shareLink(data.referralCode)
        val linkLabel = ReferralManager.shareLinkLabel(ReferralManager.shortCode(data.referralCode))

        var y = boxBottom + 64f
        val joinPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.highlight
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
        }
        canvas.drawText("GROW WITH VERDLY", cx, y, joinPaint)

        y += 40f
        val linkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.ink
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(linkLabel, cx, y, linkPaint)

        val qrSize = 168
        val qr = runCatching { ReferralQrEncoder.encodeBitmap(link, qrSize) }.getOrNull()
        if (qr != null) {
            val left = (width - qrSize) / 2f
            val top = y + 36f
            val pad = 14f
            val well = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ColorUtils.blendARGB(palette.heroBottom, 0xFFF6F3E9.toInt(), 0.22f)
            }
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 3f
                color = palette.highlight
            }
            val rect = RectF(left - pad, top - pad, left + qrSize + pad, top + qrSize + pad)
            canvas.drawRoundRect(rect, 20f, 20f, well)
            canvas.drawRoundRect(rect, 20f, 20f, stroke)
            canvas.drawBitmap(qr, left, top, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            if (!qr.isRecycled) qr.recycle()
        }

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.highlight
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Built with Verdly", cx, height - 72f, footerPaint)
    }

    private fun truncate(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.length > 1 && paint.measureText("$t…") > maxWidth) {
            t = t.dropLast(1)
        }
        return "$t…"
    }
}
