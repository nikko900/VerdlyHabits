package com.saintnico.verdlyhabits.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.saintnico.verdlyhabits.challenge.ChallengeInviteHelper
import com.saintnico.verdlyhabits.referral.ReferralQrEncoder
import com.saintnico.verdlyhabits.ui.theme.ProfileAccent
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.min

/**
 * Challenge invite share PNG — mirrors [com.saintnico.verdlyhabits.ui.components.challenge.ChallengeInvitePreviewCard]
 * (themed gradient card + QR), not the botanical profile trading card.
 */
object ChallengeInviteCardExporter {

    private const val FILE_PROVIDER_SUFFIX = "fileprovider"

    /** Landscape invite card matching the in-app Challenge Ready preview. */
    private const val WIDTH = 1080
    private const val HEIGHT = 520

    fun themeForInvite(inviteCode: String): ProfileAccent {
        val idx = abs(inviteCode.hashCode()) % ProfileAccents.all.size
        return ProfileAccents.all[idx]
    }

    fun generateInviteCardBitmap(
        context: Context,
        challengeName: String,
        inviteCode: String,
        stake: String = "",
        daysRemaining: Long? = null,
        memberCount: Int? = null,
    ): Bitmap {
        val code = ChallengeInviteHelper.normalizedCode(inviteCode)
        val link = ChallengeInviteHelper.shareLinkLabel(code)
        val theme = themeForInvite(code)

        val output = createBitmap(WIDTH, HEIGHT)
        val canvas = android.graphics.Canvas(output)

        val pad = 36f
        val card = RectF(pad, pad, WIDTH - pad, HEIGHT - pad)
        val radius = 44f

        // Themed vertical gradient (same stops as ChallengeInvitePreviewCard).
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                card.left,
                card.top,
                card.left,
                card.bottom,
                intArrayOf(
                    theme.heroTop.toArgb(),
                    theme.heroMid.toArgb(),
                    theme.heroBottom.toArgb(),
                ),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRoundRect(card, radius, radius, fill)

        // Soft corner glow.
        val glowCx = card.right - card.width() * 0.12f
        val glowCy = card.top + card.height() * 0.12f
        val glowR = min(card.width(), card.height()) * 0.45f
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                glowCx,
                glowCy,
                glowR,
                intArrayOf(theme.glow.copy(alpha = 0.35f).toArgb(), 0x00000000),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.save()
        canvas.clipRect(card)
        canvas.drawCircle(glowCx, glowCy, glowR, glowPaint)
        canvas.restore()

        val contentLeft = card.left + 40f
        val contentRight = card.right - 40f
        val qrSize = 196
        val textMax = contentRight - qrSize - 36f

        var y = card.top + 56f

        val chipPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.highlight.copy(alpha = 0.9f).toArgb()
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.14f
        }
        canvas.drawText("JOIN MY CHALLENGE", contentLeft, y, chipPaint)

        y += 52f
        val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.ink.toArgb()
            textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val title = if (challengeName.length > 48) challengeName.take(47) + "…" else challengeName
        while (namePaint.measureText(title) > textMax - contentLeft && namePaint.textSize > 34f) {
            namePaint.textSize -= 2f
        }
        val titleLines = wrapLeft(title, namePaint, textMax - contentLeft).take(2)
        for (line in titleLines) {
            canvas.drawText(line, contentLeft, y, namePaint)
            y += namePaint.textSize * 1.18f
        }

        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xD9FFFFFF.toInt()
            textSize = 30f
        }
        if (stake.isNotBlank()) {
            y += 10f
            canvas.drawText(
                truncate("Stake: $stake", metaPaint, textMax - contentLeft),
                contentLeft,
                y,
                metaPaint,
            )
        }
        daysRemaining?.let {
            y += 36f
            metaPaint.color = 0xB3FFFFFF.toInt()
            metaPaint.textSize = 28f
            canvas.drawText("$it days left", contentLeft, y, metaPaint)
        }
        memberCount?.let {
            y += 34f
            metaPaint.color = 0xB3FFFFFF.toInt()
            metaPaint.textSize = 28f
            canvas.drawText("$it members in", contentLeft, y, metaPaint)
        }

        y = card.bottom - 92f
        val codePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.glow.toArgb()
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.22f
        }
        canvas.drawText(code, contentLeft, y, codePaint)

        y += 36f
        val linkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xA6FFFFFF.toInt()
            textSize = 24f
        }
        canvas.drawText(truncate(link, linkPaint, textMax - contentLeft), contentLeft, y, linkPaint)

        // QR well (right side), same as preview card.
        val qrBitmap = ReferralQrEncoder.encodeBitmap(
            ChallengeInviteHelper.shareLink(code),
            qrSize,
        )
        val qrLeft = contentRight - qrSize
        val qrTop = card.centerY() - qrSize / 2f
        val wellPad = 14f
        val well = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val wellRect = RectF(
            qrLeft - wellPad,
            qrTop - wellPad,
            qrLeft + qrSize + wellPad,
            qrTop + qrSize + wellPad,
        )
        canvas.drawRoundRect(wellRect, 22f, 22f, well)
        canvas.drawBitmap(
            qrBitmap,
            qrLeft,
            qrTop,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        if (!qrBitmap.isRecycled) qrBitmap.recycle()

        return output
    }

    private fun wrapLeft(text: String, paint: TextPaint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = candidate
            } else {
                if (current.isNotEmpty()) lines += current
                current = word
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines.ifEmpty { listOf(text) }
    }

    private fun truncate(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.length > 1 && paint.measureText("$t…") > maxWidth) {
            t = t.dropLast(1)
        }
        return "$t…"
    }

    fun exportAndShare(
        context: Context,
        challengeName: String,
        inviteCode: String,
        stake: String = "",
        daysRemaining: Long? = null,
        memberCount: Int? = null,
        shareText: String,
    ): Boolean {
        return try {
            val bitmap = generateInviteCardBitmap(
                context = context,
                challengeName = challengeName,
                inviteCode = inviteCode,
                stake = stake,
                daysRemaining = daysRemaining,
                memberCount = memberCount,
            )
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "challenge_invite_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (!bitmap.isRecycled) bitmap.recycle()
            val authority = "${context.packageName}.$FILE_PROVIDER_SUFFIX"
            val shareUri = FileProvider.getUriForFile(context, authority, file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Invite friends to your challenge"))
            true
        } catch (_: Exception) {
            false
        }
    }
}
