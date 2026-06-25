package com.saintnico.verdlyhabits.ui.screens.goals

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Goal(
    val id: String,
    val title: String,
    val category: GoalCategory,
    val linkedHabitIds: List<String>,
    val targetDays: Int,             // e.g. "Meditate for 30 days" = 30
    val createdAt: String,           // ISO date
)

enum class GoalCategory(val label: String, val icon: ImageVector, val color: Color) {
    Health("Health",        Icons.Filled.FitnessCenter,   Color(0xFF4CAF50)),
    Mind("Mind",            Icons.Filled.SelfImprovement,  Color(0xFF7B61FF)),
    Career("Career",        Icons.Filled.Work,             Color(0xFF2196F3)),
    Finance("Finance",      Icons.Filled.AttachMoney,      Color(0xFFFFB300)),
    Relationships("Social", Icons.Filled.People,           Color(0xFFE91E63)),
    Creativity("Create",    Icons.Filled.Brush,            Color(0xFFFF5722)),
}

/**
 * Calculate real goal progress from linked habit completion data.
 * Progress = total unique completion days across linked habits / targetDays.
 */
private fun calculateGoalProgress(goal: Goal, habits: List<HabitItem>): Float {
    if (goal.linkedHabitIds.isEmpty() || goal.targetDays <= 0) return 0f
    val linked = habits.filter { it.id in goal.linkedHabitIds }
    if (linked.isEmpty()) return 0f

    // Count days since goal was created where ALL linked habits were completed
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val startDate = try { LocalDate.parse(goal.createdAt, fmt) } catch (_: Exception) { LocalDate.now() }
    val today = LocalDate.now()
    var completedDays = 0
    var checkDate = startDate
    while (!checkDate.isAfter(today)) {
        val dateStr = checkDate.format(fmt)
        val allDone = linked.all { it.completedDates.contains(dateStr) }
        if (allDone) completedDays++
        checkDate = checkDate.plusDays(1)
    }
    return (completedDays.toFloat() / goal.targetDays).coerceIn(0f, 1f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    habits: List<HabitItem>,
    onBack: () -> Unit
) {
    var goals by remember { mutableStateOf(listOf<Goal>()) }
    var selectedCategory by remember { mutableStateOf<GoalCategory?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val filteredGoals = if (selectedCategory == null) goals
    else goals.filter { it.category == selectedCategory }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Goals", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { showAddSheet = true }) {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Track long-term progress by linking habits to goals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Text("Progress updates automatically as you complete linked habits each day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f))
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") }
                        )
                    }
                    items(GoalCategory.values().toList()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            leadingIcon = { Icon(cat.icon, null, modifier = Modifier.size(14.dp)) },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color.copy(alpha = 0.15f),
                                selectedLabelColor = cat.color,
                                selectedLeadingIconColor = cat.color
                            )
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (filteredGoals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Flag, null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f))
                            Spacer(Modifier.height(12.dp))
                            Text("No goals yet.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                            Spacer(Modifier.height(4.dp))
                            Text("Tap + to set a goal, link your habits, and watch progress grow.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f))
                        }
                    }
                }
            }

            items(filteredGoals, key = { it.id }) { goal ->
                GoalCard(goal, habits, onDelete = { goals = goals.filter { it.id != goal.id } })
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showAddSheet) {
        AddGoalSheet(
            habits = habits,
            onAdd = { goal ->
                goals = goals + goal
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
private fun GoalCard(goal: Goal, habits: List<HabitItem>, onDelete: () -> Unit) {
    val progress = calculateGoalProgress(goal, habits)
    val animProgress by animateFloatAsState(progress, tween(800), label = "goal_prog_${goal.id}")
    val linkedHabits = habits.filter { it.id in goal.linkedHabitIds }
    val pct = (animProgress * 100).toInt()
    val isComplete = pct >= 100

    // Count days completed
    val fmt = DateTimeFormatter.ISO_LOCAL_DATE
    val startDate = try { LocalDate.parse(goal.createdAt, fmt) } catch (_: Exception) { LocalDate.now() }
    val today = LocalDate.now()
    var completedDays = 0
    var checkDate = startDate
    while (!checkDate.isAfter(today)) {
        val dateStr = checkDate.format(fmt)
        if (linkedHabits.all { it.completedDates.contains(dateStr) }) completedDays++
        checkDate = checkDate.plusDays(1)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isComplete) goal.category.color.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isComplete) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Progress ring
                Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(goal.category.color.copy(alpha = 0.12f), -90f, 360f, false, style = Stroke(4.dp.toPx()))
                        drawArc(goal.category.color, -90f, 360f * animProgress, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                    }
                    if (isComplete) {
                        Icon(Icons.Default.Check, null, tint = goal.category.color, modifier = Modifier.size(20.dp))
                    } else {
                        Text("$pct%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = goal.category.color)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(goal.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(goal.category.icon, null, tint = goal.category.color, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(goal.category.label, fontSize = 11.sp, color = goal.category.color)
                        Spacer(Modifier.width(8.dp))
                        Text("$completedDays / ${goal.targetDays} days", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f))
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                        modifier = Modifier.size(16.dp))
                }
            }

            // Linked habits preview
            if (linkedHabits.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    linkedHabits.take(5).forEach { habit ->
                        val todayDone = habit.completedDates.contains(LocalDate.now().format(fmt))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (todayDone) goal.category.color.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(habit.icon, null, tint = Color(habit.color.toInt()), modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(habit.title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            if (todayDone) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Check, null, tint = goal.category.color, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }
            }

            if (isComplete) {
                Spacer(Modifier.height(8.dp))
                Text("Goal achieved! Keep the momentum going.",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = goal.category.color)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalSheet(
    habits: List<HabitItem>,
    onAdd: (Goal) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(GoalCategory.Health) }
    var linkedHabitIds by remember { mutableStateOf(setOf<String>()) }
    var targetDays by remember { mutableStateOf(30) }

    val targetOptions = listOf(7, 14, 21, 30, 60, 90)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
            Text("Set a Goal", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("Link habits — progress counts automatically when all linked habits are done each day.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("What's your goal?") },
                placeholder = { Text("e.g. Build a morning routine") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            Text("Category", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GoalCategory.values().toList()) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        leadingIcon = { Icon(cat.icon, null, modifier = Modifier.size(14.dp)) },
                        label = { Text(cat.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cat.color.copy(alpha = 0.15f),
                            selectedLabelColor = cat.color,
                            selectedLeadingIconColor = cat.color
                        )
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Target Duration", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            Text("How many days of consistency to hit 100%?",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                targetOptions.forEach { days ->
                    val sel = days == targetDays
                    Surface(
                        modifier = Modifier.clickable { targetDays = days }.clip(RoundedCornerShape(10.dp)),
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("${days}d",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = if (sel) Color.White else MaterialTheme.colorScheme.onBackground,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp)
                    }
                }
            }

            if (habits.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("Link Habits", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Text("Progress counts days when ALL linked habits are completed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))
                habits.filter { !it.isArchived }.forEach { habit ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            linkedHabitIds = if (habit.id in linkedHabitIds) linkedHabitIds - habit.id
                            else linkedHabitIds + habit.id
                        }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = habit.id in linkedHabitIds, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Icon(habit.icon, null, tint = Color(habit.color.toInt()), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(habit.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (title.isBlank() || linkedHabitIds.isEmpty()) return@Button
                    onAdd(Goal(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        category = selectedCategory,
                        linkedHabitIds = linkedHabitIds.toList(),
                        targetDays = targetDays,
                        createdAt = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = title.isNotBlank() && linkedHabitIds.isNotEmpty()
            ) { Text("Set Goal", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(16.dp))
        }
    }
}
