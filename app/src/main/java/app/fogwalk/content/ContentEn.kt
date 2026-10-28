package app.fogwalk.content

import app.fogwalk.domain.Badge
import app.fogwalk.domain.Goal
import app.fogwalk.domain.Quest
import app.fogwalk.domain.Rarity

object ContentEn : Content {
    override fun rarity(r: Rarity) = when (r) {
        Rarity.COMMON -> "Common"
        Rarity.RARE -> "Rare"
        Rarity.EPIC -> "Epic"
        Rarity.LEGENDARY -> "Legendary"
    }

    override fun relicOf(r: Rarity) = when (r) {
        Rarity.COMMON -> "a common relic"
        Rarity.RARE -> "a rare relic"
        Rarity.EPIC -> "an epic relic"
        Rarity.LEGENDARY -> "a legendary relic"
    }

    private val relics = mapOf(
        "compass" to ("Brass Compass" to "Its needle wavers toward places you have never been."),
        "boots" to ("Worn Boots" to "Someone walked a thousand streets in these. Now it's your turn."),
        "lantern" to ("Tin Lantern" to "Lit once, it never quite goes out."),
        "staff" to ("Walking Staff" to "Carved with the initials of every owner. There's room for yours."),
        "flask" to ("Water Flask" to "Always cool, however long the road."),
        "rope" to ("Coil of Rope" to "Forty feet of good hemp and a knot no one can untie."),
        "torn_map" to ("Torn Map" to "Half a city. The other half is out there somewhere."),
        "sea_chart" to ("Sea Chart" to "Coastlines drawn by a sailor who never saw them."),
        "star_chart" to ("Star Chart" to "Constellations that only appear above unexplored streets."),
        "city_plan" to ("City Plan" to "Every alley marked, every name misspelled."),
        "treasure_map" to ("Treasure Map" to "The X keeps moving. Probably on purpose."),
        "nowhere_map" to ("Map of Nowhere" to "Blank paper that slowly fills as you walk."),
        "watch" to ("Pocket Watch" to "Runs fast when you rest and slow when you wander."),
        "spyglass" to ("Spyglass" to "Look through it and the fog thins a little."),
        "music_box" to ("Music Box" to "Plays a tune from the street where it was found."),
        "hourglass" to ("Hourglass" to "The sand falls upward on long walks."),
        "quill" to ("Cartographer's Quill" to "It draws the roads before you reach them."),
        "snow_globe" to ("Snow Globe" to "A tiny town inside. The lights are on."),
        "moonstone" to ("Moonstone" to "Glows faintly when the moon is up and you are out."),
        "amber" to ("Amber Drop" to "A tiny map is trapped inside, older than any city."),
        "pearl" to ("River Pearl" to "Found where a river once ran under the street."),
        "fossil" to ("Fossil Shell" to "This was the seabed. Now it's your neighbourhood."),
        "geode" to ("Geode" to "Plain outside, a whole cave of crystals within."),
        "meteorite" to ("Meteorite Shard" to "Fell from the sky to be found by a walker."),
        "dragon_scale" to ("Dragon Scale" to "Warm to the touch. Don't ask where the dragon is."),
        "phoenix_feather" to ("Phoenix Feather" to "Burns without heat and never turns to ash."),
        "mermaid_comb" to ("Mermaid's Comb" to "Smells of salt, though the sea is far away."),
        "griffin_claw" to ("Griffin Claw" to "Points north, as all good griffins do."),
        "unicorn_hair" to ("Unicorn Hair" to "A single silver strand that hums in the wind."),
        "kraken_ink" to ("Kraken Ink" to "Write a street's name in it and you'll never forget the way."),
        "comet_tail" to ("Comet Tail" to "A wisp of light caught on the way past Earth."),
        "star_seed" to ("Star Seed" to "Plant it in the dark and something will grow."),
        "sun_coin" to ("Sun Coin" to "Minted at noon on the longest day of the year."),
        "moon_key" to ("Moon Key" to "Opens a door that only exists after midnight."),
        "aurora_ribbon" to ("Aurora Ribbon" to "Green and violet, it moves on its own."),
        "void_pearl" to ("Void Pearl" to "Holds the dark between the stars. The rarest thing a walker finds."),
        "frost_bell" to ("Frost Bell" to "Rings only when it is cold enough to see your breath."),
        "icicle_wand" to ("Icicle Wand" to "Never melts in the hand that found it."),
        "yule_candle" to ("Yule Candle" to "Burns the whole long night and is whole again by morning."),
        "sleigh_bell" to ("Sleigh Bell" to "Somewhere far off, a sleigh answers."),
        "frost_rose" to ("Frost Rose" to "Blooms on window panes, but this one came loose."),
        "winter_crown" to ("Crown of Midwinter" to "Worn by whoever walks the longest night to its end."),
        "first_bud" to ("First Bud" to "The first green of the year, tucked into a pocket."),
        "nest_egg" to ("Nest Egg" to "Speckled blue and still warm."),
        "rain_flute" to ("Rain Flute" to "Play it and the puddles hum along."),
        "thaw_stone" to ("Thaw Stone" to "Ice gives way wherever it is set down."),
        "cuckoo_clock" to ("Cuckoo Clock" to "Calls the hour of the first warm day, and is never wrong."),
        "dryad_seed" to ("Dryad's Seed" to "A whole wood sleeps in it, waiting for spring."),
        "sunhat" to ("Straw Sunhat" to "Always a little shade, wherever the road runs."),
        "firefly_jar" to ("Jar of Fireflies" to "Let them out at dusk; they always come back."),
        "shell_horn" to ("Shell Horn" to "Blow it in the city and hear the sea."),
        "honey_jar" to ("Wild Honey" to "Gathered from every flower on your summer walks."),
        "midsummer_fern" to ("Midsummer Fern" to "Flowers once a year, on the shortest night."),
        "solstice_mirror" to ("Solstice Mirror" to "Holds the light of the longest day and gives it back in winter."),
        "maple_leaf" to ("Crimson Leaf" to "Pressed between the pages of an old atlas."),
        "acorn" to ("Golden Acorn" to "An oak would grow from it, if gold could grow."),
        "harvest_sickle" to ("Harvest Sickle" to "Cuts the mist like wheat."),
        "lantern_gourd" to ("Lantern Gourd" to "A grin carved by no one, lit by nothing."),
        "crane_feather" to ("Crane Feather" to "Dropped by the last flock heading south."),
        "hearth_ember" to ("Hearth Ember" to "A coal from the first fire of autumn. It keeps every walker warm."),
    )

    override fun relicName(id: String) = relics[id]?.first ?: id
    override fun relicLore(id: String) = relics[id]?.second ?: ""

    override fun setName(set: String) = when (set) {
        "winter" -> "Midwinter Hoard"
        "spring" -> "Gifts of the Thaw"
        "summer" -> "Midsummer Treasures"
        "autumn" -> "Harvest Relics"
        "wayfarer" -> "Wayfarer's Pack"
        "maps" -> "Maps of Old"
        "curios" -> "Wizard's Curios"
        "stones" -> "Gems of the Deep"
        "myths" -> "Beasts of Legend"
        else -> "Gifts of the Heavens"
    }

    override fun setTitle(set: String) = when (set) {
        "winter" -> "Frostwalker"
        "spring" -> "Herald of Spring"
        "summer" -> "Child of the Sun"
        "autumn" -> "Harvest Keeper"
        "wayfarer" -> "Seasoned Wayfarer"
        "maps" -> "Keeper of Maps"
        "curios" -> "Apprentice of Wonders"
        "stones" -> "Delver of the Deep"
        "myths" -> "Dragonfriend"
        else -> "Child of the Stars"
    }

    override fun seasonName(set: String) = when (set) {
        "winter" -> "Winter"
        "spring" -> "Spring"
        "summer" -> "Summer"
        else -> "Autumn"
    }

    private val arms = listOf(
        "Tower", "Anchor", "Oak", "Ship", "Key", "Star", "Crescent", "Sun", "Rose", "Leaf", "Flame", "Gem",
        "Wolf", "Bell", "Spring", "Thunderbolt", "Mountain", "Hearth", "Bee", "Pinewood", "Cloud", "Snowflake", "Wave", "Lantern",
    )

    override fun armName(arm: Int) = arms[arm.coerceIn(0, arms.size - 1)]

    private val ranks = listOf("Wanderer", "Squire", "Ranger", "Pathfinder", "Cartographer", "Knight-Errant", "Lorekeeper", "Legend of the Realm")
    override fun rank(index: Int) = ranks[index.coerceIn(0, ranks.size - 1)]

    private val adjectives = listOf(
        "Whispering", "Silent", "Golden", "Hidden", "Misty", "Amber", "Sleeping", "Windy",
        "Crooked", "Lantern", "Silver", "Elder", "Mossy", "Copper", "Quiet", "Wandering",
        "Velvet", "Moonlit", "Echoing", "Forgotten", "Northern", "Distant", "Starry", "Sunlit",
    )
    private val nouns = listOf(
        "Hollow", "Grove", "Reach", "Heights", "Gate", "Market", "Harbour", "Bend",
        "Meadow", "Crossing", "Garden", "Ridge", "Keep", "Hills", "Court", "Steps",
        "Fields", "Corner", "Wharf", "Square", "Lanes", "Terrace", "Woods", "Vale",
    )

    override fun sectorName(sector: Long): String {
        val (a, n) = Content.sectorWords(sector, adjectives.size, nouns.size)
        return "${adjectives[a]} ${nouns[n]}"
    }

    /** Deed titles by group, one per tier. */
    private val titles = mapOf(
        "km" to listOf("First Steps", "Stretching the Legs", "Morning Stroll", "Ten-League Boots", "Long Road", "Half a Hundred", "Centurion of the Road", "Two Hundred Miles of Song", "Pilgrim", "Five Hundred Leagues", "Tireless", "The Thousand", "Wanderer of Ages", "Two Thousand Roads", "Crosser of Kingdoms", "Walker Between Worlds"),
        "area" to listOf("A Clearing", "Patch of Light", "The Village Green", "Quarter Charted", "Half a League", "The Square League", "Two Leagues of Land", "Barony", "County", "Duchy", "March", "Principality", "Kingdom", "Empire"),
        "cells" to listOf("Hundred Steps", "Five Hundred Stones", "Thousand Tiles", "Mapmaker's Apprentice", "Mapmaker's Journeyman", "Ten Thousand Squares", "Master of the Grid", "Half a Myriad More", "One Hundred Thousand", "Weaver of the World"),
        "cache" to listOf("Lucky Find", "Treasure Hunter", "Pocketful of Loot", "Chest Cracker", "Hoarder", "Hundred Locks", "Vault Breaker", "Dragon's Envy", "Lord of Plunder", "Hoard of Legend"),
        "rare" to listOf("Glimmer of Blue", "Collector of Rarities", "Rare Breed", "Hundred Wonders"),
        "epic" to listOf("Touched by Magic", "Spellbound", "Arcane Hoard"),
        "legend" to listOf("Legendary Luck", "Fortune's Favourite", "Chosen by Fate"),
        "relic" to listOf("Curiosity Shelf", "Cabinet of Wonders", "Museum Wing", "Hall of Relics", "Royal Collection", "Keeper of Treasures", "Six Sets' Worth", "A Season's Worth", "Half the Year Kept", "Three Seasons Kept", "Every Last Relic"),
        "set" to listOf("Complete Set", "Pair of Sets", "Three Sets Whole", "Four Sets Whole", "Five Sets Whole", "Six Sets Whole", "Seven Sets Whole", "Eight Sets Whole", "Nine Sets Whole", "All Ten Sets"),
        "season" to listOf("A Season Gathered", "Two Seasons", "Three Seasons", "The Turning Year"),
        "stars" to listOf("First Glimmer", "Starlit Shelf", "Constellation", "Night Sky", "Hundred Stars", "Milky Way", "Two Hundred Stars", "Firmament"),
        "radiant" to listOf("Radiant Relic", "Three Radiant", "Ten Radiant", "Treasury of Light", "Every Relic Radiant"),
        "sector" to listOf("Beyond the Gate", "Three Lands", "Five Lands", "Ten Lands", "Twenty Lands", "Thirty Lands", "Fifty Lands", "Seventy-Five Lands", "Hundred Lands", "Two Hundred Lands"),
        "charted" to listOf("Charted Land", "Three Charted", "Five Charted", "Ten Charted", "Twenty-Five Charted", "Fifty Charted", "Hundred Charted"),
        "arms" to listOf("First Banner", "Three Banners", "Six Banners", "Twelve Banners", "Eighteen Banners", "Herald of the Realm"),
        "half" to listOf("Lord of a Land", "Lord of Three", "Lord of Ten"),
        "streak" to listOf("Kindled", "A Week of Roads", "Fortnight Faithful", "Moon's Turn", "Two Moons", "Hundred Dawns", "Unbroken", "A Year and a Day"),
        "beacon" to listOf("First Shrine", "Pilgrim's Path", "Ten Shrines", "Shrine Seeker", "Keeper of Flames", "Hundred Altars", "High Priest of Paths"),
        "quest" to listOf("First Quest", "Errand Runner", "Adventurer", "Questing Knight", "Hero of the Realm", "Saga", "Epic Cycle"),
        "weekly" to listOf("Great Quest", "Five Great Quests", "Ten Great Quests", "Champion of Weeks", "A Year of Great Quests"),
        "days" to listOf("Seven Days Out", "A Month on the Road", "Two Moons Out", "Hundred Days", "Two Hundred Days", "A Year of Roads"),
        "bigday" to listOf("Busy Day", "Big Day Out", "Day of Wonders", "Day of Legend"),
        "longday" to listOf("Five-League Day", "Ten-Kilometre Day", "Fifteen Strong", "Twenty Strong", "Thirty Strong", "Marathon of the Realm"),
        "night" to listOf("Night Owl", "Moonwalker", "Shadow of the Streets"),
        "dawn" to listOf("Early Bird", "Herald of Dawn", "Sunrise Warden"),
        "level" to listOf("Level Five", "Level Ten", "Level Fifteen", "Level Twenty", "Level Twenty-Five", "Level Thirty", "Level Forty", "Level Fifty"),
        "walks" to listOf("First Journey", "Ten Journeys", "Fifty Journeys", "Hundred Journeys", "Two Hundred Fifty Journeys", "Five Hundred Journeys"),
        "gold" to listOf("Coin Purse", "Heavy Purse", "Dragon's Gold"),
        "style" to listOf("Weather Witch", "Mistcaller", "Master of Mists"),
        "oath" to listOf("Word Kept", "Oathbound", "True to the Word", "Voice of Iron"),
        "treasure" to listOf("X Marks the Spot", "Treasure Seeker", "Digger of Hoards", "Lord of Lost Maps"),
        "race" to listOf("Faster Than Yesterday", "Shadow Chaser", "Outrunner", "Swifter Than Memory"),
        "gear" to listOf("Kitted Out", "Well Equipped", "Seasoned Outfitter", "Fully Arrayed"),
    )

    override fun badgeTitle(id: String): String {
        val group = id.substringBeforeLast('_')
        val tier = id.substringAfterLast('_').toIntOrNull()?.minus(1) ?: 0
        return titles[group]?.getOrNull(tier) ?: id
    }

    override fun groupName(group: String) = when (group) {
        "km" -> "Roads walked"
        "area" -> "Land charted"
        "cells" -> "Cells revealed"
        "cache" -> "Chests opened"
        "rare" -> "Rare chests"
        "epic" -> "Epic chests"
        "legend" -> "Legendary chests"
        "relic" -> "Relics"
        "set" -> "Relic sets"
        "season" -> "Seasonal sets"
        "stars" -> "Relic stars"
        "radiant" -> "Radiant relics"
        "arms" -> "Arms of the lands"
        "sector" -> "Lands visited"
        "charted" -> "Lands charted"
        "half" -> "Lands ruled"
        "streak" -> "Streaks"
        "beacon" -> "Shrines"
        "quest" -> "Quests"
        "weekly" -> "Great quests"
        "days" -> "Days on the road"
        "bigday" -> "Great days"
        "longday" -> "Long days"
        "night" -> "Night journeys"
        "dawn" -> "Dawn journeys"
        "level" -> "Levels"
        "walks" -> "Journeys"
        "gold" -> "Gold"
        "oath" -> "Oaths kept"
        "treasure" -> "Treasures dug up"
        "race" -> "Shadows outrun"
        "gear" -> "Gear"
        else -> "Mists"
    }

    override fun badgeText(b: Badge): String {
        val g = b.goal.toLong()
        val n = String.format(java.util.Locale.US, "%,d", g)
        return when (b.group) {
            "km" -> "Walk $n km in total"
            "area" -> "Chart ${Content.km2(b.goal)} km² of the realm"
            "cells" -> "Reveal $n cells"
            "cache" -> if (g == 1L) "Open your first chest" else "Open $n chests"
            "rare" -> if (g == 1L) "Open a rare chest" else "Open $n rare chests or better"
            "epic" -> if (g == 1L) "Open an epic chest" else "Open $n epic chests or better"
            "legend" -> if (g == 1L) "Open a legendary chest" else "Open $n legendary chests"
            "relic" -> if (g == app.fogwalk.domain.Relics.all.size.toLong()) "Collect every relic" else "Collect $n different relics"
            "set" -> if (g == 1L) "Complete a relic set" else "Complete $n relic sets"
            "season" -> if (g == 1L) "Complete a seasonal set" else "Complete $n seasonal sets"
            "stars" -> "Gather $n stars on your relics"
            "radiant" -> if (g == 1L) "Raise a relic to five stars" else "Raise $n relics to five stars"
            "arms" -> if (g == 1L) "Raise a land's banner" else "Raise banners of $n different arms"
            "sector" -> if (g == 1L) "Set foot in a land" else "Set foot in $n lands"
            "charted" -> if (g == 1L) "Chart a quarter of a land" else "Chart a quarter of $n lands"
            "half" -> if (g == 1L) "Chart half of a land" else "Chart half of $n lands"
            "streak" -> "Journey $n days in a row"
            "beacon" -> if (g == 1L) "Reach a shrine of the day" else "Reach $n shrines"
            "quest" -> if (g == 1L) "Complete a quest" else "Complete $n quests"
            "weekly" -> if (g == 1L) "Complete a great quest of the week" else "Complete $n great quests"
            "days" -> "Be on the road on $n different days"
            "bigday" -> "Reveal $n cells in one day"
            "longday" -> "Walk $n km in one day"
            "night" -> "Reveal $n cells after 22:00"
            "dawn" -> "Reveal $n cells before 7:00"
            "level" -> "Reach level $n"
            "walks" -> if (g == 1L) "Finish your first journey" else "Make $n journeys"
            "gold" -> "Earn $n gold in all"
            "style" -> "Own $n mists"
            "oath" -> if (g == 1L) "Keep an oath" else "Keep $n oaths"
            "treasure" -> if (g == 1L) "Dig up a treasure from a map" else "Dig up $n treasures"
            "race" -> if (g == 1L) "Outrun your shadow" else "Outrun your shadow $n times"
            "gear" -> if (g == 1L) "Improve a piece of gear" else "Improve your gear $n times"
            else -> ""
        }
    }

    override fun styleName(id: String) = when (id) {
        "parchment" -> "Old Parchment"
        "snow" -> "Winter's Breath"
        "midnight" -> "Starlit Night"
        "ember" -> "Dragon's Ash"
        "aurora" -> "Elven Lights"
        else -> "Silver Mist"
    }

    override fun styleText(id: String) = when (id) {
        "parchment" -> "A map from a wizard's tower"
        "snow" -> "Frost over the unknown lands"
        "midnight" -> "Deep night and scattered stars"
        "ember" -> "Smouldering ash of a dragon's flight"
        "aurora" -> "Elven lights over the city"
        else -> "The grey mist of the borderlands"
    }

    override fun quest(q: Quest): String = when (q.goal) {
        Goal.CELLS -> "Reveal ${q.target} new cells"
        Goal.METERS -> "Walk ${km(q.target)} km"
        Goal.CACHES -> if (q.target == 1) "Open a chest" else "Open ${q.target} chests"
        Goal.RARE -> "Open a rare chest or better"
        Goal.BEACON -> "Reach one of the day's shrines"
    }

    override fun oath(o: app.fogwalk.domain.Oath): String = when (o.goal) {
        app.fogwalk.domain.OathGoal.METERS -> "Walk ${km(o.target)} km by Sunday"
        app.fogwalk.domain.OathGoal.CELLS -> "Reveal ${o.target} new cells by Sunday"
        app.fogwalk.domain.OathGoal.CHESTS -> if (o.target == 1) "Open a chest by Sunday" else "Open ${o.target} chests by Sunday"
        app.fogwalk.domain.OathGoal.SHRINES -> if (o.target == 1) "Reach a shrine by Sunday" else "Reach ${o.target} shrines by Sunday"
    }

    private fun km(m: Int) = if (m % 1000 == 0) "${m / 1000}" else String.format(java.util.Locale.ROOT, "%.1f", m / 1000.0)

    private val nudgesStreak = listOf(
        "A short journey keeps the flame alive. Twenty-five new cells will do.",
        "The mist creeps back over today. Go and push it away.",
        "Somewhere near you is a road no one has charted yet. You, at least.",
        "Your streak waits by the door, cloak on.",
    )
    private val nudgesFresh = listOf(
        "Today's shrine has awakened. Shall we see where?",
        "New chests appear every Monday. Some are close.",
        "A quarter of an hour is enough to chart a corner of the realm.",
        "The mist still hides the lands around you. Let's change that.",
    )

    override fun nudge(day: Long, streak: Boolean): String =
        (if (streak) nudgesStreak else nudgesFresh)[Math.floorMod(day, 4L).toInt()]

    private val mottos = listOf(
        "Every road you walk stays on the map.",
        "The mist hides more than roads.",
        "Walk somewhere new today.",
        "Chests refill every Monday.",
        "The shrine always wakes where the mist is thickest.",
        "Turn left where you always turn right.",
        "An atlas is a chronicle written with your feet.",
    )

    override fun motto(day: Long) = mottos[Math.floorMod(day, mottos.size.toLong()).toInt()]
}
