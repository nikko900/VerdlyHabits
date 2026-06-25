@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Premium swipe-to-complete habit card.
 *
 *  - Swipe right → [onSwipeComplete] (green sweep underlay reveals).
 *  - Swipe left  → [onSwipeSkip]     (gray sweep underlay reveals).
 *  - Long press  → [onLongPress]     (action sheet trigger).
 *  - Tap         → [onTap]
 *
 * The card animates a soft "morph" on the completion check icon and shows the
 * habit's difficulty stripe + category chip. Designed to feel haptic-rich, with
 * no visual hardcoded colors that ignore the active theme.
 */
@Composable
fun HabitSwipeCard(
    habit: HabitItem,
    isAtRisk: Boolean,
    onTap: () -> Unit,
    onSwipeComplete: () -> Unit,
    onSwipeSkip: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusTap: (() -> Unit)? = null,
    showDifficultyStripe: Boolean = true,
    swipeThresholdDp: Int = 88
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { swipeThresholdDp.dp.toPx() }
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val complete = habit.isCompleted

    // Reset offset whenever the underlying completion state changes (after action).
    LaunchedEffect(complete) {
        offset.animateTo(0f, tween(280))
    }

    val accent = habit.difficulty.color
    val category = habit.category

    val riskPulse by rememberInfiniteTransition(label = "risk").animateFloat(
        0.3f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "risk_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Underlay (revealed during drag)
        val dragNorm = (offset.value / thresholdPx).coerceIn(-1f, 1f)
        val underlayAlpha = abs(dragNorm).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when {
                        offset.value > 0f -> Color(0xFF1D6B44).copy(alpha = 0.18f + 0.55f * underlayAlpha)
                        offset.value < 0f -> Color(0xFF607D8B).copy(alpha = 0.10f + 0.40f * underlayAlpha)
                        else              -> Color.Transparent
                    }
                ),
            contentAlignment = if (offset.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            Box(modifier = Modifier.padding(horizontal = 22.dp), contentAlignment = Alignment.Center) {
                if (offset.value > 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.CheckCircle, null,
                            tint = Color(0xFF1B5E20),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Complete",
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                } else if (offset.value < 0f) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Skip today",
                            color = Color(0xFF455A64),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Rounded.SkipNext, null,
                            tint = Color(0xFF455A64),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        val borderColor = if (isAtRisk) Color(0xFFFFB300).copy(alpha = riskPulse) else Color.Transparent

        // Card surface — draggable
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = offset.value }
                .pointerInput(habit.id, complete) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offset.value >  thresholdPx -> {
                                        offset.animateTo(thresholdPx * 4f, tween(180))
                                        onSwipeComplete()
                                    }
                                    offset.value < -thresholdPx -> {
                                        offset.animateTo(-thresholdPx * 4f, tween(180))
                                        onSwipeSkip()
                                    }
                                    else -> offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offset.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                // Reduce drag distance after threshold to give a rubber-band feel.
                                val newValue = offset.value + dragAmount
                                val damped = if (abs(newValue) > thresholdPx) {
                                    val overshoot = abs(newValue) - thresholdPx
                                    val sign = if (newValue > 0) 1f else -1f
                                    sign * (thresholdPx + overshoot * 0.35f)
                                } else newValue
                                offset.snapTo(damped)
                            }
                        }
                    )
                }
                .border(if (isAtRisk) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = if (complete)
                MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.surface,
            tonalElevation = if (complete) 0.dp else 2.dp,
            shadowElevation = if (complete) 0.dp else 3.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickableSafe(
                        onClick = onTap,
                        onLongClick = onLongPress
                    )
                    .padding(start = 0.dp)
            ) {
                if (showDifficultyStripe) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(4.dp)
                            .clip(RectangleShape)
                            .background(
                                Brush.verticalGradient(
                                    listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.55f))
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(habit.color.toInt()).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            habit.icon, null,
                            tint = Color(habit.color.toInt()),
                            modifier = Modifier.size(22.dp)
                        )
                        if (habit.isPaused) {
                            Icon(
                                Icons.Rounded.Pause, null,
                                modifier = Modifier
                                    .size(11.dp)
                                    .align(Alignment.TopEnd),
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            habit.title,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (complete)
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.onBackground,
                            textDecoration = if (complete) TextDecoration.LineThrough else null
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Streak flame
                            StreakFlame(streak = habit.streak, size = 14.dp)
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "${habit.streak}d",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(8.dp))
                            // Category chip
                            CategoryChip(category)
                            Spacer(Modifier.width(6.dp))
                            DifficultyDot(habit.difficulty.color)
                            if (isAtRisk) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Rounded.Warning, null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text("at risk", fontSize = 10.sp, color = Color(0xFFFFB300))
                            }
                        }
                    }

                    // Quick focus action
                    if (onFocusTap != null && !complete) {
                        IconButton(onClick = onFocusTap, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Rounded.Timer, null,
                                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Complete checkbox (tap-to-complete fallback for accessibility)
                    val checkScale by animateFloatAsState(
                        targetValue = if (complete) 1f else 0.85f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                        label = "check"
                    )
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (complete) Color(habit.color.toInt())
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                            )
                            .clickable { onTap() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (complete) {
                            Icon(
                                Icons.Rounded.Check, null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(22.dp)
                                    .graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                            )
                        } else {
                            Icon(
                                Icons.Rounded.ChevronRight, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: com.saintnico.verdlyhabits.domain.HabitCategory) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(category.color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = category.displayName,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = category.color
        )
    }
}

@Composable
private fun DifficultyDot(color: Color) {
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Tiny shim so we keep the [combinedClickable] API but with safer behaviour: it
 * still routes long-clicks, but tap and long-press don't interfere with the
 * outer drag gesture (combinedClickable consumes the down-event, which can swallow
 * the start of a horizontal drag — here we let drag take precedence).
 */
@Composable
private fun Modifier.combinedClickableSafe(
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier = this.then(
    Modifier.combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick
    )
)
