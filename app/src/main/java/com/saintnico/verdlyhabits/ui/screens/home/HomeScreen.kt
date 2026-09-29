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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.saintnico.verdlyhabits.data.model.NudgeSituation
import com.saintnico.verdlyhabits.data.model.NudgeSurface
import com.saintnico.verdlyhabits.data.remote.storage.StorageRepository
import com.saintnico.verdlyhabits.domain.CompletionWindow
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.domain.HabitScheduling
import com.saintnico.verdlyhabits.ui.components.AnimatedHabitIcon
import com.saintnico.verdlyhabits.ui.components.BurstIntensity
import com.saintnico.verdlyhabits.ui.components.CelebrationKonfetti
import com.saintnico.verdlyhabits.ui.components.CompletionBurstOverlay
import com.saintnico.verdlyhabits.ui.components.HabitSwipeCard
import com.saintnico.verdlyhabits.ui.components.PremiumInsightCard
import com.saintnico.verdlyhabits.ui.components.StreakFlame
import com.saintnico.verdlyhabits.ui.components.home.HomeCategoryFilterRow
import com.saintnico.verdlyhabits.ui.components.home.HomeSectionHeader
import com.saintnico.verdlyhabits.ui.components.home.HomeTodayFocusCard
import com.saintnico.verdlyhabits.ui.components.home.HomeTodayHeroCard
import com.saintnico.verdlyhabits.ui.components.home.HomeTodayNoteCard
import com.saintnico.verdlyhabits.ui.components.home.HomeWeekPulseStrip
import com.saintnico.verdlyhabits.engine.HomeInsightEngine
import com.saintnico.verdlyhabits.engine.MotivationalEngine
import com.saintnico.verdlyhabits.engine.StatsEngine
import com.saintnico.verdlyhabits.engine.StatsInsightEngine
import com.saintnico.verdlyhabits.ui.models.toHabitColor
import com.saintnico.verdlyhabits.ui.theme.aboveSystemNav
import com.saintnico.verdlyhabits.ui.theme.cappedSp
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.HabitViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.ui.screens.goals.HomeGoalBanner
import com.saintnico.verdlyhabits.ui.components.notifications.NotificationBellButton
import com.saintnico.verdlyhabits.ui.components.home.HomeInviteWaveOverlay
import com.saintnico.verdlyhabits.ui.components.referral.ReferralPromoBanner
import com.saintnico.verdlyhabits.ui.components.social.ArenaShortcutChip
import com.saintnico.verdlyhabits.ui.components.social.NudgeSheetHost
import com.saintnico.verdlyhabits.ui.components.social.NudgeTarget
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
    onDismissReferralFeedBanner: () -> Unit = {},
    onNavigateToDuo: () -> Unit = {},
    duoNeedsAttention: Boolean = false,
    duoAtRisk: Boolean = false,
    notificationBadgeCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    coinViewModel: com.saintnico.verdlyhabits.ui.viewmodel.CoinViewModel? = null,
    groveViewModel: com.saintnico.verdlyhabits.ui.viewmodel.GroveViewModel? = null,
    friendsViewModel: com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel? = null,
    onNavigateToGrove: () -> Unit = {},
    onNavigateToConnect: () -> Unit = {},
    onNavigateToMemberProfile: (String) -> Unit = {},
    duoState: com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState? = null,
    proofLaunchToken: Int = 0,
    habitsFocusToken: Int = 0,
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
    val checkSkinId by (coinViewModel?.equippedCheckSkin
        ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    val equippedGlowId by (coinViewModel?.equippedTitleGlow
        ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    val equippedFrameId by (coinViewModel?.equippedProfileFrame
        ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    val equippedPlantId by (coinViewModel?.equippedGrovePlant
        ?: kotlinx.coroutines.flow.flowOf(null)).collectAsState(initial = null)
    val (skinIntensity, skinColors) = com.saintnico.verdlyhabits.ui.components.coins.checkSkinKonfetti(checkSkinId)
    val dismissedPillDay by store.trialPillDismissedDay.collectAsState(initial = null)
    val todayIso = java.time.LocalDate.now().toString()
    val showTrialPill = billingViewModel.isInFreeTrial() && !isPro && dismissedPillDay != todayIso
    var showWrapped by remember { mutableStateOf(false) }
    var showInviteWave by remember { mutableStateOf(false) }
    val wrappedWeekKey = remember { com.saintnico.verdlyhabits.util.WeeklyWrappedWeek.currentKey() }
    val lastHomeWrappedWeek by themePref.lastHomeWrappedWeek.collectAsState(initial = null)
    val showWrappedBanner = lastHomeWrappedWeek != wrappedWeekKey

    // ── Source of truth ────────────────────────────────────────────────────
    val habits = viewModel.habits
    val visible = habits.filter { !it.isArchived }
    val restingHabits = visible.filter { it.isPaused }
    val shelfHabits = habits.filter { it.isArchived }
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
    val activeGoals = if (goalsViewModel != null) {
        goalsViewModel.activeGoals.collectAsState().value
    } else {
        emptyList()
    }
    val quitGoalsToday = remember(activeGoals) {
        activeGoals.filter {
            it.goalType == com.saintnico.verdlyhabits.data.local.goals.GoalType.QUIT && !it.isArchived
        }
    }

    val statsState = userStatsViewModel?.state?.collectAsState()
    val xpToday = statsState?.value?.xpEarnedToday ?: 0
    val streakSnap by remember {
        com.saintnico.verdlyhabits.data.streak.StreakRepository.get(context)
    }.snapshot.collectAsState()
    val shieldCount = streakSnap.shields.available
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
    val insightReport = remember(habits, statsState?.value, dashboardMetrics) {
        StatsInsightEngine.analyze(
            habits = habits,
            metrics = dashboardMetrics,
            longestStreakEver = statsState?.value?.longestStreakEver ?: totalStreak,
            totalCompletions = statsState?.value?.totalCompletions ?: 0,
            totalXp = statsState?.value?.totalXp ?: 0,
            totalFocusMinutes = statsState?.value?.totalFocusMinutes ?: 0,
            today = todayDate,
        )
    }
    val todayNote = remember(insightReport) { HomeInsightEngine.todayNote(insightReport) }
    val openPremiumAnalytics: () -> Unit = { onNavigateToStats?.invoke() }

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
    var pendingCare by remember { mutableStateOf<Pair<HabitItem, com.saintnico.verdlyhabits.ui.components.care.CareCommitmentKind>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var insightDismissed by remember { mutableStateOf(false) }
    var pulseMember by remember { mutableStateOf<com.saintnico.verdlyhabits.ui.viewmodel.GroveMember?>(null) }
    var pendingOptionalProof by remember { mutableStateOf<HabitItem?>(null) }
    var nudgeTarget by remember { mutableStateOf<NudgeTarget?>(null) }

    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHabitForTimer by remember { mutableStateOf<HabitItem?>(null) }
    val timePickerState = rememberTimePickerState()

    var xpPopupText by remember { mutableStateOf("") }
    var showXpPopup by remember { mutableStateOf(false) }

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
                            checkSkinId = checkSkinId,
                            onXpFlash = { xpPopupText = "+$xp XP"; showXpPopup = true },
                            onClearFlash = { showXpPopup = false },
                            onKonfettiNormal = { konfettiTrigger = System.currentTimeMillis() },
                            onKonfettiHuge = { hugeKonfettiTrigger = System.currentTimeMillis() }
                        )
                        onShowNotification?.invoke("${habit.title} verified · +$xp XP", false, Icons.Rounded.Verified)
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

    val launchProofCamera: (HabitItem, String?) -> Unit = { habit, challengeId ->
        val granted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        habitPendingPhoto = habit
        activeChallengeIdForProof = challengeId
        if (!granted) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        } else {
            val imageFile = java.io.File(context.cacheDir, "images").also { it.mkdirs() }
            val file = java.io.File(imageFile, "${habit.id}_${System.currentTimeMillis()}.jpg")
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            uriToSave = uri
            try { cameraLauncher.launch(uri) }
            catch (_: SecurityException) {
                onShowNotification?.invoke("Camera permission is required.", true, Icons.Rounded.CameraAlt)
            }
        }
    }

    val plantWithoutPhoto: (HabitItem) -> Unit = plant@{ habit ->
        if (!habit.isCompleted) {
            if (habit.mustCompleteInsideWindow() && !habit.isWithinCommitmentWindow()) {
                onShowNotification?.invoke(
                    "${habit.title} counts only ${CompletionWindow.formatRange(habit.completionWindowStart, habit.completionWindowEnd)}.",
                    true,
                    Icons.Rounded.Schedule,
                )
                return@plant
            }
            viewModel.toggleHabitCompletion(habit.id)
            groveViewModel?.let { gv ->
                val friends = friendsViewModel?.friendSummaries?.value.orEmpty()
                val me = gv.myPresence.value
                gv.onIPlanted(
                    previousWeekly = me?.weeklyPlantDays ?: 0,
                    previousStreak = me?.currentStreakPublic ?: 0,
                    friends = friends,
                    myUsername = userUsername.ifBlank { userName },
                )
            }
            val newStreak = habit.streak + 1
            val withinWindow = habit.isWithinCommitmentWindow()
            val xp = habit.displayXpPreview(newStreak, withinWindow)
            registerCompletionFx(
                scope = scope,
                sound = sound, haptics = haptics,
                soundEnabled = soundEnabled, hapticsEnabled = hapticsEnabled,
                streak = newStreak,
                checkSkinId = checkSkinId,
                onXpFlash = { xpPopupText = "+$xp XP"; showXpPopup = true },
                onClearFlash = { showXpPopup = false },
                onKonfettiNormal = { konfettiTrigger = System.currentTimeMillis() },
                onKonfettiHuge = { hugeKonfettiTrigger = System.currentTimeMillis() }
            )
            onShowNotification?.invoke("${habit.title} · +$xp XP", false, Icons.Rounded.CheckCircle)

            if (!hasFullAccess && newStreak == 7) onRequestPaywall(PaywallTrigger.SevenDayStreak)
            else if (hasFullAccess && newStreak == 7) pendingStreakMilestone = habit

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

    val handleCompletion: (HabitItem) -> Unit = habitCompletion@{ habit ->
        val activeChallenge = challengeViewModel?.getActiveChallengeForHabit(habit.title)
        val isChallengeHabit = activeChallenge != null

        if (isChallengeHabit && !habit.isCompleted) {
            if (habit.mustCompleteInsideWindow() && !habit.isWithinCommitmentWindow()) {
                onShowNotification?.invoke(
                    "${habit.title} counts only ${CompletionWindow.formatRange(habit.completionWindowStart, habit.completionWindowEnd)}.",
                    true,
                    Icons.Rounded.Schedule,
                )
                return@habitCompletion
            }
            launchProofCamera(habit, activeChallenge?.id)
        } else if (!habit.isCompleted && habit.photoProofEnabled) {
            if (habit.mustCompleteInsideWindow() && !habit.isWithinCommitmentWindow()) {
                onShowNotification?.invoke(
                    "${habit.title} counts only ${CompletionWindow.formatRange(habit.completionWindowStart, habit.completionWindowEnd)}.",
                    true,
                    Icons.Rounded.Schedule,
                )
                return@habitCompletion
            }
            pendingOptionalProof = habit
        } else {
            plantWithoutPhoto(habit)
        }
    }

    val onSwipeSkip: (HabitItem) -> Unit = { habit ->
        haptics.tick(enabled = hapticsEnabled)
        onShowNotification?.invoke("${habit.title} skipped for today", false, Icons.Rounded.SkipNext)
    }

    val listState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }
    val friendSummaries = if (friendsViewModel != null) {
        friendsViewModel.friendSummaries.collectAsState().value
    } else {
        emptyList()
    }
    val blockedUids = if (friendsViewModel != null) {
        friendsViewModel.blockedUids.collectAsState().value
    } else {
        emptySet()
    }
    val myPresence = groveViewModel?.myPresence?.collectAsState()?.value?.let { mine ->
        mine.copy(
            titleGlowId = mine.titleGlowId ?: equippedGlowId,
            profileFrameId = mine.profileFrameId ?: equippedFrameId,
            grovePlantId = mine.grovePlantId ?: equippedPlantId,
        )
    }
    val witnessedToday = groveViewModel?.witnessedToday?.collectAsState()?.value.orEmpty()
    val groveMembers = remember(friendSummaries, myPresence) {
        groveViewModel?.membersFrom(friendSummaries).orEmpty()
    }
    val grovePulse = remember(groveMembers) { groveViewModel?.pulse(groveMembers) }
    val challengeUi = challengeViewModel?.state?.collectAsState()?.value
    val meUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val feedPosts = if (grovePulse != null) {
        rememberRankedHomeFeed(
            meUid = meUid,
            members = groveMembers,
            pulse = grovePulse,
            duo = duoState,
            challenges = challengeUi?.challenges.orEmpty(),
            blockedUids = blockedUids,
            today = todayDate,
        )
    } else {
        emptyList()
    }
    val socialWeekActive = remember(groveMembers, duoState, feedPosts) {
        val groveSocial = groveMembers.any { !it.isSelf && (it.weeklyPlantDays > 0 || it.witnessCount > 0) }
        val duoSocial = duoState?.let { d ->
            d.status == "active" && (d.buddyDoneToday || d.bothDoneToday || d.streakDays > 0)
        } == true
        val feedSocial = feedPosts.any { post ->
            post is com.saintnico.verdlyhabits.engine.home.HomeFeedPost.DuoMoment ||
                post is com.saintnico.verdlyhabits.engine.home.HomeFeedPost.ProofPhoto ||
                post is com.saintnico.verdlyhabits.engine.home.HomeFeedPost.Planted
        }
        groveSocial || duoSocial || feedSocial
    }
    val featuredGoal = activeGoals.firstOrNull { !it.isArchived }
    val extraQuitGoals = quitGoalsToday.filter { it.id != featuredGoal?.id }
    val completedTitlesToday = completed.map { it.title }.distinct()
    val trialDaysLeft = billingViewModel.freeTrialDaysRemaining()
    val proofReminder = challengeViewModel?.let { cvm ->
        visibleScheduledToday
            .mapNotNull { habit ->
                cvm.challengeProofReminderForHabit(habit.title, habit.isCompleted)?.let { ch -> habit to ch }
            }
            .firstOrNull()
    }
    val oathAtRisk = run {
        val stakes = coinViewModel?.activeStakes?.collectAsState()?.value.orEmpty()
        val stake = stakes.firstOrNull()
        val ch = stake?.let { s -> challengeUi?.challenges?.firstOrNull { it.id == s.challengeId } }
        val uid = meUid
        ch != null && uid.isNotBlank() && !ch.hasCompletedToday(uid, todayDate.toString())
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        groveViewModel?.refreshMe()
        friendsViewModel?.refreshFriendPresence()
    }

    androidx.compose.runtime.LaunchedEffect(proofLaunchToken) {
        if (proofLaunchToken <= 0) return@LaunchedEffect
        val reminder = proofReminder ?: return@LaunchedEffect
        launchProofCamera(reminder.first, reminder.second.id)
    }

    androidx.compose.runtime.LaunchedEffect(habitsFocusToken) {
        if (habitsFocusToken <= 0) return@LaunchedEffect
        kotlinx.coroutines.delay(80)
        var guard = 0
        while (guard++ < 16) {
            val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "today_section" }
            if (hit != null) {
                listState.animateScrollToItem(hit.index)
                return@LaunchedEffect
            }
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            if (total <= 0 || last >= total - 1) return@LaunchedEffect
            listState.scrollToItem((last + 3).coerceAtMost(total - 1))
        }
    }

    // ── Layout ─────────────────────────────────────────────────────────────
    val visibleAndFiltered = if (categoryFilter == null) visibleScheduledToday
    else visibleScheduledToday.filter { it.category == categoryFilter }
    val activeFiltered = visibleAndFiltered.filter { !it.isCompleted && !it.isPaused }
    val completedFiltered = visibleAndFiltered.filter { it.isCompleted }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showWrapped) {
            com.saintnico.verdlyhabits.ui.screens.stats.WrappedStoryScreen(
                onClose = {
                    showWrapped = false
                    scope.launch { themePref.markHomeWrappedSeenForWeek(wrappedWeekKey) }
                },
                socialWeekActive = socialWeekActive,
            )
        } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        ) { innerPadding ->
            val refresh: () -> Unit = {
                isRefreshing = true
                groveViewModel?.refreshMe()
                friendsViewModel?.refreshFriendPresence()
                scope.launch {
                    kotlinx.coroutines.delay(700)
                    isRefreshing = false
                }
            }
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = refresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "wordmark") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box {
                            Text(
                                "Verdly",
                                fontFamily = frauncesFamily,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 26.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.clickable {
                                    haptics.tick(hapticsEnabled)
                                    if (showInviteWave) {
                                        showInviteWave = false
                                        onOpenReferral()
                                    } else {
                                        showInviteWave = true
                                    }
                                },
                            )
                            if (showInviteWave) {
                                HomeInviteWaveOverlay(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 28.dp),
                                    onFinished = {
                                        showInviteWave = false
                                        haptics.tick(hapticsEnabled)
                                        onOpenReferral()
                                    },
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                haptics.tick(hapticsEnabled)
                                scope.launch { themePref.markHomeWrappedSeenForWeek(wrappedWeekKey) }
                                showWrapped = true
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Rounded.AutoStories,
                                contentDescription = stringResource(R.string.weekly_wrapped),
                                tint = if (showWrappedBanner) Color(0xFF52B788)
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        ShieldBadge(shieldCount)
                        NotificationBellButton(
                            unreadCount = notificationBadgeCount,
                            onClick = onOpenNotifications,
                        )
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.settings),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                            )
                        }
                    }
                    Text(
                        todayDate.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM")),
                        fontFamily = com.saintnico.verdlyhabits.ui.theme.dmSansFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    )
                }

                item(key = "stories") {
                    HomeStoriesRow(
                        me = myPresence,
                        friends = groveMembers.filter { !it.isSelf },
                        witnessedToday = witnessedToday,
                        pulseCopy = grovePulse?.copy ?: stringResource(R.string.invite_grove),
                        onSelfTap = {
                            pulseMember = myPresence ?: com.saintnico.verdlyhabits.ui.viewmodel.GroveMember(
                                uid = meUid,
                                username = displayName,
                                photoUrl = userPhotoUri,
                                membershipTier = com.saintnico.verdlyhabits.data.model.MembershipTier.FREE,
                                titleGlowId = equippedGlowId,
                                profileFrameId = equippedFrameId,
                                grovePlantId = equippedPlantId,
                                pinnedTrophyLabel = null,
                                plantedToday = completed.isNotEmpty(),
                                weeklyPlantDays = 0,
                                currentStreakPublic = totalStreak,
                                groveLead = false,
                                witnessCount = 0,
                                isSelf = true,
                                plantedCountToday = completed.size,
                            )
                        },
                        onFriendTap = { uid ->
                            pulseMember = groveMembers.firstOrNull { it.uid == uid }
                        },
                        onInviteTap = onNavigateToConnect,
                        viewerSharesLastActive = groveViewModel?.shareLastActive?.collectAsState()?.value != false,
                    )
                }

                if (referralState.showFeedBanner || referralState.showBanner) {
                    item(key = "invite_focus") {
                        ReferralPromoBanner(
                            qualifiedCount = referralState.qualifiedCount,
                            friendsRequired = referralState.friendsRequired,
                            rewardDays = referralState.rewardDays,
                            onOpen = onOpenReferral,
                            onDismiss = {
                                onDismissReferralFeedBanner()
                                onDismissReferralBanner()
                            },
                        )
                    }
                }

                if (visibleScheduledToday.isNotEmpty()) {
                    item(key = "today_stats") {
                        HomeTodayHeroCard(
                            completionRate = completionRate,
                            completedCount = completed.size,
                            totalCount = visibleScheduledToday.size,
                            totalStreak = totalStreak,
                            consistencyScore = dashboardMetrics.consistencyScore,
                            weekDeltaPct = dashboardMetrics.weekDeltaPct,
                            weekTrendUp = dashboardMetrics.weekTrendUp,
                            onTap = openPremiumAnalytics,
                            onStreak = openPremiumAnalytics,
                            onConsistency = openPremiumAnalytics,
                            onWeek = openPremiumAnalytics,
                        )
                    }
                    item(key = "today_note") {
                        HomeTodayNoteCard(note = todayNote, onOpen = openPremiumAnalytics)
                    }
                }

                if (visible.isNotEmpty()) {
                    item(key = "week_pulse") {
                        HomeWeekPulseStrip(
                            dailyPulse = dashboardMetrics.dailyPulse,
                            onTap = openPremiumAnalytics,
                        )
                    }
                }

                if (showWrappedBanner) {
                    item(key = "wrapped") {
                        androidx.compose.material3.Card(
                            onClick = {
                                scope.launch { themePref.markHomeWrappedSeenForWeek(wrappedWeekKey) }
                                showWrapped = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFF1B4332))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF52B788), modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.weekly_wrapped), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = Color(0xFF52B788))
                                    Text(stringResource(R.string.weekly_wrapped_ready), fontFamily = frauncesFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                if (featuredGoal != null && goalsViewModel != null && onNavigateToGoals != null) {
                    item(key = "featured_goal") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HomeGoalBanner(
                                goal = featuredGoal,
                                progress = goalsViewModel.progressFor(featuredGoal, visible),
                                metrics = goalsViewModel.metricsFor(featuredGoal, visible),
                                onClick = onNavigateToGoals,
                            )
                            if (featuredGoal.goalType == com.saintnico.verdlyhabits.data.local.goals.GoalType.QUIT) {
                                val metrics = goalsViewModel.metricsFor(featuredGoal, emptyList())
                                if (metrics.confirmationState !=
                                    com.saintnico.verdlyhabits.data.local.goals.ConfirmationState.UNCONFIRMED
                                ) {
                                    com.saintnico.verdlyhabits.ui.screens.goals.StillClearChip(
                                        confirmedToday = metrics.confirmedToday,
                                        accent = runCatching {
                                            Color(android.graphics.Color.parseColor(featuredGoal.colorHex))
                                        }.getOrDefault(MaterialTheme.colorScheme.primary),
                                        onConfirm = { goalsViewModel.confirmStillClear(featuredGoal.id) },
                                        compact = true,
                                    )
                                }
                            }
                        }
                    }
                }

                if (extraQuitGoals.isNotEmpty() && goalsViewModel != null) {
                    item(key = "quit_goals") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            extraQuitGoals.forEach { goal ->
                                val metrics = goalsViewModel.metricsFor(goal, emptyList())
                                if (metrics.confirmationState !=
                                    com.saintnico.verdlyhabits.data.local.goals.ConfirmationState.UNCONFIRMED
                                ) {
                                    val accent = runCatching {
                                        Color(android.graphics.Color.parseColor(goal.colorHex))
                                    }.getOrDefault(MaterialTheme.colorScheme.primary)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(accent.copy(alpha = 0.10f))
                                            .padding(12.dp),
                                    ) {
                                        Text(
                                            goal.title,
                                            fontFamily = frauncesFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        com.saintnico.verdlyhabits.ui.screens.goals.StillClearChip(
                                            confirmedToday = metrics.confirmedToday,
                                            accent = accent,
                                            onConfirm = { goalsViewModel.confirmStillClear(goal.id) },
                                            compact = true,
                                        )
                                    }
                                }
                            }
                        }
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
                            onOpenDetails = openPremiumAnalytics,
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
                    item(key = "insight") {
                        PremiumInsightCard(
                            insight = topInsight,
                            onDismiss = { insightDismissed = true },
                            onOpen = openPremiumAnalytics,
                        )
                    }
                }

                if (proofReminder != null) {
                    item(key = "promo") {
                        val (habit, challenge) = proofReminder
                        Card(
                            onClick = { onNavigateToChallenge?.invoke(challenge.id) },
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
                                com.saintnico.verdlyhabits.ui.components.VerdlyCameraFlashIcon(size = 26.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Post to ${challenge.habitName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${habit.title} is done. Add your arena proof so it counts.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                    )
                                }
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else if (showTrialPill && trialDaysLeft in 1..3) {
                    item(key = "promo") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(999.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.18f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Trial: $trialDaysLeft day${if (trialDaysLeft == 1) "" else "s"} left",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    "Upgrade",
                                    color = Color(0xFF52B788),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onRequestPaywall(PaywallTrigger.GoPro) },
                                )
                            }
                        }
                    }
                } else if (oathAtRisk && coinViewModel != null && challengeViewModel != null) {
                    item(key = "promo") {
                        HomeOathBanner(
                            coinViewModel = coinViewModel,
                            challengeViewModel = challengeViewModel,
                            onOpenChallenge = { id -> onNavigateToChallenge?.invoke(id) },
                        )
                    }
                }

                // ── Empty state (only after hydrate — prevents alien flash on cold start) ──
                if (!viewModel.habitsHydrated) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator(size = 40.dp)
                        }
                    }
                } else if (visible.isEmpty() && shelfHabits.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            com.saintnico.verdlyhabits.ui.components.VerdlyHeroLottie(
                                rawRes = com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog.HomeEmptyHero,
                                size = 200.dp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "It's quiet here",
                                fontFamily = frauncesFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Let's plant your first habit and give this grove something to grow.",
                                fontFamily = dmSansFamily,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                            Spacer(Modifier.height(20.dp))
                            Button(onClick = onNavigateToAddHabit) {
                                Text("Plant a habit", fontWeight = FontWeight.Bold)
                            }
                            if (friendsViewModel != null) {
                                Spacer(Modifier.height(10.dp))
                                TextButton(onClick = onNavigateToConnect) {
                                    Text("Invite friends")
                                }
                            }
                        }
                    }
                } else if (visibleScheduledToday.isEmpty() && restingHabits.isEmpty() && visible.isNotEmpty()) {
                    item {
                        Text(
                            "No habits scheduled for today. Edit a habit to change frequency, or come back on your next scheduled day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.padding(vertical = 24.dp, horizontal = 8.dp),
                        )
                    }
                } else {
                    item(key = "today_section") {
                        HomeSectionHeader(
                            title = "Today",
                            badge = "${activeFiltered.size} to go",
                            highlight = activeFiltered.isEmpty() && completedFiltered.isNotEmpty(),
                        )
                    }
                    items(activeFiltered, key = { it.id }) { habit ->
                        val isAtRisk = streakSnap.results[habit.id]?.isAtRisk == true
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
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            TextButton(
                                onClick = onNavigateToAddHabit,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Add habit",
                                    fontFamily = com.saintnico.verdlyhabits.ui.theme.dmSansFamily,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                )
                            }
                        }
                    }
                }

                if (feedPosts.isNotEmpty()) {
                    items(feedPosts, key = { it.id }) { post ->
                        val actor = post.actorUid
                        HomeFeedPostCard(
                            post = post,
                            witnessed = actor != null && actor in witnessedToday,
                            onOpenProfile = onNavigateToMemberProfile,
                            onOpenChallenge = { id -> onNavigateToChallenge?.invoke(id) },
                            onWitness = { uid ->
                                groveViewModel?.witness(uid) { ok, err ->
                                    if (!ok) onShowNotification?.invoke(err ?: "Couldn't witness", true, null)
                                }
                            },
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

                if (restingHabits.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = "Resting",
                            badge = "${restingHabits.size}",
                            highlight = false,
                        )
                    }
                    items(restingHabits, key = { "resting_${it.id}" }) { habit ->
                        RecoverableHabitRow(
                            habit = habit,
                            subtitle = habit.lifeNote?.ifBlank { null }
                                ?: "Paused. Reminders are off. Resume when you're ready.",
                            actionLabel = "Resume",
                            onAction = { viewModel.resumeHabit(habit.id) },
                            onOpen = { selectedHabitForActions = habit },
                        )
                    }
                }

                if (shelfHabits.isNotEmpty()) {
                    item {
                        HomeSectionHeader(
                            title = "On the shelf",
                            badge = "${shelfHabits.size}",
                            highlight = false,
                        )
                    }
                    items(shelfHabits, key = { "shelf_${it.id}" }) { habit ->
                        RecoverableHabitRow(
                            habit = habit,
                            subtitle = habit.lifeNote?.ifBlank { null }
                                ?: if (habit.endedAt != null) {
                                    "Ended, not erased. Restore anytime."
                                } else {
                                    "Shelved from Today. Your history is still here."
                                },
                            actionLabel = "Restore",
                            onAction = { viewModel.restoreHabit(habit.id) },
                            onOpen = { selectedHabitForActions = habit },
                        )
                    }
                }

                item { Spacer(Modifier.height(96.dp)) }
            }
            }
        }

        // Instant reward — check burst + confetti (dopamine on the tap itself)
        CompletionBurstOverlay(
            visible = showXpPopup,
            xpLabel = xpPopupText,
            headline = when {
                xpPopupText.contains("100") -> "Legend"
                else -> "Locked in"
            },
        )
        CelebrationKonfetti(
            trigger = konfettiTrigger,
            intensity = skinIntensity,
            colors = skinColors,
        )
        com.saintnico.verdlyhabits.ui.components.coins.CheckOffSpectacle(
            trigger = konfettiTrigger,
            skinId = checkSkinId,
            modifier = Modifier.fillMaxSize(),
        )
        CelebrationKonfetti(trigger = hugeKonfettiTrigger, intensity = BurstIntensity.HUGE)
    }

    // ── Grove pulse ────────────────────────────────────────────────────────
    val openPulse = pulseMember
    if (openPulse != null) {
        ModalBottomSheet(
            onDismissRequest = { pulseMember = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            GrovePulseSheet(
                member = myPresence?.takeIf { it.uid == openPulse.uid } ?: openPulse,
                witnessed = openPulse.uid in witnessedToday,
                completedToday = completedTitlesToday,
                onWitness = {
                    groveViewModel?.witness(openPulse.uid) { ok, err ->
                        if (!ok) onShowNotification?.invoke(err ?: "Couldn't witness", true, null)
                    }
                },
                onOpenProfile = {
                    pulseMember = null
                    onNavigateToMemberProfile(openPulse.uid)
                },
                onShareHabit = { title -> groveViewModel?.sharePulse(title) },
                onClearShare = { groveViewModel?.clearPulseShare() },
                viewerSharesLastActive = groveViewModel?.shareLastActive?.collectAsState()?.value != false,
                onNudge = if (!openPulse.isSelf) {
                    {
                        pulseMember = null
                        nudgeTarget = NudgeTarget(
                            uid = openPulse.uid,
                            username = openPulse.username,
                            photoUrl = openPulse.photoUrl,
                            surface = NudgeSurface.GROVE,
                            situation = if (openPulse.plantedToday) NudgeSituation.GENERAL else NudgeSituation.DUO_WAITING_ON_THEM,
                        )
                    }
                } else {
                    null
                },
            )
        }
    }

    val optionalProofHabit = pendingOptionalProof
    if (optionalProofHabit != null) {
        ModalBottomSheet(
            onDismissRequest = { pendingOptionalProof = null },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .aboveSystemNav()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            ) {
                Text(
                    "Add a photo?",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = cappedSp(20f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Optional. Snap a moment for ${optionalProofHabit.title}, or just plant. You never have to take a picture.",
                    fontFamily = dmSansFamily,
                    fontSize = cappedSp(14f),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                )
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        val habit = optionalProofHabit
                        pendingOptionalProof = null
                        launchProofCamera(habit, null)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    com.saintnico.verdlyhabits.ui.components.VerdlyCameraFlashIcon(size = 22.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Take a photo", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        val habit = optionalProofHabit
                        pendingOptionalProof = null
                        plantWithoutPhoto(habit)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Just plant it", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    NudgeSheetHost(
        target = nudgeTarget,
        onDismiss = { nudgeTarget = null },
        onSent = { onShowNotification?.invoke("Nudge sent", false, Icons.Rounded.Bolt) },
        onError = { onShowNotification?.invoke(it, true, null) },
    )

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
                ActionRow(Icons.Rounded.QueryStats, "See stats") {
                    scope.launch { sheetState.hide() }
                    selectedHabitForActions = null
                    openPremiumAnalytics()
                }
                ActionRow(Icons.Rounded.PushPin, if (habit.isFavoriteFocus) stringResource(R.string.unpin_focus) else stringResource(R.string.pin_focus)) {
                    viewModel.setTodayFocus(if (habit.isFavoriteFocus) null else habit.id)
                    scope.launch { sheetState.hide() }
                    selectedHabitForActions = null
                }
                ActionRow(Icons.Rounded.Timer, stringResource(R.string.focus_session)) {
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
                    if (habit.isPaused) stringResource(R.string.resume_habit) else stringResource(R.string.action_pause)
                ) {
                    if (habit.isPaused) {
                        viewModel.resumeHabit(habit.id)
                        scope.launch { sheetState.hide() }
                        selectedHabitForActions = null
                    } else {
                        pendingCare = habit to com.saintnico.verdlyhabits.ui.components.care.CareCommitmentKind.PAUSE_HABIT
                        scope.launch { sheetState.hide() }
                        selectedHabitForActions = null
                    }
                }
                ActionRow(Icons.Rounded.Inventory2, stringResource(R.string.action_archive)) {
                    pendingCare = habit to com.saintnico.verdlyhabits.ui.components.care.CareCommitmentKind.ARCHIVE_HABIT
                    scope.launch { sheetState.hide() }
                    selectedHabitForActions = null
                }
                ActionRow(Icons.Rounded.DeleteOutline, stringResource(R.string.action_delete), tint = Color(0xFFEF5350)) {
                    pendingCare = habit to com.saintnico.verdlyhabits.ui.components.care.CareCommitmentKind.END_HABIT
                    scope.launch { sheetState.hide() }
                    selectedHabitForActions = null
                }
            }
        }
    }

    pendingCare?.let { (habit, kind) ->
        com.saintnico.verdlyhabits.ui.components.care.CareCommitmentSheet(
            spec = com.saintnico.verdlyhabits.ui.components.care.CareCommitmentSpec(
                kind = kind,
                titleName = habit.title,
            ),
            onConfirm = { note ->
                when (kind) {
                    com.saintnico.verdlyhabits.ui.components.care.CareCommitmentKind.PAUSE_HABIT ->
                        viewModel.pauseHabit(habit.id, note)
                    com.saintnico.verdlyhabits.ui.components.care.CareCommitmentKind.ARCHIVE_HABIT ->
                        viewModel.archiveHabit(habit.id, note)
                    com.saintnico.verdlyhabits.ui.components.care.CareCommitmentKind.END_HABIT ->
                        viewModel.endHabit(habit.id, note)
                    com.saintnico.verdlyhabits.ui.components.care.CareCommitmentKind.END_GOAL -> Unit
                }
                pendingCare = null
            },
            onDismiss = { pendingCare = null },
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
            text = { Text(stringResource(R.string.invite_friends_grow)) },
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

@Composable
private fun RecoverableHabitRow(
    habit: HabitItem,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    onOpen: () -> Unit,
) {
    val color = habit.color.toHabitColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedHabitIcon(icon = habit.icon, color = color, size = 22.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(habit.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                maxLines = 2,
            )
        }
        TextButton(onClick = onAction) {
            Text(actionLabel, fontWeight = FontWeight.Bold)
        }
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
 * Instant reward on check-off: sound + haptic + XP burst + confetti in the same beat.
 */
private fun registerCompletionFx(
    scope: kotlinx.coroutines.CoroutineScope,
    sound: SoundEngine,
    haptics: com.saintnico.verdlyhabits.haptics.HapticsEngine,
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
    streak: Int,
    checkSkinId: String? = null,
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
            onKonfettiHuge()
        }
        else -> {
            // Every ordinary check-off still gets a real hit — not a meek tick.
            sound.playChime(SoundEngine.Chime.COMPLETE, soundEnabled)
            if (checkSkinId == "check_goldleaf" || checkSkinId == "check_mythic") {
                haptics.celebration(hapticsEnabled)
            } else {
                haptics.success(hapticsEnabled)
            }
            onKonfettiNormal()
        }
    }
    scope.launch {
        delay(if (isMilestone) 2000 else 1250)
        onClearFlash()
    }
}

@Composable
private fun HomeOathBanner(
    coinViewModel: com.saintnico.verdlyhabits.ui.viewmodel.CoinViewModel,
    challengeViewModel: com.saintnico.verdlyhabits.ui.viewmodel.ChallengeViewModel,
    onOpenChallenge: (String) -> Unit,
) {
    val stakes by coinViewModel.activeStakes.collectAsState()
    val chState by challengeViewModel.state.collectAsState()
    val oathChipId by coinViewModel.equippedOathChip.collectAsState()
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val todayIso = java.time.LocalDate.now().toString()
    val stake = stakes.firstOrNull() ?: return
    val challenge = chState.challenges.firstOrNull { it.id == stake.challengeId } ?: return
    val atRisk = uid.isNotBlank() && !challenge.hasCompletedToday(uid, todayIso)
    com.saintnico.verdlyhabits.ui.components.coins.HomeOathChip(
        amount = stake.stakedAmount,
        daysRemaining = challenge.daysRemaining(),
        atRisk = atRisk,
        chipSkinId = oathChipId,
        onClick = { onOpenChallenge(challenge.id) },
    )
}


