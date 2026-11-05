package com.saurabh.sar.platform

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.saurabh.sar.SarApp
import com.saurabh.sar.core.engine.Decision
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reports foreground-app changes to the tracker. Listens to TYPE_WINDOW_STATE_CHANGED only,
 * never reads window content, and returns at once while no mode is running (FSD 11.3).
 */
class SarAccessibilityService : AccessibilityService() {
    private lateinit var app: SarApp
    private lateinit var tracker: ForegroundTracker
    private var ignored: Set<String> = emptySet()
    private var launchers: Set<String> = emptySet()
    @Volatile private var warnings: List<Int> = listOf(5, 1)

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> tracker.onScreenOff()
                Intent.ACTION_USER_PRESENT -> tracker.onScreenOn()
                Intent.ACTION_SCREEN_ON -> if (!isKeyguardLocked()) tracker.onScreenOn()
            }
        }
    }

    private fun isKeyguardLocked(): Boolean =
        getSystemService(android.app.KeyguardManager::class.java)?.isKeyguardLocked == true

    override fun onServiceConnected() {
        app = application as SarApp
        refreshPackageSets()
        tracker = ForegroundTracker(
            controller = app.controller,
            clock = app.clock,
            scope = app.scope,
            warningMinutes = { warnings },
            onBlock = { pkg, d -> showBlock(pkg, d) },
            onWarning = { pkg, m -> app.notifications.showWarning(app.installedApps.label(pkg), m) },
        )
        app.controller.foregroundListener = tracker
        ContextCompat.registerReceiver(this, screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
        app.notifications.cancelBlockingOff()
        app.scope.launch {
            warnings = app.settings.settings.first().warningMinutes
            app.settings.settings.collect { warnings = it.warningMinutes }
        }
        app.scope.launch { app.controller.evaluate() }
        Log.i(TAG, "accessibility service connected")
    }

    private fun refreshPackageSets() {
        val imm = getSystemService(InputMethodManager::class.java)
        val imes = imm?.enabledInputMethodList?.map { it.packageName }.orEmpty()
        launchers = app.installedApps.launcherPackages()
        ignored = imes.toSet() + setOf("com.android.systemui", "android", "com.samsung.android.app.cocktailbarservice",
            "com.samsung.android.app.aodservice", "com.sec.android.app.desktoplauncher")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (pkg in ignored) return // system UI, keyboard, shade: the app behind stays in front
        val front = if (pkg == packageName || pkg in launchers) null else pkg
        if (app.controller.active.value == null) {
            tracker.rememberOnly(front)
            return
        }
        tracker.onAppInFront(front)
    }

    private fun showBlock(pkg: String, d: Decision.Block) {
        val i = BlockActivity.intent(this, pkg, d.reason)
        startActivity(i)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        shutdown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }

    private var shutDown = false
    private fun shutdown() {
        if (shutDown || !::tracker.isInitialized) return
        shutDown = true
        tracker.stop()
        app.controller.foregroundListener = null
        runCatching { unregisterReceiver(screenReceiver) }
        if (app.controller.active.value != null) app.notifications.showBlockingOff() // FSD 10
        Log.i(TAG, "accessibility service stopped")
    }
}
