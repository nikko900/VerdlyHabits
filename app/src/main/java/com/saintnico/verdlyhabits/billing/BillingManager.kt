package com.saintnico.verdlyhabits.billing

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/*
 PLAY CONSOLE SETUP REQUIRED:
 1. Go to Monetize > Products > Subscriptions
 2. Create product ID: verdly_pro_weekly
    Price: KES 99 / week
    Billing period: Weekly
    Free trial: 7 days (optional, match other subs)
 3. Create product ID: verdly_pro_monthly
    Price: KES 399 / month
    Billing period: Monthly
    Free trial: 7 days
 4. Create product ID: verdly_pro_annual
    Price: KES 2,999 / year
    Billing period: Yearly
    Free trial: 7 days
 5. Go to Monetize > Products > In-app products
 6. Create product ID: verdly_pro_lifetime
    Price: KES 4,999
    Type: One-time purchase
    before billing can be tested on device
 7. Add test accounts in Play Console >
    License Testing for sandbox testing
*/

class BillingManager(
    private val context: Context,
    private val appScope: CoroutineScope,
) {

    private var billingClient: BillingClient? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val initialized = AtomicBoolean(false)

    companion object {
        private const val TAG = "VerdlyBilling"
        const val PREFS = "verdly_billing"
        const val PRODUCT_WEEKLY = "verdly_pro_weekly"
        const val PRODUCT_MONTHLY = "verdly_pro_monthly"
        const val PRODUCT_ANNUAL = "verdly_pro_annual"
        const val PRODUCT_LIFETIME = "verdly_pro_lifetime"
        private const val KEY_IS_PRO = "is_pro"
        private const val KEY_TRIAL_START = "trial_start"
        const val KEY_INSTALL_DATE = "install_date"
    }

    private val prefs
        get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val purchasesListener = PurchasesUpdatedListener { billingResult, purchases ->
        appScope.launch(Dispatchers.IO) {
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases?.forEach { handlePurchaseOnIo(it) }
            } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                Log.w(TAG, "PurchasesUpdatedListener: ${billingResult.debugMessage}")
            }
        }
    }

    private val _listeners = mutableSetOf<(Boolean) -> Unit>()
    private val _productListeners = mutableSetOf<(List<ProductDetails>) -> Unit>()

    @Volatile
    private var lastKnownPro: Boolean = false

    fun addProListener(listener: (Boolean) -> Unit) {
        _listeners.add(listener)
        listener(lastKnownPro)
    }

    fun removeProListener(listener: (Boolean) -> Unit) {
        _listeners.remove(listener)
    }

    fun addProductsListener(listener: (List<ProductDetails>) -> Unit) {
        _productListeners.add(listener)
    }

    fun removeProductsListener(listener: (List<ProductDetails>) -> Unit) {
        _productListeners.remove(listener)
    }

    private fun emitPro(value: Boolean) {
        lastKnownPro = value
        mainHandler.post {
            _listeners.forEach { it(value) }
        }
    }

    private fun emitProducts(list: List<ProductDetails>) {
        mainHandler.post {
            _productListeners.forEach { it(list) }
        }
    }

    fun initialize() {
        if (!initialized.compareAndSet(false, true)) return

        ensureInstallDate()
        lastKnownPro = getLocalProStatus()
        emitPro(lastKnownPro)

        billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesListener)
            .enablePendingPurchases()
            .build()

        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    appScope.launch(Dispatchers.IO) {
                        queryProductsOnIo()
                        restorePurchasesOnIo()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage} — keeping cached entitlements")
                    // Never strip local Pro because Play is unavailable
                    emitPro(getLocalProStatus())
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected; will retry on next initialize/restore")
            }
        })
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

    private suspend fun queryProductsOnIo() = withContext(Dispatchers.IO) {
        val client = billingClient ?: return@withContext
        val merged = mutableListOf<ProductDetails>()

        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_WEEKLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_MONTHLY)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ANNUAL)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build(),
                )
            ).build()

        val inAppParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_LIFETIME)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                )
            ).build()

        suspend fun queryOne(params: QueryProductDetailsParams): List<ProductDetails> =
            suspendCancellableCoroutine { cont ->
                client.queryProductDetailsAsync(params) { billingResult, list ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(list.orEmpty())
                    } else {
                        Log.w(TAG, "queryProductDetails failed: ${billingResult.debugMessage}")
                        cont.resume(emptyList())
                    }
                }
            }

        merged.addAll(queryOne(subsParams))
        merged.addAll(queryOne(inAppParams))
        emitProducts(merged.toList())
    }

    fun launchPurchaseFlow(
        activity: android.app.Activity,
        productDetails: ProductDetails,
        isSubscription: Boolean,
    ) {
        val offerToken = if (isSubscription) {
            productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
        } else null

        if (isSubscription && offerToken == null) {
            Log.e(TAG, "Missing subscription offer token for ${productDetails.productId}")
            return
        }

        val productDetailsParams = if (isSubscription) {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken!!)
                .build()
        } else {
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient?.launchBillingFlow(activity, flowParams)
    }

    private suspend fun handlePurchaseOnIo(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        savePurchaseLocally(true)
        emitPro(true)
        if (!purchase.isAcknowledged) {
            val client = billingClient ?: return
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            withContext(Dispatchers.IO) {
                client.acknowledgePurchase(params) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.w(TAG, "acknowledgePurchase: ${result.debugMessage}")
                    }
                }
            }
        }
    }

    suspend fun restorePurchases() = withContext(Dispatchers.IO) {
        restorePurchasesOnIo()
    }

    private suspend fun restorePurchasesOnIo() {
        val client = billingClient ?: run {
            emitPro(getLocalProStatus())
            return
        }
        if (!client.isReady) {
            emitPro(getLocalProStatus())
            return
        }

        var active = false

        suspend fun queryPurchases(type: String): List<Purchase> =
            suspendCancellableCoroutine { cont ->
                client.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(type).build()
                ) { result, purchases ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        cont.resume(purchases.orEmpty())
                    } else {
                        Log.w(TAG, "queryPurchases ($type): ${result.debugMessage}")
                        cont.resume(emptyList())
                    }
                }
            }

        val subs = queryPurchases(BillingClient.ProductType.SUBS)
        if (subs.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }) {
            active = true
        }

        val inApp = queryPurchases(BillingClient.ProductType.INAPP)
        if (inApp.any {
                it.products.contains(PRODUCT_LIFETIME) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
        ) {
            active = true
        }

        if (active) {
            savePurchaseLocally(true)
            emitPro(true)
        } else if (!client.isReady) {
            // Billing not ready — never revoke local Pro because of a flaky client
            emitPro(getLocalProStatus())
        } else {
            // Verified empty while connected — sync local cache down
            savePurchaseLocally(false)
            emitPro(false)
        }
    }

    private fun savePurchaseLocally(isPro: Boolean) {
        prefs.edit().putBoolean(KEY_IS_PRO, isPro).apply()
    }

    fun getLocalProStatus(): Boolean = prefs.getBoolean(KEY_IS_PRO, false)

    /** 7-day free trial from first app open (device-local). */
    fun isInFreeTrial(): Boolean {
        val trialStart = prefs.getLong(KEY_TRIAL_START, 0L)
        if (trialStart == 0L) {
            prefs.edit().putLong(KEY_TRIAL_START, System.currentTimeMillis()).apply()
            return true
        }
        val daysSinceStart = (System.currentTimeMillis() - trialStart) / (1000 * 60 * 60 * 24)
        return daysSinceStart < 7
    }

    /** Whole days remaining in local trial (0–7). */
    fun freeTrialDaysRemaining(): Int {
        val trialStart = prefs.getLong(KEY_TRIAL_START, 0L)
        val start = if (trialStart == 0L) System.currentTimeMillis() else trialStart
        val daysUsed = ((System.currentTimeMillis() - start) / (1000 * 60 * 60 * 24)).toInt()
        return (7 - daysUsed).coerceIn(0, 7)
    }

    fun release() {
        try {
            billingClient?.endConnection()
        } catch (_: Exception) {
        }
        billingClient = null
        initialized.set(false)
    }
}
