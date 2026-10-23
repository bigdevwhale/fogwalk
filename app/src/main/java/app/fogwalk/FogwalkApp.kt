package app.fogwalk

import android.app.Application
import android.content.Context
import app.fogwalk.data.DemoSeed
import app.fogwalk.data.FogDatabase
import app.fogwalk.data.SettingsStore
import app.fogwalk.domain.FogStore
import app.fogwalk.explore.Explorer
import app.fogwalk.explore.LocationFeed
import app.fogwalk.explore.StepCounter
import app.fogwalk.notify.Alarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class AppContainer(context: Context) {
    val db = FogDatabase.create(context)
    val settings = SettingsStore(context)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val fog = FogStore()
    val explorer = Explorer(db, settings, fog, scope)
    val steps = StepCounter(context.applicationContext, explorer)
    val feed = LocationFeed(context.applicationContext, explorer) { steps.setActive(it) }

    /** True while an activity is on screen; walk pings stay quiet then. */
    @Volatile
    var visible = false
}

class FogwalkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Alarms.ensureChannels(this)
        // Tiles come from openstreetmap.org, whose policy asks for an app-specific user agent.
        Configuration.getInstance().apply {
            userAgentValue = "Fogwalk/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
            tileFileSystemCacheMaxBytes = 300L * 1024 * 1024
            tileFileSystemCacheTrimBytes = 250L * 1024 * 1024
        }
        // The pedometer follows its switch in the settings.
        container.scope.launch(Dispatchers.Main) {
            container.settings.settings.map { it.steps }.distinctUntilChanged().collect { container.steps.setEnabled(it) }
        }
        container.scope.launch {
            if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@FogwalkApp, container.db, container.settings)
            container.explorer.load()
            if (BuildConfig.DEMO && DemoSeed.here.first != 0.0) {
                container.explorer.submit(app.fogwalk.explore.Fix(DemoSeed.here.first, DemoSeed.here.second, 6f, System.currentTimeMillis(), bearing = 40f, simulated = true))
            }
            // Re-arm on every start: OEM task killers (HyperOS & co.) silently drop alarms.
            Alarms.reschedule(this@FogwalkApp)
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as FogwalkApp).container
