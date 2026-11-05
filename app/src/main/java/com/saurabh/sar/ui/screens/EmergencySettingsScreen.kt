package com.saurabh.sar.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.core.model.EmergencyEffort
import com.saurabh.sar.ui.Nav
import com.saurabh.sar.ui.components.SarButton
import com.saurabh.sar.ui.components.SarCard
import com.saurabh.sar.ui.components.SarTextField
import com.saurabh.sar.ui.components.SarToggle
import com.saurabh.sar.ui.components.SectionLabel
import com.saurabh.sar.ui.components.Segmented
import com.saurabh.sar.ui.components.Stepper
import com.saurabh.sar.ui.components.TopBar
import com.saurabh.sar.ui.theme.SarType

/** Emergency extra time settings for the mode being edited (FSD 6.2). */
@Composable
fun EmergencySettingsScreen(nav: Nav) {
    val draft = nav.draft ?: run { nav.pop(); return }
    val m = draft.mode
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)) {
        TopBar("Emergency time", m.name.ifBlank { null }, onBack = { nav.pop() })
        SarCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Emergency extra time", style = SarType.RowTitle)
                    Text("A deliberate way to unlock a few minutes from the block screen.", style = SarType.Meta)
                }
                SarToggle(m.emergencyEnabled, { draft.mode = m.copy(emergencyEnabled = it) })
            }
        }
        if (m.emergencyEnabled) {
            Spacer(Modifier.height(16.dp))
            SarCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Minutes per use", style = SarType.Body, modifier = Modifier.weight(1f))
                    Stepper("${m.emergencyMinutes} min", { draft.mode = m.copy(emergencyMinutes = (m.emergencyMinutes - 1).coerceAtLeast(1)) },
                        { draft.mode = m.copy(emergencyMinutes = (m.emergencyMinutes + 1).coerceAtMost(15)) })
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Uses per run", style = SarType.Body, modifier = Modifier.weight(1f))
                    Stepper("${m.emergencyUses}", { draft.mode = m.copy(emergencyUses = (m.emergencyUses - 1).coerceAtLeast(1)) },
                        { draft.mode = m.copy(emergencyUses = (m.emergencyUses + 1).coerceAtMost(5)) })
                }
            }
            Spacer(Modifier.height(16.dp))
            SectionLabel("Unlock effort")
            Segmented(listOf("Wait timer", "Type a phrase"), if (m.emergencyEffort == EmergencyEffort.WAIT) 0 else 1,
                { draft.mode = m.copy(emergencyEffort = if (it == 0) EmergencyEffort.WAIT else EmergencyEffort.PHRASE) },
                Modifier.fillMaxWidth(), icons = listOf(R.drawable.ic_hourglass, R.drawable.ic_square_pen))
            Spacer(Modifier.height(12.dp))
            if (m.emergencyEffort == EmergencyEffort.WAIT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Wait", style = SarType.Body, modifier = Modifier.weight(1f))
                    Stepper("${m.emergencyWaitSec} s", { draft.mode = m.copy(emergencyWaitSec = (m.emergencyWaitSec - 10).coerceAtLeast(10)) },
                        { draft.mode = m.copy(emergencyWaitSec = (m.emergencyWaitSec + 10).coerceAtMost(120)) })
                }
            } else {
                SarTextField(m.emergencyPhrase, { draft.mode = m.copy(emergencyPhrase = it) }, placeholder = "Phrase to type", singleLine = false, maxLength = 100)
                Text("${m.emergencyPhrase.length} / 100", style = SarType.Meta, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text("Uses are shared by all apps in this mode and reset when the mode starts again. Emergency time doesn't skip a session-cap break.", style = SarType.Meta)
        }
        Spacer(Modifier.height(24.dp))
        SarButton("Done", { nav.pop() }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
    }
}
