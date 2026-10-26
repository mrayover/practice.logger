# Phone testing checklist

The owner reports the 0.1.0 catalog works. Detailed model/Android-version and individual checklist results were not supplied. Stage 2 phone testing is pending. Automated results are in CURRENT_STATE.md; they do not replace phone checks.

## Install and identify the new build

1. Open the extracted APK in dist/practice.logger-0.2.0-b3-91f4129/ on an Android 8.0+ phone. The launcher label is practice.logger 0.2.
2. This build installs alongside the old temporary-key 0.1.0 catalog; the old app/data are retained, but Blocks do not transfer automatically. Recreate desired Blocks in the new app.
3. Keep future updates installed over this new app rather than uninstalling it. Use locally signed dist APKs, not CI debug APKs. Backup/restore is not implemented yet.

## Catalog

1. Create three Blocks with Name, Action, one-minute duration and optional BPM/notes. Try blank required fields, zero/decimal duration and invalid BPM; drafts should remain with clear errors.
2. Edit and search; test Cancel/Back, Keep editing and Discard. Rotate while editing and confirm the draft remains.
3. Archive → Cancel retains an active Block. Confirm Archive, find it in Archived, and Restore it.

## Stage 2 practice loop

1. Home → Practice Now. Select the three Blocks, move them Up/Down, change one placement to two minutes, remove/re-add another. Confirm the saved Block's default duration remains unchanged. Leave/reopen and confirm the draft is retained. Start is disabled while empty.
2. Start Session. Start the first Block, practice briefly and Complete early. Skip the second. Start the third, Pause/Resume, rotate, then background the app for ten seconds. It should return paused without adding that background time.
3. Save for later, force-stop in Android settings, and reopen in airplane mode. Continue Session should reopen the correct third Block with its order, configured duration and saved active time. Resume.
4. At 00:00 it must wait without advancing. Continue should count overtime upward. Complete the last Block; Summary should appear automatically with two completed and one skipped, truthful active times and any optional notes.
5. Save an optional session note. Restart and reopen the summary from Recent Practice. Edit/archive the original Block; history should still show its original name/action.
6. Check Home, Builder, Focus, Summary, errors/dialogs and catalog for Lato, large-text scaling, keyboard scrolling and reachable controls. Android system UI/keyboard use their own typography.
7. On the next release, install its consistently signed APK over this app and confirm Blocks, history and unfinished sessions remain. Do not uninstall to resolve signature mismatches.

Record model, Android version, exact build and any failures in WORK_LOG.md. Abrupt process termination can lose the latest uncommitted timer checkpoint; committed data recovers paused. Do not treat this release as dependable daily use until Stage 3 backup/recovery work and device feedback are complete.
