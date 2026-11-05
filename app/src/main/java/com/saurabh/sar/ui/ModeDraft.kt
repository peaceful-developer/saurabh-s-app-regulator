package com.saurabh.sar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.ScheduleEntry
import com.saurabh.sar.core.modes.ModeBundle

/** Editable copy of a mode, its schedule and rules. Saved in one transaction. */
class ModeDraft(original: Mode, schedule: List<ScheduleEntry>, rules: List<AppRule>) {
    val id = original.id
    var mode by mutableStateOf(original)
    val schedule = mutableStateListOf<ScheduleEntry>().apply { addAll(schedule) }
    val rules = mutableStateMapOf<String, AppRule>().apply { rules.forEach { put(it.packageName, it) } }

    fun toBundle(): ModeBundle = ModeBundle(
        mode,
        if (mode.type == ModeType.SCHEDULED) schedule.toList() else emptyList(),
        rules.values.toList(),
    )

    companion object {
        fun blank(sortOrder: Int) = ModeDraft(
            Mode(name = "", type = ModeType.MANUAL, message = "Not now. You chose this.", imagePath = "builtin:study", icon = "layers", sortOrder = sortOrder),
            emptyList(), emptyList(),
        )
    }
}
