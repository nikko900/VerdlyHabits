package com.saintnico.verdlyhabits.engine.home

import java.time.LocalDate
import java.time.ZoneId

sealed class HomeFeedPost {
    abstract val id: String
    abstract val actorUid: String?
    abstract val weight: Int
    abstract val eventAtMillis: Long

    data class DuoMoment(
        override val id: String,
        override val actorUid: String,
        override val eventAtMillis: Long,
        val username: String,
        val photoUrl: String?,
        val streakDays: Int,
        val bothLocked: Boolean,
        val atRisk: Boolean,
        val buddyDone: Boolean,
        val myDone: Boolean,
        override val weight: Int = WEIGHT_DUO,
    ) : HomeFeedPost()

    data class Planted(
        override val id: String,
        override val actorUid: String,
        override val eventAtMillis: Long,
        val username: String,
        val photoUrl: String?,
        val streakDays: Int,
        val witnessCount: Int,
        val titleGlowId: String?,
        val profileFrameId: String?,
        val sharedHabit: String? = null,
        override val weight: Int = WEIGHT_PLANTED,
    ) : HomeFeedPost()

    data class ProofPhoto(
        override val id: String,
        override val actorUid: String,
        override val eventAtMillis: Long,
        val username: String,
        val photoUrl: String?,
        val habitName: String,
        val proofUrl: String,
        val challengeId: String,
        val streakDays: Int,
        val reactionCounts: Map<String, Int> = emptyMap(),
        override val weight: Int = WEIGHT_PROOF,
    ) : HomeFeedPost()

    data class ChallengeDone(
        override val id: String,
        override val actorUid: String,
        override val eventAtMillis: Long,
        val username: String,
        val photoUrl: String?,
        val habitName: String,
        val challengeId: String,
        override val weight: Int = WEIGHT_CHALLENGE_TEXT,
    ) : HomeFeedPost()

    data class GroveLead(
        override val id: String,
        override val actorUid: String,
        override val eventAtMillis: Long,
        val username: String,
        val photoUrl: String?,
        override val weight: Int = WEIGHT_GROVE_LEAD,
    ) : HomeFeedPost()

    data class Quote(
        override val id: String,
        val text: String,
        override val eventAtMillis: Long = 0L,
        override val actorUid: String? = null,
        override val weight: Int = WEIGHT_QUOTE,
    ) : HomeFeedPost()

    data class GroveWaking(
        override val id: String = "grove_waking",
        override val eventAtMillis: Long = 0L,
        override val actorUid: String? = null,
        override val weight: Int = WEIGHT_GROVE_LEAD,
    ) : HomeFeedPost()
}

const val WEIGHT_DUO = 100
const val WEIGHT_PLANTED = 90
const val WEIGHT_PROOF = 85
const val WEIGHT_CHALLENGE_TEXT = 70
const val WEIGHT_GROVE_LEAD = 60
const val WEIGHT_QUOTE = 40

object HomeFeedRanker {

    /**
     * Deterministic rank: category weight, then recency, then id.
     * Then interleave so the same [HomeFeedPost.actorUid] never appears twice in a row
     * (posts with null actorUid are allowed next to anyone).
     * Quote is slotted after the 2nd person-post when at least 2 exist, else after the 3rd
     * if only 3+, else at the end of person posts.
     */
    fun rank(
        candidates: List<HomeFeedPost>,
        quoteAfterPersonPosts: Int = 2,
    ): List<HomeFeedPost> {
        val quote = candidates.filterIsInstance<HomeFeedPost.Quote>().firstOrNull()
        val waking = candidates.filterIsInstance<HomeFeedPost.GroveWaking>().firstOrNull()
        val people = candidates.filter { it.actorUid != null }
        val sorted = people.sortedWith(
            compareByDescending<HomeFeedPost> { it.weight }
                .thenByDescending { it.eventAtMillis }
                .thenBy { it.id },
        )
        val interleaved = interleave(sorted)
        val withQuote = insertQuote(interleaved, quote, quoteAfterPersonPosts)
        return if (waking != null && interleaved.isEmpty()) {
            listOfNotNull(waking, quote)
        } else {
            withQuote
        }
    }

    fun interleave(sorted: List<HomeFeedPost>): List<HomeFeedPost> {
        if (sorted.size <= 1) return sorted
        val remaining = sorted.toMutableList()
        val out = ArrayList<HomeFeedPost>(sorted.size)
        while (remaining.isNotEmpty()) {
            val lastUid = out.lastOrNull()?.actorUid
            val pick = remaining.indexOfFirst { post ->
                val uid = post.actorUid
                uid == null || uid != lastUid
            }.takeIf { it >= 0 } ?: 0
            out += remaining.removeAt(pick)
        }
        return out
    }

    internal fun insertQuote(
        people: List<HomeFeedPost>,
        quote: HomeFeedPost.Quote?,
        afterPersonPosts: Int,
    ): List<HomeFeedPost> {
        if (quote == null) return people
        if (people.isEmpty()) return listOf(quote)
        val slot = afterPersonPosts.coerceIn(1, 3).coerceAtMost(people.size)
        return people.take(slot) + quote + people.drop(slot)
    }
}

fun localDayStartMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()

fun parseDayStartMillis(isoDate: String?, zone: ZoneId = ZoneId.systemDefault()): Long {
    if (isoDate.isNullOrBlank()) return 0L
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return 0L
    return localDayStartMillis(date, zone)
}
