package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.remote.firestore.ReferralStats
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.referral.ReferralShareHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReferralUiState(
    val isLoading: Boolean = true,
    val fullCode: String = "",
    val shortCode: String = "",
    val shareLink: String = "",
    val deepLink: String = "",
    val qualifiedCount: Int = 0,
    val friendsRequired: Int = ReferralManager.FRIENDS_REQUIRED,
    val rewardDays: Int = ReferralManager.REFERRER_REWARD_DAYS,
    val rewardJustGranted: Boolean = false,
    val retentionBonusesGranted: Int = 0,
    val showAnnualOffer: Boolean = false,
    val annualOfferDaysLeft: Int = 0,
    val showBanner: Boolean = false,
)

class ReferralViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>()
    private val _state = MutableStateFlow(ReferralUiState())
    val state: StateFlow<ReferralUiState> = _state.asStateFlow()

    init {
        hydrateLocalCode()
    }

    private fun hydrateLocalCode() {
        val code = ReferralManager.getReferralCode(context)
        _state.update {
            it.copy(
                fullCode = code,
                shortCode = ReferralManager.shortCode(code),
                shareLink = ReferralManager.shareLink(code),
                deepLink = ReferralManager.deepLink(code),
                isLoading = false,
            )
        }
    }

    fun ensureReady() {
        if (_state.value.fullCode.isBlank()) hydrateLocalCode()
    }

    fun refresh(hasPaidSubscription: Boolean) {
        viewModelScope.launch {
            val hadCode = _state.value.fullCode.isNotBlank()
            if (!hadCode) {
                _state.update { it.copy(isLoading = true) }
            }
            try {
                val code = ReferralManager.getReferralCode(context)
                val result = ReferralManager.syncReferralProfile(context)
                val stats = result.stats ?: ReferralStats(
                    fullCode = code,
                    shortCode = ReferralManager.shortCode(code),
                    qualifiedCount = 0,
                    friendsRequired = ReferralManager.FRIENDS_REQUIRED,
                    rewardDays = ReferralManager.REFERRER_REWARD_DAYS,
                )
                _state.update {
                    it.copy(
                        isLoading = false,
                        fullCode = stats.fullCode,
                        shortCode = stats.shortCode,
                        shareLink = ReferralManager.shareLink(stats.fullCode),
                        deepLink = ReferralManager.deepLink(stats.fullCode),
                        qualifiedCount = stats.qualifiedCount,
                        friendsRequired = stats.friendsRequired,
                        rewardDays = stats.rewardDays,
                        rewardJustGranted = result.rewardJustGranted,
                        retentionBonusesGranted = result.retentionBonusesGranted,
                        showAnnualOffer = ReferralManager.shouldShowAnnualOffer(context, hasPaidSubscription),
                        annualOfferDaysLeft = ReferralManager.annualOfferDaysRemaining(context),
                        showBanner = ReferralManager.shouldShowPromoBanner(
                            context,
                            hasPaidSubscription,
                            stats.qualifiedCount,
                        ),
                    )
                }
            } catch (_: Exception) {
                val code = ReferralManager.getReferralCode(context)
                _state.update {
                    it.copy(
                        isLoading = false,
                        fullCode = code,
                        shortCode = ReferralManager.shortCode(code),
                        shareLink = ReferralManager.shareLink(code),
                        deepLink = ReferralManager.deepLink(code),
                        showBanner = ReferralManager.shouldShowPromoBanner(
                            context,
                            hasPaidSubscription,
                            it.qualifiedCount,
                        ),
                    )
                }
            }
        }
    }

    fun dismissBanner() {
        ReferralManager.dismissPromoBanner(context)
        _state.update { it.copy(showBanner = false) }
    }

    fun copyInvite(): String = ReferralShareHelper.buildInviteMessage(context, _state.value)

    fun shareInviteFromContext(shareContext: android.content.Context): Boolean =
        ReferralShareHelper.shareInvite(shareContext, _state.value)

    fun dismissAnnualOffer() {
        ReferralManager.dismissAnnualOffer(context)
        _state.update { it.copy(showAnnualOffer = false) }
    }

    fun registerVanityCode(vanity: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = ReferralManager.registerVanityCode(context, vanity)
            result.onSuccess { short ->
                _state.update {
                    it.copy(
                        shortCode = short,
                        shareLink = ReferralManager.shareLink(it.fullCode),
                    )
                }
                onResult(true, short)
            }.onFailure { e ->
                onResult(false, e.message ?: "Could not set link")
            }
        }
    }

    fun consumeRewardCelebration() {
        _state.update { it.copy(rewardJustGranted = false) }
    }

    fun consumeRetentionBonuses() {
        _state.update { it.copy(retentionBonusesGranted = 0) }
    }
}
