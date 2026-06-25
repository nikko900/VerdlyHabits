package com.saintnico.verdlyhabits.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import com.saintnico.verdlyhabits.R

class HabitsGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore(context).readHabits()
        provideContent {
            GlanceTheme {
                HabitsWidgetContent(context, snapshot)
            }
        }
    }
}

class HabitsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitsGlanceWidget()
}

@androidx.compose.runtime.Composable
private fun HabitsWidgetContent(context: Context, snapshot: HabitsWidgetSnapshot) {
    val openApp = WidgetNavigation.openAppIntent(context, WidgetNavigation.ROUTE_HOME)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(VerdlyWidgetTheme.color(VerdlyWidgetTheme.Bg))
            .cornerRadius(20.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(openApp)),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Text(
                    text = context.getString(R.string.widget_habits_title),
                    style = TextStyle(
                        color = VerdlyWidgetTheme.color(VerdlyWidgetTheme.TextPrimary),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Text(
                    text = if (snapshot.totalToday == 0) {
                        context.getString(R.string.widget_habits_subtitle_empty)
                    } else {
                        context.getString(
                            R.string.widget_habits_subtitle_progress,
                            snapshot.doneToday,
                            snapshot.totalToday,
                        )
                    },
                    style = TextStyle(
                        color = VerdlyWidgetTheme.color(VerdlyWidgetTheme.TextMuted),
                        fontSize = 11.sp,
                    ),
                )
            }
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .cornerRadius(12.dp)
                    .background(VerdlyWidgetTheme.color(VerdlyWidgetTheme.PrimarySoft)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🌿",
                    style = TextStyle(fontSize = 16.sp),
                )
            }
        }

        Spacer(GlanceModifier.height(10.dp))

        if (snapshot.habits.isEmpty()) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .cornerRadius(14.dp)
                    .background(VerdlyWidgetTheme.color(VerdlyWidgetTheme.Surface))
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = context.getString(R.string.widget_habits_empty),
                    style = TextStyle(
                        color = VerdlyWidgetTheme.color(VerdlyWidgetTheme.TextMuted),
                        fontSize = 12.sp,
                    ),
                )
            }
        } else {
            snapshot.habits.take(4).forEachIndexed { index, habit ->
                if (index > 0) Spacer(GlanceModifier.height(6.dp))
                HabitWidgetRowView(habit)
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun HabitWidgetRowView(habit: HabitWidgetRow) {
    val accentColor = Color(habit.colorArgb)
    val accent = ColorProvider(accentColor)
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(14.dp)
            .background(VerdlyWidgetTheme.color(VerdlyWidgetTheme.Surface))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(8.dp)
                .cornerRadius(4.dp)
                .background(accent),
        ) {}
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = habit.title,
                style = TextStyle(
                    color = VerdlyWidgetTheme.color(VerdlyWidgetTheme.TextPrimary),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            if (habit.isPaused) {
                Text(
                    text = "Paused",
                    style = TextStyle(
                        color = VerdlyWidgetTheme.color(VerdlyWidgetTheme.TextMuted),
                        fontSize = 10.sp,
                    ),
                )
            }
        }
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                text = "🔥${habit.streak}",
                style = TextStyle(
                    color = VerdlyWidgetTheme.color(VerdlyWidgetTheme.Gold),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(GlanceModifier.width(8.dp))
            Box(
                modifier = GlanceModifier
                    .size(22.dp)
                    .cornerRadius(11.dp)
                    .background(
                        if (habit.isCompletedToday) {
                            VerdlyWidgetTheme.color(VerdlyWidgetTheme.Done)
                        } else {
                            VerdlyWidgetTheme.color(VerdlyWidgetTheme.SurfaceElevated)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (habit.isCompletedToday) "✓" else "",
                    style = TextStyle(
                        color = VerdlyWidgetTheme.color(VerdlyWidgetTheme.TextPrimary),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
