package com.saintnico.verdlyhabits.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.viewmodel.compose.viewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.HabitViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import androidx.compose.runtime.collectAsState

import androidx.compose.ui.res.stringResource
import com.saintnico.verdlyhabits.R
import java.util.Locale
import android.content.Intent
import com.saintnico.verdlyhabits.MainActivity
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(),
    habitViewModel: HabitViewModel = viewModel(),
    billingViewModel: BillingViewModel = viewModel(),
) {
    val darkModeEnabled by settingsViewModel.isDarkMode.collectAsState()
    val notificationsEnabled by settingsViewModel.isNotificationsEnabled.collectAsState()
    val languageCode by settingsViewModel.languageCode.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            settingsViewModel.setNotificationsEnabled(true)
            onShowNotification("Notifications enabled! We'll keep you on track.", false)
        } else {
            settingsViewModel.setNotificationsEnabled(false)
            onShowNotification("Permission denied. Go to Settings → App Info to enable.", true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_label),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ──────────────────────────────────────────────────
            // ACCOUNT SECTION
            // ──────────────────────────────────────────────────
            SettingsSectionHeader(title = stringResource(R.string.account_label))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                // Profile Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToEditProfile() }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val userUsername by settingsViewModel.userUsername.collectAsState()
                    val userPhotoUri by settingsViewModel.userPhotoUri.collectAsState()

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userPhotoUri != null) {
                            coil.compose.AsyncImage(
                                model = userPhotoUri,
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = (userUsername.ifBlank { "U" }).firstOrNull()?.toString()?.uppercase() ?: "U",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userUsername.ifBlank { "UnknownRival" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Tap to edit profile",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                }

                SettingsDivider()

                // Logout Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLogout() }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE57373).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Log Out",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE57373)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ──────────────────────────────────────────────────
            // NOTIFICATIONS SECTION
            // ──────────────────────────────────────────────────
            SettingsSectionHeader(title = stringResource(R.string.notifications_label))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.animateContentSize()) {
                    // Push Notifications Toggle
                    SettingsToggleItem(
                        icon = Icons.Default.Notifications,
                        iconTint = Color(0xFF4CAF50),
                        title = stringResource(R.string.push_notifications),
                        subtitle = stringResource(R.string.push_notifications_desc),
                        isChecked = notificationsEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    settingsViewModel.setNotificationsEnabled(true)
                                    onShowNotification("Notifications enabled!", false)
                                }
                            } else {
                                settingsViewModel.setNotificationsEnabled(false)
                            }
                        }
                    )

                    SettingsDivider()

                    // Vibration Toggle
                    val vibrationEnabled by settingsViewModel.isVibrationEnabled.collectAsState()
                    SettingsToggleItem(
                        icon = Icons.Default.Vibration,
                        iconTint = Color(0xFF9575CD),
                        title = stringResource(R.string.vibration),
                        subtitle = stringResource(R.string.vibration_desc),
                        isChecked = vibrationEnabled,
                        onCheckedChange = { settingsViewModel.toggleVibration() }
                    )

                    SettingsDivider()

                    // Sound chime toggle
                    val soundEnabled by settingsViewModel.isSoundEnabled.collectAsState()
                    SettingsToggleItem(
                        icon = Icons.Default.VolumeUp,
                        iconTint = Color(0xFF26C6DA),
                        title = "Sound effects",
                        subtitle = "Completion chimes & milestone tones",
                        isChecked = soundEnabled,
                        onCheckedChange = { settingsViewModel.toggleSound() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ──────────────────────────────────────────────────
            // APPEARANCE SECTION
            // ──────────────────────────────────────────────────
            SettingsSectionHeader(title = stringResource(R.string.appearance_label))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.animateContentSize()) {
                    // Dark Mode
                    SettingsToggleItem(
                        icon = Icons.Default.DarkMode,
                        iconTint = Color(0xFF78909C),
                        title = stringResource(R.string.dark_mode_label),
                        subtitle = stringResource(R.string.dark_mode_desc),
                        isChecked = darkModeEnabled,
                        onCheckedChange = { settingsViewModel.toggleTheme() }
                    )

                    SettingsDivider()

                    // Language
                    val langMap = mapOf("en" to "English", "es" to "Español", "de" to "Deutsch", "sw" to "Kiswahili")
                    SettingsNavigationItem(
                        icon = Icons.Default.Language,
                        iconTint = Color(0xFF42A5F5),
                        title = stringResource(R.string.language_label),
                        subtitle = langMap[languageCode] ?: "English",
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ──────────────────────────────────────────────────
            // MORE SECTION
            // ──────────────────────────────────────────────────
            SettingsSectionHeader(title = stringResource(R.string.more_label))
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SettingsNavigationItem(
                        icon = Icons.Default.Star,
                        iconTint = Color(0xFFFFB74D),
                        title = stringResource(R.string.rate_verdly),
                        subtitle = stringResource(R.string.rate_verdly_desc),
                        onClick = { /* TODO: Rate app */ }
                    )

                    SettingsDivider()

                    SettingsNavigationItem(
                        icon = Icons.Default.Security,
                        iconTint = Color(0xFF26A69A),
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.privacy_policy_desc),
                        onClick = { /* TODO: Open privacy policy */ }
                    )

                    SettingsDivider()

                    SettingsNavigationItem(
                        icon = Icons.Default.Info,
                        iconTint = Color(0xFF64B5F6),
                        title = stringResource(R.string.about),
                        subtitle = "Version 1.0.0",
                        onClick = { /* TODO: About page */ }
                    )

                    SettingsDivider()

                    SettingsNavigationItem(
                        icon = Icons.Default.WorkspacePremium,
                        iconTint = Color(0xFF52B788),
                        title = "Restore purchases",
                        subtitle = "Sync your subscription or lifetime unlock from Google Play",
                        onClick = {
                            billingViewModel.restorePurchases()
                            onShowNotification("Checking Google Play for your purchases…", false)
                        }
                    )

                    SettingsDivider()

                    // Debug-only Pro toggle. Lets us preview the locked vs unlocked
                    // state of the app while billing is dormant. Will be removed once
                    // Play Billing is wired and PremiumGate.enforce is flipped to true.
                    val isPremium by settingsViewModel.isPremium.collectAsState()
                    SettingsToggleItem(
                        icon = Icons.Default.WorkspacePremium,
                        iconTint = Color(0xFFFFB300),
                        title = "Premium (debug)",
                        subtitle = if (isPremium) "Unlocked — testing premium UI"
                                   else "Locked — toggle to preview Pro features",
                        isChecked = isPremium,
                        onCheckedChange = { settingsViewModel.togglePremiumDebug() }
                    )

                    SettingsDivider()

                    val isProDebug by settingsViewModel.isProDebugEnabled.collectAsState()
                    SettingsToggleItem(
                        icon = Icons.Default.WorkspacePremium,
                        iconTint = Color(0xFF52B788),
                        title = "Pro (Debug)",
                        subtitle = "Toggle to see all trophies in the Hall",
                        isChecked = isProDebug,
                        onCheckedChange = { settingsViewModel.toggleProDebug() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ──────────────────────────────────────────────────
            // PRIVACY & SAFETY
            // ──────────────────────────────────────────────────
            SettingsSectionHeader(title = "Privacy & safety")
            Spacer(modifier = Modifier.height(8.dp))

            var showMyReports by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                SettingsNavigationItem(
                    icon = Icons.Default.Security,
                    iconTint = Color(0xFF5C6BC0),
                    title = "Your reports",
                    subtitle = "View reports you've submitted",
                    onClick = { showMyReports = true },
                )
            }

            if (showMyReports) {
                com.saintnico.verdlyhabits.ui.components.social.MyReportsSheet(
                    onDismiss = { showMyReports = false },
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ──────────────────────────────────────────────────
            // DANGER ZONE
            // ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE57373).copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                SettingsDivider()

                SettingsNavigationItem(
                    icon = Icons.Default.DeleteForever,
                    iconTint = Color(0xFFE57373),
                    title = "Reset All Progress",
                    subtitle = "Wipe all habits, levels, and cloud data",
                    onClick = { showResetDialog = true },
                    titleColor = Color(0xFFE57373)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Footer
            Text(
                text = "Made by Nico",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (showLanguageDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(stringResource(R.string.language_label)) },
            text = {
                Column {
                    val languages = listOf("en" to "English", "es" to "Español", "de" to "Deutsch", "sw" to "Kiswahili")
                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setLanguage(code)
                                    showLanguageDialog = false
                                    // Recreate activity to apply changes
                                    val intent = Intent(context, MainActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                    if (context is android.app.Activity) {
                                        context.finish()
                                    }
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showLanguageDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showResetDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Everything?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete ALL your habits, levels, streaks, and your cloud profile. This cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showResetDialog = false
                        habitViewModel.clearAllHabits()
                        settingsViewModel.hardReset() // Clears DataStore + Firestore doc
                        onShowNotification("Data wiped. Starting fresh!", false)
                        
                        onLogout()
                        
                        // Restart app or navigate back to start
                        val intent = Intent(context, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        if (context is android.app.Activity) context.finish()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Delete Everything", color = Color.White)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ──────────────────────────────────────────────────
// REUSABLE SETTINGS COMPONENTS
// ──────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = Color.White
            )
        )
    }
}

@Composable
private fun SettingsNavigationItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    titleColor: Color = Color.Unspecified
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (titleColor != Color.Unspecified) titleColor
                else MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    )
}
