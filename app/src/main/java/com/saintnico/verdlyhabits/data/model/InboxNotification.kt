package com.saintnico.verdlyhabits.data.model

enum class NotificationCategory(val label: String) {
    ALL("All"),
    CONNECTIONS("Connections"),
    ARENA("Arena"),
    ACTIVITY("Activity"),
}

enum class InboxNotificationType(val category: NotificationCategory) {
    FRIEND_REQUEST(NotificationCategory.CONNECTIONS),
    DUO_INVITE(NotificationCategory.CONNECTIONS),
    ARENA_INVITE(NotificationCategory.ARENA),
    CHALLENGE_UPDATE(NotificationCategory.ACTIVITY),
    SYSTEM(NotificationCategory.ACTIVITY),
}

data class InboxNotification(
    val id: String,
    val type: InboxNotificationType,
    val title: String,
    val body: String,
    val actorUid: String = "",
    val actorUsername: String = "",
    val actorPhotoUrl: String? = null,
    val referenceId: String = "",
    val challengeId: String? = null,
    val route: String? = null,
    val read: Boolean = false,
    val createdAtMillis: Long = 0L,
    val actionState: String = "pending",
)
