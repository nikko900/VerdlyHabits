package com.saintnico.verdlyhabits.ui.screens.goals.creation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun GoalCreationScreen(
    habits: List<HabitItem>,
    onBack: () -> Unit,
    onNavigateToAddHabit: () -> Unit = {},
    onSaveGoal: (GoalCreationState) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val state = remember { GoalCreationState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Goal", fontFamily = dmSansFamily, fontWeight = FontWeight.Medium) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1) currentStep-- else onBack()
                    }) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (currentStep > 1) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (currentStep < 6) {
                                currentStep++
                            } else {
                                onSaveGoal(state)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            if (currentStep == 6) "Create Goal" else "Continue",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn(animationSpec = tween(400))) togetherWith
                            slideOutHorizontally(animationSpec = tween(400)) { width -> -width } + fadeOut(animationSpec = tween(400))
                } else {
                    (slideInHorizontally(animationSpec = tween(400)) { width -> -width } + fadeIn(animationSpec = tween(400))) togetherWith
                            slideOutHorizontally(animationSpec = tween(400)) { width -> width } + fadeOut(animationSpec = tween(400))
                }
            },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) { step ->
            when (step) {
                1 -> Step1TypeSelection(state) {
                    state.goalType = it
                    currentStep++
                }
                2 -> Step2Title(state)
                3 -> Step3Why(state)
                4 -> Step4Timeline(state)
                5 -> Step5Habits(state, habits, onNavigateToAddHabit)
                6 -> Step6ColorIcon(state)
            }
        }
    }
}

class GoalCreationState {
    var goalType by mutableStateOf<GoalType?>(null)
    var title by mutableStateOf("")
    var whyStatement by mutableStateOf("")
    var startDate by mutableStateOf(System.currentTimeMillis())
    var targetDate by mutableStateOf(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000) // +30 days
    var targetValue by mutableStateOf("")
    var unit by mutableStateOf("")
    var linkedHabitIds = mutableStateListOf<String>()
    var colorHex by mutableStateOf("#4CAF50")
    var iconName by mutableStateOf("Rounded.TrackChanges")
}

@Composable
fun Step1TypeSelection(state: GoalCreationState, onSelect: (GoalType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "What kind of goal are you setting?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        TypeCard(
            title = "Build",
            description = "Start a new positive habit or routine",
            icon = Icons.Rounded.Eco,
            color = Color(0xFF4CAF50),
            onClick = { onSelect(GoalType.BUILD) }
        )
        TypeCard(
            title = "Reach",
            description = "Hit a specific target or milestone",
            icon = Icons.Rounded.TrackChanges,
            color = Color(0xFF2196F3),
            onClick = { onSelect(GoalType.REACH) }
        )
        TypeCard(
            title = "Quit",
            description = "Break a bad habit or stop a behavior",
            icon = Icons.Rounded.Block,
            color = Color(0xFFF44336),
            onClick = { onSelect(GoalType.QUIT) }
        )
        TypeCard(
            title = "Maintain",
            description = "Keep a good thing going consistently",
            icon = Icons.Rounded.VerifiedUser,
            color = Color(0xFFFF9800),
            onClick = { onSelect(GoalType.MAINTAIN) }
        )
    }
}

@Composable
fun TypeCard(title: String, description: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    title,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    description,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun Step2Title(state: GoalCreationState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        BasicTextField(
            value = state.title,
            onValueChange = { state.title = it },
            textStyle = TextStyle(
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 48.sp
            ),
            decorationBox = { innerTextField ->
                if (state.title.isEmpty()) {
                    Text(
                        "What do you want to achieve?",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 40.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        lineHeight = 48.sp
                    )
                }
                innerTextField()
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Be specific. Not 'get fit' but 'Run 5K'",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun Step3Why(state: GoalCreationState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Why does this matter to you?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 40.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "This stays private. We'll remind you when it gets hard.",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(24.dp)
        ) {
            BasicTextField(
                value = state.whyStatement,
                onValueChange = { state.whyStatement = it },
                textStyle = TextStyle(
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 32.sp
                ),
                decorationBox = { innerTextField ->
                    if (state.whyStatement.isEmpty()) {
                        Text(
                            "I want to feel strong at 30...",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            lineHeight = 32.sp
                        )
                    }
                    innerTextField()
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step4Timeline(state: GoalCreationState) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.targetDate)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { state.targetDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "Timeline & Target",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Card(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Target Date", fontFamily = dmSansFamily, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(state.targetDate))
                    Text(dateStr, fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
            }
        }

        if (state.goalType == GoalType.REACH) {
            Text("What's your target?", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = state.targetValue,
                    onValueChange = { state.targetValue = it },
                    label = { Text("Value (e.g. 50)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
                OutlinedTextField(
                    value = state.unit,
                    onValueChange = { state.unit = it },
                    label = { Text("Unit (e.g. km)") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "We'll create 4 checkpoints for you automatically based on this timeline.",
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun Step5Habits(
    state: GoalCreationState,
    habits: List<HabitItem>,
    onNavigateToAddHabit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "Which habits will move this goal forward?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 40.sp
        )

        Text(
            "Tap the habits that support this goal.",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        if (habits.isEmpty()) {
            Text(
                "You do not have any habits yet. Create one first, then come back to link it.",
                fontFamily = dmSansFamily,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        } else {
            habits.filter { !it.isArchived }.forEach { habit ->
                val isSelected = state.linkedHabitIds.contains(habit.id)
                Card(
                    onClick = {
                        if (isSelected) state.linkedHabitIds.remove(habit.id)
                        else state.linkedHabitIds.add(habit.id)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            habit.icon,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            habit.title,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSelected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onNavigateToAddHabit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create a new habit", fontFamily = dmSansFamily, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun Step6ColorIcon(state: GoalCreationState) {
    val colors = listOf(
        "#4CAF50", "#2196F3", "#9C27B0", "#F44336",
        "#FF9800", "#00BCD4", "#E91E63", "#607D8B"
    )
    val icons = listOf(
        Icons.Rounded.TrackChanges, Icons.Rounded.Star, Icons.Rounded.Favorite, Icons.Rounded.FitnessCenter,
        Icons.Rounded.LocalLibrary, Icons.Rounded.AttachMoney, Icons.Rounded.FlightTakeoff, Icons.Rounded.EmojiEvents
    )
    // Map icon name to ImageVector for preview
    val currentIcon = icons.firstOrNull { it.name == state.iconName } ?: Icons.Rounded.TrackChanges

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Text(
            "Make it yours",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Live Preview Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(android.graphics.Color.parseColor(state.colorHex)).copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(state.colorHex)).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(currentIcon, contentDescription = null, tint = Color(android.graphics.Color.parseColor(state.colorHex)), modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        state.title.ifEmpty { "Your Goal Title" },
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        state.whyStatement.ifEmpty { "Your why statement..." },
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Accent Color", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colors.take(4).forEach { hex ->
                    ColorCircle(hex, state.colorHex == hex) { state.colorHex = hex }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colors.drop(4).forEach { hex ->
                    ColorCircle(hex, state.colorHex == hex) { state.colorHex = hex }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Icon", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                icons.take(4).forEach { icon ->
                    IconCircle(icon, state.iconName == icon.name, state.colorHex) { state.iconName = icon.name }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                icons.drop(4).forEach { icon ->
                    IconCircle(icon, state.iconName == icon.name, state.colorHex) { state.iconName = icon.name }
                }
            }
        }
    }
}

@Composable
fun ColorCircle(hex: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = Color(android.graphics.Color.parseColor(hex))
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 4.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}

@Composable
fun IconCircle(icon: ImageVector, isSelected: Boolean, colorHex: String, onClick: () -> Unit) {
    val color = Color(android.graphics.Color.parseColor(colorHex))
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) color else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}
