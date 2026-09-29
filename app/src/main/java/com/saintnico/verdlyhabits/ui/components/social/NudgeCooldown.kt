package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.remote.firestore.NudgeRepository
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily

/**
 * Cooldown set for the currently visible people. Loaded once per composition of [targetUids],
 * then updated locally on send. Does not tick live across the 3h window.
 */
class NudgeCooldownState {
    var cooling by mutableStateOf(setOf<String>())
        private set

    fun isCooling(uid: String): Boolean = uid in cooling

    fun markSent(uid: String) {
        if (uid.isBlank()) return
        cooling = cooling + uid
    }

    suspend fun load(targetUids: Collection<String>, repository: NudgeRepository = NudgeRepository()) {
        val remaining = repository.cooldownsRemaining(targetUids)
        cooling = remaining.filter { it.value > 0L }.keys
    }
}

@Composable
fun rememberNudgeCooldownState(targetUids: Collection<String>): NudgeCooldownState {
    val state = remember { NudgeCooldownState() }
    val key = remember(targetUids) {
        targetUids.filter { it.isNotBlank() }.distinct().sorted().joinToString()
    }
    LaunchedEffect(key) {
        state.load(targetUids)
    }
    return state
}

@Composable
fun NudgedLabel(modifier: Modifier = Modifier) {
    Text(
        "Nudged",
        modifier = modifier,
        fontFamily = dmSansFamily,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    )
}
