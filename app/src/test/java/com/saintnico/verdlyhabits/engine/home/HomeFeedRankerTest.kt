package com.saintnico.verdlyhabits.engine.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFeedRankerTest {

    private fun planted(uid: String, weight: Int = WEIGHT_PLANTED, at: Long = 10L, id: String = "p_$uid") =
        HomeFeedPost.Planted(
            id = id,
            actorUid = uid,
            eventAtMillis = at,
            username = uid,
            photoUrl = null,
            streakDays = 3,
            witnessCount = 0,
            titleGlowId = null,
            profileFrameId = null,
            weight = weight,
        )

    @Test
    fun higherWeightComesFirst() {
        val duo = HomeFeedPost.DuoMoment(
            id = "duo",
            actorUid = "b",
            eventAtMillis = 1L,
            username = "b",
            photoUrl = null,
            streakDays = 2,
            bothLocked = true,
            atRisk = false,
            buddyDone = true,
            myDone = true,
        )
        val ranked = HomeFeedRanker.rank(listOf(planted("a", at = 99L), duo))
        assertEquals("duo", ranked.first().id)
    }

    @Test
    fun sameWeightSortsByRecencyThenId() {
        val older = planted("a", at = 1L, id = "old")
        val newer = planted("c", at = 50L, id = "new")
        val ranked = HomeFeedRanker.rank(listOf(older, newer))
        assertEquals("new", ranked.first().id)
    }

    @Test
    fun interleaveBreaksSameUidStreakWithoutDropping() {
        val a1 = planted("a", weight = 90, at = 30L, id = "a1")
        val a2 = planted("a", weight = 85, at = 20L, id = "a2")
        val b = planted("b", weight = 80, at = 10L, id = "b1")
        val ranked = HomeFeedRanker.interleave(listOf(a1, a2, b))
        assertEquals(listOf("a1", "b1", "a2"), ranked.map { it.id })
    }

    @Test
    fun rankDoesNotReshuffleGivenSameInput() {
        val posts = listOf(
            planted("z", at = 5L, id = "z"),
            planted("a", at = 9L, id = "a"),
            HomeFeedPost.Quote(id = "q", text = "stay"),
        )
        val first = HomeFeedRanker.rank(posts)
        val second = HomeFeedRanker.rank(posts)
        assertEquals(first.map { it.id }, second.map { it.id })
    }

    @Test
    fun quoteSlotsAfterSecondPersonPost() {
        val ranked = HomeFeedRanker.rank(
            listOf(
                planted("a", at = 3L, id = "a"),
                planted("b", at = 2L, id = "b"),
                planted("c", at = 1L, id = "c"),
                HomeFeedPost.Quote(id = "q", text = "stay"),
            ),
        )
        assertEquals("q", ranked[2].id)
        assertTrue(ranked[0] is HomeFeedPost.Planted)
        assertTrue(ranked[1] is HomeFeedPost.Planted)
    }

    @Test
    fun wakingShowsWhenNoPersonPosts() {
        val ranked = HomeFeedRanker.rank(
            listOf(
                HomeFeedPost.GroveWaking(),
                HomeFeedPost.Quote(id = "q", text = "stay"),
            ),
        )
        assertEquals("grove_waking", ranked.first().id)
        assertEquals("q", ranked[1].id)
    }
}
