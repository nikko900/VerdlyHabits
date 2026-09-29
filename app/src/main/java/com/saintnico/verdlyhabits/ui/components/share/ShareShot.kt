package com.saintnico.verdlyhabits.ui.components.share

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.data.model.PublicUserProfile
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.engine.Achievement
import com.saintnico.verdlyhabits.engine.GamificationEngine
import com.saintnico.verdlyhabits.engine.GoalCelebrationCopy
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.engine.ProfileSocialEngine
import com.saintnico.verdlyhabits.engine.ProfileTitleEngine
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.components.profilepremium.PremiumProfileHero
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileIdentityCard
import com.saintnico.verdlyhabits.ui.screens.goals.celebration.StatCard
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary
import com.saintnico.verdlyhabits.ui.viewmodel.GoalCompletionStats
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsUiState
import com.saintnico.verdlyhabits.utils.ProfileShareCardExporter
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ProfileShotData(
    val userName: String,
    val userUsername: String,
    val userPhotoUri: String?,
    val bio: String,
    val level: Int,
    val levelTitle: String,
    val levelProgress: Float,
    val equippedTitle: ProfileTitleEngine.ProfileTitle,
    val flairs: List<ProfileSocialEngine.ProfileFlair>,
    val memberStoryLine: String,
    val titleGlowId: String?,
    val profileFrameId: String?,
    val grovePlantId: String?,
    val proclaimedLabel: String?,
    val membershipTier: MembershipTier,
    val accentKey: String?,
    val bestStreak: Int,
    val totalCompletions: Int,
    val duoStreakDays: Int,
    val isOwnCard: Boolean,
    val memberSinceMillis: Long,
) {
    fun toPlaqueFallback(): ProfileShareCardExporter.ProfileShareData = toShareCard()

    fun toShareCard(): ProfileShareCardExporter.ProfileShareData {
        val secondary = buildList {
            if (level > 0) add("Level $level")
            if (totalCompletions > 0) add("$totalCompletions planted")
            if (duoStreakDays > 0) add("${duoStreakDays}d duo")
        }.joinToString(" · ").ifBlank { null }
        return ProfileShareCardExporter.ProfileShareData(
            username = userUsername,
            displayName = userName,
            bestStreak = bestStreak,
            secondaryLine = secondary,
            isOwnCard = isOwnCard,
            referralCode = "",
            accentKey = accentKey,
            photoUri = userPhotoUri,
            title = equippedTitle.label,
            titleColorArgb = ProfileTitleEngine.tierColorArgb(equippedTitle.tier).toInt(),
            level = level,
            levelTitle = levelTitle,
            memberStoryLine = memberStoryLine,
            flairs = flairs.map { it.label },
            membershipLabel = membershipTier.compactBadgeLabel.ifBlank { null },
            totalCompletions = totalCompletions,
            duoStreakDays = duoStreakDays,
            bio = bio,
            proclaimedLabel = proclaimedLabel,
        )
    }
}

fun MemberHeroShotData.toShareCard(): ProfileShareCardExporter.ProfileShareData {
    return ProfileShareCardExporter.ProfileShareData(
        username = username,
        displayName = displayName,
        bestStreak = bestStreak,
        secondaryLine = "Level $level",
        isOwnCard = isOwnCard,
        referralCode = "",
        accentKey = accentKey,
        photoUri = photoUrl,
        title = equippedTitle.orEmpty(),
        titleColorArgb = equippedTitleColor.toArgb(),
        level = level,
        levelTitle = levelTitle.orEmpty(),
        memberStoryLine = tagline,
        membershipLabel = membershipTier.compactBadgeLabel.ifBlank { null },
        bio = tagline,
        proclaimedLabel = proclaimedLabel,
    )
}

data class MemberHeroShotData(
    val displayName: String,
    val username: String,
    val tagline: String,
    val photoUrl: String?,
    val level: Int,
    val xp: Int,
    val levelTitle: String?,
    val accentKey: String?,
    val equippedTitle: String?,
    val equippedTitleColor: Color,
    val titleGlowId: String?,
    val profileFrameId: String?,
    val grovePlantId: String?,
    val proclaimedLabel: String?,
    val membershipTier: MembershipTier,
    val bestStreak: Int,
    val isOwnCard: Boolean,
)

sealed class ShareShotRequest {
    abstract val chooserTitle: String

    data class OwnProfile(val data: ProfileShotData) : ShareShotRequest() {
        override val chooserTitle: String
            get() = if (data.isOwnCard) "Share my Verdly card" else "Share Verdly card"
    }

    data class MemberHero(val data: MemberHeroShotData) : ShareShotRequest() {
        override val chooserTitle: String
            get() = if (data.isOwnCard) "Share my Verdly card" else "Share Verdly card"
    }

    data class Trophy(val achievement: Achievement) : ShareShotRequest() {
        override val chooserTitle: String = "Share trophy"
    }

    data class GoalComplete(
        val goal: GoalEntity,
        val stats: GoalCompletionStats?,
    ) : ShareShotRequest() {
        override val chooserTitle: String = "Share goal completion"
    }

    data class GoalMilestone(
        val goal: GoalEntity,
        val milestone: MilestoneEntity,
        val metrics: GoalProgressEngine.GoalMetrics?,
        val cheerLine: String,
        val habitReps: Int,
    ) : ShareShotRequest() {
        override val chooserTitle: String = "Share milestone"
    }
}

class ShareShotController {
    var request by mutableStateOf<ShareShotRequest?>(null)
        private set

    fun submit(next: ShareShotRequest) {
        request = next
    }

    fun clear() {
        request = null
    }
}

object ShareShot {
    @Volatile
    private var controller: ShareShotController? = null

    fun attach(value: ShareShotController) {
        controller = value
    }

    fun detach(value: ShareShotController) {
        if (controller === value) controller = null
    }

    fun submit(request: ShareShotRequest): Boolean {
        val host = controller ?: return false
        host.submit(request)
        return true
    }
}

object OwnProfileShot {
    fun fromAccount(
        userName: String,
        userUsername: String,
        userPhotoUri: String?,
        userBio: String,
        statsState: UserStatsUiState,
        equippedTitle: ProfileTitleEngine.ProfileTitle,
        flairs: List<ProfileSocialEngine.ProfileFlair>,
        memberStoryLine: String,
        titleGlowId: String?,
        profileFrameId: String?,
        grovePlantId: String?,
        proclaimedLabel: String?,
        membershipTier: MembershipTier,
        accentKey: String?,
        duoStreakDays: Int,
        careerBestStreak: Int,
    ): ProfileShotData = ProfileShotData(
        userName = userName,
        userUsername = userUsername,
        userPhotoUri = userPhotoUri,
        bio = userBio.ifBlank { "Consistency is the craft." },
        level = statsState.level,
        levelTitle = statsState.levelTitle,
        levelProgress = statsState.progressToNextLevel,
        equippedTitle = equippedTitle,
        flairs = flairs,
        memberStoryLine = memberStoryLine,
        titleGlowId = titleGlowId,
        profileFrameId = profileFrameId,
        grovePlantId = grovePlantId,
        proclaimedLabel = proclaimedLabel,
        membershipTier = membershipTier,
        accentKey = accentKey,
        bestStreak = statsState.longestStreakEver.coerceAtLeast(careerBestStreak),
        totalCompletions = statsState.totalCompletions,
        duoStreakDays = duoStreakDays,
        isOwnCard = true,
        memberSinceMillis = statsState.memberSince,
    )

    fun fromShell(
        userId: String,
        userName: String,
        userUsername: String,
        userPhotoUri: String?,
        userBio: String,
        statsState: UserStatsUiState,
        equippedTitleId: String?,
        titleGlowId: String?,
        profileFrameId: String?,
        grovePlantId: String?,
        proclaimedLabel: String?,
        membershipTier: MembershipTier,
        accentKey: String?,
        friendSummaries: List<FriendSummary>,
        duoState: DuoStreakState?,
        allUserChallenges: List<Challenge>,
        referralsSent: Int,
        isPro: Boolean,
        hasPerfectWeek: Boolean,
        ownedCosmeticIds: Set<String>,
    ): ProfileShotData {
        val career = ProfileSocialEngine.careerInputs(allUserChallenges, userId)
        val signature = ProfileTitleEngine.signatureTitle(
            wins = career.wins,
            podiums = career.podiums,
            bestStreak = career.bestStreak.coerceAtLeast(statsState.longestStreakEver),
            totalShared = career.totalShared,
            level = statsState.level,
        )
        val unlocked = ProfileTitleEngine.unlockedTitles(
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
            oathsLocked = statsState.oathsLocked,
            oathsConverted = statsState.oathsConverted,
            coinsPlanted = statsState.coinsPlanted,
            coinsGifted = statsState.coinsGifted,
            cosmeticsOwned = ownedCosmeticIds.size,
        )
        val equipped = ProfileTitleEngine.resolveEquipped(equippedTitleId, unlocked, signature)
        val social = runCatching {
            ProfileSocialEngine.buildSnapshot(
                userId = userId,
                userName = userName,
                memberSinceMillis = statsState.memberSince,
                totalCompletions = statsState.totalCompletions,
                longestStreak = statsState.longestStreakEver,
                friends = friendSummaries,
                duoState = duoState,
                allChallenges = allUserChallenges,
                referralsSent = referralsSent,
                isPro = isPro,
                hasPerfectWeek = hasPerfectWeek,
            )
        }.getOrNull()
        return ProfileShotData(
            userName = userName,
            userUsername = userUsername,
            userPhotoUri = userPhotoUri,
            bio = userBio.ifBlank { "Consistency is the craft." },
            level = statsState.level,
            levelTitle = statsState.levelTitle,
            levelProgress = statsState.progressToNextLevel,
            equippedTitle = equipped,
            flairs = social?.flairs.orEmpty(),
            memberStoryLine = social?.memberStoryLine
                ?: ProfileSocialEngine.memberStoryLine(
                    statsState.memberSince,
                    statsState.totalCompletions,
                    statsState.longestStreakEver,
                ),
            titleGlowId = titleGlowId,
            profileFrameId = profileFrameId,
            grovePlantId = grovePlantId,
            proclaimedLabel = proclaimedLabel,
            membershipTier = membershipTier,
            accentKey = accentKey,
            bestStreak = statsState.longestStreakEver.coerceAtLeast(career.bestStreak),
            totalCompletions = statsState.totalCompletions,
            duoStreakDays = duoState?.streakDays ?: 0,
            isOwnCard = true,
            memberSinceMillis = statsState.memberSince,
        )
    }
}

fun memberHeroShot(p: PublicUserProfile, bestStreak: Int, isOwnCard: Boolean): MemberHeroShotData {
    val signature = ProfileTitleEngine.signatureTitle(
        wins = 0,
        podiums = 0,
        bestStreak = bestStreak,
        totalShared = 0,
        level = p.level,
    )
    val titleColor = Color(ProfileTitleEngine.tierColorArgb(signature.tier))
    return MemberHeroShotData(
        displayName = p.displayName,
        username = p.username,
        tagline = p.motto.ifBlank { p.bio },
        photoUrl = p.photoUrl,
        level = p.level,
        xp = p.xp,
        levelTitle = GamificationEngine.levelTitle(p.level),
        accentKey = p.profileAccent,
        equippedTitle = p.equippedTitleLabel?.takeIf { it.isNotBlank() } ?: signature.label,
        equippedTitleColor = titleColor,
        titleGlowId = p.titleGlowId,
        profileFrameId = p.profileFrameId,
        grovePlantId = p.grovePlantId,
        proclaimedLabel = if (p.pinnedTrophyUntil > System.currentTimeMillis()) p.pinnedTrophyLabel else null,
        membershipTier = p.membershipTier,
        bestStreak = bestStreak,
        isOwnCard = isOwnCard,
    )
}

@Composable
fun ShareShotHost(controller: ShareShotController) {
    val handle = rememberShareCaptureHandle()
    val context = LocalContext.current
    val request = controller.request

    DisposableEffect(controller) {
        ShareShot.attach(controller)
        onDispose { ShareShot.detach(controller) }
    }

    if (request == null) return

    if (request is ShareShotRequest.Trophy) {
        LaunchedEffect(request) {
            shareAchievementCardFallback(context, request.achievement)
            controller.clear()
        }
        return
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(-4000, 0),
        properties = PopupProperties(
            focusable = false,
            clippingEnabled = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .shareCapture(handle)
                .background(MaterialTheme.colorScheme.background),
        ) {
            when (request) {
                is ShareShotRequest.OwnProfile -> OwnProfileShotContent(request.data)
                is ShareShotRequest.MemberHero -> MemberHeroShotContent(request.data)
                is ShareShotRequest.Trophy -> Unit
                is ShareShotRequest.GoalComplete -> GoalCompleteShotContent(request.goal, request.stats)
                is ShareShotRequest.GoalMilestone -> GoalMilestoneShotContent(
                    goal = request.goal,
                    milestone = request.milestone,
                    metrics = request.metrics,
                    cheerLine = request.cheerLine,
                )
            }
        }
    }

    LaunchedEffect(request) {
        delay(600)
        val bitmap = handle.snapshot()
        val ok = if (bitmap != null) {
            shareCaptured(context, request, bitmap)
        } else {
            false
        }
        if (!ok) fallbackShare(context, request)
        if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
        controller.clear()
    }
}

@Composable
private fun OwnProfileShotContent(data: ProfileShotData) {
    val accent = ProfileAccents.byKey(data.accentKey)
    ProfileIdentityCard(
        primary = accent.base,
        background = MaterialTheme.colorScheme.background,
        userName = data.userName,
        userUsername = data.userUsername,
        userPhotoUri = data.userPhotoUri,
        bio = data.bio,
        level = data.level,
        levelTitle = data.levelTitle,
        levelProgress = data.levelProgress,
        equippedTitle = data.equippedTitle,
        flairs = data.flairs,
        memberStoryLine = data.memberStoryLine,
        avatarScale = 1f,
        avatarBorder = Color.White.copy(alpha = 0.85f),
        onEditClick = {},
        onOpenTitlePicker = {},
        titleGlowId = data.titleGlowId,
        profileFrameId = data.profileFrameId,
        grovePlantId = data.grovePlantId,
        proclaimedLabel = data.proclaimedLabel,
        showChrome = false,
        membershipTier = data.membershipTier,
        accent = accent,
    )
}

@Composable
private fun MemberHeroShotContent(data: MemberHeroShotData) {
    PremiumProfileHero(
        displayName = data.displayName,
        username = data.username,
        tagline = data.tagline,
        photoUrl = data.photoUrl,
        level = data.level,
        xp = data.xp,
        levelTitle = data.levelTitle,
        accent = ProfileAccents.byKey(data.accentKey),
        equippedTitle = data.equippedTitle,
        equippedTitleColor = data.equippedTitleColor,
        titleGlowId = data.titleGlowId,
        profileFrameId = data.profileFrameId,
        grovePlantId = data.grovePlantId,
        proclaimedLabel = data.proclaimedLabel,
        membershipTier = data.membershipTier,
    )
}

@Composable
private fun GoalCompleteShotContent(goal: GoalEntity, stats: GoalCompletionStats?) {
    val color = Color(android.graphics.Color.parseColor(goal.colorHex))
    val timeTaken = stats?.daysTaken ?: 0
    val completionCheer = remember(goal.id) {
        GoalCelebrationCopy.milestoneCheer(
            goal,
            MilestoneEntity(
                id = "complete",
                goalId = goal.id,
                title = "Finish line",
                targetPercent = 100f,
                completedDate = System.currentTimeMillis(),
                isCompleted = true,
            ),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = color, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            if (goal.goalType == com.saintnico.verdlyhabits.data.local.goals.GoalType.REACH) {
                "You reached it"
            } else {
                "Goal Completed"
            },
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            goal.title,
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 34.sp,
        )
        if (goal.whyStatement.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                "\"${goal.whyStatement}\"",
                fontFamily = frauncesFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            completionCheer,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = color,
        )
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Time Taken", "$timeTaken days", Modifier.weight(1f))
            StatCard(
                "Milestones",
                stats?.let { "${it.milestonesCompleted} / ${it.milestonesTotal}" } ?: ".",
                Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GoalMilestoneShotContent(
    goal: GoalEntity,
    milestone: MilestoneEntity,
    metrics: GoalProgressEngine.GoalMetrics?,
    cheerLine: String,
) {
    val copy = remember(goal.id, milestone.id) {
        GoalCelebrationCopy.milestoneMoment(goal, milestone, metrics)
    }
    val palette = GoalCelebrationCopy.palette(goal.goalType)
    val color = Color(palette.primary)
    val secondary = Color(palette.secondary)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            copy.badge,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = color,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            copy.headline,
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 32.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            milestone.title,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = secondary,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            copy.body,
            fontFamily = dmSansFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (goal.whyStatement.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                "\"${goal.whyStatement}\"",
                fontFamily = frauncesFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            cheerLine,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}

private fun shareCaptured(context: Context, request: ShareShotRequest, inner: Bitmap): Boolean {
    val code = ReferralManager.getReferralCode(context)
    return when (request) {
        is ShareShotRequest.OwnProfile -> {
            val data = request.data.toShareCard().copy(referralCode = code)
            ProfileShareCardExporter.exportAndShare(context, data, request.chooserTitle)
        }
        is ShareShotRequest.MemberHero -> {
            val data = request.data.toShareCard().copy(referralCode = code)
            ProfileShareCardExporter.exportAndShare(context, data, request.chooserTitle)
        }
        is ShareShotRequest.Trophy -> shareAchievementCardFallback(context, request.achievement)
        is ShareShotRequest.GoalComplete -> {
            val spec = request.stats?.let {
                com.saintnico.verdlyhabits.engine.share.ShareCardEngine.forGoalCompletion(request.goal, it, code)
            }
            ProfileShareCardExporter.exportFramed(
                context = context,
                innerCard = inner,
                caption = spec?.caption ?: "I finished \"${request.goal.title}\". Built with Verdly",
                chooserTitle = request.chooserTitle,
                accentKey = null,
                referralCode = code,
            )
        }
        is ShareShotRequest.GoalMilestone -> {
            val spec = com.saintnico.verdlyhabits.engine.share.ShareCardEngine.forGoalMilestone(
                goal = request.goal,
                milestone = request.milestone,
                metrics = request.metrics,
                cheerLine = request.cheerLine,
                habitReps = request.habitReps,
                referralCode = code,
            )
            ProfileShareCardExporter.exportFramed(
                context = context,
                innerCard = inner,
                caption = spec.caption,
                chooserTitle = request.chooserTitle,
                accentKey = null,
                referralCode = code,
            )
        }
    }
}

private fun fallbackShare(context: Context, request: ShareShotRequest) {
    when (request) {
        is ShareShotRequest.OwnProfile -> {
            val code = ReferralManager.getReferralCode(context)
            ProfileShareCardExporter.exportAndShare(
                context,
                request.data.toShareCard().copy(referralCode = code),
                request.chooserTitle,
            )
        }
        is ShareShotRequest.MemberHero -> {
            val code = ReferralManager.getReferralCode(context)
            ProfileShareCardExporter.exportAndShare(
                context,
                request.data.toShareCard().copy(referralCode = code),
                request.chooserTitle,
            )
        }
        is ShareShotRequest.Trophy -> shareAchievementCardFallback(context, request.achievement)
        is ShareShotRequest.GoalComplete -> {
            val stats = request.stats
            if (stats != null) {
                shareGoalCompletionCardFallback(context, request.goal, stats)
            }
        }
        is ShareShotRequest.GoalMilestone -> shareGoalMilestoneCardFallback(
            context,
            request.goal,
            request.milestone,
            request.metrics,
            request.cheerLine,
            request.habitReps,
        )
    }
}

internal fun shareAchievementCardFallback(context: Context, achievement: Achievement): Boolean {
    val code = ReferralManager.getReferralCode(context)
    val spec = com.saintnico.verdlyhabits.engine.share.ShareCardEngine.forTrophy(achievement, code)
    return com.saintnico.verdlyhabits.utils.ShareCardExporter.exportAndShare(context, spec, "Share trophy")
}

internal fun shareGoalCompletionCardFallback(
    context: Context,
    goal: GoalEntity,
    stats: GoalCompletionStats,
) {
    val code = ReferralManager.getReferralCode(context)
    val spec = com.saintnico.verdlyhabits.engine.share.ShareCardEngine.forGoalCompletion(goal, stats, code)
    com.saintnico.verdlyhabits.utils.ShareCardExporter.exportAndShare(context, spec, "Share goal completion")
}

internal fun shareGoalMilestoneCardFallback(
    context: Context,
    goal: GoalEntity,
    milestone: MilestoneEntity,
    metrics: GoalProgressEngine.GoalMetrics?,
    cheerLine: String,
    habitReps: Int,
) {
    val code = ReferralManager.getReferralCode(context)
    val spec = com.saintnico.verdlyhabits.engine.share.ShareCardEngine.forGoalMilestone(
        goal = goal,
        milestone = milestone,
        metrics = metrics,
        cheerLine = cheerLine,
        habitReps = habitReps,
        referralCode = code,
    )
    com.saintnico.verdlyhabits.utils.ShareCardExporter.exportAndShare(context, spec, "Share milestone")
}
