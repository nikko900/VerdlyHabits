package com.saintnico.verdlyhabits.ui.screens.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saintnico.verdlyhabits.BuildConfig
import com.saintnico.verdlyhabits.MainActivity
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.preferences.ThemeChoice
import com.saintnico.verdlyhabits.ui.screens.focus.AmbientSoundManager
import com.saintnico.verdlyhabits.ui.theme.DangerRed
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.CoinPackReason
import com.saintnico.verdlyhabits.ui.viewmodel.CoinViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.GroveViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.HabitViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.widget.HabitsWidgetReceiver
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SettingsNotificationsPage(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onRequestPushPermission: () -> Unit,
    onOpenInbox: () -> Unit,
) {
    val notificationsEnabled by settingsViewModel.isNotificationsEnabled.collectAsState()
    val habitReminders by settingsViewModel.habitRemindersEnabled.collectAsState()
    val dailyGoal by settingsViewModel.goalDailyReminderEnabled.collectAsState()
    val weeklyGoal by settingsViewModel.goalWeeklyCheckInEnabled.collectAsState()
    val socialOn by settingsViewModel.socialNotificationsEnabled.collectAsState()
    val goalOn = dailyGoal || weeklyGoal

    SettingsPageScaffold(title = stringResource(R.string.settings_notifications), onBack = onBack) {
        SettingsSection {
            SettingsToggle(
                title = stringResource(R.string.notif_push),
                subtitle = stringResource(R.string.notif_push_sub),
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFF4CAF50),
                checked = notificationsEnabled,
                onCheckedChange = { checked ->
                    if (checked) onRequestPushPermission()
                    else settingsViewModel.setNotificationsEnabled(false)
                },
            )
            SettingsDivider()
            SettingsToggle(
                title = stringResource(R.string.notif_habit),
                subtitle = stringResource(R.string.notif_habit_sub),
                icon = Icons.Default.Spa,
                iconTint = Color(0xFF66BB6A),
                checked = habitReminders,
                onCheckedChange = { settingsViewModel.setHabitRemindersEnabled(it) },
            )
            SettingsDivider()
            SettingsToggle(
                title = stringResource(R.string.notif_goal),
                subtitle = stringResource(R.string.notif_goal_sub),
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFF42A5F5),
                checked = goalOn,
                onCheckedChange = { settingsViewModel.setGoalNotificationsEnabled(it) },
            )
            SettingsDivider()
            SettingsToggle(
                title = stringResource(R.string.notif_social),
                subtitle = stringResource(R.string.notif_social_sub),
                icon = Icons.Outlined.People,
                iconTint = Color(0xFF9575CD),
                checked = socialOn,
                onCheckedChange = { settingsViewModel.setSocialNotificationsEnabled(it) },
            )
            SettingsDivider()
            SettingsSoonRow(
                title = stringResource(R.string.notif_duo),
                subtitle = stringResource(R.string.notif_duo_sub),
                icon = Icons.Outlined.SportsScore,
                iconTint = Color(0xFFFFB74D),
            )
            SettingsDivider()
            SettingsSoonRow(
                title = stringResource(R.string.notif_streak),
                subtitle = stringResource(R.string.notif_streak_sub),
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFFE57373),
            )
            SettingsDivider()
            SettingsRow(
                title = stringResource(R.string.notif_history),
                subtitle = stringResource(R.string.notif_history_sub),
                icon = Icons.Default.History,
                iconTint = Color(0xFF78909C),
                control = SettingsControl.Chevron,
                onClick = onOpenInbox,
            )
        }
    }
}

@Composable
fun SettingsAppearancePage(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val themeChoice by settingsViewModel.themeChoice.collectAsState()
    val languageCode by settingsViewModel.languageCode.collectAsState()
    val soundEnabled by settingsViewModel.isSoundEnabled.collectAsState()
    val hapticsEnabled by settingsViewModel.isHapticsEnabled.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    SettingsPageScaffold(title = stringResource(R.string.settings_appearance), onBack = onBack) {
        SettingsSection {
            SettingsRow(
                title = stringResource(R.string.appear_system),
                subtitle = stringResource(R.string.appear_system_sub),
                icon = Icons.Default.PhoneAndroid,
                iconTint = Color(0xFF66BB6A),
                control = SettingsControl.Value(
                    if (themeChoice == ThemeChoice.SYSTEM) "On" else "",
                ),
                onClick = { settingsViewModel.setThemeChoice(ThemeChoice.SYSTEM) },
            )
            SettingsDivider()
            SettingsRow(
                title = stringResource(R.string.appear_light),
                subtitle = stringResource(R.string.appear_light_sub),
                icon = Icons.Default.LightMode,
                iconTint = Color(0xFFFFB74D),
                control = SettingsControl.Value(
                    if (themeChoice == ThemeChoice.LIGHT) "On" else "",
                ),
                onClick = { settingsViewModel.setThemeChoice(ThemeChoice.LIGHT) },
            )
            SettingsDivider()
            SettingsRow(
                title = stringResource(R.string.appear_dark),
                subtitle = stringResource(R.string.appear_dark_sub),
                icon = Icons.Default.DarkMode,
                iconTint = Color(0xFF78909C),
                control = SettingsControl.Value(
                    if (themeChoice == ThemeChoice.DARK) "On" else "",
                ),
                onClick = { settingsViewModel.setThemeChoice(ThemeChoice.DARK) },
            )
            SettingsDivider()
            SettingsRow(
                title = "Language",
                subtitle = languageLabel(languageCode),
                icon = Icons.Default.Language,
                iconTint = Color(0xFF42A5F5),
                control = SettingsControl.Value(languageLabel(languageCode)),
                onClick = { showLanguageDialog = true },
            )
            SettingsDivider()
            SettingsToggle(
                title = stringResource(R.string.appear_sound),
                subtitle = stringResource(R.string.appear_sound_sub),
                icon = Icons.Default.VolumeUp,
                iconTint = Color(0xFF26C6DA),
                checked = soundEnabled,
                onCheckedChange = { settingsViewModel.toggleSound() },
            )
            SettingsDivider()
            SettingsToggle(
                title = stringResource(R.string.appear_haptics),
                subtitle = stringResource(R.string.appear_haptics_sub),
                icon = Icons.Default.Vibration,
                iconTint = Color(0xFF9575CD),
                checked = hapticsEnabled,
                onCheckedChange = { settingsViewModel.setHapticsEnabled(it) },
            )
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_label)) },
            text = {
                Column {
                    listOf("en" to "English", "es" to "Español", "de" to "Deutsch", "sw" to "Kiswahili")
                        .forEach { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        settingsViewModel.setLanguage(code)
                                        showLanguageDialog = false
                                        val intent = Intent(context, MainActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                        if (context is Activity) context.finish()
                                    }
                                    .padding(vertical = 12.dp),
                            ) {
                                Text(name)
                            }
                        }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun SettingsHabitsRemindersPage(
    settingsViewModel: SettingsViewModel,
    habitViewModel: HabitViewModel,
    onBack: () -> Unit,
) {
    val dailyOn by settingsViewModel.goalDailyReminderEnabled.collectAsState()
    val weeklyOn by settingsViewModel.goalWeeklyCheckInEnabled.collectAsState()
    val dailyTime by settingsViewModel.goalDailyReminderTime.collectAsState()
    var showHabitTime by remember { mutableStateOf(false) }
    var showGoalTime by remember { mutableStateOf(false) }

    val reminderHabits = habitViewModel.habits.filter { it.reminderEnabled && !it.isArchived }
    val habitTime = reminderHabits.mapNotNull { it.reminderTime }.minOrNull()

    SettingsPageScaffold(title = stringResource(R.string.settings_habits), onBack = onBack) {
        SettingsSection {
            SettingsRow(
                title = "Daily habit reminder",
                subtitle = when {
                    reminderHabits.isEmpty() -> "No habits have reminders yet"
                    habitTime != null -> "$habitTime · applies to habits with reminders on"
                    else -> "On for ${reminderHabits.size} habits"
                },
                icon = Icons.Default.Spa,
                iconTint = Color(0xFF66BB6A),
                control = SettingsControl.Value(habitTime ?: "Set"),
                onClick = { if (reminderHabits.isNotEmpty()) showHabitTime = true },
            )
            SettingsDivider()
            SettingsToggle(
                title = "Goal daily reminder",
                subtitle = "Motivation ping at $dailyTime",
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFF42A5F5),
                checked = dailyOn,
                onCheckedChange = { settingsViewModel.setGoalDailyReminderEnabled(it) },
            )
            SettingsDivider()
            SettingsRow(
                title = "Daily reminder time",
                subtitle = dailyTime,
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFF26C6DA),
                control = SettingsControl.Value(dailyTime),
                onClick = { showGoalTime = true },
            )
            SettingsDivider()
            SettingsToggle(
                title = "Weekly goal check-in",
                subtitle = "Sunday evening recap",
                icon = Icons.Default.Notifications,
                iconTint = Color(0xFF7E57C2),
                checked = weeklyOn,
                onCheckedChange = { settingsViewModel.setGoalWeeklyCheckInEnabled(it) },
            )
        }
    }

    if (showHabitTime) {
        ReminderTimeDialog(
            current = habitTime ?: "08:00",
            onDismiss = { showHabitTime = false },
            onPick = {
                habitViewModel.setReminderTimeForEnabledHabits(it)
                showHabitTime = false
            },
        )
    }
    if (showGoalTime) {
        ReminderTimeDialog(
            current = dailyTime,
            onDismiss = { showGoalTime = false },
            onPick = {
                settingsViewModel.setGoalDailyReminderTime(it)
                showGoalTime = false
            },
        )
    }
}

@Composable
fun SettingsFocusWidgetsPage(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
) {
    val resumeLast by settingsViewModel.resumeLastFocus.collectAsState()
    val ambientLast by settingsViewModel.ambientLastUsed.collectAsState()
    var showAmbientPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val ambientLabel = ambientSoundLabel(ambientLast)

    SettingsPageScaffold(title = stringResource(R.string.settings_focus), onBack = onBack) {
        SettingsSection {
            SettingsToggle(
                title = "Resume last Focus session",
                subtitle = if (ambientLast.isNullOrBlank()) "No last session yet" else "Restores $ambientLabel next time",
                icon = Icons.Default.SelfImprovement,
                iconTint = Color(0xFF26C6DA),
                checked = resumeLast,
                onCheckedChange = { settingsViewModel.setResumeLastFocus(it) },
            )
            SettingsDivider()
            SettingsRow(
                title = "Ambient sound default",
                subtitle = ambientLabel,
                icon = Icons.Default.VolumeUp,
                iconTint = Color(0xFF78909C),
                control = SettingsControl.Value(ambientLabel),
                onClick = { showAmbientPicker = true },
            )
            SettingsDivider()
            SettingsRow(
                title = "Add home screen widget",
                subtitle = "Pin today's habits to your home screen",
                icon = Icons.Default.Widgets,
                iconTint = Color(0xFF66BB6A),
                control = SettingsControl.Chevron,
                onClick = {
                    val pinned = requestPinHabitsWidget(context)
                    if (!pinned) {
                        onShowNotification(
                            "Long-press your home screen, then choose Widgets → Verdly.",
                            false,
                        )
                    }
                },
            )
        }
    }

    if (showAmbientPicker) {
        AlertDialog(
            onDismissRequest = { showAmbientPicker = false },
            title = { Text("Ambient sound", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    AmbientSoundManager.AmbientSound.entries.forEach { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setAmbientLastUsed(
                                        if (sound == AmbientSoundManager.AmbientSound.NONE) null else sound.name,
                                    )
                                    showAmbientPicker = false
                                }
                                .padding(vertical = 12.dp),
                        ) {
                            Text("${sound.emoji}  ${sound.label}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAmbientPicker = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
fun SettingsActivityPage(
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val shareLastActive by settingsViewModel.shareLastActive.collectAsState()
    SettingsPageScaffold(title = stringResource(R.string.settings_activity), onBack = onBack) {
        SettingsSection {
            SettingsToggle(
                title = stringResource(R.string.activity_show_last_active),
                subtitle = if (shareLastActive) {
                    stringResource(R.string.activity_show_last_active_on)
                } else {
                    stringResource(R.string.activity_show_last_active_off)
                },
                icon = Icons.Default.Sensors,
                iconTint = Color(0xFF2BD67B),
                checked = shareLastActive,
                onCheckedChange = { settingsViewModel.setShareLastActive(it) },
            )
        }
    }
}

@Composable
fun SettingsSocialPrivacyPage(
    settingsViewModel: SettingsViewModel,
    friendsViewModel: FriendsViewModel,
    onBack: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
    groveViewModel: GroveViewModel = viewModel(),
) {
    val showRecentProof by settingsViewModel.showRecentProof.collectAsState()
    val look by settingsViewModel.lookAtOpenGrove.collectAsState()
    val myPresence by groveViewModel.myPresence.collectAsState()
    val stand = myPresence?.standInOpenGrove == true
    val blockedSnapshot by friendsViewModel.blockedList.collectAsState()
    var showBlocked by remember { mutableStateOf(false) }
    var showReports by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { groveViewModel.refreshMe() }

    SettingsPageScaffold(title = stringResource(R.string.settings_social), onBack = onBack) {
        SettingsSection {
            SettingsToggle(
                title = "Show recent proof on profile",
                subtitle = if (showRecentProof) "Connections can see your latest proof reel" else "Hidden from connections",
                icon = Icons.Default.Visibility,
                iconTint = Color(0xFF5C6BC0),
                checked = showRecentProof,
                onCheckedChange = { settingsViewModel.setShowRecentProof(it) },
            )
            SettingsDivider()
            SettingsToggle(
                title = "Open Grove visibility",
                subtitle = if (look) "You're looking at the weekly board" else "The board stays off until you look",
                icon = Icons.Default.Park,
                iconTint = Color(0xFF66BB6A),
                checked = look,
                onCheckedChange = {
                    settingsViewModel.setLookAtOpenGrove(it)
                    groveViewModel.setLook(it)
                },
            )
            SettingsDivider()
            SettingsToggle(
                title = "Open Grove standing",
                subtitle = if (stand) "You stand on the public weekly board" else "You're not standing this week",
                icon = Icons.Default.Spa,
                iconTint = Color(0xFF52B788),
                checked = stand,
                onCheckedChange = { groveViewModel.setStand(it) },
            )
            SettingsDivider()
            SettingsSoonRow(
                title = "Who can nudge me",
                subtitle = "Everyone / Friends / Nobody. Beyond the existing cooldown",
                icon = Icons.Outlined.People,
                iconTint = Color(0xFF9575CD),
            )
            SettingsDivider()
            SettingsRow(
                title = "Blocked users",
                subtitle = when {
                    blockedSnapshot.error != null -> "Couldn't load. Tap to retry"
                    !blockedSnapshot.loaded -> "Loading…"
                    blockedSnapshot.users.isEmpty() -> "Nobody blocked"
                    else -> "${blockedSnapshot.users.size} blocked"
                },
                icon = Icons.Default.Block,
                iconTint = Color(0xFFE57373),
                control = SettingsControl.Chevron,
                onClick = { showBlocked = true },
            )
            SettingsDivider()
            SettingsRow(
                title = "Your reports",
                subtitle = "Reports you've submitted",
                icon = Icons.Default.Security,
                iconTint = Color(0xFF5C6BC0),
                control = SettingsControl.Chevron,
                onClick = { showReports = true },
            )
        }
    }

    if (showBlocked) {
        com.saintnico.verdlyhabits.ui.components.social.BlockedUsersSheet(
            friendsViewModel = friendsViewModel,
            onDismiss = { showBlocked = false },
            onShowNotification = onShowNotification,
        )
    }
    if (showReports) {
        com.saintnico.verdlyhabits.ui.components.social.MyReportsSheet(
            onDismiss = { showReports = false },
        )
    }
}

@Composable
fun SettingsDuoArenaPage(onBack: () -> Unit) {
    SettingsPageScaffold(title = stringResource(R.string.settings_duo), onBack = onBack) {
        SettingsSection {
            SettingsSoonRow(
                title = "Duo activity notifications",
                subtitle = "Once Duo has activity beyond invite/accept, this controls how noisy it is",
                icon = Icons.Outlined.People,
                iconTint = Color(0xFF42A5F5),
            )
            SettingsDivider()
            SettingsSoonRow(
                title = "Auto-join open challenges",
                subtitle = "Start Arena challenges without a manual tap",
                icon = Icons.Outlined.SportsScore,
                iconTint = Color(0xFFFFB74D),
            )
            SettingsDivider()
            SettingsSoonRow(
                title = "Leave current duo",
                subtitle = "Needs a confirmed duo-exit flow first",
                icon = Icons.AutoMirrored.Filled.Logout,
                iconTint = DangerRed,
            )
        }
    }
}

@Composable
fun SettingsSubscriptionCoinsPage(
    settingsViewModel: SettingsViewModel,
    billingViewModel: BillingViewModel,
    coinViewModel: CoinViewModel,
    onBack: () -> Unit,
    onManageSubscription: () -> Unit,
    onOpenGroveShop: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
) {
    val isPro by billingViewModel.isPro.collectAsState()
    val hasPaid by billingViewModel.hasPaidSubscription.collectAsState()
    val membership by billingViewModel.membershipTier.collectAsState()
    val balance by settingsViewModel.coinBalance.collectAsState()
    val status = when {
        hasPaid -> "Premium member"
        isPro -> "Premium access on this device"
        else -> membership.name.lowercase().replaceFirstChar { it.titlecase() }
    }

    SettingsPageScaffold(title = stringResource(R.string.settings_sub_coins), onBack = onBack) {
        SettingsSection {
            SettingsRow(
                title = "Premium status",
                subtitle = status,
                icon = Icons.Default.WorkspacePremium,
                iconTint = Color(0xFF52B788),
                control = SettingsControl.Value(status),
                onClick = onManageSubscription,
            )
            SettingsDivider()
            SettingsRow(
                title = "Manage subscription",
                subtitle = "Plans, trial, and Play Store billing",
                icon = Icons.Default.WorkspacePremium,
                iconTint = Color(0xFFFFB300),
                control = SettingsControl.Chevron,
                onClick = onManageSubscription,
            )
            SettingsDivider()
            SettingsRow(
                title = "Restore purchases",
                subtitle = "Sync your subscription or lifetime unlock from Google Play",
                icon = Icons.Default.Star,
                iconTint = Color(0xFFFFB74D),
                control = SettingsControl.Chevron,
                onClick = {
                    billingViewModel.restorePurchases()
                    onShowNotification("Checking Google Play for your purchases…", false)
                },
            )
            SettingsDivider()
            SettingsRow(
                title = "Coin balance",
                subtitle = "$balance coins",
                icon = Icons.Default.Spa,
                iconTint = Color(0xFF66BB6A),
                control = SettingsControl.Value("$balance"),
                onClick = {
                    coinViewModel.openCoinPacks(CoinPackReason.GROVE)
                    onOpenGroveShop()
                },
            )
        }
    }
}

@Composable
fun SettingsDataStoragePage(
    habitViewModel: HabitViewModel,
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
) {
    var showReset by remember { mutableStateOf(false) }
    val context = LocalContext.current

    SettingsPageScaffold(title = stringResource(R.string.settings_data), onBack = onBack) {
        SettingsSection {
            SettingsSoonRow(
                title = "Export my data",
                subtitle = "A real data export. Needs a background job",
                icon = Icons.Default.Storage,
                iconTint = Color(0xFF8D6E63),
            )
            SettingsDivider()
            SettingsSoonRow(
                title = "Clear image cache",
                subtitle = "Low priority, small storage win",
                icon = Icons.Default.Storage,
                iconTint = Color(0xFF78909C),
            )
            SettingsDivider()
            SettingsRow(
                title = "Reset all progress",
                subtitle = "Wipe all habits, levels, and cloud data",
                icon = Icons.Default.DeleteForever,
                iconTint = DangerRed,
                control = SettingsControl.Destructive,
                titleColor = DangerRed,
                onClick = { showReset = true },
            )
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text("Reset Everything?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete ALL your habits, levels, streaks, and your cloud profile. This cannot be undone.")
            },
            confirmButton = {
                com.saintnico.verdlyhabits.ui.components.VerdlyHoldToDeleteButton(
                    onConfirmed = {
                        showReset = false
                        habitViewModel.clearAllHabits()
                        settingsViewModel.hardReset()
                        onShowNotification("Data wiped. Starting fresh!", false)
                        onLogout()
                        val intent = Intent(context, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        if (context is Activity) context.finish()
                    },
                    label = "Hold to delete everything",
                    containerColor = DangerRed,
                )
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHelpLegalPage(onBack: () -> Unit) {
    val context = LocalContext.current
    var showAbout by remember { mutableStateOf(false) }

    SettingsPageScaffold(title = stringResource(R.string.settings_help), onBack = onBack) {
        SettingsSection {
            SettingsRow(
                title = "Rate Verdly",
                subtitle = stringResource(R.string.rate_verdly_desc),
                icon = Icons.Default.Star,
                iconTint = Color(0xFFFFB74D),
                control = SettingsControl.Chevron,
                onClick = { SettingsLegal.openPlayStoreListing(context) },
            )
            SettingsDivider()
            SettingsRow(
                title = "Privacy policy",
                subtitle = stringResource(R.string.privacy_policy_desc),
                icon = Icons.Default.Security,
                iconTint = Color(0xFF26A69A),
                control = SettingsControl.Chevron,
                onClick = { SettingsLegal.openUrl(context, SettingsLegal.PRIVACY_URL) },
            )
            SettingsDivider()
            SettingsSoonRow(
                title = "Terms of service",
                subtitle = "Needs the terms doc to exist first",
                icon = Icons.Outlined.Gavel,
                iconTint = Color(0xFF8D6E63),
            )
            SettingsDivider()
            SettingsSoonRow(
                title = "Help & support",
                subtitle = "Needs a support inbox or FAQ destination",
                icon = Icons.Outlined.HelpOutline,
                iconTint = Color(0xFF64B5F6),
            )
            SettingsDivider()
            SettingsSoonRow(
                title = "Send feedback",
                subtitle = "A write-to-us path, once we have an inbox",
                icon = Icons.Outlined.Feedback,
                iconTint = Color(0xFF9575CD),
            )
            SettingsDivider()
            SettingsRow(
                title = "About",
                subtitle = "Version ${BuildConfig.VERSION_NAME}",
                icon = Icons.Default.Info,
                iconTint = Color(0xFF64B5F6),
                control = SettingsControl.Chevron,
                onClick = { showAbout = true },
            )
        }
    }

    if (showAbout) {
        ModalBottomSheet(onDismissRequest = { showAbout = false }) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
                Text("Verdly", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Spacer(Modifier.height(12.dp))
                Text(
                    "A grove for habits you actually keep.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                )
                Spacer(Modifier.height(20.dp))
                Text("Made by Nico", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
fun SettingsAccountActionsPage(
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    var confirmLogout by remember { mutableStateOf(false) }
    val context = LocalContext.current

    SettingsPageScaffold(title = stringResource(R.string.settings_account), onBack = onBack) {
        SettingsSection {
            SettingsRow(
                title = "Delete account",
                subtitle = "Request permanent deletion of your account and data",
                icon = Icons.Default.DeleteForever,
                iconTint = DangerRed,
                control = SettingsControl.Chevron,
                titleColor = DangerRed,
                onClick = { SettingsLegal.openUrl(context, SettingsLegal.DELETE_ACCOUNT_URL) },
            )
            SettingsDivider()
            SettingsRow(
                title = stringResource(R.string.log_out),
                subtitle = stringResource(R.string.log_out_sub),
                icon = Icons.AutoMirrored.Filled.Logout,
                iconTint = DangerRed,
                control = SettingsControl.Destructive,
                titleColor = DangerRed,
                onClick = { confirmLogout = true },
            )
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.log_out_confirm), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.log_out_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmLogout = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                ) {
                    Text(stringResource(R.string.log_out), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ReminderTimeDialog(
    current: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    val times = listOf("07:00", "08:00", "12:00", "18:00", "20:30", "21:30")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder time", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                times.forEach { time ->
                    val selected = time == current
                    Text(
                        text = if (selected) "●  $time" else "○  $time",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(time) }
                            .padding(vertical = 10.dp),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun ambientSoundLabel(stored: String?): String {
    if (stored.isNullOrBlank()) return "None"
    return AmbientSoundManager.AmbientSound.entries
        .firstOrNull { it.name == stored || it.rawName == stored }
        ?.label
        ?: stored
}

private fun requestPinHabitsWidget(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
    val manager = AppWidgetManager.getInstance(context)
    if (!manager.isRequestPinAppWidgetSupported) return false
    val provider = ComponentName(context, HabitsWidgetReceiver::class.java)
    return runCatching { manager.requestPinAppWidget(provider, null, null) }.getOrDefault(false)
}
