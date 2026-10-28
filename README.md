# practice.logger

Latest installable release: **0.11.0/build20**, with Goals and attributed practice. Permanently signed;57unit tests/build/lint pass. Owner reports the app works and explicitly stopped final device testing. See CURRENT_STATE.md for exact verification limits.

An offline Android practice tool: create reusable Blocks and saved ordered Routines, arrange sessions, practice with a timer and inspect saved results. Kotlin + Compose + Room, with locally bundled Lato Regular/Bold throughout app-controlled text.

Stages 0, 1 and 2 passed automated verification. The owner generally confirms that 0.3.0 works on their phone (model/OS and individual checklist results not supplied). Stage 3 now has versioned backup/export and protected empty-app restore; Stage 3 engineering hardening is verified on Android 8 and 15, including real system document pickers and 200% text; owner phone acceptance is pending. Stage 4 ten-session evaluation is prepared in EVALUATION.md (0/10 real sessions recorded). Owner approved Stage 5 metronome followed by Stage 6A/6B Block recording on 2026-10-26; Stage 5 engineering is verified in 0.4.0/build 8; owner generally accepts the metronome; detailed phone timing results were not supplied. Overdubbing and other deferred features remain excluded; Goals are now implemented below; reusable Routines are now owner-approved and implemented as described below. The owner-approved standalone tuner/Tools increment is described below.

Read AGENTS.md, CURRENT_STATE.md, WORK_LOG.md and authoritative BUILD_PLAN.md completely before every development session. Every session, including incomplete work or fixes, must update the records and publish intended changes through GitHub Desktop.

## Install the versioned local build

- ZIP: dist/practice.logger-0.11.0-b20-c0b1dfe.zip
- APK: dist/practice.logger-0.11.0-b20-c0b1dfe/practice.logger-0.11.0-b20-c0b1dfe.apk
- Provenance/signature/hash: dist/practice.logger-0.11.0-b20-c0b1dfe/BUILD_INFO.json

Android8+; install over current permanent-key app without uninstalling to retain practice data/audio. All earlier releases remain under dist. JSON backup excludes recordings; export wanted takes separately. Original0.1.0 remains separate.

## Practice workflow

Home → Practice Now → add active Blocks. Create a Block through the shared editor if needed. Up/Down reorders placements; remove placements or override their duration without changing the reusable Block. A draft survives leaving the screen and restarting. Start Session requires at least one placement.

Focused Practice starts paused. Start/Resume counts only foreground active time; Pause, leaving the screen or backgrounding stops it. Each next Block waits for Start. Complete can finish early; Skip records the actual time already spent. At 00:00 the timer pauses and waits for Continue (count-up overtime) or Complete; it never auto-advances. Save for later returns Home; open Practice Now for Continue Session. Recovery is paused with the correct current Block, order, durations and committed time.

The final Block automatically saves Session Summary. Recent Practice reopens summaries. Name/action/tempo/reference snapshots remain truthful after the reusable Block is edited or archived. Quick Block notes save with Save Block note, Save for later, Back, Complete or Skip; session notes have an explicit Save note button.

Timer values use monotonic elapsed time, never wall-clock differences. Foreground checkpoints are written every 250 ms plus a pause flush. An abrupt process kill/storage failure can lose the most recent uncommitted interval (plus scheduling/write latency); recovery conservatively restores saved time and never counts background time. This release has completed Stage 3 automated engineering checks; its owner phone acceptance gate is pending, so dependable daily-use acceptance is not yet claimed.

Block Library supports Active/Archived filters, literal case-insensitive search of name/action/notes and confirmed reversible archiving. No permanent delete API. Name/Action are required; durations and optional BPM accept positive whole numbers representable as 32-bit integers. Session duration conversion uses 64-bit milliseconds.

## Build, checks and packaging

Verified source e1c884c91faa26576de8765296e6f203399df8c5; successful CI: https://github.com/mrayover/practice.logger/actions/runs/40634982208 . CI builds debug and unsigned release APKs, runs unit tests/lint, then Android emulator catalog/session/restart/history and install-over-upgrade checks. Artifacts expire after 30 days. The CI debug artifact is installable for isolated testing, but its temporary key is unsuitable for updating the permanently signed local distribution. The unsigned release artifact must be signed before installation.

Developer command: ./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease (Windows .\gradlew.bat). Toolchain: JDK 17, Android SDK 36, AGP 8.13.2, Gradle 8.14.3, Kotlin 2.2.21, Compose and Room 2.8.5. Local signing tools are under ignored .tools; private signing material is under ignored .signing. Back up the entire .signing folder securely outside Git; losing it prevents in-place updates. Only the public certificate fingerprint is committed.

scripts/Publish-LocalApk.ps1 takes a downloaded unsigned release artifact ZIP, Version, VersionCode, Commit and CiRun. It pins the permanent certificate, signs/verifies the APK, validates package/version metadata, writes BUILD_INFO.json and INSTALL.txt, creates a versioned ZIP and extracts it into dist. Preserve prior versions. Clean only generated .staging directories after checksum verification. Never commit dist, tool downloads, keys, APKs or SDKs. All Git changes use GitHub Desktop.

Room schemas 1/2/3/4/5 are committed; explicit 1→2, 2→3, 3→4 and 4→5 migrations retain existing data. Font license and attribution are in APK assets and THIRD_PARTY_NOTICES.md. No Internet permission is requested.

## Stage 3 backup increment

Home → Tools → Backup and restore → Export backup  opens Android's document picker and saves a version-4 UTF-8 JSON snapshot (legacy versions 1/2/3 are still readable) of Blocks (including archived), saved Routines/entries, sessions, placements, notes and unfinished practice. Wait for Backup saved; a failed export may leave an incomplete file. Keep the plain text file private. Saved data only: commit quick/session notes before export.

Choose backup to restore validates the complete file and shows counts for confirmation. Restore works only in an empty app (for example, a new phone); it never replaces or merges existing practice data. Validation and the empty-target check/inserts run within one database transaction. Unfinished practice restores paused. Cancelling leaves data unchanged. Do not uninstall/clear the current app to test restore; use a spare device or disposable installation.

Limits: writer version 4 / reader versions 1, 2, 3 and 4, 8 MiB, 20,000 records per type, nesting 32. Structural, type, reference/order/state and overflow checks plus transactional rollback are tested. See BACKUP_FORMAT.md and MANUAL_TESTS.md. Android system picker export/cancel/restore/process-reopen and 200% text workflows pass on API 26/35; unavailable-provider/retry tests pass. Vendor-specific phone behavior and TalkBack still need owner acceptance. Stage 3 engineering is complete, with the manual exit gate pending; Stage 4 is prepared, not completed. Source and signing/build provenance are in each distribution's BUILD_INFO.json. Android file-picker behavior: https://developer.android.com/training/data-storage/shared/documents-files .
## Stage 3 hardening and Stage 4 evaluation

Version 0.3.1 adds readable session controls at large text sizes, headings and distinct reorder labels (including placement number), a spoken timer label with its value, and a specific safe refusal message for nonempty restore. A shared application-owned Room instance fixes stale Home/catalog/practice observers immediately after restore; screen teardown cannot close another screen's database. Explicit Android backup exclusions retain the manual-export policy, and a local vector launcher icon identifies the app. Room schema/application ID/permanent signing key remain unchanged. Eighteen unit tests and both API 26/35 device jobs pass; screenshots of Summary, large-text screens and restored paused practice were reviewed. No automatic updater or deferred feature was introduced.

Owner instruction 2026-10-26: manual backup/recovery acceptance and the ten-real-session evaluation are deferred until after this build cycle; they remain unverified. Resume MANUAL_TESTS.md and EVALUATION.md when the owner is ready. Existing optional Session notes can hold observations; no new required data entry. Automated sessions never count. Fix observed friction and decide on at most one future feature after evidence. Ten real sessions and a future-feature decision cannot be manufactured by CI.
## Unfinished notes — 0.3.2

Use Save Block note while practicing to preserve a quick note without completing the Block. Save for later and Back pause/checkpoint and save it before Home; Continue Session restores the text, including after restart. Complete/Skip save final text. Typing alone is not saved against abrupt process death; use a save action. Session notes still require Save note. No schema, app identity or signing-key change. Nineteen unit tests and full API 26/35 device suites passed; CI https://github.com/mrayover/practice.logger/actions/runs/40417269518 . Backup/recovery owner acceptance and evaluation remain deferred by owner. See CURRENT_STATE.md for exact provenance and limitations.
## Independent unfinished sessions — 0.3.3

Practice Now stays available alongside Continue Session. Start fresh while keeping older practice; choose a dated session under Practice Now. Each keeps its own position/time/notes and resumes paused. Only one foreground timer runs; one builder draft remains. Format-2 backups include all unfinished sessions; legacy format-1 imports still work. Older app builds reject new format-2 backups safely. Twenty unit tests and full API26/35 suites, including multi-session switching/process restart, pass. Latest verification: https://github.com/mrayover/practice.logger/actions/runs/40417269518 .

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

## Earlier Stage 6B phrase-loop release — 0.6.0/build 11

The owner generally accepts free recording. Recordings → Record phrase now offers 30–300 BPM and 4/8/16/32 beats, four count-in clicks, one exact-frame WAV capture, and automatic repeat. Saved loops retain their recorded BPM/beats and remain independent of other free/loop takes. The timer stays independent; per-take playback, labels, confirmed deletion and WAV/M4A export remain available. Room 4→5 preserves existing data. JSON backups still exclude all recordings, take metadata and labels; export wanted audio separately.

Source faca5e5 / CI https://github.com/mrayover/practice.logger/actions/runs/40566516233 passed all 32 unit/Robolectric tests, lint (0 errors/10 existing warnings) and full Android 8/15 device suites. Downloaded artifact checksums match GitHub. Normal and 200% screenshots confirm the shortened Phrase BPM label is readable. Keyboard Done dismissal is not established by the captured screenshots and remains a phone check, along with large-text lower-control access. Input/output latency and audible loop boundaries also require physical phone testing; no seamless/latency-free claim. See CURRENT_STATE.md for exact evidence and limitations.

Signed build 11 and build 10 remain preserved in dist. Build 11 release records were published as f6e38e0; the owner generally accepts that release. The current recording-click update is below. Stage 3 phone backup acceptance and Stage 4 (0/10) remain pending. Save/storage changes await the owner discussion after phone looping checks.

## Click during loop recording — 0.6.1/build 12 delivered

In Record phrase, **Click while recording** defaults to on. Four count-in clicks continue into a phrase-tempo click while the microphone captures the selected 4/8/16/32 beats. Turn it off for a count-in followed by quiet capture. The same audio clock drives both phases; the guide stops before saved-loop playback and on cancellation/background/exit. The timer stays independent. Speaker bleed can be captured; no click is mixed digitally into the WAV. Existing saved takes, migrations, JSON exclusions, signing identity and prior releases remain intact.

Source f4f827bde0921b805b4f3126555344507bb0e3fd, CI https://github.com/mrayover/practice.logger/actions/runs/40603962994 passed build, all 33 unit/Robolectric tests, lint (0 errors/10 existing warnings) and full API26/35 native/large-text suites, including actual output-counter checks during on/off capture. Normal and 200% screenshots reviewed. Existing permanent publisher verified the signed versioned ZIP/APK above and preserved all 28 earlier distribution files. Phone audibility/timing and the checklist in MANUAL_TESTS.md remain owner checks. Save/storage changes await discussion after this fix.

## Home sketch redesign — 0.7.0/build13 delivered

Centered white Lato Bold practice.logger in a separated green header, white background, blank Goals rows with inactive settings/Edit, five stacked green buttons and plain Data usage footer match the owner's requested layout. Practice Now opens session selection; Blocks opens Block Library; Routines holds unfinished sessions/history; Tools retains backup/restore; Recording chooses a Block for its existing saved takes. Goals/reusable routine editing and storage statistics were not implemented. The owner explicitly approved visual placeholders for this increment.

Source 61b76d50e1ce0dc995cf4775d6d1840a5d10f007 / https://github.com/mrayover/practice.logger/actions/runs/40620587011 passed compilation, all33 unit tests, lint0errors/10warnings and full API26/35 native/navigation/200% checks. Actual normal/large-text home screenshots reviewed. Existing data, recordings, signing and prior releases are retained; detailed phone visual/audio acceptance remains pending. The new signed ZIP/APK are linked above; see CURRENT_STATE.md for provenance and limitations.

## Persistent banner and standalone Tools0.8.0 — delivered

Home's centered green Lato banner now stays above all app routes. Practice Now contains unfinished-session selection and new-session builder; Routines retains recent history. Tools has green Metronome/Tuner/Timer plus backup. Metronome center BPM/±1/±5 uses existing click30–300BPM; tuner analyzes microphone single notes50–1600Hz atA4=440Hz with note/cents/direction; Timer offers stopwatch and rolled1–180 whole minutes. Audio stops and clocks pause on exit/background/recreation; no background alarm or persistent tool log. Existing practice/audio data and permanent key retained.

Source c96b903c6617f0351baf99202e262cadc0a0696c, CI https://github.com/mrayover/practice.logger/actions/runs/40629201227:38unit tests, lint0errors/10warnings, full API26/35 initial+18direct phases and200% tools/navigation checks pass. Normal API26/35 and 200% tool screenshots were visually reviewed: persistent centered banner, green menu/tempo buttons, microphone Listening state, rolled minute selector and zero/Time’s up. Large text scrolls to its controls; API35 status backing stays green after background/resume (sampled #315C47). Signed ZIP/APK above; phone microphone accuracy/audibility/countdown completion acceptance pending. Historical release sections retain earlier navigation descriptions; current continuation route is Practice Now. Goals/reusable routines/storage redesign deferred.
## 2026-10-27 — Cream background and standalone recording increment

Owner explicitly requests light cream throughout app backgrounds and a Free recording block in Recording that starts a free take immediately and keeps all standalone takes together using existing Block recorder infrastructure. Implemented cream#FFF8EB across Material surfaces/dialogs/fields, Home, navigation and launch background; green banner/buttons/Lato retained. Recording has Free recording(one tap starts capture or requests mic then starts after grant) and Saved free recordings(browse without capture). Shared recorder retains Stop/save, playback, rename, confirmed delete/export and lifecycle cleanup. Phrase loop controls remain in Blocks only. No synthetic Block is created.

Room6 explicit5→6 rebuild makes take.blockId nullable, preserves every old take field/Block foreign key/index and all session data. Null owner denotes freestanding takes; active-screen guard distinguishes standalone recording from a closed screen. JSON3 still excludes all recordings/labels/metadata; per-take export retained. No bulk storage/backup redesign or new dependency/key/appID. Goals/routines remain deferred; Stage3 phone pending, Stage4 0/10. Target0.9.0/build15, upgrade16.

Prepared two meaningful Robolectric tests for frozen schema5 upgrade (free/loop takes, notes, FK enforcement) and standalone file save/reopen/rename/export/delete isolation. Two native phases test one-tap actual M4A, two independent takes, recreation save/no restart, playback/rename/browse and cold reopen without creating/changing Blocks or linked takes. 200% free-list controls/screenshots added; all prior suites retained. Full compilation/lint/unit/API26/35 pending CI; compiler-generated schema6 export must be committed before final verification. No new APK yet.
## 0.9.0/build 15 — verified signed delivery, 2026-10-28 America/Los_Angeles

Light cream #FFF8EB covers app backgrounds, Material surfaces/dialogs/fields, launch and navigation; green banner/buttons and Lato retained. Recording → Free recording starts native M4A capture immediately when permission/storage/foreground are ready, or after microphone permission is allowed. Saved free recordings opens the same collection without capture. Shared Block recorder provides Stop/save, playback, rename, confirmed delete and per-take export. Rotation/background/exit stops and saves usable capture without starting another; free takes remain together, separate from Block takes. No synthetic Block is created; phrase controls remain in Blocks.

Room schema6 explicit5→6 migration preserves old take IDs/files/labels/tempo/beats, foreign keys/indexes and all practice data; nullable blockId denotes standalone ownership. Compiler-generated6.json committed. JSON3/legacy readers unchanged and still exclude ALL takes/labels/audio, including standalone: export wanted audio separately. No new dependency, appID or permanent-key change; broader storage/backup, Goals/reusable routines remain deferred. Stage3 phone acceptance pending; Stage4 stays0/10.

Verified app/test source: e1c884c91faa26576de8765296e6f203399df8c5
Successful CI: https://github.com/mrayover/practice.logger/actions/runs/40634982208
40 unit/Robolectric tests pass, no failures/errors/skips; lint0 errors/10 warnings. Both Android8/API26 and Android15/API35 pass initial workflow plus20 direct phases, including new standalone native capture/recreation/rename/playback/cold reopen/isolation checks, frozen schema5 upgrade/file safety, all prior practice/backup/recording/phrase/tools/upgrade and200% checks. Upgrade code16. Normal Android8/15 free-take lists and cream Home plus 200% free-recording controls visually reviewed; cream surfaces, retained green branding, two saved takes and shared playback/rename/export/delete controls are visible. The first menu-named screenshot captured the preceding Home frame; automated button navigation/capture assertions and actual saved-list screenshots verify the recording destination. Physical phone/audio quality acceptance remains pending. No local Android SDK/emulator installed.

ZIP: dist/practice.logger-0.9.0-b15-e1c884c.zip
APK: dist/practice.logger-0.9.0-b15-e1c884c/practice.logger-0.9.0-b15-e1c884c.apk
APK SHA256: 1a6a9ffcda4cc80938d4b43491040f88b72dc28758eb5445dd2fd47e1810f42f
Permanent certificate: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830
Existing publisher verified signature/pinned certificate/stable com.mrayover.practicelogger.app/version0.9.0/code15/ZIP extraction. All33 prior baseline APK/ZIP files unchanged; LATEST updated after packaging. Private key unchanged/not uploaded. Install over current permanent-key app without uninstalling.

CI artifacts (30-day retention):
- practice-logger-debug: 12793865029; sha256:27a782946535999d6d8376d31fe58db74b4e4e3fae317b74d490710e1630acd5
- practice-logger-unsigned-release: 12793620700; sha256:2dfa49348b3802926c38920c3803fbf984360b8b8a8987f8d51357e9e588e7d8
- device-test-reports-api-35: 12793436676; sha256:f4e1bb520720baefb91b8ed31b2f79bb12af2a1151bf85d2ef1903eca600b358
- verification-reports: 12793062805; sha256:be6b4322679508c7ebf28fa5fd9b6c2c076160e91a316c02a5b6fc92e4efbe60
- device-test-reports-api-26: 12792904844; sha256:95863457a8735344b510036bcfc466dbe7b86ac8d0244af08648910fbb0cd517

Owner authorized Git commands for this update/schema/closeout because Desktop controls unavailable; default Desktop preference retained. Existing Windows checkout, synced sources untouched; app source/schema pushed, closing records publish separately with local/remote clean status verified in handoff.

Exact next task: owner installs signed0.9.0/build15, checks cream screens/dialogs, taps Free recording and saves two takes, listens/renames/exports, browses Saved free recordings without mic, restarts/rotates/backgrounds and confirms old Blocks/session notes/free/loop takes untouched. Report phone/Android/mic route/results; discuss broader save/storage afterward. JSON still excludes audio.

## Saved Routines (0.10.0/build16)

Owner-approved reusable Routines supersede earlier deferrals for this feature. Home → Practice Now → arrange Blocks and durations → Save as Routine names the selection without starting practice or changing the draft/library. Home → Routines now contains saved templates above retained Recent Practice/history. New Routine and Edit Routine support name, active Block selection, repeated Blocks, Up/Down, Remove and per-placement whole-minute durations. Deleting requires confirmation and removes only the template.

Practice Routine creates a fresh paused session with the current Block name/action/reference notes/tempo and saved placement durations. Each start gets independent IDs, time, notes, working BPM and outcomes; existing sessions remain snapshots. Builder drafts and other unfinished sessions are preserved. Archived/unavailable Blocks are named explicitly and prevent the entire routine from starting; edit/remove/replace the reference or restore its Block. Nothing is silently skipped. Editor changes survive recreation; Back offers discard confirmation.

Room7 explicit6→7 migration adds only two tables/indexes and preserves all practice/audio records. JSON4 backs up routines/entries; legacy1–3 readers retained. Audio remains separately exported. Cream/green/Lato/shared banner/app identity/permanent key retained. Goals, overdubbing and broader storage redesign remain deferred; Stage3 phone acceptance pending and Stage4 stays0/10. Full CI, compiler schema export and signed package are verified in the delivery record below. Earlier release sections retain historical scope/results.


## 0.10.0/build16 — Saved Routines verified delivery, 2026-10-28 America/Los_Angeles

Implemented reusable named ordered Routine templates with stable Block references and independent placement durations. Builder Save as Routine leaves draft/Blocks unchanged and starts no practice. Home → Routines provides create/edit/rename/add/remove/reorder/whole-minute duration/confirmed delete/Practice Routine above retained Recent Practice/history. Editor state survives recreation; Back confirms unsaved discard. Duplicate Block placements work. Missing/archived references are explicit and prevent the whole start; edit/remove/replace or restore the Block, never silently skip.

Changed components: new RoutineData.kt, RoutineViewModel.kt, RoutineScreens.kt, RoutineRepositoryTest.kt, RoutineBackupTest.kt and RoutineWorkflowTest.kt; integrated BlockDatabase.kt, SessionData.kt, PracticeViewModel.kt, PracticeScreens.kt, MainActivity.kt, BackupData.kt and BackupScreen.kt. Extended SystemPickerTest.kt and AccessibilityWorkflowTest.kt; synchronized existing LaunchTest.kt; updated app/build.gradle.kts, android.yml, generated7.json and all seven project documents. Publisher, signing material, audio implementation and original prefixed plans untouched.

Practice Routine creates a fresh paused IN_PROGRESS session using current Block name/action/tempo/reference notes and saved Routine durations through existing SessionRepository/PracticeViewModel. Each start gets independent session/placement IDs, time, notes, BPM/outcomes. Builder draft and other unfinished sessions remain; later Block/template changes and deleting a template cannot rewrite earlier sessions/history. Existing timers/overtime/Complete/Skip/click/Block free/loop recordings/notes/summary/recovery reused.

Room7 explicit6→7 adds routines/routine_placements and FK/indexes only; compiler-generated7.json identity752227f5c28e47ea770144988e8d8cab committed. Compared schema6→7: all four original entity definitions unchanged. Existing migrations retained, no destructive fallback. JSON4 includes routine/entry arrays and IDs; validates names/timestamps/order/references/durations and restores everything atomically into an empty target. Legacy1–3 readers retained; future versions rejected. Nonempty guard covers Blocks/sessions/routines/takes, including standalone-only metadata. All take audio/labels/metadata still excluded; export audio individually. Cream#FFF8EB, green#315C47, bundled Lato/persistent banner, appID/minSDK26/target36/permanent key unchanged; no new dependency/recording/Goals/cloud/storage redesign.

Verified source: aa78783e9b264dccec64018362b1a88cf0f5e255
Successful CI: https://github.com/mrayover/practice.logger/actions/runs/40733036079
51 unit/Robolectric tests pass,0 failures/errors/skips (11 new Routines tests); lint0errors/10existingwarnings; debug/release build pass. API26/Android8 and API35/Android15 each pass initial catalog workflow plus22 direct phases, including two new native Routine CRUD/builder-save/repeated-start/current-Block/archive/delete/recreation/cold-recovery checks, real Android picker JSON4 routine export/restore/reopen and200% routine editor/duration/save dialogs. All prior catalog/practice/timer/overtime/multiple-session/notes/history/upgrade/backup/click/native-recording/phrase/standalone-tools/free-recording suites retained and pass. Upgradecode17. Normal Routine list/editor/fresh practice/recovery/history and200% screenshots visually reviewed; cream/green/persistent banner retained and controls readable/scrollable. Automated emulators are not physical phone acceptance; real audio/phone/TalkBack/provider behavior remains owner checklist work. No local Android SDK/emulator installed.

ZIP: dist/practice.logger-0.10.0-b16-aa78783.zip
APK: dist/practice.logger-0.10.0-b16-aa78783/practice.logger-0.10.0-b16-aa78783.apk
APK SHA256: d5fc5f96ccb57ee88f3364fd8a74d2d15ee2e18f02b8622af27642a08c28a2c5
Permanent certificate: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830
Existing publisher verified signature/pinned certificate/stable com.mrayover.practicelogger.app/version0.10.0/code16/ZIP extraction; unchanged private key never uploaded. All35 prior APK/ZIP files rehashed unchanged; versioned ZIP/extracted folder retained and LATEST updated. Install over existing permanent-key app without uninstalling. Original0.1.0 remains separate.

CI artifacts (30-day retention):
- practice-logger-unsigned-release: 12836875419; sha256:baacda1d92da9f5bd8ef6d4e988e509e1acf6719674e9ea0e3f4ccc0deaa0495
- device-test-reports-api-26: 12836742349; sha256:99d13561fa7f85bf4807c319fecb7df4bd018aa62f905f09a5c8eeafc3957ae2
- practice-logger-debug: 12836417224; sha256:04a52d3a575fe057d879acf80572003fcd423e7ee3f163aa18f591bc110e7de1
- verification-reports: 12836332219; sha256:8f9267b1408ba97bbec57c2a20c55d9f0def3faf64a957f85d219b814953f803
- device-test-reports-api-35: 12836072808; sha256:52bfa41538c6d31d6428b0588329908aa8c50e59688a7087b495e3512e5e4040

Development history: f2b6d02 initial implementation/51-test build;8781678 compiler schema milestone. Initial device jobs were cancelled by newer schema-source verification, not counted as passing. Verification exposed catalog save/navigation/rotation and large-text target-readiness races. Retained every workflow assertion and both recreations; added exact-target/displayed-row waits and waited for committed navigation before rotation. Temporary forced-scroll/IME probes were removed. Bounded catalog timeout plus thread/screenshot diagnostics captured Espresso idle polling and undrawn row; final aa78783 clears editing focus/hides IME before catalog and Routine-editor save navigation. Final API26 catalog completes13.092s and API35 completes12.921s; both initial+22 phases pass. Earlier failed/cancelled runs are retained in WORK_LOG and never counted as passes. Owner's final steering prioritized immediate build: signed/package completed while final device results were arriving; all final gates are now independently confirmed successful.
All seven project documents updated (AGENTS,README,BUILD_PLAN,CURRENT_STATE,WORK_LOG,BACKUP_FORMAT,MANUAL_TESTS). WORK_LOG earlier entries preserved. Owner explicitly authorized Git commands for this update/schema/fixes/closeout; Desktop default remains for future work. Actual existing Windows checkout edited; synced sources untouched. Source/test/schema published to main; closing documentation published separately, final local/remote SHA and clean state verified in handoff. No owner phone results fabricated; Stage3 phone acceptance pending, Stage4 stays0/10.

Exact next task: owner installs signed0.10.0/build16 over current app and executes Saved Routines phone checklist in MANUAL_TESTS.md: builder save/no mutation; CRUD/reorder/durations/restart; two independent starts and existing draft; current Block changes/archive handling; save-for-later/cold recovery; confirm old notes/history/free/loop/standalone audio; JSON4 export/spare-empty restore/legacy import;200%/TalkBack. Report phone/Android/results. Address any feedback before choosing further feature scope.

## Goals — 0.11.0/build20 finalized
Home Goals settings/Edit opens management of up to five active Goals. Set outcome, notes and manual percentage, reorder priorities, complete/reopen and browse completed Goals. Top two active Goals appear on Home. Details manage shared existing Blocks and start an independent paused Goal session with current Block content/default durations; drafts and prior sessions remain. Archived/unavailable links are visible and excluded from Goal practice; add active Blocks if none are usable. Actual time sums committed placements from explicitly attributed sessions only, including unfinished/skipped practice; time never changes progress. History shows Goal attribution. Room8/JSON5 retain older migrations and backup1–4 readers. Audio remains excluded from JSON. Build20 is permanently signed;57unit tests/build/lint pass. Focused Goal/restart/200%checks pass both APIs. Final device matrix stopped at the owner's request after their general report that the app works; full final verification is not claimed. See CURRENT_STATE.md and distribution VERIFICATION_STATUS.txt.
