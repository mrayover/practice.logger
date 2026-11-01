# P5 regression handoff

Prepared 2026-11-01 (America/Los_Angeles). Run on **Medium**; use High selectively for difficult audio timing, concurrency, migration or data-preservation failures.

## Execution pointer

Automated execution is complete: full CI41329443572 passes all nine required jobs, 111 units and 168 native executions across API26/API35. Owner accepted signed 0.17.1/build67 on November 1, 2026: “Confirmed complete and running correctly”. P5 is closed; await a separate assignment. The owner authorized Git commit/push and maintenance is published. Follow [CURRENT_STATE](../CURRENT_STATE.md), [P5 report](P5_REGRESSION_REPORT.md) and [machine baseline](P5_REGRESSION_BASELINE.json) for current provenance, package and exact next action.

**The original assignment and starting snapshot below are historical.** Its uncommitted/no-build/build65/authorization statements describe preparation before execution; they are not current instructions to repeat completed work. Owner acceptance has satisfied the P5 exit; no Library phase has started.

## Assignment and exit gate

Perform **P5 — First full regression and pre-Library release gate**, defined in [the V1 master plan, section7](../PRACTICE_LOGGER_V1_MASTER_PLAN.md#7-p5--first-full-regression-and-pre-library-release-gate). Begin with the unfinished test preparation below. Freeze feature scope, fix demonstrated failures, complete the full automated checks, deliver a permanent-key signed pre-Library candidate and a short owner-phone checklist. Record actual evidence and obtain owner acceptance. **Stop at P5; do not start Media Library L1 or any later phase.** This handoff does not claim P5 has already run or passed.

## Working repository and baseline

- Reuse `C:\Users\mrove\Projects\practice.logger`; do not create a replacement repository. Synced ChatGPT project `sources/` files are read-only.
- Last verified local committed HEAD and GitHub main: `d0b8db0fc3e2472e9deccbe3a865a43da5f257d1`. Recheck both before work; preserve all existing changes.
- There is substantial **uncommitted maintenance**, including new scripts, a debug-only test manifest and review documentation. GitHub does not contain it yet. Review the complete tracked/untracked diff; do not reset, discard or assume it has been tested on GitHub.
- Current signed delivery is **0.17.0/build65**, app source `a6c095c02fc2363bcdf04cd2c4b168ce7f55569f`. Paths, hashes and prior scoped evidence belong to [CURRENT_STATE](../CURRENT_STATE.md). Existing packages remain preserved; app main source, Room11 schemas and signed packages were unchanged by the audits.
- No updated instrumentation compilation, Gradle test/lint, emulator run or full regression was performed during the audits. Local checks were static and mocked runner checks. No local Android SDK is configured at the checked standard locations; use the established GitHub CI workflow or verify a real build environment first.

Read AGENTS.md, README.md, CURRENT_STATE.md, BUILD_PLAN.md, WORK_LOG.md's mandatory efficiency procedure, the P5 master-plan section, [the two-pass review](PRE_REGRESSION_REVIEW.md), and [MANUAL_TESTS](../MANUAL_TESTS.md) before implementation. Confirm the efficiency procedure was reviewed.

## First: finish preparation and validate the maintenance

1. Trace older native tests through the current UI. Some still expect removed Quick note, Save Block note, in-practice Recordings, old reference routes and old playback descriptions. Some failure-injection tests still target placement-note writes although new notes use independent practice_notes rows. Representative files: FocusedPracticeTest, FocusedLayoutTest, PdfReferenceWorkflowTest, ImageReferenceWorkflowTest, RecordingWorkflowTest, PhraseWorkflowTest, AccessibilityWorkflowTest, PracticeWorkflowTest, MultipleSessionTest and RoutineWorkflowTest. Migrate each scenario by context; full recording management still exists outside Focused Practice. Preserve database, PCM, permission, lifecycle, visibility, geometry and failure-recovery assertions. Do not delete tests or weaken tolerances to obtain green results.
2. Review and compile the local fixes: writer7/future8 backup assertions; shared banner exit/Notes and compact zoom checks; legacy build27 seed ABI handling; generated MP3/M4A assets; API26–28 import fixture access confined to debug permissions; maximum-loop budgets; derived disposable upgrade codes; and the new primary coverage matrix. See the review for exact files and caveats. Guard against missing test assets and preserve ordered seed/cold-reopen fixtures.
3. Publish intended preparation changes through the authorized Git workflow before relying on GitHub. AGENTS.md makes GitHub Desktop the default; no explicit command-line commit/push authorization was supplied in this chat. Do not infer it from old WORK_LOG entries. A preparation commit with `[skip ci]` prevents an accidental full matrix before targeted checks. Verify the actual remote source SHA. Do not ask to rerun authorization already supplied directly by the owner later.
4. Compile debug/release/instrumentation and run the smallest affected unit/native cases first. Inspect logs, stack traces and screenshots for failures. Correct one evidenced cause at a time where practical. A source mismatch is not a demonstrated runtime app defect; a failed legacy test is not automatically flaky or pre-existing. Do not repeatedly dispatch the full matrix while guessing.

## Then: complete the full P5 regression

Run the complete unit suite, lint, debug/release and instrumentation compilation, and native gates on **API26 and API35**. The local `.github/workflows/android.yml` extends the original ordered device gates with `current_features` review, transport and appearance/import groups. Validate its wiring rather than assuming static checks prove execution. Diagnostic/build-only workflow successes do not count as a complete full-matrix pass. Preserve exact source provenance for every result.

Coverage must include:

- Blocks, archive/restore and dependency-protected deletion; Goals/Routines and navigation; independent sessions, timing/checkpoints, pause/complete/skip/overtime and history.
- New/legacy Notes, editing and failed-save/navigation barriers; dated Log/Weekly View and date-safe autosave.
- Free/recorded/imported takes, explicit temporary review/Save, successful-loop audition, cancellation/background/focus/noisy-route safety, management/export and byte preservation.
- Flexible loops/count-ins including maximum128-second capture/playback, shared metronome meter/accent/sound/flash, Settings/defaults and one-shot alarms.
- Light, Dark, System, Smooth, Sunny and Orchid; original image/PDF colors, page/zoom state and recording continuity.
- JSON7/readers1–7, atomic empty-target restore, migration/install-over and original media/data preservation; current JSON still excludes all takes/audio and reference metadata/bytes.
- Appropriate screenshots, accessibility, normal/200% text, keyboard, portrait/landscape and recreation/cold-reopen checks. Follow MANUAL_TESTS for detailed acceptance and physical-only limits.

After targeted fixes, run one coherent complete gate for the final candidate source. Record every failure, cancellation, skipped/omitted gate and limitation. Repeat the full gate only when subsequent changes or unresolved evidence justify it. Earlier Orchid/theme/import successes were scoped; recent green Android APK diagnostic runs had full jobs skipped.

## Preserve data and deliver

Keep app ID `com.mrayover.practicelogger.app`, permanent signing identity, stable IDs, explicit migrations, Room11/JSON7, saved history/audio/media and all earlier distributions. Never clear or uninstall the owner's app. Reset only disposable CI test installations. Do not start Library migration or discard attachments in P5.

For the pre-Library candidate, check the next available distributed versionCode, use the existing `scripts/Publish-LocalApk.ps1` and permanent key, and preserve every old APK/ZIP. Verify actual package/version, pinned certificate/signature, source/CI provenance, hashes, ZIP integrity and install-over behavior. Never commit packages, build outputs, SDKs, generated codec fixtures, local configuration, keys or passwords.

Provide a short physical-phone checklist covering upgrade/data retention, speaker/microphone, audio timing/loop edges, flash latency, appearance/navigation, document providers and foreground/background behavior. Emulator results cannot establish physical acceptance. Owner acceptance of the signed pre-Library candidate is the P5 exit gate; report any explicitly accepted phone-only limitations precisely.

Update CURRENT_STATE, WORK_LOG, this handoff as needed and related delivery records with actual outcomes. Report the final remote SHA, package paths, tests run/passed/failed, outstanding phone checks and one exact next action. Stop at P5; L1 requires a separate assignment.
