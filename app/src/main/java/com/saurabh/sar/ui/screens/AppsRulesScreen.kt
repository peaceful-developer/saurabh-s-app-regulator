package com.saurabh.sar.ui.screens

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.model.AppCategory
import com.saurabh.sar.core.model.AppRule
import com.saurabh.sar.core.model.InstalledApp
import com.saurabh.sar.core.model.RuleType
import com.saurabh.sar.ui.ModeDraft
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.components.AppIcon
import com.saurabh.sar.ui.components.SarButton
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.SarCheckbox
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.components.SarTextField
import com.saurabh.sar.ui.components.SarToggle
import com.saurabh.sar.ui.components.SectionLabel
import com.saurabh.sar.ui.components.Segmented
import com.saurabh.sar.ui.components.Stepper
import com.saurabh.sar.ui.components.Tag
import com.saurabh.sar.ui.components.TopBar
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun categoryLabel(c: AppCategory) = when (c) {
    AppCategory.SOCIAL -> "Social"
    AppCategory.GAME -> "Games"
    AppCategory.VIDEO -> "Video"
    AppCategory.AUDIO -> "Music & audio"
    AppCategory.PRODUCTIVITY -> "Productivity"
    AppCategory.OTHER -> "Other apps"
}

private fun stepAllowance(cur: Int, up: Boolean): Int {
    val step = if (cur < 15 || (!up && cur <= 15)) 1 else if (cur < 60 || (!up && cur <= 60)) 5 else 15
    return (if (up) cur + step else cur - step).coerceIn(AppRule.ALLOWANCE_MIN, AppRule.ALLOWANCE_MAX)
}

/** Figma 05 Apps & rules (FSD 4). Edits the shared [ModeDraft]. */
@Composable
fun AppsRulesScreen(nav: Nav) {
    val app = LocalContext.current.applicationContext as SarApp
    val draft = nav.draft ?: run { nav.pop(); return }
    val apps by produceState<List<InstalledApp>?>(null) { value = withContext(Dispatchers.Default) { app.installedApps.list() } }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(draft.rules.keys.firstOrNull()) }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        Column(Modifier.padding(horizontal = 24.dp)) {
            TopBar("Apps & rules", "${draft.mode.name.ifBlank { "New mode" }} · ${draft.rules.size} selected", onBack = { nav.pop() })
            SarTextField(query, { query = it }, placeholder = "Search apps", maxLength = 40)
            Spacer(Modifier.height(12.dp))
        }
        val list = apps
        if (list == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Loading apps…", style = SarType.BodySecondary) }
        } else {
            val q = query.trim().lowercase()
            val filtered = list.filter { q.isEmpty() || it.label.lowercase().contains(q) || it.packageName.contains(q) }
            // Selected apps first, then by category, then alphabetically.
            val groups = filtered.groupBy { if (it.packageName in draft.rules) null else it.category }
                .toList().sortedBy { (c, _) -> c?.ordinal ?: -1 }
            LazyColumn(Modifier.weight(1f).padding(horizontal = 24.dp)) {
                groups.forEach { (cat, appsInGroup) ->
                    item(key = "h-${cat?.name ?: "sel"}") { SectionLabel(cat?.let(::categoryLabel) ?: "In this mode") }
                    items(appsInGroup, key = { it.packageName }) { a ->
                        RuleRow(a, draft, expanded == a.packageName, onExpand = { expanded = if (expanded == a.packageName) null else a.packageName })
                        Spacer(Modifier.height(10.dp))
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
        Box(Modifier.padding(horizontal = 24.dp, vertical = 12.dp).navigationBarsPadding()) {
            SarButton("Done · ${draft.rules.size} apps", { nav.pop() }, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RuleRow(a: InstalledApp, draft: ModeDraft, expanded: Boolean, onExpand: () -> Unit) {
    val rule = draft.rules[a.packageName]
    val selected = rule != null
    SarCard(
        Modifier.fillMaxWidth().alpha(if (selected) 1f else 0.6f),
        borderColor = if (selected && expanded) SarColors.Cyan.copy(alpha = 0.6f) else SarColors.LineSubtle,
        tint = if (selected && expanded) SarColors.Cyan.copy(alpha = 0.04f) else null,
        onClick = { if (selected) onExpand() else { draft.rules[a.packageName] = AppRule(packageName = a.packageName, type = RuleType.BLOCK); if (!expanded) onExpand() } },
        padding = androidx.compose.foundation.layout.PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SarCheckbox(selected, { on ->
                if (on) draft.rules[a.packageName] = AppRule(packageName = a.packageName, type = RuleType.BLOCK) else draft.rules.remove(a.packageName)
            })
            Spacer(Modifier.width(12.dp))
            AppIcon(a.packageName, a.label, 32.dp)
            Spacer(Modifier.width(12.dp))
            Text(a.label, style = SarType.RowTitle, modifier = Modifier.weight(1f), maxLines = 1)
            if (rule != null) {
                if (rule.type == RuleType.BLOCK) Tag("Block", SarColors.Danger)
                else Tag("Limit · ${rule.allowanceMin}m", SarColors.Violet)
            }
        }
        if (rule != null && expanded) {
            Spacer(Modifier.height(14.dp))
            Segmented(
                listOf("Block", "Limit"), if (rule.type == RuleType.BLOCK) 0 else 1,
                { i ->
                    draft.rules[a.packageName] = if (i == 0) rule.copy(type = RuleType.BLOCK)
                    else rule.copy(type = RuleType.LIMIT, allowanceMin = rule.allowanceMin ?: 15)
                },
                Modifier.fillMaxWidth(), icons = listOf(R.drawable.ic_ban, R.drawable.ic_clock),
            )
            if (rule.type == RuleType.LIMIT) {
                val allowance = rule.allowanceMin ?: 15
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Allowed time", style = SarType.Body, modifier = Modifier.weight(1f))
                    Stepper("$allowance min",
                        onMinus = { draft.rules[a.packageName] = rule.copy(allowanceMin = stepAllowance(allowance, false)) },
                        onPlus = { draft.rules[a.packageName] = rule.copy(allowanceMin = stepAllowance(allowance, true)) })
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Session cap", style = SarType.Body)
                        Text(if (rule.hasSessionCap) "Max ${rule.capMin} min in one go, then a ${rule.breakMin} min break" else "No limit on one continuous stretch",
                            style = SarType.Meta)
                    }
                    SarToggle(rule.hasSessionCap, { on ->
                        draft.rules[a.packageName] = if (on) rule.copy(capMin = minOf(10, allowance), breakMin = 5) else rule.copy(capMin = null, breakMin = null)
                    })
                }
                if (rule.hasSessionCap) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Max in one go", style = SarType.Meta, modifier = Modifier.weight(1f))
                        Stepper("${rule.capMin} min",
                            onMinus = { draft.rules[a.packageName] = rule.copy(capMin = (rule.capMin!! - 1).coerceAtLeast(1)) },
                            onPlus = { draft.rules[a.packageName] = rule.copy(capMin = (rule.capMin!! + 1).coerceAtMost(720)) })
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Break", style = SarType.Meta, modifier = Modifier.weight(1f))
                        Stepper("${rule.breakMin} min",
                            onMinus = { draft.rules[a.packageName] = rule.copy(breakMin = (rule.breakMin!! - 1).coerceAtLeast(1)) },
                            onPlus = { draft.rules[a.packageName] = rule.copy(breakMin = (rule.breakMin!! + 1).coerceAtMost(240)) })
                    }
                }
            }
        }
    }
}
