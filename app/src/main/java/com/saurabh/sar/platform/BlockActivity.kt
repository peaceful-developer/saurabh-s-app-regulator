package com.saurabh.sar.platform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.engine.BlockReason
import com.saurabh.sar.ui.screens.BlockScreen
import com.saurabh.sar.ui.theme.SarTheme
import kotlinx.coroutines.launch

/**
 * Full-screen block screen launched over the blocked app (FSD 6.1, Figma 06). Its own task,
 * hidden from Recents; Back and Go home both lead to the launcher, never into the app.
 */
class BlockActivity : ComponentActivity() {
    private var pkg by mutableStateOf("")
    private var reason by mutableStateOf<BlockReason>(BlockReason.RuleBlock)
    private var reopening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        read(intent)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        val app = application as SarApp
        setContent {
            SarTheme {
                BlockScreen(
                    pkg = pkg,
                    reason = reason,
                    onGoHome = ::goHome,
                    onEmergencyDone = {
                        lifecycleScope.launch {
                            if (app.controller.useEmergency(pkg) == null) reopenApp()
                        }
                    },
                    onReopen = ::reopenApp,
                    onRunGone = { finish() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        read(intent)
    }

    private fun read(i: Intent) {
        pkg = i.getStringExtra(EXTRA_PKG).orEmpty()
        reason = when (i.getStringExtra(EXTRA_KIND)) {
            "allowance" -> BlockReason.AllowanceUsed(i.getLongExtra(EXTRA_A, 0), i.getLongExtra(EXTRA_B, 0))
            "break" -> BlockReason.SessionBreak(i.getLongExtra(EXTRA_A, 0))
            else -> BlockReason.RuleBlock
        }
    }

    private fun reopenApp() {
        val launch = packageManager.getLaunchIntentForPackage(pkg) ?: return goHome()
        reopening = true
        startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    override fun onStop() {
        super.onStop()
        // Never linger behind other apps; the next open of a blocked app brings a fresh one.
        if (!isChangingConfigurations && !reopening) finish()
    }

    companion object {
        private const val EXTRA_PKG = "pkg"
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_A = "a"
        private const val EXTRA_B = "b"

        fun intent(context: Context, pkg: String, reason: BlockReason): Intent =
            Intent(context, BlockActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                putExtra(EXTRA_PKG, pkg)
                when (reason) {
                    is BlockReason.AllowanceUsed -> { putExtra(EXTRA_KIND, "allowance"); putExtra(EXTRA_A, reason.usedMs); putExtra(EXTRA_B, reason.allowanceMs) }
                    is BlockReason.SessionBreak -> { putExtra(EXTRA_KIND, "break"); putExtra(EXTRA_A, reason.until) }
                    BlockReason.RuleBlock -> putExtra(EXTRA_KIND, "rule")
                }
            }
    }
}
