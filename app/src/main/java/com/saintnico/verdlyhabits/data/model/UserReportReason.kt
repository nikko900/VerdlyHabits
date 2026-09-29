package com.saintnico.verdlyhabits.data.model

import androidx.annotation.DrawableRes
import com.saintnico.verdlyhabits.R

data class UserReportReason(
    val code: String,
    val title: String,
    val description: String,
    @DrawableRes val iconRes: Int,
)

object UserReportReasons {
    val all: List<UserReportReason> = listOf(
        UserReportReason(
            code = "harassment",
            title = "Harassment or bullying",
            description = "Threats, insults, or repeated hostile behavior.",
            iconRes = R.drawable.ic_phosphor_warning,
        ),
        UserReportReason(
            code = "spam_fake",
            title = "Spam or fake profile",
            description = "Bot-like activity, misleading account, or promotional spam.",
            iconRes = R.drawable.ic_phosphor_user_focus,
        ),
        UserReportReason(
            code = "inappropriate_profile",
            title = "Inappropriate profile",
            description = "Offensive photo, bio, headline, or flair content.",
            iconRes = R.drawable.ic_phosphor_image,
        ),
        UserReportReason(
            code = "challenge_cheating",
            title = "Cheating in challenges",
            description = "Fake proof, score manipulation, or unfair play.",
            iconRes = R.drawable.ic_phosphor_trophy,
        ),
        UserReportReason(
            code = "impersonation",
            title = "Impersonation",
            description = "Pretending to be someone else on Verdly.",
            iconRes = R.drawable.ic_phosphor_badge,
        ),
        UserReportReason(
            code = "unwanted_contact",
            title = "Unwanted contact",
            description = "Persistent invites or messages after being declined.",
            iconRes = R.drawable.ic_phosphor_chat,
        ),
        UserReportReason(
            code = "other",
            title = "Something else",
            description = "Another issue that does not fit the categories above.",
            iconRes = R.drawable.ic_phosphor_dots_three,
        ),
    )

    fun byCode(code: String): UserReportReason? = all.firstOrNull { it.code == code }
}
