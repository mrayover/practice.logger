# Current state — practice.logger 0.16.0, build34 signed candidate

Updated October29,2026 (America/Los_Angeles). Timer completion audio, the sketch-based compact Focused Practice and protected Home navigation are implemented. **Full release regression is pending; physical-phone acceptance is pending.** Build34 fixes the enlarged-text layout and compact reference reset problems found during verification. Candidates31–33 and the previous verified0.15.0/build30 remain preserved.

## Delivery and source

| Item | Actual value |
|---|---|
| Signed source | `fe175d2631523bbc9d5de3843c19a9c03b1d3148` |
| Signed candidate | **0.16.0 / code34** |
| Build / units / lint | [CI41004107948](https://github.com/mrayover/practice.logger/actions/runs/41004107948):82passed,0failures/errors/skips; debug/release compile; lint0errors/13warnings |
| App / Android | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `aea920a25dae8d7a72efce4b38d0ddd53f615a213bfeaed9721eedf56ce299c5` |
| ZIP SHA256 | `e3f25deaf503bc9680cef188b52e6d2cf15beebc1bfbdc3a1e69a7fd5717a4ba` |

[Signed APK](dist/practice.logger-0.16.0-b34-fe175d2/practice.logger-0.16.0-b34-fe175d2.apk) · [ZIP](dist/practice.logger-0.16.0-b34-fe175d2.zip) · [build info](dist/practice.logger-0.16.0-b34-fe175d2/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b34-fe175d2/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b34-fe175d2/PHONE_CHECKLIST.txt) · [latest pointer](dist/LATEST.txt).

Publisher verifies signature/pinned certificate, package/version, APK checksum and ZIP round trip. Install **over the existing permanent-key app without uninstalling or clearing data**. Dist/signing stay local and ignored; the original Windows checkout and GitHub contain source/tests/records. Synced sources are untouched. Final documentation may follow the signed/tested source; exact final remote HEAD is checked at closeout.

## Actual implementation

Root cause: the old checkpoint stopped at the duration without any completion audio invocation. A new run is armed only below duration, and a successfully committed foreground running crossing consumes that arm and stops at zero. The existing AudioTrack sample clock mixes a short two-note cue, briefly ducking click amplitude while preserving BPM/beat phase. Cue-only output makes no competing focus request. Composition, database emissions, rotation, reference/recording UI and already-expired recovery never trigger a cue. Audio output failure cannot undo the saved zero state. Explicit Continue overtime, Complete and Skip remain; nothing completes/advances automatically.

The owner's sketch governs compact count/Goal/name beside a small timer, bounded Practice Description then Reference Material, a compact BPM/exact-entry/±1/click row and direct free/loop recording rows. Notes expands the Quick note editor; saving clears keyboard focus. Recordings retains the full take list, rename/export/delete and existing phrase setup/count-in/capture. Bottom actions and active Stop/Save remain outside workspace scrolling. Normal portrait ordinary content/collapsed reference fits; enlarged text permits scrolling, wraps secondary controls and keeps longer primary labels whole. Focused reference display uses a safe full-width fitted220dp image/PDF region inside a300dp scrolling area. Zoom/pan/orientation/pages remain; compact Reset is on the viewport to prevent a pan/scroll trap. Editor/sheet640dp bounds are retained. Viewer state never reenters/leaves audio; focus owns recordings while its management sheet is open.

Banner and Android Back share the practice confirmation. Continue preserves current timer/click/note; Back to Main pauses/checkpoints/saves and navigates only after success. Busy guards prevent duplicate save operations. Failed note writes remain recoverable on the same screen. Active recording must Stop/Save or cancel before exit. Block/Goal/Routine discard guards and Log's save barrier remain; summary notes have an explicit discard guard.

## Data safety

Room10, schema exports/migrations1–10, SessionData, RecordingViewModel and BackupData are unchanged. JSON writer6/readers1–6 and empty-target atomic restore remain. Instructions/reference notes retain placement snapshot meaning; archived ownership, sourceBlockId restrictions, BPM history, unfinished/history sessions, recordings, Logs, Goals/Routines and deletion dependencies remain. No schema migration, permission, dependency or new recording engine. JSON excludes all image/PDF bytes/metadata and take/audio metadata: retain originals and export wanted takes separately. Original112APK/ZIP files and candidates31–33 are preserved byte-for-byte; audit repeats at closeout.

## Verified and pending checks

- 82unit/Robolectric tests pass, including non-silent bounded cue and unchanged click phase. Compiler schemas1–10 match committed bytes. Signed build source and current test source have identical product/Gradle/schema bytes.
- Normal API35 and200% API26 focused3-class checks passed earlier; final build34's200% API35 focused checks, including actual off/on AudioTrack cue writes, once-only/recreation/overtime/BPM, save failure/retry, live recording header refusal/Continue, dirty editor retention, direct free/loop, long Goal/PDF and expired cold-open, passed in the first5phases of[41005098664](https://github.com/mrayover/practice.logger/actions/runs/41005098664).
- Complete PDF workflow and cold reopen passed at200% on API26[41005044213](https://github.com/mrayover/practice.logger/actions/runs/41005044213) andAPI35[41005098664](https://github.com/mrayover/practice.logger/actions/runs/41005098664), including actual free/phrase capture, reference pages/zoom/reset, Stop/Save, playback, dismissal and timer/BPM/note continuity. Both runs then stopped at an image geometry assertion; neither entire run is called successful.
- Minimal saved-note/compact zoom/reset diagnostic[41004105479](https://github.com/mrayover/practice.logger/actions/runs/41004105479) passes on API26/200%. Image bound check now allows one physical pixel for dp rounding; isolated final image/reopen runs passed: API26[41005701177](https://github.com/mrayover/practice.logger/actions/runs/41005701177),API35[41005703557](https://github.com/mrayover/practice.logger/actions/runs/41005703557).
- Actual normal and200% screenshots reviewed against the sketch, including final whole-word primary actions, wrapping metronome and pinned main/sheet Stop/Save. Final full API26/API35 release matrix remains pending after focused image checks. Earlier compile/test selector, nested scroll, keyboard, Reset, rounding and setup-only dispatch failures are documented in WORK_LOG; no failed/cancelled matrix is counted as passing.

## Remaining acceptance

Physical speaker audibility/simultaneous click balance, media routing/volume, permanent-key phone install-over, owner's original photo, vendor pickers, landscape and TalkBack remain phone checks. Event/PCM/output-write assertions do **not** prove acoustic output. Existing PDF encryption/validation/bounded-resolution and conservative foreground-only timing/audio limits remain. Abrupt death may lose unsaved text/latest uncommitted interval. No background alarm/capture, new audio engine or media-inclusive backup. Stage3 detailed phone acceptance remains pending; Stage4 stays0/10real sessions.

## One next task

Run and reconcile the required complete API26/API35 release gates for signed build34 before closeout.
