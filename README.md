# Local Dialer

Offline-first Android dialer starter built with Kotlin and Jetpack Compose.

## Included
- Live theme editor with per-component colors
- Hex color entry, starter presets, dark/light/AMOLED themes
- Active theme persisted locally with Preferences DataStore
- Save named themes locally
- No backend, analytics, account, or network permission
- Dial pad that hands numbers to Android's system dial screen

## Open
Open this repository in Android Studio and allow Gradle sync, then run on an Android device.

## Important status
This is the first UI/theme implementation, **not yet a complete replacement for Android's Phone app**. The dial button uses `ACTION_DIAL` and opens the system dial screen. Full replacement requires an Android `InCallService`, the dialer role, incoming-call UI, and contact/call-log integration. Those should be implemented and tested before setting it as the default dialer.

Theme values are stored on-device. The app requests no internet permission.