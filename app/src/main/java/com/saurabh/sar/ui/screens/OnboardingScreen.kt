package com.saurabh.sar.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.platform.Permissions
import com.saurabh.sar.platform.SarPermission
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.Screen
import com.saurabh.sar.ui.components.SarButton
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.SarIcon
import com.saurabh.sar.ui.components.glow
import com.saurabh.sar.ui.rememberNow
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarType

private fun iconFor(p: SarPermission) = when (p) {
    SarPermission.ACCESSIBILITY -> R.drawable.ic_eye
    SarPermission.USAGE_ACCESS -> R.drawable.ic_chart_no_axes_column
    SarPermission.OVERLAY -> R.drawable.ic_layers
    SarPermission.NOTIFICATIONS -> R.drawable.ic_bell
    SarPermission.BATTERY -> R.drawable.ic_battery
}

private val ORDER = listOf(SarPermission.USAGE_ACCESS, SarPermission.ACCESSIBILITY, SarPermission.OVERLAY, SarPermission.NOTIFICATIONS, SarPermission.BATTERY)

/** Figma 10 Onboarding (FSD 9.2): welcome, then one screen per permission with a live check. */
@Composable
fun OnboardingScreen(nav: Nav) {
    val app = LocalContext.current.applicationContext as SarApp
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(-1) } // -1 = welcome
    var why by remember { mutableStateOf(false) }
    val tick by rememberNow(1_000) // live "granted" check while the screen is visible
    val granted = remember(tick, step) { ORDER.associateWith { Permissions.isGranted(app, it) } }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun next() { why = false; if (step < ORDER.lastIndex) step++ else nav.reset(Screen.ReviewDefaults) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        if (step < 0) {
            Spacer(Modifier.weight(1f))
            Text("SAR", style = SarType.Logo.copy(fontSize = SarType.Logo.fontSize * 2))
            Text("SAURABH'S APP REGULATOR", style = SarType.Section)
            Spacer(Modifier.height(20.dp))
            Text("Limit how long chosen apps can be used while a mode is on, and block them when time is up.", style = SarType.Body.copy(fontSize = SarType.Body.fontSize * 1.2f))
            Spacer(Modifier.height(10.dp))
            Text("Everything stays on this phone. Next: five permissions SAR needs to block reliably.", style = SarType.BodySecondary)
            Spacer(Modifier.weight(1f))
            SarButton("Get started", { step = 0 }, Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            return@Column
        }
        val p = ORDER[step]
        val ok = granted[p] == true
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ORDER.indices.forEach { i ->
                Box(Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (i <= step) SarColors.Cyan else SarColors.LineSubtle))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("STEP ${step + 1} OF ${ORDER.size}", style = SarType.Section, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    listOf(1f, 0.78f, 0.56f).forEachIndexed { i, f ->
                        drawCircle(SarColors.Cyan.copy(alpha = 0.10f + i * 0.08f), size.minDimension / 2 * f, style = Stroke(2f))
                    }
                }
                Box(
                    Modifier.size(84.dp).glow(SarColors.Cyan, 24.dp, CircleShape).clip(CircleShape).background(SarColors.BgSurface)
                        .border(1.5.dp, if (ok) SarColors.Ok else SarColors.Cyan, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { SarIcon(if (ok) R.drawable.ic_check else iconFor(p), if (ok) SarColors.Ok else SarColors.Cyan, 36.dp) }
            }
            Spacer(Modifier.height(16.dp))
            Text("ALLOW ${p.title.uppercase()}", style = SarType.ScreenTitle.copy(fontSize = SarType.ScreenTitle.fontSize * 0.8f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(p.why, style = SarType.BodySecondary.copy(fontSize = SarType.Body.fontSize), textAlign = TextAlign.Center)
            if (p == SarPermission.ACCESSIBILITY && !ok) {
                Spacer(Modifier.height(10.dp))
                Text("On Samsung: Settings → Accessibility → Installed apps → SAR → On. If it's greyed out, open App info → ⋮ → Allow restricted settings, then try again.",
                    style = SarType.Meta, color = SarColors.Warn, textAlign = TextAlign.Center)
                Text("OPEN APP INFO", style = SarType.Chip, color = SarColors.Cyan,
                    modifier = Modifier.padding(top = 8.dp).clickable { context.startActivity(Permissions.appInfoIntent(context)) }.padding(8.dp))
            }
            if (why) {
                Spacer(Modifier.height(10.dp))
                Text(whyText(p), style = SarType.Meta, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(18.dp))
            SarCard(Modifier.fillMaxWidth()) {
                ORDER.forEachIndexed { i, q ->
                    val done = granted[q] == true
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(18.dp).clip(CircleShape).background(if (done) SarColors.Ok else SarColors.BgSurface)
                                .border(1.5.dp, when { done -> SarColors.Ok; i == step -> SarColors.Cyan; else -> SarColors.LineSubtle }, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { if (done) SarIcon(R.drawable.ic_check, SarColors.OnAccent, 12.dp) }
                        Spacer(Modifier.width(12.dp))
                        Text(q.title, style = SarType.Body, color = if (i == step) SarColors.Cyan else if (done) SarColors.TextPrimary else SarColors.TextSecondary)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (ok) SarButton("Granted · next", ::next, Modifier.fillMaxWidth())
        else SarButton("Open settings", {
            if (p == SarPermission.NOTIFICATIONS && Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else runCatching { context.startActivity(Permissions.settingsIntent(context, p)) }
        }, Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("WHY IS THIS NEEDED?", style = SarType.Chip, color = SarColors.Cyan, modifier = Modifier.clickable { why = !why }.padding(8.dp))
            if (!ok) Text("SKIP FOR NOW", style = SarType.Chip, color = SarColors.TextSecondary, modifier = Modifier.clickable { next() }.padding(8.dp))
        }
    }
}

private fun whyText(p: SarPermission) = when (p) {
    SarPermission.ACCESSIBILITY -> "SAR only listens for \"which app just came to the front\". It does not read what's on screen, and it does nothing while no mode is running."
    SarPermission.USAGE_ACCESS -> "Used only when you open History, after a restart, or if blocking was off, to keep minutes accurate. Never on a timer."
    SarPermission.OVERLAY -> "Android needs this so SAR can bring the block screen up over another app instantly."
    SarPermission.NOTIFICATIONS -> "One quiet notification while a mode runs, plus 5- and 1-minute warnings and the scheduled-mode prompt."
    SarPermission.BATTERY -> "Without it Samsung may delay the alarms that start and end scheduled modes. SAR uses well under 1% battery a day."
}
