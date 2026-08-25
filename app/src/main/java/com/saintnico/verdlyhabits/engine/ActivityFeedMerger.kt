package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.data.model.DevAnnouncement
import com.saintnico.verdlyhabits.data.model.InboxNotification
import com.saintnico.verdlyhabits.data.model.InboxNotificationType

/**
 * Unifies profile Live Pulse rows with Activity inbox items and dev announcements.
 */
object ActivityFeedMerger {

    fun merge(
        localPulse: List<ProfileSocialEngine.ProfileActivityItem>,
        inbox: List<InboxNotification>,
        announcements: List<DevAnnouncement> = emptyList(),
        dismissedAnnouncementIds: Set<String> = emptySet(),
    ): List<ProfileSocialEngine.ProfileActivityItem> {
        val activityInbox = inbox.filter {
            it.type == InboxNotificationType.CHALLENGE_UPDATE ||
                it.type == InboxNotificationType.SYSTEM
        }
        val inboxChallengeIds = activityInbox.mapNotNull { it.challengeId?.takeIf { id -> id.isNotBlank() } }.toSet()

        val filteredLocal = localPulse.filter { item ->
            if (item.id.startsWith("rank_") || item.id.startsWith("lead_")) {
                val chId = localChallengeIdFromItemId(item.id)
                chId == null || chId !in inboxChallengeIds
            } else {
                true
            }
        }

        val fromInbox = activityInbox.map { it.toPulseItem() }
        val fromAnnouncements = announcements
            .filter { it.id !in dismissedAnnouncementIds }
            .map { it.toPulseItem() }

        return (fromInbox + fromAnnouncements + filteredLocal)
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
    }

    fun activityInboxItems(inbox: List<InboxNotification>): List<InboxNotification> =
        inbox.filter {
            it.type == InboxNotificationType.CHALLENGE_UPDATE || it.type == InboxNotificationType.SYSTEM
        }

    private fun localChallengeIdFromItemId(id: String): String? = when {
        id.startsWith("rank_") -> id.removePrefix("rank_")
        id.startsWith("lead_") -> id.removePrefix("lead_")
        id.startsWith("done_") -> id.removePrefix("done_").substringBeforeLast("_")
        else -> null
    }

    private fun InboxNotification.toPulseItem(): ProfileSocialEngine.ProfileActivityItem {
        val lower = body.lowercase()
        val accent = when {
            lower.contains("overtook") || lower.contains("trailing") -> ProfileSocialEngine.ActivityAccent.RANK
            lower.contains("took the lead") || lower.contains("#1") -> ProfileSocialEngine.ActivityAccent.SUCCESS
            lower.contains("duo") -> ProfileSocialEngine.ActivityAccent.DUO
            lower.contains("react") || lower.contains("proof") -> ProfileSocialEngine.ActivityAccent.NEUTRAL
            type == InboxNotificationType.SYSTEM -> ProfileSocialEngine.ActivityAccent.SUCCESS
            else -> ProfileSocialEngine.ActivityAccent.NEUTRAL
        }
        val message = when {
            body.isNotBlank() -> body
            title.isNotBlank() -> title
            else -> "Activity update"
        }
        return ProfileSocialEngine.ProfileActivityItem(
            id = "inbox_$id",
            message = message,
            timestamp = createdAtMillis.coerceAtLeast(1L),
            actorUsername = actorUsername.takeIf { it.isNotBlank() },
            accent = accent,
        )
    }

    private fun DevAnnouncement.toPulseItem(): ProfileSocialEngine.ProfileActivityItem =
        ProfileSocialEngine.ProfileActivityItem(
            id = "announcement_$id",
            message = if (body.isNotBlank()) "$title — $body" else title,
            timestamp = createdAtMillis.coerceAtLeast(1L),
            accent = ProfileSocialEngine.ActivityAccent.SUCCESS,
        )
}
