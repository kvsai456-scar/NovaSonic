# NovaSonic (Phase 1: music player)

Open this folder in Android Studio (Koala / 2024.1+ or newer, JDK 17), let Gradle sync, run on a phone or emulator (Android 8.0+).

Features: scan/import songs from the phone, play/pause, next/previous, seek bar, album artwork,
queue (play next, add, reorder, remove, clear), shuffle/repeat, search, background playback with
notification + lock-screen controls.

Stack: Kotlin, Jetpack Compose, Media3 (ExoPlayer + MediaSession).
Playback lives in `PlaybackService`; the 8D effect will hook into that player's audio pipeline.
