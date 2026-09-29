package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.data.remote.firestore.BlockRepository
import com.saintnico.verdlyhabits.data.remote.firestore.BlockedListSnapshot
import com.saintnico.verdlyhabits.data.remote.firestore.BlockedUserDoc
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FriendSummary(
    val uid: String,
    val username: String,
    val photoUrl: String?,
    val membershipTier: MembershipTier = MembershipTier.FREE,
    val titleGlowId: String? = null,
    val profileFrameId: String? = null,
    val grovePlantId: String? = null,
    val pinnedTrophyLabel: String? = null,
    val pinnedTrophyUntil: Long = 0L,
    val lastPlantedDay: String? = null,
    val plantedCountToday: Int = 0,
    val weeklyPlantDays: Int = 0,
    val weeklyPlantWeekKey: String? = null,
    val currentStreakPublic: Int = 0,
    val groveLeadUntil: Long = 0L,
    val witnessCountToday: Int = 0,
    val witnessDay: String? = null,
    val equippedTitleLabel: String? = null,
    val sharedPulseHabit: String? = null,
    val sharedPulseDay: String? = null,
    val lastActiveAt: Long = 0L,
    val shareLastActive: Boolean = true,
)

data class IncomingFriendRequestUi(
    val request: FriendRequestDoc,
    val username: String,
    val photoUrl: String?,
    val membershipTier: MembershipTier = MembershipTier.FREE,
)

data class UsernameSearchResult(
    val uid: String,
    val username: String,
    val displayName: String,
    val photoUrl: String?,
    val relationship: FriendRelationship,
    val membershipTier: MembershipTier = MembershipTier.FREE,
    /** "New on Verdly" / "Active lately" — only for discover suggestions. */
    val suggestionTag: String? = null,
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

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModel(application: Application) : AndroidViewModel(application) {

    private val friendRepository = FriendRepository()
    private val blockRepository = BlockRepository()
    private val joinRequestRepository = ChallengeJoinRequestRepository()
    private val challengeRepository = ChallengeRepository()
    private val userRepository = UserRepository()

    private val myUid: String
        get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val blockedRetryTick = MutableStateFlow(0)

    val blockedList: StateFlow<BlockedListSnapshot> =
        combine(firebaseAuthUidFlow(), blockedRetryTick) { uid, _ -> uid }
            .flatMapLatest { uid ->
                if (uid.isNullOrBlank()) flowOf(BlockedListSnapshot(loaded = true))
                else blockRepository.observeBlockedUsers(uid)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                BlockedListSnapshot(),
            )

    val blockedUids: StateFlow<Set<String>> =
        blockedList
            .map { it.uids }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val blockedUsers: StateFlow<List<BlockedUserDoc>> =
        blockedList
            .map { it.users }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun retryBlockedList() {
        blockedRetryTick.value = blockedRetryTick.value + 1
    }

    val incomingRequests: StateFlow<List<FriendRequestDoc>> =
        combine(
            firebaseAuthUidFlow().flatMapLatest { uid ->
                if (uid.isNullOrBlank()) flowOf(emptyList())
                else friendRepository.observeIncomingRequests(uid)
            },
            blockedUids,
        ) { requests, blocked ->
            requests.filter { it.fromUid !in blocked }
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
                            membershipTier = profile?.membershipTier ?: MembershipTier.FREE,
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

    private val _presenceRefresh = MutableStateFlow(0L)

    fun refreshFriendPresence() {
        _presenceRefresh.value = System.currentTimeMillis()
    }

    val friendSummaries: StateFlow<List<FriendSummary>> =
        combine(acceptedFromMe, acceptedToMe, blockedUids, _presenceRefresh) { from, to, blocked, _ ->
            buildSet {
                from.forEach { if (it.toUid !in blocked) add(it.toUid) }
                to.forEach { if (it.fromUid !in blocked) add(it.fromUid) }
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
                            membershipTier = p.membershipTier,
                            titleGlowId = p.titleGlowId,
                            profileFrameId = p.profileFrameId,
                            grovePlantId = p.grovePlantId,
                            pinnedTrophyLabel = p.pinnedTrophyLabel,
                            pinnedTrophyUntil = p.pinnedTrophyUntil,
                            lastPlantedDay = p.lastPlantedDay,
                            plantedCountToday = p.plantedCountToday,
                            weeklyPlantDays = p.weeklyPlantDays,
                            weeklyPlantWeekKey = p.weeklyPlantWeekKey,
                            currentStreakPublic = p.currentStreakPublic,
                            groveLeadUntil = p.groveLeadUntil,
                            witnessCountToday = p.witnessCountToday,
                            witnessDay = p.witnessDay,
                            equippedTitleLabel = p.equippedTitleLabel,
                            sharedPulseHabit = p.sharedPulseHabit,
                            sharedPulseDay = p.sharedPulseDay,
                            lastActiveAt = p.lastActiveAt,
                            shareLastActive = p.shareLastActive,
                        )
                    }.sortedBy { it.username.lowercase() }
                    emit(list)
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _usernameSearchResults = MutableStateFlow<List<UsernameSearchResult>>(emptyList())
    val usernameSearchResults = _usernameSearchResults.asStateFlow()

    private val _usernameSearchError = MutableStateFlow<String?>(null)
    val usernameSearchError = _usernameSearchError.asStateFlow()

    private val _usernameSearchLoading = MutableStateFlow(false)
    val usernameSearchLoading = _usernameSearchLoading.asStateFlow()

    private var searchJob: Job? = null
    private var suggestionsJob: Job? = null

    private val _discoverSuggestions = MutableStateFlow<List<UsernameSearchResult>>(emptyList())
    val discoverSuggestions: StateFlow<List<UsernameSearchResult>> = _discoverSuggestions.asStateFlow()

    private val _suggestionsLoading = MutableStateFlow(false)
    val suggestionsLoading: StateFlow<Boolean> = _suggestionsLoading.asStateFlow()

    fun clearUsernameSearch() {
        searchJob?.cancel()
        _usernameSearchResults.value = emptyList()
        _usernameSearchError.value = null
        _usernameSearchLoading.value = false
    }

    fun loadDiscoverSuggestions(force: Boolean = false) {
        if (!force && _discoverSuggestions.value.isNotEmpty()) return
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            _suggestionsLoading.value = true
            try {
                val friends = friendSummaries.value.map { it.uid }.toSet()
                val blocked = blockedUids.value
                val exclude = friends + blocked + setOfNotNull(myUid.takeIf { it.isNotBlank() })
                val profiles = userRepository.fetchDiscoverSuggestions(excludeUids = exclude, limit = 12)
                val weekMs = 7L * 24 * 60 * 60 * 1000
                val now = System.currentTimeMillis()
                _discoverSuggestions.value = profiles.map { profile ->
                    val tag = when {
                        profile.lastActiveAt > 0L && now - profile.lastActiveAt < weekMs -> "Active lately"
                        else -> "New on Verdly"
                    }
                    UsernameSearchResult(
                        uid = profile.uid,
                        username = profile.username.ifBlank { profile.displayName }.ifBlank { "Rival" },
                        displayName = profile.displayName,
                        photoUrl = profile.photoUrl,
                        relationship = relationshipForSearch(profile.uid),
                        membershipTier = profile.membershipTier,
                        suggestionTag = tag,
                    )
                }
            } catch (_: Exception) {
                if (_discoverSuggestions.value.isEmpty()) {
                    _discoverSuggestions.value = emptyList()
                }
            }
            _suggestionsLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        val normalized = query.trim().removePrefix("@")
        if (normalized.isEmpty()) {
            clearUsernameSearch()
            return
        }
        searchJob = viewModelScope.launch {
            delay(80)
            performUserSearch(normalized)
        }
    }

    fun searchByUsername(query: String) {
        searchJob?.cancel()
        viewModelScope.launch {
            val normalized = query.trim().removePrefix("@")
            if (normalized.isEmpty()) {
                _usernameSearchResults.value = emptyList()
                _usernameSearchError.value = null
                _usernameSearchLoading.value = false
                return@launch
            }
            performUserSearch(normalized)
        }
    }

    private fun relationshipForSearch(uid: String): FriendRelationship {
        if (uid == myUid) return FriendRelationship.Self
        if (friendSummaries.value.any { it.uid == uid }) return FriendRelationship.Friends
        if (incomingRequests.value.any { it.fromUid == uid }) return FriendRelationship.IncomingPending
        return FriendRelationship.None
    }

    private suspend fun performUserSearch(normalized: String) {
        _usernameSearchLoading.value = true
        _usernameSearchError.value = null
        try {
            val profiles = userRepository.searchUsers(normalized)
            val blocked = blockedUids.value
            val results = profiles.mapNotNull { profile ->
                if (profile.uid == myUid) return@mapNotNull null
                if (profile.uid in blocked) return@mapNotNull null
                UsernameSearchResult(
                    uid = profile.uid,
                    username = profile.username.ifBlank { profile.displayName }.ifBlank { "Rival" },
                    displayName = profile.displayName,
                    photoUrl = profile.photoUrl,
                    relationship = relationshipForSearch(profile.uid),
                    membershipTier = profile.membershipTier,
                )
            }
            if (results.isEmpty()) {
                _usernameSearchResults.value = emptyList()
                _usernameSearchError.value = "No rivals found for \"$normalized\""
            } else {
                _usernameSearchError.value = null
                _usernameSearchResults.value = results
            }
        } catch (e: Exception) {
            _usernameSearchError.value = FriendsErrorMessages.forThrowable(e.message)
        }
        _usernameSearchLoading.value = false
    }

    fun sendFriendRequest(toUid: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (toUid in blockedUids.value) {
                onResult(false, "You've blocked this person. Unblock them in Settings first.")
                return@launch
            }
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
            if (r.isSuccess) {
                com.saintnico.verdlyhabits.engine.TrophyBus.emit(
                    com.saintnico.verdlyhabits.engine.TrophyEvent.FriendsCount(
                        friendSummaries.value.size.coerceAtLeast(1)
                    )
                )
            }
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
                val profile = userRepository.fetchPublicProfile(me)
                SocialNotificationDispatcher.notifyArenaInvite(
                    targetUid = hostUid,
                    fromUsername = fromUsername,
                    challengeTitle = challengeTitle,
                    challengeId = challengeId,
                    requestId = "${challengeId}_${me}",
                    fromPhotoUrl = profile?.photoUrl,
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

    fun isBlocked(uid: String): Boolean = uid in blockedUids.value

    fun blockUser(
        uid: String,
        username: String,
        displayName: String,
        photoUrl: String? = null,
        reason: String = "user_block",
        onResult: (Boolean, String?) -> Unit = { _, _ -> },
    ) {
        viewModelScope.launch {
            val prior = friendRepository.relationshipWith(uid)
            val wasFriend = prior == FriendRelationship.Friends ||
                prior == FriendRelationship.IncomingPending ||
                prior == FriendRelationship.OutgoingPending
            val block = blockRepository.blockUser(
                blockedUid = uid,
                username = username,
                displayName = displayName,
                photoUrl = photoUrl,
                reason = reason,
                wasFriend = wasFriend,
            )
            if (block.isFailure) {
                onResult(false, FriendsErrorMessages.forThrowable(block.exceptionOrNull()?.message))
                return@launch
            }
            // Keep them out of your surfaces via blockedUids filter — do not delete the
            // friendship, so unblock brings them straight back to Connections.
            onResult(true, null)
        }
    }

    fun unblockUser(uid: String, onResult: (Boolean, String?) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val r = blockRepository.unblockUser(uid)
            if (r.isFailure) {
                onResult(false, FriendsErrorMessages.forThrowable(r.exceptionOrNull()?.message))
                return@launch
            }
            val wasFriend = r.getOrNull()?.wasFriend == true
            if (wasFriend) {
                // Legacy blocks deleted the friendRequests docs — put the connection back.
                val restore = friendRepository.restoreFriendship(uid)
                if (restore.isFailure) {
                    onResult(
                        true,
                        "Unblocked, but couldn't restore the connection. Search their username and Connect again.",
                    )
                    return@launch
                }
            }
            onResult(true, null)
        }
    }
}
