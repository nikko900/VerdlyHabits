package com.saintnico.verdlyhabits.ui.components.notifications

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Simple bell — no glass shell. Subtle shake when unread. */
@Composable
fun NotificationBellButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val shakeAnim = remember { Animatable(0f) }

    LaunchedEffect(unreadCount) {
        if (unreadCount > 0) {
            while (true) {
                delay(4000)
                shakeAnim.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 500
                        0f at 0
                        12f at 80
                        -12f at 160
                        8f at 240
                        0f at 500 with FastOutSlowInEasing
                    },
                )
            }
        } else {
            shakeAnim.snapTo(0f)
        }
    }

    IconButton(onClick = onClick, modifier = modifier) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                Icons.Rounded.Notifications,
                contentDescription = "Notifications",
                tint = if (unreadCount > 0) accent else tint,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        rotationZ = shakeAnim.value
                        transformOrigin = TransformOrigin(0.5f, 0.15f)
                    },
            )
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

/** Settings keeps the premium glass circle + spin on tap. */
@Composable
fun AnimatedSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 400f),
        label = "press_scale",
    )

    val rotation = remember { Animatable(0f) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        rotation.snapTo(-24f)
        delay(280)
        rotation.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = modifier
            .scale(pressScale)
            .size(42.dp)
            .shadow(elevation = 2.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    coroutineScope.launch {
                        rotation.animateTo(
                            targetValue = rotation.value + 180f,
                            animationSpec = tween(450, easing = FastOutSlowInEasing),
                        )
                    }
                    onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Tune,
            contentDescription = "Settings",
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = rotation.value },
        )
    }
}
