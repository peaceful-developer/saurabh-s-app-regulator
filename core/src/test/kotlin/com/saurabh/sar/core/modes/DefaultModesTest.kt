package com.saurabh.sar.core.modes

import com.google.common.truth.Truth.assertThat
import com.saurabh.sar.core.model.AppCategory
import com.saurabh.sar.core.model.InstalledApp
import com.saurabh.sar.core.model.ModeType
import com.saurabh.sar.core.model.RuleType
import org.junit.Test

class DefaultModesTest {
    private val apps = listOf(
        InstalledApp("ig", "Instagram", AppCategory.SOCIAL),
        InstalledApp("yt", "YouTube", AppCategory.VIDEO),
        InstalledApp("chess", "Chess", AppCategory.GAME),
        InstalledApp("spotify", "Spotify", AppCategory.AUDIO),
    )
    private val modes = DefaultModes.build(apps).associateBy { it.mode.name }

    @Test fun `five defaults with the right types`() {
        assertThat(modes.keys).containsExactly("Self Study", "Gym", "Meditation", "Office Work", "Weekend").inOrder()
        assertThat(modes["Office Work"]!!.mode.type).isEqualTo(ModeType.SCHEDULED)
        assertThat(modes["Weekend"]!!.schedule).hasSize(1)
        assertThat(modes["Meditation"]!!.mode.emergencyEnabled).isFalse()
    }

    @Test fun `suggestions follow the FSD table`() {
        val study = modes["Self Study"]!!.rules.associateBy { it.packageName }
        assertThat(study["ig"]!!.type).isEqualTo(RuleType.BLOCK)
        assertThat(study["chess"]!!.type).isEqualTo(RuleType.BLOCK)
        assertThat(study["yt"]!!.allowanceMin).isEqualTo(20)
        assertThat(modes["Gym"]!!.rules.map { it.packageName }).containsExactly("ig")
        assertThat(modes.values.flatMap { it.rules }.map { it.packageName }).doesNotContain("spotify")
        assertThat(modes["Office Work"]!!.rules.first { it.packageName == "ig" }.allowanceMin).isEqualTo(15)
    }
}
