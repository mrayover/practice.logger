# Visual polish0.11.1/build21 — build pending

Cream#F1E6D3, thin Home saved-percentage Goal bars, outlined/bold Back to Home. No functional/data/schema changes. Existing signed build20 retained. Exact next task: run established CI, sign build21 immediately, inspect normal/200% screenshots, update records and push. Usage23%remaining; development cutoff8%.

## Previous release record

# Current state — Goals0.11.0/build20 finalized

## Goals 0.11.0/build20 — finalized at owner request, 2026-10-28 America/Los_Angeles

Goals implemented: up to five active Goals, manual0–100%progress, title/outcome/notes, stable IDs/timestamps, priority order/top-two Home, completion/reopen and completed list. Details associate shared existing Blocks, identify archived/unavailable links and show actual explicitly attributed practice time. Practice Goal creates independent paused sessions using active Block defaults; drafts/unfinished sessions and all existing timer/audio/notes/summary/history behavior remain. Goal time is observed only while detail is open, avoiding a new all-history aggregate every250ms. Existing cream/green/Lato/banner/Home design retained. Catalog keyboard/inset and search viewport corrections preserve original workflow assertions.

Room8 additive7→8 migration creates only goals/goal_blocks/session_goals; all six previous entities exactly unchanged. Compiler identity14213defd37c40eaad2f7265f9110363. JSON5 preserves Goals/order/links/progress/notes/one-Goal session attribution, validates relationships/limits/order/totals and restores atomically into an empty target. Readers1–4 retained; audio/take metadata/labels still excluded. Stable appID/permanent signing key/data/audio/previous distributions retained.

Source: c0b1dfe5c8d9f3f0f8ef8084a741963e18eb3bb4
CI: https://github.com/mrayover/practice.logger/actions/runs/40769976160
Verified build123104223349:57unit/Robolectric tests,0failures/errors/skips; lint0errors/10existingwarnings; debug/release successful; generated8.json exactly matches committed schema. Focused native Goal/restart/200%checks pass API26/35. Earlier build19 full suites each pass initial catalog plus24direct phases, then the final old accessibility test fails its empty-draft fixture assumption after Goal correctly preserved a draft. Test-only setup isolation and explicit cold draft assertion added; targeted full200%AccessibilityWorkflowTest/API26 passes40769225695. Final build20 device matrix cancellation requested by owner on2026-10-28; full final matrix completion is NOT claimed.

Owner says: "The app works. Stop testing and finalize this output." General owner acceptance recorded; exact installed build, phone/Android and individual checklist results not supplied. No further tests run after this instruction. This explicit owner instruction supersedes waiting for all final device gates. Stage3 detailed phone backup acceptance pending; Stage4 stays0/10.

ZIP: dist/practice.logger-0.11.0-b20-c0b1dfe.zip
APK: dist/practice.logger-0.11.0-b20-c0b1dfe/practice.logger-0.11.0-b20-c0b1dfe.apk
APK SHA256: c6285627aa5b5082d193e0464d77c53b7599cffcaa49666435d4e1b59f8029ca
Permanent certificate: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830
Existing publisher verified signature, pinned certificate, package/version and extracted APK hash. All56pre-session APK/ZIP files rehashed unchanged; signed17/18/19candidates also preserved. BUILD_INFO.json, INSTALL.txt and VERIFICATION_STATUS.txt accompany the final distribution; LATEST points to20. No private signing material uploaded.

All intended source/schema/tests and closing records pushed to main using the owner's explicit Git-command authorization; existing Windows checkout used and synced sources untouched. No new feature work remains authorized in this session. Known verification limitation: final combined device matrix stopped by owner; no unresolved product defect is claimed without evidence. Phone/provider/TalkBack/audio details remain checklist items.

Exact next action: use signed build20 over the existing permanent-key app without uninstalling, and report any observed issue. Detailed optional phone checklist remains in MANUAL_TESTS.md; resume deferred acceptance/evaluation only when the owner requests it.

## Previous release history

# Current state — Saved Routines 0.10.0/build16


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


## 2026-10-28 — Efficiency procedure adopted

Owner requested and authorized a documentation-only update after the Routines postmortem. Every future instance must review and follow [the mandatory efficiency procedure](WORK_LOG.md#2026-10-28--mandatory-efficiency-procedure-for-all-future-work) before implementation, as required upfront in AGENTS.md. It covers reuse, early candidate builds, isolated evidence-driven debugging, targeted checks before full CI, usage/context discipline and concise truthful handoff. Existing app0.10.0/build16, verified sourceaa78783, successful CI40733036079 and signed packages remain unchanged. Markdown-only change; no new app build/test run required. This documentation update is published with the owner's explicit Git-command authorization; future Desktop preference is retained. Exact next product task: execute the existing Saved Routines phone checklist in MANUAL_TESTS.md and report results before further feature scope.
