package com.saurabh.sar.core.engine

import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.MINUTE_MS
import com.saurabh.sar.core.model.RuleType

/** Decides allow / block / break for one app (FSD 4, 6.1). Pure and stateless. */
object RulesEngine {
    /**
     * @param rule the app's rule in the running mode, or null if the mode does not control it.
     * @param usage the app's usage in this run, already including any time it has been open
     *   right up to [now] (see [UsageLedger.accrue]).
     * @param warningMinutes "minutes left" warnings, e.g. [5, 1] (FSD 8.2).
     */
    fun decide(
        rule: AppRule?,
        usage: AppUsage,
        now: Long,
        warningMinutes: List<Int> = listOf(5, 1),
    ): Decision {
        if (rule == null) return Decision.Allow()

        val breakUntil = usage.breakUntil
        if (rule.type == RuleType.LIMIT && rule.hasSessionCap && breakUntil != null && now < breakUntil) {
            return Decision.Block(BlockReason.SessionBreak(breakUntil))
        }

        val emergencyUntil = usage.emergencyUntil
        if (emergencyUntil != null && now < emergencyUntil) {
            return Decision.Allow(limitAt = emergencyUntil)
        }

        if (rule.type == RuleType.BLOCK) return Decision.Block(BlockReason.RuleBlock)

        val allowanceMs = rule.allowanceMs
        val remaining = allowanceMs - usage.usedMs
        if (remaining <= 0) return Decision.Block(BlockReason.AllowanceUsed(usage.usedMs, allowanceMs))

        var untilLimit = remaining
        if (rule.hasSessionCap) {
            val capLeft = (rule.capMin!! * MINUTE_MS - usage.stretchUsedMs).coerceAtLeast(0)
            untilLimit = minOf(untilLimit, capLeft)
        }
        val limitAt = now + untilLimit

        var warnAt: Long? = null
        var warnMinutes: Int? = null
        for (w in warningMinutes.distinct().sortedDescending()) {
            val threshold = w * MINUTE_MS
            if (remaining > threshold) {
                val at = now + remaining - threshold
                if (at < limitAt) {
                    warnAt = at
                    warnMinutes = w
                }
                break
            }
        }
        return Decision.Allow(limitAt = limitAt, warnAt = warnAt, warnMinutes = warnMinutes)
    }
}
