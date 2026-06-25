package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.data.model.UserReportReason
import com.saintnico.verdlyhabits.data.model.UserReportReasons
import com.saintnico.verdlyhabits.data.remote.firestore.UserReportRepository
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.util.UserFacingErrors
import kotlinx.coroutines.launch

private enum class ReportStep { WHY, DETAILS, DONE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserSheet(
    reportedUid: String,
    reportedUsername: String,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val repository = remember { UserReportRepository() }
    var step by remember { mutableStateOf(ReportStep.WHY) }
    var selectedReason by remember { mutableStateOf<UserReportReason?>(null) }
    var details by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val handle = reportedUsername.ifBlank { "this rival" }.let { if (it.startsWith("@")) it else "@$it" }
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
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
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (step != ReportStep.DONE) {
                    TextButton(onClick = onDismiss, enabled = !submitting) {
                        Text("Cancel", fontFamily = dmSansFamily, color = onSurface.copy(0.55f))
                    }
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                },
                label = "report_step",
            ) { current ->
                when (current) {
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
                                val result = repository.submitUserReport(
                                    reportedUid = reportedUid,
                                    reportedUsername = reportedUsername,
                                    reasonCode = reason.code,
                                    reasonLabel = reason.title,
                                    details = details.takeIf { it.isNotBlank() },
                                )
                                submitting = false
                                if (result.isSuccess) {
                                    step = ReportStep.DONE
                                    onSubmitted()
                                } else {
                                    error = UserFacingErrors.message(result.exceptionOrNull())
                                }
                            }
                        },
                    )
                    ReportStep.DONE -> ReportDoneStep(
                        onClose = onDismiss,
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
    Column {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            ReportPhosphorIcon(R.drawable.ic_phosphor_flag, accent, 24.dp)
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
            "Your report is confidential. We review every submission to keep Verdly safe.",
            fontFamily = dmSansFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
        )
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
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
        Spacer(Modifier.height(18.dp))
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
    Column {
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
                    ReportPhosphorIcon(reason.iconRes, accent, 20.dp)
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
            minLines = 3,
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
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onSubmit,
            enabled = !submitting && reason != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            if (submitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
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
private fun ReportDoneStep(onClose: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(tween(320)) + fadeIn(tween(280)),
        ) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                ReportPhosphorIcon(R.drawable.ic_phosphor_check, accent, 32.dp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Report received",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Thank you for helping keep the community safe. Our team will review this report.",
            fontFamily = dmSansFamily,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
        ) {
            Text("Done", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
                ReportPhosphorIcon(
                    reason.iconRes,
                    if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(0.55f),
                    20.dp,
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
                ReportPhosphorIcon(R.drawable.ic_phosphor_check, accent, 18.dp)
            }
        }
    }
}

@Composable
private fun ReportPhosphorIcon(
    drawableId: Int,
    tint: Color,
    size: androidx.compose.ui.unit.Dp,
) {
    Icon(
        painter = painterResource(drawableId),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(size),
    )
}
