package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

private val Mint = Color(0xFF52B788)
private val Shell = Color(0xFF0F1A14)

@Composable
fun DuoStreakCard(
    state: DuoStreakState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infinite = rememberInfiniteTransition(label = "duo_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "pulse",
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Shell, Color(0xFF132A1F))),
                    RoundedCornerShape(22.dp),
                )
                .border(1.dp, Mint.copy(0.28f), RoundedCornerShape(22.dp))
                .padding(16.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = Mint, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Duo streak",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.status == "active") {
                        Text(
                            "${state.streakDays}d",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFFFE8A3),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (state.isIncomingInvite) {
                    Text(
                        "${state.buddyUsername} wants to be your accountability buddy.",
                        fontFamily = dmSansFamily,
                        fontSize = 13.sp,
                        color = Color.White.copy(0.75f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) {
                            Text("Decline", fontFamily = dmSansFamily)
                        }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Mint),
                        ) {
                            Text("Accept", fontFamily = dmSansFamily, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DuoMemberColumn(
                            label = "You",
                            progress = state.myProgress,
                            done = state.myDoneToday,
                            photoUrl = null,
                            pulse = if (!state.myDoneToday && state.buddyDoneToday) pulse else 1f,
                        )
                        Text(
                            "vs",
                            fontFamily = frauncesFamily,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = Color.White.copy(0.35f),
                            fontSize = 14.sp,
                        )
                        DuoMemberColumn(
                            label = state.buddyUsername.ifBlank { "Buddy" },
                            progress = state.buddyProgress,
                            done = state.buddyDoneToday,
                            photoUrl = state.buddyPhotoUrl,
                            pulse = 1f,
                        )
                    }
                    if (state.status == "pending") {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Waiting for ${state.buddyUsername} to accept…",
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = Color(0xFF95D5B2),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuoMemberColumn(
    label: String,
    progress: String,
    done: Boolean,
    photoUrl: String?,
    pulse: Float,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size((48 * pulse).dp)
                .clip(CircleShape)
                .background(Mint.copy(0.15f))
                .border(1.dp, if (done) Mint else Mint.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Rounded.Person, null, tint = Mint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
        Text(progress, fontFamily = dmSansFamily, fontSize = 11.sp, color = Color.White.copy(0.55f))
        Icon(
            if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            null,
            tint = if (done) Mint else Color.White.copy(0.35f),
            modifier = Modifier.size(18.dp),
        )
    }
}
