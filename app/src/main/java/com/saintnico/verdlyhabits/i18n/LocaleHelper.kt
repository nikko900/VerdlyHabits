package com.saintnico.verdlyhabits.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Tiny locale store so language applies on the next process start without extra libraries.
 * SharedPreferences is read in [attachBaseContext]; DataStore stays the Settings source of truth.
 */
object LocaleHelper {
    private const val PREFS = "verdly_locale"
    private const val KEY = "code"
    const val DEFAULT = "en"
    val SUPPORTED = listOf("en", "es", "de", "sw")

    fun currentCode(context: Context): String {
        val store = context.applicationContext ?: context
        val raw = store.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, DEFAULT)
            ?: DEFAULT
        return if (raw in SUPPORTED) raw else DEFAULT
    }

    fun persist(context: Context, code: String) {
        val safe = if (code in SUPPORTED) code else DEFAULT
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, safe)
            .apply()
    }

    fun wrap(base: Context): Context {
        val locale = Locale.forLanguageTag(currentCode(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
