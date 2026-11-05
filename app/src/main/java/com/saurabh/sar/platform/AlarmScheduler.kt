package com.saurabh.sar.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Exactly one exact alarm, at the next run boundary (a run's end or a schedule's start/end).
 * No periodic work of any kind (FSD 11.3). On Android 13+ SAR holds USE_EXACT_ALARM, which is
 * granted at install for a sideloaded app (DECISIONS #22).
 */
class AlarmScheduler(private val context: Context) {
    private val am = context.getSystemService(AlarmManager::class.java)

    private fun pending(): PendingIntent = PendingIntent.getBroadcast(
        context, 1,
        Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_BOUNDARY),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun setBoundary(at: Long?) {
        val pi = pending()
        if (at == null) { am.cancel(pi); Log.i(TAG, "no boundary alarm"); return }
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        Log.i(TAG, "boundary alarm at $at exact=$exact")
    }
}
