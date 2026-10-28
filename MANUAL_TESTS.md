# Phone testing checklist

The owner generally reports 0.3.0 works. Manual backup/recovery and evaluation checks are deferred by owner until after this build cycle; no detailed results are inferred. Detailed model/Android-version and individual checklist results were not supplied. The owner generally accepts 0.4.0; detailed audible checks were not supplied. The owner generally accepts 0.5.0 recording; 0.6.0 phrase-loop phone acceptance is pending and the deferred checks can wait until the owner is ready. Automated Android 8/15 picker and 200% text workflows pass; EVALUATION.md prepares Stage 4 (0/10 real sessions). Automated results are in CURRENT_STATE.md; they do not replace phone checks.

## Install and identify the new build

1. Open the extracted APK in dist/practice.logger-0.7.0-b13-61b76d5/ on an Android 8.0+ phone. The launcher label is practice.logger; app details show 0.7.0 (build 13). Install over the permanent-key app through 0.5.0 or 0.6.0/build 10 and confirm its Blocks/history remain. Do not uninstall first.
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
2. Save notes, pause practice and go Home → Tools → Backup and restore. Export to a local folder using Android's picker; wait for Backup saved. Confirm the JSON exists. Cancel a second picker and check data remains unchanged. Repeat export; retain both files.
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
## 0.3.3 independent sessions

1. Leave session A unfinished with a saved note/time. Home still offers Practice Now; open Routines for Continue Session.
2. Practice Now → build/start B, practice briefly and Save for later. Both unfinished sessions must remain selectable.
3. Open each, confirm its own Block/time/note and paused state. Restart the app and repeat; the default may be newest, but the other dated control must still work.
4. Complete B and confirm A remains unfinished with unchanged data. Then finish A; summaries must remain separate. One shared builder draft remains supported.
## Stage 5 metronome — 0.4.0/build 8 (owner checks pending)

1. Install the permanently signed build over 0.3.3; keep Blocks, unfinished sessions, notes and history. Do not uninstall. Export JSON before/after as desired; new format 3 needs 0.4.0 or later to restore.
2. Open a Block with saved BPM (or no BPM: default 120). Start click while timer is paused; listen at normal media volume. Change by −1/+1 and Set BPM, including 30/120/300. Invalid/decimal/out-of-range input must not apply. Judge audible steadiness on the phone.
3. Run the timer and click together; adjust live. Pause the timer and confirm click continues; stop/restart click and confirm timer state/time remains independent.
4. Background, rotate, leave/reopen, switch Block/session and force-stop/reopen. Audio must stop and remain silent until Start click. Repeat start/stop rapidly; try an audio interruption/headphone route safely. Never infer audible success from a displayed Stop click label.
5. Use 126 BPM, leave/reopen and confirm working BPM persists without changing the Block's reference. Complete/Skip and reopen Summary: Last used click is 126 BPM. An unused click leaves no used BPM. Old history stays intact. Check large text/TalkBack.

Record exact phone/Android/build and audible timing/lifecycle findings in WORK_LOG.md. Automated emulators do not prove physical click audibility/timing. Free recording is now delivered in 0.5.0; phrase looping remains later.

## Stage 6A free recordings — generally accepted by owner 2026-10-27; individual results not supplied

1. Install the permanently signed 0.5.0 update over 0.4.0 without uninstalling. Check old Blocks, sessions, notes, history and metronome. In Focused Practice tap Recordings. Deny microphone permission once: no take or crash. Allow it on a later request (or in app settings if Android stops asking), then tap Record take to begin.
2. Capture at least two takes while speaking/playing; stop and save each. Check recording state/elapsed time and listen to actual content. BPM is optional. Start click before recording if desired; bleed is accepted. Use Pause timer/Resume timer inside the sheet during capture; capture must continue. Starting/stopping capture must not change the timer.
3. Play each take, switch playback, stop, rename one, cancel deletion, then confirm deletion of an unwanted take. Export one through the file picker and play the exported M4A outside the app. Failed/cancelled export must retain the source. No arbitrary take-count cap.
4. Start a take, background/leave/rotate: microphone must stop, with usable capture saved after validation; playback must stop and never restart automatically. If a very short capture is invalid, show an error and preserve older takes. Force-stop during an unsaved capture only with disposable material; it may be discarded. Restart and verify previously saved takes still play.
5. Edit/rename/archive the Block, open Recordings from its editor, and verify the same takes remain. Revisit the Block in another session; recordings follow the reusable Block, not a placement. Try large text/TalkBack and normal output routes; playback stops the click, capture can include it.
6. Verify the prominent JSON exclusion notice. Export each take you want to keep elsewhere: JSON backups do not contain audio or take labels, and audio cannot be restored from JSON. Do not uninstall/clear the real app for testing. Use a spare/disposable installation for JSON restore checks.

Record exact phone/Android/build, microphone/output quality, permission/interruption behavior and any failures in WORK_LOG.md. CI encoder/container/UI tests do not establish physical microphone quality. Phrase looping/count-in remain Stage 6B, not this increment.

## Stage 6B phrase loops — physical phone checks pending
1. Install signed 0.6.1/build 12 (f4f827b) over the existing permanently signed app without uninstalling. Verify saved free takes, labels, Blocks, notes/history and sessions still work. Open an existing Block's Recordings and keep at least one free take alongside new loops.
2. Record phrase: confirm 120 BPM and 8 beats (4.00 seconds). Tap Start 4-click count-in, hear four even clicks, and start playing on the next beat. Recording must stop automatically after one phrase, save a new loop and repeat only that phrase until Stop loop. Listen across at least ten repeats for a gap/click or drift. No measured/latency-free/seamless phone result is inferred from CI.
3. Create a second loop at another tempo/length; use 4/8/16/32 beats and try 60/120/180 BPM. BPM input outside30–300 or non-whole values must not start. Compare intended phrase length with a known click/reference. Input/output latency may shift the capture onset; report route and audible boundary behavior.
4. Stop/restart each saved loop; labels show its recorded BPM/beats/date. Regular working BPM changes must not speed/pitch-shift existing audio. To record another tempo create another take. Pause/resume the practice timer during capture; it stays independent. No overdubs/layers/trimming.
5. Cancel during count-in and during capture; no partial saved loop or overwrite. Background/leave/rotate during capture: incomplete phrase discarded, saved takes kept. Background/leave/rotate during repeat: stop, remain silent until explicit Play loop. Try interruption/route change safely. No background mic or automatic restart.
6. Rename a loop, cancel/confirm deletion of an unwanted take, export a loop through the file picker and play the WAV outside the app. Export is a single phrase, not an infinitely repeated file. Failed/cancelled export keeps original. Reopen/restart the app and play both retained loops and the free take; rename/archive the Block and verify its takes remain accessible in the editor. Try 200% text and TalkBack.
7. JSON backups still exclude free/loop audio and take metadata/labels. Export desired takes separately. Do not uninstall/clear your real app. Full save/storage/bulk restoration decisions are deferred to the owner's discussion after looping.
Record phone, Android version, app build, output route, microphone quality, four-click onset alignment and repeat-boundary findings in WORK_LOG.md. Existing Stage3 manual backup acceptance and Stage4 0/10 are unchanged.

### Build 11 phrase setup visual acceptance

At normal and 200% font size, open Record phrase, edit Phrase BPM and verify its short floating label and 30–300 support range stay readable. Tap the keyboard Done key and report whether it hides; also check Android Back and scrolling can reveal all beat choices, Start 4-click count-in and Cancel. CI screenshots establish label readability, but do not establish reliable Done dismissal. Record actual phone/Android/input keyboard and results; do not count the emulator screenshot as a completed phone check.

## 0.6.1/build 12 recording click — signed delivery, phone checks pending

Install the signed 0.6.1/build 12 update from dist/practice.logger-0.7.0-b13-61b76d5/ over the existing app without uninstalling. Earlier build 11 does not include this fix. In Record phrase, confirm "Click while recording" is on by default. Choose 120 BPM / 8 beats; hear four count-in clicks followed by a steady click throughout the one-phrase capture. Pause/resume the practice timer during capture; the click and phrase length stay independent. The guide should stop before the saved phrase starts repeating. Speaker bleed in microphone audio is expected; no digital click is added to the file.

Turn the option off and record a second phrase: four count-in clicks, then no guide during capture. Retain/play both takes and a prior free take; export the exact WAV. Cancel during count-in/capture and background/leave while the guide runs; no guide/microphone may continue, saved takes remain, and reopen stays silent. Check the full checkbox row at 200% text/TalkBack. Report actual phone/Android/output route and guide/onset/boundary behavior; emulator counters do not establish audible phone timing.

## Home sketch redesign — signed0.7.0/build13, phone visual checks pending

Install the signed update over the existing app without uninstalling, then check the home screen against the supplied drawing: centered bold Lato title in the separated green header; white body; blank Goals rows with inactive settings/Edit; five green buttons in order Practice Now, Blocks, Routines, Tools, Recording; Data usage footer. Scroll at large text/small height and check status-bar icon contrast. Goals and the footer deliberately have no data/edit behavior yet.

Practice Now opens the session builder; Blocks opens Block Library. Open Routines to continue each unfinished session and reopen recent history; saved practice/timer/notes must remain intact. Tools → Backup and restore retains existing safe export/empty-only restore. Recording lists active/archived Blocks and opens the same per-Block saved takes; test closing/Back and a previous free/loop take. The default-enabled click-during-loop-recording behavior remains. Do not uninstall the existing app or clear real data. No functional Goals/reusable routines/storage measurement is claimed.
