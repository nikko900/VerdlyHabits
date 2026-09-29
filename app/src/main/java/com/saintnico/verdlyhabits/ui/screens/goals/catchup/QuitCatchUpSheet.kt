package com.saintnico.verdlyhabits.ui.screens.goals.catchup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.SilentGapResolution
import com.saintnico.verdlyhabits.engine.QuitConfirmationEngine
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class CatchUpStep { ASK, SLIP_DATE, UNSURE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuitCatchUpSheet(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onResolve: (SilentGapResolution, LocalDate?) -> Unit,
) {
    val eval = remember(goal.lastConfirmedAt, goal.streakAnchorAt, goal.lastLapseAt) {
        QuitConfirmationEngine.evaluate(goal)
    }
    val (headline, body) = QuitConfirmationEngine.catchUpCopy(goal.title, eval.silentDays)
    var step by remember { mutableStateOf(CatchUpStep.ASK) }
    var showDatePicker by remember { mutableStateOf(false) }
    val firstSilent = remember(goal) {
        QuitConfirmationEngine.firstSilentDay(
            QuitConfirmationEngine.snapshotOf(goal),
            ZoneId.systemDefault(),
        )
    }
    var slipDate by remember { mutableStateOf(firstSilent) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                CatchUpStep.ASK -> {
                    Text(
                        headline,
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        body,
                        fontFamily = dmSansFamily,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onResolve(SilentGapResolution.YES_CLEAR, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text("I stayed clear the whole time", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { step = CatchUpStep.SLIP_DATE },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text("I slipped. Let's log it", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { step = CatchUpStep.UNSURE }) {
                        Text("I'm honestly not sure", fontFamily = dmSansFamily)
                    }
                }
                CatchUpStep.SLIP_DATE -> {
                    Text(
                        "When did it happen?",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                    )
                    Text(
                        "Defaults to the first quiet day. Change it if that's not right.",
                        fontFamily = dmSansFamily,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Date: $slipDate", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onResolve(SilentGapResolution.NO_SLIPPED, slipDate) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Log & restart", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { step = CatchUpStep.ASK }) {
                        Text("Back", fontFamily = dmSansFamily)
                    }
                }
                CatchUpStep.UNSURE -> {
                    Text(
                        "That's okay. Uncertainty is normal.",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "What feels safer?",
                        fontFamily = dmSansFamily,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Button(
                        onClick = { onResolve(SilentGapResolution.UNSURE_KEEP, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Keep my streak going", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { onResolve(SilentGapResolution.UNSURE_RESET, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text("Restart today, just to be safe", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { step = CatchUpStep.ASK }) {
                        Text("Back", fontFamily = dmSansFamily)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = slipDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        slipDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
