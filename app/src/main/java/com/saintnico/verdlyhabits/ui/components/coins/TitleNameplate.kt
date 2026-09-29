package com.saintnico.verdlyhabits.ui.components.coins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.CoinCosmetics

@Composable
fun TitleNameplate(
    label: String,
    titleColor: Color,
    glowId: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val glow = CoinCosmetics.find(glowId)
    val glowColor = glow?.let { Color(it.previewArgb) } ?: titleColor
    val pulse = pulseAlpha(glowId)
    val loud = glowIsLoud(glowId)
    Row(
        modifier = modifier
            .groveTitleAura(glowId, glowColor)
            .clip(RoundedCornerShape(999.dp))
            .background(glowColor.copy(alpha = if (glow != null) 0.38f else 0.22f))
            .border(
                width = titleBorderWidth(glowId),
                brush = if (glow != null) {
                    groveRingBrush(glowId, glowColor)
                } else {
                    androidx.compose.ui.graphics.SolidColor(glowColor.copy(alpha = pulse))
                },
                shape = RoundedCornerShape(999.dp),
            )
            .then(
                if (loud) Modifier.border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.35f * pulse),
                    shape = RoundedCornerShape(999.dp),
                ) else Modifier,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = if (loud) 16.dp else 12.dp,
                vertical = if (loud) 7.dp else 5.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.WorkspacePremium, null, tint = glowColor, modifier = Modifier.size(if (loud) 16.dp else 14.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            label,
            color = Color.White,
            fontSize = if (loud) 12.sp else 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun ProclaimedChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.PushPin, null, tint = accent, modifier = Modifier.size(12.dp))
        Spacer(Modifier.size(5.dp))
        Text(
            "Proclaiming · $label",
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
