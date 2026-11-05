package com.saurabh.sar.core.engine

import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.EndReason
import com.saurabh.sar.core.model.MINUTE_MS
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.Run

/** Why a run command was refused. The UI turns these into plain messages. */
enum class RunError {
    ANOTHER_RUN_ACTIVE,
    SCHEDULED_MODE_IS_AUTOMATIC,
    BAD_DURATION,
    RUN_IS_LOCKED,
    RUN_NOT_LOCKED,
    CANNOT_EXTEND_SCHEDULED,
    EXTEND_MUST_BE_LATER,
    NOT_ACTIVE,
    NO_EMERGENCY_LEFT,
    EMERGENCY_DISABLED,
    EMERGENCY_NOT_DURING_BREAK,
}

sealed interface RunResult<out T> {
    data class Ok<T>(val value: T) : RunResult<T>
    data class Err(val error: RunError) : RunResult<Nothing>
}

/**
 * The run lifecycle (FSD 5). Every function is pure: it returns the new [Run] (or an error)
 * and the caller stores it. Allowance and emergency uses live on the run, so a new run
 * always starts fresh (FSD 5.3).
 */
object RunStateMachine {
    const val MIN_DURATION_MS = 15 * MINUTE_MS
    const val MAX_DURATION_MS = 12 * 60 * MINUTE_MS
    const val EXTEND_STEP_MS = 15 * MINUTE_MS

    /** "Start" (open-ended, unlocked) or "Start for…" [durationMs] (locked) on a manual mode. */
    fun startManual(mode: Mode, current: Run?, now: Long, durationMs: Long? = null): RunResult<Run> {
        if (current != null && current.isActive) return RunResult.Err(RunError.ANOTHER_RUN_ACTIVE)
        if (mode.type != ModeType.MANUAL) return RunResult.Err(RunError.SCHEDULED_MODE_IS_AUTOMATIC)
        if (durationMs != null && durationMs !in MIN_DURATION_MS..MAX_DURATION_MS) {
            return RunResult.Err(RunError.BAD_DURATION)
        }
        val endAt = durationMs?.let { now + it }
        return RunResult.Ok(
            Run(modeId = mode.id, startedAt = now, plannedEndAt = endAt, locked = endAt != null, scheduled = false),
        )
    }

    /** A scheduled run is always locked and ends with its window (FSD 5.2). */
    fun startScheduled(mode: Mode, now: Long, windowEnd: Long): Run =
        Run(modeId = mode.id, startedAt = now, plannedEndAt = windowEnd, locked = true, scheduled = true)

    /** One-tap Stop, only for an unlocked run (FSD 5.1). */
    fun stop(run: Run, now: Long): RunResult<Run> {
        if (!run.isActive) return RunResult.Err(RunError.NOT_ACTIVE)
        if (run.locked) return RunResult.Err(RunError.RUN_IS_LOCKED)
        return RunResult.Ok(end(run, now, EndReason.STOPPED))
    }

    /** Locked manual runs can be made longer, never shorter, and get no extra allowance (FSD 5.1). */
    fun extend(run: Run, newEndAt: Long): RunResult<Run> {
        if (!run.isActive) return RunResult.Err(RunError.NOT_ACTIVE)
        if (!run.locked) return RunResult.Err(RunError.RUN_NOT_LOCKED)
        if (run.scheduled) return RunResult.Err(RunError.CANNOT_EXTEND_SCHEDULED)
        val end = run.plannedEndAt ?: return RunResult.Err(RunError.RUN_NOT_LOCKED)
        if (newEndAt <= end) return RunResult.Err(RunError.EXTEND_MUST_BE_LATER)
        return RunResult.Ok(run.copy(plannedEndAt = newEndAt))
    }

    /** Free a mode: ends a locked run after the math challenge (FSD 7). */
    fun free(run: Run, now: Long): RunResult<Run> {
        if (!run.isActive) return RunResult.Err(RunError.NOT_ACTIVE)
        if (!run.locked) return RunResult.Err(RunError.RUN_NOT_LOCKED)
        return RunResult.Ok(end(run, now, EndReason.FREED))
    }

    fun end(run: Run, now: Long, reason: EndReason): Run =
        run.copy(endedAt = now, endReason = reason)

    fun emergencyUsesLeft(mode: Mode, run: Run): Int =
        if (!mode.emergencyEnabled) 0 else (mode.emergencyUses - run.emergencyUsesUsed).coerceAtLeast(0)

    /**
     * Emergency extra time (FSD 6.2): uses are shared across the mode's apps and counted per run.
     * Returns the updated run and app usage.
     */
    fun useEmergency(mode: Mode, run: Run, usage: AppUsage, now: Long): RunResult<Pair<Run, AppUsage>> {
        if (!run.isActive) return RunResult.Err(RunError.NOT_ACTIVE)
        if (!mode.emergencyEnabled) return RunResult.Err(RunError.EMERGENCY_DISABLED)
        if (emergencyUsesLeft(mode, run) <= 0) return RunResult.Err(RunError.NO_EMERGENCY_LEFT)
        val breakUntil = usage.breakUntil
        if (breakUntil != null && now < breakUntil) return RunResult.Err(RunError.EMERGENCY_NOT_DURING_BREAK)
        val until = now + mode.emergencyMinutes * MINUTE_MS
        return RunResult.Ok(
            run.copy(emergencyUsesUsed = run.emergencyUsesUsed + 1) to usage.copy(emergencyUntil = until),
        )
    }
}
