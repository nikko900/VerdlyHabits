package com.saintnico.verdlyhabits.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.remote.firestore.AccountabilityRepository
import com.saintnico.verdlyhabits.data.remote.firestore.ChallengeJoinRequestRepository
import com.saintnico.verdlyhabits.data.remote.firestore.FriendRepository
import com.saintnico.verdlyhabits.data.remote.firestore.InboxRepository
import com.saintnico.verdlyhabits.widget.WidgetNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Accept / decline from the notification shade — same actions as the in-app Activity hub.
 */
class SocialNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext
        val notificationId = intent.getIntExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, 0)
        val referenceId = intent.getStringExtra(EXTRA_REFERENCE_ID).orEmpty()
        val inboxId = intent.getStringExtra(EXTRA_INBOX_ID).orEmpty().ifBlank { referenceId }
        val actorUid = intent.getStringExtra(EXTRA_ACTOR_UID).orEmpty()
        val actorName = intent.getStringExtra(EXTRA_ACTOR_NAME).orEmpty()
        val actorPhotoUrl = intent.getStringExtra(EXTRA_ACTOR_PHOTO)
        val challengeId = intent.getStringExtra(EXTRA_CHALLENGE_ID).orEmpty()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid.isNullOrBlank() || referenceId.isBlank()) return@launch
                val inbox = InboxRepository()
                val ok = when (action) {
                    ACTION_ACCEPT_FRIEND -> FriendRepository().acceptRequest(referenceId).isSuccess
                    ACTION_DECLINE_FRIEND -> FriendRepository().declineRequest(referenceId).isSuccess
                    ACTION_ACCEPT_DUO -> AccountabilityRepository().acceptInvite(referenceId).isSuccess
                    ACTION_DECLINE_DUO -> AccountabilityRepository().declineInvite(referenceId).isSuccess
                    ACTION_APPROVE_ARENA ->
                        ChallengeJoinRequestRepository().approveRequest(referenceId).isSuccess
                    ACTION_DECLINE_ARENA ->
                        ChallengeJoinRequestRepository().declineRequest(referenceId).isSuccess
                    else -> false
                }
                val accepted = action in setOf(
                    ACTION_ACCEPT_FRIEND,
                    ACTION_ACCEPT_DUO,
                    ACTION_APPROVE_ARENA,
                )
                inbox.updateActionState(uid, inboxId, if (accepted) "accepted" else "declined")
                if (notificationId != 0) NotificationHelper.cancel(app, notificationId)
                if (ok && accepted) {
                    val confirm = when (action) {
                        ACTION_ACCEPT_FRIEND ->
                            "You're now connected" to "You and ${actorName.ifBlank { "your rival" }} are friends."
                        ACTION_ACCEPT_DUO ->
                            "Duo streak started" to "You and ${actorName.ifBlank { "your buddy" }} are linked."
                        ACTION_APPROVE_ARENA ->
                            "They're in" to "${actorName.ifBlank { "A rival" }} joined your arena."
                        else -> null
                    }
                    if (confirm != null) {
                        val extras = when (action) {
                            ACTION_ACCEPT_DUO -> mapOf("open_duo" to true)
                            ACTION_APPROVE_ARENA -> mapOf(
                                WidgetNavigation.EXTRA_ROUTE to WidgetNavigation.ROUTE_CHALLENGES,
                                WidgetNavigation.EXTRA_CHALLENGE_ID to challengeId,
                            )
                            else -> mapOf("open_notifications" to true)
                        }
                        NotificationHelper.showPersonNotification(
                            context = app,
                            channelId = VerdlyFcmService.CHANNEL_CONNECTIONS,
                            channelName = "Connections",
                            title = confirm.first,
                            body = confirm.second,
                            type = "confirm",
                            actorUid = actorUid,
                            actorName = actorName,
                            actorPhotoUrl = actorPhotoUrl,
                            referenceId = "confirm_$referenceId",
                            challengeId = challengeId,
                            intentExtras = extras,
                            vibrationEnabled = false,
                            soundEnabled = false,
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_ACCEPT_FRIEND = "com.saintnico.verdlyhabits.NOTIF_ACCEPT_FRIEND"
        const val ACTION_DECLINE_FRIEND = "com.saintnico.verdlyhabits.NOTIF_DECLINE_FRIEND"
        const val ACTION_ACCEPT_DUO = "com.saintnico.verdlyhabits.NOTIF_ACCEPT_DUO"
        const val ACTION_DECLINE_DUO = "com.saintnico.verdlyhabits.NOTIF_DECLINE_DUO"
        const val ACTION_APPROVE_ARENA = "com.saintnico.verdlyhabits.NOTIF_APPROVE_ARENA"
        const val ACTION_DECLINE_ARENA = "com.saintnico.verdlyhabits.NOTIF_DECLINE_ARENA"

        const val EXTRA_TYPE = "type"
        const val EXTRA_REFERENCE_ID = "reference_id"
        const val EXTRA_INBOX_ID = "inbox_id"
        const val EXTRA_ACTOR_UID = "actor_uid"
        const val EXTRA_ACTOR_NAME = "actor_name"
        const val EXTRA_ACTOR_PHOTO = "actor_photo"
        const val EXTRA_CHALLENGE_ID = "challenge_id"
    }
}
