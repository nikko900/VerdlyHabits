package com.saintnico.verdlyhabits.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinEconomyTest {

    @Test
    fun firstOathGrantIsTheMinimumThatCanActuallyStake() {
        assertTrue(CoinEconomy.FIRST_OATH_GRANT >= CoinEconomy.minBalanceToStake())
        assertTrue(CoinEconomy.canStake(CoinEconomy.FIRST_OATH_GRANT))
        assertFalse(CoinEconomy.canStake(CoinEconomy.MIN_STAKE))
        assertEquals(20, CoinEconomy.minBalanceToStake())
    }

    @Test
    fun afterMinimumLockTheWalletCannotRestake() {
        val remaining = CoinEconomy.FIRST_OATH_GRANT - CoinEconomy.MIN_STAKE
        assertEquals(10, remaining)
        assertFalse(CoinEconomy.canStake(remaining))
    }

    @Test
    fun premiumPlantsDoubleXp() {
        assertEquals(50, CoinEconomy.plantXp(50, isPremium = false))
        assertEquals(100, CoinEconomy.plantXp(50, isPremium = true))
        assertEquals(500, CoinEconomy.plantXp(600, isPremium = false))
        assertEquals(1200, CoinEconomy.plantXp(600, isPremium = true))
    }

    @Test
    fun suggestPackPicksTheSmallestCoveringOffer() {
        assertEquals("verdly_coins_100", CoinEconomy.suggestPackFor(40)?.productId)
        assertEquals("verdly_coins_100", CoinEconomy.suggestPackFor(100)?.productId)
        assertEquals("verdly_coins_500", CoinEconomy.suggestPackFor(101)?.productId)
        assertEquals("verdly_coins_500", CoinEconomy.suggestPackFor(400)?.productId)
        assertEquals("verdly_coins_1200", CoinEconomy.suggestPackFor(501)?.productId)
        assertEquals("verdly_coins_3000", CoinEconomy.suggestPackFor(2000)?.productId)
        assertEquals("verdly_coins_3000", CoinEconomy.suggestPackFor(99999)?.productId)
    }
}
