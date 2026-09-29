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
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.saintnico.verdlyhabits.challenge.ChallengeInviteHelper
import com.saintnico.verdlyhabits.challenge.ChallengeInviteManager
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val settingsViewModel: SettingsViewModel by viewModels()
    private val billingViewModel: BillingViewModel by viewModels()
    private var presenceJob: Job? = null

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.saintnico.verdlyhabits.i18n.LocaleHelper.wrap(newBase))
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* granted or denied — alarms still schedule; user can enable later in Settings */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        handleReferralIntent(intent)
        if (handleChallengeInviteIntent(intent)) {
            // Fresh composition so NavGraph picks up pending join → Challenges tab.
        }

        val startRoute = resolveAppStartDestination(this)
        ProfileNudgeScheduler.schedule(this)
        ReminderScheduler.scheduleAll(this)
        GoalReminderScheduler.scheduleAll(this)
        com.saintnico.verdlyhabits.streak.DayChangeCoordinator.start(this)

        enableEdgeToEdge()
        setContent {
            val themeChoice by settingsViewModel.themeChoice.collectAsState()
            val systemDark = isSystemInDarkTheme()
            var widgetLaunch by remember { mutableStateOf(intent.readWidgetLaunch()) }
            var notificationLaunch by remember { mutableStateOf(intent.readNotificationLaunch()) }

            VerdlyTheme(darkTheme = themeChoice.resolvesDark(systemDark)) {
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

    override fun onStart() {
        super.onStart()
        presenceJob?.cancel()
        presenceJob = lifecycleScope.launch {
            com.saintnico.verdlyhabits.session.PresenceHeartbeat.ping(this@MainActivity, force = true)
            while (isActive) {
                delay(120_000)
                com.saintnico.verdlyhabits.session.PresenceHeartbeat.ping(this@MainActivity)
            }
        }
    }

    override fun onStop() {
        presenceJob?.cancel()
        presenceJob = null
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReferralIntent(intent)
        val challengeInvite = handleChallengeInviteIntent(intent)
        if (intent.hasExtra(WidgetNavigation.EXTRA_ROUTE)) {
            recreate()
        }
        if (intent.readNotificationLaunch() != null) {
            recreate()
        }
        if (challengeInvite) {
            recreate()
        }
    }

    /** @return true when a challenge join code was stored from the intent URI */
    private fun handleChallengeInviteIntent(intent: Intent?): Boolean {
        val input = extractChallengeJoinInput(intent?.data) ?: return false
        ChallengeInviteManager.storePendingJoin(this, input)
        return true
    }

    private fun extractChallengeJoinInput(uri: Uri?): String? {
        if (uri == null) return null
        return when {
            uri.scheme.equals("verdly", ignoreCase = true) &&
                uri.host.equals("challenge", ignoreCase = true) ->
                uri.getQueryParameter("code") ?: uri.getQueryParameter("join")
            uri.host.equals(ReferralManager.REFERRAL_WEB_HOST, ignoreCase = true) &&
                uri.pathSegments.firstOrNull()?.equals(ChallengeInviteHelper.WEB_PATH, ignoreCase = true) == true ->
                uri.pathSegments.getOrNull(1)
            else -> null
        }?.trim()?.takeIf { it.isNotEmpty() }
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
            uri.host.equals(ReferralManager.REFERRAL_WEB_HOST, ignoreCase = true) &&
                uri.pathSegments.firstOrNull()?.equals(ReferralManager.REFERRAL_WEB_PATH, ignoreCase = true) == true ->
                uri.pathSegments.getOrNull(1)
            uri.host.equals("verdly.app", ignoreCase = true) &&
                uri.pathSegments.firstOrNull()?.equals("r", ignoreCase = true) == true ->
                uri.pathSegments.getOrNull(1)
            else -> null
        }
    }
}
