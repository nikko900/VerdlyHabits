package com.saintnico.verdlyhabits.ui.components.share

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import com.saintnico.verdlyhabits.engine.DuoStreakEngine
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.screens.focus.drawGrowingPlant
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

/**
 * In-app share card (400×220 dp). Image export uses [shareStreakInvite] (text + link) for maximum reliability.
 */
@Composable
fun ShareableStreakCard(
    headline: String,
    habitName: String,
    referralCode: String,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF0A1610), Color(0xFF1B4332), Color(0xFF0D1F14)),
    )
    Box(
        modifier
            .width(400.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawGrowingPlant(1f, isWilting = false, wiltAmount = 0f)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            ) {
                Text(
                    headline,
                    fontFamily = frauncesFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = Color(0xFFD8F3DC),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    habitName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(0.85f),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Built with Verdly",
                    fontSize = 11.sp,
                    color = Color.White.copy(0.45f),
                )
                Text(
                    "Join free: ${ReferralManager.shareLinkLabel(referralCode)}",
                    fontSize = 11.sp,
                    color = Color(0xFF95D5B2),
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun referralPathCode(fullCode: String): String =
    fullCode.removePrefix("VERDLY-").uppercase()

/**
 * Shares streak / session copy with referral link. No Firebase — uses the system share sheet only.
 */
fun shareStreakCardWithImage(
    context: Context,
    headline: String,
    habitName: String,
) {
    com.saintnico.verdlyhabits.engine.TrophyBus.emit(com.saintnico.verdlyhabits.engine.TrophyEvent.Shared)
    val fullCode = ReferralManager.getReferralCode(context)
    val pathCode = referralPathCode(fullCode)
    com.saintnico.verdlyhabits.utils.StreakCardExporter.exportAndShare(
        context = context,
        headline = headline,
        habitName = habitName,
        referralCode = pathCode,
        theme = com.saintnico.verdlyhabits.utils.StreakCardTheme.EmeraldDark,
        plantProgress = 1f
    )
}

fun shareReferralInviteCard(
    context: Context,
    fullCode: String,
    shortCode: String,
    shareLink: String,
    shareText: String,
): Boolean {
    val activity = context.findActivity() ?: return false
    val displayCode = shortCode.ifBlank { fullCode }
    val link = shareLink.ifBlank { ReferralManager.shareLink(fullCode) }
    return com.saintnico.verdlyhabits.utils.ReferralInviteCardExporter.exportAndShare(
        context = activity,
        displayCode = displayCode,
        shareLink = link,
        shareText = shareText,
    )
}

fun shareChallengeInviteCard(
    context: Context,
    challengeName: String,
    stake: String,
    inviteCode: String,
    challengeId: String,
    daysRemaining: Long? = null,
    memberCount: Int? = null,
) {
    val activity = context.findActivity() ?: return
    val shareText = com.saintnico.verdlyhabits.challenge.ChallengeInviteHelper.buildShareMessage(
        challengeName = challengeName,
        inviteCode = inviteCode,
        stake = stake,
        daysRemaining = daysRemaining,
        memberCount = memberCount,
    )
    val shared = com.saintnico.verdlyhabits.utils.ChallengeInviteCardExporter.exportAndShare(
        context = activity,
        challengeName = challengeName,
        inviteCode = inviteCode,
        stake = stake,
        daysRemaining = daysRemaining,
        memberCount = memberCount,
        shareText = shareText,
    )
    if (!shared) {
        ShareCompat.IntentBuilder(activity)
            .setType("text/plain")
            .setText(shareText)
            .setChooserTitle("Share challenge invite")
            .startChooser()
    }
}

fun shareGoalDailyRecapCard(
    context: Context,
    goal: com.saintnico.verdlyhabits.data.local.goals.GoalEntity,
    recap: com.saintnico.verdlyhabits.engine.GoalDailyRecapEngine.DailyRecap,
) {
    val code = ReferralManager.getReferralCode(context)
    val spec = com.saintnico.verdlyhabits.engine.share.ShareCardEngine.forGoalDailyRecap(goal, recap, code)
    if (!com.saintnico.verdlyhabits.utils.ShareCardExporter.exportAndShare(context, spec, "Share today's recap")) {
        fallbackTextShare(context, spec.caption, "Share today's recap")
    }
}

fun shareAchievementCard(context: Context, achievement: com.saintnico.verdlyhabits.engine.Achievement) {
    com.saintnico.verdlyhabits.engine.TrophyBus.emit(com.saintnico.verdlyhabits.engine.TrophyEvent.Shared)
    shareAchievementCardFallback(context, achievement)
}

fun shareGoalMilestoneCard(
    context: Context,
    goal: com.saintnico.verdlyhabits.data.local.goals.GoalEntity,
    milestone: com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity,
    metrics: com.saintnico.verdlyhabits.engine.GoalProgressEngine.GoalMetrics?,
    cheerLine: String,
    habitReps: Int,
) {
    if (!ShareShot.submit(
            ShareShotRequest.GoalMilestone(
                goal = goal,
                milestone = milestone,
                metrics = metrics,
                cheerLine = cheerLine,
                habitReps = habitReps,
            ),
        )
    ) {
        shareGoalMilestoneCardFallback(context, goal, milestone, metrics, cheerLine, habitReps)
    }
}

fun shareGoalCompletionCard(
    context: Context,
    goal: com.saintnico.verdlyhabits.data.local.goals.GoalEntity,
    stats: com.saintnico.verdlyhabits.ui.viewmodel.GoalCompletionStats,
) {
    if (!ShareShot.submit(ShareShotRequest.GoalComplete(goal, stats))) {
        shareGoalCompletionCardFallback(context, goal, stats)
    }
}

/** @deprecated Use [shareGoalCompletionCard] with goal + stats for image cards. */
fun shareGoalCompletionCard(
    context: Context,
    goalTitle: String,
    whyStatement: String,
    daysTaken: Long,
) {
    val activity = context.findActivity() ?: return
    val code = ReferralManager.getReferralCode(context)
    val link = com.saintnico.verdlyhabits.engine.share.ShareCardEngine.referralLinkLabel(code)
    ShareCompat.IntentBuilder(activity)
        .setType("text/plain")
        .setText(
            buildString {
                append("\"$goalTitle\". Done. $daysTaken days. Grateful I stuck with it. $link")
                if (whyStatement.isNotBlank()) append("\nWhy: $whyStatement")
            },
        )
        .setChooserTitle("Share goal completion")
        .startChooser()
}

fun shareProfileCard(context: Context, shot: ProfileShotData) {
    com.saintnico.verdlyhabits.engine.TrophyBus.emit(com.saintnico.verdlyhabits.engine.TrophyEvent.Shared)
    val activity = context.findActivity() ?: return
    val code = ReferralManager.getReferralCode(context)
    val data = shot.toShareCard().copy(referralCode = code)
    val title = if (shot.isOwnCard) "Share my Verdly card" else "Share Verdly card"
    if (!com.saintnico.verdlyhabits.utils.ProfileShareCardExporter.exportAndShare(activity, data, title)) {
        fallbackTextShare(
            activity,
            com.saintnico.verdlyhabits.utils.ProfileShareCardExporter.buildCaption(data),
            title,
        )
    }
}

fun shareMemberHeroCard(context: Context, shot: MemberHeroShotData) {
    com.saintnico.verdlyhabits.engine.TrophyBus.emit(com.saintnico.verdlyhabits.engine.TrophyEvent.Shared)
    val activity = context.findActivity() ?: return
    val code = ReferralManager.getReferralCode(context)
    val data = shot.toShareCard().copy(referralCode = code)
    val title = if (shot.isOwnCard) "Share my Verdly card" else "Share Verdly card"
    if (!com.saintnico.verdlyhabits.utils.ProfileShareCardExporter.exportAndShare(activity, data, title)) {
        fallbackTextShare(activity, com.saintnico.verdlyhabits.utils.ProfileShareCardExporter.buildCaption(data), title)
    }
}

fun shareProfileCard(
    context: Context,
    username: String,
    displayName: String,
    bestStreak: Int,
    totalCompletions: Int = 0,
    duoStreakDays: Int = 0,
    level: Int = 0,
    isOwnCard: Boolean = true,
    memberSinceMillis: Long = 0L,
    achievements: List<com.saintnico.verdlyhabits.engine.Achievement> = emptyList(),
    profileAccentKey: String? = null,
    photoUri: String? = null,
    bio: String = "",
    levelTitle: String = com.saintnico.verdlyhabits.engine.GamificationEngine.levelTitle(level),
    levelProgress: Float = 0f,
    equippedTitle: com.saintnico.verdlyhabits.engine.ProfileTitleEngine.ProfileTitle =
        com.saintnico.verdlyhabits.engine.ProfileTitleEngine.ProfileTitle(
            id = "sig_level",
            label = levelTitle,
            tier = com.saintnico.verdlyhabits.engine.ProfileTitleEngine.Tier.COMMON,
        ),
    flairs: List<com.saintnico.verdlyhabits.engine.ProfileSocialEngine.ProfileFlair> = emptyList(),
    memberStoryLine: String = com.saintnico.verdlyhabits.engine.ProfileSocialEngine.memberStoryLine(
        memberSinceMillis,
        totalCompletions,
        bestStreak,
    ),
    titleGlowId: String? = null,
    profileFrameId: String? = null,
    grovePlantId: String? = null,
    proclaimedLabel: String? = null,
    membershipTier: com.saintnico.verdlyhabits.data.model.MembershipTier =
        com.saintnico.verdlyhabits.data.model.MembershipTier.FREE,
) {
    shareProfileCard(
        context,
        ProfileShotData(
            userName = displayName,
            userUsername = username,
            userPhotoUri = photoUri,
            bio = bio.ifBlank { "Consistency is the craft." },
            level = level,
            levelTitle = levelTitle,
            levelProgress = levelProgress,
            equippedTitle = equippedTitle,
            flairs = flairs,
            memberStoryLine = memberStoryLine,
            titleGlowId = titleGlowId,
            profileFrameId = profileFrameId,
            grovePlantId = grovePlantId,
            proclaimedLabel = proclaimedLabel,
            membershipTier = membershipTier,
            accentKey = profileAccentKey,
            bestStreak = bestStreak,
            totalCompletions = totalCompletions,
            duoStreakDays = duoStreakDays,
            isOwnCard = isOwnCard,
            memberSinceMillis = memberSinceMillis,
        ),
    )
}

fun shareDuoMilestoneCard(
    context: Context,
    streakDays: Int,
    buddyUsername: String,
) {
    val code = ReferralManager.getReferralCode(context)
    val spec = com.saintnico.verdlyhabits.engine.share.ShareCardEngine.forDuoMilestone(streakDays, buddyUsername, code)
    if (!com.saintnico.verdlyhabits.utils.ShareCardExporter.exportAndShare(context, spec, "Share duo milestone")) {
        fallbackTextShare(context, spec.caption, "Share duo milestone")
    }
}

private fun fallbackTextShare(context: Context, text: String, title: String) {
    val activity = context.findActivity() ?: return
    ShareCompat.IntentBuilder(activity)
        .setType("text/plain")
        .setText(text)
        .setChooserTitle(title)
        .startChooser()
}
