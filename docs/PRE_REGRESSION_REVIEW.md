# Pre-regression repository review

**Historical preparation audit:** its held/uncommitted/next-task statements describe the pre-P5 snapshot. P5 is now assigned and this maintenance is published; current execution and resolved/outstanding findings are in [P5 report](P5_REGRESSION_REPORT.md) and [CURRENT_STATE](../CURRENT_STATE.md).

## Second deep check — 2026-11-01

Owner requested an additional file/GitHub review, then requested wrap-up at5% remaining usage. Current GitHub main remains `d0b8db0fc3e2472e9deccbe3a865a43da5f257d1`; local maintenance is uncommitted. Read-only GitHub branch/run/issue snapshots, commit metadata and job logs were checked. The repository has no open issues/PRs in the returned snapshot. Recent successful Android APK runs [41007781246](https://github.com/mrayover/practice.logger/actions/runs/41007781246) and [41007778661](https://github.com/mrayover/practice.logger/actions/runs/41007778661) were diagnostic-only with build/device jobs skipped. They establish no current full regression pass. Orchid41281008402 remains scoped evidence. Imported-audio41269123574 logs confirm two import cases passed before a separate geometry assertion failed; its overall result remains failure, as previously recorded.

Further confirmed preparation issues corrected locally:

- Goal, Log and Routine backup unit tests expected writer6 despite the current writer7; the Routine future-format rejection case incorrectly used supported7. Expectations now require7 and reject8, with the safety assertions retained.
- The added primary matrix compiled import tests without generating their MP3/M4A assets. A shared repeatable Prepare-AudioFixtures.sh now supplies both the primary and existing scoped workflow; targeted import diagnostics also prepare them. The appearance/import runner checks packaged asset entries before launching instrumentation. Generated codec files are ignored individually.
- ImportedAudioWorkflowTest used API29 Downloads on the API26 matrix. Its fixture helper now guards that API and uses the legacy external Files collection/Downloads location on26–28. Legacy read/write permissions are confined to the debug manifest with maxSdk28; the main/release manifest is unchanged. Actual API26 execution remains pending.
- The128-second capture-plus-full-loop playback case cannot fit a120-second per-test budget. Current-feature and targeted diagnostic runners now allow360seconds for that case; the matrix also allows420seconds at the host boundary.
- Shared practice leave/Notes helpers and two direct exit calls now use the current banner/Notes controls. Eight compact zoom expectations in meter/Notes/refinement scenarios use a shared assertion checking actual retained zoom and accessible Reset zoom, also reused by the theme case. Existing full-size viewer checks remain. Other legacy scenario migrations remain unfinished.

Validation after these changes: all13 workflow documents parse, all16 shell scripts plus gradlew pass Bash syntax, class/method/count references agree, both API selections in each feature group pass mocked runner checks, long-loop timeout arguments are checked, missing M4A assets are rejected before device launch, fixture generation is repeatable under a codec stub, and seed/reopen ordering retains data. JSON/XML/local Markdown and Git whitespace checks pass. Git object integrity (`git fsck --no-dangling`) passes. All200 current tracked/intended-new files were scanned for case collisions, UTF-8/conflict damage and schema/version consistency. The946 internal checks include links/files/hashes and mocks, not Android test results. All278 prior APK/ZIP hashes, main-source Git contents and11 schema bytes remain unchanged.

**Readiness is still held.** Older native scenarios retain positive expectations for removed Quick note/Save Block note/in-practice management/reference controls, and some note-failure tests inject errors into the old placement-note write path. These require context-aware migration with their original assertions. Broad search results also include generated tags and negative absence assertions; those were not reported as proven failures. No SDK is configured locally, no updated instrumentation was compiled, and no Gradle/emulator/full regression, publication or new signed APK was started. Wrap-up preserves all unfinished changes for review. Next action remains targeted legacy-scenario alignment and compilation before the assigned complete P5 regression.



Reviewed 2026-11-01 (America/Los_Angeles). **Normalization completed; current full-regression readiness is not certified.** This was a repository/provenance/static review and runner maintenance, not a full runtime correctness proof. Legacy native UI expectations still require targeted alignment and execution. Do not weaken or remove their preservation/permission/PCM/accessibility assertions to obtain a green run.

## Baseline and review scope

The existing Windows checkout was clean on `main` at `d0b8db0fc3e2472e9deccbe3a865a43da5f257d1`, matching actual `origin/main` from `git ls-remote`. Its last app source is `a6c095c02fc2363bcdf04cd2c4b168ce7f55569f`; subsequent committed changes are four documentation files only. No replacement checkout was created. Synced project sources were not edited.

All 195 baseline tracked files were inventoried against Git; the per-file list below covers app source/resources, unit/native tests and fixtures, schemas, build configuration, workflows, scripts and operational/historical records. JSON/XML parsing, local Markdown targets, text encoding/conflict markers/line endings, workflow and script references, test counts and release provenance were checked. Focused semantic inspection covered the Room migration chain/deletion dependencies, JSON exclusions/atomic empty-target restore, private recording/import save and rollback/recovery, selected appearance/native styles, foreground capture/playback ownership and regression entry paths. This does not claim every statement in every source file was formally proven correct.

| Baseline category | Files |
|---|---:|
| Application source/resources | 53 |
| Native tests/helpers/PDF fixtures | 47 |
| Unit tests | 27 |
| Room schema exports | 11 |
| Workflows | 13 |
| Scripts/certificate pin | 15 |
| Markdown records | 19 |
| Build and repository configuration | 10 |

## Corrections made

1. The primary Android workflow previously installed current build65 and then attempted hard-coded45/46 updates. It now derives disposable upgrade versions from the current Gradle version (65→66→67). These are CI-only packages; the signed delivery remains65. Schema logging now selects Room11.
2. The legacy reference upgrade gate depended on a build27 artifact from run40915328499 with finite retention. Full and targeted paths now rebuild immutable build27 source `3911c15136f97fd5fbdc827cdd82c49f34d60e46`; the full gate shares that run's baseline artifact. The existing signing preparation retains one disposable CI identity. The seed's PhraseSpec construction uses build27's actual two-Int JVM constructor; a current default-count-in constructor would not exist inside that old app. Real execution of this corrected upgrade path is still pending.
3. ThemeWorkflowTest now checks the shared-header gear and Home Library instead of the removed Home Settings-below-Log layout. Compact PDF zoom checks use the existing accessible Reset zoom action and zoom state. The theme runner expects six theme cases, or twelve with the existing API35 control/audio cases, and has a bounded420-second run with120-second per-test timeouts. Assertions on native palette, persistence, audio and original media remain.
4. The primary workflow historically omitted later P1–P4/current-feature suites. An additional isolated API26/API35 matrix now invokes review/flexible/max-loop, meter/output/Settings/pulse/loop/Notes/navigation/transport, all six theme cases and ordered imported-audio save/cold-reopen cases. Existing legacy gates remain. It reuses the current run's compiled app and test APKs, resets only disposable CI app state between independent fixtures and preserves state within seed/reopen pairs. New jobs are declarations, not passing evidence; they have not been dispatched.
5. README, BUILD_PLAN, backup documentation, current handoff/checklist and historical wrappers now distinguish current65 behavior from build23/42/53-era records. The current manual checklist covers all appearance choices, header navigation, explicit note/review saves, permanent loop click, imported audio and backup exclusions. Removed obsolete optional-guide, Home Settings, Quick note and format6 instructions from the active checklist. Historical release facts and the mandatory efficiency procedure remain in WORK_LOG/Git history.
6. Shell runners and gradlew explicitly use LF, Windows batch uses CRLF, and fonts/JARs/PDFs are binary. One mixed-line-ending unit file and a missing final newline in the navigation helper were normalized. Room compiler snapshots retain their original bytes, including generator-owned final-newline choices. Existing Git-normalized application source has no logic changes.

## Remaining regression risks

**Legacy selectors are a known test-maintenance blocker, not a demonstrated app failure.** The primary legacy tests still refer to removed `Save for later`, `Quick note (optional)`, `Save Block note`, and the old in-practice `Recordings`/`Show reference` routes. Representative entry points are `FocusedPracticeTest.kt` (including shared leave/notes helpers), `FocusedLayoutTest.kt`, `PdfReferenceWorkflowTest.kt`, `ImageReferenceWorkflowTest.kt`, `RecordingWorkflowTest.kt`, `PhraseWorkflowTest.kt` and `AccessibilityWorkflowTest.kt`; other session/routine callers inherit the helper behavior. Current UI uses header exit, independent timestamped Notes, retained workspace headers and compact inline capture/review. Full management still exists in saved Block editors/main Recording, so a blanket replacement of every Recordings selector would be wrong. Align these scenarios to their intended current context and retain all original data/audio/accessibility assertions. No failing runtime result was fabricated, and these tests were not disabled or declared pre-existing app defects.

The new complete coverage matrix and two edited instrumentation classes have not been compiled/executed in this pass. Local tooling supplies signing inspection but no configured full Android SDK/Gradle build environment. Use focused CI compilation/diagnostics first, then the assigned complete P5 gate. Previously scoped successes on source65 do not validate this new test/runner source. No new APK or version increment is needed for this source-unchanged maintenance.

The existing primary device job retains ordered legacy seed/reopen/install-over scenarios. Its runtime budget and actual emulator/provider behavior still need observation during the assigned regression; syntax/mocked-device checks cannot establish runtime success. Physical System/appearance/accessibility/acoustic/provider acceptance remains pending, and Stage4 remains0/10 documented sessions.

## Release integrity and available evidence

The signed build65 APK has package `com.mrayover.practicelogger.app`, version0.17.0/code65 and target36. Local apksigner verifies v2/v3 and certificate `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`, equal to the repository pin and BUILD_INFO. APK SHA256 `4d871a43296e8fb2edbdf5e81915d3e2124ef775427e7bd69641efa7a81a5f7b`; ZIP SHA256 `1eebdbcdbfc1f925e57a4598af2d2ffc4901be2bc3252fe7f774064ec8fae60f`. ZIP CRC, unique/exact members and every directory/member byte match. All278 existing APK/ZIP files are preserved byte-for-byte. No signing key/password content was printed or changed.

The GitHub connector confirms the targeted job and all its steps in [Orchid CI41281008402](https://github.com/mrayover/practice.logger/actions/runs/41281008402) completed successfully. That is prior scoped source65 evidence, not a pass for this maintenance. Current package provenance and phone limitations remain in [CURRENT_STATE](../CURRENT_STATE.md). dist/LATEST continues to identify65; no historical package or packaged verification file was rewritten.

Local validation: all13 workflows parsed with duplicate-key checks; every shell runner and gradlew passed Bash syntax; JSON/XML/local Markdown checks passed; real shell branches were exercised against stubbed device output for valid6/12 and rejected obsolete3/9 theme counts; disposable upgrade calculation produced66/67; baseline copy preserved exact mock bytes. The additional current-feature matrix has class/method/count and ordering checks. These are static/mocked runner checks, not Android tests. All app main-source Git contents and all11 schema bytes are unchanged. Git whitespace checks pass. Exact local validation is recorded in this session's WORK_LOG entry.

## Publication and next action

Changes are prepared locally for review. This chat has not supplied explicit Git-command commit/push authorization; AGENTS.md names GitHub Desktop as the default mutation workflow. No commit, push, workflow dispatch or new signed build is claimed. The verified remote baseline is the SHA above. Use a maintenance commit with `[skip ci]` if publishing before the separately assigned major regression, so publication does not unexpectedly launch the full workflow.

Next engineering action: align the enumerated legacy UI scenarios to current65 controls, compile the instrumentation and run targeted affected cases before launching the assigned complete P5 pre-Library regression. Carry the current signed build65 phone checklist forward; no later feature phase starts from this review.

## Baseline per-file inventory

“Git content retained” includes portable checkout line-ending changes where applicable. “Reviewed/updated” denotes an intentional Git content edit; new maintenance/report files are listed after the baseline inventory. This table is an integrity/scope inventory, not a claim of runtime acceptance per file.

| File | Review disposition |
|---|---|
| [.gitattributes](../.gitattributes) | Reviewed/updated |
| [.github/workflows/android.yml](../.github/workflows/android.yml) | Reviewed/updated |
| [.github/workflows/audio-import.yml](../.github/workflows/audio-import.yml) | Git content retained |
| [.github/workflows/orchid-theme.yml](../.github/workflows/orchid-theme.yml) | Git content retained |
| [.github/workflows/owner-preview.yml](../.github/workflows/owner-preview.yml) | Git content retained |
| [.github/workflows/p1-review.yml](../.github/workflows/p1-review.yml) | Git content retained |
| [.github/workflows/p2-phrase.yml](../.github/workflows/p2-phrase.yml) | Git content retained |
| [.github/workflows/p2-playback26.yml](../.github/workflows/p2-playback26.yml) | Git content retained |
| [.github/workflows/p3-meter.yml](../.github/workflows/p3-meter.yml) | Git content retained |
| [.github/workflows/p4-settings.yml](../.github/workflows/p4-settings.yml) | Git content retained |
| [.github/workflows/p4-targeted.yml](../.github/workflows/p4-targeted.yml) | Git content retained |
| [.github/workflows/smooth-theme.yml](../.github/workflows/smooth-theme.yml) | Git content retained |
| [.github/workflows/sunny-theme.yml](../.github/workflows/sunny-theme.yml) | Git content retained |
| [.github/workflows/theme.yml](../.github/workflows/theme.yml) | Git content retained |
| [.gitignore](../.gitignore) | Reviewed/updated |
| [AGENTS.md](../AGENTS.md) | Git content retained |
| [BACKUP_FORMAT.md](../BACKUP_FORMAT.md) | Reviewed/updated |
| [BUILD_PLAN.md](../BUILD_PLAN.md) | Reviewed/updated |
| [CURRENT_STATE.md](../CURRENT_STATE.md) | Git content retained |
| [EVALUATION.md](../EVALUATION.md) | Git content retained |
| [FIRST_WORK_HANDOFF.md](../FIRST_WORK_HANDOFF.md) | Git content retained |
| [MANUAL_TESTS.md](../MANUAL_TESTS.md) | Reviewed/updated |
| [PRACTICE_LOGGER_BUILD_PLAN.md](../PRACTICE_LOGGER_BUILD_PLAN.md) | Reviewed/updated |
| [PRACTICE_LOGGER_FIRST_WORK_HANDOFF.md](../PRACTICE_LOGGER_FIRST_WORK_HANDOFF.md) | Reviewed/updated |
| [PRACTICE_LOGGER_V1_MASTER_PLAN.md](../PRACTICE_LOGGER_V1_MASTER_PLAN.md) | Git content retained |
| [README.md](../README.md) | Reviewed/updated |
| [THIRD_PARTY_NOTICES.md](../THIRD_PARTY_NOTICES.md) | Git content retained |
| [WORK_LOG.md](../WORK_LOG.md) | Git content retained |
| [app/build.gradle.kts](../app/build.gradle.kts) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/1.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/1.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/10.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/10.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/11.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/11.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/2.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/2.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/3.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/3.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/4.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/4.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/5.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/5.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/6.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/6.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/7.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/7.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/8.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/8.json) | Git content retained |
| [app/schemas/com.mrayover.practicelogger.BlockDatabase/9.json](../app/schemas/com.mrayover.practicelogger.BlockDatabase/9.json) | Git content retained |
| [app/src/androidTest/assets/references/corrupt.pdf](../app/src/androidTest/assets/references/corrupt.pdf) | Git content retained |
| [app/src/androidTest/assets/references/locked.pdf](../app/src/androidTest/assets/references/locked.pdf) | Git content retained |
| [app/src/androidTest/assets/references/owner-locked.pdf](../app/src/androidTest/assets/references/owner-locked.pdf) | Git content retained |
| [app/src/androidTest/assets/references/score.pdf](../app/src/androidTest/assets/references/score.pdf) | Git content retained |
| [app/src/androidTest/assets/references/single.pdf](../app/src/androidTest/assets/references/single.pdf) | Git content retained |
| [app/src/androidTest/assets/references/too-many.pdf](../app/src/androidTest/assets/references/too-many.pdf) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/AccessibilityWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/AccessibilityWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/AudioSettingsWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/AudioSettingsWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/BackupWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/BackupWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/BlockDeletionWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/BlockDeletionWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/FlexiblePhraseWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/FlexiblePhraseWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/FocusedLayoutTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/FocusedLayoutTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/FocusedPracticeTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/FocusedPracticeTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/FreeRecordingWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/FreeRecordingWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/GoalAccessibilityTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/GoalAccessibilityTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/GoalWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/GoalWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/HomeNavigation.kt](../app/src/androidTest/java/com/mrayover/practicelogger/HomeNavigation.kt) | Reviewed/updated |
| [app/src/androidTest/java/com/mrayover/practicelogger/HomeNavigationPreviewTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/HomeNavigationPreviewTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/ImageReferenceWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/ImageReferenceWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/ImportedAudioWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/ImportedAudioWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/LaunchTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/LaunchTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/LogWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/LogWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/LongPhrasePlaybackTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/LongPhrasePlaybackTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/LoopRecorderPreviewTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/LoopRecorderPreviewTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/MeterWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/MeterWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/MetronomeButtonPulseTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/MetronomeButtonPulseTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/MetronomeOutputTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/MetronomeOutputTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/MetronomeWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/MetronomeWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/MultipleSessionTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/MultipleSessionTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/NestedReferenceDiagnosticTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/NestedReferenceDiagnosticTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/NotesHistoryPreviewTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/NotesHistoryPreviewTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/PdfReferenceStorageTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/PdfReferenceStorageTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/PdfReferenceWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/PdfReferenceWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/PhraseWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/PhraseWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/PracticeRefinementPreviewTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/PracticeRefinementPreviewTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/PracticeWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/PracticeWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/ProcessReopenTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/ProcessReopenTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/RecordingWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/RecordingWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/ReferenceUpgradeTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/ReferenceUpgradeTest.kt) | Reviewed/updated |
| [app/src/androidTest/java/com/mrayover/practicelogger/RoutineWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/RoutineWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/SystemPickerTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/SystemPickerTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/TakeReviewTestSupport.kt](../app/src/androidTest/java/com/mrayover/practicelogger/TakeReviewTestSupport.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/TakeReviewWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/TakeReviewWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/TargetedP4PreviewTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/TargetedP4PreviewTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/ThemeWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/ThemeWorkflowTest.kt) | Reviewed/updated |
| [app/src/androidTest/java/com/mrayover/practicelogger/ToolsWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/ToolsWorkflowTest.kt) | Git content retained |
| [app/src/androidTest/java/com/mrayover/practicelogger/TransportPreviewTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/TransportPreviewTest.kt) | Git content retained |
| [app/src/main/AndroidManifest.xml](../app/src/main/AndroidManifest.xml) | Git content retained |
| [app/src/main/assets/Lato-OFL.txt](../app/src/main/assets/Lato-OFL.txt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/AudioImport.kt](../app/src/main/java/com/mrayover/practicelogger/AudioImport.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/AudioPreferences.kt](../app/src/main/java/com/mrayover/practicelogger/AudioPreferences.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/BackupData.kt](../app/src/main/java/com/mrayover/practicelogger/BackupData.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/BackupScreen.kt](../app/src/main/java/com/mrayover/practicelogger/BackupScreen.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/BlockDatabase.kt](../app/src/main/java/com/mrayover/practicelogger/BlockDatabase.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/BlockRepository.kt](../app/src/main/java/com/mrayover/practicelogger/BlockRepository.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/CatalogViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/CatalogViewModel.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/CountInSettings.kt](../app/src/main/java/com/mrayover/practicelogger/CountInSettings.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/GoalData.kt](../app/src/main/java/com/mrayover/practicelogger/GoalData.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/GoalScreens.kt](../app/src/main/java/com/mrayover/practicelogger/GoalScreens.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/GoalViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/GoalViewModel.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/HomeScreens.kt](../app/src/main/java/com/mrayover/practicelogger/HomeScreens.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/ImageReferenceData.kt](../app/src/main/java/com/mrayover/practicelogger/ImageReferenceData.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/ImageReferenceView.kt](../app/src/main/java/com/mrayover/practicelogger/ImageReferenceView.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/ImageReferenceViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/ImageReferenceViewModel.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/LogData.kt](../app/src/main/java/com/mrayover/practicelogger/LogData.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/LogScreen.kt](../app/src/main/java/com/mrayover/practicelogger/LogScreen.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/LogViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/LogViewModel.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/MainActivity.kt](../app/src/main/java/com/mrayover/practicelogger/MainActivity.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/MetronomeAudio.kt](../app/src/main/java/com/mrayover/practicelogger/MetronomeAudio.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/MetronomeControls.kt](../app/src/main/java/com/mrayover/practicelogger/MetronomeControls.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/PhraseAudio.kt](../app/src/main/java/com/mrayover/practicelogger/PhraseAudio.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/PitchDetector.kt](../app/src/main/java/com/mrayover/practicelogger/PitchDetector.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/PracticeNotes.kt](../app/src/main/java/com/mrayover/practicelogger/PracticeNotes.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/PracticePanels.kt](../app/src/main/java/com/mrayover/practicelogger/PracticePanels.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/PracticeScreens.kt](../app/src/main/java/com/mrayover/practicelogger/PracticeScreens.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/PracticeViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/PracticeViewModel.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/RecordingData.kt](../app/src/main/java/com/mrayover/practicelogger/RecordingData.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/RecordingSheet.kt](../app/src/main/java/com/mrayover/practicelogger/RecordingSheet.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/RecordingViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/RecordingViewModel.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/RoutineData.kt](../app/src/main/java/com/mrayover/practicelogger/RoutineData.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/RoutineScreens.kt](../app/src/main/java/com/mrayover/practicelogger/RoutineScreens.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/RoutineViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/RoutineViewModel.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/SelectionPills.kt](../app/src/main/java/com/mrayover/practicelogger/SelectionPills.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/SessionData.kt](../app/src/main/java/com/mrayover/practicelogger/SessionData.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/Theme.kt](../app/src/main/java/com/mrayover/practicelogger/Theme.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/ThemePreferences.kt](../app/src/main/java/com/mrayover/practicelogger/ThemePreferences.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/ToolClock.kt](../app/src/main/java/com/mrayover/practicelogger/ToolClock.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/ToolScreens.kt](../app/src/main/java/com/mrayover/practicelogger/ToolScreens.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/ToolsViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/ToolsViewModel.kt) | Git content retained |
| [app/src/main/java/com/mrayover/practicelogger/TunerAudio.kt](../app/src/main/java/com/mrayover/practicelogger/TunerAudio.kt) | Git content retained |
| [app/src/main/res/drawable/ic_launcher_foreground.xml](../app/src/main/res/drawable/ic_launcher_foreground.xml) | Git content retained |
| [app/src/main/res/font/lato.xml](../app/src/main/res/font/lato.xml) | Git content retained |
| [app/src/main/res/font/lato_bold.ttf](../app/src/main/res/font/lato_bold.ttf) | Git content retained |
| [app/src/main/res/font/lato_regular.ttf](../app/src/main/res/font/lato_regular.ttf) | Git content retained |
| [app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml](../app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml) | Git content retained |
| [app/src/main/res/values-night/styles.xml](../app/src/main/res/values-night/styles.xml) | Git content retained |
| [app/src/main/res/values/colors.xml](../app/src/main/res/values/colors.xml) | Git content retained |
| [app/src/main/res/values/styles.xml](../app/src/main/res/values/styles.xml) | Git content retained |
| [app/src/main/res/xml/backup_rules.xml](../app/src/main/res/xml/backup_rules.xml) | Git content retained |
| [app/src/main/res/xml/data_extraction_rules.xml](../app/src/main/res/xml/data_extraction_rules.xml) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/AudioImportTest.kt](../app/src/test/java/com/mrayover/practicelogger/AudioImportTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/AudioSettingsPcmTest.kt](../app/src/test/java/com/mrayover/practicelogger/AudioSettingsPcmTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/BackupFailureTest.kt](../app/src/test/java/com/mrayover/practicelogger/BackupFailureTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/BackupRepositoryTest.kt](../app/src/test/java/com/mrayover/practicelogger/BackupRepositoryTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/BlockDeletionTest.kt](../app/src/test/java/com/mrayover/practicelogger/BlockDeletionTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/BlockRepositoryTest.kt](../app/src/test/java/com/mrayover/practicelogger/BlockRepositoryTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/CompletionPcmTest.kt](../app/src/test/java/com/mrayover/practicelogger/CompletionPcmTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/FlexiblePhraseStorageTest.kt](../app/src/test/java/com/mrayover/practicelogger/FlexiblePhraseStorageTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/FoundationTest.kt](../app/src/test/java/com/mrayover/practicelogger/FoundationTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/GoalRepositoryTest.kt](../app/src/test/java/com/mrayover/practicelogger/GoalRepositoryTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/ImageMigrationTest.kt](../app/src/test/java/com/mrayover/practicelogger/ImageMigrationTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/ImageReferenceTest.kt](../app/src/test/java/com/mrayover/practicelogger/ImageReferenceTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/LogRepositoryTest.kt](../app/src/test/java/com/mrayover/practicelogger/LogRepositoryTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/MeterPcmTest.kt](../app/src/test/java/com/mrayover/practicelogger/MeterPcmTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/MetronomeTest.kt](../app/src/test/java/com/mrayover/practicelogger/MetronomeTest.kt) | Reviewed/updated |
| [app/src/test/java/com/mrayover/practicelogger/PhraseAudioTest.kt](../app/src/test/java/com/mrayover/practicelogger/PhraseAudioTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/PhraseRepositoryTest.kt](../app/src/test/java/com/mrayover/practicelogger/PhraseRepositoryTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/PracticeNoteRepositoryTest.kt](../app/src/test/java/com/mrayover/practicelogger/PracticeNoteRepositoryTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/RecordingRepositoryTest.kt](../app/src/test/java/com/mrayover/practicelogger/RecordingRepositoryTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/RoutineBackupTest.kt](../app/src/test/java/com/mrayover/practicelogger/RoutineBackupTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/RoutineRepositoryTest.kt](../app/src/test/java/com/mrayover/practicelogger/RoutineRepositoryTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/SessionRepositoryTest.kt](../app/src/test/java/com/mrayover/practicelogger/SessionRepositoryTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/StandaloneRecordingTest.kt](../app/src/test/java/com/mrayover/practicelogger/StandaloneRecordingTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/TakeReviewTest.kt](../app/src/test/java/com/mrayover/practicelogger/TakeReviewTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/ThemePaletteTest.kt](../app/src/test/java/com/mrayover/practicelogger/ThemePaletteTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/ThemePreferencesTest.kt](../app/src/test/java/com/mrayover/practicelogger/ThemePreferencesTest.kt) | Git content retained |
| [app/src/test/java/com/mrayover/practicelogger/ToolLogicTest.kt](../app/src/test/java/com/mrayover/practicelogger/ToolLogicTest.kt) | Git content retained |
| [build.gradle.kts](../build.gradle.kts) | Git content retained |
| [docs/IMPORTED_AUDIO.md](../docs/IMPORTED_AUDIO.md) | Git content retained |
| [docs/ORCHID_THEME.md](../docs/ORCHID_THEME.md) | Git content retained |
| [docs/P4_TARGETED_UI_DELIVERY.md](../docs/P4_TARGETED_UI_DELIVERY.md) | Reviewed/updated |
| [docs/P4_THEME_READINESS.md](../docs/P4_THEME_READINESS.md) | Reviewed/updated |
| [docs/SMOOTH_THEME.md](../docs/SMOOTH_THEME.md) | Git content retained |
| [docs/SUNNY_THEME.md](../docs/SUNNY_THEME.md) | Git content retained |
| [gradle.properties](../gradle.properties) | Git content retained |
| [gradle/wrapper/gradle-wrapper.jar](../gradle/wrapper/gradle-wrapper.jar) | Git content retained |
| [gradle/wrapper/gradle-wrapper.properties](../gradle/wrapper/gradle-wrapper.properties) | Git content retained |
| [gradlew](../gradlew) | Git content retained |
| [gradlew.bat](../gradlew.bat) | Git content retained |
| [scripts/Check-AndroidClass.sh](../scripts/Check-AndroidClass.sh) | Git content retained |
| [scripts/Check-AudioImport.sh](../scripts/Check-AudioImport.sh) | Git content retained |
| [scripts/Check-AudioSettings.sh](../scripts/Check-AudioSettings.sh) | Git content retained |
| [scripts/Check-FlexiblePhrase.sh](../scripts/Check-FlexiblePhrase.sh) | Git content retained |
| [scripts/Check-Meter.sh](../scripts/Check-Meter.sh) | Git content retained |
| [scripts/Check-OrchidTheme.sh](../scripts/Check-OrchidTheme.sh) | Git content retained |
| [scripts/Check-P4Targeted.sh](../scripts/Check-P4Targeted.sh) | Git content retained |
| [scripts/Check-PreviewLaunch.sh](../scripts/Check-PreviewLaunch.sh) | Git content retained |
| [scripts/Check-SmoothTheme.sh](../scripts/Check-SmoothTheme.sh) | Git content retained |
| [scripts/Check-SunnyTheme.sh](../scripts/Check-SunnyTheme.sh) | Git content retained |
| [scripts/Check-TakeReview.sh](../scripts/Check-TakeReview.sh) | Git content retained |
| [scripts/Check-Themes.sh](../scripts/Check-Themes.sh) | Reviewed/updated |
| [scripts/Prepare-ReferenceUpgrade.sh](../scripts/Prepare-ReferenceUpgrade.sh) | Git content retained |
| [scripts/Publish-LocalApk.ps1](../scripts/Publish-LocalApk.ps1) | Git content retained |
| [scripts/signing-certificate.sha256](../scripts/signing-certificate.sha256) | Git content retained |
| [settings.gradle.kts](../settings.gradle.kts) | Git content retained |

New files: `scripts/Build-ReferenceBaseline.sh`, `scripts/Check-CurrentFeatures.sh` and this review.
