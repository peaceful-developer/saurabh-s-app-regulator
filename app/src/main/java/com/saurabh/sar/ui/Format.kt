package com.saurabh.sar.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val clockFmt = DateTimeFormatter.ofPattern("HH:mm")

fun formatClock(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    clockFmt.format(Instant.ofEpochMilli(epochMs).atZone(zone))

fun minutesOf(ms: Long): Long = ms / 60_000

/** "1:24" for hours:minutes, rounded up so a running clock never shows 0:00 early. */
fun formatHm(ms: Long): String {
    val totalMin = ((ms.coerceAtLeast(0) + 59_999) / 60_000)
    return "${totalMin / 60}:${(totalMin % 60).toString().padStart(2, '0')}"
}

/** "3h 10m" / "45m". */
fun formatDuration(ms: Long): String {
    val m = ms.coerceAtLeast(0) / 60_000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "${m}m"
}

fun formatMinuteOfDay(min: Int): String = "${(min / 60).toString().padStart(2, '0')}:${(min % 60).toString().padStart(2, '0')}"

fun formatDurationLabel(ms: Long): String {
    val m = ms / 60_000
    return when {
        m < 60 -> "$m MIN"
        m % 60 == 0L -> "${m / 60} HR"
        m % 60 == 30L -> "${m / 60}.5 HR"
        else -> "${m / 60}H ${m % 60}M"
    }
}
