# Current state — Home gear removed, 0.16.0/build48

Updated 2026-10-31 (America/Los_Angeles). **Signed limited owner preview.** The duplicate Goal gear is removed; existing **Edit** opens Manage Goals. There is no general Settings destination; Tools → Audio Settings retains P4 preferences. P1–P3 generally owner-accepted; P4/build48 physical acceptance and full regression pending. P5 has not started.

## Delivery

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code48 |
| Signed source | `8cbe3478c5b05f98ad6720c460d50d0e595d62c5` |
| Preview CI | [41180090915](https://github.com/mrayover/practice.logger/actions/runs/41180090915) — completed/success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `ea76e39070d98ffa1a6134e323d85331c49bff7e1178e81955d4ad9b1bc2cbe3` |
| ZIP SHA256 | `84a5cc97579999f33b00180ec1b8c52e2b41054eed058e2ff20bcf3aee253f33` |

[APK](dist/practice.logger-0.16.0-b48-8cbe347/practice.logger-0.16.0-b48-8cbe347.apk) · [ZIP](dist/practice.logger-0.16.0-b48-8cbe347.zip) · [build info](dist/practice.logger-0.16.0-b48-8cbe347/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b48-8cbe347/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b48-8cbe347/PHONE_CHECKLIST.txt) · [latest](dist/LATEST.txt).

Publisher verifies permanent signature/certificate/app/version/hash; ZIP CRC/member bytes and all 182 prior APK/ZIP files byte-identical. Install over the permanent-key app **without uninstalling or clearing data**. Key/packages remain local and Git-ignored. Final remote main SHA confirmed in chat; later documentation follows signed app source.

## Current behavior and safeguards

Home keeps centered Goals heading, Goal cards and Edit link; only duplicate gear/drawing imports removed. Existing navigation and data unchanged. P4 retains four click timbres, shared last/default Sound+Flash/Sound only/Flash only and four completion sounds plus Off, previews and count-in default4/per-loop override. Both metronomes observe defaults without clock reset; meter/accent remain runtime. /8 BPM counts each eighth; actual display/audio latency uncalibrated. Block cue follows committed armed foreground crossing; standalone countdown alarms once at foreground running zero. Off/capture suppress output without clock changes; previews refuse capture/take playback and stop on leave/background. Explicit recording-click On/Off retained.

Compact transport/exclusive panels, retained PDF page/zoom/Notes drafts, immediate note save/edit/history and pinned controls remain. Cream/green/Lato retained; optional dark mode explicitly deferred. Room11/JSON7 readers1–7/schema/migrations/IDs/media/history remain; JSON excludes reference/take metadata/audio. No Library/storage/analytics changes. Stage3 detailed phone acceptance pending; Stage4 remains0/10 documented sessions.

## Exact verification

- **Build48:** debug/release/test APK compilation, one API35 install/launch smoke and one existing HomeNavigationPreviewTest pass, including Home sections/Edit → Manage Goals/banner navigation/Log save. Screenshot inspected; scope/diff/local links, signed-source equivalence and package integrity checked. No new tests added for this reversible icon removal.
- **Historical P4 build47** source4341af132882e26e24cc3fc85be8d289977b0e6e, [CI41178720243](https://github.com/mrayover/practice.logger/actions/runs/41178720243) completed/success:22 scoped JVM/Robolectric tests and10 API35 Settings/countdown/Notes/metronome/review scenarios, including Settings200%-text/landscape. Not rerun for this icon-only change. Detailed phase evidence retained in WORK_LOG. P4 audio/capture/schema/backup/transport source unchanged by build48.

**Not run:** full unit suite/lint/API26–35 regression matrix, whole-app enlarged-text/landscape/TalkBack, physical-phone install-over/acoustic/flash/mic acceptance or forced-process-restart checklist. P5 and final V1 full gates remain required. General owner acceptance does not infer individual phone results.

## Baseline and publication

Clean local/remote main c262e1324c520dc74a45b691c722aaf58cd7a947 and latest scoped P4 CI41178720243 success verified before changes. Mandatory efficiency procedure/master roadmap reviewed. Owner explicitly requested duplicate gear removal. Existing owner-preview workflow reused; automatic broad CI skipped with `[skip ci]`. No unrelated failure investigation, owner-data edits, uninstall/clear or later phase started.

## One next task

Owner install-over and **build48 PHONE_CHECKLIST.txt**, covering Home Edit access and remaining P4 phone checks. Recommend assigning P5 full pre-Library regression after acceptance; do not start automatically. L1 remains behind P5.
