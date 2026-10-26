# Current state

Owner instruction 2026-10-26: defer manual backup/recovery acceptance and the ten-real-session evaluation until this build cycle finishes. They remain unverified and do not block this authorized 0.3.2 note-reliability increment. General owner report: 0.3.0 works; no model/OS or individual checklist evidence supplied. Feature work stops by 10% usage remaining; prioritize stable signed APK and closing records. The next action for this cycle is to verify and deliver 0.3.2, then resume owner-directed testing/evaluation later.

Updated 2026-10-26 (America/Los_Angeles). Stage 3 engineering complete and verified; owner phone acceptance is pending. Stage 4 ten-session evaluation prepared, 0/10 real sessions recorded. Owner authorized finishing 3 and continuing to 4 after usage reset; no phone model/OS or new acceptance results supplied. Do not mark manual acceptance or real sessions complete without evidence.

## Verified release

- Source/config: main 16075ab6ad07cb6a35c4c5a9b561134076775a04; successful CI https://github.com/mrayover/practice.logger/actions/runs/40407963404 . Closing docs follow this source; verify/report the closing remote SHA separately.
- 0.3.1, versionCode 5; Android 8.0+ (API 26). Stable ID com.mrayover.practicelogger.app and permanent local signing key unchanged. Install over permanently signed 0.2/0.3 without uninstalling; retain data. Temporary-key 0.1 remains separate with no automatic transfer.
- ZIP: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.3.1-b5-16075ab.zip
- APK: C:\Users\mrove\Projects\practice.logger\dist\practice.logger-0.3.1-b5-16075ab\practice.logger-0.3.1-b5-16075ab.apk
- APK SHA-256: 86f3a4e02e1a2b72e280d490720341782b57b856ebdd253d85627b49d026ee10
- Certificate SHA-256: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Signature/package/version/ZIP round-trip hash verified. BUILD_INFO.json and INSTALL.txt accompany APK; LATEST.txt updated; every older release ZIP/folder preserved. Private .signing, .tools and dist ignored; back up existing signing material securely outside Git. Never replace/upload the private key.

## Working features

Offline Lato UI; Room catalog create/edit/search/archive/restore; durable draft selection/order/removal and placement-only durations; focused foreground timer with Start/Pause/Resume, early Complete, Skip, explicit zero/overtime; auto-pause on background/rotation/leave, recover correct current placement/time paused, screen awake only during practice; summaries, notes and reopenable immutable instruction/reference snapshots.

Versioned format-1 JSON export through Android document picker; validated preview/confirmation, transactional restore only into an empty app, no overwrite/merge/reset. Includes archives, saved notes/history/draft/unfinished practice, restores paused. One application-owned Room instance fixes stale Home/catalog/practice state after restore; ViewModels no longer close that shared connection on screen teardown. Independent open() remains available for isolated tests; no schema/data migration required. Validation rejects malformed/types/version/IDs/references/order/status/outcomes/time/overflow/size/nesting and overtime-before-zero. Nonempty restore has a specific refusal; status changes announce politely. Unavailable-provider errors preserve database and allow retry. Explicit platform backup exclusions retain manual-export policy; vector launcher icon added. Full-width duration/remove actions, wrapping reorder controls, heading semantics, distinct placement-aware labels and timer accessibility label including current value. Room schema 2 unchanged, explicit 1→2 migration retained.

## Verification

- Build/unit/lint/debug/release passed in CI. Lint 0 errors, 10 warnings: newer target/dependencies/Gradle plus cosmetic adaptive-icon configuration/monochrome warnings; no toolchain upgrade in this hardening release. Eighteen unit/Robolectric tests: prior catalog/fonts/migration/session/clock and full JSON/database round trip, archived/Unicode/snapshot/notes/draft/override/overtime preservation, invalid-input/no-overwrite/rollback; new unavailable provider export/import plus retry and interrupted-read tests. Local compilation/emulator execution not performed.
- Both Android API 26 and 35 device jobs passed. Each runs catalog/recreation/reopen, three-Block order/override/early complete/skip/pause/background/zero/overtime, force-stop recovery, note/history/snapshot reopen, higher versionCode 6 install-over preserving history; direct backup UI/recreation/confirmation/cancel/no-overwrite/error workflow.
- At system font scale 2.0 both devices pass Home/Builder/Focus/Summary/Library/Editor/Backup workflows, text-layout overflow checks, reachable controls and assistive labels. Real Android system picker export to Downloads and cancellation, import preview/cancel/rotation/confirmed restore into a fresh disposable test app, exact all-record comparison, paused current position/time and history reopen after force-stop pass on both APIs. Only disposable emulator data is cleared. Summary/restored-paused/large-text/backup screenshots reviewed; phone TalkBack and vendor/provider acceptance remain distinct.

## Remaining gates and known limits

Stage 3 engineering is complete, but the plan's owner-phone persistence/recovery acceptance gate has not been supplied for this build. Do not claim dependable daily-use acceptance yet. Phone model/OS/provider, install-over, picker export, spare-empty-install restore/restart comparison, large text and TalkBack remain to record. Synthetic emulator sessions never count toward Stage 4. EVALUATION.md has ten blank real-session rows and no invented pain points or feature decision. Do not add deferred features until actual evidence and approval.

Backup: format 1, 8 MiB / 20,000 records/type / nesting 32; plain text, not encrypted/authenticated. No merge/replace restore; never clear/uninstall the current app to test it. A failed export can leave a partial document: only trust a success-confirmed backup. Process death loses an unconfirmed restore preview; choose it again. Provider outages and read interruption are simulated, not claims of physical full-disk/vendor coverage.

Timing: monotonic foreground deltas, 250-ms checkpoints plus pause flush. Abrupt kill/storage failure may lose latest uncommitted interval plus scheduling/write latency; committed time restores paused without wall/background time. Quick notes save on Complete/Skip; session notes require Save note before backup. No Goals, Routines, metronome/tuner/audio, accounts/cloud/backend or in-app updater/version controls. Future code changes still require a new versioned build.

## Exact next task

Record MANUAL_TESTS.md's 0.3.1 owner-phone acceptance results with model/OS/provider; fix any reliability issue. Then conduct and record EVALUATION.md's ten real practice sessions, use existing optional session notes for friction, and decide on at most one future feature after repeated observations. Stage 4 prepared, not complete. Read all five operational records every development session; update records and publish via GitHub Desktop, monitor usage and stop feature work at 8% remaining to reserve stable APK/tests/records.

Actual checkout C:\Users\mrove\Projects\practice.logger, all Git mutations via GitHub Desktop. No cloud Fetch/Pull synchronization step is needed. dist retains all ZIPs/extracted APKs.
