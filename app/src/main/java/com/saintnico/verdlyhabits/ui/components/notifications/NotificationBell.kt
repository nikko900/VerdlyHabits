package com.saintnico.verdlyhabits.ui.components.notifications

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NotificationBellButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val pulse by animateFloatAsState(
        targetValue = if (unreadCount > 0) 1.04f else 1f,
        animationSpec = spring(dampingRatio = 0.55f),
        label = "bellPulse",
    )
    IconButton(
        onClick = onClick,
        modifier = modifier.scale(pulse),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .then(
                        if (unreadCount > 0) {
                            Modifier.border(1.5.dp, accent.copy(alpha = 0.45f), CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Notifications,
                    contentDescription = "Notifications",
                    tint = if (unreadCount > 0) accent else tint,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (unreadCount > 0) {
                val label = if (unreadCount > 99) "99+" else unreadCount.toString()
                Box(
                    modifier = Modifier
                        .size(if (unreadCount > 9) 20.dp else 18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE85D4C))
                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = Color.White,
                        fontSize = if (unreadCount > 9) 9.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp,
                    )
                }
            }
        }
    }
}
