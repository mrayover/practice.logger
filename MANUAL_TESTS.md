# Current phone and P5 regression checklist

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

Use the short PHONE_CHECKLIST in the current delivery linked from [CURRENT_STATE](CURRENT_STATE.md): files/image/PDF, capture/orientation, gallery, cancellation/replacement, recreation, unavailable camera and enlarged-text layout. Automated focused checks pass; phone/vendor acceptance is pending.

## L1 owner-phone checklist — 0.18.0/build71

Build69 camera/gallery functionality was owner confirmed on November3,2026. P5 remains complete; do not repeat its full checklist for L1. These L1 checks are pending owner verification, independent of automated results.

1. Install the permanent-key L1 APK over build69 without uninstalling/clearing; confirm existing Blocks, references and recordings remain.
2. Home → Library: import one image, one PDF and representative MP3/M4A/PCM16 WAV audio files. Check a failed/canceled import leaves saved items intact.
3. Search, filter, sort and rename Library assets.
4. Force-close/reopen and confirm files remain.
5. Confirm deletion of one Library item leaves unrelated originals/legacy media intact.
6. Recheck a previously saved imported backing track (play/rename/export) and camera/gallery reference.

Library previews/annotations are L2; Block links are L3. JSON backups exclude Library media: keep originals separately until S2. Next implementation phase, only when separately assigned: **L2 — Media Preview and Master Audio Annotation Workspace**.
