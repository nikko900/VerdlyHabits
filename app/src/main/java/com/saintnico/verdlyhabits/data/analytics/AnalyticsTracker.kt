package com.saintnico.verdlyhabits.data.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Thin Firebase Analytics wrapper — all product events go through here.
 */
object AnalyticsTracker {

    fun log(context: Context, event: String, params: Map<String, Any?> = emptyMap()) {
        runCatching {
            val bundle = Bundle()
            params.forEach { (key, value) ->
                when (value) {
                    null -> Unit
                    is String -> bundle.putString(key, value)
                    is Int -> bundle.putInt(key, value)
                    is Long -> bundle.putLong(key, value)
                    is Double -> bundle.putDouble(key, value)
                    is Boolean -> bundle.putBoolean(key, value)
                    else -> bundle.putString(key, value.toString())
                }
            }
            FirebaseAnalytics.getInstance(context.applicationContext).logEvent(event, bundle)
        }
    }

    object ReEngagement {
        const val SHOWN = "reengagement_shown"
        const val COMPLETED = "reengagement_completed"
        const val DISMISSED = "reengagement_dismissed"
    }
}
