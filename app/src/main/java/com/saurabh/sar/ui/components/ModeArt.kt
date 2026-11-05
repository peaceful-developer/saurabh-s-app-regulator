package com.saurabh.sar.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.saurabh.sar.SarApp
import com.saurabh.sar.ui.theme.SarColors
import kotlin.random.Random

/** Built-in block-screen images, drawn as static vector art (planet + ring + stars). */
val BUILTIN_IMAGES = listOf("study", "gym", "meditation", "office", "weekend", "night")

private fun palette(key: String): Pair<Color, Color> = when (key) {
    "study" -> Color(0xFF22E5FF) to Color(0xFF8B5CF6)
    "gym" -> Color(0xFFFF3D7F) to Color(0xFFFFB020)
    "meditation" -> Color(0xFF2BFFA3) to Color(0xFF22E5FF)
    "office" -> Color(0xFF8B5CF6) to Color(0xFF22E5FF)
    "weekend" -> Color(0xFFFFB020) to Color(0xFFFF3D7F)
    else -> Color(0xFF3A4CFF) to Color(0xFF8B5CF6)
}

@Composable
fun BuiltinArt(key: String, modifier: Modifier = Modifier) {
    val (a, b) = palette(key)
    val stars = remember(key) {
        val r = Random(key.hashCode())
        List(40) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat() * 2.2f + 0.6f) }
    }
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF120A2E), Color(0xFF0B1030), SarColors.BgBase)))
        stars.forEach { (x, y, s) -> drawCircle(Color.White.copy(alpha = 0.55f), s, Offset(x * size.width, y * size.height * 0.9f)) }
        val c = Offset(size.width * 0.68f, size.height * 0.42f)
        val r = size.minDimension * 0.30f
        drawCircle(Brush.radialGradient(listOf(a.copy(alpha = 0.35f), Color.Transparent), c, r * 1.8f), r * 1.8f, c)
        drawCircle(Brush.linearGradient(listOf(a, b, Color(0xFF1A1050)), start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r)), r, c)
        rotate(-18f, c) {
            drawOval(a.copy(alpha = 0.7f), topLeft = Offset(c.x - r * 1.9f, c.y - r * 0.38f), size = Size(r * 3.8f, r * 0.76f), style = Stroke(3f))
        }
    }
}

/** The mode's chosen image: a downscaled user photo, a built-in, or the default art. */
@Composable
fun ModeImage(imagePath: String?, modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as SarApp
    if (imagePath != null && !imagePath.startsWith("builtin:")) {
        val bmp: Bitmap? = remember(imagePath) { app.images.load(imagePath) }
        if (bmp != null) {
            val img = remember(bmp) { bmp.asImageBitmap() }
            Image(img, null, modifier, contentScale = ContentScale.Crop)
            return
        }
    }
    Box(modifier) { BuiltinArt(imagePath?.removePrefix("builtin:") ?: "study", Modifier.fillMaxSize()) }
}
