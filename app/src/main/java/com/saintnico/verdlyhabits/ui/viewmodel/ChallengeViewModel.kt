package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.ChallengeMode
import com.saintnico.verdlyhabits.data.remote.firestore.ChallengeRepository
import com.saintnico.verdlyhabits.data.remote.storage.StorageRepository
import com.saintnico.verdlyhabits.data.remote.firestore.CreatedChallenge
import com.saintnico.verdlyhabits.data.remote.firestore.JoinChallengeOutcome
import com.saintnico.verdlyhabits.util.UserFacingErrors
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class ChallengeUiState(
    val challenges: List<Challenge> = emptyList(),
    val archivedChallenges: List<Challenge> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed class ProofState {
    data object Idle : ProofState()
    data object Uploading : ProofState()
    data object Success : ProofState()
    data object WindowClosed : ProofState()
    data object LateSuccess : ProofState()
    data object LateWindowClosed : ProofState()
    data object AlreadyPostedLate : ProofState()
    data class Error(val message: String) : ProofState()
}

/**
 * Holds challenge lists for the **currently signed-in Firebase user only**.
 * Must be scoped to a navigation graph entry that is cleared on logout (e.g. [Screen.Main])
 * so state and Firestore listeners never leak across Google account switches.
 */
class ChallengeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChallengeRepository()
    private val _state = MutableStateFlow(ChallengeUiState())
    private var hasCheckedMisses = false
    private val finalizedExpiredIds = mutableSetOf<String>()

    /** Per-challenge last observed rank for the current user — powers live overtake/lead banners. */
    private val lastKnownRank = mutableMapOf<String, Int>()
    val state: StateFlow<ChallengeUiState> = _state.asStateFlow()

    private val _proofState = MutableStateFlow<ProofState>(ProofState.Idle)
    val proofState: StateFlow<ProofState> = _proofState.asStateFlow()

    private val _milestoneEvent = MutableSharedFlow<Int>()
    val milestoneEvent: SharedFlow<Int> = _milestoneEvent.asSharedFlow()

    /** Pair(newRank, rankDelta) where positive delta = moved up. */
    private val _rankChangeEvent = MutableSharedFlow<Pair<Int, Int>>(extraBufferCapacity = 1)
    val rankChangeEvent: SharedFlow<Pair<Int, Int>> = _rankChangeEvent.asSharedFlow()

    private val storageRepository = StorageRepository()

    private val _createResult = MutableSharedFlow<CreatedChallenge?>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val createResult: SharedFlow<CreatedChallenge?> = _createResult.asSharedFlow()

    private val _joinResult = MutableSharedFlow<JoinChallengeOutcome>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val joinResult: SharedFlow<JoinChallengeOutcome> = _joinResult.asSharedFlow()

    /**
     * One-shot id consumed by [com.saintnico.verdlyhabits.ui.screens.challenge.ChallengeScreen] after navigating
     * to the `challenges` tab (same back stack as bottom nav) so Profile can be restored via the bar.
     */
    private val _pendingOpenChallengeId = MutableStateFlow<String?>(null)
    val pendingOpenChallengeId: StateFlow<String?> = _pendingOpenChallengeId.asStateFlow()

    fun requestOpenChallengeDetail(challengeId: String) {
        _pendingOpenChallengeId.value = challengeId
    }

    fun consumePendingOpenChallengeDetail() {
        _pendingOpenChallengeId.value = null
    }

    private val _pendingOpenCreate = MutableStateFlow(false)
    val pendingOpenCreate: StateFlow<Boolean> = _pendingOpenCreate.asStateFlow()

    fun requestOpenCreate() {
        _pendingOpenCreate.value = true
    }

    fun consumePendingOpenCreate() {
        _pendingOpenCreate.value = false
    }

    /** Optional challenge context when opening a member profile from the challenge sheet. */
    private val _memberProfileHighlightChallengeId = MutableStateFlow<String?>(null)
    val memberProfileHighlightChallengeId: StateFlow<String?> = _memberProfileHighlightChallengeId.asStateFlow()

    fun setMemberProfileHighlightChallengeId(id: String?) {
        _memberProfileHighlightChallengeId.value = id
    }

    fun consumeMemberProfileHighlightChallengeId(): String? {
        val v = _memberProfileHighlightChallengeId.value
        _memberProfileHighlightChallengeId.value = null
        return v
    }

    init {
        viewModelScope.launch {
            firebaseAuthUidFlow()
                .distinctUntilChanged()
                .collectLatest { uid ->
                    observeChallengesForSignedInUser(uid)
                }
        }
    }

    /**
     * Emits [FirebaseAuth.currentUser] uid whenever auth state changes (sign in, sign out, switch account).
     */
    private fun firebaseAuthUidFlow() = callbackFlow {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { a ->
            trySend(a.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.uid)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Cancels all prior Firestore listeners (via [collectLatest]), clears stale UI, then subscribes
     * using the **captured** [uid] so we never mix data from two accounts.
     */
    private suspend fun observeChallengesForSignedInUser(uid: String?) {
        hasCheckedMisses = false
        if (uid.isNullOrBlank()) {
            _state.value = ChallengeUiState(
                challenges = emptyList(),
                archivedChallenges = emptyList(),
                isLoading = false,
                error = null
            )
            return
        }

        _state.value = ChallengeUiState(
            challenges = emptyList(),
            archivedChallenges = emptyList(),
            isLoading = true,
            error = null
        )

        coroutineScope {
            combine(
                repository.observeMyChallengesForUser(uid)
                    .catch { e ->
                        val cur = _state.value
                        _state.value = cur.copy(isLoading = false, error = UserFacingErrors.message(e))
                        emit(emptyList())
                    },
                repository.observeArchivedChallengesForUser(uid)
                    .catch { emit(emptyList()) }
            ) { myChallenges, myArchived ->
                val activeSorted = myChallenges
                    .filter { ch -> uid in ch.members }
                    .sortedByDescending { it.startDate }
                val archivedSorted = myArchived
                    .filter { ch -> uid in ch.members || uid in ch.leftMembers }
                    .sortedByDescending { it.endedAt ?: it.endDate }
                activeSorted to archivedSorted
            }.collect { (activeSorted, archivedSorted) ->
                val prev = _state.value
                _state.value = prev.copy(
                    challenges = activeSorted,
                    archivedChallenges = archivedSorted,
                    isLoading = false,
                    error = prev.error
                )
                detectLiveRankChanges(uid, activeSorted)
                launch {
                    val app = getApplication<Application>()
                    com.saintnico.verdlyhabits.widget.WidgetSnapshotWriter.updateLeaderboard(app, activeSorted)
                    com.saintnico.verdlyhabits.widget.WidgetRefresh.updateLeaderboardOnly(app)
                }
                if (!hasCheckedMisses && activeSorted.isNotEmpty()) {
                    hasCheckedMisses = true
                    activeSorted.filter { it.isEffectivelyActive() }.forEach { ch ->
                        launch {
                            try {
                                repository.checkMissedDays(ch.id)
                            } catch (_: Exception) {
                            }
                        }
                    }
                }
                activeSorted
                    .filter { it.isActive && !it.isArchived && it.isPastEndDate() }
                    .filter { it.id !in finalizedExpiredIds }
                    .forEach { ch ->
                        finalizedExpiredIds.add(ch.id)
                        launch {
                            try {
                                repository.finaliseChallenge(
                                    ch.id,
                                    endedByUid = uid,
                                    archivedReason = "completed",
                                )
                            } catch (_: Exception) {
                                finalizedExpiredIds.remove(ch.id)
                            }
                        }
                    }
            }
        }
    }

    fun createChallenge(
        habitName: String,
        stake: String,
        durationDays: Int,
        dailyDeadline: String = "ANYTIME",
        challengeMode: ChallengeMode = ChallengeMode.HYBRID,
    ) {
        viewModelScope.launch {
            try {
                val created = repository.createChallenge(habitName, stake, durationDays, dailyDeadline, challengeMode)
                if (created == null) {
                    _state.value = _state.value.copy(error = "Sign in to create a challenge.")
                    return@launch
                }
                mergeFetchedChallengeIntoState(created.documentId)
                _createResult.tryEmit(created)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UserFacingErrors.message(e))
            }
        }
    }

    fun joinChallenge(rawChallengeId: String) {
        viewModelScope.launch {
            val outcome = try {
                repository.joinChallenge(rawChallengeId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UserFacingErrors.message(e))
                JoinChallengeOutcome.Error(UserFacingErrors.message(e))
            }
            if (outcome is JoinChallengeOutcome.Success) {
                mergeFetchedChallengeIntoState(outcome.challengeDocumentId)
            }
            _joinResult.tryEmit(outcome)
        }
    }

    suspend fun peekChallenge(challengeId: String): Challenge? =
        repository.fetchChallenge(challengeId)

    private suspend fun mergeFetchedChallengeIntoState(challengeId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        repeat(8) { attempt ->
            val fresh = repository.fetchChallenge(challengeId)
            if (fresh != null) {
                if (uid !in fresh.members) return
                val without = _state.value.challenges.filter { it.id != fresh.id }
                val prev = _state.value
                _state.value = prev.copy(
                    challenges = (without + fresh).sortedByDescending { it.startDate }
                )
                return
            }
            delay((120L * (attempt + 1)).coerceAtMost(900L))
        }
    }

    fun leaveChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                repository.leaveChallenge(challengeId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UserFacingErrors.message(e))
            }
        }
    }

    fun endChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                repository.endChallenge(challengeId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UserFacingErrors.message(e))
            }
        }
    }

    /**
     * Host-only: directly add one of the user's connections to a challenge.
     * Returns a human-readable result via [onResult].
     */
    fun addMemberToChallenge(
        challengeId: String,
        memberUid: String,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                when (repository.addMemberByCreator(challengeId, memberUid)) {
                    is com.saintnico.verdlyhabits.data.remote.firestore.JoinChallengeOutcome.Success ->
                        onResult(true, "Added to the arena")
                    com.saintnico.verdlyhabits.data.remote.firestore.JoinChallengeOutcome.PermissionDenied ->
                        onResult(false, "Only the host can add members. Share the invite link instead.")
                    com.saintnico.verdlyhabits.data.remote.firestore.JoinChallengeOutcome.Ended ->
                        onResult(false, "This challenge has ended.")
                    com.saintnico.verdlyhabits.data.remote.firestore.JoinChallengeOutcome.NotFound ->
                        onResult(false, "Challenge not found.")
                    com.saintnico.verdlyhabits.data.remote.firestore.JoinChallengeOutcome.NotSignedIn ->
                        onResult(false, "Sign in with Google first.")
                    else -> onResult(false, "Could not add them right now.")
                }
            } catch (e: Exception) {
                onResult(false, UserFacingErrors.message(e))
            }
        }
    }

    /** Removes today's proof for the signed-in user so they can post again (same rules as one check-in per day). */
    fun deleteTodayCheckIn(challengeId: String, date: String) {
        viewModelScope.launch {
            try {
                repository.deleteTodayCheckIn(challengeId, date)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UserFacingErrors.message(e))
            }
        }
    }

    fun markCompletion(challengeId: String, date: String, photoUrl: String, lat: Double? = null, lng: Double? = null) {
        viewModelScope.launch {
            _proofState.value = ProofState.Uploading
            try {
                repository.markCompletion(challengeId, date, photoUrl, lat, lng, isLate = false)
                _proofState.value = ProofState.Success

                // Push notifications (proof posted, rank moves, reactions) are sent server-side
                // by Cloud Functions watching the challenge doc + activity feed. The client only
                // surfaces live in-app feedback (rank banners come from the Firestore listener).
                val uid = currentUserId ?: return@launch
                val challenge = state.value.challenges.find { it.id == challengeId }
                val streak = challenge?.memberStreaks?.get(uid) ?: 0
                if (streak in listOf(3, 7, 14, 21)) {
                    _milestoneEvent.emit(streak)
                }
            } catch (e: IllegalStateException) {
                _proofState.value = when (e.message) {
                    "LATE_WINDOW_CLOSED" -> ProofState.LateWindowClosed
                    "EARLY_MORNING_BLOCK" -> ProofState.WindowClosed
                    "ALREADY_CHECKED_IN" -> ProofState.Error("Already checked in for this day")
                    else -> ProofState.Error(UserFacingErrors.message(e))
                }
            } catch (e: Exception) {
                _proofState.value = ProofState.Error(UserFacingErrors.message(e))
            }
        }
    }

    fun markLateCompletion(challengeId: String, photoUri: Uri, context: Context) {
        viewModelScope.launch {
            val hour = java.time.LocalTime.now().hour
            if (hour >= 3) {
                _proofState.value = ProofState.LateWindowClosed
                return@launch
            }
            _proofState.value = ProofState.Uploading
            try {
                val url = storageRepository.uploadProofPhoto(
                    context,
                    "late_$challengeId",
                    photoUri,
                    challengeId
                ) ?: run {
                    _proofState.value = ProofState.Error("Upload failed")
                    return@launch
                }
                val yesterdayStr = java.time.LocalDate.now()
                    .minusDays(1)
                    .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                repository.markCompletion(challengeId, yesterdayStr, url, null, null, isLate = true)
                _proofState.value = ProofState.LateSuccess
            } catch (e: IllegalStateException) {
                _proofState.value = when (e.message) {
                    "LATE_WINDOW_CLOSED" -> ProofState.LateWindowClosed
                    "ALREADY_POSTED_LATE" -> ProofState.AlreadyPostedLate
                    else -> ProofState.Error(UserFacingErrors.message(e))
                }
            } catch (e: Exception) {
                _proofState.value = ProofState.Error(UserFacingErrors.message(e))
            }
        }
    }

    /**
     * Live rank banners: compares the current user's rank in each active challenge against the
     * last value seen from the Firestore listener. Fires for self-moves AND being overtaken while
     * idle. Positive delta = climbed; negative = dropped.
     */
    private suspend fun detectLiveRankChanges(uid: String, challenges: List<Challenge>) {
        challenges.filter { it.isActive }.forEach { ch ->
            val rank = ch.rankOf(uid)
            if (rank <= 0) return@forEach
            val prev = lastKnownRank.put(ch.id, rank)
            if (prev != null && prev != rank) {
                _rankChangeEvent.emit(rank to (prev - rank))
            }
        }
    }

    fun resetProofState() {
        _proofState.value = ProofState.Idle
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    private val _reactionFeedback = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val reactionFeedback: SharedFlow<String> = _reactionFeedback.asSharedFlow()

    fun addReaction(challengeId: String, proofKey: String, emoji: String) {
        viewModelScope.launch {
            try {
                val pointDelta = repository.addReaction(challengeId, proofKey, emoji)
                if (pointDelta == 0) return@launch
                delay(500)
                val after = repository.fetchChallenge(challengeId) ?: return@launch
                mergeFetchedChallengeIntoState(challengeId)
                // Reaction + resulting rank-move pushes are sent server-side by Cloud Functions
                // (onChallengeActivity + onChallengeRankChange). Here we only show live feedback
                // to the reactor for instant gratification.
                val myRank = after.rankOf(currentUserId.orEmpty())
                val rankSuffix = if (myRank > 0) " — you're now #$myRank" else ""
                _reactionFeedback.emit("+$pointDelta pts$rankSuffix")
            } catch (_: Exception) {
            }
        }
    }

    fun reportProof(challengeId: String, proofKey: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.reportProof(challengeId, proofKey)
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    /** Habit marked done on Home but arena proof not posted today. */
    fun challengeProofReminderForHabit(habitTitle: String, habitCompleted: Boolean): Challenge? {
        if (!habitCompleted) return null
        val ch = getActiveChallengeForHabit(habitTitle) ?: return null
        val uid = currentUserId ?: return null
        val today = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        return if (ch.hasCompletedToday(uid, today)) null else ch
    }

    fun getActiveChallengeForHabit(habitName: String): Challenge? {
        val uid = currentUserId ?: return null
        return state.value.challenges.find {
            uid in it.members &&
                it.isActive &&
                it.habitName.equals(habitName, ignoreCase = true)
        }
    }

    fun observeActivity(challengeId: String) = repository.observeActivity(challengeId)

    val currentUserId: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid
}
