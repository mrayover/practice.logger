# Current state — Increment3 signed candidate0.15.0/build28

Updated2026-10-29(America/Los_Angeles). Authoritative delivery/verification record. Increment3 source implements one PDF OR image per saved Block and inline reference viewing within the recording sheet during capture. This is an installable signed candidate; final native recording-continuity/regression/200% verification is pending. Historical verified release0.14.1/build27 remains preserved.

## Candidate delivery and provenance

- Version0.15.0/versionCode28; app ID com.mrayover.practicelogger.app; minAPI26/target36.
- Signed APK source:`4a33b7fded54d349378935ff958993ee97b53fbc`; app source unchanged since initial6726aac.
- [Signed APK](dist/practice.logger-0.15.0-b28-4a33b7f/practice.logger-0.15.0-b28-4a33b7f.apk) and [ZIP](dist/practice.logger-0.15.0-b28-4a33b7f.zip).
- [Build info](dist/practice.logger-0.15.0-b28-4a33b7f/BUILD_INFO.json), [verification status](dist/practice.logger-0.15.0-b28-4a33b7f/VERIFICATION_STATUS.txt), [latest pointer](dist/LATEST.txt).
- APK SHA256:`0a3393974fd42997dff6550fc3ac27534989feb61661a299142b4405bc574119`; ZIP SHA256:`2abce2bb414db162e9cdc8353934d119ccf77a0ca73341a36bb1c4415606a49f`.
- Permanent certificate SHA256:`f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- Publisher verified signature,pinned certificate,identity/version,hash and ZIP roundtrip. All100 prior packages and schemas1–10 remain byte-identical.

Install over the permanent-key app **without uninstalling or clearing data**. Existing Windows checkout is updated; signing/distributions stay local/Git-ignored and synced sources untouched. No permanent-key phone installation is claimed.

## Implemented behavior and data

One generated private image OR PDF per stable Block, with safe replace and confirmed removal. Existing MIME/dimension columns in block_images support PDFs without a database migration; Room10/migrations1–10 and old rows/files are retained. Images keep EXIF orientation,full safe-width Focused Practice layout,180–640dp bounds and zoom/pan. PDFs:20MiB,1–100pages,supported1–14400point dimensions; validate each page with a256px temporary bitmap before ownership commit. PdfRenderer renders one requested page at2048px on IO and closes every page/document/descriptor. Viewer has Show/Hide,Previous/Next,page count,pinch/pan/reset; page changes reset zoom and discarded UI work cannot publish stale bitmaps.

Recording sheet contains the viewer and retains the existing enterBlock/leaveBlock/foreground lifecycle. Active status and Stop and save take/Cancel phrase are outside scrolling content. Recording/practice engines remain unchanged; real continuity verification is pending. Archive keeps ownership; attachment count/FK/transactional deletion recheck protect either media type alongside existing dependencies.

JSON writer6/readers1–6 and empty-target atomic restore are unchanged. Image/PDF bytes,reference metadata/paths/URIs and take metadata/audio are excluded. Keep originals separately and export wanted takes individually. Restore creates no broken attachment/take rows. No multiple attachments,annotation/OCR,camera capture,media-inclusive backup or background recording.

## Actual verification

- [Candidate build CI40942064682](https://github.com/mrayover/practice.logger/actions/runs/40942064682):81 unit/Robolectric tests,0failures/errors/skips; debug/release compilation passed; lint0errors/13warnings(12existing,one createBitmap KTX suggestion).
- [Real API26 PDF storage CI40942068595](https://github.com/mrayover/practice.logger/actions/runs/40942068595):all3 substantive persistence/render/portrait-landscape/replacement/encrypted-corrupt-size-cancellation-low-storage-commit-failure/deletion/backup/recovery tests passed,including owner-encrypted/no-opening-password fixture.
- API35 native validation passed in40941670196; picker/render/page/zoom assertions passed until test-only replacement-cancellation timing failure at line64. Screenshot shows DocumentsUI still open: the test checked currentPackageName before picker launch completed. Adding an explicit picker-open wait before Back. No recording-continuity pass claimed yet.
- Initial failed checks40940867493/40941665858 established missing/stub Robolectric PDF support. The three substantive tests moved to real Android without weakening assertions. Initial native40940871813 failed on stale Downloads drawer object; corrected with idle/reacquire. Failures retained in WORK_LOG.
- Full API26/API35 release matrix,true build27→28 install-over and200% PDF/image checks pending. Owner phone/provider/TalkBack/landscape/acoustic acceptance pending; Stage3 detailed acceptance pending and Stage4 remains0/10real sessions.

## Corrected build29 in progress

API35 edge check40943395356 found owner-encrypted/no-opening-password PDFs accepted by the platform. Build28 is superseded for final delivery; it remains an early candidate. Source now adds a bounded explicit encryption-name rejection and advances to0.15.0/build29. Corrected build/signing/native verification are pending. Baseline-upgrade CI40943505589 failed before install on the assumed debug-store path; it is being corrected without touching the permanent key.

## One next task

Finish isolated recording-continuity checks,then complete the required native release/200% gates and reconcile candidate status before final delivery. Phone checklist:[MANUAL_TESTS.md](MANUAL_TESTS.md).
