package com.saintnico.verdlyhabits.ui.components.referral

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import kotlinx.coroutines.delay

private val Mint = Color(0xFF52B788)
private val DeepGreen = Color(0xFF1B4332)
private val Gold = Color(0xFFFFE8A3)

@Composable
fun ReferralAnnualOfferSheet(
    visible: Boolean,
    daysLeft: Int,
    onClaimAnnual: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    var showContent by remember { mutableStateOf(false) }
    val infinite = rememberInfiniteTransition(label = "offer_glow")
    val glow by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "glow",
    )

    LaunchedEffect(Unit) {
        delay(60)
        showContent = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF0F1A14),
        ) {
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.92f, animationSpec = tween(400)),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(DeepGreen, Mint.copy(glow)),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = Gold, modifier = Modifier.size(32.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Loved Pro?",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = Color.White,
                    )
                    Text(
                        "Lock in annual at ${ReferralManager.ANNUAL_REFERRAL_DISCOUNT_PERCENT}% off — this week only.",
                        fontFamily = dmSansFamily,
                        fontSize = 14.sp,
                        color = Color.White.copy(0.72f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Spacer(Modifier.height(20.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(DeepGreen.copy(0.55f))
                            .border(1.dp, Mint.copy(0.4f), RoundedCornerShape(18.dp))
                            .padding(18.dp),
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Verified, null, tint = Mint, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Referral reward pricing",
                                    fontFamily = dmSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Mint,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        "Annual Pro",
                                        fontFamily = frauncesFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White,
                                    )
                                    Text(
                                        "Unlimited habits · sounds · shields",
                                        fontFamily = dmSansFamily,
                                        fontSize = 11.sp,
                                        color = Color.White.copy(0.55f),
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        ReferralManager.discountedAnnualPriceKes(),
                                        fontFamily = dmSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = Gold,
                                    )
                                    Text(
                                        "Ksh 2,999",
                                        fontFamily = dmSansFamily,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(0.45f),
                                        textDecoration = TextDecoration.LineThrough,
                                    )
                                }
                            }
                        }
                    }

                    if (daysLeft > 0) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Offer ends in $daysLeft day${if (daysLeft == 1) "" else "s"}",
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error.copy(0.85f),
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    Button(
                        onClick = onClaimAnnual,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Mint,
                            contentColor = Color(0xFF0D1510),
                        ),
                    ) {
                        Text(
                            "Claim annual offer",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text("Maybe later", fontFamily = dmSansFamily, color = Color.White.copy(0.55f))
                    }
                }
            }
        }
    }
}
