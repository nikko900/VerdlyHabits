package com.saintnico.verdlyhabits.ui.screens.habit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TimePicker
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import com.saintnico.verdlyhabits.ui.theme.AccentGold
import com.saintnico.verdlyhabits.ui.theme.PrimaryGreen
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.components.AnimatedHabitIcon
import com.saintnico.verdlyhabits.ui.models.premiumHabitIcons
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.HabitScheduling
import com.saintnico.verdlyhabits.domain.ReminderWindow
import com.saintnico.verdlyhabits.ui.utils.getPlantedDateText

val presetColors = listOf(
    PrimaryGreen,
    AccentGold,
    Color(0xFFE57373),
    Color(0xFF81C784),
    Color(0xFF64B5F6),
    Color(0xFF9575CD),
    Color(0xFFFFB74D),
    Color(0xFF4DB6AC)
)

private val weekDayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    habitToEdit: HabitItem? = null,
    onBack: () -> Unit,
    onSave: (HabitItem) -> Unit,
    onShowNotification: (String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var description by remember { mutableStateOf(habitToEdit?.notes ?: "") }
    var selectedIcon by remember { mutableStateOf(habitToEdit?.icon ?: premiumHabitIcons[0].vector) }
    var selectedColor by remember { mutableStateOf(if (habitToEdit != null) Color(habitToEdit.color.toInt()) else presetColors[0]) }
    var selectedFrequency by remember(habitToEdit?.id) { mutableStateOf(habitToEdit?.frequency ?: HabitFrequency.DAILY) }
    var reminderEnabled by remember { mutableStateOf(habitToEdit?.reminderEnabled ?: false) }
    var reminderTime by remember { mutableStateOf(habitToEdit?.reminderTime) }
    var reminderTime2 by remember { mutableStateOf(habitToEdit?.reminderTime2) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(habitToEdit?.difficulty ?: Difficulty.EASY) }
    var selectedCategory by remember { mutableStateOf(habitToEdit?.category ?: HabitCategory.OTHER) }
    var selectedWindow by remember { mutableStateOf(habitToEdit?.reminderWindow ?: ReminderWindow.EXACT) }
    var customDaySelected by remember(habitToEdit?.id) {
        mutableStateOf(HabitScheduling.booleansFromMask(habitToEdit?.customDaysMask))
    }
    var showTimePicker2 by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
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
                title = { Text(if (habitToEdit == null) stringResource(R.string.add_habit_title) else stringResource(R.string.edit_habit_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            // Preview Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(selectedColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedHabitIcon(
                        icon = selectedIcon, 
                        color = selectedColor,
                        size = 32.dp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = if (title.isEmpty()) stringResource(R.string.habit_name_hint) else title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.level_0_root),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    
                    if (habitToEdit != null) {
                        Text(
                            text = stringResource(R.string.planted_on, com.saintnico.verdlyhabits.ui.utils.getPlantedDateText(habitToEdit.plantedAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title & Description
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.habit_name_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = selectedColor,
                    focusedLabelColor = selectedColor
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(stringResource(R.string.habit_description_hint)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = selectedColor,
                    focusedLabelColor = selectedColor
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Emoji Picker
            Text(stringResource(R.string.choose_icon), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(premiumHabitIcons) { premiumIcon ->
                    val isSelected = selectedIcon == premiumIcon.vector
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) selectedColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .clickable { selectedIcon = premiumIcon.vector },
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedHabitIcon(
                            icon = premiumIcon.vector,
                            color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            size = 24.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Color Picker
            Text(stringResource(R.string.theme_color), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(presetColors) { color ->
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = color },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Difficulty ────────────────────────────────────────────────
            Text("Difficulty", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Difficulty.entries.forEach { diff ->
                    val on = selectedDifficulty == diff
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (on) diff.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (on) 2.dp else 1.dp,
                            if (on) diff.color
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedDifficulty = diff }
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                diff.displayName,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (on) diff.color
                                else MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "+${diff.xp} XP",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
            }
            Text(
                "Each completion uses this tier as its base XP. Streak and bonus XP from the engine stack on top when you check in.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 8.dp),
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Category ──────────────────────────────────────────────────
            Text("Category", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(HabitCategory.entries) { cat ->
                    val on = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (on) cat.color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (on) 2.dp else 1.dp,
                            if (on) cat.color
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
                        ),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(cat.color)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                cat.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                color = if (on) cat.color
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Reminder window (Smart vs Exact) ──────────────────────────
            Text("Reminder window", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReminderWindow.entries.forEach { win ->
                    val on = selectedWindow == win
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (on) selectedColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            if (on) 2.dp else 1.dp,
                            if (on) selectedColor
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedWindow = win }
                    ) {
                        Text(
                            win.displayName,
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (on) selectedColor
                            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Frequency
            Text(stringResource(R.string.frequency_label), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HabitFrequency.entries.forEach { freq ->
                    val isSelected = selectedFrequency == freq
                    val fill = if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    val labelColor = if (isSelected) contentColorFor(fill) else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = fill,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFrequency = freq },
                        shadowElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Text(
                            text = freq.label,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = labelColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = selectedFrequency == HabitFrequency.CUSTOM) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        "Pick the days this habit counts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        weekDayLabels.forEachIndexed { index, day ->
                            val on = customDaySelected.getOrElse(index) { false }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (on) selectedColor.copy(alpha = 0.22f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (on) selectedColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        CircleShape
                                    )
                                    .clickable {
                                        val next = customDaySelected.copyOf()
                                        if (index < next.size) next[index] = !next[index]
                                        customDaySelected = next
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    day,
                                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                    color = if (on) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Toggles
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(stringResource(R.string.reminder_label), fontWeight = FontWeight.SemiBold)
                            if (reminderEnabled && reminderTime != null) {
                                Text(stringResource(R.string.reminder_at, reminderTime!!), fontSize = 12.sp, color = selectedColor, fontWeight = FontWeight.SemiBold)
                            } else {
                                Text(stringResource(R.string.reminder_subtitle), fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                            }
                        }
                    }
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = { checked -> 
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
                        colors = SwitchDefaults.colors(checkedTrackColor = selectedColor)
                    )
                }

                AnimatedVisibility(visible = reminderEnabled) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .clickable { showTimePicker = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (reminderTime != null) "Primary time: $reminderTime" else "Set primary reminder time",
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Button(
                                onClick = { showTimePicker = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                            ) {
                                Text("Change")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker2 = true },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Second ping (optional)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                )
                                Text(
                                    if (reminderTime2 != null) "Also at $reminderTime2" else "Add another time the same day",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                )
                            }
                            Row {
                                if (reminderTime2 != null) {
                                    TextButton(onClick = { reminderTime2 = null }) {
                                        Text("Clear")
                                    }
                                }
                                Button(
                                    onClick = { showTimePicker2 = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                ) {
                                    Text(if (reminderTime2 == null) "Add" else "Edit")
                                }
                            }
                        }
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
                            onShowNotification("Reminder set for $hour:$minute.", false)
                        }) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker = false }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Primary reminder time", fontWeight = FontWeight.Bold) },
                    text = {
                        TimePicker(state = timePickerState)
                    }
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
                        }) {
                            Text("Confirm")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTimePicker2 = false }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Second reminder time", fontWeight = FontWeight.Bold) },
                    text = {
                        TimePicker(state = timePickerState2)
                    }
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        onShowNotification("Please give your habit a name to plant the seed.", true)
                    } else if (reminderEnabled && reminderTime == null) {
                        onShowNotification("Set a primary reminder time, or turn reminders off.", true)
                    } else if (selectedFrequency == HabitFrequency.CUSTOM && !customDaySelected.any { it }) {
                        onShowNotification("For Custom, pick at least one weekday.", true)
                    } else {
                        val mask =
                            if (selectedFrequency == HabitFrequency.CUSTOM) HabitScheduling.maskFromBooleans(customDaySelected)
                            else null
                        val newHabit = HabitItem(
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
                        )
                        onSave(newHabit)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = selectedColor)
            ) {
                Text(
                    text = if (habitToEdit == null) stringResource(R.string.save_button) else stringResource(R.string.edit_save_button),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColorFor(selectedColor)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
