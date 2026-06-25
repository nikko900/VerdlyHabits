package com.saintnico.verdlyhabits.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.saintnico.verdlyhabits.R

class LeaderboardGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotStore(context).readLeaderboard()
        provideContent {
            GlanceTheme {
                LeaderboardWidgetContent(context, snapshot)
            }
        }
    }
}

class LeaderboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LeaderboardGlanceWidget()
}

@androidx.compose.runtime.Composable
private fun LeaderboardWidgetContent(context: Context, snapshot: LeaderboardWidgetSnapshot) {
    val openApp = WidgetNavigation.openAppIntent(
        context,
        WidgetNavigation.ROUTE_CHALLENGES,
        snapshot.challengeId.takeIf { it.isNotBlank() },
    )
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(R.drawable.widget_leaderboard_bright_bg))
            .cornerRadius(22.dp)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)
            .clickable(actionStartActivity(openApp)),
    ) {
        LeaderboardHeroHeader(context, snapshot.challengeTitle)

        Spacer(GlanceModifier.height(4.dp))

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(ImageProvider(R.drawable.widget_card_glass))
                .cornerRadius(16.dp)
                .padding(6.dp),
        ) {
            if (snapshot.rows.isEmpty()) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_trophy_header),
                        contentDescription = null,
                        modifier = GlanceModifier.size(32.dp),
                    )
                    Spacer(GlanceModifier.height(6.dp))
                    Text(
                        text = context.getString(R.string.widget_leaderboard_empty),
                        style = TextStyle(
                            color = VerdlyWidgetTheme.color(Color.White),
                            fontSize = 12.sp,
                        ),
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    snapshot.rows.forEachIndexed { index, row ->
                        if (index > 0) Spacer(GlanceModifier.height(3.dp))
                        PremiumLeaderboardRow(row)
                    }
                }
            }
        }

        if (snapshot.myRank > 0) {
            Spacer(GlanceModifier.height(4.dp))
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ImageProvider(R.drawable.widget_footer_rank_bg))
                    .cornerRadius(12.dp)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_trophy_header),
                    contentDescription = null,
                    modifier = GlanceModifier.size(16.dp),
                )
                Spacer(GlanceModifier.width(8.dp))
                Text(
                    text = context.getString(R.string.widget_leaderboard_you_rank, snapshot.myRank),
                    style = TextStyle(
                        color = VerdlyWidgetTheme.color(VerdlyWidgetTheme.Gold),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun VerdlyWordmarkBadge() {
    Box(
        modifier = GlanceModifier
            .background(ImageProvider(R.drawable.widget_verdly_logo_badge_bg))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Verdly",
            style = TextStyle(
                color = VerdlyWidgetTheme.color(Color.White),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
private fun LeaderboardHeroHeader(context: Context, challengeTitle: String) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End,
        ) {
            VerdlyWordmarkBadge()
        }
        Spacer(GlanceModifier.height(3.dp))
        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .background(ImageProvider(R.drawable.widget_live_badge_bg))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = context.getString(R.string.widget_leaderboard_live_badge),
                    style = TextStyle(
                        color = VerdlyWidgetTheme.color(Color(0xFF39FF14)),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.height(3.dp))
        Text(
            text = challengeTitle.ifBlank {
                context.getString(R.string.widget_leaderboard_subtitle_empty)
            },
            style = TextStyle(
                color = VerdlyWidgetTheme.color(Color.White),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
        )
        Text(
            text = context.getString(R.string.widget_leaderboard_hero_subtitle),
            style = TextStyle(
                color = VerdlyWidgetTheme.color(Color(0xE6FFFFFF)),
                fontSize = 10.sp,
            ),
        )
    }
}

@androidx.compose.runtime.Composable
private fun PremiumLeaderboardRow(row: LeaderboardWidgetRow) {
    val rowBgRes = when (row.rank) {
        1 -> R.drawable.widget_row_rank1_bg
        2 -> R.drawable.widget_row_rank2_bg
        3 -> R.drawable.widget_row_rank3_bg
        else -> R.drawable.widget_card_glass
    }
    val rankBgRes = when (row.rank) {
        1 -> R.drawable.widget_rank_circle_gold
        2 -> R.drawable.widget_rank_circle_silver
        3 -> R.drawable.widget_rank_circle_bronze
        else -> R.drawable.widget_rank_circle_default
    }
    val rankTextColor = if (row.rank <= 3) Color(0xFF111410) else Color.White
    val nameColor = when (row.rank) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> Color.White
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(ImageProvider(rowBgRes))
            .cornerRadius(12.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .size(20.dp)
                .background(ImageProvider(rankBgRes))
                .cornerRadius(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${row.rank}",
                style = TextStyle(
                    color = ColorProvider(rankTextColor),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        Spacer(GlanceModifier.width(6.dp))

        Box(modifier = GlanceModifier.defaultWeight()) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = row.name,
                    style = TextStyle(
                        color = ColorProvider(nameColor),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                    ),
                    maxLines = 1,
                )
                if (row.isMe) {
                    Spacer(GlanceModifier.width(4.dp))
                    Box(
                        modifier = GlanceModifier
                            .background(ImageProvider(R.drawable.widget_you_chip_bg))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    ) {
                        Text(
                            text = "YOU",
                            style = TextStyle(
                                color = VerdlyWidgetTheme.color(Color(0xFF39FF14)),
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }

        Row(
            modifier = GlanceModifier
                .background(ImageProvider(R.drawable.widget_streak_pill_bg))
                .cornerRadius(14.dp)
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_fire_streak),
                contentDescription = null,
                modifier = GlanceModifier.size(12.dp),
            )
            Spacer(GlanceModifier.width(3.dp))
            Text(
                text = if (row.scoreLabel == "streak") "${row.streak}" else "${row.score} pts",
                style = TextStyle(
                    color = VerdlyWidgetTheme.color(VerdlyWidgetTheme.Gold),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }

        if (row.doneToday) {
            Spacer(GlanceModifier.width(4.dp))
            Image(
                provider = ImageProvider(R.drawable.ic_widget_check_done),
                contentDescription = null,
                modifier = GlanceModifier.size(14.dp),
            )
        }
    }
}
