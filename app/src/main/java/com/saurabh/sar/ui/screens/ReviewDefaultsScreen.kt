package com.saurabh.sar.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.model.InstalledApp
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.core.modes.DefaultModes
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.components.AppIcon
import com.saurabh.sar.ui.components.SarButton
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.SarCheckbox
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.components.Tag
import com.saurabh.sar.ui.components.TopBar
import com.saurabh.sar.ui.components.modeIcon
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Onboarding step 3 (FSD 3.4, 9.2): the five default modes with suggested apps to review. */
@Composable
fun ReviewDefaultsScreen(nav: Nav) {
    val app = LocalContext.current.applicationContext as SarApp
    val scope = rememberCoroutineScope()
    val data by produceState<Pair<List<InstalledApp>, Int>?>(null) {
        value = withContext(Dispatchers.Default) { app.installedApps.list(refresh = true) } to app.repository.modeCount()
    }
    /** "mode|package" → kept. */
    val off = remember { mutableStateMapOf<String, Boolean>() }
    var saving by remember { mutableStateOf(false) }

    suspend fun finish() {
        app.settings.setOnboardingDone(true)
        nav.reset()
    }

    val d = data ?: run { Box(Modifier.fillMaxSize()); return }
    val (apps, existing) = d
    val defaults = remember(apps) { DefaultModes.build(apps) }
    val labels = remember(apps) { apps.associate { it.packageName to it.label } }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        TopBar("Your modes", "Suggested from your apps · untick anything you don't want")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (existing > 0) {
                Text("You already have $existing modes, so no defaults will be added.", style = SarType.BodySecondary)
            } else defaults.forEach { b ->
                SarCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SarIcon(modeIcon(b.mode.icon), SarColors.Cyan, 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(b.mode.name.uppercase(), style = SarType.CardTitle, modifier = Modifier.weight(1f))
                        Tag(if (b.schedule.isEmpty()) "Manual" else "Auto", if (b.schedule.isEmpty()) SarColors.TextSecondary else SarColors.Violet)
                    }
                    if (b.schedule.isNotEmpty()) {
                        val s = b.schedule.first()
                        Text("${daysText(s.daysMask)} · ${com.saurabh.sar.ui.formatMinuteOfDay(s.startMinute)}–${com.saurabh.sar.ui.formatMinuteOfDay(s.endMinute)}", style = SarType.Meta)
                    }
                    if (b.rules.isEmpty()) Text("No matching apps found. You can add apps later.", style = SarType.Meta, modifier = Modifier.padding(top = 8.dp))
                    b.rules.forEach { r ->
                        val key = "${b.mode.name}|${r.packageName}"
                        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SarCheckbox(off[key] != true, { off[key] = !it })
                            Spacer(Modifier.width(10.dp))
                            AppIcon(r.packageName, labels[r.packageName] ?: r.packageName, 28.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(labels[r.packageName] ?: r.packageName, style = SarType.Body, modifier = Modifier.weight(1f))
                            if (r.type == RuleType.BLOCK) Tag("Block", SarColors.Danger) else Tag("Limit · ${r.allowanceMin}m", SarColors.Violet)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        SarButton(if (existing > 0) "Go to Home" else "Create these modes", {
            if (saving) return@SarButton
            saving = true
            scope.launch {
                val settings = app.settings.current()
                if (existing == 0 && !settings.defaultsCreated) {
                    val kept = defaults.map { b -> b.copy(rules = b.rules.filter { off["${b.mode.name}|${it.packageName}"] != true }) }
                    app.repository.saveModes(kept)
                    app.settings.setDefaultsCreated(true)
                    app.controller.onModesChanged()
                }
                finish()
            }
        }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}
