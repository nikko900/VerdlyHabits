package com.saintnico.verdlyhabits.data.remote.storage

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Uploads a habit proof photo to Firebase Storage and returns the download URL.
     * Path: proofs/{userId}/{habitId}/{timestamp}.jpg
     * Includes compression logic to save storage space.
     */
    suspend fun uploadProofPhoto(
        context: android.content.Context, 
        habitId: String, 
        fileUri: Uri,
        challengeId: String? = null
    ): String? = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext null
        val fileName = "${System.currentTimeMillis()}.jpg"
        
        val folder = if (challengeId != null) "challenges/$challengeId" else "habits"
        val ref = storage.reference.child("proofs").child(userId).child(folder).child(habitId).child(fileName)

        return@withContext try {
            // 1. Load and Compress the image
            val inputStream = context.contentResolver.openInputStream(fileUri)
            val originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return@withContext null

            // Scale down if too large (max 1024px width/height)
            val scaledBitmap = scaleBitmapIfNeeded(originalBitmap, 1024)
            
            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos) // 70% quality is perfect for mobile
            val data = baos.toByteArray()

            // 2. Upload the compressed bytes
            ref.putBytes(data).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            println("FIREBASE_STORAGE_ERROR: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: android.graphics.Bitmap, maxDimension: Int): android.graphics.Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) return bitmap
        
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        
        return android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
