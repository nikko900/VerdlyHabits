package com.saintnico.verdlyhabits.ui.screens.stats

import com.saintnico.verdlyhabits.engine.StatsInsightEngine

sealed class StatsExplore {
    data object Consistency : StatsExplore()
    data object Week : StatsExplore()
    data object Trend : StatsExplore()
    data object Rhythm : StatsExplore()
    data class Metric(val key: StatsInsightEngine.MetricKey) : StatsExplore()
    data class Habit(val habitId: String) : StatsExplore()
    data class Signal(val title: String) : StatsExplore()
}
