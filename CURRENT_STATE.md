# Current state

Updated: 2026-10-26 (America/Los_Angeles). Stage 2 practice loop delivered; owner phone acceptance of 0.2.0 is pending. Owner reports the earlier 0.1.0 catalog works, with no model/OS or individual checklist results supplied.

## Verified source and local build

- main app source: 91f4129c4f186b4d95b99d1ab8244cc52c34af0c. Final CI https://github.com/mrayover/practice.logger/actions/runs/40378217900 passed build and device jobs. Closing documentation follows this source and does not alter the APK.
- Version 0.2.0, versionCode 3; stable application ID com.mrayover.practicelogger.app; launcher practice.logger 0.2; Android 8.0+.
- ZIP: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.2.0-b3-91f4129.zip
- Extracted APK: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.2.0-b3-91f4129\practice.logger-0.2.0-b3-91f4129.apk
- APK SHA-256: 0f23a7b81f76063bb08ae26c070b3d1e0b947d9df35f6839a613d77daa8d0c79
- Permanent certificate SHA-256: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830
- BUILD_INFO.json and INSTALL.txt are beside the APK. ZIP extraction checksum matched. Old 0.1.0 ZIP/folder retained as practice.logger-0.1.0-d9b8354; original downloads also preserved.

## Working behavior

Stages 0/1 catalog remain functional with bundled Lato and transactional Room persistence. Stage 2 adds durable draft sessions, Block selection and shared create editor, ordered placements, removal and placement-only duration overrides; focused countdown, Start/Pause/Resume, early Complete, Skip, explicit zero-time Continue into overtime; auto-pause on background/rotation/leaving Focus and screen-awake only during active practice; automatic completed summaries, optional Block/session notes and reopenable Recent Practice history. Each next Block starts paused. Only an actual unfinished session exposes Continue Session.

Room version 2 stores session/placement records and immutable instruction/reference snapshots, retaining stable Block IDs. Explicit 1→2 migration preserves catalog data; schemas 1/2 retained. Editing/archiving a source Block does not rewrite history. Draft selection/order/overrides and active-session recovery survive restart.

## Verified checks

- Final CI ran testDebugUnitTest lintDebug assembleDebug assembleRelease successfully. Thirteen unit/Robolectric tests cover catalog validation/persistence/fonts, migration, draft order/removal/overrides, snapshots, three-Block outcomes and reopen, timer boundaries/overtime/rounding/overflow and completed-history protection.
- API 35 emulator: catalog create/edit/search/archive/restore/discard/recreation; catalog process reopen; three-Block selection/reorder/override and unchanged default; early Complete, Skip, pause/background without time inflation, explicit zero/overtime; interrupted-session process recovery and completion; saved summary/note/history snapshots after editing/archiving the original Block; another process restart.
- Higher versionCode 4 test APK installed over the versionCode 3 test app and reopened unchanged history successfully. This tests Android install-over persistence with a consistent CI key. Delivered local release APK has independently verified permanent signature, pinned certificate, package/version and ZIP round-trip hash.
- Home/Builder/Focus/zero-choice and paused-screen emulator screenshots were reviewed. The attempted Summary screenshot captured the preceding frame; Summary behavior passed semantic tests, but its visual review remains part of owner phone checks. Real-phone Stage 2 usage, scaling/accessibility and update installation remain pending. Local compilation/emulator execution was not performed; local tools are Java runtime and APK signing/inspection utilities only.

## Update delivery and limits

The old 0.1.0 key was temporary/lost. It cannot be used for an in-place update. Safe side-by-side transition preserves that installed app and Blocks; there is **no automatic transfer** into the new app. Future dist APKs use the permanent local key and increasing versionCode to update the new installation in place. Do not install temporary-key CI debug APKs over it. New code still requires a build; no network updater/backend was added.

dist/, .tools/ and .signing/ are ignored. Private signing key/password stay in .signing and must be securely backed up outside Git; only the public fingerprint is tracked. Use scripts/Publish-LocalApk.ps1 for future verified unsigned CI releases, preserve prior versions and verify certificate/package/version/hash. Losing this permanent key prevents in-place updates.

Time uses monotonic foreground deltas with 250-ms checkpoints and pause flush. Recovery is always paused and excludes wall/background time. Abrupt kill or disk failure can lose the most recent uncommitted interval plus scheduling/write latency. Optional quick notes are saved on Complete/Skip; session notes require Save note. No backup/restore yet; avoid irreplaceable data. No Goals, Routines, metronome, tuner, recording or deferred advanced functionality. Stage 3 hardening remains; daily-use reliability is not yet claimed.

## Next task and continuity

Exact next task: perform/record the Stage 2 phone workflow in MANUAL_TESTS.md, resolve practice-loop issues, then begin Stage 3 versioned backup/export and safe restore. Record phone model/OS. Read the five operational records before each session; append WORK_LOG and update this state even for incomplete sessions.

Actual checkout is C:\Users\mrove\Projects\practice.logger; all Git mutations used GitHub Desktop. No cloud checkout or local Fetch/Pull sync step is required. Report and remotely verify the closing documentation commit SHA after push.
