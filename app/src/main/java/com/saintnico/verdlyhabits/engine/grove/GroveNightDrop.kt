package com.saintnico.verdlyhabits.engine.grove

import com.saintnico.verdlyhabits.data.model.CoinCosmetics
import com.saintnico.verdlyhabits.data.model.CosmeticItem
import java.time.DayOfWeek
import java.time.LocalDate

/** Weekend-only rare Grove item. Owned items stay after the window closes. */
object GroveNightDrop {
    const val ITEM_ID = "glow_moonveil"

    fun isLive(today: LocalDate = LocalDate.now()): Boolean =
        today.dayOfWeek in setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    fun liveItem(today: LocalDate = LocalDate.now()): CosmeticItem? =
        if (isLive(today)) CoinCosmetics.find(ITEM_ID) else null

    fun shopHint(today: LocalDate = LocalDate.now()): String? =
        if (isLive(today)) "In the grove tonight. Gone Monday." else null
}
