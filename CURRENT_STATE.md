# Current state — Sunny theme /0.17.0/build64

## Orchid increment in progress

Orchid source is prepared for build65: dark plum surfaces and light purple controls with native styles/persisted selector. Focused verification/signing pending; signed build64 below remains the installable baseline until delivery. No phone acceptance claimed.

## Delivery

Signed **0.17.0/build64**, source `4d71c1e05b2d3f5b4725f68d566e00d5aa346880`. Later closeout commits change records only. [APK](dist/practice.logger-0.17.0-b64-4d71c1e/practice.logger-0.17.0-b64-4d71c1e.apk) · [ZIP](dist/practice.logger-0.17.0-b64-4d71c1e.zip) · [Build info](dist/practice.logger-0.17.0-b64-4d71c1e/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.17.0-b64-4d71c1e/PHONE_CHECKLIST.txt). Install over the permanent-key app without uninstalling/clearing data. Earlier signed packages remain preserved and ignored.

APK SHA256 `49c5b28846edd34b9948a12fd0ba9a90bed9c28d1be9191b99d3d9d268ef8ad4`; ZIP SHA256 `30607b9d2456216ad23652d718fe42da60f0d07f4d67b8fb6e38741f084dbdc2`. Publisher verifies package/version/signature/pinned certificate. Signed non-signature entries equal the CI unsigned release; ZIP CRC, unique/exact members and all bytes match the distribution folder.

## Behavior

Settings gear → Appearance → **Sunny** adds pale yellow surfaces, warm orange/amber actions and banner, golden selected/transport roles and dark warm text through the central theme. Native window/accent colors match. Shared selected/disabled/pressed/recording/flash behavior, Lato and accepted layout remain. Sunny persists and explicitly overrides OS appearance; Light is the default/fallback and System Default follows Light/Dark. Light/Dark/Smooth remain selectable. [Change record](docs/SUNNY_THEME.md).

Imported audio remains MP3, AAC-in-M4A and PCM16 WAV, mono/stereo8–96kHz,50MiB/15minutes with explicit Save and format-preserving export. [Contract/prior evidence](docs/IMPORTED_AUDIO.md). No data/audio-engine change; Room11/JSON7 and media/audio backup exclusions remain. Earlier imported-audio physical/provider checks still apply.

## Scoped verification

[CI41279928548](https://github.com/mrayover/practice.logger/actions/runs/41279928548), exact source above: debug/release/test compilation, **5** ThemePalette/ThemePreferences unit tests and **2** API35 native scenarios pass. Units cover color contrast/Goal slots, explicit/System resolution and persistence/failure fallback. Native checks cover Sunny selection/recreation, rendered background/native accent/navigation icons, and practice button lit/decay/Stop surface pixels plus nonzero PCM across Light/Dark/Smooth/Sunny. Home/Settings/live-pulse screenshots inspected. Local palette contrast/XML and diff checks also pass. No full regression, lint, API26 or enlarged-text matrix for this addition. Phone/physical audio/display acceptance pending.

Prior Light/Dark are owner accepted. Detailed System/audio/accessibility and P4/P5/final gates remain. Stage4 remains0/10 documented sessions; no later phase started.

## Exact next task

Install build64 and perform its Sunny phone checklist, especially Practice control colors/flash/disabled states, reopening and device-appearance override. No automatic later phase/full regression.
