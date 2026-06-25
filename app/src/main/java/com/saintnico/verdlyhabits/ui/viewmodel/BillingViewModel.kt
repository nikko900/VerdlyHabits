package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.saintnico.verdlyhabits.billing.BillingManager
import com.saintnico.verdlyhabits.domain.PremiumGate
import com.saintnico.verdlyhabits.referral.ReferralManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val billing = BillingManager(application, viewModelScope)

    private val _isPro = MutableStateFlow(billing.getLocalProStatus())
    /** Real Google Play subscription — not affected by [PremiumGate] dev overrides. */
    val hasPaidSubscription: StateFlow<Boolean> = _isPro.asStateFlow()
    val isPro: StateFlow<Boolean> = _isPro
        .map { PremiumGate.effectiveIsPro(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PremiumGate.effectiveIsPro(billing.getLocalProStatus()),
        )

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

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

    private val proListener: (Boolean) -> Unit = { _isPro.value = it }
    private val productsListener: (List<ProductDetails>) -> Unit = { _products.value = it }

    init {
        billing.addProListener(proListener)
        billing.addProductsListener(productsListener)
        billing.initialize()
        refreshBonusAccess()
    }

    /** Call after applying a referral deep link so [hasFullAccess] recomputes. */
    fun refreshBonusAccess() {
        _accessRefresh.value += 1
    }

    fun isInFreeTrial(): Boolean = billing.isInFreeTrial()

    fun freeTrialDaysRemaining(): Int = billing.freeTrialDaysRemaining()

    fun getInstallDateMillis(): Long = billing.getInstallDateMillis()

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails, isSubscription: Boolean) {
        billing.launchPurchaseFlow(activity, productDetails, isSubscription)
    }

    fun restorePurchases() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            billing.restorePurchases()
        }
    }

    override fun onCleared() {
        billing.removeProListener(proListener)
        billing.removeProductsListener(productsListener)
        billing.release()
        super.onCleared()
    }
}
