package app.fogwalk.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import app.fogwalk.R

/** The painted figures of the map: companions (#41) and monuments (#50), decoded once and kept. */
class Sprites(private val context: Context) {
    private val cache = HashMap<String, Bitmap?>()

    operator fun get(name: String): Bitmap? = synchronized(cache) {
        cache.getOrPut(name) { ids[name]?.let { BitmapFactory.decodeResource(context.resources, it) } }
    }

    companion object {
        /** Named in code, so resource shrinking keeps them. */
        val ids = mapOf(
            "companion_hedgehog" to R.drawable.companion_hedgehog,
            "companion_cat" to R.drawable.companion_cat,
            "companion_fox" to R.drawable.companion_fox,
            "companion_owl" to R.drawable.companion_owl,
            "companion_elder" to R.drawable.companion_elder,
            "companion_granny" to R.drawable.companion_granny,
            "monument_1" to R.drawable.monument_1,
            "monument_2" to R.drawable.monument_2,
            "monument_3" to R.drawable.monument_3,
            "monument_4" to R.drawable.monument_4,
            "chest_unknown" to app.fogwalk.ui.Art.chest(null),
        ) + app.fogwalk.domain.Rarity.entries.associate { "chest_${it.ordinal}" to app.fogwalk.ui.Art.chest(it) } +
            app.fogwalk.domain.ShrineKind.entries.associate { "shrine_${it.name}" to app.fogwalk.ui.Art.shrine(it) }
    }
}
