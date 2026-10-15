package com.novasonic.app

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class QueueEntry(val index: Int, val song: Song)

data class PlayerUiState(
    val currentSong: Song? = null,
    val currentIndex: Int = -1,
    val queue: List<QueueEntry> = emptyList(),
    val itemCount: Int = 0,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _ui = MutableStateFlow(PlayerUiState())
    val ui: StateFlow<PlayerUiState> = _ui.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var byId: Map<String, Song> = emptyMap()
    private var ticker: Job? = null

    private val prefs = app.getSharedPreferences("novasonic", Context.MODE_PRIVATE)
    private val _recentIds = MutableStateFlow(loadRecents())
    val recentIds: StateFlow<List<Long>> = _recentIds.asStateFlow()
    private var lastRecorded = -1L

    private val _eightD = MutableStateFlow(loadEightD())
    val eightD: StateFlow<EightDState> = _eightD.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = refresh(player)
    }

    init {
        connect()
    }

    // ---------- connection to PlaybackService ----------

    private fun connect() {
        val ctx = getApplication<Application>()
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        future.addListener({
            val c = try {
                future.get()
            } catch (e: Exception) {
                return@addListener
            }
            controller = c
            c.addListener(listener)
            refresh(c)
            pushEightD()
            startTicker()
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                controller?.let { _position.value = it.currentPosition.coerceAtLeast(0L) }
                delay(250)
            }
        }
    }

    private fun refresh(p: Player) {
        val entries = (0 until p.mediaItemCount).mapNotNull { i ->
            byId[p.getMediaItemAt(i).mediaId]?.let { QueueEntry(i, it) }
        }
        val current = p.currentMediaItem?.mediaId?.let { byId[it] }
        if (current != null && current.id != lastRecorded) recordRecent(current.id)
        val duration = p.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: current?.durationMs ?: 0L
        _ui.value = PlayerUiState(
            currentSong = current,
            currentIndex = if (p.mediaItemCount == 0) -1 else p.currentMediaItemIndex,
            queue = entries,
            itemCount = p.mediaItemCount,
            isPlaying = p.playWhenReady &&
                p.playbackState != Player.STATE_ENDED && p.playbackState != Player.STATE_IDLE,
            durationMs = duration,
            shuffle = p.shuffleModeEnabled,
            repeatMode = p.repeatMode
        )
    }

    // ---------- 8D audio ----------

    private fun loadEightD() = EightDState(
        enabled = prefs.getBoolean("8d_on", false),
        mode = prefs.getInt("8d_mode", EightD.MODE_ORBIT),
        intensity = prefs.getFloat("8d_intensity", 0.85f),
        speed = prefs.getFloat("8d_speed", 0.7f)
    )

    private fun updateEightD(change: (EightDState) -> EightDState) {
        val s = change(_eightD.value)
        _eightD.value = s
        prefs.edit()
            .putBoolean("8d_on", s.enabled)
            .putInt("8d_mode", s.mode)
            .putFloat("8d_intensity", s.intensity)
            .putFloat("8d_speed", s.speed)
            .apply()
        pushEightD(s)
    }

    private fun pushEightD(s: EightDState = _eightD.value) {
        val c = controller ?: return
        val args = Bundle().apply {
            putBoolean(EightD.KEY_ENABLED, s.enabled)
            putInt(EightD.KEY_MODE, s.mode)
            putFloat(EightD.KEY_INTENSITY, s.intensity)
            putFloat(EightD.KEY_PERIOD, s.periodSeconds)
        }
        c.sendCustomCommand(SessionCommand(EightD.ACTION, Bundle.EMPTY), args)
    }

    /** The one-tap 8D button. */
    fun toggleEightD() = updateEightD { it.copy(enabled = !it.enabled) }
    fun setEightDEnabled(on: Boolean) = updateEightD { it.copy(enabled = on) }
    fun setEightDMode(mode: Int) = updateEightD { it.copy(mode = mode, enabled = true) }
    fun setEightDIntensity(v: Float) = updateEightD { it.copy(intensity = v.coerceIn(0f, 1f)) }
    fun setEightDSpeed(v: Float) = updateEightD { it.copy(speed = v.coerceIn(0f, 1f)) }

    // ---------- recently played ----------

    private fun loadRecents(): List<Long> =
        prefs.getString("recents", "")?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

    private fun recordRecent(id: Long) {
        lastRecorded = id
        val updated = (listOf(id) + _recentIds.value.filter { it != id }).take(20)
        _recentIds.value = updated
        prefs.edit().putString("recents", updated.joinToString(",")).apply()
    }

    // ---------- library ----------

    fun scanLibrary() {
        viewModelScope.launch {
            _loading.value = true
            val list = MusicLibrary.scan(getApplication())
            byId = list.associateBy { it.id.toString() }
            _songs.value = list
            _loading.value = false
            controller?.let { refresh(it) }
        }
    }

    // ---------- playback ----------

    /** Plays [song] and queues the surrounding songs of [list]. Capped so the IPC payload stays small. */
    fun playSong(song: Song, list: List<Song> = _songs.value) {
        val c = controller ?: return
        val idx = list.indexOfFirst { it.id == song.id }
        if (idx < 0) return
        val start = (idx - 50).coerceAtLeast(0)
        val window = list.subList(start, minOf(list.size, start + MAX_QUEUE))
        c.setMediaItems(window.map { it.toMediaItem() }, idx - start, 0L)
        c.prepare()
        c.play()
    }

    fun playShuffled(list: List<Song>) {
        val c = controller ?: return
        if (list.isEmpty()) return
        c.setMediaItems(list.shuffled().take(MAX_QUEUE).map { it.toMediaItem() }, 0, 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (_ui.value.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            c.play()
        }
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        _position.value = ms
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    // ---------- queue ----------

    fun addToQueue(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) playSong(song, listOf(song)) else c.addMediaItem(song.toMediaItem())
    }

    fun playNext(song: Song) {
        val c = controller ?: return
        if (c.mediaItemCount == 0) playSong(song, listOf(song))
        else c.addMediaItem(c.currentMediaItemIndex + 1, song.toMediaItem())
    }

    fun playQueueIndex(index: Int) {
        val c = controller ?: return
        c.seekTo(index, 0L)
        c.play()
    }

    fun removeFromQueue(index: Int) { controller?.removeMediaItem(index) }

    fun moveInQueue(from: Int, to: Int) { controller?.moveMediaItem(from, to) }

    fun clearQueue() {
        controller?.run {
            stop()
            clearMediaItems()
        }
    }

    override fun onCleared() {
        ticker?.cancel()
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }

    private companion object {
        const val MAX_QUEUE = 300
    }
}
