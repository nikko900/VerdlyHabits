package com.saintnico.verdlyhabits.ui.screens.habit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.domain.CompletionWindow
import com.saintnico.verdlyhabits.domain.Difficulty
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitFrequency
import com.saintnico.verdlyhabits.domain.ReminderWindow
import com.saintnico.verdlyhabits.ui.components.AnimatedHabitIcon
import com.saintnico.verdlyhabits.ui.models.HabitIconRegistry
import com.saintnico.verdlyhabits.ui.models.PremiumIcon
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

@Composable
fun HabitFormSectionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            accent.copy(alpha = 0.12f),
        ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    )
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        lineHeight = 16.sp,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun HabitFormHeroPreview(
    title: String,
    titleHint: String,
    plantedLabel: String?,
    selectedIconId: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val icon = HabitIconRegistry.iconFor(selectedIconId)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(accent.copy(0.35f), accent.copy(0.08f))),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.10f))
                            .border(2.dp, accent.copy(alpha = 0.22f), CircleShape),
                    )
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(accent.copy(0.28f), accent.copy(0.08f)),
                                ),
                            )
                            .border(1.dp, accent.copy(0.22f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnimatedHabitIcon(icon = icon, color = accent, size = 28.dp)
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.ifBlank { titleHint },
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "How it will look on your home screen",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                    plantedLabel?.let {
                        Text(
                            it,
                            fontSize = 11.sp,
                            color = accent,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitIconPickerSection(
    selectedIconId: String,
    accent: Color,
    onSelect: (String) -> Unit,
) {
    var expandedGroup by remember { mutableStateOf<String?>(null) }
    val groups = HabitIconRegistry.groups

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (group in groups) {
                val active = expandedGroup == group
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (active) accent.copy(0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.45f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (active) accent.copy(0.45f) else MaterialTheme.colorScheme.outline.copy(0.15f),
                    ),
                    modifier = Modifier.clickable {
                        expandedGroup = if (active) null else group
                    },
                ) {
                    Text(
                        group,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        color = if (active) accent else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                    )
                }
            }
        }

        val iconsToShow = expandedGroup?.let { g ->
            HabitIconRegistry.allIcons.filter { it.group == g }
        } ?: HabitIconRegistry.allIcons.take(12)

        for (rowIcons in iconsToShow.chunked(6)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (premiumIcon in rowIcons) {
                    HabitIconChip(
                        premiumIcon = premiumIcon,
                        isSelected = selectedIconId == premiumIcon.id,
                        accent = accent,
                        onClick = { onSelect(premiumIcon.id) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (expandedGroup == null) {
            Text(
                "Tap a category above to browse all ${HabitIconRegistry.allIcons.size} icons",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(0.45f),
            )
        }
    }
}

@Composable
private fun HabitIconChip(
    premiumIcon: PremiumIcon,
    isSelected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) accent.copy(0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(0.45f),
            )
            .border(
                if (isSelected) 2.dp else 1.dp,
                if (isSelected) accent else MaterialTheme.colorScheme.outline.copy(0.12f),
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedHabitIcon(
            icon = premiumIcon.vector,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onBackground.copy(0.55f),
            size = 22.dp,
        )
    }
}

@Composable
fun HabitColorPickerSection(
    selectedColor: Color,
    onSelect: (Color) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (rowColors in HabitIconRegistry.themeColors.chunked(8)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for ((_, color) in rowColors) {
                    val isSelected = selectedColor == color
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                if (isSelected) 2.5.dp else 0.dp,
                                MaterialTheme.colorScheme.onBackground.copy(if (isSelected) 0.85f else 0f),
                                CircleShape,
                            )
                            .clickable { onSelect(color) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        val selectedName = HabitIconRegistry.themeColors
            .firstOrNull { it.second == selectedColor }
            ?.first ?: "Custom"
        Text(
            "Theme: $selectedName — used for icon tint and card accent on home",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
        )
    }
}

@Composable
fun HabitIdentityFields(
    title: String,
    description: String,
    accent: Color,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Habit name") },
            placeholder = { Text("e.g. Morning run") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedLabelColor = accent,
                cursorColor = accent,
            ),
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Why it matters (optional)") },
            placeholder = { Text("A short note to future you") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            minLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                focusedLabelColor = accent,
                cursorColor = accent,
            ),
        )
    }
}

private val weekDayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private val frequencyDescriptions = mapOf(
    HabitFrequency.DAILY to "Counts every day",
    HabitFrequency.WEEKDAYS to "Mon–Fri only",
    HabitFrequency.WEEKENDS to "Sat–Sun only",
    HabitFrequency.CUSTOM to "Pick specific days",
)

@Composable
fun HabitFrequencySection(
    selected: HabitFrequency,
    customDays: BooleanArray,
    accent: Color,
    onSelect: (HabitFrequency) -> Unit,
    onToggleDay: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (freq in HabitFrequency.entries) {
            val on = selected == freq
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (on) accent.copy(0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    if (on) 1.5.dp else 1.dp,
                    if (on) accent.copy(0.45f) else MaterialTheme.colorScheme.outline.copy(0.12f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(freq) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            freq.label,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (on) accent else MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            frequencyDescriptions[freq].orEmpty(),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                        )
                    }
                    if (on) {
                        Icon(Icons.Default.Check, null, tint = accent, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = selected == HabitFrequency.CUSTOM,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "Active on these days",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.65f),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    for ((index, day) in weekDayLabels.withIndex()) {
                        val on = customDays.getOrElse(index) { false }
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (on) accent.copy(0.20f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.45f))
                                .border(
                                    1.dp,
                                    if (on) accent else MaterialTheme.colorScheme.outline.copy(0.18f),
                                    CircleShape,
                                )
                                .clickable { onToggleDay(index) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.take(1),
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (on) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitDifficultySection(
    selected: Difficulty,
    completionStart: String?,
    completionEnd: String?,
    mediumWindowEnabled: Boolean,
    accent: Color,
    onSelect: (Difficulty) -> Unit,
    onCompletionStartClick: () -> Unit,
    onCompletionEndClick: () -> Unit,
    onMediumWindowToggle: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Harder habits earn more XP — and Hard / Epic require a daily time window to check in.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
            lineHeight = 17.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (diff in Difficulty.entries) {
                val on = selected == diff
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (on) diff.color.copy(0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.35f),
                    border = androidx.compose.foundation.BorderStroke(
                        if (on) 2.dp else 1.dp,
                        if (on) diff.color else MaterialTheme.colorScheme.outline.copy(0.10f),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(diff) },
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            diff.displayName,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (on) diff.color else MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            "+${diff.xp} XP",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                        )
                    }
                }
            }
        }

        val showWindow = CompletionWindow.requiresWindow(selected)
            || (CompletionWindow.supportsOptionalWindow(selected) && mediumWindowEnabled)

        AnimatedVisibility(
            visible = showWindow,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    CompletionWindow.windowHint(selected),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    lineHeight = 15.sp,
                )
                ReminderTimeRow(
                    label = "Complete from",
                    value = completionStart,
                    placeholder = "Start time",
                    accent = accent,
                    onClick = onCompletionStartClick,
                )
                ReminderTimeRow(
                    label = "Complete by",
                    value = completionEnd,
                    placeholder = "End time",
                    accent = accent,
                    onClick = onCompletionEndClick,
                )
                if (completionStart != null && completionEnd != null) {
                    Text(
                        "Window: ${CompletionWindow.formatRange(completionStart, completionEnd)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = accent,
                    )
                }
            }
        }

        if (CompletionWindow.supportsOptionalWindow(selected)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Commitment window", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text(
                        "Optional on-time bonus XP",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                    )
                }
                Switch(
                    checked = mediumWindowEnabled,
                    onCheckedChange = onMediumWindowToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = accent),
                )
            }
        }
    }
}

@Composable
fun HabitCategorySection(
    selected: HabitCategory,
    onSelect: (HabitCategory) -> Unit,
) {
    Text(
        "Organizes habits on home filters and your stats dashboard.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
        modifier = Modifier.padding(bottom = 10.dp),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (cat in HabitCategory.entries) {
            val on = selected == cat
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (on) cat.color.copy(0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.35f),
                border = androidx.compose.foundation.BorderStroke(
                    if (on) 1.5.dp else 1.dp,
                    if (on) cat.color else MaterialTheme.colorScheme.outline.copy(0.12f),
                ),
                modifier = Modifier.clickable { onSelect(cat) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(cat.color),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        cat.displayName,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) cat.color else MaterialTheme.colorScheme.onBackground.copy(0.75f),
                    )
                }
            }
        }
    }
}

@Composable
fun HabitReminderSection(
    enabled: Boolean,
    primaryTime: String?,
    secondaryTime: String?,
    selectedWindow: ReminderWindow,
    accent: Color,
    onToggle: (Boolean) -> Unit,
    onPrimaryTimeClick: () -> Unit,
    onSecondaryTimeClick: () -> Unit,
    onClearSecondary: () -> Unit,
    onWindowSelect: (ReminderWindow) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Daily reminder", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    if (enabled && primaryTime != null) "We'll nudge you at $primaryTime"
                    else "Get a push when it's time to act",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = accent),
            )
        }

        AnimatedVisibility(visible = enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "When should we remind you?",
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                )
                Text(
                    "Exact time uses your pick below. Morning / Afternoon / Evening spreads habits across a window so notifications don't pile up.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                    lineHeight = 15.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (win in ReminderWindow.entries) {
                        val on = selectedWindow == win
                        val label = when (win) {
                            ReminderWindow.EXACT -> "Exact"
                            ReminderWindow.MORNING -> "6–9 AM"
                            ReminderWindow.AFTERNOON -> "12–3 PM"
                            ReminderWindow.EVENING -> "6–9 PM"
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (on) accent.copy(0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                if (on) 1.5.dp else 1.dp,
                                if (on) accent else MaterialTheme.colorScheme.outline.copy(0.12f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onWindowSelect(win) },
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                                color = if (on) accent else MaterialTheme.colorScheme.onBackground.copy(0.7f),
                            )
                        }
                    }
                }

                ReminderTimeRow(
                    label = if (selectedWindow == ReminderWindow.EXACT) "Reminder time" else "Anchor time (optional)",
                    value = primaryTime,
                    placeholder = "Set time",
                    accent = accent,
                    onClick = onPrimaryTimeClick,
                )

                if (selectedWindow == ReminderWindow.EXACT) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Second ping", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(
                                secondaryTime?.let { "Also at $it" } ?: "Optional follow-up same day",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
                            )
                        }
                        if (secondaryTime != null) {
                            TextButton(onClick = onClearSecondary) { Text("Clear") }
                        }
                        Button(
                            onClick = onSecondaryTimeClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        ) {
                            Text(if (secondaryTime == null) "Add" else "Edit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderTimeRow(
    label: String,
    value: String?,
    placeholder: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AccessTime, null, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(
                    value ?: placeholder,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (value != null) accent else MaterialTheme.colorScheme.onBackground.copy(0.45f),
                )
            }
            Text("Change", fontSize = 12.sp, color = accent, fontWeight = FontWeight.Bold)
        }
    }
}
