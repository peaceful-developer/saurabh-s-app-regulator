package com.saurabh.sar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.saurabh.sar.ui.SarRoot
import com.saurabh.sar.ui.theme.SarTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent { SarTheme { SarRoot() } }
    }

    override fun onResume() {
        super.onResume()
        val app = application as SarApp
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.Default) { app.installedApps.list(refresh = true) }
            app.repository.pruneUninstalled(apps.map { it.packageName })
            app.controller.onModesChanged()
            app.usageReconciler.reconcile()
        }
    }
}
