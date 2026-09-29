package com.saintnico.verdlyhabits.ui.components.reactions

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.ReactionWeights
import com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay

fun proofReactionKey(userId: String, date: String): String = "${userId}_$date"

fun reactionCountsFor(challenge: Challenge, proofKey: String): Map<String, Int> =
    challenge.reactions[proofKey]
        ?.values
        ?.flatten()
        ?.groupingBy { it }
        ?.eachCount()
        .orEmpty()

/** Sticker shown in the picker; [weightKey] is what Firestore stores. */
data class ReactionStickerOption(
    val drawableRes: Int,
    val weightKey: String,
)

object ReactionStickers {
    val pickerOptions: List<ReactionStickerOption> = listOf(
        ReactionStickerOption(VerdlyAssetCatalog.RxnAwesome, ReactionWeights.AWESOME),
        ReactionStickerOption(VerdlyAssetCatalog.RxnYass, ReactionWeights.YASS),
        ReactionStickerOption(VerdlyAssetCatalog.RxnHeart, ReactionWeights.HEART),
        ReactionStickerOption(VerdlyAssetCatalog.RxnHeartPink, ReactionWeights.HEART_PINK),
        ReactionStickerOption(VerdlyAssetCatalog.RxnHeartEyes, ReactionWeights.HEART_EYES),
        ReactionStickerOption(VerdlyAssetCatalog.RxnHeartEyes2, ReactionWeights.HEART_EYES2),
        ReactionStickerOption(VerdlyAssetCatalog.RxnWow, ReactionWeights.WOW),
        ReactionStickerOption(VerdlyAssetCatalog.RxnOmg, ReactionWeights.OMG),
        ReactionStickerOption(VerdlyAssetCatalog.RxnThanks, ReactionWeights.THANKS),
        ReactionStickerOption(VerdlyAssetCatalog.RxnAppreciate, ReactionWeights.APPRECIATE),
        ReactionStickerOption(VerdlyAssetCatalog.RxnAppreciate2, ReactionWeights.APPRECIATE2),
        ReactionStickerOption(VerdlyAssetCatalog.RxnDone, ReactionWeights.DONE),
        ReactionStickerOption(VerdlyAssetCatalog.RxnPlease, ReactionWeights.PLEASE),
        ReactionStickerOption(VerdlyAssetCatalog.RxnOk, ReactionWeights.OK),
        ReactionStickerOption(VerdlyAssetCatalog.RxnSad, ReactionWeights.SAD),
        ReactionStickerOption(VerdlyAssetCatalog.RxnSadEyes, ReactionWeights.SAD_EYES),
        ReactionStickerOption(VerdlyAssetCatalog.RxnAngry, ReactionWeights.ANGRY),
        ReactionStickerOption(VerdlyAssetCatalog.RxnSuspicious, ReactionWeights.SUSPICIOUS),
    )

    private val drawableByKey: Map<String, Int> = mapOf(
        ReactionWeights.FIRE to VerdlyAssetCatalog.RxnAwesome,
        ReactionWeights.AWESOME to VerdlyAssetCatalog.RxnAwesome,
        ReactionWeights.YASS to VerdlyAssetCatalog.RxnYass,
        ReactionWeights.DONE to VerdlyAssetCatalog.RxnDone,
        ReactionWeights.HEART to VerdlyAssetCatalog.RxnHeart,
        ReactionWeights.HEART_PINK to VerdlyAssetCatalog.RxnHeartPink,
        ReactionWeights.HEART_EYES to VerdlyAssetCatalog.RxnHeartEyes,
        ReactionWeights.HEART_EYES2 to VerdlyAssetCatalog.RxnHeartEyes2,
        ReactionWeights.THANKS to VerdlyAssetCatalog.RxnThanks,
        ReactionWeights.APPRECIATE to VerdlyAssetCatalog.RxnAppreciate,
        ReactionWeights.APPRECIATE2 to VerdlyAssetCatalog.RxnAppreciate2,
        ReactionWeights.PLEASE to VerdlyAssetCatalog.RxnPlease,
        ReactionWeights.LIGHTNING to VerdlyAssetCatalog.RxnWow,
        ReactionWeights.WOW to VerdlyAssetCatalog.RxnWow,
        ReactionWeights.OMG to VerdlyAssetCatalog.RxnOmg,
        ReactionWeights.OK to VerdlyAssetCatalog.RxnOk,
        ReactionWeights.SAD to VerdlyAssetCatalog.RxnSad,
        ReactionWeights.SAD_EYES to VerdlyAssetCatalog.RxnSadEyes,
        ReactionWeights.ANGRY to VerdlyAssetCatalog.RxnAngry,
        ReactionWeights.SUSPICIOUS to VerdlyAssetCatalog.RxnSuspicious,
    )

    fun drawableFor(weightKey: String, occurrenceIndex: Int = 0): Int {
        drawableByKey[weightKey]?.let { return it }
        val fallback = pickerOptions.getOrNull(occurrenceIndex % pickerOptions.size)
        return fallback?.drawableRes ?: VerdlyAssetCatalog.RxnOk
    }

    fun primaryPickerFor(weightKey: String): Int =
        drawableByKey[weightKey] ?: VerdlyAssetCatalog.RxnOk
}

private data class FloatingSticker(val drawableRes: Int, val slot: Int)

private fun expandStickers(reactionCounts: Map<String, Int>, cap: Int): Pair<List<FloatingSticker>, Int> {
    val expanded = mutableListOf<FloatingSticker>()
    var slot = 0
    reactionCounts.forEach { (key, count) ->
        repeat(count) { i ->
            expanded += FloatingSticker(ReactionStickers.drawableFor(key, i), slot++)
        }
    }
    val overflow = (expanded.size - cap).coerceAtLeast(0)
    return expanded.take(cap) to overflow
}

/**
 * Airbuds-style stickers around a proof photo: first lands on the right,
 * then left, then top, then fills the ring. Optional full sticker picker.
 *
 * Tap expands when [onExpand] is set; long-press opens the sticker picker.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProofPhotoReactionFrame(
    modifier: Modifier = Modifier,
    reactionCounts: Map<String, Int> = emptyMap(),
    maxFloating: Int = 12,
    reactionScale: Float = 0.13f,
    enablePicker: Boolean = false,
    onReact: ((String) -> Unit)? = null,
    onExpand: (() -> Unit)? = null,
    photo: @Composable BoxScope.() -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    // Local flash until parent optimistic / Firestore counts catch up — never stacked on top
    // of an already-updated count (that double-drew stickers).
    var pendingKey by remember { mutableStateOf<String?>(null) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val minEdgePx = minOf(boxSize.width, boxSize.height).toFloat().coerceAtLeast(1f)
    val stickerPx = (minEdgePx * reactionScale).coerceIn(
        with(density) { 26.dp.toPx() },
        with(density) { 52.dp.toPx() },
    )
    LaunchedEffect(reactionCounts) {
        pendingKey = null
    }
    val mergedCounts = remember(reactionCounts, pendingKey) {
        val pk = pendingKey
        if (pk.isNullOrBlank()) reactionCounts
        else {
            val next = reactionCounts.toMutableMap()
            next[pk] = (next[pk] ?: 0) + 1
            next
        }
    }
    val (stickers, overflow) = remember(mergedCounts, maxFloating) {
        expandStickers(mergedCounts, maxFloating)
    }

    Box(
        modifier = modifier
            .onSizeChanged { boxSize = it }
            .then(
                when {
                    // Tap opens the proof; long-press opens stickers (no FlowRow — crashes on our BOM).
                    onExpand != null && enablePicker && onReact != null -> Modifier.combinedClickable(
                        onClick = { onExpand() },
                        onLongClick = { showPicker = true },
                    )
                    onExpand != null -> Modifier.clickable { onExpand() }
                    enablePicker && onReact != null -> Modifier.combinedClickable(
                        onClick = { showPicker = true },
                        onLongClick = { showPicker = true },
                    )
                    else -> Modifier
                },
            ),
    ) {
        photo()
        if (boxSize.width > 0) {
            stickers.forEachIndexed { index, sticker ->
                key("${sticker.slot}_${sticker.drawableRes}") {
                    FloatingReactionSticker(
                        drawableRes = sticker.drawableRes,
                        index = index,
                        boxWidthPx = boxSize.width.toFloat(),
                        boxHeightPx = boxSize.height.toFloat(),
                        stickerPx = stickerPx,
                        popIn = true,
                    )
                }
            }
            if (overflow > 0) {
                ReactionOverflowBubble(
                    overflow = overflow,
                    boxWidthPx = boxSize.width.toFloat(),
                    boxHeightPx = boxSize.height.toFloat(),
                )
            }
        }
        if (showPicker && onReact != null) {
            ProofReactionGridPicker(
                onSelect = { key ->
                    pendingKey = key
                    onReact(key)
                    showPicker = false
                },
                onDismiss = { showPicker = false },
            )
        }
    }
}

/** Right → left → top → bottom → diagonals. */
private val RING_ANGLES_RAD = floatArrayOf(
    0f,
    PI.toFloat(),
    (-PI / 2f).toFloat(),
    (PI / 2f).toFloat(),
    (-PI / 4f).toFloat(),
    (3f * PI / 4f).toFloat(),
    (PI / 4f).toFloat(),
    (-3f * PI / 4f).toFloat(),
)

@Composable
private fun BoxScope.FloatingReactionSticker(
    drawableRes: Int,
    index: Int,
    boxWidthPx: Float,
    boxHeightPx: Float,
    stickerPx: Float,
    popIn: Boolean = true,
) {
    val angle = RING_ANGLES_RAD[index % RING_ANGLES_RAD.size]
    val margin = stickerPx * 0.22f
    val rx = (boxWidthPx / 2f - margin - stickerPx / 2f).coerceAtLeast(stickerPx * 0.4f)
    val ry = (boxHeightPx / 2f - margin - stickerPx / 2f).coerceAtLeast(stickerPx * 0.4f)
    val cx = boxWidthPx / 2f + cos(angle) * rx
    val cy = boxHeightPx / 2f + sin(angle) * ry
    var appeared by remember { mutableStateOf(!popIn) }
    LaunchedEffect(drawableRes, index) {
        if (!popIn) {
            appeared = true
            return@LaunchedEffect
        }
        appeared = false
        delay((index % 4) * 28L)
        appeared = true
    }
    val bounce by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "sticker_bounce_$index",
    )
    if (bounce <= 0.01f) return
    Image(
        painter = painterResource(drawableRes),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.TopStart)
            .offset {
                IntOffset(
                    (cx - stickerPx / 2f).roundToInt(),
                    (cy - stickerPx / 2f).roundToInt(),
                )
            }
            .size(with(LocalDensity.current) { stickerPx.toDp() })
            .scale(0.35f + 0.85f * bounce)
            .rotate((index * 9f) - 12f),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun BoxScope.ReactionOverflowBubble(
    overflow: Int,
    boxWidthPx: Float,
    boxHeightPx: Float,
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(6.dp)
            .offset {
                IntOffset(
                    (boxWidthPx * 0.02f).roundToInt(),
                    (boxHeightPx * 0.02f).roundToInt(),
                )
            }
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Text("+$overflow", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProofReactionGridPicker(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Chunked Rows instead of FlowRow — FlowRowOverflow overload crashes at runtime
    // (NoSuchMethodError) on our Compose BOM / foundation pairing.
    val rows = remember { ReactionStickers.pickerOptions.chunked(6) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
                .clickable(enabled = false, onClick = {})
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "React",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Stickers land around the proof — right, left, then the ring.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(bottom = 4.dp),
            )
            rows.forEach { rowOptions ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    rowOptions.forEach { option ->
                        ProofReactionPickerOption(
                            option = option,
                            onSelect = onSelect,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProofReactionRadialPicker(
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProofReactionGridPicker(onSelect = onSelect, onDismiss = onDismiss, modifier = modifier)
}

@Composable
private fun ProofReactionPickerOption(
    option: ReactionStickerOption,
    onSelect: (String) -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "pick_scale",
    )
    Image(
        painter = painterResource(option.drawableRes),
        contentDescription = null,
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .clickable(interactionSource = interaction, indication = null) {
                onSelect(option.weightKey)
            }
            .padding(6.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun ReactionStickerButton(
    weightKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.88f else 1f,
        spring(stiffness = Spring.StiffnessMedium),
        label = "react_btn",
    )
    Box(
        modifier = modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(ReactionStickers.primaryPickerFor(weightKey)),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
fun ReactionStickerRow(
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReactionStickers.pickerOptions.forEach { option ->
            ReactionStickerButton(
                weightKey = option.weightKey,
                onClick = { onReact(option.weightKey) },
            )
        }
    }
}
