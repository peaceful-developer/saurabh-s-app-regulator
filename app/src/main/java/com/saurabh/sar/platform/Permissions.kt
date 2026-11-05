package com.saurabh.sar.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

enum class SarPermission(val title: String, val why: String) {
    ACCESSIBILITY("Accessibility", "SAR uses this to notice the instant a blocked app opens, so the block screen can cover it. Nothing is read, stored or sent anywhere."),
    USAGE_ACCESS("Usage access", "Lets SAR double-check per-app screen time for usage counting and history, for example after a restart."),
    OVERLAY("Display over other apps", "Lets the block screen appear on top of the blocked app straight away."),
    NOTIFICATIONS("Notifications", "Shows the active mode, minutes-left warnings and the scheduled-mode warning."),
    BATTERY("Ignore battery optimisation", "Lets schedule alarms fire on time. SAR still does no background work between events."),
}

/** Live checks and the right system page for each permission (FSD 9.1). */
object Permissions {
    fun isGranted(context: Context, p: SarPermission): Boolean = when (p) {
        SarPermission.ACCESSIBILITY -> isAccessibilityEnabled(context)
        SarPermission.USAGE_ACCESS -> hasUsageAccess(context)
        SarPermission.OVERLAY -> Settings.canDrawOverlays(context)
        SarPermission.NOTIFICATIONS -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        SarPermission.BATTERY -> context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)
    }

    fun missing(context: Context): List<SarPermission> = SarPermission.entries.filterNot { isGranted(context, it) }

    fun isAccessibilityEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val me = ComponentName(context, SarAccessibilityService::class.java)
        return flat.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageAccess(context: Context): Boolean {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Intent for the system page. Notifications is a runtime prompt, handled by the caller on 13+. */
    @SuppressLint("BatteryLife")
    fun settingsIntent(context: Context, p: SarPermission): Intent {
        val pkg = Uri.parse("package:${context.packageName}")
        return when (p) {
            SarPermission.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            SarPermission.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            SarPermission.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, pkg)
            SarPermission.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            SarPermission.BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** App info page, where Android 13+ hides "Allow restricted settings" for sideloaded apps. */
    fun appInfoIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
