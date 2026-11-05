package com.saurabh.sar.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormatTest {
    @Test fun `hours and minutes round up`() {
        assertThat(formatHm(84 * 60_000L)).isEqualTo("1:24")
        assertThat(formatHm(30_000L)).isEqualTo("0:01")
        assertThat(formatHm(0)).isEqualTo("0:00")
    }

    @Test fun `duration chips`() {
        assertThat(formatDurationLabel(30 * 60_000L)).isEqualTo("30 MIN")
        assertThat(formatDurationLabel(60 * 60_000L)).isEqualTo("1 HR")
        assertThat(formatDurationLabel(90 * 60_000L)).isEqualTo("1.5 HR")
        assertThat(formatDurationLabel(75 * 60_000L)).isEqualTo("1H 15M")
    }

    @Test fun `durations`() {
        assertThat(formatDuration(190 * 60_000L)).isEqualTo("3h 10m")
        assertThat(formatDuration(45 * 60_000L)).isEqualTo("45m")
        assertThat(formatMinuteOfDay(9 * 60 + 5)).isEqualTo("09:05")
    }
}
