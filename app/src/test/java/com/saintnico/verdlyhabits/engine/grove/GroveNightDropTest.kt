package com.saintnico.verdlyhabits.engine.grove

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class GroveNightDropTest {

    @Test
    fun liveFridayThroughSunday() {
        assertTrue(GroveNightDrop.isLive(LocalDate.of(2026, 9, 11)))
        assertTrue(GroveNightDrop.isLive(LocalDate.of(2026, 9, 12)))
        assertTrue(GroveNightDrop.isLive(LocalDate.of(2026, 9, 13)))
        assertFalse(GroveNightDrop.isLive(LocalDate.of(2026, 9, 9)))
        assertFalse(GroveNightDrop.isLive(LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun shopHintAndItemFollowTheWindow() {
        assertNotNull(GroveNightDrop.liveItem(LocalDate.of(2026, 9, 12)))
        assertNotNull(GroveNightDrop.shopHint(LocalDate.of(2026, 9, 12)))
        assertNull(GroveNightDrop.liveItem(LocalDate.of(2026, 9, 9)))
        assertNull(GroveNightDrop.shopHint(LocalDate.of(2026, 9, 9)))
    }
}
