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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.goals.GoalCreationDraft
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.local.goals.PaceProfile
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun GoalCreationScreen(
    habits: List<HabitItem>,
    savedDraft: GoalCreationDraft? = null,
    onBack: () -> Unit,
    onNavigateToAddHabit: () -> Unit = {},
    onPersistDraft: (GoalCreationState, Int, Boolean) -> Unit = { _, _, _ -> },
    onClearDraft: () -> Unit = {},
    onSaveGoal: (GoalCreationState) -> Unit,
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val state = remember { GoalCreationState() }
    var hydrated by remember { mutableStateOf(false) }

    LaunchedEffect(savedDraft) {
        if (!hydrated) {
            savedDraft?.let { draft ->
                state.applyDraft(draft)
                currentStep = draft.currentStep.coerceIn(1, 6)
            }
            hydrated = true
        }
    }

    LaunchedEffect(
        hydrated,
        currentStep,
        state.goalType,
        state.title,
        state.whyStatement,
        state.targetValue,
        state.linkedHabitIds.size,
        state.triggerText,
        state.replacementText,
        state.tinyVersionText,
        state.floorCount,
    ) {
        if (!hydrated) return@LaunchedEffect
        onPersistDraft(state, currentStep, false)
    }

    val navigateToAddHabit = {
        onPersistDraft(state, currentStep, true)
        onNavigateToAddHabit()
    }

    val handleBack: () -> Unit = {
        if (currentStep > 1) {
            currentStep -= 1
        } else {
            if (state.goalType == null && state.title.isBlank()) {
                onClearDraft()
            } else {
                onPersistDraft(state, currentStep, false)
            }
            onBack()
        }
    }

    val maxStep = if (state.goalType == null) 1 else 6

    val canContinue = when {
        currentStep == 1 -> state.goalType != null
        currentStep == 2 -> state.title.isNotBlank()
        currentStep == 3 -> state.whyStatement.isNotBlank()
        currentStep == 4 -> when (state.goalType) {
            GoalType.REACH -> state.targetValue.toFloatOrNull() != null && state.targetValue.toFloatOrNull()!! > 0f
            GoalType.MAINTAIN -> (state.floorCount.toIntOrNull() ?: 0) > 0
            GoalType.BUILD -> true
            GoalType.QUIT -> true
            null -> false
        }
        currentStep == 5 -> when (state.goalType) {
            GoalType.BUILD, GoalType.MAINTAIN -> state.linkedHabitIds.isNotEmpty()
            else -> true
        }
        else -> true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state.goalType) {
                            GoalType.BUILD -> "Build"
                            GoalType.REACH -> "Reach"
                            GoalType.QUIT -> "Quit"
                            GoalType.MAINTAIN -> "Maintain"
                            null -> "New Goal"
                        },
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            if (currentStep > 1) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (currentStep < maxStep) currentStep++
                            else onSaveGoal(state)
                        },
                        enabled = canContinue,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = if (currentStep == maxStep) "Create Goal" else "Continue",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = tween(400)) { width -> width } + fadeIn(animationSpec = tween(400))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(400)) { width -> -width } + fadeOut(animationSpec = tween(400)))
                } else {
                    (slideInHorizontally(animationSpec = tween(400)) { width -> -width } + fadeIn(animationSpec = tween(400))) togetherWith
                        (slideOutHorizontally(animationSpec = tween(400)) { width -> width } + fadeOut(animationSpec = tween(400)))
                }
            },
        ) { step ->
            when (step) {
                1 -> Step1TypeSelection(state) {
                    state.goalType = it
                    currentStep++
                }
                2 -> Step2Title(state)
                3 -> Step3PrivateLine(state)
                4 -> Step4TypeSpecific(state)
                5 -> Step5Habits(state, habits, navigateToAddHabit)
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
    var targetDate by mutableStateOf(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000)
    var targetValue by mutableStateOf("")
    var unit by mutableStateOf("")
    var linkedHabitIds = mutableStateListOf<String>()
    var colorHex by mutableStateOf("#4CAF50")
    var iconName by mutableStateOf("Rounded.TrackChanges")
    // BUILD
    var tinyVersionText by mutableStateOf("")
    var anchorCue by mutableStateOf("")
    var softReviewEnabled by mutableStateOf(false)
    // REACH
    var paceProfile by mutableStateOf(PaceProfile.STEADY)
    // QUIT
    var triggerText by mutableStateOf("")
    var replacementText by mutableStateOf("")
    var trackCost by mutableStateOf(false)
    var costPerOccurrence by mutableStateOf("")
    var costUnit by mutableStateOf("money")
    // MAINTAIN
    var floorCount by mutableStateOf("3")
    var floorPeriodDays by mutableStateOf("7")
}

private fun GoalCreationState.applyDraft(draft: GoalCreationDraft) {
    goalType = draft.goalType
    title = draft.title
    whyStatement = draft.whyStatement
    startDate = draft.startDate
    targetDate = draft.targetDate
    targetValue = draft.targetValue
    unit = draft.unit
    linkedHabitIds.clear()
    linkedHabitIds.addAll(draft.linkedHabitIds)
    colorHex = draft.colorHex
    iconName = draft.iconName
    tinyVersionText = draft.tinyVersionText
    anchorCue = draft.anchorCue
    softReviewEnabled = draft.softReviewEnabled
    paceProfile = draft.paceProfile
    triggerText = draft.triggerText
    replacementText = draft.replacementText
    trackCost = draft.trackCost
    costPerOccurrence = draft.costPerOccurrence
    costUnit = draft.costUnit
    floorCount = draft.floorCount
    floorPeriodDays = draft.floorPeriodDays
}

@Composable
fun Step1TypeSelection(state: GoalCreationState, onSelect: (GoalType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Which room are you entering?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Each one is a different relationship with time — not a label on the same wizard.",
            modifier = Modifier.padding(bottom = 8.dp),
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )

        TypeCard(
            title = "Build",
            essence = "Turn something I don't yet do into something I no longer have to think about.",
            icon = Icons.Rounded.Eco,
            color = Color(0xFF4CAF50),
            onClick = { onSelect(GoalType.BUILD) },
        )
        TypeCard(
            title = "Reach",
            essence = "Get this specific number to that specific number by that specific date.",
            icon = Icons.Rounded.TrackChanges,
            color = Color(0xFF2196F3),
            onClick = { onSelect(GoalType.REACH) },
        )
        TypeCard(
            title = "Quit",
            essence = "Put more space between me and the thing, one clean moment at a time.",
            icon = Icons.Rounded.Block,
            color = Color(0xFF8D6E63),
            onClick = { onSelect(GoalType.QUIT) },
        )
        TypeCard(
            title = "Maintain",
            essence = "Keep the line I already earned from slipping, forever.",
            icon = Icons.Rounded.LocalFireDepartment,
            color = Color(0xFFFF9800),
            onClick = { onSelect(GoalType.MAINTAIN) },
        )
    }
}

@Composable
fun TypeCard(
    title: String,
    essence: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
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
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    essence,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
fun Step2Title(state: GoalCreationState) {
    val (prompt, hint) = when (state.goalType) {
        GoalType.BUILD -> "What are you building?" to "Not 'get fit' — 'move before coffee'"
        GoalType.REACH -> "What number are you chasing?" to "Be specific. 'Run a 10K' not 'run more'"
        GoalType.QUIT -> "What are you putting distance from?" to "Name the thing plainly"
        GoalType.MAINTAIN -> "What standard are you defending?" to "Something you've already earned"
        null -> "What do you want?" to ""
    }
    PromptField(
        prompt = prompt,
        value = state.title,
        onValueChange = { state.title = it },
        placeholder = hint,
        hint = hint,
    )
}

@Composable
fun Step3PrivateLine(state: GoalCreationState) {
    val (prompt, sub, placeholder) = when (state.goalType) {
        GoalType.BUILD -> Triple(
            "Who are you becoming?",
            "This stays private. We'll resurface it when a day gets hard.",
            "A person who moves their body before checking their phone",
        )
        GoalType.REACH -> Triple(
            "What does hitting this actually get you?",
            "Stakes, not slogans. Private.",
            "Run my first 10K without stopping",
        )
        GoalType.QUIT -> Triple(
            "What is this costing you?",
            "Concrete costs stick. This stays private.",
            "My mornings, my money, my patience",
        )
        GoalType.MAINTAIN -> Triple(
            "What have you earned that you don't want to lose?",
            "You're defending a standard — not chasing a finish line.",
            "The 3x/week gym habit I finally built",
        )
        null -> Triple("Why?", "", "")
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            prompt,
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 40.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            sub,
            fontFamily = dmSansFamily,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(24.dp),
        ) {
            BasicTextField(
                value = state.whyStatement,
                onValueChange = { state.whyStatement = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 32.sp,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (state.whyStatement.isEmpty()) {
                        Text(
                            placeholder,
                            fontFamily = frauncesFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            lineHeight = 32.sp,
                        )
                    }
                    inner()
                },
            )
        }
    }
}

@Composable
fun Step4TypeSpecific(state: GoalCreationState) {
    when (state.goalType) {
        GoalType.BUILD -> Step4Build(state)
        GoalType.REACH -> Step4Reach(state)
        GoalType.QUIT -> Step4Quit(state)
        GoalType.MAINTAIN -> Step4Maintain(state)
        null -> Box {}
    }
}

@Composable
private fun Step4Build(state: GoalCreationState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            "Make it tiny",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "What's the smallest version you'd still do on your worst day?",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )
        OutlinedTextField(
            value = state.tinyVersionText,
            onValueChange = { state.tinyVersionText = it },
            label = { Text("Tiny version") },
            placeholder = { Text("2 push-ups, one page, 1 minute") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        Text(
            "After I ___, I will ___",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        OutlinedTextField(
            value = state.anchorCue,
            onValueChange = { state.anchorCue = it },
            label = { Text("Anchor") },
            placeholder = { Text("After I pour coffee, I will stretch") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Soft check-in in 8 weeks", fontFamily = dmSansFamily, fontWeight = FontWeight.Medium)
                Text(
                    "A review point — not a deadline.",
                    fontFamily = dmSansFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
            }
            Switch(
                checked = state.softReviewEnabled,
                onCheckedChange = { state.softReviewEnabled = it },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step4Reach(state: GoalCreationState) {
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
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            "Timeline & target",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Card(
            onClick = { showDatePicker = true },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Target date", fontFamily = dmSansFamily, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(state.targetDate))
                    Text(dateStr, fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = state.targetValue,
                onValueChange = { state.targetValue = it },
                label = { Text("Value") },
                placeholder = { Text("40") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
            )
            OutlinedTextField(
                value = state.unit,
                onValueChange = { state.unit = it },
                label = { Text("Unit") },
                placeholder = { Text("km / sessions") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
            )
        }
        Text("Pace preference", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PaceChip("Front-load", PaceProfile.FRONT_LOAD, state)
            PaceChip("Steady", PaceProfile.STEADY, state)
            PaceChip("Push later", PaceProfile.BACK_LOAD, state)
        }
        Text(
            "Steady pacers get extra support at the midpoint — that's where most people fade.",
            fontFamily = dmSansFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun RowScope.PaceChip(label: String, profile: PaceProfile, state: GoalCreationState) {
    val selected = state.paceProfile == profile
    FilterChip(
        selected = selected,
        onClick = { state.paceProfile = profile },
        modifier = Modifier.weight(1f),
        label = { Text(text = label, fontFamily = dmSansFamily, fontSize = 12.sp) },
    )
}

@Composable
private fun Step4Quit(state: GoalCreationState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            "Cue & instead",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "You rarely delete a habit — you replace the routine, or remove the cue.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )
        OutlinedTextField(
            value = state.triggerText,
            onValueChange = { state.triggerText = it },
            label = { Text("What's your trigger?") },
            placeholder = { Text("After dinner, stress, boredom…") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        OutlinedTextField(
            value = state.replacementText,
            onValueChange = { state.replacementText = it },
            label = { Text("What will you do instead?") },
            placeholder = { Text("A 5-minute walk, tea, text a friend") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Track what you reclaim", fontFamily = dmSansFamily, fontWeight = FontWeight.Medium)
                Text(
                    "Optional. Motivating for some — skip if it feels transactional.",
                    fontFamily = dmSansFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
            }
            Switch(checked = state.trackCost, onCheckedChange = { state.trackCost = it })
        }
        if (state.trackCost) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.costPerOccurrence,
                    onValueChange = { state.costPerOccurrence = it },
                    label = { Text("Per day / occurrence") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                )
                OutlinedTextField(
                    value = state.costUnit,
                    onValueChange = { state.costUnit = it },
                    label = { Text("Unit") },
                    placeholder = { Text("money / minutes") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                )
            }
        }
    }
}

@Composable
private fun Step4Maintain(state: GoalCreationState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            "Define the floor",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "What's the minimum that still counts? One miss is noise. Two in a row is the danger zone.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = state.floorCount,
                onValueChange = { state.floorCount = it.filter { c -> c.isDigit() } },
                label = { Text("Minimum times") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
            )
            OutlinedTextField(
                value = state.floorPeriodDays,
                onValueChange = { state.floorPeriodDays = it.filter { c -> c.isDigit() } },
                label = { Text("Per how many days") },
                placeholder = { Text("7") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
            )
        }
        Text(
            "Example: 3 times every 7 days.",
            fontFamily = dmSansFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun Step5Habits(
    state: GoalCreationState,
    habits: List<HabitItem>,
    onNavigateToAddHabit: () -> Unit,
) {
    val (headline, sub) = when (state.goalType) {
        GoalType.BUILD -> "Which habit is the daily action?" to "Build without a linked habit is just a wish. Pick at least one."
        GoalType.REACH -> "Which habits move the number?" to "Input habits. Each completion counts toward the target."
        GoalType.QUIT -> "Replacement habits (optional)" to "What you do instead when the urge hits. Quit still works without one."
        GoalType.MAINTAIN -> "Which habits hold the floor?" to "We'll check these against your weekly minimum."
        null -> "Link habits" to ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            headline,
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            sub,
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )

        if (habits.none { !it.isArchived }) {
            Text(
                "No habits yet. Create one, then come back to link it.",
                fontFamily = dmSansFamily,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        } else {
            habits.filter { !it.isArchived }.forEach { habit ->
                val selected = state.linkedHabitIds.contains(habit.id)
                Card(
                    onClick = {
                        if (selected) state.linkedHabitIds.remove(habit.id)
                        else state.linkedHabitIds.add(habit.id)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                    ),
                    border = if (selected) {
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null,
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            habit.icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            habit.title,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onNavigateToAddHabit,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
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
        "#4CAF50", "#2196F3", "#9C27B0", "#8D6E63",
        "#FF9800", "#00BCD4", "#E91E63", "#607D8B",
    )
    val icons = listOf(
        Icons.Rounded.TrackChanges, Icons.Rounded.Star, Icons.Rounded.Favorite, Icons.Rounded.FitnessCenter,
        Icons.Rounded.LocalLibrary, Icons.Rounded.AttachMoney, Icons.Rounded.FlightTakeoff, Icons.Rounded.EmojiEvents,
    )
    val currentIcon = icons.firstOrNull { it.name == state.iconName } ?: Icons.Rounded.TrackChanges
    val typeLabel = state.goalType?.name?.lowercase()?.replaceFirstChar { it.titlecase() } ?: "Goal"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Text(
            "Make it yours",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(android.graphics.Color.parseColor(state.colorHex)).copy(alpha = 0.1f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(state.colorHex)).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        currentIcon,
                        contentDescription = null,
                        tint = Color(android.graphics.Color.parseColor(state.colorHex)),
                        modifier = Modifier.size(32.dp),
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text(
                        typeLabel.uppercase(),
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = Color(android.graphics.Color.parseColor(state.colorHex)),
                    )
                    Text(
                        state.title.ifEmpty { "Your goal" },
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                    )
                    Text(
                        state.whyStatement.ifEmpty { "Your private line…" },
                        fontFamily = dmSansFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Accent", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                colors.take(4).forEach { hex -> ColorCircle(hex, state.colorHex == hex) { state.colorHex = hex } }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                colors.drop(4).forEach { hex -> ColorCircle(hex, state.colorHex == hex) { state.colorHex = hex } }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Icon", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                icons.take(4).forEach { icon ->
                    IconCircle(icon, state.iconName == icon.name, state.colorHex) { state.iconName = icon.name }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                icons.drop(4).forEach { icon ->
                    IconCircle(icon, state.iconName == icon.name, state.colorHex) { state.iconName = icon.name }
                }
            }
        }
    }
}

@Composable
private fun PromptField(
    prompt: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    hint: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 48.sp,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        prompt,
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 40.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        lineHeight = 48.sp,
                    )
                }
                inner()
            },
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            hint,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
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
                shape = CircleShape,
            )
            .clickable { onClick() },
    )
}

@Composable
fun IconCircle(icon: ImageVector, isSelected: Boolean, colorHex: String, onClick: () -> Unit) {
    val color = Color(android.graphics.Color.parseColor(colorHex))
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isSelected) color else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
    }
}
