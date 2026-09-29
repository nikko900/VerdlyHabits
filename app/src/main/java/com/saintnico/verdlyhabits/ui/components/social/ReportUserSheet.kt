package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PersonSearch
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.UserReportReason
import com.saintnico.verdlyhabits.data.model.UserReportReasons
import com.saintnico.verdlyhabits.data.remote.firestore.UserReportRepository
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.util.UserFacingErrors
import kotlinx.coroutines.launch
import com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator

private enum class ReportStep { WHY, DETAILS, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserSheet(
    reportedUid: String,
    reportedUsername: String,
    reportedDisplayName: String = "",
    source: String = "unknown",
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit = {},
    onBlockUser: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    val repository = remember { UserReportRepository() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var step by remember { mutableStateOf(ReportStep.WHY) }
    var selectedReason by remember { mutableStateOf<UserReportReason?>(null) }
    var details by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val handle = reportedUsername.ifBlank { "this rival" }.let { if (it.startsWith("@")) it else "@$it" }
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val scroll = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = { if (!submitting) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(onSurface.copy(alpha = 0.22f)),
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step == ReportStep.DETAILS) {
                    TextButton(
                        onClick = { step = ReportStep.WHY },
                        enabled = !submitting,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Back", fontFamily = dmSansFamily)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (step != ReportStep.DONE) {
                    TextButton(onClick = onDismiss, enabled = !submitting) {
                        Text("Cancel", fontFamily = dmSansFamily, color = onSurface.copy(0.55f))
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(scroll),
            ) {
                when (step) {
                    ReportStep.WHY -> ReportWhyStep(
                        handle = handle,
                        selectedReason = selectedReason,
                        onSelect = { selectedReason = it },
                        onContinue = {
                            if (selectedReason != null) {
                                error = null
                                step = ReportStep.DETAILS
                            }
                        },
                    )
                    ReportStep.DETAILS -> ReportDetailsStep(
                        handle = handle,
                        reason = selectedReason,
                        details = details,
                        onDetailsChange = { details = it },
                        error = error,
                        submitting = submitting,
                        onSubmit = {
                            val reason = selectedReason ?: return@ReportDetailsStep
                            submitting = true
                            error = null
                            scope.launch {
                                val result = runCatching {
                                    repository.submitUserReport(
                                        reportedUid = reportedUid,
                                        reportedUsername = reportedUsername,
                                        reportedDisplayName = reportedDisplayName,
                                        reasonCode = reason.code,
                                        reasonLabel = reason.title,
                                        details = details.takeIf { it.isNotBlank() },
                                        source = source,
                                    ).getOrThrow()
                                }
                                submitting = false
                                result.fold(
                                    onSuccess = {
                                        step = ReportStep.DONE
                                        onSubmitted()
                                    },
                                    onFailure = { e ->
                                        error = UserFacingErrors.message(e)
                                    },
                                )
                            }
                        },
                    )
                    ReportStep.DONE -> ReportDoneStep(
                        handle = handle,
                        onClose = onDismiss,
                        onBlock = onBlockUser?.let { block ->
                            {
                                block()
                                onDismiss()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportWhyStep(
    handle: String,
    selectedReason: UserReportReason?,
    onSelect: (UserReportReason) -> Unit,
    onContinue: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Flag, null, tint = accent, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Why are you reporting $handle?",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Your report is confidential. Every submission is reviewed by our team.",
            fontFamily = dmSansFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
        )
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            UserReportReasons.all.forEach { reason ->
                ReportReasonCard(
                    reason = reason,
                    selected = selectedReason?.code == reason.code,
                    onClick = { onSelect(reason) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onContinue,
            enabled = selectedReason != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            Text("Continue", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ReportDetailsStep(
    handle: String,
    reason: UserReportReason?,
    details: String,
    onDetailsChange: (String) -> Unit,
    error: String?,
    submitting: Boolean,
    onSubmit: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Add context",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Reporting $handle for ${reason?.title?.lowercase() ?: "this issue"}.",
            fontFamily = dmSansFamily,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
        )
        Spacer(Modifier.height(16.dp))
        if (reason != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(reasonIcon(reason.code), null, tint = accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            reason.title,
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Text(
                            reason.description,
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.55f),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = details,
            onValueChange = onDetailsChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !submitting,
            label = { Text("Additional details (optional)", fontFamily = dmSansFamily) },
            placeholder = {
                Text(
                    "What happened? Include challenge names or dates if relevant.",
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                )
            },
            minLines = 4,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                cursorColor = accent,
            ),
        )
        AnimatedVisibility(visible = error != null) {
            Text(
                error.orEmpty(),
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            enabled = !submitting && reason != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            if (submitting) {
                VerdlyLoadingIndicator(size = 22.dp)
            } else {
                Text("Submit report", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "False reports may lead to action on your account.",
            fontFamily = dmSansFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
        )
    }
}

@Composable
private fun ReportDoneStep(
    handle: String,
    onClose: () -> Unit,
    onBlock: (() -> Unit)? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.ThumbUp,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Thanks. We've got it",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Your report about $handle was received. Our team will investigate and take action if it breaks Verdly’s community rules.",
            fontFamily = dmSansFamily,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = accent.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "What happens next",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "• We review reports confidentially\n• Status lives under Privacy → Your reports: Received → In review → Closed\n• When we close it, you’ll see what we found (warning, restriction, or no action)\n• You can also block them so they disappear from your social feed",
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        if (onBlock != null) {
            OutlinedButton(
                onClick = onBlock,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(
                    "Block $handle",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            Icon(Icons.Rounded.ThumbUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Got it", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ReportReasonCard(
    reason: UserReportReason,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val borderColor by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200),
        label = "reason_border",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(200),
        label = "reason_icon_scale",
    )
    val bg = if (selected) accent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.35f)
    val stroke = lerp(
        MaterialTheme.colorScheme.outlineVariant.copy(0.45f),
        accent,
        borderColor,
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.dp, stroke),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(
                        if (selected) accent.copy(0.18f)
                        else MaterialTheme.colorScheme.onSurface.copy(0.06f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    reasonIcon(reason.code),
                    contentDescription = null,
                    tint = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(0.55f),
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    reason.title,
                    fontFamily = dmSansFamily,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    reason.description,
                    fontFamily = dmSansFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                )
            }
            AnimatedVisibility(visible = selected, enter = scaleIn() + fadeIn()) {
                Icon(Icons.Rounded.CheckCircle, null, tint = accent, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun reasonIcon(code: String): ImageVector = when (code) {
    "harassment" -> Icons.Rounded.WarningAmber
    "spam_fake" -> Icons.Rounded.PersonSearch
    "inappropriate_profile" -> Icons.Rounded.Image
    "challenge_cheating" -> Icons.Rounded.EmojiEvents
    "impersonation" -> Icons.Rounded.Badge
    "unwanted_contact" -> Icons.Rounded.ChatBubbleOutline
    else -> Icons.Rounded.MoreHoriz
}
