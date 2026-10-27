# practice.logger

Latest installable release: **0.6.0/build 11**, generally accepted by the owner. The requested click-during-loop-recording fix is prepared for **0.6.1/build 12**, awaiting CI and signed packaging. See CURRENT_STATE.md.

An offline Android practice tool: create reusable Blocks, arrange a one-off session, practice with a timer and inspect saved results. Kotlin + Compose + Room, with locally bundled Lato Regular/Bold throughout app-controlled text.

Stages 0, 1 and 2 passed automated verification. The owner generally confirms that 0.3.0 works on their phone (model/OS and individual checklist results not supplied). Stage 3 now has versioned backup/export and protected empty-app restore; Stage 3 engineering hardening is verified on Android 8 and 15, including real system document pickers and 200% text; owner phone acceptance is pending. Stage 4 ten-session evaluation is prepared in EVALUATION.md (0/10 real sessions recorded). Owner approved Stage 5 metronome followed by Stage 6A/6B Block recording on 2026-10-26; Stage 5 engineering is verified in 0.4.0/build 8; owner generally accepts the metronome; detailed phone timing results were not supplied. Goals, Routines, tuner, overdubbing and other deferred features remain excluded.

Read AGENTS.md, CURRENT_STATE.md, WORK_LOG.md and authoritative BUILD_PLAN.md completely before every development session. Every session, including incomplete work or fixes, must update the records and publish intended changes through GitHub Desktop.

## Install the versioned local build

The verified, permanently signed 0.6.0 phrase-loop build (versionCode 11) is in:

- ZIP: dist/practice.logger-0.6.0-b11-faca5e5.zip
- Extracted APK: dist/practice.logger-0.6.0-b11-faca5e5/practice.logger-0.6.0-b11-faca5e5.apk
- Exact commit, CI link, APK SHA-256 and certificate: dist/practice.logger-0.6.0-b11-faca5e5/BUILD_INFO.json

Transfer the APK to an Android 8.0+ phone, open it, and approve Android's installation prompt for the source you trust. The launcher label is practice.logger; Android app details show version 0.6.0. Install over signed 0.2.0, 0.3.0, 0.3.1 or 0.3.3/0.4.0/0.5.0 or 0.6.0/build 10 to retain existing data. Every previous ZIP and extracted folder remains in dist; no in-app updater/version control has been added. See MANUAL_TESTS.md before storing important practice data.

The original 0.1.0 APK used a lost temporary CI debug key. To preserve its installed app and Blocks, 0.2.0 uses the stable application ID com.mrayover.practicelogger.app and installs alongside it. **Blocks do not transfer automatically from 0.1.0.** Keep the old catalog if it contains needed data; recreate desired Blocks in the new app. The old APK ZIP and extracted folder are also preserved under a versioned name in dist.

Future locally signed builds use the same permanent key and a higher versionCode: install over the new app to retain its database. Do not uninstall it first, and do not substitute a temporary-key CI debug APK. Code changes still require compilation; CI builds them without an Android developer installation on the owner's computer. No account, online update checker or backend has been added. Android's update-signing requirements: https://developer.android.com/studio/publish/app-signing .

## Practice workflow

Home → Practice Now → add active Blocks. Create a Block through the shared editor if needed. Up/Down reorders placements; remove placements or override their duration without changing the reusable Block. A draft survives leaving the screen and restarting. Start Session requires at least one placement.

Focused Practice starts paused. Start/Resume counts only foreground active time; Pause, leaving the screen or backgrounding stops it. Each next Block waits for Start. Complete can finish early; Skip records the actual time already spent. At 00:00 the timer pauses and waits for Continue (count-up overtime) or Complete; it never auto-advances. Save for later returns Home with Continue Session. Recovery is paused with the correct current Block, order, durations and committed time.

The final Block automatically saves Session Summary. Recent Practice reopens summaries. Name/action/tempo/reference snapshots remain truthful after the reusable Block is edited or archived. Quick Block notes save with Save Block note, Save for later, Back, Complete or Skip; session notes have an explicit Save note button.

Timer values use monotonic elapsed time, never wall-clock differences. Foreground checkpoints are written every 250 ms plus a pause flush. An abrupt process kill/storage failure can lose the most recent uncommitted interval (plus scheduling/write latency); recovery conservatively restores saved time and never counts background time. This release has completed Stage 3 automated engineering checks; its owner phone acceptance gate is pending, so dependable daily-use acceptance is not yet claimed.

Block Library supports Active/Archived filters, literal case-insensitive search of name/action/notes and confirmed reversible archiving. No permanent delete API. Name/Action are required; durations and optional BPM accept positive whole numbers representable as 32-bit integers. Session duration conversion uses 64-bit milliseconds.

## Build, checks and packaging

Verified source faca5e5c01e0dfed9136dd58cf4a771b1362b6c8; successful CI: https://github.com/mrayover/practice.logger/actions/runs/40566516233 . CI builds debug and unsigned release APKs, runs unit tests/lint, then Android emulator catalog/session/restart/history and install-over-upgrade checks. Artifacts expire after 30 days. The CI debug artifact is installable for isolated testing, but its temporary key is unsuitable for updating the permanently signed local distribution. The unsigned release artifact must be signed before installation.

Developer command: ./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease (Windows .\gradlew.bat). Toolchain: JDK 17, Android SDK 36, AGP 8.13.2, Gradle 8.14.3, Kotlin 2.2.21, Compose and Room 2.8.5. Local signing tools are under ignored .tools; private signing material is under ignored .signing. Back up the entire .signing folder securely outside Git; losing it prevents in-place updates. Only the public certificate fingerprint is committed.

scripts/Publish-LocalApk.ps1 takes a downloaded unsigned release artifact ZIP, Version, VersionCode, Commit and CiRun. It pins the permanent certificate, signs/verifies the APK, validates package/version metadata, writes BUILD_INFO.json and INSTALL.txt, creates a versioned ZIP and extracts it into dist. Preserve prior versions. Clean only generated .staging directories after checksum verification. Never commit dist, tool downloads, keys, APKs or SDKs. All Git changes use GitHub Desktop.

Room schemas 1/2/3/4/5 are committed; explicit 1→2, 2→3, 3→4 and 4→5 migrations retain existing data. Font license and attribution are in APK assets and THIRD_PARTY_NOTICES.md. No Internet permission is requested.

## Stage 3 backup increment

Home → Backup and restore → Export backup opens Android's document picker and saves a version-3 UTF-8 JSON snapshot (legacy versions 1/2 are still readable) of Blocks (including archived), sessions, placements, notes and unfinished practice. Wait for Backup saved; a failed export may leave an incomplete file. Keep the plain text file private. Saved data only: commit quick/session notes before export.

Choose backup to restore validates the complete file and shows counts for confirmation. Restore works only in an empty app (for example, a new phone); it never replaces or merges existing practice data. Validation and the empty-target check/inserts run within one database transaction. Unfinished practice restores paused. Cancelling leaves data unchanged. Do not uninstall/clear the current app to test restore; use a spare device or disposable installation.

Limits: writer version 3 / reader versions 1, 2 and 3, 8 MiB, 20,000 records per type, nesting 32. Structural, type, reference/order/state and overflow checks plus transactional rollback are tested. See BACKUP_FORMAT.md and MANUAL_TESTS.md. Android system picker export/cancel/restore/process-reopen and 200% text workflows pass on API 26/35; unavailable-provider/retry tests pass. Vendor-specific phone behavior and TalkBack still need owner acceptance. Stage 3 engineering is complete, with the manual exit gate pending; Stage 4 is prepared, not completed. Source and signing/build provenance are in each distribution's BUILD_INFO.json. Android file-picker behavior: https://developer.android.com/training/data-storage/shared/documents-files .
## Stage 3 hardening and Stage 4 evaluation

Version 0.3.1 adds readable session controls at large text sizes, headings and distinct reorder labels (including placement number), a spoken timer label with its value, and a specific safe refusal message for nonempty restore. A shared application-owned Room instance fixes stale Home/catalog/practice observers immediately after restore; screen teardown cannot close another screen's database. Explicit Android backup exclusions retain the manual-export policy, and a local vector launcher icon identifies the app. Room schema/application ID/permanent signing key remain unchanged. Eighteen unit tests and both API 26/35 device jobs pass; screenshots of Summary, large-text screens and restored paused practice were reviewed. No automatic updater or deferred feature was introduced.

Owner instruction 2026-10-26: manual backup/recovery acceptance and the ten-real-session evaluation are deferred until after this build cycle; they remain unverified. Resume MANUAL_TESTS.md and EVALUATION.md when the owner is ready. Existing optional Session notes can hold observations; no new required data entry. Automated sessions never count. Fix observed friction and decide on at most one future feature after evidence. Ten real sessions and a future-feature decision cannot be manufactured by CI.
## Unfinished notes — 0.3.2

Use Save Block note while practicing to preserve a quick note without completing the Block. Save for later and Back pause/checkpoint and save it before Home; Continue Session restores the text, including after restart. Complete/Skip save final text. Typing alone is not saved against abrupt process death; use a save action. Session notes still require Save note. No schema, app identity or signing-key change. Nineteen unit tests and full API 26/35 device suites passed; CI https://github.com/mrayover/practice.logger/actions/runs/40417269518 . Backup/recovery owner acceptance and evaluation remain deferred by owner. See CURRENT_STATE.md for exact provenance and limitations.
## Independent unfinished sessions — 0.3.3

Practice Now stays available alongside Continue Session. Start fresh while keeping older practice; choose a dated session in Home's Unfinished sessions section. Each keeps its own position/time/notes and resumes paused. Only one foreground timer runs; one builder draft remains. Format-2 backups include all unfinished sessions; legacy format-1 imports still work. Older app builds reject new format-2 backups safely. Twenty unit tests and full API26/35 suites, including multi-session switching/process restart, pass. Latest verification: https://github.com/mrayover/practice.logger/actions/runs/40417269518 .

## Stage 5 metronome — 0.4.0 verified engineering release

0.4.0/build 8 adds Start/Stop click, working BPM (30–300), −1/+1 and exact Set BPM inside Focused Practice. It starts from the placement's last-used BPM, otherwise valid Block reference, otherwise 120. Timer and click are independent. A 48 kHz PCM stream schedules beats by sample position; tempo changes apply as new small buffers are rendered. Background/exit/recreation/Block changes stop audio, with no automatic restart. Media volume and output route affect audibility. All app text inherits bundled Lato.

Placement history retains the last BPM used by the click, shown in Summary; editing an idle working tempo alone doesn't record usage or change the reusable Block. Room migration 2→3 adds one nullable field. JSON writer 3 reads formats 1/2/3, restoring missing old BPM history as null; older app versions reject new backups. No recording bytes or playback state are included.

Source 4e08da13555696965094d74d5730ba73094329b6; successful CI https://github.com/mrayover/practice.logger/actions/runs/40439799999 . Build, all 24 unit tests, lint (0 errors/10 existing warnings), full API26/35 suites including metronome and 200% text checks passed. Screenshots reviewed; permanent certificate, package/version and ZIP extraction verified. Stage 6A free takes and 6B single-phrase loops remain separate later increments in BUILD_PLAN sections 12–16. Owner now generally reports the metronome works; individual timing/upgrade results were not supplied. Stage 4 is still 0/10.

Stage 6A free Block recording was delivered in 0.5.0/build 9 (2026-10-26) and generally accepted by the owner on 2026-10-27. Stage 6B phrase looping is now locally delivered in 0.6.0/build 11; detailed loop phone acceptance remains pending. JSON backups do not contain audio, so per-take export is required.

## Stage 6A — free Block recordings — 0.5.0 verified engineering release

Focused Practice → Recordings or an existing Block editor → Recordings opens the same Block's saved takes, including archived Blocks. Tap Record take to request microphone permission if needed; after allowing it, tap Record take to start. Stop and save creates a new independent take with duration/date. Play/Stop, Rename, confirmed Delete and Export audio all operate on individual takes. Export is normal AAC/M4A through Android's file picker. Recordings follow the Block ID through renaming/archiving and different sessions. No take-count cap, overdubbing or loop controls.

Timer and capture are independent; the Focused Practice recordings sheet includes the existing timer Start/Pause/Resume/Continue control, usable during capture. Capture can include the running metronome. Starting playback stops the click; background/leave stops microphone/playback, finalizing usable capture. Very short/failed/interrupted unsaved capture may be discarded with an error; previous saved takes remain. Restart/recreation never starts audio automatically. The screen stays awake during recording. Permission is requested only by Record take.

**JSON backups exclude recordings and take labels.** Export each take you want to keep outside this app. Clearing/uninstalling/device loss destroys unexported audio. JSON restores practice data with no takes; there is no audio-inclusive bulk restore or automatic audio import. Full media backup is not claimed.

Room 4 adds a separate take table via explicit 3→4 migration; practice data and JSON format 3/legacy readers remain intact. Private UUID pending files are validated/synced/renamed before metadata commit, with startup interrupted-file recovery. Source dfdf83b194c95fb0847ae5a1705b35708be0ead4 / https://github.com/mrayover/practice.logger/actions/runs/40447257853 passed 28 unit/Robolectric tests, lint (0 errors/10 existing warnings), and full Android 8/15 device suites including real encoder, document-picker export, reopen, upgrade and 200% text. The earlier 0.5.0 distribution remains preserved under dist/practice.logger-0.5.0-b9-dfdf83b; its BUILD_INFO.json retains exact provenance. Detailed microphone/output quality remains a phone acceptance item. Stage 6B adds phrase looping in the current release below.

## Current release — Stage 6B phrase loops, 0.6.0/build 11

The owner generally accepts free recording. Recordings → Record phrase now offers 30–300 BPM and 4/8/16/32 beats, four count-in clicks, one exact-frame WAV capture, and automatic repeat. Saved loops retain their recorded BPM/beats and remain independent of other free/loop takes. The timer stays independent; per-take playback, labels, confirmed deletion and WAV/M4A export remain available. Room 4→5 preserves existing data. JSON backups still exclude all recordings, take metadata and labels; export wanted audio separately.

Source faca5e5 / CI https://github.com/mrayover/practice.logger/actions/runs/40566516233 passed all 32 unit/Robolectric tests, lint (0 errors/10 existing warnings) and full Android 8/15 device suites. Downloaded artifact checksums match GitHub. Normal and 200% screenshots confirm the shortened Phrase BPM label is readable. Keyboard Done dismissal is not established by the captured screenshots and remains a phone check, along with large-text lower-control access. Input/output latency and audible loop boundaries also require physical phone testing; no seamless/latency-free claim. See CURRENT_STATE.md for exact evidence and limitations.

Signed build 11 uses the existing permanent certificate and is available above. Build 10 and all prior releases are preserved. Documentation is saved locally but awaits a GitHub Desktop commit/push because desktop automation is unavailable. Stage 3 phone backup acceptance and Stage 4 (0/10) remain pending. Save/storage changes await the owner discussion after phone looping checks.

## Click during loop recording — 0.6.1/build 12 prepared, not yet built

Owner reports phrase looping works and requests an audible guide through capture. Record phrase now has "Click while recording", enabled by default. Four count-in clicks always play; with the option on, the same output audio clock continues clicking at the phrase BPM while the microphone records. With it off, the count-in is followed by quiet capture. The guide ends before saved-loop playback and stops on cancellation/leave/background. Speaker bleed can be captured by the microphone; the app does not mix clicks digitally into the WAV. The practice timer and exact saved phrase stay independent. Save/storage changes await the owner discussion.

Three isolated PCM/WAV unit tests pass locally; Android compilation, full CI/native/large-text checks and signed packaging are pending. The existing build 11 APK above does not contain this fix. Source/tests/config and release records will use the owner-authorized Git-command exception to publish this update for CI.
