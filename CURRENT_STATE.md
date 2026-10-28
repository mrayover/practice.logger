# Current state

## Current task — sketched home redesign, 0.7.0/build 13 prepared

2026-10-27 America/Los_Angeles. Owner supplies a hand-drawn home sketch and explicitly requests a visual rearrangement even where functionality is not yet implemented. New home: centered white Lato Bold practice.logger in a separated green header, white background, blank two-row Goals panel with inactive settings/Edit, five stacked green buttons (Practice Now, Blocks, Routines, Tools, Recording), and plain Data usage footer. Green is the existing #315C47; body surfaces are white. Layout scrolls at small heights/large text; status-bar icons adapt to the green header.

Navigation: Practice Now → existing session-selection/builder; Blocks → existing Block Library; Routines → existing practice overview with independent unfinished sessions/Continue and recent history; Tools → a simple tools page retaining Backup and restore; Recording → select an active or archived Block and open the existing recording sheet. Goals and its settings/Edit controls are deliberately inactive visual placeholders as expressly approved by owner. No reusable routine engine, standalone metronome/tuner, new recording system or storage statistics has been implemented. Data usage is a label, not a fabricated value. Save/storage redesign remains deferred.

Source is prepared in the existing clean-baseline Windows repository (main/local and remote 5e0b24e2f8b192e24d56f73020ee5aec5f9e59bc). No schema, data, audio engine, signing identity or backup-format change. Old installable builds remain intact; dist/LATEST still points to verified 0.6.1/build 12 until the redesigned APK passes CI and signing. New target 0.7.0/code13; CI install-over code14. All 33 existing unit tests and full Android 8/15 build/lint/native workflows must run; no new build or phone success is claimed yet.

Existing navigation tests explicitly follow the new Routines/Tools paths; catalog tests now use Blocks. Launch workflow checks all five home routes, blank/inactive Goals, empty and populated Recording selector, and existing catalog persistence. Normal and 200% home screenshots are collected for visual review. Existing timer, backup, recording/loop, restart, migration and export assertions remain. Source diff whitespace checks pass; Android/Compose compilation, full CI and screenshot review are pending.

Publication method resolved: owner selected option1 authorizing Git commands for this home redesign and release closeout; Desktop controls remain unavailable. The previous Git-command exception was explicitly limited to the 0.6.1 recording-click update. Prepared changes are local/uncommitted/unpushed; no new Git mutation has been made for this redesign. Exact next task: commit/push under the owner-authorized exception, verify full CI/screenshots, repair layout issues if found, and permanently sign/package 0.7.0/build13 while preserving prior releases. Stage3 manual backup acceptance and Stage4 0/10 remain unchanged.

## Last delivered release — retained provenance and limitations
Updated 2026-10-27 (America/Los_Angeles). Delivered engineering update **0.6.1/build 12**: optional click throughout phrase recording. Owner generally accepted 0.6.0/build 11 ("it's working great!") and requested this fix before discussing save/storage. Detailed phone results for build 12 remain pending. Stage 3 manual backup acceptance remains pending; Stage 4 stays 0/10.

## Current behavior

Recordings → Record phrase → **Click while recording**, enabled by default. Four count-in clicks always play. With the option on, the same native output track continues a click at the selected phrase BPM throughout one microphone capture; with it off, capture follows a count-in without further guide clicks. This works from Focused Practice and the saved/archived Block editor. The ordinary metronome stops before phrase startup so a second tempo/focus owner cannot compete. The guide stops before saved-loop playback and on cancellation, background, exit, interruption or failure. The practice timer stays independent.

Phrase BPM remains 30–300; supported lengths are 4/8/16/32 beats. Saved loops retain exact PCM16 mono 48 kHz WAV frames and immutable recorded BPM/beats. Changing working BPM never changes saved audio. Multiple independent FREE M4A and LOOP WAV takes retain play/stop, rename, confirmed delete, per-take file-picker export and archived Block/restart access. No click is mixed digitally into the WAV, but speaker output can be picked up by the microphone. Input/output latency, audible onset and boundary quality still need the owner's phone; no latency-free/seamless claim.

Room schema 5 and migrations 1→2→3→4→5 are unchanged. Existing Blocks, sessions, notes, history, free/loop takes, labels and audio are retained. No identity/key/backup-format change. Existing foreground timer/zero/overtime, independent unfinished sessions, saved notes, catalog/search/archive, bundled Lato and empty-only validated practice JSON restore remain. Incomplete phrases are discarded with feedback; prior complete takes remain. No audio auto-resume after leaving/restart. Keyboard Done dismissal was not established by earlier captures; retain Done/Back and lower-control access in phone acceptance.

## Verification

App source main: f4f827bde0921b805b4f3126555344507bb0e3fd
Successful CI: https://github.com/mrayover/practice.logger/actions/runs/40603962994
build 122538731105 SUCCESS; device (26) 122539950561 SUCCESS; device (35) 122539950595 SUCCESS.
All 33 unit/Robolectric tests pass with zero failures/errors/skips; lint has 0 errors/10 existing warnings. Both Android 8/API26 and Android 15/API35 pass initial catalog plus 17 direct phases, including native microphone/guide/capture/repeat, option-on/off, timer independence, cancellation/background cleanup, export/full-file hash, saved takes/archived Block/process reopen, migration/upgrade and 200% text. Downloaded reports confirm empty phrase/free-recording crash buffers. Normal phrase setup and both200% screenshots reviewed. API35 shows a readable selected checkbox. API26 captures retain the keyboard over the lower dialog, so screenshot evidence does not confirm checkbox visibility after keyboard dismissal there; retain that phone check. Automated checkbox selection/accessibility assertions passed on both APIs.

New PCM guide tests verify unchanged four-beat count-in, exact extended output size and beat/silence positions at 30/60/73/120/180/299/300 BPM across all lengths. Native tests verify the actual guide playback head advances during enabled capture, is absent during disabled capture, and is released on cancellation/completion. Prior exact WAV/repeat/export assertions remain. Three isolated pure PCM/WAV JUnit tests also passed locally using cached Kotlin/JRE; full Android compilation/native execution took place in CI. No local Android SDK/emulator or physical phone acceptance claimed.

Earlier CI40599974541/source5e86e31 passed build/33 tests/lint/API26 but API35's new short-phrase assertion waited for UI idle after capture could finish. The live state/counter check now occurs in one poll. CI40601576976/source9de76d5 passed build/tests/lint but API35 selected Add Loop fixture before asynchronous Room/Flow publication. Both phrase click helpers now wait up to 15 seconds for the exact enabled control. Run40602791872/source74ed0ff passed full API35 but API26 timer UI interactions outlasted the four-second8beat recording; the screenshot showed saved looping audio and running timer. Timer pause/resume now runs during a32beat capture before background cancellation, while the original8beat exact-frame/repeat/export checks remain. No production audio changes followed5e86e31. Final source f4f827bde0921b805b4f3126555344507bb0e3fd passed full CI; no failed-run APK was distributed. WORK_LOG preserves evidence.

## Signed distribution

ZIP: dist/practice.logger-0.6.1-b12-f4f827b.zip
APK: dist/practice.logger-0.6.1-b12-f4f827b/practice.logger-0.6.1-b12-f4f827b.apk
BUILD_INFO.json and INSTALL.txt accompany the APK; dist/LATEST.txt now points here.
APK SHA-256: 31d38af9325d6fda5252fa2fd69b927cfc4b352111b769548bd9ccf4984a6445
Permanent certificate SHA-256: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830

Existing Publish-LocalApk.ps1 verified signature, pinned certificate, stable package com.mrayover.practicelogger.app, version 0.6.1/code 12, and ZIP-extracted APK checksum. Android 8+; install over the existing permanently signed app without uninstalling. All 28 earlier APK/ZIP baseline files remain byte-identical, including builds 10/11. Old temporary-key 0.1.0 stays separate. Private signing material was unchanged and never uploaded.

CI artifacts (30-day retention), under the successful run's /artifacts/ path:
- device-test-reports-api-26: 12780741242; sha256:557d12754ced38ca862f38014c2a8a1a16c540f79e552e25c7d3c4039132c402
- verification-reports: 12780151700; sha256:82dadd933b700353853f0d7bd75b39eeda82025447d50c8d5887672a77c2f785
- practice-logger-unsigned-release: 12780021797; sha256:fec96723caa33cf23aad9ff8a166a903e8949b3fdca5bd5de13a09a7023139db
- practice-logger-debug: 12779991679; sha256:b0c73137aa63bd5405415183130132a6cc42cf0133deefbcb4c97e65126cc359
- device-test-reports-api-35: 12779393932; sha256:6d7691ed9a3c0addf9287694828ca14429fa8ac567ef55d24ef2f8e46ff70c21
Downloaded unsigned release, verification and both device reports match their official SHA-256 metadata. Temporary-key debug builds are unsuitable as updates to the permanent-key installation.

## Limits and exact next task

JSON format 3/readers 1–3 protects practice data only, excluding take metadata/labels/M4A/WAV. Export wanted takes separately. No audio import/bulk media restore; uninstall/clear/device loss destroys unexported audio. Failed export retains the source but may leave a partial destination. Typing notes alone is not durable; use a save/leave/complete action. Abrupt process death can lose the last uncommitted timer/BPM interval (250 ms plus write latency) or an unsaved capture. Plain-text bounded JSON and empty-target transactional restore limits remain.

Owner explicitly approved Git commands for this update and release closeout because Desktop controls were unavailable. Actual existing Windows checkout was used; source is committed/pushed on main. Closing release documents follow as a separate commit/push, with final local/remote SHA and clean status verified in the handoff. Default Desktop preference remains for future work. No cloud checkout/PR or alternate repository.

Previous release next task (superseded by the current home redesign): install signed 0.6.1/build 12 over the current app, leave Click while recording on, record 8 beats at 120 BPM and confirm four count-in clicks continue through capture. Try the option off, timer pause/resume, cancellation/background silence, retained free/loop takes and export/reopen. Report phone/Android/output route and any audible issues. Then discuss desired save/storage behavior before implementation. Broader storage redesign remains deferred to that discussion.
