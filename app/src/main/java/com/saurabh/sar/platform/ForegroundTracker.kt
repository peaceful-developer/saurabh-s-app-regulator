package com.saurabh.sar.platform

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.saurabh.sar.core.Clock
import com.saurabh.sar.core.engine.Decision
import com.saurabh.sar.core.engine.RulesEngine
import com.saurabh.sar.core.engine.UsageLedger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Follows which controlled app is in front and asks core what to do. Event driven only:
 * an app switch, screen off/on, a run change, or the ONE delayed callback for the moment the
 * current app's limit (or a warning) is due. Main thread only (FSD 11.3).
 */
class ForegroundTracker(
    private val controller: RunController,
    private val clock: Clock,
    private val scope: CoroutineScope,
    private val warningMinutes: () -> List<Int>,
    private val onBlock: (pkg: String, decision: Decision.Block) -> Unit,
    private val onWarning: (pkg: String, minutes: Int) -> Unit,
) : ForegroundListener {
    private val handler = Handler(Looper.getMainLooper())
    private val recheck = Runnable { onCallback() }

    /** Controlled app currently counted, and since when. */
    private var current: String? = null
    private var since: Long = 0L
    /** Last real app seen in front (even uncontrolled), so a run that starts mid-use applies at once. */
    var lastPackage: String? = null
        private set
    private var screenOn = true
    private var pendingWarning: Pair<String, Int>? = null

    /** A different app (or the launcher/SAR = null) came to the front. */
    fun onAppInFront(pkg: String?) {
        if (pkg == lastPackage && (pkg == null || pkg == current)) return
        lastPackage = pkg
        val now = clock.now()
        closeInterval(now)
        if (pkg == null || !screenOn) return
        val state = controller.active.value ?: return
        if (pkg !in state.rules) return
        current = pkg
        since = now
        check(now)
    }

    /** No mode is running: just remember the app, nothing else (keeps the idle path free). */
    fun rememberOnly(pkg: String?) {
        lastPackage = pkg
    }

    fun onScreenOff() {
        screenOn = false
        closeInterval(clock.now())
    }

    /** Screen unlocked: the app that was in front before is in front again. */
    fun onScreenOn() {
        if (screenOn) return
        screenOn = true
        val pkg = lastPackage
        lastPackage = null
        onAppInFront(pkg)
    }

    override fun flush(now: Long) = closeInterval(now)

    override fun onActiveChanged() {
        val pkg = lastPackage
        closeInterval(clock.now())
        lastPackage = null
        if (screenOn) onAppInFront(pkg)
    }

    fun stop() {
        closeInterval(clock.now())
        handler.removeCallbacks(recheck)
    }

    private fun onCallback() {
        val pkg = current ?: return
        pendingWarning?.let { (p, m) -> if (p == pkg) onWarning(p, m) }
        pendingWarning = null
        check(clock.now())
    }

    /** Ask core about the counted app right now, then block or schedule the single callback. */
    private fun check(now: Long) {
        handler.removeCallbacks(recheck)
        val pkg = current ?: return
        val state = controller.active.value ?: return
        val rule = state.rules[pkg]
        val snapshot = UsageLedger.accrue(state.usageOf(pkg), rule, since, now)
        when (val d = RulesEngine.decide(rule, snapshot, now, warningMinutes())) {
            is Decision.Block -> {
                Log.i(TAG, "block $pkg ${d.reason}")
                closeInterval(now) // time behind the block screen does not count
                onBlock(pkg, d)
            }
            is Decision.Allow -> {
                val at = d.nextCheckAt ?: return
                pendingWarning = if (d.warnAt != null && d.warnAt == at && d.warnMinutes != null) pkg to d.warnMinutes!! else null
                handler.postDelayed(recheck, (at - now).coerceAtLeast(0) + 50)
            }
        }
    }

    /** Ends the counted interval: adds it to usage and saves once (one DB write per switch). */
    private fun closeInterval(now: Long) {
        handler.removeCallbacks(recheck)
        pendingWarning = null
        val pkg = current ?: return
        current = null
        val state = controller.active.value ?: return
        val updated = UsageLedger.accrue(state.usageOf(pkg), state.rules[pkg], since, now)
        controller.recordUsage(updated)
        scope.launch { controller.persistUsage(updated) }
    }
}
