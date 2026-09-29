package com.saintnico.verdlyhabits.engine

/**
 * Profile completion scoring for onboarding nudges and in-app banners.
 *
 * Only counts fields required by profile setup (name, username, story). Photo is optional.
 * Default motto/bio/flair from setup count as complete — they are valid choices, not gaps.
 */
object ProfileCompletionEngine {

    data class Step(val id: String, val label: String, val done: Boolean)

    fun steps(
        displayName: String,
        username: String,
        photoUrl: String?,
        motto: String,
        bio: String,
        favoritePlant: String,
    ): List<Step> {
        val hasName = displayName.isNotBlank() && !displayName.equals("Rival", ignoreCase = true)
        val hasUsername = username.isNotBlank() && !username.equals("UnknownRival", ignoreCase = true)
        val hasHeadline = motto.isNotBlank()
        val hasBio = bio.isNotBlank()
        val hasFlair = favoritePlant.isNotBlank()
        return listOf(
            Step("name", "Display name", hasName),
            Step("username", "Username", hasUsername),
            Step("headline", "Headline", hasHeadline),
            Step("bio", "About you", hasBio),
            Step("flair", "Flair tag", hasFlair),
        )
    }

    /** Optional polish — shown in the banner but does not block hiding once required fields are done. */
    fun hasOptionalPhoto(photoUrl: String?): Boolean = !photoUrl.isNullOrBlank()

    fun incompleteSteps(
        displayName: String,
        username: String,
        photoUrl: String?,
        motto: String,
        bio: String,
        favoritePlant: String,
    ): List<Step> = steps(displayName, username, photoUrl, motto, bio, favoritePlant).filter { !it.done }

    /** Labels for the home banner — required gaps first, then optional photo if missing. */
    fun remainingLabels(
        displayName: String,
        username: String,
        photoUrl: String?,
        motto: String,
        bio: String,
        favoritePlant: String,
    ): List<String> {
        val required = incompleteSteps(displayName, username, photoUrl, motto, bio, favoritePlant)
            .map { it.label }
        if (required.isNotEmpty()) return required
        return if (!hasOptionalPhoto(photoUrl)) listOf("Profile photo") else emptyList()
    }

    fun shouldShowNudge(
        displayName: String,
        username: String,
        photoUrl: String?,
        motto: String,
        bio: String,
        favoritePlant: String,
    ): Boolean = incompleteSteps(displayName, username, photoUrl, motto, bio, favoritePlant).isNotEmpty()

    fun completionFraction(
        displayName: String,
        username: String,
        photoUrl: String?,
        motto: String,
        bio: String,
        favoritePlant: String,
    ): Float {
        val s = steps(displayName, username, photoUrl, motto, bio, favoritePlant)
        return s.count { it.done }.toFloat() / s.size.toFloat()
    }

    fun nextIncompleteStep(
        displayName: String,
        username: String,
        photoUrl: String?,
        motto: String,
        bio: String,
        favoritePlant: String,
    ): Step? = steps(displayName, username, photoUrl, motto, bio, favoritePlant).firstOrNull { !it.done }

    fun isComplete(
        displayName: String,
        username: String,
        photoUrl: String?,
        motto: String,
        bio: String,
        favoritePlant: String,
    ): Boolean = !shouldShowNudge(displayName, username, photoUrl, motto, bio, favoritePlant)
}
