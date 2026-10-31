# Current state — P4 loop recorder refinement, 0.16.0/build51

Updated 2026-10-31 (America/Los_Angeles). **Signed scoped debug preview.** Compact loop setup, permanent loop click, inline review and aligned recording actions implemented. Focused checks pass. Physical-phone fit/acoustic acceptance remains pending. This is a bounded P4 refinement; P5 and Media Library have not started.

## Delivery

| Item | Verified value |
|---|---|
| Version / variant | 0.16.0 / code51 / debug |
| APK source | `b81ecb5a1ff97fcf20fd3895f8e48469f6ed24a2` |
| Focused CI | [41232807446](https://github.com/mrayover/practice.logger/actions/runs/41232807446) — completed/success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `943dfbcbbe0f9875a561dd41a45a5f88ea5aabc439047acc7217ff6c2d40e963` |
| ZIP SHA256 | `3de7b52f547205187df6dd854ca562e173259367e3b281d126452cbb68a8ecef` |

[Debug APK](dist/practice.logger-0.16.0-b51-b81ecb5/practice.logger-0.16.0-b51-b81ecb5.apk) · [ZIP](dist/practice.logger-0.16.0-b51-b81ecb5.zip) · [Build info](dist/practice.logger-0.16.0-b51-b81ecb5/BUILD_INFO.json) · [Verification](dist/practice.logger-0.16.0-b51-b81ecb5/VERIFICATION_STATUS.txt) · [Phone checklist](dist/practice.logger-0.16.0-b51-b81ecb5/PHONE_CHECKLIST.txt) · [Latest](dist/LATEST.txt).

The tested CI debug variant is re-signed with the permanent key; all non-signature app members match. Signature/certificate/package/version/debuggable flag and hashes verified; ZIP CRC/member bytes checked and all 194 previous APK/ZIP files unchanged. Install over the permanent-key app **without uninstalling or clearing data**. Artifacts/keys remain local and ignored. Later documentation commits follow the APK source.

## Behavior and safeguards

Loop setup keeps BPM, loop beats and count-in on one row, manual validation and duration; eight shortcuts stay in two rows. Instructional paragraphs and recording-click toggle are removed. While loop setup is open, redundant discard confirmation and time-reached guidance are omitted to retain viewport space; actual errors, timer state and Continue remain. Loop capture always uses its existing phrase click, with count-in, automatic completion, sample timing and low-level engine unchanged. Free recording and global metronome settings remain separate.

Record Loop uses theme primary green/onPrimary. A completed loop replaces it in place with Keep/Re-record; compact Preview/Stop and Discard share the budget row. Existing save/preview/re-record/discard operations retain temporary ownership, retryable save and saved-file guards. Keep/Discard restores Record Loop. Workspace collapse retains pending review and its pinned save/reopen access; expanded loop review has a single action set. Free review remains separate even when capture starts with loop setup open.

Free/Loop rows share fixed action and Play columns, spacing and heights; take labels/dropdowns retained. Bottom practice pills keep their existing aligned heights. Cream/green/Lato, prior P4 metronome/workspace behavior, Notes and PDF viewer state remain. Room11/JSON7/readers1–7, IDs/history/media and underlying sample engines unchanged.

## Exact verification and limits

- Debug/release/instrumentation compilation, seven focused phrase audio/storage unit checks and one API35 native loop UI/audio/review scenario pass in the final CI above. Normal portrait loop setup/review visibility and action/field/footer geometry checked without scrolling; screenshots inspected.
- Native capture verifies selected count-in, guide output during capture, automatic completion and exact WAV frame count; explicit Keep/Discard/Re-record and free-review separation preserve saved audio.
- Initial scoped attempts/results and repairs are recorded in WORK_LOG. Other affected native callers/TransportPreviewTest were updated and compiled, not executed as broader workflows. No full regression, lint/API matrix, enlarged-text native run or physical-phone/acoustic acceptance is claimed.
- Existing P1–P3 general owner acceptance and P4 Settings/metronome evidence remain historical facts in WORK_LOG. P5/final regression gates remain unassigned/unrun. Stage3 detailed phone acceptance pending; Stage4 remains0/10 documented sessions. JSON still excludes references and all take metadata/audio; no media-inclusive backup. Dark mode remains deferred.

## Exact next task

Owner install-over build51 and execute its PHONE_CHECKLIST at the target phone’s normal display settings, especially full setup/review fit and physical loop-click/count-in timing. No further feature phase is implied.
