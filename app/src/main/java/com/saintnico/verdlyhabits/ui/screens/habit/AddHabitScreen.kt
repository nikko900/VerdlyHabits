package com.saintnico.verdlyhabits.ui.screens.habit

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.HabitScheduling
import com.saintnico.verdlyhabits.domain.ReminderWindow
import com.saintnico.verdlyhabits.ui.models.HabitIconRegistry
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.utils.getPlantedDateText
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    habitToEdit: HabitItem? = null,
    onBack: () -> Unit,
    onSave: (HabitItem) -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
) {
    val initialIconId = remember(habitToEdit?.id) {
        habitToEdit?.let { HabitIconRegistry.stableId(it.icon) } ?: HabitIconRegistry.allIcons.first().id
    }
    val initialColor = remember(habitToEdit?.id) {
        if (habitToEdit != null) Color(habitToEdit.color.toInt()) else HabitIconRegistry.themeColors.first().second
    }

    var title by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var description by remember { mutableStateOf(habitToEdit?.notes ?: "") }
    var selectedIconId by remember { mutableStateOf(initialIconId) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    var selectedFrequency by remember(habitToEdit?.id) { mutableStateOf(habitToEdit?.frequency ?: HabitFrequency.DAILY) }
    var reminderEnabled by remember { mutableStateOf(habitToEdit?.reminderEnabled ?: false) }
    var reminderTime by remember { mutableStateOf(habitToEdit?.reminderTime) }
    var reminderTime2 by remember { mutableStateOf(habitToEdit?.reminderTime2) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTimePicker2 by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(habitToEdit?.difficulty ?: Difficulty.EASY) }
    var selectedCategory by remember { mutableStateOf(habitToEdit?.category ?: HabitCategory.OTHER) }
    var selectedWindow by remember { mutableStateOf(habitToEdit?.reminderWindow ?: ReminderWindow.EXACT) }
    var customDaySelected by remember(habitToEdit?.id) {
        mutableStateOf(HabitScheduling.booleansFromMask(habitToEdit?.customDaysMask))
    }

    val selectedIcon = HabitIconRegistry.iconFor(selectedIconId)

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (habitToEdit == null) stringResource(R.string.add_habit_title)
                        else stringResource(R.string.edit_habit_title),
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            HabitFormHeroPreview(
                title = title,
                titleHint = stringResource(R.string.habit_name_hint),
                plantedLabel = habitToEdit?.let {
                    stringResource(R.string.planted_on, getPlantedDateText(it.plantedAt))
                },
                selectedIconId = selectedIconId,
                accent = selectedColor,
            )

            HabitFormSectionCard(
                title = "Identity",
                subtitle = "Name your habit and add a personal note",
                icon = Icons.Rounded.Star,
                accent = selectedColor,
            ) {
                HabitIdentityFields(
                    title = title,
                    description = description,
                    accent = selectedColor,
                    onTitleChange = { title = it },
                    onDescriptionChange = { description = it },
                )
            }

            HabitFormSectionCard(
                title = "Look & feel",
                subtitle = "Icon and theme color shown on your home screen",
                icon = Icons.Rounded.Palette,
                accent = selectedColor,
            ) {
                HabitIconPickerSection(
                    selectedIconId = selectedIconId,
                    accent = selectedColor,
                    onSelect = { selectedIconId = it },
                )
                Spacer(Modifier.height(16.dp))
                HabitColorPickerSection(
                    selectedColor = selectedColor,
                    onSelect = { selectedColor = it },
                )
            }

            HabitFormSectionCard(
                title = "Schedule",
                subtitle = "When does this habit count toward your day?",
                icon = Icons.Rounded.CalendarMonth,
                accent = selectedColor,
            ) {
                HabitFrequencySection(
                    selected = selectedFrequency,
                    customDays = customDaySelected,
                    accent = selectedColor,
                    onSelect = { selectedFrequency = it },
                    onToggleDay = { index ->
                        val next = customDaySelected.copyOf()
                        if (index < next.size) next[index] = !next[index]
                        customDaySelected = next
                    },
                )
            }

            HabitFormSectionCard(
                title = "Effort & XP",
                subtitle = "How challenging is this habit for you?",
                icon = Icons.Rounded.Tune,
                accent = selectedColor,
            ) {
                HabitDifficultySection(
                    selected = selectedDifficulty,
                    onSelect = { selectedDifficulty = it },
                )
            }

            HabitFormSectionCard(
                title = "Category",
                subtitle = "Group it for filters and analytics",
                icon = Icons.Rounded.Category,
                accent = selectedColor,
            ) {
                HabitCategorySection(
                    selected = selectedCategory,
                    onSelect = { selectedCategory = it },
                )
            }

            HabitFormSectionCard(
                title = "Reminders",
                subtitle = "Optional push notifications to stay on track",
                icon = Icons.Rounded.Notifications,
                accent = selectedColor,
            ) {
                HabitReminderSection(
                    enabled = reminderEnabled,
                    primaryTime = reminderTime,
                    secondaryTime = reminderTime2,
                    selectedWindow = selectedWindow,
                    accent = selectedColor,
                    onToggle = { checked ->
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
                            onShowNotification("Reminder set for $hour:$minute.", false)
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
                            onShowNotification("Second reminder at $hour:$minute", false)
                        }) { Text("Confirm") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker2 = false }) { Text("Cancel") }
                    },
                    title = { Text("Second reminder", fontWeight = FontWeight.Bold) },
                    text = { TimePicker(state = timePickerState2) },
                )
            }

            Button(
                onClick = {
                    when {
                        title.isBlank() ->
                            onShowNotification("Please give your habit a name to plant the seed.", true)
                        reminderEnabled && reminderTime == null ->
                            onShowNotification("Set a reminder time, or turn reminders off.", true)
                        selectedFrequency == HabitFrequency.CUSTOM && !customDaySelected.any { it } ->
                            onShowNotification("For Custom, pick at least one weekday.", true)
                        else -> {
                            val mask = if (selectedFrequency == HabitFrequency.CUSTOM) {
                                HabitScheduling.maskFromBooleans(customDaySelected)
                            } else null
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
                                ),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
            ) {
                Text(
                    if (habitToEdit == null) stringResource(R.string.save_button)
                    else stringResource(R.string.edit_save_button),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColorFor(selectedColor),
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
