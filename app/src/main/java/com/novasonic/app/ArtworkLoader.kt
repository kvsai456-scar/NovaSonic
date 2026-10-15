package com.novasonic.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads album artwork for a song: system thumbnail first, embedded tag picture as a fallback. */
object ArtworkLoader {
    private val cache = object : LruCache<String, Bitmap>(32 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val misses = java.util.Collections.synchronizedSet(HashSet<String>())

    suspend fun load(context: Context, uri: Uri, size: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$uri@$size"
        cache.get(key)?.let { return@withContext it }
        if (misses.contains(key)) return@withContext null

        var bmp: Bitmap? = null
        if (Build.VERSION.SDK_INT >= 29) {
            bmp = try {
                context.contentResolver.loadThumbnail(uri, Size(size, size), null)
            } catch (e: Exception) {
                null
            }
        }
        if (bmp == null) {
            bmp = try {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    retriever.embeddedPicture?.let { decode(it, size) }
                } finally {
                    retriever.release()
                }
            } catch (e: Exception) {
                null
            }
        }
        if (bmp != null) cache.put(key, bmp) else misses.add(key)
        bmp
    }

    private fun decode(bytes: ByteArray, target: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }
}

@Composable
fun rememberArtwork(uri: Uri?, sizePx: Int): State<Bitmap?> {
    val context = LocalContext.current
    return produceState<Bitmap?>(initialValue = null, uri, sizePx) {
        value = if (uri == null) null else ArtworkLoader.load(context, uri, sizePx)
    }
}
