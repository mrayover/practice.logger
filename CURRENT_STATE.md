# Current state — practice.logger0.15.0,build29

Updated2026-10-29(America/Los_Angeles). Authoritative delivery and verification record. Increment3 implements PDF references and inline image/PDF viewing while recording. Build29 is signed/installable and focused native verification passes; final release/200% gates are in progress. Early build28 is retained and superseded after its API35 owner-encryption edge case was corrected.

## Delivery and provenance

| Item | Verified value |
|---|---|
| Version | **0.15.0/versionCode29** |
| Signed APK and current verification source | `4f97f3ddbfc9024d385e6e95984c974cfc116a0f` |
| Package | `com.mrayover.practicelogger.app`;Android8+/minAPI26,target/compile36 |
| Data | Room**10**,all schemas/migrations1–10 retained;JSON writer**6**,readers**1–6** |
| APK SHA256 | `bddcb96335768d4dc84308fa090f762260f57f5d88a84d1b5cc5424752edaf70` |
| ZIP SHA256 | `3d33be2fb432349952374d775167305c4a01f12a2d5b7670658da056b61fff5a` |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |

- [Signed APK](dist/practice.logger-0.15.0-b29-4f97f3d/practice.logger-0.15.0-b29-4f97f3d.apk)
- [ZIP](dist/practice.logger-0.15.0-b29-4f97f3d.zip)
- [Build provenance](dist/practice.logger-0.15.0-b29-4f97f3d/BUILD_INFO.json),[verification status](dist/practice.logger-0.15.0-b29-4f97f3d/VERIFICATION_STATUS.txt),[phone checklist](dist/practice.logger-0.15.0-b29-4f97f3d/PHONE_CHECKLIST.txt),[latest pointer](dist/LATEST.txt).

The existing permanent publisher verified signature,pinned certificate,app identity/version,checksum and ZIP integrity. All100 pre-session APK/ZIP files and early build28 remain byte-identical. Install **over the permanent-key app without uninstalling or clearing data**. The original Windows checkout is updated;signing/distributions remain local and Git-ignored;synced project sources are untouched. No permanent-key phone install is claimed.

## Implemented reference and recording behavior

One image(JPEG/PNG/WebP) OR PDF per saved Block. The existing block_images row already has MIME/dimensions;using those fields preserves schema10,old rows and original files without any migration. Archive retains stable Block ownership. System picker sources are read-only and copied to generated private files;no persistent external URI grant. Replacement validates before transactionally committing ownership,then cleans the prior file. Failure/cancellation retains the current attachment. Confirmed removal and conservative startup cleanup include PDFs and leave recordings/unknown files alone. Either attachment type blocks permanent Block deletion,with existing counts,FK restrictions and transactional recheck retained.

PDFs:20MiB,1–100pages,1–14400point page dimensions. A bounded encryption-name scan rejects encrypted files consistently across APIs,including owner-encrypted PDFs that API35 opens without a password. PDFs containing reserved encryption markers are conservatively unsupported. PdfRenderer validates every page with one256px temporary bitmap and renders the requested page at2048px on IO. Every page,renderer and descriptor closes;no whole-document bitmap cache. Show/Hide,Previous/Next,page count,pinch/pan/reset and aspect ratio are supported;page changes reset zoom and stale results are discarded. Existing EXIF orientation,full safe-width Focused Practice view,180–640dp region and image zoom/panning are retained.

The existing recording sheet now contains the reusable viewer during free capture,phrase count-in/capture and loop playback. Active recording status and Stop and save take/Cancel phrase stay above scrolling content. Showing,hiding,zooming and page changes do not call sheet enter/leave,change selected Block or touch recording/practice/metronome engines. Deliberate sheet close and actual backgrounding retain existing foreground-only safety. Standalone Free recording has no Block reference.

JSON6/readers1–6 and empty-target atomic restore remain unchanged. **Image/PDF bytes,reference metadata/paths/URIs and take metadata/audio are excluded. Keep originals separately;export wanted takes individually.** Restore creates no broken reference/take rows. No multiple attachments,annotation/OCR,camera capture,media-inclusive backup or background recording.

## Actual verification

| Check | Actual evidence |
|---|---|
| Build/units/lint | [CI40944290626](https://github.com/mrayover/practice.logger/actions/runs/40944290626):81unit/Robolectric tests,0failures/errors/skips;debug/release compilation passed;lint0errors/13warnings(12baseline plus one createBitmap KTX suggestion).Compiler schemas1–10 byte-identical. |
| Real PDF storage | [API26 CI40942068595](https://github.com/mrayover/practice.logger/actions/runs/40942068595):all3 substantive PDF storage/render/persistence/portrait-landscape/size-cancel-low-space-failed-commit/replacement/deletion/backup/recovery tests passed.Real updated full-API storage reruns are pending below. |
| Normal API35 PDF/recording | [CI40944294502](https://github.com/mrayover/practice.logger/actions/runs/40944294502),job123695454032:validation/picker/page-navigation/zoom/cancel/removal-protection,practice timer/click/BPM/notes,continuous free capture while showing/hiding/changing pages,save/playback,phrase count-in/capture/loop,deliberate dismissal,archive and cold reopen passed. |
| True previous-release upgrade | [API26 CI40944299002](https://github.com/mrayover/practice.logger/actions/runs/40944299002),job123695469410:seed retained actual build27,install build29 over it,every table row and private image/audio byte unchanged;legacy image still decodes and FK check passes.CI uses a shared temporary debug identity,not the permanent phone key. |
| Full API26/API35 suites | In progress:[CI40945038634](https://github.com/mrayover/practice.logger/actions/runs/40945038634).No final matrix pass claimed yet. |
| 200% text | In progress:[API26 CI40945043010](https://github.com/mrayover/practice.logger/actions/runs/40945043010),[API35 CI40945047534](https://github.com/mrayover/practice.logger/actions/runs/40945047534);both exercise PDF recording/reopen and existing EXIF image workflow/reopen. |
| Visual/package review | Normal API35 portrait PDF and active recording/page controls reviewed;Stop and save remains visible above the reference.Permanent publisher identity/signature/certificate/hash/ZIP verified.All prior packages preserved. |

Failed checks and fixes are retained in WORK_LOG:API35 Robolectric lacks the required FileDescriptor method;API34 uses a non-rendering stub,so all3 substantive PDF tests moved to real Android without weakened assertions.Picker drawer/cancellation waits were corrected from screenshots/hierarchy.API35 owner-encryption acceptance was a product bug fixed in29;build28 stays an early candidate.The first upgrade diagnostic failed before installation on a debug-store path assumption;store discovery/consistent test signing corrected it. No failed/canceled run is counted as passing.

Physical-phone permanent-key install-over,owner’s original photographed page,vendor pickers,landscape,TalkBack and acoustic/loop quality acceptance remain pending.Stage3 detailed phone acceptance is pending;Stage4 remains0/10documented real sessions.

## One next task

Complete the running release/200% gates and reconcile this candidate’s final verification record;then the owner phone checklist in [MANUAL_TESTS.md](MANUAL_TESTS.md) is the next acceptance task.
