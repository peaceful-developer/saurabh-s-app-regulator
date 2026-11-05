package com.saurabh.sar.core.engine

import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.MINUTE_MS

/**
 * Turns foreground intervals (app came to front → app left front / screen off) into used time.
 * Nothing ticks: usage is computed from the two timestamps (FSD 4.4, 11.3).
 */
object UsageLedger {
    /** A gap of this long or more resets the continuous stretch (FSD 4.3). */
    const val STRETCH_GAP_MS = 2 * MINUTE_MS

    /**
     * Adds the foreground interval [from]..[to] to [usage]. A zero-length interval is a
     * "snapshot": it only applies the stretch-gap reset, which is what the engine needs
     * the moment an app comes to the front.
     */
    fun accrue(usage: AppUsage, rule: AppRule?, from: Long, to: Long): AppUsage {
        val duration = (to - from).coerceAtLeast(0)
        val last = usage.lastForegroundEndAt
        var stretch = if (last != null && from - last >= STRETCH_GAP_MS) 0L else usage.stretchUsedMs
        var breakUntil = usage.breakUntil

        if (rule != null && rule.hasSessionCap && duration > 0) {
            val capMs = rule.capMin!! * MINUTE_MS
            val breakMs = rule.breakMin!! * MINUTE_MS
            if (stretch + duration >= capMs) {
                val hitAt = from + (capMs - stretch).coerceAtLeast(0)
                breakUntil = hitAt + breakMs
                // Time after the cap was hit (if the block screen was late) starts no new stretch.
                stretch = 0
            } else {
                stretch += duration
            }
        } else {
            stretch += duration
        }

        return usage.copy(
            usedMs = usage.usedMs + duration,
            stretchUsedMs = stretch,
            lastForegroundEndAt = if (duration > 0) to else (last ?: to),
            breakUntil = breakUntil,
        )
    }
}
