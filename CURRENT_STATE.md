# Current state — practice.logger 0.14.1, build 27

Updated 2026-10-29 (America/Los_Angeles). This is the authoritative delivery and verification record. Daily Log/Weekly View and single-image Block references are delivered. This update refines the owner’s phone feedback: compact Block top matter, upright portrait photos and wider inline images. History and the mandatory efficiency procedure remain in [WORK_LOG.md](WORK_LOG.md).

## Delivery and provenance

| Item | Verified value |
|---|---|
| Release | **0.14.1 / versionCode 27**, superseding retained 0.14.0/build26 |
| Signed APK source | `ff50707374108c16c4b4ab2653a5fdfd6110853c` |
| Final native test/workflow source | `04c1f41c504c65e97ffd052b1ee828e0a5be655a`; product source unchanged since the signed APK source |
| Package/Android | `com.mrayover.practicelogger.app`, Android8+/minAPI26, compile/targetAPI36 |
| Database/JSON | Room **10**, migrations/schemas1–10 retained; JSON writer**6**, readers**1–6** unchanged |
| APK SHA256 | `7066ce3522dec435c8e55e2fc78830a3a2c8c5216cdecea9936d848ecd6f5217` |
| ZIP SHA256 | `a0f16f1dd53a5c56838a40abd7626139a3c807df17ca31e54e2c540198a07666` |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |

- [Signed APK](dist/practice.logger-0.14.1-b27-ff50707/practice.logger-0.14.1-b27-ff50707.apk)
- [ZIP distribution](dist/practice.logger-0.14.1-b27-ff50707.zip)
- [Build provenance](dist/practice.logger-0.14.1-b27-ff50707/BUILD_INFO.json)
- [Verification status](dist/practice.logger-0.14.1-b27-ff50707/VERIFICATION_STATUS.txt)
- [Latest pointer](dist/LATEST.txt)

The permanent publisher verified signature, pinned certificate, app identity/version, checksum and ZIP round trip. All 96 earlier APK/ZIP files remain byte-identical. **Install over the permanent-key app without uninstalling or clearing data.** APK source precedes test/documentation corrections; app and Gradle product sources are identical at final verification. Existing checkout `C:\Users\mrove\Projects\practice.logger` is updated; dist/signing stay local and Git-ignored. Synced project sources are untouched.

## Refinements and retained behavior

Focused Practice groups the Block count/name/action/BPM/reference notes with **4dp gaps**, replacing the former 20dp gaps. Controls retain **16dp side insets**. The image itself spans the complete safe phone width. Its region follows the oriented image aspect ratio within **180–640dp**, so a normal portrait page uses the width rather than shrinking into the old fixed240dp landscape region; unusually tall images remain bounded. Show/Hide and fitted aspect ratio, pinch zoom up to6×, enlarged panning and Reset image zoom are retained. Normal scrolling works when not manipulating the image.

AndroidX ExifInterface1.4.2 reads camera rotation/flip metadata while decoding on IO and applies it to the sampled display bitmap. All eight EXIF orientations are handled. **Existing attachments are corrected at display time without reattaching or rewriting original bytes or metadata.** Untagged images retain their stored pixel orientation; this update does not infer orientation from musical content or add an image editor. The owner’s particular source photo has not been inspected directly, so its physical-phone acceptance remains pending.

One private JPEG/PNG/WebP per saved Block; generated UUID files, up to20MiB/40MP/16384px validation, sampled decode around2048px, safe file-first transactional replacement, confirmed removal and conservative startup orphan cleanup remain. Picker cancellation/failed imports retain the old image. Archive preserves stable Block ownership. Missing/corrupt files show Retry or editor repair/removal. New Blocks require Save/reopen first.

Practice resolves sourceBlockId without changing immutable textual snapshots. Viewer state remains separate from timer/click and recording lifecycle; their engine/model files are unchanged. Image attachments still block permanent Block deletion alongside every earlier Goal/Routine/take/session dependency, with confirmation-time transactional recheck and a restrictive foreign key. No Room migration is needed for this refinement.

JSON6/readers1–6 retains Logs, Blocks, Goals, Routines, sessions/history and notes. Image bytes/metadata/filenames/URIs and recordings remain excluded. Keep originals separately and reattach after restore; no missing-media metadata is recreated and empty-target restore protections remain. Private media is lost on uninstall/clear/device loss, and platform backup remains excluded. No PDF, multiple attachments, camera capture, annotation, image editing, media-inclusive backup or recording-sheet reference viewing is included.

## Actual verification

| Check | Result |
|---|---|
| Build/units/lint | 80 unit/Robolectric tests passed, 0 failures/errors/skips; debug/release compilation passed; lint 0 errors / 12 unchanged warnings. [Final release CI](https://github.com/mrayover/practice.logger/actions/runs/40915328499); signed package originated from the successful build job in [initial CI](https://github.com/mrayover/practice.logger/actions/runs/40913878853). |
| Orientation regression | Asymmetric corner-color assertions verify every EXIF rotation/flip, expected portrait/landscape dimensions and unchanged source/private bytes. Existing image persistence/failure/deletion/backup tests pass. |
| Native normal API35 | Portrait-tagged camera-style JPEG imported through actual picker; upright dimensions, compact title/action gap, full safe edge-to-edge viewer width and portrait shape asserted. Timer/click/BPM/notes continuity, repeated zoom/pan/show/hide, replacement/removal/archive and cold reopen pass in [CI40914467244](https://github.com/mrayover/practice.logger/actions/runs/40914467244). |
| API26 full release | Complete catalog and all34 direct phases passed in job123595592990, including portrait image workflow/reopen/install-over, Log, practice/history, backup/pickers, recordings/loops, Tools, Routines, Goals, deletion and enlarged-text workflows. |
| API35 full release | Complete catalog and all34 direct phases passed in job123595593190, including portrait image workflow/reopen/install-over, Log, practice/history, backup/pickers, recordings/loops, Tools, Routines, Goals, deletion and enlarged-text workflows. |
| 200% text API26 | Corrected portrait workflow and cold-process reopen passed at 200% text, job123593933297, including EXIF upright dimensions, compact heading gap, full safe-width bounds and timer/click/BPM/notes continuity. [CI](https://github.com/mrayover/practice.logger/actions/runs/40915333166). |
| 200% text API35 | Corrected portrait workflow and cold-process reopen passed at 200% text, job123593977317, including EXIF upright dimensions, compact heading gap, full safe-width bounds and timer/click/BPM/notes continuity. [CI](https://github.com/mrayover/practice.logger/actions/runs/40915337470). |
| Visual review | Normal API35 and 200% API26/API35 portrait staff-page screenshots reviewed: upright page, full safe screen width and bounded aspect-preserving display; compact heading gap is asserted natively. |
| Data/package safety | Schemas1–10 unchanged; no JSON/data-model migration. Existing timer/session/recording/backup engine files unchanged. Signature/certificate/package/version/hash/ZIP verified; 96 prior packages preserved. |

Initial native diagnostic40913944791 failed compilation of a new test assertion because this Compose rectangle API exposes edges rather than width/height properties. The assertion was corrected to bottom−top/right−left, preserving its coverage; app code did not change. Initial full CI40913878853’s build passed, but superseded native jobs were canceled after this correction to avoid repeating a known compile failure. Corrected focused checks preceded final release gates; no native success is claimed from those first attempts. Earlier build26 results/failures remain historical in WORK_LOG.md.

## Limits and exact next task

Automated install-over probes use CI debug signing; matching permanent package/certificate and preserved schemas establish update compatibility. Physical-phone install-over/data/audio retention, the owner’s original music photo, provider/low-storage/landscape/TalkBack and acoustic acceptance remain pending. Stage3 detailed phone acceptance remains pending; Stage4 remains **0/10 documented real sessions**. Original temporary-key0.1.0 is separate.

**Exact next task:** install signed0.14.1/build27 over the existing app, reopen the already-attached photographed page and confirm upright orientation, compact heading, wider image and uninterrupted timer/click. Follow [MANUAL_TESTS.md](MANUAL_TESTS.md), retaining originals separately. No later feature is selected.
