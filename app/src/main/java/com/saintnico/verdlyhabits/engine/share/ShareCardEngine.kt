package com.saintnico.verdlyhabits.engine.share

import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import com.saintnico.verdlyhabits.engine.Achievement
import com.saintnico.verdlyhabits.engine.GoalDailyRecapEngine
import com.saintnico.verdlyhabits.engine.GoalMilestoneFactory
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.engine.TrophyCatalog
import com.saintnico.verdlyhabits.engine.TrophyTierLevel
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.viewmodel.GoalCompletionStats
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object ShareCardEngine {

    private const val W_STANDARD = 1080
    private const val H_STANDARD = 1350
    private const val H_DUO = 1920

    object Tokens {
        const val EMERALD_DEEP = 0xFF0B3D2EL
        const val EMERALD_MID = 0xFF1B7A4DL
        const val EMERALD_LIGHT = 0xFF5CBE87L
        const val GOLD_ACCENT = 0xFFE8C468L
        const val TEXT_CREAM = 0xFFF6F3E9L
    }

    fun referralLinkLabel(referralCode: String): String {
        val short = referralCode.removePrefix("VERDLY-").uppercase()
        return ReferralManager.shareLinkLabel(short)
    }

    fun forTrophy(achievement: Achievement, referralCode: String): ShareCardSpec {
        val def = TrophyCatalog.find(achievement.id)
        val tier = def?.tier ?: TrophyTierLevel.FOUNDATION
        val cardType = cardTypeForTier(tier)
        val theme = themeForTier(tier)
        val isMythic = tier == TrophyTierLevel.MYTHIC
        val motifCategory = TrophyShareMapper.motifFor(achievement.id, isMythic)
        val link = referralLinkLabel(referralCode)
        val tierLabel = tier.rankName
        val earnedOn = achievement.unlockedAt?.takeIf { it > 0L }?.let { millis ->
            Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        }

        val caption = if (isMythic) {
            "Something unlocked on Verdly. I'm not sure I've seen this tier before. $link"
        } else {
            "Just unlocked ${achievement.title} on Verdly. $tierLabel. $link"
        }

        return ShareCardSpec(
            cardType = cardType,
            width = W_STANDARD,
            height = H_STANDARD,
            theme = theme,
            motif = ShareMotif(category = motifCategory),
            chipLabel = tierLabel.uppercase(),
            headline = achievement.title,
            subhead = achievement.description,
            statLine = earnedOn?.let { "Earned $it" },
            xpChip = "+${achievement.xpReward} XP",
            whyQuote = null,
            footerLink = link,
            caption = caption,
            trophyAccent = theme.accent,
        )
    }

    fun forGoalMilestone(
        goal: GoalEntity,
        milestone: MilestoneEntity,
        metrics: GoalProgressEngine.GoalMetrics?,
        cheerLine: String,
        habitReps: Int,
        referralCode: String,
    ): ShareCardSpec {
        val ritual = GoalMilestoneFactory.isRitualMilestone(goal, milestone)
        val theme = themeForGoalType(goal.goalType)
        val link = referralLinkLabel(referralCode)
        val tag = goalTypeTag(goal.goalType)
        val heroStat = milestoneHeroStat(goal, milestone)
        val progressPct = ((metrics?.progressFraction ?: 0f) * 100f).toInt().coerceIn(0, 100)
        val daysElapsed = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - goal.startDate)
        val growthMark = ritual &&
            (goal.goalType == GoalType.BUILD || goal.goalType == GoalType.MAINTAIN) &&
            milestone.targetPercent >= 50f

        val caption = if (ritual) {
            "Hit the $heroStat mark on \"${goal.title}.\" $cheerLine. $link"
        } else {
            "$heroStat of the way to \"${goal.title}\". $link"
        }

        return ShareCardSpec(
            cardType = if (ritual) ShareCardType.GOAL_MILESTONE_RITUAL else ShareCardType.GOAL_MILESTONE_STANDARD,
            width = W_STANDARD,
            height = H_STANDARD,
            theme = theme,
            motif = if (ritual) {
                ShareMotif(
                    category = ShareMotifCategory.PROGRESS_RING,
                    progressPercent = progressPct,
                    growthMarkEnabled = growthMark,
                )
            } else null,
            chipLabel = tag,
            headline = heroStat,
            subhead = milestone.title,
            statLine = "$daysElapsed days · $habitReps reps",
            xpChip = cheerLine,
            whyQuote = if (ritual && goal.whyStatement.isNotBlank()) goal.whyStatement else null,
            footerLink = link,
            caption = caption,
        )
    }

    fun forGoalCompletion(
        goal: GoalEntity,
        stats: GoalCompletionStats,
        referralCode: String,
    ): ShareCardSpec {
        val theme = themeForGoalType(goal.goalType)
        val link = referralLinkLabel(referralCode)
        val headline = when (goal.goalType) {
            GoalType.BUILD -> "Goal completed."
            GoalType.REACH -> "You reached it."
            GoalType.QUIT -> "You broke free."
            GoalType.MAINTAIN -> "You held the line."
        }
        val caption =
            "\"${goal.title}\". Done. ${stats.daysTaken} days, ${stats.milestonesCompleted} milestones. Grateful I stuck with it. $link"

        return ShareCardSpec(
            cardType = ShareCardType.GOAL_COMPLETION,
            width = W_STANDARD,
            height = H_STANDARD,
            theme = theme,
            motif = ShareMotif(category = ShareMotifCategory.CLOSED_RING),
            chipLabel = goalTypeTag(goal.goalType),
            headline = headline,
            subhead = goal.title,
            statLine = "${stats.daysTaken}d · ${stats.milestonesCompleted} milestones · ${stats.checkInCount} check-ins",
            xpChip = null,
            whyQuote = goal.whyStatement.takeIf { it.isNotBlank() },
            footerLink = link,
            caption = caption,
            reclaimedChip = stats.costSavedLabel,
        )
    }

    fun forGoalDailyRecap(
        goal: GoalEntity,
        recap: GoalDailyRecapEngine.DailyRecap,
        referralCode: String,
    ): ShareCardSpec {
        val theme = themeForGoalType(goal.goalType)
        val link = referralLinkLabel(referralCode)
        val caption =
            "${recap.dateLabel}: ${recap.heroNumber} on \"${goal.title}\". ${recap.cheerLine}. $link"
        return ShareCardSpec(
            cardType = ShareCardType.GOAL_DAILY_RECAP,
            width = W_STANDARD,
            height = H_STANDARD,
            theme = theme,
            motif = ShareMotif(
                category = ShareMotifCategory.PROGRESS_RING,
                progressPercent = recap.todayProgressPercent,
                growthMarkEnabled = goal.goalType == GoalType.BUILD || goal.goalType == GoalType.MAINTAIN,
            ),
            chipLabel = "TODAY'S RECAP",
            headline = recap.heroNumber,
            subhead = goal.title,
            statLine = "${recap.dateLabel} · ${recap.statLine}",
            xpChip = recap.cheerLine,
            whyQuote = goal.whyStatement.takeIf { it.isNotBlank() },
            footerLink = link,
            caption = caption,
        )
    }

    fun featuredTrophy(achievements: List<Achievement>): Achievement? =
        achievements
            .filter { it.isUnlocked }
            .maxWithOrNull(
                compareBy<Achievement> { TrophyCatalog.find(it.id)?.tier?.ordinalIndex ?: -1 }
                    .thenBy { it.xpReward }
                    .thenBy { it.unlockedAt ?: 0L },
            )

    fun forProfile(
        username: String,
        displayName: String,
        memberSinceMillis: Long,
        bestStreak: Int,
        totalCompletions: Int,
        duoStreakDays: Int,
        featured: Achievement?,
        referralCode: String,
    ): ShareCardSpec {
        val link = referralLinkLabel(referralCode)
        val handle = username.trim().removePrefix("@")
        val identity = when {
            handle.isNotBlank() && handle != "UnknownRival" -> "@$handle"
            displayName.isNotBlank() -> displayName
            else -> "Verdly"
        }
        val letter = identity.removePrefix("@").firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "V"
        val memberSince = Instant.ofEpochMilli(memberSinceMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        val def = featured?.let { TrophyCatalog.find(it.id) }
        val tier = def?.tier
        val isMythic = tier == TrophyTierLevel.MYTHIC
        val trophyTheme = tier?.let { themeForTier(it) }
        val duoPart = if (duoStreakDays > 0) " · ${duoStreakDays}d duo" else ""
        val caption = "My Verdly card. ${bestStreak}-day best streak, still going. $link"

        return ShareCardSpec(
            cardType = ShareCardType.PROFILE_CARD,
            width = W_STANDARD,
            height = H_STANDARD,
            theme = ShareCardTheme(
                gradient = listOf(0xFF0B3D2EL, 0xFF102E24L, 0xFF163D2EL),
                accent = trophyTheme?.accent ?: Tokens.GOLD_ACCENT,
                textColor = Tokens.TEXT_CREAM,
                particleTreatment = isMythic,
            ),
            motif = featured?.let {
                ShareMotif(category = TrophyShareMapper.motifFor(it.id, isMythic))
            },
            chipLabel = null,
            headline = "$bestStreak",
            subhead = "best streak",
            statLine = "$totalCompletions completions$duoPart",
            xpChip = featured?.title,
            whyQuote = null,
            footerLink = link,
            caption = caption,
            identityLabel = identity,
            memberSinceLabel = "Member since $memberSince",
            monogram = letter,
            qrPayload = ReferralManager.shareLink(referralCode),
            trophyAccent = trophyTheme?.accent,
        )
    }

    fun forDuoMilestone(
        streakDays: Int,
        buddyUsername: String,
        referralCode: String,
    ): ShareCardSpec {
        val link = referralLinkLabel(referralCode)
        val combined = streakDays * 2
        return ShareCardSpec(
            cardType = ShareCardType.DUO_STREAK_MILESTONE,
            width = W_STANDARD,
            height = H_DUO,
            theme = ShareCardTheme(
                gradient = listOf(0xFF0A1610L, 0xFF1B4332L, 0xFF0D1F14L),
                accent = Tokens.GOLD_ACCENT,
                textColor = Tokens.TEXT_CREAM,
            ),
            motif = ShareMotif(
                category = ShareMotifCategory.DUO_RINGS,
                streakDays = streakDays,
            ),
            chipLabel = "DUO STREAK",
            headline = "$streakDays",
            subhead = "day streak",
            statLine = "you + $buddyUsername · $combined combined",
            xpChip = null,
            whyQuote = null,
            footerLink = link,
            caption = "$streakDays days strong with $buddyUsername. $link",
        )
    }

    fun forGrovePulse(
        plantedCount: Int,
        friendCount: Int,
        lead: String?,
        referralCode: String,
    ): ShareCardSpec {
        val link = referralLinkLabel(referralCode)
        val headline = if (plantedCount <= 0) "Still growing" else "$plantedCount planted"
        return ShareCardSpec(
            cardType = ShareCardType.DUO_STREAK_MILESTONE,
            width = W_STANDARD,
            height = H_DUO,
            theme = ShareCardTheme(
                gradient = listOf(0xFF0A1610L, 0xFF1B4332L, 0xFF0D1F14L),
                accent = Tokens.GOLD_ACCENT,
                textColor = Tokens.TEXT_CREAM,
            ),
            motif = ShareMotif(category = ShareMotifCategory.SOCIAL, streakDays = plantedCount),
            chipLabel = "FRIENDS GROVE",
            headline = headline,
            subhead = if (friendCount <= 0) "Invite a friend into the grove" else "in my grove today",
            statLine = lead?.let { "@$it leads this week" },
            xpChip = null,
            whyQuote = "Beat my week. $link",
            footerLink = link,
            caption = "Plant with me on Verdly. $link",
            qrPayload = ReferralManager.shareLink(referralCode),
        )
    }

    private fun cardTypeForTier(tier: TrophyTierLevel): ShareCardType = when (tier) {
        TrophyTierLevel.FOUNDATION -> ShareCardType.TROPHY_TIER_BRONZE
        TrophyTierLevel.MOMENTUM -> ShareCardType.TROPHY_TIER_SILVER
        TrophyTierLevel.MASTERY -> ShareCardType.TROPHY_TIER_GOLD
        TrophyTierLevel.LEGEND -> ShareCardType.TROPHY_TIER_OBSIDIAN
        TrophyTierLevel.MYTHIC -> ShareCardType.TROPHY_TIER_MYTHIC
    }

    private fun themeForTier(tier: TrophyTierLevel): ShareCardTheme = when (tier) {
        TrophyTierLevel.FOUNDATION -> ShareCardTheme(
            gradient = listOf(0xFF1C1008L, 0xFF5A3016L, 0xFFC47832L),
            accent = 0xFFE0A05AL,
            textColor = Tokens.TEXT_CREAM,
        )
        TrophyTierLevel.MOMENTUM -> ShareCardTheme(
            gradient = listOf(0xFF14181CL, 0xFF4A5560L, 0xFFC5CDD4L),
            accent = 0xFFE8EEF2L,
            textColor = Tokens.TEXT_CREAM,
        )
        TrophyTierLevel.MASTERY -> ShareCardTheme(
            gradient = listOf(0xFF1A1406L, 0xFF8A6A12L, 0xFFE8C468L),
            accent = Tokens.GOLD_ACCENT,
            textColor = Tokens.TEXT_CREAM,
        )
        TrophyTierLevel.LEGEND -> ShareCardTheme(
            gradient = listOf(0xFF07060CL, 0xFF1A1228L, 0xFF7B6CF6L),
            accent = 0xFFB388FFL,
            textColor = Tokens.TEXT_CREAM,
        )
        TrophyTierLevel.MYTHIC -> ShareCardTheme(
            gradient = listOf(0xFF050308L, 0xFF1A0A18L, 0xFFE040FBL),
            accent = 0xFFFF80ABL,
            textColor = Tokens.TEXT_CREAM,
            particleTreatment = true,
        )
    }

    private fun themeForGoalType(type: GoalType): ShareCardTheme = when (type) {
        GoalType.BUILD -> ShareCardTheme(
            gradient = listOf(0xFF0B3D2EL, 0xFF1B7A4DL, 0xFF5CBE87L),
            accent = 0xFF5CBE87L,
            textColor = Tokens.TEXT_CREAM,
        )
        GoalType.REACH -> ShareCardTheme(
            gradient = listOf(0xFF0B2A4DL, 0xFF1B5A9EL, 0xFF5CA3E8L),
            accent = 0xFF5CA3E8L,
            textColor = Tokens.TEXT_CREAM,
        )
        GoalType.QUIT -> ShareCardTheme(
            gradient = listOf(0xFF241B3DL, 0xFF4A2E6BL, 0xFF1FA391L),
            accent = 0xFF1FA391L,
            textColor = Tokens.TEXT_CREAM,
        )
        GoalType.MAINTAIN -> ShareCardTheme(
            gradient = listOf(0xFF3D2A0BL, 0xFF8A5A1BL, 0xFFE0A24EL),
            accent = 0xFFE0A24EL,
            textColor = Tokens.TEXT_CREAM,
        )
    }

    private fun goalTypeTag(type: GoalType): String = when (type) {
        GoalType.BUILD -> "BUILDING"
        GoalType.REACH -> "REACHING"
        GoalType.QUIT -> "BREAKING FREE"
        GoalType.MAINTAIN -> "HOLDING STEADY"
    }

    private fun milestoneHeroStat(goal: GoalEntity, milestone: MilestoneEntity): String = when (goal.goalType) {
        GoalType.REACH, GoalType.BUILD -> "${milestone.targetPercent.toInt()}%"
        GoalType.QUIT -> "${milestone.targetPercent.toInt()} days"
        GoalType.MAINTAIN -> "${milestone.targetPercent.toInt()} weeks"
    }
}
