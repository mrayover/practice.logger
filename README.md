# practice.logger

An offline Android practice app built with Kotlin, Jetpack Compose, Room and locally bundled Lato. Current signed release: **0.13.0/build 25**, with Daily Log and Weekly View.

## Install and verify

Use the current APK or ZIP linked in [CURRENT_STATE.md](CURRENT_STATE.md). That file is the single authority for delivery paths, source SHA, signature/hash, actual CI results and known limitations. The local distribution pointer is [dist/LATEST.txt](dist/LATEST.txt). Packages are local, ignored by Git, and earlier releases remain preserved.

Android 8.0+; install over the existing permanent-key app **without uninstalling** to retain data/audio. Package ID: `com.mrayover.practicelogger.app`. Original temporary-key 0.1.0 is a separate app with no automatic data transfer. CI debug APKs use a temporary key and cannot update the permanent-key distribution.

## Using the app

- **Blocks:** create/edit with required Name/Action, default duration and optional tempo/notes. Search name/action/notes literally, switch Active/Archived, Archive/Restore, or select Delete Permanently in a saved Block's editor.
- **Safe deletion:** unused active or archived Blocks can be deleted after confirmation. Goal links, saved Routines, recordings and all draft/unfinished/completed session entries protect the Block. Refusal shows persistent counts and explains completed history. Associations/audio/history are never removed automatically; use Archive when dependencies remain.
- **Practice Now:** build a session, reorder placements and override their durations, or select an unfinished session. Starting fresh retains older unfinished practice and the shared builder draft. Focus starts paused; Start/Resume, Pause, Complete and Skip record foreground active time. At zero, choose Continue for overtime or Complete. Save for later/Back saves and pauses; restart resumes saved practice paused.
- **Notes and history:** Quick Block notes save via Save Block note, Save for later, Back, Complete or Skip. Session notes require Save note. Routines retains Recent Practice and reopenable summaries with historical instruction snapshots.
- **Saved Routines:** reusable ordered Block references with independent durations, create/edit/reorder/delete, builder Save as Routine, and independent fresh starts. Archived or unavailable references prevent starting until repaired/restored.
- **Goals:** up to five active Goals, manual percentage/notes/outcome, priority, top-two Home progress bars, completion/reopen, shared Block links and Goal practice. Displayed practice time uses actual explicitly attributed sessions; it does not automatically set progress.
- **Recordings:** each Block retains multiple free M4A takes and fixed-phrase WAV loops; Recordings is available in practice and the saved Block editor, including archived Blocks. Record phrase supports 30–300 BPM, 4/8/16/32 beats, four count-in clicks and optional Click while recording (on by default). Takes support playback, rename, confirmed deletion and per-take export. Recording also provides standalone Free recording and Saved free recordings without creating a synthetic Block. Audio stops on leave/background and never restarts automatically.
- **Log:** Home → Log opens today in your local calendar. Write freely; text saves automatically. Wait for Saved. Choose date to revisit and edit any day. Weekly View displays Monday–Sunday from the same entries; Previous/Next week navigate, and tapping a day opens its editor. Back returns from the week to the daily editor; Back to Home saves before leaving. If saving fails, the text stays in the editor for Retry. Returning on another day opens that day without changing earlier text.
- **Tools:** standalone metronome, chromatic microphone tuner, stopwatch/countdown and Backup and restore. The practice click and Block timer are independent. The green banner, cream surfaces, Lato and prominent Back to Home controls are shared throughout app screens.

## Backup and important limits

Tools → Backup and restore exports **format-6 UTF-8 JSON**, reading versions **1–6**. It protects independent dated Logs, Blocks, Routines, Goals, sessions, placements, notes and saved recovery state. A Log-only installation is nonempty and protected from restore. Restore validates references/state and inserts atomically into a completely empty app; it never merges or replaces existing data. Use a spare empty installation for restore testing.

**JSON excludes every recording, take label and take metadata row. Export wanted takes individually.** There is no full audio-inclusive restore/import. Wait for Backup saved; failed exports may leave incomplete documents. Save notes before exporting. See [BACKUP_FORMAT.md](BACKUP_FORMAT.md).

Only committed foreground practice time is recovered. Abrupt death/storage failure can lose the latest uncommitted timer interval or unsaved text. Acoustic timing, tuner accuracy, loop onset/boundaries, vendor document providers and TalkBack need physical-phone checks.

## Current verification

All **72 unit/Robolectric tests**, debug/release compilation and lint pass (0 errors / 10 existing warnings). Native results, including Log reopen/install-over and enlarged-text checks, exact source/package provenance and any regression-suite limits are recorded in [CURRENT_STATE.md](CURRENT_STATE.md). Physical-phone acceptance remains pending.

Next: install the signed update without uninstalling and follow the six Log checks in [MANUAL_TESTS.md](MANUAL_TESTS.md). Stage 3 detailed phone acceptance remains pending; [EVALUATION.md](EVALUATION.md) stays deferred at0/10 documented real sessions.

## Development

Read [AGENTS.md](AGENTS.md), [CURRENT_STATE.md](CURRENT_STATE.md), [BUILD_PLAN.md](BUILD_PLAN.md) and [WORK_LOG.md](WORK_LOG.md), including the mandatory efficiency procedure. Use [FIRST_WORK_HANDOFF.md](FIRST_WORK_HANDOFF.md) to start a new work session.

Build/check command: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease` (Windows: `.\gradlew.bat` with the same tasks). The repository uses JDK 17, SDK 36, Gradle wrapper, Kotlin/Compose and Room 2.8.5; exact dependency versions are in Gradle files. Room schemas 1–9 and explicit migrations are retained. GitHub Actions supports full release checks and isolated class/API/text-scale diagnostics; documentation-only changes skip CI.

Use `scripts/Publish-LocalApk.ps1` for the permanent-key distribution. Keep private `.signing`, tools, build outputs and APKs out of Git; retain earlier releases and verify signatures, metadata and hashes. License attribution: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
