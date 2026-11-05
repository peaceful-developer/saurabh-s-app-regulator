package com.saurabh.sar

import android.app.Application
import com.saurabh.sar.core.Clock
import com.saurabh.sar.data.SarDatabase
import com.saurabh.sar.data.SarRepository
import com.saurabh.sar.data.SettingsStore
import com.saurabh.sar.platform.AlarmScheduler
import com.saurabh.sar.platform.ImageStore
import com.saurabh.sar.platform.InstalledApps
import com.saurabh.sar.platform.Notifications
import com.saurabh.sar.platform.RunController
import com.saurabh.sar.platform.UsageReconciler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Application and the simple manual dependency container (no DI framework). */
class SarApp : Application() {
    /** The only place SAR reads the system clock; everything else gets this [Clock]. */
    val clock = Clock { System.currentTimeMillis() }
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val database by lazy { SarDatabase.create(this) }
    val repository by lazy { SarRepository(database) }
    val settings by lazy { SettingsStore(this) }
    val notifications by lazy { Notifications(this) }
    val alarms by lazy { AlarmScheduler(this) }
    val installedApps by lazy { InstalledApps(this) }
    val images by lazy { ImageStore(this) }
    val controller by lazy { RunController(repository, settings, alarms, notifications, clock) }
    val usageReconciler by lazy { UsageReconciler(this, controller, clock) }

    override fun onCreate() {
        super.onCreate()
        notifications.createChannels()
        scope.launch { controller.evaluate() }
    }
}
