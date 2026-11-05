package com.saurabh.sar.platform

import android.util.Log
import com.saurabh.sar.core.Clock
import com.saurabh.sar.core.engine.PendingScheduled
import com.saurabh.sar.core.engine.RunError
import com.saurabh.sar.core.engine.RunPlanner
import com.saurabh.sar.core.engine.RunResult
import com.saurabh.sar.core.engine.RunStateMachine
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.EndReason
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.Run
import com.saurabh.sar.data.SarRepository
import com.saurabh.sar.data.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId

const val TAG = "SAR"

/** The running mode with everything the blocking path needs, held in memory for instant decisions. */
data class ActiveState(
    val run: Run,
    val mode: Mode,
    val rules: Map<String, AppRule>,
    val usage: Map<String, AppUsage>,
) {
    fun usageOf(pkg: String) = usage[pkg] ?: AppUsage(runId = run.id, packageName = pkg)
    val emergencyLeft: Int get() = RunStateMachine.emergencyUsesLeft(mode, run)
}

/** Implemented by the accessibility service so run changes reach the foreground tracker. */
interface ForegroundListener {
    /** Close the open foreground interval at [now] and save it, before the run changes. */
    fun flush(now: Long)
    /** The run, its rules or usage changed: re-check the app in front. */
    fun onActiveChanged()
}

/**
 * Carries out decisions from core: starts/ends runs, applies the [RunPlanner] plan, keeps the
 * one boundary alarm set, and keeps the status notification in step. All calls happen on the
 * main thread; database work suspends on Room's own executor.
 */
class RunController(
    private val repo: SarRepository,
    private val settings: SettingsStore,
    private val alarms: AlarmScheduler,
    private val notifications: Notifications,
    private val clock: Clock,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val mutex = Mutex()
    private val _active = MutableStateFlow<ActiveState?>(null)
    val active: StateFlow<ActiveState?> = _active.asStateFlow()
    private val _pending = MutableStateFlow<List<PendingScheduled>>(emptyList())
    val pending: StateFlow<List<PendingScheduled>> = _pending.asStateFlow()

    @Volatile var foregroundListener: ForegroundListener? = null
    private var loaded = false

    /** Load the active run (if any) and bring everything up to date. Safe to call repeatedly. */
    suspend fun evaluate(isBoot: Boolean = false) = mutex.withLock { evaluateLocked(isBoot) }

    private suspend fun evaluateLocked(isBoot: Boolean) {
        if (!loaded || isBoot) { reloadActive(); loaded = true }
        val now = clock.now()
        val modes = repo.allModes()
        val schedules = repo.schedulesByMode()
        val plan = RunPlanner.plan(now, zone(), _active.value?.run, modes, schedules, repo.lastRunStarts(), isBoot)
        Log.i(TAG, "plan $plan")

        plan.endCurrent?.let { reason ->
            val cur = _active.value
            if (cur != null) {
                // The run ended at its fixed end time, not "now", unless it never had one.
                val endAt = cur.run.plannedEndAt?.coerceAtMost(now) ?: now
                endRunLocked(reason, endAt)
            }
        }
        plan.startScheduled?.let { p ->
            val mode = modes.firstOrNull { it.id == p.modeId }
            if (mode != null) beginRunLocked(RunStateMachine.startScheduled(mode, now, p.window.end), mode)
        }
        _pending.value = plan.pending
        val pendingNames = plan.pending.mapNotNull { pm -> modes.firstOrNull { it.id == pm.modeId } }
        val cur = _active.value
        if (cur != null && pendingNames.isNotEmpty()) notifications.showCollision(pendingNames, cur.mode)
        else notifications.cancelCollision()

        // Re-plan once more if we just started or ended something, to get the right next wake.
        val next = if (plan.endCurrent != null || plan.startScheduled != null) {
            RunPlanner.plan(clock.now(), zone(), _active.value?.run, modes, schedules, repo.lastRunStarts()).nextWakeAt
        } else plan.nextWakeAt
        alarms.setBoundary(next)
        notifications.showRunStatus(_active.value)
    }

    private suspend fun reloadActive() {
        val run = repo.activeRunNow()
        _active.value = run?.let { r ->
            val mode = repo.mode(r.modeId) ?: return@let null
            ActiveState(r, mode, repo.rulesFor(mode.id).associateBy { it.packageName }, repo.usageFor(r.id).associateBy { it.packageName })
        } ?: run?.let { r ->
            // Mode deleted while running: keep a placeholder so the planner can close the run.
            ActiveState(r, Mode(id = r.modeId, name = "Deleted mode", type = com.saurabh.sar.core.model.ModeType.MANUAL, message = ""), emptyMap(), emptyMap())
        }
    }

    /** Start / Start for… a manual mode (FSD 5.1). */
    suspend fun startManual(modeId: Long, durationMs: Long?): RunError? = mutex.withLock {
        val mode = repo.mode(modeId) ?: return@withLock RunError.NOT_ACTIVE
        when (val r = RunStateMachine.startManual(mode, _active.value?.run, clock.now(), durationMs)) {
            is RunResult.Err -> r.error
            is RunResult.Ok -> { beginRunLocked(r.value, mode); afterChangeLocked(); null }
        }
    }

    suspend fun stop(): RunError? = mutex.withLock {
        val cur = _active.value ?: return@withLock RunError.NOT_ACTIVE
        when (val r = RunStateMachine.stop(cur.run, clock.now())) {
            is RunResult.Err -> r.error
            is RunResult.Ok -> { endRunLocked(EndReason.STOPPED, clock.now()); afterChangeLocked(); null }
        }
    }

    suspend fun extend(byMs: Long): RunError? = mutex.withLock {
        val cur = _active.value ?: return@withLock RunError.NOT_ACTIVE
        val end = cur.run.plannedEndAt ?: return@withLock RunError.RUN_NOT_LOCKED
        val newEnd = end + byMs
        if (newEnd - cur.run.startedAt > 24 * 3_600_000L) return@withLock RunError.BAD_DURATION
        when (val r = RunStateMachine.extend(cur.run, newEnd)) {
            is RunResult.Err -> r.error
            is RunResult.Ok -> {
                repo.saveRun(r.value, cur.mode.name)
                _active.value = cur.copy(run = r.value)
                afterChangeLocked(); null
            }
        }
    }

    /** Free a mode, after the math challenge has been passed (FSD 7). */
    suspend fun free(): RunError? = mutex.withLock {
        val cur = _active.value ?: return@withLock RunError.NOT_ACTIVE
        when (val r = RunStateMachine.free(cur.run, clock.now())) {
            is RunResult.Err -> r.error
            is RunResult.Ok -> { endRunLocked(EndReason.FREED, clock.now()); afterChangeLocked(); null }
        }
    }

    /** "Start <Mode>" on the collision warning: ends the current run, no challenge (FSD 5.4). */
    suspend fun switchToScheduled(modeId: Long): Boolean = mutex.withLock {
        val p = _pending.value.firstOrNull { it.modeId == modeId } ?: return@withLock false
        val now = clock.now()
        if (now >= p.window.end) return@withLock false
        val mode = repo.mode(modeId) ?: return@withLock false
        if (_active.value != null) endRunLocked(EndReason.SWITCHED, now)
        beginRunLocked(RunStateMachine.startScheduled(mode, now, p.window.end), mode)
        afterChangeLocked()
        true
    }

    /** Emergency extra time for [pkg] after the wait/phrase effort (FSD 6.2). */
    suspend fun useEmergency(pkg: String): RunError? = mutex.withLock {
        val cur = _active.value ?: return@withLock RunError.NOT_ACTIVE
        when (val r = RunStateMachine.useEmergency(cur.mode, cur.run, cur.usageOf(pkg), clock.now())) {
            is RunResult.Err -> r.error
            is RunResult.Ok -> {
                val (run, usage) = r.value
                repo.saveRun(run, cur.mode.name, listOf(usage))
                _active.value = cur.copy(run = run, usage = cur.usage + (pkg to usage))
                notifications.showRunStatus(_active.value)
                null
            }
        }
    }

    /** A mode was edited: changes to a running mode apply at once, minutes used are kept (FSD 3.2). */
    suspend fun onModesChanged() = mutex.withLock {
        val cur = _active.value
        if (cur != null) {
            val mode = repo.mode(cur.mode.id)
            if (mode != null) {
                _active.value = cur.copy(mode = mode, rules = repo.rulesFor(mode.id).associateBy { it.packageName })
                foregroundListener?.onActiveChanged()
            }
        }
        evaluateLocked(false)
    }

    /** Called by the foreground tracker after each app switch: one DB write per switch (FSD 11.3). */
    fun recordUsage(usage: AppUsage) {
        val cur = _active.value ?: return
        if (usage.runId != cur.run.id) return
        _active.value = cur.copy(usage = cur.usage + (usage.packageName to usage))
    }

    suspend fun persistUsage(usage: AppUsage) {
        runCatching { repo.saveUsage(listOf(usage)) }.onFailure { Log.w(TAG, "save usage", it) }
    }

    /** Raises recorded usage to what Android's usage stats saw (used after reboot / with blocking off). */
    suspend fun mergeUsageStats(statsMs: Map<String, Long>) = mutex.withLock {
        val cur = _active.value ?: return@withLock
        val changed = statsMs.mapNotNull { (pkg, ms) ->
            if (pkg !in cur.rules) return@mapNotNull null
            val u = cur.usageOf(pkg)
            if (ms > u.usedMs + 60_000) u.copy(usedMs = ms) else null
        }
        if (changed.isEmpty()) return@withLock
        repo.saveUsage(changed)
        _active.value = cur.copy(usage = cur.usage + changed.associateBy { it.packageName })
        foregroundListener?.onActiveChanged()
    }

    private suspend fun beginRunLocked(run: Run, mode: Mode) {
        val s = settings.current()
        repo.deleteHistoryBefore(clock.now() - s.historyRetentionDays * 86_400_000L) // FSD 8.3, 11.3
        val saved = repo.insertRun(run, mode.name)
        _active.value = ActiveState(saved, mode, repo.rulesFor(mode.id).associateBy { it.packageName }, emptyMap())
        Log.i(TAG, "run started ${mode.name} locked=${saved.locked} end=${saved.plannedEndAt}")
        foregroundListener?.onActiveChanged()
    }

    private suspend fun endRunLocked(reason: EndReason, at: Long) {
        foregroundListener?.flush(at)
        val cur = _active.value ?: return
        val ended = RunStateMachine.end(cur.run, at, reason)
        repo.saveRun(ended, cur.mode.name, cur.usage.values)
        _active.value = null
        notifications.cancelWarnings()
        Log.i(TAG, "run ended ${cur.mode.name} $reason")
        foregroundListener?.onActiveChanged()
    }

    private suspend fun afterChangeLocked() = evaluateLocked(false)
}
