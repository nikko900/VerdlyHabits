package com.saintnico.verdlyhabits.engine.share

object TrophyShareMapper {

    fun categoryFor(trophyId: String): ShareMotifCategory = when (trophyId) {
        "first_light", "threes_company", "week_one", "the_ritual", "iron_will", "the_compound",
        "chain_reaction", "recovery", "centurion", "the_philosopher", "dual_threat", "the_constant",
        "garden_master", "still_standing", "the_origin", "the_long_shadow", "legacy" ->
            ShareMotifCategory.STREAK

        "early_bird", "midnight_oil", "rise_and_grind", "unseen_hours", "night_shift", "unseen_war" ->
            ShareMotifCategory.TIME_OF_DAY

        "the_spark", "full_house", "no_excuses", "quiet_achiever", "perfectionist", "the_long_game",
        "ghost_protocol", "ironclad", "the_observer", "velocity", "eclipse", "launch_day" ->
            ShareMotifCategory.CONSISTENCY

        "first_challenge_w", "first_friend", "first_duo", "join_arena", "social_five",
        "challenge_three", "first_profile_visit", "first_visitor", "first_share" ->
            ShareMotifCategory.SOCIAL

        "committed", "the_architect", "data_driven", "the_collector", "five_hundred", "polyglot",
        "pantheon", "architects_master", "obsidian_chain", "deep_roots", "royalty", "ancient_one",
        "full_spectrum", "infinite", "first_stats", "stats_habit", "first_goal", "goal_done",
        "first_focus", "hour_in_garden", "focus_flow", "first_proof", "photo_five", "two_today" ->
            ShareMotifCategory.LEVEL

        else -> ShareMotifCategory.CONSISTENCY
    }

    /** Mythic trophies use the secret motif; all others use their functional category. */
    fun motifFor(trophyId: String, tierIsMythic: Boolean): ShareMotifCategory =
        if (tierIsMythic) ShareMotifCategory.SECRET else categoryFor(trophyId)
}
