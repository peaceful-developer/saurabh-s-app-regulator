package com.saurabh.sar.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saurabh.sar.BuildConfig
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.data.SarSettings
import com.saurabh.sar.platform.Backup
import com.saurabh.sar.platform.Permissions
import com.saurabh.sar.platform.SarPermission
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.Screen
import com.saurabh.sar.ui.components.Divider
import com.saurabh.sar.ui.components.NavRow
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.SarDialog
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.components.SectionLabel
import com.saurabh.sar.ui.components.SelectChip
import com.saurabh.sar.ui.formatClock
import com.saurabh.sar.ui.rememberNow
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarType
import kotlinx.coroutines.launch

private val WARNING_PRESETS = listOf(listOf(5, 1), listOf(10, 5, 1), listOf(1), emptyList())

/** Figma 09 Settings (FSD 9.3). */
@Composable
fun SettingsScreen(nav: Nav) {
    val app = LocalContext.current.applicationContext as SarApp
    val context = LocalContext.current
    val active by app.controller.active.collectAsState()
    val settings by app.settings.settings.collectAsState(initial = SarSettings())
    val now by rememberNow()
    val scope = rememberCoroutineScope()
    var showPerms by remember { mutableStateOf(false) }
    var showWarnings by remember { mutableStateOf(false) }
    var showRetention by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val missing = remember(now, showPerms) { Permissions.missing(app) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching { Backup.export(app, uri) }.isSuccess
            Toast.makeText(context, if (ok) "Modes exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val r = runCatching { Backup.import(app, uri) }
            Toast.makeText(context, r.fold({ "Imported $it modes" }, { "Import failed: ${it.message}" }), Toast.LENGTH_LONG).show()
        }
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { showPerms = !showPerms; showPerms = true }

    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        Text("SETTINGS", style = SarType.ScreenTitle, modifier = Modifier.padding(top = 16.dp, bottom = 18.dp))

        val locked = active?.takeIf { it.run.locked }
        SarCard(
            Modifier.fillMaxWidth().alpha(if (locked != null) 1f else 0.55f),
            tint = SarColors.Violet.copy(alpha = if (locked != null) 0.12f else 0.04f),
            borderColor = SarColors.Violet.copy(alpha = if (locked != null) 0.6f else 0.25f),
            glowColor = if (locked != null) SarColors.Violet else null,
            onClick = if (locked != null) ({ nav.push(Screen.FreeMode) }) else null,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(SarColors.Violet.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    SarIcon(R.drawable.ic_lock_open, SarColors.Violet, 20.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("FREE A MODE", style = SarType.CardTitle)
                    Text(locked?.let { "${it.mode.name} is locked until ${it.run.plannedEndAt?.let(::formatClock) ?: "—"}" } ?: "No locked mode right now", style = SarType.Meta)
                }
                SarIcon(R.drawable.ic_chevron_right, SarColors.Violet, 18.dp)
            }
        }
        Spacer(Modifier.height(18.dp))

        SectionLabel("Permissions")
        SarCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            NavRow("Permissions", if (missing.isEmpty()) "All granted" else "${missing.size} missing", R.drawable.ic_eye,
                valueColor = if (missing.isEmpty()) SarColors.Ok else SarColors.Danger) { showPerms = !showPerms }
            if (showPerms) SarPermission.entries.forEach { p ->
                Divider()
                val granted = p !in missing
                NavRow(p.title, if (granted) "Granted" else "Fix", if (granted) R.drawable.ic_circle_check else R.drawable.ic_triangle_alert,
                    valueColor = if (granted) SarColors.Ok else SarColors.Danger, iconTint = if (granted) SarColors.Ok else SarColors.Danger) {
                    if (p == SarPermission.NOTIFICATIONS && Build.VERSION.SDK_INT >= 33 && !granted) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else runCatching { context.startActivity(Permissions.settingsIntent(context, p)) }
                }
            }
            if (showPerms) {
                Divider()
                NavRow("App info (restricted settings)", null, R.drawable.ic_shield_check, subtitle = "If Accessibility is greyed out: ⋮ → Allow restricted settings") {
                    context.startActivity(Permissions.appInfoIntent(context))
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        SectionLabel("Alerts")
        SarCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            NavRow("Minutes-left warnings", settings.warningMinutes.ifEmpty { null }?.joinToString(" · ", postfix = " min") ?: "Off", R.drawable.ic_bell) { showWarnings = !showWarnings }
            if (showWarnings) Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WARNING_PRESETS.forEach { p ->
                    SelectChip(if (p.isEmpty()) "Off" else p.joinToString("·"), settings.warningMinutes == p, { scope.launch { app.settings.setWarningMinutes(p) } })
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        SectionLabel("History")
        SarCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            NavRow("Keep history", "${settings.historyRetentionDays} days", R.drawable.ic_chart_no_axes_column) { showRetention = !showRetention }
            if (showRetention) Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(7, 30, 90).forEach { d ->
                    SelectChip("$d days", settings.historyRetentionDays == d, { scope.launch { app.settings.setRetentionDays(d) } })
                }
            }
            Divider()
            NavRow("Clear history", null, R.drawable.ic_trash_2, color = SarColors.Danger, iconTint = SarColors.Danger) { confirmClear = true }
        }
        Spacer(Modifier.height(14.dp))

        SectionLabel("Backup")
        SarCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
            NavRow("Export modes", null, R.drawable.ic_upload) { exportLauncher.launch("sar-modes.json") }
            Divider()
            NavRow("Import modes", null, R.drawable.ic_download) { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        }
        Spacer(Modifier.height(18.dp))
        Text("SAR ${BuildConfig.VERSION_NAME} · all data stays on this phone", style = SarType.Meta)
        Spacer(Modifier.height(24.dp))
    }

    if (confirmClear) SarDialog("Clear history?", "Finished runs and their app times are deleted. The running mode is kept.", "Clear", {
        confirmClear = false; scope.launch { app.repository.clearHistory() }
    }, { confirmClear = false }, danger = true)
}
