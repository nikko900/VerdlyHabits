package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily

/**
 * Instant dopamine flash on habit check-off: bouncing check + XP + soft glow.
 * Meant to fire the same millisecond as sound/haptics/confetti.
 */
@Composable
fun CompletionBurstOverlay(
    visible: Boolean,
    xpLabel: String,
    headline: String = "Locked in",
    modifier: Modifier = Modifier,
) {
    val ringScale by animateFloatAsState(
        targetValue = if (visible) 1.35f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "completion_ring",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (visible) 0.28f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "completion_glow",
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(90)) + scaleIn(
            initialScale = 0.55f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        ),
        exit = fadeOut(tween(280)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = glowAlpha * 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .graphicsLayer {
                        scaleX = ringScale
                        scaleY = ringScale
                        alpha = glowAlpha
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary,
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    headline,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    xpLabel,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
