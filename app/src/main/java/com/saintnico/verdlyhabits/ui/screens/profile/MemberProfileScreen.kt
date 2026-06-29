package com.saintnico.verdlyhabits.ui.screens.profile

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.PublicUserProfile
import com.saintnico.verdlyhabits.data.remote.firestore.FriendRelationship
import com.saintnico.verdlyhabits.data.remote.firestore.JoinChallengeOutcome
import com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
import com.saintnico.verdlyhabits.engine.GamificationEngine
import com.saintnico.verdlyhabits.engine.ProfileTitleEngine
import com.saintnico.verdlyhabits.ui.components.profilepremium.PremiumProfileHero
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileStreakArc
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.theme.isAppearanceDark
import com.saintnico.verdlyhabits.ui.viewmodel.ChallengeViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsErrorMessages
import com.saintnico.verdlyhabits.util.UserFacingErrors
import com.saintnico.verdlyhabits.ui.components.social.ReportUserSheet
import com.saintnico.verdlyhabits.R
import androidx.compose.ui.res.painterResource
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.ProfileSocialViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Mint accent that reads well on dark; on light we fall back to the deep brand green. */
@Composable
private fun profileAccentColor(): Color =
    if (MaterialTheme.colorScheme.isAppearanceDark()) {
        Color(0xFF95D5B2)
    } else {
        MaterialTheme.colorScheme.primary
    }

@Composable
private fun cardContainer(): Color =
    MaterialTheme.colorScheme.surfaceVariant.copy(
        alpha = if (MaterialTheme.colorScheme.isAppearanceDark()) 0.32f else 0.55f,
    )

@Composable
fun MemberProfileScreen(
    memberUid: String,
    challengeViewModel: ChallengeViewModel,
    friendsViewModel: FriendsViewModel,
    onBack: () -> Unit,
    onShowNotification: (String, Boolean, ImageVector?) -> Unit,
) {
    val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val userRepository = remember { UserRepository() }
    var profile by remember { mutableStateOf<PublicUserProfile?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var relationship by remember { mutableStateOf(FriendRelationship.None) }
    val challengeState by challengeViewModel.state.collectAsState()
    var spotlight by remember { mutableStateOf<Challenge?>(null) }
    val scope = rememberCoroutineScope()
    val settingsViewModel: SettingsViewModel = viewModel()
    val profileSocialViewModel: ProfileSocialViewModel = viewModel()
    val myUsername by settingsViewModel.userUsername.collectAsState()
    var joinRequestPending by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    LaunchedEffect(memberUid) {
        if (memberUid.isBlank()) {
            loading = false
            profile = null
            loadError = "Invalid profile link."
            return@LaunchedEffect
        }
        loading = true
        loadError = null
        profile = null
        spotlight = null
        try {
            val highlightId = challengeViewModel.consumeMemberProfileHighlightChallengeId()
            if (highlightId != null) {
                spotlight = try {
                    challengeViewModel.peekChallenge(highlightId)
                } catch (_: Exception) {
                    null
                }
            }
            profile = userRepository.fetchPublicProfile(memberUid)
            relationship = try {
                friendsViewModel.relationshipWith(memberUid)
            } catch (_: Exception) {
                FriendRelationship.None
            }
        } catch (e: Exception) {
            loadError = UserFacingErrors.message(e)
            profile = null
        } finally {
            loading = false
        }
    }

    LaunchedEffect(memberUid, me, profile?.uid) {
        if (memberUid.isNotBlank() && me.isNotBlank() && me != memberUid && profile != null) {
            runCatching {
                profileSocialViewModel.recordProfileView(memberUid)
            }
        }
    }

    LaunchedEffect(Unit) {
        challengeViewModel.joinResult.collect { outcome ->
            when (outcome) {
                is JoinChallengeOutcome.Success ->
                    onShowNotification("Joined challenge", false, null)
                JoinChallengeOutcome.NotFound ->
                    onShowNotification("Challenge not found", true, null)
                JoinChallengeOutcome.Ended ->
                    onShowNotification("Challenge has ended", true, null)
                JoinChallengeOutcome.PermissionDenied ->
                    onShowNotification("Could not join", true, null)
                JoinChallengeOutcome.NotSignedIn ->
                    onShowNotification("Sign in to join", true, null)
                is JoinChallengeOutcome.Error ->
                    onShowNotification(
                        outcome.message?.let { UserFacingErrors.forRawMessage(it) } ?: "Couldn't join — try again.",
                        true,
                        null,
                    )
            }
        }
    }

    val sharedChallenges = remember(challengeState.challenges, challengeState.archivedChallenges, memberUid, me) {
        (challengeState.challenges + challengeState.archivedChallenges)
            .distinctBy { it.id }
            .filter { memberUid in it.members && me.isNotEmpty() && me in it.members }
            .sortedByDescending { it.startDate }
    }

    // Head-to-head + career stats across challenges this member appears in (with me).
    val careerStats = remember(sharedChallenges, memberUid, me) {
        computeCareerStats(sharedChallenges, memberUid, me)
    }

    val joinableFromSpotlight = remember(spotlight, me, memberUid) {
        val ch = spotlight ?: return@remember null
        if (!ch.isActive) return@remember null
        if (me.isBlank() || me in ch.members) return@remember null
        if (memberUid !in ch.members) return@remember null
        ch
    }

    LaunchedEffect(joinableFromSpotlight?.id, me) {
        joinRequestPending = false
        val ch = joinableFromSpotlight ?: return@LaunchedEffect
        joinRequestPending = friendsViewModel.hasPendingJoinRequest(ch.id)
    }

    val dateFmt = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault())
    }

    val accent = profileAccentColor()
    val onBg = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            loading -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = accent)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Opening profile…",
                    color = muted,
                    fontSize = 14.sp,
                )
            }

            loadError != null -> Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Something went wrong", fontWeight = FontWeight.Bold, color = onBg)
                Spacer(Modifier.height(8.dp))
                Text(
                    loadError ?: "",
                    color = muted,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onBack) {
                    Text("Go back", fontWeight = FontWeight.SemiBold)
                }
            }

            profile == null -> Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Profile unavailable", fontWeight = FontWeight.Bold, color = onBg)
                Spacer(Modifier.height(8.dp))
                Text(
                    "This person has not opened the app yet, or their profile is not on the server. " +
                        "They do not need a specific app version—once they sign in once, a public card can appear.",
                    color = muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onBack) {
                    Text("Go back", fontWeight = FontWeight.SemiBold)
                }
            }

            else -> {
                val p = profile!!
                val levelTitle = GamificationEngine.levelTitle(p.level)
                val memberAccent = ProfileAccents.byKey(p.profileAccent)
                val signature = remember(careerStats, p.level) {
                    ProfileTitleEngine.signatureTitle(
                        wins = careerStats.wins,
                        podiums = careerStats.podiums,
                        bestStreak = careerStats.bestStreak,
                        totalShared = careerStats.totalShared,
                        level = p.level,
                    )
                }
                val titleColor = tierColor(signature.tier)
                val displayEquippedTitle = p.equippedTitleLabel?.takeIf { it.isNotBlank() } ?: signature.label
                val highlights = remember(sharedChallenges, memberUid) {
                    collectHighlights(sharedChallenges, memberUid)
                }
                val isSelf = me == memberUid
                val showHighlights = highlights.isNotEmpty() &&
                    (isSelf || p.showRecentProof)
                var fullScreenPhoto by remember { mutableStateOf<String?>(null) }
                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                ) {
                    item {
                        Box(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(26.dp),
                                    ),
                            ) {
                                PremiumProfileHero(
                                    displayName = p.displayName,
                                    username = p.username,
                                    tagline = p.motto.ifBlank { p.bio },
                                    photoUrl = p.photoUrl,
                                    level = p.level,
                                    xp = p.xp,
                                    levelTitle = levelTitle,
                                    accent = memberAccent,
                                    equippedTitle = displayEquippedTitle,
                                    equippedTitleColor = titleColor,
                                )
                            }
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .statusBarsPadding()
                                    .padding(10.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.35f)),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                )
                            }
                            if (me != memberUid) {
                                IconButton(
                                    onClick = { showReportSheet = true },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .statusBarsPadding()
                                        .padding(10.dp)
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_phosphor_flag),
                                        contentDescription = "Report user",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        FriendActionRow(
                            relationship = relationship,
                            isSelf = me == memberUid,
                            onConnect = {
                                if (FirebaseAuth.getInstance().currentUser == null) {
                                    onShowNotification("Sign in with Google first.", true, null)
                                } else {
                                friendsViewModel.sendFriendRequest(memberUid) { ok, err ->
                                    if (ok) {
                                        onShowNotification("Request sent", false, Icons.Rounded.SentimentSatisfied)
                                        scope.launch {
                                            relationship = try {
                                                friendsViewModel.relationshipWith(memberUid)
                                            } catch (_: Exception) {
                                                FriendRelationship.None
                                            }
                                        }
                                    } else {
                                        onShowNotification(
                                            FriendsErrorMessages.forThrowable(err),
                                            true,
                                            null,
                                        )
                                    }
                                }
                                }
                            },
                            onAcceptIncoming = {
                                scope.launch {
                                    val reqId = try {
                                        friendsViewModel.findPendingIncomingRequestId(memberUid)
                                    } catch (_: Exception) {
                                        null
                                    }
                                    if (reqId == null) {
                                        onShowNotification("Request not found", true, null)
                                        return@launch
                                    }
                                    friendsViewModel.acceptRequest(reqId) { ok, err ->
                                        if (ok) {
                                            onShowNotification("Connected", false, null)
                                            scope.launch {
                                                relationship = try {
                                                    friendsViewModel.relationshipWith(memberUid)
                                                } catch (_: Exception) {
                                                    FriendRelationship.None
                                                }
                                            }
                                        } else {
                                            onShowNotification(err ?: "Accept failed", true, null)
                                        }
                                    }
                                }
                            },
                            onDeclineIncoming = {
                                scope.launch {
                                    val reqId = try {
                                        friendsViewModel.findPendingIncomingRequestId(memberUid)
                                    } catch (_: Exception) {
                                        null
                                    }
                                    if (reqId == null) return@launch
                                    friendsViewModel.declineRequest(reqId) { ok, _ ->
                                        if (ok) {
                                            onShowNotification("Declined", false, null)
                                            scope.launch {
                                                relationship = try {
                                                    friendsViewModel.relationshipWith(memberUid)
                                                } catch (_: Exception) {
                                                    FriendRelationship.None
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        )
                    }

                    if (me != memberUid) {
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                TextButton(onClick = { showReportSheet = true }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_phosphor_flag),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                    )
                                    Spacer(Modifier.size(6.dp))
                                    Text(
                                        "Report user",
                                        fontFamily = com.saintnico.verdlyhabits.ui.theme.dmSansFamily,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                                    )
                                }
                            }
                        }
                    }

                    if (showHighlights) {
                        item {
                            Spacer(Modifier.height(14.dp))
                            HighlightsReel(
                                photos = highlights,
                                accent = accent,
                                onOpen = { fullScreenPhoto = it },
                                subtitle = if (isSelf && !p.showRecentProof) {
                                    "Only you see this — hidden from your public profile"
                                } else {
                                    null
                                },
                            )
                        }
                    }

                    item {
                        if (p.favoritePlant.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                Modifier
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(cardContainer())
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    p.favoritePlant,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accent,
                                    letterSpacing = 0.4.sp,
                                )
                            }
                        }
                    }

                    // Career / titles strip — accolades earned across challenges.
                    if (careerStats.totalShared > 0) {
                        item {
                            Spacer(Modifier.height(14.dp))
                            CareerAccoladesCard(
                                stats = careerStats,
                                signatureLabel = signature.label,
                                titleColor = titleColor,
                            )
                        }
                    }

                    // Rivalry deep-dive — only when there's a real head-to-head record.
                    if (careerStats.hasRivalry && me != memberUid) {
                        item {
                            Spacer(Modifier.height(14.dp))
                            RivalrySection(
                                stats = careerStats,
                                theirName = p.displayName.ifBlank { p.username.ifBlank { "Them" } },
                            )
                        }
                    }

                    item {
                        if (p.bio.isNotBlank() && p.bio != p.motto) {
                            Spacer(Modifier.height(14.dp))
                            Card(
                                Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardContainer(),
                                ),
                            ) {
                                Column(Modifier.padding(18.dp)) {
                                    Text(
                                        "About",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp,
                                        color = accent,
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        p.bio,
                                        color = onBg.copy(alpha = 0.82f),
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                    )
                                }
                            }
                        }
                    }

                    item {
                        spotlight?.let { ch ->
                            Spacer(Modifier.height(8.dp))
                            SpotlightChallengeCard(
                                challenge = ch,
                                memberUid = memberUid,
                                me = me,
                                joinable = joinableFromSpotlight != null,
                                joinRequestPending = joinRequestPending,
                                dateFmt = dateFmt,
                                onJoin = {
                                    val raw = ch.inviteCode.ifBlank { ch.id }
                                    challengeViewModel.joinChallenge(raw)
                                },
                                onRequestJoin = {
                                    friendsViewModel.sendChallengeJoinRequest(
                                        challengeId = ch.id,
                                        challengeTitle = ch.habitName,
                                        hostUid = ch.creatorId,
                                        targetMemberUid = memberUid,
                                        fromUsername = myUsername.ifBlank { "rival" },
                                    ) { ok, err ->
                                        if (ok) {
                                            joinRequestPending = true
                                            onShowNotification("Request sent", false, Icons.Rounded.SentimentSatisfied)
                                        } else {
                                            onShowNotification(
                                                FriendsErrorMessages.forThrowable(err),
                                                true,
                                                null,
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }

                    item {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            "Shared challenges",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        )
                    }

                    if (sharedChallenges.isEmpty()) {
                        item {
                            Text(
                                "No shared challenges yet.",
                                modifier = Modifier.padding(horizontal = 20.dp),
                                color = muted,
                                fontSize = 14.sp,
                            )
                        }
                    } else {
                        items(sharedChallenges, key = { it.id }) { ch ->
                            SharedChallengeRow(challenge = ch, memberUid = memberUid, dateFmt = dateFmt)
                        }
                    }

                    item { Spacer(Modifier.height(40.dp)) }
                }

                fullScreenPhoto?.let { url ->
                    HighlightViewerDialog(url = url, onDismiss = { fullScreenPhoto = null })
                }
            }
        }
    }

    if (showReportSheet && profile != null) {
        ReportUserSheet(
            reportedUid = memberUid,
            reportedUsername = profile!!.username.ifBlank { profile!!.displayName },
            onDismiss = { showReportSheet = false },
            onSubmitted = {
                onShowNotification(
                    "Report submitted. Thank you for keeping Verdly safe.",
                    false,
                    null,
                )
            },
        )
    }
}

@Composable
private fun FriendActionRow(
    relationship: FriendRelationship,
    isSelf: Boolean,
    onConnect: () -> Unit,
    onAcceptIncoming: () -> Unit,
    onDeclineIncoming: () -> Unit,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val outline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    if (!isSelf) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            when (relationship) {
                FriendRelationship.Friends -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            disabledContentColor = onBg.copy(alpha = 0.75f),
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Connected", fontWeight = FontWeight.Bold)
                    }
                }

                FriendRelationship.IncomingPending -> {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onAcceptIncoming,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40916C)),
                        ) {
                            Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Accept", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        OutlinedButton(
                            onClick = onDeclineIncoming,
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, outline),
                        ) {
                            Text("Decline", fontWeight = FontWeight.SemiBold, color = onBg)
                        }
                    }
                }

                FriendRelationship.OutgoingPending -> {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, outline),
                    ) {
                        Text("Request sent", fontWeight = FontWeight.SemiBold, color = onBg.copy(0.7f))
                    }
                }

                FriendRelationship.Self -> {
                    Spacer(Modifier.height(0.dp))
                }

                FriendRelationship.None -> {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40916C)),
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(Modifier.size(8.dp))
                        Text("Connect", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Accolades summary across challenges this member has shared with me — signature
 * title, a win-rate ring, and count-up career stats. Gives the profile a "career" feel.
 */
@Composable
private fun CareerAccoladesCard(
    stats: CareerStats,
    signatureLabel: String,
    titleColor: Color,
) {
    val accent = profileAccentColor()
    val onBg = MaterialTheme.colorScheme.onBackground
    Card(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainer()),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFC857), modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    "Track record",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = accent,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stats.endedCount > 0) {
                    WinRateRing(progress = stats.winRate, accent = titleColor)
                    Spacer(Modifier.size(14.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        signatureLabel,
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = onBg,
                    )
                    Text(
                        if (stats.endedCount > 0) {
                            "${(stats.winRate * 100).toInt()}% win rate · ${stats.endedCount} finished"
                        } else {
                            "${stats.totalShared} ${if (stats.totalShared == 1) "arena" else "arenas"} entered"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth()) {
                StatCell(value = stats.wins, label = "Wins", modifier = Modifier.weight(1f))
                StatCell(value = stats.podiums, label = "Podiums", modifier = Modifier.weight(1f))
                StatCell(value = stats.bestStreak, label = "Best streak", modifier = Modifier.weight(1f))
                StatCell(value = stats.totalShared, label = "Arenas", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(value: Int, label: String, modifier: Modifier = Modifier) {
    var target by remember { mutableStateOf(0) }
    LaunchedEffect(value) { target = value }
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700),
        label = "statCount",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "$animated",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Circular win-rate dial with the percentage in the center. */
@Composable
private fun WinRateRing(progress: Float, accent: Color, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 900),
        label = "winRing",
    )
    val track = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)
    Box(modifier.size(56.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(56.dp)) {
            val stroke = 6.dp.toPx()
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(accent, accent.copy(alpha = 0.5f), accent)),
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            "${(animated * 100).toInt()}%",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/**
 * Instagram-style story reel of recent proof photos. Tapping a thumbnail opens it
 * full-screen. Gives the profile a living, "what have they been doing" feel.
 */
@Composable
private fun HighlightsReel(
    photos: List<HighlightPhoto>,
    accent: Color,
    onOpen: (String) -> Unit,
    subtitle: String? = null,
) {
    Column {
        Text(
            "Recent proof",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = accent,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(photos, key = { it.url }) { photo ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        accent,
                                        Color(0xFFFFC857),
                                        accent,
                                    ),
                                ),
                            )
                            .padding(2.5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            .padding(2.dp)
                            .clickable { onOpen(photo.url) },
                    ) {
                        AsyncImage(
                            model = photo.url,
                            contentDescription = "Proof",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        photo.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun HighlightViewerDialog(url: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Proof",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** Rivalry head-to-head card — record bar, last meeting, streak comparison. */
@Composable
private fun RivalrySection(stats: CareerStats, theirName: String) {
    val accent = profileAccentColor()
    val onBg = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val total = (stats.h2hWins + stats.h2hLosses).coerceAtLeast(1)
    val youLead = stats.h2hWins >= stats.h2hLosses
    val winColor = Color(0xFF40916C)
    val lossColor = MaterialTheme.colorScheme.error
    Card(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainer()),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SportsScore, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    "Rivalry",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = accent,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("YOU", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = muted, letterSpacing = 1.sp)
                    Text("${stats.h2hWins}", fontWeight = FontWeight.Black, fontSize = 30.sp, color = winColor)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (youLead) "Leading" else "Trailing",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (youLead) winColor else lossColor,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(theirName.uppercase().take(10), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = muted, letterSpacing = 1.sp)
                    Text("${stats.h2hLosses}", fontWeight = FontWeight.Black, fontSize = 30.sp, color = lossColor)
                }
            }
            Spacer(Modifier.height(10.dp))
            // Record bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp)),
            ) {
                Box(
                    Modifier
                        .weight(stats.h2hWins.toFloat().coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(winColor),
                )
                Box(
                    Modifier
                        .weight(stats.h2hLosses.toFloat().coerceAtLeast(0.001f))
                        .fillMaxHeight()
                        .background(lossColor.copy(alpha = 0.8f)),
                )
            }
            if (stats.lastMeeting != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Last meeting",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = muted,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(stats.lastMeeting, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = onBg)
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                StreakCompareCell("Your best streak", stats.myBestStreak, winColor, Modifier.weight(1f))
                Spacer(Modifier.size(12.dp))
                StreakCompareCell("Their best streak", stats.bestStreak, accent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StreakCompareCell(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$value", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = color)
            Spacer(Modifier.size(4.dp))
            Text("days", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

// ─── Title / highlight helpers ───────────────────────────────────────────────

@Composable
private fun tierColor(tier: ProfileTitleEngine.Tier): Color = when (tier) {
    ProfileTitleEngine.Tier.LEGENDARY -> Color(0xFFFFC857)
    ProfileTitleEngine.Tier.EPIC -> Color(0xFFB388FF)
    ProfileTitleEngine.Tier.RARE -> Color(0xFF4FC3F7)
    ProfileTitleEngine.Tier.COMMON -> profileAccentColor()
}

private data class HighlightPhoto(val url: String, val label: String, val dateKey: String)

/** Newest proof photos for [memberUid] across shared challenges, most recent first. */
private fun collectHighlights(shared: List<Challenge>, memberUid: String): List<HighlightPhoto> {
    val out = mutableListOf<HighlightPhoto>()
    shared.forEach { ch ->
        ch.proofPhotos[memberUid]?.forEach { (dateKey, url) ->
            if (url.isNotBlank()) {
                out.add(HighlightPhoto(url = url, label = ch.habitName, dateKey = dateKey))
            }
        }
    }
    return out
        .sortedByDescending { it.dateKey }
        .distinctBy { it.url }
        .take(12)
}

/** Small medal/placement chip used on ended challenges. Champions get a gentle glow. */
@Composable
private fun PlacementChip(place: Int, title: String) {
    val (chipColor, icon) = when (place) {
        1 -> Color(0xFFFFC857) to Icons.Default.EmojiEvents
        2 -> Color(0xFFC0C7D0) to Icons.Default.MilitaryTech
        3 -> Color(0xFFCD9B6A) to Icons.Default.MilitaryTech
        else -> MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.SportsScore
    }
    val glowAlpha = if (place == 1) {
        val shimmer = rememberInfiniteTransition(label = "championGlow")
        shimmer.animateFloat(
            initialValue = 0.16f,
            targetValue = 0.34f,
            animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Reverse),
            label = "glow",
        ).value
    } else {
        0.16f
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(chipColor.copy(alpha = glowAlpha))
            .border(
                1.dp,
                if (place == 1) chipColor.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = chipColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(5.dp))
        Text(
            title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SpotlightChallengeCard(
    challenge: Challenge,
    memberUid: String,
    me: String,
    joinable: Boolean,
    joinRequestPending: Boolean,
    dateFmt: DateTimeFormatter,
    onJoin: () -> Unit,
    onRequestJoin: () -> Unit,
) {
    val accent = profileAccentColor()
    val onBg = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val streak = challenge.streakFor(memberUid)
    val rank = challenge.rankOf(memberUid)
    val rankLabel = if (rank > 0) "#$rank" else "—"
    val total = challengeTotalDays(challenge)
    val progress = if (total > 0) streak.toFloat() / total.toFloat() else 0f
    val finalPlace = if (!challenge.isActive) memberFinalPlacement(challenge, memberUid) else null
    Card(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardContainer(),
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (challenge.isActive) "In this challenge" else "Finished challenge",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
                if (finalPlace != null) {
                    PlacementChip(finalPlace, placementTitleFor(challenge, finalPlace))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                challenge.habitName,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = onBg,
            )
            Text(
                "${dateFmt.format(Instant.ofEpochMilli(challenge.startDate))} → ${dateFmt.format(Instant.ofEpochMilli(challenge.endDate))}",
                fontSize = 12.sp,
                color = muted,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileStreakArc(progress = progress, accent = accent)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        if (challenge.isActive) "Rank $rankLabel" else "Finished $rankLabel",
                        fontWeight = FontWeight.Bold,
                        color = onBg,
                    )
                    Text(
                        "$streak day streak in this arena",
                        fontSize = 12.sp,
                        color = muted,
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.SportsScore, contentDescription = null, tint = accent)
            }
            if (joinable && me.isNotBlank()) {
                Spacer(Modifier.height(14.dp))
                if (joinRequestPending) {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    ) {
                        Text("Join request sent", fontWeight = FontWeight.SemiBold, color = onBg.copy(0.7f))
                    }
                } else {
                    Button(
                        onClick = onRequestJoin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40916C)),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Ask to join this arena", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onJoin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                ) {
                    Text("Join instantly", fontWeight = FontWeight.Medium, color = accent)
                }
            }
        }
    }
}

@Composable
private fun SharedChallengeRow(
    challenge: Challenge,
    memberUid: String,
    dateFmt: DateTimeFormatter,
) {
    val accent = profileAccentColor()
    val onBg = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val streak = challenge.streakFor(memberUid)
    val finalPlace = if (!challenge.isActive) memberFinalPlacement(challenge, memberUid) else null
    Card(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardContainer()),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    challenge.habitName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    color = onBg,
                )
                if (finalPlace != null) {
                    PlacementChip(finalPlace, placementTitleFor(challenge, finalPlace))
                } else {
                    Text(
                        "Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "${dateFmt.format(Instant.ofEpochMilli(challenge.startDate))} · $streak day streak",
                fontSize = 12.sp,
                color = muted,
            )
        }
    }
}

// ─── Placement + career helpers ──────────────────────────────────────────────

private fun challengeTotalDays(challenge: Challenge): Int {
    val dayMs = 24 * 60 * 60 * 1000L
    return (((challenge.endDate - challenge.startDate) / dayMs).toInt()).coerceAtLeast(1)
}

/** Final 1-based placement for [uid] in an ended challenge, or null if not derivable. */
private fun memberFinalPlacement(challenge: Challenge, uid: String): Int? {
    val order = challenge.resultSnapshot?.finalLeaderboard
        ?.map { it.uid }
        ?.takeIf { it.isNotEmpty() }
        ?: challenge.finalStanding().map { it.first }
    val idx = order.indexOf(uid)
    return if (idx >= 0) idx + 1 else null
}

private fun placementTitleFor(challenge: Challenge, place: Int): String = when (place) {
    1 -> challenge.resultSnapshot?.winnerTitle?.takeIf { it.isNotBlank() } ?: challenge.winnerTitle()
    2 -> "Runner-up"
    3 -> "3rd place"
    else -> "#$place"
}

private data class CareerStats(
    val totalShared: Int,
    val endedCount: Int,
    val wins: Int,
    val podiums: Int,
    val bestStreak: Int,
    val myBestStreak: Int,
    val h2hWins: Int,
    val h2hLosses: Int,
    val lastMeeting: String?,
) {
    val hasRivalry: Boolean get() = h2hWins + h2hLosses > 0
    val winRate: Float get() = if (endedCount > 0) wins.toFloat() / endedCount.toFloat() else 0f
}

/** Aggregate accolades for [memberUid] across challenges they share with [me]. */
private fun computeCareerStats(
    shared: List<Challenge>,
    memberUid: String,
    me: String,
): CareerStats {
    var wins = 0
    var podiums = 0
    var bestStreak = 0
    var myBestStreak = 0
    var endedCount = 0
    var h2hWins = 0
    var h2hLosses = 0
    var lastMeeting: String? = null
    var lastMeetingDate = Long.MIN_VALUE

    shared.forEach { ch ->
        bestStreak = maxOf(bestStreak, ch.streakFor(memberUid))
        if (me.isNotBlank()) myBestStreak = maxOf(myBestStreak, ch.streakFor(me))
        if (!ch.isActive) {
            endedCount++
            val place = memberFinalPlacement(ch, memberUid)
            if (place == 1) wins++
            if (place != null && place <= 3) podiums++
            // Head-to-head: compare final placement of me vs them in ended shared arenas.
            val mine = if (me.isNotBlank()) memberFinalPlacement(ch, me) else null
            if (place != null && mine != null && me != memberUid && mine != place) {
                val iWon = mine < place
                if (iWon) h2hWins++ else h2hLosses++
                val when_ = ch.endedAt ?: ch.endDate
                if (when_ > lastMeetingDate) {
                    lastMeetingDate = when_
                    lastMeeting = if (iWon) {
                        "You beat them in \u201C${ch.habitName}\u201D"
                    } else {
                        "They beat you in \u201C${ch.habitName}\u201D"
                    }
                }
            }
        }
    }

    return CareerStats(
        totalShared = shared.size,
        endedCount = endedCount,
        wins = wins,
        podiums = podiums,
        bestStreak = bestStreak,
        myBestStreak = myBestStreak,
        h2hWins = h2hWins,
        h2hLosses = h2hLosses,
        lastMeeting = lastMeeting,
    )
}
