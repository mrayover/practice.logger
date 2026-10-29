# Permanent development instructions

## Read and verify first

Read README.md, CURRENT_STATE.md, BUILD_PLAN.md and WORK_LOG.md before implementation. Review and follow the [mandatory efficiency procedure](WORK_LOG.md#2026-10-28--mandatory-efficiency-procedure-for-all-future-work); confirm that review in the initial work plan. Inspect local changes, branch, remote, current version and CI before editing. Reuse the existing repository and working architecture.

The authoritative delivery/verification record is CURRENT_STATE.md. BUILD_PLAN.md records current approved scope and future boundaries. WORK_LOG.md contains release history and session records. FIRST_WORK_HANDOFF.md is the current entry point for a new worker. Prefixed planning documents are historical references, not current instructions. App source/package provenance may precede later documentation commits; report the distinction.

## Repository and Git

Use the existing checkout `C:\Users\mrove\Projects\practice.logger`; preserve user changes, commit history and unrelated work. Do not create a replacement repository. Files under a ChatGPT project's synced `sources/` are read-only references.

GitHub Desktop is the owner's default for Git mutations. The owner's explicit Git-command/commit/push authorization in the current chat covers Increment 2 implementation, tests, documentation and signed delivery. This is a current-chat exception, not a permanent change to the Desktop preference. Honor later direct owner authorization and mandatory platform authentication/security approvals.

At the end of every session, including incomplete work: record factual work/results in WORK_LOG.md, refresh CURRENT_STATE.md with verified status/limitations and one exact next task, update related records, publish intended changes through the authorized Git workflow and verify the actual remote SHA/branch. Report anything not committed, pushed, built or tested. If work happens in a different checkout, explain the real sync step; do not claim the Windows checkout changed unless it did.

## Current app and data rules

Daily Log/Weekly View shipped in 0.13.0/build25; the authorized image increment is 0.14.0/build26; consult CURRENT_STATE.md for actual delivery/verification status. Blocks, independent sessions/history, integrated click, Block and standalone recordings, phrase loops, Tools/tuner/timers, saved Routines, Goals and safe permanent Block deletion are implemented. Do not restart Stage 0 or treat those capabilities as deferred. Stage 3 detailed phone acceptance is pending; Stage 4 remains 0/10 documented real sessions. New work requires an actual owner request or approved scope in BUILD_PLAN.md.

Keep Kotlin, Jetpack Compose, Room, Lifecycle and bundled Lato Regular/Bold throughout app-controlled text. Preserve the current cream/green theme, navigation and functioning controls. No destructive database fallback; retain schema exports and explicit data-preserving migrations.

Block deletion is allowed only when there are zero Goal links, saved Routine references, attached takes and session placements of every status. Reference images also block deletion and require deliberate confirmed removal first. Count persistent data and recheck/delete in one transaction after confirmation. Preserve foreign-key restrictions and explicitly inspect session sourceBlockId references. Never detach associations, remove recordings or rewrite completed history automatically. No long-press or historical-reference redesign is authorized by this feature.

Room schema is 10; explicit 9→10 only adds block_images. Retain schemas/migrations1–9. JSON writer is 6, readers 1–6, with atomic restore into a completely empty target. Keep relationship validation, including historical source Block references. JSON excludes all images/image metadata/paths/URIs and take metadata/labels/audio. Keep originals separately; restore creates no image metadata. Retain per-take export; do not claim full audio portability. Preserve existing practice data, audio, IDs and migrations.

Daily Logs are independent date-keyed plain text; keep date captured with every queued edit, flush before date/navigation/background transitions, and retain failed drafts for retry. Use local calendar boundaries. Weekly views derive from daily_logs; no separate weekly persistence or practice relationships. Backup validates canonical dates and protects Log-only targets.

One reference image per saved Block: stable Block-keyed RESTRICT table, generated private filenames, bounded validated copy/decoding on IO, old file retained until replacement metadata commits, shared file mutex and conservative orphan recovery. Archive preserves attachments; Focused Practice resolves sourceBlockId without changing textual snapshots. Viewer UI state must not call practice timing/metronome or recording lifecycle. Recording-sheet integration, PDFs, multiple media and media-inclusive backups remain outside Increment 2.

## Verification and delivery

Use targeted tests/diagnostics before full release checks; keep assertions and investigate concrete evidence. Distinguish a signed candidate, compilation/unit/lint success, native automated workflow results and owner phone acceptance. A failing legacy test does not establish a pre-existing defect or a regression without evidence. Record failures and cancellations honestly. Current exact API 35 limitations are in CURRENT_STATE.md.

For an app increment, deliver an installable versioned APK and ZIP with provenance and a short phone checklist. Increment versionCode for a changed distributed update. Use scripts/Publish-LocalApk.ps1 and the existing permanent key; verify package/version, pinned public certificate, signature and ZIP/APK integrity. Stable app ID: com.mrayover.practicelogger.app. Preserve all existing dist packages. Install updates over the permanent-key app without uninstalling. Original temporary-key 0.1.0 remains separate, with no automatic Block transfer.

Never commit APKs, dist, build outputs, SDKs, tool downloads, local.properties, keys, passwords or machine-specific active configuration. Keep dist/, .tools/ and .signing/ ignored. Preserve and securely back up permanent signing material; never replace it or publish credentials. Documentation-only maintenance needs consistency/link/provenance checks, not a new APK or redundant emulator matrix.

## Record maintenance

Current operational documents must describe the current app, not accumulate contradictory old “current states,” obsolete install paths or completed next tasks. Keep current delivery paths in CURRENT_STATE.md and dist/LATEST.txt; other records should link to that authority. Record actual source/CI evidence and uncertainty; do not invent phone results or resolve ambiguous future decisions.

The owner explicitly requested consolidation on 2026-10-28, superseding the former append-only/preserve-every-entry rule for that cleanup. The complete pre-cleanup records remain in Git history at 84565ea. Retain meaningful release facts and the mandatory efficiency procedure. After this consolidation, append concise factual session entries; further broad historical cleanup requires an owner request. Avoid repeating entire release narratives across every file.
