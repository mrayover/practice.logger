# Current state

Owner-approved scope 2026-10-26: unfinished practice must not block starting new sessions. Implementing always-available Practice Now and individually selectable unfinished sessions on Home; one foreground timer and one builder draft, no new screen/schema/dependency. Version 0.3.3/build 7 verification and delivery pending. Backup format 2 supports multiple unfinished sessions and reads legacy version 1; older apps reject new version 2 backups. Reserve remaining capacity for stable build/logs/push. Manual acceptance/evaluation remain deferred.

Updated 2026-10-26 (America/Los_Angeles). Stage 3 engineering verified; 0.3.2 adds durable unfinished Block notes. Owner generally confirms 0.3.0 works, without model/OS or individual checklist evidence. Owner explicitly deferred manual backup/recovery acceptance and Stage 4 ten-session evaluation until after this build cycle. They are unverified/deferred, not blockers for this authorized increment. No synthetic sessions count toward Stage 4 (0/10).

## Verified release

- App/config source main 3f7730f3eb6a2eba7a8df860951d72d64b15736e; successful CI https://github.com/mrayover/practice.logger/actions/runs/40411796049 . Closing documentation commit follows; verify/report its SHA separately.
- Version 0.3.2, versionCode 6, Android 8.0+ (API 26), stable ID com.mrayover.practicelogger.app. Install over permanently signed 0.2/0.3 to retain data; do not uninstall. Old temporary-key 0.1.0 remains separate, no automatic transfer.
- ZIP: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.3.2-b6-3f7730f.zip
- APK: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.3.2-b6-3f7730f\practice.logger-0.3.2-b6-3f7730f.apk
- APK SHA-256: 6bf7c318af10733b26e3aa65fb5153c9ce24cc68bd7cbb829335bfe5a231335d
- Permanent certificate SHA-256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Signature, package/version and ZIP-extracted hash verified. BUILD_INFO.json / INSTALL.txt accompany APK; LATEST.txt updated. All older ZIP/folders preserved. dist/.tools/.signing ignored. Private signing material unchanged and never uploaded; preserve secure backup outside Git.

## Working features

Offline bundled Lato; Room Block catalog create/edit/search/archive/restore; persistent one-off session builder with order/removal/placement-only durations; monotonic foreground timer, Start/Pause/Resume, early Complete, Skip, explicit zero/overtime; pause on background/rotation/leave and reopen current placement/time paused; summaries, optional notes, recent history with fixed instruction/reference snapshots. No background time counted.

NEW: Save Block note stores the current unfinished placement note without completing/skipping it. Save for later and Back pause/checkpoint and save the note before returning Home; failed writes retain Focus for retry. Reopening reads the saved note. Save feedback only appears for the matching placement and stored text. Future/prior placements and completed history cannot be changed through this API. Existing note column reused, no schema change. Complete/Skip still save final text; session notes still require Save note.

Format-1 JSON export through Android document picker, validated preview/cancel/confirm and transactional empty-app restore. Includes archives, history/snapshots, saved notes, drafts and paused unfinished practice. One application-owned Room instance ensures immediate restored Home/catalog/session state. Independent test connections remain available. Room schema 2 / explicit 1→2 migration retained. Strict type/reference/order/state/time/overflow/size/nesting validation; rollback and no-overwrite protected. Unavailable-provider handling/retry and explicit platform backup exclusions remain. Full-width controls, wrapping reorder labels, headings and spoken timer value support large text/accessibility.

## Verification

- CI build, unit tests, lint, debug and unsigned release packaging passed. Nineteen unit/Robolectric tests passed with zero failures/errors/skips: catalog/fonts/migration/session/clock, backup roundtrip/validation/no-overwrite/rollback/provider failures; new file-database reopen note preservation/clearing/current-placement/history protection. Lint 0 errors / 10 existing warnings (newer tools/dependencies and cosmetic icon configuration). No local compilation/emulator or physical-phone test claimed.
- Both API 26 and 35 device jobs passed: catalog/recreation/reopen; three-Block builder/order/override/early complete/skip/pause/background/zero/overtime; explicit Block-note save, Save for later and Back, restored draft text after actual force-stop, final summary note; history/snapshots/process reopen and higher versionCode 7 install-over retaining data.
- Existing direct backup and actual Android system picker export/cancel/preview/rotation/empty-app confirmed restore/exact-record comparison/immediate Home/reopen tests pass on both APIs; only disposable emulator app data cleared. 200% font-size Home/Builder/Focus/Summary/Library/Editor/Backup workflows and drawn-text bounds checks pass. Real owner phone/vendor/TalkBack coverage remains distinct and deferred.

## Known limits and unfinished work

Typing alone is not a durable write. Use Save Block note, Save for later, Back, Complete or Skip; abrupt kill before saving can lose edits. Session notes require explicit Save note. Saved note edits are included in JSON export. Abrupt kill/storage failure may lose the latest uncommitted 250-ms timer interval plus scheduling/write latency; committed time always restores paused without wall-time inflation.

Backup format 1 is plain text, 8 MiB / 20,000 records per type / nesting 32. Empty-target restore only; no overwrite/merge/reset. Failed provider export may leave an incomplete file; trust success-confirmed backups only. Unconfirmed restore preview is lost on process death. Simulated provider failures are not physical full-disk/vendor tests. Phone model/OS/provider and detailed acceptance are not supplied. Stage 4 remains 0/10 real sessions; no future-feature decision invented.

Goals, Routines, audio, metronome/tuner, deep analytics, accounts/cloud/backend and in-app updater remain deferred. Code changes require a new versioned APK. No unfinished feature controls added.

## Exact next task

Owner test 0.3.2 during normal practice: save an unfinished Block note, leave/reopen, restart and finish, then confirm the note in Summary. Fix any reported issue before expanding scope. Manual backup/recovery acceptance and EVALUATION.md's ten-session evaluation remain deferred until the owner resumes them; do not repeat them as prerequisites for this completed build cycle or claim completion.

Read all five operational records each session, append logs and refresh state, publish intended changes through GitHub Desktop. Latest owner cutoff: stop feature work by 10% remaining and prioritize stable APK/tests/records. Feature work froze with 29% remaining; closing check 21% remaining. Actual checkout C:\Users\mrove\Projects\practice.logger; all Git mutations use GitHub Desktop, no cloud synchronization required. Preserve every dist ZIP/folder and existing key.