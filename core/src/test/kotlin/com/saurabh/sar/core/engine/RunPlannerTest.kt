package com.saurabh.sar.core.engine

import com.google.common.truth.Truth.assertThat
import com.saurabh.sar.core.model.EndReason
import com.saurabh.sar.core.model.MINUTE_MS
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.Run
import com.saurabh.sar.core.model.ScheduleEntry
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RunPlannerTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun t(s: String) = LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()
    private val study = Mode(id = 1, name = "Self Study", type = ModeType.MANUAL, message = "m")
    private val office = Mode(id = 2, name = "Office Work", type = ModeType.SCHEDULED, message = "m", sortOrder = 3)
    private val lunch = Mode(id = 3, name = "Lunch", type = ModeType.SCHEDULED, message = "m", sortOrder = 4)
    private val modes = listOf(study, office, lunch)
    private val schedules = mapOf(
        2L to listOf(ScheduleEntry(daysMask = ScheduleEntry.MON_FRI, startMinute = 9 * 60, endMinute = 18 * 60)),
        3L to listOf(ScheduleEntry(daysMask = ScheduleEntry.MON_FRI, startMinute = 12 * 60, endMinute = 13 * 60)),
    )

    private fun plan(now: Long, current: Run?, last: Map<Long, Long> = emptyMap(), boot: Boolean = false) =
        RunPlanner.plan(now, zone, current, modes, schedules, last, boot)

    @Test fun `scheduled mode starts on its own at its start time`() {
        val p = plan(t("2026-11-02T09:00"), null)
        assertThat(p.startScheduled?.modeId).isEqualTo(2)
        assertThat(p.startScheduled?.window?.end).isEqualTo(t("2026-11-02T18:00"))
        assertThat(p.nextWakeAt).isEqualTo(t("2026-11-02T12:00"))
    }

    @Test fun `next wake is the next schedule start when nothing is running`() {
        val p = plan(t("2026-11-02T07:00"), null)
        assertThat(p.startScheduled).isNull()
        assertThat(p.nextWakeAt).isEqualTo(t("2026-11-02T09:00"))
    }

    @Test fun `timed run ends at its end time`() {
        val run = Run(id = 5, modeId = 1, startedAt = t("2026-11-02T06:00"), plannedEndAt = t("2026-11-02T07:00"), locked = true)
        val p = plan(t("2026-11-02T07:00"), run, mapOf(1L to run.startedAt))
        assertThat(p.endCurrent).isEqualTo(EndReason.TIME_UP)
    }

    @Test fun `collision - scheduled mode waits and is offered while another runs`() {
        val run = Run(id = 5, modeId = 1, startedAt = t("2026-11-02T08:30"), locked = false)
        val p = plan(t("2026-11-02T09:00"), run, mapOf(1L to run.startedAt))
        assertThat(p.endCurrent).isNull()
        assertThat(p.startScheduled).isNull()
        assertThat(p.pending.map { it.modeId }).containsExactly(2L)
    }

    @Test fun `collision - scheduled mode starts when the current run ends if window is still open`() {
        val run = Run(id = 5, modeId = 1, startedAt = t("2026-11-02T08:30"), plannedEndAt = t("2026-11-02T10:00"), locked = true)
        val p = plan(t("2026-11-02T10:00"), run, mapOf(1L to run.startedAt))
        assertThat(p.endCurrent).isEqualTo(EndReason.TIME_UP)
        assertThat(p.startScheduled?.modeId).isEqualTo(2)
    }

    @Test fun `collision - nothing starts if the window has closed`() {
        val p = plan(t("2026-11-02T19:00"), null)
        assertThat(p.startScheduled).isNull()
    }

    @Test fun `freed scheduled mode waits for its next window`() {
        val p = plan(t("2026-11-02T11:00"), null, mapOf(2L to t("2026-11-02T09:00")))
        assertThat(p.startScheduled).isNull()
        val next = plan(t("2026-11-03T09:00"), null, mapOf(2L to t("2026-11-02T09:00")))
        assertThat(next.startScheduled?.modeId).isEqualTo(2)
    }

    @Test fun `two scheduled modes - the one that started first keeps running`() {
        val officeRun = Run(id = 7, modeId = 2, startedAt = t("2026-11-02T09:00"), plannedEndAt = t("2026-11-02T18:00"), locked = true, scheduled = true)
        val p = plan(t("2026-11-02T12:00"), officeRun, mapOf(2L to officeRun.startedAt))
        assertThat(p.endCurrent).isNull()
        assertThat(p.pending.map { it.modeId }).containsExactly(3L)
        assertThat(p.nextWakeAt).isEqualTo(t("2026-11-02T13:00"))
    }

    @Test fun `boot - run that ended while the phone was off is closed as expired`() {
        val run = Run(id = 5, modeId = 1, startedAt = t("2026-11-02T06:00"), plannedEndAt = t("2026-11-02T07:00"), locked = true)
        val p = plan(t("2026-11-02T08:00"), run, mapOf(1L to run.startedAt), boot = true)
        assertThat(p.endCurrent).isEqualTo(EndReason.EXPIRED_WHILE_OFF)
    }

    @Test fun `boot - run still in time resumes`() {
        val run = Run(id = 5, modeId = 2, startedAt = t("2026-11-02T09:00"), plannedEndAt = t("2026-11-02T18:00"), locked = true, scheduled = true)
        val p = plan(t("2026-11-02T10:00"), run, mapOf(2L to run.startedAt), boot = true)
        assertThat(p.endCurrent).isNull()
        assertThat(p.startScheduled).isNull()
    }

    @Test fun `boot - phone off when schedule started starts it for the remaining time`() {
        val p = plan(t("2026-11-02T10:30"), null, boot = true)
        assertThat(p.startScheduled?.modeId).isEqualTo(2)
        assertThat(p.startScheduled?.window?.end).isEqualTo(t("2026-11-02T18:00"))
    }

    @Test fun `run of a deleted mode ends`() {
        val run = Run(id = 5, modeId = 99, startedAt = 0, locked = false)
        assertThat(plan(t("2026-11-02T07:00"), run).endCurrent).isEqualTo(EndReason.STOPPED)
    }
}
