package com.saintnico.verdlyhabits.monetization

sealed class PaywallTrigger(val headline: String, val subtext: String) {
    data object HabitLimit : PaywallTrigger(
        headline = "You're building something real.",
        subtext = "Unlock unlimited habits and keep the momentum going.",
    )

    data object FocusSounds : PaywallTrigger(
        headline = "Set the mood for deep focus.",
        subtext = "Rain, forest, lo-fi and more — sounds that help you stay locked in.",
    )

    data object Analytics : PaywallTrigger(
        headline = "Your data has a story.",
        subtext = "See 90-day heatmaps, habit scores, and trends — all in Pro.",
    )

    data object StreakShield : PaywallTrigger(
        headline = "Don't let one bad day erase everything.",
        subtext = "Streak shields protect your progress. Available in Pro.",
    )

    data object SevenDayStreak : PaywallTrigger(
        headline = "7 days straight. That's real commitment.",
        subtext = "Protect it with Pro. Streak shields, deep analytics, and more.",
    )

    data object GoPro : PaywallTrigger(
        headline = "Go Pro with Verdly",
        subtext = "Unlimited habits, focus sounds, analytics, and streak shields.",
    )

    data object ReferralAnnual : PaywallTrigger(
        headline = "Your referral reward unlocked a deal.",
        subtext = "Lock in annual Pro at 20% off — priced for Kenya, less than a snack per week on weekly.",
    )
}
