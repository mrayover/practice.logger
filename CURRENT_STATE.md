# Current state — practice.logger 0.15.0, build 30

**New owner-approved increment in progress (2026-10-29):** timer completion cue, compact sketch-based Focused Practice and protected header-to-Home navigation. Candidate 0.16.0/build31 is implemented at 4c55480; compilation, native checks and signing are pending. The build30 delivery record below remains the last verified release until replaced. Next task: finish focused validation, publish the first signed candidate, then run the required final release gates.
Updated October 29, 2026 (America/Los_Angeles). This is the authoritative release record. **Increment 3 is delivered:** one PDF or image per saved Block, with inline reference viewing during practice and recording. Automated release gates pass. Physical-phone acceptance remains pending.

## Delivery and provenance

| Item | Verified value |
|---|---|
| Release | **0.15.0 / versionCode 30**; early candidates28/29 preserved and superseded |
| Signed APK source | `dce8b3241d8dfa59270fbb3fdee4054d29d56082` |
| Final full release test/workflow source | `994fae11cf0931440f45054931beafb455081a41` |
| Enlarged-text test source | `dce8b3241d8dfa59270fbb3fdee4054d29d56082` |
| Package / Android | `com.mrayover.practicelogger.app`; Android 8+ / min API26, compile/target36 |
| Data | Room **10**, migrations/schemas 1–10 retained; JSON writer **6**, readers **1–6** |
| APK SHA256 | `e9747a15e929505b98df76c235061b9ca1596218818df270265f376e01ccc559` |
| ZIP SHA256 | `56b5de2f31ffba71f8247335ce16854cd662a026ac09a8081f826002fea2a4f3` |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |

- [Signed APK](dist/practice.logger-0.15.0-b30-dce8b32/practice.logger-0.15.0-b30-dce8b32.apk)
- [ZIP distribution](dist/practice.logger-0.15.0-b30-dce8b32.zip)
- [Build provenance](dist/practice.logger-0.15.0-b30-dce8b32/BUILD_INFO.json), [verification status](dist/practice.logger-0.15.0-b30-dce8b32/VERIFICATION_STATUS.txt), [phone checklist](dist/practice.logger-0.15.0-b30-dce8b32/PHONE_CHECKLIST.txt), [latest pointer](dist/LATEST.txt).

App/Gradle product source and all Room schemas are identical across the signed, full-release and enlarged-text commits; later commits correct tests/CI and documentation. The permanent publisher verified signature, pinned certificate, identity/version, checksum and ZIP integrity. All 100 pre-session APK/ZIP packages and earlier candidates28/29 remain byte-identical. **Install over the permanent-key app without uninstalling or clearing data.**

The existing Windows checkout `C:\Users\mrove\Projects\practice.logger` is updated. GitHub is authoritative; source/tests/required records are committed and pushed to main. Distribution/signing stay local and Git-ignored. Synced project sources are untouched. The final documentation commit follows the verification source; exact remote HEAD is checked at closeout.

## Implemented behavior and data safety

One image (JPEG/PNG/WebP) **or** PDF per saved Block. Existing MIME/dimension fields in `block_images` support both types, so Room10 and every previous row/file survive without a migration. Generated private files, stable archived ownership, safe replacement, confirmed removal and conservative startup cleanup reuse the existing architecture. The picker source is never modified/deleted; no persistent external URI permission is needed. Replacement validates before ownership commits and retains the prior file until then. Both media types block permanent Block deletion through existing dependency counts, restrictive FK and transactional recheck, alongside Goals/Routines/takes/all session history.

PDF import is limited to 20 MiB, 1–100 pages and 1–14400-point page dimensions. A bounded complete startxref/EOF and cross-reference table/stream preflight rejects truncated or repair-dependent files before native parsing, avoiding the reproduced API26 renderer state failure after malformed input. An IO encryption-name scan rejects encrypted PDFs, including owner-encrypted files Android15 can open without a password. Reserved encryption markers are conservatively unsupported. PdfRenderer validates every page with one temporary256px bitmap, then displays only the requested page at2048px on IO. Pages/renderers/descriptors close correctly; there is no whole-document bitmap cache. The viewer provides Show/Hide, Previous/Next, page count, pinch/pan and reset; page changes reset zoom and stale results are discarded. Existing EXIF orientation, full safe-width Focused Practice display, 180–640dp height and image zoom/pan remain intact.

The same viewer is inline in the existing recording sheet during free capture, fixed-phrase count-in/capture and loop playback. Active status and Stop and save take/Cancel phrase stay above scrolling content. Reference interactions do not dispose/recreate the sheet or call recording/practice/metronome engines. Deliberate dismissal and genuine backgrounding retain existing foreground-only cleanup. Standalone Free recording has no Block reference. Playback, rename, export and deletion remain covered by release regression tests.

JSON6/readers1–6 and empty-target atomic restore are unchanged. **Image/PDF bytes, reference metadata/paths/URIs and all take metadata/audio are excluded. Keep originals separately and export wanted takes individually.** Restore creates no broken reference/take rows. There are no new runtime dependencies or permissions; existing notices remain applicable. Multiple attachments, annotation/OCR, camera capture, media-inclusive backup and background recording remain excluded.

## Actual verification

| Check | Actual result and evidence |
|---|---|
| Build / units / lint | **81 unit/Robolectric tests passed**, 0 failures/errors/skips; debug/release compilation passed; lint 0 errors / 13 warnings (12 baseline and one createBitmap KTX suggestion). [Final CI40957534780](https://github.com/mrayover/practice.logger/actions/runs/40957534780), build job123741402862. Signed package came from [CI40949598879](https://github.com/mrayover/practice.logger/actions/runs/40949598879). |
| Complete API26 release | Catalog plus **all 41 direct phases passed**, job123743503207 in final CI. Includes real PDF storage/rejection, recording continuity, cold reopen, actual build27-to30 upgrade, images/EXIF, Log/Weekly View, practice/recovery/history, backup/pickers, recordings/loops, Tools, Routines, Goals, deletion and enlarged text. |
| Complete API35 release | Catalog plus **all 41 direct phases passed**, job123743503037 in final CI, with the same coverage and encrypted-PDF edge case. |
| 200% API26 | All 4 PDF recording/reopen and existing image/EXIF workflow/reopen phases passed. [CI40950305578](https://github.com/mrayover/practice.logger/actions/runs/40950305578), job123716358670. |
| 200% API35 | All 4 PDF recording/reopen and existing image/EXIF workflow/reopen phases passed. [CI40950309794](https://github.com/mrayover/practice.logger/actions/runs/40950309794), job123716373567. |
| Data and lifecycle | True old-APK upgrade compares every table and private original image/audio byte; no uninstall or data clear between baseline seed and upgrade. Native tests assert timer/BPM/notes/capture continuity, saved/playable takes, phrase loop, intentional dismissal, archived ownership and restart retention. CI upgrades use temporary debug signing; permanent-key phone installation is not claimed. |
| Visual / package | Normal API35 and 200% API26/API35 PDF/image/active-recording screenshots reviewed: aspect ratio, upright full-width photos and reachable pinned Stop/Save. Signature/certificate/identity/version/hash/ZIP verified; schemas1–10 and prior packages preserved. |

Failed checks and resolutions remain in WORK_LOG. Robolectric lacks functioning PDF rendering on the attempted APIs, so all3 substantive PDF storage/render tests run on real Android with assertions retained. Picker drawer/cancellation timing and the new enlarged-text playback scroll were corrected from logs/screenshots. Owner-encrypted PDFs revealed an API35 product difference, fixed in build30. CI upgrade setup was corrected to discover the debug store and retain shell variables in a shared helper. The API26 corrupt-import diagnostic confirmed saved bytes were unchanged while native rendering failed; build30 preflights complete PDF structure before native parsing, and the retained original now renders immediately after rejection. A test keyboard-focus issue after dismissing recordings was corrected from its screenshot. The legacy phrase test’s 16s capture completed before its UI/background sequence; measured API26 interaction consumed 29.7s. Its isolated fixture now allows 64s and asserts live capture with more than 5s remaining before backgrounding, while preserving exact-frame/loop and 3-take cancellation assertions. A fixture-readiness enabled-state wait was also added. The isolated API26 sequence passed with 21.3s elapsed of 64s; API35’s sequence passed independently. Failed or setup-only runs are not counted as passing.

## Pending acceptance and known limits

Physical-phone permanent-key install-over, the owner’s original photographed page, vendor pickers, device landscape, TalkBack and acoustic/loop quality checks remain pending. Automated PDFs include portrait and landscape pages; no physical-phone result is inferred. JSON is not media backup, PDF encryption/oversized or unsupported documents are excluded, and display resolution is bounded. Deep unsupported native parsing failures may require reopening the app before retrying; retained bytes and metadata remain intact. Stage3 detailed phone acceptance is pending; Stage4 remains **0/10 documented real sessions**.

## One next task

Install signed build30 over the existing permanent-key app and complete the Increment3 phone checklist in [MANUAL_TESTS.md](MANUAL_TESTS.md), checking old data/photo retention and PDF viewing while saving a recording. Do not uninstall or clear data. No further feature work is selected.
