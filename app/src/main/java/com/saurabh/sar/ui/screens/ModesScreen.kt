package com.saurabh.sar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.data.ModeWithDetails
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.Screen
import com.saurabh.sar.ui.components.ModeCard
import com.saurabh.sar.ui.components.ModeCardState
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.components.SectionLabel
import com.saurabh.sar.ui.components.glow
import com.saurabh.sar.ui.formatClock
import com.saurabh.sar.ui.formatMinuteOfDay
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarType

/** Figma 02 Modes: tap to start, hold to edit. */
@Composable
fun ModesScreen(nav: Nav) {
    val app = LocalContext.current.applicationContext as SarApp
    val modes by app.repository.modes.collectAsState(initial = emptyList())
    val active by app.controller.active.collectAsState()

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("MODES", style = SarType.ScreenTitle)
                Text("Tap to start · hold to edit", style = SarType.Meta)
            }
            Box(
                Modifier.size(48.dp).glow(SarColors.Cyan, 14.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp))
                    .background(SarColors.Cyan).clickable { nav.push(Screen.EditMode(null)) },
                contentAlignment = Alignment.Center,
            ) { SarIcon(R.drawable.ic_plus, SarColors.OnAccent, 24.dp) }
        }
        listOf(ModeType.MANUAL to "Manual", ModeType.SCHEDULED to "Scheduled").forEach { (type, label) ->
            val list = modes.filter { it.mode.type == type }
            if (list.isNotEmpty()) {
                SectionLabel(label)
                list.forEach { m ->
                    val running = active?.mode?.id == m.mode.id
                    val state = when {
                        running -> ModeCardState.RUNNING
                        type == ModeType.SCHEDULED -> ModeCardState.SCHEDULED
                        else -> ModeCardState.IDLE
                    }
                    ModeCard(
                        name = m.mode.name,
                        meta = meta(m, running, active?.run?.plannedEndAt),
                        icon = m.mode.icon,
                        state = state,
                        tag = when (state) { ModeCardState.RUNNING -> "Running"; ModeCardState.SCHEDULED -> "Auto"; else -> "Start" },
                        onClick = { if (type == ModeType.MANUAL && !running) nav.startSheetFor = m.mode.id else nav.push(Screen.EditMode(m.mode.id)) },
                        onLongClick = { nav.push(Screen.EditMode(m.mode.id)) },
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        if (modes.isEmpty()) Text("No modes yet. Tap + to create one.", style = SarType.BodySecondary)
        Spacer(Modifier.height(24.dp))
    }
}

private val DAY_LETTERS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun daysText(mask: Int): String = when (mask) {
    0b0011111 -> "Mon–Fri"
    0b1100000 -> "Sat–Sun"
    0b1111111 -> "Every day"
    else -> DAY_LETTERS.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.joinToString(" ")
}

private fun meta(m: ModeWithDetails, running: Boolean, endAt: Long?): String {
    if (running) return if (endAt != null) "Locked · ends ${formatClock(endAt)}" else "Running · until you stop it"
    val blocked = m.rules.count { it.type == RuleType.BLOCK }
    return if (m.mode.type == ModeType.SCHEDULED) {
        m.schedule.firstOrNull()?.let { s ->
            val all = s.startMinute == 0 && s.endMinute == 23 * 60 + 59
            "${daysText(s.daysMask)} · ${if (all) "all day" else formatMinuteOfDay(s.startMinute) + "–" + formatMinuteOfDay(s.endMinute)}" +
                if (m.schedule.size > 1) " +${m.schedule.size - 1}" else ""
        } ?: "No schedule"
    } else "Manual · ${m.rules.size} apps" + if (blocked > 0) " · $blocked blocked" else ""
}
