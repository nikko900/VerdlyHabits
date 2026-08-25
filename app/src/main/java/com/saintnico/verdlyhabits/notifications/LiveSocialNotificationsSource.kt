package com.saintnico.verdlyhabits.notifications

import com.saintnico.verdlyhabits.data.model.InboxNotification
import com.saintnico.verdlyhabits.data.model.InboxNotificationType
import com.saintnico.verdlyhabits.data.remote.firestore.AccountabilityRepository
import com.saintnico.verdlyhabits.data.remote.firestore.ChallengeJoinRequestRepository
import com.saintnico.verdlyhabits.data.remote.firestore.FriendRepository
import com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * Builds in-app notifications directly from live Firestore request collections.
 * This is the source of truth when inbox mirror writes fail or rules are not deployed yet.
 */
class LiveSocialNotificationsSource(
    private val friendRepository: FriendRepository = FriendRepository(),
    private val accountabilityRepository: AccountabilityRepository = AccountabilityRepository(),
    private val joinRequestRepository: ChallengeJoinRequestRepository = ChallengeJoinRequestRepository(),
    private val userRepository: UserRepository = UserRepository(),
) {
    fun observe(uid: String): Flow<List<InboxNotification>> {
        if (uid.isBlank()) return flowOf(emptyList())
        return combine(
            friendRepository.observeIncomingRequests(uid),
            accountabilityRepository.observeActivePair(uid),
            joinRequestRepository.observePendingForHost(uid),
        ) { friendRequests, duoState, arenaRequests ->
            Triple(friendRequests, duoState, arenaRequests)
        }.flatMapLatest { (friendRequests, duoState, arenaRequests) ->
            flow {
                val items = mutableListOf<InboxNotification>()
                for (req in friendRequests) {
                    val profile = userRepository.fetchPublicProfile(req.fromUid)
                    val username = profile?.username?.ifBlank { profile.displayName }
                        ?.ifBlank { "rival" } ?: "rival"
                    items += InboxNotification(
                        id = req.id,
                        type = InboxNotificationType.FRIEND_REQUEST,
                        title = "New friend request",
                        body = "@$username wants to connect on Verdly",
                        actorUid = req.fromUid,
                        actorUsername = username,
                        actorPhotoUrl = profile?.photoUrl,
                        referenceId = req.id,
                        actionState = "pending",
                    )
                }
                if (duoState?.isIncomingInvite == true && duoState.pairId.isNotBlank()) {
                    val username = duoState.buddyUsername.ifBlank { "rival" }
                    items += InboxNotification(
                        id = duoState.pairId,
                        type = InboxNotificationType.DUO_INVITE,
                        title = "Accountability buddy invite",
                        body = "@$username invited you to a duo streak",
                        actorUid = duoState.buddyUid,
                        actorUsername = username,
                        actorPhotoUrl = duoState.buddyPhotoUrl,
                        referenceId = duoState.pairId,
                        actionState = "pending",
                    )
                }
                for (req in arenaRequests) {
                    val username = req.fromUsername.ifBlank { "rival" }
                    items += InboxNotification(
                        id = req.id,
                        type = InboxNotificationType.ARENA_INVITE,
                        title = "Arena join request",
                        body = "@$username wants to join \"${req.challengeTitle}\"",
                        actorUid = req.fromUid,
                        actorUsername = username,
                        referenceId = req.id,
                        challengeId = req.challengeId,
                        actionState = "pending",
                    )
                }
                emit(items)
            }
        }
    }
}

fun mergeInboxWithLive(
    inbox: List<InboxNotification>,
    live: List<InboxNotification>,
): List<InboxNotification> {
    val merged = linkedMapOf<String, InboxNotification>()
    inbox.forEach { item ->
        merged["${item.type.name}:${item.referenceId}"] = item
    }
    live.forEach { item ->
        val key = "${item.type.name}:${item.referenceId}"
        val existing = merged[key]
        if (existing == null || existing.actionState != "pending") {
            merged[key] = item.copy(
                read = existing?.read ?: false,
                createdAtMillis = maxOf(existing?.createdAtMillis ?: 0L, item.createdAtMillis),
            )
        }
    }
    return merged.values.sortedWith(
        compareByDescending<InboxNotification> { it.actionState == "pending" }
            .thenByDescending { it.createdAtMillis }
            .thenBy { it.id },
    )
}
