package com.saintnico.verdlyhabits.challenge

import android.content.Context

/** Stores a challenge join code from a deep link until the user reaches Challenges. */
object ChallengeInviteManager {

    private const val PREFS = "verdly_challenge_invite"
    private const val KEY_PENDING = "pending_join_input"

    fun storePendingJoin(context: Context, rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PENDING, trimmed)
            .apply()
    }

    fun peekPendingJoin(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PENDING, null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    fun clearPendingJoin(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_PENDING)
            .apply()
    }
}
