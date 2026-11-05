package com.saurabh.sar.core.model

const val MINUTE_MS = 60_000L

enum class ModeType { MANUAL, SCHEDULED }

enum class EmergencyEffort { WAIT, PHRASE }

/** A named set of app rules with its own block screen (FSD 3.1). */
data class Mode(
    val id: Long = 0,
    val name: String,
    val type: ModeType,
    val message: String,
    /** "builtin:<key>" for a bundled image, an absolute file path for a user image, or null. */
    val imagePath: String? = null,
    /** Key of the icon shown on mode cards (book, dumbbell, wind, briefcase, sun, ...). */
    val icon: String = "layers",
    val emergencyEnabled: Boolean = true,
    val emergencyMinutes: Int = 5,
    val emergencyUses: Int = 2,
    val emergencyEffort: EmergencyEffort = EmergencyEffort.WAIT,
    val emergencyWaitSec: Int = 30,
    val emergencyPhrase: String = DEFAULT_PHRASE,
    val sortOrder: Int = 0,
) {
    companion object {
        const val DEFAULT_PHRASE = "I am choosing to break my own rule"
        const val NAME_MAX = 30
        const val MESSAGE_MAX = 120
        const val PHRASE_MAX = 100
    }
}

/**
 * One schedule line: days + start + end (FSD 3.1). [daysMask] uses Mon = bit 0 ... Sun = bit 6.
 * Minutes are minutes after midnight. An end before the start runs past midnight; the
 * days refer to the day the window starts.
 */
data class ScheduleEntry(
    val id: Long = 0,
    val modeId: Long = 0,
    val daysMask: Int,
    val startMinute: Int,
    val endMinute: Int,
) {
    val isOvernight: Boolean get() = endMinute < startMinute

    fun hasDay(dayIndexMonZero: Int): Boolean = daysMask and (1 shl dayIndexMonZero) != 0

    companion object {
        const val MON_FRI = 0b0011111
        const val SAT_SUN = 0b1100000
        const val EVERY_DAY = 0b1111111
    }
}

enum class RuleType { BLOCK, LIMIT }

/** The single rule for one controlled app inside a mode (FSD 4). */
data class AppRule(
    val modeId: Long = 0,
    val packageName: String,
    val type: RuleType,
    /** Minutes per run, 1–720. LIMIT only. */
    val allowanceMin: Int? = null,
    /** Session cap: max continuous minutes. LIMIT only, optional. */
    val capMin: Int? = null,
    /** Session cap: break minutes after the cap is hit. */
    val breakMin: Int? = null,
) {
    val hasSessionCap: Boolean get() = type == RuleType.LIMIT && capMin != null && breakMin != null
    val allowanceMs: Long get() = (allowanceMin ?: 0) * MINUTE_MS

    companion object {
        const val ALLOWANCE_MIN = 1
        const val ALLOWANCE_MAX = 720
    }
}

enum class EndReason { TIME_UP, STOPPED, FREED, SWITCHED, EXPIRED_WHILE_OFF }

/** One continuous period during which a mode is active (FSD 2). */
data class Run(
    val id: Long = 0,
    val modeId: Long,
    val startedAt: Long,
    /** Fixed when the run starts (FSD 10). Null for an open-ended manual run. */
    val plannedEndAt: Long? = null,
    val locked: Boolean,
    val scheduled: Boolean = false,
    val endedAt: Long? = null,
    val endReason: EndReason? = null,
    val emergencyUsesUsed: Int = 0,
) {
    val isActive: Boolean get() = endedAt == null
}

/** Usage of one app within one run. */
data class AppUsage(
    val runId: Long,
    val packageName: String,
    val usedMs: Long = 0,
    /** Length of the current continuous stretch (for the session cap). */
    val stretchUsedMs: Long = 0,
    val lastForegroundEndAt: Long? = null,
    val breakUntil: Long? = null,
    val emergencyUntil: Long? = null,
)

/** Android app categories SAR cares about, mapped from ApplicationInfo.category by the platform layer. */
enum class AppCategory { SOCIAL, GAME, VIDEO, AUDIO, PRODUCTIVITY, OTHER }

data class InstalledApp(val packageName: String, val label: String, val category: AppCategory)
