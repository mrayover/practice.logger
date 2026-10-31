# Current state — Home button removal visual preview,0.16.0/build42

Updated 2026-10-30 (America/Los_Angeles). **Signed, installable limited owner preview; full regression pending.** Build42 removed redundant Home buttons and their layout rows throughout the app. P0 audited the baseline and integrated the forward roadmap with documentation changes only. Existing Room11/JSON7 and notes behavior remain. No later audio acceptance workflow or unrelated feature started. No further automated work scheduled.

## Delivery

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code42 |
| Signed app source | `cafa1bfad09cc4bcf3539902f2ddb1c8d49153b5` |
| Limited preview | [41145916544](https://github.com/mrayover/practice.logger/actions/runs/41145916544) |
| Workflow status | completed / success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `ef739401ff70dc3880eda9d90fdd895396be21f1cfa832f732ead52c260b6daa` |
| ZIP SHA256 | `10376148ceea3dc22b5569a3b14b05e7a0e6676b63e8835e693938dc8bef5c72` |

[APK](dist/practice.logger-0.16.0-b42-cafa1bf/practice.logger-0.16.0-b42-cafa1bf.apk) · [ZIP](dist/practice.logger-0.16.0-b42-cafa1bf.zip) · [build info](dist/practice.logger-0.16.0-b42-cafa1bf/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b42-cafa1bf/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b42-cafa1bf/PHONE_CHECKLIST.txt) · [latest](dist/LATEST.txt).

Install over the permanent-key app without uninstalling/clearing data. Existing publisher verifies signature, pinned certificate, identity/version and APK hash. Final ZIP CRC/member audit passes; all 158 prior APK/ZIP files match their pre-work hashes. Packages remain local/ignored. Application code remains identical to signed source; schema11 is unchanged.

## Behavior

Removed the standalone Back to Home button and its layout row from Block Library, Practice Now/session builder, Routines, Recording, Goals, Tools, Backup and Log/Weekly View. Removed redundant Home actions from practice empty/summary screens too. Following content moves up with the existing padding/spacing. Library search scroll index is adjusted for its removed lazy item. The shared green banner and Android Back retain their existing navigation and save/discard barriers. Parent-specific Back to Tools/Goals and editor Cancel remain.

Build41 workspace/footer behavior remains: Description defaults open, one section stays expanded outside temporary transport panel suppression, Notes is a footer pill and Save for later is absent. Timestamped practice note history/editing/draft guards, PDF state restoration and audio ownership remain.

## Persistence and compatibility

Room11 adds only `practice_notes`: id, blockId, sessionId, placementId, createdAt and text, restrictive foreign keys and indices. Migration10→11 creates an empty table; all schema10 tables/rows stay intact. Read-through history combines original nonblank placement notes with new rows using separate internal keys. No backfill/copy or duplicate legacy entries. New/edited notes do not overwrite Block instructions/reference notes, overall session notes or daily Log.

JSON writer7/readers1–7 adds practiceNotes with strict ownership/nonblank/timestamp/ID validation and atomic empty-target restore. Versions1–6 restore legacy placement notes and no new rows. Legacy notes remain in placements for export, new entries in their own array; no duplication. Older app versions cannot import format7. Images/PDFs and all take media/metadata/labels remain excluded. Schemas/migrations1–10 retained; compiler schema11 is checked against all previous table definitions. Recording engine, timing/transition algorithms, image/PDF loader, branding and unrelated navigation remain unchanged. Focused Practice now uses an opt-in legacy-note preservation flag for Complete/Skip, preventing a stale screen copy from overwriting an edit.

## Exact verification

- Debug/release/test APK compilation passes.
- One clean API35 debug install/launch/live process/visible UI/no-crash check passes.
- HomeNavigationPreviewTest passes: visits Blocks, builder, Routines, Recording, Goals, Tools, Backup, Log and Weekly View; checks no Back to Home text, visible headings, banner return and Log edit save/reentry. Screenshots from all nine sections reviewed before signing.
- Permanent-key publisher verifies signature/certificate/package/version; final APK/ZIP member/CRC/hash audit and all 158 old package hashes pass. Application source matches signed source. Only six presentation files/versionCode changed in app main; DB/schema/storage/audio engines unchanged.

Not run: full units/lint/regression/API matrix, dedicated enlarged-text/landscape/TalkBack, editor/session guard campaign, audio or physical-phone install-over acceptance. Prior build41 notes/storage checks are historical evidence and were not rerun. Full release requirements remain unchanged. Stage3 phone acceptance pending; Stage4 remains0/10 real sessions.

## P0 baseline audit and V1 integration — 2026-10-30

P0 documentation integration is complete. Audited the existing Windows checkout on clean `main` at `e030fbf57adb88386b6cadb8af3d91b1eaf0c528`, confirmed by live `git ls-remote origin refs/heads/main`. Since starting reference `5ebbd58b8705b0fc80f95b2443b9db464db07a75`, the only commit was `e030fbf`: added the master plan and updated CURRENT_STATE, FIRST_WORK_HANDOFF and WORK_LOG. All tracked differences since signed source `cafa1bfad09cc4bcf3539902f2ddb1c8d49153b5` were Markdown only; app/configuration/schema/scripts/workflows were unchanged.

Live GitHub Actions listing confirms the latest run is still [41145916544](https://github.com/mrayover/practice.logger/actions/runs/41145916544), completed/success at the signed source. This is limited preview evidence, not full regression. Existing BUILD_INFO, LATEST and Gradle agree on version0.16.0/code42, source and app ID; current APK/ZIP hashes match the delivery table. The certificate is retained from the publisher's recorded verification; P0 did not rerun signing or physical-device checks. Earlier failed runs remain historical evidence and were not investigated.

README and BUILD_PLAN now link the [master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md). BUILD_PLAN/AGENTS distinguish future review, loop/meter/Settings, Library, storage/recovery and Goal/statistics phases from the implemented baseline. Existing data safeguards, historical delivery facts, JSON7 exclusions and phone limitations remain. The narrow future L1 test-reference exception does not permit P0 data removal. The previous P0 handoff is refreshed as a completion/next-assignment handoff.

Checks: documentation content and local links/anchors, baseline and final Git diffs/whitespace, master-plan/mandatory-procedure preservation and documentation-only changed-file scope. Automatic build CI is skipped via Markdown path exclusions and `[skip ci]`; owner-preview CI is manual-only. No compilation, APK build/signing, Gradle tests/lint, emulator launch, workflow dispatch or regression run for P0. P1–P4 retain scoped delivery checks; P5 and final V1 retain full-regression gates. Publication SHA/branch is confirmed in the closing handoff after push; this audit baseline is not the final commit SHA.

## One next task

Recommend assigning **P1 temporary audio take review** under master-plan §3: shared preview/save/re-record/discard before permanent take acceptance, with scoped audio/storage checks and versioned permanent-key signed delivery. P1 has not started and must not begin automatically. Build42 full regression and owner-phone acceptance remain pending; later P5/final gates are not waived.
