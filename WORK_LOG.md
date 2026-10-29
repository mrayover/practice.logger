# Work log — consolidated release and session record

Updated 2026-10-28 (America/Los_Angeles), at the owner's explicit request to clean all logs, remove obsolete paths and resolve conflicting current-state claims. This consolidation supersedes the former append-only rule for this cleanup. The full prior records remain recoverable in Git at `84565ea6898e60852538ebcb4997e7c6b647547f`. Historical entries below are past delivery facts, not current next tasks. Current package paths/source/hash/CI and pending work live in [CURRENT_STATE.md](CURRENT_STATE.md).

## Release history

| Release/build | Delivered capability and historical verification |
|---|---|
| 0.1.0 /2 | Foundation then persistent Block catalog; automated checks passed. Temporary CI signing key later unavailable; this remains a separate legacy installation. |
| 0.2.0 /3 | Persistent session builder/practice/timer/history; Room 1→2; stable permanent-key package established. Automated checks passed. |
| 0.3.0 /4 | Versioned JSON export and protected empty-app restore; automated checks passed. Owner generally reported the app works. |
| 0.3.1 /5 | Backup/picker/recovery/accessibility hardening, shared app-owned database, explicit platform backup exclusions and launcher icon. API 26/35 engineering checks passed; detailed phone acceptance remains pending. |
| 0.3.2 /6 | Explicit/save-before-leave unfinished Block notes, no schema change; automated checks passed. Owner deferred phone acceptance/evaluation until ready. |
| 0.3.3 /7 | Independent unfinished sessions and always-available new practice; JSON 2; automated checks passed. |
| 0.4.0 /8 | Integrated sample-clock click, actual used-BPM history; Room 3/JSON 3. Automated checks passed; owner generally accepted the metronome. |
| 0.5.0 /9 | Free Block M4A takes, playback/rename/delete/export/lifecycle; Room 4. Automated API 26/35 checks passed; owner generally accepted recording. JSON continued excluding all audio/take data. |
| 0.6.0 /11 | Fixed-phrase WAV loops/count-in; Room 5. API 26/35 automated checks passed; owner generally accepted loops. Earlier build 10 remained a candidate after visual review. Detailed phone onset/boundary/Done-dismissal results were not established. |
| 0.6.1 /12 | Default-on optional click during phrase capture; automated API 26/35 checks passed. No digitally mixed click or audio backup change. |
| 0.7.0 /13 | Owner sketch Home layout; automated checks passed. Blank/inactive Goal placeholders were approved for this historical visual increment only; current Goals are functional. |
| 0.8.0 /14 | Persistent banner, unfinished sessions under Practice Now, standalone metronome/tuner/timers; automated API 26/35 checks passed. |
| 0.9.0 /15 | Shared cream and standalone free takes; Room 6 nullable take Block ownership. API 26/35 automated checks passed. Current darker cream was delivered later. |
| 0.10.0 /16 | Saved Routines/builder save/fresh starts; Room 7/JSON 4. 51 unit tests and full API 26/35 suites passed. Catalog/keyboard test detours prompted the mandatory efficiency procedure retained below. |
| 0.11.0 /20 | Goals/shared Blocks/explicit practice attribution; Room 8/JSON 5. 57 unit tests/build/lint and focused native/200% Goal checks passed. Final full device matrix was stopped at owner request: “The app works. Stop testing and finalize this output.” Builds 17–19 remain candidates, not separate current releases. |
| 0.11.1 /21 | Cream#F1E6D3, thin Home Goal bars, prominent Back to Home. 57 unit tests/build/lint and full API 26 passed. API 35 passed catalog+24 direct phases, then failed the legacy 200% Routine fixture lookup; isolated check also failed. No full API 35 pass claimed. |
| 0.12.0 /23 | Safe permanent Block deletion; schema 8/JSON 5 unchanged. 65 unit tests/build/lint and final API 26 catalog+28 direct phases passed. Final API 35 full run failed a practice-fixture lookup; the unchanged five-class sequence passed isolated. Focused deletion/restart/200% both-API checks passed with source-specific provenance in CURRENT_STATE.md. Build 22 remains an early signed candidate; final delivery is 23. |

## Durable decisions and limits

- Owner generally accepted earlier app/metronome/recording/loop releases. Phone model/Android and individual checklist results were not supplied; do not infer detailed acceptance. Stage 3 detailed phone acceptance is pending and Stage 4 remains 0/10 documented real sessions. Later owner-approved increments did not complete those gates.
- Stable app ID and permanent certificate preserve in-place updates; keep all prior packages and signing material. The old temporary-key app is separate. All 76 pre-deletion APK/ZIP files were rehashed unchanged at release closeout; the documentation cleanup additionally checks all current package files.
- Historical snapshots remain truthful, timers count saved foreground time only and recover paused, notes need save actions. No destructive migrations or automatic associated-data removal.
- JSON writer 6 reads versions 1–6; current Room is 10. All images/image metadata and take metadata/labels/audio remain excluded; retain original images separately. Per-take export exists; bulk media restore/import/cloud/storage redesign remain deferred.
- Initial safe-deletion CI 40799694201 passed all 65 unit tests (including the eight focused deletion tests), compilation and lint. Native evidence prompted test-only explicit empty-search scrolling and fixture isolation: inserting deletion's protected-history fixture before old practice tests interfered with their unordered completed-session lookup. Final source advances to build 23; no speculative product fix.
- Final [release CI 40801285208](https://github.com/mrayover/practice.logger/actions/runs/40801285208): build job 123209739216 passed, API 26 job 123211043224 passed, API 35 job 123211043268 failed at PracticeWorkflowTest.kt:26 waiting for Add Warmup fixture. Its cause remains unestablished and differs from the earlier 200% Routine failure. [Isolated final-source sequence 40802423099](https://github.com/mrayover/practice.logger/actions/runs/40802423099) passes all five unchanged catalog/process/practice/recovery/history classes. No full final API 35 pass is claimed; later phases were not reached. Focused both-API deletion/restart/200% proofs and exact APK source are recorded in CURRENT_STATE.md.
- Existing publisher verified final signature/certificate/package/version/APK hash/ZIP integrity; candidate and final APKs are distinct, older distributions retained. No private signing material was uploaded. Physical phone/provider/TalkBack/audio checks remain pending.

## 2026-10-28 — Mandatory efficiency procedure for all future work

Owner requested this procedure after the Saved Routines postmortem and explicitly authorized this documentation change to be committed/pushed with Git commands. All future instances must review and follow this section before implementation, alongside the existing required project records. This procedure governs execution efficiency; it does not waive data preservation, release verification, signing, or truthful reporting requirements.

Why this is required: the first Routines implementation already built and passed all51 unit tests around08:37Pacific. Most subsequent delay was an Android8 catalog-screen verification detour, repeated speculative fixes/full CI runs, and excessive analysis/tool output/status narration. Eight CI runs were started; first implementation commit08:33 to final documentation10:23 was approximately1hour51minutes, with preparation/coding additional. An unsigned release build existed early, but a signed handoff was delayed until the owner intervened. New routine persistence/editor/migration/backup work was necessary; it did not justify the debugging inefficiency. Final passing changes were combined, so keyboard cleanup alone was not independently proven as the sole cause. Failure in an existing test does not establish a pre-existing product defect.

Required procedure:

1. Establish scope and reuse first. Inspect the current implementation and verified baseline, identify existing components to reuse, and state the smallest necessary additions. Distinguish a new persisted capability from an existing screen label. Avoid rebuilding working infrastructure or adding unrelated improvements.
2. Get to a build early. After the smallest coherent implementation compiles and basic checks pass, prepare an installable candidate using existing versioning/signing rules when delivery is requested. Clearly label incomplete verification. Continue required release checks; never describe a candidate as fully verified or phone-accepted. A user request to prioritize a build takes precedence over optional work.
3. Isolate a failure before repeating the full pipeline. Run the smallest relevant test/class/device/job available. Keep required assertions and acceptance coverage. If CI cannot target a failure, state that limitation and avoid firing the entire matrix for each guess. Run the complete required regression suite after targeted checks pass; repeat it only when new changes, failures or unresolved concerns justify it.
4. Gather evidence promptly. For an unexplained stall or failed attempted fix, capture the exact failing step/stack, relevant logs, screen/semantics state and bounded timeout evidence before another behavioral change. Do not spend multiple cycles guessing at scrolling, keyboard, lifecycle or database causes without new evidence.
5. Change one causal variable at a time where practical. State the hypothesis and expected observation; verify the result. Revert unsuccessful probes. If several changes are necessary together, report a successful combination rather than claiming an independently established root cause. Do not label a failure a test flake or pre-existing defect without evidence.
6. Separate implementation, debugging, release checks and delivery. Do not hold the first usable candidate solely because an unrelated regression investigation is continuing; disclose its status. Preserve every required final release gate and existing data/audio. A passing build, passing automated workflows and owner phone acceptance are distinct claims.
7. Control context and usage. Read required records, then retain a concise working summary instead of repeatedly rereading large files. Batch independent inspections; request bounded relevant output. Monitor usage at meaningful milestones and reserve capacity for verification, signing, final records and publication. Keep progress updates concise and decision-relevant; avoid repeating unchanged status or rehearsing the same hypotheses.
8. Keep records useful. Append facts and outcomes to WORK_LOG, keep CURRENT_STATE current with one exact next task, and put durable rules in AGENTS. Link to the authoritative explanation rather than duplicating long release narratives across every document. Record failed/cancelled runs honestly, preserve old entries, and close with artifact locations, actual tests/status, remaining limitations and verified local/remote commit state.

For the next instance: explicitly confirm in the initial work plan that this procedure was reviewed; apply it throughout the session. If an exception is necessary, record the concrete reason and its effect on time, verification or delivery. The owner should not need to repeat this instruction.

## 2026-10-28 — Owner-requested record cleanup

Compared clean local/remote main`84565ea6898e60852538ebcb4997e7c6b647547f` with app/build.gradle.kts, schema/backup/deletion/navigation code, current signed BUILD_INFO/APKchecksum, delivery pointer and actual GitHub job outcomes. Reviewed mandatory efficiency procedure. Consolidated README, AGENTS, BUILD_PLAN, CURRENT_STATE, WORK_LOG, MANUAL_TESTS, BACKUP_FORMAT, EVALUATION and current handoff; superseded the duplicate first-session prompt and clearly marked the retained original plan as historical. Removed obsolete distribution references, repeated release narratives, stale schema/JSON/navigation/placeholder claims, completed next tasks and overlapping scope exceptions from active records. Kept uncertain phone results, future boundaries/evaluation template and the original plan's ambiguous future/reference content. The owner's cleanup request authorizes this exception to prior record-preservation instructions; original content remains in Git history.

Documentation only: application source/configuration/schema, signed APKs/ZIPs and package provenance are unchanged. Checks passed: all local Markdown links resolve; no obsolete distribution paths remain; all 91 tracked non-Markdown files and all 84 current APK/ZIP files match the pre-cleanup hashes; the release manifest, APK checksum and ZIP integrity agree; the mandatory efficiency procedure is preserved verbatim; Git diff whitespace checks pass. No app build, emulator rerun or new phone result is claimed. Git publication and the verified remote SHA are reported in the session handoff. Exact next product action remains the current-release deletion phone checklist; no feature or evaluation result is invented.

## 2026-10-28 — Git/project accuracy audit

Owner requested comparison of Git with current project files. Clean local main and actual GitHub main both94eed943a3ed97beca0e68f10ba70fa60c2accba. All tracked checkout files match HEAD; changes since APK source8b469dc are documentation only (no app/scripts/workflow changes). Current Gradle0.12.0/code23/applicationID, Room8, JSON5/readers1–5, cream#F1E6D3/green#315C47, Home progress bars/outlined Home returns and transactional dependency-protected deletion agree with operational records. dist/LATEST and BUILD_INFO identify the same build23/source; actual APK SHA256e481ea500b331611c769829014b9b38ff0fcaf300736745a363b1d7c5d13cfcd matches both. Distributions/signing material are intentionally Git-ignored, not missing source.

Actual release CI40801285208 confirms build success/API26 success/API35 failure. Cached final reports contain65tests with0failures/errors/skips; diagnostic40802423099 is successful. Documentation accurately retains the unresolved full API35 limitation and distinguishes isolated passes. No material source/documentation/release mismatch found. Refreshed CURRENT_STATE review snapshot; no app changes, builds or tests run for this read-only audit. Prior current-chat Git-command/push authorization used for these two audit records; final remote/clean state verified at handoff. Exact next product action remains the existing build23 phone checklist/observed feedback, not new feature work.

## 2026-10-29 — Daily Log and Weekly View implementation

Reviewed mandatory efficiency procedure; clean main baseline 9c87acb, version 0.12.0/build23, Room8/JSON5 verified. Owner explicitly authorizes this increment and Git publication. Reused existing checkout, Compose/theme/Home, Room and atomic backup infrastructure. Added date-keyed independent plain-text Log, Monday–Sunday weekly aggregation and date picker; date-captured coalescing IO autosave with flush barriers before navigation/date loads/backgrounding. Added explicit Room8→9 migration, backup6 writer/readers1–6 with strict dates, Log-only nonempty guard, atomic restore and existing 8MiB export limit. Candidate version0.13.0/build24. Added focused persistence/concurrency/failure/weekly/backup/migration tests and native daily/weekly/recreation/rollover/cold-reopen/install-over phases. Source written; compilation and release verification pending. Prior artifacts and signing key retained. Generated schema9 will be retrieved from compiler output before closeout.

Focused seven Log tests passed in CI40895602401. First broad unit run:71/72 passed; RoutineBackupTest's old future-version rejection used6, which is now the supported format. Updated it to7, retaining rejection assertion. No product workaround; focused gate now includes that class. Compiler schema9 retrieved unchanged from the build artifact and committed alongside schema1–8. Native test waits for asynchronous Log loads before interacting. Full release gates pending rerun.

Build24 source1605885:72/72 unit tests passed, compilation/release/lint passed (0errors/10existingwarnings); schema9 compiler output matches committed file. Permanent-key candidate APK/ZIP published locally with certificate/package/version/hash/ZIP checks. Focused LogWorkflowTest+LogReopenTest passed at200% on API26 CI40896235913 and API35 CI40896231087. Screenshot review found week controls clipped by the fixed header viewport, despite reachable scrolling in tests. Build25 removes that viewport: weekly heading scrolls with the dated entries, daily editor/page scrolls for keyboard/enlarged text, paired buttons share width. Candidate24 retained. Static workflow review also found a downgrade after its first upgrade probe; subsequent test installation now retains26 and later history probe advances27. Backup native malformed-file expectation updated to the specific unsupported-format error now surfaced by the UI. No data model or autosave change. Final gates pending.

## 2026-10-29 — Log release closeout

Delivered 0.13.0/build 25 from d29e4bf7c7287467a724b8405ea24a68a462ed8d, Room 9/JSON 6 (readers 1–6), independent daily text/weekly aggregation only. Source/tests/schema/docs committed in the original checkout; Git publication explicitly reconfirmed after automatic approval review initially rejected it. Existing permanent publisher produced APK/ZIP with pinned certificate/signature/stable identity/version/hash/ZIP checks. Earlier packages including candidate24 retained; prior package checksums verified unchanged. Distribution/signing files are intentionally local/ignored; synced sources untouched.

practice.logger 0.13.0/build 25 — Daily Log and Weekly View
Source: d29e4bf7c7287467a724b8405ea24a68a462ed8d
Release CI: https://github.com/mrayover/practice.logger/actions/runs/40897188240
72 unit/Robolectric tests: 0 failures/errors/skips; debug/release compilation passed; lint 0 errors / 10 existing warnings.
API26 full release workflow: Log daily/weekly workflow, cold reopen and install-over retention passed. Catalog, practice/recovery/history, history upgrade, backup/picker restore/reopen, multiple sessions and metronome passed. Full job 123532311920 stopped at RecordingWorkflowTest.kt:101 waiting for Delete this take? after rename; subsequent phases were not reached. The unchanged recording class passed alone in diagnostic CI 40899553803 (job 123537782782). Cause remains unestablished; no full API26 pass is claimed.
API35 full release workflow: Complete final suite passed in job 123532313006: original catalog plus all 31 direct phases, including Log daily/weekly, cold reopen, install-over retention, backup/pickers, practice/history, audio/loops, Tools/Routines/Goals/deletion and enlarged-text checks.
200% focused Log workflow/cold reopen: Final LogWorkflowTest and LogReopenTest passed at 200% on API26 (CI 40897266669/job 123529683140) and API35 (CI 40897260725/job 123529666316). Tests cover today/yesterday edits, immediate navigation, week selection/navigation, recreation, simulated local-day change and process reopen.
Screenshots: Final daily and weekly views reviewed at normal text on API35 and enlarged text on API26/35. The clipped build 24 weekly header was replaced by scrolling content; build 25 controls remain readable and the editor scrolls into view. Physical-phone landscape/TalkBack/provider acceptance remains pending.
Permanent certificate, app identity/version, signature, APK checksum and ZIP round trip verified.
Room 9 data-preserving migration tested against all schema8 tables; schema 1–8 retained unchanged. JSON 6 reads1–6, Logs included, atomic/nonempty guards retained.
Physical-phone install over the owner's permanent-key app and phone/provider/TalkBack checks remain pending; automated install-over uses CI debug signing. Never uninstall or clear the owner's app.
Stage 3 detailed phone acceptance pending; Stage 4 stays0/10. JSON excludes recordings/labels/metadata.
APK SHA256: aad00c5f137f2c6ed9b27435cdfc8dfba8f67cdd523805b24fd7f5b5b3d3796d

Final native/visual findings and exact next phone task are authoritative in CURRENT_STATE.md; no permanent-key phone installation, phone acceptance or new real evaluation session is invented. Documentation-only closeout follows APK source; final local/remote main SHA and clean state are verified at handoff. No image/PDF attachments or unrelated feature work.


## 2026-10-29 — Increment 2 implementation started

Reviewed mandatory efficiency procedure and required source/records before edits. Clean existing Windows main c916963 matches actual GitHub main; Room9/JSON6/build25 verified. Owner explicitly authorizes implementation, tests, signing, commit and push. Added one Block-keyed RESTRICT attachment table and explicit 9→10 migration, private generated filenames, bounded 20MiB/40MP imports and 2048px sampled decoding on IO, transactional replacement retaining prior file until commit, startup orphan cleanup, confirmed removal and confirmation-time deletion protection. Editor system picker and local bounded inline show/hide/zoom/pan reuse Compose/Lato/cream/green; sourceBlockId resolves archived ownership without changing snapshots. Practice timer/click and recording engine/sheet unchanged. JSON stays6/readers1–6; images/paths excluded, limitations disclosed in editor and backup UI. Focused storage/failure/migration/backup/dependency tests and native picker/timer/click/zoom/reopen/upgrade coverage added. Candidate0.14.0/build26; verification/signing pending, not delivered yet. CI remains the existing build environment (local checkout has publisher tools but no full Android SDK/JDK).


Increment2 first CI40904738845/source3106e7e: all79unit tests passed, debug/release compilation and lint0errors/12warnings (10existing plus2conservative UsableSpace recommendations). Compiler schema10 retained; schema1–9 byte-identical. Early permanent-key build26 APK/ZIP packaged and verified, SHA2567b465a51f89211b720bfd981b7a6b7857dfdf3633589be037d20872cf048a03f. All92prior APK/ZIP files byte-identical. Both native jobs stopped at new ImageReferenceWorkflowTest:46 selecting a missing picker fixture, before import/viewer checks. Fixture setup used a nested quoted shell pipeline unsupported by the instrumentation command path; replaced with writing an external test JPEG, direct shell cp, explicit size check and media scan. Product code unchanged; isolated image workflow/reopen diagnostics next. Added failure log/screenshot retention at the new CI gate. No native success or completed Increment2 claim yet.


Isolated CI40906218662/API35/sourceb43fce4 still stopped selecting the picker item (line50); copied file size check passed. Retrieved actual screenshot and hierarchy: the green JPEG is visible in DocumentsUI grid, whose item exposes `content-desc="reference-fixture.jpg, ..."` with empty text. Changed the selector to the observed filename accessibility description, retaining text fallback for list mode. The earlier nested-shell/absent-file diagnosis was not independently established; this is a test selector failure, and the product import has not yet been reached. Product and candidate APK unchanged. Continue isolated class checks before broader rerun.


## 2026-10-29 — Increment 2 signed delivery and closeout

Delivered the smallest single-image Block reference feature as 0.14.0/build 26 from 3106e7e1ce95f4ef25e9ee0c11a182ae1d1a8c46. Final test/workflow source is cb75a69d3cc298029e4f9aa6ea915fd9dbc53db8; the app/Gradle product sources are byte-equivalent to the APK source. Room 10 only adds restrictive Block-keyed image metadata through an explicit 9→10 migration; JSON 6/readers 1–6 excludes all image/media references and retains Logs/structured records. Stored private original bytes, generated filenames, bounded IO validation/decode, file-first transactional replacement, conservative orphan recovery, confirmed removal and deletion dependency protection reuse existing architecture. Inline placement-keyed show/hide/zoom/pan leaves timing/click/recording infrastructure and immutable practice snapshots unchanged. No PDF or recording-sheet reference integration. Required documentation, native/unit tests and compiler schema are in Git; synced sources untouched.

practice.logger 0.14.0 / build 26 — Increment 2: collapsible Block reference image
APK source: 3106e7e1ce95f4ef25e9ee0c11a182ae1d1a8c46
Final native test/workflow source: cb75a69d3cc298029e4f9aa6ea915fd9dbc53db8; later closeout changes are documentation only.
Build/package CI: https://github.com/mrayover/practice.logger/actions/runs/40904738845
Final release checks: https://github.com/mrayover/practice.logger/actions/runs/40907898581
79 unit/Robolectric tests passed with 0 failures/errors/skips; debug/release compilation passed; lint 0 errors / 12 warnings (10 existing and 2 conservative free-space recommendations).
Android 8 / API26: Complete final suite passed in job 123569735286: catalog plus all 34 direct phases, including image picker/zoom/timer/click, cold reopen and install-over retention; Log, practice/history, backup/pickers, recording/loops, Tools, Routines, Goals, deletion and enlarged-text workflows.
Android 15 / API35: Complete final suite passed in job 123569735205: catalog plus all 34 direct phases, including image picker/zoom/timer/click, cold reopen and install-over retention; Log, practice/history, backup/pickers, recording/loops, Tools, Routines, Goals, deletion and enlarged-text workflows.
200% text API26: Image workflow and cold-process reopen passed at 200% text (job 123567637479), including real picker import/cancel, zoom/pan, running timer/click/BPM/notes preservation, replacement/removal and archived ownership. (https://github.com/mrayover/practice.logger/actions/runs/40907908344)
200% text API35: Image workflow and cold-process reopen passed at 200% text (job 123567643837), including real picker import/cancel, zoom/pan, running timer/click/BPM/notes preservation, replacement/removal and archived ownership. (https://github.com/mrayover/practice.logger/actions/runs/40907912062)
Normal API35 isolated image workflow/cold reopen passed in CI40907101426.
Final screenshots reviewed: Normal API35 and 200% API26/API35 editor and inline zoom-view screenshots reviewed. Image controls and backup disclosure remain readable and scrollable; the viewer stays bounded and its controls remain reachable.
Permanent signature, pinned certificate, stable package/version, APK hash and ZIP integrity verified.
APK SHA256: 7b465a51f89211b720bfd981b7a6b7857dfdf3633589be037d20872cf048a03f
ZIP SHA256: ec9ad9343f78e793dc123356a9f5910b3c4cc153613a78c17adb83f8d7f9cad6
All 92 pre-session APK/ZIP files retained byte-identical.
Room10 explicit9→10 data preservation and schemas1–9 retained; JSON6/readers1–6 unchanged.
JSON excludes images and recordings. Keep original images separately; restore does not recreate media references.
Install OVER the permanent-key app WITHOUT uninstalling or clearing data.
Phone install-over/provider/landscape/TalkBack/acoustic acceptance remains pending.
No PDF, multiple images, camera, annotation, media-inclusive backup or recording-sheet image integration.
Remaining limits: Physical-phone permanent-key install-over and provider/landscape/TalkBack/acoustic acceptance remain pending. No simultaneous image viewing inside the recording sheet, PDF, multi-image or media-inclusive backup is implemented.


The first native failures and corrected accessibility selector are documented above. Normal and enlarged-text evidence was gathered before final release acceptance. No physical phone/provider/TalkBack/audio result is invented. All required records are updated; final local/remote main SHA and clean state are verified at handoff. Signing/distribution remain local and ignored. The next task is the six image/in-place-update phone checks; no future increment is selected.


## 2026-10-29 — Owner phone refinements: compact heading and upright, wider image

Reviewed mandatory efficiency procedure and required current records; clean local/remote main8b77c10 verified. Owner phone screenshot requests less whitespace between Block name/action/BPM, upright portrait reference and full phone width. Existing BitmapFactory decoding ignored EXIF orientation. Added AndroidX ExifInterface1.4.2 read-only orientation handling for all eight rotation/flip cases during IO display decoding; existing private originals are read at display time, without rewriting files or metadata. Focused Practice now groups top matter at4dp spacing with16dp control insets, and lets the reference itself span the full safe width. Viewer follows oriented aspect ratio within180–640dp so normal portrait pages use the width rather than being shrunk into the old240dp landscape region. Timer/click/recording logic and Room10/JSON6 unchanged. Candidate0.14.1/build27. Added corner-color/byte-preservation regression for all EXIF cases and upgraded native fixture to an asymmetrical upright musical-staff page stored sideways with a portrait tag, plus compact-gap/edge-width/portrait assertions. Targeted checks, signed package and native verification pending. Prior distributions retained; source files in synced projects untouched.


Refinement focused unit gate passed in CI40913878853. Initial native diagnostic40913944791 failed compilation of the newly added layout assertion: this Compose DpRect exposes edges rather than width/height properties. Corrected the assertion to compare bottom−top and right−left; product code unchanged. Isolated native rerun precedes the final full matrix. No native runtime pass claimed from the compile-failed diagnostic.


## 2026-10-29 — Portrait/layout refinement signed delivery

Delivered0.14.1/build27 from ff50707374108c16c4b4ab2653a5fdfd6110853c, final test/workflow source 04c1f41c504c65e97ffd052b1ee828e0a5be655a; subsequent changes are records only. Owner-requested compact4dp Block top matter, full safe-width portrait region180–640dp and EXIF rotation/flip decoding are implemented. Existing private originals and Room10/JSON6 are unchanged; no reattachment, data migration, timer/click engine or recording lifecycle change. Required guidance/manual checks/license attribution updated. Original owner photo acceptance remains pending because the supplied screenshot is not its original JPEG; asymmetric tagged JPEG fixtures verify all eight orientations and the requested native layout.

practice.logger 0.14.1 / build27 — compact Block heading and upright full-width references
APK source: ff50707374108c16c4b4ab2653a5fdfd6110853c
Final test/workflow source: 04c1f41c504c65e97ffd052b1ee828e0a5be655a; later closeout is documentation only.
Initial build/package: https://github.com/mrayover/practice.logger/actions/runs/40913878853 (successful build; superseded native jobs canceled after test compile correction)
Final release CI: https://github.com/mrayover/practice.logger/actions/runs/40915328499
80 unit/Robolectric tests passed, 0 failures/errors/skips; debug/release compilation passed; lint 0 errors / 12 unchanged warnings.
Android8/API26: Complete catalog and all34 direct phases passed in job123595592990, including portrait image workflow/reopen/install-over, Log, practice/history, backup/pickers, recordings/loops, Tools, Routines, Goals, deletion and enlarged-text workflows.
Android15/API35: Complete catalog and all34 direct phases passed in job123595593190, including portrait image workflow/reopen/install-over, Log, practice/history, backup/pickers, recordings/loops, Tools, Routines, Goals, deletion and enlarged-text workflows.
200% text API26: Corrected portrait workflow and cold-process reopen passed at 200% text, job123593933297, including EXIF upright dimensions, compact heading gap, full safe-width bounds and timer/click/BPM/notes continuity. (https://github.com/mrayover/practice.logger/actions/runs/40915333166)
200% text API35: Corrected portrait workflow and cold-process reopen passed at 200% text, job123593977317, including EXIF upright dimensions, compact heading gap, full safe-width bounds and timer/click/BPM/notes continuity. (https://github.com/mrayover/practice.logger/actions/runs/40915337470)
Normal API35 portrait workflow/reopen passed in https://github.com/mrayover/practice.logger/actions/runs/40914467244.
Visual review: Normal API35 and 200% API26/API35 portrait staff-page screenshots reviewed: upright page, full safe screen width and bounded aspect-preserving display; compact heading gap is asserted natively.
All eight EXIF rotation/flip cases tested with asymmetric corner colors; original/private bytes unchanged.
Room10/JSON6 unchanged; schema exports1–10 unchanged. No migration or media backup change.
Signature, pinned permanent certificate, package/version, APK checksum and ZIP integrity verified.
APK SHA256: 7066ce3522dec435c8e55e2fc78830a3a2c8c5216cdecea9936d848ecd6f5217
ZIP SHA256: a0f16f1dd53a5c56838a40abd7626139a3c807df17ca31e54e2c540198a07666
Permanent certificate SHA256: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830
All 96 prior packages preserved byte-identical.
Install in place; never uninstall/clear the existing permanent-key app.
Phone acceptance of the owner's original photographed page remains pending; it was supplied as a screenshot, not the source image.
Images stay excluded from JSON backups; keep originals separately. Recording-sheet image/PDF integration remains deferred.


Recorded the initial native-test compile error and cancellation of superseded native jobs above. Corrected targeted checks preceded final gates; no passing claim for canceled or compile-failed runs. Local/remote main and clean state are verified at final handoff. Distribution/signing remain local and ignored; all prior packages retained and synced sources untouched. Next task: install this update in place and check the owner’s already-attached photo and practice controls. No future feature selected.

## 2026-10-29 — Increment 3 baseline and authorized scope

Reviewed and explicitly acknowledged the mandatory efficiency procedure before implementation. Clean existing Windows main and actual remote main are 3911c15136f97fd5fbdc827cdd82c49f34d60e46. Baseline0.14.1/build27, Room10/JSON6/readers1–6, 80 unit tests and complete API26/API35 release suites verified against CI40915328499. Local tooling has permanent publisher/JRE17/build-tools35 but no full Android build environment; use established CI. Owner authorizes implementation, tests, commit/push and permanent-key packaging.

Smallest scope: generalize existing block_images row using its existing MIME/dimension fields for one image OR PDF, preserving table/schema10, existing rows, original bytes, RESTRICT ownership and deletion counts without any database migration. Keep historical internal names to avoid unrelated refactoring. Add generated .pdf files in existing private directory, 20MiB/100-page limits and Android PdfRenderer validation/rendering on IO; one bounded page at a time. Reuse inline zoom/viewer in Focused Practice and inside the existing recording sheet, retaining its enter/leave lifecycle and foreground-only capture. Keep essential recording controls outside scrolling reference content. JSON6 remains unchanged and excludes all media. Candidate planned0.15.0/build28; not delivered until verified. Required docs/test coverage will track actual progress and failures.

Implementation milestone: reused MIME-bearing block_images/schema10 without migration; retained EXIF image path and full-width portrait layout. PDF copies/100-page validation/rendering use PdfRenderer on IO with resource closure, at most one2048px displayed page and256px validation bitmap. Added inline recording-sheet viewer with active capture status/Stop or Cancel pinned outside LazyColumn; enterBlock/leaveBlock and recording/practice engines are unchanged. Added realistic small PDF fixtures, focused storage/failure/replacement/backup/deletion/legacy-schema tests and actual picker/practice/free/phrase/reopen native workflows. Version0.15.0/build28. Candidate build/units/lint and isolated native checks are next; no passing result or signed delivery claimed yet. Corrected a preparation-script relative path before source application; no product change resulted from that tooling error.


First focused results (source6726aac): product and test Kotlin compilation passed. Candidate CI40940867493 targeted12 tests:9 passed,3 PDF cases failed before rendering with Robolectric API35 NoSuchMethodError java.io.FileDescriptor.getOwnerId$ from PdfProcessor/ParcelFileDescriptor.detachFd (XML stack retained locally). Run PDF Robolectric cases on API34’s supported native renderer path; real-device API26/API35 renderer validation remains required. Isolated native CI40940871813 stopped before import at PdfReferenceWorkflowTest:46: StaleObjectException selecting Downloads during drawer transition. Screenshot/hierarchy show open drawer and Downloads; updated test to wait for idle and reacquire like the established image picker helper. No app workaround or native success claimed. Static review also adapted old recording-test list helpers for pinned Stop controls and added true build27→28 retention of every table/private image/audio byte. Required usage/backup/lifecycle/manual records updated; final verification remains pending.


Follow-up candidate40941665858/sourcec1934cb: same9 image/schema tests passed;3 PDF tests failed at page-count validation because Robolectric’s API34 PdfRenderer is a non-rendering stub (valid1/2-page fixture yields zero pages); XML retained. API35 had no native PdfProcessor support. Moved these three unchanged substantive storage/render/failure tests to real Android instrumentation, preserving assertions/fixtures instead of faking PdfRenderer. JVM regression gate retains all existing80 tests plus the new schema10 preservation case; actual renderer coverage runs on API26/API35. Added owner-encrypted/no-opening-password PDF rejection fixture and baseline artifact actions:read for true install-over. App source remains unchanged from6726aac while focused native check proceeds.


Candidate milestone: 0.15.0/build28 signed candidate — Increment3 PDF references and recording-sheet viewing.
Source:4a33b7fded54d349378935ff958993ee97b53fbc
Build CI40942064682:81 unit/Robolectric tests passed,debug/release compile and lint0errors/13warnings.
All schemas1–10 unchanged. Room10/JSON6 readers1–6.
Real API26 PDF storage/render/failure/replacement/cleanup tests passed:CI40942068595.
Permanent signature, pinned certificate, package/version, APK hash and ZIP round trip verified by publisher.
Full native PDF recording-continuity/upgrade/regression and200% checks pending; phone acceptance pending.
APK SHA256:0a3393974fd42997dff6550fc3ac27534989feb61661a299142b4405bc574119
ZIP SHA256:2abce2bb414db162e9cdc8353934d119ccf77a0ca73341a36bb1c4415606a49f
Install over the permanent-key app without uninstalling/clearing data. Keep original images/PDFs separately; JSON excludes media.

All100 prior distributions and compiler schemas1–10 verified byte-identical. Native40941670196 validated real API35 rejection and PDF picker/render/page/zoom, then failed its test-only picker-cancellation wait at line64; screenshot retained. Final native and phone acceptance remain pending.


Native edge finding40943395356/source8accc0e: API35 accepted owner-locked.pdf (encrypted with an empty opening password), contrary to this increment’s encrypted-file exclusion; API26 storage checks had rejected it. Added a bounded8192-byte/96-byte-overlap scan for the reserved /Encrypt PDF name including hex escapes, conservatively rejecting encryption before PdfRenderer on every API. This is a product fix based on a failing real fixture. Build28 remains preserved as an early signed candidate; corrected distributed update advances versionCode29 (0.15.0). No production key change. Isolated baseline-upgrade40943505589 stopped before install because assumed ~/.android/debug.keystore path did not exist; locate the actual CI debug store, validate/generate the ordinary Gradle debug identity if absent, and re-sign baseline/current/test APKs consistently within that CI job. Permanent signing materials are untouched. Full native recording/upgrade gates remain pending; no continuity success claimed.


Corrected signed-candidate milestone: practice.logger0.15.0/build29 — corrected signed candidate, final release gates in progress.
Source:4f97f3ddbfc9024d385e6e95984c974cfc116a0f
Build/package CI40944290626:81 unit/Robolectric tests,0failures/errors/skips; debug/release compile; lint0errors/13warnings.
API35 PDF validation/picker/viewer/free-recording/page-zoom-show-hide continuity/phrase-count-in-loop/dismissal/reopen:passed40944294502(job123695454032).
API26 actual build27-to29 install-over:passed40944299002(job123695469410),every table and private original image/audio byte retained.
Signature,pinned permanent certificate,identity/version,APK checksum and ZIP round trip verified. Room10/JSON6/readers1–6 unchanged;schemas1–10 byte-identical.
All100 prior packages and early build28 retained byte-identical.
Final full API26/API35 suites40945038634 and200% API26/35 checks40945043010/40945047534 pending.
Physical-phone acceptance pending. Install over permanent-key app WITHOUT uninstalling or clearing data.
APK SHA256:bddcb96335768d4dc84308fa090f762260f57f5d88a84d1b5cc5424752edaf70
ZIP SHA256:3d33be2fb432349952374d775167305c4a01f12a2d5b7670658da056b61fff5a
JSON excludes images/PDFs and recordings; keep originals separately and export wanted takes individually.

Normal API35 active-reference/phrase screenshots reviewed:readable page controls and pinned Stop/Save;real native assertions verify timer,BPM and take continuity. Required full release/200% gates started only after focused checks passed. Exact signed artifacts/checksums are now in CURRENT_STATE.md;no phone acceptance invented.


Final200% checks40945043010(API26)/40945047534(API35) both reached saved free take then timed out at PdfReferenceWorkflowTest:97 waiting for playback. Screenshot/hierarchy show “Take saved.”,Resume timer and the top of the sheet;no MediaPlayer preparation appears in the captured log. Hypothesis:generic child performScrollTo targeted the wrong visible control after the large lazy layout shifted. Change only that playback navigation to the established take-list.performScrollToNode(hasTestTag(play-id)),then assert displayed and click. Keep playback,timer and capture assertions. App source/APK29 unchanged;full release40945038634 continues on4f97f3d while isolated enlarged-text checks rerun. Do not claim this hypothesis established until the focused result.


Full release40945038634 build passed(81units/compile/lint),but both emulator jobs stopped before installation at test -n "$DEBUG_STORE". Exact logs show emulator-runner executes each YAML script line in a separate /usr/bin/sh -c,so the prior variable assignment does not survive;the identical operations had passed in the single-shell isolated upgrade script40944299002. Extracted preparation into scripts/Prepare-ReferenceUpgrade.sh and invoke that same helper from full/diagnostic paths. No product/package change;no full native pass claimed from40945038634. Restart the required full release matrix only after this concrete CI scoping correction.


Full rerun40946722042 passed build/81units/lint and both actual legacy upgrade checks; real PDF storage3 tests pass on both APIs. API26 then failed PdfValidationNativeTest while reopening the saved good PDF after an invalid import,with PdfRenderer IOException(corrupt format). Add fixture-name logging and assert retained private bytes equal the original before decode to isolate native state versus data damage;run only that class on26. API35 passed PDF/free/phrase/dismissal behavior but stopped at line113 leaving practice;failure screenshot shows quick-note keyboard restored over controls after sheet dismissal. Clear focus/hide IME in that test before leaving and after entering the note. App source/package unchanged while diagnostics run. Earlier enlarged-text PDF/image checks are fully passing on26/35;no complete matrix pass claimed yet.


Isolated API26 diagnostic40948852656/sourceb50851f reproduced the native failure specifically after corrupt.pdf;locked/owner-encrypted rejection reopened normally. Before decode,the saved private PDF bytes were asserted identical to the original,so this is native rendering state after malformed input rather than media loss. Added bounded4KiB tail/64-byte xref preflight requiring a complete standard startxref/EOF and declared table/stream before native parsing. Unsupported/truncated originals are rejected before PdfRenderer;all page validation remains. Viewer error now includes reopen/retry recovery for deeper platform parse failures. Corrected installable source advances to0.15.0/build30;candidate28 and29 APK/ZIP files remain preserved. Room10/JSON6 and recording engines unchanged. Focused malformed-import/retained-render tests and build/signing precede another full matrix;no final release pass claimed yet.


Build30 signed-candidate milestone:
practice.logger 0.15.0 / build30 — signed candidate; final release gates pending.
Source: dce8b3241d8dfa59270fbb3fdee4054d29d56082
Build CI40949598879:81 unit/Robolectric tests passed; debug/release compilation; lint0errors/13warnings.
Focused API26 CI40949603306:encrypted/corrupt replacement retains intact renderable original; PDF picker/practice/free/phrase/close/reopen workflow passed.
API35 keyboard-focus test correction passed in40948857925; app30 full validation below remains pending.
Room10/JSON6/readers1–6 unchanged; schemas1–10 retained. No recording/practice engine changes.
Permanent signature,pinned certificate,identity/version,APK hash and ZIP integrity verified by publisher.
Full API26/API35 release CI40950300706 and200% API26/35 CI40950305578/40950309794 in progress.
APK SHA256: e9747a15e929505b98df76c235061b9ca1596218818df270265f376e01ccc559
ZIP SHA256: 56b5de2f31ffba71f8247335ce16854cd662a026ac09a8081f826002fea2a4f3
Install over the permanent-key app without uninstalling/clearing data. Physical-phone acceptance pending.
JSON excludes images/PDFs and recordings;keep original source files separately.

The bounded preflight resolves the reproduced API26 corrupt-fixture renderer problem:focused tests now assert unchanged original bytes and successfully render immediately after every rejection. Corrected full and200% gates started only after this focused pass. Earlier candidates retain their original APK/ZIP bytes. Final phone acceptance remains pending.


Build30 enlarged-text milestone:all4 PDF recording/reopen and image/EXIF workflow/reopen phases pass at200% on API26 CI40950305578/job123716358670 and API35 CI40950309794/job123716373567,source dce8b32. Build30 package/source/document-link audit passes;all100 pre-session packages and early candidate28/29 APK/ZIP hashes remain unchanged. Full release40950300706 build passes;both native regression jobs remain in progress. No complete matrix or physical-phone pass claimed yet.


Build30 full40950300706:both APIs passed actual old-release upgrade/all-table-original-byte retention,PDF storage/validation/workflow/reopen/install-over,image workflow/reopen/install-over,Log, catalog,practice/history/recovery,backup/pickers,multiple sessions,metronome and free-recording/reopen. Stopped in existing PhraseWorkflowTest timer-during-long-capture section:API26 line96(recording already false at Pause timer),API35 line103(expected3 takes,found4 after background cancellation). Recording/practice engines are unchanged. Inspection found the increment’s test helper modification skipped original lazy-list scrolling for any partially visible control,not just the newly pinned Stop/Cancel. Restore original always-scroll behavior for all ordinary sheet controls;only pinned Stop and save take/Cancel phrase bypass list scrolling. Isolate RecordingWorkflow/Reopen→PhraseWorkflow/Reopen on26/35 before the next full run. This is a test-layout hypothesis,not an unverified claim of a pre-existing bug or flake. Signed build30 remains byte-identical;full matrix not yet passed.


Isolated audio40953263950/API26 and40953268123/API35:free recording/reopen pass;PhraseWorkflowTest consistently reaches line103 with4 saved takes instead of3 after the16s long capture. Restoring original control scrolling alone did not resolve it;no sole-cause claim. The first3 phrase length/counter/cancellation assertions pass;the extra take can only commit after exact-frame validation in unchanged TakeRepository,so the long fixture has completed before background cancellation. Change only that timer-interaction fixture from120BPM×32beats(16s)to60BPM×32beats(32s),retain all original8beat/120 and60/180 exact-frame/loop assertions,and add explicit recording+phrasing/remaining-time preconditions immediately before backgrounding. Keep the exact3-take cancellation result. Rerun only the isolated audio sequence on both APIs;APK30/source remains unchanged.


32s cancellation-window diagnostic:API35 all4 audio classes passed in40954391648/job123730578932. API26 40954386353/job123730561945 failed the new remaining-time precondition before backgrounding,not the cancellation assertion. Actual captured log:elapsed29700ms,duration32000ms;the test’s timer/scroll operations had consumed29.7s. Extend only that fixture to30BPM×32beats(64s),with15s wait accommodating its8s count-in. Preserve live capture and>5s remaining checks and exact3-take discard result. Rerun only API26’s implicated audio sequence,then the required matrix. Product source,APK30,original8beat/120/60/180 precision assertions remain unchanged. Earlier helper restoration is retained for original scrolling behavior but not credited as the sole failure cause.
