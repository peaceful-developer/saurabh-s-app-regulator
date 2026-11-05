package com.saurabh.sar.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.saurabh.sar.SarApp
import kotlinx.coroutines.launch

/** Runs [block] on the app scope while keeping the broadcast alive until it finishes. */
private fun BroadcastReceiver.work(context: Context, block: suspend (SarApp) -> Unit) {
    val app = context.applicationContext as SarApp
    val result = goAsync()
    app.scope.launch {
        try { block(app) } catch (t: Throwable) { Log.e(TAG, "receiver", t) } finally { result.finish() }
    }
}

/** The single boundary alarm fired: re-plan (start/end runs, set the next alarm). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = work(context) { it.controller.evaluate() }
    companion object { const val ACTION_BOUNDARY = "com.saurabh.sar.BOUNDARY" }
}

/**
 * Boot, app update, time / zone change (FSD 10): resume or close runs, start a scheduled mode
 * whose window is still open, and set the alarm again.
 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val boot = intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        Log.i(TAG, "system event ${intent.action}")
        work(context) { app ->
            app.controller.evaluate(isBoot = boot)
            if (boot) app.usageReconciler.reconcile()
        }
    }
}

/** A controlled app was uninstalled: remove it from all modes silently (FSD 10). */
class PackageRemovedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.data?.schemeSpecificPart ?: return
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return
        work(context) { app ->
            app.repository.removePackage(pkg)
            app.controller.onModesChanged()
        }
    }
}

/** "Start <Mode>" pressed on the collision notification. */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val modeId = intent.getLongExtra(EXTRA_MODE_ID, -1)
        if (modeId < 0) return
        work(context) { it.controller.switchToScheduled(modeId) }
    }
    companion object {
        const val ACTION_SWITCH = "com.saurabh.sar.SWITCH"
        const val EXTRA_MODE_ID = "modeId"
    }
}
