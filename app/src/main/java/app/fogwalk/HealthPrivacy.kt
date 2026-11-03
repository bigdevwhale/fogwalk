package app.fogwalk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri

/** Opened by Health Connect to show why the app wants the steps: the privacy policy, in the browser. */
class HealthPrivacy : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, URL.toUri())) }
        finish()
    }

    private companion object {
        const val URL = "https://github.com/bigdevwhale/fogwalk/blob/main/PRIVACY.md"
    }
}
