@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saintnico.verdlyhabits.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.engine.LastActive
import com.saintnico.verdlyhabits.ui.components.coins.GroveAvatarHalo
import com.saintnico.verdlyhabits.ui.components.social.LastActiveAvatarWrap
import com.saintnico.verdlyhabits.ui.theme.aboveSystemNav
import com.saintnico.verdlyhabits.ui.theme.cappedSp
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.GroveMember
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val Mint = Color(0xFF52B788)
private val Gold = Color(0xFFFFC857)

@Composable
fun GrovePulseSheet(
    member: GroveMember,
    witnessed: Boolean,
    completedToday: List<String>,
    onWitness: () -> Unit,
    onOpenProfile: () -> Unit,
    onShareHabit: (String) -> Unit,
    onClearShare: () -> Unit,
    modifier: Modifier = Modifier,
    onNudge: (() -> Unit)? = null,
    viewerSharesLastActive: Boolean = true,
) {
    val planted = member.plantedToday
    val story = groveStory(member)
    val onBg = MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .aboveSystemNav()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LastActiveAvatarWrap(
            snapshot = LastActive.resolve(
                viewerShares = viewerSharesLastActive,
                targetShares = member.shareLastActive,
                lastActiveAt = member.lastActiveAt,
                isSelf = member.isSelf,
            ),
            compact = true,
            dotSize = 10.dp,
        ) {
            GroveAvatarHalo(
                glowId = member.titleGlowId,
                frameId = member.profileFrameId,
                size = 76.dp,
            ) {
            val photo = member.photoUrl
            if (!photo.isNullOrBlank()) {
                AsyncImage(
                    model = photo,
                    contentDescription = member.username,
                    modifier = Modifier.size(76.dp),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    Modifier.size(76.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        member.username.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Mint,
                        fontSize = cappedSp(26f),
                    )
                }
            }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (member.isSelf) "You" else "@${member.username}",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = cappedSp(22f),
                color = onBg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (member.membershipTier.showsPremiumBadge) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "PRO",
                    fontSize = cappedSp(10f),
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
            }
            if (member.groveLead) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.Spa, contentDescription = "Grove lead", tint = Mint, modifier = Modifier.size(16.dp))
            }
        }
        LastActive.resolve(
            viewerShares = viewerSharesLastActive,
            targetShares = member.shareLastActive,
            lastActiveAt = member.lastActiveAt,
            isSelf = member.isSelf,
        )?.let { snap ->
            Text(
                snap.profileLabel,
                fontFamily = dmSansFamily,
                fontSize = cappedSp(12f),
                color = if (snap.online) Color(0xFF2BD67B) else onBg.copy(alpha = 0.5f),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        member.equippedTitleLabel?.takeIf { it.isNotBlank() }?.let { title ->
            Text(
                title,
                fontFamily = dmSansFamily,
                fontSize = cappedSp(12f),
                color = Gold.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        member.pinnedTrophyLabel?.let { pin ->
            Text(
                pin,
                fontFamily = dmSansFamily,
                fontSize = cappedSp(12f),
                color = Gold,
                modifier = Modifier.padding(top = 2.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(16.dp))
        GroveStoryCard(headline = story.first, body = story.second, planted = planted)

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PulseStat(modifier = Modifier.weight(1f), value = "${member.currentStreakPublic}", label = "Streak")
            PulseStat(modifier = Modifier.weight(1f), value = "${member.weeklyPlantDays}/7", label = "This week")
            PulseStat(
                modifier = Modifier.weight(1f),
                value = if (member.plantedCountToday > 0) "${member.plantedCountToday}" else if (planted) "1" else "0",
                label = "Today",
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PulseStat(modifier = Modifier.weight(1f), value = "${member.witnessCount}", label = "Saw this")
            PulseStat(
                modifier = Modifier.weight(1f),
                value = if (member.currentStreakPublic >= 7) "Rooted" else if (planted) "Lit" else "Quiet",
                label = "Pulse",
            )
            PulseStat(
                modifier = Modifier.weight(1f),
                value = formatPlantDay(member.lastPlantedDay) ?: if (planted) "Today" else "None",
                label = "Last plant",
            )
        }

        Spacer(Modifier.height(16.dp))
        WeekRhythmRow(days = member.weeklyPlantDays.coerceIn(0, 7))

        if (!member.sharedHabitTitle.isNullOrBlank() && planted) {
            Spacer(Modifier.height(14.dp))
            SharedHabitBanner(title = member.sharedHabitTitle)
        }

        if (member.isSelf) {
            Spacer(Modifier.height(18.dp))
            if (!planted) {
                Text(
                    "Complete a habit to light your ring. Friends only see a habit if you share it.",
                    fontFamily = dmSansFamily,
                    fontSize = cappedSp(13f),
                    color = onBg.copy(alpha = 0.55f),
                )
            } else if (completedToday.isNotEmpty()) {
                Text(
                    "Share with the grove",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = cappedSp(15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                Text(
                    "Optional. Friends see the title. Nothing else.",
                    fontFamily = dmSansFamily,
                    fontSize = cappedSp(12f),
                    color = onBg.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(completedToday, key = { it }) { title ->
                        val selected = member.sharedHabitTitle == title
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) onClearShare() else onShareHabit(title)
                            },
                            label = { Text(title, fontSize = cappedSp(13f), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Mint.copy(alpha = 0.18f),
                                selectedLabelColor = Mint,
                            ),
                        )
                    }
                }
            }
        } else {
            Spacer(Modifier.height(18.dp))
            if (planted) {
                Button(
                    onClick = onWitness,
                    enabled = !witnessed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint),
                ) {
                    Icon(
                        if (witnessed) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (witnessed) "You saw this" else "Saw you", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (onNudge != null) {
                Button(
                    onClick = onNudge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A45)),
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (planted) "Send a note" else "Nudge them", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
            OutlinedButton(
                onClick = onOpenProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("View profile", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun GroveStoryCard(headline: String, body: String, planted: Boolean) {
    val accent = if (planted) Mint else Color(0xFF7B6CF6)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.05f)),
                ),
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (planted) Icons.Rounded.LocalFireDepartment else Icons.Rounded.Star,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                headline,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = cappedSp(16f),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            fontFamily = dmSansFamily,
            fontSize = cappedSp(13f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
            lineHeight = cappedSp(18f),
        )
    }
}

@Composable
private fun SharedHabitBanner(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Mint.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🌱", fontSize = cappedSp(16f))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Shared today",
                fontFamily = dmSansFamily,
                fontSize = cappedSp(11f),
                color = Mint,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                title,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = cappedSp(15f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WeekRhythmRow(days: Int) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "This week's rhythm",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = cappedSp(13f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(7) { index ->
                val filled = index < days
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            if (filled) Mint else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        ),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (days >= 5) "On pace. Protect this week."
            else if (days >= 1) "$days of 7 days planted. There's still room."
            else "No plants yet this week. One tiny one counts.",
            fontFamily = dmSansFamily,
            fontSize = cappedSp(12f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.48f),
        )
    }
}

@Composable
private fun PulseStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = cappedSp(15f),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            fontSize = cappedSp(11f),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun groveStory(member: GroveMember): Pair<String, String> {
    val name = if (member.isSelf) "You" else member.username
    val planted = member.plantedToday
    val shared = member.sharedHabitTitle
    return when {
        planted && !shared.isNullOrBlank() ->
            "Just planted" to "$name showed the grove \"$shared\". That's the kind of honesty that keeps people coming back."
        planted && member.plantedCountToday > 1 ->
            "On a roll today" to "$name planted ${member.plantedCountToday} habits today. The grove felt that."
        planted && member.currentStreakPublic >= 7 ->
            "${member.currentStreakPublic} days rooted" to "$name showed up again. This is identity, not a mood."
        planted ->
            "Showed up today" to "$name lit their ring. A witness from you makes it land."
        member.currentStreakPublic >= 7 ->
            "Streak is live" to "$name is ${member.currentStreakPublic} days in, and still hasn't planted today. A nudge now is gold."
        member.weeklyPlantDays >= 4 ->
            "Strong week so far" to "$name has ${member.weeklyPlantDays} of 7 days. One more plant keeps the week honest."
        else ->
            "Still growing" to if (member.isSelf) {
                "Your grove is waiting. Plant one tiny habit and the ring lights up."
            } else {
                "$name hasn't planted yet. A short nudge beats a silent grove."
            }
    }
}

private fun formatPlantDay(day: String?): String? {
    if (day.isNullOrBlank()) return null
    return try {
        LocalDate.parse(day).format(DateTimeFormatter.ofPattern("EEE d MMM"))
    } catch (_: DateTimeParseException) {
        day
    }
}
