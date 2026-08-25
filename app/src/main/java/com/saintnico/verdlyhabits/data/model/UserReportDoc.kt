package com.saintnico.verdlyhabits.data.model

import com.google.firebase.Timestamp

data class UserReportDoc(
    val id: String,
    val reporterUid: String,
    val reporterUsername: String,
    val reportedUid: String,
    val reportedUsername: String,
    val reportedDisplayName: String,
    val reasonCode: String,
    val reasonLabel: String,
    val details: String,
    val source: String,
    val status: String,
    val submitCount: Int,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
)

enum class UserReportStatus(val label: String) {
    Pending("Pending review"),
    Reviewing("Under review"),
    Resolved("Resolved"),
    Dismissed("Closed"),
    ;

    companion object {
        fun labelFor(code: String?): String =
            entries.firstOrNull { it.name.equals(code, ignoreCase = true) }?.label
                ?: Pending.label
    }
}
