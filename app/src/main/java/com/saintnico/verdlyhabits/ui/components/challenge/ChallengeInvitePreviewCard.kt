package com.saintnico.verdlyhabits.ui.components.challenge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.challenge.ChallengeInviteHelper
import com.saintnico.verdlyhabits.referral.ReferralQrEncoder
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.utils.ChallengeInviteCardExporter

@Composable
fun ChallengeInvitePreviewCard(
    challengeName: String,
    inviteCode: String,
    stake: String = "",
    daysRemaining: Long? = null,
    memberCount: Int? = null,
    modifier: Modifier = Modifier,
) {
    val code = ChallengeInviteHelper.normalizedCode(inviteCode)
    val theme = remember(code) { ChallengeInviteCardExporter.themeForInvite(code) }
    val link = remember(code) { ChallengeInviteHelper.shareLinkLabel(code) }
    val qrBitmap = remember(code) {
        runCatching { ReferralQrEncoder.encode(ChallengeInviteHelper.shareLink(code), 360) }.getOrNull()
    }
    val gradient = Brush.verticalGradient(listOf(theme.heroTop, theme.heroMid, theme.heroBottom))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(gradient),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.glow.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(size.width * 0.88f, size.height * 0.1f),
                    radius = size.minDimension * 0.5f,
                ),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "JOIN MY CHALLENGE",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    color = theme.highlight.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    challengeName,
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = theme.ink,
                    maxLines = 2,
                )
                if (stake.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Stake: $stake",
                        fontFamily = dmSansFamily,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                daysRemaining?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$it days left",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
                memberCount?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$it members in",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    code,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 4.sp,
                    color = theme.glow,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    link,
                    fontFamily = dmSansFamily,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f),
                )
            }
            if (qrBitmap != null) {
                Spacer(Modifier.size(12.dp))
                Image(
                    bitmap = qrBitmap,
                    contentDescription = "Challenge invite QR",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .padding(6.dp),
                )
            }
        }
    }
}
