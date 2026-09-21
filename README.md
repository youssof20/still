# still

A text-first Android launcher. Quiet Home, local search, optional tasks, local appearance. No account or network connection is required.

## Shipped

- Home: clock, date, favorites, optional task preview — no permanent navigation buttons
- Swipe up for apps; swipe down for notifications where supported; left/right actions configurable
- Tap clock/date to open Clock/Calendar; long-press to change those actions
- Long-press empty Home for Tasks, Appearance, Settings
- Long-press apps and favorites for contextual actions
- Tasks focused on quick capture; export/import under More
- Appearance with black default, presets, progressive settings
- Settings as compact preference rows
- Short skippable onboarding
- No `INTERNET` permission

Widgets host code remains in the tree but is not shown in the UI for this release.

## Build

```text
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:assertNoInternetPermission
```

## License

Apache License 2.0. See `LICENSE`.
