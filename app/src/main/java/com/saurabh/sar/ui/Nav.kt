package com.saurabh.sar.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Tab { HOME, MODES, HISTORY, SETTINGS }

sealed interface Screen {
    data object Tabs : Screen
    /** [modeId] null = new mode; [duplicateOf] copies another mode. */
    data class EditMode(val modeId: Long?, val duplicateOf: Long? = null) : Screen
    data object AppsRules : Screen
    data object Emergency : Screen
    data object FreeMode : Screen
    data object Onboarding : Screen
    data object ReviewDefaults : Screen
}

/** A tiny back stack; no navigation library needed (CLAUDE.md: minimal dependencies). */
class Nav {
    val stack = mutableStateListOf<Screen>(Screen.Tabs)
    var tab by mutableStateOf(Tab.HOME)
    /** The mode being edited, shared by the editor and its sub-screens. */
    var draft: ModeDraft? = null
    /** Which editor screen [draft] belongs to. */
    var draftFor: Screen? = null
    /** Mode id whose Start sheet is open (Figma 03). */
    var startSheetFor by mutableStateOf<Long?>(null)

    val top: Screen get() = stack.last()
    fun push(s: Screen) { stack.add(s) }
    fun pop(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
    fun reset(s: Screen = Screen.Tabs) { stack.clear(); stack.add(s) }
}
