package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.data.remote.firestore.GroveSocialRepository
import com.saintnico.verdlyhabits.data.remote.firestore.OpenGroveBoard
import com.saintnico.verdlyhabits.data.remote.firestore.OpenGroveWeekRank
import com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
import com.saintnico.verdlyhabits.engine.grove.GroveCalendar
import com.saintnico.verdlyhabits.notifications.SocialNotificationDispatcher
import com.saintnico.verdlyhabits.preferences.ThemePreference
import com.saintnico.verdlyhabits.session.firebaseAuthUidFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class GroveMember(
    val uid: String,
    val username: String,
    val photoUrl: String?,
    val membershipTier: MembershipTier,
    val titleGlowId: String?,
    val profileFrameId: String?,
    val grovePlantId: String?,
    val pinnedTrophyLabel: String?,
    val plantedToday: Boolean,
    val weeklyPlantDays: Int,
    val currentStreakPublic: Int,
    val groveLead: Boolean,
    val witnessCount: Int,
    val isSelf: Boolean,
    val standInOpenGrove: Boolean = false,
    val openGroveWeekRank: Int = 0,
    val openGrovePercentile: Int = 0,
    val plantedCountToday: Int = 0,
    val sharedHabitTitle: String? = null,
    val lastPlantedDay: String? = null,
    val equippedTitleLabel: String? = null,
    val lastActiveAt: Long = 0L,
    val shareLastActive: Boolean = true,
)

data class GrovePulse(
    val friendCount: Int,
    val plantedFriends: List<GroveMember>,
    val copy: String,
    val firstInGrove: Boolean,
    val rival: GroveMember?,
    val iPlantedToday: Boolean,
)

/**
 * Friends Grove extras beyond the spec:
 * - Rival pin: one friend at a time, Home shows planted vs still growing.
 * - Grove Lead frame (`frame_grove_lead`) is earned by weekly #1 among friends — never sold.
 */
class GroveViewModel(application: Application) : AndroidViewModel(application) {

    private val themePref = ThemePreference(application)
    private val users = UserRepository()
    private val social = GroveSocialRepository()

    private val _myPresence = MutableStateFlow<GroveMember?>(null)
    private val _witnessed = MutableStateFlow<Set<String>>(emptySet())
    private val _openBoard = MutableStateFlow(OpenGroveBoard(GroveCalendar.weekKey(), 0, emptyList()))
    private val _openGroveHistory = MutableStateFlow<List<OpenGroveWeekRank>>(emptyList())
    private val _firstInMoment = MutableStateFlow(false)
    private val witnessing = ConcurrentHashMap.newKeySet<String>()
    val firstInMoment: StateFlow<Boolean> = _firstInMoment.asStateFlow()

    val lookAtOpenGrove: StateFlow<Boolean> = themePref.lookAtOpenGrove.stateIn(
        viewModelScope, SharingStarted.Eagerly, false,
    )
    val shareLastActive: StateFlow<Boolean> = themePref.shareLastActive.stateIn(
        viewModelScope, SharingStarted.Eagerly, true,
    )
    val rivalUid: StateFlow<String?> = themePref.groveRivalUid.stateIn(
        viewModelScope, SharingStarted.Eagerly, null,
    )
    val openBoard: StateFlow<OpenGroveBoard> = _openBoard.asStateFlow()
    val openGroveHistory: StateFlow<List<OpenGroveWeekRank>> = _openGroveHistory.asStateFlow()
    val witnessedToday: StateFlow<Set<String>> = _witnessed.asStateFlow()
    val myPresence: StateFlow<GroveMember?> = _myPresence.asStateFlow()

    fun membersFrom(friends: List<FriendSummary>): List<GroveMember> {
        val week = GroveCalendar.weekKey()
        val today = GroveCalendar.todayKey()
        val me = _myPresence.value
        val ranked = friends.map { it.toMember(today, week, isSelf = false) } + listOfNotNull(me)
        val leadUid = ranked.maxWithOrNull(
            compareBy<GroveMember> { it.weeklyPlantDays }.thenBy { it.currentStreakPublic },
        )?.takeIf { it.weeklyPlantDays > 0 }?.uid
        return ranked
            .filter { it.uid.isNotBlank() }
            .distinctBy { it.uid }
            .map { it.copy(groveLead = it.uid == leadUid || it.groveLead) }
            .sortedWith(
                compareByDescending<GroveMember> { it.plantedToday }
                    .thenByDescending { it.weeklyPlantDays }
                    .thenByDescending { it.currentStreakPublic }
                    .thenBy { it.username.lowercase() },
            )
    }

    fun pulse(members: List<GroveMember>): GrovePulse {
        val friends = members.filter { !it.isSelf }
        val planted = friends.filter { it.plantedToday }
        val me = members.firstOrNull { it.isSelf }
        val iPlanted = me?.plantedToday == true
        val copy = when {
            friends.isEmpty() -> "Invite someone into the grove."
            planted.isEmpty() && !iPlanted -> "The grove is still waking"
            planted.isEmpty() && iPlanted -> "You're the first in the grove today"
            planted.size == 1 -> "1 in your grove already planted"
            else -> "${planted.size} in your grove already planted"
        }
        val first = friends.size >= 2 && iPlanted && planted.isEmpty()
        val rival = rivalUid.value?.let { id -> friends.firstOrNull { it.uid == id } }
        return GrovePulse(friends.size, planted, copy, first, rival, iPlanted)
    }

    fun refreshMe() {
        viewModelScope.launch(Dispatchers.IO) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val p = users.fetchPublicProfile(uid) ?: return@launch
            val today = GroveCalendar.todayKey()
            val week = GroveCalendar.weekKey()
            _myPresence.value = GroveMember(
                uid = uid,
                username = p.username.ifBlank { p.displayName.ifBlank { "you" } },
                photoUrl = p.photoUrl,
                membershipTier = p.membershipTier,
                titleGlowId = p.titleGlowId,
                profileFrameId = p.profileFrameId,
                grovePlantId = p.grovePlantId,
                pinnedTrophyLabel = p.livePinnedLabel(),
                plantedToday = GroveCalendar.plantedToday(p.lastPlantedDay, java.time.LocalDate.now()),
                weeklyPlantDays = if (p.weeklyPlantWeekKey == week) p.weeklyPlantDays else 0,
                currentStreakPublic = p.currentStreakPublic,
                groveLead = p.groveLeadUntil > System.currentTimeMillis(),
                witnessCount = if (p.witnessDay == today) p.witnessCountToday else 0,
                isSelf = true,
                standInOpenGrove = p.standInOpenGrove,
                openGroveWeekRank = p.openGroveWeekRank,
                openGrovePercentile = p.openGrovePercentile,
                plantedCountToday = p.plantedCountToday,
                sharedHabitTitle = p.sharedPulseHabit?.takeIf {
                    !it.isBlank() && p.sharedPulseDay == today
                },
                lastPlantedDay = p.lastPlantedDay,
                equippedTitleLabel = p.equippedTitleLabel,
                lastActiveAt = p.lastActiveAt,
                shareLastActive = p.shareLastActive,
            )
            _witnessed.value = themePref.witnessedUidsToday(today)
        }
    }

    fun refreshOpenGrove() {
        viewModelScope.launch(Dispatchers.IO) {
            _openBoard.value = social.fetchOpenGroveBoard()
        }
    }

    fun refreshOpenGroveHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _openGroveHistory.value = social.fetchMyOpenGroveHistory()
        }
    }

    fun setLook(value: Boolean) {
        viewModelScope.launch { themePref.setLookAtOpenGrove(value) }
        if (value) refreshOpenGrove()
    }

    fun setStand(value: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { users.setStandInOpenGrove(value) }
            runCatching {
                FirebaseFunctions.getInstance()
                    .getHttpsCallable("refreshOpenGrove")
                    .call()
                    .await()
            }
            refreshMe()
            refreshOpenGrove()
        }
    }

    fun setRival(uid: String?) {
        viewModelScope.launch { themePref.setGroveRivalUid(uid) }
    }

    fun witness(targetUid: String, onResult: (Boolean, String?) -> Unit) {
        if (targetUid.isBlank() || _myPresence.value?.uid == targetUid) {
            onResult(false, "Can't witness that")
            return
        }
        if (targetUid in _witnessed.value || !witnessing.add(targetUid)) {
            onResult(true, null)
            return
        }
        val previous = _witnessed.value
        _witnessed.value = previous + targetUid
        viewModelScope.launch(Dispatchers.IO) {
            val r = social.witness(targetUid)
            if (r.isSuccess) {
                themePref.addWitnessedUid(GroveCalendar.todayKey(), targetUid)
            } else {
                _witnessed.value = previous
            }
            witnessing.remove(targetUid)
            withContext(Dispatchers.Main) {
                onResult(r.isSuccess, r.exceptionOrNull()?.message)
            }
        }
    }

    fun onIPlanted(previousWeekly: Int, previousStreak: Int, friends: List<FriendSummary>, myUsername: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val mine = users.fetchPublicProfile(uid) ?: return@launch
                val week = GroveCalendar.weekKey()
                val myWeekly = if (mine.weeklyPlantWeekKey == week) mine.weeklyPlantDays else 0
                val myStreak = mine.currentStreakPublic
                friends.forEach { friend ->
                    val theirsWeekly = if (friend.weeklyPlantWeekKey == week) friend.weeklyPlantDays else 0
                    if (GroveCalendar.passed(previousWeekly, previousStreak, myWeekly, myStreak, theirsWeekly, friend.currentStreakPublic)) {
                        SocialNotificationDispatcher.notifyGroveOvertake(
                            targetUid = friend.uid,
                            fromUsername = myUsername,
                            weeklyDays = myWeekly,
                            fromPhotoUrl = mine.photoUrl,
                        )
                    }
                }
                val plantedFriends = friends.count { GroveCalendar.plantedToday(it.lastPlantedDay) }
                _firstInMoment.value = friends.size >= 2 && plantedFriends == 0
                val lead = (friends + listOf(
                    FriendSummary(uid, mine.username, mine.photoUrl, weeklyPlantDays = myWeekly, currentStreakPublic = myStreak),
                )).maxWithOrNull(compareBy<FriendSummary> { it.weeklyPlantDays }.thenBy { it.currentStreakPublic })
                if (lead?.uid == uid && myWeekly > 0) {
                    val until = System.currentTimeMillis() + java.util.concurrent.TimeUnit.DAYS.toMillis(7)
                    runCatching { users.setGroveLeadUntil(until) }
                }
            }
            refreshMe()
        }
    }

    fun consumeFirstInMoment() {
        _firstInMoment.value = false
    }

    private fun FriendSummary.toMember(today: String, week: String, isSelf: Boolean): GroveMember {
        val pinLive = !pinnedTrophyLabel.isNullOrBlank() && pinnedTrophyUntil > System.currentTimeMillis()
        return GroveMember(
            uid = uid,
            username = username,
            photoUrl = photoUrl,
            membershipTier = membershipTier,
            titleGlowId = titleGlowId,
            profileFrameId = profileFrameId,
            grovePlantId = grovePlantId,
            pinnedTrophyLabel = if (pinLive) pinnedTrophyLabel else null,
            plantedToday = lastPlantedDay == today,
            weeklyPlantDays = if (weeklyPlantWeekKey == week || weeklyPlantWeekKey.isNullOrBlank()) weeklyPlantDays else 0,
            currentStreakPublic = currentStreakPublic,
            groveLead = groveLeadUntil > System.currentTimeMillis(),
            witnessCount = if (witnessDay == today) witnessCountToday else 0,
            isSelf = isSelf,
            plantedCountToday = plantedCountToday,
            sharedHabitTitle = sharedPulseHabit?.takeIf {
                !it.isBlank() && sharedPulseDay == today
            },
            lastPlantedDay = lastPlantedDay,
            equippedTitleLabel = equippedTitleLabel,
            lastActiveAt = lastActiveAt,
            shareLastActive = shareLastActive,
        )
    }

    fun sharePulse(habitTitle: String) {
        val title = habitTitle.trim().take(48)
        if (title.isBlank()) return
        _myPresence.value = _myPresence.value?.copy(sharedHabitTitle = title)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { users.shareGrovePulse(title) }
            refreshMe()
        }
    }

    fun clearPulseShare() {
        _myPresence.value = _myPresence.value?.copy(sharedHabitTitle = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { users.clearGrovePulseShare() }
            refreshMe()
        }
    }

    private fun com.saintnico.verdlyhabits.data.model.PublicUserProfile.livePinnedLabel(): String? {
        return if (pinnedTrophyUntil > System.currentTimeMillis() && !pinnedTrophyLabel.isNullOrBlank()) {
            pinnedTrophyLabel
        } else null
    }

    init {
        viewModelScope.launch {
            firebaseAuthUidFlow().collect {
                refreshMe()
                refreshOpenGrove()
            }
        }
    }
}
