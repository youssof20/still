# still

An Android launcher with text favorites, local app search, and configurable Camera/Phone actions. No account or network connection is required.

## Shipped

- Home with ordered favorites (no fixed count), optional clock/date, and layout lock
- Apps list with search that never auto-launches while typing
- Aliases that keep the original label searchable
- Hide from app list, or hide from list and search, with a Settings recovery screen
- Edit home: reorder and remove favorites; missing favorites stay recoverable
- Default Home role request and system Home settings access
- Camera and Phone actions (buttons + left/right swipe on Home); swipe up from the bottom strip opens Apps
- Profile-aware app identity (component + user serial)
- No `INTERNET` permission

## Not shipped yet

Tasks, widgets, font import, appearance presets, Private Space UI, and shortcuts completeness.

## Build

Requires JDK 17+ and an Android SDK with platform 37.0 and build-tools 36.0.0.

Toolchain: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.2.10 (AGP built-in Kotlin), Compose BOM 2026.08.00.

```text
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assertNoInternetPermission
```

Windows: `gradlew.bat` with the same tasks. Set `sdk.dir` in `local.properties` (not committed).

## Permissions and privacy

Core launching does not require Internet, accessibility, notification, contacts, location, or usage access. Settings and favorites stay on device. OS backup of launcher data is disabled in the manifest rules for this stage.

## Known limits

- Device and OEM Home gesture behavior is unverified on physical hardware.
- Package visibility for the app list is incomplete until this app holds the Home role on Android 11+.
- Hiding apps is launcher UX only, not Android security.

## License

Apache License 2.0. See `LICENSE`.
