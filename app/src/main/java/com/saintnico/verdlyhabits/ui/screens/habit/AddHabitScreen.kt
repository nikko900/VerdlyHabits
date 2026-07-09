package com.saintnico.verdlyhabits.ui.screens.habit

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.domain.CompletionWindow
import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.HabitScheduling
import com.saintnico.verdlyhabits.domain.ReminderWindow
import com.saintnico.verdlyhabits.ui.models.HabitIconRegistry
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.utils.getPlantedDateText
import java.util.UUID

private const val PLANT_STEPS = 6

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AddHabitScreen(
    habitToEdit: HabitItem? = null,
    onBack: () -> Unit,
    onSave: (HabitItem) -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
) {
    val isEditing = habitToEdit != null
    val initialIconId = remember(habitToEdit?.id) {
        habitToEdit?.let { HabitIconRegistry.stableId(it.icon) } ?: HabitIconRegistry.allIcons.first().id
    }
    val initialColor = remember(habitToEdit?.id) {
        if (habitToEdit != null) Color(habitToEdit.color.toInt()) else HabitIconRegistry.themeColors.first().second
    }

    var step by remember { mutableIntStateOf(1) }
    var title by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var description by remember { mutableStateOf(habitToEdit?.notes ?: "") }
    var selectedIconId by remember { mutableStateOf(initialIconId) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedFrequency by remember(habitToEdit?.id) {
        mutableStateOf(habitToEdit?.frequency ?: HabitFrequency.DAILY)
    }
    var reminderEnabled by remember { mutableStateOf(habitToEdit?.reminderEnabled ?: false) }
    var reminderTime by remember { mutableStateOf(habitToEdit?.reminderTime) }
    var reminderTime2 by remember { mutableStateOf(habitToEdit?.reminderTime2) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTimePicker2 by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(habitToEdit?.difficulty ?: Difficulty.EASY) }
    var completionWindowStart by remember { mutableStateOf(habitToEdit?.completionWindowStart) }
    var completionWindowEnd by remember { mutableStateOf(habitToEdit?.completionWindowEnd) }
    var mediumWindowEnabled by remember(habitToEdit?.id) {
        mutableStateOf(
            habitToEdit?.difficulty == Difficulty.MEDIUM &&
                !habitToEdit.completionWindowStart.isNullOrBlank(),
        )
    }
    var showCompletionStartPicker by remember { mutableStateOf(false) }
    var showCompletionEndPicker by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(habitToEdit?.category ?: HabitCategory.OTHER) }
    var selectedWindow by remember { mutableStateOf(habitToEdit?.reminderWindow ?: ReminderWindow.EXACT) }
    var customDaySelected by remember(habitToEdit?.id) {
        mutableStateOf(HabitScheduling.booleansFromMask(habitToEdit?.customDaysMask))
    }

    val selectedIcon = HabitIconRegistry.iconFor(selectedIconId)
    val progress = step / PLANT_STEPS.toFloat()

    LaunchedEffect(selectedDifficulty) {
        if (CompletionWindow.requiresWindow(selectedDifficulty) &&
            (completionWindowStart.isNullOrBlank() || completionWindowEnd.isNullOrBlank())
        ) {
            val (start, end) = CompletionWindow.suggestedRange(selectedDifficulty)
            if (completionWindowStart.isNullOrBlank()) completionWindowStart = start
            if (completionWindowEnd.isNullOrBlank()) completionWindowEnd = end
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            reminderEnabled = true
            showTimePicker = true
        } else {
            reminderEnabled = false
            onShowNotification("Permission denied. We can't remind you.", true)
        }
    }

    fun tryAdvance() {
        when (step) {
            1 -> if (title.isBlank()) {
                onShowNotification("Give your seed a name — even a small one counts.", true)
            } else {
                step++
            }
            2 -> step++ // why is optional
            3 -> if (selectedFrequency == HabitFrequency.CUSTOM && !customDaySelected.any { it }) {
                onShowNotification("Pick at least one day for your custom rhythm.", true)
            } else {
                step++
            }
            4 -> {
                val needsWindow = CompletionWindow.requiresWindow(selectedDifficulty) ||
                    (selectedDifficulty == Difficulty.MEDIUM && mediumWindowEnabled)
                if (needsWindow &&
                    !CompletionWindow.isValidRange(completionWindowStart, completionWindowEnd, selectedDifficulty)
                ) {
                    onShowNotification(
                        "Set a real time window for this effort — e.g. 05:00 – 07:00.",
                        true,
                    )
                } else {
                    step++
                }
            }
            5 -> step++
            6 -> plantHabit(
                habitToEdit = habitToEdit,
                title = title,
                description = description,
                selectedIcon = selectedIcon,
                selectedColor = selectedColor,
                reminderEnabled = reminderEnabled,
                reminderTime = reminderTime,
                reminderTime2 = reminderTime2,
                selectedDifficulty = selectedDifficulty,
                selectedCategory = selectedCategory,
                selectedWindow = selectedWindow,
                selectedFrequency = selectedFrequency,
                customDaySelected = customDaySelected,
                completionWindowStart = completionWindowStart,
                completionWindowEnd = completionWindowEnd,
                mediumWindowEnabled = mediumWindowEnabled,
                onSave = onSave,
                onShowNotification = onShowNotification,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (isEditing) "Tend this habit" else "Plant a habit",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                        )
                        Text(
                            journeyStepLabel(step, isEditing),
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (step > 1) step-- else onBack()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.background,
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50)),
                        color = selectedColor,
                        trackColor = selectedColor.copy(alpha = 0.15f),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "$step of $PLANT_STEPS",
                            fontFamily = dmSansFamily,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        )
                        Button(
                            onClick = { tryAdvance() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                        ) {
                            Text(
                                when {
                                    step < PLANT_STEPS -> "Continue"
                                    isEditing -> "Save changes"
                                    else -> "Plant the seed"
                                },
                                fontFamily = dmSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColorFor(selectedColor),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(tween(400)) { it } + fadeIn(tween(400))) togetherWith
                        (slideOutHorizontally(tween(400)) { -it } + fadeOut(tween(400)))
                } else {
                    (slideInHorizontally(tween(400)) { -it } + fadeIn(tween(400))) togetherWith
                        (slideOutHorizontally(tween(400)) { it } + fadeOut(tween(400)))
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            label = "plantJourney",
        ) { current ->
            when (current) {
                1 -> PlantStepName(title = title, onTitleChange = { title = it })
                2 -> PlantStepWhy(description = description, onDescriptionChange = { description = it })
                3 -> PlantStepRhythm(
                    selectedFrequency = selectedFrequency,
                    customDaySelected = customDaySelected,
                    accent = selectedColor,
                    onSelectFrequency = { selectedFrequency = it },
                    onToggleDay = { index ->
                        val next = customDaySelected.copyOf()
                        if (index < next.size) next[index] = !next[index]
                        customDaySelected = next
                    },
                )
                4 -> PlantStepEffort(
                    selectedDifficulty = selectedDifficulty,
                    completionWindowStart = completionWindowStart,
                    completionWindowEnd = completionWindowEnd,
                    mediumWindowEnabled = mediumWindowEnabled,
                    accent = selectedColor,
                    onSelectDifficulty = { diff ->
                        selectedDifficulty = diff
                        if (CompletionWindow.requiresWindow(diff)) {
                            val (start, end) = CompletionWindow.suggestedRange(diff)
                            completionWindowStart = start
                            completionWindowEnd = end
                        } else if (diff == Difficulty.MEDIUM && mediumWindowEnabled) {
                            val (start, end) = CompletionWindow.suggestedRange(diff)
                            if (completionWindowStart.isNullOrBlank()) completionWindowStart = start
                            if (completionWindowEnd.isNullOrBlank()) completionWindowEnd = end
                        } else if (diff == Difficulty.EASY) {
                            completionWindowStart = null
                            completionWindowEnd = null
                            mediumWindowEnabled = false
                        }
                    },
                    onCompletionStartClick = { showCompletionStartPicker = true },
                    onCompletionEndClick = { showCompletionEndPicker = true },
                    onMediumWindowToggle = { enabled ->
                        mediumWindowEnabled = enabled
                        if (enabled) {
                            val (start, end) = CompletionWindow.suggestedRange(Difficulty.MEDIUM)
                            if (completionWindowStart.isNullOrBlank()) completionWindowStart = start
                            if (completionWindowEnd.isNullOrBlank()) completionWindowEnd = end
                        } else {
                            completionWindowStart = null
                            completionWindowEnd = null
                        }
                    },
                )
                5 -> PlantStepLook(
                    title = title,
                    selectedIconId = selectedIconId,
                    selectedColor = selectedColor,
                    selectedCategory = selectedCategory,
                    plantedLabel = habitToEdit?.let {
                        stringResource(R.string.planted_on, getPlantedDateText(it.plantedAt))
                    },
                    onSelectIcon = { selectedIconId = it },
                    onSelectColor = { selectedColor = it },
                    onSelectCategory = { selectedCategory = it },
                )
                6 -> PlantStepReady(
                    title = title,
                    description = description,
                    selectedIconId = selectedIconId,
                    selectedColor = selectedColor,
                    selectedDifficulty = selectedDifficulty,
                    selectedFrequency = selectedFrequency,
                    completionWindowStart = completionWindowStart,
                    completionWindowEnd = completionWindowEnd,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    reminderTime2 = reminderTime2,
                    selectedWindow = selectedWindow,
                    isEditing = isEditing,
                    onToggleReminder = { checked ->
                        if (checked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                reminderEnabled = true
                                showTimePicker = true
                            }
                        } else {
                            reminderEnabled = false
                            reminderTime = null
                            reminderTime2 = null
                        }
                    },
                    onPrimaryTimeClick = { showTimePicker = true },
                    onSecondaryTimeClick = { showTimePicker2 = true },
                    onClearSecondary = { reminderTime2 = null },
                    onWindowSelect = { selectedWindow = it },
                )
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    reminderTime = "$hour:$minute"
                    showTimePicker = false
                    onShowNotification("We'll gently nudge you at $hour:$minute.", false)
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            title = { Text("Reminder time", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
        )
    }

    if (showTimePicker2) {
        val timePickerState2 = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showTimePicker2 = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState2.hour.toString().padStart(2, '0')
                    val minute = timePickerState2.minute.toString().padStart(2, '0')
                    reminderTime2 = "$hour:$minute"
                    showTimePicker2 = false
                    onShowNotification("Second nudge at $hour:$minute", false)
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker2 = false }) { Text("Cancel") }
            },
            title = { Text("Second reminder", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState2) },
        )
    }

    if (showCompletionStartPicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showCompletionStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    completionWindowStart = "$hour:$minute"
                    showCompletionStartPicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showCompletionStartPicker = false }) { Text("Cancel") }
            },
            title = { Text("Window starts", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
        )
    }

    if (showCompletionEndPicker) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showCompletionEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour.toString().padStart(2, '0')
                    val minute = timePickerState.minute.toString().padStart(2, '0')
                    completionWindowEnd = "$hour:$minute"
                    showCompletionEndPicker = false
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showCompletionEndPicker = false }) { Text("Cancel") }
            },
            title = { Text("Window ends", fontWeight = FontWeight.Bold) },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

private fun journeyStepLabel(step: Int, isEditing: Boolean): String = when (step) {
    1 -> if (isEditing) "Rename with care" else "Name the seed"
    2 -> "Why it matters"
    3 -> "Your rhythm"
    4 -> "Your effort"
    5 -> "Make it yours"
    else -> if (isEditing) "Ready to save" else "Ready to plant"
}

@Composable
private fun PlantStepName(title: String, onTitleChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "What are you planting?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 34.sp,
            lineHeight = 42.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Say it like a promise to yourself — short, clear, kind.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            lineHeight = 22.sp,
        )
        Spacer(modifier = Modifier.height(36.dp))
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            textStyle = TextStyle(
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 36.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 44.sp,
            ),
            decorationBox = { inner ->
                if (title.isEmpty()) {
                    Text(
                        "Early morning walk",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f),
                        lineHeight = 44.sp,
                    )
                }
                inner()
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "Not “be healthier.” Something you can do today.",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        )
    }
}

@Composable
private fun PlantStepWhy(description: String, onDescriptionChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Why does this matter?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Private to you. We’ll hold this gently when motivation dips.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            lineHeight = 22.sp,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(22.dp),
        ) {
            BasicTextField(
                value = description,
                onValueChange = onDescriptionChange,
                textStyle = TextStyle(
                    fontFamily = frauncesFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 30.sp,
                ),
                decorationBox = { inner ->
                    if (description.isEmpty()) {
                        Text(
                            "I want mornings that feel like mine…",
                            fontFamily = frauncesFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.32f),
                            lineHeight = 30.sp,
                        )
                    }
                    inner()
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Skip if you’re not ready — the seed still grows.",
            fontFamily = dmSansFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun PlantStepRhythm(
    selectedFrequency: HabitFrequency,
    customDaySelected: BooleanArray,
    accent: Color,
    onSelectFrequency: (HabitFrequency) -> Unit,
    onToggleDay: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            "When does this count?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Choose a rhythm that loves your real life — not a fantasy schedule.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            lineHeight = 22.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        HabitFrequencySection(
            selected = selectedFrequency,
            customDays = customDaySelected,
            accent = accent,
            onSelect = onSelectFrequency,
            onToggleDay = onToggleDay,
        )
    }
}

@Composable
private fun PlantStepEffort(
    selectedDifficulty: Difficulty,
    completionWindowStart: String?,
    completionWindowEnd: String?,
    mediumWindowEnabled: Boolean,
    accent: Color,
    onSelectDifficulty: (Difficulty) -> Unit,
    onCompletionStartClick: () -> Unit,
    onCompletionEndClick: () -> Unit,
    onMediumWindowToggle: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Text(
            "How hard should this feel?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Honest effort earns more XP. Hard & Epic ask for a daily time window — that’s how you win bigger.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            lineHeight = 22.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        HabitDifficultySection(
            selected = selectedDifficulty,
            completionStart = completionWindowStart,
            completionEnd = completionWindowEnd,
            mediumWindowEnabled = mediumWindowEnabled,
            accent = accent,
            onSelect = onSelectDifficulty,
            onCompletionStartClick = onCompletionStartClick,
            onCompletionEndClick = onCompletionEndClick,
            onMediumWindowToggle = onMediumWindowToggle,
        )
    }
}

@Composable
private fun PlantStepLook(
    title: String,
    selectedIconId: String,
    selectedColor: Color,
    selectedCategory: HabitCategory,
    plantedLabel: String?,
    onSelectIcon: (String) -> Unit,
    onSelectColor: (Color) -> Unit,
    onSelectCategory: (HabitCategory) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            "Make it feel like yours",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
        )
        Text(
            "A look you’ll smile at every time it shows up on home.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        HabitFormHeroPreview(
            title = title,
            titleHint = "Your habit",
            plantedLabel = plantedLabel,
            selectedIconId = selectedIconId,
            accent = selectedColor,
        )
        HabitIconPickerSection(
            selectedIconId = selectedIconId,
            accent = selectedColor,
            onSelect = onSelectIcon,
        )
        HabitColorPickerSection(
            selectedColor = selectedColor,
            onSelect = onSelectColor,
        )
        HabitCategorySection(
            selected = selectedCategory,
            onSelect = onSelectCategory,
        )
    }
}

@Composable
private fun PlantStepReady(
    title: String,
    description: String,
    selectedIconId: String,
    selectedColor: Color,
    selectedDifficulty: Difficulty,
    selectedFrequency: HabitFrequency,
    completionWindowStart: String?,
    completionWindowEnd: String?,
    reminderEnabled: Boolean,
    reminderTime: String?,
    reminderTime2: String?,
    selectedWindow: ReminderWindow,
    isEditing: Boolean,
    onToggleReminder: (Boolean) -> Unit,
    onPrimaryTimeClick: () -> Unit,
    onSecondaryTimeClick: () -> Unit,
    onClearSecondary: () -> Unit,
    onWindowSelect: (ReminderWindow) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            if (isEditing) "You’re almost there" else "You’re already becoming someone who shows up",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
        )
        Text(
            if (isEditing) "One last look — then we save your care."
            else "Planting is the first win. Everything after is proof.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            lineHeight = 22.sp,
        )

        HabitFormHeroPreview(
            title = title,
            titleHint = "Your habit",
            plantedLabel = null,
            selectedIconId = selectedIconId,
            accent = selectedColor,
        )

        PlantSummaryCard(
            accent = selectedColor,
            difficulty = selectedDifficulty,
            frequency = selectedFrequency,
            windowStart = completionWindowStart,
            windowEnd = completionWindowEnd,
            why = description,
        )

        HabitReminderSection(
            enabled = reminderEnabled,
            primaryTime = reminderTime,
            secondaryTime = reminderTime2,
            selectedWindow = selectedWindow,
            accent = selectedColor,
            onToggle = onToggleReminder,
            onPrimaryTimeClick = onPrimaryTimeClick,
            onSecondaryTimeClick = onSecondaryTimeClick,
            onClearSecondary = onClearSecondary,
            onWindowSelect = onWindowSelect,
        )

        AnimatedVisibility(visible = !isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                selectedColor.copy(alpha = 0.16f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            ),
                        ),
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(selectedColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Eco, null, tint = selectedColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "The seed is ready",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        "Tap Plant — then take one tiny action today.",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PlantSummaryCard(
    accent: Color,
    difficulty: Difficulty,
    frequency: HabitFrequency,
    windowStart: String?,
    windowEnd: String?,
    why: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Your commitment",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        SummaryLine("Rhythm", frequency.label)
        SummaryLine("Effort", "${difficulty.displayName} · +${difficulty.xp} XP")
        if (!windowStart.isNullOrBlank() && !windowEnd.isNullOrBlank()) {
            SummaryLine("Window", CompletionWindow.formatRange(windowStart, windowEnd))
        }
        if (why.isNotBlank()) {
            Text(
                "“$why”",
                fontFamily = frauncesFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                lineHeight = 20.sp,
            )
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            label,
            fontFamily = dmSansFamily,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        )
        Text(
            value,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

private fun plantHabit(
    habitToEdit: HabitItem?,
    title: String,
    description: String,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedColor: Color,
    reminderEnabled: Boolean,
    reminderTime: String?,
    reminderTime2: String?,
    selectedDifficulty: Difficulty,
    selectedCategory: HabitCategory,
    selectedWindow: ReminderWindow,
    selectedFrequency: HabitFrequency,
    customDaySelected: BooleanArray,
    completionWindowStart: String?,
    completionWindowEnd: String?,
    mediumWindowEnabled: Boolean,
    onSave: (HabitItem) -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
) {
    when {
        title.isBlank() ->
            onShowNotification("Please give your habit a name to plant the seed.", true)
        reminderEnabled && reminderTime == null ->
            onShowNotification("Set a reminder time, or turn reminders off.", true)
        selectedFrequency == HabitFrequency.CUSTOM && !customDaySelected.any { it } ->
            onShowNotification("For Custom, pick at least one weekday.", true)
        CompletionWindow.requiresWindow(selectedDifficulty) &&
            !CompletionWindow.isValidRange(completionWindowStart, completionWindowEnd, selectedDifficulty) ->
            onShowNotification("Hard & Epic habits need a valid time window.", true)
        mediumWindowEnabled && selectedDifficulty == Difficulty.MEDIUM &&
            !CompletionWindow.isValidRange(completionWindowStart, completionWindowEnd, selectedDifficulty) ->
            onShowNotification("Set both start and end times for your commitment window.", true)
        else -> {
            val windowStart = completionWindowStart?.takeIf {
                CompletionWindow.requiresWindow(selectedDifficulty) ||
                    (selectedDifficulty == Difficulty.MEDIUM && mediumWindowEnabled)
            }
            val windowEnd = completionWindowEnd?.takeIf {
                CompletionWindow.requiresWindow(selectedDifficulty) ||
                    (selectedDifficulty == Difficulty.MEDIUM && mediumWindowEnabled)
            }
            val mask = if (selectedFrequency == HabitFrequency.CUSTOM) {
                HabitScheduling.maskFromBooleans(customDaySelected)
            } else {
                null
            }
            onSave(
                HabitItem(
                    id = habitToEdit?.id ?: UUID.randomUUID().toString(),
                    title = title.trim(),
                    icon = selectedIcon,
                    streak = habitToEdit?.streak ?: 0,
                    reminderEnabled = reminderEnabled,
                    reminderTime = reminderTime,
                    reminderTime2 = reminderTime2?.takeIf { reminderEnabled },
                    color = selectedColor.value.toLong(),
                    notes = description.trim(),
                    plantedAt = habitToEdit?.plantedAt ?: System.currentTimeMillis(),
                    isCompleted = habitToEdit?.isCompleted ?: false,
                    completedDates = habitToEdit?.completedDates ?: emptySet(),
                    isPaused = habitToEdit?.isPaused ?: false,
                    isArchived = habitToEdit?.isArchived ?: false,
                    completionProofs = habitToEdit?.completionProofs ?: emptyMap(),
                    difficulty = selectedDifficulty,
                    category = selectedCategory,
                    reminderWindow = selectedWindow,
                    linkedHabitId = habitToEdit?.linkedHabitId,
                    isFavoriteFocus = habitToEdit?.isFavoriteFocus ?: false,
                    frequency = selectedFrequency,
                    customDaysMask = mask,
                    completionWindowStart = windowStart,
                    completionWindowEnd = windowEnd,
                ),
            )
        }
    }
}
