package com.saintnico.verdlyhabits.ui.screens.challenge

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.ui.viewmodel.ChallengeViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.CoinViewModel
import com.saintnico.verdlyhabits.ui.components.coins.CoinBalanceChip
import com.saintnico.verdlyhabits.ui.components.coins.CoinBurstAnimation
import com.saintnico.verdlyhabits.ui.components.coins.CoinPackSheet
import com.saintnico.verdlyhabits.ui.components.coins.GiftArcAnimation
import com.saintnico.verdlyhabits.ui.components.coins.GiftSupportSheet
import com.saintnico.verdlyhabits.ui.components.coins.StakeConfirmSheet
import com.saintnico.verdlyhabits.ui.components.coins.StakeConsistencyCard
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.data.remote.storage.StorageRepository
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.ChallengeMode
import com.saintnico.verdlyhabits.data.model.ProofWindowState
import com.saintnico.verdlyhabits.data.model.ReactionWeights
import com.saintnico.verdlyhabits.data.remote.firestore.CreatedChallenge
import com.saintnico.verdlyhabits.data.remote.firestore.JoinChallengeOutcome
import com.saintnico.verdlyhabits.challenge.ChallengeInviteHelper
import com.saintnico.verdlyhabits.challenge.ChallengeInviteManager
import com.saintnico.verdlyhabits.ui.components.challenge.ChallengeInvitePreviewCard
import com.saintnico.verdlyhabits.ui.components.share.shareChallengeInviteCard
import com.saintnico.verdlyhabits.ui.theme.DangerRed
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import com.saintnico.verdlyhabits.ui.theme.SilverColor
import com.saintnico.verdlyhabits.ui.theme.BronzeColor
import com.saintnico.verdlyhabits.ui.theme.StakeAmber
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.theme.SuccessGreen
import com.saintnico.verdlyhabits.ui.theme.XPPurple
import com.saintnico.verdlyhabits.ui.viewmodel.ProofState
import java.io.File

private data class CreateDeadlineOption(
    val key: String,
    val title: String,
    val subtitle: String,
    val hint: String,
    val icon: ImageVector
)

private data class CreateChallengeModeOption(
    val mode: ChallengeMode,
    val title: String,
    val description: String,
    val icon: ImageVector,
)

private data class CreatedChallengeShareInfo(
    val created: CreatedChallenge,
    val habitName: String,
    val stake: String,
    val durationDays: Int
)

private fun proofReactionKey(userId: String, date: String): String = "${userId}_$date"

private fun reactionCountsFor(challenge: Challenge, proofKey: String): Map<String, Int> =
    challenge.reactions[proofKey]
        ?.values
        ?.flatten()
        ?.groupingBy { it }
        ?.eachCount()
        .orEmpty()

private fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / 60_000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        else -> "${mins / 1440}d ago"
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChallengeScreen(
    viewModel: ChallengeViewModel,
    onShowNotification: ((String, Boolean) -> Unit)? = null,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToMemberProfile: (String) -> Unit = {},
    settingsViewModel: SettingsViewModel = viewModel(),
    friendsViewModel: FriendsViewModel = viewModel(),
    coinViewModel: CoinViewModel = viewModel(),
    billingViewModel: BillingViewModel,
) {
    val connections by friendsViewModel.friendSummaries.collectAsState()
    val state by viewModel.state.collectAsState()
    val pendingOpenChallengeId by viewModel.pendingOpenChallengeId.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var selectedChallenge by remember { mutableStateOf<Challenge?>(null) }
    var createdChallenge by remember { mutableStateOf<CreatedChallenge?>(null) }
    var createdChallengeShareInfo by remember { mutableStateOf<CreatedChallengeShareInfo?>(null) }
    var pendingCreateShareDraft by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    var autoOpenAfterJoin by remember { mutableStateOf(false) }

    LaunchedEffect(pendingOpenChallengeId, state.isLoading, state.challenges, state.archivedChallenges) {
        val id = pendingOpenChallengeId ?: return@LaunchedEffect
        if (state.isLoading) return@LaunchedEffect
        val match = state.challenges.firstOrNull { it.id == id }
            ?: state.archivedChallenges.firstOrNull { it.id == id }
        if (match != null) {
            selectedChallenge = match
        }
        viewModel.consumePendingOpenChallengeDetail()
    }

    val userUsername by settingsViewModel.userUsername.collectAsState()
    val coinBalance by coinViewModel.balance.collectAsState()
    val isPremium by billingViewModel.isPro.collectAsState()
    val purchaseCelebration by coinViewModel.purchaseCelebration.collectAsState()
    val coinNotice by coinViewModel.notice.collectAsState()
    var showCoinPackSheet by remember { mutableStateOf(false) }
    var giftTargetUid by remember { mutableStateOf<String?>(null) }
    var showGiftArc by remember { mutableStateOf(false) }
    var stakeConfirmAmount by remember { mutableStateOf<Int?>(null) }
    var stakeConfirmChallengeId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.challenges, isPremium) {
        coinViewModel.resolveStakes(state.challenges + state.archivedChallenges, isPremium)
        coinViewModel.grantMonthlyDripIfDue(isPremium)
    }

    LaunchedEffect(coinNotice) {
        coinNotice?.let { notice ->
            onShowNotification?.invoke(notice.message, notice.isPositive)
            coinViewModel.consumeNotice()
        }
    }
    var challengeTabPast by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var proofViewer by remember { mutableStateOf<ProofViewerArgs?>(null) }
    val todayStr = remember {
        java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    }
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled by settingsViewModel.isVibrationEnabled.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val pending = ChallengeInviteManager.peekPendingJoin(context) ?: return@LaunchedEffect
        ChallengeInviteManager.clearPendingJoin(context)
        autoOpenAfterJoin = true
        viewModel.joinChallenge(pending)
    }

    val themePref = remember(context) { com.saintnico.verdlyhabits.preferences.ThemePreference(context) }
    val arenaOnboardingSeen by themePref.hasSeenChallengeArenaOnboarding.collectAsState(initial = false)
    var showArenaOnboarding by remember { mutableStateOf(false) }
    var pendingArenaOnboarding by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val storageRepository = remember { StorageRepository() }

    // Photo Verification state
    var uriToSave by remember { mutableStateOf<Uri?>(null) }
    var challengeForPhoto by remember { mutableStateOf<Challenge?>(null) }
    var pendingCameraChallenge by remember { mutableStateOf<Challenge?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && uriToSave != null && challengeForPhoto != null) {
            coroutineScope.launch {
                try {
                    val downloadUrl = storageRepository.uploadProofPhoto(context, "challenge_${challengeForPhoto!!.id}", uriToSave!!, challengeForPhoto!!.id)
                    if (downloadUrl != null) {
                        val todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                        
                        // Try to get location
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { location ->
                                    viewModel.markCompletion(challengeForPhoto!!.id, todayStr, downloadUrl, location?.latitude, location?.longitude)
                                }
                        } else {
                            viewModel.markCompletion(challengeForPhoto!!.id, todayStr, downloadUrl)
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    // Camera permission launcher — declared after cameraLauncher so it can reference it
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingCameraChallenge != null) {
            val imagesDir = File(context.cacheDir, "images").also { it.mkdirs() }
            val file = File(imagesDir, "proof_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            uriToSave = uri
            challengeForPhoto = pendingCameraChallenge
            pendingCameraChallenge = null
            cameraLauncher.launch(uri)
        }
    }

    fun launchCameraForChallenge(challenge: Challenge) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val imagesDir = File(context.cacheDir, "images").also { it.mkdirs() }
            val file = File(imagesDir, "proof_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            uriToSave = uri
            challengeForPhoto = challenge
            cameraLauncher.launch(uri)
        } else {
            pendingCameraChallenge = challenge
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }


    // ProofState observation
    val proofState by viewModel.proofState.collectAsState()
    var showMilestone by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var rankToast by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(Unit) {
        viewModel.rankChangeEvent.collect { (newRank, delta) ->
            rankToast = newRank to delta
            delay(3000)
            rankToast = null
        }
    }

    LaunchedEffect(proofState) {
        when (proofState) {
            is ProofState.Success -> {
                onShowNotification?.invoke("Proof posted", false)
                viewModel.resetProofState()
            }
            is ProofState.LateSuccess -> {
                onShowNotification?.invoke("Late proof saved. Streak protected. +5 XP", false)
                viewModel.resetProofState()
            }
            is ProofState.Error -> {
                snackbarHostState.showSnackbar((proofState as ProofState.Error).message)
                viewModel.resetProofState()
            }
            is ProofState.WindowClosed -> {
                snackbarHostState.showSnackbar("Daily proof opens after 3:00 AM")
                viewModel.resetProofState()
            }
            is ProofState.LateWindowClosed -> {
                snackbarHostState.showSnackbar("Late proof window closed at 3:00 AM")
                viewModel.resetProofState()
            }
            is ProofState.AlreadyPostedLate -> {
                snackbarHostState.showSnackbar("You already covered yesterday")
                viewModel.resetProofState()
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.milestoneEvent.collect { streak -> showMilestone = streak }
    }

    // Error snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.createResult.collect { created ->
            if (created != null) {
                createdChallenge = created
                pendingArenaOnboarding = true
                pendingCreateShareDraft?.let { (habitName, stake, days) ->
                    createdChallengeShareInfo = CreatedChallengeShareInfo(created, habitName, stake, days)
                }
                pendingCreateShareDraft = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.reactionFeedback.collect { msg ->
            onShowNotification?.invoke(msg, false)
        }
    }

    LaunchedEffect(pendingArenaOnboarding, arenaOnboardingSeen) {
        if (pendingArenaOnboarding && !arenaOnboardingSeen) {
            showArenaOnboarding = true
            pendingArenaOnboarding = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.joinResult.collect { outcome ->
            when (outcome) {
                is JoinChallengeOutcome.Success -> {
                    pendingArenaOnboarding = true
                    if (autoOpenAfterJoin) {
                        viewModel.requestOpenChallengeDetail(outcome.challengeDocumentId)
                        autoOpenAfterJoin = false
                        onShowNotification?.invoke("Welcome to the challenge — you're in!", false)
                    } else {
                        onShowNotification?.invoke("You're in! Find it under Active.", false)
                    }
                }
                JoinChallengeOutcome.NotFound ->
                    onShowNotification?.invoke(
                        "No challenge matches that code. Double-check the 6 letters or paste the full link.",
                        true
                    )
                JoinChallengeOutcome.Ended ->
                    onShowNotification?.invoke(
                        "That challenge has already ended.",
                        true
                    )
                JoinChallengeOutcome.PermissionDenied ->
                    onShowNotification?.invoke(
                        "You don't have permission to join this challenge. Sign out and back in, then try again.",
                        true
                    )
                JoinChallengeOutcome.NotSignedIn ->
                    onShowNotification?.invoke("Sign in to join a challenge.", true)
                is JoinChallengeOutcome.Error ->
                    onShowNotification?.invoke(
                        outcome.message?.let { "Couldn't join: $it" }
                            ?: "Couldn't join — try again.",
                        true
                    )
            }
        }
    }

    val accent = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onBg = MaterialTheme.colorScheme.onBackground
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            floatingActionButton = {}
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        PaddingValues(
                            top = innerPadding.calculateTopPadding(),
                            bottom = innerPadding.calculateBottomPadding(),
                            start = 0.dp,
                            end = 0.dp
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    val firstActive = state.challenges.firstOrNull { it.isEffectivelyActive() }
                    val uid = viewModel.currentUserId
                    val rankLabel = if (firstActive != null && uid != null) {
                        val r = firstActive.rankOf(uid)
                        if (r > 0) "#$r" else "—"
                    } else "—"
                    val activeN = state.challenges.count { it.isEffectivelyActive() }
                    val streakV = if (firstActive != null && uid != null) firstActive.streakFor(uid) else 0
                    PremiumChallengesHeader(rankLabel, activeN, streakV, accent, tertiary)
                    Spacer(Modifier.height(4.dp))
                    CoinBalanceChip(
                        balance = coinBalance,
                        isPremium = isPremium,
                        onClick = { showCoinPackSheet = true },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                if (userUsername.isBlank() || userUsername == "UnknownRival" || userUsername.equals("unknownrival", ignoreCase = true)) {
                    item {
                        Box(Modifier.padding(horizontal = 12.dp)) {
                            AnonymousBannerPremium {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onNavigateToEditProfile()
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                } else if (state.challenges.isEmpty()) {
                    item {
                        Box(Modifier.padding(horizontal = 12.dp)) {
                            IllustratedEmptyChallenges(
                                onStartChallenge = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showCreateDialog = true
                                },
                                onJoinWithId = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showJoinDialog = true
                                }
                            )
                        }
                    }
                } else {
                    val active = state.challenges.filter { it.isEffectivelyActive() }
                    val ended = (state.challenges.filter { !it.isEffectivelyActive() } + state.archivedChallenges).distinctBy { it.id }

                    item {
                        Box(Modifier.padding(horizontal = 12.dp)) {
                            ActivePastToggle(isPast = challengeTabPast) { past ->
                                if (challengeTabPast != past) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                challengeTabPast = past
                            }
                        }
                    }

                    if (!challengeTabPast) {
                        if (active.isEmpty()) {
                            item {
                                Box(Modifier.padding(horizontal = 12.dp)) {
                                    IllustratedEmptyChallenges(
                                        onStartChallenge = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showCreateDialog = true
                                        },
                                        onJoinWithId = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showJoinDialog = true
                                        }
                                    )
                                }
                            }
                        } else {
                            items(active, key = { it.id }) { challenge ->
                                val coinStake by coinViewModel.observeStakeFor(challenge.id).collectAsState(initial = null)
                                Box(Modifier.padding(horizontal = 12.dp)) {
                                    ChallengeCardPremium(
                                        challenge = challenge,
                                        currentUserId = viewModel.currentUserId ?: "",
                                        onClick = { selectedChallenge = challenge },
                                        isEnded = false,
                                        todayStr = todayStr,
                                        coinStakeAmount = coinStake?.stakedAmount,
                                        isPremium = isPremium,
                                    )
                                }
                            }
                        }
                    } else if (ended.isNotEmpty()) {
                        items(ended, key = { it.id }) { challenge ->
                            Box(Modifier.padding(horizontal = 12.dp)) {
                                ChallengeCardPremium(
                                    challenge = challenge,
                                    currentUserId = viewModel.currentUserId ?: "",
                                    onClick = { selectedChallenge = challenge },
                                    isEnded = true,
                                    todayStr = todayStr
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                "No past challenges yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = onBg.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 36.dp, vertical = 24.dp)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(88.dp)) }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, end = 12.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            PremiumExpandableFab(
                expanded = fabExpanded,
                onToggle = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    fabExpanded = !fabExpanded
                },
                onCreate = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    fabExpanded = false
                    showCreateDialog = true
                },
                onJoin = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    fabExpanded = false
                    showJoinDialog = true
                },
                primary = accent,
                tertiary = tertiary
            )
        }

        rankToast?.let { (nr, delta) ->
            RankMoveToast(
                newRank = nr,
                rankChange = delta,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            )
        }

        proofViewer?.let { pv ->
            ProofViewerScreen(
                args = pv,
                onDismiss = { proofViewer = null },
                hapticsEnabled = hapticsEnabled,
                onReport = if (pv.challengeId.isNotBlank() && pv.proofKey.isNotBlank()) {
                    {
                        viewModel.reportProof(pv.challengeId, pv.proofKey) { ok ->
                            onShowNotification?.invoke(
                                if (ok) "Report submitted. Thanks for keeping the arena safe."
                                else "Couldn't submit report. Try again.",
                                !ok,
                            )
                        }
                    }
                } else null,
            )
        }
    }

    if (showArenaOnboarding) {
        ChallengeArenaOnboardingDialog(
            onDismiss = {
                showArenaOnboarding = false
                coroutineScope.launch { themePref.setChallengeArenaOnboardingSeen() }
            },
        )
    }

    // Milestone Celebration Overlay
    if (showMilestone != null) {
        MilestoneCelebration(
            streak = showMilestone!!,
            onDismiss = { showMilestone = null }
        )
    }

    // Uploading Overlay
    if (proofState is ProofState.Uploading) {
        UploadingOverlay()
    }

    // ─── Create Challenge Dialog ─────────────────────────────────────────────
    if (showCreateDialog) {
        CreateChallengeDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, stake, days, deadline, mode ->
                pendingCreateShareDraft = Triple(name, stake, days)
                viewModel.createChallenge(name, stake, days, deadline, mode)
                showCreateDialog = false
                onShowNotification?.invoke("Challenge created! Share the link to invite friends.", false)
            }
        )
    }

    // ─── Join Challenge Dialog ───────────────────────────────────────────────
    if (showJoinDialog) {
        JoinChallengeDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { id ->
                showJoinDialog = false
                viewModel.joinChallenge(id)
            }
        )
    }

    // ─── Challenge Detail Sheet ──────────────────────────────────────────────
    if (selectedChallenge != null) {
        ChallengeDetailSheet(
            challenge = selectedChallenge!!,
            currentUserId = viewModel.currentUserId ?: "",
            viewModel = viewModel,
            coinViewModel = coinViewModel,
            isPremium = isPremium,
            coinBalance = coinBalance,
            senderName = userUsername.ifBlank { "You" },
            hapticsEnabled = hapticsEnabled,
            connections = connections,
            onAddConnection = { friend ->
                selectedChallenge?.let { ch ->
                    viewModel.addMemberToChallenge(ch.id, friend.uid) { ok, msg ->
                        onShowNotification?.invoke(msg, !ok)
                    }
                }
            },
            onDismiss = { selectedChallenge = null },
            onPostProof = { selectedChallenge?.let { launchCameraForChallenge(it) } },
            onNavigateToMemberProfile = { memberId ->
                selectedChallenge?.let { viewModel.setMemberProfileHighlightChallengeId(it.id) }
                selectedChallenge = null
                onNavigateToMemberProfile(memberId)
            },
            onRemoveTodaysProof = {
                val todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                selectedChallenge?.let { viewModel.deleteTodayCheckIn(it.id, todayStr) }
                onShowNotification?.invoke(
                    "Check-in removed. Post a new proof today if you want it to count.",
                    false
                )
            },
            onLeave = {
                selectedChallenge?.let { viewModel.leaveChallenge(it.id) }
                selectedChallenge = null
                onShowNotification?.invoke("You left the challenge", false)
            },
            onEnd = {
                selectedChallenge?.let { viewModel.endChallenge(it.id) }
                selectedChallenge = null
                onShowNotification?.invoke("Challenge ended", false)
            },
            onViewProof = { proofViewer = it },
            onShowNotification = onShowNotification,
            onBuyCoins = { showCoinPackSheet = true },
            onRequestStakeConfirm = { challengeId, amount ->
                stakeConfirmChallengeId = challengeId
                stakeConfirmAmount = amount
            },
            onSupportMember = { uid -> giftTargetUid = uid },
        )
    }

    giftTargetUid?.let { receiverId ->
        val ch = selectedChallenge
        if (ch != null && receiverId != viewModel.currentUserId) {
            GiftSupportSheet(
                receiverName = ch.memberNames[receiverId] ?: "Rival",
                balance = coinBalance,
                isPremium = isPremium,
                onDismiss = { giftTargetUid = null },
                onSend = { amount, message ->
                    coinViewModel.sendGift(
                        challenge = ch,
                        receiverId = receiverId,
                        amount = amount,
                        message = message,
                        senderName = userUsername.ifBlank { "You" },
                    ) { ok, msg ->
                        onShowNotification?.invoke(msg, ok)
                        if (ok) {
                            showGiftArc = true
                            giftTargetUid = null
                        }
                    }
                },
            )
        }
    }

    stakeConfirmAmount?.let { amount ->
        StakeConfirmSheet(
            amount = amount,
            onDismiss = {
                stakeConfirmAmount = null
                stakeConfirmChallengeId = null
            },
            onConfirm = {
                val cid = stakeConfirmChallengeId
                stakeConfirmAmount = null
                stakeConfirmChallengeId = null
                if (cid != null) {
                    coinViewModel.stake(cid, amount) { ok, msg ->
                        onShowNotification?.invoke(msg, ok)
                    }
                }
            },
        )
    }

    if (showCoinPackSheet) {
        CoinPackSheet(
            isOpen = true,
            billingViewModel = billingViewModel,
            isPremium = isPremium,
            onDismiss = { showCoinPackSheet = false },
            onPurchaseSuccess = { coinViewModel.celebratePurchase() },
        )
    }

    if (purchaseCelebration || showGiftArc) {
        Box(Modifier.fillMaxSize()) {
            if (purchaseCelebration) {
                CoinBurstAnimation(
                    premium = isPremium,
                    onFinished = { coinViewModel.endCelebration() },
                )
            }
            if (showGiftArc) {
                GiftArcAnimation(
                    premium = isPremium,
                    onFinished = { showGiftArc = false },
                )
            }
        }
    }

    // ─── Creation Success Dialog ─────────────────────────────────────────────
    if (createdChallenge != null) {
        val cc = createdChallenge!!
        val shareInfo = createdChallengeShareInfo
        AlertDialog(
            onDismissRequest = {
                createdChallenge = null
                createdChallengeShareInfo = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RocketLaunch, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Challenge Ready!")
                }
            },
            text = {
                Column {
                    ChallengeInvitePreviewCard(
                        challengeName = shareInfo?.habitName ?: "Verdly challenge",
                        inviteCode = cc.inviteCode,
                        stake = shareInfo?.stake.orEmpty(),
                        daysRemaining = shareInfo?.durationDays?.toLong(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Share the card or link below. Friends tap it to join automatically in Verdly.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBg.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Invite link",
                        style = MaterialTheme.typography.labelMedium,
                        color = onBg.copy(alpha = 0.8f),
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .clickable {
                                val link = ChallengeInviteHelper.shareLink(cc.inviteCode)
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Challenge invite link", link))
                                onShowNotification?.invoke("Invite link copied!", false)
                            }
                            .padding(14.dp),
                    ) {
                        Text(
                            ChallengeInviteHelper.shareLinkLabel(cc.inviteCode),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "6-letter code",
                        style = MaterialTheme.typography.labelMedium,
                        color = onBg.copy(alpha = 0.8f),
                    )
                    Text(
                        cc.inviteCode,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = accent,
                        letterSpacing = 4.sp,
                        modifier = Modifier.clickable {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Challenge invite code", cc.inviteCode))
                            onShowNotification?.invoke("Invite code copied!", false)
                        },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shareChallengeInviteCard(
                            context = context,
                            challengeName = shareInfo?.habitName ?: "Verdly challenge",
                            stake = shareInfo?.stake.orEmpty(),
                            inviteCode = cc.inviteCode,
                            challengeId = cc.documentId,
                            daysRemaining = shareInfo?.durationDays?.toLong(),
                            memberCount = null
                        )
                        createdChallenge = null
                        createdChallengeShareInfo = null
                    }
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    createdChallenge = null
                    createdChallengeShareInfo = null
                }) { Text("Close") }
            }
        )
    }
}

// ─── Create Challenge (modal bottom sheet, 3-step pager) ───────────────────
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun CreateChallengeDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int, String, ChallengeMode) -> Unit
) {
    var habitName by remember { mutableStateOf("") }
    var stake by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf(7) }
    var dailyDeadline by remember { mutableStateOf("ANYTIME") }
    var challengeMode by remember { mutableStateOf(ChallengeMode.HYBRID) }
    val pagerState = rememberPagerState(pageCount = { 3 }, initialPage = 0)
    val scope = rememberCoroutineScope()
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onBg = MaterialTheme.colorScheme.onBackground

    val deadlineCards = listOf(
        CreateDeadlineOption("MORNING", "Morning", "by 12:00 PM", "Early risers, gym before work", Icons.Default.WbSunny),
        CreateDeadlineOption("AFTERNOON", "Afternoon", "by 6:00 PM", "Lunch breaks, after school", Icons.Default.LightMode),
        CreateDeadlineOption("NIGHT", "Night", "by 11:00 PM", "Evening workouts, wind-down", Icons.Default.NightsStay),
        CreateDeadlineOption("ANYTIME", "Anytime", "by 11:00 PM", "Flexible schedules", Icons.Default.Schedule)
    )
    val modeCards = listOf(
        CreateChallengeModeOption(
            ChallengeMode.STREAK,
            "Consistency Cup",
            "Winner is decided by the longest streak.",
            Icons.Default.LocalFireDepartment,
        ),
        CreateChallengeModeOption(
            ChallengeMode.REACTIONS,
            "Crowd Favourite",
            "Winner is decided by proof reaction points.",
            Icons.Default.Favorite,
        ),
        CreateChallengeModeOption(
            ChallengeMode.HYBRID,
            "Hybrid",
            "Winner uses streak x10 plus reaction points.",
            Icons.Default.EmojiEvents,
        ),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(primary.copy(alpha = 0.3f))
            )
        }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(primary, tertiary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Launch an arena",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = onBg,
                    )
                    Text(
                        when (pagerState.currentPage) {
                            0 -> "Name the battle — you’re already a host"
                            1 -> "Set the rules that make it fair"
                            else -> "Pick the daily finish line"
                        },
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = onBg.copy(alpha = 0.5f),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(4.dp)
                            .width(if (pagerState.currentPage == i) 28.dp else 10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (pagerState.currentPage == i) primary else onBg.copy(alpha = 0.18f))
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 280.dp, max = 420.dp)
            ) { page ->
                when (page) {
                    0 -> Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "What will you all prove together?",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            lineHeight = 26.sp,
                            color = onBg,
                        )
                        Text(
                            "A clear habit + a playful stake turns friends into rivals who care.",
                            fontFamily = dmSansFamily,
                            fontSize = 13.sp,
                            color = onBg.copy(alpha = 0.55f),
                            lineHeight = 18.sp,
                        )
                        OutlinedTextField(
                            value = habitName,
                            onValueChange = { habitName = it.take(30) },
                            label = { Text("Habit name") },
                            placeholder = { Text("e.g., Morning run") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            supportingText = {
                                Text(
                                    "${habitName.length}/30",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.End,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onBg.copy(alpha = 0.45f)
                                )
                            }
                        )
                        OutlinedTextField(
                            value = stake,
                            onValueChange = { stake = it },
                            label = { Text("What's at stake?") },
                            placeholder = { Text("e.g., Loser buys lunch") },
                            leadingIcon = { Icon(Icons.Default.Gavel, null, tint = StakeAmber) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StakeAmber,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                focusedLeadingIconColor = StakeAmber,
                                unfocusedLeadingIconColor = StakeAmber.copy(alpha = 0.7f)
                            )
                        )
                    }

                    1 -> Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "How long is this chapter?",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = onBg,
                        )
                        Text(
                            "Short enough to finish. Long enough to change you.",
                            fontFamily = dmSansFamily,
                            fontSize = 13.sp,
                            color = onBg.copy(alpha = 0.55f),
                        )
                        Text("Duration", fontWeight = FontWeight.SemiBold, color = onBg.copy(alpha = 0.7f), fontSize = 13.sp)
                        val durations = listOf(7, 14, 21, 30)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, onBg.copy(alpha = 0.12f), RoundedCornerShape(50))
                        ) {
                            durations.forEach { days ->
                                val selected = durationDays == days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) Brush.horizontalGradient(listOf(primary, tertiary))
                                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                        )
                                        .clickable { durationDays = days },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${days}d",
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else onBg.copy(alpha = 0.45f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Scoring mode", fontWeight = FontWeight.SemiBold, color = onBg.copy(alpha = 0.7f), fontSize = 13.sp)
                        modeCards.forEach { card ->
                            val selected = challengeMode == card.mode
                            Surface(
                                onClick = { challengeMode = card.mode },
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) primary else onBg.copy(alpha = 0.1f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(card.icon, null, tint = primary, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(card.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = onBg)
                                        Text(card.description, fontSize = 12.sp, color = onBg.copy(alpha = 0.55f))
                                    }
                                    if (selected) {
                                        Icon(Icons.Default.CheckCircle, null, tint = primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    else -> Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "When does the day close?",
                            fontFamily = frauncesFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            color = onBg,
                        )
                        Text(
                            "Everyone posts proof before this line. Fair clocks make fair wins.",
                            fontFamily = dmSansFamily,
                            fontSize = 13.sp,
                            color = onBg.copy(alpha = 0.55f),
                            lineHeight = 18.sp,
                        )
                        deadlineCards.chunked(2).forEach { rowCards ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                rowCards.forEach { card ->
                                    val selected = dailyDeadline == card.key
                                    val scale by androidx.compose.animation.core.animateFloatAsState(
                                        targetValue = if (selected) 1.03f else 1f,
                                        animationSpec = androidx.compose.animation.core.spring(
                                            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                                        ),
                                        label = "deadlineScale"
                                    )
                                    Surface(
                                        onClick = { dailyDeadline = card.key },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(110.dp)
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                            },
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (selected) primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            if (selected) 2.dp else 1.dp,
                                            if (selected) primary else onBg.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Box(Modifier.fillMaxSize().padding(10.dp)) {
                                            if (selected) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    null,
                                                    tint = primary,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .size(18.dp)
                                                )
                                            }
                                            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                                                Icon(card.icon, null, tint = primary, modifier = Modifier.size(28.dp))
                                                Spacer(Modifier.height(6.dp))
                                                Text(card.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = onBg)
                                                Text(card.subtitle, style = MaterialTheme.typography.labelSmall, color = onBg.copy(alpha = 0.6f))
                                                Spacer(Modifier.height(4.dp))
                                                Text(card.hint, style = MaterialTheme.typography.labelSmall, color = onBg.copy(alpha = 0.45f), maxLines = 2)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        } else onDismiss()
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (pagerState.currentPage > 0) "Back" else "Cancel")
                }
                val lastPage = pagerState.currentPage == 2
                val canAdvanceStep0 = habitName.isNotBlank()
                Button(
                    onClick = {
                        when {
                            pagerState.currentPage < 2 -> {
                                if (pagerState.currentPage == 0 && !canAdvanceStep0) return@Button
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                            else -> onCreate(habitName.trim(), stake.trim(), durationDays, dailyDeadline, challengeMode)
                        }
                    },
                    enabled = if (pagerState.currentPage == 0) canAdvanceStep0 else true,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = primary)
                ) {
                    Text(
                        if (lastPage) "Open the arena" else "Continue",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = dmSansFamily,
                    )
                    if (!lastPage) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.RocketLaunch, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ─── Join Challenge Dialog ───────────────────────────────────────────────────
@Composable
private fun JoinChallengeDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var challengeId by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.GroupAdd, null, tint = primary, modifier = Modifier.size(28.dp))
            }
        },
        title = {
            Text(
                "Step into an arena",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste a code from a friend. The moment you join, you’re already in the story — proof starts tomorrow.",
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = challengeId,
                    onValueChange = { challengeId = it },
                    label = { Text("Invite code or challenge ID") },
                    placeholder = { Text("6-letter code, link, or long ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (challengeId.isNotBlank()) onJoin(challengeId.trim()) },
                enabled = challengeId.isNotBlank(),
                shape = RoundedCornerShape(50),
            ) {
                Text("Join the arena", fontFamily = dmSansFamily, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}

// ─── Challenge Detail Bottom Sheet ───────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChallengeDetailSheet(
    challenge: Challenge,
    currentUserId: String,
    viewModel: ChallengeViewModel,
    coinViewModel: CoinViewModel,
    isPremium: Boolean,
    coinBalance: Int,
    senderName: String,
    hapticsEnabled: Boolean,
    connections: List<FriendSummary>,
    onAddConnection: (FriendSummary) -> Unit,
    onDismiss: () -> Unit,
    onPostProof: () -> Unit,
    onNavigateToMemberProfile: (String) -> Unit,
    onRemoveTodaysProof: () -> Unit,
    onLeave: () -> Unit,
    onEnd: () -> Unit,
    onViewProof: (ProofViewerArgs) -> Unit,
    onShowNotification: ((String, Boolean) -> Unit)? = null,
    onBuyCoins: () -> Unit = {},
    onRequestStakeConfirm: (challengeId: String, amount: Int) -> Unit = { _, _ -> },
    onSupportMember: (String) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activityItems by viewModel.observeActivity(challenge.id).collectAsState(initial = emptyList())
    val activeStake by coinViewModel.observeStakeFor(challenge.id).collectAsState(initial = null)
    val gifts by coinViewModel.observeGiftsFor(challenge.id).collectAsState(initial = emptyList())
    var showConnectionsPicker by remember { mutableStateOf(false) }
    var nudgeTarget by remember { mutableStateOf<com.saintnico.verdlyhabits.ui.components.social.NudgeTarget?>(null) }
    val sheetHaptic = LocalHapticFeedback.current
    // Bound to this sheet's non-null [challenge] so reactions never depend on the parent's
    // selectedChallenge state (the proof viewer can outlive the sheet). Fixes a hard NPE crash.
    val onReact: (String, String) -> Unit = remember(challenge.id, hapticsEnabled) {
        { proofKey, emoji ->
            if (hapticsEnabled) sheetHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.addReaction(challenge.id, proofKey, emoji)
        }
    }
    var showRemoveCheckInDialog by remember { mutableStateOf(false) }
    val leaderboard = challenge.leaderboard()
    val reactionIcons = listOf(
        Icons.Rounded.LocalFireDepartment to ReactionWeights.FIRE,
        Icons.Rounded.Favorite to ReactionWeights.HEART,
        Icons.Rounded.ElectricBolt to ReactionWeights.LIGHTNING,
    )
    val context = androidx.compose.ui.platform.LocalContext.current
    val todayStr = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    val yesterdayStr = java.time.LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
    val hasCompletedToday = challenge.hasCompletedToday(currentUserId, todayStr)
    val windowState = challenge.currentProofWindowState(currentUserId, todayStr)
    val hourNow = java.time.LocalTime.now().hour
    val missedYesterday = !challenge.hasCompletedToday(currentUserId, yesterdayStr)
    val inGraceWindow = hourNow < 3

    LaunchedEffect(challenge.id) {
        coinViewModel.refreshGiftsFor(challenge.id)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.markLateCompletion(challenge.id, it, context) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    challenge.habitName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                CoinBalanceChip(
                    balance = coinBalance,
                    isPremium = isPremium,
                    onClick = onBuyCoins,
                )
            }
            
            val creatorName = challenge.memberNames[challenge.creatorId] ?: "UnknownRival"
            Text("Started by @$creatorName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            
            if (!challenge.isActive) {
                Spacer(Modifier.height(16.dp))
                ChallengeResultsPodium(challenge)
            }
            
            // Display Challenge ID for easy sharing
            Surface(
                onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Challenge ID", challenge.id)
                    clipboard.setPrimaryClip(clip)
                },
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ID: ${challenge.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${challenge.daysRemaining()} days remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                Spacer(Modifier.width(16.dp))
                Icon(Icons.Default.People, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${challenge.members.size} members",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            }

            if (challenge.isActive && currentUserId.isNotBlank()) {
                challenge.gapToLeader(currentUserId)?.let { gap ->
                    if (gap > 0 && challenge.challengeMode != com.saintnico.verdlyhabits.data.model.ChallengeMode.STREAK) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "$gap ${challenge.scoreLabel()} behind #1",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            if (challenge.stake.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFB300).copy(alpha = 0.08f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Gavel, null, tint = Color(0xFFFFB300))
                    Spacer(Modifier.width(10.dp))
                    Text(challenge.stake, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground)
                }
            }

            if (challenge.isActive && currentUserId.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                StakeConsistencyCard(
                    balance = coinBalance,
                    activeStake = activeStake,
                    streak = challenge.streakFor(currentUserId),
                    isPremium = isPremium,
                    challengeActive = challenge.isActive,
                    onCommitClick = { amount -> onRequestStakeConfirm(challenge.id, amount) },
                    onBuyCoins = onBuyCoins,
                )
            }

            if (challenge.isActive) {
                Spacer(Modifier.height(14.dp))
                DeadlineStatusBanner(challenge = challenge, windowState = windowState)
            }

            if (challenge.isActive) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "TODAY'S BOARD",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                challenge.members.chunked(2).forEach { rowIds ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowIds.forEach { uid ->
                            TodayBoardTile(
                                challenge = challenge,
                                userId = uid,
                                todayStr = todayStr,
                                modifier = Modifier.weight(1f),
                                onMemberProfile = { onNavigateToMemberProfile(uid) }
                            )
                        }
                        if (rowIds.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (challenge.isActive) {
                Spacer(Modifier.height(8.dp))
                val postedAfterDeadline = hasCompletedToday && challenge.isAfterDeadlineProof(currentUserId, todayStr)
                val showLateYesterdayVerified = !hasCompletedToday &&
                    challenge.hasCompletedToday(currentUserId, yesterdayStr) &&
                    challenge.isLateProof(currentUserId, yesterdayStr)

                when {
                    hasCompletedToday && challenge.isLateProof(currentUserId, todayStr) -> {
                        Spacer(Modifier.height(12.dp))
                        ProofVerifiedLateCard(onRemove = { showRemoveCheckInDialog = true }, challengeActive = challenge.isActive)
                    }
                    hasCompletedToday && postedAfterDeadline -> {
                        Spacer(Modifier.height(12.dp))
                        ProofVerifiedReducedCard(onRemove = { showRemoveCheckInDialog = true }, challengeActive = challenge.isActive)
                    }
                    hasCompletedToday -> {
                        Spacer(Modifier.height(12.dp))
                        ProofVerifiedOnTimeCard(onRemove = { showRemoveCheckInDialog = true }, challengeActive = challenge.isActive)
                    }
                    showLateYesterdayVerified -> {
                        Spacer(Modifier.height(12.dp))
                        ProofVerifiedLateSaveCard()
                    }
                    windowState == ProofWindowState.GraceWindow && missedYesterday && inGraceWindow -> {
                        Spacer(Modifier.height(12.dp))
                        GraceLateProofSection(
                            onPickGallery = { galleryLauncher.launch("image/*") }
                        )
                    }
                    windowState == ProofWindowState.OpenFullXP || windowState == ProofWindowState.OpenReducedXP -> {
                        Spacer(Modifier.height(12.dp))
                        val reduced = windowState == ProofWindowState.OpenReducedXP
                        Button(
                            onClick = onPostProof,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (reduced) StakeAmber else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (reduced) "Post proof · +8 XP" else "Post proof · +10 XP",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                        if (reduced) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "After deadline — full XP window has passed",
                                style = MaterialTheme.typography.labelSmall,
                                color = StakeAmber.copy(alpha = 0.75f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                        if (!reduced && missedYesterday && inGraceWindow) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, StakeAmber.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = StakeAmber.copy(alpha = 0.08f),
                                    contentColor = StakeAmber
                                )
                            ) {
                                Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp), tint = StakeAmber)
                                Spacer(Modifier.width(8.dp))
                                Text("Post yesterday's proof", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            GraceWindowCountdownLine()
                        }
                    }
                    windowState == ProofWindowState.Locked -> {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Daily proof opens after 3:00 AM for a new check-in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> { }
                }
            }

            // Full leaderboard
            LeaderboardList(
                leaderboard,
                challenge,
                currentUserId,
                todayStr,
                gifts,
                onReact,
                onNavigateToMemberProfile,
                onViewProof,
                onNudgeClick = { userId ->
                    val late = challenge.isLateProof(userId, todayStr)
                    val posted = challenge.hasCompletedToday(userId, todayStr)
                    nudgeTarget = com.saintnico.verdlyhabits.ui.components.social.NudgeTarget(
                        uid = userId,
                        username = challenge.memberNames[userId] ?: "rival",
                        photoUrl = challenge.memberPhotos[userId],
                        surface = com.saintnico.verdlyhabits.data.model.NudgeSurface.CHALLENGE,
                        situation = if (!posted || late) {
                            com.saintnico.verdlyhabits.data.model.NudgeSituation.CHALLENGE_BEHIND
                        } else {
                            com.saintnico.verdlyhabits.data.model.NudgeSituation.GENERAL
                        },
                        contextId = challenge.id,
                    )
                },
                onSupportClick = { uid ->
                    if (uid != currentUserId) onSupportMember(uid)
                },
            )

            // Reactions
            if (challenge.proofPhotos.isNotEmpty()) {
                val latestProof = challenge.proofPhotos
                    .flatMap { (userId, photosByDate) -> photosByDate.keys.map { date -> userId to date } }
                    .maxByOrNull { it.second }
                val latestProofKey = latestProof?.let { proofReactionKey(it.first, it.second) }.orEmpty()
                Spacer(Modifier.height(20.dp))
                Text("React to latest proof", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Tap a proof photo for the full view, or hype the latest check-in below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    reactionIcons.forEach { (icon, name) ->
                        val tint = when(name) {
                            ReactionWeights.FIRE -> Color(0xFFFF9800)
                            ReactionWeights.HEART -> Color(0xFFE91E63)
                            ReactionWeights.LIGHTNING -> Color(0xFFFFEB3B)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape)
                                .background(tint.copy(alpha = 0.1f))
                                .clickable {
                                    if (latestProofKey.isNotBlank()) onReact(latestProofKey, name)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }

            // Share invite
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    shareChallengeInviteCard(
                        context = context,
                        challengeName = challenge.habitName,
                        stake = challenge.stake,
                        inviteCode = challenge.inviteCode,
                        challengeId = challenge.id,
                        daysRemaining = challenge.daysRemaining(),
                        memberCount = challenge.members.size
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Invite more friends", fontWeight = FontWeight.Bold)
            }

            // Add from existing connections (host can add directly)
            if (challenge.isActive && connections.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showConnectionsPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.GroupAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add from connections", fontWeight = FontWeight.Bold)
                }
            }

            if (showConnectionsPicker) {
                ConnectionsPickerDialog(
                    connections = connections,
                    existingMemberIds = challenge.members.toSet(),
                    onAdd = { friend ->
                        onAddConnection(friend)
                    },
                    onDismiss = { showConnectionsPicker = false }
                )
            }

            // Live Activity Feed
            if (activityItems.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                val livePulse = rememberInfiniteTransition(label = "liveFeed")
                val liveDotAlpha by livePulse.animateFloat(
                    initialValue = 0.45f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "liveDotAlpha"
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "LIVE FEED",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(DangerRed.copy(alpha = liveDotAlpha))
                    )
                }
                Spacer(Modifier.height(12.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val boostedActivityItems = activityItems.sortedByDescending { item ->
                        val date = item.metadata["completionDate"]?.toString().orEmpty()
                        val proofKey = if (date.isNotBlank()) proofReactionKey(item.actorId, date) else ""
                        proofKey.isNotBlank() && reactionCountsFor(challenge, proofKey).isEmpty()
                    }
                    boostedActivityItems.forEach { item ->
                        val date = item.metadata["completionDate"]?.toString().orEmpty()
                        val proofKey = if (date.isNotBlank()) proofReactionKey(item.actorId, date) else ""
                        ActivityFeedItem(
                            item = item,
                            isUnrecognizedProof = proofKey.isNotBlank() && reactionCountsFor(challenge, proofKey).isEmpty(),
                            onProofTap = { url ->
                                onViewProof(
                                    ProofViewerArgs(
                                        photoUrl = url,
                                        username = item.actorUsername,
                                        avatarUrl = item.actorPhotoURL.ifBlank { null },
                                        streakDay = 0,
                                        relativeTime = relativeTime(item.timestamp),
                                        proofKey = proofKey,
                                        challengeId = challenge.id,
                                        reactionCounts = if (proofKey.isNotBlank()) reactionCountsFor(challenge, proofKey) else emptyMap(),
                                        onReact = { emoji ->
                                            if (proofKey.isNotBlank()) onReact(proofKey, emoji)
                                        }
                                    )
                                )
                            }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))

            // Actions (Leave / End)
            if (challenge.isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = onLeave,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Leave Challenge")
                    }
                    if (challenge.creatorId == currentUserId) {
                        TextButton(
                            onClick = onEnd,
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF5350))
                        ) {
                            Icon(Icons.Default.Flag, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("End Challenge Early")
                        }
                    }
                }
            }
        }
    }

    if (showRemoveCheckInDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveCheckInDialog = false },
            title = { Text("Remove today’s check-in?") },
            text = {
                Text(
                    "Your streak and XP for this proof will roll back. You can post a new photo today. " +
                        "If you don’t check in again before the day ends, it still counts as a missed day for the challenge."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveCheckInDialog = false
                        onRemoveTodaysProof()
                    }
                ) {
                    Text("Remove", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveCheckInDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    com.saintnico.verdlyhabits.ui.components.social.NudgeSheetHost(
        target = nudgeTarget,
        onDismiss = { nudgeTarget = null },
        onSent = { onShowNotification?.invoke("Nudge sent", false) },
        onError = { message -> onShowNotification?.invoke(message, true) },
    )
}

@Composable
private fun ConnectionsPickerDialog(
    connections: List<FriendSummary>,
    existingMemberIds: Set<String>,
    onAdd: (FriendSummary) -> Unit,
    onDismiss: () -> Unit
) {
    // Track who's been added in this session so the row updates instantly.
    val addedIds = remember { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GroupAdd, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Add from connections", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            val available = connections.filter { it.uid !in existingMemberIds }
            if (available.isEmpty()) {
                Text(
                    "All of your connections are already in this arena. Invite more friends with the share link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(available, key = { it.uid }) { friend ->
                        val isAdded = friend.uid in addedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!friend.photoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = friend.photoUrl,
                                    contentDescription = friend.username,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        friend.username.firstOrNull()?.uppercase() ?: "?",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                friend.username,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isAdded) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Added",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                FilledTonalButton(
                                    onClick = {
                                        addedIds.add(friend.uid)
                                        onAdd(friend)
                                    },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                ) {
                                    Text("Add", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
private fun LeaderboardList(
    leaderboard: List<Pair<String, Int>>,
    challenge: Challenge,
    currentUserId: String,
    todayStr: String,
    gifts: List<com.saintnico.verdlyhabits.data.local.coins.GiftEventEntity>,
    onReact: (String, String) -> Unit,
    onNavigateToMemberProfile: (String) -> Unit,
    onViewProof: (ProofViewerArgs) -> Unit,
    onNudgeClick: ((String) -> Unit)? = null,
    onSupportClick: ((String) -> Unit)? = null,
) {
    val viewProofFor: (String, String) -> Unit = { userId, url ->
        val displayName = challenge.memberNames[userId] ?: "Unknown"
        val proofKey = proofReactionKey(userId, todayStr)
        onViewProof(
            ProofViewerArgs(
                photoUrl = url,
                username = displayName,
                avatarUrl = challenge.memberPhotos[userId]?.takeIf { it.isNotBlank() },
                streakDay = challenge.streakFor(userId),
                relativeTime = "Today",
                proofKey = proofKey,
                challengeId = challenge.id,
                reactionCounts = reactionCountsFor(challenge, proofKey),
                onReact = { emoji -> onReact(proofKey, emoji) }
            )
        )
    }

    Spacer(Modifier.height(20.dp))
    var standingsTab by remember { mutableIntStateOf(0) }
    val supportedBoard = remember(gifts, challenge.members) {
        challenge.members
            .map { uid -> uid to gifts.filter { it.receiverId == uid }.sumOf { it.amount } }
            .sortedByDescending { it.second }
    }
    val displayBoard = if (standingsTab == 0) leaderboard else supportedBoard

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = standingsTab == 0,
            onClick = { standingsTab = 0 },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) { Text("Most Consistent", fontSize = 12.sp) }
        SegmentedButton(
            selected = standingsTab == 1,
            onClick = { standingsTab = 1 },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) { Text("Most Supported", fontSize = 12.sp) }
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = GoldColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "STANDINGS",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "${displayBoard.size} rivals",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        )
    }
    Spacer(Modifier.height(12.dp))

    val podium = displayBoard.take(3)
    val rest = displayBoard.drop(3)

    key(challenge.id, standingsTab, podium.map { it.first }) {
        ChallengePodium(
            entries = podium,
            challenge = challenge,
            currentUserId = currentUserId,
            todayStr = todayStr,
            onProfileClick = onNavigateToMemberProfile,
            onProofClick = viewProofFor,
        )
    }

    if (rest.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        rest.forEachIndexed { index, (userId, score) ->
            key(userId) {
                PremiumStandingRow(
                    rank = index + 4,
                    userId = userId,
                    score = score,
                    challenge = challenge,
                    currentUserId = currentUserId,
                    todayStr = todayStr,
                    onProfileClick = onNavigateToMemberProfile,
                    onProofClick = viewProofFor,
                    onNudgeClick = onNudgeClick,
                    onSupportClick = onSupportClick,
                    scoreSuffix = if (standingsTab == 1) "coins received" else null,
                )
            }
        }
    }
}

@Composable
private fun ActivityFeedItem(
    item: com.saintnico.verdlyhabits.data.model.ActivityItem,
    isUnrecognizedProof: Boolean = false,
    onProofTap: (String) -> Unit
) {
    val typeLabel = when (item.type) {
        "joined" -> "Joined"
        "left" -> "Left"
        "checkin" -> "Check-in"
        "late_checkin" -> "Late proof"
        "missed" -> "Missed"
        "rank_up" -> "Rank up"
        "took_lead" -> "Took lead"
        "milestone" -> "Milestone"
        "streak_freeze" -> "Freeze"
        "ended" -> "Ended"
        "deadline_warning" -> "Deadline"
        "grace_window_open" -> "Grace open"
        "grace_window_closed" -> "Grace closed"
        else -> item.type.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    val indicatorColor = when (item.type) {
        "checkin", "milestone", "joined" -> SuccessGreen
        "late_checkin" -> StakeAmber
        "left", "missed" -> DangerRed
        "took_lead", "ended" -> GoldColor
        "rank_up", "streak_freeze" -> XPPurple
        "deadline_warning" -> StakeAmber
        "grace_window_open", "grace_window_closed" -> DangerRed
        else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
    }

    val chipBg = indicatorColor.copy(alpha = 0.18f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor)
            )
            Column(modifier = Modifier.weight(1f).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.actorPhotoURL.isNotBlank()) {
                        AsyncImage(
                            model = item.actorPhotoURL,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(indicatorColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                item.actorUsername.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = indicatorColor
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        item.actorUsername,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = chipBg,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            typeLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                    }
                    Text(
                        relativeTime(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (item.type == "late_checkin") {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = StakeAmber.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "LATE PROOF",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = StakeAmber,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (isUnrecognizedProof) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ) {
                        Text(
                            "Be first to cheer",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    item.message,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                if (!item.proofPhotoUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    val latePhoto = item.type == "late_checkin"
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(
                                width = if (latePhoto) 2.dp else 0.dp,
                                color = if (latePhoto) StakeAmber else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onProofTap(item.proofPhotoUrl!!) }
                    ) {
                        AsyncImage(
                            model = item.proofPhotoUrl,
                            contentDescription = "Proof",
                            modifier = Modifier.matchParentSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankMoveToast(
    newRank: Int,
    rankChange: Int,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(newRank, rankChange) {
        visible = true
    }
    val enter = slideInVertically(
        initialOffsetY = { -it },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    ) + fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    val exit = slideOutVertically(
        targetOffsetY = { -it },
        animationSpec = tween(280)
    ) + fadeOut(tween(200))

    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = modifier
    ) {
        when {
            rankChange > 0 && newRank == 1 -> LeadTakenRankToast(newRank = newRank, delta = rankChange)
            rankChange > 0 -> RankUpToastContent(newRank = newRank, delta = rankChange)
            rankChange < 0 -> RankDropToastContent(newRank = newRank, delta = rankChange)
            else -> Spacer(Modifier.height(0.dp))
        }
    }
}

@Composable
private fun RankUpToastContent(newRank: Int, delta: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowDropUp, null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text("Rank up", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = SuccessGreen)
                Text(
                    "You are now #$newRank on the board",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(SuccessGreen, MaterialTheme.colorScheme.primary))),
                contentAlignment = Alignment.Center
            ) {
                Text("#$newRank", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun LeadTakenRankToast(newRank: Int, delta: Int) {
    val pulse = rememberInfiniteTransition(label = "leadBorder")
    val borderA by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "b"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.88f),
        border = BorderStroke(1.5.dp, GoldColor.copy(alpha = borderA))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.EmojiEvents, null, tint = GoldColor, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text("You took the lead", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = GoldColor)
                Text(
                    "You are now #$newRank · +$delta",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GoldColor, MaterialTheme.colorScheme.primary))),
                contentAlignment = Alignment.Center
            ) {
                Text("#$newRank", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
            }
        }
    }
}

@Composable
private fun RankDropToastContent(newRank: Int, delta: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.85f))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ArrowDropDown, null, tint = DangerRed, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text("You've been overtaken", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = DangerRed)
                Text(
                    "Now #$newRank — post proof or react to climb back",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Text(
                "$delta",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = DangerRed,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun DeadlineStatusBanner(challenge: Challenge, windowState: ProofWindowState) {
    val bg = when (windowState) {
        ProofWindowState.OpenFullXP -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ProofWindowState.OpenReducedXP -> StakeAmber.copy(alpha = 0.1f)
        ProofWindowState.GraceWindow -> DangerRed.copy(alpha = 0.1f)
        ProofWindowState.Locked -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
        ProofWindowState.AlreadyPosted, ProofWindowState.AlreadyPostedLate -> SuccessGreen.copy(alpha = 0.08f)
    }
    val borderC = when (windowState) {
        ProofWindowState.OpenFullXP -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ProofWindowState.OpenReducedXP -> StakeAmber.copy(alpha = 0.3f)
        ProofWindowState.GraceWindow -> DangerRed.copy(alpha = 0.3f)
        ProofWindowState.Locked -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f)
        ProofWindowState.AlreadyPosted, ProofWindowState.AlreadyPostedLate -> SuccessGreen.copy(alpha = 0.22f)
    }
    val subtitle = when (windowState) {
        ProofWindowState.OpenFullXP -> "Full XP available · post before the cutoff"
        ProofWindowState.OpenReducedXP -> "Past cutoff · reduced XP (8 XP)"
        ProofWindowState.GraceWindow -> "Grace window · late proof only (5 XP)"
        ProofWindowState.Locked -> "Window closed · come back later"
        ProofWindowState.AlreadyPosted -> "You are done for today"
        ProofWindowState.AlreadyPostedLate -> "Late save recorded"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, borderC, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        challenge.deadlineLabel(),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
            when (windowState) {
                ProofWindowState.AlreadyPosted, ProofWindowState.AlreadyPostedLate ->
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
                ProofWindowState.Locked ->
                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f), modifier = Modifier.size(26.dp))
                else -> DeadlineCountdownChip(challenge = challenge, windowState = windowState)
            }
        }
    }
}

@Composable
private fun DeadlineCountdownChip(challenge: Challenge, windowState: ProofWindowState) {
    val zone = java.time.ZoneId.systemDefault()
    var label by remember { mutableStateOf("") }
    LaunchedEffect(challenge.id, challenge.dailyDeadline, windowState) {
        while (true) {
            val now = java.time.LocalDateTime.now(zone)
            val end = when (challenge.dailyDeadline) {
                "ANYTIME" -> java.time.LocalDate.now(zone).atTime(23, 59, 59)
                else -> java.time.LocalDate.now(zone).atTime(challenge.deadlineHour().coerceAtMost(23), 0)
            }
            val diff = java.time.Duration.between(now, end)
            label = if (diff.isNegative || diff.isZero) "Closed" else {
                val h = diff.toHours()
                if (h >= 1) "${h}h ${diff.toMinutesPart()}m left" else "${diff.toMinutesPart()}m ${diff.toSecondsPart()}s left"
            }
            kotlinx.coroutines.delay(1000)
        }
    }
    Text(
        label,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        color = when (windowState) {
            ProofWindowState.GraceWindow -> DangerRed
            ProofWindowState.OpenReducedXP -> StakeAmber
            else -> MaterialTheme.colorScheme.primary
        }
    )
}

@Composable
private fun TodayBoardTile(
    challenge: Challenge,
    userId: String,
    todayStr: String,
    modifier: Modifier = Modifier,
    onMemberProfile: (() -> Unit)? = null
) {
    val name = challenge.memberNames[userId] ?: "Rival"
    val photo = challenge.memberPhotos[userId]
    val posted = challenge.hasCompletedToday(userId, todayStr)
    val missed = !posted && challenge.currentProofWindowState(userId, todayStr) == ProofWindowState.Locked
    val bg = when {
        posted -> SuccessGreen.copy(alpha = 0.1f)
        missed -> DangerRed.copy(alpha = 0.06f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
    }
    val ring = when {
        posted -> SuccessGreen
        missed -> DangerRed
        else -> {
            val h = java.time.LocalTime.now().hour
            val afterCutoff = challenge.dailyDeadline != "ANYTIME" &&
                h >= challenge.deadlineHour().coerceAtMost(23)
            if (afterCutoff) StakeAmber else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
        }
    }
    Column(
        modifier
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, ring.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
            .then(
                if (onMemberProfile != null) Modifier.clickable { onMemberProfile() } else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                if (!photo.isNullOrBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(2.dp, ring, CircleShape)
                    )
                } else {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .border(2.dp, ring, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(name.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                if (missed) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .offset(2.dp, 2.dp)
                            .clip(CircleShape)
                            .background(DangerRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (!posted && !missed) 0.7f else 1f)
                )
                ProofStatusBadge(challenge = challenge, userId = userId, todayStr = todayStr, compact = true)
            }
        }
    }
}

@Composable
private fun ProofVerifiedOnTimeCard(onRemove: () -> Unit, challengeActive: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SuccessGreen.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text("Verified", fontWeight = FontWeight.Bold, color = SuccessGreen)
            Text("Day secured · proof on time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            if (challengeActive) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Remove today’s check-in", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProofVerifiedReducedCard(onRemove: () -> Unit, challengeActive: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StakeAmber.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, StakeAmber.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Timer, null, tint = StakeAmber, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text("After cutoff", fontWeight = FontWeight.Bold, color = StakeAmber)
            Text("Posted late in the day · +8 XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            if (challengeActive) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Remove today’s check-in", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ProofVerifiedLateCard(onRemove: () -> Unit, challengeActive: Boolean) {
    Box(Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StakeAmber.copy(alpha = 0.1f),
            border = BorderStroke(1.dp, StakeAmber.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, null, tint = StakeAmber, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(8.dp))
                Text("Late proof", fontWeight = FontWeight.Bold, color = StakeAmber)
                Text("Late streak save · +5 XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                if (challengeActive) {
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onRemove, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DeleteOutline, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Remove today’s check-in", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = StakeAmber.copy(alpha = 0.18f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Text(
                "LATE",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = StakeAmber,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun ProofVerifiedLateSaveCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StakeAmber.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, StakeAmber.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.History, null, tint = StakeAmber, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text("Yesterday secured", fontWeight = FontWeight.Bold, color = StakeAmber)
            Text("Late proof saved · streak protected · +5 XP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun GraceWindowCountdownLine() {
    var countdown by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.LocalTime.now()
            val target = java.time.LocalTime.of(3, 0)
            val diff = java.time.Duration.between(now, target)
            if (diff.isNegative || diff.isZero) {
                countdown = "Closed"
                break
            }
            countdown = "${diff.toMinutes()}m ${diff.toSecondsPart()}s left"
            kotlinx.coroutines.delay(1000)
        }
    }
    Text(
        countdown,
        style = MaterialTheme.typography.labelSmall,
        color = DangerRed,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun GraceLateProofSection(onPickGallery: () -> Unit) {
    var countdown by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.LocalTime.now()
            val target = java.time.LocalTime.of(3, 0)
            val diff = java.time.Duration.between(now, target)
            if (diff.isNegative || diff.isZero) {
                countdown = "Closed"
                break
            }
            countdown = "${diff.toMinutes()}m ${diff.toSecondsPart()}s"
            kotlinx.coroutines.delay(1000)
        }
    }
    OutlinedButton(
        onClick = onPickGallery,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = DangerRed,
            containerColor = DangerRed.copy(alpha = 0.04f)
        )
    ) {
        Icon(Icons.Default.History, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Save yesterday’s streak · +5 XP", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
    Spacer(Modifier.height(6.dp))
    Text(
        "Grace window closes at 3:00 AM · gallery only",
        style = MaterialTheme.typography.labelSmall,
        color = StakeAmber.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    Text(
        countdown,
        style = MaterialTheme.typography.labelSmall,
        color = DangerRed,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Gallery only · live camera not required for late proof",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

@Composable
fun UploadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Uploading Proof...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Verifying your habit...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun MilestoneCelebration(streak: Int, onDismiss: () -> Unit) {
    val title = when (streak) {
        3 -> "3 Day Starter"
        7 -> "7 Day Warrior"
        14 -> "14 Day Machine"
        21 -> "21 Day Legend"
        else -> "$streak Day Streak"
    }
    
    val bonus = when (streak) {
        3 -> 5; 7 -> 10; 14 -> 15; 21 -> 20; else -> 0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated Ring logic would go here, using a simpler icon for now
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = GoldColor,
                modifier = Modifier.size(100.dp)
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                "You're built different.",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    "+$bonus XP BONUS",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(Modifier.height(48.dp))
            
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
            ) {
                Text("Keep Grinding")
            }
        }
    }
}
@Composable
fun ChallengeResultsPodium(challenge: Challenge) {
    val standings = challenge.resultSnapshot?.finalLeaderboard
        ?.map { Triple(it.uid, it.streak, it.score) }
        ?: challenge.finalStanding()
    if (standings.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            challenge.resultSnapshot?.winnerTitle?.uppercase() ?: challenge.winnerTitle().uppercase(),
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.height(20.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd Place
            if (standings.size >= 2) {
                PodiumSpot(standings[1], 2, SilverColor, 80.dp, challenge)
            }
            
            // 1st Place
            if (standings.size >= 1) {
                PodiumSpot(standings[0], 1, GoldColor, 110.dp, challenge)
            }
            
            // 3rd Place
            if (standings.size >= 3) {
                PodiumSpot(standings[2], 3, BronzeColor, 70.dp, challenge)
            }
        }
    }
}

@Composable
fun PodiumSpot(standing: Triple<String, Int, Int>, rank: Int, color: Color, height: androidx.compose.ui.unit.Dp, challenge: Challenge) {
    val name = challenge.memberNames[standing.first] ?: "UnknownRival"
    val photo = challenge.memberPhotos[standing.first]
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            if (!photo.isNullOrBlank()) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, color, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)).border(2.dp, color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(name.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = color)
                }
            }
            Icon(
                Icons.Default.Stars,
                null,
                tint = color,
                modifier = Modifier.size(16.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.1f)))),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                "#$rank",
                modifier = Modifier.padding(top = 8.dp),
                fontWeight = FontWeight.Black,
                color = color
            )
        }
        
        Text(name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("${standing.third} ${challenge.scoreLabel()}", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }
}
