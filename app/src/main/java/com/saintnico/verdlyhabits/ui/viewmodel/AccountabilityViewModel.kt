package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.remote.firestore.AccountabilityRepository
import com.saintnico.verdlyhabits.data.remote.firestore.DuoMilestoneReached
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakBroken
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.domain.PremiumGate
import com.saintnico.verdlyhabits.notifications.SocialNotificationDispatcher
import com.saintnico.verdlyhabits.session.firebaseAuthUidFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountabilityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AccountabilityRepository()
    private val _isPremium = MutableStateFlow(false)

    fun setPremiumAccess(enabled: Boolean) {
        _isPremium.value = enabled
    }

    val duoState: StateFlow<DuoStreakState?> = combine(
        firebaseAuthUidFlow(),
        _isPremium,
    ) { uid, premium -> uid to premium }
        .flatMapLatest { (uid, premium) ->
            if (uid.isNullOrBlank()) flowOf(null)
            else repository.observeActivePair(uid, isPremium = premium)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _nudgeMessage = MutableStateFlow<String?>(null)
    val nudgeMessage: StateFlow<String?> = _nudgeMessage.asStateFlow()

    private val _milestoneEvent = MutableSharedFlow<DuoMilestoneReached>(extraBufferCapacity = 1)
    val milestoneEvent = _milestoneEvent.asSharedFlow()

    private val _streakBrokenEvent = MutableSharedFlow<DuoStreakBroken>(extraBufferCapacity = 1)
    val streakBrokenEvent = _streakBrokenEvent.asSharedFlow()

    private var lastBuddyDone: Boolean? = null
    private var myUsernameCache: String = "rival"

    init {
        viewModelScope.launch {
            duoState.collect { state ->
                if (state == null) {
                    lastBuddyDone = null
                    return@collect
                }
                val buddyDone = state.buddyDoneToday
                val wasDone = lastBuddyDone
                lastBuddyDone = buddyDone
                if (wasDone == false && buddyDone && !state.myDoneToday && state.status == "active") {
                    val name = state.buddyUsername.ifBlank { "Your buddy" }
                    _nudgeMessage.value = "$name finished today — your turn."
                }
            }
        }
    }

    fun clearNudge() {
        _nudgeMessage.value = null
    }

    fun setMyUsername(username: String) {
        myUsernameCache = username.ifBlank { "rival" }
    }

    fun inviteBuddy(
        uid: String,
        username: String,
        photoUrl: String?,
        myUsername: String,
        myPhotoUrl: String?,
        onResult: ((Boolean, String?) -> Unit)? = null,
    ) {
        viewModelScope.launch {
            myUsernameCache = myUsername.ifBlank { "rival" }
            val result = repository.inviteBuddy(uid, username, photoUrl, myUsername, myPhotoUrl)
            if (result.isSuccess) {
                val me = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                val pairId = AccountabilityRepository.pairId(me, uid)
                SocialNotificationDispatcher.notifyDuoInvite(
                    targetUid = uid,
                    fromUsername = myUsername,
                    fromPhotoUrl = myPhotoUrl,
                    pairId = pairId,
                )
            }
            onResult?.invoke(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    fun acceptInvite(pairId: String) {
        viewModelScope.launch {
            runCatching { repository.acceptInvite(pairId) }
        }
    }

    fun declineInvite(pairId: String) {
        viewModelScope.launch {
            runCatching { repository.declineInvite(pairId) }
        }
    }

    fun applyGrace(pairId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val premium = PremiumGate.isUnlocked(PremiumGate.Feature.STREAK_SHIELDS, _isPremium.value)
            val r = repository.applyDuoGrace(pairId, isPremium = premium)
            onResult(r.isSuccess, r.exceptionOrNull()?.message)
        }
    }

    fun syncFromHabits(
        completedToday: Int,
        activeHabits: Int,
        userStatsViewModel: UserStatsViewModel?,
    ) {
        if (activeHabits <= 0) return
        val allDone = completedToday >= activeHabits
        viewModelScope.launch {
            val premium = PremiumGate.isUnlocked(PremiumGate.Feature.STREAK_SHIELDS, _isPremium.value)
            val outcome = repository.syncMyProgress(
                completed = completedToday,
                total = activeHabits,
                allDone = allDone,
                myUsername = myUsernameCache,
                isPremium = premium,
            )
            if (outcome.queuedOffline) {
                val pairId = duoState.value?.pairId
                if (!pairId.isNullOrBlank()) {
                    enqueueDuoDayClose(
                        pairId = pairId,
                        completed = completedToday,
                        total = activeHabits,
                        allDone = allDone,
                    )
                }
            }
            outcome.broken?.let { _streakBrokenEvent.emit(it) }
            outcome.milestone?.let { m ->
                userStatsViewModel?.awardDuoMilestoneXp(m.streakDays)
                _milestoneEvent.emit(m)
            }
            if (outcome.buddyJustFinished) {
                val state = duoState.value ?: return@launch
                if (state.buddyUid.isNotBlank()) {
                    SocialNotificationDispatcher.notifyDuoBuddyDone(
                        targetUid = state.buddyUid,
                        fromUsername = myUsernameCache,
                        pairId = state.pairId,
                        streakDays = state.streakDays,
                    )
                }
            }
        }
    }

    private suspend fun enqueueDuoDayClose(
        pairId: String,
        completed: Int,
        total: Int,
        allDone: Boolean,
    ) = withContext(Dispatchers.IO) {
        // Blocking Room DAO — must never run on Main (viewModelScope default).
        val app = getApplication<Application>()
        val dao = com.saintnico.verdlyhabits.data.local.AppDatabase.getDatabase(app).streakDao()
        val zoneOffsetMinutes = java.time.ZoneId.systemDefault().rules
            .getOffset(java.time.Instant.now()).totalSeconds / 60
        val payload = mapOf(
            "pairId" to pairId,
            "completed" to completed,
            "total" to total,
            "allDone" to allDone,
            "zoneOffsetMinutes" to zoneOffsetMinutes,
        )
        dao.enqueue(
            com.saintnico.verdlyhabits.data.local.streak.PendingOpEntity(
                kind = com.saintnico.verdlyhabits.data.local.streak.PendingOpKind.DUO_DAY_CLOSE.name,
                payload = com.google.gson.Gson().toJson(payload),
                createdAt = System.currentTimeMillis(),
            ),
        )
        com.saintnico.verdlyhabits.streak.StreakSyncWorker.enqueue(app)
    }
}
