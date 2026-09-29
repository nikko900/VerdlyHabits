package com.saintnico.verdlyhabits.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.engine.home.HomeFeedPost
import com.saintnico.verdlyhabits.ui.components.reactions.ProofPhotoReactionFrame
import com.saintnico.verdlyhabits.ui.components.coins.GroveAvatarHalo
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

private val Mint = Color(0xFF52B788)

@Composable
fun HomeFeedPostCard(
    post: HomeFeedPost,
    witnessed: Boolean,
    onOpenProfile: (String) -> Unit,
    onOpenChallenge: (String) -> Unit,
    onWitness: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (post) {
        is HomeFeedPost.Quote -> QuotePost(post, modifier)
        is HomeFeedPost.GroveWaking -> {
            Text(
                "The grove is still waking.",
                fontFamily = frauncesFamily,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = modifier.padding(vertical = 8.dp),
            )
        }
        is HomeFeedPost.DuoMoment -> PersonPost(
            uid = post.actorUid,
            username = post.username,
            photoUrl = post.photoUrl,
            streakDays = post.streakDays,
            caption = when {
                post.bothLocked -> "both locked in"
                post.atRisk -> "streak at risk"
                post.buddyDone && !post.myDone -> "finished. Your turn"
                else -> "duo update"
            },
            proofUrl = null,
            canWitness = false,
            witnessed = false,
            witnessCount = 0,
            onOpenProfile = onOpenProfile,
            onOpenMedia = { onOpenProfile(post.actorUid) },
            onWitness = {},
            modifier = modifier,
        )
        is HomeFeedPost.Planted -> PersonPost(
            uid = post.actorUid,
            username = post.username,
            photoUrl = post.photoUrl,
            streakDays = post.streakDays,
            caption = post.sharedHabit?.takeIf { it.isNotBlank() } ?: "planted",
            proofUrl = null,
            canWitness = !witnessed,
            witnessed = witnessed,
            witnessCount = post.witnessCount + if (witnessed && post.witnessCount == 0) 1 else 0,
            glowId = post.titleGlowId,
            frameId = post.profileFrameId,
            onOpenProfile = onOpenProfile,
            onOpenMedia = { onOpenProfile(post.actorUid) },
            onWitness = { onWitness(post.actorUid) },
            modifier = modifier,
        )
        is HomeFeedPost.ProofPhoto -> PersonPost(
            uid = post.actorUid,
            username = post.username,
            photoUrl = post.photoUrl,
            streakDays = post.streakDays,
            caption = post.habitName,
            proofUrl = post.proofUrl,
            proofReactionCounts = post.reactionCounts,
            canWitness = false,
            witnessed = false,
            witnessCount = 0,
            onOpenProfile = onOpenProfile,
            onOpenMedia = { onOpenChallenge(post.challengeId) },
            onWitness = {},
            modifier = modifier,
        )
        is HomeFeedPost.ChallengeDone -> PersonPost(
            uid = post.actorUid,
            username = post.username,
            photoUrl = post.photoUrl,
            streakDays = 0,
            caption = "finished ${post.habitName}",
            proofUrl = null,
            canWitness = false,
            witnessed = false,
            witnessCount = 0,
            onOpenProfile = onOpenProfile,
            onOpenMedia = { onOpenChallenge(post.challengeId) },
            onWitness = {},
            modifier = modifier,
        )
        is HomeFeedPost.GroveLead -> PersonPost(
            uid = post.actorUid,
            username = post.username,
            photoUrl = post.photoUrl,
            streakDays = 0,
            caption = "grove lead this week",
            proofUrl = null,
            canWitness = false,
            witnessed = false,
            witnessCount = 0,
            onOpenProfile = onOpenProfile,
            onOpenMedia = { onOpenProfile(post.actorUid) },
            onWitness = {},
            modifier = modifier,
        )
    }
}

@Composable
private fun QuotePost(post: HomeFeedPost.Quote, modifier: Modifier = Modifier) {
    Text(
        post.text,
        fontFamily = frauncesFamily,
        fontStyle = FontStyle.Italic,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.78f),
        modifier = modifier.padding(vertical = 10.dp, horizontal = 4.dp),
    )
}

@Composable
private fun PersonPost(
    uid: String,
    username: String,
    photoUrl: String?,
    streakDays: Int,
    caption: String,
    proofUrl: String?,
    proofReactionCounts: Map<String, Int> = emptyMap(),
    canWitness: Boolean,
    witnessed: Boolean,
    witnessCount: Int,
    onOpenProfile: (String) -> Unit,
    onOpenMedia: () -> Unit,
    onWitness: () -> Unit,
    modifier: Modifier = Modifier,
    glowId: String? = null,
    frameId: String? = null,
) {
    val heartScale by animateFloatAsState(
        targetValue = if (witnessed) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = 700f),
        label = "heart",
    )
    Column(modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenProfile(uid) }
                .padding(vertical = 6.dp),
        ) {
            GroveAvatarHalo(glowId = glowId, frameId = frameId, size = 34.dp) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = username,
                        modifier = Modifier.size(34.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        username.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Mint,
                        fontSize = 13.sp,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "@$username",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (streakDays > 1) {
                Text(
                    " · ${streakDays}d streak",
                    fontFamily = dmSansFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "today",
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
        }
        if (!proofUrl.isNullOrBlank()) {
            ProofPhotoReactionFrame(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .aspectRatio(4f / 5f)
                    .pointerInput(canWitness, witnessed) {
                        detectTapGestures(
                            onDoubleTap = { if (canWitness) onWitness() },
                            onTap = { onOpenMedia() },
                        )
                    },
                reactionCounts = proofReactionCounts,
                reactionScale = 0.12f,
                maxFloating = 8,
            ) {
                AsyncImage(
                    model = proofUrl,
                    contentDescription = caption,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            Text(
                caption,
                fontFamily = dmSansFamily,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 4.dp)
                    .pointerInput(canWitness, witnessed) {
                        detectTapGestures(
                            onDoubleTap = { if (canWitness) onWitness() },
                            onTap = { onOpenMedia() },
                        )
                    },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (canWitness || witnessed) {
                IconButton(
                    onClick = { if (canWitness) onWitness() },
                    enabled = canWitness,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        if (witnessed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Saw you",
                        tint = if (witnessed) Mint else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        modifier = Modifier
                            .size(22.dp)
                            .graphicsLayer { scaleX = heartScale; scaleY = heartScale },
                    )
                }
            }
            Column(Modifier.padding(start = 2.dp)) {
                Text(
                    "@$username $caption",
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (witnessCount > 0) {
                    Text(
                        "$witnessCount saw this",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}
