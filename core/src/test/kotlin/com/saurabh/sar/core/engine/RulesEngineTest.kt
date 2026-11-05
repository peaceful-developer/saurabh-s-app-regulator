package com.saurabh.sar.core.engine

import com.google.common.truth.Truth.assertThat
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.AppUsage
import com.saurabh.sar.core.model.MINUTE_MS
import com.saurabh.sar.core.model.RuleType
import org.junit.Test

class RulesEngineTest {
    private val now = 1_000 * MINUTE_MS
    private val block = AppRule(packageName = "ig", type = RuleType.BLOCK)
    private val limit30 = AppRule(packageName = "yt", type = RuleType.LIMIT, allowanceMin = 30)
    private fun used(min: Long, pkg: String = "yt") = AppUsage(1, pkg, usedMs = min * MINUTE_MS)

    @Test fun `complete block always blocks`() {
        assertThat(RulesEngine.decide(block, AppUsage(1, "ig"), now))
            .isEqualTo(Decision.Block(BlockReason.RuleBlock))
    }

    @Test fun `allowance left allows and schedules one check when it runs out`() {
        val d = RulesEngine.decide(limit30, used(10), now, warningMinutes = emptyList()) as Decision.Allow
        assertThat(d.limitAt).isEqualTo(now + 20 * MINUTE_MS)
        assertThat(d.nextCheckAt).isEqualTo(now + 20 * MINUTE_MS)
    }

    @Test fun `allowance used up blocks`() {
        val d = RulesEngine.decide(limit30, used(30), now)
        assertThat(d).isEqualTo(Decision.Block(BlockReason.AllowanceUsed(30 * MINUTE_MS, 30 * MINUTE_MS)))
    }

    @Test fun `allowance edited below minutes used blocks immediately`() {
        val edited = limit30.copy(allowanceMin = 10)
        assertThat(RulesEngine.decide(edited, used(18), now)).isInstanceOf(Decision.Block::class.java)
    }

    @Test fun `allowance edited above minutes used unblocks`() {
        val edited = limit30.copy(allowanceMin = 40)
        assertThat(RulesEngine.decide(edited, used(30), now)).isInstanceOf(Decision.Allow::class.java)
    }

    @Test fun `warnings fire at 5 then 1 minute left`() {
        val d = RulesEngine.decide(limit30, used(10), now, listOf(5, 1)) as Decision.Allow
        assertThat(d.warnAt).isEqualTo(now + 15 * MINUTE_MS)
        assertThat(d.warnMinutes).isEqualTo(5)
        assertThat(d.nextCheckAt).isEqualTo(now + 15 * MINUTE_MS)

        val later = RulesEngine.decide(limit30, used(25), now, listOf(5, 1)) as Decision.Allow
        assertThat(later.warnMinutes).isEqualTo(1)
        assertThat(later.warnAt).isEqualTo(now + 4 * MINUTE_MS)
    }

    @Test fun `no warning when less than a minute is left`() {
        val usage = AppUsage(1, "yt", usedMs = 30 * MINUTE_MS - 30_000)
        val d = RulesEngine.decide(limit30, usage, now, listOf(5, 1)) as Decision.Allow
        assertThat(d.warnAt).isNull()
        assertThat(d.limitAt).isEqualTo(now + 30_000)
    }

    @Test fun `session cap brings the check forward`() {
        val capped = limit30.copy(capMin = 15, breakMin = 10)
        val usage = AppUsage(1, "yt", usedMs = 10 * MINUTE_MS, stretchUsedMs = 10 * MINUTE_MS)
        val d = RulesEngine.decide(capped, usage, now, emptyList()) as Decision.Allow
        assertThat(d.limitAt).isEqualTo(now + 5 * MINUTE_MS)
    }

    @Test fun `during a session break the app is blocked with a countdown`() {
        val capped = limit30.copy(capMin = 15, breakMin = 10)
        val usage = AppUsage(1, "yt", usedMs = 15 * MINUTE_MS, breakUntil = now + 7 * MINUTE_MS)
        assertThat(RulesEngine.decide(capped, usage, now))
            .isEqualTo(Decision.Block(BlockReason.SessionBreak(now + 7 * MINUTE_MS)))
    }

    @Test fun `after the break the app opens again if allowance remains`() {
        val capped = limit30.copy(capMin = 15, breakMin = 10)
        val usage = AppUsage(1, "yt", usedMs = 15 * MINUTE_MS, breakUntil = now - 1)
        assertThat(RulesEngine.decide(capped, usage, now)).isInstanceOf(Decision.Allow::class.java)
    }

    @Test fun `session cap never adds time beyond the allowance`() {
        val capped = limit30.copy(capMin = 15, breakMin = 10)
        val usage = AppUsage(1, "yt", usedMs = 28 * MINUTE_MS, stretchUsedMs = 2 * MINUTE_MS)
        val d = RulesEngine.decide(capped, usage, now, emptyList()) as Decision.Allow
        assertThat(d.limitAt).isEqualTo(now + 2 * MINUTE_MS)
    }

    @Test fun `emergency window opens a blocked app until it ends`() {
        val usage = AppUsage(1, "ig", emergencyUntil = now + 5 * MINUTE_MS)
        val d = RulesEngine.decide(block, usage, now) as Decision.Allow
        assertThat(d.limitAt).isEqualTo(now + 5 * MINUTE_MS)
        val usage2 = used(30).copy(emergencyUntil = now + 3 * MINUTE_MS)
        assertThat(RulesEngine.decide(limit30, usage2, now)).isEqualTo(Decision.Allow(limitAt = now + 3 * MINUTE_MS))
    }

    @Test fun `expired emergency window blocks again`() {
        val usage = AppUsage(1, "ig", emergencyUntil = now)
        assertThat(RulesEngine.decide(block, usage, now)).isEqualTo(Decision.Block(BlockReason.RuleBlock))
    }

    @Test fun `emergency does not apply to a session break`() {
        val capped = limit30.copy(capMin = 15, breakMin = 10)
        val usage = AppUsage(1, "yt", usedMs = 15 * MINUTE_MS, breakUntil = now + 5 * MINUTE_MS, emergencyUntil = now + 5 * MINUTE_MS)
        assertThat(RulesEngine.decide(capped, usage, now)).isInstanceOf(Decision.Block::class.java)
    }

    @Test fun `apps without a rule are not affected`() {
        assertThat(RulesEngine.decide(null, AppUsage(1, "x"), now)).isEqualTo(Decision.Allow())
    }
}
