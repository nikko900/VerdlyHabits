package com.saintnico.verdlyhabits.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class CreateViewStateTest {

    private fun state(
        proof: Boolean = false,
        milestone: CreateMilestone? = null,
        planted: Boolean = false,
        shared: String? = null,
        hasHabits: Boolean = true,
        friends: Int = 3,
        completed: List<String> = if (planted) listOf("Run") else emptyList(),
    ) = CreateViewState(
        plantedToday = planted,
        completedToday = completed,
        alreadySharedTitle = shared,
        proofWindowOpen = proof,
        proofHabitTitle = if (proof) "Run" else null,
        milestone = milestone,
        friendCount = friends,
        nudgeFriends = emptyList(),
        hasDuo = false,
        duoSwapAvailable = false,
        isSignedIn = true,
        canAddHabit = true,
        hasHabits = hasHabits,
        photoUrl = null,
        username = "maya",
        titleGlowId = null,
        profileFrameId = null,
    )

    @Test
    fun heroPrefersProofThenMilestoneThenPlant() {
        val mile = CreateMilestone(
            kind = CreateMilestone.Kind.STREAK,
            shareKey = "streak:1:7:2026-09-11",
            headline = "Share your streak",
            why = "A streak worth showing",
            habitTitle = "Run",
            streakDays = 7,
        )
        assertEquals(CreateHeroKind.PostProof, state(proof = true, milestone = mile, planted = true).hero)
        assertEquals(CreateHeroKind.ShareMilestone, state(milestone = mile, planted = true).hero)
        assertEquals(CreateHeroKind.SharePlant, state(planted = true).hero)
        assertEquals(CreateHeroKind.PlantSomething, state(hasHabits = true).hero)
        assertEquals(CreateHeroKind.Invite, state(hasHabits = false, friends = 1).hero)
        assertEquals(CreateHeroKind.AddHabit, state(hasHabits = false, friends = 4, planted = false).hero)
    }

    @Test
    fun milestoneSkipsAlreadySharedStreak() {
        val today = LocalDate.of(2026, 9, 11)
        val habits = listOf(CreateHabitSnap("h1", "Run", 7, completedToday = true))
        val key = "streak:h1:7:2026-09-11"
        assertNull(
            CreateMilestones.resolve(
                achievements = emptyList(),
                habits = habits,
                today = today,
                alreadySharedKey = key,
                zone = ZoneOffset.UTC,
            ),
        )
        val fresh = CreateMilestones.resolve(
            achievements = emptyList(),
            habits = habits,
            today = today,
            alreadySharedKey = null,
            zone = ZoneOffset.UTC,
        )
        assertEquals(CreateMilestone.Kind.STREAK, fresh?.kind)
        assertEquals(7, fresh?.streakDays)
    }
}
