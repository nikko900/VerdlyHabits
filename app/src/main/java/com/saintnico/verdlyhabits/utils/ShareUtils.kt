package com.saintnico.verdlyhabits.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.saintnico.verdlyhabits.referral.ReferralManager
import java.io.File
import java.io.FileOutputStream

object ShareUtils {

    fun overlayStreakOnBitmap(context: Context, sourceUri: Uri, streak: Int, habitName: String): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream?.close()

            val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(resultBitmap)
            val width = resultBitmap.width.toFloat()
            val height = resultBitmap.height.toFloat()

            val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    height * 0.48f,
                    0f,
                    height,
                    intArrayOf(Color.TRANSPARENT, Color.argb(210, 3, 12, 8)),
                    null,
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, height * 0.42f, width, height, scrimPaint)

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(205, 8, 27, 18)
            }
            val card = RectF(width * 0.055f, height * 0.63f, width * 0.945f, height * 0.94f)
            canvas.drawRoundRect(card, width * 0.05f, width * 0.05f, cardPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = width * 0.006f
                color = Color.argb(115, 149, 213, 178)
            }
            canvas.drawRoundRect(card, width * 0.05f, width * 0.05f, borderPaint)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = width * 0.076f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                setShadowLayer(8f, 0f, 5f, Color.BLACK)
            }

            val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#95D5B2")
                textSize = width * 0.045f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(190, 255, 255, 255)
                textSize = width * 0.036f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val referralCode = ReferralManager.getReferralCode(context).removePrefix("VERDLY-").uppercase()
            val left = card.left + width * 0.055f
            var y = card.top + height * 0.095f
            canvas.drawText("$streak day streak", left, y, paint)
            y += paint.textSize + height * 0.022f
            canvas.drawText(habitName, left, y, subtitlePaint)
            y += subtitlePaint.textSize + height * 0.038f
            canvas.drawText("Built with Verdly · verdly.app/r/$referralCode", left, y, detailPaint)

            val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 220
                textSize = width * 0.04f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Verdly", width * 0.9f, card.bottom - height * 0.045f, brandPaint)

            resultBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareHabitCompletion(context: Context, sourceUri: Uri, streak: Int, habitName: String, level: Int) {
        val overlayBitmap = overlayStreakOnBitmap(context, sourceUri, streak, habitName) ?: return

        try {
            // Save modified bitmap to cache
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "share_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            overlayBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val shareText = "Just crushed my $habitName habit! \uD83D\uDD25\n" +
                    "Current Streak: $streak days\n" +
                    "My Level: $level\n\n" +
                    "Join me on Verdly: verdly.app/r/${ReferralManager.getReferralCode(context).removePrefix("VERDLY-").uppercase()}"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share your victory"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
