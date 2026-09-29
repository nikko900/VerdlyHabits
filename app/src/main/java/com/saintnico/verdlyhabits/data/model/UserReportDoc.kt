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
    /** pending → reviewing → resolved */
    val status: String,
    /**
     * Final decision once status is resolved:
     * no_action | warned | banned | removed_content
     */
    val outcome: String,
    /** Short note you (admin) write for the reporter, shown in-app. */
    val resolutionNote: String,
    val submitCount: Int,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
    val resolvedAt: Timestamp?,
)

enum class UserReportStatus(val code: String, val label: String) {
    Pending("pending", "Received"),
    Reviewing("reviewing", "In review"),
    Resolved("resolved", "Closed"),
    ;

    companion object {
        fun from(code: String?): UserReportStatus {
            val n = code.orEmpty().trim().lowercase()
            return when (n) {
                "reviewing", "under_review", "investigating" -> Reviewing
                "resolved", "action_taken", "closed", "dismissed" -> Resolved
                // legacy outcome-as-status
                "banned", "warned", "no_action" -> Resolved
                else -> Pending
            }
        }

        fun labelFor(code: String?): String = from(code).label
    }
}

enum class UserReportOutcome(val code: String, val label: String, val reporterMessage: String) {
    None("", ".", ""),
    NoAction(
        "no_action",
        "No violation found",
        "We reviewed this report and didn’t find a clear community guideline violation. Thanks for looking out for the grove.",
    ),
    Warned(
        "warned",
        "Warning issued",
        "We reviewed your report and issued a warning to that account. Thanks for helping keep Verdly safe.",
    ),
    Banned(
        "banned",
        "Account restricted",
        "We reviewed your report and restricted that account. Thanks for speaking up.",
    ),
    RemovedContent(
        "removed_content",
        "Content removed",
        "We removed content tied to this report. Thanks for helping keep Verdly safe.",
    ),
    ;

    companion object {
        fun from(code: String?): UserReportOutcome {
            val n = code.orEmpty().trim().lowercase()
            return entries.firstOrNull { it.code == n }
                ?: when (n) {
                    "dismissed", "cleared", "innocent" -> NoAction
                    "warn", "warning" -> Warned
                    "ban", "banned_user", "suspended" -> Banned
                    "removed", "takedown" -> RemovedContent
                    else -> None
                }
        }
    }
}
