# Current phone and P5 regression checklist

## T1 — Block Transcribe owner-phone checklist

1. Install the signed build 83 APK over build 81 without clearing data.
2. Open a practice Block and switch between Record Loop and Transcribe.
3. Choose or change its Library audio and play it.
4. Slow to 75%, isolate a phrase and repeat A–B.
5. Jump between existing master markers and load a named section.
6. Expand Piano and play while the reference runs, then while paused. Resume the reference and collapse Piano.
7. Close Transcribe and confirm Record Loop, Notes, the timer and ordinary practice controls still work.

Owner acceptance for T1 is pending. The owner reports that the K1 Reference Piano works on their phone. This confirms the reported keyboard functionality.


Reviewed 2026-11-01 (America/Los_Angeles) for signed P5 build67. The full automated gate passes; the owner accepted build67 on November 1, 2026 as complete and running correctly. Itemized physical observations were not supplied. Use [CURRENT_STATE.md](CURRENT_STATE.md) for the signed package, exact source and verification limits; record the installed build, phone, Android version, text scale and observations. This checklist remains the acceptance reference; it does not assert independently observed results for every item. See [pre-regression review](docs/PRE_REGRESSION_REVIEW.md) for runner corrections and legacy test risks. P5 and final F1/F2 retain full automated and physical-device gates; do not dispatch them automatically from this document.

## Install, preserve and identify

1. Install the current permanent-key package over the existing app without uninstalling or clearing data. Verify Blocks, Goals, saved Routines, completed/unfinished sessions, Notes, dated Logs, references and all saved/imported recordings survive.
2. Record version/build and source from its build information. The original temporary-key 0.1.0 app is separate and has no automatic transfer. CI debug builds cannot update the permanent-key installation.
3. Preserve earlier packages and signing material. Before destructive recovery experiments, keep structured JSON, original images/PDFs and individually exported audio outside the app. Restore testing uses a spare empty installation.

## Navigation, appearance and accessibility

1. Open Blocks, Practice Now, Routines/Recent Practice, Recording, Goals, Tools, Log/Weekly View, summaries and the shared-header Settings gear. Home Library now provides L1 import/management; use the focused L1 checklist below. Preview/annotation/Block links remain later phases. Settings is in the header, not below Log. Tools → Audio Settings opens the same Settings screen.
2. Use the banner and Android Back. Unsaved Block/Goal/Routine edits require discard protection; Log must save before navigation. During practice, Continue preserves the session; Back to Main saves/checkpoints before navigation, with recoverable failure and rapid-tap protection. Separate Back to Home/Home buttons remain removed.
3. Test Light, Dark, System Default, Smooth, Sunny and Orchid. Check persistence after cold reopen/recreation, explicit override of OS appearance, System following both OS modes, native bars/icons/dialogs, contrast and selected/disabled/pressed/error/recording states. Existing installs and unknown preferences fall back to Light. Failed preference writes must retain the committed choice and offer retry.
4. Verify original image/PDF colors and reference page/zoom, Notes drafts, active practice timing, capture and temporary playback survive appearance changes. Light/Dark are generally owner accepted; detailed physical/System/accessibility checks and new palette phone acceptance are still pending.
5. At normal, 130% and 200% text, portrait/landscape and TalkBack, check Lato, readable labels, touch targets, keyboard Done/Back, dialogs, scrolling, controls and active Stop/Save reachability. Check each palette's metronome pulse and disabled/recording states. Do not infer physical contrast/display/acoustic acceptance from screenshots alone.

## Blocks, deletion, Goals and Routines

1. Validate required Name/Action, positive whole-minute duration and optional tempo. Test literal search, Active/Archived, canceled edits, Archive/Restore and cold reopen.
2. Delete an unused active and archived saved Block through confirmed Delete Permanently. Cancel must retain the Block/draft. An unsaved Block has no deletion action.
3. Goal links, distinct saved Routine references, saved free/recorded/imported takes, every session placement (including completed history), and image/PDF attachments block permanent deletion. Check the displayed counts and completed-history explanation. Remove a reference only deliberately and with confirmation. No automatic detachment, recording removal or historical rewrite is allowed; archive remains available.
4. Goals: five active maximum, validation, progress/notes/outcome, top-two Home bars, ordering, linked/shared Blocks, completion/reopen and actual explicitly attributed practice time. Progress is manual. Future Goal identity/statistics are G1–G3, not shipped.
5. Saved Routines: create/edit/reorder/delete, independent durations, Save as Routine, fresh independent starts and confirmation. Archived/unavailable references block the entire start. Preserve prior routines/history.

## Sessions, practice transport, Notes and history

1. Build/reorder a session and override placement duration without changing Block defaults. Keep session A unfinished, start B, resume each independently and confirm one builder draft remains.
2. Focus begins paused. Start/Pause/Resume/Complete/Skip must record foreground active time and retain snapshots. Reaching zero stops and cues once; overtime requires Continue. No automatic completion or advance. Background time must not count; recovery resumes paused. Abrupt loss can lose the latest uncommitted interval.
3. Check compact count/Goal/timer, bounded Description/Reference, metronome/free/loop rows and pinned horizontal actions against the accepted layout. One workspace section stays open outside transport panels. Opening one transport hides contents and retains both headers; switching a workspace header closes transport and opens that section without resetting audio, Notes draft or PDF page/zoom. Only one transport opens.
4. Notes is the footer pill. Save Note appends a timestamped entry, clears the editor after commit and keeps Notes open; Return to Practice closes it separately. Edit a new note and a legacy Earlier session note; Save Changes changes text only, Cancel Changes preserves it. Do not expect the removed Quick note/Save Block note/Save for later controls.
5. Protect unsaved new/edit drafts across panels, recreation and leaving/completing. Save failure stays recoverable. Practice notes, Block instructions, overall session notes and daily Log are distinct.
6. From another session, reopen the Block's newest-first Notes history. From Recent Practice, reopen a completed summary: display/edit only that exact session placement's timestamped/legacy notes, without changing original dates, timing or associations. Session notes require their own Save note.
7. Compact Focused Practice uses inline recording/review controls; full take management remains in the saved Block editor and main Recording. Do not expect a legacy Recordings management button within Focused Practice.

## Image/PDF references and recording continuity

1. For a saved Block, import JPEG/PNG/WebP (up to 40 megapixels/16384px per side) or an unencrypted 1–100-page PDF, at most 20 MiB. One image OR PDF belongs to a Block. Confirm bounded portrait layout, camera orientation, original bytes and archived ownership.
2. Cancel picker, fail import, choose malformed/truncated/locked/oversized/over-page-limit PDFs, replace across image/PDF types and confirm deliberate removal. Failed replacement keeps prior metadata/file. A PDF must render successfully before replacing the old reference. Missing files show retry/replacement/removal recovery.
3. In editor, Focused Practice and saved-Block recording sheet, show/hide, pinch to 6×, pan, navigate PDF pages and reset zoom. Page changes reset zoom. The compact practice viewer uses the accessible Reset zoom action; larger viewers retain visible reset controls.
4. Test reference toggles/page/zoom during free capture and phrase count-in/capture. Preserve capture, practice time, click phase and pinned Stop/Cancel; the viewer must not recreate recording ownership. Reopen references after process restart and install-over.
5. JSON and platform backup exclude reference metadata/paths/URIs and bytes. Keep originals separately and reattach after structured restore. There is no Media Library or image export yet.

## Temporary review and recorded loops (P1/P2)

1. In Focused Practice, saved-Block recording and standalone Free recording: record → Stop and review take → Preview/Stop → Save; then Re-record/Save and Discard. Only successful explicit Save adds a permanent row. Failed Save retains temporary audio for retry; Discard never removes saved audio.
2. Completed foreground loops automatically audition once after capture/focus cleanup; Keep/Save remains explicit. Incomplete/canceled/background loops must not audition or save. Free review stays manual. Background stops mic/output without automatic restart/save; process loss discards unsaved temporary files. Leaving/dismissing unresolved review must be protected.
3. Test BPM30–300, any whole loop length1–64 (including3/5/7/11/64), positive whole count-in1/3/7/12, shared default4 and independent override. Reject zero/negative/decimal/65-or-more loop lengths and overflow/resource-budget violations with useful feedback. The 15-minute execution budget is a resource limit. Count-in must not appear in WAV bytes.
4. Loop recording click is always enabled; there is no optional guide toggle. Check displayed count-in N of total, exact captured loop duration and 128-second loop capture/playback at30BPM×64beats. Keep recorded PCM16 WAV sample timing distinct from imported file repeat.
5. In full management, play/switch/stop, rename, cancel/confirm deletion and export free M4A and recorded WAV. Verify source bytes survive failed/canceled export and saved data survives recreation/reopen/update. Test permission denial/retry, interruption/noisy route, microphone unavailable, low storage and background cleanup. Physical microphone quality/onset/routing/loop boundaries remain separate acceptance.

## Imported backing audio

1. Focused Practice → Loop Setup → Add Backing Track switches creation to Choose file/Save. Import MP3, AAC-in-M4A and ordinary PCM16 RIFF WAV, mono/stereo8–96kHz, at most50MiB/15minutes. Confirm filename/duration, long-name fit and explicit Save/select without autoplay.
2. Reject incorrect type/content, corrupt/protected/multitrack/non-audio, excessive size/duration and failed/canceled provider reads. Keep prior candidate/selection and saved takes. Background/leave/restart must not leak unsaved staging or damage saved files.
3. Saved imports retain IMPORTED_LOOP classification when the creation checkbox changes; BPM/count-in do not retime them. Check repeat/Stop/focus/noisy/background/reopen, rename/delete and byte-preserving format-correct export. Preserve existing recorded/free takes. Codec/file/device joins may be audible; seamless repeat is not certified.
4. Test real phone providers, long files and acoustic joins. JSON excludes imports and their metadata/labels just like other takes. Individually export wanted files; bulk media-inclusive restore is not implemented.

## Metronome, audio Settings and Tools (P3/P4)

1. Practice and Tools share2/4,3/4,4/4,5/4,6/8,7/8; accent defaults Off; Sound+Flash/Sound only/Flash only. /8 BPM counts each eighth, not grouped/dotted-quarter pulse. Count bars even with accent Off.
2. BPM changes preserve fractional phase; meter/accent/output apply on the next beat. Whole Stop click buttons pulse on consumed-audio beats and decay between beats; Sound only stays static, Flash only has silent click PCM. Stop/background/focus loss clears pulse/output without restart. Acoustic/display latency still requires a phone.
3. Settings persists four click timbres, output default, default count-in and independent completion alarm (four sounds or Off). Previews reuse output ownership, refuse conflicting capture/playback and stop on leave/background. Shared defaults do not reset practice time/phase.
4. Block and foreground countdown completion fire once on a genuine armed running crossing, never on composition/database emission/recovery. Off/capture suppress sound but not stopping/accounting. Brief click ducking must preserve beat phase. Test real audibility/media volume/routing independently from PCM writes.
5. Tools tuner: single-note chromatic tuning with A4=440, permission/error behavior and physical low/high notes. No audio files are saved. Stopwatch/countdown: start/stop/resume/reset and zero; clocks pause on leave/background, same-process state remains paused and process death resets temporary state. No background alarm is promised.

## Log, JSON and recovery

1. Log opens today's local date. Autosave paragraphs/whitespace, wait for Saved, choose another date while typing, immediately navigate/background and cold reopen. Each queued edit retains its date; failed saves retain drafts for retry. Test local midnight and year/week boundaries; earlier days must not be overwritten.
2. Weekly View shows Monday–Sunday from the same entries, with previous/next navigation and tap-through editing. No separate weekly persistence or practice association exists.
3. Export current format7 JSON through a real document picker after saving notes. On a spare empty target, restore and compare Blocks/Routines/Goals, associations/order, dated Logs, timestamped/legacy notes, snapshots/history, unfinished practice and recovery. Readers1–7 remain supported. Reject malformed relationships/overflow and every nonempty target, including Log-only or take-only targets, atomically without mutation.
4. JSON excludes all image/PDF metadata/bytes and free/loop/imported take metadata/labels/audio. Export audio individually and preserve original references separately; restore does not recreate media. Canceled/failed exports can leave partial destination documents; source data stays intact.
5. Test actual read-only/unavailable/cloud document providers and disposable low-storage failure recovery. Never clear the owner's main installation for testing.

## Full regression and acceptance record

Run the assigned full build/unit/lint and API26/API35 native gates only after preflight review. Include P1 review, P2 extremes, P3 pulse/meter, P4 Settings/completion/Notes, all appearance choices and imported-audio checks; the historical primary workflow alone does not cover every later increment. Keep seed/reopen/install-over ordering and original safety assertions. Isolate failures with logs/screenshots before repeating a full matrix.

Record exact source, commands/workflows, actual counts/failures/cancellations and limitations. Compilation, emulator passes, signed artifact integrity and physical acceptance are separate. Stage3 detailed phone acceptance remains pending. [EVALUATION.md](EVALUATION.md) remains0/10 documented real sessions; synthetic tests do not count. Future Media Library/recovery/Goal phases require their own assignments and dependency gates.


## Build69 reference camera/gallery phone checks

Use the short PHONE_CHECKLIST in the current delivery linked from [CURRENT_STATE](CURRENT_STATE.md): files/image/PDF, capture/orientation, gallery, cancellation/replacement, recreation, unavailable camera and enlarged-text layout. Owner confirmed build69 camera/gallery works correctly on November3,2026; no itemized device/edge-case observations were supplied.

## L1 owner-phone checklist — 0.18.0/build72

Build69 camera/gallery functionality was owner confirmed on November3,2026. P5 remains complete; do not repeat its full checklist for L1. These L1 checks are pending owner verification, independent of automated results.

1. Install the permanent-key L1 APK over build69 without uninstalling/clearing; confirm existing Blocks, references and recordings remain.
2. Home → Library: import one image, one PDF and representative MP3/M4A/PCM16 WAV audio files. Check a failed/canceled import leaves saved items intact.
3. Search, filter, sort and rename Library assets.
4. Force-close/reopen and confirm files remain.
5. Confirm deletion of one Library item leaves unrelated originals/legacy media intact.
6. Recheck a previously saved imported backing track (play/rename/export) and camera/gallery reference.

Library previews/annotations are L2; Block links are L3. JSON backups exclude Library media: keep originals separately until S2. Next implementation phase, only when separately assigned: **L2 — Media Preview and Master Audio Annotation Workspace**.

## L2 owner-phone checklist (pending)

Install the final L2 permanent-key APK over build72 without uninstalling or clearing data. Actual automated verification and package source are authoritative in CURRENT_STATE.

1. Open a Library image/PDF, zoom/reset, and navigate PDF pages.
2. Open MP3/M4A/WAV; play, pause, stop and seek while playing.
3. Slow to75% and50%; confirm pitch stays unchanged on the phone.
4. Zoom to a short phrase, set/fine-tune A/B and repeat; check useful phrase joins at slower speed. Very short compressed loops can have codec/output gaps; seamless/sample-accurate joins are not certified.
5. Add a point marker and named section, edit their labels/notes/positions and try marker navigation. Loading a section does not enable repeat automatically.
6. Close/reopen (including app restart) and verify media and master markers; check rotation, landscape, enlarged text, speaker/headphones, interruption and background stop without automatic resume.
7. Confirm existing Block recordings and image/PDF references still work.

L1 phone acceptance remains separately pending. JSON7 excludes Library media and master annotations; keep originals separately until S2. No phone result is inferred from automated tests.

## L3 owner-phone acceptance — pending

L1 and L2 phone acceptance remain separately pending. Emulator checks do not close these items. Use the final L3 APK linked by CURRENT_STATE; K1/T1 features are not part of this checklist.

- [ ] Install over build76 without uninstalling or clearing data.
- [ ] Attach the same Library PDF to two different saved Blocks.
- [ ] Attach the same Library audio file to both Blocks alongside the PDF.
- [ ] Replace and unlink one reference; verify the other Block and Library original remain intact. Confirm referenced Library deletion is refused.
- [ ] Open the PDF during Focused Practice and recording; change pages/zoom, then use pinned Stop and review actions.
- [ ] Confirm existing imported backing tracks and camera/gallery references still display/play/export. Try new camera/gallery import; deliberately confirm any legacy visual replacement.
- [ ] Reopen the app and confirm links remain intact. Check portrait/landscape and enlarged text.

JSON7 does not include Library originals, associations or master annotations. Keep originals separately until S2. The old Loop Setup importer remains available during L3; its convergence with canonical audio selection belongs to T1 after K1.


## K1 — Reference Piano owner-phone checks

1. Install the current permanent-key update over build80 without clearing data.
2. Home → Tools → Reference Piano: no microphone permission prompt; confirm readable compact keyboard.
3. Play/release single white/black notes, intervals, unisons and triads; rapidly repeat and slide between keys. Check accurate pitch, sustained sound, clean releases and comfortable volume.
4. Shift down/up by an octave; check C1/lower guitar and upper registers, plus C4–C5 reference pitches.
5. Rotate and try enlarged text; keep keys and Back reachable. Leave/reopen/background while holding notes and confirm silence with no automatic restart.
6. Check Tuner and Metronome, Timer and Audio Settings.
7. Embedded audio coexistence is checked by the K1 harness; owner acoustic headphone/speaker/vendor interruption acceptance remains separate. T1 exposes it in Focused Practice.

The owner confirms the K1 keyboard works on their phone. Individual acoustic measurements and itemized checklist results were not supplied. Actual K1 automated results and signed package: CURRENT_STATE.md.


## T2 - Owner phone checklist

1. Install the T2 signed update over build84 without uninstalling or clearing data.
2. Link two Blocks to the same recording. Set different speeds and A-B ranges, then reopen both. Check that each resumes at its own position with playback paused.
3. Add a local marker and customize a Master marker in one Block. Confirm the other Block and Library retain their original markers.
4. Publish with Add to Master or Update Master. Check the Library and the other Block. Hide and reset a marker to check inheritance.
5. Open Piano, switch to Record Loop and complete a normal practice Block.


## T3 owner phone checklists

Run each checklist on its separately assigned signed install-over build. The owner reported that T3A works after installing and testing it. The owner also reported that T3B works. Individual checklist observations were not supplied for either release. See the [Standalone Transcribe plan](docs/T3_STANDALONE_TRANSCRIBE_PLAN.md) for focused engineering checks.

### T3A — Standalone Transcribe Workspace

1. Install over signed build86 without uninstalling or clearing data.
2. Open **Practice Now**. Check **Add Blocks** and the draft after Back. Start a saved Routine directly. Resume an unfinished session when one is present.
3. Open **Transcribe** without creating a Block. Choose an existing Library recording or import an MP3, M4A or WAV. Cancel a subsequent selection and confirm the original choice stays.
4. Set playback to **75%**, seek and zoom, and listen for the original pitch. Set an A–B phrase, repeat it, and try **Play selection** once.
5. Add and edit a master marker. Open an attached Block to check that inherited markers reflect the edit and a local override keeps its own values.
6. Expand **Reference Piano**. Play a chord with the reference running and paused. Drag the range, jump an octave, collapse, and resume playback.
7. Rotate, background and exit while playing. Verify playback stays paused on return and output stops on exit. Check the existing Block sessions, Routines and Library.

### T3B — Standalone Timer and Goal Attribution

1. Install over signed build87 without uninstalling or clearing data.
2. Open **Practice Now → Transcribe**. Play a reference before pressing **Start** and confirm **00:00:00** stays visible.
3. Press **Start**, transcribe briefly, pause playback and confirm the stopwatch continues. Tap **Stop & Save**. Check that **Home → Total practice** and **Log → Weekly View** increase once.
4. Repeat with an active Goal selected and verify its time increases once and manual progress stays the same.
5. Background a running activity and return. Confirm it stays paused with its saved time. Resume explicitly, then leave and reopen Transcribe to check the unfinished activity is retained.
6. Check existing Block sessions, waveform, marker editing and Reference Piano. Try the controls in landscape and with enlarged text.

### T3C — Record Reference Directly in Transcribe

1. Install over build88 without uninstalling or clearing data.
2. In **Practice Now → Transcribe**, tap **Record Reference**. Check microphone denial and grant. Start recording, watch the elapsed time, and tap **Stop and Review**.
3. Play the preview. Try **Rerecord** and **Discard**. Keep a new recording and check that it opens in Transcribe for slowdown and A–B looping. Confirm one new Library original and no ordinary take.
4. Open a Block in Transcribe and record a replacement. When the old reference has local playback settings or markers, cancel the replacement first and confirm the old link and edits survive. Then confirm the replacement and verify the old original and its master markers are still in the Library.
5. Start with a reference playing and confirm it pauses before recording. Background during capture and check that the microphone stops. Check ordinary Free Record and Record Loop separately.

### T3D — Concurrent Transcribe Playback and Free Record

Physical owner-phone acceptance is pending. Record the Android version, device and route for each check.

- [ ] Install the signed T3D APK over build90. Open a Block's Transcribe panel, choose a canonical reference, start the Block timer, set 75% speed and repeat a short A–B range. With headphones connected, tap **Record Take** while the reference plays. Check that it keeps playing through a loop boundary and the pitch stays steady. Verify the recording status and **Stop and review take** remain reachable, including at enlarged text.
- [ ] While recording, pause and resume the reference, seek, change speed, and press the reference's **Stop**. The mic should continue. Resume playback, then stop the mic. Opening **Review Take** should let the reference continue from its current position with speed and range intact. **Play Preview** pauses the reference. Check that preview completion or Stop Preview leaves the reference paused until you press Play.
- [ ] Try **Re-record**, **Discard**, and **Save**. Re-record creates a replacement temporary take. Confirm one saved Free Record take under the Block, playable and exportable, with the original Library audio and its master/local markers intact. Check the existing Block timer and Goal totals count practice once. Repeat with the reference paused when starting capture.
- [ ] Repeat with the phone speaker. The microphone may pick up sound from the speaker. Listen to the take to verify usable capture for that setup.
- [ ] During capture, background the app, rotate, unplug headphones, connect or disconnect Bluetooth, and trigger an audio interruption. Check that capture stops on foreground loss, device connection changes or playback focus loss. Valid temporary audio should remain available for review where possible. Neither stream should restart automatically on returning to the app. Check microphone denial and recovery, rapid Record/Stop, and that leaving the session requires resolving the pending take.
- [ ] Check **Record Reference** separately in Block and standalone Transcribe, ordinary Free Record, Record Loop, Reference Piano and metronome. The build90 keyboard **Done** BPM check and the T3C phone checklist also remain pending.

## S1 — Organized Audio Export phone checks

Use disposable folders and sample takes. Record the phone Android version and the installed Drive and OneDrive versions. Repeat for local storage, Drive and OneDrive where each appears in the system picker.

1. From the Settings gear, open Storage. Choose a writable folder. Cancel Change Folder and confirm the previous selection remains. Restart the app, export again, then select a different folder and confirm only new exports go there.
2. Export a Block free take, a recorded WAV loop, a standalone free take and an imported backing track. Check the date is the saved take's local date and verify the file plays with its original format. Repeat an export, export from two same-name Blocks and rename one Block. Check that earlier copies remain and all original takes still play in practice.logger.
3. When automatic export fails, use Save As and select a writable location. Verify that an unavailable provider can be replaced with a local destination. If the provider appears only in Save As, record that result separately.
4. Forget the folder and confirm Export audio still opens Save As. Re-select, then revoke access, move the test folder, sign out or go offline as appropriate. Check that the app explains the failure and offers Choose Folder and Save As.
5. Interrupt a large sample export to a disposable destination. Check that the original stays playable and that an incomplete copy is removed or reported. Retry to a new document. For cloud providers, verify the completed copy separately after reconnecting.

A provider accepting the write does not establish cloud synchronization.

## S2 complete portable backup phone checks

1. Install the signed update over build93. Keep your current installation and its data. Check that existing Blocks, time totals, visual references and takes still open.
2. Prepare a complete ZIP from Tools → Backup and restore. Save locally using Save ZIP as and confirm the read-back result. If Settings has a suitable selected folder, also check that a new ZIP appears under Backups without replacing earlier copies.
3. If Drive or OneDrive is available, try each separately. Record which picker, write, read-back and folder operations succeed. Check that the saved file is visible from a second client after sync. For a disposable destination, try revoked access or an interrupted copy and check the app's cleanup or retry message.
4. On a separate empty installation, choose the ZIP, verify the preview and cancel once. Reopen it and confirm restore. Restart the app. Check the same Blocks, Goal and Routine links, Logs, notes, practice totals, both independent audio annotation sets, PDF pages, recordings and imported loop. Confirm unfinished timers are paused and portable appearance/audio settings were applied. Select export-folder access again if needed.
5. On the spare now-populated installation, try selecting the same archive again. Confirm the app refuses it and the restored data stays available.

## O1 practice overlay phone checks

- Install the permanent-key update over the current practice.logger app. Open existing Blocks, sessions and references to confirm your data is present.
- Open **Reference Material → Markings → Edit markings** on a saved image or PDF. Draw Highlight, Cover and Focus rectangles. Drag a selected shape by its center and resize it from a corner. Change a highlight color, delete a shape and undo it.
- Hold **Peek** over a page with a Cover, then release it. Confirm the cover returns. Try Hide/Show markings. Tap **Done** to return to pinch zoom and panning. Rotate and reopen the reference, and check alignment at enlarged text and with TalkBack.
- On a multipage PDF, confirm that each shape appears on its own page. Copy the markings to another Block using the same Library original and edit one copy. Cancel and then confirm removal of a test reference with markings.
- While recording in Focused Practice, edit or peek at a marking and change pages. Confirm the timer and recording continue and the Stop control stays reachable.
- Save a complete ZIP and restore it on a separate empty installation. Reopen the app, find the expected markings and edit one. If you back up to Drive or OneDrive, confirm the provider finishes syncing before relying on the copy.


## G1 Goal priority and primary association phone checks

1. Install over build95 with the permanent key. Open the Block Library and check an archived Block, a sole legacy Goal link and a Block with multiple legacy links. Verify the older links still appear in Goal management.
2. Assign primary Goals to several Blocks. Check the five colors in priority order, a black No Goal cap and accessible names and ranks with TalkBack. Check the Block Library, Practice Now → Add Blocks, Session and Routine builders and Goal-associated Blocks. Repeat at enlarged text in Light, Dark and the other appearance choices.
3. Move Goal C from third to first. Check that C is red, A orange and B yellow. Complete C and verify its Blocks show a completed label and hollow cap. Reopen C and check its new active rank.
4. Practice and pause an assigned Block in an ordinary session, a Routine and a Goal-launched session. Change the Block’s primary Goal and resume the unfinished placement. Verify the visible Block association updates and the existing practice totals increase only by newly committed time. Skip a separate placement before starting it.
5. On a disposable empty installation, restore a ZIP v3 and restart. Verify the primary assignments, editable markings, originals, notes, practice history and totals. Keep the owner’s existing installation and data in place.

## Android display compatibility phone checks

Use the signed compatibility candidate linked in [CURRENT_STATE](CURRENT_STATE.md). Install it over the permanent-key app and check that a saved Block, session and existing take are still present. Keep the same saved practice fixture while changing display settings.

1. On the Galaxy S26 Ultra, open **Practice Now → active Block → Loop Setup**. At default text and display size, check that BPM, Loop beats and Count-in read clearly above centered values. Edit all three. Increase font and display size, check the new arrangement, and confirm every value survives rotation and the keyboard. Scroll to **Record Loop** with the keyboard still open. Record a short loop, preview it, and save it. Repeat with a count-in cancellation and check that the canceled take is absent.
2. Open **Record one phrase** from a saved Block's recording sheet and repeat the label and keyboard checks. Try **Free recording**, **Stop and review take**, and **Discard**. Open a reference during capture and verify that **Stop** stays reachable.
3. In Focused Practice and Tools, test **−1**, **+1**, the meter, **Accent**, the expansion control, **Apply BPM** and **Start/Stop click**. Check that the running button flashes in the existing flash-enabled modes. Expand and collapse the panel with a PDF or a Notes draft open and verify the prior content returns.
4. Repeat on a narrow phone and a large screen. Use default, intermediate and maximum text sizes, then change display size. Try landscape and both gesture and three-button navigation where available. Scroll to each action at the smallest usable viewport.
5. Check Home, Settings, the Block editor, the Session builder, Goals, Routines, journal and native pickers. In Library audio and Transcribe, reach the waveform, seek, A–B and markers at large text and in landscape. Check Light, Dark and a custom theme. With TalkBack, verify that the numeric fields announce **Phrase BPM**, **Loop beats**, and **Count-in beats**, and that the metronome announces expansion and the Accent checkbox state.

## W1 This Week phone checks

1. Install build98 over the existing permanent-key app. Confirm saved Blocks, unfinished sessions, Journal entries and media still open.
2. Open Practice Now → This Week. Confirm the current Sunday–Saturday range. Visit the previous and next weeks and confirm that neither inherits Blocks automatically.
3. In Prepare Week, add an existing Block and create another. Carry forward from an earlier week, repeat the carry, and confirm each Block appears once in saved order. Remove a membership and confirm its Block is still in the Library. Try Add to This Week from the Library and editor.
4. Tap a single Block and confirm practice opens paused. Leave it unfinished, return to This Week, select two Blocks and confirm the existing builder opens with them in weekly order. Open In Progress and resume an unfinished session.
5. Close and reopen the app and verify both plans. Create a complete ZIP and restore it only into a separate empty installation to check weekly membership and media together.


## Compact metronome phone check

Install build101 over build100 on the Samsung S26 Ultra. Open a Block at your usual font and display sizes. Check that Metronome/BPM, Accent, expand, Start click, −1 and +1 share one row above Free Recording. Start and stop the click, adjust BPM, and open and close the metronome settings. At enlarged text, check that the controls can wrap and remain reachable.

## P1 Intentional Practice Lifecycle phone checks

1. Install the permanent-key build100 over build99. Confirm a saved Block, unfinished session, Goal, note and recording still open.
2. Open Blocks from This Week (single and multi), Add Blocks, Routines and a Goal. Check Ready shows the Block, position and planned duration. Wait at Ready, then tap Start Block and check the timer moves. Turn off system animations and confirm the Start highlight is static.
3. Choose Prep Block. Play a reference, use the click and make a short recording. Confirm the countdown stays fixed until Start Practice. Check that an unsaved note or unresolved take still protects Complete, Skip and leaving.
4. Pause and resume. Leave the app during both Prep and active practice, then reopen the unfinished placement from In Progress. Confirm it waits for your choice. At zero, tap Continue to enter overtime.
5. Complete or skip a Block. Confirm Up Next waits, then try both Start Next Block and Prep Next Block. Finish the final Block to reach Session Summary. Repeat at maximum text size on a narrow display. Test complete ZIP restore on a separate empty installation.

## G2 Shared Goal Filtering phone checks

1. Install the signed build99 APK over build98 without uninstalling. Open the Library and This Week to confirm the existing Blocks and weekly plans are present.
2. In Manage Blocks, try All Goals, an active Goal and No Goal. Combine a Goal with a name, action or notes search, and switch between Active and Archived. Confirm a completed Goal's Block and label are visible under All Goals.
3. In Practice Now → Add Blocks and in a saved Routine editor, add Blocks from different Goal filters. Change the filter again and confirm the existing placements and order.
4. In This Week → Prepare Week, choose a Goal and add an existing Block. Confirm it appears in the week and is removed from the available results. With enlarged text, check that the filter menu scrolls and can return to All Goals.
