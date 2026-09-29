package com.saintnico.verdlyhabits.domain

import com.saintnico.verdlyhabits.data.model.MembershipTier

object MembershipStatusResolver {
    fun resolve(
        hasPaidSubscription: Boolean,
        inFreeTrial: Boolean,
        inReferralBonus: Boolean,
    ): MembershipTier = when {
        hasPaidSubscription -> MembershipTier.PREMIUM
        inReferralBonus -> MembershipTier.PREMIUM
        inFreeTrial -> MembershipTier.TRIAL
        else -> MembershipTier.FREE
    }
}
