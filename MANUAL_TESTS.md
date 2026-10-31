# Build42 phone visual check

Build42 visual preview - phone acceptance pending
1. Install over the existing app without uninstalling/clearing data; confirm your Blocks, sessions, notes, references and recordings remain.
2. Open Blocks, Practice Now, Routines, Recording, Goals, Tools, Backup, Log/Weekly View and a session summary. Separate Back to Home/Home buttons should be absent and following content should move up.
3. Tap the green practice.logger banner to return Home. Check Android Back and existing edit/discard/session-exit guards.
4. Edit Log, return through the banner and reopen: text must remain. Check library search, keyboard and enlarged text on your phone.

The retained checks below describe existing features; they were not all rerun for build42.

# Current phone checklist — Notes history/editing,0.16.0/build42

Updated 2026-10-30 (America/Los_Angeles). Pending phone acceptance; use current signed delivery in CURRENT_STATE and record phone/Android/build/observations.

## Workspace/footer visual check

Description should start open even with a reference attached. Switch either header: one section remains open. Notes sits beside the footer actions; Save for later and its old row are gone. Transport sits lower. Check Notes open/close, PDF page/zoom restore, Back to Main/resume, enlarged text wrapping and keyboard access.

## Retained Notes preview checks

1. Install build42 over the existing permanent-key app without uninstalling/clearing it. Confirm old Blocks, sessions, recordings, references, placement notes and daily Logs remain.
2. Open Notes for a practiced Block: see legacy notes from earlier sessions. Add two notes with Save Note; each appears as its own timestamped history entry, editor clears, and panel stays open. Expand/collapse full text; Return to Practice closes separately.
3. Edit a saved new note and an earlier-session note; Save Changes updates only that text and keeps its original date. Cancel Changes keeps the saved text. Unsaved new/edit text should survive panel switches; leaving/completing asks before discarding it.
4. Start from Reference expanded on a later PDF page/zoom. Open Notes, free/loop lists and loop setup; both upper headers should collapse. Switch panels directly; only one expands. Close the final panel and verify reference/page/zoom restored. Repeat from Description and with one workspace always open.
5. Check a long Notes history with persistent scrollbar, editor + Save Note while keyboard is open, portrait/landscape and enlarged text. Main controls/actions remain accessible; compact controls may scroll in constrained space. Try notes during free recording and with metronome/timer running; toggles must not stop/recreate audio or practice.
6. Close/reopen app and revisit this Block from another session; old and new notes/edits remain. Export JSON7 and restore only into a spare empty installation: notes/history should match. Legacy1–6 imports remain supported; older app versions cannot import format7. Keep original media/export audio separately as before.

The broader retained checks below are future full-release/phone references and were not run for this preview. Earlier single Quick note instructions are historical; Focused Practice now uses Save Note/Save Changes and separate Return to Practice.

## Install and preserve data

Install over the existing permanent-key app **without uninstalling or clearing it**. Confirm existing Blocks, Goals, Routines, notes, unfinished practice, history and recordings remain. The old temporary-key 0.1.0 app is separate; no automatic transfer is provided. Test restore only in a spare empty installation. JSON excludes reference images/PDFs and all audio; keep original images/PDFs separately and export wanted takes individually.

## Daily Log and Weekly View (Increment 1 retained acceptance)

1. Install over the existing permanent-key app; confirm old Blocks, Goals, Routines, history and recordings remain. Home → Log must open today's local date with a large plain-text editor. Write several paragraphs, punctuation and blank lines; wait for Saved, close/reopen and edit again.
2. Choose date → yesterday, write different text and immediately return Home. Reopen Log: today must retain its own text. Revisit yesterday and edit it. Alternate dates rapidly while typing; dismiss the keyboard and background immediately. Check both texts after a cold reopen; no text may cross dates.
3. Leave Log overnight or advance the device date in a controlled test. Reenter/resume Log: the new local day's editor is blank if unused; earlier days remain intact and editable. Also check a late evening before UTC midnight and a year/week boundary.
4. Weekly View shows seven chronological Monday–Sunday dates, full saved text and No entry for empty days. Navigate Previous/Next week; select an empty or populated day and edit. Its next weekly display must reflect that edit. No separate weekly record exists.
5. Export format6 JSON and restore on a spare empty installation; compare dates, whitespace and complete Log text as well as existing practice records. Try a legacy version1–5 file on another empty target. A target containing only a Log must refuse restore. Never clear/uninstall the main app to test this.
6. At normal/200% text, check date picker, Back to Home, weekly controls, scrolling long entries and keyboard access in portrait/landscape. After a later in-place update, confirm both Logs and existing recordings/data remain. Report phone, Android/build, timezone and any failures. These phone checks remain pending.

## Safe permanent Block deletion

1. Create an unused active Block, reopen its editor and scroll below Archive to Delete Permanently. Cancel: saved Block and unsaved draft stay intact. Confirm: return to Library, row disappears, and it remains absent after restart.
2. Repeat with an unused archived Block using the Archived filter. A new unsaved Block has no deletion action. Check that a canceled confirmation leaves the editor usable.
3. Try Blocks linked to active/completed Goals, saved Routines (including duplicate placements in one Routine), and saved free/loop takes. Verify Goal association, distinct-Routine and recording counts. Refusal changes none of that data. Export wanted audio before manually deleting an unwanted take.
4. Try a Block in the builder draft, an unfinished session and completed history. All are protected. Completed history must be explicitly explained; removing Goal/Routine associations cannot bypass it. Archive remains the alternative.
5. Export JSON after deleting an eligible Block; on a spare empty installation restore and compare remaining Blocks/Goals/Routines/history/notes. Confirm recordings still play in the original app; JSON does not restore them.
6. Check action/dialog buttons at normal and 200% text, scroll a long refusal explanation, Cancel/Back and editor recreation. No long-press interaction is included.

## Blocks and session recovery

1. Create/edit/search using Name/Action/notes, required-field validation, positive whole-minute duration and optional positive whole BPM. Invalid values must not save. Archive/cancel/restore; confirm stable records after restart.
2. Home → Practice Now: add three active Blocks, reorder them and override one placement duration. Saved Block defaults stay unchanged. Leave/reopen the draft and start it.
3. Complete one Block early, Skip another, Pause/Resume the last, then reach zero and explicitly Continue overtime or Complete. Each next Block begins paused; active time/outcomes and historical snapshots must be accurate.
4. Use Save Block note, edit and Save for later/Back, reopen via Practice Now, restart and verify paused recovery with saved note/order/time. Complete/Skip saves the final quick note; session notes require Save note. Typing alone is not durable against abrupt death.
5. Keep session A unfinished, start B, save both, then use Practice Now's dated Continue controls to resume each independently. Complete B and confirm A remains. One shared builder draft is retained.
6. Home → Routines → Recent Practice: reopen summaries after editing/archiving the reusable Block. Names/actions/reference snapshots, actual time/outcomes, notes and last-used click BPM remain truthful. Background time must not inflate practice time.

## Goals and saved Routines

1. Goals: create/edit title/outcome/notes/manual 0–100% progress; test the five-active limit, priority/top-two Home bars, completed/reopen and shared existing Block links. Archived links are explicit. Practice Goal creates fresh paused sessions, preserves drafts/unfinished practice and attributes actual committed time; manual progress does not auto-update.
2. Routines: create/name/add/remove/reorder/duration/edit/restart; test Save as Routine from the builder without changing its draft. Duplicate placements are allowed. Start twice and verify independent sessions using current Block defaults/snapshots and saved placement durations.
3. Archive a referenced Block: the Routine must refuse its entire start until repaired/restored. Cancel and confirm Routine deletion; only the template/its placements disappear, with old practice/history/audio unchanged. Back from unsaved editing confirms discard.

## Click, recordings and Tools

1. In practice, start/stop/adjust click at 30–300 BPM while the timer is paused/running. Last actually used BPM appears in history without overwriting the saved Block reference. Repeated Start/Stop, leaving/background/rotation and audio route interruptions must stop audio with no automatic restart. Judge real audibility/steadiness at normal media volume.
2. Block Recordings: deny/grant microphone permission, record/save at least two M4A takes, listen, switch playback, rename, cancel/confirm deletion, export and play an M4A outside the app. Practice timer controls stay independent during capture. Rename/archive/revisit the Block and confirm the same saved takes.
3. Record phrase at 120 BPM / 8 beats: four clicks, then one fixed phrase and repeated playback. Click while recording defaults on; off gives count-in then quiet capture. Speaker bleed is acceptable. Try4/8/16/32 beats and another tempo; recorded audio must keep its original tempo/pitch when the working click changes. Listen across ten repeats for onset/boundary problems; no seamless or latency-free claim.
4. Retain two loops and a free take; rename/restart/play/export WAV, which contains one phrase. Cancel count-in/capture and background/leave/recreate: incomplete phrases are discarded explicitly; saved takes remain; no background mic/guide/playback or auto restart. A very short/invalid free capture may fail without harming prior takes.
5. Recording → Free recording starts a standalone take immediately when permitted, or requests permission then starts after grant. Save two takes and browse Saved free recordings without starting the microphone. Restart/play/rename/export. These takes never create a synthetic Block or appear among a Block's takes.
6. Tools → Metronome: centered BPM, ±1/±5, exact tempo behavior and lifecycle silence. Tuner: permission on Start, real single-note response against a trusted A4=440 reference, cents/direction, silence/noise and low/high notes; no files are saved. Emulator pitch tests do not establish phone acoustic accuracy.
7. Tools → Timer: stopwatch Start/Stop/Resume/Reset; roll whole-minute countdown, pause/resume/reset and let one minute finish atzero / Time's up. Clocks pause on leaving/background; same-process reentry preserves paused values, process death resets temporary tool state. This is not a background alarm.

## JSON backup, accessibility and evaluation

1. Tools → Backup and restore → Export backup through the actual phone document picker; wait for Backup saved. Commit notes first. Cancel picker/import preview and verify unchanged data. Keep the plain-text backup private; failed exports may leave incomplete files.
2. On a spare empty installation, restore the complete format6 export, compare Blocks/archive, Routines/order/durations, Goals/progress/links/attribution, notes/history/draft/unfinished practice and restart paused. Reject nonempty restore/malformed data without mutation. Legacy1–5 remain readable; compare original local dates and Log text as well. Do not clear the owner's main app for testing.
3. Record actual document provider, unavailable/read-only-provider errors/retry and disposable low-storage behavior. Confirm original recordings remain and exported M4A/WAV plays externally; JSON contains no audio/take metadata/labels.
4. At normal/200% text, landscape and TalkBack check shared Lato/cream/green/banner, Home Goal bars, Back to Home, keyboard/scrolling and reachable controls/dialogs. Specifically report Phrase BPM label/range, keyboard Done/Back dismissal and access to lower phrase controls; old screenshots did not establish reliable Done dismissal.
5. Detailed Stage 3 phone backup/recovery acceptance remains pending. Resume [EVALUATION.md](EVALUATION.md) when the owner is ready; it currently records0/10 real sessions. Save observations in existing optional notes and report concrete friction. Synthetic CI sessions never count.

Actual automated release results and remaining phone acceptance are in CURRENT_STATE.md. Passing emulator checks do not complete this phone checklist.


## Increment 2 — Historical image acceptance checklist (superseded where noted)

This records the build 26 scope. For current PDF and recording behavior use the Increment 3 checklist below; its recording-sheet integration supersedes the image-only limitations here. Install the actual signed version in CURRENT_STATE.md over the permanent-key app without uninstalling/clearing it. Record phone/Android/build/text size and actual pass/fail; phone results remain pending.

1. Before updating, note saved Logs, Goals, Routines, Blocks, unfinished/history notes/BPM and a playable recording. Install the signed update in place and confirm all remain accessible and audio plays.
2. Create/save a Block; reopen it, Add reference image and select a local JPEG. Reopen/process-restart and confirm Show reference persists. Also try PNG/WebP, portrait photos and normal/200% text, landscape and TalkBack.
3. Start practice with that Block: initially collapsed, Show/Hide repeatedly, pinch zoom/pan and Reset image zoom. Confirm fitted aspect ratio, bounded height, recovered layout/normal scrolling, timer continues, click stays active at its BPM and saved notes/time stay correct. Move to an image-free Block; no prior image appears. Recording remains its existing modal sheet with no image integration.
4. Replace the image and restart; new image persists. Cancel the picker and cancel removal; attachment stays. Confirm Remove image; removal persists and app-owned file cleanup succeeds. Try permanent deletion on an otherwise unused image-bearing Block: refusal explains Remove image first; after confirmed removal, deletion is allowed if all other dependencies are absent. Archive a Block and confirm its existing practice reference still works.
5. Try canceled picker, corrupt/missing/unsupported/oversize image and low storage (use a disposable test installation for injected corruption). Existing image/data must survive failed replacements; inline viewer errors allow Retry or editor replacement/removal. Original source image stays untouched.
6. Export JSON6, verify Logs/structured records persist, restore into a spare empty installation and confirm no missing-media attachment is recreated; reattach the separately retained original. Existing nonempty-target refusal remains. Check ordinary Goal/Routine/Log/recording workflows and do not infer simultaneous image viewing while recording.

No media-inclusive backup, PDF, multi-image gallery, annotation, camera capture or recording-view integration is included.


## 0.14.1 / build 27 — Portrait photo and compact practice refinement

Install in place without uninstalling/clearing data. Reopen an already-attached portrait camera photo: it should display upright without reattachment; original image bytes stay intact. Verify the Block count/name/action/BPM use compact spacing and the reference itself reaches both safe phone edges. A normal portrait page should use the phone width, with a taller aspect-preserving region; very tall images remain bounded and can be zoomed/panned. Show/Hide must return space to the practice controls. Repeat with timer and click running, normal and 200% text, then check saved notes/BPM/time and a landscape image. Test the owner’s photographed music page specifically: automated tagged fixtures do not establish acceptance of that original file. No in-app image editing or change to JSON/media backup is included.


## Increment 3 — PDF references and recording continuity (phone acceptance pending)

1. Install the new permanent-key APK over the current app **without uninstalling or clearing data**. Check existing Blocks, archived photo references (upright/full-width/zoom), Goals/Routines, dated Logs/Weekly View, notes/history and playable old takes.
2. Save/reopen a Block. Add image or PDF → choose a small single-page PDF, then a multipage portrait/landscape score. Reopen after force-stop: one reference persists. Originals remain in their original location.
3. Show/Hide repeatedly; check page counter, Previous/Next, portrait/landscape aspect ratio, pinch/drag/Reset PDF zoom and zoom reset on page change. Check normal and200% text, landscape and TalkBack. Controls should scroll into view without accidental sheet dismissal.
4. Replace image→PDF and PDF→image; cancel the picker and removal confirmation. Try corrupt, password-protected and oversized PDFs: the prior reference remains usable. Confirm deliberate removal, then verify deletion blockers and Archive ownership.
5. During active practice set BPM/start click and timer; save a quick note. Open Recordings, start a free take and repeatedly show/hide/zoom/pan/change pages. Timer/BPM/notes and recording elapsed time must continue. Stop and save take must stay visible above the scrolling reference. Save, play, rename and export the take; verify one complete take without duplicates.
6. Start a fixed phrase/count-in with its reference available. View/change pages during capture; check automatic save/loop playback and Cancel phrase. Close the sheet deliberately during a separate free take and background the app during another: retain existing foreground-only stop/save/discard behavior. Reopen without automatic recording restart. Standalone Free recording stays usable and has no reference viewer.
7. JSON export/restore on a spare empty app retains structured data/Logs but creates **no image/PDF or take rows**. Keep original images/PDFs separately and export desired takes individually; reattach references after restore. Never clear the owner’s main app to test restore.

Automated emulator checks exercise real PDF rendering/encoding and debug-signed install-over. Permanent-key phone upgrade, vendor pickers, acoustic quality/loop timing, landscape and TalkBack remain owner checks; do not infer acceptance from emulator success.


## Increment 4 — compact practice, completion cue and protected exit

1. Install the current permanent-key APK in place without uninstalling/clearing. Compare old Blocks, Logs, Goals/Routines, unfinished/history snapshots/notes/BPM, references and playable takes.
2. On a normal portrait phone at normal font size compare with the owner's sketch: Block count/optional Goal and small right timer, bounded Practice Description then Reference Material, compact BPM/±1/click, free and loop rows, horizontal bottom actions. Ordinary instructions with collapsed reference must need no scrolling to operate these controls. Check 48dp touch targets and readable Lato/cream/green contrast.
3. Expand Notes, write/save a Quick note and reopen it. Long instructions scroll inside their bounded area. Expand an upright portrait image and multipage PDF; compact Reset stays visible on the zoomed viewport, and Show/Hide, pinch/pan/reset/page controls work while timer/click and free/phrase capture continue. At 200% text and with keyboard open allow workspace scrolling; bottom controls and active Stop/Save remain reachable above Android navigation/IME. Check landscape and TalkBack separately.
4. Let a running Block reach zero with click OFF, then ON at a distinctive BPM. Hear one short recognizable two-note cue at ordinary media volume, with brief click ducking, no lasting interruption/reset or BPM change. Verify visual zero even with media muted/unavailable route. This phone listening check is mandatory: event/PCM/native write assertions alone do not establish speaker audibility or acoustic mixing quality.
5. Pause before zero: no cue. Recompose/show-hide/reference/page/recording controls, rotate, save/reopen an already-expired session and cold-reopen the app: no replay. Check foreground-only elapsed time, explicit Continue overtime and Complete/Skip outcomes; no automatic completion/advance.
6. Use direct Record → pinned Stop and save take → Play latest free take. Use Record Loop → existing BPM/beat/count-in/capture options → loop playback/Stop. Browse Recordings for older takes/rename/export/delete. Stop/Save stays visible during scrolling; Complete/Skip/exit cannot discard active capture. Background/rotation/deliberate sheet dismissal retain existing safety and no automatic restart.
7. With running timer/click and an unsaved Quick note tap banner: Leave current session? → Continue leaves note/timer/click untouched. Repeat → Back to Main saves/pauses/stops click and preserves unfinished session. Practice Now resumes it paused with note/time/BPM intact. Android Back uses the same dialog. Try rapid taps; no duplicate completion/save/navigation. On a disposable error-injection build, failed note/checkpoint save stays on practice with a retryable error and preserved text.
8. In dirty Block/Goal/Routine editors tap banner, cancel discard and verify all edits remain; confirm discard then verify Home. Leave an edited Log through banner and verify save/retry barrier. Do not clear the owner's phone for failure testing.


Closeout2026-10-29: signed0.16.0/build34 is a candidate. Targeted checks pass; complete release regression remains incomplete after legacy UI test failures. The owner stopped further work. No physical-phone acceptance is claimed. See CURRENT_STATE.md for exact results and remaining work.

## P1 temporary take review — build44

Current P1 phone acceptance is pending. Use the versioned PHONE_CHECKLIST.txt linked from [CURRENT_STATE.md](CURRENT_STATE.md). In Focused Practice, saved-Block recordings and standalone Free Recording, Stop → Review Take → Play/Stop Preview → Save; then repeat with Re-record and Discard. Saved listing must stay unchanged before Save. Complete loops require review/Save before explicit playback; incomplete loops remain invalid. Verify failed-save retry/earlier-take safety where feasible, background mic/playback stop without automatic saving/resume, deliberate close/discard guards, Notes/reference/timer continuity, real acoustic output and existing data after install-over. Unsaved temporary review is not durable across process death. Earlier checklists describing automatic stop/save or loop autoplay are historical; explicit acceptance now applies. Full regression remains at P5 and final V1.

## P2 flexible loops/count-ins — build45

P1 is generally owner-accepted; P2 physical acceptance pending. Use current PHONE_CHECKLIST.txt from [CURRENT_STATE.md](CURRENT_STATE.md). Verify persistent shared count-in default and independent override, any1–64 loop beats (especially3/5/7/11/64), count-in1/3/7/12 and tempo30/120/300. Check disabled invalid0/65/128/decimal/huge requests, clear time-budget errors, correct N-of-total count, exact loop-only WAV duration, optional guide On/Off,128-second capture/repetition, P1 preview/save/re-record/discard, low-space/error recovery and background cancellation. Verify saved owner data after in-place update, physical I/O timing, large text/keyboard/landscape. Do not infer full regression or acoustic acceptance from scoped emulator passes; P5/final gates remain.

## P3 metronome — build46

P2 is generally owner-accepted; P3 physical acceptance pending. Use PHONE_CHECKLIST.txt linked from [CURRENT_STATE.md](CURRENT_STATE.md). Check compact pulse/running-only meter/chevron and shared expanded controls in practice/Tools;3/4,5/4 and6/8,7/8 counting; accent Off identical ticks and On first-beat distinction; Sound+Flash/Sound only/Flash only; phase-preserving BPM and next-beat config changes; PDF page/zoom/Notes draft/timer retention; active capture/review ownership; background/stop without restart. /8 BPM counts each eighth, not grouped/dotted-quarter beats. Verify real acoustic/display latency, install-over saved data and larger-text/keyboard/landscape reachability. Completion cue remains separate. No full regression or P4 settings result is inferred.


## P4 audio Settings scoped candidate

Use CURRENT_STATE.md and the current package PHONE_CHECKLIST.txt for build47 sound/mode persistence, previews, shared Block/countdown alarms/Off/capture safety, retained Notes/PDF/timing, large text/landscape and physical acoustic/install-over checks. Scoped automated results are distinct from pending phone and P5/final full-regression gates. Dark mode is explicitly deferred.


## Build48 Home gear removal

Check no gear beside Goals; Edit still opens Manage Goals, Goal cards work and Tools → Audio Settings remains. Follow the current package checklist for remaining P4 physical checks. P5/final gates unchanged.


## Build49 P4 metronome button feedback

Follow the current package PHONE_CHECKLIST: both complete Stop click surfaces pulse in flash-enabled modes, return between beats and remain static immediately on stop; verify readable labels, BPM/start-stop/meter behavior and acoustic/display synchronization. Sound only remains static. Layout/geometry unchanged; no full regression required for this refinement.
