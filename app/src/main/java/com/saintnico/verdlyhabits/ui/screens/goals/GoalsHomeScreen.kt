package com.saintnico.verdlyhabits.ui.screens.goals

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.saintnico.verdlyhabits.ui.viewmodel.GoalsViewModel
import java.util.concurrent.TimeUnit
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.saintnico.verdlyhabits.notifications.GoalReminderScheduler
import com.saintnico.verdlyhabits.preferences.ThemePreference
import kotlinx.coroutines.launch

@Composable
fun GoalsHomeScreen(
    viewModel: GoalsViewModel,
    habits: List<com.saintnico.verdlyhabits.ui.screens.home.HabitItem>,
    onNavigateToAddGoal: () -> Unit,
    onNavigateToGoalDetail: (String) -> Unit
) {
    val activeGoals by viewModel.activeGoals.collectAsState()
    val context = LocalContext.current
    val themePref = remember { ThemePreference(context) }
    val scope = rememberCoroutineScope()
    val dailyOn by themePref.goalDailyReminderEnabled.collectAsState(initial = true)
    val weeklyOn by themePref.goalWeeklyCheckInEnabled.collectAsState(initial = true)
    val dailyTime by themePref.goalDailyReminderTime.collectAsState(initial = "20:30")

    if (activeGoals.isEmpty()) {
        GoalsEmptyState(onNavigateToAddGoal)
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                GoalRemindersCard(
                    dailyEnabled = dailyOn,
                    weeklyEnabled = weeklyOn,
                    dailyTime = dailyTime,
                    onDailyToggle = { enabled ->
                        scope.launch {
                            themePref.setGoalDailyReminderEnabled(enabled)
                            GoalReminderScheduler.scheduleAll(context)
                        }
                    },
                    onWeeklyToggle = { enabled ->
                        scope.launch {
                            themePref.setGoalWeeklyCheckInEnabled(enabled)
                            GoalReminderScheduler.scheduleAll(context)
                        }
                    },
                    onDailyTimeChange = { time ->
                        scope.launch {
                            themePref.setGoalDailyReminderTime(time)
                            GoalReminderScheduler.scheduleAll(context)
                        }
                    },
                )
            }
            item {
                Text(
                    "${activeGoals.size} goals in motion.",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            items(activeGoals) { goal ->
                GoalCard(
                    goal = goal,
                    progress = viewModel.progressFor(goal, habits),
                    onClick = onNavigateToGoalDetail,
                )
            }

            item {
                TextButton(
                    onClick = onNavigateToAddGoal,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Goal", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalCard(goal: GoalEntity, progress: Float, onClick: (String) -> Unit) {
    val color = Color(android.graphics.Color.parseColor(goal.colorHex))

    val daysLeft = TimeUnit.MILLISECONDS.toDays(goal.targetDate - System.currentTimeMillis()).coerceAtLeast(0)
    val daysLeftColor = when {
        daysLeft < 7 -> Color(0xFFF44336)
        daysLeft < 30 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        onClick = { onClick(goal.id) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
        ) {
            // Left border accent
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(color))
            
            Column(modifier = Modifier.padding(20.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.TrackChanges, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        goal.title,
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.background(daysLeftColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "$daysLeft days left",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = daysLeftColor
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Energy indicator could go here
                }
            }
            
            // Right side progress
            Box(
                modifier = Modifier.padding(20.dp).align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(64.dp)) {
                    drawArc(
                        color = color.copy(alpha = 0.2f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = color,
                        startAngle = 135f,
                        sweepAngle = 270f * progress,
                        useCenter = false,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Text(
                    "${(progress * 100).toInt()}%",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun GoalRemindersCard(
    dailyEnabled: Boolean,
    weeklyEnabled: Boolean,
    dailyTime: String,
    onDailyToggle: (Boolean) -> Unit,
    onWeeklyToggle: (Boolean) -> Unit,
    onDailyTimeChange: (String) -> Unit,
) {
    val times = listOf("08:00", "12:00", "18:00", "20:30")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Goal reminders",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
            Text(
                "Exact alarms with vibration — weekly check-in (Sun 6pm) and daily motivation.",
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Weekly check-in", fontFamily = dmSansFamily, fontSize = 14.sp)
                Switch(checked = weeklyEnabled, onCheckedChange = onWeeklyToggle)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Daily motivation", fontFamily = dmSansFamily, fontSize = 14.sp)
                Switch(checked = dailyEnabled, onCheckedChange = onDailyToggle)
            }
            if (dailyEnabled) {
                Text("Daily time", fontFamily = dmSansFamily, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(0.5f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    times.forEach { t ->
                        val selected = t == dailyTime
                        FilterChip(
                            selected = selected,
                            onClick = { onDailyTimeChange(t) },
                            label = { Text(t, fontSize = 12.sp) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoalsEmptyState(onNavigateToAddGoal: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mocking the custom vector illustration
        Box(
            modifier = Modifier.size(120.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "What are you working toward?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Set your first goal and Verdly will help you build the habits to get there.",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Button(
            onClick = onNavigateToAddGoal,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Set My First Goal", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}
