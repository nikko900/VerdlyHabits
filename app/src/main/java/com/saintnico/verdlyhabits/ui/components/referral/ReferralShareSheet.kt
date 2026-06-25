package com.saintnico.verdlyhabits.ui.components.referral

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.referral.ReferralQrEncoder
import com.saintnico.verdlyhabits.referral.ReferralShareHelper
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralUiState
import kotlinx.coroutines.delay

@Composable
fun ReferralShareSheet(
    visible: Boolean,
    state: ReferralUiState,
    onDismiss: () -> Unit,
    onCopiedAck: () -> Unit = {},
    onShareOpened: () -> Unit = {},
    onRegisterVanityCode: ((String, (Boolean, String) -> Unit) -> Unit)? = null,
) {
    if (!visible) return

    val context = LocalContext.current

    var showContent by remember { mutableStateOf(false) }
    var copiedFlash by remember { mutableStateOf(false) }
    var vanityInput by remember { mutableStateOf("") }
    var vanityMessage by remember { mutableStateOf<String?>(null) }
    val accent = Color(0xFF52B788)
    val qrPayload = state.shareLink.ifBlank { state.deepLink }
    val qrBitmap = remember(qrPayload) {
        if (qrPayload.isBlank()) null
        else runCatching { ReferralQrEncoder.encode(qrPayload, 480) }.getOrNull()
    }

    LaunchedEffect(Unit) {
        delay(80)
        showContent = true
    }

    val copyScale by animateFloatAsState(
        targetValue = if (copiedFlash) 1.05f else 1f,
        animationSpec = tween(180),
        label = "copy_scale",
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 28.dp, top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.94f, animationSpec = tween(400)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Grow together",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "Friends install with your code — you unlock ${state.rewardDays} days of Pro.",
                        fontFamily = dmSansFamily,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
                    )

                    ReferralProgressDots(
                        qualifiedCount = state.qualifiedCount,
                        friendsRequired = state.friendsRequired,
                        activeColor = accent,
                        inactiveColor = MaterialTheme.colorScheme.outlineVariant.copy(0.5f),
                    )

                    Spacer(Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        accent.copy(0.12f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(0.35f),
                                    ),
                                ),
                            )
                            .border(
                                1.dp,
                                accent.copy(0.35f),
                                RoundedCornerShape(22.dp),
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap,
                                    contentDescription = "Referral QR code",
                                    modifier = Modifier
                                        .size(168.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White)
                                        .padding(8.dp),
                                )
                                Spacer(Modifier.height(14.dp))
                            }
                            Text(
                                state.fullCode,
                                fontFamily = frauncesFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.scale(copyScale),
                            )
                            Text(
                                "verdly.app/r/${state.shortCode}",
                                fontFamily = dmSansFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    ReferralHowItWorksCard(
                        friendsRequired = state.friendsRequired,
                        rewardDays = state.rewardDays,
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                ReferralShareHelper.copyInvite(context, state)
                                copiedFlash = true
                                onCopiedAck()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copy invite", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                if (ReferralShareHelper.shareInvite(context, state)) {
                                    onShareOpened()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                        ) {
                            Icon(Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Share",
                                fontFamily = dmSansFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D1510),
                            )
                        }
                    }

                    if (onRegisterVanityCode != null) {
                        Spacer(Modifier.height(16.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(0.35f),
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(
                                    "Creator / campus link",
                                    fontFamily = frauncesFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    "Custom verdly.app/r/YOURCODE for campaigns",
                                    fontFamily = dmSansFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = vanityInput,
                                    onValueChange = { vanityInput = it.filter { c -> c.isLetterOrDigit() }.take(12) },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("e.g. KRU", fontFamily = dmSansFamily) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accent,
                                        cursorColor = accent,
                                    ),
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        onRegisterVanityCode(vanityInput) { ok, msg ->
                                            vanityMessage = if (ok) "Live at verdly.app/r/$msg" else msg
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = vanityInput.length >= 3,
                                ) {
                                    Text("Set custom link", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                                }
                                vanityMessage?.let {
                                    Text(
                                        it,
                                        fontFamily = dmSansFamily,
                                        fontSize = 11.sp,
                                        color = accent,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Maybe later", fontFamily = dmSansFamily)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ReferralHowItWorksCard(
    friendsRequired: Int,
    rewardDays: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(0.35f),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "How it works",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            ReferralStepRow(1, "Share your code or QR with friends.")
            ReferralStepRow(2, "They install Verdly and sign up with your link.")
            ReferralStepRow(3, "When $friendsRequired join, you get $rewardDays days of Pro.")
            ReferralStepRow(4, "Friends who stay 7 days earn you +3 bonus days each.")
        }
    }
}

@Composable
private fun ReferralStepRow(step: Int, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$step",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            body,
            fontFamily = dmSansFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
            lineHeight = 18.sp,
        )
    }
}
