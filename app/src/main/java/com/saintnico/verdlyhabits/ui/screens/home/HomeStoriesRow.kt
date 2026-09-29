package com.saintnico.verdlyhabits.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.R
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.engine.LastActive
import com.saintnico.verdlyhabits.ui.components.coins.GroveAvatarHalo
import com.saintnico.verdlyhabits.ui.components.social.LastActiveAvatarWrap
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.GroveMember

private val Mint = Color(0xFF52B788)

enum class StoryRing { Empty, Unseen, Seen, Dim }

@Composable
fun HomeStoriesRow(
    me: GroveMember?,
    friends: List<GroveMember>,
    witnessedToday: Set<String>,
    pulseCopy: String,
    onSelfTap: () -> Unit,
    onFriendTap: (String) -> Unit,
    onInviteTap: () -> Unit,
    modifier: Modifier = Modifier,
    viewerSharesLastActive: Boolean = true,
) {
    val ordered = friends.filter { !it.isSelf }
        .sortedWith(
            compareBy<GroveMember> { member ->
                when {
                    member.plantedToday && member.uid !in witnessedToday -> 0
                    member.plantedToday -> 1
                    else -> 2
                }
            }.thenBy { it.username.lowercase() },
        )
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Grove",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                pulseCopy,
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            item(key = "story_self") {
                StoryCircle(
                    member = me,
                    label = "You",
                    ring = if (me?.plantedToday == true) {
                        if (me.sharedHabitTitle.isNullOrBlank()) StoryRing.Seen else StoryRing.Unseen
                    } else {
                        StoryRing.Empty
                    },
                    onClick = onSelfTap,
                    viewerSharesLastActive = viewerSharesLastActive,
                )
            }
            items(ordered, key = { it.uid }) { member ->
                val ring = when {
                    member.plantedToday && member.uid !in witnessedToday -> StoryRing.Unseen
                    member.plantedToday -> StoryRing.Seen
                    else -> StoryRing.Dim
                }
                StoryCircle(
                    member = member,
                    label = member.username,
                    ring = ring,
                    onClick = { onFriendTap(member.uid) },
                    viewerSharesLastActive = viewerSharesLastActive,
                )
            }
            item(key = "story_invite") {
                InviteCircle(onClick = onInviteTap)
            }
        }
    }
}

@Composable
private fun StoryCircle(
    member: GroveMember?,
    label: String,
    ring: StoryRing,
    onClick: () -> Unit,
    viewerSharesLastActive: Boolean = true,
) {
    val dim = ring == StoryRing.Dim
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(58.dp)
            .alpha(if (dim) 0.45f else 1f)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
            RingStroke(ring)
            LastActiveAvatarWrap(
                snapshot = member?.let {
                    LastActive.resolve(
                        viewerShares = viewerSharesLastActive,
                        targetShares = it.shareLastActive,
                        lastActiveAt = it.lastActiveAt,
                        isSelf = it.isSelf,
                    )
                },
                compact = true,
                dotSize = 8.dp,
            ) {
                GroveAvatarHalo(
                    glowId = member?.titleGlowId,
                    frameId = member?.profileFrameId,
                    size = 44.dp,
                ) {
                val photo = member?.photoUrl
                if (!photo.isNullOrBlank()) {
                    AsyncImage(
                        model = photo,
                        contentDescription = label,
                        modifier = Modifier.size(44.dp),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier
                            .size(44.dp)
                            .border(0.dp, Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Mint,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontFamily = dmSansFamily,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (dim) 0.5f else 0.82f),
        )
    }
}

@Composable
private fun InviteCircle(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(58.dp)
            .clickable(onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(54.dp)) {
            Canvas(Modifier.size(54.dp)) {
                drawCircle(
                    color = Mint.copy(alpha = 0.55f),
                    radius = size.minDimension / 2f - 1.5.dp.toPx(),
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()), 0f),
                    ),
                )
            }
            Icon(
                Icons.Rounded.Add,
                contentDescription = stringResource(R.string.invite),
                tint = Mint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.invite),
            fontFamily = dmSansFamily,
            fontSize = 10.sp,
            maxLines = 1,
            color = Mint,
        )
    }
}

@Composable
private fun RingStroke(ring: StoryRing) {
    val color = when (ring) {
        StoryRing.Empty -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.28f)
        StoryRing.Unseen -> Mint
        StoryRing.Seen -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.22f)
        StoryRing.Dim -> Color.Transparent
    }
    Canvas(Modifier.size(54.dp)) {
        if (ring == StoryRing.Dim) return@Canvas
        val stroke = Stroke(
            width = 2.5.dp.toPx(),
            pathEffect = if (ring == StoryRing.Empty) {
                PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx()), 0f)
            } else null,
        )
        drawCircle(
            color = color,
            radius = size.minDimension / 2f - 1.5.dp.toPx(),
            center = Offset(size.width / 2f, size.height / 2f),
            style = stroke,
        )
    }
}
