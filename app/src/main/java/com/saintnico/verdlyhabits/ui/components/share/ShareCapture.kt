package com.saintnico.verdlyhabits.ui.components.share

import android.graphics.Bitmap
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer

/**
 * Holds a Compose [GraphicsLayer] so a real on-screen (or off-screen) composable
 * can be snapshotted as a bitmap — the same pixels the user already sees.
 */
class ShareCaptureHandle {
    internal var layer: GraphicsLayer? = null

    suspend fun snapshot(): Bitmap? {
        val graphics = layer ?: return null
        return runCatching {
            val image = graphics.toImageBitmap()
            val android = image.asAndroidBitmap()
            if (android.width <= 1 || android.height <= 1) null
            else android.copy(Bitmap.Config.ARGB_8888, false)
        }.getOrNull()
    }
}

@androidx.compose.runtime.Composable
fun rememberShareCaptureHandle(): ShareCaptureHandle = remember { ShareCaptureHandle() }

fun Modifier.shareCapture(handle: ShareCaptureHandle): Modifier = composed {
    val layer = rememberGraphicsLayer()
    DisposableEffect(layer) {
        handle.layer = layer
        onDispose {
            if (handle.layer === layer) handle.layer = null
        }
    }
    this.drawWithContent {
        layer.record {
            this@drawWithContent.drawContent()
        }
        drawLayer(layer)
    }
}
