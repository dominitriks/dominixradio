# Dominix

Bulgarian Radio

Created by Dominantus

© 2026 Dominantus. All rights reserved.

## Download

The latest Android APK is available in [GitHub Releases](../../releases/latest).

## Installation

1. Download the APK.
2. Open the APK on your Android device.
3. If Android asks, allow installation from this source.
4. Install Dominix.
5. Open the application.

Requires Android 7.0 or newer. If Google Play Protect shows a warning, tap **More details → Install anyway** (normal for apps installed outside Google Play).

## Features

* Bulgarian radio stations
* Background playback
* Lock screen controls
* Notification controls
* Favorites
* Search
* Categories
* Bluetooth playback
* Sleep timer
* Recently played
* Song info (when the station provides it)
* Update check via GitHub Releases
* Listening statistics
* Share stations and QR code
* Report broken stations

Also: recently played, automatic reconnect after network loss, Wi-Fi-only mode, dark/light theme, Android Auto browsing. The interface is in Bulgarian. No account, no ads, no server: favorites and settings stay on the phone.

## Radio stations

Station names, logos, programmes and audio streams belong to their respective owners. Dominix only links to publicly available internet streams and claims no rights over them.

The station list is in [`app/src/main/assets/stations.json`](app/src/main/assets/stations.json):

```json
{
  "id": "radio1",
  "name": "Радио 1",
  "streamUrl": "http://stream.metacast.eu/radio1128",
  "fallbackUrls": ["http://149.13.0.80/radio1128"],
  "genre": "Хитове",
  "city": "София",
  "logoUrl": "",
  "popular": true,
  "status": "verified"
}
```

**Updating stations without a new app version:** the app downloads this same file from the `main`
branch at most once a day (and when you tap *Settings → Каталог на станциите*). Edit
`app/src/main/assets/stations.json` on GitHub, commit — every installed DOMINIX picks it up within
24 hours. Rules the app enforces before using a downloaded list:

- valid JSON array, at most 512 KB, at least half as many stations as the built-in list (min. 10);
- at least half of the built-in station `id`s must still be present (protects against uploading a wrong file);
- each entry needs `id` (`a-z`, `0-9`, `_`), `name` and an `http(s)` `streamUrl`; invalid entries are skipped;
- keep existing `id`s unchanged — favourites and statistics are linked to them.

If a download is invalid, the app keeps the previous list (or the one built into the APK).
A new app version always starts from its own built-in list.

`genre` is one of: Поп, Хитове, Рок, Денс, Българска музика, Чалга, Новини, Спорт, Джаз, Класическа музика, Други.

## Partners

Partners shown in *Партньори на DOMINIX* (home screen, bottom, and Settings → Партньори на DOMINIX)
come from [`app/src/main/assets/partners.json`](app/src/main/assets/partners.json). Edit it on GitHub —
the app downloads it at most once a day (or when *Каталог на станциите* is tapped in Settings).

```json
[
  {
    "id": "cafe_radio",
    "name": "Кафе Радио",
    "url": "https://caferadio.bg",
    "logoUrl": "https://caferadio.bg/logo.png",
    "tier": "premium",
    "until": "2027-10-31"
  }
]
```

- `tier`: `basic` (Основен — name + link) or `premium` (Премиум / Годишен — logo + name + link, shown first and larger).
- `until`: last day the partner is shown (`yyyy-MM-dd`); after it the entry disappears automatically. Monthly: set one month ahead; yearly: 12 months.
- `id`: `a-z`, `0-9`, `_`; `url` and `logoUrl` must be `https://`. Invalid entries are skipped; an invalid file keeps the previous list.
- An empty list `[]` hides the section.

## Building from source

Requirements: JDK 17 and the Android SDK (platform 35). Create `local.properties` with `sdk.dir=<path to Android SDK>`.

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/Dominix-v1.5.2-release.apk
./gradlew assembleDebug     # app/build/outputs/apk/debug/Dominix-v1.5.2-debug.apk
```

Release signing reads `keystore.properties` (not committed):

```properties
storeFile=bgradio-release.jks
storePassword=...
keyAlias=bgradio
keyPassword=...
```

Without it the release build is signed with the debug key.

## Architecture

```
UI (Jetpack Compose) → MainViewModel → PlayerConnection (MediaController)
   → MediaSession → PlaybackService (MediaLibraryService, foreground) → ExoPlayer → stream
```

- `service/PlaybackService.kt` — Media3 `MediaLibraryService`: foreground playback, notification, lock screen, Bluetooth buttons, playback resumption, Android Auto browse tree, Stop and idle cleanup.
- `service/RadioLoadErrorPolicy.kt` — bounded reconnect (≈4 min) that keeps the player buffering, so the service stays in the foreground while the network is down.
- `service/RadioPlayer.kt` — Previous/Next switch stations; station name stays the title on the lock screen.
- `data/` — `stations.json` loader and DataStore (favorites, recent, settings).
- `ui/` — screens, components, theme.

## Privacy

Dominix collects no personal data. See [PRIVACY.md](PRIVACY.md).

## Feedback

Report problems or ideas in [Issues](../../issues) (also from the app: Settings → Send feedback).

## Continuous integration

[`.github/workflows/android.yml`](.github/workflows/android.yml) runs tests, lint and a debug build on every push.
A release APK is built when you run the workflow manually or push a `v*` tag; it is uploaded as a
workflow artifact only — the workflow never publishes a GitHub Release.

For a release APK signed with the official key, add these repository secrets
(Settings → Secrets and variables → Actions): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

## License

© 2026 Dominantus. All rights reserved. Third-party libraries (AndroidX, Jetpack Compose, Media3, Coil) are used under their own licenses (Apache 2.0).
