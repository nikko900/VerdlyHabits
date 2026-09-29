package com.saintnico.verdlyhabits.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoinCosmeticsTest {

    @Test
    fun catalogIdsAreUnique() {
        val ids = CoinCosmetics.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun pinIsARepeatableSink() {
        assertTrue(CoinCosmetics.PIN_COST >= CoinEconomy.MIN_STAKE)
        assertEquals(14L, CoinCosmetics.PIN_DAYS)
    }

    @Test
    fun goldleafCostsLessThanDeepRootsPack() {
        val goldleaf = CoinCosmetics.find("glow_goldleaf")!!
        assertTrue(goldleaf.price < 3000)
        assertTrue(goldleaf.price > 100)
    }

    @Test
    fun groveLeadFrameIsEarnedNeverSold() {
        val lead = CoinCosmetics.find("frame_grove_lead")!!
        assertEquals(0, lead.price)
        assertTrue(!lead.listedInShop)
        val shop = CoinCosmetics.shopItems(CosmeticKind.PROFILE_FRAME, nightDropLive = true)
        assertTrue(shop.none { it.id == "frame_grove_lead" })
    }

    @Test
    fun moonveilOnlyAppearsInShopOnNightDrop() {
        val weekday = CoinCosmetics.shopItems(CosmeticKind.TITLE_GLOW, nightDropLive = false)
        val weekend = CoinCosmetics.shopItems(CosmeticKind.TITLE_GLOW, nightDropLive = true)
        assertTrue(weekday.none { it.id == "glow_moonveil" })
        assertTrue(weekend.any { it.id == "glow_moonveil" })
    }
}
