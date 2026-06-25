package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.model.InboxNotification
import com.saintnico.verdlyhabits.data.model.InboxNotificationType
import com.saintnico.verdlyhabits.data.model.NotificationCategory
import com.saintnico.verdlyhabits.data.remote.firestore.InboxRepository
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
)

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val inboxRepository = InboxRepository()
    private val _selectedCategory = MutableStateFlow(NotificationCategory.ALL)
    val selectedCategory: StateFlow<NotificationCategory> = _selectedCategory.asStateFlow()

    private val inboxItems: StateFlow<List<InboxNotification>> = firebaseAuthUidFlow()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(emptyList())
            else inboxRepository.observeInbox(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<NotificationsUiState> = combine(
        inboxItems,
        _selectedCategory,
    ) { items, category ->
        val filtered = when (category) {
            NotificationCategory.ALL -> items
            else -> items.filter { it.type.category == category }
        }
        val unread = items.count { !it.read }
        val pending = items.count {
            !it.read && it.actionState == "pending" && it.type in setOf(
                InboxNotificationType.FRIEND_REQUEST,
                InboxNotificationType.DUO_INVITE,
                InboxNotificationType.ARENA_INVITE,
            )
        }
        NotificationsUiState(
            items = items,
            filtered = filtered,
            selectedCategory = category,
            unreadCount = unread,
            pendingActionCount = pending,
            isLoading = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        NotificationsUiState(isLoading = true),
    )

    fun selectCategory(category: NotificationCategory) {
        _selectedCategory.value = category
    }

    fun markRead(notificationId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            inboxRepository.markRead(uid, notificationId)
        }
    }

    fun markAllRead() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            inboxRepository.markAllRead(uid)
        }
    }

    fun resolveAction(notificationId: String, actionState: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            inboxRepository.updateActionState(uid, notificationId, actionState)
        }
    }
}
