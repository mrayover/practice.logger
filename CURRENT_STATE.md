# Current state — Smooth theme /0.17.0/build63

## Delivery

Signed **0.17.0/build63**, source `bcfd69021a3766b541eef9a502afc2a1a2ac0292`. Later closeout commits change records only. [APK](dist/practice.logger-0.17.0-b63-bcfd690/practice.logger-0.17.0-b63-bcfd690.apk) · [ZIP](dist/practice.logger-0.17.0-b63-bcfd690.zip) · [Build info](dist/practice.logger-0.17.0-b63-bcfd690/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.17.0-b63-bcfd690/PHONE_CHECKLIST.txt). Install over the permanent-key app without uninstalling/clearing data. Previous signed packages remain preserved and ignored.

APK SHA256 `83067266d586cf3a2b04d84e281b2c0bf78b7910af2ca15c7709d4a8d560c02d`; ZIP SHA256 `c2eb285b050024b56690cbb2cedab651a22abc8b20da144872dc47c8b3f185ce`. Publisher verifies package/version/signature/pinned certificate. Signed non-signature entries equal the actual CI unsigned release. ZIP CRC, unique members and all bytes match the distribution folder.

## Behavior

Settings gear → Appearance → **Smooth** adds deep teal surfaces/aqua accents through the central palette. Native windows/accent match. Existing selected/disabled/pressed/recording/flash states use shared components; distinct error/recording and Goal category hues remain. Lato and accepted layout stay unchanged. Smooth persists across reopening and overrides OS night mode. Light remains the default/fallback; System Default still follows Light/Dark. [Design/change record](docs/SMOOTH_THEME.md).

Imported backing audio from build62 remains: MP3, AAC-in-M4A, PCM16 WAV, mono/stereo8–96kHz,50MiB/15minutes, explicit staged Save/selection and format-preserving export. [Contract/prior verification](docs/IMPORTED_AUDIO.md). No database/audio-engine change; Room11/JSON7 and media/audio backup exclusions remain. Prior imported-audio phone checklist still applies.

## Scoped verification

[CI41278023116](https://github.com/mrayover/practice.logger/actions/runs/41278023116), exact source above: debug/release/test compilation; **5** ThemePalette/ThemePreferences units; **2** API35 native scenarios pass. Units cover contrast, Goal slots, resolver/system behavior and persistence/failure fallback. Native checks cover Smooth selection/recreation, rendered background/native accent/chrome and practice button lit/decay/Stop pixels plus PCM writes in Light/Dark/Smooth. Home/Settings/live-pulse screenshots inspected. No full regression, lint, API26 or enlarged-text matrix for this bounded addition. Phone/physical acoustic acceptance pending.

Prior Light/Dark implementation is owner accepted. Detailed System/audio/accessibility and P4/P5/final gates remain. Stage4 remains0/10 documented sessions; no later phase started.

## Exact next task

Install build63 and perform its Smooth phone checklist, especially Focused Practice colors/flash/disabled controls and persistence. No automatic later phase/full regression.
