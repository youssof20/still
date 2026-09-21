# still

An Android launcher with text favorites, local search, optional tasks and widgets, and local appearance controls. No account or network connection is required.

## Shipped

- Quiet Home by default: optional clock/date, favorites, optional task preview, optional widgets — no permanent Apps/Camera/Phone/Tasks buttons
- Swipe up for apps; swipe down for the notification shade where supported; swipe left/right for configurable actions (defaults: Camera / Phone)
- Tap clock or date to open Clock/Calendar; long-press to change or disable those actions
- Long-press empty Home for Add favorite, widgets, Tasks, Appearance, Gestures, Settings, layout lock
- Long-press favorites and apps for contextual actions (rename, hide, uninstall request, shortcuts)
- Local tasks with a subtle Home preview (off by default)
- Appearance: black default, light/dark/system, presets (Still, OLED, Paper, Terminal, Mono, Retro LCD, Classic Phone, Material), fonts, local TTF/OTF import
- Short skippable onboarding; Settings → Help → Quick guide to reopen
- Settings grouped into Home, Appearance, Gestures, Apps, Tasks, Widgets, Privacy, Help, About
- Profile-aware apps, work-profile quiet-mode notice, Private Space labeled unsupported
- No `INTERNET` permission

## Not shipped yet

Private Space UI (lifecycle and lock-state acceptance tests still required).

## Build

Requires JDK 17+ and an Android SDK with platform 37.0 and build-tools 36.0.0.

```text
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assertNoInternetPermission
```

Windows: `gradlew.bat` with the same tasks. Set `sdk.dir` in `local.properties` (not committed).

## Permissions and privacy

Core launching, tasks, appearance, and the widget host do not require Internet, accessibility, notification, contacts, location, or usage access. Expanding the notification shade uses a supported system API when available and fails quietly otherwise. Data stays on device.

Widget providers may use their own network access. Widget instance IDs are not portable across devices after backup.

## Known limits

- Device and OEM Home gesture and shade behavior is unverified on every OEM.
- Package visibility for the app list is incomplete until this app holds the Home role on Android 11+.
- Hiding apps is launcher UX only, not Android security.
- Shortcut lists can be empty when still is not the default Home app.
- Private Space remains unsupported.
- Lock-screen gesture requires device-admin capabilities the app does not request; the action reports unavailable when lock fails.

## License

Apache License 2.0. See `LICENSE`.
