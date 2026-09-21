# still

An Android home-screen app (platform-proof stage). It can request the default Home role, list launchable apps by component and profile, search without auto-launch, and open configurable Camera/Phone actions.

This is not a finished daily-driver launcher. Favorites, tasks, widgets, font import, and appearance editing are not implemented yet.

## Shipped in this tree

- Home activity with `singleTask` launch mode and HOME/LAUNCHER intent filters
- Default Home role request through the OS role UI, plus a Settings action to reopen system Home settings
- App list from `LauncherApps` using activity component + user serial
- Tap-to-launch with messages for missing, disabled, or unavailable targets
- Search with deterministic ranking; typing never launches an app
- Camera and Phone actions that can be chosen or disabled, with on-screen buttons as well as left/right swipes on Home
- No `INTERNET` permission in the source manifest

## Build

Requires JDK 17+ (this machine used Android Studio JBR 25) and an Android SDK with platform 37.0 and build-tools 36.0.0.

Toolchain pinned in the Gradle version catalog: AGP 9.4.0, Gradle 9.6.0, Kotlin 2.2.10 (AGP built-in Kotlin), Compose BOM 2026.08.00.

```text
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assertNoInternetPermission
```

On Windows:

```text
gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assertNoInternetPermission
```

Set `sdk.dir` in `local.properties` (not committed) to your SDK path.

## Permissions and privacy

Core launching does not require Internet, accessibility, notification, contacts, location, or usage access. App data stays on device. OS backup of launcher settings is disabled in the manifest rules for this stage.

## Known limits

- Device and OEM Home gesture behavior is unverified on physical hardware for this build.
- Package visibility for the app list is incomplete until this app holds the Home role on Android 11+.
- Private Space UI, widgets, tasks, aliases UI, and layout lock are not present.

## License

Apache License 2.0. See `LICENSE`.
