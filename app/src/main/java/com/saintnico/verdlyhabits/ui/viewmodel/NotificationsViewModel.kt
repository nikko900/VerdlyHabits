package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.model.DevAnnouncement
import com.saintnico.verdlyhabits.data.model.InboxNotification
import com.saintnico.verdlyhabits.data.model.InboxNotificationType
import com.saintnico.verdlyhabits.data.model.NotificationCategory
import com.saintnico.verdlyhabits.data.remote.firestore.AnnouncementsRepository
import com.saintnico.verdlyhabits.data.remote.firestore.InboxRepository
import com.saintnico.verdlyhabits.engine.ActivityFeedMerger
import com.saintnico.verdlyhabits.engine.ProfileSocialEngine
import com.saintnico.verdlyhabits.notifications.LiveSocialNotificationsSource
import com.saintnico.verdlyhabits.notifications.mergeInboxWithLive
import com.saintnico.verdlyhabits.session.firebaseAuthUidFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<InboxNotification> = emptyList(),
    val filtered: List<InboxNotification> = emptyList(),
    val selectedCategory: NotificationCategory = NotificationCategory.ALL,
    val unreadCount: Int = 0,
    val pendingActionCount: Int = 0,
    val isLoading: Boolean = true,
    val announcements: List<DevAnnouncement> = emptyList(),
    val dismissedAnnouncementIds: Set<String> = emptySet(),
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val inboxRepository = InboxRepository()
    private val announcementsRepository = AnnouncementsRepository()
    private val liveSocialSource = LiveSocialNotificationsSource()
    private val _selectedCategory = MutableStateFlow(NotificationCategory.ALL)
    val selectedCategory: StateFlow<NotificationCategory> = _selectedCategory.asStateFlow()
    private val _isPro = MutableStateFlow(false)

    fun setPremiumAccess(isPro: Boolean) {
        _isPro.value = isPro
    }

    private val mergedInbox: StateFlow<List<InboxNotification>> = firebaseAuthUidFlow()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList())
            else combine(
                inboxRepository.observeInbox(uid),
                liveSocialSource.observe(uid),
            ) { inbox, live -> mergeInboxWithLive(inbox, live) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val announcements: StateFlow<List<DevAnnouncement>> = combine(
        firebaseAuthUidFlow(),
        _isPro,
    ) { uid, isPro -> uid to isPro }
        .flatMapLatest { (uid, isPro) ->
            if (uid.isNullOrBlank()) flowOf(emptyList())
            else announcementsRepository.observeActive(isPro)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val dismissedAnnouncementIds: StateFlow<Set<String>> = firebaseAuthUidFlow()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptySet())
            else announcementsRepository.observeDismissedIds(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val state: StateFlow<NotificationsUiState> = combine(
        mergedInbox,
        announcements,
        dismissedAnnouncementIds,
        _selectedCategory,
    ) { items, announcementList, dismissed, category ->
        val announcementNotifications = announcementList
            .filter { it.id !in dismissed }
            .map { ann ->
                InboxNotification(
                    id = "announcement_${ann.id}",
                    type = InboxNotificationType.SYSTEM,
                    title = ann.title,
                    body = ann.body,
                    referenceId = ann.id,
                    route = ann.route.ifBlank { null },
                    read = false,
                    createdAtMillis = ann.createdAtMillis.coerceAtLeast(1L),
                    actionState = "none",
                )
            }
        val allItems = (items + announcementNotifications)
            .sortedWith(
                compareByDescending<InboxNotification> { it.actionState == "pending" }
                    .thenByDescending { it.createdAtMillis }
                    .thenBy { it.id },
            )
        val filtered = when (category) {
            NotificationCategory.ALL -> allItems
            else -> allItems.filter { it.type.category == category }
        }
        val unread = allItems.count { !it.read }
        val pending = allItems.count {
            !it.read && it.actionState == "pending" && it.type in setOf(
                InboxNotificationType.FRIEND_REQUEST,
                InboxNotificationType.DUO_INVITE,
                InboxNotificationType.ARENA_INVITE,
            )
        }
        NotificationsUiState(
            items = allItems,
            filtered = filtered,
            selectedCategory = category,
            unreadCount = unread,
            pendingActionCount = pending,
            isLoading = false,
            announcements = announcementList,
            dismissedAnnouncementIds = dismissed,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NotificationsUiState(isLoading = true),
    )

    /** Merged Live Pulse feed for profile — pass local engine items, get unified list. */
    fun pulseFeed(localPulse: List<ProfileSocialEngine.ProfileActivityItem>): List<ProfileSocialEngine.ProfileActivityItem> {
        val s = state.value
        return ActivityFeedMerger.merge(
            localPulse = localPulse,
            inbox = s.items,
            announcements = s.announcements,
            dismissedAnnouncementIds = s.dismissedAnnouncementIds,
        ).take(8)
    }

    fun selectCategory(category: NotificationCategory) {
        _selectedCategory.value = category
    }

    fun openActivityTab() {
        _selectedCategory.value = NotificationCategory.ACTIVITY
    }

    fun markRead(notificationId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (notificationId.startsWith("announcement_")) {
            val annId = notificationId.removePrefix("announcement_")
            viewModelScope.launch {
                announcementsRepository.markDismissed(uid, annId)
            }
            return
        }
        viewModelScope.launch {
            inboxRepository.markRead(uid, notificationId)
        }
    }

    fun markAllRead() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            inboxRepository.markAllRead(uid)
            state.value.announcements.forEach { ann ->
                announcementsRepository.markDismissed(uid, ann.id)
            }
        }
    }

    fun resolveAction(notificationId: String, actionState: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            inboxRepository.updateActionState(uid, notificationId, actionState)
        }
    }
}
