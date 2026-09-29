package com.saintnico.verdlyhabits.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorNode
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.saintnico.verdlyhabits.R
import kotlin.math.max
import kotlin.math.min

/**
 * Circular avatars for the notification shade — the Instagram / iMessage look.
 * Hardware bitmaps are disabled so the result can be posted on a [android.app.Notification].
 */
object NotificationAvatars {
    private const val SIZE_PX = 256
    @Volatile private var cachedLoader: ImageLoader? = null

    private fun imageLoader(context: Context): ImageLoader {
        return cachedLoader ?: ImageLoader(context.applicationContext).also { cachedLoader = it }
    }

    private val fallbackPalette = intArrayOf(
        0xFF1D6B44.toInt(),
        0xFF40916C.toInt(),
        0xFF2D6A4F.toInt(),
        0xFFD4890A.toInt(),
        0xFF1B4332.toInt(),
        0xFF52796F.toInt(),
    )

    suspend fun loadCircular(
        context: Context,
        photoUrl: String?,
        displayName: String,
    ): Bitmap {
        val remote = photoUrl?.takeIf { it.isNotBlank() }?.let { url ->
            runCatching { fetch(context, url) }.getOrNull()
        }
        if (remote != null) return toCircle(remote)
        return letterCircle(displayName)
    }

    fun brandCircle(context: Context): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, R.drawable.app_icon) ?: return null
        return toCircle(drawableAsBitmap(drawable))
    }

    /** Circular large-icon for a habit/goal/motivation glyph (book, flame, etc.). */
    fun glyphCircle(
        vector: ImageVector,
        backgroundColor: Int = 0xFF1B4332.toInt(),
        glyphColor: Int = Color.WHITE,
    ): Bitmap {
        val out = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
        canvas.drawCircle(SIZE_PX / 2f, SIZE_PX / 2f, SIZE_PX / 2f, fill)

        val inset = SIZE_PX * 0.28f
        val dest = SIZE_PX - inset * 2f
        val scale = dest / max(vector.viewportWidth, vector.viewportHeight)
        canvas.save()
        canvas.translate(
            (SIZE_PX - vector.viewportWidth * scale) / 2f,
            (SIZE_PX - vector.viewportHeight * scale) / 2f,
        )
        canvas.scale(scale, scale)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = glyphColor
            style = Paint.Style.FILL
            isDither = true
        }
        drawVectorNode(canvas, vector.root, paint, glyphColor)
        canvas.restore()
        return out
    }

    fun glyphCircleForColor(vector: ImageVector, color: Int): Bitmap {
        val bg = color or 0xFF000000.toInt()
        val glyph = if (isDark(bg)) Color.WHITE else 0xFF102018.toInt()
        return glyphCircle(vector, bg, glyph)
    }

    fun glyphCircleForColor(vector: ImageVector, colorLong: Long): Bitmap =
        glyphCircleForColor(vector, ((colorLong and 0xFFFFFFFFL) or 0xFF000000L).toInt())

    private fun drawVectorNode(canvas: Canvas, node: VectorNode, paint: Paint, glyphColor: Int) {
        when (node) {
            is VectorGroup -> {
                canvas.save()
                if (node.translationX != 0f || node.translationY != 0f) {
                    canvas.translate(node.translationX, node.translationY)
                }
                if (node.rotation != 0f) {
                    canvas.rotate(node.rotation, node.pivotX, node.pivotY)
                }
                if (node.scaleX != 1f || node.scaleY != 1f) {
                    canvas.scale(node.scaleX, node.scaleY, node.pivotX, node.pivotY)
                }
                for (child in node) {
                    drawVectorNode(canvas, child, paint, glyphColor)
                }
                canvas.restore()
            }
            is VectorPath -> {
                val androidPath = PathParser()
                    .addPathNodes(node.pathData)
                    .toPath()
                    .asAndroidPath()
                paint.style = Paint.Style.FILL
                paint.color = glyphColor
                paint.alpha = (node.fillAlpha * 255f).toInt().coerceIn(0, 255)
                canvas.drawPath(androidPath, paint)
                if (node.strokeLineWidth > 0f) {
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = node.strokeLineWidth
                    paint.alpha = (node.strokeAlpha * 255f).toInt().coerceIn(0, 255)
                    canvas.drawPath(androidPath, paint)
                }
            }
        }
    }

    private fun isDark(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return (r * 299 + g * 587 + b * 114) / 1000 < 158
    }

    private suspend fun fetch(context: Context, url: String): Bitmap? {
        val request = ImageRequest.Builder(context.applicationContext)
            .data(url)
            .size(SIZE_PX)
            .allowHardware(false)
            .build()
        val result = imageLoader(context).execute(request)
        val drawable = (result as? SuccessResult)?.drawable ?: return null
        return drawableAsBitmap(drawable)
    }

    private fun drawableAsBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        return drawable.toBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
    }

    fun toCircle(src: Bitmap): Bitmap {
        val size = min(src.width, src.height).coerceAtLeast(1)
        val squared = if (src.width != size || src.height != size) {
            val x = ((src.width - size) / 2).coerceAtLeast(0)
            val y = ((src.height - size) / 2).coerceAtLeast(0)
            Bitmap.createBitmap(src, x, y, size, size)
        } else {
            src
        }
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        return out
    }

    fun letterCircle(name: String): Bitmap {
        val letter = name.trim()
            .removePrefix("@")
            .firstOrNull { it.isLetterOrDigit() }
            ?.uppercaseChar()
            ?.toString()
            ?: "V"
        val bg = fallbackPalette[name.hashCode().and(0x7FFFFFFF) % fallbackPalette.size]
        val out = Bitmap.createBitmap(SIZE_PX, SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bg }
        canvas.drawCircle(SIZE_PX / 2f, SIZE_PX / 2f, SIZE_PX / 2f, fill)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = SIZE_PX * 0.44f
        }
        val y = SIZE_PX / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(letter, SIZE_PX / 2f, y, textPaint)
        return out
    }
}
