package com.saintnico.verdlyhabits.ml

import android.content.Context
import android.util.Log
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.InterpreterApi
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SmartNudgeEngine(private val context: Context) {
    private var interpreter: InterpreterApi? = null
    private var isInitialized = false

    init {
        val options = TfLiteInitializationOptions.builder()
            .setEnableGpuDelegateSupport(true)
            .build()

        TfLite.initialize(context, options)
            .addOnSuccessListener {
                isInitialized = true
                try {
                    interpreter = InterpreterApi.create(loadModelFile("smart_nudge_model.tflite"), InterpreterApi.Options())
                } catch (e: Exception) {
                    Log.e("SmartNudgeEngine", "Error loading model (ensure it exists in assets)", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("SmartNudgeEngine", "TfLite init failed", e)
            }
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predictEngagementProbability(
        timeOfDay: Float,
        dayOfWeek: Float,
        activityState: Float,
        timeSinceLastCompletion: Float
    ): Float {
        if (!isInitialized || interpreter == null) return 0.5f

        val inputArray = floatArrayOf(timeOfDay, dayOfWeek, activityState, timeSinceLastCompletion)
        val outputArray = arrayOf(floatArrayOf(0f))

        try {
            interpreter?.run(inputArray, outputArray)
            return outputArray[0][0]
        } catch (e: Exception) {
            Log.e("SmartNudgeEngine", "Error running inference", e)
        }

        return 0.5f
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
