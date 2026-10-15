package com.novasonic.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.novasonic.app.MusicViewModel
import com.novasonic.app.PlayerUiState
import com.novasonic.app.Song
import kotlinx.coroutines.delay

@Composable
fun NovaSonicApp(vm: MusicViewModel, hasPermission: Boolean, onImport: () -> Unit) {
    val songs by vm.songs.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val recentIds by vm.recentIds.collectAsStateWithLifecycle()
    val eightD by vm.eightD.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(0) }
    var showPlayer by rememberSaveable { mutableStateOf(false) }
    var showQueue by rememberSaveable { mutableStateOf(false) }
    var showEightD by rememberSaveable { mutableStateOf(false) }
    var showSplash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(1400)
        showSplash = false
    }
    BackHandler(enabled = showPlayer) { showPlayer = false }
    BackHandler(enabled = showQueue) { showQueue = false }
    BackHandler(enabled = showEightD) { showEightD = false }
    LaunchedEffect(ui.currentSong) {
        if (ui.currentSong == null) {
            showPlayer = false
            showQueue = false
        }
    }

    Box(Modifier.fillMaxSize().background(Nova.Bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (tab) {
                    0 -> HomeScreen(songs, ui, recentIds, loading, hasPermission, onImport,
                        onOpenSearch = { tab = 2 }, onSeeAll = { tab = 1 }, vm = vm)
                    1 -> LibraryScreen(songs, ui, hasPermission, loading, onImport, vm)
                    else -> SearchScreen(songs, ui, vm)
                }
            }
            if (ui.currentSong != null) {
                MiniPlayer(ui, vm.position, vm) { showPlayer = true }
            }
            BottomBar(tab) { tab = it }
        }

        AnimatedVisibility(
            visible = showPlayer,
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(320)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(260))
        ) {
            NowPlayingScreen(
                ui = ui,
                position = vm.position,
                vm = vm,
                onClose = { showPlayer = false },
                onOpenQueue = { showQueue = true },
                eightD = eightD,
                onOpenEightD = { showEightD = true }
            )
        }

        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(250))
        ) {
            QueueOverlay(ui, vm) { showQueue = false }
        }

        AnimatedVisibility(
            visible = showEightD,
            enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
            exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(250))
        ) {
            EightDScreen(eightD, vm) { showEightD = false }
        }

        AnimatedVisibility(visible = showSplash, exit = fadeOut(tween(500))) {
            SplashScreen()
        }
    }
}

@Composable
private fun SplashScreen() {
    Column(
        Modifier.fillMaxSize().background(Nova.Bg).pointerInput(Unit) { detectTapGestures { } },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Logo(132.dp)
        Spacer(Modifier.height(20.dp))
        Wordmark(40.sp)
        Spacer(Modifier.height(12.dp))
        Text("MUSIC BEYOND BOUNDARIES", color = Nova.TextLo, fontSize = 11.sp, letterSpacing = 4.sp)
    }
}

@Composable
private fun BottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        "Home" to Icons.Rounded.Home,
        "Library" to Icons.Rounded.LibraryMusic,
        "Search" to Icons.Rounded.Search
    )
    Column(Modifier.fillMaxWidth().background(Nova.Bg)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Nova.Border))
        Row(Modifier.fillMaxWidth()) {
            items.forEachIndexed { i, (label, icon) ->
                val sel = i == selected
                Column(
                    Modifier.weight(1f).clickable { onSelect(i) }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, label, tint = if (sel) Nova.Accent else Nova.TextLo, modifier = Modifier.size(24.dp))
                    Text(label, color = if (sel) Nova.Accent else Nova.TextLo, fontSize = 11.sp)
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Home

@Composable
private fun HomeHeader(onImport: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Logo(34.dp)
        Spacer(Modifier.width(10.dp))
        Wordmark(26.sp, Modifier.weight(1f))
        IconCircleButton(Icons.Rounded.Download, "Import songs", onImport)
    }
}

@Composable
private fun HomeScreen(
    songs: List<Song>,
    ui: PlayerUiState,
    recentIds: List<Long>,
    loading: Boolean,
    hasPermission: Boolean,
    onImport: () -> Unit,
    onOpenSearch: () -> Unit,
    onSeeAll: () -> Unit,
    vm: MusicViewModel
) {
    val forYou = remember(songs) { songs.shuffled().take(12) }
    val recents = remember(songs, recentIds) {
        val byId = songs.associateBy { it.id }
        recentIds.mapNotNull { byId[it] }.take(8)
    }
    val listSongs = if (recents.isNotEmpty()) recents else songs.take(8)

    if (songs.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(onImport)
            EmptyState(
                icon = Icons.Rounded.LibraryMusic,
                title = if (!hasPermission) "Import your music" else "No songs found",
                body = if (!hasPermission) "Allow access to the audio files on your phone to build your library."
                else "We couldn't find any music on this phone. Add some songs and scan again.",
                loading = loading,
                action = if (!hasPermission) "Import songs from phone" else "Scan again",
                onAction = onImport,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    } else {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
            item { HomeHeader(onImport) }
            item { SearchBarStatic(onOpenSearch) }
            item { SectionHeader("For You") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(forYou, key = { it.id }) { song ->
                        Column(
                            Modifier.width(150.dp).clip(RoundedCornerShape(16.dp))
                                .clickable { vm.playSong(song, forYou) }
                        ) {
                            Artwork(song, 450, Modifier.size(150.dp), 16.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                song.title, color = if (ui.currentSong?.id == song.id) Nova.Accent else Nova.TextHi,
                                fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(song.artist, color = Nova.TextLo, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item { SectionHeader(if (recents.isNotEmpty()) "Recently Played" else "Your Songs", "See All", onSeeAll) }
            items(listSongs, key = { it.id }) { song ->
                SongRow(
                    song = song,
                    isCurrent = ui.currentSong?.id == song.id,
                    isPlaying = ui.isPlaying,
                    onClick = { vm.playSong(song, listSongs) },
                    onPlayNext = { vm.playNext(song) },
                    onAddToQueue = { vm.addToQueue(song) }
                )
            }
        }
    }
}

// ------------------------------------------------------------------ Library

@Composable
private fun SongList(list: List<Song>, ui: PlayerUiState, vm: MusicViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PillButton("Play all", Icons.Rounded.PlayArrow, true) {
                    list.firstOrNull()?.let { vm.playSong(it, list) }
                }
                PillButton("Shuffle", Icons.Rounded.Shuffle, false) { vm.playShuffled(list) }
            }
        }
        items(list, key = { it.id }) { song ->
            SongRow(
                song = song,
                isCurrent = ui.currentSong?.id == song.id,
                isPlaying = ui.isPlaying,
                onClick = { vm.playSong(song, list) },
                onPlayNext = { vm.playNext(song) },
                onAddToQueue = { vm.addToQueue(song) }
            )
        }
    }
}

@Composable
private fun LibraryScreen(
    songs: List<Song>,
    ui: PlayerUiState,
    hasPermission: Boolean,
    loading: Boolean,
    onImport: () -> Unit,
    vm: MusicViewModel
) {
    var section by rememberSaveable { mutableStateOf(0) }
    var detailKind by rememberSaveable { mutableStateOf(0) } // 0 = none, 1 = artist, 2 = album
    var detailName by rememberSaveable { mutableStateOf("") }

    BackHandler(enabled = detailKind != 0) { detailKind = 0 }

    val artists = remember(songs) { songs.groupBy { it.artist }.toList().sortedBy { it.first.lowercase() } }
    val albums = remember(songs) { songs.groupBy { it.album }.toList().sortedBy { it.first.lowercase() } }
    val detailSongs = remember(songs, detailKind, detailName) {
        when (detailKind) {
            1 -> songs.filter { it.artist == detailName }
            2 -> songs.filter { it.album == detailName }
            else -> emptyList()
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (detailKind != 0) {
            ScreenHeader(detailName, onBack = { detailKind = 0 })
        } else {
            ScreenHeader("Your Library", onImport = onImport)
        }

        if (songs.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.LibraryMusic,
                title = if (!hasPermission) "Import your music" else "No songs found",
                body = if (!hasPermission) "Allow access to the audio files on your phone to build your library."
                else "We couldn't find any music on this phone. Add some songs and scan again.",
                loading = loading,
                action = if (!hasPermission) "Import songs from phone" else "Scan again",
                onAction = onImport,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        } else if (detailKind != 0) {
            SongList(detailSongs, ui, vm)
        } else {
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("Songs", "Artists", "Albums").forEachIndexed { i, label ->
                    Chip(label, section == i) { section = i }
                }
            }
            when (section) {
                0 -> SongList(songs, ui, vm)
                1 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(artists, key = { it.first }) { (name, list) ->
                        GroupRow(
                            song = list.first(), title = name,
                            subtitle = if (list.size == 1) "1 song" else "${list.size} songs",
                            round = true
                        ) { detailKind = 1; detailName = name }
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    items(albums, key = { it.first }) { (name, list) ->
                        GroupRow(
                            song = list.first(), title = name,
                            subtitle = "${list.first().artist}  •  " + if (list.size == 1) "1 song" else "${list.size} songs",
                            round = false
                        ) { detailKind = 2; detailName = name }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Search

@Composable
private fun SearchScreen(songs: List<Song>, ui: PlayerUiState, vm: MusicViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(songs, query) {
        if (query.isBlank()) emptyList()
        else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Search")
        SearchField(query, { query = it })
        Spacer(Modifier.height(8.dp))
        if (songs.isEmpty() || query.isBlank() || results.isEmpty()) {
            val hint = when {
                songs.isEmpty() -> "Import your songs first"
                query.isBlank() -> "Search by song, artist or album"
                else -> "No results for \"$query\""
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(hint, color = Nova.TextLo, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(results, key = { it.id }) { song ->
                    SongRow(
                        song = song,
                        isCurrent = ui.currentSong?.id == song.id,
                        isPlaying = ui.isPlaying,
                        onClick = { vm.playSong(song, results) },
                        onPlayNext = { vm.playNext(song) },
                        onAddToQueue = { vm.addToQueue(song) }
                    )
                }
            }
        }
    }
}
