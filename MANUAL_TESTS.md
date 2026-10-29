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

## 0.8.0 tools/navigation phone acceptance — pending

Install signed build14 over existing app without uninstalling after verified packaging. Check persistent centered Lato banner on Home, Blocks/editor, Practice Now/focus/summary, Routines/history, Tools and each tool, backup and Recording selection. Open Practice Now to continue each unfinished session; ensure starting fresh retains older sessions and notes. Routines should show history without unfinished sessions.

Tools → Metronome: center120, +/−1 then +/−5, Start/Stop at slow/fast tempos and changed tempo while clicking; Back/background/recreation stops audio and never auto-restarts. Tools → Tuner: permission only on Start, denial informative; allow then Start; compare known single notes against a trusted440Hz reference, flat/sharp/cents, silence/room noise, low/high instruments. Back/background/recreation stops mic indicator. No tuner audio should be saved. Emulator captures and synthetic pitch tests do not prove acoustic accuracy.

Tools → Timer: Start/Stop/Resume/Reset stopwatch; roll whole-minute selector to1 and another value, Start countdown, Stop/Resume/Reset and let1minute finish atzero/Time’s up. Both clocks pause when leaving/backgrounding; this is a foreground utility, not an alarm. Reenter preserves paused values in the same process; process death resets temporary tool state. Repeat at200% text and with TalkBack wheel adjustment. Reopen old practice/takes, backup and export to confirm existing data. Record actual phone/Android/output route and findings; no results inferred.
Automated0.8.0 status: sourcec96b903c6617f0351baf99202e262cadc0a0696c, CIhttps://github.com/mrayover/practice.logger/actions/runs/40629201227,38unit tests/fullAPI26/35native tools+regressions and200% labels/screenshots verified. Signed0.8.0/build14 is available under dist/practice.logger-0.8.0-b14-c96b903. The phone checklist above remains unperformed; report actual acoustic accuracy/audibility,1minute completion and TalkBack rather than treating emulator checks as phone acceptance.

## 0.9.0 cream and standalone recordings — pending phone checks

Install signed build15 over current app without uninstalling after CI/signing. Check light cream across Home, all pages, dialogs/recorder and bottom navigation, with green banner/buttons retained. Recording → Free recording should start immediately with microphone access, or request permission then start after allowing. Stop/save two takes, play/rename/export, close/reopen/cold restart; Saved free recordings browses without starting mic. Rotate/background during capture: stops/saves usable audio and never starts another take automatically. Confirm existing Blocks, notes, Block free/loop takes untouched; free takes never appear in a Block or create a fake Block. Repeat at200% text. JSON backup still excludes ALL recordings, including standalone; export wanted audio separately. Report actual phone/Android/mic route/results; no phone test inferred.
## 0.9.0/build 15 — verified signed delivery, 2026-10-28 America/Los_Angeles

Light cream #FFF8EB covers app backgrounds, Material surfaces/dialogs/fields, launch and navigation; green banner/buttons and Lato retained. Recording → Free recording starts native M4A capture immediately when permission/storage/foreground are ready, or after microphone permission is allowed. Saved free recordings opens the same collection without capture. Shared Block recorder provides Stop/save, playback, rename, confirmed delete and per-take export. Rotation/background/exit stops and saves usable capture without starting another; free takes remain together, separate from Block takes. No synthetic Block is created; phrase controls remain in Blocks.

Room schema6 explicit5→6 migration preserves old take IDs/files/labels/tempo/beats, foreign keys/indexes and all practice data; nullable blockId denotes standalone ownership. Compiler-generated6.json committed. JSON3/legacy readers unchanged and still exclude ALL takes/labels/audio, including standalone: export wanted audio separately. No new dependency, appID or permanent-key change; broader storage/backup, Goals/reusable routines remain deferred. Stage3 phone acceptance pending; Stage4 stays0/10.

Verified app/test source: e1c884c91faa26576de8765296e6f203399df8c5
Successful CI: https://github.com/mrayover/practice.logger/actions/runs/40634982208
40 unit/Robolectric tests pass, no failures/errors/skips; lint0 errors/10 warnings. Both Android8/API26 and Android15/API35 pass initial workflow plus20 direct phases, including new standalone native capture/recreation/rename/playback/cold reopen/isolation checks, frozen schema5 upgrade/file safety, all prior practice/backup/recording/phrase/tools/upgrade and200% checks. Upgrade code16. Normal Android8/15 free-take lists and cream Home plus 200% free-recording controls visually reviewed; cream surfaces, retained green branding, two saved takes and shared playback/rename/export/delete controls are visible. The first menu-named screenshot captured the preceding Home frame; automated button navigation/capture assertions and actual saved-list screenshots verify the recording destination. Physical phone/audio quality acceptance remains pending. No local Android SDK/emulator installed.

ZIP: dist/practice.logger-0.9.0-b15-e1c884c.zip
APK: dist/practice.logger-0.9.0-b15-e1c884c/practice.logger-0.9.0-b15-e1c884c.apk
APK SHA256: 1a6a9ffcda4cc80938d4b43491040f88b72dc28758eb5445dd2fd47e1810f42f
Permanent certificate: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830
Existing publisher verified signature/pinned certificate/stable com.mrayover.practicelogger.app/version0.9.0/code15/ZIP extraction. All33 prior baseline APK/ZIP files unchanged; LATEST updated after packaging. Private key unchanged/not uploaded. Install over current permanent-key app without uninstalling.

CI artifacts (30-day retention):
- practice-logger-debug: 12793865029; sha256:27a782946535999d6d8376d31fe58db74b4e4e3fae317b74d490710e1630acd5
- practice-logger-unsigned-release: 12793620700; sha256:2dfa49348b3802926c38920c3803fbf984360b8b8a8987f8d51357e9e588e7d8
- device-test-reports-api-35: 12793436676; sha256:f4e1bb520720baefb91b8ed31b2f79bb12af2a1151bf85d2ef1903eca600b358
- verification-reports: 12793062805; sha256:be6b4322679508c7ebf28fa5fd9b6c2c076160e91a316c02a5b6fc92e4efbe60
- device-test-reports-api-26: 12792904844; sha256:95863457a8735344b510036bcfc466dbe7b86ac8d0244af08648910fbb0cd517

Owner authorized Git commands for this update/schema/closeout because Desktop controls unavailable; default Desktop preference retained. Existing Windows checkout, synced sources untouched; app source/schema pushed, closing records publish separately with local/remote clean status verified in handoff.

Exact next task: owner installs signed0.9.0/build15, checks cream screens/dialogs, taps Free recording and saves two takes, listens/renames/exports, browses Saved free recordings without mic, restarts/rotates/backgrounds and confirms old Blocks/session notes/free/loop takes untouched. Report phone/Android/mic route/results; discuss broader save/storage afterward. JSON still excludes audio.

## Saved Routines phone checklist — 0.10.0/build16 (pending)

1. Install signed update over the permanent-key app. Verify existing Blocks, archives, unfinished/completed practice, quick/session notes, working BPM and every old Block free/loop/standalone take still exist and play. Never uninstall needed data.
2. Practice Now: add A/B/A, reorder and change one duration. Save as Routine with a name. Verify no practice started, same builder draft and unchanged Block defaults. Cancel/blank-name saves should do nothing.
3. Routines: saved list and Recent Practice/history both remain. Create a second routine; edit/rename/add/remove/reorder/override minutes; rotate during unsaved edits, save and restart. Verify stable current Block references/durations and Back discard confirmation. Delete with cancel then confirm; Blocks/history/audio remain.
4. Practice Routine twice: each starts paused with saved order/durations and clean independent notes/time/BPM/outcomes. Complete/Skip/overtime/click/recordings/quick notes/summary/save-for-later work as before. An existing builder draft and other unfinished practice stay intact. Restart and Continue Session resumes paused with saved notes/time.
5. Edit a referenced Block then start again: new session uses current details, older snapshots stay unchanged. Archive one referenced Block: explicit name/status, disabled start, no partial session. Restore it or replace/remove the entry, save, and start normally. Delete a routine with unfinished/history sessions: those sessions remain usable.
6. Export JSON4 through the Android picker. On a disposable empty test installation restore it; verify routine names/IDs/order/durations, archived references and history/recovery. Legacy1/2/3 backups restore without routines. Nonempty restore refuses safely. Audio/labels remain excluded; export wanted audio separately.
7. At 200% text verify persistent banner, builder Save as Routine/name dialog, routine name/add/reorder/remove/duration/save/delete/start controls fit/read/scroll. Report phone/Android/mic route/results. Automated device checks do not replace owner phone acceptance.


Saved Routines automated gate passed2026-10-28: sourceaa78783e9b264dccec64018362b1a88cf0f5e255, CIhttps://github.com/mrayover/practice.logger/actions/runs/40733036079;51unit tests/lint0errors10warnings/fullAPI26/35initial+22phases, real picker JSON4, upgrade17 and200% checks pass. Normal/large Routine screenshots reviewed. Signed0.10.0/build16 available in dist/practice.logger-0.10.0-b16-aa78783; phone checklist above remains pending, not marked accepted. Stage3 phone pending/Stage4 0/10.

## Goals phone acceptance — 0.11.0/build20 (pending)
Install permanent-key APK over existing app without uninstalling. Create/edit5Goals with outcome/notes/manual progress; verify sixth/reopen limit, complete/reopen without erasing identity, reorder and confirm top-two Home. Open detail, add shared Blocks, edit/archive/restore Block and verify current display and explicit excluded state. Practice Goal twice with current default durations; confirm builder draft and older unfinished sessions retained, paused recovery/notes/timer/click/recorder/skip/summary/history and exact actual time without shared-Block retroactive attribution or automatic progress. Restart and update retaining old data/audio. JSON5export/spare-empty restore and legacy1–4import; refuse nonempty restore including Goals-only. Audio must be exported separately. At200%text and TalkBack inspect Home/manage/editor/detail/picker and every lower action. Report phone/Android/results; these checks are not fabricated by emulator runs. Stage3 phone backup acceptance pending; Stage4 remains0/10.

2026-10-28: owner generally reports the app works and explicitly stops testing. Individual checklist/model/OS results were not supplied; do not fabricate them or mark ten-session evaluation complete. No further tests in this session.

## Visual polish0.11.1/build21 — phone check pending
Install over the existing app without uninstalling. Inspect darker cream on Home/Goals/Routines/Blocks/dialogs/recording; green/Lato unchanged. Verify each Home bar matches its saved percentage and empty slots stay functional. Check outlined/bold Back to Home at normal and200%text; navigation unchanged. Confirm existing practice/takes still available. Report phone/Android and visual findings; no extra feature evaluation required.

## Safe permanent Block deletion — 0.12.0/build22 phone check

Install over the permanent-key app without uninstalling. Physical-phone checks pending:
1. Create an unused Block, reopen its editor, scroll below Archive to Delete Permanently. Cancel: saved Block and unsaved draft remain unchanged. Confirm: return to Library, row disappears; restart and verify absence.
2. Repeat for an unused archived Block through Archived filter. New unsaved Blocks have no deletion action.
3. Try Blocks with Goal links (including completed Goals), duplicate placements in a saved Routine, and saved free/loop recordings. Verify actual Goal/distinct-Routine/take counts; refusal changes nothing. Export wanted takes before manually deleting recordings.
4. Try a Block in the builder draft, an unfinished session and completed history. All are protected; completed history is explicitly explained. Removing Goal/Routine links cannot bypass historical protection. Use Archive for these Blocks.
5. Export JSON after eligible deletion; restore on a spare empty installation and confirm remaining Blocks/Goals/Routines/history/notes. JSON still excludes all audio. Confirm existing recordings play on the original installation.
6. At normal and200%text check deletion action, confirmation/cancel/refusal dialogs and Back. No long-press behavior is included. Report phone/Android and results.
