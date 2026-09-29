package com.saintnico.verdlyhabits.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LastActiveTest {
    private val now = 1_000_000_000L

    @Test
    fun onlineWithinFiveMinutes() {
        val snap = LastActive.resolve(
            viewerShares = true,
            targetShares = true,
            lastActiveAt = now - 60_000L,
            nowMillis = now,
        )
        assertTrue(snap!!.online)
        assertEquals("Active now", snap.profileLabel)
        assertNull(snap.compactLabel)
    }

    @Test
    fun compactHoursWhenAway() {
        val snap = LastActive.resolve(
            viewerShares = true,
            targetShares = true,
            lastActiveAt = now - 2 * 60 * 60 * 1000L,
            nowMillis = now,
        )
        assertFalse(snap!!.online)
        assertEquals("2h", snap.compactLabel)
        assertEquals("2 hours ago", snap.profileLabel)
    }

    @Test
    fun hiddenWhenViewerTurnsSharingOff() {
        assertNull(
            LastActive.resolve(
                viewerShares = false,
                targetShares = true,
                lastActiveAt = now - 1_000L,
                nowMillis = now,
            ),
        )
    }

    @Test
    fun hiddenWhenTargetTurnsSharingOff() {
        assertNull(
            LastActive.resolve(
                viewerShares = true,
                targetShares = false,
                lastActiveAt = now - 1_000L,
                nowMillis = now,
            ),
        )
    }

    @Test
    fun hiddenOnSelf() {
        assertNull(
            LastActive.resolve(
                viewerShares = true,
                targetShares = true,
                lastActiveAt = now,
                nowMillis = now,
                isSelf = true,
            ),
        )
    }
}
