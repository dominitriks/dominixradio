# Changelog

## [1.5.0] — 2026-10-12

### Added
- **Self-updating station catalog:** DOMINIX checks the station list on GitHub at most once a day (or from Settings → Каталог на станциите) and applies a valid new list right away, so fixed or new stations arrive without a new app version. Invalid downloads are ignored; the built-in list is always the fallback.
- **"Рекламирай в DOMINIX"** (Settings → Information): partner packages for businesses and a contact button that opens your e-mail app.

## [1.4.1] — 2026-10-12

### Fixed
- Tapping "Начало" (or another tab) while Statistics was open kept showing Statistics; tabs now close it and switch correctly.

## [1.4.0] — 2026-10-12

### Changed
- **New look — Dark Premium + Radio Experience:** blue-black background, graphite cards, purple accent with blue and cyan; clearer "playing now" indication; calmer category tiles; redesigned player with a big Play button, live status badge and labelled Favourite / Stop.
- Brand slogan under DOMINIX: „Няма нищо по-хубаво от лошото време“.
- Accessibility: readable contrast (tested), state never shown by colour alone, 48 dp touch targets, text no longer clipped with large font sizes, display-cutout aware layout.

## [1.3.4] — 2026-10-12

### Changed
- **Update check on every start** (in the background, silent unless a newer stable version is confirmed). "Later" no longer hides a version — it is offered again at the next start.
- Update dialog: "Налична е нова версия на DOMINIX" with current and new version, release notes, "Обнови сега" / "По-късно".
- Safer in-app updates: only APKs from the official GitHub repository are accepted; the download goes to a temporary file and is used only after checking size, GitHub's SHA-256, package name, a higher version code and the signing key — an incomplete download never replaces a good file. Only one check or download at a time.

## [1.3.3] — 2026-10-12

### Added
- "Сподели DOMINIX" button on the home screen (next to statistics), in addition to Settings → Information.

## [1.3.2] — 2026-10-12

### Added
- **"Сподели DOMINIX"** (Settings → Information): recommend the app to friends through the Android share sheet with the official download link.

## [1.3.1] — 2026-10-12

### Fixed
- **In-app update stuck at 100 %.** "Download" used to open the APK link in the default browser, and some browsers (e.g. Brave on Xiaomi) hold APK files at 100 % forever. Dominix now downloads the update itself with Android's download manager (progress in the dialog and in notifications), verifies it (GitHub SHA-256 checksum, package name, version) and opens Android's installer — you still confirm the installation there. Android asks once to allow Dominix to install updates. "Open in browser" remains as a fallback.

## [1.3.0] — 2026-10-12

### Added
- **Listening statistics** (Settings → Statistics, or the chart icon on the home screen): total, today, last 7 days, this month, most listened station, top 5, number of sessions, 14-day chart and session history. Counts only time when audio is actually playing (also in the background), splits sessions at midnight, never double counts; stored locally and can be deleted.
- **Share a station** through the Android share sheet (name + DOMINIX download link; never the raw stream).
- **QR code** generated on the phone, pointing to the official download page (there is no per-station deep link), with share and save-to-gallery (Android 10+).
- **Report a problem** with a station: reason + optional description; send via GitHub, any app (share sheet) or copy. Never claims "sent" — the report goes out only when you confirm it in the chosen app.
- **Check station** on demand: up to 2 attempts per address, tells "no internet" apart from a station problem, does not interrupt what is playing.
- Station actions: ⋮ in the player or long-press on a station card.

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
