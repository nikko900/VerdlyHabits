package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.saintnico.verdlyhabits.billing.BillingErrors
import com.saintnico.verdlyhabits.billing.BillingManager
import com.saintnico.verdlyhabits.domain.MembershipStatusResolver
import com.saintnico.verdlyhabits.domain.PremiumGate
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.referral.ReferralManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    val billing = BillingManager(application, viewModelScope)

    private val _isPro = MutableStateFlow(billing.getLocalProStatus())
    /** Real RevenueCat entitlement — not affected by [PremiumGate] dev overrides. */
    val hasPaidSubscription: StateFlow<Boolean> = _isPro.asStateFlow()
    val isPro: StateFlow<Boolean> = _isPro
        .map { PremiumGate.effectiveIsPro(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PremiumGate.effectiveIsPro(billing.getLocalProStatus()),
        )

    private val _proOffering = MutableStateFlow<Offering?>(null)
    val proOffering: StateFlow<Offering?> = _proOffering.asStateFlow()

    private val _proOfferingLoading = MutableStateFlow(false)
    val proOfferingLoading: StateFlow<Boolean> = _proOfferingLoading.asStateFlow()

    private val _proOfferingError = MutableStateFlow<String?>(null)
    val proOfferingError: StateFlow<String?> = _proOfferingError.asStateFlow()

    /** Packages from the default offering, for the Pro paywall. */
    val proPackages: StateFlow<List<Package>> = _proOffering
        .map { it?.availablePackages.orEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _accessRefresh = MutableStateFlow(0)

    private fun realFullAccess(pro: Boolean): Boolean =
        pro || billing.isInFreeTrial() || ReferralManager.isInBonusPeriod(getApplication())

    val hasFullAccess: StateFlow<Boolean> = combine(_isPro, _accessRefresh) { pro, _ ->
        PremiumGate.grantsFullAccess(realFullAccess(pro))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PremiumGate.grantsFullAccess(
            realFullAccess(billing.getLocalProStatus()),
        ),
    )

    val membershipTier: StateFlow<MembershipTier> = combine(_isPro, _accessRefresh) { pro, _ ->
        MembershipStatusResolver.resolve(
            hasPaidSubscription = pro,
            inFreeTrial = billing.isInFreeTrial() && !pro,
            inReferralBonus = ReferralManager.isInBonusPeriod(getApplication()) && !pro,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MembershipStatusResolver.resolve(
            hasPaidSubscription = billing.getLocalProStatus(),
            inFreeTrial = billing.isInFreeTrial() && !billing.getLocalProStatus(),
            inReferralBonus = ReferralManager.isInBonusPeriod(getApplication()) &&
                !billing.getLocalProStatus(),
        ),
    )

    /** Coin pack offering (coin_packs) — loaded lazily for the coin sheet. */
    private val _coinOffering = MutableStateFlow<Offering?>(null)
    val coinOffering: StateFlow<Offering?> = _coinOffering.asStateFlow()

    private val _coinOfferingError = MutableStateFlow(false)
    val coinOfferingError: StateFlow<Boolean> = _coinOfferingError.asStateFlow()

    private val _coinOfferingStatusMessage = MutableStateFlow<String?>(null)
    val coinOfferingStatusMessage: StateFlow<String?> = _coinOfferingStatusMessage.asStateFlow()

    private val _coinOfferingLoading = MutableStateFlow(false)
    val coinOfferingLoading: StateFlow<Boolean> = _coinOfferingLoading.asStateFlow()

    val isRevenueCatReady: Boolean get() = billing.isRevenueCatReady()

    private val proListener: (Boolean) -> Unit = { _isPro.value = it }
    private val offeringListener: (Offering?) -> Unit = { _proOffering.value = it }

    init {
        // Coin purchases confirmed by RevenueCat credit the wallet exactly once.
        billing.coinPurchaseCrediter = { coins, productId, txId ->
            com.saintnico.verdlyhabits.data.coins.CoinWalletRepository
                .get(getApplication())
                .creditPurchase(coins, productId, txId)
        }
        billing.addProListener(proListener)
        billing.addProOfferingListener(offeringListener)
        billing.initialize()
        refreshBonusAccess()
    }

    fun loadProOffering() {
        viewModelScope.launch(Dispatchers.IO) {
            _proOfferingLoading.value = true
            _proOfferingError.value = null
            val result = billing.fetchOfferingDetailed(BillingManager.OFFERING_DEFAULT)
            _proOffering.value = result.offering
            val packages = result.offering?.availablePackages.orEmpty()
            _proOfferingError.value = when {
                result.offering != null && packages.isNotEmpty() -> null
                else -> result.errorMessage ?: BillingErrors.STORE_UNAVAILABLE
            }
            _proOfferingLoading.value = false
        }
    }

    fun loadCoinOffering() {
        viewModelScope.launch(Dispatchers.IO) {
            _coinOfferingLoading.value = true
            _coinOfferingError.value = false
            _coinOfferingStatusMessage.value = null
            val result = billing.fetchOfferingDetailed(BillingManager.OFFERING_COIN_PACKS)
            _coinOffering.value = result.offering
            val packages = result.offering?.availablePackages.orEmpty()
            val matched = BillingManager.COIN_PACKS.keys.count { id ->
                packages.any { it.product.id.substringBefore(":") == id }
            }
            _coinOfferingError.value = when {
                !result.revenueCatReady -> true
                result.offering == null -> true
                packages.isEmpty() -> true
                matched == 0 -> true
                else -> false
            }
            _coinOfferingStatusMessage.value = when {
                matched > 0 -> null
                !result.revenueCatReady ->
                    result.errorMessage ?: BillingErrors.STORE_UNAVAILABLE
                else ->
                    result.errorMessage ?: BillingErrors.STORE_UNAVAILABLE
            }
            _coinOfferingLoading.value = false
        }
    }

    /** Call after applying a referral deep link so [hasFullAccess] recomputes. */
    fun refreshBonusAccess() {
        _accessRefresh.value += 1
    }

    fun isInFreeTrial(): Boolean = billing.isInFreeTrial()

    fun freeTrialDaysRemaining(): Int = billing.freeTrialDaysRemaining()

    fun getInstallDateMillis(): Long = billing.getInstallDateMillis()

    fun purchase(
        activity: Activity,
        pkg: Package,
        onSuccess: () -> Unit = {},
        onError: (message: String, userCancelled: Boolean) -> Unit = { _, _ -> },
    ) {
        billing.purchasePackage(
            activity = activity,
            pkg = pkg,
            onSuccess = { onSuccess() },
            onError = { error, cancelled -> onError(BillingErrors.userMessage(error), cancelled) },
        )
    }

    fun restorePurchases() {
        viewModelScope.launch(Dispatchers.IO) {
            billing.restorePurchases()
        }
    }

    /** Keep RevenueCat identity aligned with the signed-in Firebase user. */
    fun syncIdentity(firebaseUid: String?) {
        billing.syncFirebaseUser(firebaseUid)
        if (!firebaseUid.isNullOrBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                billing.restorePurchases()
            }
        }
    }

    override fun onCleared() {
        billing.removeProListener(proListener)
        billing.removeProOfferingListener(offeringListener)
        billing.release()
        super.onCleared()
    }
}
