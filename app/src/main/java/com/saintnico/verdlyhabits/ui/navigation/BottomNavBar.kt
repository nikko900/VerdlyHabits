package com.saintnico.verdlyhabits.ui.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.haptics.HapticsEngine

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector = icon,
    val useArenaIcon: Boolean = false,
)

val bottomNavRoutes = listOf("home", "arena", "goals", "profile")

@Composable
private fun localizedBottomNavItems(): List<BottomNavItem> = listOf(
    BottomNavItem("home", stringResource(R.string.nav_today), Icons.Rounded.Home, Icons.Rounded.Home),
    BottomNavItem("arena", stringResource(R.string.nav_arena), Icons.Rounded.EmojiEvents, Icons.Rounded.EmojiEvents, useArenaIcon = true),
    BottomNavItem("goals", stringResource(R.string.nav_goals), Icons.Rounded.Flag, Icons.Rounded.Flag),
    BottomNavItem("profile", stringResource(R.string.nav_profile), Icons.Rounded.Person, Icons.Rounded.Person),
)

private const val FAB_SLOT_INDEX = 2

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
    onCreateClick: () -> Unit,
    haptics: HapticsEngine,
    createOpen: Boolean = false,
    onCreateLongClick: () -> Unit = onCreateClick,
    proofDue: Boolean = false,
    onCreateFabPositioned: (Offset) -> Unit = {},
    duoGlowing: Boolean = false,
    duoAtRisk: Boolean = false,
    modifier: Modifier = Modifier
) {
    val items = localizedBottomNavItems()
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0

    var navWidth by remember { mutableStateOf(0f) }
    val density = LocalDensity.current

    val slotCount = items.size + 1
    val slotWidth = if (navWidth > 0) navWidth / slotCount else 0f
    val cutoutCenterX = if (slotWidth > 0) (FAB_SLOT_INDEX * slotWidth) + (slotWidth / 2f) else 0f

    val barHeight = 72.dp
    val floatingButtonSize = 52.dp

    val cutoutRadiusPx = with(density) { 30.dp.toPx() }
    val cutoutDepthPx = with(density) { 36.dp.toPx() }
    val cornerRadiusPx = with(density) { 24.dp.toPx() }

    val infinite = rememberInfiniteTransition(label = "nav_idle")
    val duoPulse by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "arena_nav_pulse",
    )
    val breath by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(NavMotion.FAB_BREATH_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fab_breath",
    )
    val duoAccent = if (duoAtRisk) Color(0xFFE08A2E) else Color(0xFF52B788)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(barHeight + (floatingButtonSize / 2))
    ) {
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

        if (slotWidth > 0) {
            val floatingButtonOffset = with(density) { cutoutCenterX.toDp() - (floatingButtonSize / 2) }
            CreateFab(
                modifier = Modifier
                    .offset(x = floatingButtonOffset, y = 4.dp)
                    .size(floatingButtonSize),
                createOpen = createOpen,
                proofDue = proofDue,
                breath = breath,
                haptics = haptics,
                onClick = onCreateClick,
                onLongClick = onCreateLongClick,
                onPositioned = onCreateFabPositioned,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.take(2).forEachIndexed { index, item ->
                NavTabSlot(
                    item = item,
                    isSelected = selectedIndex == index,
                    duoGlowing = duoGlowing,
                    duoAtRisk = duoAtRisk,
                    duoPulse = duoPulse,
                    duoAccent = duoAccent,
                    onNavigate = onNavigate,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.weight(1f))
            items.drop(2).forEachIndexed { offset, item ->
                val index = offset + 2
                NavTabSlot(
                    item = item,
                    isSelected = selectedIndex == index,
                    duoGlowing = duoGlowing,
                    duoAtRisk = duoAtRisk,
                    duoPulse = duoPulse,
                    duoAccent = duoAccent,
                    onNavigate = onNavigate,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreateFab(
    createOpen: Boolean,
    proofDue: Boolean,
    breath: Float,
    haptics: HapticsEngine,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPositioned: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) {
        if (pressed) haptics.tick()
    }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) NavMotion.FAB_PRESS_SCALE else 1f,
        animationSpec = NavMotion.press,
        label = "fab_press",
    )
    val plusRotation by animateFloatAsState(
        targetValue = if (createOpen) NavMotion.PLUS_TO_X_DEG else 0f,
        animationSpec = NavMotion.settle,
        label = "plus_to_x",
    )
    val breathPx = with(LocalDensity.current) { NavMotion.FAB_BREATH_DP.dp.toPx() }
    val breathOffset = if (pressed || createOpen) 0f else -(breath * breathPx)
    val createGradient = listOf(Color(0xFF74C69D), Color(0xFF2D6A4F))
    val shadowColor = createGradient.first()

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val origin = coords.positionInWindow()
                onPositioned(
                    Offset(
                        origin.x + coords.size.width / 2f,
                        origin.y + coords.size.height / 2f,
                    ),
                )
            }
            .graphicsLayer {
                translationY = breathOffset
                scaleX = pressScale
                scaleY = pressScale
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    ambientColor = shadowColor,
                    spotColor = shadowColor,
                )
                .background(
                    brush = Brush.linearGradient(colors = createGradient),
                    shape = CircleShape,
                )
                .clip(CircleShape)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = {
                        haptics.tick()
                        onLongClick()
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = if (createOpen) stringResource(R.string.close_create) else stringResource(R.string.create),
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .graphicsLayer { rotationZ = plusRotation },
            )
        }
        if (proofDue && !createOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE08A2E)),
            )
        }
    }
}

@Composable
private fun NavTabSlot(
    item: BottomNavItem,
    isSelected: Boolean,
    duoGlowing: Boolean,
    duoAtRisk: Boolean,
    duoPulse: Float,
    duoAccent: Color,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isArena = item.route == "arena"
    val lift = rememberTabSelectLift(isSelected)
    var arenaFire by remember { mutableStateOf(false) }
    LaunchedEffect(isSelected) {
        if (isArena && isSelected) {
            arenaFire = true
            kotlinx.coroutines.delay(900)
            arenaFire = false
        } else {
            arenaFire = false
        }
    }
    val pillAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(NavMotion.PILL_MS),
        label = "tab_pill",
    )
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onNavigate(item.route) },
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.graphicsLayer { translationY = lift },
            ) {
                if (item.useArenaIcon) {
                    ArenaConjoinedIcon(
                        tint = when {
                            isArena && duoGlowing -> duoAccent
                            isSelected -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        },
                        iconSize = 18.dp,
                        modifier = Modifier.graphicsLayer {
                            if (isArena && duoGlowing) {
                                scaleX = duoPulse
                                scaleY = duoPulse
                            }
                        },
                    )
                    ArenaSelectFire(playing = arenaFire)
                } else {
                    Icon(
                        imageVector = if (isSelected) item.iconSelected else item.icon,
                        contentDescription = item.label,
                        tint = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (isSelected) 0.9f else 0.4f
                        ),
                        modifier = Modifier.size(24.dp),
                    )
                }
                if (isArena && duoGlowing && !arenaFire) {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = duoAccent,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-6).dp)
                            .size(10.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected || (isArena && duoGlowing)) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isArena && duoGlowing -> duoAccent
                    isSelected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
            )
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .width(12.dp)
                    .height(3.dp)
                    .graphicsLayer { alpha = pillAlpha }
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
            )
        }
    }
}
