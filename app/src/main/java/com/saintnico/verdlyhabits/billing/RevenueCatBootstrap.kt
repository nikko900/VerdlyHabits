package com.saintnico.verdlyhabits.billing

import android.app.Application
import android.util.Log
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.saintnico.verdlyhabits.BuildConfig

/**
 * One-time RevenueCat SDK setup. Requires `REVENUECAT_API_KEY` in `local.properties`
 * (Public Google Play API key from RevenueCat dashboard → Project → API keys).
 */
object RevenueCatBootstrap {

    private const val TAG = "VerdlyRevenueCat"

    fun configure(application: Application) {
        val apiKey = BuildConfig.REVENUECAT_API_KEY.trim()
        if (apiKey.isBlank()) {
            Log.w(TAG, "REVENUECAT_API_KEY missing. add it to local.properties for Shipaton billing")
            return
        }
        if (Purchases.isConfigured) return
        if (BuildConfig.DEBUG) {
            Purchases.logLevel = LogLevel.DEBUG
        }
        Purchases.configure(
            PurchasesConfiguration.Builder(application, apiKey).build()
        )
        Log.i(TAG, "RevenueCat configured")
    }

    fun isConfigured(): Boolean = Purchases.isConfigured
}
