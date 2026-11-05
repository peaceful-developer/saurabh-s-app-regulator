package com.saurabh.sar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.saurabh.sar.SarApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * "Now" for on-screen clocks. Updates on whole [periodMs] boundaries ONLY while the screen is
 * resumed (visible), and stops completely otherwise. This is a UI animation, not background
 * work (FSD 11.3 "animations run only while their screen is visible"; DECISIONS #23).
 */
@Composable
fun rememberNow(periodMs: Long = 60_000L): State<Long> {
    val app = LocalContext.current.applicationContext as SarApp
    val now = remember { mutableLongStateOf(app.clock.now()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, periodMs) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                val t = app.clock.now()
                now.longValue = t
                delay(periodMs - t % periodMs + 5)
            }
        }
    }
    return now
}
