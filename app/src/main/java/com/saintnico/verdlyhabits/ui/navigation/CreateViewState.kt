package com.saintnico.verdlyhabits.ui.navigation

import com.saintnico.verdlyhabits.engine.Achievement
import java.time.LocalDate
import java.time.ZoneId

enum class CreateAction {
    AddHabit,
    NewGoal,
    CreateChallenge,
    StartFocus,
    SharePlant,
    PostProof,
    ShareCard,
    Nudge,
    Invite,
    Duo,
    ShareMilestone,
    PlantSomething,
}

enum class CreateHeroKind {
    PostProof,
    ShareMilestone,
    SharePlant,
    PlantSomething,
    Invite,
    AddHabit,
}

data class CreateMilestone(
    val kind: Kind,
    val shareKey: String,
    val headline: String,
    val why: String,
    val achievement: Achievement? = null,
    val habitTitle: String? = null,
    val streakDays: Int = 0,
) {
    enum class Kind { TROPHY, STREAK }
}

data class CreateNudgeFriend(
    val uid: String,
    val username: String,
    val photoUrl: String?,
)

data class CreateHabitSnap(
    val id: String,
    val title: String,
    val streak: Int,
    val completedToday: Boolean,
)

data class CreateViewState(
    val plantedToday: Boolean,
    val completedToday: List<String>,
    val alreadySharedTitle: String?,
    val proofWindowOpen: Boolean,
    val proofHabitTitle: String?,
    val milestone: CreateMilestone?,
    val friendCount: Int,
    val nudgeFriends: List<CreateNudgeFriend>,
    val hasDuo: Boolean,
    val duoSwapAvailable: Boolean,
    val isSignedIn: Boolean,
    val canAddHabit: Boolean,
    val hasHabits: Boolean,
    val photoUrl: String?,
    val username: String,
    val titleGlowId: String?,
    val profileFrameId: String?,
) {
    val hero: CreateHeroKind = resolveHero()
    val canSharePlant: Boolean = completedToday.isNotEmpty()
    val canLongPressComposer: Boolean = completedToday.isNotEmpty()
    val alreadyShared: Boolean = !alreadySharedTitle.isNullOrBlank()
    val firstNudgeName: String? = nudgeFriends.firstOrNull()?.username?.takeIf { it.isNotBlank() }

    fun resolveHero(): CreateHeroKind = when {
        proofWindowOpen -> CreateHeroKind.PostProof
        milestone != null -> CreateHeroKind.ShareMilestone
        plantedToday && alreadySharedTitle.isNullOrBlank() -> CreateHeroKind.SharePlant
        !plantedToday && hasHabits -> CreateHeroKind.PlantSomething
        friendCount <= 1 -> CreateHeroKind.Invite
        else -> CreateHeroKind.AddHabit
    }

    fun heroAction(): CreateAction = when (hero) {
        CreateHeroKind.PostProof -> CreateAction.PostProof
        CreateHeroKind.ShareMilestone -> CreateAction.ShareMilestone
        CreateHeroKind.SharePlant -> CreateAction.SharePlant
        CreateHeroKind.PlantSomething -> CreateAction.PlantSomething
        CreateHeroKind.Invite -> CreateAction.Invite
        CreateHeroKind.AddHabit -> CreateAction.AddHabit
    }
}

object CreateMilestones {
    val STREAK_MARKS = setOf(7, 14, 21, 30, 50, 75, 100, 150, 200, 365)

    fun resolve(
        achievements: List<Achievement>,
        habits: List<CreateHabitSnap>,
        today: LocalDate,
        alreadySharedKey: String?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): CreateMilestone? {
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val trophy = achievements
            .filter { it.isUnlocked && (it.unlockedAt ?: 0L) >= dayStart }
            .maxByOrNull { it.unlockedAt ?: 0L }
        if (trophy != null) {
            val key = "trophy:${trophy.id}:${today}"
            if (key != alreadySharedKey) {
                return CreateMilestone(
                    kind = CreateMilestone.Kind.TROPHY,
                    shareKey = key,
                    headline = "Share this trophy",
                    why = "You unlocked this today",
                    achievement = trophy,
                )
            }
        }
        val streakHit = habits
            .filter { it.completedToday && it.streak in STREAK_MARKS }
            .maxByOrNull { it.streak }
        if (streakHit != null) {
            val key = "streak:${streakHit.id}:${streakHit.streak}:${today}"
            if (key != alreadySharedKey) {
                return CreateMilestone(
                    kind = CreateMilestone.Kind.STREAK,
                    shareKey = key,
                    headline = "Share your streak",
                    why = "A streak worth showing",
                    habitTitle = streakHit.title,
                    streakDays = streakHit.streak,
                )
            }
        }
        return null
    }
}
