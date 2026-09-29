package com.saintnico.verdlyhabits.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.haptics.HapticsEngine
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

private val Mint = Color(0xFF52B788)
private val HabitGreen = Color(0xFF43A047)
private val GoalBlue = Color(0xFF42A5F5)
private val ChallengeGold = Color(0xFFFFCA28)
private val FocusTeal = Color(0xFF26C6DA)
private val ProofAmber = Color(0xFFE08A2E)
private val TrophyGold = Color(0xFFD4A017)
private val InviteSage = Color(0xFF74C69D)
private val NudgeCoral = Color(0xFFFF7A45)
private val DuoGreen = Color(0xFF2D6A4F)

private data class ShareCell(
    val action: CreateAction,
    val label: String,
    val why: String,
    val icon: ImageVector,
    val accent: Color,
    val enabled: Boolean,
    val disabledWhy: String? = null,
)

private data class StartChip(
    val action: CreateAction,
    val label: String,
    val icon: ImageVector,
    val accent: Color,
)

private fun emphasizedActionForTab(tabRoute: String): CreateAction? = when (tabRoute) {
    "home" -> CreateAction.AddHabit
    "goals" -> CreateAction.NewGoal
    "arena" -> CreateAction.CreateChallenge
    else -> null
}

@Composable
fun CreateSheet(
    visible: Boolean,
    currentTabRoute: String,
    state: CreateViewState,
    haptics: HapticsEngine,
    onDismiss: () -> Unit,
    onAction: (CreateAction) -> Unit,
    fabOriginInWindow: Offset = Offset.Unspecified,
) {
    var shown by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            shown = true
            haptics.tick()
            progress.animateTo(
                1f,
                tween(NavMotion.SHEET_OPEN_MS, easing = FastOutSlowInEasing),
            )
        } else if (shown) {
            progress.animateTo(
                0f,
                tween(NavMotion.SHEET_CLOSE_MS, easing = FastOutLinearInEasing),
            )
            shown = false
        }
    }

    if (!shown && progress.value == 0f) return

    val p = progress.value
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 96.dp),
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
        val origin = if (fabOriginInWindow != Offset.Unspecified) {
            TransformOrigin(
                pivotFractionX = (fabOriginInWindow.x / widthPx).coerceIn(0.15f, 0.85f),
                pivotFractionY = (fabOriginInWindow.y / heightPx).coerceIn(0.55f, 0.99f),
            )
        } else {
            TransformOrigin(0.5f, 1f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = p }
                .background(Color.Black.copy(alpha = 0.45f * p))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .graphicsLayer {
                    transformOrigin = origin
                    val scale = 0.86f + 0.14f * p
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - p) * 48f
                    alpha = p
                }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)),
            )
            Text(
                stringResource(R.string.create_plant_or_share),
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 14.dp, top = 8.dp),
            )

            CreateHeroCard(
                state = state,
                progress = p,
                onClick = {
                    haptics.tick()
                    onAction(state.heroAction())
                    onDismiss()
                },
            )
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.create_share),
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            CreateShareGrid(
                state = state,
                progress = p,
                onCell = { cell ->
                    haptics.tick()
                    if (!cell.enabled) return@CreateShareGrid
                    onAction(cell.action)
                    onDismiss()
                },
            )
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.create_start),
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier.padding(bottom = 8.dp),
            )
            CreateStartRow(
                currentTabRoute = currentTabRoute,
                onChip = { action ->
                    haptics.tick()
                    onAction(action)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun CreateHeroCard(
    state: CreateViewState,
    progress: Float,
    onClick: () -> Unit,
) {
    val (title, why, icon, accent) = when (state.hero) {
        CreateHeroKind.PostProof -> HeroCopy(
            "Post proof",
            state.proofHabitTitle?.let { "For $it" } ?: "Your arena needs the photo",
            Icons.Rounded.CameraAlt,
            ProofAmber,
        )
        CreateHeroKind.ShareMilestone -> HeroCopy(
            state.milestone?.headline ?: "Share this win",
            state.milestone?.why ?: "A win worth showing",
            if (state.milestone?.kind == CreateMilestone.Kind.STREAK) {
                Icons.Rounded.LocalFireDepartment
            } else {
                Icons.Rounded.WorkspacePremium
            },
            TrophyGold,
        )
        CreateHeroKind.SharePlant -> HeroCopy(
            "Share today's plant",
            "Friends only see what you share",
            Icons.Rounded.Spa,
            Mint,
        )
        CreateHeroKind.PlantSomething -> HeroCopy(
            "Plant something",
            "Light your ring first",
            Icons.Rounded.Spa,
            HabitGreen,
        )
        CreateHeroKind.Invite -> HeroCopy(
            "Invite into the grove",
            "A grove starts with two",
            Icons.Rounded.PersonAdd,
            InviteSage,
        )
        CreateHeroKind.AddHabit -> HeroCopy(
            "Add a habit",
            "Start something you'll keep",
            Icons.Rounded.Add,
            HabitGreen,
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = progress
                translationY = (1f - progress) * 10f
            }
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.38f), RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                if (state.hero == CreateHeroKind.PostProof) {
                    com.saintnico.verdlyhabits.ui.components.VerdlyCameraFlashIcon(size = 28.dp)
                } else {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    why,
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
            }
        }
    }
}

private data class HeroCopy(
    val title: String,
    val why: String,
    val icon: ImageVector,
    val accent: Color,
)

@Composable
private fun CreateShareGrid(
    state: CreateViewState,
    progress: Float,
    onCell: (ShareCell) -> Unit,
) {
    val nudgeLabel = state.firstNudgeName?.let { "Nudge @$it" } ?: "Nudge someone"
    val plantLabel = if (state.alreadyShared) "Change plant" else "Share plant"
    val duoLabel = if (state.hasDuo) "Open duo" else "Start a duo"
    val duoWhy = when {
        state.hasDuo && state.duoSwapAvailable -> "Gift a swap day"
        state.hasDuo -> "Keep the pair going"
        else -> "Root a streak together"
    }
    val cells = listOf(
        ShareCell(
            action = CreateAction.SharePlant,
            label = plantLabel,
            why = if (state.canSharePlant) "Show the grove" else "Plant one first.",
            icon = Icons.Rounded.Spa,
            accent = Mint,
            enabled = state.canSharePlant,
            disabledWhy = "Plant one first.",
        ),
        ShareCell(
            action = CreateAction.PostProof,
            label = "Post proof",
            why = if (state.proofWindowOpen) "Camera, then it counts" else "No proof window.",
            icon = Icons.Rounded.CameraAlt,
            accent = ProofAmber,
            enabled = state.proofWindowOpen,
            disabledWhy = "No proof window.",
        ),
        ShareCell(
            action = CreateAction.ShareCard,
            label = "Share card",
            why = "Your card, their sheet",
            icon = Icons.Rounded.Share,
            accent = GoalBlue,
            enabled = true,
        ),
        ShareCell(
            action = CreateAction.Nudge,
            label = nudgeLabel,
            why = if (state.friendCount > 0) "A quiet poke" else "Need a grove first.",
            icon = Icons.Rounded.Bolt,
            accent = NudgeCoral,
            enabled = state.friendCount > 0,
            disabledWhy = "Need a grove first.",
        ),
        ShareCell(
            action = CreateAction.Invite,
            label = "Invite",
            why = if (state.isSignedIn) "Send a grove link" else "Sign in first.",
            icon = Icons.Rounded.PersonAdd,
            accent = InviteSage,
            enabled = state.isSignedIn,
            disabledWhy = "Sign in first.",
        ),
        ShareCell(
            action = CreateAction.Duo,
            label = duoLabel,
            why = if (!state.hasDuo && state.friendCount == 0) "Need a grove first." else duoWhy,
            icon = Icons.Rounded.EmojiEvents,
            accent = DuoGreen,
            enabled = state.hasDuo || state.friendCount > 0,
            disabledWhy = "Need a grove first.",
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cells.chunked(2).forEachIndexed { row, pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                pair.forEachIndexed { col, cell ->
                    val index = row * 2 + col
                    val delay = 0.08f * index
                    val cellP = ((progress - delay) / (1f - delay)).coerceIn(0f, 1f)
                    ShareGridCell(
                        cell = cell,
                        appear = cellP,
                        modifier = Modifier.weight(1f),
                        onClick = { onCell(cell) },
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShareGridCell(
    cell: ShareCell,
    appear: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val muted = !cell.enabled
    Column(
        modifier = modifier
            .graphicsLayer {
                alpha = appear * if (muted) 0.55f else 1f
                translationY = (1f - appear) * 14f
            }
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(cell.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            if (cell.action == CreateAction.PostProof) {
                com.saintnico.verdlyhabits.ui.components.VerdlyCameraFlashIcon(size = 22.dp)
            } else {
                Icon(cell.icon, contentDescription = null, tint = cell.accent, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            cell.label,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            if (muted) cell.disabledWhy ?: cell.why else cell.why,
            fontFamily = dmSansFamily,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            maxLines = 2,
        )
    }
}

@Composable
private fun CreateStartRow(
    currentTabRoute: String,
    onChip: (CreateAction) -> Unit,
) {
    val emphasized = emphasizedActionForTab(currentTabRoute)
    val chips = listOf(
        StartChip(CreateAction.AddHabit, "Habit", Icons.Rounded.Add, HabitGreen),
        StartChip(CreateAction.NewGoal, "Goal", Icons.Rounded.Flag, GoalBlue),
        StartChip(CreateAction.CreateChallenge, "Challenge", Icons.Rounded.EmojiEvents, ChallengeGold),
        StartChip(CreateAction.StartFocus, stringResource(R.string.focus), Icons.Rounded.Spa, FocusTeal),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            val on = chip.action == emphasized
            val scale by animateFloatAsState(
                targetValue = if (on) 1f else 0.98f,
                animationSpec = tween(220),
                label = "start_chip",
            )
            Row(
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (on) chip.accent.copy(alpha = 0.16f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    )
                    .then(
                        if (on) Modifier.border(1.dp, chip.accent.copy(alpha = 0.4f), RoundedCornerShape(999.dp))
                        else Modifier,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onChip(chip.action) },
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    chip.icon,
                    contentDescription = null,
                    tint = chip.accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    chip.label,
                    fontFamily = dmSansFamily,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (chip.action == CreateAction.StartFocus) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = chip.accent.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}
