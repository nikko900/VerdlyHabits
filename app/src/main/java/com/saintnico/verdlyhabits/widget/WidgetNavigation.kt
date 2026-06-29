package com.saintnico.verdlyhabits.widget

import android.content.Context
import android.content.Intent
import com.saintnico.verdlyhabits.MainActivity

object WidgetNavigation {
    const val EXTRA_ROUTE = "verdly_widget_route"
    const val EXTRA_CHALLENGE_ID = "verdly_widget_challenge_id"

    const val ROUTE_HOME = "home"
    const val ROUTE_CHALLENGES = "challenges"
    const val ROUTE_DUO = "duo"

    fun openAppIntent(context: Context, route: String, challengeId: String? = null): Intent =
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ROUTE, route)
            challengeId?.let { putExtra(EXTRA_CHALLENGE_ID, it) }
        }
}
