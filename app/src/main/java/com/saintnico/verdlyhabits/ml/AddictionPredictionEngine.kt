package com.saintnico.verdlyhabits.ml

import android.content.Context
import android.util.Log
import com.google.android.gms.tflite.client.TfLiteInitializationOptions
import com.google.android.gms.tflite.java.TfLite
import org.tensorflow.lite.InterpreterApi
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class AddictionPredictionEngine(private val context: Context) {
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
                    interpreter = InterpreterApi.create(loadModelFile("addiction_prediction_model.tflite"), InterpreterApi.Options())
                } catch (e: Exception) {
                    Log.e("AddictionPredictionEngine", "Error loading model (ensure it exists in assets)", e)
                }
            }
            .addOnFailureListener { e ->
                Log.e("AddictionPredictionEngine", "TfLite init failed", e)
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

    fun predictAddictionScore(screenTimeHours: Float): Float {
        if (!isInitialized || interpreter == null) return 0.5f

        val inputArray = floatArrayOf(screenTimeHours)
        val outputArray = arrayOf(floatArrayOf(0f))

        try {
            interpreter?.run(inputArray, outputArray)
            return outputArray[0][0]
        } catch (e: Exception) {
            Log.e("AddictionPredictionEngine", "Error running inference", e)
        }

        return 0.5f
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
