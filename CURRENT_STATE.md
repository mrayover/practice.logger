# Current state — Orchid theme /0.17.0/build65

## Delivery

Signed **0.17.0/build65**, source `a6c095c02fc2363bcdf04cd2c4b168ce7f55569f`. Later closeout commits change records only. [APK](dist/practice.logger-0.17.0-b65-a6c095c/practice.logger-0.17.0-b65-a6c095c.apk) · [ZIP](dist/practice.logger-0.17.0-b65-a6c095c.zip) · [Build info](dist/practice.logger-0.17.0-b65-a6c095c/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.17.0-b65-a6c095c/PHONE_CHECKLIST.txt). Install over the permanent-key app without uninstalling/clearing data. Earlier signed packages remain preserved and ignored.

APK SHA256 `4d871a43296e8fb2edbdf5e81915d3e2124ef775427e7bd69641efa7a81a5f7b`; ZIP SHA256 `1eebdbcdbfc1f925e57a4598af2d2ffc4901be2bc3252fe7f774064ec8fae60f`. Publisher verifies package/version/signature/pinned certificate. Signed non-signature entries equal the CI unsigned release; ZIP CRC, unique/exact members and all bytes match the distribution folder.

## Behavior

Settings gear → Appearance → **Orchid** adds dark plum surfaces, lavender actions and banner, soft purple selected/transport roles and pale lilac text through the central theme. Native window/accent colors match. Shared selected/disabled/pressed/recording/flash behavior, Lato and accepted layout remain. Orchid persists and explicitly overrides OS appearance; Light is the default/fallback and System Default follows Light/Dark. Light/Dark/Smooth/Sunny remain selectable. [Change record](docs/ORCHID_THEME.md).

Imported audio remains MP3, AAC-in-M4A and PCM16 WAV, mono/stereo8–96kHz,50MiB/15minutes with explicit Save and format-preserving export. [Contract/prior evidence](docs/IMPORTED_AUDIO.md). No data/audio-engine change; Room11/JSON7 and media/audio backup exclusions remain. Earlier imported-audio physical/provider checks still apply.

## Scoped verification

[CI41281008402](https://github.com/mrayover/practice.logger/actions/runs/41281008402), exact source above: debug/release/test compilation, **5** ThemePalette/ThemePreferences unit tests and **2** API35 native scenarios pass. Units cover color contrast/Goal slots, explicit/System resolution and persistence/failure fallback. Native checks cover Orchid selection/recreation, rendered background/native accent/navigation icons, and practice button lit/decay/Stop surface pixels plus nonzero PCM across Light/Dark/Smooth/Sunny/Orchid. Home/Settings/live-pulse screenshots inspected. Local palette contrast/XML and diff checks also pass. No full regression, lint, API26 or enlarged-text matrix for this addition. Phone/physical audio/display acceptance pending.

Prior Light/Dark are owner accepted. Detailed System/audio/accessibility and P4/P5/final gates remain. Stage4 remains0/10 documented sessions; no later phase started.

## Exact next task

Install build65 and perform its Orchid phone checklist, especially Practice control colors/flash/disabled states, reopening and device-appearance override. No automatic later phase/full regression.
