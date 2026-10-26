# Current state

Owner-approved scope 2026-10-26: unfinished practice must not block starting new sessions. Implemented always-available Practice Now and individually selectable unfinished sessions on Home; one foreground timer and one builder draft, no new screen/schema/dependency. Version 0.3.3/build 7 verification and signed delivery complete. Backup format 2 supports multiple unfinished sessions and reads legacy version 1; older apps reject new version 2 backups. Reserve remaining capacity for stable build/logs/push. Manual acceptance/evaluation remain deferred.

Updated 2026-10-26 (America/Los_Angeles). Stage 3 engineering verified; 0.3.3 adds independent new and unfinished sessions, retaining 0.3.2 note improvements. Owner generally confirms 0.3.0 works, without model/OS or individual checklist evidence. Owner explicitly deferred manual backup/recovery acceptance and Stage 4 ten-session evaluation until after this build cycle. They are unverified/deferred, not blockers for this authorized increment. No synthetic sessions count toward Stage 4 (0/10).

## Verified release

- App/config source main 19489c7fd8f947b0a4d194d1f7a6031ab62f478d; successful CI https://github.com/mrayover/practice.logger/actions/runs/40417269518 . Closing documentation commit follows; verify/report its SHA separately.
- Version 0.3.3, versionCode 7, Android 8.0+ (API 26), stable ID com.mrayover.practicelogger.app. Install over permanently signed 0.2/0.3 to retain data; do not uninstall. Old temporary-key 0.1.0 remains separate, no automatic transfer.
- ZIP: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.3.3-b7-19489c7.zip
- APK: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.3.3-b7-19489c7\practice.logger-0.3.3-b7-19489c7.apk
- APK SHA-256: 02e247076ef8d43c0ad5d010e3a6ce6ec475ea18b4ff6fe7e29c8965c7e9aabd
- Permanent certificate SHA-256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Signature, package/version and ZIP-extracted hash verified. BUILD_INFO.json / INSTALL.txt accompany APK; LATEST.txt updated. All older ZIP/folders preserved. dist/.tools/.signing ignored. Private signing material unchanged and never uploaded; preserve secure backup outside Git.

## Working features

Offline bundled Lato; Room Block catalog create/edit/search/archive/restore; persistent one-off session builder with order/removal/placement-only durations; monotonic foreground timer, Start/Pause/Resume, early Complete, Skip, explicit zero/overtime; pause on background/rotation/leave and reopen current placement/time paused; summaries, optional notes, recent history with fixed instruction/reference snapshots. No background time counted.

NEW: Save Block note stores the current unfinished placement note without completing/skipping it. Save for later and Back pause/checkpoint and save the note before returning Home; failed writes retain Focus for retry. Reopening reads the saved note. Save feedback only appears for the matching placement and stored text. Future/prior placements and completed history cannot be changed through this API. Existing note column reused, no schema change. Complete/Skip still save final text; session notes still require Save note.

Format-2 JSON export (also imports legacy format 1) through Android document picker, validated preview/cancel/confirm and transactional empty-app restore. Includes archives, history/snapshots, saved notes, drafts and paused unfinished practice. One application-owned Room instance ensures immediate restored Home/catalog/session state. Independent test connections remain available. Room schema 2 / explicit 1→2 migration retained. Strict type/reference/order/state/time/overflow/size/nesting validation; rollback and no-overwrite protected. Unavailable-provider handling/retry and explicit platform backup exclusions remain. Full-width controls, wrapping reorder labels, headings and spoken timer value support large text/accessibility.

## Verification

- CI build, unit tests, lint, debug and unsigned release packaging passed. Twenty unit/Robolectric tests passed with zero failures/errors/skips: catalog/fonts/migration/session/clock, backup roundtrip/validation/no-overwrite/rollback/provider failures; new file-database reopen note preservation/clearing/current-placement/history protection. Lint 0 errors / 10 existing warnings (newer tools/dependencies and cosmetic icon configuration). No local compilation/emulator or physical-phone test claimed.
- Both API 26 and 35 device jobs passed: catalog/recreation/reopen; three-Block builder/order/override/early complete/skip/pause/background/zero/overtime; explicit Block-note save, Save for later and Back, restored draft text after actual force-stop, final summary note; history/snapshots/process reopen and higher versionCode 8 install-over retaining data.
- Existing direct backup and actual Android system picker export/cancel/preview/rotation/empty-app confirmed restore/exact-record comparison/immediate Home/reopen tests pass on both APIs; only disposable emulator app data cleared. 200% font-size Home/Builder/Focus/Summary/Library/Editor/Backup workflows and drawn-text bounds checks pass. Real owner phone/vendor/TalkBack coverage remains distinct and deferred.

## Known limits and unfinished work

Typing alone is not a durable write. Use Save Block note, Save for later, Back, Complete or Skip; abrupt kill before saving can lose edits. Session notes require explicit Save note. Saved note edits are included in JSON export. Abrupt kill/storage failure may lose the latest uncommitted 250-ms timer interval plus scheduling/write latency; committed time always restores paused without wall-time inflation.

Backup format 2 (legacy format 1 readable) is plain text, 8 MiB / 20,000 records per type / nesting 32. Empty-target restore only; no overwrite/merge/reset. Failed provider export may leave an incomplete file; trust success-confirmed backups only. Unconfirmed restore preview is lost on process death. Simulated provider failures are not physical full-disk/vendor tests. Phone model/OS/provider and detailed acceptance are not supplied. Stage 4 remains 0/10 real sessions; no future-feature decision invented.

Goals, Routines, audio, metronome/tuner, deep analytics, accounts/cloud/backend and in-app updater remain deferred. Code changes require a new versioned APK. No unfinished feature controls added.

## Exact next task

Owner test 0.3.3: leave session A unfinished, start B through Practice Now, save and switch between A/B, restart and confirm both remain selectable with independent notes/time, then complete either without affecting the other. Fix any reported issue before expanding scope. Manual backup/recovery acceptance and EVALUATION.md's ten-session evaluation remain deferred until the owner resumes them; do not repeat them as prerequisites for this completed build cycle or claim completion.

Read all five operational records each session, append logs and refresh state, publish intended changes through GitHub Desktop. Latest owner cutoff: stop feature work by 10% remaining and prioritize stable APK/tests/records. This increment froze feature work with 17% remaining; closing check 10% remaining. Actual checkout C:\Users\mrove\Projects\practice.logger; all Git mutations use GitHub Desktop, no cloud synchronization required. Preserve every dist ZIP/folder and existing key.
## Independent sessions — 0.3.3

Practice Now always opens the single builder draft, even with unfinished sessions. Home lists every in-progress session separately; Continue Session opens the selected/default session, other dated controls select specific sessions. Each retains current Block, time, outcomes and notes. Only one foreground timer runs; switching pauses/checkpoints and opens paused. Selection survives Activity recreation through SavedStateHandle; a cold force-stop may default to the newest unfinished session, and every other session remains individually selectable. One draft remains; no delete/abandon or naming UI added.

Room schema 2 and signing identity unchanged. JSON backup writer uses format 2 for multiple unfinished sessions plus at most one draft; reader accepts legacy format 1. Older apps reject version 2 backups, so restore those with 0.3.3 or later. Unit test round-trips two active sessions and a draft and reopens source DB; both API26/35 device suites create A/B, switch, force-stop/reopen both, and complete independently with notes retained. Twenty unit tests and 13 instrumentation phases per API passed; build/lint/debug/release passed. Owner phone checks/evaluation stay deferred.
