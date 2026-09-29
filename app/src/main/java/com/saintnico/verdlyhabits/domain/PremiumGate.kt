package com.saintnico.verdlyhabits.domain

/**
 * Central feature-gate and dev billing override.
 *
 * **Quick dev toggle** — change only [DEV_BILLING_MODE] below:
 * - [DevBillingMode.UNLOCKED] — Pro/premium gates OFF; full access while you build (current)
 * - [DevBillingMode.FREE_ONLY] — Pro OFF; paywalls + 5-habit cap like a free user
 * - [DevBillingMode.OFF] — Production: real Google Play Pro, trial, and referral rules
 *
 * UI paywalls read [com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel.hasFullAccess],
 * which respects [DEV_BILLING_MODE].
 */
object PremiumGate {

    enum class DevBillingMode {
        /** Real billing — subscription, 7-day trial, referral bonus. */
        OFF,
        /** Dev: everything unlocked, no paywalls. */
        UNLOCKED,
        /** Dev: force free tier (paywalls on, trial/subscription ignored). */
        FREE_ONLY,
    }

    /**
     * ← Flip this one constant to turn Pro on/off for local testing.
     * Set back to [DevBillingMode.OFF] before release.
     */
    val DEV_BILLING_MODE: DevBillingMode = DevBillingMode.OFF

    /** Master switch — when false, every [PremiumGate] feature check passes. */
    const val enforce: Boolean = true

    /** Free tier hard cap on number of active (non-archived) habits. */
    const val FREE_HABIT_CAP: Int = 5

    enum class Feature {
        UNLIMITED_HABITS,
        FOCUS_AMBIENT_SOUNDS,
        ADVANCED_ANALYTICS,
        STREAK_SHIELDS,
        ADVANCED_SCHEDULING,
        HOME_WIDGETS,
        EXPORT_CSV
    }

    fun grantsFullAccess(realFullAccess: Boolean): Boolean = when (DEV_BILLING_MODE) {
        DevBillingMode.UNLOCKED -> true
        DevBillingMode.FREE_ONLY -> false
        DevBillingMode.OFF -> realFullAccess
    }

    fun effectiveIsPro(realIsPro: Boolean): Boolean = when (DEV_BILLING_MODE) {
        DevBillingMode.UNLOCKED -> true
        DevBillingMode.FREE_ONLY -> false
        DevBillingMode.OFF -> realIsPro
    }

    /**
     * Returns true when the user can use [feature]. Components should prefer the
     * higher-level helpers (e.g. [canAddHabit]) where they exist.
     */
    fun isUnlocked(feature: Feature, isPremium: Boolean): Boolean {
        if (!enforce) return true
        return isPremium
    }

    fun canAddHabit(currentActiveCount: Int, isPremium: Boolean): Boolean {
        if (!enforce) return true
        if (isPremium) return true
        return currentActiveCount < FREE_HABIT_CAP
    }

    fun remainingFreeSlots(currentActiveCount: Int, isPremium: Boolean): Int? {
        if (!enforce || isPremium) return null
        return (FREE_HABIT_CAP - currentActiveCount).coerceAtLeast(0)
    }
}
