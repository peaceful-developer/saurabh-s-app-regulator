package com.saurabh.sar.core.modes

import com.google.common.truth.Truth.assertThat
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.EmergencyEffort
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.core.model.ScheduleEntry
import org.junit.Test

class ModeValidatorTest {
    private val ok = Mode(name = "Gym", type = ModeType.MANUAL, message = "Lift.")

    @Test fun `valid manual mode`() {
        assertThat(ModeValidator.validate(ok, emptyList(), emptyList(), listOf("Self Study"))).isEmpty()
    }

    @Test fun `name rules`() {
        assertThat(ModeValidator.validate(ok.copy(name = " "), emptyList(), emptyList(), emptyList())).contains(ModeProblem.NAME_EMPTY)
        assertThat(ModeValidator.validate(ok.copy(name = "x".repeat(31)), emptyList(), emptyList(), emptyList())).contains(ModeProblem.NAME_TOO_LONG)
        assertThat(ModeValidator.validate(ok, emptyList(), emptyList(), listOf("gym"))).contains(ModeProblem.NAME_TAKEN)
    }

    @Test fun `scheduled mode needs a valid schedule`() {
        val s = ok.copy(type = ModeType.SCHEDULED)
        assertThat(ModeValidator.validate(s, emptyList(), emptyList(), emptyList())).contains(ModeProblem.SCHEDULE_MISSING)
        assertThat(ModeValidator.validate(s, listOf(ScheduleEntry(daysMask = 1, startMinute = 60, endMinute = 60)), emptyList(), emptyList()))
            .contains(ModeProblem.SCHEDULE_START_EQUALS_END)
    }

    @Test fun `allowance and phrase ranges`() {
        val bad = AppRule(packageName = "x", type = RuleType.LIMIT, allowanceMin = 0)
        assertThat(ModeValidator.validate(ok, emptyList(), listOf(bad), emptyList())).contains(ModeProblem.ALLOWANCE_RANGE)
        val phrase = ok.copy(emergencyEffort = EmergencyEffort.PHRASE, emergencyPhrase = "")
        assertThat(ModeValidator.validate(phrase, emptyList(), emptyList(), emptyList())).contains(ModeProblem.PHRASE_INVALID)
        assertThat(ModeValidator.phraseMatches("I am choosing", " I am choosing ")).isTrue()
        assertThat(ModeValidator.phraseMatches("I am choosing", "i am choosing")).isFalse()
    }
}
