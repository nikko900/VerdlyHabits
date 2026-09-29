package com.saintnico.verdlyhabits.ui.components.care

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

enum class CareCommitmentKind {
    PAUSE_HABIT,
    ARCHIVE_HABIT,
    END_HABIT,
    END_GOAL,
}

data class CareCommitmentSpec(
    val kind: CareCommitmentKind,
    val titleName: String,
)

private data class CareCopy(
    val headline: String,
    val body: String,
    val reminder: String,
    val chips: List<String>,
    val confirmLabel: String,
    val destructive: Boolean,
)

private fun copyFor(spec: CareCommitmentSpec): CareCopy = when (spec.kind) {
    CareCommitmentKind.PAUSE_HABIT -> CareCopy(
        headline = "Taking a breath from ${spec.titleName}?",
        body = "Pausing is allowed. Reminders stop. Your streak history stays. Resume whenever you're ready — it lives under Resting on Home.",
        reminder = "Next time life gets loud, pause again instead of disappearing. We'll hold the root.",
        chips = listOf("Need rest", "Traveling", "Too much right now", "Timing's off", "Just for a bit"),
        confirmLabel = "Pause and keep it safe",
        destructive = false,
    )
    CareCommitmentKind.ARCHIVE_HABIT -> CareCopy(
        headline = "Put ${spec.titleName} on the shelf?",
        body = "It leaves Today, but it isn't gone. Open On the shelf on Home and restore it in one tap. Your days still count in analytics.",
        reminder = "Shelving is not failure. It's making room. Come back when it fits again.",
        chips = listOf("Not this season", "Replaced by another habit", "Need a simpler grove", "Outgrew it"),
        confirmLabel = "Shelf it",
        destructive = false,
    )
    CareCommitmentKind.END_HABIT -> CareCopy(
        headline = "Let ${spec.titleName} rest for good?",
        body = "We won't erase it. It moves to On the shelf so you can restore it later. History stays yours.",
        reminder = "If you change your mind tomorrow, it's still here. You don't have to start from zero.",
        chips = listOf("It no longer fits", "I was forcing it", "Want a clean grove", "Something kinder replaced it"),
        confirmLabel = "Move to the shelf",
        destructive = true,
    )
    CareCommitmentKind.END_GOAL -> CareCopy(
        headline = "Step back from ${spec.titleName}?",
        body = "No judgment. Goals shift. This one goes to your goal shelf. You can restore it whenever the why comes back.",
        reminder = "Write a line for future you, or skip. Either way, we still want the best for you.",
        chips = listOf("Life changed", "Wrong season", "Need a smaller goal", "Proud, and done"),
        confirmLabel = "Shelf this goal",
        destructive = true,
    )
}

/**
 * Premium care moment before pause / shelf / end. Reason is optional. Skip is always available.
 */
@Composable
fun CareCommitmentSheet(
    spec: CareCommitmentSpec,
    onConfirm: (note: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val copy = remember(spec) { copyFor(spec) }
    var selectedChip by remember { mutableStateOf<String?>(null) }
    var extra by remember { mutableStateOf("") }
    val accent = if (copy.destructive) Color(0xFFE07A5F) else Color(0xFF52B788)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.12f), MaterialTheme.colorScheme.surface),
                        ),
                    )
                    .padding(22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.FavoriteBorder, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        copy.headline,
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    copy.body,
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    copy.chips.chunked(3).forEach { rowChips ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowChips.forEach { chip ->
                                val on = selectedChip == chip
                                Surface(
                                    onClick = { selectedChip = if (on) null else chip },
                                    shape = RoundedCornerShape(50),
                                    color = if (on) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (on) accent.copy(alpha = 0.45f) else Color.Transparent,
                                    ),
                                ) {
                                    Text(
                                        chip,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        fontFamily = dmSansFamily,
                                        fontSize = 12.sp,
                                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = extra,
                    onValueChange = { extra = it.take(160) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Optional. A note for future you.") },
                    minLines = 2,
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.08f))
                        .padding(12.dp),
                ) {
                    Text(
                        copy.reminder,
                        fontFamily = dmSansFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    )
                }
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        val note = listOfNotNull(selectedChip, extra.trim().ifBlank { null })
                            .joinToString(" · ")
                            .ifBlank { null }
                        onConfirm(note)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (copy.destructive) Color(0xFFC75D4A) else accent,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(copy.confirmLabel, fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = { onConfirm(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Skip — still continue", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Keep going", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
