package com.saintnico.verdlyhabits.data.streak

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.HabitStoreReader
import com.saintnico.verdlyhabits.data.local.streak.CompletionRecordEntity
import com.saintnico.verdlyhabits.data.local.streak.PendingOpEntity
import com.saintnico.verdlyhabits.data.local.streak.PendingOpKind
import com.saintnico.verdlyhabits.data.local.streak.ShieldBalanceEntity
import com.saintnico.verdlyhabits.data.local.streak.ShieldSpendEntity
import com.saintnico.verdlyhabits.data.local.streak.StreakLossEntity
import com.saintnico.verdlyhabits.data.local.streak.StreakWriteOps
import com.saintnico.verdlyhabits.data.local.streak.buildShieldLedger
import com.saintnico.verdlyhabits.domain.streak.CompletionRecord
import com.saintnico.verdlyhabits.domain.streak.CompletionSource
import com.saintnico.verdlyhabits.domain.streak.Schedule
import com.saintnico.verdlyhabits.domain.streak.ShieldLedger
import com.saintnico.verdlyhabits.domain.streak.StreakConfig
import com.saintnico.verdlyhabits.domain.streak.StreakEngine
import com.saintnico.verdlyhabits.domain.streak.StreakInput
import com.saintnico.verdlyhabits.domain.streak.StreakLossRecord
import com.saintnico.verdlyhabits.domain.streak.StreakResult
import com.saintnico.verdlyhabits.domain.streak.StreakStatus
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The clock, made explicit so day rollover and timezone travel are observable events. */
data class DayContext(
    val today: LocalDate,
    val zone: ZoneId,
    val at: Instant,
) {
    companion object {
        fun current(): DayContext {
            val zone = ZoneId.systemDefault()
            val now = Instant.now()
            return DayContext(today = LocalDate.now(zone), zone = zone, at = now)
        }
    }
}

/** Everything the UI needs, derived in one place. */
data class StreakSnapshot(
    val results: Map<String, StreakResult> = emptyMap(),
    val shields: ShieldLedger = ShieldLedger(),
    val unacknowledgedLosses: List<StreakLossRecord> = emptyList(),
    val day: DayContext = DayContext.current(),
) {
    fun forHabit(habitId: String): StreakResult? = results[habitId]

    /** Highest live run across all habits — the number the profile and stats should show. */
    val longestCurrent: Int get() = results.values.maxOfOrNull { it.currentStreak } ?: 0

    val longestEver: Int get() = results.values.maxOfOrNull { it.longestStreak } ?: 0

    val atRiskHabitIds: List<String>
        get() = results.values.filter { it.isAtRisk }.map { it.habitId }

    val repairable: List<StreakResult>
        get() = results.values.filter { it.isRepairable }

    /**
     * Single eligibility check for the habit-repair sheet.
     * Duo weekly grace is a different mechanic ([com.saintnico.verdlyhabits.engine.DuoStreakEngine.graceAvailableThisWeek])
     * and must not use this offer — mixing them is how two "shield" CTAs appear at once.
     */
    fun activeRepairOffer(dismissedTokens: Set<String>): com.saintnico.verdlyhabits.domain.streak.RepairOffer? =
        repairable.asSequence()
            .mapNotNull { it.repairOffer }
            .firstOrNull { o ->
                o.streakAtStake >= 1 &&
                    "${o.habitId}:${o.missedDate}" !in dismissedTokens
            }

    /** The loss worth interrupting the user for, if any. */
    val lossToSurface: StreakLossRecord?
        get() = unacknowledgedLosses
            .filter { it.isWorthSurfacing() }
            .maxByOrNull { it.finalStreak }
}

/**
 * The single writer and single reader of streak state.
 *
 * Reads are derived: [snapshot] recomputes from persisted facts, so nothing can go stale.
 * Writes are funnelled through this class and always land in Room first, then enqueue an
 * outbox op, so a completion made offline is durable before any network call is attempted.
 */
class StreakRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val dao = db.streakDao()
    private val writes = StreakWriteOps(db)
    private val gson = Gson()
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, e ->
            Log.e(TAG, "uncaught streak repo error", e)
        }
    )
    private val reconcileLock = Mutex()

    private val dayContext = MutableStateFlow(DayContext.current())

    /** Bumped whenever habit definitions change, since schedules live in DataStore. */
    private val scheduleRevision = MutableStateFlow(0L)

    var config: StreakConfig = StreakConfig.DEFAULT
        private set

    @OptIn(ExperimentalCoroutinesApi::class)
    val snapshot: StateFlow<StreakSnapshot> = run {
        val dayFlow = combine(dayContext, scheduleRevision) { day, _ -> day }
        val ledgerFlow = combine(
            dao.observeCompletions(),
            dao.observeSpends(),
            dao.observeBalance(),
        ) { completions, spends, balance ->
            Triple(completions, spends, balance)
        }
        combine(ledgerFlow, dao.observeLosses(), dayFlow) { ledger, losses, day ->
            SnapshotInputs(
                completions = ledger.first,
                spends = ledger.second,
                balance = ledger.third,
                losses = losses,
                day = day,
            )
        }.mapLatest { inputs ->
            try {
                val shields = buildShieldLedger(inputs.balance, inputs.spends)
                val records = inputs.completions.mapNotNull { it.toDomain() }
                val lossRecords = inputs.losses.mapNotNull { it.toDomain() }
                val recordedIds = inputs.losses.map { it.id }.toSet()
                val schedules = loadSchedules()
                val results = schedules.mapValues { (habitId, schedule) ->
                    StreakEngine.evaluate(
                        StreakInput(
                            habitId = habitId,
                            completions = records.filter { it.habitId == habitId },
                            schedule = schedule,
                            today = inputs.day.today,
                            now = inputs.day.at,
                            zone = inputs.day.zone,
                            shields = shields,
                            config = config,
                            swaps = emptySet(),
                            recordedLosses = recordedIds,
                        ),
                    )
                }
                StreakSnapshot(
                    results = results,
                    shields = shields,
                    unacknowledgedLosses = lossRecords.filter { !it.acknowledged },
                    day = inputs.day,
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                Log.e(TAG, "snapshot evaluation failed", e)
                StreakSnapshot(day = inputs.day)
            }
        }.catch { e ->
            Log.e(TAG, "snapshot flow failed", e)
            emit(StreakSnapshot())
        }.stateIn(scope, SharingStarted.Eagerly, StreakSnapshot())
    }

    private data class SnapshotInputs(
        val completions: List<CompletionRecordEntity>,
        val spends: List<com.saintnico.verdlyhabits.data.local.streak.ShieldSpendEntity>,
        val balance: ShieldBalanceEntity?,
        val losses: List<StreakLossEntity>,
        val day: DayContext,
    )

    // ── Clock ─────────────────────────────────────────────────────────────────

    /**
     * Re-reads the wall clock and timezone. Called on date change, timezone change, app
     * foreground, and a periodic tick — which is what makes a screen left open overnight
     * correct itself instead of showing yesterday's state.
     */
    fun refreshDay() {
        dayContext.value = DayContext.current()
        scope.launch { reconcile() }
    }

    /** Signals that habit definitions (and therefore schedules) changed. */
    fun invalidateSchedules() {
        scheduleRevision.value = scheduleRevision.value + 1
    }

    private suspend fun loadSchedules(): Map<String, Schedule> = withContext(Dispatchers.IO) {
        HabitStoreReader.loadHabits(appContext)
            .filter { !it.isArchived }
            .associate { it.id to it.toSchedule() }
    }

    // ── Writes ────────────────────────────────────────────────────────────────

    /**
     * Records that a habit was completed today, locking the day to the current zone so a
     * later flight cannot move it.
     */
    suspend fun complete(
        habitId: String,
        proofUrl: String? = null,
        source: CompletionSource = if (proofUrl != null) CompletionSource.PROOF else CompletionSource.MANUAL,
    ): StreakResult? = withContext(Dispatchers.IO) {
        val day = DayContext.current()
        val record = CompletionRecord(
            habitId = habitId,
            localDate = day.today,
            completedAt = day.at,
            zoneId = day.zone.id,
            offsetSeconds = day.zone.rules.getOffset(day.at).totalSeconds,
            source = source,
            proofUrl = proofUrl,
        )
        val entity = CompletionRecordEntity.from(record)
        writes.recordCompletion(
            row = entity,
            op = outboxOp(PendingOpKind.COMPLETION_UPSERT, entity),
        )
        dayContext.value = day
        reconcile()
        evaluateNow(habitId)
    }

    /** Undo for a same-day mistap. Only today's record may be removed. */
    suspend fun undoToday(habitId: String) = withContext(Dispatchers.IO) {
        val day = DayContext.current()
        dao.deleteCompletion(habitId, day.today.toString())
        dao.enqueue(
            outboxOp(
                PendingOpKind.COMPLETION_UPSERT,
                mapOf("habitId" to habitId, "localDate" to day.today.toString(), "deleted" to true),
            ),
        )
        refreshDay()
    }

    /**
     * Spends one shield to repair a missed day. Atomic and idempotent — calling twice for the
     * same day consumes one shield, not two.
     */
    suspend fun repairWithShield(habitId: String, missedDate: LocalDate): Boolean =
        withContext(Dispatchers.IO) {
            val spend = ShieldSpendEntity(
                id = ShieldSpendEntity.idFor(habitId, missedDate),
                habitId = habitId,
                coveredDate = missedDate.toString(),
                committedAt = System.currentTimeMillis(),
            )
            val spent = writes.spendShield(spend)
            if (spent) {
                dao.enqueue(outboxOp(PendingOpKind.SHIELD_SPEND, spend))
                refreshDay()
            }
            spent
        }

    /** Credits purchased shields, capped so purchases cannot exceed the same ceiling. */
    suspend fun creditPurchasedShields(count: Int): Int = withContext(Dispatchers.IO) {
        val applied = writes.creditPurchasedShields(count, config.shieldCap)
        if (applied > 0) refreshDay()
        applied
    }

    suspend fun acknowledgeLoss(lossId: String) = withContext(Dispatchers.IO) {
        dao.acknowledgeLoss(lossId)
    }

    suspend fun markLossNotified(lossId: String) = withContext(Dispatchers.IO) {
        dao.markLossNotified(lossId, System.currentTimeMillis())
    }

    suspend fun unnotifiedLosses(): List<StreakLossRecord> = withContext(Dispatchers.IO) {
        dao.unnotifiedLosses().mapNotNull { it.toDomain() }
    }

    suspend fun shieldLedger(): ShieldLedger = withContext(Dispatchers.IO) {
        buildShieldLedger(dao.balance(), dao.allSpends())
    }

    // ── Reconciliation ────────────────────────────────────────────────────────

    /**
     * Applies the write-side consequences of the current state: credits earned shields and
     * commits any newly detected loss.
     *
     * Kept out of the read flow deliberately. Deriving state must stay free of side effects,
     * or a recomposition could grant shields.
     */
    suspend fun reconcile(): List<StreakLossRecord> = reconcileLock.withLock {
        withContext(Dispatchers.IO) {
            val day = dayContext.value
            val schedules = loadSchedules()
            if (schedules.isEmpty()) return@withContext emptyList()

            val completions = dao.allCompletions().mapNotNull { it.toDomain() }
            val newLosses = mutableListOf<StreakLossRecord>()

            for ((habitId, schedule) in schedules) {
                val ledger = buildShieldLedger(dao.balance(), dao.allSpends())
                val recorded = dao.allLosses().map { it.id }.toSet()
                val result = StreakEngine.evaluate(
                    StreakInput(
                        habitId = habitId,
                        completions = completions.filter { it.habitId == habitId },
                        schedule = schedule,
                        today = day.today,
                        now = day.at,
                        zone = day.zone,
                        shields = ledger,
                        config = config,
                        recordedLosses = recorded,
                    ),
                )

                creditShieldsFor(habitId, result)

                val loss = result.pendingLoss
                if (loss != null) {
                    // IGNORE on conflict is what guarantees the loss moment fires once even
                    // if this runs concurrently from a worker and the UI.
                    val inserted = dao.insertLossIfAbsent(StreakLossEntity.from(loss))
                    if (inserted != -1L) newLosses += loss
                }
            }
            newLosses
        }
    }

    private suspend fun creditShieldsFor(habitId: String, result: StreakResult) {
        if (result.status == StreakStatus.BROKEN) return
        val earned = StreakEngine.shieldsEarnedBy(result.currentStreak, config)
        if (earned <= 0) return
        val milestones = (1..earned).map { it * config.shieldEarnIntervalDays }
        writes.creditEarnedShields(habitId, milestones, config.shieldCap, System.currentTimeMillis())
    }

    /** One-off evaluation outside the flow, for callers that need an immediate answer. */
    suspend fun evaluateNow(habitId: String): StreakResult? = withContext(Dispatchers.IO) {
        val schedule = loadSchedules()[habitId] ?: return@withContext null
        val day = dayContext.value
        StreakEngine.evaluate(
            StreakInput(
                habitId = habitId,
                completions = dao.completionsFor(habitId).mapNotNull { it.toDomain() },
                schedule = schedule,
                today = day.today,
                now = Instant.now(),
                zone = day.zone,
                shields = buildShieldLedger(dao.balance(), dao.allSpends()),
                config = config,
                recordedLosses = dao.allLosses().map { it.id }.toSet(),
            ),
        )
    }

    /** Evaluates every habit once, for workers and widgets that have no flow subscription. */
    suspend fun evaluateAll(): Map<String, StreakResult> = withContext(Dispatchers.IO) {
        val day = DayContext.current()
        val ledger = buildShieldLedger(dao.balance(), dao.allSpends())
        val recorded = dao.allLosses().map { it.id }.toSet()
        val completions = dao.allCompletions().mapNotNull { it.toDomain() }
        loadSchedules().mapValues { (habitId, schedule) ->
            StreakEngine.evaluate(
                StreakInput(
                    habitId = habitId,
                    completions = completions.filter { it.habitId == habitId },
                    schedule = schedule,
                    today = day.today,
                    now = day.at,
                    zone = day.zone,
                    shields = ledger,
                    config = config,
                    recordedLosses = recorded,
                ),
            )
        }
    }

    // ── Migration from the legacy DataStore representation ────────────────────

    /**
     * Backfills Room from the legacy `completedDates` string set exactly once.
     *
     * The original data has no instant and no zone, so each day is anchored at local noon:
     * far enough from either boundary that no plausible offset can shift it into an adjacent
     * day, which keeps migrated runs from moving.
     */
    suspend fun migrateLegacyIfNeeded(): Int = withContext(Dispatchers.IO) {
        if (dao.completionCount() > 0) return@withContext 0

        val zone = ZoneId.systemDefault()
        val habits = HabitStoreReader.loadHabits(appContext)
        val rows = mutableListOf<CompletionRecordEntity>()
        for (habit in habits) {
            for (raw in habit.completedDates) {
                val date = runCatching { LocalDate.parse(raw) }.getOrNull() ?: continue
                val at = date.atTime(LocalTime.NOON).atZone(zone).toInstant()
                rows += CompletionRecordEntity(
                    habitId = habit.id,
                    localDate = date.toString(),
                    completedAt = at.toEpochMilli(),
                    zoneId = zone.id,
                    offsetSeconds = zone.rules.getOffset(at).totalSeconds,
                    source = CompletionSource.MIGRATED.name,
                    // Legacy proofs were keyed by the same date string.
                    proofUrl = habit.completionProofs[raw],
                    // Already present in the cloud blob, so nothing to push.
                    syncedAt = at.toEpochMilli(),
                )
            }
        }
        if (rows.isNotEmpty()) dao.upsertCompletions(rows)

        migrateLegacyShieldBalance()
        Log.i(TAG, "migrated ${rows.size} legacy completions into Room")
        rows.size
    }

    /** Moves the DataStore shield count into Room so there is one balance, not two. */
    private suspend fun migrateLegacyShieldBalance() {
        if (dao.balance() != null) return
        val legacy = runCatching {
            com.saintnico.verdlyhabits.data.local.AppDataStore(appContext).streakShields.first()
        }.getOrDefault(0)
        dao.putBalance(
            ShieldBalanceEntity(
                available = legacy.coerceIn(0, config.shieldCap),
                lifetimeEarned = legacy.coerceAtLeast(0),
                lifetimePurchased = 0,
            ),
        )
    }

    private fun outboxOp(kind: PendingOpKind, payload: Any): PendingOpEntity = PendingOpEntity(
        kind = kind.name,
        payload = gson.toJson(payload),
        createdAt = System.currentTimeMillis(),
    )

    companion object {
        private const val TAG = "StreakRepository"

        @Volatile
        private var INSTANCE: StreakRepository? = null

        fun get(context: Context): StreakRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: StreakRepository(context).also { INSTANCE = it }
            }
    }
}

/** Maps a habit's stored scheduling fields onto the engine's [Schedule]. */
fun HabitItem.toSchedule(): Schedule = Schedule(
    frequency = frequency,
    customDaysMask = customDaysMask,
    windowStart = parseLocalTimeOrNull(completionWindowStart),
    windowEnd = parseLocalTimeOrNull(completionWindowEnd),
)

private fun parseLocalTimeOrNull(value: String?): LocalTime? {
    if (value.isNullOrBlank()) return null
    return runCatching { LocalTime.parse(value) }.getOrNull()
}
