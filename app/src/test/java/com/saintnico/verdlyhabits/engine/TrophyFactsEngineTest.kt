package com.saintnico.verdlyhabits.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TrophyFactsEngineTest {

    private val zone: ZoneId = ZoneId.of("Africa/Nairobi")
    private val today: LocalDate = LocalDate.of(2026, 9, 7)

    @Test
    fun statsVisitsTrackConsecutiveDays() {
        var facts = TrophyFacts()
        facts = TrophyFactsEngine.onStatsOpen(facts, today)
        facts = TrophyFactsEngine.onStatsOpen(facts, today)
        assertEquals(2, facts.statsVisitCount)
        assertEquals(1, facts.consecutiveStatsDays)

        facts = TrophyFactsEngine.onStatsOpen(facts, today.plusDays(1))
        assertEquals(3, facts.statsVisitCount)
        assertEquals(2, facts.consecutiveStatsDays)

        facts = TrophyFactsEngine.onStatsOpen(facts, today.plusDays(3))
        assertEquals(1, facts.consecutiveStatsDays)
    }

    @Test
    fun appOpenStreakBreaksAfterAGap() {
        var facts = TrophyFactsEngine.onAppOpen(TrophyFacts(), today)
        facts = TrophyFactsEngine.onAppOpen(facts, today.plusDays(1))
        facts = TrophyFactsEngine.onAppOpen(facts, today.plusDays(2))
        assertEquals(3, facts.consecutiveOpenDays)
        facts = TrophyFactsEngine.onAppOpen(facts, today.plusDays(4))
        assertEquals(1, facts.consecutiveOpenDays)
    }

    @Test
    fun earlyBirdRequiresFiveConsecutiveMornings() {
        var facts = TrophyFacts()
        repeat(5) { day ->
            facts = TrophyFactsEngine.onHabitComplete(
                facts = facts,
                today = today.plusDays(day.toLong()),
                time = LocalTime.of(6, 30),
                habitId = "h1",
                plantedAt = 1L,
                previousStreak = day,
                newStreak = day + 1,
                isPerfectDay = false,
                completedTodayCount = 1,
                reminderEnabled = false,
                reminderMinutes = null,
                hasPhotoProof = false,
                isInstallDay = false,
                zone = zone,
            )
        }
        assertEquals(5, facts.consecutiveEarlyBirdDays)
        assertEquals(0, facts.earlyMorningCount)
    }

    @Test
    fun noExcusesFiresAnHourAfterTheReminder() {
        val facts = TrophyFactsEngine.onHabitComplete(
            facts = TrophyFacts(),
            today = today,
            time = LocalTime.of(9, 5),
            habitId = "h1",
            plantedAt = 1L,
            previousStreak = 0,
            newStreak = 1,
            isPerfectDay = false,
            completedTodayCount = 1,
            reminderEnabled = true,
            reminderMinutes = 8 * 60,
            hasPhotoProof = false,
            isInstallDay = false,
            zone = zone,
        )
        assertTrue(facts.noExcuses)
    }

    @Test
    fun twoHabitsSameDayIncrementsOnce() {
        var facts = TrophyFactsEngine.onHabitComplete(
            facts = TrophyFacts(),
            today = today,
            time = LocalTime.of(12, 0),
            habitId = "h1",
            plantedAt = 1L,
            previousStreak = 0,
            newStreak = 1,
            isPerfectDay = false,
            completedTodayCount = 1,
            reminderEnabled = false,
            reminderMinutes = null,
            hasPhotoProof = false,
            isInstallDay = false,
            zone = zone,
        )
        facts = TrophyFactsEngine.onHabitComplete(
            facts = facts,
            today = today,
            time = LocalTime.of(13, 0),
            habitId = "h2",
            plantedAt = 1L,
            previousStreak = 0,
            newStreak = 1,
            isPerfectDay = false,
            completedTodayCount = 2,
            reminderEnabled = false,
            reminderMinutes = null,
            hasPhotoProof = true,
            isInstallDay = true,
            zone = zone,
        )
        assertEquals(1, facts.twoHabitDays)
        assertEquals(1, facts.photoProofs)
        assertTrue(facts.launchDayPerfect.not())
        facts = TrophyFactsEngine.onHabitComplete(
            facts = facts,
            today = today,
            time = LocalTime.of(14, 0),
            habitId = "h3",
            plantedAt = 1L,
            previousStreak = 0,
            newStreak = 1,
            isPerfectDay = true,
            completedTodayCount = 3,
            reminderEnabled = false,
            reminderMinutes = null,
            hasPhotoProof = false,
            isInstallDay = true,
            zone = zone,
        )
        assertEquals(1, facts.twoHabitDays)
        assertTrue(facts.launchDayPerfect)
        assertEquals(1, facts.perfectDayCount)
    }

    @Test
    fun recoveryThenSurpassCountsStillStanding() {
        var facts = TrophyFacts(peakStreakCsv = "h1:10")
        facts = TrophyFactsEngine.onHabitComplete(
            facts = facts,
            today = today,
            time = LocalTime.of(10, 0),
            habitId = "h1",
            plantedAt = 1L,
            previousStreak = 0,
            newStreak = 1,
            isPerfectDay = false,
            completedTodayCount = 1,
            reminderEnabled = false,
            reminderMinutes = null,
            hasPhotoProof = false,
            isInstallDay = false,
            zone = zone,
        )
        assertEquals(1, facts.recoveryCount)
        assertEquals(0, facts.surpassedCount)

        facts = TrophyFactsEngine.onHabitComplete(
            facts = facts.copy(peakStreakCsv = "h1:10", pendingSurpassCsv = "h1"),
            today = today.plusDays(10),
            time = LocalTime.of(10, 0),
            habitId = "h1",
            plantedAt = 1L,
            previousStreak = 10,
            newStreak = 11,
            isPerfectDay = false,
            completedTodayCount = 1,
            reminderEnabled = false,
            reminderMinutes = null,
            hasPhotoProof = false,
            isInstallDay = false,
            zone = zone,
        )
        assertEquals(1, facts.surpassedCount)
        assertTrue(facts.pendingSurpassCsv.isBlank())
    }

    @Test
    fun chainReactionDetectsSameWeekFourteenDayHits() {
        val facts = TrophyFacts(firstHit14Csv = "a:2026-09-01,b:2026-09-06")
        assertEquals(2, TrophyFactsEngine.chainReactionProgress(facts))
        val far = TrophyFacts(firstHit14Csv = "a:2026-09-01,b:2026-09-20")
        assertEquals(1, TrophyFactsEngine.chainReactionProgress(far))
    }

    @Test
    fun reminderParserReadsHourMinute() {
        assertEquals(8 * 60 + 30, TrophyFactsEngine.parseReminderMinutes("08:30"))
        assertEquals(null, TrophyFactsEngine.parseReminderMinutes("nope"))
    }
}

class TrophyEvaluatorTest {

    @Test
    fun firstLightRequiresACompletion() {
        val locked = TrophyEvaluator.evaluate(
            habits = emptyList(),
            totalCompletions = 0,
            currentLevel = 1,
            languageChanged = false,
            existing = emptyList(),
        )
        assertFalse(locked.first { it.id == "first_light" }.isUnlocked)

        val unlocked = TrophyEvaluator.evaluate(
            habits = emptyList(),
            totalCompletions = 1,
            currentLevel = 1,
            languageChanged = false,
            existing = locked,
        )
        assertTrue(unlocked.first { it.id == "first_light" }.isUnlocked)
    }

    @Test
    fun observerUsesConsecutiveStatsDaysNotTotalVisits() {
        val facts = TrophyFacts(statsVisitCount = 21, consecutiveStatsDays = 3)
        val evaluated = TrophyEvaluator.evaluate(
            habits = emptyList(),
            totalCompletions = 0,
            currentLevel = 1,
            languageChanged = false,
            existing = emptyList(),
            facts = facts,
        )
        assertFalse(evaluated.first { it.id == "the_observer" }.isUnlocked)
        assertEquals(3, evaluated.first { it.id == "the_observer" }.progressCurrent)
        assertTrue(evaluated.first { it.id == "data_driven" }.isUnlocked)
        assertTrue(evaluated.first { it.id == "first_stats" }.isUnlocked)
    }

    @Test
    fun launchDayStaysLockedUnlessInstallDayWasPerfect() {
        val locked = TrophyEvaluator.evaluate(
            habits = emptyList(),
            totalCompletions = 4,
            currentLevel = 1,
            languageChanged = false,
            existing = emptyList(),
            facts = TrophyFacts(perfectDayCount = 4),
        )
        assertFalse(locked.first { it.id == "launch_day" }.isUnlocked)

        val unlocked = TrophyEvaluator.evaluate(
            habits = emptyList(),
            totalCompletions = 4,
            currentLevel = 1,
            languageChanged = false,
            existing = locked,
            facts = TrophyFacts(launchDayPerfect = true),
        )
        assertTrue(unlocked.first { it.id == "launch_day" }.isUnlocked)
    }

    @Test
    fun newSocialTrophiesUnlockFromFacts() {
        val evaluated = TrophyEvaluator.evaluate(
            habits = emptyList(),
            totalCompletions = 0,
            currentLevel = 1,
            languageChanged = true,
            existing = emptyList(),
            facts = TrophyFacts(
                goalsCreated = 1,
                goalsCompleted = 1,
                focusSessions = 10,
                focusMinutes = 60,
                friendsCount = 5,
                profilesViewed = 1,
                profileViewsReceived = 1,
                photoProofs = 5,
                shares = 1,
                duoStarted = true,
                challengesJoined = 1,
                challengeWins = 3,
                twoHabitDays = 1,
            ),
        )
        listOf(
            "first_goal", "goal_done", "first_focus", "hour_in_garden", "focus_flow",
            "first_friend", "social_five", "first_profile_visit", "first_visitor",
            "first_proof", "photo_five", "first_share", "first_duo", "join_arena",
            "challenge_three", "first_challenge_w", "two_today", "polyglot",
        ).forEach { id ->
            assertTrue(id, evaluated.first { it.id == id }.isUnlocked)
        }
    }

    @Test
    fun alreadyUnlockedTrophiesDoNotRegress() {
        val existing = AchievementEngine.allAchievements.map {
            if (it.id == "first_light") it.copy(isUnlocked = true, unlockedAt = 99L, progressCurrent = 1)
            else it
        }
        val evaluated = TrophyEvaluator.evaluate(
            habits = emptyList(),
            totalCompletions = 0,
            currentLevel = 1,
            languageChanged = false,
            existing = existing,
        )
        val first = evaluated.first { it.id == "first_light" }
        assertTrue(first.isUnlocked)
        assertEquals(99L, first.unlockedAt)
    }
}
