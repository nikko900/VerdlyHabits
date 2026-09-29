package com.saintnico.verdlyhabits.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.data.streak.StreakRepository
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.launch

/**
 * Habit-streak repair offer — only on Arena/Duo, never on home open.
 *
 * Trigger is [com.saintnico.verdlyhabits.data.streak.StreakSnapshot.activeRepairOffer] only.
 * Duo weekly grace on [com.saintnico.verdlyhabits.ui.components.social.DuoStreakCard] is a
 * different eligibility check and stays an in-card action, not a second sheet.
 *
 * "Ask me later" snoozes until the next local day via [AppDataStore.snoozeRepairPromptUntilTomorrow]
 * (same LocalDate.now(zone).plusDays(1).atStartOfDay used by streak day-boundary).
 */
@Composable
fun StreakRepairPromptHost(
    visible: Boolean,
    habits: List<HabitItem>,
    onUseShield: (habitId: String, missedDate: java.time.LocalDate, onResult: (Boolean) -> Unit) -> Unit,
    onRequestPaywall: () -> Unit,
) {
    if (!visible || habits.isEmpty()) return

    val context = LocalContext.current
    val store = remember { AppDataStore(context) }
    val streakRepo = remember { StreakRepository.get(context.applicationContext) }
    val snap by streakRepo.snapshot.collectAsState()
    val snoozeUntil by store.repairPromptSnoozeUntil.collectAsState(initial = 0L)
    val dismissed by store.dismissedRepairTokens.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()

    if (System.currentTimeMillis() < snoozeUntil) return

    val offer = snap.activeRepairOffer(dismissed) ?: return
    val title = habits.firstOrNull { it.id == offer.habitId }?.title ?: "Your habit"
    val token = "${offer.habitId}:${offer.missedDate}"

    key(token) {
        StreakShieldDialog(
            offer = offer,
            habitTitle = title,
            onUseShield = {
                scope.launch { store.dismissRepairToken(token) }
                onUseShield(offer.habitId, offer.missedDate) { }
            },
            onGetShield = {
                scope.launch { store.dismissRepairToken(token) }
                onRequestPaywall()
            },
            onAskMeLater = {
                scope.launch { store.snoozeRepairPromptUntilTomorrow() }
            },
        )
    }
}
