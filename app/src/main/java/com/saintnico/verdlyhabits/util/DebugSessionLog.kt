package com.saintnico.verdlyhabits.util

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** Debug-mode logger for session 789ebb — posts NDJSON to host ingest (use adb reverse on device). */
object DebugSessionLog {
    private const val TAG = "DEBUG_789ebb"
    private const val SESSION = "789ebb"
    private const val INGEST = "http://127.0.0.1:7927/ingest/284b2c1d-3138-404e-afdc-5e4e3502171a"

    fun log(location: String, message: String, hypothesisId: String, data: Map<String, Any?> = emptyMap()) {
        val line = JSONObject().apply {
            put("sessionId", SESSION)
            put("timestamp", System.currentTimeMillis())
            put("location", location)
            put("message", message)
            put("hypothesisId", hypothesisId)
            put("data", JSONObject(data))
        }.toString()
        Log.i(TAG, line)
        postToIngest(line, "http://127.0.0.1:7927/ingest/284b2c1d-3138-404e-afdc-5e4e3502171a")
        postToIngest(line, "http://10.0.2.2:7927/ingest/284b2c1d-3138-404e-afdc-5e4e3502171a")
    }

    private fun postToIngest(line: String, url: String) {
        thread(name = "debug-ingest") {
            runCatching {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("X-Debug-Session-Id", SESSION)
                    doOutput = true
                    connectTimeout = 2000
                    readTimeout = 2000
                }
                conn.outputStream.use { it.write(line.toByteArray()) }
                conn.inputStream.close()
                conn.disconnect()
            }
        }
    }

    fun logCrash(throwable: Throwable, location: String = "uncaught") {
        log(
            location = location,
            message = throwable.javaClass.simpleName + ": " + (throwable.message ?: "no message"),
            hypothesisId = "CRASH",
            data = mapOf("stack" to throwable.stackTraceToString().take(4000)),
        )
    }
}
