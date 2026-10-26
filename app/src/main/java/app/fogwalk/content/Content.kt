package app.fogwalk.content

import app.fogwalk.domain.Badge
import app.fogwalk.domain.Quest
import app.fogwalk.domain.Rarity
import java.util.Locale

/** Words of the world in one language: relics, sectors, ranks, badges, quests and nudges. */
interface Content {
    fun rarity(r: Rarity): String
    /** "a rare relic", "редкая реликвия". */
    fun relicOf(r: Rarity): String
    fun relicName(id: String): String
    fun relicLore(id: String): String
    fun setName(set: String): String
    /** Title earned by completing [set]. */
    fun setTitle(set: String): String
    /** "Autumn", for the seasonal sets. */
    fun seasonName(set: String): String
    /** The charge on a land's arms, see [app.fogwalk.domain.Arms]. */
    fun armName(arm: Int): String
    fun rank(index: Int): String
    fun sectorName(sector: Long): String
    fun badgeTitle(id: String): String
    /** Heading for a kind of deed in the book of deeds. */
    fun groupName(group: String): String
    fun badgeText(b: Badge): String
    fun styleName(id: String): String
    fun styleText(id: String): String
    fun quest(q: Quest): String
    /** The vow itself, e.g. "Walk 12 km by Sunday". */
    fun oath(o: app.fogwalk.domain.Oath): String
    /** The evening nudge, rotating by [day]. */
    fun nudge(day: Long, streak: Boolean): String
    /** A line for the map's empty fog, rotating by [day]. */
    fun motto(day: Long): String

    companion object {
        fun of(locale: Locale): Content = if (locale.language == "ru") ContentRu else ContentEn

        /** Adjective and noun indexes for a sector: the same pair in every language. */
        fun sectorWords(sector: Long, adjectives: Int, nouns: Int): Pair<Int, Int> {
            var h = sector * -0x61c8864680b583ebL
            h = h xor (h ushr 29)
            val u = (h and Long.MAX_VALUE)
            return (u % adjectives).toInt() to ((u / adjectives) % nouns).toInt()
        }

        fun km2(km2: Double): String = when {
            km2 < 0.1 -> String.format(Locale.ROOT, "%.2f", km2)
            km2 < 10 -> String.format(Locale.ROOT, "%.1f", km2).removeSuffix(".0")
            else -> km2.toInt().toString()
        }
    }
}
