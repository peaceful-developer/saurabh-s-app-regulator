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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.challenge.AnswerResult
import com.saurabh.sar.core.challenge.MathChallenge
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.SarDialog
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.components.TopBar
import com.saurabh.sar.ui.components.glow
import com.saurabh.sar.ui.formatClock
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarShapes
import com.saurabh.sar.ui.theme.SarType
import kotlinx.coroutines.launch

/** Figma 07 Free a mode (FSD 7). Leaving the screen discards progress (state lives only here). */
@Composable
fun FreeModeScreen(nav: Nav) {
    val app = LocalContext.current.applicationContext as SarApp
    val active by app.controller.active.collectAsState()
    val scope = rememberCoroutineScope()
    val challenge = remember { MathChallenge() }
    var version by remember { mutableIntStateOf(0) } // bumps to recompose after each answer
    var answer by remember { mutableStateOf("") }
    var flash by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    val state = active

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        TopBar("Free a mode", "Settings", onBack = { nav.pop() })
        if (state == null || !state.run.locked) {
            SarCard(Modifier.fillMaxWidth()) {
                Text("No locked mode is running.", style = SarType.RowTitle)
                Text("An unlocked run never needs this: it has a plain Stop button.", style = SarType.Meta)
            }
            return@Column
        }
        version.let { }
        SarCard(Modifier.fillMaxWidth(), tint = SarColors.Violet.copy(alpha = 0.10f), borderColor = SarColors.Violet.copy(alpha = 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SarColors.Violet.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    SarIcon(R.drawable.ic_lock_open, SarColors.Violet, 18.dp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Ending ${state.mode.name} early", style = SarType.RowTitle)
                    Text("Locked until ${state.run.plannedEndAt?.let { formatClock(it) } ?: "—"} · solve 5 in a row", style = SarType.Meta)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("QUESTION ${(challenge.correctCount + 1).coerceAtMost(5)} OF 5", style = SarType.Section, modifier = Modifier.weight(1f))
            Text("${challenge.correctCount} CORRECT", style = SarType.Section, color = SarColors.Ok)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(5) { i ->
                val c = when {
                    i < challenge.correctCount -> SarColors.Ok
                    i == challenge.correctCount -> SarColors.Cyan
                    else -> SarColors.LineSubtle
                }
                Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(c))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(challenge.current.toString(), style = SarType.Huge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("= ?", style = SarType.Body.copy(color = SarColors.TextSecondary), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier.fillMaxWidth().height(64.dp).glow(SarColors.Cyan, 12.dp, SarShapes.Input).clip(SarShapes.Input)
                .background(SarColors.BgSurface).border(1.dp, SarColors.Cyan.copy(alpha = 0.7f), SarShapes.Input),
            contentAlignment = Alignment.Center,
        ) {
            Text(answer.ifEmpty { flash ?: "" } + "|", style = SarType.BigNumber.copy(color = if (answer.isEmpty() && flash != null) SarColors.Danger else SarColors.Cyan))
        }
        Spacer(Modifier.height(14.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✓")
        keys.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { k ->
                    val ok = k == "✓"
                    Box(
                        Modifier.weight(1f).height(56.dp)
                            .then(if (ok) Modifier.glow(SarColors.Cyan, 10.dp, SarShapes.Key) else Modifier)
                            .clip(SarShapes.Key)
                            .background(if (ok) SarColors.Cyan else SarColors.BgSurface)
                            .border(1.dp, if (ok) Color.Transparent else SarColors.LineSubtle, SarShapes.Key)
                            .clickable {
                                flash = null
                                when (k) {
                                    "⌫" -> answer = answer.dropLast(1)
                                    "✓" -> {
                                        val n = answer.toIntOrNull() ?: return@clickable
                                        when (challenge.submit(n)) {
                                            AnswerResult.CORRECT -> Unit
                                            AnswerResult.WRONG_NEW_SET -> flash = "Wrong · new set"
                                            AnswerResult.COMPLETED -> confirm = true
                                        }
                                        answer = ""
                                        version++
                                    }
                                    else -> if (answer.length < 5) answer += k
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (k) {
                            "⌫" -> SarIcon(R.drawable.ic_delete, SarColors.TextPrimary, 22.dp)
                            "✓" -> SarIcon(R.drawable.ic_check, SarColors.OnAccent, 24.dp)
                            else -> Text(k, style = SarType.BigNumber.copy(fontSize = SarType.BigNumber.fontSize * 0.8f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Text("A wrong answer starts a new set of 5.", style = SarType.Meta, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }

    if (confirm && state != null) {
        SarDialog(
            "End ${state.mode.name} now?",
            if (state.run.scheduled) "All blocks lift. ${state.mode.name} starts again at its next scheduled time." else "All blocks lift.",
            "End now",
            onConfirm = { confirm = false; scope.launch { app.controller.free(); nav.pop() } },
            onDismiss = { confirm = false; nav.pop() },
            danger = true,
        )
    }
}
