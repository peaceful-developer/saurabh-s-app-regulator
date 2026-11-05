package com.saurabh.sar.core.history

import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.Run
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayTotal(val date: LocalDate, val totalMs: Long)
data class AppTotal(val packageName: String, val totalMs: Long)
data class ModeTotal(val modeId: Long, val totalMs: Long)

data class HistorySummary(
    val days: List<DayTotal>,
    val totalMs: Long,
    val previousTotalMs: Long,
    val topApps: List<AppTotal>,
    val byMode: List<ModeTotal>,
    val runs: List<Run>,
)

/** Minutes per controlled app, per mode, for Today and the last 7 days (FSD 8.3). */
object HistoryCalculator {
    /**
     * @param days 1 for Today, 7 for the 7-day view. Usage counts on the day its run started.
     */
    fun summarise(
        runs: List<Run>,
        usage: List<AppUsage>,
        today: LocalDate,
        zone: ZoneId,
        days: Int,
    ): HistorySummary {
        fun dateOf(r: Run) = Instant.ofEpochMilli(r.startedAt).atZone(zone).toLocalDate()
        val firstDay = today.minusDays(days - 1L)
        val prevFirst = firstDay.minusDays(days.toLong())
        val byRun = usage.groupBy { it.runId }
        val inRange = runs.filter { dateOf(it) in firstDay..today }
        val prev = runs.filter { dateOf(it) in prevFirst..firstDay.minusDays(1) }
        fun total(rs: List<Run>) = rs.sumOf { r -> byRun[r.id].orEmpty().sumOf { it.usedMs } }

        val dayTotals = (0 until days).map { i ->
            val d = firstDay.plusDays(i.toLong())
            DayTotal(d, total(inRange.filter { dateOf(it) == d }))
        }
        val rangeUsage = inRange.flatMap { byRun[it.id].orEmpty() }
        val topApps = rangeUsage.groupBy { it.packageName }
            .map { (p, l) -> AppTotal(p, l.sumOf { it.usedMs }) }
            .filter { it.totalMs > 0 }
            .sortedByDescending { it.totalMs }
        val byMode = inRange.groupBy { it.modeId }
            .map { (m, rs) -> ModeTotal(m, total(rs)) }
            .sortedByDescending { it.totalMs }
        return HistorySummary(
            days = dayTotals,
            totalMs = dayTotals.sumOf { it.totalMs },
            previousTotalMs = total(prev),
            topApps = topApps,
            byMode = byMode,
            runs = inRange.sortedByDescending { it.startedAt },
        )
    }
}
