# Changelog

<!--
Telegram: end each version's section with a hidden block for the StillCraft chat. Start it with a
line "<!-- tg", then 2-4 short lines in Russian for users (what is new, what is fixed), no markdown,
then close it like any HTML comment. The release notes carry it, and the chat bot posts those lines.
-->

All notable changes to Fogwalk. Versions follow [semantic versioning](https://semver.org/).

## [1.10.0] - 2026-11-13
### Added
- **Bring the mist back:** pick a stretch of a journey on its route, and what a GPS glitch charted there goes back under the mist (#46).
- **Altars in the shop,** for gold with nothing left to buy: 1500, then 3000, then 6000, three a week (#51).
- **The grappling hook as gear:** five levels, 30 m farther each, and a second and third throw a day (#46).
- **A chest you can't reach** can be taken off the map until Monday; walk up to it and it still opens (#46).
- **A shrine reached is shown big,** like a chest, with everything it gave (#52).
- **Companions:** a hedgehog, a cat, a fox, an owl, a wanderer or a granny can walk the map in your place, bought for good in the shop (#41).
- **Monuments** on lands you have charted a quarter of: each level costs twice the last, with no ceiling, and the stone grows from a standing stone to a tower on the map (#50).
- **A story for the mist:** three painted chapters open the game: the Wayfarers' Atlas, the Mist of Forgetting that makes the world forget, and the walker who brings it back step by step. The screens speak of it too: lands are remembered, the Chronicle counts what is restored to the Atlas, the quests hang on the Guild's board. Walkers who began before it see the story once on their next launch, without the permission pages; it can be read again from the settings.
### Changed
- **Everything painted anew:** relics, chests, shrines, the shop's wares and the game's own icons (tabs, deeds, arms, figures) are hand-painted pictures now, one set with the companions. Controls stay plain.
- **The shop, room by room:** a painted banner over each room (looks, charms, maps, gear, monuments, tavern), wares as tiles, and a sheet with the whole story and the price to buy.
- **Quests, two boards:** a notice board over today's errands and the week's, the oath folded into a card that opens its vows and stakes in a sheet.
- **The map as an old atlas:** browned edges with mist drifting along them, a painted compass rose that turns with the map and sets north up on a tap, and parchment under its panels.
- **Panels of the atlas:** cards are old paper with a gilt edge, a fine inner rule and gold corners instead of plain white; the map's buttons and the open tab are brass medallions; the tab bar sits on paper under a double gilt rule; the shop's rooms are painted signs.
- **A race's start and finish are pennants,** and the shadow walks as your companion in smoke, so neither passes for a second walker. A tap on either flag opens the race's card, with a way to call it off, and the route leaves out GPS leaps no walker could make.
- **Settings, shorter:** four cards of painted lines with a word each (reminders, look, steps, more), the theme, language and units as one-tap choices, the backup, the story and the community in one card, and the app's details in small print at the foot.
- **The chronicle, lighter:** the walker's title over an old atlas, one card of figures with all time and today, three doors to deeds, relics and arms, and the lands and journeys folded to the first few.
- **The scroll of wandering** can be read again the same day, each time at twice the price (#46).
- **The map turns only on a plain twist** of two fingers, not with a pinch (#45).
- **The boots' next level** gives both distances, the chest's and the shrine's (#48).
### Fixed
- **A GPS glitch no longer charts the streets and opens the chests along it** (#53): fixes running off faster than any walker are left behind even when the satellites claim a stroll, and while the pedometer hears steps nothing faster than a runner counts.
<!-- tg
У тумана появилась история: Туман Забвения стирает мир из памяти, и только шаги странника возвращают его в Атлас. Те, кто уже играет, увидят её при следующем запуске.
Игра нарисована заново: реликвии, сундуки, святилища и значки, а Карта, Лавка, Квесты, Летопись и Настройки стали проще и похожи на страницы старого атласа.
По карте может шагать спутник — ёжик, кот, лис, сова, странник или бабушка, а на освобождённых землях можно ставить памятники.
Сбой GPS больше не открывает туман и сундуки вдоль ложного пути, а если такое уже случилось, туман на отрезке можно вернуть.
-->

## [1.9.0] - 2026-11-08
### Added
- **Delete a journey** from the chronicle: its route and record go, the cells it cleared, its finds and coins stay (#43).
- **How altars work:** a guide from the altars line, with the steps, an example and the limits (#28).
### Changed
- **Backups carry what you bought:** fog styles, gear, wards of the streak and altars move to a new phone too, and merging keeps the more of each side (#22).
- **One name for each boost:** the grove's gift is the same torch and ward of the streak the shop sells (#42), and the shop says how wide the torch burns: «112 m instead of 70 m» (#40).
<!-- tg
Странствие теперь можно удалить из летописи, а у алтарей появился понятный гайд.
При переносе на новый телефон резервная копия забирает и покупки: стили тумана, снаряжение, обереги и алтари.
Факел и оберег серии называются одинаково и в Лавке, и в даре святилища, а у факела написан точный радиус.
-->
## [1.8.0] - 2026-11-07
### Added
- **Zoom buttons** on the map, for one hand (#38).
- **Turn the map** with two fingers; a compass button, shown while it is turned, sets north back up (#37).
### Changed
- **Shrines stand by paths.** Once the day's shrines are placed, Fogwalk asks OpenStreetMap for the footways and streets near them and moves each onto the nearest one, away from private ground and from industrial, military, railway and building-site land (#21). Without a connection they stay where they fell.
- **2,000 steps keep the streak** too, so a walk along known streets counts even with no new cells (#39).
### Fixed
- The dot no longer jumps about while there's no good GPS fix (indoors, among tall buildings, where GPS is jammed): it follows only a fix as good as the one shown, or one inside its circle, or a fresh one after 15 seconds (#35).
<!-- tg
Кнопки зума на карте, а саму карту можно повернуть двумя пальцами (компас вернёт север наверх).
Святилища теперь встают у дорожек и улиц, а не посреди заводов и закрытых территорий.
2000 шагов за день тоже сохраняют серию, а точка на карте больше не скачет без хорошего GPS.
-->
## [1.7.0] - 2026-11-03

### Added
- **Steps from Health Connect.** Turn it on in **Settings → Feel** and Fogwalk shows the steps Samsung Health, Mi Fitness, Zepp Life, Google Fit, watches and bands counted, for the whole day and for each journey, not only what the phone heard while a journey ran. Steps a watch hands over late arrive on the next look; a count once shown is never lowered. While it is on, the phone's pedometer only tells walking from resting and no longer adds steps of its own, so nothing is counted twice (#32).

### Fixed
- A long rank such as "Legend of the Realm" no longer gets cut off on the chronicle postcard; it shrinks to fit (#30).

<!-- tg
Новое: шаги из Health Connect — включите в Настройках, и Fogwalk покажет шаги из Mi Fitness, Samsung Health, Google Fit, часов и браслетов за весь день и за каждое странствие.
Исправлено: длинное звание вроде «Легенда королевства» больше не обрезается на открытке летописи.
-->

## [1.6.0] - 2026-11-02

### Added
- **Postcards.** Share your chronicle, a journey, or a deed, relic, set, banner, race or level from its celebration, as a 1080 × 1920 postcard framed like a page from an old atlas, by day or by night. The chronicle's card shows the shape of the ground you have charted in the mist, a journey's card its route and the swath it cleared; the map can be turned off to keep your streets to yourself. Every card carries a small QR code to Fogwalk on Google Play.
- **Today in the chronicle.** The cells, distance, chests and lands of the day, next to the totals (#25).

### Changed
- **The map is the map again.** The panel under it folds each pursuit (treasure map, tavern rumour, race, altars, shrines) into a compass chip with its distance and a needle toward it; tap one to open its line and see it on the map. With everything under way the panel takes about a third of the room it did.
- A land's card on the map wears the land's arms instead of a magnifier, in colour once its banner is raised (#24).

### Fixed
- Setting out without location access no longer closes the app on Android 8–11.

<!-- tg
Новое: открытки — делитесь летописью, странствием, деянием или реликвией в сторис, с QR-кодом на Fogwalk. Карту на открытке можно выключить.
Карта снова видна: цели в нижней панели свернулись в компактные значки со стрелкой и расстоянием, нажмите — откроется подробнее.
В летописи появились итоги дня, а в карточке земли — её герб.
-->

## [1.5.1] - 2026-10-29

### Fixed
- Sitting at home no longer adds kilometres to the day. Indoors the satellites drift between neighbouring houses and still report a walking speed; with the pedometer on, distance now counts only while it hears steps. A bike or a scooter at riding pace still counts (#20).

<!-- tg
Исправлено: дома телефон больше не «гуляет» между соседними домами и не набирает километры.
Если включён шагомер, расстояние засчитывается только когда он слышит шаги; велосипед и самокат считаются как раньше.
-->

## [1.5.0] - 2026-10-28

### Added
- **Three shrines a day.** Golden, grove and azure shrines awaken each in its own direction, so one at least can be reached when the others stand behind fences. Only one wakes a day. The golden adds 60 gold; the grove adds 80 XP and lights a lantern or a seeing stone, or gives a streak shield; the azure gives an altar. Pick one to set out for, and the others won't wake on the way by chance (#15).
- **Altars.** Set altars from azure shrines around a place you can't walk into, each within 400 m of the last, then close the ring: the mist inside it lifts (#14).
- **Routes kept at hand.** A star on a journey keeps it at the top of the history, ready for races. A race is now a journey of its own, and after a win the new route can take the beaten one's place (#19).

### Changed
- The shadow's route is drawn in violet, apart from your own green trail (#19).
- The shrines are shown from their row in the map panel, which can be tapped like the treasure map's; the round shrine button is gone (#18).
- Relic tiles are all the same size, and timers on the map no longer break onto two lines (#18).

<!-- tg
Новое: каждый день три святилища — золотое, рощи и лазурное, в разных сторонах. Пробуждается одно, можно выбрать цель.
Алтари из лазурных святилищ: поставьте их вокруг места, куда не пройти, замкните круг — и туман внутри рассеется.
Избранные маршруты для гонок с тенью, а выигранную гонку можно закрепить вместо старого маршрута.
Исправлено: ровная сетка реликвий, таймеры на карте в одну строку, путь тени теперь фиолетовый.
-->

## [1.4.0] - 2026-10-26

### Added
- **Relic stars.** A relic found again now counts: the 2nd, 4th, 8th, 16th and 32nd copy raise it to ★1…★5, and a set whose every relic reaches a star pays again, more for each star. Copies found before this update count too. A copy still brings its gold.
- **Seasonal sets.** Four new sets of six, from the Midwinter Hoard to the Harvest Relics, each turning up in chests, treasure maps and oaths only in its own season (turned around south of the equator). Sixty relics in all.
- **Arms of the lands.** Every land bears one of 24 arms. Charting a quarter of a land raises its banner, and the first banner of each kind brings 100 gold. Lands charted before this update raise theirs at once. The chronicle shows the roll of arms, and the arms of each land in the list.
- 33 new deeds, 209 in all: relic stars, five-star relics, seasonal sets, arms, and longer ladders for relics, sets and charted lands.

### Fixed
- Turning on location after Fogwalk is already open now works at once; before, the app had to be closed and started again to find you.

<!-- tg
Новое: звёзды реликвий — повторные находки поднимают реликвию до пяти звёзд, а наборы награждают за каждую звезду.
Четыре сезонных набора: их реликвии попадаются только в свой сезон. Сейчас осень — «Реликвии урожая».
Гербы земель: нанесите землю на карту, чтобы поднять её знамя, и соберите все 24 герба.
И 33 новых деяния.
Исправлено: если включить геолокацию уже после запуска, приложение сразу находит вас, без перезапуска.
-->

## [1.3.2] - 2026-10-25

### Added
- Community links in settings: report a bug, suggest an idea or follow updates through the StillCraft bot on Telegram, and the StillCraft chat.

<!-- tg skip -->

## [1.3.1] - 2026-10-24

### Added
- Fogwalk comes to Google Play.

### Fixed
- Choosing a language in the settings works in the Google Play install too: Play kept only the phone's own language, so the switch found nothing to switch to.

## [1.3.0] - 2026-10-23

### Added
- **A pedometer**, if you want one: the first launch asks, and a switch in the settings turns it on or off at any time. Steps counted by the phone's own sensor show on the map, in the walk's notification and in the history of journeys. Backups carry them.

### Changed
- A kept oath stays on its card after it is claimed, with what was vowed and what it brought.
- A weekly quest that is done no longer counts the days left.

### Fixed
- Chests within your senses shimmer in their rarity again while the seeing stone is lit; since 1.2.0 the stone hid them as unknown.
- Times in the history of journeys could come out garbled ("12 ├minute: 2┤") under some regional settings.

## [1.2.0] - 2026-10-22

### Added
- **The grappling hook.** A chest behind a fence or across a pond can be pulled in from up to 120 m past your reach: tap it and throw, for 20 gold and a coin for every two metres. One throw a day.
- Tapping a chest or the shrine draws a dashed ring around it: step inside and it is yours.
- Zoom with one thumb: tap, then tap again and hold, and slide down to come closer or up to step back.
- Tapping your gold on the map opens the merchant.

### Changed
- The merchant has a tab of its own, with quick links to each kind of ware, a line on how long each kind lasts, and larger type. Relics moved to the Chronicle.
- Seven-league boots now reach the shrine from farther too: 40 m bare, up to 60 m.
- The seeing stone shows where chests lie, but through the mist not what they are worth.

### Fixed
- Back from the quests, the merchant or the chronicle returns to the map instead of closing the app.

## [1.1.2] - 2026-10-17

### Fixed
- A chest's card on the map now says how close you need to come with your seven-league boots on, not the bare 30 m.

## [1.1.1] - 2026-10-15

### Fixed
- The shrine of the day appears again as soon as the app knows roughly where you are, indoors too. Since 1.0.1 it waited for a precise satellite fix, so on days begun at home it could stay asleep.
- Indoors your position shows on the map again, however rough, with its circle of uncertainty, until the satellites answer.

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
