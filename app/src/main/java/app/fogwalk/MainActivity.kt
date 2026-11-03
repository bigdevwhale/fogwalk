package app.fogwalk

import android.content.Intent
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fogwalk.data.Settings
import app.fogwalk.explore.ExploreService
import app.fogwalk.notify.Alarms
import app.fogwalk.ui.FogwalkRoot
import app.fogwalk.ui.theme.FogwalkTheme
import app.fogwalk.ui.theme.isDark
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    /** Screen requested from outside: a notification. */
    private val openRoute = MutableStateFlow<String?>(null)
    /** Reads Health Connect while the app is in sight. */
    private var healthSync: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val dark = isDark((settings ?: Settings()).theme)
            LaunchedEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            FogwalkTheme(dark) {
                // Wait for settings so the theme doesn't flash light-then-dark.
                if (settings != null) FogwalkRoot(openRoute = openRoute, settings = settings!!)
            }
        }
        // A walk the system cut short (task killer, reboot) picks up again when the app is opened.
        container.scope.launch {
            if (container.settings.now().walking && container.feed.permitted()) runOnUiThread { ExploreService.start(this@MainActivity) }
        }
    }

    override fun onStart() {
        super.onStart()
        container.visible = true
        healthSync = container.scope.launch {
            while (true) { container.syncHealth(); delay(60_000) }
        }
    }

    override fun onStop() {
        container.visible = false
        healthSync?.cancel()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        intent?.getStringExtra(Alarms.EXTRA_ROUTE)?.let { openRoute.value = it }
    }
}
