# still

An Android launcher with text favorites, local app search, a simple on-device task list, and local appearance controls. No account or network connection is required.

## Shipped

- Home with ordered favorites (no fixed count), optional clock/date, layout lock, and a small task preview
- Local tasks: add, edit, complete, Done, Trash (30-day retention), manual reorder, draft recovery
- Task export as versioned JSON or Markdown; JSON import with replace or merge (failed import leaves data unchanged)
- Appearance: light/dark/black/system modes, neutral/dynamic/custom colors, text scale/weight/spacing/alignment
- Local TTF/OTF font import into app-private storage; Settings stays on the system font if a custom font fails
- Appearance draft with Apply/Cancel/Reset; shareable theme preset (appearance only; imported font files excluded)
- Optional wallpaper scrim (OEM wallpaper visibility unverified)
- Apps list with search that never auto-launches while typing
- Aliases that keep the original label searchable
- Hide from app list, or hide from list and search, with a Settings recovery screen
- Default Home role request and system Home settings access
- Camera and Phone actions (buttons + left/right swipe on Home); swipe up from the bottom strip opens Apps
- Profile-aware app identity (component + user serial)
- No `INTERNET` permission

## Not shipped yet

Widgets, Private Space UI, and shortcuts completeness.

## Build

Requires JDK 17+ and an Android SDK with platform 37.0 and build-tools 36.0.0.

Toolchain: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.2.10 (AGP built-in Kotlin), Compose BOM 2026.08.00, Room 2.8.5.

```text
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assertNoInternetPermission
```

Windows: `gradlew.bat` with the same tasks. Set `sdk.dir` in `local.properties` (not committed).

## Permissions and privacy

Core launching, tasks, and appearance do not require Internet, accessibility, notification, contacts, location, or usage access. Data stays on device. OS backup of launcher data is disabled in the manifest rules for this stage.

Theme presets contain appearance settings only. Imported font files are not redistributed in presets.

## Dependencies (license)

Room and Robolectric test support are Apache License 2.0. Room is used only for local task storage; Robolectric only for unit tests.

## Known limits

- Device and OEM Home gesture behavior is unverified on physical hardware.
- Package visibility for the app list is incomplete until this app holds the Home role on Android 11+.
- Hiding apps is launcher UX only, not Android security.
- Wallpaper scrim depends on the window showing the system wallpaper; behavior varies by OEM and is unverified here.
- Task database is at schema version 1; upgrades will use explicit Room migrations (no destructive fallback).
- Contrast warnings are a simple luminance check, not a claimed accessibility standard certification.

## License

Apache License 2.0. See `LICENSE`.
