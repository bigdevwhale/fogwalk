# Changelog

All notable changes to Fogwalk. Versions follow [semantic versioning](https://semver.org/).

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
