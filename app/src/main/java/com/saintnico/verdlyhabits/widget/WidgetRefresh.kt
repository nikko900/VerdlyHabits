package com.saintnico.verdlyhabits.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

object WidgetRefresh {
    suspend fun updateAll(context: Context) {
        HabitsGlanceWidget().updateAll(context)
        LeaderboardGlanceWidget().updateAll(context)
    }

    suspend fun updateHabitsOnly(context: Context) {
        HabitsGlanceWidget().updateAll(context)
    }

    suspend fun updateLeaderboardOnly(context: Context) {
        LeaderboardGlanceWidget().updateAll(context)
    }

    suspend fun hasAnyPlacedWidgets(context: Context): Boolean {
        val manager = GlanceAppWidgetManager(context)
        return manager.getGlanceIds(HabitsGlanceWidget::class.java).isNotEmpty() ||
            manager.getGlanceIds(LeaderboardGlanceWidget::class.java).isNotEmpty()
    }
}
