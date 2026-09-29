package com.saintnico.verdlyhabits.widget

import android.content.Intent

data class WidgetLaunchRequest(
    val route: String,
    val challengeId: String? = null,
)

fun Intent?.readWidgetLaunch(): WidgetLaunchRequest? {
    val route = this?.getStringExtra(WidgetNavigation.EXTRA_ROUTE) ?: return null
    return WidgetLaunchRequest(
        route = route,
        challengeId = getStringExtra(WidgetNavigation.EXTRA_CHALLENGE_ID),
    )
}
