package com.saintnico.verdlyhabits.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object SettingsLegal {
    const val PRIVACY_URL = "https://saintnico.site/privacy/"
    const val TERMS_URL = "https://saintnico.site/terms/"
    const val DELETE_ACCOUNT_URL = "https://saintnico.site/delete-account/"

    fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openPlayStoreListing(context: Context) {
        val pkg = context.packageName
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        } catch (_: ActivityNotFoundException) {
            openUrl(context, "https://play.google.com/store/apps/details?id=$pkg")
        }
    }
}
