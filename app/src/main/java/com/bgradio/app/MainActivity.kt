package com.bgradio.app

import android.Manifest
import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bgradio.app.data.model.ThemeMode
import com.bgradio.app.ui.MainViewModel
import com.bgradio.app.ui.navigation.AppRoot
import com.bgradio.app.ui.theme.BGRadioTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels { MainViewModel.Factory }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* optional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val settings by vm.settings.collectAsStateWithLifecycle()
            BGRadioTheme(themeMode = settings?.themeMode ?: ThemeMode.DARK) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    AppRoot(vm)
                }
            }
        }

        // Android 13+: allow the playback notification (asked once, can be declined).
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (savedInstanceState == null) handleVoiceSearch(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleVoiceSearch(intent)
    }

    /** "Ok Google, play Radio 1 on Dominix". */
    private fun handleVoiceSearch(intent: Intent?) {
        if (intent?.action != MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) return
        vm.playFromSearch(intent.getStringExtra(SearchManager.QUERY).orEmpty())
    }

    override fun onStart() {
        super.onStart()
        (application as BGRadioApp).container.player.connect()
    }

    override fun onStop() {
        // Releasing the controller does not stop playback — the service keeps running.
        (application as BGRadioApp).container.player.release()
        super.onStop()
    }
}
