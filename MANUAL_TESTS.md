# Phone testing checklist

The owner generally reports 0.3.0 works. Manual backup/recovery and evaluation checks are deferred by owner until after this build cycle; no detailed results are inferred. Detailed model/Android-version and individual checklist results were not supplied. Version 0.3.2 owner phone acceptance is pending and the deferred checks can wait until the owner is ready. Automated Android 8/15 picker and 200% text workflows pass; EVALUATION.md prepares Stage 4 (0/10 real sessions). Automated results are in CURRENT_STATE.md; they do not replace phone checks.

## Install and identify the new build

1. Open the extracted APK in dist/practice.logger-0.3.2-b6-3f7730f/ on an Android 8.0+ phone. The launcher label is practice.logger; app details show 0.3.2 (build 6). Install over 0.2.0 or 0.3.0 and confirm its Blocks/history remain. Do not uninstall first.
2. The permanent-key app remains separate from the old temporary-key 0.1.0 catalog; the old app/data are retained, but Blocks do not transfer automatically. Recreate desired Blocks in the new app.
3. Keep future updates installed over this new app rather than uninstalling it. Use locally signed dist APKs, not CI debug APKs. Versioned JSON backup/empty-app restore is available from Home.

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

## Stage 3 backup increment (0.3.1)

1. Install the signed 0.3.1 APK over 0.2.0 or 0.3.0. Verify existing Blocks, draft/unfinished session and saved history. The older dist files must still be present on the computer.
2. Save notes, pause practice and go Home → Backup and restore. Export to a local folder using Android's picker; wait for Backup saved. Confirm the JSON exists. Cancel a second picker and check data remains unchanged. Repeat export; retain both files.
3. Choose the valid file to restore. Review counts, rotate, then Cancel. Try again and confirm: this nonempty app must reject restore without changing Blocks/history/unfinished time. Try malformed or unsupported-version JSON; it must reject it without a crash. Never clear the current app just to make restore possible.
4. On a spare device/disposable empty installation of the signed 0.3.1 app, choose the exported file, confirm restore and inspect archived Blocks, notes, historical instruction snapshots and any unfinished session. Restart; verify records remain and unfinished practice is paused with its saved order/time. Export again and compare representative records.
5. Test unavailable/read-only provider or low storage using disposable test files only. An error must leave database data intact; a failed export's partial file is not a completed backup. Check airplane mode with local storage, large text, TalkBack labels, landscape, keyboard and reachable dialogs/buttons.

Record model/OS, build, chosen document provider, pass/fail and any error in WORK_LOG.md. CI now automates the real Android system picker, cancellation, restore into a cleared disposable emulator app and force-stop/reopen on API 26/35. Unavailable-provider/retry behavior is tested; real-phone provider/disk-full/TalkBack behavior still needs owner checks. No automatic backup scheduling or in-app updater exists. Exact next development action: fix any issue found by this checklist before expanding scope.
## Close Stage 3, then begin Stage 4

Record phone model/Android/build and pass/fail for install-over data preservation, pause/background/process recovery, completed history reopening, successful picker export, spare-empty-install restore/restart comparison and readable/reachable controls at your chosen text size. CI screenshots and emulator checks are engineering evidence, not owner phone confirmation. Do not clear the main app for testing.

Once these checks pass, begin EVALUATION.md's ten real sessions. After each, save a short optional session note covering taps/time to start, editing friction, timer usefulness, notes/history and any missing flexibility. Report exact failures before adding features. Current real-session count: zero documented; synthetic test data does not count.
## 0.3.2 quick note check (normal practice)

1. Continue/start a session, type a Quick note and tap Save Block note. Check Block note saved; the Block stays unfinished and the timer remains usable.
2. Edit the text, choose Save for later, then Continue Session; confirm the latest text and paused timer. Repeat with Android Back after closing the keyboard.
3. Save, force-stop and reopen; continue and confirm the same current Block/note, then Complete or Skip. Summary must contain the final note.
4. Clear the note and save if desired. Confirm another Block's note and older summaries remain unchanged. At large text sizes, scroll to reach the save controls.

Typing alone remains unsaved against abrupt process death; use the save actions. Backup/recovery and the ten-session evaluation are deferred by owner, not prerequisites for testing this incremental APK.