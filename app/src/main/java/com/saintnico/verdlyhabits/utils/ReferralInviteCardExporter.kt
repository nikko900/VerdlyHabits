package com.saintnico.verdlyhabits.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.referral.ReferralQrEncoder
import java.io.File
import java.io.FileOutputStream

object ReferralInviteCardExporter {

    private const val FILE_PROVIDER_SUFFIX = "fileprovider"

    // Measured from share_referral_invite_bg.png (576×1024), scaled to 1080×1920.
    private const val SRC_W = 576f
    private const val BOX_LEFT_SRC = 47f
    private const val BOX_TOP_SRC = 383f
    private const val BOX_RIGHT_SRC = 528f
    private const val BOX_BOTTOM_SRC = 640f
    private const val CIRCLE_CX_SRC = 288f
    private const val CIRCLE_CY_SRC = 512f
    private const val CIRCLE_R_SRC = 198f

    fun generateInviteCardBitmap(
        context: Context,
        displayCode: String,
        shareLink: String,
    ): Bitmap {
        val width = 1080
        val height = 1920
        val scale = width / SRC_W

        val output = createBitmap(width, height)
        val canvas = android.graphics.Canvas(output)

        val bg = BitmapFactory.decodeResource(context.resources, R.drawable.share_referral_invite_bg)
        if (bg != null) {
            canvas.drawBitmap(
                bg,
                null,
                RectF(0f, 0f, width.toFloat(), height.toFloat()),
                Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
            )
            if (!bg.isRecycled) bg.recycle()
        } else {
            canvas.drawColor(0xFF5CBE87.toInt())
        }

        val cx = CIRCLE_CX_SRC * scale
        val cy = CIRCLE_CY_SRC * scale
        val circleR = CIRCLE_R_SRC * scale

        drawArcHeadline(
            canvas = canvas,
            text = "INVITE A FRIEND",
            cx = cx,
            cy = cy,
            radius = circleR + 28f,
        )

        val box = RectF(
            BOX_LEFT_SRC * scale,
            BOX_TOP_SRC * scale,
            BOX_RIGHT_SRC * scale,
            BOX_BOTTOM_SRC * scale,
        )
        drawQrWell(canvas, box, shareLink)
        drawFooterCopy(canvas, width, height, box.bottom, displayCode, shareLink)
        return output
    }

    private fun drawArcHeadline(
        canvas: android.graphics.Canvas,
        text: String,
        cx: Float,
        cy: Float,
        radius: Float,
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B3D2E.toInt()
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.28f
            setShadowLayer(6f, 0f, 1.5f, 0x33000000)
        }
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val path = Path().apply { addArc(oval, 210f, -120f) }
        val measure = PathMeasure(path, false)
        val hOffset = ((measure.length - paint.measureText(text)) / 2f).coerceAtLeast(0f)
        canvas.drawTextOnPath(text, path, hOffset, 0f, paint)
    }

    private fun drawQrWell(
        canvas: android.graphics.Canvas,
        box: RectF,
        shareLink: String,
    ) {
        val ink = 0xFF0B3D2E.toInt()
        val scanPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.22f
        }
        canvas.drawText("SCAN TO JOIN", box.centerX(), box.top + 48f, scanPaint)

        val qrSize = 280
        val qrBitmap = ReferralQrEncoder.encodeBitmap(shareLink, qrSize)
        val qrLeft = box.centerX() - qrSize / 2f
        val qrTop = box.centerY() - qrSize / 2f + 8f
        val pad = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF6F3E9.toInt() }
        canvas.drawRoundRect(
            RectF(qrLeft - 18f, qrTop - 18f, qrLeft + qrSize + 18f, qrTop + qrSize + 18f),
            22f, 22f, pad,
        )
        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)
    }

    private fun drawFooterCopy(
        canvas: android.graphics.Canvas,
        width: Int,
        height: Int,
        boxBottom: Float,
        displayCode: String,
        shareLink: String,
    ) {
        val ink = 0xFF0B3D2E.toInt()
        val gold = 0xFFC4922A.toInt()
        val cx = width / 2f
        var y = boxBottom + 72f

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.24f
        }
        val codePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 52f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.12f
        }
        val linkPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1B5E40.toInt()
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("YOUR CODE", cx, y, labelPaint)
        y += 58f
        canvas.drawText(displayCode, cx, y, codePaint)
        y += 44f
        canvas.drawText(shareLink.removePrefix("https://"), cx, y, linkPaint)

        y += 48f
        val divider = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold
            strokeWidth = 3f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx - 48f, y, cx + 48f, y, divider)
        canvas.drawCircle(cx - 48f, y, 4f, divider)
        canvas.drawCircle(cx + 48f, y, 4f, divider)

        y += 36f
        drawBonusChips(
            canvas = canvas,
            cx = cx,
            y = y,
            theyDays = ReferralManager.REFEREE_BONUS_DAYS,
            youDays = ReferralManager.REFERRER_REWARD_DAYS,
        )

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF1B5E40.toInt()
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Built with Verdly", cx, height - 72f, footerPaint)
    }

    private fun drawBonusChips(
        canvas: android.graphics.Canvas,
        cx: Float,
        y: Float,
        theyDays: Int,
        youDays: Int,
    ) {
        val chips = listOf(
            "THEY GET" to "$theyDays days Premium",
            "YOU GET" to "$youDays days Premium",
        )
        val chipW = 280f
        val chipH = 92f
        val gap = 28f
        val total = chips.size * chipW + gap
        var left = cx - total / 2f

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6F6F3E9.toInt() }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            color = 0xFF1B7A4D.toInt()
        }
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFC4922A.toInt()
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.16f
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B3D2E.toInt()
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        for ((label, value) in chips) {
            val rect = RectF(left, y, left + chipW, y + chipH)
            canvas.drawRoundRect(rect, 24f, 24f, bg)
            canvas.drawRoundRect(rect, 24f, 24f, stroke)
            canvas.drawText(label, left + chipW / 2f, y + 32f, labelPaint)
            canvas.drawText(value, left + chipW / 2f, y + 68f, valuePaint)
            left += chipW + gap
        }
    }

    fun exportAndShare(
        context: Context,
        displayCode: String,
        shareLink: String,
        shareText: String,
    ): Boolean {
        return try {
            val bitmap = generateInviteCardBitmap(context, displayCode, shareLink)
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "referral_invite_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            val authority = "${context.packageName}.$FILE_PROVIDER_SUFFIX"
            val shareUri = FileProvider.getUriForFile(context, authority, file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Invite friends to Verdly"))
            true
        } catch (_: Exception) {
            false
        }
    }
}
