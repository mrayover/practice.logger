# Current phone checklist — 0.13.0/build 25

Updated 2026-10-29 (America/Los_Angeles). These are pending owner checks, not claims of phone tests already performed. Earlier general app/metronome/recording/loop acceptance does not establish individual results. Use only the current signed delivery linked in [CURRENT_STATE.md](CURRENT_STATE.md); record phone, Android version, exact build and pass/fail observations.

## Install and preserve data

Install over the existing permanent-key app **without uninstalling or clearing it**. Confirm existing Blocks, Goals, Routines, notes, unfinished practice, history and recordings remain. The old temporary-key 0.1.0 app is separate; no automatic transfer is provided. Test restore only in a spare empty installation. JSON excludes all audio; export wanted takes individually.

## Next check: Daily Log and Weekly View

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

Actual automated release results and the unresolved final full Android15 practice-fixture timeout are in CURRENT_STATE.md. Passing emulator checks do not complete this phone checklist.
