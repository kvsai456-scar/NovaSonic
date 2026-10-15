package com.novasonic.app.ui

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novasonic.app.R
import com.novasonic.app.Song
import com.novasonic.app.rememberArtwork

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

@Composable
fun Logo(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.logo),
        contentDescription = "NovaSonic",
        modifier = modifier.size(size)
    )
}

@Composable
fun Wordmark(fontSize: TextUnit, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Nova.TextHi)) { append("Nova") }
            withStyle(SpanStyle(color = Nova.Accent)) { append("Sonic") }
        },
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp
    )
}

@Composable
fun Artwork(song: Song?, sizePx: Int, modifier: Modifier = Modifier, corner: Dp = 14.dp) {
    val bitmap by rememberArtwork(song?.uri, sizePx)
    Box(
        modifier.clip(RoundedCornerShape(corner)).background(Nova.SurfaceHi),
        contentAlignment = Alignment.Center
    ) {
        val b = bitmap
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = song?.album,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Rounded.MusicNote, null, tint = Nova.Accent.copy(alpha = 0.8f), modifier = Modifier.fillMaxSize(0.45f))
        }
    }
}

@Composable
private fun barValue(t: InfiniteTransition, ms: Int): State<Float> =
    t.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq"
    )

@Composable
fun EqualizerBars(active: Boolean, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "eqT")
    val h0 by barValue(t, 420)
    val h1 by barValue(t, 560)
    val h2 by barValue(t, 350)
    val hs = listOf(h0, h1, h2)
    Row(
        modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        hs.forEach { h ->
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight(if (active) h else 0.25f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Nova.Accent)
            )
        }
    }
}

@Composable
fun IconCircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Nova.TextHi
) {
    Box(
        modifier.size(42.dp).panel(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, tint = tint, modifier = Modifier.size(22.dp)) }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) Nova.Accent else Nova.Surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (selected) Color.White else Nova.TextLo, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PillButton(
    text: String,
    icon: ImageVector,
    filled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .then(if (filled) Modifier.clip(shape).background(Nova.Accent) else Modifier.panel(shape))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (filled) Color.White else Nova.Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
fun PlayPauseButton(isPlaying: Boolean, size: Dp, ring: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (ring) Modifier.background(Nova.AccentSoft).border(2.dp, Nova.Accent, CircleShape)
                else Modifier.background(Nova.Accent)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) "Pause" else "Play",
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Nova.TextHi, fontSize = 20.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(action, color = Nova.Accent, fontSize = 14.sp, modifier = Modifier.clickable(onClick = onAction))
        }
    }
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)? = null, onImport: (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconCircleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.width(12.dp))
        }
        Text(
            title, color = Nova.TextHi, fontSize = 26.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        if (onImport != null) IconCircleButton(Icons.Rounded.Download, "Import songs", onImport)
    }
}

@Composable
fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp).panel(RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = Nova.TextLo, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Nova.TextHi, fontSize = 15.sp),
            cursorBrush = SolidColor(Nova.Accent),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) Text("Search songs, artists, albums…", color = Nova.TextLo, fontSize = 15.sp)
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close, "Clear", tint = Nova.TextLo,
                modifier = Modifier.size(20.dp).clickable { onChange("") }
            )
        }
    }
}

@Composable
fun SearchBarStatic(onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).panel(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = Nova.TextLo, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Search songs, artists, albums…", color = Nova.TextLo, fontSize = 15.sp)
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    loading: Boolean,
    action: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(Nova.Surface), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Nova.Accent, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = Nova.TextHi, fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(body, color = Nova.TextLo, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        if (loading) {
            CircularProgressIndicator(color = Nova.Accent)
        } else if (action != null) {
            PillButton(action, Icons.Rounded.Download, filled = true, onClick = onAction)
        }
    }
}

@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(song, 160, Modifier.size(52.dp), 12.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, color = if (isCurrent) Nova.Accent else Nova.TextHi,
                fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(
                "${song.artist}  •  ${formatTime(song.durationMs)}", color = Nova.TextLo,
                fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrent) EqualizerBars(isPlaying, Modifier.padding(horizontal = 8.dp))
        Box {
            IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = Nova.TextLo) }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text("Play next") },
                    leadingIcon = { Icon(Icons.Rounded.QueuePlayNext, null) },
                    onClick = { menu = false; onPlayNext() }
                )
                DropdownMenuItem(
                    text = { Text("Add to queue") },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                    onClick = { menu = false; onAddToQueue() }
                )
            }
        }
    }
}

@Composable
fun GroupRow(song: Song, title: String, subtitle: String, round: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Artwork(song, 160, Modifier.size(52.dp), if (round) 26.dp else 12.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Nova.TextHi, fontWeight = FontWeight.Medium, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Nova.TextLo, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Nova.TextLo)
    }
}

private fun seekFraction(x: Float, widthPx: Int, padPx: Float): Float =
    ((x - padPx) / (widthPx - 2 * padPx).coerceAtLeast(1f)).coerceIn(0f, 1f)

/** Thin seek bar: tap anywhere or drag the thumb. Commits the seek when the finger lifts. */
@Composable
fun NovaSeekBar(positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val seek by rememberUpdatedState(onSeek)
    val fraction = dragFraction
        ?: if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Box(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(durationMs) {
                val pad = 8.dp.toPx()
                detectTapGestures { o -> seek((seekFraction(o.x, size.width, pad) * durationMs).toLong()) }
            }
            .pointerInput(durationMs) {
                val pad = 8.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragFraction = seekFraction(o.x, size.width, pad) },
                    onDragEnd = {
                        dragFraction?.let { seek((it * durationMs).toLong()) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = seekFraction(change.position.x, size.width, pad)
                    }
                )
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val pad = 8.dp.toPx()
            val w = size.width - pad * 2
            val cy = size.height / 2
            val x = pad + w * fraction
            val stroke = 3.dp.toPx()
            drawLine(Color.White.copy(alpha = 0.14f), Offset(pad, cy), Offset(pad + w, cy), stroke, StrokeCap.Round)
            if (x > pad) drawLine(Nova.Accent, Offset(pad, cy), Offset(x, cy), stroke, StrokeCap.Round)
            drawCircle(Nova.Accent.copy(alpha = 0.25f), 11.dp.toPx(), Offset(x, cy))
            drawCircle(Color.White, 5.dp.toPx(), Offset(x, cy))
        }
    }
}
