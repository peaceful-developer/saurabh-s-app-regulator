package com.saurabh.sar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.engine.RulesEngine
import com.saurabh.sar.core.engine.Decision
import com.saurabh.sar.core.engine.BlockReason
import com.saurabh.sar.core.engine.RunStateMachine
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.core.schedule.ScheduleCalculator
import com.saurabh.sar.data.ModeWithDetails
import com.saurabh.sar.platform.ActiveState
import com.saurabh.sar.platform.Permissions
import com.saurabh.sar.platform.SarPermission
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.Screen
import com.saurabh.sar.ui.Tab
import com.saurabh.sar.ui.components.AppRow
import com.saurabh.sar.ui.components.AppRowState
import com.saurabh.sar.ui.components.Banner
import com.saurabh.sar.ui.components.ButtonKind
import com.saurabh.sar.ui.components.ModeCard
import com.saurabh.sar.ui.components.ModeCardState
import com.saurabh.sar.ui.components.ProgressRing
import com.saurabh.sar.ui.components.SarButton
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.components.SectionLabel
import com.saurabh.sar.ui.components.Tag
import com.saurabh.sar.ui.formatClock
import com.saurabh.sar.ui.formatHm
import com.saurabh.sar.ui.minutesOf
import com.saurabh.sar.ui.rememberNow
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarType
import kotlinx.coroutines.launch
import java.time.ZoneId

/** Figma 01 Home (FSD 8.1). */
@Composable
fun HomeScreen(nav: Nav) {
    val app = LocalContext.current.applicationContext as SarApp
    val active by app.controller.active.collectAsState()
    val pending by app.controller.pending.collectAsState()
    val modes by app.repository.modes.collectAsState(initial = emptyList())
    val now by rememberNow()
    val scope = rememberCoroutineScope()
    val missing = remember(now, active) { Permissions.missing(app) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("SAR", style = SarType.Logo)
                Text("APP REGULATOR", style = SarType.Section.copy(fontSize = SarType.Section.fontSize * 0.8f))
            }
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(SarColors.BgSurface)
                    .border(1.dp, SarColors.LineSubtle, RoundedCornerShape(14.dp)).clickable { nav.tab = Tab.SETTINGS },
                contentAlignment = Alignment.Center,
            ) { SarIcon(R.drawable.ic_bell, SarColors.TextPrimary, 20.dp) }
        }

        if (SarPermission.ACCESSIBILITY in missing && active != null) {
            Banner("Blocking is off. Turn SAR's accessibility service back on.", SarColors.Danger, "Fix", {
                app.startActivity(Permissions.settingsIntent(app, SarPermission.ACCESSIBILITY))
            })
            Spacer(Modifier.height(12.dp))
        } else if (missing.isNotEmpty()) {
            Banner("${missing.size} permission${if (missing.size > 1) "s" else ""} missing: ${missing.joinToString { it.title }}", SarColors.Danger, "Fix", {
                nav.tab = Tab.SETTINGS
            })
            Spacer(Modifier.height(12.dp))
        }

        val cur = active
        pending.firstOrNull()?.let { p ->
            val m = modes.firstOrNull { it.mode.id == p.modeId }?.mode
            if (m != null && cur != null) {
                Banner("${m.name} is scheduled now. ${cur.mode.name} is still active.", SarColors.Violet, "Start", {
                    scope.launch { app.controller.switchToScheduled(m.id) }
                })
                Spacer(Modifier.height(12.dp))
            }
        }

        if (cur != null) {
            ActiveCard(cur, now, onExtend = { scope.launch { message = app.controller.extend(RunStateMachine.EXTEND_STEP_MS)?.let { "Can't extend: ${it.name.lowercase().replace('_', ' ')}" } } },
                onStop = { scope.launch { app.controller.stop() } },
                onFree = { nav.push(Screen.FreeMode) })
            message?.let { Spacer(Modifier.height(8.dp)); Text(it, style = SarType.Meta, color = SarColors.Warn) }
            Spacer(Modifier.height(20.dp))
            SectionLabel("Apps in this mode", trailing = { Text("${cur.rules.size}", style = SarType.Section, color = SarColors.Cyan) })
            if (cur.rules.isEmpty()) Text("This mode controls no apps yet. Long-press it in Modes to edit.", style = SarType.BodySecondary)
            cur.rules.values.sortedBy { app.installedApps.label(it.packageName).lowercase() }.forEach { rule ->
                val usage = cur.usageOf(rule.packageName)
                val label = remember(rule.packageName) { app.installedApps.label(rule.packageName) }
                val decision = RulesEngine.decide(rule, usage, now, emptyList())
                val (text, frac, st) = when {
                    rule.type == RuleType.BLOCK && decision is Decision.Block -> Triple("Blocked", 1f, AppRowState.BLOCKED)
                    rule.type == RuleType.BLOCK -> Triple("Emergency", 0.5f, AppRowState.WARNING)
                    else -> {
                        val used = minutesOf(usage.usedMs)
                        val total = rule.allowanceMin ?: 0
                        val f = if (total == 0) 1f else (usage.usedMs.toFloat() / (total * 60_000f))
                        val br = (decision as? Decision.Block)?.reason as? BlockReason.SessionBreak
                        when {
                            br != null -> Triple("Break · ${formatHm(br.until - now)}", f, AppRowState.WARNING)
                            decision is Decision.Block -> Triple("$used / $total min", 1f, AppRowState.BLOCKED)
                            f >= 0.8f -> Triple("$used / $total min", f, AppRowState.WARNING)
                            else -> Triple("$used / $total min", f, AppRowState.LIMIT)
                        }
                    }
                }
                AppRow(rule.packageName, label, text, frac, st)
                Spacer(Modifier.height(10.dp))
            }
        } else {
            NoModeCard()
            Spacer(Modifier.height(20.dp))
            SectionLabel("Start a mode")
            modes.filter { it.mode.type == ModeType.MANUAL }.forEach { m ->
                ModeCard(m.mode.name, "Manual · ${m.rules.size} apps", m.mode.icon, ModeCardState.IDLE, "Start",
                    onClick = { nav.startSheetFor = m.mode.id }, onLongClick = { nav.push(Screen.EditMode(m.mode.id)) })
                Spacer(Modifier.height(10.dp))
            }
        }
        UpNext(modes, now, cur)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ActiveCard(state: ActiveState, now: Long, onExtend: () -> Unit, onStop: () -> Unit, onFree: () -> Unit) {
    val run = state.run
    SarCard(
        Modifier.fillMaxWidth(),
        tint = SarColors.Cyan.copy(alpha = 0.05f),
        borderColor = SarColors.Cyan.copy(alpha = 0.45f),
        glowColor = SarColors.Cyan,
        shape = RoundedCornerShape(24.dp),
        padding = androidx.compose.foundation.layout.PaddingValues(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ACTIVE MODE", style = SarType.Section, modifier = Modifier.weight(1f))
            if (run.locked) Tag("Locked", SarColors.Violet, icon = R.drawable.ic_lock) else Tag("Unlocked", SarColors.TextSecondary, icon = R.drawable.ic_lock_open)
        }
        Spacer(Modifier.height(10.dp))
        Text(state.mode.name.uppercase(), style = SarType.Hero)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val end = run.plannedEndAt
            val frac = if (end != null) ((end - now).toFloat() / (end - run.startedAt).coerceAtLeast(1)) else 1f
            ProgressRing(frac, 116.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (end != null) formatHm(end - now) else formatHm(now - run.startedAt), style = SarType.BigNumber)
                    Text(if (end != null) "LEFT" else "RUNNING", style = SarType.Section.copy(fontSize = SarType.Section.fontSize * 0.85f))
                }
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Stat("Ends at", end?.let { formatClock(it) } ?: "When you stop")
                Stat("Started", formatClock(run.startedAt))
                if (state.mode.emergencyEnabled) Stat("Emergency", "${state.emergencyLeft} left", SarColors.Warn)
            }
        }
        Spacer(Modifier.height(18.dp))
        when {
            !run.locked -> SarButton("Stop", onStop, Modifier.fillMaxWidth(), kind = ButtonKind.GHOST, icon = R.drawable.ic_x)
            !run.scheduled -> SarButton("Extend +15 min", onExtend, Modifier.fillMaxWidth(), kind = ButtonKind.GHOST, icon = R.drawable.ic_plus)
            else -> Text("Scheduled · ends on its own. To end early use Free a mode.", style = SarType.Meta)
        }
        if (run.locked) {
            Spacer(Modifier.height(6.dp))
            Text("Free a mode", style = SarType.Meta, color = SarColors.Violet, modifier = Modifier.clickable(onClick = onFree).padding(vertical = 6.dp))
        }
    }
}

@Composable
private fun Stat(label: String, value: String, color: androidx.compose.ui.graphics.Color = SarColors.TextPrimary) {
    Column {
        Text(label.uppercase(), style = SarType.Section.copy(fontSize = SarType.Section.fontSize * 0.85f))
        Text(value.uppercase(), style = SarType.Button.copy(fontSize = SarType.Button.fontSize * 1.15f), color = color)
    }
}

@Composable
private fun NoModeCard() {
    SarCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
        Text("NO MODE ACTIVE", style = SarType.Section)
        Spacer(Modifier.height(10.dp))
        Text("All apps are free right now.", style = SarType.Hero.copy(fontSize = SarType.Hero.fontSize * 0.7f))
        Spacer(Modifier.height(6.dp))
        Text("Tap a mode to start it, or wait for a scheduled one.", style = SarType.BodySecondary)
    }
}

@Composable
private fun UpNext(modes: List<ModeWithDetails>, now: Long, cur: ActiveState?) {
    val zone = ZoneId.systemDefault()
    val next = modes.filter { it.mode.type == ModeType.SCHEDULED && it.mode.id != cur?.mode?.id }
        .mapNotNull { m -> ScheduleCalculator.nextStart(m.schedule, now, zone)?.let { m to it } }
        .minByOrNull { it.second } ?: return
    Spacer(Modifier.height(10.dp))
    SectionLabel("Up next")
    SarCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SarIcon(R.drawable.ic_calendar, SarColors.Violet, 18.dp)
            Spacer(Modifier.width(12.dp))
            Text(next.first.mode.name, style = SarType.RowTitle, modifier = Modifier.weight(1f))
            val days = java.time.Duration.ofMillis(next.second - now).toDays()
            val whenText = (if (days >= 1) java.time.Instant.ofEpochMilli(next.second).atZone(zone).dayOfWeek.name.take(3) + " " else "") + formatClock(next.second)
            Tag(whenText, SarColors.Violet)
        }
    }
}
