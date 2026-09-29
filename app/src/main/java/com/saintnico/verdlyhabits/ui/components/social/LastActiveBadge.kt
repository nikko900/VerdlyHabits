package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.LastActive
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily

private val ActiveGreen = Color(0xFF2BD67B)
private val AwayGray = Color(0xFFB8C0CC)

/**
 * Presence indicator on an avatar.
 * Online → green dot only.
 * Away → green/grey dot + bare relative time (no pill / rectangle).
 */
@Composable
fun BoxScope.LastActiveBadge(
    snapshot: LastActive.Snapshot?,
    compact: Boolean = true,
    dotSize: Dp = 8.dp,
) {
    if (snapshot == null) return
    val fill = if (snapshot.online) ActiveGreen else AwayGray
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .offset(x = 4.dp, y = (-2).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (compact && !snapshot.online && !snapshot.compactLabel.isNullOrBlank()) {
            Text(
                snapshot.compactLabel,
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = dmSansFamily,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(fill)
                .border(1.5.dp, Color.White, CircleShape),
        )
    }
}

/** Standalone timeline line under a name — no chip, no box. */
@Composable
fun LastActiveTimeline(
    snapshot: LastActive.Snapshot?,
    modifier: Modifier = Modifier,
    lightOnDark: Boolean = true,
) {
    if (snapshot == null) return
    val color = when {
        snapshot.online && lightOnDark -> ActiveGreen
        snapshot.online -> Color(0xFF1B8A4A)
        lightOnDark -> Color.White.copy(alpha = 0.78f)
        else -> Color(0xFF5C6670)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (snapshot.online) ActiveGreen else color.copy(alpha = 0.85f)),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            snapshot.profileLabel,
            fontFamily = dmSansFamily,
            fontWeight = if (snapshot.online) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 13.sp,
            letterSpacing = 0.2.sp,
            color = color,
        )
    }
}

@Composable
fun LastActiveAvatarWrap(
    snapshot: LastActive.Snapshot?,
    compact: Boolean = true,
    dotSize: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Box {
        content()
        LastActiveBadge(snapshot = snapshot, compact = compact, dotSize = dotSize)
    }
}
