package com.saurabh.sar.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.saurabh.sar.core.model.AppCategory
import com.saurabh.sar.core.model.InstalledApp

/** Launchable installed apps, their icons and categories (FSD 4.1). */
class InstalledApps(private val context: Context) {
    private val pm = context.packageManager
    private val iconCache = LruCache<String, ImageBitmap>(80)
    @Volatile private var cached: List<InstalledApp>? = null

    /** Packages that can never be controlled: SAR, the dialer(s) and Settings (FSD 4.1, DECISIONS #15). */
    fun protectedPackages(): Set<String> {
        val out = mutableSetOf(context.packageName, "com.android.settings", "com.samsung.android.dialer",
            "com.android.dialer", "com.google.android.dialer", "com.android.phone", "com.samsung.android.incallui")
        runCatching { context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage }.getOrNull()?.let(out::add)
        pm.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName?.let(out::add)
        pm.resolveActivity(Intent(Intent.ACTION_DIAL), 0)?.activityInfo?.packageName?.let(out::add)
        return out
    }

    /** Home-screen apps; the launcher is treated as "no app in front". */
    fun launcherPackages(): Set<String> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return pm.queryIntentActivities(i, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo.packageName }.toSet()
    }

    fun list(refresh: Boolean = false): List<InstalledApp> {
        if (!refresh) cached?.let { return it }
        val protected = protectedPackages()
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(i, 0)
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName !in protected }
            .map { ai -> InstalledApp(ai.packageName, pm.getApplicationLabel(ai).toString(), categoryOf(ai)) }
            .sortedBy { it.label.lowercase() }
        cached = apps
        return apps
    }

    fun label(pkg: String): String = cached?.firstOrNull { it.packageName == pkg }?.label
        ?: runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

    fun isInstalled(pkg: String) = runCatching { pm.getApplicationInfo(pkg, 0); true }.getOrDefault(false)

    /** Icons are drawn once at 96 px and cached. */
    fun icon(pkg: String): ImageBitmap? {
        iconCache.get(pkg)?.let { return it }
        val d = runCatching { pm.getApplicationIcon(pkg) }.getOrNull() ?: return null
        val size = 96
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        d.setBounds(0, 0, size, size)
        d.draw(Canvas(bmp))
        return bmp.asImageBitmap().also { iconCache.put(pkg, it) }
    }

    private fun categoryOf(ai: ApplicationInfo): AppCategory {
        KNOWN[ai.packageName]?.let { return it }
        return when (ai.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.VIDEO
            ApplicationInfo.CATEGORY_AUDIO -> AppCategory.AUDIO
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.PRODUCTIVITY
            else -> if (ai.flags and ApplicationInfo.FLAG_IS_GAME != 0) AppCategory.GAME else AppCategory.OTHER
        }
    }

    companion object {
        /** Popular apps that don't declare a category, so the default modes still get sensible suggestions. */
        private val KNOWN = mapOf(
            "com.instagram.android" to AppCategory.SOCIAL,
            "com.instagram.barcelona" to AppCategory.SOCIAL,
            "com.facebook.katana" to AppCategory.SOCIAL,
            "com.facebook.lite" to AppCategory.SOCIAL,
            "com.twitter.android" to AppCategory.SOCIAL,
            "com.snapchat.android" to AppCategory.SOCIAL,
            "com.zhiliaoapp.musically" to AppCategory.SOCIAL,
            "com.ss.android.ugc.trill" to AppCategory.SOCIAL,
            "com.reddit.frontpage" to AppCategory.SOCIAL,
            "com.linkedin.android" to AppCategory.SOCIAL,
            "com.pinterest" to AppCategory.SOCIAL,
            "com.discord" to AppCategory.SOCIAL,
            "org.telegram.messenger" to AppCategory.SOCIAL,
            "com.whatsapp" to AppCategory.SOCIAL,
            "com.sharechat.app" to AppCategory.SOCIAL,
            "in.mohalla.sharechat" to AppCategory.SOCIAL,
            "com.google.android.youtube" to AppCategory.VIDEO,
            "com.netflix.mediaclient" to AppCategory.VIDEO,
            "in.startv.hotstar" to AppCategory.VIDEO,
            "com.amazon.avod.thirdpartyclient" to AppCategory.VIDEO,
            "com.jio.media.ondemand" to AppCategory.VIDEO,
            "com.jiocinema" to AppCategory.VIDEO,
            "tv.twitch.android.app" to AppCategory.VIDEO,
            "com.mxtech.videoplayer.ad" to AppCategory.VIDEO,
            "com.spotify.music" to AppCategory.AUDIO,
            "com.google.android.apps.youtube.music" to AppCategory.AUDIO,
            "com.jio.media.jiobeats" to AppCategory.AUDIO,
            "com.gaana" to AppCategory.AUDIO,
        )
    }
}
