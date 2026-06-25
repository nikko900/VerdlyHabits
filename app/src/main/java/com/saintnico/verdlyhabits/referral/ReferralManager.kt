package com.saintnico.verdlyhabits.referral

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.billing.BillingManager
import com.saintnico.verdlyhabits.data.remote.firestore.ReferralRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

object ReferralManager {

    const val FRIENDS_REQUIRED = 2
    const val REFERRER_REWARD_DAYS = 7
    const val REFEREE_BONUS_DAYS = 3
    const val REFERRER_RETENTION_BONUS_DAYS = 3
    const val RETENTION_DAYS_REQUIRED = 7
    const val ANNUAL_REFERRAL_DISCOUNT_PERCENT = 20

    private const val PREFS = "verdly_referral"
    private const val KEY_CODE = "referral_code"
    private const val KEY_USED = "referral_used"
    private const val KEY_PENDING_CODE = "pending_referral_code"
    private const val KEY_REWARD_GRANTED = "referral_reward_granted"
    private const val KEY_PROMO_DISMISSED_UNTIL = "referral_promo_dismissed_until"
    private const val KEY_ANNUAL_OFFER_START = "referral_annual_offer_start"
    private const val KEY_ANNUAL_OFFER_DISMISSED = "referral_annual_offer_dismissed"
    private const val BONUS_KEY = "bonus_days_expiry"
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val PROMO_DISMISS_MS = 48L * 60 * 60 * 1000
    private const val ANNUAL_OFFER_WINDOW_MS = 7L * DAY_MS

    private val repository = ReferralRepository()

    fun getReferralCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var code = prefs.getString(KEY_CODE, null)
        if (code == null) {
            code = "VERDLY-" + UUID.randomUUID().toString().take(6).uppercase()
            prefs.edit().putString(KEY_CODE, code).apply()
        }
        return code
    }

    fun shortCode(fullCode: String): String = ReferralRepository.shortCodeFrom(fullCode)

    fun shareLink(fullCode: String): String =
        "https://verdly.app/r/${shortCode(fullCode)}"

    fun deepLink(fullCode: String): String =
        "verdly://referral?code=${ReferralRepository.normalizeCode(fullCode)}"

    fun storePendingReferralCode(context: Context, code: String) {
        val normalized = ReferralRepository.normalizeCode(code)
        if (normalized.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING_CODE, normalized)
            .apply()
    }

    fun peekPendingReferralCode(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PENDING_CODE, null)

    fun clearPendingReferralCode(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING_CODE)
            .apply()
    }

    suspend fun syncReferralProfile(context: Context): ReferralSyncResult = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return@withContext ReferralSyncResult(localOnly = true)
        val fullCode = getReferralCode(context)
        repository.ensureCodeRegistered(fullCode, shortCode(fullCode))
        val stats = repository.fetchStats(fullCode, FRIENDS_REQUIRED, REFERRER_REWARD_DAYS)
        var rewardJustGranted = false
        var retentionBonusesGranted = 0
        if (stats.qualifiedCount >= FRIENDS_REQUIRED && !isReferralRewardGranted(context)) {
            addBonusDays(context, REFERRER_REWARD_DAYS)
            markReferralRewardGranted(context)
            markAnnualOfferEligible(context)
            rewardJustGranted = true
        }
        retentionBonusesGranted = repository.processRetentionBonuses(uid, RETENTION_DAYS_REQUIRED)
        if (retentionBonusesGranted > 0) {
            addBonusDays(context, REFERRER_RETENTION_BONUS_DAYS * retentionBonusesGranted)
        }
        ReferralSyncResult(
            stats = stats,
            rewardJustGranted = rewardJustGranted,
            retentionBonusesGranted = retentionBonusesGranted,
            localOnly = false,
        )
    }

    suspend fun processPendingReferral(context: Context): Boolean = withContext(Dispatchers.IO) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val pending = prefs.getString(KEY_PENDING_CODE, null)?.trim().orEmpty()
        if (pending.isBlank()) return@withContext false

        val ownCode = getReferralCode(context).uppercase()
        if (pending.uppercase() == ownCode) {
            clearPendingReferralCode(context)
            return@withContext false
        }

        val alreadyUsed = prefs.getBoolean(KEY_USED, false)
        if (alreadyUsed) {
            clearPendingReferralCode(context)
            return@withContext false
        }

        val referrerUid = repository.claimReferral(pending, uid)
        if (referrerUid != null) {
            addBonusDays(context, REFEREE_BONUS_DAYS)
            prefs.edit()
                .putBoolean(KEY_USED, true)
                .apply()
            clearPendingReferralCode(context)
            true
        } else {
            false
        }
    }

    /** Deep link entry — stores code and applies when signed in. */
    suspend fun handleReferralLink(context: Context, code: String): Boolean {
        val normalized = ReferralRepository.normalizeCode(code)
        if (normalized.isBlank()) return false
        storePendingReferralCode(context, normalized)
        return processPendingReferral(context)
    }

    fun addBonusDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(BillingManager.PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val existing = prefs.getLong(BONUS_KEY, 0L)
        val base = maxOf(existing, now)
        prefs.edit().putLong(BONUS_KEY, base + days * DAY_MS).apply()
    }

    fun isInBonusPeriod(context: Context): Boolean {
        val prefs = context.getSharedPreferences(BillingManager.PREFS, Context.MODE_PRIVATE)
        val expiry = prefs.getLong(BONUS_KEY, 0L)
        return System.currentTimeMillis() < expiry
    }

    fun bonusDaysRemaining(context: Context): Int {
        val prefs = context.getSharedPreferences(BillingManager.PREFS, Context.MODE_PRIVATE)
        val expiry = prefs.getLong(BONUS_KEY, 0L)
        val remainingMs = expiry - System.currentTimeMillis()
        if (remainingMs <= 0) return 0
        return ((remainingMs + DAY_MS - 1) / DAY_MS).toInt()
    }

    fun shouldShowPromoBanner(context: Context, isPaidPro: Boolean, qualifiedCount: Int): Boolean {
        if (isPaidPro) return false
        if (qualifiedCount >= FRIENDS_REQUIRED && isReferralRewardGranted(context)) return false
        val dismissedUntil = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_PROMO_DISMISSED_UNTIL, 0L)
        return System.currentTimeMillis() >= dismissedUntil
    }

    fun dismissPromoBanner(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_PROMO_DISMISSED_UNTIL, System.currentTimeMillis() + PROMO_DISMISS_MS)
            .apply()
    }

    private fun isReferralRewardGranted(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_REWARD_GRANTED, false)

    private fun markReferralRewardGranted(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_REWARD_GRANTED, true)
            .apply()
    }

    private fun markAnnualOfferEligible(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_ANNUAL_OFFER_START, 0L) == 0L) {
            prefs.edit().putLong(KEY_ANNUAL_OFFER_START, System.currentTimeMillis()).apply()
        }
    }

    fun shouldShowAnnualOffer(context: Context, hasPaidSubscription: Boolean): Boolean {
        if (hasPaidSubscription) return false
        if (!isReferralRewardGranted(context)) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ANNUAL_OFFER_DISMISSED, false)) return false
        val start = prefs.getLong(KEY_ANNUAL_OFFER_START, 0L)
        if (start == 0L) return false
        return System.currentTimeMillis() < start + ANNUAL_OFFER_WINDOW_MS
    }

    fun annualOfferDaysRemaining(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val start = prefs.getLong(KEY_ANNUAL_OFFER_START, 0L)
        if (start == 0L) return 0
        val remainingMs = (start + ANNUAL_OFFER_WINDOW_MS) - System.currentTimeMillis()
        if (remainingMs <= 0) return 0
        return ((remainingMs + DAY_MS - 1) / DAY_MS).toInt()
    }

    fun dismissAnnualOffer(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ANNUAL_OFFER_DISMISSED, true)
            .apply()
    }

    suspend fun registerVanityCode(context: Context, vanity: String): Result<String> {
        val fullCode = getReferralCode(context)
        return repository.registerVanityShortCode(fullCode, vanity)
    }

    fun discountedAnnualPriceKes(): String {
        val base = 2999
        val discounted = base * (100 - ANNUAL_REFERRAL_DISCOUNT_PERCENT) / 100
        return "Ksh %,d".format(discounted)
    }
}

data class ReferralSyncResult(
    val stats: com.saintnico.verdlyhabits.data.remote.firestore.ReferralStats? = null,
    val rewardJustGranted: Boolean = false,
    val retentionBonusesGranted: Int = 0,
    val localOnly: Boolean = false,
)
