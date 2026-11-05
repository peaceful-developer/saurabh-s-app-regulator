package com.saurabh.sar.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.saurabh.sar.MainActivity
import com.saurabh.sar.R
import com.saurabh.sar.core.model.Mode
import com.saurabh.sar.ui.formatClock

/**
 * Status-only notifications (FSD 8.2). The run notification is an ordinary ongoing
 * notification, not a foreground service: the accessibility service already keeps SAR alive.
 */
@android.annotation.SuppressLint("MissingPermission")
class Notifications(private val context: Context) {
    private val nm = NotificationManagerCompat.from(context)

    fun createChannels() {
        val sys = context.getSystemService(NotificationManager::class.java)
        sys.createNotificationChannel(NotificationChannel(CH_RUN, "Active mode", NotificationManager.IMPORTANCE_LOW).apply {
            setShowBadge(false)
        })
        sys.createNotificationChannel(NotificationChannel(CH_WARN, "Minutes-left warnings", NotificationManager.IMPORTANCE_HIGH))
        sys.createNotificationChannel(NotificationChannel(CH_COLLISION, "Scheduled mode due", NotificationManager.IMPORTANCE_HIGH))
        sys.createNotificationChannel(NotificationChannel(CH_ALERT, "Blocking problems", NotificationManager.IMPORTANCE_HIGH))
    }

    private fun allowed(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun showRunStatus(state: ActiveState?) {
        if (state == null) { nm.cancel(ID_RUN); return }
        if (!allowed()) return
        val end = state.run.plannedEndAt?.let { "ends ${formatClock(it)}" } ?: "until you stop it"
        val lock = if (state.run.locked) " · locked" else ""
        val n = NotificationCompat.Builder(context, CH_RUN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${state.mode.name} active · $end")
            .setContentText("Emergency uses left: ${state.emergencyLeft}$lock")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openApp())
            .setColor(0xFF22E5FF.toInt())
            .build()
        nm.notify(ID_RUN, n)
    }

    fun showWarning(appLabel: String, minutesLeft: Int) {
        if (!allowed()) return
        val n = NotificationCompat.Builder(context, CH_WARN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$appLabel: $minutesLeft min left")
            .setContentText("Your allowance in this mode is almost used up.")
            .setAutoCancel(true)
            .setTimeoutAfter(5 * 60_000L)
            .setContentIntent(openApp())
            .build()
        nm.notify(ID_WARN, n)
    }

    fun cancelWarnings() = nm.cancel(ID_WARN)

    fun showCollision(due: List<Mode>, current: Mode) {
        if (!allowed()) return
        val first = due.first()
        val action = PendingIntent.getBroadcast(
            context, first.id.toInt(),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(NotificationActionReceiver.ACTION_SWITCH)
                .putExtra(NotificationActionReceiver.EXTRA_MODE_ID, first.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CH_COLLISION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${first.name} is scheduled now")
            .setContentText("${current.name} is still active.")
            .addAction(0, "Start ${first.name}", action)
            .setContentIntent(openApp())
            .setOnlyAlertOnce(true)
            .build()
        nm.notify(ID_COLLISION, n)
    }

    fun cancelCollision() = nm.cancel(ID_COLLISION)

    fun showBlockingOff() {
        if (!allowed()) return
        val n = NotificationCompat.Builder(context, CH_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Blocking is off")
            .setContentText("SAR's accessibility service was turned off. Tap to fix it.")
            .setOngoing(true)
            .setContentIntent(openApp())
            .setColor(0xFFFF3D7F.toInt())
            .build()
        nm.notify(ID_ALERT, n)
    }

    fun cancelBlockingOff() = nm.cancel(ID_ALERT)

    companion object {
        const val CH_RUN = "run"
        const val CH_WARN = "warn"
        const val CH_COLLISION = "collision"
        const val CH_ALERT = "alert"
        const val ID_RUN = 1
        const val ID_WARN = 2
        const val ID_COLLISION = 3
        const val ID_ALERT = 4
    }
}
