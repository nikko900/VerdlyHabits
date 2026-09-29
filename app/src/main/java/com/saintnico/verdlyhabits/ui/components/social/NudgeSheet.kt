package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saintnico.verdlyhabits.data.model.NudgeMessages
import com.saintnico.verdlyhabits.data.model.NudgeSituation
import com.saintnico.verdlyhabits.data.model.NudgeSurface
import com.saintnico.verdlyhabits.data.remote.firestore.NudgeOutcome
import com.saintnico.verdlyhabits.data.remote.firestore.NudgeRepository
import com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog
import com.saintnico.verdlyhabits.ui.components.VerdlyActionLottie
import com.saintnico.verdlyhabits.ui.components.VerdlySendingIndicator
import com.saintnico.verdlyhabits.ui.theme.aboveSystemNav
import com.saintnico.verdlyhabits.ui.theme.cappedSp
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
 * a preset message, optionally add a short note, and sends it.
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
    var showSentConfirm by remember(target.uid) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(target.uid) {
        val remaining = repository.cooldownRemaining(target.uid)
        if (remaining > 0L) {
            onDismiss()
            return@LaunchedEffect
        }
        cooldownMillis = 0L
    }

    if (cooldownMillis < 0L) return

    if (showSentConfirm) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    com.saintnico.verdlyhabits.ui.components.VerdlyHandshakeBeat(
                        size = 140.dp,
                        play = true,
                        onFinished = {
                            onSent?.invoke()
                            onDismiss()
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Nudge sent",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = cappedSp(18f),
                    )
                }
            }
        }
        return
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
                    is NudgeOutcome.Sent -> showSentConfirm = true
                    is NudgeOutcome.OnCooldown -> onDismiss()
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
    var note by remember(situation) { mutableStateOf("") }
    val onCooldown = cooldownRemainingMillis > 0
    val composed = remember(selected, note, presets) {
        val preset = presets.firstOrNull { it.id == selected }?.message.orEmpty()
        NudgeMessages.compose(preset, note)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .aboveSystemNav()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(NudgeAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    VerdlyActionLottie(
                        rawRes = VerdlyAssetCatalog.NudgeLottie,
                        size = 32.dp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Nudge $targetName",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = cappedSp(18f),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Pick a line. Add a short note if you want.",
                        fontFamily = dmSansFamily,
                        fontSize = cappedSp(12f),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
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
                                "Already nudged $targetName recently. Try again in ${formatCooldown(cooldownRemainingMillis)}.",
                                fontFamily = dmSansFamily,
                                fontSize = cappedSp(12f),
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
                                    fontSize = cappedSp(13f),
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

                Spacer(Modifier.height(16.dp))
                Text(
                    "Your note (optional)",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = cappedSp(13f),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Keep it short. 80 characters. Overwrites the last nudge to this friend, so it stays tiny in the cloud.",
                    fontFamily = dmSansFamily,
                    fontSize = cappedSp(11f),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NudgeMessages.emojiPalette.forEach { emoji ->
                        Surface(
                            onClick = {
                                if (note.length < NudgeMessages.NOTE_MAX_CHARS) {
                                    note += emoji
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                            enabled = !onCooldown,
                        ) {
                            Text(
                                emoji,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                fontSize = cappedSp(16f),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { incoming ->
                        note = incoming.replace('\n', ' ').take(NudgeMessages.NOTE_MAX_CHARS)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !onCooldown,
                    placeholder = {
                        Text(
                            "e.g. Proud of yesterday. Same time?",
                            fontFamily = dmSansFamily,
                            fontSize = cappedSp(13f),
                        )
                    },
                    supportingText = {
                        Text(
                            "${note.length} / ${NudgeMessages.NOTE_MAX_CHARS}",
                            fontFamily = dmSansFamily,
                            fontSize = cappedSp(11f),
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NudgeAccent,
                        cursorColor = NudgeAccent,
                    ),
                    singleLine = true,
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (composed.isNotBlank()) onSend(composed) },
                enabled = !onCooldown && !isSending && !isCheckingCooldown && selected != null && composed.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NudgeAccent, disabledContainerColor = NudgeAccent.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (isSending || isCheckingCooldown) {
                    VerdlySendingIndicator(size = 18.dp)
                } else {
                    Icon(Icons.Rounded.Bolt, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Send nudge", fontFamily = dmSansFamily, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
