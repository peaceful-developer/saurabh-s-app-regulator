package com.saurabh.sar.core.history

import com.google.common.truth.Truth.assertThat
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.MINUTE_MS
import com.saurabh.sar.core.model.Run
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HistoryCalculatorTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 11, 5)
    private fun at(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000

    @Test fun `totals per day and top apps`() {
        val runs = listOf(
            Run(id = 1, modeId = 1, startedAt = at(today), locked = false),
            Run(id = 2, modeId = 2, startedAt = at(today.minusDays(2)), locked = false),
            Run(id = 3, modeId = 2, startedAt = at(today.minusDays(9)), locked = false),
        )
        val usage = listOf(
            AppUsage(1, "yt", usedMs = 10 * MINUTE_MS),
            AppUsage(1, "ig", usedMs = 5 * MINUTE_MS),
            AppUsage(2, "yt", usedMs = 20 * MINUTE_MS),
            AppUsage(3, "yt", usedMs = 40 * MINUTE_MS),
        )
        val week = HistoryCalculator.summarise(runs, usage, today, zone, 7)
        assertThat(week.days).hasSize(7)
        assertThat(week.days.last().totalMs).isEqualTo(15 * MINUTE_MS)
        assertThat(week.totalMs).isEqualTo(35 * MINUTE_MS)
        assertThat(week.previousTotalMs).isEqualTo(40 * MINUTE_MS)
        assertThat(week.topApps.first().packageName).isEqualTo("yt")
        val day = HistoryCalculator.summarise(runs, usage, today, zone, 1)
        assertThat(day.totalMs).isEqualTo(15 * MINUTE_MS)
        assertThat(day.runs.map { it.id }).containsExactly(1L)
    }
}
