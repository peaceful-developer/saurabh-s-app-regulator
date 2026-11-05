package com.saurabh.sar.core.schedule

import com.saurabh.sar.core.model.ScheduleEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** One concrete occurrence of a schedule entry, in epoch millis. [end] is exclusive. */
data class Window(val start: Long, val end: Long) {
    operator fun contains(t: Long) = t in start until end
    fun overlaps(o: Window) = start < o.end && o.start < end
}

enum class ScheduleProblem { NO_DAYS, START_EQUALS_END, OUT_OF_RANGE }

/**
 * Schedule maths (FSD 3.1, 5.2). Uses java.time, so it follows the phone's time zone and
 * daylight-saving changes; nothing here reads the clock itself.
 */
object ScheduleCalculator {

    fun validate(entry: ScheduleEntry): ScheduleProblem? = when {
        entry.daysMask and ScheduleEntry.EVERY_DAY == 0 -> ScheduleProblem.NO_DAYS
        entry.startMinute !in 0 until 1440 || entry.endMinute !in 0 until 1440 -> ScheduleProblem.OUT_OF_RANGE
        entry.startMinute == entry.endMinute -> ScheduleProblem.START_EQUALS_END
        else -> null
    }

    /** All windows of [entries] that start on dates from [fromDate] to [toDate] inclusive. */
    fun windows(entries: List<ScheduleEntry>, fromDate: LocalDate, toDate: LocalDate, zone: ZoneId): List<Window> {
        val out = ArrayList<Window>()
        var d = fromDate
        while (!d.isAfter(toDate)) {
            val dayIdx = d.dayOfWeek.value - 1 // Mon = 0
            for (e in entries) {
                if (validate(e) != null || !e.hasDay(dayIdx)) continue
                val start = d.atTime(LocalTime.of(e.startMinute / 60, e.startMinute % 60)).atZone(zone)
                val endDate = if (e.isOvernight) d.plusDays(1) else d
                val end = endDate.atTime(LocalTime.of(e.endMinute / 60, e.endMinute % 60)).atZone(zone)
                out += Window(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
            }
            d = d.plusDays(1)
        }
        return out.sortedBy { it.start }
    }

    private fun dateOf(t: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(t).atZone(zone).toLocalDate()

    /** The window containing [at], if any. If several do, the one that started first. */
    fun activeWindow(entries: List<ScheduleEntry>, at: Long, zone: ZoneId): Window? {
        val day = dateOf(at, zone)
        return windows(entries, day.minusDays(1), day, zone).filter { at in it }.minByOrNull { it.start }
    }

    /** The first window start strictly after [after], looking up to 8 days ahead. */
    fun nextStart(entries: List<ScheduleEntry>, after: Long, zone: ZoneId): Long? {
        val day = dateOf(after, zone)
        return windows(entries, day, day.plusDays(8), zone).firstOrNull { it.start > after }?.start
    }

    /** The next window (active or future) relative to [at], for "Up next". */
    fun nextWindow(entries: List<ScheduleEntry>, at: Long, zone: ZoneId): Window? {
        val day = dateOf(at, zone)
        return windows(entries, day.minusDays(1), day.plusDays(8), zone).firstOrNull { it.end > at }
    }

    /**
     * True if any window of [a] overlaps any window of [b] in a normal week, including an
     * overnight window that runs from Sunday into Monday. Used by the editor warning (FSD 5.4).
     */
    fun overlaps(a: List<ScheduleEntry>, b: List<ScheduleEntry>): Boolean {
        // 2024-01-01 is a Monday; UTC has no DST, so this is a clean reference week.
        val zone = ZoneOffset.UTC
        val from = LocalDate.of(2023, 12, 31)
        val to = LocalDate.of(2024, 1, 8)
        val wa = windows(a, from, to, zone)
        val wb = windows(b, from, to, zone)
        return wa.any { x -> wb.any { y -> x.overlaps(y) } }
    }
}
