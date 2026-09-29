package com.saintnico.verdlyhabits.monetization

/**
 * Paywall entry points. Each trigger carries a stable [analyticsId] so conversion can be
 * attributed to the moment that actually moved the user — especially the three streak-shield
 * moments, which feel similar in the UI but convert very differently.
 */
sealed class PaywallTrigger(
    val headline: String,
    val subtext: String,
    val analyticsId: String,
) {
    data object HabitLimit : PaywallTrigger(
        headline = "You're building something real.",
        subtext = "Unlock unlimited habits and keep the momentum going.",
        analyticsId = "habit_limit",
    )

    data object FocusSounds : PaywallTrigger(
        headline = "Set the mood for deep focus.",
        subtext = "Rain, forest, lo-fi and more. Sounds that help you stay locked in.",
        analyticsId = "focus_sounds",
    )

    data object Analytics : PaywallTrigger(
        headline = "Your data has a story.",
        subtext = "See 90-day heatmaps, habit scores, and trends. All in Premium.",
        analyticsId = "analytics",
    )

    /** Generic shield storefront — inventory empty, user tapped buy. */
    data object StreakShield : PaywallTrigger(
        headline = "Don't let one bad day erase everything.",
        subtext = "Streak shields protect your progress when life gets in the way.",
        analyticsId = "streak_shield_store",
    )

    /** At-risk / repairable with zero shields in hand — soft urgency. */
    data object StreakAtRiskNoShield : PaywallTrigger(
        headline = "Your streak is still open. For now.",
        subtext = "A shield would cover today if you miss it. Get one before the window closes.",
        analyticsId = "streak_at_risk_no_shield",
    )

    /** Repair offer open, user has no shields — concrete save opportunity. */
    data object StreakRepairableNoShield : PaywallTrigger(
        headline = "You can still save this streak.",
        subtext = "One shield covers the missed day. Without it, the run ends when the window closes.",
        analyticsId = "streak_repairable_no_shield",
    )

    /**
     * Loss moment — the highest-intent trigger. Only fire when
     * [com.saintnico.verdlyhabits.domain.streak.StreakLossRecord.wasPreventableWithShield]
     * is true, so the copy stays honest.
     */
    data class StreakLost(
        val finalStreak: Int,
        val habitTitle: String,
    ) : PaywallTrigger(
        headline = "Your $finalStreak-day streak ended.",
        subtext = "A shield would have saved \"$habitTitle\". Hold one for next time.",
        analyticsId = "streak_lost",
    )

    data object SevenDayStreak : PaywallTrigger(
        headline = "7 days straight. That's real commitment.",
        subtext = "Protect it with Premium. Streak shields, deep analytics, and more.",
        analyticsId = "seven_day_streak",
    )

    data object HabitPlanted : PaywallTrigger(
        headline = "You're building something real.",
        subtext = "Premium keeps your habits alive. Shields, unlimited seeds, and analytics when motivation dips.",
        analyticsId = "habit_planted",
    )

    data object GoPro : PaywallTrigger(
        headline = "Go Premium with Verdly",
        subtext = "Unlimited habits, focus sounds, analytics, and streak shields.",
        analyticsId = "go_pro",
    )

    data object OathDoubleXp : PaywallTrigger(
        headline = "Premium plants twice the XP.",
        subtext = "Every oath you finish is 2× trophy progress. Plus the monthly coin drip.",
        analyticsId = "oath_double_xp",
    )

    data object ReferralAnnual : PaywallTrigger(
        headline = "Your referral reward unlocked a deal.",
        subtext = "Lock in annual Premium at 20% off. Priced for Kenya, less than a snack per week on weekly.",
        analyticsId = "referral_annual",
    )
}
