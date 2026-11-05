package com.saurabh.sar.core.engine

import com.google.common.truth.Truth.assertThat
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.EndReason
import com.saurabh.sar.core.model.MINUTE_MS
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.Run
import org.junit.Test

class RunStateMachineTest {
    private val now = 10_000 * MINUTE_MS
    private val study = Mode(id = 1, name = "Self Study", type = ModeType.MANUAL, message = "m", emergencyUses = 2, emergencyMinutes = 5)
    private val office = Mode(id = 2, name = "Office", type = ModeType.SCHEDULED, message = "m")

    private fun <T> RunResult<T>.ok(): T = (this as RunResult.Ok<T>).value
    private fun RunResult<*>.err(): RunError = (this as RunResult.Err).error

    @Test fun `start gives an unlocked open-ended run`() {
        val r = RunStateMachine.startManual(study, null, now).ok()
        assertThat(r.locked).isFalse()
        assertThat(r.plannedEndAt).isNull()
        assertThat(r.emergencyUsesUsed).isEqualTo(0)
    }

    @Test fun `start for gives a locked run with a fixed end`() {
        val r = RunStateMachine.startManual(study, null, now, 60 * MINUTE_MS).ok()
        assertThat(r.locked).isTrue()
        assertThat(r.plannedEndAt).isEqualTo(now + 60 * MINUTE_MS)
    }

    @Test fun `durations outside 15 min to 12 h are refused`() {
        assertThat(RunStateMachine.startManual(study, null, now, 5 * MINUTE_MS).err()).isEqualTo(RunError.BAD_DURATION)
        assertThat(RunStateMachine.startManual(study, null, now, 13 * 60 * MINUTE_MS).err()).isEqualTo(RunError.BAD_DURATION)
    }

    @Test fun `only one mode runs at a time`() {
        val running = Run(id = 9, modeId = 3, startedAt = now, locked = false)
        assertThat(RunStateMachine.startManual(study, running, now).err()).isEqualTo(RunError.ANOTHER_RUN_ACTIVE)
    }

    @Test fun `scheduled modes cannot be started by hand`() {
        assertThat(RunStateMachine.startManual(office, null, now).err()).isEqualTo(RunError.SCHEDULED_MODE_IS_AUTOMATIC)
    }

    @Test fun `unlocked runs stop, locked runs do not`() {
        val open = RunStateMachine.startManual(study, null, now).ok()
        val stopped = RunStateMachine.stop(open, now + 1).ok()
        assertThat(stopped.endReason).isEqualTo(EndReason.STOPPED)
        val locked = RunStateMachine.startManual(study, null, now, 60 * MINUTE_MS).ok()
        assertThat(RunStateMachine.stop(locked, now + 1).err()).isEqualTo(RunError.RUN_IS_LOCKED)
    }

    @Test fun `extend only makes a locked manual run longer and keeps usage`() {
        val locked = RunStateMachine.startManual(study, null, now, 60 * MINUTE_MS).ok().copy(emergencyUsesUsed = 1)
        val longer = RunStateMachine.extend(locked, locked.plannedEndAt!! + 15 * MINUTE_MS).ok()
        assertThat(longer.plannedEndAt).isEqualTo(now + 75 * MINUTE_MS)
        assertThat(longer.startedAt).isEqualTo(locked.startedAt)
        assertThat(longer.emergencyUsesUsed).isEqualTo(1)
        assertThat(RunStateMachine.extend(locked, locked.plannedEndAt!! - 1).err()).isEqualTo(RunError.EXTEND_MUST_BE_LATER)
        val open = RunStateMachine.startManual(study, null, now).ok()
        assertThat(RunStateMachine.extend(open, now + 1).err()).isEqualTo(RunError.RUN_NOT_LOCKED)
        val sched = RunStateMachine.startScheduled(office, now, now + 60 * MINUTE_MS)
        assertThat(RunStateMachine.extend(sched, now + 90 * MINUTE_MS).err()).isEqualTo(RunError.CANNOT_EXTEND_SCHEDULED)
    }

    @Test fun `free ends a locked run`() {
        val sched = RunStateMachine.startScheduled(office, now, now + 60 * MINUTE_MS)
        assertThat(sched.locked).isTrue()
        assertThat(RunStateMachine.free(sched, now + 1).ok().endReason).isEqualTo(EndReason.FREED)
    }

    @Test fun `emergency uses are counted per run and shared across apps`() {
        var run = RunStateMachine.startManual(study, null, now).ok().copy(id = 1)
        val (r1, u1) = RunStateMachine.useEmergency(study, run, AppUsage(1, "ig"), now).ok()
        assertThat(u1.emergencyUntil).isEqualTo(now + 5 * MINUTE_MS)
        val (r2, _) = RunStateMachine.useEmergency(study, r1, AppUsage(1, "yt"), now).ok()
        assertThat(RunStateMachine.emergencyUsesLeft(study, r2)).isEqualTo(0)
        assertThat(RunStateMachine.useEmergency(study, r2, AppUsage(1, "x"), now).err()).isEqualTo(RunError.NO_EMERGENCY_LEFT)
        // A new run starts with a fresh count.
        run = RunStateMachine.startManual(study, null, now + 1).ok()
        assertThat(RunStateMachine.emergencyUsesLeft(study, run)).isEqualTo(2)
    }

    @Test fun `emergency is refused when disabled or during a break`() {
        val run = RunStateMachine.startManual(study, null, now).ok()
        assertThat(RunStateMachine.useEmergency(study.copy(emergencyEnabled = false), run, AppUsage(1, "x"), now).err())
            .isEqualTo(RunError.EMERGENCY_DISABLED)
        assertThat(RunStateMachine.useEmergency(study, run, AppUsage(1, "x", breakUntil = now + 1), now).err())
            .isEqualTo(RunError.EMERGENCY_NOT_DURING_BREAK)
    }
}
