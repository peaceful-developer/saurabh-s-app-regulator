package com.saurabh.sar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.engine.RunError
import com.saurabh.sar.core.engine.RunStateMachine
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.Screen
import com.saurabh.sar.ui.components.SarButton
import com.saurabh.sar.ui.components.SarDialog
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.components.SelectChip
import com.saurabh.sar.ui.components.Stepper
import com.saurabh.sar.ui.components.modeIcon
import com.saurabh.sar.ui.formatClock
import com.saurabh.sar.ui.formatDurationLabel
import com.saurabh.sar.ui.rememberNow
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarShapes
import com.saurabh.sar.ui.theme.SarType
import kotlinx.coroutines.launch

private const val MIN = 60_000L
private val CHIPS = listOf(30 * MIN, 45 * MIN, 60 * MIN, 90 * MIN, 120 * MIN)

/** Figma 03 Start sheet: START (unlocked) or START FOR… (locked), with duration chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartSheet(modeId: Long, nav: Nav, onClose: () -> Unit) {
    val app = LocalContext.current.applicationContext as SarApp
    val modes by app.repository.modes.collectAsState(initial = emptyList())
    val active by app.controller.active.collectAsState()
    val m = modes.firstOrNull { it.mode.id == modeId } ?: return
    val scope = rememberCoroutineScope()
    var locked by remember { mutableStateOf(true) }
    var duration by remember { mutableLongStateOf(60 * MIN) }
    var custom by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<RunError?>(null) }
    val now by rememberNow()

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(SarShapes.Sheet).background(SarColors.BgRaised)
                .border(1.dp, SarColors.LineSubtle, SarShapes.Sheet)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding().padding(24.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).clip(CircleShape).background(SarColors.LineSubtle))
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SarColors.BgBase).border(1.dp, SarColors.LineSubtle, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center) { SarIcon(modeIcon(m.mode.icon), SarColors.Cyan, 22.dp) }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("START ${m.mode.name.uppercase()}", style = SarType.CardTitle.copy(fontSize = SarType.CardTitle.fontSize * 1.2f))
                    Text("Manual mode · ${m.rules.size} apps controlled", style = SarType.Meta)
                }
            }
            Spacer(Modifier.height(18.dp))
            OptionCard("Start", "Runs until you stop it", R.drawable.ic_play, !locked) { locked = false }
            Spacer(Modifier.height(10.dp))
            OptionCard("Start for…", "Locked until the time is up", R.drawable.ic_lock, locked) { locked = true }
            if (locked) {
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CHIPS.forEach { d -> SelectChip(formatDurationLabel(d), !custom && duration == d, { custom = false; duration = d }) }
                    SelectChip("Custom", custom, { custom = true })
                }
                if (custom) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Duration", style = SarType.Body, modifier = Modifier.weight(1f))
                        Stepper(formatDurationLabel(duration),
                            onMinus = { duration = (duration - 15 * MIN).coerceAtLeast(RunStateMachine.MIN_DURATION_MS) },
                            onPlus = { duration = (duration + 15 * MIN).coerceAtMost(RunStateMachine.MAX_DURATION_MS) })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SarIcon(R.drawable.ic_clock, SarColors.TextSecondary, 14.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("Ends at ${formatClock(now + duration)} · can be extended, never shortened", style = SarType.Meta)
                }
            }
            Spacer(Modifier.height(18.dp))
            SarButton(if (locked) "Lock in · ${formatDurationLabel(duration).lowercase()}" else "Start ${m.mode.name}", {
                scope.launch {
                    val e = app.controller.startManual(modeId, if (locked) duration else null)
                    if (e == null) { onClose(); nav.tab = com.saurabh.sar.ui.Tab.HOME } else error = e
                }
            }, Modifier.fillMaxWidth())
        }
    }

    if (error == RunError.ANOTHER_RUN_ACTIVE) {
        val cur = active
        val locked2 = cur?.run?.locked == true
        SarDialog(
            title = "${cur?.mode?.name ?: "Another mode"} is running",
            text = if (locked2) "It's locked. Only Free a mode can end it early." else "Stop it first, then start ${m.mode.name}.",
            confirm = if (locked2) "Free a mode" else "Stop and start",
            onConfirm = {
                error = null
                if (locked2) { onClose(); nav.push(Screen.FreeMode) } else scope.launch {
                    app.controller.stop()
                    if (app.controller.startManual(modeId, if (locked) duration else null) == null) onClose()
                }
            },
            onDismiss = { error = null },
        )
    } else if (error != null) {
        SarDialog("Can't start", error!!.name.lowercase().replace('_', ' '), "OK", { error = null }, { error = null }, dismiss = null)
    }
}

@Composable
private fun OptionCard(title: String, subtitle: String, icon: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(SarShapes.Card)
            .background(if (selected) SarColors.Cyan.copy(alpha = 0.08f) else SarColors.BgSurface)
            .border(1.dp, if (selected) SarColors.Cyan.copy(alpha = 0.8f) else SarColors.LineSubtle, SarShapes.Card)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SarColors.BgBase), contentAlignment = Alignment.Center) {
            SarIcon(icon, if (selected) SarColors.Cyan else SarColors.TextPrimary, 18.dp)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = SarType.CardTitle.copy(fontSize = SarType.CardTitle.fontSize * 0.9f), color = if (selected) SarColors.Cyan else SarColors.TextPrimary)
            Text(subtitle, style = SarType.Meta)
        }
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(if (selected) SarColors.Cyan else Color.Transparent)
                .border(1.5.dp, if (selected) SarColors.Cyan else SarColors.TextSecondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (selected) SarIcon(R.drawable.ic_check, SarColors.OnAccent, 14.dp) }
    }
}
