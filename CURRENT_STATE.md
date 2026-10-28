# Saved Routines checkpoint — 2026-10-28

Implementation and11 focused tests/two native phases prepared; Room7/JSON4 migration and backward readers implemented. No new build/test/signing claims yet. Exact next task: authorize publication workflow, run CI/export schema7, fix failures, verify API26/35 and sign0.10.0/build16. Prior verified0.9.0 release remains below. Stage3 phone pending; Stage4 0/10.

# Current work — saved Routines, 2026-10-28

Target0.10.0/build16 is approved and in implementation; no new tests/build/APK claimed. Last verified signed release remains0.9.0/build15 below. Exact next task: implement and verify the Routines-only scope in latest BUILD_PLAN/WORK_LOG; publish via Desktop or a new explicit owner exception, then full CI/schema7/permanent-key delivery.

# Current state

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
Routines first build milestone: source f2b6d02, CI40717132554 build success;51 unit tests pass/lint0errors10warnings. Compiler-generated7.json retained locally and now publishing. Final device checks/screenshots/signing pending.

Device checkpoint: CI40718045337 API35 initial+22phases pass. API26 LaunchTest fails to find editor after unscrolled tap; test scroll/wait fix prepared and now publishing. All51 unit/lint/debug/release pass; full rerun, screenshot review, signing and closing records still next.

CI40720805649: source2e47c58 build51unit/lint0errors10warnings passes; API35 initial+21phases passes then200% click races catalog fixture readiness. Exact-target wait correction prepared and now publishing. API26 long-running job will be superseded by this rerun; inspect cancelled log, do not count as pass. No signed0.10.0 release yet; final gates/signing/closing records still next.
