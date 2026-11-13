# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow теперь кладёт в GitHub Release ещё и `Fogwalk-v<версия>.aab`.
  Для Play — `Fogwalk-v1.3.1.aab` из релиза v1.3.1, подписан тем же ключом, что и APK (секреты `SIGNING_*` заданы).
- **Языки.** В `app/build.gradle.kts` отключено деление AAB по языкам (`bundle.language.enableSplit = false`).
  Без этого Play ставит только язык телефона, и переключение на русский в настройках не находит строк.
  Не удаляйте этот блок.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Так `app.fogwalk` остаётся за одним
  зарегистрированным ключом, а APK из GitHub и версия из Play обновляют друг друга.
  Если оставить «Google создаст ключ подписи», версии из Play и GitHub друг на друга не встанут.
- **Package name `app.fogwalk` нужно сначала зарегистрировать.** Fogwalk уже ставится из GitHub, поэтому
  Play Console при создании приложения пишет «You can't use this package name because it hasn't been
  registered». Порядок ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.fogwalk`.
  2. **Select key** → ключ с SHA-256 `ee2862dcf3be1212cb3ac7679b613f5ff85b140ffdcc781e0ef96f63eeef8838`
     (сертификат CN=Fogwalk, O=Stillcraft — им подписаны все релизы).
  3. **Upload APK** → скопировать сниппет, собрать релизный APK с файлом `assets/adi-registration.properties`,
     в котором этот сниппет, подписать тем же ключом и загрузить. Ключ есть только в секретах GitHub,
     так что APK собирает workflow **Package registration proof** (Actions → Run workflow → вставить сниппет),
     APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд.
- **Политика конфиденциальности** — `PRIVACY.md` в корне репозитория. После push ссылка:
  `https://github.com/bigdevwhale/fogwalk/blob/main/PRIVACY.md`
  (или перенесите текст на сайт Stillcraft).
- **Видео для фоновой службы.** Play требует ролик (YouTube, можно «доступ по ссылке») для разрешения
  `FOREGROUND_SERVICE_LOCATION`, см. раздел 4. Снимите его заранее: 20–40 секунд записи экрана —
  нажать «В путь», показать уведомление, выключить экран, пройтись, включить — путь нанесён.

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Fogwalk: Explore Your City` |
| Default language | English (United States) – en-US |
| App or game | **App** (см. «Категория» в разделе 3) |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…7.png` | `graphics/screenshots-ru/1…7.png` |
| Tablet screenshots | можно не загружать | — |
| Video | пусто | — |

Скриншоты сняты с демо-сборки (`./gradlew installDebug -Pdemo`, старая Прага) в 1080×2340, строка
состояния обрезана. Порядок: карта, история, квесты, лавка со спутниками, реликвии, летопись, деяния.
Все картинки (эти скриншоты с подписями, обложка, галерея README и сайта) собирает
`python tools/store_shots.py <папка со снимками> --site <репозиторий stillcraft>`; как называть снимки,
написано в начале скрипта. Перед съёмкой очистите данные демо-сборки (`adb shell pm clear app.fogwalk.demo`),
иначе Android восстановит старые демо-данные из автобэкапа и серия с сегодняшним днём будут пустыми.

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | Application → **Health & Fitness** |
| Tags (до 5) | Activity tracker, Health & fitness, Maps & navigation, Travel & local, Entertainment |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/fogwalk/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

Категория: Fogwalk — игра, но приходят в неё за прогулками, поэтому рекомендую App → Health & Fitness:
там ищут «ходьба», «шагомер», «гулять по городу». Альтернатива — Game → Adventure (рядом с Pokémon GO);
тогда анкета IARC будет игровой, ответы те же. Категорию можно сменить позже в Store settings.

**Имя разработчика и страница разработчика** (Settings → Developer account → Developer page /
About you): имя — **Stillcraft**, одинаковое для всех приложений. Тогда Play сам покажет под карточкой
«Другие приложения этого разработчика», а клик по имени откроет страницу со всеми приложениями.
На Developer page: логотип, обложка 4096×2304 и пара строк о Stillcraft, ссылка
`https://bigdevwhale.github.io/stillcraft/`. Кросс-реклама в описании — только одна строка в конце
(уже есть в `listing/*/full_description.txt`), без списков приложений и ссылок.

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.*

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types** (для Game → Adventure — игровая).
- На все вопросы — **No**: насилие (драконы и сундуки — только названия, сражений нет), страх, сексуальный
  контент, ненормативная лексика, наркотики/алкоголь/табак, азартные игры (золото внутриигровое,
  за деньги не покупается и не выводится), грубый юмор, общение между пользователями, обмен контентом,
  передача местоположения другим пользователям, цифровые покупки, браузер или поиск.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over**. Младше 13 не отмечать: иначе действует политика Families,
  а приложение гонит гулять одного с GPS.
- *Could your store listing unintentionally appeal to children?* → **No**.

**News apps** — No.

**COVID-19 contact tracing and status apps** — My app is not a publicly available COVID-19 app.

**Data safety**
- Разработчику не уходит ничего: нет серверов, аналитики, отчётов о сбоях. Маршрут, шаги и прогресс
  лежат в локальной базе. Единственный трафик — тайлы карты с `tile.openstreetmap.org`.
- Тонкость: запросы тайлов раскрывают серверу OSM район, который смотрит пользователь (обычно вокруг него).
  По определению Google «сбор» — это любая передача данных с устройства, поэтому честнее задекларировать:
  - *Does your app collect or share…?* → **Yes**.
  - *Is all of the user data collected by your app encrypted in transit?* → **Yes** (тайлы по HTTPS).
  - *Do you provide a way for users to request that their data is deleted?* → **No** (на сервере ничего не хранится;
    данные на телефоне удаляются вместе с приложением).
  - Data types: **Location → Approximate location** — *Collected*: Yes, *Shared*: No;
    *Processed ephemerally*: **Yes**; *Required* (пользователь не может отказаться, карта без тайлов не работает);
    purpose: **App functionality**.
  - Больше ничего не отмечать: точное местоположение, шаги («Fitness info»), маршруты с устройства не уходят.
    Системное резервное копирование Android делает система, а не приложение, его не декларируем.
  - Итог в карточке: «Collects: Location», «No data shared with third parties».
- Вариант смелее — **No** на первый вопрос («No data collected»): так делают многие приложения на osmdroid,
  но при придирке ревью придётся спорить. Рекомендую декларацию выше.

**Government apps** — No.

**Financial features** — *My app doesn't provide any financial features.*

**Health apps** — отметьте **Activity and fitness** (шагомер и прогулки). Health Connect, медицинские функции
и прочее — не отмечать.

**Advertising ID** — *No* (рекламы и SDK аналитики нет).

**Foreground service permissions** (появится после загрузки AAB: targetSdk 36 и `FOREGROUND_SERVICE_LOCATION`)
- Тип: **Location**. Задача: *User-initiated location sharing* не подходит — выберите
  **«Other»** и опишите:
  > Fogwalk reveals a fog-of-war map wherever the user walks. When the user taps "Set out", a foreground
  > service with a visible, ongoing notification keeps receiving GPS positions so the walk is charted while
  > the screen is off or the phone is in a pocket. The service only starts on the user's tap, stops when they
  > end the journey from the app or the notification, and positions never leave the device.
- Ссылка на видео — см. раздел 0.
- Если в списке есть **Fitness / Workout tracking** (*Workout or fitness tracking, e.g. walks*), берите его
  вместо «Other» — описание то же.

**Location permissions** — отдельной декларации не нужно: `ACCESS_BACKGROUND_LOCATION` не запрашивается,
местоположение берётся только при открытом приложении или в начатом пользователем странствии с уведомлением.

**Прочие разрешения** — особых деклараций не нужно: `ACTIVITY_RECOGNITION` (шагомер), `POST_NOTIFICATIONS`,
`RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `INTERNET` не относятся к чувствительным. Точных будильников
(exact alarm) нет — напоминание ставится через `setAndAllowWhileIdle`.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Internal testing** (или Closed testing, если аккаунт новый) → Create new release.
2. Загрузить `Fogwalk-v1.3.1.aab` из GitHub Release.
3. Release name: `1.3.1`. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели;
   с фоновой службой по местоположению — может дольше.
5. После установки из Play проверьте: **Летопись → ⚙ → Язык → Русский** — интерфейс должен смениться
   (это и был баг SwissZen).

## Риск: тайлы OpenStreetMap

Карта грузится с `tile.openstreetmap.org`. Его [политика](https://operations.osmfoundation.org/policies/tiles/)
разрешает приложения с собственным User-Agent (он задан: `Fogwalk/<версия> (app.fogwalk)`) и кешем
(есть, 300 МБ), но серверы OSM — на пожертвованиях, и при большом трафике приложение могут заблокировать
по User-Agent. Пока установок немного, это нормально. Если Fogwalk разойдётся, переключите источник тайлов
на коммерческий (MapTiler, Stadia, Thunderforest — у всех есть бесплатный уровень) или свой сервер.
Ссылка «© OpenStreetMap contributors» на карте обязательна — она есть.
