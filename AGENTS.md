# Permanent development instructions

Read README.md, AGENTS.md, CURRENT_STATE.md, WORK_LOG.md and BUILD_PLAN.md completely before implementation in every session. Inspect local changes, branch, remote and CI first. BUILD_PLAN.md governs the first usable release; preserve the original prefixed planning documents as provenance.

Work in the existing repository, preferably C:\Users\mrove\Projects\practice.logger. Preserve user changes and commit history. The owner uses GitHub Desktop exclusively: use Desktop for Git mutations, including commit/push/fetch/pull. Read-only repository inspection is allowed. Do not create another repository.

Start at Stage 0 and enter Stage 1 only after Stage 0 builds successfully. Respect each stage's exit gate. Keep scope small: Kotlin, Compose, Room, Lifecycle, bundled Lato Regular/Bold throughout app-controlled text. No unfinished feature controls. Goals, Routines, audio, metronome, tuner and advanced features remain deferred.

Every completed stage needs a working installable APK, verified relevant tests and a simple manual phone checklist. Run build, lint and targeted tests. Demonstrate real workflows before claiming completion. Distinguish compilation, automated execution and owner phone testing. Never claim a phone test without evidence. Keep schema exports and explicit migrations; never use destructive fallback for production data.

At the end of EVERY session (including fixes, blocked or incomplete sessions): append a detailed entry dated in America/Los_Angeles to WORK_LOG.md, preserving all earlier entries; update CURRENT_STATE.md with verified progress, working features, unfinished work, known issues, test outcomes and one exact next task; update related documentation for actual decisions. Commit and push intended changes via GitHub Desktop, verify the remote SHA and branch, and report them. Explicitly state anything not committed, pushed, built or tested. Do not depend on chat memory.

If work is in a cloud checkout, tell the owner to Fetch origin and Pull origin in GitHub Desktop. Changes in a PR are not default-branch changes. Report actual CI run/artifact URLs, functionality, tests, limitations, manual checklist and next action.

Do not commit build outputs, APKs, SDKs, keys, local.properties or machine-dependent active configuration. Use GitHub Actions artifacts for APKs. Never erase existing work-log entries.

Owner delivery preference (2026-10-26): keep each verified version as a ZIP and extracted folder under dist/ using versionName, versionCode and source SHA. Ignore dist/, .tools/ and .signing/ in Git. Locally sign unsigned CI release APKs using the existing permanent .signing key and scripts/Publish-LocalApk.ps1; verify the pinned public certificate. Never silently replace this key or recommend uninstalling an app containing needed data. The stable app ID is com.mrayover.practicelogger.app; old 0.1.0 stays separate because its ephemeral debug key was lost. Increment versionCode for each distributed update; no automatic Block transfer from 0.1.0. Back up private signing material securely outside Git.
