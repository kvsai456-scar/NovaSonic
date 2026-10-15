package com.novasonic.app

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long
) {
    fun toMediaItem(): MediaItem {
        val artUri = ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"), albumId
        )
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artUri)
                    .build()
            )
            .build()
    }
}

object MusicLibrary {
    /** Reads every music file on the phone through MediaStore. Needs the audio permission. */
    suspend fun scan(context: Context): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        try {
            context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val artist = c.getString(artistCol)
                    val album = c.getString(albumCol)
                    songs += Song(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        title = c.getString(titleCol) ?: "Unknown title",
                        artist = if (artist.isNullOrBlank() || artist == MediaStore.UNKNOWN_STRING) "Unknown artist" else artist,
                        album = if (album.isNullOrBlank() || album == MediaStore.UNKNOWN_STRING) "Unknown album" else album,
                        albumId = c.getLong(albumIdCol),
                        durationMs = c.getLong(durCol)
                    )
                }
            }
        } catch (e: SecurityException) {
            // permission revoked — return whatever we have (nothing)
        }
        songs
    }
}
