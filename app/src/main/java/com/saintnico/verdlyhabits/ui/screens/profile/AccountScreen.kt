package com.saintnico.verdlyhabits.ui.screens.profile

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.engine.Achievement
import com.saintnico.verdlyhabits.ui.components.AchievementTrophy
import com.saintnico.verdlyhabits.ui.components.AchievementMedallion
import com.saintnico.verdlyhabits.ui.components.TrophyHallFramedCard
import com.saintnico.verdlyhabits.ui.components.TrophyHallHeaderRow
import com.saintnico.verdlyhabits.ui.components.TrophyHallProgressBar
import com.saintnico.verdlyhabits.ui.components.TrophyRankChip
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.engine.ProfileSocialEngine
import com.saintnico.verdlyhabits.engine.ProfileTitleEngine
import com.saintnico.verdlyhabits.util.DebugSessionLog
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileIdentityCard
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileSocialProofSection
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileTitlePickerSheet
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsUiState
import com.saintnico.verdlyhabits.ui.components.notifications.NotificationBellButton
import com.saintnico.verdlyhabits.ui.components.social.FriendsConnectionsSection
import com.saintnico.verdlyhabits.ui.components.visualTierForAchievement
import com.saintnico.verdlyhabits.ui.theme.DangerRed
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.vector.ImageVector

private data class ArenaCardTheme(
    val gradient: List<Color>,
    val accent: Color,
    val glow: Color
)

/** Richer palettes so consecutive cards read as distinct “battle passes”. */
private val arenaThemes = listOf(
    ArenaCardTheme(
        gradient = listOf(Color(0xFF0D1B2A), Color(0xFF1B4332), Color(0xFF2D6A4F)),
        accent = Color(0xFF95D5B2),
        glow = Color(0xFF40916C)
    ),
    ArenaCardTheme(
        gradient = listOf(Color(0xFF1A0A2E), Color(0xFF2D1B4E), Color(0xFF4A148C)),
        accent = Color(0xFFE1BEE7),
        glow = Color(0xFFAB47BC)
    ),
    ArenaCardTheme(
        gradient = listOf(Color(0xFF1B263B), Color(0xFF243B55), Color(0xFF415A77)),
        accent = Color(0xFF90CAF9),
        glow = Color(0xFF42A5F5)
    ),
    ArenaCardTheme(
        gradient = listOf(Color(0xFF3E2723), Color(0xFF5D4037), Color(0xFF6D4C41)),
        accent = Color(0xFFFFCC80),
        glow = Color(0xFFFF9800)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    statsState: UserStatsUiState,
    activeChallenges: List<Challenge>,
    allUserChallenges: List<Challenge>,
    userId: String,
    userName: String,
    userUsername: String,
    userPhotoUri: String?,
    userBio: String,
    equippedTitleId: String?,
    onEquipTitle: (titleId: String?, titleLabel: String?) -> Unit,
    billingViewModel: BillingViewModel,
    friendsViewModel: FriendsViewModel,
    friendSummaries: List<FriendSummary>,
    duoState: DuoStreakState?,
    referralState: ReferralUiState,
    weeklyProfileViews: Int,
    hasPerfectWeek: Boolean,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToChallenges: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToMemberProfile: (String) -> Unit,
    onOpenChallengeDetail: (String) -> Unit,
    onLogout: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit = { _, _ -> },
    hasDuoBuddy: Boolean = false,
    myUsername: String = "",
    myPhotoUrl: String? = null,
    onInviteAccountabilityBuddy: ((uid: String, username: String, photoUrl: String?, onResult: (Boolean, String?) -> Unit) -> Unit)? = null,
    notificationBadgeCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    notificationsViewModel: com.saintnico.verdlyhabits.ui.viewmodel.NotificationsViewModel,
) {
    val context = LocalContext.current
    val bg = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground
    val isRankOneSomewhere = activeChallenges.any { ch ->
        ch.leaderboard().firstOrNull()?.first == userId
    }
    val avatarBorder = if (isRankOneSomewhere) GoldColor else primary

    val displayedXp by animateIntAsState(statsState.totalXp, tween(1200), label = "xp")
    val displayedStreak by animateIntAsState(statsState.longestStreakEver, tween(1200), label = "streak")
    val displayedChallenges by animateIntAsState(activeChallenges.size, tween(1200), label = "ch")
    val displayedCompletions by animateIntAsState(statsState.totalCompletions, tween(1200), label = "comp")

    var avatarEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { avatarEntered = true }

    // #region agent log
    LaunchedEffect(userId) {
        DebugSessionLog.log(
            location = "AccountScreen.kt:entry",
            message = "AccountScreen opened",
            hypothesisId = "H2",
            data = mapOf("hasUserId" to userId.isNotBlank()),
        )
    }
    // #endregion

    val avatarScale by animateFloatAsState(
        targetValue = if (avatarEntered) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "avatarScale"
    )

    var badgeDetail by remember { mutableStateOf<Achievement?>(null) }
    var showTitlePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val titleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isPro by billingViewModel.isPro.collectAsState()
    val career = remember(allUserChallenges, userId) {
        ProfileSocialEngine.careerInputs(allUserChallenges, userId)
    }
    val signature = remember(career, statsState.level) {
        ProfileTitleEngine.signatureTitle(
            wins = career.wins,
            podiums = career.podiums,
            bestStreak = career.bestStreak,
            totalShared = career.totalShared,
            level = statsState.level,
        )
    }
    val unlockedTitles = remember(
        career, statsState.level, statsState.totalFocusMinutes, statsState.totalCompletions,
        statsState.achievements, duoState, isPro, hasPerfectWeek, statsState.longestStreakEver,
    ) {
        ProfileTitleEngine.unlockedTitles(
            wins = career.wins,
            podiums = career.podiums,
            bestStreak = career.bestStreak.coerceAtLeast(statsState.longestStreakEver),
            totalShared = career.totalShared,
            level = statsState.level,
            duoStreakDays = duoState?.streakDays ?: 0,
            totalFocusMinutes = statsState.totalFocusMinutes,
            totalCompletions = statsState.totalCompletions,
            isPro = isPro,
            achievements = statsState.achievements,
        )
    }
    val equippedTitle = remember(equippedTitleId, unlockedTitles, signature) {
        ProfileTitleEngine.resolveEquipped(equippedTitleId, unlockedTitles, signature)
    }
    val socialSnapshot = remember(
        userId, userName, statsState.memberSince, statsState.totalCompletions,
        statsState.longestStreakEver, friendSummaries, duoState, allUserChallenges,
        referralState.qualifiedCount, isPro, hasPerfectWeek,
    ) {
        runCatching {
            ProfileSocialEngine.buildSnapshot(
                userId = userId,
                userName = userName,
                memberSinceMillis = statsState.memberSince,
                totalCompletions = statsState.totalCompletions,
                longestStreak = statsState.longestStreakEver,
                friends = friendSummaries,
                duoState = duoState,
                allChallenges = allUserChallenges,
                referralsSent = referralState.qualifiedCount,
                isPro = isPro,
                hasPerfectWeek = hasPerfectWeek,
            )
        }.getOrElse {
            ProfileSocialEngine.ProfileSocialSnapshot(
                stats = ProfileSocialEngine.SocialStats(
                    friendsCount = friendSummaries.size,
                    duoStreakDays = duoState?.streakDays ?: 0,
                    challengeWins = 0,
                    referralsSent = referralState.qualifiedCount,
                ),
                peopleMotivatedToday = 0,
                activityFeed = emptyList(),
                flairs = emptyList(),
                memberStoryLine = ProfileSocialEngine.memberStoryLine(
                    statsState.memberSince,
                    statsState.totalCompletions,
                    statsState.longestStreakEver,
                ),
            )
        }
    }

    val memberMonthYear = remember(statsState.memberSince) {
        Instant.ofEpochMilli(statsState.memberSince)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    val inBonus = com.saintnico.verdlyhabits.referral.ReferralManager.isInBonusPeriod(context)
    val proSubtitle = when {
        isPro -> "Full Pro access on this device"
        billingViewModel.isInFreeTrial() && !isPro ->
            "Pro trial · ${billingViewModel.freeTrialDaysRemaining()} day${if (billingViewModel.freeTrialDaysRemaining() == 1) "" else "s"} left"
        inBonus && !isPro -> "Referral bonus — Pro perks unlocked"
        else -> "Sounds, deep stats & unlimited habits"
    }

    val displayBio = userBio.ifBlank { "Consistency is the craft." }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ProfileIdentityCard(
                    primary = primary,
                    background = bg,
                    userName = userName,
                    userUsername = userUsername,
                    userPhotoUri = userPhotoUri,
                    bio = displayBio,
                    level = statsState.level,
                    levelTitle = statsState.levelTitle,
                    levelProgress = statsState.progressToNextLevel,
                    equippedTitle = equippedTitle,
                    flairs = socialSnapshot.flairs,
                    memberStoryLine = socialSnapshot.memberStoryLine,
                    avatarScale = avatarScale,
                    avatarBorder = avatarBorder,
                    onEditClick = onNavigateToEditProfile,
                    onOpenTitlePicker = { showTitlePicker = true },
                    notificationBadgeCount = notificationBadgeCount,
                    onOpenNotifications = onOpenNotifications,
                )
            }

            item {
                Spacer(Modifier.height((-24).dp))
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatColumn("${displayedXp}", "XP", primary, onBg)
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(onBg.copy(alpha = 0.1f))
                        )
                        StatColumn("${displayedStreak}", "STREAK", primary, onBg)
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(onBg.copy(alpha = 0.1f))
                        )
                        StatColumn("${displayedChallenges}", "CHALLENGES", primary, onBg)
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(24.dp)
                                .background(onBg.copy(alpha = 0.1f))
                        )
                        StatColumn("${displayedCompletions}", "COMPLETIONS", primary, onBg)
                    }
                }
            }

            item {
                Spacer(Modifier.height(22.dp))
                val notifState by notificationsViewModel.state.collectAsState()
                val pulseFeed = remember(
                    socialSnapshot.activityFeed,
                    notifState.items,
                    notifState.announcements,
                    notifState.dismissedAnnouncementIds,
                ) {
                    notificationsViewModel.pulseFeed(socialSnapshot.activityFeed)
                }
                ProfileSocialProofSection(
                    snapshot = socialSnapshot,
                    weeklyProfileViews = weeklyProfileViews,
                    isPro = isPro,
                    primary = primary,
                    onBg = onBg,
                    onUnlockPro = onNavigateToSubscription,
                    pulseFeed = pulseFeed,
                    onSeeAllActivity = {
                        notificationsViewModel.openActivityTab()
                        onOpenNotifications()
                    },
                )
            }

            item {
                Spacer(Modifier.height(22.dp))
                FriendsConnectionsSection(
                    friendsViewModel = friendsViewModel,
                    primary = primary,
                    onBg = onBg,
                    onNavigateToMemberProfile = onNavigateToMemberProfile,
                    onShowNotification = onShowNotification,
                    hasDuoBuddy = hasDuoBuddy,
                    myUsername = myUsername,
                    myPhotoUrl = myPhotoUrl,
                    onInviteAccountabilityBuddy = onInviteAccountabilityBuddy,
                )
            }

            item {
                Spacer(Modifier.height(28.dp))
                ArenaSectionHeader(
                    activeCount = activeChallenges.size,
                    primary = primary,
                    onBg = onBg,
                    onOpenChallenges = onNavigateToChallenges
                )
                Spacer(Modifier.height(12.dp))
                if (activeChallenges.isEmpty()) {
                    ArenaEmptyState(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        primary = primary,
                        onBg = onBg,
                        onOpenChallenges = onNavigateToChallenges
                    )
                } else {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(activeChallenges, key = { _, ch -> ch.id }) { idx, ch ->
                            ArenaChallengeCard(
                                challenge = ch,
                                userId = userId,
                                theme = arenaThemes[idx % arenaThemes.size],
                                onOpenChallenge = { onOpenChallengeDetail(ch.id) }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(28.dp))
                BadgesShowcase(
                    achievements = statsState.achievements,
                    primary = primary,
                    onBg = onBg,
                    onNavigateToAchievements = onNavigateToAchievements,
                    onBadgeClick = { badgeDetail = it }
                )
            }

            item {
                Spacer(Modifier.height(28.dp))
                Text(
                    "ACCOUNT",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = onBg.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(8.dp))
                val rows: List<Pair<Triple<ImageVector, String, String>, () -> Unit>> = listOf(
                    Triple(Icons.Default.WorkspacePremium, "Verdly Pro", proSubtitle) to onNavigateToSubscription,
                    Triple(Icons.Default.Edit, "Edit Profile", "Photo, name, and bio") to onNavigateToEditProfile,
                    Triple(Icons.Default.Notifications, "Notifications", "Reminders and alerts") to onNavigateToSettings,
                    Triple(Icons.Default.PrivacyTip, "Privacy", "Data and visibility") to onNavigateToSettings,
                    Triple(Icons.Default.Translate, "Language", "App language") to onNavigateToSettings,
                    Triple(Icons.Default.Palette, "Appearance", "Theme and display") to onNavigateToSettings,
                    Triple(Icons.Default.Help, "Help & Support", "FAQs and contact") to onNavigateToSettings,
                )
                Column(Modifier.padding(horizontal = 16.dp)) {
                    rows.forEachIndexed { index, (triple, onClick) ->
                        val (icon, title, subtitle) = triple
                        SettingsRow(
                            icon = icon,
                            title = title,
                            subtitle = subtitle,
                            primary = primary,
                            danger = false,
                            showChevron = true,
                            onClick = onClick
                        )
                        if (index < rows.lastIndex) {
                            HorizontalDivider(color = onBg.copy(alpha = 0.06f))
                        }
                    }
                    HorizontalDivider(color = onBg.copy(alpha = 0.06f))
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Sign Out",
                        subtitle = "Sign out of this device",
                        primary = DangerRed,
                        danger = true,
                        showChevron = false,
                        onClick = onLogout
                    )
                }
            }

            item {
                Spacer(Modifier.height(32.dp))
                Text(
                    "Member since $memberMonthYear",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = onBg.copy(alpha = 0.35f)
                )
                Text(
                    "Verdly v$versionName",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = onBg.copy(alpha = 0.35f)
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showTitlePicker) {
        ProfileTitlePickerSheet(
            unlockedTitles = unlockedTitles,
            equippedId = equippedTitle.id,
            sheetState = titleSheetState,
            onDismiss = { showTitlePicker = false },
            onSelect = { id, label ->
                onEquipTitle(id, label)
                showTitlePicker = false
            },
            onAuto = {
                onEquipTitle(null, null)
                showTitlePicker = false
            },
        )
    }

    if (badgeDetail != null) {
        val a = badgeDetail!!
        ModalBottomSheet(
            onDismissRequest = { badgeDetail = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val badgeTier = visualTierForAchievement(a.id, a.xpReward)
                AchievementTrophy(
                    icon = a.icon,
                    tier = badgeTier,
                    unlocked = a.isUnlocked,
                    size = 96.dp
                )
                Spacer(Modifier.height(10.dp))
                TrophyRankChip(tier = badgeTier, unlocked = a.isUnlocked)
                Spacer(Modifier.height(8.dp))
                Text(
                    a.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = onBg
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    a.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBg.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
                if (!a.isUnlocked && a.progressTarget > 1) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Progress: ${a.progressCurrent} / ${a.progressTarget}",
                        style = MaterialTheme.typography.bodySmall,
                        color = onBg.copy(alpha = 0.55f)
                    )
                    Spacer(Modifier.height(6.dp))
                    val p = (a.progressCurrent.toFloat() / a.progressTarget).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = primary,
                        trackColor = onBg.copy(alpha = 0.08f)
                    )
                }
                a.unlockedAt?.let { at ->
                    Spacer(Modifier.height(16.dp))
                    val d = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    Text(
                        "Earned on $d",
                        style = MaterialTheme.typography.labelMedium,
                        color = onBg.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, primary: Color, onBg: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            color = primary,
            fontWeight = FontWeight.Normal
        )
        Text(
            label,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            color = onBg.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ArenaSectionHeader(
    activeCount: Int,
    primary: Color,
    onBg: Color,
    onOpenChallenges: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(color = primary.copy(alpha = 0.12f)),
                    onClick = onOpenChallenges,
                    onClickLabel = "Open challenges"
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(primary.copy(alpha = 0.25f), GoldColor.copy(alpha = 0.18f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Whatshot,
                    contentDescription = null,
                    tint = GoldColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "IN THE ARENA",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        letterSpacing = 2.2.sp,
                        color = primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = onBg.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    if (activeCount == 0) "Challenge the crew — prove the habit sticks. Tap to open Challenges."
                    else "$activeCount active ${if (activeCount == 1) "rivalry" else "rivalries"} · climb the board daily. Tap to open.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onBg.copy(alpha = 0.55f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ArenaEmptyState(
    modifier: Modifier = Modifier,
    primary: Color,
    onBg: Color,
    onOpenChallenges: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = primary.copy(alpha = 0.12f)),
                onClick = onOpenChallenges,
                onClickLabel = "Open challenges"
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, primary.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface,
                            GoldColor.copy(alpha = 0.06f)
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "No rivalries yet",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = onBg
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Join or create a challenge from the Challenges tab — or tap this card.",
                    style = MaterialTheme.typography.bodySmall,
                    color = onBg.copy(alpha = 0.55f),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun BadgesShowcase(
    achievements: List<Achievement>,
    primary: Color,
    onBg: Color,
    onNavigateToAchievements: () -> Unit,
    onBadgeClick: (Achievement) -> Unit
) {
    val sorted = remember(achievements) {
        achievements.sortedWith(
            compareByDescending<Achievement> { it.isUnlocked }
                .thenByDescending { it.xpReward }
                .thenBy { it.title }
        )
    }
    val unlocked = achievements.count { it.isUnlocked }
    val total = achievements.size
    TrophyHallFramedCard {
        TrophyHallHeaderRow(
            primary = primary,
            onBg = onBg,
            unlocked = unlocked,
            total = total,
            trailing = {
                TextButton(onClick = onNavigateToAchievements) {
                    Text("View all", fontWeight = FontWeight.SemiBold)
                }
            }
        )
        TrophyHallProgressBar(unlocked = unlocked, total = total, onBg = onBg)
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 18.dp,
                vertical = 4.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(sorted.size, key = { sorted[it].id }) { i ->
                val a = sorted[i]
                AchievementMedallion(
                    achievement = a,
                    onClick = { onBadgeClick(a) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ArenaChallengeCard(
    challenge: Challenge,
    userId: String,
    theme: ArenaCardTheme,
    onOpenChallenge: () -> Unit
) {
    val rank = challenge.rankOf(userId).takeIf { it > 0 } ?: 0
    val streak = challenge.streakFor(userId)
    val dayMs = 24 * 60 * 60 * 1000L
    val totalDays = (((challenge.endDate - challenge.startDate) / dayMs).toInt()).coerceAtLeast(1)
    val elapsed = challenge.daysElapsed().coerceAtMost(totalDays)
    val frac = (elapsed.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
    val daysLeft = challenge.daysRemaining().toInt()
    val isLeader = rank == 1 && challenge.members.size > 1
    val cardInteraction = remember { MutableInteractionSource() }

    val rankGold = Color(0xFFFFD700)
    val rankSilver = Color(0xFFC0C0C0)
    val rankBronze = Color(0xFFCD7F32)
    val rankAccent = when (rank) {
        1 -> rankGold
        2 -> rankSilver
        3 -> rankBronze
        else -> Color.White.copy(alpha = 0.9f)
    }

    Card(
        modifier = Modifier
            .width(210.dp)
            .height(186.dp)
            .clip(RoundedCornerShape(26.dp))
            .clickable(
                interactionSource = cardInteraction,
                indication = ripple(color = Color.White.copy(alpha = 0.18f)),
                onClick = onOpenChallenge,
                onClickLabel = "Open challenge"
            ),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(
            width = if (isLeader) 2.dp else 1.dp,
            color = if (isLeader) rankGold.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.14f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(theme.gradient))
        ) {
            // Readability veil
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.05f),
                                Color.Black.copy(alpha = 0.42f)
                            )
                        )
                    )
            )
            // Soft accent glow (top corner)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(theme.glow.copy(alpha = 0.45f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2E7D32).copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF69F0AE))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "LIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                color = Color.White
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (daysLeft > 0) "${daysLeft}d left" else "Ending",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    challenge.habitName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                if (challenge.stake.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        challenge.stake,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { frac },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = theme.accent,
                    trackColor = Color.White.copy(alpha = 0.18f),
                )
                Text(
                    "Day $elapsed of $totalDays",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.55f),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.32f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isLeader) {
                                Icon(
                                    Icons.Rounded.EmojiEvents,
                                    contentDescription = null,
                                    tint = rankGold,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                if (rank > 0) "#$rank" else "—",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = rankAccent,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFF6E40),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            "$streak",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    primary: Color,
    danger: Boolean,
    showChevron: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = ripple(color = (if (danger) DangerRed else primary).copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (danger) primary.copy(alpha = 0.12f) else primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                null,
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f)
            )
        }
    }
}
