package com.saintnico.verdlyhabits.engine

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The definitive trophy catalog — trophies across 5 tiers.
 *
 * Display rules (see [TrophyEvaluator.hallTrophies]):
 *   • Full catalog is always visible — locked trophies show name, tier, and progress.
 *   • Rank names (Bronze → Mythic) signal rarity; the journey never "ends".
 *
 * All evaluation logic lives in [TrophyEvaluator]. This file is purely the
 * definitions — IDs, tiers, metadata, and progression targets.
 */

// ─── Data Models ────────────────────────────────────────────────────────────

enum class TrophyTierLevel(
    val rankName: String,
    val chapterName: String,
    val ordinalIndex: Int
) {
    FOUNDATION("Bronze", "First Steps", 0),
    MOMENTUM("Silver", "Momentum", 1),
    MASTERY("Gold", "Rare Territory", 2),
    LEGEND("Obsidian", "The Few", 3),
    MYTHIC("Mythic", "Apex", 4);

    /** Section header in the Hall of Trophies, e.g. "Platinum · The Few". */
    val displayName: String get() = "$rankName · $chapterName"
}

data class TrophyDef(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tier: TrophyTierLevel,
    val xpReward: Int,
    val progressTarget: Int = 1,
    /** Mythic trophies are never teased — they appear only at unlock. */
    val isSecret: Boolean = false
)

// ─── The Catalog ────────────────────────────────────────────────────────────

object TrophyCatalog {

    val all: List<TrophyDef> by lazy { tier1 + tier2 + tier3 + tier4 + tier5 }

    // ── TIER 1 — FOUNDATION (1–10) "The First Steps" ────────────────────
    // Earned in weeks 1–4. Hook the user early.

    private val tier1 = listOf(
        TrophyDef("first_light",      "First Light",      "Complete your first habit ever",                      Icons.Filled.LightMode,           TrophyTierLevel.FOUNDATION, 50,  progressTarget = 1),
        TrophyDef("the_spark",        "The Spark",        "Complete all habits in a single day",                 Icons.Filled.Bolt,               TrophyTierLevel.FOUNDATION, 100,  progressTarget = 1),
        TrophyDef("threes_company",   "Three's Company",  "Reach a 3-day streak on any habit",                  Icons.Filled.Looks3,              TrophyTierLevel.FOUNDATION, 50,  progressTarget = 3),
        TrophyDef("week_one",         "Week One",         "Complete 7 consecutive days",                         Icons.Filled.DateRange,          TrophyTierLevel.FOUNDATION, 100,  progressTarget = 7),
        TrophyDef("early_bird",       "Early Bird",       "Complete a habit before 7 AM, 5 days running",       Icons.Filled.WbTwilight,          TrophyTierLevel.FOUNDATION, 75,  progressTarget = 5),
        TrophyDef("full_house",       "Full House",       "Complete every habit in a single day, 3 times",      Icons.Filled.GridView,            TrophyTierLevel.FOUNDATION, 75,  progressTarget = 3),
        TrophyDef("committed",        "Committed",        "Add your 3rd habit to the tracker",                  Icons.Filled.PostAdd,             TrophyTierLevel.FOUNDATION, 50,  progressTarget = 3),
        TrophyDef("the_ritual",       "The Ritual",       "Same habit completed 7 days straight",               Icons.Filled.Autorenew,          TrophyTierLevel.FOUNDATION, 100,  progressTarget = 7),
        TrophyDef("no_excuses",       "No Excuses",       "Complete a habit after ignoring a reminder",          Icons.Filled.NotificationsOff,    TrophyTierLevel.FOUNDATION, 50,  progressTarget = 1),
        TrophyDef("quiet_achiever",   "Quiet Achiever",   "Complete 5 habits in one day",                       Icons.Filled.Visibility,          TrophyTierLevel.FOUNDATION, 75,  progressTarget = 5),
        TrophyDef("first_stats",      "The Numbers",      "Open the Stats screen for the first time",            Icons.Filled.Insights,            TrophyTierLevel.FOUNDATION, 50,  progressTarget = 1),
        TrophyDef("first_goal",       "Aim True",         "Create your first goal",                              Icons.Filled.Flag,                TrophyTierLevel.FOUNDATION, 50,  progressTarget = 1),
        TrophyDef("first_focus",      "Deep Work",        "Finish your first Focus session",                     Icons.Filled.Timer,               TrophyTierLevel.FOUNDATION, 50,  progressTarget = 1),
        TrophyDef("first_friend",     "Not Alone",        "Connect with a friend",                               Icons.Filled.PersonAdd,           TrophyTierLevel.FOUNDATION, 75,  progressTarget = 1),
        TrophyDef("first_proof",      "Receipts",         "Verify a habit with a photo",                         Icons.Filled.PhotoCamera,         TrophyTierLevel.FOUNDATION, 50,  progressTarget = 1),
        TrophyDef("two_today",        "Double Up",        "Complete two different habits in one day",            Icons.Filled.DoneAll,             TrophyTierLevel.FOUNDATION, 50,  progressTarget = 1),
        TrophyDef("first_profile_visit", "Curious",       "Visit another member's profile",                      Icons.Filled.TravelExplore,       TrophyTierLevel.FOUNDATION, 50,  progressTarget = 1),
        TrophyDef("first_visitor",    "Seen",             "Someone checks your profile",                         Icons.Filled.WavingHand,          TrophyTierLevel.FOUNDATION, 75,  progressTarget = 1),
    )

    // ── TIER 2 — MOMENTUM (11–20) "The Grind Begins" ───────────────────
    // Earned between weeks 2–8. Requires consistency, not just activity.

    private val tier2 = listOf(
        TrophyDef("iron_will",        "Iron Will",        "14-day streak on any single habit",                  Icons.Filled.Shield,             TrophyTierLevel.MOMENTUM, 150,  progressTarget = 14),
        TrophyDef("the_compound",     "The Compound",     "3 habits each with 7+ day streaks simultaneously",   Icons.Filled.Diamond,            TrophyTierLevel.MOMENTUM, 150,  progressTarget = 3),
        TrophyDef("midnight_oil",     "Midnight Oil",     "Complete a habit after 11 PM, 7 times",              Icons.Filled.NightsStay,         TrophyTierLevel.MOMENTUM, 100,  progressTarget = 7),
        TrophyDef("perfectionist",    "Perfectionist",    "100% completion for an entire week",                 Icons.Filled.Brightness7,        TrophyTierLevel.MOMENTUM, 200,  progressTarget = 7),
        TrophyDef("the_architect",    "The Architect",    "Have 5 active habits running simultaneously",        Icons.Filled.Architecture,       TrophyTierLevel.MOMENTUM, 100,  progressTarget = 5),
        TrophyDef("chain_reaction",   "Chain Reaction",   "Two habits both hit 14-day streaks in the same week",Icons.Filled.Link,               TrophyTierLevel.MOMENTUM, 200,  progressTarget = 2),
        TrophyDef("rise_and_grind",   "Rise & Grind",     "Morning habit completed before 6 AM, 10 times",     Icons.Filled.Terrain,            TrophyTierLevel.MOMENTUM, 150,  progressTarget = 10),
        TrophyDef("recovery",         "Recovery",         "Resume a broken streak within 24 hours, 3 times",   Icons.Filled.Replay,             TrophyTierLevel.MOMENTUM, 100,  progressTarget = 3),
        TrophyDef("the_long_game",    "The Long Game",    "Be active in the app for 30 consecutive days",       Icons.Filled.CalendarMonth,      TrophyTierLevel.MOMENTUM, 200,  progressTarget = 30),
        TrophyDef("data_driven",      "Data Driven",      "Visit the Stats screen 20 times",                   Icons.Filled.BarChart,           TrophyTierLevel.MOMENTUM, 100,  progressTarget = 20),
        TrophyDef("stats_habit",      "Stats Habit",      "Check Stats 7 days in a row",                       Icons.Filled.QueryStats,         TrophyTierLevel.MOMENTUM, 150,  progressTarget = 7),
        TrophyDef("first_duo",        "Better Together",  "Start a Duo streak",                                 Icons.Filled.Favorite,           TrophyTierLevel.MOMENTUM, 150,  progressTarget = 1),
        TrophyDef("join_arena",       "Enter the Arena",  "Join a group challenge",                             Icons.Filled.Groups,             TrophyTierLevel.MOMENTUM, 100,  progressTarget = 1),
        TrophyDef("hour_in_garden",   "Hour in the Garden","Log 60 minutes of Focus",                           Icons.Filled.HourglassBottom,    TrophyTierLevel.MOMENTUM, 150,  progressTarget = 60),
        TrophyDef("photo_five",       "Proof of Life",    "Verify 5 habits with photos",                        Icons.Filled.Collections,        TrophyTierLevel.MOMENTUM, 150,  progressTarget = 5),
        TrophyDef("first_share",      "Pass It On",       "Share a card from Verdly",                           Icons.Filled.Share,              TrophyTierLevel.MOMENTUM, 75,   progressTarget = 1),
        TrophyDef("first_oath",       "First Oath",       "Lock coins on a challenge",                          Icons.Filled.Spa,                TrophyTierLevel.MOMENTUM, 100,  progressTarget = 1),
        TrophyDef("oath_proven",      "Proven",           "Finish a challenge with your oath intact",            Icons.Filled.CheckCircle,        TrophyTierLevel.MOMENTUM, 200,  progressTarget = 1),
    )

    // ── TIER 3 — MASTERY (21–32) "Rare Territory" ───────────────────────
    // Earned between months 2–4. Many users never reach this tier.

    private val tier3 = listOf(
        TrophyDef("centurion",        "The Centurion",    "100-day streak on a single habit",                   Icons.Filled.MilitaryTech,       TrophyTierLevel.MASTERY, 500,  progressTarget = 100),
        TrophyDef("ghost_protocol",   "Ghost Protocol",   "30 days. Zero missed habits, not even one",         Icons.Filled.VisibilityOff,      TrophyTierLevel.MASTERY, 400,  progressTarget = 30),
        TrophyDef("the_philosopher",  "The Philosopher",  "Meditation or mindfulness habit, 60-day streak",     Icons.Filled.SelfImprovement,    TrophyTierLevel.MASTERY, 400,  progressTarget = 60),
        TrophyDef("unseen_hours",     "Unseen Hours",     "50 completions before 6 AM total",                   Icons.Filled.DarkMode,           TrophyTierLevel.MASTERY, 300,  progressTarget = 50),
        TrophyDef("dual_threat",      "Dual Threat",      "Two habits both with 50+ day streaks simultaneously",Icons.Filled.Hexagon,            TrophyTierLevel.MASTERY, 400,  progressTarget = 2),
        TrophyDef("the_collector",    "The Collector",    "Have 8 different habits in the tracker at once",     Icons.Filled.CollectionsBookmark, TrophyTierLevel.MASTERY, 250, progressTarget = 8),
        TrophyDef("ironclad",         "Ironclad",         "Zero broken streaks across all habits for 60 days",  Icons.Filled.Security,           TrophyTierLevel.MASTERY, 500,  progressTarget = 60),
        TrophyDef("the_observer",     "The Observer",     "Check stats every day for 21 days straight",         Icons.Filled.RemoveRedEye,       TrophyTierLevel.MASTERY, 300,  progressTarget = 21),
        TrophyDef("velocity",         "Velocity",         "Increase completion rate 3 months in a row",         Icons.Filled.Speed,              TrophyTierLevel.MASTERY, 350,  progressTarget = 3),
        TrophyDef("legacy",           "Legacy",           "First anniversary in the app. 365 days since install", Icons.Filled.WorkspacePremium, TrophyTierLevel.MASTERY, 1000, progressTarget = 365),
        TrophyDef("five_hundred",     "Five Hundred",     "500 total habit completions",                        Icons.Filled.Star,               TrophyTierLevel.MASTERY, 500,  progressTarget = 500),
        TrophyDef("polyglot",         "Polyglot",         "Change the app language",                            Icons.Filled.Translate,          TrophyTierLevel.MASTERY, 100,  progressTarget = 1),
        TrophyDef("goal_done",        "Closed the Loop",  "Complete a goal",                                    Icons.Filled.EmojiFlags,         TrophyTierLevel.MASTERY, 400,  progressTarget = 1),
        TrophyDef("social_five",      "Inner Circle",     "Have 5 friends connected",                           Icons.Filled.Group,              TrophyTierLevel.MASTERY, 300,  progressTarget = 5),
        TrophyDef("focus_flow",       "Flow State",       "Finish 10 Focus sessions",                           Icons.Filled.FitnessCenter,      TrophyTierLevel.MASTERY, 300,  progressTarget = 10),
        TrophyDef("challenge_three",  "Hat Trick",        "Win 3 group challenges",                             Icons.Filled.SportsScore,        TrophyTierLevel.MASTERY, 500,  progressTarget = 3),
        TrophyDef("unbroken_oath",    "Unbroken Oath",    "Convert 3 oaths without a forfeit on record",        Icons.Filled.Shield,             TrophyTierLevel.MASTERY, 400,  progressTarget = 3),
        TrophyDef("benefactor",       "Benefactor",       "An oath of yours boosted the group",                 Icons.Filled.VolunteerActivism,  TrophyTierLevel.MASTERY, 200,  progressTarget = 1),
        TrophyDef("oath_grove",       "Grove Roots",      "Plant 500 coins into trophy progress",               Icons.Filled.Forest,             TrophyTierLevel.MASTERY, 500,  progressTarget = 500),
        TrophyDef("oath_five",        "Proven Five",      "Convert 5 oaths",                                    Icons.Filled.CheckCircle,        TrophyTierLevel.MASTERY, 400,  progressTarget = 5),
        TrophyDef("grove_patron",     "Grove Patron",     "Gift 50 coins to keep others going",                 Icons.Filled.Favorite,           TrophyTierLevel.MASTERY, 250,  progressTarget = 50),
        TrophyDef("grove_dressed",    "Dressed",          "Unlock a Grove cosmetic",                            Icons.Filled.AutoAwesome,        TrophyTierLevel.MASTERY, 150,  progressTarget = 1),
    )

    // ── TIER 4 — LEGEND (33–44) "The Few" ───────────────────────────────
    // Earned by users with 6–12 months of commitment. NEVER previewed.

    private val tier4 = listOf(
        TrophyDef("the_constant",     "The Constant",     "365-day streak on any single habit",                 Icons.Filled.AllInclusive,      TrophyTierLevel.LEGEND, 2000,  progressTarget = 365),
        TrophyDef("pantheon",         "Pantheon",         "All Tier 1 and Tier 2 trophies earned",              Icons.Filled.AccountBalance,    TrophyTierLevel.LEGEND, 1500,  progressTarget = 34),
        TrophyDef("architects_master","Architect's Master","10 habits maintained simultaneously for 30 days",   Icons.Filled.ViewInAr,          TrophyTierLevel.LEGEND, 1500,  progressTarget = 10),
        TrophyDef("eclipse",          "Eclipse",          "Complete habits across all 12 calendar months",       Icons.Filled.WbSunny,          TrophyTierLevel.LEGEND, 1000,  progressTarget = 12),
        TrophyDef("obsidian_chain",   "Obsidian Chain",   "1000 total habit completions",                       Icons.Filled.LinkOff,           TrophyTierLevel.LEGEND, 1500,  progressTarget = 1000),
        TrophyDef("first_challenge_w","Challenge Victor", "Win your first group challenge",                     Icons.Filled.EmojiEvents,       TrophyTierLevel.LEGEND, 500,   progressTarget = 1),
        TrophyDef("night_shift",      "Night Shift",      "100 habits completed after 10 PM total",             Icons.Filled.Bedtime,           TrophyTierLevel.LEGEND, 750,   progressTarget = 100),
        TrophyDef("deep_roots",       "Deep Roots",       "Reach Level 30",                                    Icons.Filled.Park,              TrophyTierLevel.LEGEND, 1000,  progressTarget = 30),
        TrophyDef("garden_master",    "Garden Master",    "7 simultaneous habits each with 30+ day streaks",    Icons.Filled.Spa,               TrophyTierLevel.LEGEND, 1500,  progressTarget = 7),
        TrophyDef("royalty",          "Royalty",          "Reach Level 20",                                     Icons.Filled.EmojiEvents,       TrophyTierLevel.LEGEND, 500,   progressTarget = 20),
        TrophyDef("ancient_one",      "Ancient One",      "Reach Level 60",                                    Icons.Filled.AutoAwesome,       TrophyTierLevel.LEGEND, 2000,  progressTarget = 60),
        TrophyDef("full_spectrum",    "Full Spectrum",    "Have habits in 5 different categories",               Icons.Filled.Category,          TrophyTierLevel.LEGEND, 500,   progressTarget = 5),
        TrophyDef("vowkeeper",        "Vowkeeper",        "Convert 10 oaths",                                    Icons.Filled.WorkspacePremium,  TrophyTierLevel.LEGEND, 800,   progressTarget = 10),
        TrophyDef("deep_planted",     "Deep Planted",     "Plant 1,500 coins into trophy progress",              Icons.Filled.Park,              TrophyTierLevel.LEGEND, 1000,  progressTarget = 1500),
    )

    // ── TIER 5 — MYTHIC (45–50) "Unknown" ───────────────────────────────
    // Completely hidden. Surface only at unlock. Full-screen ceremony.

    private val tier5 = listOf(
        TrophyDef("the_long_shadow",  "The Long Shadow",  "500-day streak on a single habit",                   Icons.Filled.Layers,            TrophyTierLevel.MYTHIC, 5000,  progressTarget = 500, isSecret = true),
        TrophyDef("infinite",         "Infinite",         "2000 total completions",                             Icons.Filled.AllInclusive,      TrophyTierLevel.MYTHIC, 5000,  progressTarget = 2000, isSecret = true),
        TrophyDef("unseen_war",       "The Unseen War",   "100 habits completed before 5 AM",                   Icons.Filled.Visibility,        TrophyTierLevel.MYTHIC, 3000,  progressTarget = 100, isSecret = true),
        TrophyDef("still_standing",   "Still Standing",   "Recover from a broken streak and surpass it, 10x",  Icons.Filled.Healing,           TrophyTierLevel.MYTHIC, 4000,  progressTarget = 10, isSecret = true),
        TrophyDef("the_origin",       "The Origin",       "Complete your first-ever habit exactly 1 year later",Icons.Filled.RestartAlt,        TrophyTierLevel.MYTHIC, 5000,  progressTarget = 1, isSecret = true),
        TrophyDef("launch_day",       "Launch Day",       "Complete all habits on day 1",                       Icons.Filled.RocketLaunch,      TrophyTierLevel.MYTHIC, 100,   progressTarget = 1, isSecret = true),
    )

    /** Find a definition by ID. Never returns null for a valid ID. */
    fun find(id: String): TrophyDef? = all.find { it.id == id }

    /** All trophy IDs in a given tier. */
    fun idsForTier(tier: TrophyTierLevel): List<String> = all.filter { it.tier == tier }.map { it.id }
}
