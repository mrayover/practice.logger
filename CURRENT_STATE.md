# Current work

0.9.0/build15 cream/standalone recording is prepared, not CI-verified or delivered. Last verified distribution remains0.8.0/build14 below. Changes local/uncommitted/unpushed pending publication workflow. Exact next task: publish prepared change, obtain compiler-exported Room6 schema, verify final-source fullCI/screenshots, sign/package with permanent key, update final logs.

## 2026-10-27 — Cream background and standalone recording increment

Owner explicitly requests light cream throughout app backgrounds and a Free recording block in Recording that starts a free take immediately and keeps all standalone takes together using existing Block recorder infrastructure. Implemented cream#FFF8EB across Material surfaces/dialogs/fields, Home, navigation and launch background; green banner/buttons/Lato retained. Recording has Free recording(one tap starts capture or requests mic then starts after grant) and Saved free recordings(browse without capture). Shared recorder retains Stop/save, playback, rename, confirmed delete/export and lifecycle cleanup. Phrase loop controls remain in Blocks only. No synthetic Block is created.

Room6 explicit5→6 rebuild makes take.blockId nullable, preserves every old take field/Block foreign key/index and all session data. Null owner denotes freestanding takes; active-screen guard distinguishes standalone recording from a closed screen. JSON3 still excludes all recordings/labels/metadata; per-take export retained. No bulk storage/backup redesign or new dependency/key/appID. Goals/routines remain deferred; Stage3 phone pending, Stage4 0/10. Target0.9.0/build15, upgrade16.

Prepared two meaningful Robolectric tests for frozen schema5 upgrade (free/loop takes, notes, FK enforcement) and standalone file save/reopen/rename/export/delete isolation. Two native phases test one-tap actual M4A, two independent takes, recreation save/no restart, playback/rename/browse and cold reopen without creating/changing Blocks or linked takes. 200% free-list controls/screenshots added; all prior suites retained. Full compilation/lint/unit/API26/35 pending CI; compiler-generated schema6 export must be committed before final verification. No new APK yet.
## Previous verified release

# Current state

Updated 2026-10-27 America/Los_Angeles. **0.8.0/build14** implements the owner's persistent banner, Practice Now recovery and standalone Tools request. Full engineering verification and permanent-key signed packaging complete. Owner generally accepts0.7.0; detailed0.8.0 phone/audio acceptance remains pending. Stage3 phone backup acceptance pending; Stage4 stays0/10.

## Screens and tools

- Shared green header with centered white practice.logger in bundled Lato Bold above every app route, including loading/error states; explicit Activity edge-to-edge keeps green behind white status icons after background/resume on Android15. Android dialogs/system pickers remain overlays. White body/home design retained; blank Goals/settings/Edit and Data usage remain explicitly approved placeholders.
- Practice Now contains Continue Session and individually selectable unfinished sessions plus the existing new-session builder. Old practice resumes paused; independent sessions/draft/notes retained. Routines retains recent history; no reusable routine editor implemented.
- Tools: matching green Metronome, Tuner, Timer and existing Backup and restore buttons.
- Metronome: one centered 30–300 BPM number, −1/+1 row then−5/+5 row, Start/Stop; existing sample-clock PCM engine, media volume. Independent of logged practice tempo/history.
- Tuner: actual in-memory mono microphone input; YIN normalized-difference/refined pitch with RMS/confidence gate; single-note 50–1600 Hz, A4=440Hz, note/octave/frequency/cents and flat/sharp/in-tune. No fake note or saved recording. Mic permission on Start, then explicit second Start; denial message supports Android settings. Quiet/no confident pitch shows Listening/play one note.
- Timer: independent stopwatch Start/Stop/Reset, snap wheel 1–180 whole minutes plus countdown Start/Stop/Reset and visible Time’s up at zero. Monotonic intervals. All tool audio stops and tool clocks pause on exit/background/recreation; never auto restart. Paused values survive ordinary recreation/navigation in the same process. Process death resets temporary tool state; no tool logs or background alarm/service.

No appID/com.mrayover.practicelogger.app, permanent key, schema5/migration, JSON3/legacy readers or dependency change. Existing catalog/practice/notes/recovery/history/metronome/free takes/phrase loops/default recording click/export retained. JSON backups exclude all take metadata/labels/audio; export wanted takes separately. Goals/reusable routines and broader save/storage remain deferred.

## Verification

App/test source main: c96b903c6617f0351baf99202e262cadc0a0696c
Successful CI: https://github.com/mrayover/practice.logger/actions/runs/40629201227
build 122621402598 SUCCESS; device (26) 122622391077 SUCCESS; device (35) 122622391133 SUCCESS.
All 38 unit/Robolectric tests pass, 0 failures/errors/skips; lint 0 errors/10 warnings. Android8/API26 and Android15/API35 pass expanded initial home/catalog workflow plus 18 direct phases, including the new Tools workflow and existing session recovery/history/backup/system pickers/install-over upgrade/native recording/phrase/export/200% checks. Upgrade test uses code15.

Five added pure tool tests cover44.1/48kHz pitch55–1318Hz, dominant harmonics, concert note/octave/cents/detuning, silence/DC/noise/quiet/invalid input and started/paused/resumed/reset elapsed intervals. Native Tools test checks ±1/±5/live click and recreation cleanup, actual microphone frames, background stop, rolled minute selection/accessible adjustment, running/stopped stopwatch/countdown, a full one-minute completion at zero/Time’s up and restart, paused reentry/recreation and reset. Existing recovery tests now use Practice Now; Routines has no unfinished-session list. Strict200% tool label checks and native screenshots retained. Normal API26/35 and 200% tool screenshots were visually reviewed: persistent centered banner, green menu/tempo buttons, microphone Listening state, rolled minute selector and zero/Time’s up. Large text scrolls to its controls; API35 status backing stays green after background/resume (sampled #315C47). Physical acoustic accuracy/audibility and owner phone acceptance remain pending. No local Android SDK/emulator installed; isolated local cached-Kotlin tool tests also pass.

Initial95ff51a/CI40625179105 superseded before final verification: review caught wheel initialization resetting paused countdown. Final source ignores unchanged selection; native check requires remaining time preserved on reentry/recreation. Sourceb1bc9fe/CI40625266383 passed all suites; source3ed1e8b/CI40626839878 additionally verified rendered Stop click state and actual one-minute completion/restart on both APIs. Screenshot review then caught white status-bar backing after API35 background/resume; explicit Activity edge-to-edge/status style corrects it. ac5d090/CI40628222978 passed build/38tests/lint, but API35 recorder denial test left the native permission dialog open after its first tap. Final test waits for dismissal and reacquires/retries once if still present, retaining all denial/no-capture/data assertions. Initial local4:1-decimated pitch analysis failed strict high-note accuracy; corrected to2:1 and all5 focused tests pass. No failed/superseded-source APK distributed. Downloaded verification, release and native reports match official GitHub SHA256 metadata. See WORK_LOG for chronology.

## Signed distribution

ZIP: dist/practice.logger-0.8.0-b14-c96b903.zip
APK: dist/practice.logger-0.8.0-b14-c96b903/practice.logger-0.8.0-b14-c96b903.apk
BUILD_INFO.json and INSTALL.txt accompany APK; dist/LATEST.txt points here.
APK SHA256: 106e653f3a18c3bb4b3b90f34fe823ce5f9cbea127e0b6d019474575d1240ba9
Permanent certificate SHA256: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830

Existing Publish-LocalApk.ps1 verified signature/pinned certificate/stable package/version0.8.0/code14 and ZIP-extracted APK checksum. All 31 preexisting baseline APK/ZIP files remain byte-identical. Private key unchanged/not uploaded. Android8+; install over permanent-key app without uninstalling. Old0.1.0 remains separate; no automatic transfer. Temporary CI debug key is unsuitable for updating the owner's installation.

CI artifacts retained30days under successful run:
- device-test-reports-api-26: 12791316896; sha256:594ddbc9c540057ba97b13b4a567467849aa65b8410907e84720e53708f797fe
- verification-reports: 12790792375; sha256:9aea2fc1f761a5fd8c425b5c9a050f903757bdbb155e42342c2a29924162dc8b
- practice-logger-unsigned-release: 12790687944; sha256:0327528d4c89505280ee9bf5b538c54c7b16e0dc7abc7b50c4f1174c48b9fc5d
- practice-logger-debug: 12790578562; sha256:01fcb837b7041ee748968fd89bcbbf6478092341152b68b29eb96f1a3b6bd4e3
- device-test-reports-api-35: 12790519413; sha256:17f7773f27b6725e9ef2a71c66415ff90728a170ae50f93acb54770be2bea257

## Limits and exact next task

Tuner is a foreground single-note utility; room noise/overtones/microphone route can affect readings. Synthetic/emulator tests do not establish acoustic phone accuracy. Tool timers pause away from the screen and provide no background alarm; temporary state resets after process death. Physical timing/output quality and TalkBack/vendor-specific permission behavior remain owner checks. Existing data/audio-backup exclusions, explicit note saves, possible last250ms+write-latency checkpoint loss, and partial failed export destinations remain documented in MANUAL_TESTS/BACKUP_FORMAT. No storage redesign or fabricated usage statistics.

Owner selected option1 authorizing Git commands for this update/release records because Desktop controls unavailable. App source committed/pushed in existing Windows checkout; closing records follow as separate commit. Default Desktop preference retained for future work. No synced source modification, cloud checkout, alternate repo or PR.

Exact next task: install signed0.8.0/build14 over the current app; check banner/routes and unfinished sessions under Practice Now, hear ±1/±5 click, compare tuner with trusted440Hz single notes/flat/sharp, roll1minute and check countdown completion plus stopwatch/Stop/Reset/background pause, then reopen old practice/takes. Report actual phone/Android/audio route and findings; discuss save/storage afterward before implementation.