package com.saintnico.verdlyhabits.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.haptics.HapticsEngine
import com.saintnico.verdlyhabits.ui.components.coins.GroveAvatarHalo
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

private val Mint = Color(0xFF52B788)

@Composable
fun CreatePulseComposer(
    visible: Boolean,
    state: CreateViewState,
    haptics: HapticsEngine,
    fabOriginInWindow: Offset,
    onShare: (String) -> Unit,
    onClearShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    var shown by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    var selected by remember(state.alreadySharedTitle, state.completedToday) {
        mutableStateOf(state.alreadySharedTitle ?: state.completedToday.firstOrNull().orEmpty())
    }

    LaunchedEffect(visible) {
        if (visible) {
            shown = true
            haptics.tick()
            progress.animateTo(1f, tween(NavMotion.SHEET_OPEN_MS, easing = FastOutSlowInEasing))
        } else if (shown) {
            progress.animateTo(0f, tween(NavMotion.SHEET_CLOSE_MS, easing = FastOutLinearInEasing))
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
                pivotFractionY = (fabOriginInWindow.y / heightPx).coerceIn(0.55f, 1f),
            )
        } else {
            TransformOrigin(0.5f, 1f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = p }
                .background(Color.Black.copy(alpha = 0.52f * p))
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
                .fillMaxHeight(0.78f)
                .graphicsLayer {
                    transformOrigin = origin
                    val scale = 0.88f + 0.12f * p
                    scaleX = scale
                    scaleY = scale
                    translationY = (1f - p) * 40f
                    alpha = p
                }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 22.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 18.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)),
            )
            GroveAvatarHalo(
                glowId = state.titleGlowId,
                frameId = state.profileFrameId,
                size = 72.dp,
            ) {
                val photo = state.photoUrl
                if (!photo.isNullOrBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = state.username,
                        modifier = Modifier.size(72.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Mint.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            state.username.take(1).uppercase().ifBlank { "Y" },
                            fontWeight = FontWeight.Bold,
                            color = Mint,
                            fontSize = 22.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                if (state.alreadyShared) "Change today's plant" else "Share today's plant",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Friends see the title. Nothing else.",
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(22.dp))
            if (state.completedToday.isEmpty()) {
                Text(
                    "Plant one first.",
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.completedToday, key = { it }) { title ->
                        val on = selected == title
                        FilterChip(
                            selected = on,
                            onClick = {
                                haptics.tick()
                                selected = title
                            },
                            label = {
                                Text(title, fontFamily = dmSansFamily, fontSize = 13.sp)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Mint.copy(alpha = 0.18f),
                                selectedLabelColor = Mint,
                            ),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    val title = selected.trim()
                    if (title.isBlank()) return@Button
                    haptics.tick()
                    onShare(title)
                },
                enabled = selected.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Mint),
            ) {
                Text(
                    "Share with grove",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                )
            }
            if (state.alreadyShared) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        haptics.tick()
                        onClearShare()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Take it down", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Not now",
                fontFamily = dmSansFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier = Modifier
                    .clickable { onDismiss() }
                    .padding(8.dp),
            )
        }
    }
}
