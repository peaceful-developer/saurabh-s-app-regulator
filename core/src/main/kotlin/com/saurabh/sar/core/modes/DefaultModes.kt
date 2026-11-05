package com.saurabh.sar.core.modes

import com.saurabh.sar.core.model.AppCategory
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.InstalledApp
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.core.model.ScheduleEntry

/** A mode with its rules and schedule, before it has database ids. */
data class ModeBundle(val mode: Mode, val rules: List<AppRule>, val schedule: List<ScheduleEntry>)

/** The five default modes with suggestions from installed app categories (FSD 3.4). */
object DefaultModes {
    fun build(apps: List<InstalledApp>): List<ModeBundle> {
        val social = apps.filter { it.category == AppCategory.SOCIAL }
        val games = apps.filter { it.category == AppCategory.GAME }
        val video = apps.filter { it.category == AppCategory.VIDEO }
        fun block(list: List<InstalledApp>) = list.map { AppRule(packageName = it.packageName, type = RuleType.BLOCK) }
        fun limit(list: List<InstalledApp>, min: Int) =
            list.map { AppRule(packageName = it.packageName, type = RuleType.LIMIT, allowanceMin = min) }

        return listOf(
            ModeBundle(
                Mode(name = "Self Study", type = ModeType.MANUAL, message = "Back to the books.",
                    imagePath = "builtin:study", icon = "book", sortOrder = 0),
                block(social) + block(games) + limit(video, 20), emptyList(),
            ),
            ModeBundle(
                Mode(name = "Gym", type = ModeType.MANUAL, message = "Lift now. Scroll later.",
                    imagePath = "builtin:gym", icon = "activity", sortOrder = 1),
                block(social), emptyList(),
            ),
            ModeBundle(
                Mode(name = "Meditation", type = ModeType.MANUAL, message = "Breathe. The feed can wait.",
                    imagePath = "builtin:meditation", icon = "wind", emergencyEnabled = false, sortOrder = 2),
                block(social + games + video), emptyList(),
            ),
            ModeBundle(
                Mode(name = "Office Work", type = ModeType.SCHEDULED, message = "Focus. The feed will still be there at 6.",
                    imagePath = "builtin:office", icon = "briefcase", sortOrder = 3),
                limit(social, 15) + block(games),
                listOf(ScheduleEntry(daysMask = ScheduleEntry.MON_FRI, startMinute = 9 * 60, endMinute = 18 * 60)),
            ),
            ModeBundle(
                Mode(name = "Weekend", type = ModeType.SCHEDULED, message = "Enjoy the day, not the feed.",
                    imagePath = "builtin:weekend", icon = "sun", sortOrder = 4),
                limit(social, 60) + limit(games, 60),
                listOf(ScheduleEntry(daysMask = ScheduleEntry.SAT_SUN, startMinute = 0, endMinute = 23 * 60 + 59)),
            ),
        ).map { b -> b.copy(rules = b.rules.distinctBy { it.packageName }) }
    }
}
