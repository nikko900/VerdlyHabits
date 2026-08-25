package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saintnico.verdlyhabits.data.model.NudgeMessages
import com.saintnico.verdlyhabits.data.model.NudgeSituation
import com.saintnico.verdlyhabits.data.model.NudgeSurface
import com.saintnico.verdlyhabits.data.remote.firestore.NudgeOutcome
import com.saintnico.verdlyhabits.data.remote.firestore.NudgeRepository
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

val NudgeAccent = Color(0xFFFF7A45)

/** Who/what a nudge is being sent to — carries enough context for a smart default message. */
data class NudgeTarget(
    val uid: String,
    val username: String,
    val photoUrl: String? = null,
    val surface: NudgeSurface,
    val situation: NudgeSituation = NudgeSituation.GENERAL,
    val contextId: String? = null,
)

private fun iconFor(key: String): ImageVector = when (key) {
    "flame" -> Icons.Rounded.LocalFireDepartment
    "waiting" -> Icons.Rounded.HourglassBottom
    "bolt" -> Icons.Rounded.Bolt
    "wave" -> Icons.Rounded.WavingHand
    "trophy" -> Icons.Rounded.EmojiEvents
    "star" -> Icons.Rounded.Star
    "heart" -> Icons.Rounded.Favorite
    else -> Icons.Rounded.Bolt
}

private fun formatCooldown(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(1L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

/**
 * Self-contained nudge flow: checks the sender's cooldown against [target], lets them pick
 * a preset message, and sends it. Drop this into any screen and just flip [target] between
 * `null` and a [NudgeTarget] to open/close it.
 */
@Composable
fun NudgeSheetHost(
    target: NudgeTarget?,
    onDismiss: () -> Unit,
    onSent: (() -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    if (target == null) return
    val repository = remember { NudgeRepository() }
    val myUsername by settingsViewModel.userUsername.collectAsState()
    val myPhotoUrl by settingsViewModel.userPhotoUri.collectAsState()
    var cooldownMillis by remember(target.uid) { mutableStateOf(-1L) }
    var isSending by remember(target.uid) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(target.uid) {
        cooldownMillis = repository.cooldownRemaining(target.uid)
    }

    NudgeSheet(
        targetName = target.username.ifBlank { "them" },
        situation = target.situation,
        isSending = isSending,
        isCheckingCooldown = cooldownMillis < 0,
        cooldownRemainingMillis = cooldownMillis.coerceAtLeast(0L),
        onSend = { message ->
            if (isSending) return@NudgeSheet
            isSending = true
            scope.launch {
                val outcome = repository.sendNudge(
                    targetUid = target.uid,
                    message = message,
                    surface = target.surface,
                    fromUsername = myUsername,
                    fromPhotoUrl = myPhotoUrl,
                    contextId = target.contextId,
                )
                isSending = false
                when (outcome) {
                    is NudgeOutcome.Sent -> {
                        onSent?.invoke()
                        onDismiss()
                    }
                    is NudgeOutcome.OnCooldown -> cooldownMillis = outcome.remainingMillis
                    is NudgeOutcome.Failed -> onError?.invoke(outcome.message)
                }
            }
        },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NudgeSheet(
    targetName: String,
    situation: NudgeSituation,
    isSending: Boolean,
    isCheckingCooldown: Boolean,
    cooldownRemainingMillis: Long,
    onSend: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val presets = remember(situation) { NudgeMessages.forSituation(situation) }
    var selected by remember(situation) { mutableStateOf(presets.firstOrNull()?.id) }
    val onCooldown = cooldownRemainingMillis > 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NudgeAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Bolt, null, tint = NudgeAccent, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Nudge $targetName",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Send a quick push to get their attention",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))

            if (onCooldown) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.HourglassBottom,
                            null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Already nudged $targetName recently — try again in ${formatCooldown(cooldownRemainingMillis)}.",
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    val isSelected = preset.id == selected
                    Surface(
                        onClick = { selected = preset.id },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) NudgeAccent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) NudgeAccent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        ),
                        enabled = !onCooldown,
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                iconFor(preset.iconKey),
                                null,
                                tint = if (isSelected) NudgeAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                preset.message,
                                fontFamily = dmSansFamily,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = NudgeAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { presets.firstOrNull { it.id == selected }?.let { onSend(it.message) } },
                enabled = !onCooldown && !isSending && !isCheckingCooldown && selected != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NudgeAccent, disabledContainerColor = NudgeAccent.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (isSending || isCheckingCooldown) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.Bolt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send nudge", fontFamily = dmSansFamily, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
