package com.saurabh.sar.core.schedule

import com.google.common.truth.Truth.assertThat
import com.saurabh.sar.core.model.ScheduleEntry
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleCalculatorTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun t(s: String) = LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()
    private val office = listOf(ScheduleEntry(daysMask = ScheduleEntry.MON_FRI, startMinute = 9 * 60, endMinute = 18 * 60))
    // 2026-11-02 is a Monday.

    @Test fun `office hours are active on a weekday`() {
        val w = ScheduleCalculator.activeWindow(office, t("2026-11-02T10:00"), zone)
        assertThat(w).isEqualTo(Window(t("2026-11-02T09:00"), t("2026-11-02T18:00")))
        assertThat(ScheduleCalculator.activeWindow(office, t("2026-11-02T18:00"), zone)).isNull()
        assertThat(ScheduleCalculator.activeWindow(office, t("2026-11-07T10:00"), zone)).isNull()
    }

    @Test fun `next start skips the weekend`() {
        assertThat(ScheduleCalculator.nextStart(office, t("2026-11-06T18:30"), zone)).isEqualTo(t("2026-11-09T09:00"))
    }

    @Test fun `overnight window runs across midnight`() {
        val sleep = listOf(ScheduleEntry(daysMask = 1 shl 0, startMinute = 23 * 60, endMinute = 7 * 60)) // Mon night
        assertThat(ScheduleCalculator.activeWindow(sleep, t("2026-11-03T02:00"), zone))
            .isEqualTo(Window(t("2026-11-02T23:00"), t("2026-11-03T07:00")))
        assertThat(ScheduleCalculator.activeWindow(sleep, t("2026-11-03T08:00"), zone)).isNull()
    }

    @Test fun `overnight window runs from Sunday into Monday`() {
        val sun = listOf(ScheduleEntry(daysMask = 1 shl 6, startMinute = 22 * 60, endMinute = 6 * 60))
        assertThat(ScheduleCalculator.activeWindow(sun, t("2026-11-09T05:00"), zone))
            .isEqualTo(Window(t("2026-11-08T22:00"), t("2026-11-09T06:00")))
    }

    @Test fun `start equal to end is rejected`() {
        assertThat(ScheduleCalculator.validate(ScheduleEntry(daysMask = 1, startMinute = 600, endMinute = 600)))
            .isEqualTo(ScheduleProblem.START_EQUALS_END)
        assertThat(ScheduleCalculator.validate(ScheduleEntry(daysMask = 0, startMinute = 600, endMinute = 700)))
            .isEqualTo(ScheduleProblem.NO_DAYS)
    }

    @Test fun `overlap detection`() {
        val lunch = listOf(ScheduleEntry(daysMask = 1 shl 2, startMinute = 12 * 60, endMinute = 13 * 60))
        val weekend = listOf(ScheduleEntry(daysMask = ScheduleEntry.SAT_SUN, startMinute = 0, endMinute = 23 * 60 + 59))
        assertThat(ScheduleCalculator.overlaps(office, lunch)).isTrue()
        assertThat(ScheduleCalculator.overlaps(office, weekend)).isFalse()
        val sunNight = listOf(ScheduleEntry(daysMask = 1 shl 6, startMinute = 23 * 60, endMinute = 10 * 60))
        assertThat(ScheduleCalculator.overlaps(office, sunNight)).isTrue()
    }

    @Test fun `daylight saving day still gives a correct window`() {
        val berlin = ZoneId.of("Europe/Berlin")
        val night = listOf(ScheduleEntry(daysMask = 1 shl 5, startMinute = 0, endMinute = 6 * 60)) // Saturday
        // 2026-03-28 is a Saturday; DST starts on Sunday 29 March, so Saturday is normal.
        val w = ScheduleCalculator.activeWindow(night, LocalDateTime.parse("2026-03-28T01:00").atZone(berlin).toInstant().toEpochMilli(), berlin)
        assertThat(w).isNotNull()
        assertThat(w!!.end - w.start).isEqualTo(6 * 3_600_000L)
    }
}
