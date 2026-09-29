package com.saintnico.verdlyhabits.engine

import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.data.local.goals.MilestoneEntity
import java.time.LocalDate

/**
 * Type-specific, high-variance copy for milestones, rituals, nudges, and live narratives.
 * Selection is stable per goal + moment so it feels personal, not random every frame.
 */
object GoalCelebrationCopy {

    data class MomentCopy(
        val badge: String,
        val headline: String,
        val subhead: String,
        val body: String,
        val cheer: String,
        val emoji: String,
    )

    data class TypePalette(
        val primary: Long,
        val secondary: Long,
        val tertiary: Long,
    )

    fun palette(type: GoalType): TypePalette = when (type) {
        GoalType.BUILD -> TypePalette(0xFF43A047, 0xFF81C784, 0xFFA5D6A7)
        GoalType.REACH -> TypePalette(0xFF1E88E5, 0xFF64B5F6, 0xFF90CAF9)
        GoalType.QUIT -> TypePalette(0xFF7E57C2, 0xFF26A69A, 0xFF80CBC4)
        GoalType.MAINTAIN -> TypePalette(0xFFFF7043, 0xFFFFB74D, 0xFFFFCC80)
    }

    fun milestoneMoment(
        goal: GoalEntity,
        milestone: MilestoneEntity,
        metrics: GoalProgressEngine.GoalMetrics?,
    ): MomentCopy {
        val seed = "${goal.id}:${milestone.targetPercent}:${milestone.title}"
        val titleRef = goal.title.ifBlank { "this" }
        val why = goal.whyStatement.takeIf { it.isNotBlank() }

        return when (goal.goalType) {
            GoalType.REACH -> reachMoment(seed, milestone, titleRef, why, metrics)
            GoalType.BUILD -> buildMoment(seed, milestone, titleRef, why)
            GoalType.QUIT -> quitMoment(seed, milestone, titleRef, why, metrics)
            GoalType.MAINTAIN -> maintainMoment(seed, milestone, titleRef, why)
        }
    }

    fun isRitualMilestone(goal: GoalEntity, milestone: MilestoneEntity): Boolean =
        GoalMilestoneFactory.isRitualMilestone(goal, milestone)

    fun maintainNudge(goal: GoalEntity): MomentCopy {
        val seed = "${goal.id}:nudge:${LocalDate.now()}"
        val lines = maintainNudgeBodies
        val i = stableIndex(seed, lines.size)
        val titleRef = goal.title.ifBlank { "your standard" }
        return MomentCopy(
            badge = "Floor check",
            headline = maintainNudgeHeadlines[stableIndex(seed + "h", maintainNudgeHeadlines.size)],
            subhead = titleRef,
            body = lines[i].format(titleRef),
            cheer = maintainNudgeCheers[stableIndex(seed + "c", maintainNudgeCheers.size)],
            emoji = "🔥",
        )
    }

    fun weeklyCheckInIntro(goal: GoalEntity): String {
        val pool = when (goal.goalType) {
            GoalType.BUILD -> buildCheckInIntros
            GoalType.REACH -> reachCheckInIntros
            GoalType.QUIT -> quitCheckInIntros
            GoalType.MAINTAIN -> maintainCheckInIntros
        }
        val raw = pool[stableIndex("${goal.id}:checkin:${LocalDate.now().dayOfYear}", pool.size)]
        return formatOptional(raw, goal.title.ifBlank { "this goal" })
    }

    fun liveNarrative(goal: GoalEntity, metrics: GoalProgressEngine.GoalMetrics): String {
        val pool = when (goal.goalType) {
            GoalType.BUILD -> buildNarratives(metrics.buildStage, goal.title)
            GoalType.REACH -> reachNarratives(metrics, goal)
            GoalType.QUIT -> quitNarratives(metrics, goal)
            GoalType.MAINTAIN -> maintainNarratives(metrics, goal)
        }
        val key = when (goal.goalType) {
            GoalType.BUILD -> "${goal.id}:b:${metrics.buildStage}"
            GoalType.REACH -> "${goal.id}:r:${(metrics.progressFraction * 100).toInt()}"
            GoalType.QUIT -> "${goal.id}:q:${metrics.cleanStreakDays}"
            GoalType.MAINTAIN -> "${goal.id}:m:${metrics.consecutiveMissDays}:${metrics.weeksHeld}"
        }
        return pool[stableIndex(key, pool.size)]
    }

    fun milestoneCheer(goal: GoalEntity, milestone: MilestoneEntity): String {
        val pool = when (goal.goalType) {
            GoalType.BUILD -> buildCheers
            GoalType.REACH -> reachCheers
            GoalType.QUIT -> quitCheers
            GoalType.MAINTAIN -> maintainCheers
        }
        return pool[stableIndex("${goal.id}:${milestone.targetPercent}", pool.size)]
    }

    // ── REACH ────────────────────────────────────────────────────────────────

    private fun reachMoment(
        seed: String,
        milestone: MilestoneEntity,
        title: String,
        why: String?,
        metrics: GoalProgressEngine.GoalMetrics?,
    ): MomentCopy {
        val isMid = milestone.targetPercent == 50f
        val headlines = if (isMid) reachMidpointHeadlines else reachMilestoneHeadlines
        val bodies = if (isMid) reachMidpointBodies else reachMilestoneBodies
        val h = stableIndex(seed, headlines.size)
        val pace = metrics?.paceDeltaLabel?.let { " · $it" }.orEmpty()
        return MomentCopy(
            badge = if (isMid) "MIDPOINT RITUAL" else "TRAIL MARKER",
            headline = formatOptional(headlines[h], title),
            subhead = milestone.title + pace,
            body = bodies[stableIndex(seed + "b", bodies.size)]
                .format(title, why ?: "the number you chose"),
            cheer = reachCheers[stableIndex(seed + "c", reachCheers.size)],
            emoji = if (isMid) "🏔️" else "🎯",
        )
    }

    private val reachMidpointHeadlines = listOf(
        "Halfway there. And you're still moving.",
        "50% down. The summit got closer today.",
        "Midpoint unlocked. Most people quit here. You didn't.",
        "You're in the rare air now. Halfway to real.",
        "The hard middle? You're walking through it.",
        "Half the trail behind you. Half the story still unwritten.",
        "Midpoint. Breathe. You're doing the real work.",
        "50%. That's not luck, that's reps.",
        "Halfway isn't halfway effort. You showed up.",
        "You're past the dip. Keep the pace that got you here.",
        "Mid-trail magic. You're still climbing.",
        "Halfway mark: proof the goal isn't fantasy.",
        "50%. Your future self is watching this moment.",
        "The middle is where identity forms. You're forming.",
        "Halfway there. Full commitment.",
        "Midpoint reached. The finish line can see you now.",
        "50%: not a pause button. A launch pad.",
        "You're halfway to proving ${"{"}0} was worth it.",
        "Mid-trail. Full heart. Keep going.",
        "Half the distance. All of your discipline.",
        "50%. Celebrate, then climb.",
        "Midpoint milestone: you're the kind of person who continues.",
    )

    private val reachMidpointBodies = listOf(
        "%s isn't a wish anymore. You're halfway to making it real. Remember why: %s.",
        "The middle is where goals go to die. Yours is very much alive. %s still matters.",
        "Halfway to %s. This is the chapter most people skip. You're writing it.",
        "You're not chasing %s anymore. You're collecting evidence you can.",
        "Midpoint on %s. Adjust the pace if you need to, but don't abandon the trail.",
        "%s at 50%%: the number is moving because you are.",
        "Halfway there on %s. The urge to coast? Normal. The choice to continue? Yours.",
        "%s. Halfway done, fully capable. %s is still the north star.",
        "50%% of %s in the bag. The second half rewards consistency, not heroics.",
        "You're halfway through %s. That's not average. That's committed.",
        "Midpoint on %s: slow is fine. Stopped is not.",
        "%s at the halfway mark. %s. Hold that close today.",
        "Half the target on %s. The other half is just more of what you've already done.",
        "50%% toward %s. Future you will point to this week.",
        "Mid-trail on %s. You're building proof, not just progress.",
        "%s: halfway. The finish line isn't luck; it's the next several choices.",
        "Halfway to %s. You're not hoping anymore. You're executing.",
        "50%% on %s. %s deserves the second half of your effort.",
        "Midpoint reached for %s. Breathe, recalibrate, continue.",
        "Halfway on %s. The trail gets easier when you trust your feet.",
        "%s at 50%%. Most fade here. You're still lit.",
        "Half the journey on %s. All of your reason still counts: %s.",
    )

    private val reachMilestoneHeadlines = listOf(
        "Another marker on the trail.",
        "Progress you can point to.",
        "The number moved. Because you did.",
        "Trail marker claimed.",
        "That's real distance on %s.",
        "Momentum looks good on you.",
        "One more proof point for %s.",
        "You're stacking evidence.",
        "The target flinched.",
        "Forward is a habit now.",
    )

    private val reachMilestoneBodies = listOf(
        "%s keeps getting less hypothetical. %s",
        "Another marker down on %s. The finish line hears you coming.",
        "%s. You're not guessing anymore. You're measuring.",
        "Trail marker on %s. %s still fuels this.",
        "%s is closer than it was. That's the whole game.",
        "Progress on %s isn't accidental. You built it.",
        "%s moved because you moved. Simple. Powerful.",
        "Another step on %s. %s is working.",
        "The trail on %s is real. Keep walking.",
        "%s: evidence over excuses. More coming.",
    )

    private val reachCheers = listOf(
        "The trail remembers who kept walking.",
        "Forward is your signature now.",
        "Numbers move for people like you.",
        "Keep the pace. Keep the proof.",
        "Summit energy.",
        "One rep closer to done.",
        "You're making the goal nervous.",
        "Trail marker: claimed.",
        "Momentum is a choice. You chose it.",
        "The finish line just got closer.",
        "Progress looks good on you.",
        "Keep stacking wins.",
        "You're not dabbling. You're doing.",
        "The target can feel you coming.",
        "Proof > promises.",
        "Another chapter, another win.",
        "You're building a finish story.",
        "Pace by pace. Win by win.",
        "The number bends for consistency.",
        "Keep going. The trail rewards stubborn hearts.",
        "You're the kind of finisher who shows up mid-journey.",
        "More trail. More you.",
    )

    private val reachCheckInIntros = listOf(
        "Sunday pulse. How's the pace on this one?",
        "Quick trail check: still moving?",
        "Weekly honesty: ahead, on pace, or recalibrating?",
        "How did the number move this week?",
        "Pace check. No judgment, just truth.",
        "One week closer. How'd it feel?",
        "Trail report time.",
        "Still climbing, or need a gear shift?",
        "Weekly reach check. Where are you?",
        "How's the rhythm toward the target?",
        "Sunday sync: pace vs. Plan?",
        "Did the goal feel closer this week?",
        "Honest week-in-review. 10 seconds.",
        "Reach check: momentum or maintenance?",
        "How'd the target treat you this week?",
        "Weekly pulse on the number.",
        "Still hunting the finish, or coasting?",
        "Trail log: this week in one word?",
        "Pace reality check. You got this.",
        "How far did %s move? (metaphorically. Or literally).",
        "Weekly checkpoint. Be honest, be kind.",
        "Sunday: celebrate or recalibrate?",
    )

    private fun reachNarratives(
        metrics: GoalProgressEngine.GoalMetrics,
        goal: GoalEntity,
    ): List<String> {
        val pct = (metrics.progressFraction * 100).roundInt()
        val title = goal.title.ifBlank { "your target" }
        return when {
            metrics.canAutoComplete -> listOf(
                "You hit the number on $title. That's a real finish line. Take the win.",
                "$title: target met. Proof delivered.",
                "Done. $title isn't a someday thing anymore.",
            )
            pct in 45..55 -> listOf(
                "Midpoint on $title. The famous fade zone. You're still here.",
                "Halfway on $title. Breathe, recalibrate, keep the pace.",
                "50% on $title. Most quit in the middle. You're writing a different story.",
                "Mid-trail on $title. Identity forms here. Keep showing up.",
                "Halfway there on $title. The second half rewards patience.",
            )
            metrics.paceDeltaLabel?.contains("behind", true) == true -> listOf(
                "Behind pace on $title. Adjust the date or pick up one small win today.",
                "$title is lagging the calendar. One focused week changes the graph.",
                "Behind on $title? Not failure. Feedback. Respond, don't retreat.",
            )
            metrics.paceDeltaLabel?.contains("ahead", true) == true -> listOf(
                "Ahead on $title. Protect the rhythm, don't burn out proving a point.",
                "$title: ahead of pace. Steady beats sprinting from here.",
                "You're early on $title. Enjoy it, then keep the same calm pace.",
            )
            else -> listOf(
                "$title: every completion is a vote for who you're becoming.",
                "The number on $title moves when you do. Keep moving.",
                "$title isn't waiting. It's counting. So are you.",
                "Stakes on $title: ${goal.whyStatement.ifBlank { "finish what you started" }}.",
                "X of Y on $title. The math is simple. The discipline isn't. And you're doing it.",
            )
        }
    }

    // ── BUILD ────────────────────────────────────────────────────────────────

    private fun buildMoment(
        seed: String,
        milestone: MilestoneEntity,
        title: String,
        why: String?,
    ): MomentCopy {
        val isMid = milestone.targetPercent == 50f
        val headlines = if (isMid) buildMidpointHeadlines else buildMilestoneHeadlines
        return MomentCopy(
            badge = if (isMid) "ROOTS RITUAL" else "GROWTH STAGE",
            headline = formatOptional(headlines[stableIndex(seed, headlines.size)], title),
            subhead = milestone.title,
            body = (if (isMid) buildMidpointBodies else buildMilestoneBodies)[
                stableIndex(seed + "b", if (isMid) buildMidpointBodies.size else buildMilestoneBodies.size)
            ].format(title, why ?: "who you're becoming"),
            cheer = buildCheers[stableIndex(seed + "c", buildCheers.size)],
            emoji = if (isMid) "🌱" else "🌿",
        )
    }

    private val buildMidpointHeadlines = listOf(
        "Taking root. This is starting to stick.",
        "Halfway grown. The habit has soil now.",
        "Roots down. You're not pretending anymore.",
        "Mid-growth magic. Identity is sprouting.",
        "50% grown into this version of you.",
        "The habit has roots. You put them there.",
        "Taking root. Messy, real, yours.",
        "Halfway to automatic. Keep watering.",
        "Roots ritual: you're becoming the person who does this.",
        "Mid-growth on %s. The plant remembers who showed up.",
        "Taking root isn't pretty. It's powerful.",
        "Halfway to second nature. Feel that?",
        "Roots down on %s. Consistency is compost.",
        "Mid-growth milestone. You're not starting anymore.",
        "50% grown. The daily version of you is real.",
        "Taking root: proof that tiny beats perfect.",
        "Halfway to identity shift. Keep the rhythm.",
        "The habit has grip now. You gave it that.",
        "Mid-growth. Still small, already stubborn.",
        "Roots ritual unlocked. You're the gardener.",
        "Halfway to effortless. Almost there.",
        "Taking root on %s. Show up tomorrow like you mean it.",
    )

    private val buildMidpointBodies = listOf(
        "%s is taking root. Not because it's easy. Because you kept the tiny promise.",
        "Halfway grown into %s. %s. That's the soil this habit grows in.",
        "%s has roots now. Miss a day? The plant bends. It doesn't die.",
        "Mid-growth on %s: you're not forcing anymore. You're forming.",
        "%s at halfway: identity beats motivation. You're proving it.",
        "Taking root on %s. %s is the reason the roots go deep.",
        "Halfway to automatic on %s. Tomorrow is another vote.",
        "%s is sticking. That's not luck. That's repetition.",
        "Roots on %s: small daily proof > occasional heroics.",
        "Mid-growth milestone for %s. The boring days built this.",
        "%s halfway grown. Keep it tiny. Keep it true.",
        "Taking root means %s survives a bad week. You're building that.",
        "Halfway on %s. %s still matters. Water it today.",
        "%s has soil now. You are the weather it learns to trust.",
        "Mid-growth: %s isn't a project anymore. It's a pattern.",
        "Roots down on %s. The next stage is rhythm.",
        "Halfway to second nature on %s. Show up like it's already you.",
        "%s taking root. Celebrate the boring wins.",
        "Mid-growth on %s. %s. Read that when motivation dips.",
        "Halfway grown. %s is becoming default, not debate.",
        "Roots ritual for %s: you're the kind of person who returns.",
        "%s at 50%% growth. Keep the anchor cue close today.",
    )

    private val buildMilestoneHeadlines = listOf(
        "New growth stage.",
        "The plant noticed you.",
        "Another layer of identity.",
        "Bloom by bloom.",
        "Growth logged.",
        "You're tending something real.",
        "Stage unlocked.",
        "The rhythm is building.",
        "Small → significant.",
        "Another day, another root.",
    )

    private val buildMilestoneBodies = listOf(
        "%s grew again because you showed up. %s",
        "New stage on %s. Consistency is the sunlight.",
        "%s: another proof you return.",
        "Growth on %s isn't dramatic. It's daily. It's working.",
        "%s. %s is becoming muscle memory.",
        "Stage up on %s. Keep the version tiny.",
        "%s bloomed a little more. You did that.",
        "Another layer on %s. Identity stacks.",
        "%s grows when you do. Literally.",
        "Growth logged on %s. Boring days count.",
    )

    private val buildCheers = listOf(
        "Keep watering the tiny version.",
        "Roots before results.",
        "Identity is a daily vote.",
        "Bloom by bloom.",
        "You're becoming the habit.",
        "Small still counts.",
        "The plant sees you.",
        "Rhythm > motivation.",
        "Another day, another root.",
        "Growth looks quiet. It's not.",
        "You're tending something real.",
        "Show up tomorrow like it's already you.",
        "Consistency is your superpower.",
        "The boring days are the magic days.",
        "Keep the promise tiny.",
        "You're building default behavior.",
        "Another vote for future you.",
        "The habit remembers.",
        "Grow slow. Grow sure.",
        "You're not dabbling. You're planting.",
        "Tomorrow wants the same you.",
        "Keep the rhythm alive.",
    )

    private val buildCheckInIntros = listOf(
        "How automatic did %s feel this week?",
        "Sunday soil check. Still growing?",
        "Is %s becoming default yet?",
        "Weekly build pulse: effort or ease?",
        "How much did %s feel like you?",
        "Rhythm check. Natural or forced?",
        "Did the tiny version win this week?",
        "Build check: identity forming?",
        "How sticky was %s?",
        "Weekly honesty on the daily action.",
        "Still planting, or already rooted?",
        "How automatic. 1 to 'that's just me'?",
        "Sunday sync on the habit you're building.",
        "Did %s feel lighter this week?",
        "Build pulse: friction or flow?",
        "Weekly check. Are you becoming it?",
        "How did the daily vote go?",
        "Rhythm report for %s.",
        "Still showing up small?",
        "Build check. Roots or rut?",
        "How much of %s was choice vs. Default?",
        "Sunday: celebrate the boring wins.",
    )

    private fun buildNarratives(stage: Int, title: String): List<String> {
        val t = title.ifBlank { "this habit" }
        return when (stage) {
            0 -> listOf(
                "$t needs a daily action linked. Plant the seed. Pick a habit.",
                "Day one energy on $t. Tiny beats perfect.",
                "$t: link a habit and start the clock.",
            )
            4 -> listOf(
                "$t feels automatic. Ready to defend it in Maintain?",
                "Second nature on $t. Protect it with a floor.",
                "$t isn't a project anymore. It's you.",
            )
            3 -> listOf(
                "Steady rhythm on $t. Identity is forming. Keep the vote daily.",
                "$t has a pulse now. Don't break the chain without reason.",
                "Rhythm stage on $t: boring days are the win days.",
            )
            2 -> listOf(
                "$t is taking root. Misses slow growth. They don't erase it.",
                "Roots on $t. Show up tomorrow like the plant depends on it.",
                "Taking root on $t. Halfway to effortless.",
            )
            1 -> listOf(
                "First bloom on $t. Tiny and consistent beats heroic and rare.",
                "$t bloomed. Keep it small. Keep it daily.",
                "First stage on $t. The hardest part is starting. You started.",
            )
            else -> listOf(
                "You're becoming: $t.",
                "$t grows one daily vote at a time.",
                "Build mode on $t. Rhythm before results.",
            )
        }
    }

    // ── QUIT ─────────────────────────────────────────────────────────────────

    private fun quitMoment(
        seed: String,
        milestone: MilestoneEntity,
        title: String,
        why: String?,
        metrics: GoalProgressEngine.GoalMetrics?,
    ): MomentCopy {
        val isWeek = milestone.targetPercent == 7f
        val isTwoWeeks = milestone.targetPercent == 14f
        val headlines = when {
            isWeek -> quitWeekHeadlines
            isTwoWeeks -> quitTwoWeekHeadlines
            else -> quitMilestoneHeadlines
        }
        val bodies = when {
            isWeek -> quitWeekBodies
            isTwoWeeks -> quitTwoWeekBodies
            else -> quitMilestoneBodies
        }
        val badge = when {
            isWeek || isTwoWeeks -> "CLEAR DAYS RITUAL"
            else -> "CLEAR MILESTONE"
        }
        val saved = metrics?.costSavedLabel?.let { " · $it" }.orEmpty()
        return MomentCopy(
            badge = badge,
            headline = formatOptional(headlines[stableIndex(seed, headlines.size)], title),
            subhead = milestone.title + saved,
            body = bodies[stableIndex(seed + "b", bodies.size)]
                .format(title, why ?: "the life you're choosing"),
            cheer = quitCheers[stableIndex(seed + "c", quitCheers.size)],
            emoji = when {
                isWeek -> "✨"
                isTwoWeeks -> "💜"
                milestone.targetPercent >= 30f -> "🏆"
                else -> "🌤️"
            },
        )
    }

    private val quitWeekHeadlines = listOf(
        "One week clear. That's not luck. That's you.",
        "7 days. The urge didn't win.",
        "A full week of choosing differently.",
        "One week clear. Feel that weight lift?",
        "7 days: proof the pattern can break.",
        "Week one done. The hardest chapter is often first.",
        "Seven clear days. You're rewriting the script.",
        "One week. The body is learning a new normal.",
        "7 days clear on %s. That's courage in calendar form.",
        "A week without giving in. That's real.",
        "Seven days. The old habit got no votes.",
        "One week clear. You're building evidence.",
        "7 days: the wave still comes, but you're surfing.",
        "Week one: %s is becoming choice, not craving.",
        "Seven clear days. Celebrate before the next one.",
        "One week. The identity shift is starting.",
        "7 days clear. Longer than the urge usually lasts.",
        "A week of clear days. That's a statement.",
        "Seven days. You're not white-knuckling forever. Just today × 7.",
        "One week clear. The future noticed.",
        "7 days: %s is losing its grip.",
        "Week one ritual: you stayed.",
    )

    private val quitWeekBodies = listOf(
        "%s. Seven clear days. %s is worth every one.",
        "One week on %s. Urges visited; you didn't move out.",
        "7 days clear from %s. The brain is taking notes.",
        "%s: a week of choosing %s over the old loop.",
        "Seven days on %s. That's not perfection. That's persistence.",
        "One week clear. %s. Read that when the wave returns.",
        "%s at 7 days: you're proving the trigger isn't the boss.",
        "Seven clear days on %s. Small wins stacked.",
        "%s. Week one done. The next week wants the same you.",
        "7 days on %s. %s is becoming louder than the urge.",
        "One week clear. %s is evidence, not hope.",
        "Seven days on %s. You're not 'trying to quit'. You're quitting.",
        "%s: 7 clear days. Breathe. You earned this pause.",
        "One week on %s. Replacement habits are working. Keep them close.",
        "Seven days clear. %s remembers why.",
        "%s at one week: the habit of stopping is forming.",
        "7 days on %s. Urges pass. You stayed.",
        "One week clear on %s. %s. Hold that on day eight.",
        "Seven days: %s isn't a fantasy timeline anymore.",
        "%s. Week one. The longest streak starts with seven.",
        "7 clear days on %s. You're the author now.",
        "One week on %s. Celebrate, then protect day eight.",
    )

    private val quitTwoWeekHeadlines = listOf(
        "Two weeks clear. The groove is forming.",
        "14 days. The old loop is getting quieter.",
        "Fortnight of freedom. Notice the space?",
        "Two weeks. You're past the steepest climb.",
        "14 clear days on %s. That's a pattern.",
        "Two weeks: identity is shifting.",
        "Fortnight clear. The urge isn't news anymore.",
        "14 days. You're collecting proof.",
        "Two weeks on %s. The replacement is sticking.",
        "14 clear days. Keep the next one small.",
    )

    private val quitTwoWeekBodies = listOf(
        "%s. Two weeks clear. %s is winning the argument.",
        "14 days on %s. The wave still rises; you rise faster.",
        "Two weeks on %s. You're not surviving. You're choosing.",
        "%s at 14 days: %s is becoming default.",
        "Fortnight clear on %s. Protect the next day like day one.",
        "Two weeks on %s. The old story is losing readers.",
        "14 days clear. %s. That's the plot twist.",
        "%s: two weeks. Urges are weather. You're the climate.",
        "Fourteen days on %s. Celebrate the boring victories.",
        "Two weeks clear on %s. %s still matters. Keep it close.",
    )

    private val quitMilestoneHeadlines = listOf(
        "Another clear day stacked.",
        "The streak grows.",
        "Clear milestone claimed.",
        "Evidence over excuses.",
        "Another vote for the new loop.",
        "The old habit lost another day.",
        "Clear days compound.",
        "You're choosing again.",
        "Milestone: clarity.",
        "The wave passed. You stayed.",
    )

    private val quitMilestoneBodies = listOf(
        "%s. Another clear marker. %s",
        "Clear milestone on %s. The identity shift continues.",
        "%s stacked another win. %s is the reason.",
        "Milestone on %s. Urges aren't commands.",
        "%s: proof that the replacement works.",
        "Another clear day on %s. Keep the next one tiny.",
        "%s. The streak is a story you're writing.",
        "Clear milestone for %s. %s. Read on hard days.",
        "%s grows clearer one day at a time.",
        "Milestone on %s. You're not the urge.",
    )

    private val quitCheers = listOf(
        "Clear days compound.",
        "The urge isn't the boss.",
        "One day at a time. Stacked.",
        "You're rewriting the loop.",
        "Replacement > willpower.",
        "The wave passes. You stay.",
        "Evidence over excuses.",
        "Choose again tomorrow.",
        "Clear is a skill. You're training it.",
        "The old habit lost today.",
        "Breathe. You stayed.",
        "Urges visit. You don't have to host.",
        "Another vote for future you.",
        "Clear days are courage in calendar form.",
        "The replacement is working.",
        "You're not white-knuckling. You're choosing.",
        "Small clear day > perfect fantasy.",
        "The streak is proof.",
        "Keep the next day small.",
        "You're building a new normal.",
        "Clear milestone: claimed.",
        "The identity shift is real.",
    )

    private val quitCheckInIntros = listOf(
        "How did clear days feel this week?",
        "Sunday check. Urges vs. Choices?",
        "Weekly quit pulse: steady or stormy?",
        "How loud were the triggers?",
        "Clear-day check-in. Be honest, be kind.",
        "Did the replacement show up?",
        "Weekly wave report.",
        "How many days felt like choice?",
        "Sunday sync on %s.",
        "Urges visited. How'd you respond?",
        "Weekly clarity check.",
        "How did the week treat your streak?",
        "Clear-day honesty hour.",
        "Did the old loop get any votes?",
        "Weekly quit report. No shame, just data.",
        "How's the new normal feeling?",
        "Sunday: celebrate clear days.",
        "Trigger check. What showed up?",
        "Weekly proof collection.",
        "How did %s feel this week?",
        "Clear days: more, fewer, or about the same?",
        "Sunday pulse on the life you're choosing.",
    )

    private fun quitNarratives(
        metrics: GoalProgressEngine.GoalMetrics,
        goal: GoalEntity,
    ): List<String> {
        val days = metrics.cleanStreakDays
        val title = goal.title.ifBlank { "this" }
        val longest = metrics.longestCleanStreakDays
        return when {
            days == 0 && goal.lastLapseAt != null -> listOf(
                "One moment on $title. Not a reset. Longest still $longest days. Start today.",
                "A slip on $title isn't the whole story. Longest: $longest. Choose the next hour.",
                "The wave hit $title. It passed before. It will again. Begin now.",
            )
            days == 0 -> listOf(
                "Clear days on $title begin with the next choice.",
                "${goal.whyStatement.ifBlank { "What this costs" }}. Let that anchor the first day.",
                "$title: day one is always available.",
            )
            days == 1 -> listOf(
                "First clear day on $title. The hardest hour is often the first.",
                "Day one clear on $title. Protect day two like it's gold.",
                "One day on $title. The replacement habit is your friend today.",
            )
            days < 7 -> listOf(
                "$days clear days on $title. Early days are steep. You're in them.",
                "$title: $days days of proof. Urges are weather.",
                "$days clear on $title. Stack tomorrow the same way.",
            )
            else -> listOf(
                "$days clear days on $title. Longest ever: $longest.",
                "$title at $days days. The new loop is sticking.",
                "$days clear on $title. ${metrics.costSavedLabel ?: "Keep the replacement close."}",
            )
        }
    }

    // ── MAINTAIN ─────────────────────────────────────────────────────────────

    private fun maintainMoment(
        seed: String,
        milestone: MilestoneEntity,
        title: String,
        why: String?,
    ): MomentCopy {
        val isMonth = milestone.targetPercent == 4f
        val headlines = if (isMonth) maintainMonthHeadlines else maintainMilestoneHeadlines
        val bodies = if (isMonth) maintainMonthBodies else maintainMilestoneBodies
        return MomentCopy(
            badge = if (isMonth) "HELD RITUAL" else "WEEKS HELD",
            headline = formatOptional(headlines[stableIndex(seed, headlines.size)], title),
            subhead = milestone.title,
            body = bodies[stableIndex(seed + "b", bodies.size)]
                .format(title, why ?: "the standard you set"),
            cheer = maintainCheers[stableIndex(seed + "c", maintainCheers.size)],
            emoji = if (isMonth) "🔥" else "🛡️",
        )
    }

    private val maintainMonthHeadlines = listOf(
        "One month held. That's identity.",
        "Four weeks defended. You're not dabbling.",
        "A month of meeting the floor.",
        "30-ish days of showing up. That's you now.",
        "One month held on %s. The flame is real.",
        "Four weeks. The standard stuck.",
        "A month of holding the line.",
        "One month: maintenance became personality.",
        "Four weeks held. Boring wins count.",
        "A month on %s. You're the kind of person who keeps standards.",
        "One month held. Celebrate the quiet consistency.",
        "Four weeks. The floor wasn't optional. You met it.",
        "A month of %s. Identity unlocked.",
        "One month held. The drift didn't win.",
        "Four weeks. You're defending something real.",
        "A month on %s. Keep the flame.",
        "One month held. That's not luck.",
        "Four weeks of floor met. Respect.",
        "A month. %s is part of you now.",
        "One month held ritual: you're the standard.",
        "Four weeks. The habit of holding habits.",
        "A month on %s. Still you. Still holding.",
    )

    private val maintainMonthBodies = listOf(
        "%s. One month held. %s is who you are now, not who you're trying to be.",
        "Four weeks defending %s. The floor wasn't a suggestion. You met it.",
        "%s at one month: maintenance is the real flex.",
        "One month on %s. %s is the reason the flame stays lit.",
        "Four weeks held on %s. Drift tried. You didn't.",
        "%s. A month of showing up. That's identity, not discipline porn.",
        "One month held on %s. Protect next week the same way.",
        "Four weeks on %s. %s deserves this consistency.",
        "%s: one month of floor met. You're not performing anymore.",
        "A month holding %s. Celebrate before the next week starts.",
        "One month on %s. The standard is sticking because you are.",
        "Four weeks on %s. %s. Read that when motivation dips.",
        "%s held for a month. Boring days built this.",
        "One month on %s. You're defending a future self.",
        "Four weeks held. %s is default now.",
        "%s at one month: the flame is yours to keep.",
        "One month on %s. %s still matters. Hold the floor.",
        "Four weeks defending %s. Respect the quiet wins.",
        "%s. A month held. The next month wants the same you.",
        "One month on %s. Identity > inspiration.",
        "Four weeks on %s. You're the thermostat, not the weather.",
        "A month held on %s. Keep the next week small and sure.",
    )

    private val maintainMilestoneHeadlines = listOf(
        "Another week defended.",
        "The floor held.",
        "Standard maintained.",
        "Flame still lit.",
        "Another period, another proof.",
        "You kept the line.",
        "Maintenance is winning.",
        "Weeks stacking.",
        "The drift lost this round.",
        "Held. Again.",
    )

    private val maintainMilestoneBodies = listOf(
        "%s. Another period held. %s",
        "Weeks stacking on %s. The floor is non-negotiable. You agreed.",
        "%s defended again. %s fuels that.",
        "Another held milestone on %s. Boring consistency wins.",
        "%s: the standard sticks because you do.",
        "Held milestone on %s. Protect the next window.",
        "%s. Still you. Still holding.",
        "Another week on %s. %s is working.",
        "%s maintained. Identity compounds.",
        "Held on %s. Drift can wait.",
    )

    private val maintainCheers = listOf(
        "Hold the floor.",
        "The flame is yours.",
        "Maintenance is the real flex.",
        "Defend the standard.",
        "Boring wins count.",
        "You're the thermostat.",
        "Another week, another proof.",
        "Drift loses. You don't.",
        "Keep the line.",
        "Identity > inspiration.",
        "The floor met you. Meet it back.",
        "Standards stick because you do.",
        "Quiet consistency wins.",
        "Protect the next week.",
        "You're not performing. You're being.",
        "The habit of holding habits.",
        "Flame lit. Keep it.",
        "Another held period.",
        "Maintenance mode: elite.",
        "The standard is you.",
        "Hold. Again.",
        "Weeks held = life held.",
    )

    private val maintainCheckInIntros = listOf(
        "Did you hold the floor this week?",
        "Sunday standard check.",
        "Weekly maintain pulse. Floor met?",
        "How's the flame on %s?",
        "Did the minimum happen?",
        "Weekly defense report.",
        "Floor check. Honest edition.",
        "How many days did the standard win?",
        "Sunday sync: held or drifting?",
        "Weekly maintain honesty.",
        "Did %s survive the week?",
        "Floor met, close, or under?",
        "How's the identity holding?",
        "Weekly flame report.",
        "Standard check. No drama, just truth.",
        "Did you defend %s?",
        "Sunday: celebrate weeks held.",
        "Weekly minimum audit.",
        "How's maintenance feeling?",
        "Floor vs. Drift. Who won?",
        "Weekly hold check on %s.",
        "Still the person who keeps standards?",
    )

    private val maintainNudgeHeadlines = listOf(
        "Two slips. The floor is wobbling.",
        "The flame's dimming. One small reset?",
        "Drift is knocking. Answer with one win.",
        "Two misses in a row. Intervene early.",
        "The standard needs you today.",
        "Floor alert. Still reversible.",
        "Two days missed. Don't make it three.",
        "Maintenance mode: gentle emergency.",
        "The habit of holding is slipping.",
        "Two slips. Small action, big save.",
    )

    private val maintainNudgeBodies = listOf(
        "%s missed twice in a row. One tiny completion today steers the week.",
        "The floor on %s is wobbling. Do the smallest version now. Not tomorrow.",
        "%s: two slips isn't failure. It's a yellow flag. Respond.",
        "Drift on %s is normal. Three misses is optional. Choose one win.",
        "%s needs one completion today. That's the whole intervention.",
        "Two misses on %s. The flame dims before it dies. Relight it small.",
        "%s is drifting. One check-in with the habit saves the week.",
        "The standard on %s wobbled. You can steady it in ten minutes.",
        "%s: two slips. Future you wants one small action today.",
        "Maintenance on %s. Intervene now, celebrate later.",
    )

    private val maintainNudgeCheers = listOf(
        "One small win today.",
        "Steer before it drifts.",
        "The floor is still reachable.",
        "Ten minutes saves the week.",
        "Relight the flame.",
        "Respond, don't retreat.",
        "Tiny completion > perfect plan.",
        "You know what to do.",
        "The standard wants one vote.",
        "Save the streak small.",
    )

    private fun maintainNarratives(
        metrics: GoalProgressEngine.GoalMetrics,
        goal: GoalEntity,
    ): List<String> {
        val title = goal.title.ifBlank { "this standard" }
        return when {
            metrics.consecutiveMissDays >= 2 -> listOf(
                "Two slips on $title. One small completion today steers the week.",
                "$title is wobbling. Intervene small, not someday.",
                "Drift on $title is reversible. Do the floor version now.",
                "Two misses on $title. Yellow flag, not red. Respond.",
                "$title: one win today beats a perfect plan tomorrow.",
            )
            metrics.floorMetThisWeek && metrics.weeksHeld > 0 -> listOf(
                "${metrics.weeksHeld} weeks held on $title. This is who you are now.",
                "$title: floor met. The flame stays lit because you do.",
                "Week held on $title. Boring consistency is the whole game.",
            )
            else -> listOf(
                "Defending $title: ${goal.whyStatement.ifBlank { "hold the floor" }}.",
                "$title. Maintenance is the real finish line.",
                "Hold the floor on $title. Consistency is the point.",
                "$title: meet the minimum. Identity follows.",
                "Standard on $title. One completion counts.",
            )
        }
    }

    private fun stableIndex(seed: String, size: Int): Int {
        if (size <= 0) return 0
        return (seed.hashCode() and 0x7FFFFFFF) % size
    }

    private fun formatOptional(template: String, vararg args: String): String {
        if (!template.contains("%")) return template
        return runCatching { template.format(*args) }.getOrDefault(template)
    }

    private fun Float.roundInt(): Int = kotlin.math.round(this).toInt()
}
