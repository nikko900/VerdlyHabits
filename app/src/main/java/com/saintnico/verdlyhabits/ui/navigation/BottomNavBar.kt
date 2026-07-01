package com.saintnico.verdlyhabits.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector = icon
)

val bottomNavItems = listOf(
    BottomNavItem("home", "Today",   Icons.Rounded.Home,        Icons.Rounded.Home),
    BottomNavItem("challenges", "Challenges", Icons.Rounded.EmojiEvents, Icons.Rounded.EmojiEvents),
    BottomNavItem("duo", "Duo", Icons.Rounded.LocalFireDepartment, Icons.Rounded.LocalFireDepartment),
    BottomNavItem("focus", "Focus",   Icons.Rounded.Spa,         Icons.Rounded.Spa),
    BottomNavItem("goals", "Goals",   Icons.Rounded.Flag,        Icons.Rounded.Flag),
    BottomNavItem("profile", "Profile", Icons.Rounded.Person,   Icons.Rounded.Person),
)

class CutoutShape(
    private val cutoutCenterX: Float,
    private val cutoutRadius: Float,
    private val cutoutDepth: Float,
    private val cornerRadius: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, cornerRadius)
            quadraticBezierTo(0f, 0f, cornerRadius, 0f)

            if (cutoutCenterX > 0) {
                val startX = cutoutCenterX - cutoutRadius * 1.5f
                val endX = cutoutCenterX + cutoutRadius * 1.5f

                lineTo(startX, 0f)

                cubicTo(
                    startX + cutoutRadius * 0.5f, 0f,
                    startX + cutoutRadius * 0.5f, cutoutDepth,
                    cutoutCenterX, cutoutDepth
                )

                cubicTo(
                    endX - cutoutRadius * 0.5f, cutoutDepth,
                    endX - cutoutRadius * 0.5f, 0f,
                    endX, 0f
                )
            }

            lineTo(size.width - cornerRadius, 0f)
            quadraticBezierTo(size.width, 0f, size.width, cornerRadius)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun VerdlyBottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    duoGlowing: Boolean = false,
    duoAtRisk: Boolean = false,
    modifier: Modifier = Modifier
) {
    val items = bottomNavItems
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0

    val animatedSelectedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "nav_index"
    )

    var navWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    val itemWidth = if (items.isNotEmpty() && navWidth > 0) navWidth / items.size else 0f
    val cutoutCenterX = if (itemWidth > 0) (animatedSelectedIndex * itemWidth) + (itemWidth / 2f) else 0f

    val barHeight = 72.dp
    val floatingButtonSize = 52.dp

    val cutoutRadiusPx = with(density) { 30.dp.toPx() }
    val cutoutDepthPx = with(density) { 36.dp.toPx() }
    val cornerRadiusPx = with(density) { 24.dp.toPx() }

    val infinite = rememberInfiniteTransition(label = "duo_nav_glow")
    val duoPulse by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "duo_nav_pulse",
    )
    val duoAccent = if (duoAtRisk) Color(0xFFFFB300) else Color(0xFF52B788)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(barHeight + (floatingButtonSize / 2))
    ) {
        // Background with cutout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter)
                .onGloballyPositioned { navWidth = it.size.width.toFloat() }
                .graphicsLayer {
                    shape = CutoutShape(
                        cutoutCenterX = cutoutCenterX,
                        cutoutRadius = cutoutRadiusPx,
                        cutoutDepth = cutoutDepthPx,
                        cornerRadius = cornerRadiusPx
                    )
                    clip = true
                    shadowElevation = 20.dp.toPx()
                }
                .background(MaterialTheme.colorScheme.surface)
        )

        // Floating Button
        if (itemWidth > 0) {
            val floatingButtonOffset = with(density) { cutoutCenterX.toDp() - (floatingButtonSize / 2) }

            val gradientColors = when (items[selectedIndex].route) {
                "home" -> listOf(Color(0xFF43A047), Color(0xFF1B5E20))
                "challenges" -> listOf(Color(0xFFFFCA28), Color(0xFFFF8F00))
                "duo" -> listOf(Color(0xFFFF7043), Color(0xFFD84315))
                "focus" -> listOf(Color(0xFF26C6DA), Color(0xFF00695C))
                "goals" -> listOf(Color(0xFF42A5F5), Color(0xFF1565C0))
                "profile" -> listOf(Color(0xFFAB47BC), Color(0xFF6A1B9A))
                else -> listOf(Color(0xFF74C69D), Color(0xFF2D6A4F))
            }
            val shadowColor = gradientColors.first()

            Box(
                modifier = Modifier
                    .offset(x = floatingButtonOffset, y = 4.dp)
                    .size(floatingButtonSize)
                    .shadow(
                        elevation = 16.dp, 
                        shape = CircleShape,
                        ambientColor = shadowColor,
                        spotColor = shadowColor
                    )
                    .background(
                        brush = Brush.linearGradient(colors = gradientColors),
                        shape = CircleShape
                    )
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // Clicking the floating button re-selects the current tab
                        onNavigate(items[selectedIndex].route)
                    },
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = selectedIndex,
                    transitionSpec = {
                        (scaleIn(spring(dampingRatio = 0.6f, stiffness = 300f)) + fadeIn(tween(250))) togetherWith 
                        (scaleOut(spring(dampingRatio = 0.7f, stiffness = 400f)) + fadeOut(tween(200)))
                    },
                    label = "floating_icon"
                ) { index ->
                    AnimatedFloatingNavIcon(
                        route = items[index].route,
                        icon = items[index].iconSelected,
                    )
                }
            }
        }

        // Navigation Items
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                val isDuo = item.route == "duo"

                val itemAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 0f else 1f,
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                    label = "item_alpha"
                )
                val itemY by animateFloatAsState(
                    targetValue = if (isSelected) 30f else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                    label = "item_y"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(item.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .offset(y = itemY.dp)
                            .alpha(itemAlpha)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isDuo && duoGlowing) duoAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        if (isDuo && duoGlowing) {
                                            scaleX = duoPulse
                                            scaleY = duoPulse
                                        }
                                    }
                            )
                            if (isDuo && duoGlowing) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(8.dp)
                                        .background(duoAccent, CircleShape)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            fontSize = 10.sp,
                            fontWeight = if (isDuo && duoGlowing) FontWeight.Bold else FontWeight.Normal,
                            color = if (isDuo && duoGlowing) duoAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}