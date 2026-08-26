package com.saintnico.verdlyhabits

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.saintnico.verdlyhabits.navigation.resolveAppStartDestination
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.navigation.readNotificationLaunch
import com.saintnico.verdlyhabits.notifications.ProfileNudgeScheduler
import com.saintnico.verdlyhabits.notifications.ReminderScheduler
import com.saintnico.verdlyhabits.notifications.GoalReminderScheduler
import com.saintnico.verdlyhabits.ui.navigation.VerdlyNavGraph
import com.saintnico.verdlyhabits.widget.WidgetLaunchRequest
import com.saintnico.verdlyhabits.widget.WidgetNavigation
import com.saintnico.verdlyhabits.widget.readWidgetLaunch
import com.saintnico.verdlyhabits.ui.theme.VerdlyTheme
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val billingViewModel: BillingViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or denied — alarms still schedule; user can enable later in Settings */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        handleReferralIntent(intent)

        // Apply Locale
        val themePref = com.saintnico.verdlyhabits.preferences.ThemePreference(this)
        val languageCode = kotlinx.coroutines.runBlocking {
            themePref.languageCode.first()
        }
        val locale = java.util.Locale(languageCode)
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)

        val startRoute = resolveAppStartDestination(this)
        ProfileNudgeScheduler.schedule(this)
        ReminderScheduler.scheduleAll(this)
        GoalReminderScheduler.scheduleAll(this)
        com.saintnico.verdlyhabits.streak.DayChangeCoordinator.start(this)

        enableEdgeToEdge()
        setContent {
            val isDarkMode by settingsViewModel.isDarkMode.collectAsState()
            var widgetLaunch by remember { mutableStateOf(intent.readWidgetLaunch()) }
            var notificationLaunch by remember { mutableStateOf(intent.readNotificationLaunch()) }

            VerdlyTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VerdlyNavGraph(
                        startDestination = startRoute,
                        billingViewModel = billingViewModel,
                        widgetLaunch = widgetLaunch,
                        onWidgetLaunchConsumed = { widgetLaunch = null },
                        notificationLaunch = notificationLaunch,
                        onNotificationLaunchConsumed = { notificationLaunch = null },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.saintnico.verdlyhabits.streak.DayChangeCoordinator.onForeground(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReferralIntent(intent)
        if (intent.hasExtra(WidgetNavigation.EXTRA_ROUTE)) {
            recreate()
        }
        if (intent.readNotificationLaunch() != null) {
            recreate()
        }
    }

    private fun handleReferralIntent(intent: Intent?) {
        val code = extractReferralCode(intent?.data) ?: return
        ReferralManager.storePendingReferralCode(this, code)
        lifecycleScope.launch {
            try {
                val claimed = ReferralManager.handleReferralLink(this@MainActivity, code)
                if (claimed) {
                    billingViewModel.refreshBonusAccess()
                }
            } catch (_: Exception) {
                // Firestore may be unavailable — pending code stays for next sign-in
            }
        }
    }

    private fun extractReferralCode(uri: Uri?): String? {
        if (uri == null) return null
        return when {
            uri.scheme == "verdly" && uri.host == "referral" ->
                uri.getQueryParameter("code")
            uri.host.equals("verdly.app", ignoreCase = true) &&
                uri.pathSegments.firstOrNull()?.equals("r", ignoreCase = true) == true ->
                uri.pathSegments.getOrNull(1)
            else -> null
        }
    }
}
