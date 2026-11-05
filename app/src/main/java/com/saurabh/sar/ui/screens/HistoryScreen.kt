package com.saurabh.sar.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.history.HistoryCalculator
import com.saurabh.sar.core.model.EndReason
import com.saurabh.sar.data.toModel
import com.saurabh.sar.ui.components.AppRow
import com.saurabh.sar.ui.components.AppRowState
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.SectionLabel
import com.saurabh.sar.ui.components.Segmented
import com.saurabh.sar.ui.components.Tag
import com.saurabh.sar.ui.components.glow
import com.saurabh.sar.ui.formatClock
import com.saurabh.sar.ui.formatDuration
import com.saurabh.sar.ui.rememberNow
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Figma 08 History (FSD 8.3): Today / 7 days, top apps, recent runs. */
@Composable
fun HistoryScreen() {
    val app = LocalContext.current.applicationContext as SarApp
    val now by rememberNow()
    var view by remember { mutableIntStateOf(1) }
    val zone = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val from = today.minusDays(14).atStartOfDay(zone).toInstant().toEpochMilli()
    val runEntities by remember(from) { app.repository.observeRunsSince(from) }.collectAsState(initial = emptyList())
    val usage by remember(from) { app.repository.observeUsageSince(from) }.collectAsState(initial = emptyList())
    val active by app.controller.active.collectAsState()
    LaunchedEffect(Unit) { app.usageReconciler.reconcile() }

    // Include the live, not-yet-saved usage of the running mode.
    val liveUsage = active?.let { a -> usage.filter { it.runId != a.run.id } + a.usage.values } ?: usage
    val names = runEntities.associate { it.id to it.modeName }
    val days = if (view == 0) 1 else 7
    val s = HistoryCalculator.summarise(runEntities.map { it.toModel() }, liveUsage, today, zone, days)

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("HISTORY", style = SarType.ScreenTitle, modifier = Modifier.weight(1f))
            Segmented(listOf("Today", "7 days"), view, { view = it }, Modifier.width(170.dp))
        }
        SarCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
            Text("CONTROLLED APPS", style = SarType.Section)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatDuration(s.totalMs), style = SarType.BigNumber, modifier = Modifier.weight(1f))
                if (s.previousTotalMs > 0) {
                    val change = ((s.totalMs - s.previousTotalMs) * 100 / s.previousTotalMs)
                    Text("${if (change > 0) "+" else ""}$change% vs last ${if (days == 1) "day" else "week"}", style = SarType.Meta,
                        color = if (change <= 0) SarColors.Ok else SarColors.Warn)
                }
            }
            if (days == 7) {
                Spacer(Modifier.height(16.dp))
                val max = (s.days.maxOfOrNull { it.totalMs } ?: 0L).coerceAtLeast(1)
                Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
                    s.days.forEach { d ->
                        val isToday = d.date == today
                        Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            val frac = (d.totalMs.toFloat() / max).coerceIn(0.04f, 1f)
                            Box(
                                Modifier.fillMaxWidth().fillMaxHeight(frac * 0.82f)
                                    .then(if (isToday) Modifier.glow(SarColors.Cyan, 10.dp, RoundedCornerShape(8.dp)) else Modifier)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isToday) SarColors.Cyan else SarColors.Violet.copy(alpha = 0.55f)),
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(d.date.dayOfWeek.name.take(1), style = SarType.Chip, color = if (isToday) SarColors.Cyan else SarColors.TextSecondary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        if (s.byMode.isNotEmpty()) {
            SectionLabel("By mode")
            SarCard(Modifier.fillMaxWidth()) {
                s.byMode.forEach { mt ->
                    val name = s.runs.firstOrNull { it.modeId == mt.modeId }?.let { names[it.id] } ?: "Mode"
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text(name, style = SarType.Body, modifier = Modifier.weight(1f))
                        Text(formatDuration(mt.totalMs), style = SarType.Meta)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        SectionLabel("Top apps")
        if (s.topApps.isEmpty()) Text("No controlled-app use yet.", style = SarType.BodySecondary)
        val topMax = s.topApps.firstOrNull()?.totalMs?.coerceAtLeast(1) ?: 1
        s.topApps.take(8).forEach { a ->
            val label = remember(a.packageName) { app.installedApps.label(a.packageName) }
            AppRow(a.packageName, label, formatDuration(a.totalMs), a.totalMs.toFloat() / topMax, AppRowState.LIMIT)
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(10.dp))
        SectionLabel("Recent runs")
        if (s.runs.isEmpty()) Text("No runs in this period.", style = SarType.BodySecondary)
        s.runs.take(20).forEach { r ->
            SarCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text((names[r.id] ?: "Mode").uppercase(), style = SarType.CardTitle.copy(fontSize = SarType.CardTitle.fontSize * 0.9f))
                        val day = Instant.ofEpochMilli(r.startedAt).atZone(zone).toLocalDate()
                        val dayText = if (day == today) "Today" else if (day == today.minusDays(1)) "Yesterday" else day.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
                        val endText = r.endedAt?.let { formatClock(it) } ?: "now"
                        val em = if (r.emergencyUsesUsed > 0) " · ${r.emergencyUsesUsed} emergency" else ""
                        Text("$dayText · ${formatClock(r.startedAt)}–$endText$em", style = SarType.Meta)
                    }
                    val (t, c) = when (r.endReason) {
                        EndReason.TIME_UP -> "Time up" to SarColors.Ok
                        EndReason.STOPPED -> "Stopped" to SarColors.TextSecondary
                        EndReason.FREED -> "Freed" to SarColors.Violet
                        EndReason.SWITCHED -> "Switched" to SarColors.Cyan
                        EndReason.EXPIRED_WHILE_OFF -> "Phone off" to SarColors.Warn
                        null -> "Running" to SarColors.Cyan
                    }
                    Tag(t, c)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(24.dp))
    }
}
