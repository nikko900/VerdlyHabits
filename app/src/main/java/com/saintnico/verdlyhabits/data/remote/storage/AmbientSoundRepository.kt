package com.saintnico.verdlyhabits.data.remote.storage

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-demand ambient audio: downloads once from Firebase Storage, then plays from local cache.
 * Keeps ~150MB of MP3s out of the APK install size.
 *
 * Upload path (one-time, from dev machine): `ambient_sounds/{rawName}.mp3`
 * e.g. `ambient_sounds/ambient_rain.mp3`
 */
class AmbientSoundRepository(context: Context) {

    private val storage = FirebaseStorage.getInstance()
    private val cacheDir = File(context.filesDir, "ambient").also { it.mkdirs() }

    fun cachedFile(rawName: String): File = File(cacheDir, "$rawName.mp3")

    fun isCached(rawName: String): Boolean {
        val f = cachedFile(rawName)
        return f.exists() && f.length() > 1_024L
    }

    fun cachedSizeBytes(): Long = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L

    suspend fun ensureCached(rawName: String): File? = withContext(Dispatchers.IO) {
        val local = cachedFile(rawName)
        if (local.exists() && local.length() > 1_024L) return@withContext local
        try {
            storage.reference.child("ambient_sounds/$rawName.mp3").getFile(local).await()
            if (local.length() > 1_024L) local else null
        } catch (_: Exception) {
            if (local.exists()) local.delete()
            null
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
}
