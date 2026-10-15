package com.novasonic.app.ui

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.novasonic.app.EightDState
import com.novasonic.app.MusicViewModel
import com.novasonic.app.PlayerUiState
import com.novasonic.app.QueueEntry
import kotlinx.coroutines.flow.StateFlow

// ------------------------------------------------------------------ Mini player

@Composable
fun MiniPlayer(ui: PlayerUiState, position: StateFlow<Long>, vm: MusicViewModel, onOpen: () -> Unit) {
    val song = ui.currentSong ?: return
    val pos by position.collectAsStateWithLifecycle()
    val frac = if (ui.durationMs > 0) (pos.toFloat() / ui.durationMs).coerceIn(0f, 1f) else 0f

    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth()
            .panel(RoundedCornerShape(18.dp)).clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(song, 140, Modifier.size(44.dp), 10.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, color = Nova.TextHi, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artist, color = Nova.TextLo, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            PlayPauseButton(ui.isPlaying, 40.dp, ring = false, onClick = { vm.togglePlay() })
            IconButton(onClick = { vm.next() }) { Icon(Icons.Rounded.SkipNext, "Next", tint = Nova.TextHi) }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.08f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Nova.Accent))
        }
    }
}

// ------------------------------------------------------------------ Now playing

@Composable
private fun ActionPill(
    label: String?,
    icon: ImageVector,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = ""
) {
    Row(
        modifier.panel(RoundedCornerShape(16.dp), highlight = accent).clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label ?: description, tint = if (accent) Nova.Accent else Nova.TextHi, modifier = Modifier.size(20.dp))
        if (label != null) {
            Spacer(Modifier.width(8.dp))
            Text(label, color = if (accent) Nova.Accent else Nova.TextHi, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun NowPlayingScreen(
    ui: PlayerUiState,
    position: StateFlow<Long>,
    vm: MusicViewModel,
    onClose: () -> Unit,
    onOpenQueue: () -> Unit,
    eightD: EightDState,
    onOpenEightD: () -> Unit
) {
    val song = ui.currentSong
    val pos by position.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val soon = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }
    val artScale by animateFloatAsState(
        targetValue = if (ui.isPlaying) 1f else 0.93f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 150f),
        label = "artScale"
    )

    Box(
        Modifier.fillMaxSize().background(Nova.Bg).pointerInput(Unit) { detectTapGestures { } }
    ) {
        Column(
            Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconCircleButton(Icons.Rounded.KeyboardArrowDown, "Close", onClose)
                Text(
                    "Now Playing", color = Nova.TextLo, fontSize = 14.sp, letterSpacing = 1.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.size(42.dp))
            }

            Spacer(Modifier.height(16.dp))

            Artwork(
                song, 800,
                Modifier.fillMaxWidth().widthIn(max = 420.dp).aspectRatio(1f)
                    .graphicsLayer { scaleX = artScale; scaleY = artScale },
                corner = 26.dp
            )

            Spacer(Modifier.height(28.dp))

            Column(Modifier.fillMaxWidth()) {
                Text(
                    song?.title ?: "Nothing playing", color = Nova.TextHi, fontSize = 24.sp,
                    fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    song?.artist ?: "", color = Nova.TextLo, fontSize = 16.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(16.dp))

            NovaSeekBar(positionMs = pos, durationMs = ui.durationMs, onSeek = { vm.seekTo(it) })
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(pos), color = Nova.TextLo, fontSize = 12.sp)
                Text(formatTime(ui.durationMs), color = Nova.TextLo, fontSize = 12.sp)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.toggleShuffle() }) {
                    Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (ui.shuffle) Nova.Accent else Nova.TextLo)
                }
                IconButton(onClick = { vm.previous() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", tint = Nova.TextHi, modifier = Modifier.size(36.dp))
                }
                PlayPauseButton(ui.isPlaying, 76.dp, ring = true, onClick = { vm.togglePlay() })
                IconButton(onClick = { vm.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Next", tint = Nova.TextHi, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { vm.cycleRepeat() }) {
                    Icon(
                        if (ui.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Repeat",
                        tint = if (ui.repeatMode == Player.REPEAT_MODE_OFF) Nova.TextLo else Nova.Accent
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionPill("8D", Icons.Rounded.Headphones, accent = eightD.enabled, onClick = { vm.toggleEightD() })
                ActionPill(null, Icons.Rounded.Tune, accent = false, onClick = onOpenEightD, description = "8D settings")
                ActionPill(null, Icons.Rounded.Equalizer, accent = false, onClick = { soon() }, description = "Equalizer")
                ActionPill(null, Icons.Rounded.QueueMusic, accent = false, onClick = onOpenQueue, description = "Queue")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ------------------------------------------------------------------ Queue

@Composable
fun QueueOverlay(ui: PlayerUiState, vm: MusicViewModel, onClose: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val i = ui.queue.indexOfFirst { it.index == ui.currentIndex }
        if (i > 0) listState.scrollToItem(i)
    }

    Column(
        Modifier.fillMaxSize().background(Nova.Bg).systemBarsPadding().pointerInput(Unit) { detectTapGestures { } }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClose)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Queue", color = Nova.TextHi, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                Text("${ui.itemCount} songs", color = Nova.TextLo, fontSize = 13.sp)
            }
            if (ui.queue.isNotEmpty()) {
                PillButton("Clear", Icons.Rounded.DeleteSweep, filled = false) {
                    vm.clearQueue()
                    onClose()
                }
            }
        }

        if (ui.queue.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Queue is empty", color = Nova.TextLo, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(ui.queue, key = { "${it.index}-${it.song.id}" }) { entry ->
                    QueueRow(
                        entry = entry,
                        isCurrent = entry.index == ui.currentIndex,
                        isPlaying = ui.isPlaying,
                        canMoveUp = entry.index > 0,
                        canMoveDown = entry.index < ui.itemCount - 1,
                        onPlay = { vm.playQueueIndex(entry.index) },
                        onUp = { vm.moveInQueue(entry.index, entry.index - 1) },
                        onDown = { vm.moveInQueue(entry.index, entry.index + 1) },
                        onRemove = { vm.removeFromQueue(entry.index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    isCurrent: Boolean,
    isPlaying: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onPlay: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onRemove: () -> Unit
) {
    val song = entry.song
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay)
            .padding(start = 20.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(song, 140, Modifier.size(46.dp), 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, color = if (isCurrent) Nova.Accent else Nova.TextHi,
                fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(song.artist, color = Nova.TextLo, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isCurrent) EqualizerBars(isPlaying, Modifier.padding(horizontal = 6.dp))
        IconButton(onClick = onUp, enabled = canMoveUp, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.ArrowUpward, "Move up", tint = if (canMoveUp) Nova.TextHi else Nova.TextLo.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDown, enabled = canMoveDown, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.ArrowDownward, "Move down", tint = if (canMoveDown) Nova.TextHi else Nova.TextLo.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, "Remove", tint = Nova.Danger, modifier = Modifier.size(20.dp))
        }
    }
}
