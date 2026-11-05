package com.saurabh.sar.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.data.SarSettings
import com.saurabh.sar.ui.components.SarBackground
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.screens.StartSheet
import com.saurabh.sar.ui.screens.AppsRulesScreen
import com.saurabh.sar.ui.screens.EditModeScreen
import com.saurabh.sar.ui.screens.EmergencySettingsScreen
import com.saurabh.sar.ui.screens.FreeModeScreen
import com.saurabh.sar.ui.screens.HistoryScreen
import com.saurabh.sar.ui.screens.HomeScreen
import com.saurabh.sar.ui.screens.ModesScreen
import com.saurabh.sar.ui.screens.OnboardingScreen
import com.saurabh.sar.ui.screens.ReviewDefaultsScreen
import com.saurabh.sar.ui.screens.SettingsScreen
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarType

@Composable
fun SarRoot() {
    val app = LocalContext.current.applicationContext as SarApp
    val nav = remember { Nav() }
    val settings by app.settings.settings.collectAsState(initial = null as SarSettings?)
    val s = settings ?: run { Box(Modifier.fillMaxSize().background(SarColors.BgBase)); return }

    LaunchedEffect(s.onboardingDone) {
        if (!s.onboardingDone && nav.top != Screen.Onboarding && nav.top != Screen.ReviewDefaults) nav.reset(Screen.Onboarding)
    }

    BackHandler(enabled = nav.stack.size > 1 || nav.tab != Tab.HOME) {
        if (!nav.pop()) nav.tab = Tab.HOME
    }

    SarBackground(accent = if (nav.tab == Tab.MODES || nav.top is Screen.FreeMode) SarColors.Violet else SarColors.Cyan) {
        when (val top = nav.top) {
            Screen.Tabs -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (nav.tab) {
                        Tab.HOME -> HomeScreen(nav)
                        Tab.MODES -> ModesScreen(nav)
                        Tab.HISTORY -> HistoryScreen()
                        Tab.SETTINGS -> SettingsScreen(nav)
                    }
                }
                BottomNav(nav.tab) { nav.tab = it }
            }
            is Screen.EditMode -> EditModeScreen(nav, top)
            Screen.AppsRules -> AppsRulesScreen(nav)
            Screen.Emergency -> EmergencySettingsScreen(nav)
            Screen.FreeMode -> FreeModeScreen(nav)
            Screen.Onboarding -> OnboardingScreen(nav)
            Screen.ReviewDefaults -> ReviewDefaultsScreen(nav)
        }
        nav.startSheetFor?.let { id -> StartSheet(id, nav) { nav.startSheetFor = null } }
    }
}

/** Bottom Nav (Figma Components): 76 high; the active tab gets a cyan 10 % pill and a glowing icon. */
@Composable
private fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    val items = listOf(
        Triple(Tab.HOME, "Home", R.drawable.ic_house),
        Triple(Tab.MODES, "Modes", R.drawable.ic_layers),
        Triple(Tab.HISTORY, "History", R.drawable.ic_chart_no_axes_column),
        Triple(Tab.SETTINGS, "Settings", R.drawable.ic_sliders_vertical),
    )
    Column(Modifier.fillMaxWidth().background(SarColors.BgSurface).navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SarColors.LineSubtle))
        Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            items.forEach { (tab, label, icon) ->
                val on = tab == current
                Column(
                    Modifier
                        .size(width = 76.dp, height = 60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) SarColors.Cyan.copy(alpha = 0.10f) else SarColors.BgSurface)
                        .then(if (on) Modifier.border(1.dp, SarColors.Cyan.copy(alpha = 0.25f), RoundedCornerShape(16.dp)) else Modifier)
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    SarIcon(icon, if (on) SarColors.Cyan else SarColors.TextSecondary, 22.dp)
                    Text(label.uppercase(), style = SarType.Chip.copy(fontSize = SarType.Chip.fontSize * 0.9f),
                        color = if (on) SarColors.Cyan else SarColors.TextSecondary, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}
