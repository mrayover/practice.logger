# Current state

Updated 2026-10-26 (America/Los_Angeles). Owner confirms 0.2.0 works; model/OS and individual checklist results were not supplied. Stage 3 backup increment delivered; full daily-use/accessibility/provider acceptance is still pending.

## Verified build and delivery

- App/config source main: 1a85ecbe1a6d9882f6d3d2b4adf391e06997a473. Successful CI: https://github.com/mrayover/practice.logger/actions/runs/40385332935 . Closing documentation-only commit follows this source; verify/report its remote SHA separately.
- Version 0.3.0, versionCode 4; Android 8.0+; unchanged application ID com.mrayover.practicelogger.app. Launcher label practice.logger. Install over permanently signed 0.2.0 to retain its database. Old temporary-key 0.1.0 stays separate; no automatic transfer from that app.
- ZIP: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.3.0-b4-1a85ecb.zip
- APK: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.3.0-b4-1a85ecb\practice.logger-0.3.0-b4-1a85ecb.apk
- APK SHA-256: 8a7d6589d2069c543ed8a4b47118161f392a75553e705bf6029fcc0792701607
- Permanent certificate: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. BUILD_INFO.json / INSTALL.txt beside APK. Signature, metadata and ZIP extraction checksum verified. Prior ZIPs/folders retained, LATEST.txt updated. dist/.tools/.signing ignored; secure backup of existing .signing remains required. Never recreate/upload that private key or use CI's temporary-key debug APK as an update.

## Working functionality

Stages 0–2 retain bundled Lato throughout app text; Room catalog create/edit/search/archive/restore; durable draft selection/order/removal/placement-only duration; focused countdown, Start/Pause/Resume, early Complete, Skip, explicit zero/overtime; auto-pause on background/rotation/leaving Focus; recovery to correct Block/time paused; summaries, optional notes and reopenable snapshot-based history. Screen stays awake only while actively practicing.

Stage 3 adds Home → Backup and restore, Android system document pickers, consistent version-1 UTF-8 JSON export of archived/active Blocks and all sessions/placements/notes/unfinished practice; complete validation, preview counts/confirmation and transactional restore only into an empty database. No replacement/merge/reset path. Structural/type/reference/order/status/time/overflow/size/nesting checks precede inserts. Empty-target check and inserts share one transaction. Rotation retains preview and ongoing work; process death discards unconfirmed preview. Restored practice is paused. Room remains version 2; explicit 1→2 migration and schemas retained.

## Verified checks

- CI testDebugUnitTest lintDebug assembleDebug assembleRelease passed. Lint: 0 errors, 10 warnings (newer target/dependencies/Gradle, Android data-extraction rules and missing custom launcher icon); toolchain remains pinned for this bounded increment. Sixteen unit/Robolectric tests: prior catalog/font/session/migration/clock checks plus JSON file round-trip and database reopen; Unicode/notes/archive/snapshot/draft/override preservation; active overtime/current position recovery; invalid/version/type/depth/trailing/size/reference/order/state rejection; protected nonempty database; injected mid-insert failure rolls back all records.
- API 35 emulator passed catalog/recreation/process reopen, three-Block reorder/override/complete/skip/pause/background/zero/overtime, interrupted-session process recovery, saved summaries/notes/snapshot history, force-stop reopen and install-over higher versionCode 5 retaining history.
- Added device backup workflow passed screen navigation/recreation, real file export, empty test database restore/reopen and exact snapshot comparison, retained confirmation on rotation, cancellation, rejected nonempty restore with exact database comparison and malformed-file rejection. Harness supplies file URIs to real ViewModel operations; Android system document-picker clicks/providers were not automated. Local compilation/emulator/physical-phone 0.3.0 tests were not performed; CI provides execution evidence. Prior screenshot review covers Home/Builder/Focus/zero/paused only; Summary and new backup visuals still need phone acceptance.

## Limits and unfinished work

Stage 3 remains partial. Backup limit 8 MiB / 20,000 records per type / nesting 32, format 1. Plain text (not encrypted/authenticated); keep private and trust its origin. Failed exports may leave partial files; only use a success-confirmed backup. No overwrite/merge restore; use a spare/empty installation to test safely. Never uninstall/clear the current app as a troubleshooting step. Provider cancellation/errors, low storage, real phone update/restore, scaling/TalkBack and recovery acceptance remain pending.

Timer uses monotonic foreground deltas with 250-ms checkpoints plus pause flush. Abrupt termination/disk failure may lose latest uncommitted interval plus scheduling/write latency; committed time restores paused, excluding background/wall time. Quick notes persist on Complete/Skip; session notes require Save note. No Goals, Routines, metronome, tuner, audio, accounts, cloud sync or deferred advanced features. No in-app updater/version controls; owner defers them until stable. Code still requires a new build. Each delivered build gets a new ZIP and extracted folder without replacing prior dist files.

## Exact next task

Perform and record MANUAL_TESTS.md's 0.3.0 phone document-picker export and spare-device empty-app restore checklist, including Android model/OS/provider and large-text/accessibility checks; fix observed issues before declaring Stage 3 complete or adding features. Owner's 8%-remaining feature-freeze policy applies to future sessions too: monitor usage and reserve time for a stable APK, tests, records, Desktop commit/push and remote verification.

Actual checkout: C:\Users\mrove\Projects\practice.logger. All Git mutations use GitHub Desktop; no cloud-sync step needed. Read the five operational records completely each session; append WORK_LOG and update this state even if incomplete.
