package com.saurabh.sar.core.engine

import com.google.common.truth.Truth.assertThat
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.MINUTE_MS
import com.saurabh.sar.core.model.RuleType
import org.junit.Test

class UsageLedgerTest {
    private val limit = AppRule(packageName = "yt", type = RuleType.LIMIT, allowanceMin = 30)
    private val capped = limit.copy(capMin = 15, breakMin = 10)
    private val empty = AppUsage(runId = 1, packageName = "yt")

    @Test fun `foreground time is added to used time`() {
        val u = UsageLedger.accrue(empty, limit, from = 0, to = 5 * MINUTE_MS)
        assertThat(u.usedMs).isEqualTo(5 * MINUTE_MS)
        assertThat(u.stretchUsedMs).isEqualTo(5 * MINUTE_MS)
        assertThat(u.lastForegroundEndAt).isEqualTo(5 * MINUTE_MS)
    }

    @Test fun `a gap under 2 minutes continues the stretch`() {
        val a = UsageLedger.accrue(empty, capped, 0, 5 * MINUTE_MS)
        val b = UsageLedger.accrue(a, capped, 6 * MINUTE_MS, 8 * MINUTE_MS)
        assertThat(b.stretchUsedMs).isEqualTo(7 * MINUTE_MS)
        assertThat(b.usedMs).isEqualTo(7 * MINUTE_MS)
    }

    @Test fun `a gap of 2 minutes or more resets the stretch but not the allowance`() {
        val a = UsageLedger.accrue(empty, capped, 0, 5 * MINUTE_MS)
        val b = UsageLedger.accrue(a, capped, 7 * MINUTE_MS, 8 * MINUTE_MS)
        assertThat(b.stretchUsedMs).isEqualTo(1 * MINUTE_MS)
        assertThat(b.usedMs).isEqualTo(6 * MINUTE_MS)
    }

    @Test fun `hitting the session cap starts a break from the moment the cap was reached`() {
        val u = UsageLedger.accrue(empty, capped, 0, 15 * MINUTE_MS)
        assertThat(u.breakUntil).isEqualTo(25 * MINUTE_MS)
        assertThat(u.stretchUsedMs).isEqualTo(0)
        assertThat(u.usedMs).isEqualTo(15 * MINUTE_MS)
    }

    @Test fun `zero length interval only refreshes the stretch gap`() {
        val a = UsageLedger.accrue(empty, capped, 0, 5 * MINUTE_MS)
        val snap = UsageLedger.accrue(a, capped, 10 * MINUTE_MS, 10 * MINUTE_MS)
        assertThat(snap.stretchUsedMs).isEqualTo(0)
        assertThat(snap.usedMs).isEqualTo(5 * MINUTE_MS)
    }

    @Test fun `no session cap means no break`() {
        val u = UsageLedger.accrue(empty, limit, 0, 60 * MINUTE_MS)
        assertThat(u.breakUntil).isNull()
    }
}
