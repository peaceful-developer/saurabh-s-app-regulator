package com.saurabh.sar.platform

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.saurabh.sar.core.Clock

/**
 * Cross-checks SAR's own count against Android's usage stats. Queried only after a reboot,
 * when History or Home opens, or when blocking was off — never on a loop (FSD 11.2, 11.3).
 */
class UsageReconciler(private val context: Context, private val controller: RunController, private val clock: Clock) {
    suspend fun reconcile() {
        if (!Permissions.isGranted(context, SarPermission.USAGE_ACCESS)) return
        val state = controller.active.value ?: return
        val from = state.run.startedAt
        val to = clock.now()
        val totals = runCatching { foregroundTotals(from, to, state.rules.keys) }
            .onFailure { Log.w(TAG, "usage stats", it) }.getOrNull() ?: return
        controller.mergeUsageStats(totals)
    }

    private fun foregroundTotals(from: Long, to: Long, pkgs: Set<String>): Map<String, Long> {
        val usm = context.getSystemService(UsageStatsManager::class.java)
        val events = usm.queryEvents(from, to)
        val started = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val pkg = e.packageName ?: continue
            if (pkg !in pkgs) continue
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> started.putIfAbsent(pkg, e.timeStamp)
                UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> started.remove(pkg)?.let {
                    total[pkg] = (total[pkg] ?: 0) + (e.timeStamp - it)
                }
            }
        }
        started.forEach { (pkg, s) -> total[pkg] = (total[pkg] ?: 0) + (to - s) }
        return total
    }
}
