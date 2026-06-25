package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Overlay that locks any subtree behind premium. The content is rendered
 * underneath, blurred + dimmed, with a padlock + "Pro" pill on top.
 *
 *  - When [locked] is false, the wrapped [content] is shown as-is (zero cost).
 *  - When [locked] is true, the content is shown blurred and tap is intercepted
 *    so it routes to [onUnlockTap] (which should open [PaywallSheet]).
 *
 * Important: the master switch in [com.saintnico.verdlyhabits.domain.PremiumGate]
 * controls whether anything is actually locked — by default it's `false`, so the
 * overlay is effectively dormant.
 */
@Composable
fun PaywallLockOverlay(
    locked: Boolean,
    onUnlockTap: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Pro",
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier.then(
                if (locked) Modifier.blur(6.dp) else Modifier
            )
        ) {
            Box(content = content)
        }

        if (locked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable { onUnlockTap() },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(12.dp), tint = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Unlock $label",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFFD54F))
                    }
                }
            }
        }
    }
}
