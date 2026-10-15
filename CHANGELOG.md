# Changelog

All notable changes to Fogwalk. Versions follow [semantic versioning](https://semver.org/).

## [1.1.1] - 2026-10-15

### Fixed
- The shrine of the day appears again as soon as the app knows roughly where you are, indoors too. Since 1.0.1 it waited for a precise satellite fix, so on days begun at home it could stay asleep.

## [1.1.0] - 2026-10-15

Gold has somewhere to go, and there is more to do on a walk.

### Added
- **Oath of the week.** Three vows for the rest of the week, easy to heroic, sized to your own pace of the last four weeks. Stake 100 to 2,000 gold; keep the oath and the stake comes back with a relic (rarer for bigger stakes) and experience, break it and the gold is gone.
- **Treasure maps** from the merchant: a circle in the mist 0.8–4 km away with a treasure hidden inside, found by *hot, warm, cool, cold*. A rare, epic or legendary relic inside, a missing one first; the gold inside is a small part of the price.
- **The wanderer's gear**: a mistcloak, hunter's senses, an alchemist's satchel and seven-league boots, each improving in five steps from 300 to 5,000 gold.
- **Tavern rumours**: once a week, for 80 gold, the finest unopened chest within 2 km shows through the mist with an arrow to it.
- **Race your shadow**: walk a past journey again while its shadow retraces it at the old pace.
- 16 new deeds (176 in all) for oaths kept, treasures dug up, shadows outrun and gear.

### Changed
- Chest deeds count chests only, not treasures or oath rewards.

## [1.0.3] - 2026-10-14

### Added
- A history of journeys: every walk by month, with a sketch of its route, and a page for each with the route on the map, distance, time, pace, new cells and chests. Routes are kept from this version on; backups now carry them.

### Fixed
- The mist no longer shimmers while the map moves. It is drawn once, pinned to the world, and only slid along as you walk or pan; it is redrawn sharp when new ground opens or a zoom comes to rest.

## [1.0.2] - 2026-10-14

### Changed
- Land names no longer sit on the map. Tap a land to see its name, how much of it is charted and the next reward; the land lights up until you tap it again.

## [1.0.1] - 2026-10-14

### Fixed
- Walks no longer zigzag or count several times the real distance. Coarse network positions only place the dot until the satellites answer, a filter tuned to walking pace smooths the rest, lone wild fixes are dropped, and standing still adds nothing.
- No more "too fast" warnings on foot: a vehicle is now told by the satellites' own speed, or by several fast steps in a row, never by a single jumpy fix.
- Chests and the shrine are found anywhere along the way between two positions, so a jumpy fix no longer walks you past them.

### Added
- The borders of the lands on the map, the one you are in drawn bolder, with each visited land's name and how much of it is charted.

## [1.0.0] - 2026-10-14

The first release.

### The mist
- An OpenStreetMap atlas in sepia (night ink in the dark theme) hidden under a cloudy mist. Everything within 40 m of your path is revealed for good, as soft clearings.
- Journeys with the screen off in a quiet notification. Anything faster than a bike doesn't count.
- Six mists to choose from: Silver Mist, Old Parchment, Winter's Breath, Starlit Night, Dragon's Ash and Elven Lights.

### Things to find
- Common, rare, epic and legendary treasure chests all over the map, refilled every Monday. Nearby ones glimmer through the mist; a call when one is close.
- A shrine of the day 0.6–1.7 km away where the mist is thickest, with an arrow pointing the way.
- 36 relics in six sets, with a title and gold for every full set.
- Named lands about a kilometre across, with rewards for charting 5, 15, 25 and 40 % of each.

### Reasons to come back
- Three new quests every dawn and a great quest every week.
- A streak with wards that guard a missed day.
- 160 deeds in 25 ladders, from 1 km to 5,000 km walked, from a clearing to a kingdom charted.
- Levels and ranks from Wanderer to Legend of the Realm, with gold at every level.
- A merchant: mists, a torch for a wider path, a seeing stone that shows every chest, wards and a scroll that moves the shrine.
- An evening reminder on days you haven't been out yet.

### Chronicle
- Charted area and your share of the known world, lands with their progress, days on the road, recent journeys and the book of deeds.
- Backup to and from a JSON file.

### Everywhere
- English and Russian, written natively; Russian land names agree in gender.
- A storybook onboarding: a little realm under mist, a traveller, a chest at the end of the road.
- No accounts, no ads, no analytics. Your path never leaves the phone.
