package com.saurabh.sar.core.engine

/** What SAR should do with an app that is in front right now. */
sealed interface Decision {
    /**
     * Let the app run. [limitAt] is the single moment to check again (allowance or session cap
     * runs out, or an emergency window ends); null means nothing will change while it is open.
     * [warnAt] is an earlier moment to post a "minutes left" warning, with [warnMinutes] left.
     */
    data class Allow(
        val limitAt: Long? = null,
        val warnAt: Long? = null,
        val warnMinutes: Int? = null,
    ) : Decision {
        /** The one callback the platform should schedule. */
        val nextCheckAt: Long? get() = listOfNotNull(limitAt, warnAt).minOrNull()
    }

    data class Block(val reason: BlockReason) : Decision
}

sealed interface BlockReason {
    /** Complete block rule. "Blocked in <Mode>". */
    data object RuleBlock : BlockReason

    /** Allowance used up. "30 of 30 min used". */
    data class AllowanceUsed(val usedMs: Long, val allowanceMs: Long) : BlockReason

    /** Session-cap break. "Break: 7 min left". */
    data class SessionBreak(val until: Long) : BlockReason
}
