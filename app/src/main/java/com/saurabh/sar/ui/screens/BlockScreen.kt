package com.saurabh.sar.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.engine.BlockReason
import com.saurabh.sar.core.model.EmergencyEffort
import com.saurabh.sar.core.modes.ModeValidator
import com.saurabh.sar.ui.components.AppIcon
import com.saurabh.sar.ui.components.ButtonKind
import com.saurabh.sar.ui.components.ModeImage
import com.saurabh.sar.ui.components.ProgressRing
import com.saurabh.sar.ui.components.SarButton
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.Tag
import com.saurabh.sar.ui.formatClock
import com.saurabh.sar.ui.formatHm
import com.saurabh.sar.ui.minutesOf
import com.saurabh.sar.ui.rememberNow
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarShapes
import com.saurabh.sar.ui.theme.SarType

private enum class BlockStep { BLOCKED, EFFORT }

/** Figma 06 Block screen, plus the emergency wait/phrase step (not in Figma, same style). */
@Composable
fun BlockScreen(
    pkg: String,
    reason: BlockReason,
    onGoHome: () -> Unit,
    onEmergencyDone: () -> Unit,
    onReopen: () -> Unit,
    onRunGone: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as SarApp
    val active by app.controller.active.collectAsState()
    val state = active
    LaunchedEffect(state == null) { if (state == null) onRunGone() }
    if (state == null) { Box(Modifier.fillMaxSize().background(SarColors.BgBase)); return }

    var step by rememberSaveable(pkg) { mutableStateOf(BlockStep.BLOCKED) }
    val label = remember(pkg) { app.installedApps.label(pkg) }
    val mode = state.mode
    val isBreak = reason is BlockReason.SessionBreak
    val emergencyLeft = state.emergencyLeft
    val canEmergency = mode.emergencyEnabled && emergencyLeft > 0 && !isBreak

    Box(Modifier.fillMaxSize().background(SarColors.BgBase)) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(0.45f)) {
            ModeImage(mode.imagePath, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, SarColors.BgBase))))
        }
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Tag("${mode.name} · ${if (state.run.locked) "locked" else "active"}", SarColors.Cyan, icon = if (state.run.locked) R.drawable.ic_lock else null)
            Spacer(Modifier.weight(1f))
            when (step) {
                BlockStep.BLOCKED -> BlockedBody(pkg, label, reason, state.run.plannedEndAt, mode.name, mode.message, state.rules[pkg]?.allowanceMin)
                BlockStep.EFFORT -> EffortBody(
                    effort = mode.emergencyEffort,
                    waitSec = mode.emergencyWaitSec,
                    phrase = mode.emergencyPhrase,
                    minutes = mode.emergencyMinutes,
                    label = label,
                    onDone = onEmergencyDone,
                )
            }
            Spacer(Modifier.height(20.dp))
            if (isBreak) {
                val now by rememberNow(1_000)
                if (now >= (reason as BlockReason.SessionBreak).until) {
                    SarButton("Open $label", onReopen, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                }
            }
            SarButton("Go home", onGoHome, Modifier.fillMaxWidth(), kind = if (step == BlockStep.EFFORT) ButtonKind.GHOST else ButtonKind.PRIMARY)
            if (canEmergency && step == BlockStep.BLOCKED) {
                Spacer(Modifier.height(12.dp))
                SarButton("Emergency time · $emergencyLeft left", { step = BlockStep.EFFORT }, Modifier.fillMaxWidth(), kind = ButtonKind.GHOST)
            }
        }
    }
}

@Composable
private fun BlockedBody(pkg: String, label: String, reason: BlockReason, endAt: Long?, modeName: String, message: String, allowanceMin: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(pkg, label, 40.dp, SarColors.Danger)
        Spacer(Modifier.width(12.dp))
        Text(label, style = SarType.RowTitle, modifier = Modifier.weight(1f))
        Tag(if (reason is BlockReason.SessionBreak) "break" else "blocked", if (reason is BlockReason.SessionBreak) SarColors.Warn else SarColors.Danger)
    }
    Spacer(Modifier.height(20.dp))
    Text(message, style = SarType.BlockMessage)
    Spacer(Modifier.height(6.dp))
    val now by rememberNow(if (reason is BlockReason.SessionBreak) 1_000 else 60_000)
    val sub = when (reason) {
        BlockReason.RuleBlock -> "Blocked in $modeName"
        is BlockReason.AllowanceUsed -> "${minutesOf(reason.usedMs)} of ${minutesOf(reason.allowanceMs)} min used"
        is BlockReason.SessionBreak -> "Break: ${((reason.until - now).coerceAtLeast(0) + 59_999) / 60_000} min left"
    }
    Text(sub, style = SarType.BodySecondary.copy(fontSize = SarType.Body.fontSize))
    Spacer(Modifier.height(18.dp))
    SarCard(Modifier.fillMaxWidth()) {
        Row {
            Column(Modifier.weight(1f)) {
                Text(if (reason is BlockReason.SessionBreak) "BREAK LEFT" else "USED", style = SarType.Section)
                Spacer(Modifier.height(6.dp))
                val v = when (reason) {
                    BlockReason.RuleBlock -> "BLOCKED"
                    is BlockReason.AllowanceUsed -> "${minutesOf(reason.usedMs)} / ${minutesOf(reason.allowanceMs)} MIN"
                    is BlockReason.SessionBreak -> {
                        val left = (reason.until - now).coerceAtLeast(0)
                        "${left / 60_000}:${((left / 1000) % 60).toString().padStart(2, '0')}"
                    }
                }
                Text(v, style = SarType.Button.copy(fontSize = SarType.Button.fontSize * 1.25f), color = if (reason is BlockReason.SessionBreak) SarColors.Warn else SarColors.Danger)
            }
            Column(Modifier.weight(1f)) {
                Text("UNLOCKS AT", style = SarType.Section)
                Spacer(Modifier.height(6.dp))
                val v = when {
                    reason is BlockReason.SessionBreak -> formatClock(reason.until)
                    endAt != null -> formatClock(endAt)
                    else -> "WHEN YOU STOP"
                }
                Text(v, style = SarType.Button.copy(fontSize = SarType.Button.fontSize * 1.25f), color = SarColors.TextPrimary)
            }
        }
        if (endAt == null && reason !is BlockReason.SessionBreak) {
            Spacer(Modifier.height(8.dp))
            Text("Unblocks when you stop $modeName.", style = SarType.Meta)
        }
    }
}

@Composable
private fun EffortBody(effort: EmergencyEffort, waitSec: Int, phrase: String, minutes: Int, label: String, onDone: () -> Unit) {
    Text("Emergency time", style = SarType.Hero.copy(fontSize = SarType.Hero.fontSize * 0.8f))
    Spacer(Modifier.height(6.dp))
    Text("$label opens for $minutes min, then the block screen returns.", style = SarType.BodySecondary)
    Spacer(Modifier.height(20.dp))
    when (effort) {
        EmergencyEffort.WAIT -> {
            val progress = remember { Animatable(0f) }
            LaunchedEffect(Unit) { progress.animateTo(1f, tween(waitSec * 1000, easing = LinearEasing)) }
            val left = (waitSec * (1f - progress.value) + 0.99f).toInt()
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                ProgressRing(progress.value, 150.dp, color = SarColors.Warn) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (left > 0) "$left" else "✓", style = SarType.Huge)
                        Text(if (left > 0) "SECONDS" else "READY", style = SarType.Section)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            SarButton("Open $label · $minutes min", onDone, Modifier.fillMaxWidth(), enabled = progress.value >= 1f)
        }
        EmergencyEffort.PHRASE -> {
            var typed by remember { mutableStateOf("") }
            Text("Type exactly:", style = SarType.Meta)
            Spacer(Modifier.height(6.dp))
            Text("“$phrase”", style = SarType.Body.copy(color = SarColors.Warn))
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = typed,
                onValueChange = { typed = it.take(120) },
                textStyle = SarType.Body,
                cursorBrush = SolidColor(SarColors.Cyan),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                modifier = Modifier.fillMaxWidth().background(SarColors.BgSurface, SarShapes.Input)
                    .border(1.dp, SarColors.Cyan.copy(alpha = 0.6f), SarShapes.Input).padding(16.dp),
            )
            Spacer(Modifier.height(16.dp))
            SarButton("Open $label · $minutes min", onDone, Modifier.fillMaxWidth(), enabled = ModeValidator.phraseMatches(phrase, typed))
        }
    }
}
