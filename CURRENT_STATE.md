# Current state — P4 loop spacing correction, 0.16.0/build52

Updated 2026-10-31 (America/Los_Angeles). **Signed scoped debug preview.** Owner phone screenshot exposed clipped digits and unused space below Record Loop in build51. Build52 corrects numeric field sizing, spreads loop setup proportionally through its viewport and anchors the recording/review footer directly above the lower transport. Focused compact/tall checks pass; confirmation on the owner’s phone remains pending. P5 and Media Library have not started.

## Delivery

| Item | Verified value |
|---|---|
| Version / variant | 0.16.0 / code52 / debug |
| APK source | `1dbaa4d25d903c091908012ad670e328ba3ca177` |
| Focused CI | [41234666560](https://github.com/mrayover/practice.logger/actions/runs/41234666560) — completed/success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `9152dc3a143e83f6e59bd41193820e9aa9abec8457147f22d15e47e904998c80` |
| ZIP SHA256 | `1ab4dcd3979ac95d44bce805b7359292d258398f13939565a7543aceaa230d82` |

[Debug APK](dist/practice.logger-0.16.0-b52-1dbaa4d/practice.logger-0.16.0-b52-1dbaa4d.apk) · [ZIP](dist/practice.logger-0.16.0-b52-1dbaa4d.zip) · [Build info](dist/practice.logger-0.16.0-b52-1dbaa4d/BUILD_INFO.json) · [Verification](dist/practice.logger-0.16.0-b52-1dbaa4d/VERIFICATION_STATUS.txt) · [Phone checklist](dist/practice.logger-0.16.0-b52-1dbaa4d/PHONE_CHECKLIST.txt) · [Latest](dist/LATEST.txt).

Existing publisher re-signed the tested CI debug variant with the permanent key; all non-signature app members match. Signature/certificate/package/version/debuggable flag, APK hash and ZIP CRC/member bytes verified. All 198 prior APK/ZIP hashes preserved. Install over the permanent-key app **without uninstalling or clearing data**. Packages/keys stay local and ignored. Later documentation commits follow APK source.

## Behavior and safeguards

The three numeric fields use intrinsic sizing with a64dp minimum rather than a forced56dp height. The inline form receives its existing viewport height and distributes spare space equally between fields, shortcut rows, duration and the recording footer. Record Loop/Keep/Re-record and the small budget/Preview/Discard row stay together at the transport boundary. Existing overflow behavior remains available; no new scrolling container is added.

Build51’s eight shortcuts, validation, permanent loop recording click, count-in, automatic completion, temporary review/explicit Keep, re-record/preview/discard and aligned Free/Loop action columns remain. Free review, global metronome and bottom practice actions retain their behavior. This follow-up changes presentation only: no RecordingViewModel/sample engine/storage changes. Cream/green/Lato, retained PDF/Notes/workspace guards, Room11/JSON7/readers1–7, IDs/history/media and saved recordings remain. JSON still excludes references/take metadata/audio; dark mode remains deferred.

## Exact verification and limits

- Final focused CI compiles debug/release/instrumentation APKs; seven existing phrase timing/storage unit checks and one expanded API35 native path pass.
- Native assertions cover compact1080x1920 and tall1080x2340 screens: numeric text line height, no scrollbar, proportional gaps, footer against the transport boundary, field/action/footer alignment; then the existing count-in/permanent guide/exact WAV/Preview/Stop/Re-record/review collapse/Keep/Discard/free-review flow. Screenshots inspected.
- Build51’s earlier emulator pass did not establish fit on the owner’s phone; their screenshot is the evidence for this correction. Other affected legacy test callers/TransportPreviewTest remain updated/compiled rather than executed as broader workflows. No full regression, lint/API matrix, enlarged-text native run or physical-phone/acoustic acceptance is claimed.
- Prior P1–P4 evidence remains historical in WORK_LOG. P5/final gates remain unassigned/unrun; Stage3 detailed phone checks pending, Stage4 remains0/10 documented sessions.

## Exact next task

Owner install-over build52 on the same phone and confirm fully visible digits, proportional setup spacing and Record Loop/review footer immediately above the transport, using its PHONE_CHECKLIST.
