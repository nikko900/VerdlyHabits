package com.saintnico.verdlyhabits.challenge

import com.saintnico.verdlyhabits.referral.ReferralManager

object ChallengeInviteHelper {

    const val WEB_PATH = "c"

    fun normalizedCode(raw: String): String =
        raw.trim().uppercase().filter { it.isLetter() }.take(6)

    fun shareLink(inviteCode: String): String {
        val code = normalizedCode(inviteCode)
        return "https://${ReferralManager.REFERRAL_WEB_HOST}/$WEB_PATH/$code"
    }

    fun shareLinkLabel(inviteCode: String): String {
        val code = normalizedCode(inviteCode)
        return "${ReferralManager.REFERRAL_WEB_HOST}/$WEB_PATH/$code"
    }

    fun deepLink(inviteCode: String): String {
        val code = normalizedCode(inviteCode)
        return "verdly://challenge?code=$code"
    }

    fun buildShareMessage(
        challengeName: String,
        inviteCode: String,
        stake: String = "",
        daysRemaining: Long? = null,
        memberCount: Int? = null,
    ): String {
        val code = normalizedCode(inviteCode)
        val link = shareLink(code)
        return buildString {
            appendLine("Join my Verdly challenge: $challengeName")
            if (stake.isNotBlank()) appendLine("Stake: $stake")
            daysRemaining?.let { appendLine("$it days left") }
            memberCount?.let { appendLine("$it members already in") }
            appendLine()
            appendLine("Tap to join: $link")
            appendLine("Or open Verdly → Challenges → Join and enter: $code")
            append("Built with Verdly")
        }
    }

    /** Pulls a 6-letter invite code from share URLs / deep links. */
    fun extractInviteCode(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.contains("://", ignoreCase = true)) {
            try {
                val uri = android.net.Uri.parse(trimmed)
                if (uri.scheme.equals("verdly", ignoreCase = true) &&
                    uri.host.equals("challenge", ignoreCase = true)
                ) {
                    uri.getQueryParameter("code")?.let { c ->
                        val n = normalizedCode(c)
                        if (n.length == 6) return n
                    }
                }
                val segs = uri.pathSegments
                val cIdx = segs.indexOfFirst { it.equals(WEB_PATH, ignoreCase = true) }
                if (cIdx >= 0 && cIdx + 1 < segs.size) {
                    val n = normalizedCode(segs[cIdx + 1])
                    if (n.length == 6) return n
                }
            } catch (_: Exception) {
                return null
            }
        }
        val letters = normalizedCode(trimmed)
        return letters.takeIf { it.length == 6 }
    }
}
