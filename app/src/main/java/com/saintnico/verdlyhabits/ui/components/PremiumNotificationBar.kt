package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PremiumNotificationBar(
    message: String,
    isVisible: Boolean,
    isError: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(animationSpec = tween(500), initialOffsetY = { -it }),
        exit = slideOutVertically(animationSpec = tween(500), targetOffsetY = { -it })
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp) // Top padding to avoid status bar
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isError) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    com.saintnico.verdlyhabits.ui.components.AnimatedHabitIcon(
                        icon = icon,
                        color = Color.White,
                        size = 24.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
