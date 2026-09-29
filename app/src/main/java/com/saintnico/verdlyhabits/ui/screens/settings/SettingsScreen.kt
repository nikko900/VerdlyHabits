package com.saintnico.verdlyhabits.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.saintnico.verdlyhabits.BuildConfig
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.HabitViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.CoinViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
    friendsViewModel: FriendsViewModel,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotificationsInbox: () -> Unit = {},
    onNavigateToManageSubscription: () -> Unit = {},
    onOpenGroveShop: () -> Unit = {},
    startSection: String? = null,
    settingsViewModel: SettingsViewModel = viewModel(),
    habitViewModel: HabitViewModel = viewModel(),
    billingViewModel: BillingViewModel = viewModel(),
    coinViewModel: CoinViewModel = viewModel(),
) {
    val innerNav = rememberNavController()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            settingsViewModel.setNotificationsEnabled(true)
            onShowNotification("Notifications enabled! We'll keep you on track.", false)
        } else {
            settingsViewModel.setNotificationsEnabled(false)
            onShowNotification("Permission denied. Go to Settings → App Info to enable.", true)
        }
    }

    LaunchedEffect(startSection) {
        val dest = SettingsDest.fromSectionArg(startSection)
        if (dest != SettingsDest.Root) {
            innerNav.navigate(dest) { launchSingleTop = true }
        }
    }

    NavHost(
        navController = innerNav,
        startDestination = SettingsDest.Root,
    ) {
        composable(SettingsDest.Root) {
            SettingsHub(
                settingsViewModel = settingsViewModel,
                billingViewModel = billingViewModel,
                onBack = onBack,
                onOpenProfile = onNavigateToProfile,
                onOpenSection = { dest -> innerNav.navigate(dest) { launchSingleTop = true } },
            )
        }
        composable(SettingsDest.Notifications) {
            SettingsNotificationsPage(
                settingsViewModel = settingsViewModel,
                onBack = { innerNav.popBackStack() },
                onRequestPushPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        settingsViewModel.setNotificationsEnabled(true)
                        onShowNotification("Notifications enabled!", false)
                    }
                },
                onOpenInbox = onNavigateToNotificationsInbox,
            )
        }
        composable(SettingsDest.Appearance) {
            SettingsAppearancePage(
                settingsViewModel = settingsViewModel,
                onBack = { innerNav.popBackStack() },
            )
        }
        composable(SettingsDest.HabitsReminders) {
            SettingsHabitsRemindersPage(
                settingsViewModel = settingsViewModel,
                habitViewModel = habitViewModel,
                onBack = { innerNav.popBackStack() },
            )
        }
        composable(SettingsDest.FocusWidgets) {
            SettingsFocusWidgetsPage(
                settingsViewModel = settingsViewModel,
                onBack = { innerNav.popBackStack() },
                onShowNotification = onShowNotification,
            )
        }
        composable(SettingsDest.SocialPrivacy) {
            SettingsSocialPrivacyPage(
                settingsViewModel = settingsViewModel,
                friendsViewModel = friendsViewModel,
                onBack = { innerNav.popBackStack() },
                onShowNotification = onShowNotification,
            )
        }
        composable(SettingsDest.Activity) {
            SettingsActivityPage(
                settingsViewModel = settingsViewModel,
                onBack = { innerNav.popBackStack() },
            )
        }
        composable(SettingsDest.DuoArena) {
            SettingsDuoArenaPage(onBack = { innerNav.popBackStack() })
        }
        composable(SettingsDest.SubscriptionCoins) {
            SettingsSubscriptionCoinsPage(
                settingsViewModel = settingsViewModel,
                billingViewModel = billingViewModel,
                coinViewModel = coinViewModel,
                onBack = { innerNav.popBackStack() },
                onManageSubscription = onNavigateToManageSubscription,
                onOpenGroveShop = onOpenGroveShop,
                onShowNotification = onShowNotification,
            )
        }
        composable(SettingsDest.DataStorage) {
            SettingsDataStoragePage(
                habitViewModel = habitViewModel,
                settingsViewModel = settingsViewModel,
                onBack = { innerNav.popBackStack() },
                onLogout = onLogout,
                onShowNotification = onShowNotification,
            )
        }
        composable(SettingsDest.HelpLegal) {
            SettingsHelpLegalPage(onBack = { innerNav.popBackStack() })
        }
        composable(SettingsDest.AccountActions) {
            SettingsAccountActionsPage(
                onBack = { innerNav.popBackStack() },
                onLogout = onLogout,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsHub(
    settingsViewModel: SettingsViewModel,
    billingViewModel: BillingViewModel,
    onBack: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSection: (String) -> Unit,
) {
    val userUsername by settingsViewModel.userUsername.collectAsState()
    val userPhotoUri by settingsViewModel.userPhotoUri.collectAsState()
    val notificationsEnabled by settingsViewModel.isNotificationsEnabled.collectAsState()
    val themeChoice by settingsViewModel.themeChoice.collectAsState()
    val languageCode by settingsViewModel.languageCode.collectAsState()
    val soundEnabled by settingsViewModel.isSoundEnabled.collectAsState()
    val isPro by billingViewModel.isPro.collectAsState()
    var showDebug by remember { mutableStateOf(false) }

    val langName = languageLabel(languageCode)
    val appearanceSubtitle = buildString {
        append(
            when (themeChoice) {
                com.saintnico.verdlyhabits.preferences.ThemeChoice.SYSTEM ->
                    stringResource(R.string.appearance_system)
                com.saintnico.verdlyhabits.preferences.ThemeChoice.DARK ->
                    stringResource(R.string.appearance_dark)
                com.saintnico.verdlyhabits.preferences.ThemeChoice.LIGHT ->
                    stringResource(R.string.appearance_light)
            },
        )
        append(" · ")
        append(langName)
        append(" · ")
        append(stringResource(R.string.sound_dot, stringResource(if (soundEnabled) R.string.sound_on else R.string.sound_off)))
    }

    SettingsPageScaffold(title = stringResource(R.string.settings_label), onBack = onBack) {
        Spacer(Modifier.height(4.dp))
        SettingsSection {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenProfile)
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!userPhotoUri.isNullOrBlank()) {
                        com.saintnico.verdlyhabits.ui.components.ProfileAvatar(
                            photoUri = userPhotoUri,
                            size = 52.dp,
                            contentDescription = "Profile Picture",
                            fallbackTint = MaterialTheme.colorScheme.primary,
                            fallbackBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                    } else {
                        Text(
                            text = (userUsername.ifBlank { "U" }).firstOrNull()?.toString()?.uppercase() ?: "U",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userUsername.ifBlank { "UnknownRival" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.view_profile),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    )
                }
                androidx.compose.material3.Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        SettingsSection(title = stringResource(R.string.settings_controls)) {
            HubRow(
                icon = Icons.Default.Notifications,
                tint = Color(0xFF4CAF50),
                title = stringResource(R.string.settings_notifications),
                subtitle = if (notificationsEnabled) stringResource(R.string.settings_notifications_on) else stringResource(R.string.settings_notifications_off),
                onClick = { onOpenSection(SettingsDest.Notifications) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.DarkMode,
                tint = Color(0xFF78909C),
                title = stringResource(R.string.settings_appearance),
                subtitle = appearanceSubtitle,
                onClick = { onOpenSection(SettingsDest.Appearance) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.Spa,
                tint = Color(0xFF66BB6A),
                title = stringResource(R.string.settings_habits),
                subtitle = stringResource(R.string.settings_habits_sub),
                onClick = { onOpenSection(SettingsDest.HabitsReminders) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.SelfImprovement,
                tint = Color(0xFF26C6DA),
                title = stringResource(R.string.settings_focus),
                subtitle = stringResource(R.string.settings_focus_sub),
                onClick = { onOpenSection(SettingsDest.FocusWidgets) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.PrivacyTip,
                tint = Color(0xFF5C6BC0),
                title = stringResource(R.string.settings_social),
                subtitle = stringResource(R.string.settings_social_sub),
                onClick = { onOpenSection(SettingsDest.SocialPrivacy) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.Sensors,
                tint = Color(0xFF2BD67B),
                title = stringResource(R.string.settings_activity),
                subtitle = stringResource(R.string.settings_activity_sub),
                onClick = { onOpenSection(SettingsDest.Activity) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.SportsScore,
                tint = Color(0xFFFFB74D),
                title = stringResource(R.string.settings_duo),
                subtitle = stringResource(R.string.settings_duo_sub),
                onClick = { onOpenSection(SettingsDest.DuoArena) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.WorkspacePremium,
                tint = Color(0xFF52B788),
                title = stringResource(R.string.settings_sub_coins),
                subtitle = stringResource(R.string.settings_sub_coins_sub),
                onClick = { onOpenSection(SettingsDest.SubscriptionCoins) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.Storage,
                tint = Color(0xFF8D6E63),
                title = stringResource(R.string.settings_data),
                subtitle = stringResource(R.string.settings_data_sub),
                onClick = { onOpenSection(SettingsDest.DataStorage) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.HelpOutline,
                tint = Color(0xFF64B5F6),
                title = stringResource(R.string.settings_help),
                subtitle = stringResource(R.string.settings_help_sub),
                onClick = { onOpenSection(SettingsDest.HelpLegal) },
            )
            SettingsDivider()
            HubRow(
                icon = Icons.Default.ManageAccounts,
                tint = Color(0xFFE57373),
                title = stringResource(R.string.settings_account),
                subtitle = stringResource(R.string.settings_account_sub),
                onClick = { onOpenSection(SettingsDest.AccountActions) },
            )
        }

        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.made_by_nico),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (BuildConfig.DEBUG) {
                        Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { showDebug = !showDebug },
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(bottom = 8.dp),
        )

        if (BuildConfig.DEBUG && showDebug) {
            Spacer(Modifier.height(12.dp))
            SettingsDebugPanel(settingsViewModel)
        }
    }
}

@Composable
private fun HubRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = tint,
        control = SettingsControl.Chevron,
        onClick = onClick,
    )
}

@Composable
private fun SettingsDebugPanel(settingsViewModel: SettingsViewModel) {
    val isPremium by settingsViewModel.isPremium.collectAsState()
    val isProDebug by settingsViewModel.isProDebugEnabled.collectAsState()
    SettingsSection(title = "Internal") {
        SettingsToggle(
            title = "Premium (debug)",
            subtitle = if (isPremium) "Unlocked. Testing premium UI" else "Locked. Preview Premium",
            icon = Icons.Default.WorkspacePremium,
            iconTint = Color(0xFFFFB300),
            checked = isPremium,
            onCheckedChange = { settingsViewModel.togglePremiumDebug() },
        )
        SettingsDivider()
        SettingsToggle(
            title = "Premium trophies (debug)",
            subtitle = "Show all trophies in the Hall",
            icon = Icons.Default.WorkspacePremium,
            iconTint = Color(0xFF52B788),
            checked = isProDebug,
            onCheckedChange = { settingsViewModel.toggleProDebug() },
        )
    }
}

internal fun languageLabel(code: String): String = when (code) {
    "es" -> "Español"
    "de" -> "Deutsch"
    "sw" -> "Kiswahili"
    else -> "English"
}
