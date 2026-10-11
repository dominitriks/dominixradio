# Changelog

## [1.2.0] — 2026-10-11

### Added
- **Song info you can trust:** artist and title are shown only when the stream sends real track data. Ads with web addresses, station slogans, empty titles, codes and broken encodings are filtered out (tested on real titles from the stations). No covers from streams — the station logo stays.
- **Station status:** a station that failed to play is marked "Temporarily unavailable" or "Stream not working" for 30 minutes (cleared as soon as it plays). Decided only from real playback, never from background probing; no internet is never blamed on the station.
- **"Try again"** button in the player and in the error message.
- **Update check** via public GitHub Releases: at most once a day on app start and on demand in Settings. Shows version, release notes and a button to the real APK — nothing is downloaded or installed automatically.
- **Feedback** (pre-filled GitHub issue, shown before sending) and a **privacy policy** (in app and PRIVACY.md).
- **GitHub Actions:** tests, lint and debug APK on every push; release APK artifact on demand or on tags. Never publishes a release by itself; signing via repository secrets.

## [1.1.0] — 2026-10-11

### Added
- Sleep timer (15 min – 2 h): fades out and pauses; runs in the playback service, so it works with the app closed. Countdown in the player, moon icon in the mini player.
- Last station is restored (paused) when the app is reopened — one tap to continue.
- Favorites screen: "Favorites | Recently played" switch (last 10 stations).
- Catalog: sort by popularity or name (Bulgarian alphabet), station counts on category chips, "Clear filters".
- Background status in Settings (Android background restriction, battery optimization, Xiaomi Autostart shortcut) and a warning on the home screen when Android blocks background playback.
- Unit tests for search/sorting, sleep timer and error handling (17 tests).

### Changed
- Redesigned player: genre-coloured backdrop, rounded artwork with glow, LIVE / CONNECTING / PAUSED / ERROR badge, landscape layout.
- Adaptive grid: two or more columns on tablets and in landscape.
- Accessibility: TalkBack roles, labels and playing state on cards and controls; status changes announced.

## [1.0.1] — 2026-10-07

### Fixed
- **Radio stopped when the phone was locked and no notification was shown.** The player wrapper did
  not forward playback events to the MediaSession (Kotlin interface delegation skips Java default
  methods), so Media3 never started the foreground service. All 37 listener callbacks are now
  forwarded; covered by a unit test.

## [1.0.0] — 2026-10-07

First public release of **Dominix** — Bulgarian Radio, created by Dominantus.

### Features
- 106 Bulgarian radio stations with verified public streams, 10 categories
- Search by name, genre or city (Cyrillic and Latin)
- Favorites and recently played, stored only on the device
- Background playback with the screen off, phone locked or app closed (foreground media service)
- Notification and lock screen controls: Play/Pause, Previous, Next, Stop
- Bluetooth headphones and car controls; resume last station from a Bluetooth Play press
- Android Auto browsing (Favorites, Recently played, All stations, Categories) and voice search
- Automatic reconnect after network loss (bounded, battery friendly); alternative stream addresses
- Clear error messages in Bulgarian
- Settings: dark / light / automatic theme, Wi-Fi only, auto reconnect, battery optimization shortcut
- Releases resources and stops the service after Stop or 10 minutes paused
