package com.saurabh.sar.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Block-screen images: copied into app storage when chosen (FSD 3.1), downscaled to screen
 * width once, and cached in memory (FSD 11.3).
 */
class ImageStore(private val context: Context) {
    private val cache = LruCache<String, Bitmap>(4)
    private val dir get() = File(context.filesDir, "images").apply { mkdirs() }

    suspend fun import(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val maxW = context.resources.displayMetrics.widthPixels.coerceAtMost(1440)
            val src = ImageDecoder.createSource(context.contentResolver, uri)
            val bmp = ImageDecoder.decodeBitmap(src) { decoder, info, _ ->
                val w = info.size.width
                if (w > maxW) {
                    val scale = maxW.toFloat() / w
                    decoder.setTargetSize(maxW, (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            val file = File(dir, "mode_${System.nanoTime()}.jpg")
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            file.absolutePath
        }.getOrNull()
    }

    fun load(path: String): Bitmap? {
        cache.get(path)?.let { return it }
        val f = File(path)
        if (!f.exists()) return null
        return BitmapFactory.decodeFile(path)?.also { cache.put(path, it) }
    }

    /** Remove image files no mode uses any more. */
    fun prune(inUse: Set<String>) {
        dir.listFiles()?.filter { it.absolutePath !in inUse }?.forEach { it.delete() }
    }
}
