@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saintnico.verdlyhabits.ui.screens.home

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.audio.SoundEngine
import com.saintnico.verdlyhabits.audio.rememberAppHaptics
import com.saintnico.verdlyhabits.audio.rememberAppSound
import com.saintnico.verdlyhabits.data.remote.storage.StorageRepository
import com.saintnico.verdlyhabits.domain.CompletionWindow
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitScheduling
import com.saintnico.verdlyhabits.ui.components.AnimatedHabitIcon
import com.saintnico.verdlyhabits.ui.components.BurstIntensity
import com.saintnico.verdlyhabits.ui.components.CelebrationKonfetti
import com.saintnico.verdlyhabits.ui.components.HabitSwipeCard
import com.saintnico.verdlyhabits.ui.components.MomentumBar
import com.saintnico.verdlyhabits.ui.components.PremiumInsightCard
import com.saintnico.verdlyhabits.ui.components.StreakFlame
import com.saintnico.verdlyhabits.ui.components.home.HomeCategoryFilterRow
import com.saintnico.verdlyhabits.ui.components.home.HomeSectionHeader
import com.saintnico.verdlyhabits.ui.components.home.HomeTodayFocusCard
import com.saintnico.verdlyhabits.ui.components.home.HomeTodayHeroCard
import com.saintnico.verdlyhabits.ui.components.home.HomeWeekPulseStrip
import com.saintnico.verdlyhabits.engine.MotivationalEngine
import com.saintnico.verdlyhabits.engine.StatsEngine
import com.saintnico.verdlyhabits.ui.models.toHabitColor
import com.saintnico.verdlyhabits.ui.viewmodel.HabitViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.ui.components.notifications.AnimatedSettingsButton
import com.saintnico.verdlyhabits.ui.components.notifications.NotificationBellButton
import com.saintnico.verdlyhabits.ui.components.referral.ReferralPromoBanner
import com.saintnico.verdlyhabits.ui.components.social.DuoHubButton
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState
import com.saintnico.verdlyhabits.monetization.PaywallTrigger
import com.saintnico.verdlyhabits.ui.components.share.shareStreakCardWithImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private fun HabitItem.hasCommitmentWindow(): Boolean =
    !completionWindowStart.isNullOrBlank() && !completionWindowEnd.isNullOrBlank()

private fun HabitItem.isWithinCommitmentWindow(now: LocalTime = LocalTime.now()): Boolean {
    if (!hasCommitmentWindow()) return true
    return CompletionWindow.isActiveNow(completionWindowStart, completionWindowEnd, now)
}

private fun HabitItem.mustCompleteInsideWindow(): Boolean =
    CompletionWindow.requiresWindow(difficulty) && hasCommitmentWindow()

private fun HabitItem.displayXpPreview(streakAfter: Int, withinWindow: Boolean): Int {
    var xp = difficulty.xp + minOf(streakAfter * 2, 30)
    if (hasCommitmentWindow() && withinWindow) {
        xp += CompletionWindow.onTimeBonusXp(difficulty)
    }
    return xp
}

@Composable
fun HomeScreen(
    viewModel: HabitViewModel,
    userStatsViewModel: UserStatsViewModel? = null,
    challengeViewModel: com.saintnico.verdlyhabits.ui.viewmodel.ChallengeViewModel? = null,
    billingViewModel: BillingViewModel,
    hasFullAccess: Boolean,
    onRequestPaywall: (PaywallTrigger) -> Unit,
    userName: String = "there",
    userUsername: String = "",
    onNavigateToAddHabit: () -> Unit,
    onNavigateToEditHabit: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFocus: ((String?) -> Unit)? = null,
    onNavigateToStats: (() -> Unit)? = null,
    onNavigateToChallenge: ((String) -> Unit)? = null,
    onShowNotification: ((String, Boolean, androidx.compose.ui.graphics.vector.ImageVector?) -> Unit)? = null,
    goalsViewModel: com.saintnico.verdlyhabits.ui.viewmodel.GoalsViewModel? = null,
    onNavigateToGoals: (() -> Unit)? = null,
    onNavigateToEditProfile: (() -> Unit)? = null,
    userPhotoUri: String? = null,
    userMotto: String = "",
    userBio: String = "",
    userFavoritePlant: String = "",
    referralState: ReferralUiState = ReferralUiState(),
    onOpenReferral: () -> Unit = {},
    onDismissReferralBanner: () -> Unit = {},
    onNavigateToDuo: () -> Unit = {},
    duoNeedsAttention: Boolean = false,
    duoAtRisk: Boolean = false,
    notificationBadgeCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sound = rememberAppSound()
    val haptics = rememberAppHaptics()

    // User prefs — drive whether SoundEngine/HapticsEngine fire feedback.
    val themePref = remember { com.saintnico.verdlyhabits.preferences.ThemePreference(context) }
    val store = remember { com.saintnico.verdlyhabits.data.local.AppDataStore(context) }
    val hapticsEnabled by themePref.isVibrationEnabled.collectAsState(initial = true)
    val soundEnabled by store.soundEnabled.collectAsState(initial = true)
    val isPro by billingViewModel.isPro.collectAsState()
    val dismissedPillDay by store.trialPillDismissedDay.collectAsState(initial = null)
    val todayIso = java.time.LocalDate.now().toString()
    val showTrialPill = billingViewModel.isInFreeTrial() && !isPro && dismissedPillDay != todayIso

    // ── Source of truth ────────────────────────────────────────────────────
    val habits = viewModel.habits
    val visible = habits.filter { !it.isArchived }
    val todayDate = LocalDate.now()
    val visibleScheduledToday = visible.filter {
        HabitScheduling.isDueOn(it.frequency, it.customDaysMask, todayDate)
    }
    val active = visibleScheduledToday.filter { !it.isCompleted && !it.isPaused }
    val completed = visibleScheduledToday.filter { it.isCompleted }
    val totalStreak = visible.sumOf { it.streak }
    val completionRate =
        if (visibleScheduledToday.isEmpty()) 0f
        else completed.size.toFloat() / visibleScheduledToday.size
    val pinned: HabitItem? = run {
        val fav = visible.firstOrNull { it.isFavoriteFocus }
        if (fav != null && !fav.isCompleted && !fav.isPaused &&
            HabitScheduling.isDueOn(fav.frequency, fav.customDaysMask, todayDate)
        ) {
            fav
        } else {
            highestPriority(active)
        }
    }

    val statsState = userStatsViewModel?.state?.collectAsState()
    val xpToday = statsState?.value?.xpEarnedToday ?: 0
    val shieldCount = statsState?.value?.streakShields ?: 0
    val dashboardMetrics = remember(habits, statsState?.value) {
        StatsEngine.computeDashboard(
            habits = habits,
            today = todayDate,
            totalFocusMinutes = statsState?.value?.totalFocusMinutes ?: 0,
            memberSinceMillis = statsState?.value?.memberSince ?: System.currentTimeMillis(),
        )
    }
    val topInsight = remember(habits, statsState?.value) {
        MotivationalEngine.topInsight(
            habits = habits,
            longestStreakEver = statsState?.value?.longestStreakEver ?: totalStreak,
            totalCompletions = statsState?.value?.totalCompletions ?: 0,
            totalXp = statsState?.value?.totalXp ?: 0,
        )
    }

    // ── Time / greeting ────────────────────────────────────────────────────
    val now = remember { LocalTime.now() }
    val (timeGreeting, greetingEmoji) = remember(now) {
        when (now.hour) {
            in 5..11  -> "Good morning" to "☕"
            in 12..16 -> "Good afternoon" to "☀️"
            in 17..21 -> "Good evening" to "🌙"
            else      -> "Late night" to "🦉"
        }
    }
    val moodCopy = remember(visible, completionRate, totalStreak) {
        when {
            visible.isEmpty()           -> "Plant your first habit to begin."
            completionRate >= 1f        -> "Day completed. You're untouchable."
            totalStreak >= 7            -> "Your streak is alive. Keep it that way."
            now.hour in 17..21          -> "Evening check-in time."
            now.hour in 5..11           -> "Set the tone of the day."
            else                        -> "One habit at a time."
        }
    }
    val displayName = if (userUsername.isNotBlank()) userUsername
                      else (if (userName.isBlank()) "there" else userName.split(" ").first())

    // ── Local UI state ─────────────────────────────────────────────────────
    var selectedHabitForActions by remember { mutableStateOf<HabitItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var insightDismissed by remember { mutableStateOf(false) }

    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHabitForTimer by remember { mutableStateOf<HabitItem?>(null) }
    val timePickerState = rememberTimePickerState()

    var xpPopupText by remember { mutableStateOf("") }
    var showXpPopup by remember { mutableStateOf(false) }
    val xpAlpha by animateFloatAsState(if (showXpPopup) 1f else 0f, tween(300), label = "xp_alpha")
    val xpOffset by animateFloatAsState(if (showXpPopup) -80f else 0f, tween(800), label = "xp_offset")

    var konfettiTrigger by remember { mutableStateOf<Long?>(null) }
    var hugeKonfettiTrigger by remember { mutableStateOf<Long?>(null) }
    var pendingStreakMilestone by remember { mutableStateOf<HabitItem?>(null) }
    var categoryFilter by remember { mutableStateOf<HabitCategory?>(null) }

    // ── Photo / challenge proof (preserved from original) ─────────────────
    var uriToSave by remember { mutableStateOf<android.net.Uri?>(null) }
    var habitPendingPhoto by remember { mutableStateOf<HabitItem?>(null) }
    var showSharePromptFor by remember { mutableStateOf<HabitItem?>(null) }
    var sharedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var activeChallengeIdForProof by remember { mutableStateOf<String?>(null) }
    val storageRepository = remember { StorageRepository() }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = uriToSave
        if (success && habitPendingPhoto != null && uri != null) {
            val habit = habitPendingPhoto!!
            val challengeId = activeChallengeIdForProof
            scope.launch {
                onShowNotification?.invoke("Uploading proof to the cloud…", false, Icons.Rounded.CloudUpload)
                val downloadUrl = storageRepository.uploadProofPhoto(context, habit.id, uri, challengeId)
                try {
                    context.contentResolver.delete(uri, null, null)
                    val file = java.io.File(uri.path ?: "")
                    if (file.exists()) file.delete()
                } catch (_: Exception) {}

                if (downloadUrl != null) {
                    viewModel.completeHabitWithProof(habit, downloadUrl)
                    if (challengeId != null) {
                        val todayStr = java.time.LocalDate.now()
                            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        val locClient = LocationServices.getFusedLocationProviderClient(context)
                        try {
                            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                                locClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                    .addOnSuccessListener { location ->
                                        challengeViewModel?.markCompletion(
                                            challengeId, todayStr, downloadUrl,
                                            location?.latitude, location?.longitude
                                        )
                                    }
                            } else {
                                challengeViewModel?.markCompletion(challengeId, todayStr, downloadUrl)
                            }
                        } catch (_: Exception) {
                            challengeViewModel?.markCompletion(challengeId, todayStr, downloadUrl)
                        }
                    }

                    if (!habit.isCompleted) {
                        if (habit.mustCompleteInsideWindow() && !habit.isWithinCommitmentWindow()) {
                            onShowNotification?.invoke(
                                "${habit.title} counts only ${CompletionWindow.formatRange(habit.completionWindowStart, habit.completionWindowEnd)}.",
                                true,
                                Icons.Rounded.Schedule,
                            )
                            habitPendingPhoto = null
                            uriToSave = null
                            return@launch
                        }
                        val withinWindow = habit.isWithinCommitmentWindow()
                        val xp = habit.displayXpPreview(habit.streak + 1, withinWindow)
                        registerCompletionFx(
                            scope = scope,
                            sound = sound, haptics = haptics,
                            soundEnabled = soundEnabled, hapticsEnabled = hapticsEnabled,
                            streak = habit.streak + 1,
                            onXpFlash = { xpPopupText = "+$xp XP"; showXpPopup = true },
                            onClearFlash = { showXpPopup = false },
                            onKonfettiNormal = { konfettiTrigger = System.currentTimeMillis() },
                            onKonfettiHuge = { hugeKonfettiTrigger = System.currentTimeMillis() }
                        )
                        onShowNotification?.invoke("${habit.title} verified · +$xp XP", false, Icons.Rounded.Verified)
                        userStatsViewModel?.onHabitCompleted(
                            streak = habit.streak + 1,
                            isPerfectDay = active.size == 1,
                            isFirstCompletion = habit.completedDates.isEmpty(),
                            habits = viewModel.habits.toList(),
                            difficulty = habit.difficulty,
                            withinCommitmentWindow = habit.hasCommitmentWindow() && withinWindow,
                        )
                        val ns = habit.streak + 1
                        if (!hasFullAccess && ns == 7) onRequestPaywall(PaywallTrigger.SevenDayStreak)
                        else if (hasFullAccess && ns == 7) pendingStreakMilestone = habit
                        sharedPhotoUri = uri
                        showSharePromptFor = habit
                    }
                } else {
                    onShowNotification?.invoke("Upload failed. Streak not verified.", true, Icons.Rounded.Error)
                }
            }
        }
        habitPendingPhoto = null
        uriToSave = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && habitPendingPhoto != null) {
            val habit = habitPendingPhoto!!
            val imageFile = java.io.File(context.cacheDir, "images").also { it.mkdirs() }
            val file = java.io.File(imageFile, "${habit.id}_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            uriToSave = uri
            try { cameraLauncher.launch(uri) }
            catch (_: SecurityException) {
                onShowNotification?.invoke("Camera permission was denied.", true, Icons.Rounded.CameraAlt)
            }
        } else if (!isGranted) {
            onShowNotification?.invoke("Camera permission is required to verify habits.", true, Icons.Rounded.CameraAlt)
        }
    }

    val handleCompletion: (HabitItem) -> Unit = { habit ->
        val activeChallenge = challengeViewModel?.getActiveChallengeForHabit(habit.title)
        val isChallengeHabit = activeChallenge != null

        if (isChallengeHabit && !habit.isCompleted) {
            if (habit.mustCompleteInsideWindow() && !habit.isWithinCommitmentWindow()) {
                onShowNotification?.invoke(
                    "${habit.title} counts only ${CompletionWindow.formatRange(habit.completionWindowStart, habit.completionWindowEnd)}.",
                    true,
                    Icons.Rounded.Schedule,
                )
                return@handleCompletion
            }
            val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) {
                habitPendingPhoto = habit
                activeChallengeIdForProof = activeChallenge?.id
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            } else {
                val imageFile = java.io.File(context.cacheDir, "images").also { it.mkdirs() }
                val file = java.io.File(imageFile, "${habit.id}_${System.currentTimeMillis()}.jpg")
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                uriToSave = uri
                habitPendingPhoto = habit
                activeChallengeIdForProof = activeChallenge?.id
                try { cameraLauncher.launch(uri) }
                catch (_: SecurityException) {
                    onShowNotification?.invoke("Camera permission is required.", true, Icons.Rounded.CameraAlt)
                }
            }
        } else {
            if (!habit.isCompleted) {
                if (habit.mustCompleteInsideWindow() && !habit.isWithinCommitmentWindow()) {
                    onShowNotification?.invoke(
                        "${habit.title} counts only ${CompletionWindow.formatRange(habit.completionWindowStart, habit.completionWindowEnd)}.",
                        true,
                        Icons.Rounded.Schedule,
                    )
                    return@handleCompletion
                }
                viewModel.toggleHabitCompletion(habit.id)
                val newStreak = habit.streak + 1
                val withinWindow = habit.isWithinCommitmentWindow()
                val xp = habit.displayXpPreview(newStreak, withinWindow)
                registerCompletionFx(
                    scope = scope,
                    sound = sound, haptics = haptics,
                    soundEnabled = soundEnabled, hapticsEnabled = hapticsEnabled,
                    streak = newStreak,
                    onXpFlash = { xpPopupText = "+$xp XP"; showXpPopup = true },
                    onClearFlash = { showXpPopup = false },
                    onKonfettiNormal = { konfettiTrigger = System.currentTimeMillis() },
                    onKonfettiHuge = { hugeKonfettiTrigger = System.currentTimeMillis() }
                )
                onShowNotification?.invoke("${habit.title} · +$xp XP", false, Icons.Rounded.CheckCircle)

                userStatsViewModel?.onHabitCompleted(
                    streak = newStreak,
                    isPerfectDay = active.size == 1,
                    isFirstCompletion = habit.completedDates.isEmpty(),
                    habits = viewModel.habits.toList(),
                    difficulty = habit.difficulty,
                    withinCommitmentWindow = habit.hasCommitmentWindow() && withinWindow,
                )
                if (!hasFullAccess && newStreak == 7) onRequestPaywall(PaywallTrigger.SevenDayStreak)
                else if (hasFullAccess && newStreak == 7) pendingStreakMilestone = habit

                // Habit stacking — fire follow-up notifications for chained habits
                val chained = viewModel.stackedAfter(habit.id)
                if (chained.isNotEmpty()) {
                    onShowNotification?.invoke(
                        "Stacked next: ${chained.first().title}",
                        false,
                        Icons.Rounded.Link
                    )
                }
            } else {
                viewModel.toggleHabitCompletion(habit.id)
            }
        }
    }

    val onSwipeSkip: (HabitItem) -> Unit = { habit ->
        haptics.tick(enabled = hapticsEnabled)
        onShowNotification?.invoke("${habit.title} skipped for today", false, Icons.Rounded.SkipNext)
    }

    // ── Layout ─────────────────────────────────────────────────────────────
    val visibleAndFiltered = if (categoryFilter == null) visibleScheduledToday
    else visibleScheduledToday.filter { it.category == categoryFilter }
    val activeFiltered = visibleAndFiltered.filter { !it.isCompleted && !it.isPaused }
    val completedFiltered = visibleAndFiltered.filter { it.isCompleted }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToAddHabit,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) { Icon(Icons.Rounded.Add, "Add Habit") }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Trial pill (dismissible for the rest of the day) ─────────────
                if (showTrialPill) {
                    item {
                        val days = billingViewModel.freeTrialDaysRemaining()
                        val pillColor = when (days) {
                            1 -> Color(0xFFE53935)
                            in 2..3 -> Color(0xFFFFB300)
                            else -> Color(0xFF2D6A4F)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(999.dp),
                            colors = CardDefaults.cardColors(containerColor = pillColor.copy(alpha = 0.18f)),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Pro Trial: $days day${if (days == 1) "" else "s"} left",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "Upgrade",
                                    color = Color(0xFF52B788),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.clickable { onRequestPaywall(PaywallTrigger.GoPro) },
                                )
                                IconButton(
                                    onClick = { scope.launch { store.dismissTrialPillForToday() } },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onBackground.copy(0.5f))
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                // ── Challenge proof reminder (habit done, arena proof missing) ──
                challengeViewModel?.let { cvm ->
                    val proofReminder = visibleScheduledToday
                        .mapNotNull { habit ->
                            cvm.challengeProofReminderForHabit(habit.title, habit.isCompleted)?.let { ch -> habit to ch }
                        }
                        .firstOrNull()
                    if (proofReminder != null) {
                        val (habit, challenge) = proofReminder
                        item {
                            Card(
                                onClick = {
                                    onNavigateToChallenge?.invoke(challenge.id)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        Icons.Rounded.CameraAlt,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Post to ${challenge.habitName}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        Text(
                                            "${habit.title} is done — add your arena proof so it counts.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                        )
                                    }
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Referral promo ───────────────────────────────────────────
                if (referralState.showBanner) {
                    item {
                        ReferralPromoBanner(
                            qualifiedCount = referralState.qualifiedCount,
                            friendsRequired = referralState.friendsRequired,
                            rewardDays = referralState.rewardDays,
                            onOpen = onOpenReferral,
                            onDismiss = onDismissReferralBanner,
                        )
                    }
                }

                // ── Profile completion nudge ─────────────────────────────────
                if (onNavigateToEditProfile != null) {
                    item {
                        com.saintnico.verdlyhabits.ui.screens.profile.ProfileCompletionNudgeBanner(
                            displayName = userName,
                            username = userUsername,
                            photoUrl = userPhotoUri,
                            motto = userMotto,
                            bio = userBio,
                            favoritePlant = userFavoritePlant,
                            onCompleteProfile = onNavigateToEditProfile,
                        )
                    }
                }

                // ── Greeting + momentum bar ────────────────────────────────
                item {
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "$timeGreeting, $displayName.",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    moodCopy,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                                )
                            }
                            ShieldBadge(shieldCount)
                            Spacer(Modifier.width(4.dp))
                            DuoHubButton(
                                onClick = onNavigateToDuo,
                                glowing = duoNeedsAttention,
                                atRisk = duoAtRisk,
                            )
                            Spacer(Modifier.width(4.dp))
                            NotificationBellButton(
                                unreadCount = notificationBadgeCount,
                                onClick = onOpenNotifications,
                            )
                            AnimatedSettingsButton(onClick = onNavigateToSettings)
                        }
                        if (visibleScheduledToday.isNotEmpty()) {
                            MomentumBar(progress = completionRate, xpToday = xpToday)
                        }
                    }
                }

                // ── Hero ring + streak summary ─────────────────────────────
                if (visibleScheduledToday.isNotEmpty()) {
                    item {
                        HomeTodayHeroCard(
                            completionRate = completionRate,
                            completedCount = completed.size,
                            totalCount = visibleScheduledToday.size,
                            totalStreak = totalStreak,
                            consistencyScore = dashboardMetrics.consistencyScore,
                            weekDeltaPct = dashboardMetrics.weekDeltaPct,
                            weekTrendUp = dashboardMetrics.weekTrendUp,
                            onTap = { onNavigateToStats?.invoke() },
                        )
                    }
                }

                // ── Today's Focus pinned card ──────────────────────────────
                if (pinned != null && !pinned.isCompleted) {
                    item {
                        HomeTodayFocusCard(
                            habit = pinned,
                            onTapComplete = { handleCompletion(pinned) },
                            onTapFocus = { onNavigateToFocus?.invoke(pinned.id) },
                            onEdit = { onNavigateToEditHabit(pinned.id) },
                        )
                    }
                }

                // ── Category filter chips ──────────────────────────────────
                if (visible.size >= 3) {
                    item {
                        HomeCategoryFilterRow(
                            selected = categoryFilter,
                            onSelect = { categoryFilter = if (categoryFilter == it) null else it },
                        )
                    }
                }

                // ── Insight card ───────────────────────────────────────────
                if (!insightDismissed && visible.isNotEmpty()) {
                    item {
                        PremiumInsightCard(
                            insight = topInsight,
                            onDismiss = { insightDismissed = true },
                        )
                    }
                }

                // ── Empty state ────────────────────────────────────────────
                if (visible.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.Eco, null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.start_planting),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else if (visibleScheduledToday.isEmpty()) {
                    item {
                        Text(
                            "No habits scheduled for today. Edit a habit to change frequency, or come back on your next scheduled day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp),
                        )
                    }
                } else {
                    item {
                        HomeSectionHeader(
                            title = "Today",
                            badge = "${activeFiltered.size} to go",
                            highlight = activeFiltered.isEmpty() && completedFiltered.isNotEmpty(),
                        )
                    }
                    items(activeFiltered, key = { it.id }) { habit ->
                        val isAtRisk = now.hour >= 21 && habit.streak > 7
                        HabitSwipeCard(
                            habit = habit,
                            isAtRisk = isAtRisk,
                            onTap = { handleCompletion(habit) },
                            onSwipeComplete = { handleCompletion(habit) },
                            onSwipeSkip = { onSwipeSkip(habit) },
                            onLongPress = { selectedHabitForActions = habit },
                            onFocusTap = {
                                selectedHabitForTimer = habit
                                showTimePicker = true
                            }
                        )
                    }
                }

                // ── 30-day strip ───────────────────────────────────────────
                if (visible.isNotEmpty()) {
                    item {
                        HomeWeekPulseStrip(
                            dailyPulse = dashboardMetrics.dailyPulse,
                            onTap = { onNavigateToStats?.invoke() },
                        )
                    }
                }

                // ── Completed section ──────────────────────────────────────
                if (completedFiltered.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = "Completed",
                            badge = "${completedFiltered.size}",
                            highlight = false,
                        )
                    }
                    items(completedFiltered, key = { it.id }) { habit ->
                        HabitSwipeCard(
                            habit = habit,
                            isAtRisk = false,
                            onTap = { viewModel.toggleHabitCompletion(habit.id) },
                            onSwipeComplete = { viewModel.toggleHabitCompletion(habit.id) },
                            onSwipeSkip = { viewModel.toggleHabitCompletion(habit.id) },
                            onLongPress = { selectedHabitForActions = habit },
                            onFocusTap = null
                        )
                    }
                }

                item { Spacer(Modifier.height(96.dp)) }
            }
        }

        // XP popup
        AnimatedVisibility(visible = showXpPopup, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    xpPopupText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.graphicsLayer {
                        alpha = xpAlpha; translationY = xpOffset
                    }
                )
            }
        }

        // Confetti overlays
        CelebrationKonfetti(trigger = konfettiTrigger, intensity = BurstIntensity.NORMAL)
        CelebrationKonfetti(trigger = hugeKonfettiTrigger, intensity = BurstIntensity.HUGE)
    }

    // ── Action sheet ───────────────────────────────────────────────────────
    if (selectedHabitForActions != null) {
        val habit = selectedHabitForActions!!
        ModalBottomSheet(
            onDismissRequest = { selectedHabitForActions = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 32.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sheetColor = habit.color.toHabitColor()
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(sheetColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedHabitIcon(
                            icon = habit.icon,
                            color = sheetColor,
                            size = 22.dp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(habit.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${habit.difficulty.displayName} · ${habit.category.displayName} · ${habit.streak}d",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ActionRow(Icons.Rounded.PushPin, if (habit.isFavoriteFocus) "Unpin from Today's Focus" else "Pin to Today's Focus") {
                    viewModel.setTodayFocus(if (habit.isFavoriteFocus) null else habit.id)
                    scope.launch { sheetState.hide() }
                    selectedHabitForActions = null
                }
                ActionRow(Icons.Rounded.Timer, "Focus session") {
                    onNavigateToFocus?.invoke(habit.id)
                    scope.launch { sheetState.hide() }
                    selectedHabitForActions = null
                }
                ActionRow(Icons.Rounded.Edit, stringResource(R.string.action_edit)) {
                    scope.launch { sheetState.hide() }
                    selectedHabitForActions = null
                    onNavigateToEditHabit(habit.id)
                }
                ActionRow(
                    if (habit.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    if (habit.isPaused) "Resume habit" else stringResource(R.string.action_pause)
                ) {
                    viewModel.pauseHabit(habit.id)
                    scope.launch { sheetState.hide() }
                    selectedHabitForActions = null
                }
                ActionRow(Icons.Rounded.Inventory2, stringResource(R.string.action_archive)) {
                    viewModel.archiveHabit(habit.id)
                    scope.launch { sheetState.hide() }
                    selectedHabitForActions = null
                }
                ActionRow(Icons.Rounded.DeleteOutline, stringResource(R.string.action_delete), tint = Color(0xFFEF5350)) {
                    showDeleteConfirm = true
                    scope.launch { sheetState.hide() }
                }
            }
        }
    }

    if (showDeleteConfirm && selectedHabitForActions != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; selectedHabitForActions = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body, selectedHabitForActions!!.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHabit(selectedHabitForActions!!.id)
                    showDeleteConfirm = false; selectedHabitForActions = null
                }) {
                    Text(stringResource(R.string.delete_confirm_button), color = Color(0xFFEF5350))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; selectedHabitForActions = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showTimePicker && selectedHabitForTimer != null) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    val timeStr = "%02d:%02d".format(hour, minute)
                    val updated = selectedHabitForTimer!!.copy(
                        reminderTime = timeStr,
                        reminderEnabled = true
                    )
                    viewModel.updateHabit(updated)
                    showTimePicker = false
                    onShowNotification?.invoke("Reminder for ${updated.title} set at $timeStr", false, updated.icon)
                }) { Text("Set reminder") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            title = { Text("Set Daily Reminder", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    if (showSharePromptFor != null && sharedPhotoUri != null) {
        val habit = showSharePromptFor!!
        AlertDialog(
            onDismissRequest = { showSharePromptFor = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StreakFlame(streak = habit.streak + 1, size = 24.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Share Your Victory", fontWeight = FontWeight.Bold)
                }
            },
            text = { Text("You crushed ${habit.title}. Flex your ${habit.streak + 1}-day streak.") },
            confirmButton = {
                Button(onClick = {
                    com.saintnico.verdlyhabits.utils.ShareUtils.shareHabitCompletion(
                        context = context,
                        sourceUri = sharedPhotoUri!!,
                        streak = habit.streak + 1,
                        habitName = habit.title,
                        level = userStatsViewModel?.state?.value?.level ?: 1
                    )
                    showSharePromptFor = null
                }) {
                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share proof")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSharePromptFor = null }) { Text("Not now") }
            }
        )
    }

    if (pendingStreakMilestone != null) {
        val habit = pendingStreakMilestone!!
        AlertDialog(
            onDismissRequest = { pendingStreakMilestone = null },
            title = { Text("Share your streak?", fontWeight = FontWeight.Bold) },
            text = { Text("Invite friends — you both grow with Verdly.") },
            confirmButton = {
                Button(onClick = {
                    pendingStreakMilestone = null
                    shareStreakCardWithImage(
                        context,
                        headline = "7-Day Streak",
                        habitName = habit.title,
                    )
                }) { Text("Share card") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStreakMilestone = null }) { Text("Not now") }
            },
        )
    }

    // Paywall is hosted at the navigation root (ModalBottomSheet).
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun ShieldBadge(count: Int) {
    if (count <= 0) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFFFB300).copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(Icons.Rounded.Shield, null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("$count", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB8860B))
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = LocalContentColor.current,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = tint)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────

private fun highestPriority(active: List<HabitItem>): HabitItem? {
    // Pick a sensible default for the Today's Focus card: longest current streak
    // (so the user is protecting their most valuable streak first), tie-break to
    // hardest difficulty.
    return active
        .filter { !it.isPaused }
        .maxWithOrNull(
            compareBy<HabitItem> { it.streak }
                .thenBy { it.difficulty.xp }
        )
}

/**
 * Centralised feedback for a completion. Runs sound, haptics, XP flash and the
 * right intensity of confetti. Keeps the call site small.
 */
private fun registerCompletionFx(
    scope: kotlinx.coroutines.CoroutineScope,
    sound: SoundEngine,
    haptics: com.saintnico.verdlyhabits.haptics.HapticsEngine,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    streak: Int,
    onXpFlash: () -> Unit,
    onClearFlash: () -> Unit,
    onKonfettiNormal: () -> Unit,
    onKonfettiHuge: () -> Unit
) {
    onXpFlash()
    val isMilestone = streak == 7 || streak == 30 || streak == 100
    when {
        streak == 100 -> {
            sound.playChime(SoundEngine.Chime.MILESTONE_100, soundEnabled)
            haptics.celebration(hapticsEnabled)
            onKonfettiHuge()
        }
        streak == 30 -> {
            sound.playChime(SoundEngine.Chime.MILESTONE_30, soundEnabled)
            haptics.celebration(hapticsEnabled)
            onKonfettiHuge()
        }
        streak == 7 -> {
            sound.playChime(SoundEngine.Chime.MILESTONE_7, soundEnabled)
            haptics.celebration(hapticsEnabled)
            onKonfettiNormal()
        }
        else -> {
            sound.playChime(SoundEngine.Chime.COMPLETE, soundEnabled)
            haptics.success(hapticsEnabled)
            onKonfettiNormal()
        }
    }
    scope.launch {
        delay(if (isMilestone) 2200 else 1400)
        onClearFlash()
    }
}


