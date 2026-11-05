package com.saurabh.sar.ui.screens

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.core.model.ScheduleEntry
import com.saurabh.sar.core.model.EmergencyEffort
import com.saurabh.sar.core.modes.ModeValidator
import com.saurabh.sar.core.schedule.ScheduleCalculator
import com.saurabh.sar.ui.ModeDraft
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.Screen
import com.saurabh.sar.ui.components.BUILTIN_IMAGES
import com.saurabh.sar.ui.components.BuiltinArt
import com.saurabh.sar.ui.components.ButtonKind
import com.saurabh.sar.ui.components.MODE_ICON_KEYS
import com.saurabh.sar.ui.components.ModeImage
import com.saurabh.sar.ui.components.NavRow
import com.saurabh.sar.ui.components.SarButton
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.SarDialog
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.components.SarTextField
import com.saurabh.sar.ui.components.SectionLabel
import com.saurabh.sar.ui.components.Segmented
import com.saurabh.sar.ui.components.TopBar
import com.saurabh.sar.ui.components.modeIcon
import com.saurabh.sar.ui.formatMinuteOfDay
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarShapes
import com.saurabh.sar.ui.theme.SarType
import kotlinx.coroutines.launch

/** Figma 04 Edit mode (FSD 3.1–3.3). Also used to create and duplicate. */
@Composable
fun EditModeScreen(nav: Nav, screen: Screen.EditMode) {
    val app = LocalContext.current.applicationContext as SarApp
    val context = LocalContext.current
    val modes by app.repository.modes.collectAsState(initial = null)
    val active by app.controller.active.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(screen, modes != null) {
        val all = modes ?: return@LaunchedEffect
        if (nav.draftFor == screen && nav.draft != null) return@LaunchedEffect
        val srcId = screen.modeId ?: screen.duplicateOf
        val src = all.firstOrNull { it.mode.id == srcId }
        nav.draft = when {
            src == null -> ModeDraft.blank(all.size)
            screen.duplicateOf != null -> ModeDraft(
                src.mode.copy(id = 0, name = (src.mode.name + " copy").take(30), sortOrder = all.size),
                src.schedule.map { it.copy(id = 0, modeId = 0) }, src.rules.map { it.copy(modeId = 0) },
            )
            else -> ModeDraft(src.mode, src.schedule, src.rules)
        }
        nav.draftFor = screen
    }
    val draft = nav.draft?.takeIf { nav.draftFor == screen } ?: run {
        Box(Modifier.fillMaxSize()); return
    }
    val all = modes.orEmpty()
    val isNew = draft.id == 0L
    val running = active?.mode?.id == draft.id && !isNew
    val lockedRunning = running && active?.run?.locked == true
    var problems by remember { mutableStateOf<List<String>>(emptyList()) }
    var confirmManual by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showImages by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch { app.images.import(uri)?.let { draft.mode = draft.mode.copy(imagePath = it) } }
    }

    fun close() { nav.draft = null; nav.draftFor = null; nav.pop() }

    fun save() {
        val others = all.filter { it.mode.id != draft.id }
        val list = ModeValidator.validate(draft.mode, draft.schedule.toList(), draft.rules.values.toList(), others.map { it.mode.name })
        problems = list.map { it.message }
        if (list.isNotEmpty()) return
        scope.launch {
            app.repository.saveMode(draft.toBundle())
            app.images.prune(app.repository.allModes().mapNotNull { it.imagePath }.filterNot { it.startsWith("builtin:") }.toSet())
            app.controller.onModesChanged()
            close()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            TopBar(
                title = if (isNew) "New mode" else "Edit mode",
                subtitle = draft.mode.name.ifBlank { null },
                onBack = ::close,
                action = {
                    Text("SAVE", style = SarType.Button, color = SarColors.Cyan, modifier = Modifier.clip(SarShapes.Chip).clickable { save() }.padding(10.dp))
                },
            )
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
            if (problems.isNotEmpty()) {
                SarCard(Modifier.fillMaxWidth(), tint = SarColors.Danger.copy(alpha = 0.08f), borderColor = SarColors.Danger.copy(alpha = 0.6f)) {
                    problems.forEach { Text("• $it", style = SarType.Body, color = SarColors.Danger) }
                }
                Spacer(Modifier.height(12.dp))
            }
            SectionLabel("Name")
            SarTextField(draft.mode.name, { draft.mode = draft.mode.copy(name = it) }, placeholder = "e.g. Self Study", maxLength = 30)
            Spacer(Modifier.height(14.dp))

            SectionLabel("Icon")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MODE_ICON_KEYS.forEach { k ->
                    val on = draft.mode.icon == k
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (on) SarColors.Cyan.copy(alpha = 0.12f) else SarColors.BgSurface)
                            .border(1.dp, if (on) SarColors.Cyan else SarColors.LineSubtle, RoundedCornerShape(12.dp))
                            .clickable { draft.mode = draft.mode.copy(icon = k) },
                        contentAlignment = Alignment.Center,
                    ) { SarIcon(modeIcon(k), if (on) SarColors.Cyan else SarColors.TextSecondary, 20.dp) }
                }
            }
            Spacer(Modifier.height(14.dp))

            SectionLabel("Type")
            Segmented(
                listOf("Manual", "Scheduled"),
                if (draft.mode.type == ModeType.MANUAL) 0 else 1,
                { i ->
                    if (i == 0 && draft.mode.type == ModeType.SCHEDULED) {
                        if (draft.schedule.isEmpty()) draft.mode = draft.mode.copy(type = ModeType.MANUAL) else confirmManual = true
                    } else if (i == 1) {
                        draft.mode = draft.mode.copy(type = ModeType.SCHEDULED)
                        if (draft.schedule.isEmpty()) draft.schedule.add(ScheduleEntry(daysMask = ScheduleEntry.MON_FRI, startMinute = 9 * 60, endMinute = 18 * 60))
                    }
                },
                Modifier.fillMaxWidth(),
                icons = listOf(R.drawable.ic_play, R.drawable.ic_calendar),
                enabled = !lockedRunning,
            )
            if (lockedRunning) Text("The type can't change while this mode is running and locked.", style = SarType.Meta, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(14.dp))

            if (draft.mode.type == ModeType.SCHEDULED) {
                SectionLabel("Schedule")
                draft.schedule.forEachIndexed { index, entry ->
                    ScheduleCard(entry, onChange = { draft.schedule[index] = it }, onRemove = if (draft.schedule.size > 1) ({ draft.schedule.removeAt(index); Unit }) else null)
                    Spacer(Modifier.height(10.dp))
                }
                Text("+ ADD ANOTHER TIME", style = SarType.Button.copy(fontSize = SarType.Button.fontSize * 0.85f), color = SarColors.Cyan,
                    modifier = Modifier.clickable { draft.schedule.add(ScheduleEntry(daysMask = ScheduleEntry.SAT_SUN, startMinute = 10 * 60, endMinute = 12 * 60)) }.padding(vertical = 8.dp))
                val overlapping = all.filter { it.mode.id != draft.id && it.mode.type == ModeType.SCHEDULED && ScheduleCalculator.overlaps(draft.schedule.toList(), it.schedule) }
                if (overlapping.isNotEmpty()) {
                    Text("Overlaps with ${overlapping.joinToString { it.mode.name }}. The one that starts first keeps running; you'll be asked about the other.",
                        style = SarType.Meta, color = SarColors.Warn, modifier = Modifier.padding(vertical = 4.dp))
                }
                Spacer(Modifier.height(10.dp))
            }

            SectionLabel("Block screen")
            SarCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).clickable { showImages = !showImages }) {
                        ModeImage(draft.mode.imagePath, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MESSAGE", style = SarType.Section)
                        Text(draft.mode.message.ifBlank { "—" }, style = SarType.Body)
                    }
                }
                Spacer(Modifier.height(12.dp))
                SarTextField(draft.mode.message, { draft.mode = draft.mode.copy(message = it) }, placeholder = "Block-screen message", singleLine = false, maxLength = 120)
                Text("${draft.mode.message.length} / 120", style = SarType.Meta, modifier = Modifier.padding(top = 4.dp))
                if (showImages) {
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BUILTIN_IMAGES.forEach { k ->
                            val on = draft.mode.imagePath == "builtin:$k"
                            Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                                .border(2.dp, if (on) SarColors.Cyan else SarColors.LineSubtle, RoundedCornerShape(12.dp))
                                .clickable { draft.mode = draft.mode.copy(imagePath = "builtin:$k") }) {
                                BuiltinArt(k, Modifier.fillMaxSize())
                            }
                        }
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(SarColors.BgBase)
                            .border(1.dp, SarColors.LineSubtle, RoundedCornerShape(12.dp))
                            .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            contentAlignment = Alignment.Center) { SarIcon(R.drawable.ic_image, SarColors.Cyan, 22.dp) }
                    }
                } else {
                    Text("Tap the image to change it", style = SarType.Meta, color = SarColors.Cyan, modifier = Modifier.padding(top = 8.dp).clickable { showImages = true })
                }
            }
            Spacer(Modifier.height(14.dp))

            val blocked = draft.rules.values.count { it.type == RuleType.BLOCK }
            val limited = draft.rules.size - blocked
            SarCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                NavRow("Apps & rules", icon = R.drawable.ic_layers, iconTint = SarColors.Cyan,
                    subtitle = "${draft.rules.size} apps · $blocked blocked · $limited limited") { nav.push(Screen.AppsRules) }
            }
            Spacer(Modifier.height(10.dp))
            val m = draft.mode
            SarCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                NavRow("Emergency time", icon = R.drawable.ic_zap, iconTint = SarColors.Warn,
                    subtitle = if (!m.emergencyEnabled) "Off" else "On · ${m.emergencyMinutes} min · ${m.emergencyUses} per run · " +
                        if (m.emergencyEffort == EmergencyEffort.WAIT) "wait ${m.emergencyWaitSec} s" else "type a phrase") { nav.push(Screen.Emergency) }
            }
            Spacer(Modifier.height(20.dp))
            if (!isNew) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SarButton("Duplicate", { nav.draft = null; nav.draftFor = null; nav.pop(); nav.push(Screen.EditMode(null, duplicateOf = draft.id)) },
                        Modifier.weight(1f), kind = ButtonKind.GHOST, icon = R.drawable.ic_copy)
                    SarButton("Delete", { confirmDelete = true }, Modifier.weight(1f), kind = ButtonKind.DANGER, icon = R.drawable.ic_trash_2, enabled = !running)
                }
                if (running) Text("A running mode can't be deleted; it must end first.", style = SarType.Meta, modifier = Modifier.padding(top = 6.dp))
            }
            Spacer(Modifier.height(20.dp))
            SarButton("Save", ::save, Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }

    if (confirmManual) SarDialog("Make it manual?", "The schedule will be removed.", "Remove schedule", {
        draft.schedule.clear(); draft.mode = draft.mode.copy(type = ModeType.MANUAL); confirmManual = false
    }, { confirmManual = false }, danger = true)

    if (confirmDelete) SarDialog("Delete ${draft.mode.name}?", "Its rules and schedule are deleted. History stays.", "Delete", {
        confirmDelete = false
        scope.launch { app.repository.deleteMode(draft.id); app.controller.onModesChanged(); close() }
    }, { confirmDelete = false }, danger = true)
}

@Composable
private fun ScheduleCard(entry: ScheduleEntry, onChange: (ScheduleEntry) -> Unit, onRemove: (() -> Unit)?) {
    val context = LocalContext.current
    SarCard(Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, l ->
                val on = entry.hasDay(i)
                Box(
                    Modifier.weight(1f).height(36.dp).clip(CircleShape)
                        .background(if (on) SarColors.Cyan else SarColors.BgBase)
                        .border(1.dp, if (on) SarColors.Cyan else SarColors.LineSubtle, CircleShape)
                        .clickable { onChange(entry.copy(daysMask = entry.daysMask xor (1 shl i))) },
                    contentAlignment = Alignment.Center,
                ) { Text(l, style = SarType.Chip.copy(fontSize = SarType.Chip.fontSize * 1.3f), color = if (on) SarColors.OnAccent else SarColors.TextSecondary) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeBox("Start", entry.startMinute, Modifier.weight(1f)) {
                TimePickerDialog(context, { _, h, m -> onChange(entry.copy(startMinute = h * 60 + m)) }, entry.startMinute / 60, entry.startMinute % 60, true).show()
            }
            Text("→", style = SarType.Body, color = SarColors.TextSecondary, modifier = Modifier.padding(horizontal = 10.dp))
            TimeBox("End", entry.endMinute, Modifier.weight(1f)) {
                TimePickerDialog(context, { _, h, m -> onChange(entry.copy(endMinute = h * 60 + m)) }, entry.endMinute / 60, entry.endMinute % 60, true).show()
            }
        }
        if (entry.isOvernight) Text("Runs past midnight", style = SarType.Meta, color = SarColors.Violet, modifier = Modifier.padding(top = 8.dp))
        if (onRemove != null) Text("REMOVE", style = SarType.Chip, color = SarColors.Danger, modifier = Modifier.padding(top = 10.dp).clickable(onClick = onRemove))
    }
}

@Composable
private fun TimeBox(label: String, minute: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(SarShapes.Input).background(SarColors.BgBase).border(1.dp, SarColors.LineSubtle, SarShapes.Input)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label.uppercase(), style = SarType.Section.copy(fontSize = SarType.Section.fontSize * 0.85f))
        Text(formatMinuteOfDay(minute), style = SarType.BigNumber.copy(fontSize = SarType.BigNumber.fontSize * 0.75f))
    }
}
