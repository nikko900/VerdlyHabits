package com.saintnico.verdlyhabits.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import com.saintnico.verdlyhabits.data.streak.StreakRepository
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/*
 REVENUECAT DASHBOARD SETUP (Shipaton 2026):
 1. Project → API keys: Test Store key is bundled as default; put the production
    goog_… key in local.properties as REVENUECAT_API_KEY before release.
 2. Entitlement: verdly_habits_pro — attach weekly/monthly/annual/lifetime products.
 3. Products (must match Play Console IDs):
    Subscriptions: verdly_pro_weekly, verdly_pro_monthly, verdly_pro_annual
    Non-consumable: verdly_pro_lifetime
    Consumables: verdly_streak_shield, verdly_coins_100, verdly_coins_500,
                 verdly_coins_1200, verdly_coins_3000
 4. Offerings:
    - default   → pro packages (weekly/monthly/annual/lifetime)
    - coin_packs → the four coin consumables
 5. Mark all coin + shield products as CONSUMABLE in RevenueCat so it consumes
    them on Play automatically. Never call consumeAsync manually.
*/

class BillingManager(
    private val context: Context,
    private val appScope: CoroutineScope,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val initialized = AtomicBoolean(false)

    companion object {
        private const val TAG = "VerdlyBilling"
        const val PREFS = "verdly_billing"

        /** RevenueCat entitlement id — all Pro products attach to this. */
        const val ENTITLEMENT_PRO = "verdly_habits_pro"

        /** Offering ids configured in the RevenueCat dashboard. */
        const val OFFERING_DEFAULT = "default"
        const val OFFERING_COIN_PACKS = "coin_packs"

        const val PRODUCT_WEEKLY = "verdly_pro_weekly"
        const val PRODUCT_MONTHLY = "verdly_pro_monthly"
        const val PRODUCT_ANNUAL = "verdly_pro_annual"
        const val PRODUCT_LIFETIME = "verdly_pro_lifetime"
        const val PRODUCT_STREAK_SHIELD = "verdly_streak_shield"

        /** Coin consumables — amount already includes the bonus curve. */
        val COIN_PACKS: Map<String, Int> = mapOf(
            "verdly_coins_100" to 100,
            "verdly_coins_500" to 500,
            "verdly_coins_1200" to 1200,
            "verdly_coins_3000" to 3000,
        )

        private const val KEY_IS_PRO = "is_pro"
        private const val KEY_TRIAL_START = "trial_start"
        const val KEY_INSTALL_DATE = "install_date"
        private const val KEY_PROCESSED_TX = "processed_consumable_tx"
    }

    private val prefs
        get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _proListeners = mutableSetOf<(Boolean) -> Unit>()
    private val _proOfferingListeners = mutableSetOf<(Offering?) -> Unit>()

    @Volatile
    private var lastKnownPro: Boolean = false

    @Volatile
    var proOffering: Offering? = null
        private set

    /** Set by the wallet layer so coin purchases credit through one code path. */
    @Volatile
    var coinPurchaseCrediter: (suspend (coins: Int, productId: String, transactionId: String) -> Unit)? = null

    private val customerInfoListener =
        com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener { info ->
            applyEntitlement(info)
        }

    fun isRevenueCatReady(): Boolean = RevenueCatBootstrap.isConfigured()

    fun addProListener(listener: (Boolean) -> Unit) {
        _proListeners.add(listener)
        listener(lastKnownPro)
    }

    fun removeProListener(listener: (Boolean) -> Unit) {
        _proListeners.remove(listener)
    }

    fun addProOfferingListener(listener: (Offering?) -> Unit) {
        _proOfferingListeners.add(listener)
        listener(proOffering)
    }

    fun removeProOfferingListener(listener: (Offering?) -> Unit) {
        _proOfferingListeners.remove(listener)
    }

    private fun emitPro(value: Boolean) {
        lastKnownPro = value
        mainHandler.post { _proListeners.forEach { it(value) } }
    }

    private fun emitProOffering(offering: Offering?) {
        proOffering = offering
        mainHandler.post { _proOfferingListeners.forEach { it(offering) } }
    }

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        ensureInstallDate()
        lastKnownPro = getLocalProStatus()
        emitPro(lastKnownPro)

        if (!isRevenueCatReady()) {
            Log.w(TAG, "RevenueCat not configured. cached entitlements + local trial only")
            return
        }

        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { error ->
                Log.w(TAG, "getCustomerInfo: ${error.message}. keeping cached Pro")
                emitPro(getLocalProStatus())
            },
            onSuccess = { info -> applyEntitlement(info) },
        )
        Purchases.sharedInstance.updatedCustomerInfoListener = customerInfoListener

        appScope.launch(Dispatchers.IO) {
            emitProOffering(fetchOffering(OFFERING_DEFAULT))
        }
    }

    data class OfferingFetchResult(
        val offering: Offering?,
        val revenueCatReady: Boolean,
        val errorMessage: String? = null,
        val allOfferingIds: List<String> = emptyList(),
    )

    /** Resolves an offering by id, falling back to the dashboard "current" offering. */
    suspend fun fetchOffering(offeringId: String?): Offering? =
        fetchOfferingDetailed(offeringId).offering

    suspend fun fetchOfferingDetailed(offeringId: String?): OfferingFetchResult = withContext(Dispatchers.IO) {
        if (!isRevenueCatReady()) {
            return@withContext OfferingFetchResult(
                offering = null,
                revenueCatReady = false,
                errorMessage = BillingErrors.STORE_UNAVAILABLE,
            )
        }
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    Log.w(
                        TAG,
                        "getOfferings: ${error.code} ${error.message} underlying=${error.underlyingErrorMessage}",
                    )
                    cont.resume(
                        OfferingFetchResult(
                            offering = null,
                            revenueCatReady = true,
                            errorMessage = BillingErrors.userMessage(error),
                        ),
                    )
                },
                onSuccess = { offerings ->
                    val ids = offerings.all.keys.toList()
                    val offering = resolveOffering(offerings, offeringId)
                    if (offering == null) {
                        cont.resume(
                            OfferingFetchResult(
                                offering = null,
                                revenueCatReady = true,
                                errorMessage = BillingErrors.STORE_UNAVAILABLE,
                                allOfferingIds = ids,
                            ),
                        )
                    } else {
                        val productIds = offering.availablePackages.map { it.product.id }
                        Log.i(TAG, "offering \"${offering.identifier}\" packages: $productIds")
                        cont.resume(
                            OfferingFetchResult(
                                offering = offering,
                                revenueCatReady = true,
                                allOfferingIds = ids,
                            ),
                        )
                    }
                },
            )
        }
    }

    private fun resolveOffering(offerings: Offerings, offeringId: String?): Offering? {
        if (offeringId == OFFERING_COIN_PACKS) {
            val wanted = COIN_PACKS.keys
            val named = offerings.getOffering(OFFERING_COIN_PACKS)
            if (named != null && coinMatchCount(named, wanted) > 0) return named
            return offerings.all.values
                .maxByOrNull { coinMatchCount(it, wanted) }
                ?.takeIf { coinMatchCount(it, wanted) > 0 }
        }
        return offeringId?.let { offerings.getOffering(it) } ?: offerings.current
    }

    private fun coinMatchCount(offering: Offering, wanted: Set<String>): Int =
        offering.availablePackages.count { pkg ->
            pkg.product.id.substringBefore(":") in wanted
        }

    private fun applyEntitlement(customerInfo: CustomerInfo) {
        val active = customerInfo.entitlements[ENTITLEMENT_PRO]?.isActive == true
        savePurchaseLocally(active)
        emitPro(active)
    }

    /**
     * Purchases any package (pro subscription, lifetime, shield, or coin pack).
     * Consumable crediting happens exactly once per transaction, guarded by a
     * processed-transaction set so restores can never double-credit.
     */
    fun purchasePackage(
        activity: Activity,
        pkg: Package,
        onSuccess: (CustomerInfo) -> Unit = {},
        onError: (PurchasesError, Boolean) -> Unit = { _, _ -> },
    ) {
        if (!isRevenueCatReady()) {
            onError(
                PurchasesError(com.revenuecat.purchases.PurchasesErrorCode.ConfigurationError),
                false,
            )
            return
        }
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, pkg).build(),
            onError = { error, userCancelled ->
                if (!userCancelled) Log.w(TAG, "purchase: ${error.message}")
                mainHandler.post { onError(error, userCancelled) }
            },
            onSuccess = { transaction, customerInfo ->
                applyEntitlement(customerInfo)
                appScope.launch(Dispatchers.IO) {
                    creditConsumableIfNeeded(
                        pkg,
                        transaction?.orderId?.takeIf { it.isNotBlank() } ?: transaction?.purchaseToken,
                    )
                    // Wait until coins are in Room before UI celebrates / refreshes.
                    mainHandler.post { onSuccess(customerInfo) }
                }
            },
        )
    }

    private suspend fun creditConsumableIfNeeded(pkg: Package, transactionId: String?) {
        val baseProductId = pkg.product.id.substringBefore(":")
        val txId = transactionId ?: "${baseProductId}_${System.currentTimeMillis()}"
        if (isTransactionProcessed(txId)) return

        val coins = COIN_PACKS[baseProductId]
        when {
            coins != null -> {
                markTransactionProcessed(txId)
                val crediter = coinPurchaseCrediter
                if (crediter != null) {
                    runCatching { crediter(coins, baseProductId, txId) }
                        .onFailure { Log.e(TAG, "Coin credit failed. $baseProductId", it) }
                } else {
                    Log.e(TAG, "Coin purchase with no crediter attached. $baseProductId")
                }
            }
            baseProductId == PRODUCT_STREAK_SHIELD -> {
                markTransactionProcessed(txId)
                val credited = StreakRepository.get(context).creditPurchasedShields(1)
                Log.i(TAG, "streak shield credited=$credited")
            }
        }
    }

    private fun isTransactionProcessed(txId: String): Boolean =
        prefs.getStringSet(KEY_PROCESSED_TX, emptySet())?.contains(txId) == true

    private fun markTransactionProcessed(txId: String) {
        val current = prefs.getStringSet(KEY_PROCESSED_TX, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(txId)
        // Keep the guard set bounded.
        val trimmed = if (current.size > 200) current.drop(current.size - 200).toSet() else current
        prefs.edit().putStringSet(KEY_PROCESSED_TX, trimmed).apply()
    }

    suspend fun restorePurchases() = withContext(Dispatchers.IO) {
        if (!isRevenueCatReady()) {
            emitPro(getLocalProStatus())
            return@withContext
        }
        suspendCancellableCoroutine { cont ->
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { error ->
                    Log.w(TAG, "restore: ${error.message}")
                    emitPro(getLocalProStatus())
                    cont.resume(Unit)
                },
                onSuccess = { info ->
                    applyEntitlement(info)
                    cont.resume(Unit)
                },
            )
        }
    }

    /** Ties RevenueCat identity to the Firebase uid for cross-device entitlements. */
    fun syncFirebaseUser(firebaseUid: String?) {
        if (!isRevenueCatReady()) return
        if (firebaseUid.isNullOrBlank()) {
            Purchases.sharedInstance.logOutWith(
                onError = { Log.w(TAG, "logOut: ${it.message}") },
                onSuccess = { info -> applyEntitlement(info) },
            )
        } else if (Purchases.sharedInstance.appUserID != firebaseUid) {
            Purchases.sharedInstance.logInWith(
                firebaseUid,
                onError = { error ->
                    Log.w(TAG, "logIn: ${error.message}")
                    emitPro(getLocalProStatus())
                },
                onSuccess = { info, _ -> applyEntitlement(info) },
            )
        } else {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = {
                    Log.w(TAG, "getCustomerInfo after sync: ${it.message}")
                    emitPro(getLocalProStatus())
                },
                onSuccess = { info -> applyEntitlement(info) },
            )
        }
    }

    private fun ensureInstallDate() {
        if (prefs.getLong(KEY_INSTALL_DATE, 0L) == 0L) {
            prefs.edit().putLong(KEY_INSTALL_DATE, System.currentTimeMillis()).apply()
        }
    }

    fun getInstallDateMillis(): Long {
        ensureInstallDate()
        return prefs.getLong(KEY_INSTALL_DATE, System.currentTimeMillis())
    }

    private fun savePurchaseLocally(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO, isPro).apply()
    }

    fun getLocalProStatus(): Boolean = prefs.getBoolean(KEY_IS_PRO, false)

    /** 7-day device-local trial — doubles as the Shipaton judge free-trial path. */
    fun isInFreeTrial(): Boolean {
        val trialStart = prefs.getLong(KEY_TRIAL_START, 0L)
        if (trialStart == 0L) {
            prefs.edit().putLong(KEY_TRIAL_START, System.currentTimeMillis()).apply()
            return true
        }
        val daysSinceStart = (System.currentTimeMillis() - trialStart) / (1000 * 60 * 60 * 24)
        return daysSinceStart < 7
    }

    fun freeTrialDaysRemaining(): Int {
        val trialStart = prefs.getLong(KEY_TRIAL_START, 0L)
        val start = if (trialStart == 0L) System.currentTimeMillis() else trialStart
        val daysUsed = ((System.currentTimeMillis() - start) / (1000 * 60 * 60 * 24)).toInt()
        return (7 - daysUsed).coerceIn(0, 7)
    }

    fun release() {
        if (isRevenueCatReady()) {
            runCatching { Purchases.sharedInstance.updatedCustomerInfoListener = null }
        }
        initialized.set(false)
    }
}
