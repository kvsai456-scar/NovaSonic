package com.novasonic.app

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.novasonic.app.ui.NovaSonicApp
import com.novasonic.app.ui.NovaSonicTheme

class MainActivity : ComponentActivity() {

    private val vm: MusicViewModel by viewModels()
    private var audioGranted by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            audioGranted = hasAudioPermission()
            if (audioGranted) vm.scanLibrary()
        }

    private fun audioPermission() =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(this, audioPermission()) == PackageManager.PERMISSION_GRANTED

    private fun importSongs() {
        if (hasAudioPermission()) {
            audioGranted = true
            vm.scanLibrary()
        } else {
            val perms = mutableListOf(audioPermission())
            if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
            permissionLauncher.launch(perms.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        audioGranted = hasAudioPermission()
        if (audioGranted && vm.songs.value.isEmpty()) vm.scanLibrary()

        setContent {
            NovaSonicTheme {
                NovaSonicApp(vm = vm, hasPermission = audioGranted, onImport = ::importSongs)
            }
        }
    }
}
