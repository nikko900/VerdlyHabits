package com.saintnico.verdlyhabits.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import com.saintnico.verdlyhabits.ui.screens.focus.drawGrowingPlant
import java.io.File
import java.io.FileOutputStream

enum class StreakCardTheme(
    val bgTop: Color,
    val bgMid: Color,
    val bgBottom: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color
) {
    EmeraldDark(
        bgTop = Color(0xFF0A1610),
        bgMid = Color(0xFF1B4332),
        bgBottom = Color(0xFF0D1F14),
        textPrimary = Color(0xFFD8F3DC),
        textSecondary = Color(0xCCFFFFFF),
        accent = Color(0xFF52B788)
    ),
    CyberpunkNeon(
        bgTop = Color(0xFF0A021C),
        bgMid = Color(0xFF2E0942),
        bgBottom = Color(0xFF100224),
        textPrimary = Color(0xFF00FFCC),
        textSecondary = Color(0xCCFFFFFF),
        accent = Color(0xFFFF00FF)
    ),
    GoldenTitan(
        bgTop = Color(0xFF1A1505),
        bgMid = Color(0xFF42350A),
        bgBottom = Color(0xFF1F1A08),
        textPrimary = Color(0xFFFFD700),
        textSecondary = Color(0xCCFFFFFF),
        accent = Color(0xFFFFA500)
    ),
    MidnightViolet(
        bgTop = Color(0xFF0B0914),
        bgMid = Color(0xFF231A45),
        bgBottom = Color(0xFF120E22),
        textPrimary = Color(0xFFE2D6FF),
        textSecondary = Color(0xCCFFFFFF),
        accent = Color(0xFF9D7CFF)
    )
}

object StreakCardExporter {

    fun generateStreakCardBitmap(
        context: Context,
        headline: String,
        habitName: String,
        referralCode: String,
        theme: StreakCardTheme = StreakCardTheme.EmeraldDark,
        plantProgress: Float = 1f
    ): Bitmap {
        val width = 1080
        val height = 1920
        val imageBitmap = ImageBitmap(width, height)
        val canvas = Canvas(imageBitmap)
        val drawScope = CanvasDrawScope()

        drawScope.draw(
            Density(context),
            LayoutDirection.Ltr,
            canvas,
            Size(width.toFloat(), height.toFloat())
        ) {
            // Background Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(theme.bgTop, theme.bgMid, theme.bgBottom)
                ),
                size = Size(width.toFloat(), height.toFloat())
            )

            // Draw Plant in Center
            val plantSize = 500f
            val plantOffsetX = (width - plantSize) / 2f
            val plantOffsetY = height * 0.45f
            
            drawContext.canvas.save()
            drawContext.canvas.translate(plantOffsetX, plantOffsetY - plantSize / 2f)
            drawGrowingPlant(
                progress = plantProgress,
                isWilting = false,
                wiltAmount = 0f,
                width = plantSize,
                height = plantSize,
                colorTint = theme.accent
            )
            drawContext.canvas.restore()

            // Draw Texts using native canvas
            drawIntoCanvas { composeCanvas ->
                val nativeCanvas = composeCanvas.nativeCanvas

                val paintTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(
                        (theme.textPrimary.alpha * 255).toInt(),
                        (theme.textPrimary.red * 255).toInt(),
                        (theme.textPrimary.green * 255).toInt(),
                        (theme.textPrimary.blue * 255).toInt()
                    )
                    textSize = 120f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
                    textAlign = Paint.Align.CENTER
                    setShadowLayer(10f, 0f, 6f, android.graphics.Color.BLACK)
                }

                val paintHabit = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(
                        (theme.textSecondary.alpha * 255).toInt(),
                        (theme.textSecondary.red * 255).toInt(),
                        (theme.textSecondary.green * 255).toInt(),
                        (theme.textSecondary.blue * 255).toInt()
                    )
                    textSize = 60f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }

                val paintFooter = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(150, 255, 255, 255)
                    textSize = 40f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textAlign = Paint.Align.CENTER
                }

                val paintReferral = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(
                        255,
                        (theme.accent.red * 255).toInt(),
                        (theme.accent.green * 255).toInt(),
                        (theme.accent.blue * 255).toInt()
                    )
                    textSize = 45f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }

                nativeCanvas.drawText(headline, width / 2f, height * 0.65f, paintTitle)
                nativeCanvas.drawText(habitName, width / 2f, height * 0.72f, paintHabit)

                nativeCanvas.drawText("Built with Verdly", width / 2f, height * 0.88f, paintFooter)
                nativeCanvas.drawText("verdly.app/r/$referralCode", width / 2f, height * 0.92f, paintReferral)
            }
        }

        return imageBitmap.asAndroidBitmap()
    }

    fun exportAndShare(
        context: Context,
        headline: String,
        habitName: String,
        referralCode: String,
        theme: StreakCardTheme = StreakCardTheme.EmeraldDark,
        plantProgress: Float = 1f
    ) {
        try {
            val bitmap = generateStreakCardBitmap(context, headline, habitName, referralCode, theme, plantProgress)
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "streak_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareText = "$headline — $habitName\nI'm building better habits with Verdly. Join free: verdly.app/r/$referralCode"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share your milestone"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
