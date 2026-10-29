# Refinement in progress — 0.14.1 / build 27

2026-10-29: owner requests compact Block top matter, upright portrait reference and full-width phone display. Implementation and targeted orientation/layout tests are added; verification and permanent-key package pending. Room10 and JSON6 are unchanged. Exact next task: run focused EXIF/image tests, build and sign candidate27, verify native normal/enlarged behavior, then update release evidence and publish. Previous delivered build26 follows as historical baseline until this update is verified.

# Current state — practice.logger 0.14.0, build 26

Updated 2026-10-29 (America/Los_Angeles). This is the authoritative delivery and verification record. Increment 1 (Daily Log / Weekly View) is complete and retained. Increment 2 delivers one collapsible Block reference image. Historical evidence and the mandatory efficiency procedure remain in [WORK_LOG.md](WORK_LOG.md).

## Delivery and provenance

| Item | Verified value |
|---|---|
| Release | **0.14.0 / versionCode 26** |
| Signed APK source | `3106e7e1ce95f4ef25e9ee0c11a182ae1d1a8c46` |
| Final native test/workflow source | `cb75a69d3cc298029e4f9aa6ea915fd9dbc53db8`; product sources unchanged since the APK source |
| Package | `com.mrayover.practicelogger.app`, Android 8.0+ / min API26 / compile and target API36 |
| Database | Room **10**; explicit migrations 1→10, compiler schemas 1–10 retained |
| JSON | Writer **6**, readers **1–6**; Logs and structured data included, images and all recording media/metadata excluded |
| APK SHA-256 | `7b465a51f89211b720bfd981b7a6b7857dfdf3633589be037d20872cf048a03f` |
| ZIP SHA-256 | `ec9ad9343f78e793dc123356a9f5910b3c4cc153613a78c17adb83f8d7f9cad6` |
| Permanent certificate SHA-256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |

- [Signed APK](dist/practice.logger-0.14.0-b26-3106e7e/practice.logger-0.14.0-b26-3106e7e.apk)
- [ZIP distribution](dist/practice.logger-0.14.0-b26-3106e7e.zip)
- [Build provenance](dist/practice.logger-0.14.0-b26-3106e7e/BUILD_INFO.json)
- [Verification status](dist/practice.logger-0.14.0-b26-3106e7e/VERIFICATION_STATUS.txt)
- [Latest pointer](dist/LATEST.txt)

Existing Windows checkout: `C:\Users\mrove\Projects\practice.logger`. Source, tests, schemas and documentation are in Git; dist and signing material remain local and intentionally ignored. No synced sources were changed. The permanent publisher verified signature, pinned certificate, package/version, checksum and ZIP round trip. All 92 earlier APK/ZIP packages remain byte-identical. **Install over the permanent-key app without uninstalling or clearing data.** Physical-phone installation is pending; the old temporary-key 0.1.0 app is separate.

## Implemented behavior and data safety

After saving a Block, reopen its editor and Add reference image through Android’s system picker. One JPEG, PNG or WebP is copied to app-owned private storage. View with Show reference / Hide reference; Replace reference image safely retains the old attachment until the new file and metadata commit. Remove reference image requires confirmation. Picker cancellation and failed imports retain the previous attachment. Archive preserves ownership.

Focused Practice retrieves the live attachment by sourceBlockId, including archived Blocks, without changing immutable textual snapshots or adding media to session history. Each placement starts collapsed. The fitted viewer preserves aspect ratio, occupies a bounded 240dp region, supports pinch zoom up to 6×, panning while enlarged and Reset image zoom. Local viewer state does not invoke timer/click or recording lifecycle. Native checks exercise repeated show/hide/gestures with timer and click running, notes/BPM retention and switching to an image-free Block. Recording engine, lifecycle and modal sheet are unchanged; **reference viewing inside the recording sheet is not implemented**.

Private imports use generated UUID names, up to 20 MiB, 40 million decoded pixels and 16384 pixels per side. Copy/validation/decoding run on IO; preview decoding is sampled to about 2048 pixels per side. Files are synced and finalized before a transactional metadata upsert. A shared file mutex serializes replacement, removal and conservative recovery. Known generated orphans are removed only after a successful metadata read; interrupted cleanup retries on next launch. Missing/corrupt files show an inline error with Retry and editor repair/removal. Original selected files are untouched.

Room 9→10 only adds the one-row-per-Block block_images table with a restrictive foreign key. Images join Goal links, saved Routine references, takes and every session placement as permanent-deletion blockers. Confirmation-time recheck/delete remains transactional; the refusal explains Remove image first. Confirmed removal deletes metadata then the private file. No historical protection or association is weakened.

JSON remains format 6/readers 1–6. All Logs, Blocks, Goals, Routines, sessions/history and notes remain backed up. **Image bytes, metadata, filenames and URIs are excluded**, as are recordings. Restore creates no missing-media image references and retains all empty-target protections. Keep original images separately and reattach after restore. Clearing/uninstalling/device loss destroys app-owned media; platform backup/transfer remains excluded. No media-inclusive ZIP backup or per-image export is included.

## Actual verification

| Check | Result |
|---|---|
| Build, units and lint | 79 unit/Robolectric tests passed with 0 failures/errors/skips; debug/release compilation passed; lint 0 errors / 12 warnings (10 existing and 2 conservative free-space recommendations). [Final release CI](https://github.com/mrayover/practice.logger/actions/runs/40907898581); initial package build [CI40904738845](https://github.com/mrayover/practice.logger/actions/runs/40904738845). |
| Focused persistence/failures | JPEG/PNG/WebP, reopen/archive/replacement/removal, malformed/oversize/low-space/interrupted IO and failed-commit preservation, path safety, missing/corrupt decode, dependency recheck/restrictive FK, JSON exclusion/empty-target refusal and orphan recovery pass. |
| Migration/schema | Schema 9 fixture preserves every prior table and row, including Logs, notes, Goal/Routine/session links and takes. Earlier migration tests pass. Compiler schema 10 retained; schemas 1–9 unchanged. |
| API26 full release workflow | Complete final suite passed in job 123569735286: catalog plus all 34 direct phases, including image picker/zoom/timer/click, cold reopen and install-over retention; Log, practice/history, backup/pickers, recording/loops, Tools, Routines, Goals, deletion and enlarged-text workflows. |
| API35 full release workflow | Complete final suite passed in job 123569735205: catalog plus all 34 direct phases, including image picker/zoom/timer/click, cold reopen and install-over retention; Log, practice/history, backup/pickers, recording/loops, Tools, Routines, Goals, deletion and enlarged-text workflows. |
| 200% text API26 | Image workflow and cold-process reopen passed at 200% text (job 123567637479), including real picker import/cancel, zoom/pan, running timer/click/BPM/notes preservation, replacement/removal and archived ownership. [CI](https://github.com/mrayover/practice.logger/actions/runs/40907908344). |
| 200% text API35 | Image workflow and cold-process reopen passed at 200% text (job 123567643837), including real picker import/cancel, zoom/pan, running timer/click/BPM/notes preservation, replacement/removal and archived ownership. [CI](https://github.com/mrayover/practice.logger/actions/runs/40907912062). |
| Visual review | Normal API35 and 200% API26/API35 editor and inline zoom-view screenshots reviewed. Image controls and backup disclosure remain readable and scrollable; the viewer stays bounded and its controls remain reachable. |
| Package/previous releases | Signature/certificate/identity/version, manifest hash, ZIP CRC and extracted APK hash verified. All 92 pre-session packages retained. |

The focused native workflow uses an actual system-picker JPEG, saved editor reopen, cancel replacement/removal, deletion refusal, zoom/pan, repeated show/hide while timer/click run at 111 BPM, saved note/time retention, movement to an image-free placement, confirmed removal and replacement retained on an archived Block after cold restart. Full release checks include an install-over probe, using CI debug signing; matching permanent certificate/package and migration tests establish update compatibility, while physical-phone permanent-key installation remains pending.

Initial CI40904738845’s native jobs stopped at the new picker selector before import; isolated CI40906218662 showed the same symptom. Captured screenshot/hierarchy revealed a present JPEG whose grid filename was an accessibility description with empty text. Correcting that selector produced the isolated normal API35 pass in [CI40907101426](https://github.com/mrayover/practice.logger/actions/runs/40907101426). The earlier file-copy hypothesis was not independently established. Product code and signed APK were unchanged by these test corrections. Failures are retained in WORK_LOG.md; no initial full-device success is claimed.

## Limits and exact next task

Physical-phone permanent-key install-over and provider/landscape/TalkBack/acoustic acceptance remain pending. No simultaneous image viewing inside the recording sheet, PDF, multi-image or media-inclusive backup is implemented.

Physical-phone install-over/data/audio retention, vendor provider/low-storage behavior, portrait/landscape, TalkBack and acoustic acceptance remain pending. Use the reference-image and in-place-update checklist in [MANUAL_TESTS.md](MANUAL_TESTS.md), retaining originals separately. No PDF, multiple attachments, camera capture, annotation, image editing, media-inclusive backup or recording-sheet image integration. Stage 3 detailed phone acceptance is pending; Stage 4 remains **0/10 documented real sessions**.

**Exact next task:** install signed 0.14.0/build 26 over the existing permanent-key app and run the six Increment 2 phone checks in MANUAL_TESTS.md; report phone, Android/build and actual pass/fail. Do not uninstall or clear the app. No later feature is selected.
