# Current state — Orchid theme /0.17.0/build65

## P5 in progress — 2026-11-01

The owner assigned P5 and explicitly authorized Git command publication. Preparation e90e540 and legacy native alignment67cb91a are published. Initial build/unit/lint CI41286865140 and targeted API35 CI41287071761 are running. No full-regression pass or owner-phone acceptance is claimed. Existing signed65 remains current until a pre-Library candidate is packaged.

## Pre-regression normalization — 2026-11-01

Repository-wide Git/provenance/static review is complete; [review and inventory](docs/PRE_REGRESSION_REVIEW.md) records runner/documentation corrections and the remaining legacy native-selector blocker. Current application source and signed build65 are unchanged. No new build, Android test, lint/full regression or phone acceptance is claimed. Changes are local and uncommitted; remote main was verified at `d0b8db0fc3e2472e9deccbe3a865a43da5f257d1` before maintenance. The primary workflow now includes the later increment coverage, but has not run.


Second deep check on2026-11-01 corrected stale backup unit expectations, generated import fixtures, API26 debug test fixture access, maximum-loop timeouts and additional exit/zoom checks. [Detailed findings](docs/PRE_REGRESSION_REVIEW.md) distinguish GitHub diagnostic-only successes from a full pass. Static/mocked validation passes; compilation and remaining legacy UI-test alignment are pending. Owner requested wrap-up at5% usage. All changes remain local/uncommitted; signed65/main-source/schema/package bytes remain unchanged.

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

Follow the prepared [P5 regression handoff](docs/P5_REGRESSION_HANDOFF.md): align the legacy UI scenarios identified in [the review](docs/PRE_REGRESSION_REVIEW.md), compile instrumentation and run the affected targeted cases before the assigned complete P5 pre-Library regression. Preserve original safety assertions. Build65's Orchid/imported-audio phone checks remain pending and carry forward into that gate. Preparing the handoff did not start regression or a later feature phase.
