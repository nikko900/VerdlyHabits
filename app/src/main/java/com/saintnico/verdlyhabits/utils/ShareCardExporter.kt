package com.saintnico.verdlyhabits.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.saintnico.verdlyhabits.engine.share.ShareCardSpec
import java.io.File
import java.io.FileOutputStream

object ShareCardExporter {

    fun generateBitmap(context: Context, spec: ShareCardSpec): Bitmap =
        ShareCardRenderer.render(context, spec)

    fun exportAndShare(
        context: Context,
        spec: ShareCardSpec,
        chooserTitle: String = "Share your win",
    ): Boolean {
        return try {
            val bitmap = generateBitmap(context, spec)
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "share_${spec.cardType.name.lowercase()}_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            val shareUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_TEXT, spec.caption)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
            true
        } catch (_: Exception) {
            false
        }
    }
}
