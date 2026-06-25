package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

@Composable
fun AnimatedHabitIcon(icon: ImageVector, color: Color, size: Dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "habit_icon_transition")
    
    val iconName = icon.name ?: ""
    
    when {
        iconName.contains("DirectionsRun", ignoreCase = true) -> {
            val ty by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(300, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "run_bounce"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size).graphicsLayer { translationY = ty }
            )
        }
        iconName.contains("SelfImprovement", ignoreCase = true) -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "meditate_scale"
            )
            val ringAlpha by infiniteTransition.animateFloat(
                initialValue = 0.5f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseOut),
                    repeatMode = RepeatMode.Restart
                ),
                label = "meditate_ring"
            )
            val ringScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = EaseOut),
                    repeatMode = RepeatMode.Restart
                ),
                label = "meditate_ring_scale"
            )
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color.copy(alpha = ringAlpha),
                    modifier = Modifier.size(size).scale(ringScale)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(size).scale(scale)
                )
            }
        }
        iconName.contains("WaterDrop", ignoreCase = true) || iconName.contains("LocalDrink", ignoreCase = true) -> {
            val ty by infiniteTransition.animateFloat(
                initialValue = -8f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = EaseIn),
                    repeatMode = RepeatMode.Restart
                ),
                label = "water_drop"
            )
            val scaleY by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.7f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 800
                        1f at 0
                        1f at 700
                        0.7f at 800
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "water_squish"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 800
                        0f at 0
                        1f at 200
                        1f at 700
                        0f at 800
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "water_alpha"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(size)
                    .alpha(alpha)
                    .graphicsLayer { 
                        translationY = ty * density
                        this.scaleY = scaleY 
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    }
            )
        }
        iconName.contains("Book", ignoreCase = true) -> {
            val rot by infiniteTransition.animateFloat(
                initialValue = -3f,
                targetValue = 3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "book_sway"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size).graphicsLayer { rotationZ = rot }
            )
        }
        iconName.contains("FitnessCenter", ignoreCase = true) -> {
            val ty by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "gym_lift"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size).graphicsLayer { translationY = ty }
            )
        }
        iconName.contains("Bedtime", ignoreCase = true) -> {
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sleep_fade"
            )
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(size).alpha(alpha)
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = color.copy(alpha = alpha),
                    modifier = Modifier.size(size * 0.3f).graphicsLayer { translationX = size.value * 0.8f; translationY = -size.value * 0.8f }
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = color.copy(alpha = 1f - alpha * 0.5f),
                    modifier = Modifier.size(size * 0.2f).graphicsLayer { translationX = -size.value * 0.6f; translationY = -size.value * 0.4f }
                )
            }
        }
        iconName.contains("EditNote", ignoreCase = true) || iconName.contains("Brush", ignoreCase = true) -> {
            val tx by infiniteTransition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "journal_pen"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size).graphicsLayer { translationX = tx }
            )
        }
        iconName.contains("Favorite", ignoreCase = true) || iconName.contains("MonitorHeart", ignoreCase = true) -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        1f at 0
                        1.2f at 150
                        1f at 300
                        1.1f at 450
                        1f at 600
                        1f at 1000
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "heartbeat"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size).scale(scale)
            )
        }
        iconName.contains("Shower", ignoreCase = true) -> {
            val ty by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "shower"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size).graphicsLayer { translationY = ty }
            )
        }
        iconName.contains("PhoneLocked", ignoreCase = true) -> {
            val rot by infiniteTransition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "phone_shake"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size).graphicsLayer { rotationZ = rot }
            )
        }
        else -> {
            val ty by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOut),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "default_bounce"
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(size).graphicsLayer { translationY = ty }
            )
        }
    }
}
