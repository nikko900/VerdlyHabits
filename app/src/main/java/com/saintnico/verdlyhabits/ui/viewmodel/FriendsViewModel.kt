package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.remote.firestore.ChallengeJoinRequestDoc
import com.saintnico.verdlyhabits.data.remote.firestore.ChallengeJoinRequestRepository
import com.saintnico.verdlyhabits.data.remote.firestore.ChallengeRepository
import com.saintnico.verdlyhabits.data.remote.firestore.FriendRelationship
import com.saintnico.verdlyhabits.data.remote.firestore.FriendRepository
import com.saintnico.verdlyhabits.data.remote.firestore.FriendRequestDoc
import com.saintnico.verdlyhabits.data.remote.firestore.JoinChallengeOutcome
import com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
import com.saintnico.verdlyhabits.notifications.SocialNotificationDispatcher
import com.saintnico.verdlyhabits.session.firebaseAuthUidFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendSummary(
    val uid: String,
    val username: String,
    val photoUrl: String?,
)

data class IncomingFriendRequestUi(
    val request: FriendRequestDoc,
    val username: String,
    val photoUrl: String?,
)

data class UsernameSearchResult(
    val uid: String,
    val username: String,
    val displayName: String,
    val photoUrl: String?,
    val relationship: FriendRelationship,
)

object FriendsErrorMessages {
    fun forThrowable(message: String?): String {
        val m = message.orEmpty()
        return when {
            m.contains("Sign in with Google", ignoreCase = true) ->
                "Sign in with Google first, then try Connect again."
            m.contains("PERMISSION_DENIED", ignoreCase = true) ||
                m.contains("Missing or insufficient permissions", ignoreCase = true) ->
                "Couldn't connect right now. Sign out and back in, then try again."
            m.isBlank() -> "Connect failed. Try again."
            else -> com.saintnico.verdlyhabits.util.UserFacingErrors.forRawMessage(m)
        }
    }
}

class FriendsViewModel(application: Application) : AndroidViewModel(application) {

    private val friendRepository = FriendRepository()
    private val joinRequestRepository = ChallengeJoinRequestRepository()
    private val challengeRepository = ChallengeRepository()
    private val userRepository = UserRepository()

    private val myUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    val incomingRequests: StateFlow<List<FriendRequestDoc>> =
        firebaseAuthUidFlow()
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) flowOf(emptyList())
                else friendRepository.observeIncomingRequests(uid)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

    val incomingRequestsUi: StateFlow<List<IncomingFriendRequestUi>> =
        incomingRequests
            .flatMapLatest { requests ->
                flow {
                    val enriched = requests.mapNotNull { req ->
                        val profile = userRepository.fetchPublicProfile(req.fromUid)
                        IncomingFriendRequestUi(
                            request = req,
                            username = profile?.username?.ifBlank { profile.displayName }?.ifBlank { "Rival" }
                                ?: "Rival",
                            photoUrl = profile?.photoUrl,
                        )
                    }
                    emit(enriched)
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingChallengeJoinRequests: StateFlow<List<ChallengeJoinRequestDoc>> =
        firebaseAuthUidFlow()
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) flowOf(emptyList())
                else joinRequestRepository.observePendingForHost(uid)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList(),
            )

    private val acceptedFromMe = firebaseAuthUidFlow()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList())
            else friendRepository.observeAcceptedFromMe(uid)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    private val acceptedToMe = firebaseAuthUidFlow()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList())
            else friendRepository.observeAcceptedToMe(uid)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList(),
        )

    val friendSummaries: StateFlow<List<FriendSummary>> =
        combine(acceptedFromMe, acceptedToMe) { from, to ->
            buildSet {
                from.forEach { add(it.toUid) }
                to.forEach { add(it.fromUid) }
            }
        }
            .flatMapLatest { others ->
                flow {
                    val list = others.mapNotNull { uid ->
                        val p = userRepository.fetchPublicProfile(uid) ?: return@mapNotNull null
                        FriendSummary(
                            uid = p.uid,
                            username = p.username.ifBlank { p.displayName.ifBlank { "Rival" } },
                            photoUrl = p.photoUrl,
                        )
                    }.sortedBy { it.username.lowercase() }
                    emit(list)
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _usernameSearchResult = MutableStateFlow<UsernameSearchResult?>(null)
    val usernameSearchResult = _usernameSearchResult.asStateFlow()

    private val _usernameSearchError = MutableStateFlow<String?>(null)
    val usernameSearchError = _usernameSearchError.asStateFlow()

    private val _usernameSearchLoading = MutableStateFlow(false)
    val usernameSearchLoading = _usernameSearchLoading.asStateFlow()

    fun clearUsernameSearch() {
        _usernameSearchResult.value = null
        _usernameSearchError.value = null
        _usernameSearchLoading.value = false
    }

    fun searchByUsername(query: String) {
        viewModelScope.launch {
            _usernameSearchLoading.value = true
            _usernameSearchError.value = null
            _usernameSearchResult.value = null
            val normalized = query.trim().removePrefix("@")
            try {
                when {
                    normalized.length < 2 -> {
                        _usernameSearchError.value = "Enter at least 2 characters"
                    }
                    else -> {
                        val profile = userRepository.findByUsername(normalized)
                        if (profile == null) {
                            _usernameSearchError.value = "No rival found for @$normalized"
                        } else if (profile.uid == myUid) {
                            _usernameSearchError.value = "That's your account"
                        } else {
                            val rel = friendRepository.relationshipWith(profile.uid)
                            _usernameSearchResult.value = UsernameSearchResult(
                                uid = profile.uid,
                                username = profile.username.ifBlank { profile.displayName }.ifBlank { "Rival" },
                                displayName = profile.displayName,
                                photoUrl = profile.photoUrl,
                                relationship = rel,
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _usernameSearchError.value = FriendsErrorMessages.forThrowable(e.message)
            }
            _usernameSearchLoading.value = false
        }
    }

    fun sendFriendRequest(toUid: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val r = friendRepository.sendFriendRequest(toUid)
            if (r.isSuccess) {
                val me = myUid
                val profile = userRepository.fetchPublicProfile(me)
                val username = profile?.username?.ifBlank { profile.displayName }?.ifBlank { "rival" } ?: "rival"
                SocialNotificationDispatcher.notifyFriendRequest(
                    targetUid = toUid,
                    fromUsername = username,
                    fromPhotoUrl = profile?.photoUrl,
                    requestId = SocialNotificationDispatcher.friendRequestId(me, toUid),
                )
            }
            onResult(r.isSuccess, r.exceptionOrNull()?.let { FriendsErrorMessages.forThrowable(it.message) })
        }
    }

    fun acceptRequest(requestId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val r = friendRepository.acceptRequest(requestId)
            onResult(r.isSuccess, r.exceptionOrNull()?.let { FriendsErrorMessages.forThrowable(it.message) })
        }
    }

    fun declineRequest(requestId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val r = friendRepository.declineRequest(requestId)
            onResult(r.isSuccess, r.exceptionOrNull()?.let { FriendsErrorMessages.forThrowable(it.message) })
        }
    }

    fun sendChallengeJoinRequest(
        challengeId: String,
        challengeTitle: String,
        hostUid: String,
        targetMemberUid: String,
        fromUsername: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        viewModelScope.launch {
            val r = joinRequestRepository.sendJoinRequest(
                challengeId = challengeId,
                challengeTitle = challengeTitle,
                hostUid = hostUid,
                targetMemberUid = targetMemberUid,
                fromUsername = fromUsername,
            )
            if (r.isSuccess) {
                val me = myUid
                SocialNotificationDispatcher.notifyArenaInvite(
                    targetUid = hostUid,
                    fromUsername = fromUsername,
                    challengeTitle = challengeTitle,
                    challengeId = challengeId,
                    requestId = "${challengeId}_${me}",
                )
            }
            onResult(r.isSuccess, r.exceptionOrNull()?.let { FriendsErrorMessages.forThrowable(it.message) })
        }
    }

    suspend fun hasPendingJoinRequest(challengeId: String): Boolean {
        val me = myUid
        if (me.isBlank()) return false
        return joinRequestRepository.findPendingRequest(challengeId, me) != null
    }

    fun approveChallengeJoinRequest(requestId: String, challengeId: String, requesterUid: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val approve = joinRequestRepository.approveRequest(requestId)
            if (approve.isFailure) {
                onResult(false, FriendsErrorMessages.forThrowable(approve.exceptionOrNull()?.message))
                return@launch
            }
            when (challengeRepository.addMemberByCreator(challengeId, requesterUid)) {
                is JoinChallengeOutcome.Success -> onResult(true, null)
                JoinChallengeOutcome.PermissionDenied ->
                    onResult(false, "Could not add them to the challenge. Check Firestore rules.")
                else -> onResult(false, "Could not add them to the challenge.")
            }
        }
    }

    fun declineChallengeJoinRequest(requestId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val r = joinRequestRepository.declineRequest(requestId)
            onResult(r.isSuccess, r.exceptionOrNull()?.let { FriendsErrorMessages.forThrowable(it.message) })
        }
    }

    suspend fun findPendingIncomingRequestId(fromUid: String): String? =
        friendRepository.findPendingIncomingRequestId(fromUid)

    suspend fun relationshipWith(otherUid: String): FriendRelationship =
        friendRepository.relationshipWith(otherUid)
}
