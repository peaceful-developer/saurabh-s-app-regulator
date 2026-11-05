package com.saurabh.sar.core.modes

import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.EmergencyEffort
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.core.model.ScheduleEntry
import com.saurabh.sar.core.schedule.ScheduleCalculator
import com.saurabh.sar.core.schedule.ScheduleProblem

enum class ModeProblem(val message: String) {
    NAME_EMPTY("Give the mode a name."),
    NAME_TOO_LONG("Names can be up to 30 characters."),
    NAME_TAKEN("Another mode already has this name."),
    MESSAGE_EMPTY("Write a block-screen message."),
    MESSAGE_TOO_LONG("The message can be up to 120 characters."),
    SCHEDULE_MISSING("Add at least one schedule time."),
    SCHEDULE_NO_DAYS("Pick at least one day for each schedule time."),
    SCHEDULE_START_EQUALS_END("A schedule's end time can't equal its start time."),
    EMERGENCY_RANGE("Emergency time settings are out of range."),
    PHRASE_INVALID("The phrase must be 1–100 characters."),
    ALLOWANCE_RANGE("Allowed time must be 1–720 minutes."),
    CAP_RANGE("Session cap and break must be at least 1 minute."),
}

/** Checks a mode before it is saved (FSD 3.1, 3.3, 4.2, 6.2). */
object ModeValidator {
    fun validate(
        mode: Mode,
        schedule: List<ScheduleEntry>,
        rules: List<AppRule>,
        otherModeNames: Collection<String>,
    ): List<ModeProblem> = buildList {
        val name = mode.name.trim()
        if (name.isEmpty()) add(ModeProblem.NAME_EMPTY)
        if (name.length > Mode.NAME_MAX) add(ModeProblem.NAME_TOO_LONG)
        if (otherModeNames.any { it.trim().equals(name, ignoreCase = true) }) add(ModeProblem.NAME_TAKEN)
        if (mode.message.isBlank()) add(ModeProblem.MESSAGE_EMPTY)
        if (mode.message.length > Mode.MESSAGE_MAX) add(ModeProblem.MESSAGE_TOO_LONG)
        if (mode.type == ModeType.SCHEDULED) {
            if (schedule.isEmpty()) add(ModeProblem.SCHEDULE_MISSING)
            schedule.forEach {
                when (ScheduleCalculator.validate(it)) {
                    ScheduleProblem.NO_DAYS -> add(ModeProblem.SCHEDULE_NO_DAYS)
                    ScheduleProblem.START_EQUALS_END, ScheduleProblem.OUT_OF_RANGE -> add(ModeProblem.SCHEDULE_START_EQUALS_END)
                    null -> Unit
                }
            }
        }
        if (mode.emergencyMinutes !in 1..15 || mode.emergencyUses !in 1..5 || mode.emergencyWaitSec !in 10..120) {
            add(ModeProblem.EMERGENCY_RANGE)
        }
        if (mode.emergencyEffort == EmergencyEffort.PHRASE &&
            (mode.emergencyPhrase.isBlank() || mode.emergencyPhrase.length > Mode.PHRASE_MAX)
        ) add(ModeProblem.PHRASE_INVALID)
        rules.filter { it.type == RuleType.LIMIT }.forEach { r ->
            if ((r.allowanceMin ?: 0) !in AppRule.ALLOWANCE_MIN..AppRule.ALLOWANCE_MAX) add(ModeProblem.ALLOWANCE_RANGE)
            if ((r.capMin != null) != (r.breakMin != null) || (r.capMin ?: 1) < 1 || (r.breakMin ?: 1) < 1) add(ModeProblem.CAP_RANGE)
        }
    }.distinct()

    /** Phrase check for emergency time: typed exactly (FSD 6.2). Surrounding spaces are forgiven. */
    fun phraseMatches(expected: String, typed: String): Boolean = expected.trim() == typed.trim()
}
