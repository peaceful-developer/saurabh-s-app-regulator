package com.saurabh.sar.core.engine

import com.saurabh.sar.core.model.EndReason
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.Run
import com.saurabh.sar.core.model.ScheduleEntry
import com.saurabh.sar.core.schedule.ScheduleCalculator
import com.saurabh.sar.core.schedule.Window
import java.time.ZoneId

/** A scheduled mode whose window is open but which is waiting behind the current run (FSD 5.4). */
data class PendingScheduled(val modeId: Long, val window: Window)

data class Plan(
    /** End the current run with this reason (time up, or expired while the phone was off). */
    val endCurrent: EndReason? = null,
    /** Start this scheduled mode now, locked until the end of [window]. */
    val startScheduled: PendingScheduled? = null,
    /** Due scheduled modes the user may switch to: show the collision banner/notification. */
    val pending: List<PendingScheduled> = emptyList(),
    /** The one exact alarm to set: the next run boundary. Null = nothing to wake up for. */
    val nextWakeAt: Long? = null,
)

/**
 * Decides what should be running right now. Called on every boundary alarm, at boot, after a
 * time or zone change, and after any run or mode change. Pure: the caller carries out the plan.
 *
 * Rules (FSD 5.2–5.5, 10):
 * - A run whose fixed end time has passed ends (TIME_UP, or EXPIRED_WHILE_OFF after a reboot).
 * - A scheduled mode is "due" when one of its windows is open and it has not run yet in that
 *   window. A freed or switched-away mode therefore waits for its next window.
 * - With nothing running, the due mode whose window opened first starts. Otherwise due modes
 *   are offered to the user and start on their own when the current run ends, if still open.
 */
object RunPlanner {
    /** A run end more than this far in the past at boot counts as "expired while off". */
    const val BOOT_GRACE_MS = 60_000L

    fun plan(
        now: Long,
        zone: ZoneId,
        current: Run?,
        modes: List<Mode>,
        schedules: Map<Long, List<ScheduleEntry>>,
        /** Latest run start per mode id, including the current run. */
        lastRunStart: Map<Long, Long>,
        isBoot: Boolean = false,
    ): Plan {
        var running = current?.takeIf { it.isActive }
        var endCurrent: EndReason? = null
        val end = running?.plannedEndAt
        if (running != null && end != null && now >= end) {
            endCurrent = if (isBoot && now - end > BOOT_GRACE_MS) EndReason.EXPIRED_WHILE_OFF else EndReason.TIME_UP
            running = null
        }
        // A deleted mode cannot keep running.
        if (running != null && modes.none { it.id == running!!.modeId }) {
            endCurrent = EndReason.STOPPED
            running = null
        }

        val scheduled = modes.filter { it.type == ModeType.SCHEDULED }
        val due = scheduled.mapNotNull { m ->
            if (m.id == running?.modeId) return@mapNotNull null
            val w = ScheduleCalculator.activeWindow(schedules[m.id].orEmpty(), now, zone) ?: return@mapNotNull null
            val last = lastRunStart[m.id]
            if (last != null && last >= w.start) null else m to w
        }.sortedWith(compareBy({ it.second.start }, { it.first.sortOrder }))

        var start: PendingScheduled? = null
        val pending: List<PendingScheduled>
        if (running == null && due.isNotEmpty()) {
            val (m, w) = due.first()
            start = PendingScheduled(m.id, w)
            pending = due.drop(1).map { PendingScheduled(it.first.id, it.second) }
        } else {
            pending = due.map { PendingScheduled(it.first.id, it.second) }
        }

        val wakes = buildList {
            running?.plannedEndAt?.let(::add)
            start?.window?.end?.let(::add)
            pending.forEach { add(it.window.end) }
            scheduled.forEach { m -> ScheduleCalculator.nextStart(schedules[m.id].orEmpty(), now, zone)?.let(::add) }
        }.filter { it > now }

        return Plan(endCurrent = endCurrent, startScheduled = start, pending = pending, nextWakeAt = wakes.minOrNull())
    }
}
