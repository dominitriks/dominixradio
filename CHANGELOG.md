# Changelog

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
