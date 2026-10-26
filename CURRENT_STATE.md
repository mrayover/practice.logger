# Current state

Updated: 2026-10-26, 10:40 PDT (America/Los_Angeles).

## Verified delivery

Stage 0 and Stage 1 are complete in CI. Owner phone acceptance is pending. This is an offline Block catalog test release, not the complete practice-session application.

- Branch: main. Verified app-source commit: d9b83548b2fa0e17afb68aea73a211266b1dd955 (pushed with GitHub Desktop and verified remotely).
- Final CI: https://github.com/mrayover/practice.logger/actions/runs/40370047278 — build and device jobs both successful.
- Installable debug APK (0.1.0, versionCode 2, Android 8+): https://github.com/mrayover/practice.logger/actions/runs/40370047278/artifacts/12671548458 . Download ZIP and extract app-debug.apk. Artifact retention: 30 days.
- Stage 0 evidence: https://github.com/mrayover/practice.logger/actions/runs/40367248107 (main c3c8e8f5b71a34ef3e68cebd6ce16be59f062979), including launch/recreation smoke test and foundation APK artifact 12671091904.
- The closing documentation-only commit follows the source commit above and changes no app behavior. CI deliberately ignores Markdown-only commits; use the verified source run above for the delivered APK. Verify and report the closing commit SHA after its Desktop push.

## Working functionality

Home shows the real active-Block count and opens Manage Blocks or New Block. Block Library lists and searches Active or Archived Blocks. Block Editor requires Name and Action; defaults to five minutes; accepts positive whole duration and optional numeric BPM plus notes/source. Save creates/updates Room records transactionally. Cancel/Android Back confirms discarding changed drafts. Drafts survive Activity recreation/saved-state restoration. Save outcomes remain in the ViewModel across recreation. Storage failure retains a draft and shows an error.

Archive/restore is confirmed and reversible. No hard-delete control/API exists: stable IDs and created timestamps are preserved for future history. Archive with unsaved edits explicitly discards those edits after confirmation. Search matches name/action/notes literally, ignoring case. All app-controlled Material text styles and XML theme use bundled Lato Regular/Bold; license retained in APK assets. No Internet permission, accounts or service.

## Verified tests

- ./gradlew testDebugUnitTest lintDebug assembleDebug — success in final build job. Six targeted JUnit/Robolectric tests cover every Material typography style, file database close/reopen, transactional create/edit/archive/restore, stable IDs/timestamps, invalid create/edit rejection, numeric boundaries and literal/archive-aware search.
- API 35 Android emulator: installed/launched app; created a Block through UI, displayed required-field errors, retained draft during recreation, edited duration, searched, archived/restored, kept/discarded edits; recreated after transitions. Full catalog test passed.
- Same installed app force-stopped, then second instrumentation process launched Home, reopened the saved Block, and verified action and edited duration. Direct instrumentation reported OK (1 test); no reinstall/clear between phases.
- Initial SDK setup failure and invalid restart harness were corrected; details remain in WORK_LOG.md. Final CI is green. Local Gradle/device testing not performed: local JDK/SDK were not found. Physical phone testing remains pending; no claim of phone acceptance or visual approval.

## Known limitations and remaining scope

No Session Builder, practice timer, session recovery, history, backup/restore or stable release signing yet. No Goals, Routines, metronome, tuner, recording or advanced features. Practice Now and other future controls are intentionally absent until they work. Provisional restrained light-theme design; phone text scaling, keyboard usability and visual approval need owner checks. CI debug keys can differ between runs, so updates may fail signature matching; do not uninstall an app containing needed data. Avoid irreplaceable data before stable signing/backup. Unsaved drafts are not saved Blocks and can be lost on force-stop. A read failure asks the user to close/reopen the app. Migration baseline version 1 is committed; future schema changes require migrations, never destructive fallback.

## Repository and next task

Actual Windows checkout: C:\Users\mrove\Projects\practice.logger. All Git mutations this session used GitHub Desktop. Original prefixed plan/handoff and commit history remain intact. Canonical operational records were created because they were absent initially. No cloud checkout was used; this folder is already updated and does not need Fetch/Pull to receive this session's work.

Exact next task: run the Stage 1 checklist in MANUAL_TESTS.md on the owner's Android phone, record model/Android version/results, and resolve any catalog defects before beginning Stage 2 Session Builder. Read all five operational records first in every future session. Append WORK_LOG and refresh this state at every session close, including incomplete sessions.
