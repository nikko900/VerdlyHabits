package com.saintnico.verdlyhabits.ui.components.share

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import com.saintnico.verdlyhabits.engine.DuoStreakEngine
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.screens.focus.drawGrowingPlant
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

/**
 * In-app share card (400×220 dp). Image export uses [shareStreakInvite] (text + link) for maximum reliability.
 */
@Composable
fun ShareableStreakCard(
    headline: String,
    habitName: String,
    referralCode: String,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF0A1610), Color(0xFF1B4332), Color(0xFF0D1F14)),
    )
    Box(
        modifier
            .width(400.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(gradient),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(100.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawGrowingPlant(1f, isWilting = false, wiltAmount = 0f)
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            ) {
                Text(
                    headline,
                    fontFamily = frauncesFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = Color(0xFFD8F3DC),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    habitName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(0.85f),
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Built with Verdly",
                    fontSize = 11.sp,
                    color = Color.White.copy(0.45f),
                )
                Text(
                    "Join free: verdly.app/r/$referralCode",
                    fontSize = 11.sp,
                    color = Color(0xFF95D5B2),
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun referralPathCode(fullCode: String): String =
    fullCode.removePrefix("VERDLY-").uppercase()

/**
 * Shares streak / session copy with referral link. No Firebase — uses the system share sheet only.
 */
fun shareStreakCardWithImage(
    context: Context,
    headline: String,
    habitName: String,
) {
    val fullCode = ReferralManager.getReferralCode(context)
    val pathCode = referralPathCode(fullCode)
    com.saintnico.verdlyhabits.utils.StreakCardExporter.exportAndShare(
        context = context,
        headline = headline,
        habitName = habitName,
        referralCode = pathCode,
        theme = com.saintnico.verdlyhabits.utils.StreakCardTheme.EmeraldDark,
        plantProgress = 1f
    )
}

fun shareReferralInviteCard(
    context: Context,
    fullCode: String,
    shortCode: String,
    shareLink: String,
    shareText: String,
): Boolean {
    val activity = context.findActivity() ?: return false
    ShareCompat.IntentBuilder(activity)
        .setType("text/plain")
        .setText(shareText)
        .setChooserTitle("Invite friends to Verdly")
        .startChooser()
    return true
}

fun shareChallengeInviteCard(
    context: Context,
    challengeName: String,
    stake: String,
    inviteCode: String,
    challengeId: String,
    daysRemaining: Long? = null,
    memberCount: Int? = null,
) {
    val activity = context.findActivity() ?: return
    val details = buildString {
        append("Join my Verdly challenge: $challengeName")
        if (stake.isNotBlank()) append("\nStake: $stake")
        append("\nInvite code: $inviteCode")
        daysRemaining?.let { append("\n$it days left") }
        memberCount?.let { append("\n$it members already in") }
    }
    ShareCompat.IntentBuilder(activity)
        .setType("text/plain")
        .setText(details)
        .setChooserTitle("Share challenge invite")
        .startChooser()
}

fun shareGoalCompletionCard(
    context: Context,
    goalTitle: String,
    whyStatement: String,
    daysTaken: Long,
) {
    val activity = context.findActivity() ?: return
    ShareCompat.IntentBuilder(activity)
        .setType("text/plain")
        .setText(
            buildString {
                append("I completed my goal on Verdly: $goalTitle")
                if (whyStatement.isNotBlank()) append("\nWhy: $whyStatement")
                append("\nTook $daysTaken days")
            },
        )
        .setChooserTitle("Share goal completion")
        .startChooser()
}

fun shareDuoMilestoneCard(
    context: Context,
    streakDays: Int,
    buddyUsername: String,
) {
    val activity = context.findActivity() ?: return
    ShareCompat.IntentBuilder(activity)
        .setType("text/plain")
        .setText("$streakDays-day duo streak with @$buddyUsername on Verdly!")
        .setChooserTitle("Share duo milestone")
        .startChooser()
}
