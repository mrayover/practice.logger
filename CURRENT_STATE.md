# Increment in progress — Daily Log and Weekly View

2026-10-29: authorized 0.13.0/build24 source implementation in progress from clean9c87acb. Room9 migration and JSON6 readers1–6 added; compilation, generated schema, native checks and permanent-key package pending. No new verified release claimed yet. Previous delivered build23 and evidence follow. Next task: compile focused Log tests, produce signed candidate, then complete release checks and update this record.

# Current state — practice.logger 0.12.0, build 23

Reviewed 2026-10-28 (America/Los_Angeles) against local and remote `main` snapshot `94eed943a3ed97beca0e68f10ba70fa60c2accba` (documentation; APK source remains `8b469dc3246bbe34841b0945089e553303308132`), app configuration, source, signed release manifest, APK checksum and GitHub Actions results. This file is the authoritative current delivery and verification record. Historical release facts are in [WORK_LOG.md](WORK_LOG.md).

## Delivery

| Item | Verified value |
|---|---|
| Release | **0.12.0 / versionCode 23** |
| APK source commit | `8b469dc3246bbe34841b0945089e553303308132` |
| Package ID | `com.mrayover.practicelogger.app` |
| Android | 8.0+ (minimum API 26; target/compile API 36) |
| Database | Room schema **8**; explicit migrations 1→8 retained |
| JSON backup | Writer **5**, readers **1–5**; excludes all recordings, take metadata and labels |
| APK SHA-256 | `e481ea500b331611c769829014b9b38ff0fcaf300736745a363b1d7c5d13cfcd` |
| Permanent certificate SHA-256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |

Current local delivery:

- [APK](dist/practice.logger-0.12.0-b23-8b469dc/practice.logger-0.12.0-b23-8b469dc.apk)
- [ZIP](dist/practice.logger-0.12.0-b23-8b469dc.zip)
- [Build provenance](dist/practice.logger-0.12.0-b23-8b469dc/BUILD_INFO.json)
- [Verification details](dist/practice.logger-0.12.0-b23-8b469dc/VERIFICATION_STATUS.txt)
- [Latest delivery pointer](dist/LATEST.txt)

The signed package and documentation commits have different SHAs: the APK was built from the source commit above; later commits update documentation only. The local checkout is `C:\Users\mrove\Projects\practice.logger`. Distribution files are intentionally ignored by Git. Install over the existing permanent-key app **without uninstalling**. The original temporary-key 0.1.0 app remains a separate installation; no automatic data transfer is provided.

## Implemented app

- Blocks: validated create/edit, literal case-insensitive search, Active/Archived filters, confirmed Archive/Restore, and **Delete Permanently** below Archive/Restore in a saved Block's editor.
- Safe deletion: unused active and archived Blocks can be deleted after confirmation. Persistent Goal associations, distinct saved Routines, attached takes and **all** draft/unfinished/completed session placements block deletion. The dialog gives real counts, explains completed-history protection and advises exporting wanted recordings. Inspection and confirmation-time recheck/deletion are transactional. No automatic detachment, take deletion or historical rewrite.
- Practice: persistent builder draft, independent unfinished sessions under Practice Now, ordered placements and duration overrides, paused recovery, foreground timer, Complete/Skip, explicit overtime, saved notes, summaries and Recent Practice under Routines.
- Saved Routines: create/edit/order/duration/delete, builder Save as Routine, independent starts using current Block snapshots. Archived or unavailable references prevent starting the Routine.
- Goals: up to five active Goals, manual progress, priority/top-two Home bars, completion/reopen, shared Block associations, Goal practice and actual explicitly attributed practice time.
- Audio: integrated practice click; multiple Block free takes and fixed-phrase loops with count-in and optional recording guide; playback/rename/confirmed take deletion/per-take export. Recording also has standalone free takes and Saved free recordings. Audio stops on exit/background; no automatic restart.
- Tools: standalone metronome, chromatic tuner, stopwatch/countdown, and protected JSON export/empty-app restore. Persistent green banner, cream `#F1E6D3`, green `#315C47`, bundled Lato and prominent Back to Home controls.

## Actual verification

| Check | Result and evidence |
|---|---|
| Final source build | Debug/release compile; **65 unit/Robolectric tests**, zero failures/errors/skips; lint zero errors, 10 existing warnings; compiler schema 8 unchanged. [Release CI](https://github.com/mrayover/practice.logger/actions/runs/40801285208), build job 123209739216. The eight focused deletion tests are included in the 65, not additional tests. |
| Android 8 / API 26 | **Complete final suite passed**: original catalog plus all 28 direct phases, including normal/200% deletion, cold restart, backup/picker/restore, engine/notes/history, recordings/loops/Tools/Routines/Goals and install-over upgrade to test code 24. Job 123211043224 in the release CI. Code 24 is a CI upgrade probe, not another delivered release. |
| Android 15 / API 35 full run | **Failed** after catalog and process-reopen passed: `PracticeWorkflowTest.kt:26` timed out waiting for `Add Warmup fixture`. Later phases were not reached. Job 123211043268. Cause remains unestablished; no full API 35 pass is claimed. |
| Android 15 isolated final-source sequence | All five unchanged catalog/process/practice/recovery/history classes **passed** in [diagnostic CI](https://github.com/mrayover/practice.logger/actions/runs/40802423099), job 123213388034. This does not turn the failed full run into a pass. |
| Focused native deletion | Normal Android 15 deletion/restart passed in [candidate CI](https://github.com/mrayover/practice.logger/actions/runs/40799694201). Deletion/cancellation/recreation/late-history guard/cold restart at 200% passed on [API 26](https://github.com/mrayover/practice.logger/actions/runs/40800682954) and [API 35](https://github.com/mrayover/practice.logger/actions/runs/40800685477), using candidate build 22 source `d1368b2`. Final build 23 changes only versionCode and test scrolling/fixture isolation. Final API 26 additionally passes these checks on build 23. |
| Package and visual checks | Permanent signature, pinned certificate, package/version, APK hash and ZIP integrity verified. Normal/enlarged deletion dialogs reviewed. All earlier release APK/ZIP files retained; 76 pre-increment files were verified byte-identical at release closeout. |

## Open limits and pending acceptance

- The final Android 15 full-run timeout remains unresolved even though the isolated sequence passes. An earlier Android 15 200% Routine fixture lookup failure from the visual-polish release is a separate unresolved verification limitation; that later phase was not reached in the final Android 15 run. Neither issue establishes a product defect or its cause.
- Physical-phone acceptance for this deletion release, detailed provider/TalkBack/audio checks, and Stage 3 backup/recovery acceptance remain pending. Earlier general owner reports that the app/metronome/recording/loops work are retained as general acceptance, not fabricated checklist results.
- Ten-session evaluation remains **0/10 documented real sessions**, deferred until the owner resumes it. No phone model, Android version or detailed checklist results have been supplied.
- JSON excludes audio. Export wanted takes individually. Full audio-inclusive backup/restore/import, cloud sync and broader storage redesign remain deferred.
- Abrupt process death/storage failure can lose the latest uncommitted timer interval or unsaved text. Recovery uses saved foreground time and resumes paused. Phone acoustic timing, tuner accuracy and loop boundary quality are not established by emulator tests.

## Exact next task

Use signed **0.12.0/build 23** over the existing installation and execute the deletion checks in [MANUAL_TESTS.md](MANUAL_TESTS.md), then report phone/Android/build and results. No additional feature is selected. See [BUILD_PLAN.md](BUILD_PLAN.md) for retained future boundaries.
