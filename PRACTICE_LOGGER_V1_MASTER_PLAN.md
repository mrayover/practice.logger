# practice.logger — V1 Master Build & Release Plan

**Status:** Owner-approved product direction; implementation pending. **One authoritative, self-contained plan for all remaining V1 work.**  
**Prepared:** 2026-10-30 (America/Los_Angeles).  
**Repository:** https://github.com/mrayover/practice.logger  
**Starting reference:** `main` commit `5ebbd58b8705b0fc80f95b2443b9db464db07a75`; signed application source `cafa1bfad09cc4bcf3539902f2ddb1c8d49153b5`, **0.16.0/build 42**. Reinspect the live branch before each task; this reference is not a permanent assertion of current state.  
**Important:** The existing signed build 42 is a **limited owner preview**. It has **not passed full regression**. No new work, phone acceptance, or feature described below should be marked complete without evidence.

> **Purpose.** Finish practice.logger as a dependable, offline-first Android application for daily music practice. V1 integrates reusable Blocks, sessions, recordings, reference materials, Goals and measured practice time, with export and full recovery. This document *supersedes the prior uncommitted separate pre-Library and Goal/Statistics proposals as the unified forward roadmap*. It does not erase verified implementation history in Git.

---

## 1. Rules for every Work instance

This document is meant to be committed to the **root of the existing repository**, at this exact path: `PRACTICE_LOGGER_V1_MASTER_PLAN.md`. The owner has not yet added earlier proposal documents to Git. **Do not require any of those drafts. This file contains the complete future scope.**

1. **Inspect first:** read `README.md`, `AGENTS.md`, `CURRENT_STATE.md`, `BUILD_PLAN.md`, the mandatory efficiency section in `WORK_LOG.md`, the source affected by the assigned task, current local changes, branch and latest CI. The live repository wins over the starting reference above. Do not restart already completed features.
2. **Integrate this plan into the repo:** link it from `BUILD_PLAN.md` and `README.md` as the V1 forward roadmap, and reconcile obsolete future-scope exclusions in `BUILD_PLAN.md`/`AGENTS.md` without rewriting historical facts. Keep `CURRENT_STATE.md` as the authority for **what is actually implemented, packaged and verified**; use `WORK_LOG.md` for factual session notes. Update those after each increment. Never replace them with optimistic plan checkboxes.
3. **One bounded increment at a time:** a handoff explicitly assigns a phase or subphase. Read the phase dependencies; implement only its described deliverable. **Do not automatically move to the next phase.** Early focused tests and a build are preferable to spending an entire usage window debugging unrelated suites.
4. **Use existing architecture:** Kotlin, Jetpack Compose, Room, Lifecycle, existing PCM/metronome, recording repositories, session accounting and SAF document pickers. Use bundled **Lato**. No second audio engine, database, account system, network backend, or gratuitous UI rewrite.
5. **Data safety:** preserve app ID `com.mrayover.practicelogger.app`, permanent release signing certificate, existing stable IDs, existing recordings, sessions, Goals, Routines, Logs, Notes and old migration paths. Use explicit non-destructive Room/backup migrations. Never uninstall or clear app data as part of an update. **Exception:** the owner's *existing test-only Block image/PDF references* need not be migrated into the new Media Library; see Library phase. This is not permission to delete other data.
6. **Delivery per implementation increment:** produce a usable **permanent-key signed installable APK and ZIP**, increment `versionCode`, verify identity/certificate/signature/hash and preserve earlier distributions. Include a short owner-phone test checklist, exact source commit SHA, actual tests, failures, deferrals and a confirmed pushed remote SHA/branch. Do not claim the owner's Windows checkout was changed unless verified; GitHub Desktop is the normal local workflow. Never commit APKs, build outputs, private keys or signing materials.
7. **Documentation-only increment:** verify links/content and commit/push; **do not compile or run emulator regression** merely for documentation changes. If GitHub write permission fails, provide the files and state clearly that nothing was committed.
8. **Tests are proportionate:** focused unit/instrumentation checks and one build per increment; run the full suite at the **pre-Library** and **final V1** gates (and any narrower storage safety gate described below). Do not silently dismiss failures; isolate an actual failure with logs before retesting the whole matrix. Physical phone checks are separate from emulator evidence.
9. **Usage efficiency:** prefer **High** for difficult audio timing, concurrency, Room/backup design or migration fixes; **Medium** for normal Compose UI, settings, docs, routine test/build work. Extra High is not the default. No speculative refactoring, repeated full rebuilds after each tiny visual tweak, or unrequested new features.
10. **Every session ends with one unambiguous next task.** Even if incomplete: record exactly what changed, what passed/failed, what is not verified, and where the signed package is (if one was produced). Do not claim planned phases are already implemented.

### V1 definition of done

V1 is complete only when all required phases in §2 have owner-accepted deliverables, **including media-inclusive recoverable backups**, full V1 regression, signed upgradeable APK, critical real-device tests, and truthful release documentation. A green CI build alone is not V1 acceptance. Optional visual enhancements may be deferred explicitly without blocking V1.

---

## 2. Authoritative phase order, gates and effort

| ID | Phase / deliverable | Dependency / release gate | Suggested effort |
|---|---|---|---|
| **P0** | Baseline audit and integrate this single master plan | Documentation only; confirm build 42 state and actual changes since | **Medium** |
| **P1** | Temporary audio take review | Signed candidate + focused audio/storage tests | **High** |
| **P2** | Flexible phrase loops and count-ins | Signed candidate + audio boundary/memory tests | **High** |
| **P3** | Expandable metronome, meter, optional accent, visual pulse | Signed candidate + clock/UI tests | **High** |
| **P4** | Audio Settings and bounded UI polish | Signed candidate + settings/transport tests | **Medium** (High only for engine problems) |
| **P5** | **Full pre-Library regression** | Freeze pre-Library work; full suite, signed candidate and phone checks | **Medium** |
| **L1–L3** | **Media Library**: storage, full-screen viewer, Block picker | Independently usable library + signed owner-tested APK | **High** for storage; **Medium** for UI |
| **S0–S2** | **Storage & recovery**: feasibility gate, organized exports, complete backup/restore | Verify real Drive-provider behavior + round-trip restore before proceeding | **High** for backup; **Medium** for export UI |
| **G1–G3** | **Goals & Statistics**: colors/primary association, filtering, historical attribution | Focused migration/data tests, signed candidate and owner checks | **High** for attribution; **Medium** for UI |
| **F1–F2** | **V1 Finalization**: cleanup, full regression, physical-device tests, final signed APK | Required release gate | **Medium**, High only when justified |

**Gating rule:** P5 precedes L1. L3 precedes S0. S2 precedes G1 (because backups must already preserve the new Goal attribution schema). F1 follows G3. An individual Work task may only span adjacent subphases if the owner explicitly authorizes doing so. The phase sequence is the planned scope, **not** authorization to start everything at once.

### What is already implemented — do not rebuild it

Current Git baseline includes Block creation/editing/archive/safe delete, independent sessions and builder, foreground timers, metronome, tuner, stopwatch/countdown, free M4A recording, WAV phrase loops at preset beat counts, take management/export, saved Routines, up to five Goals with existing links and manual progress, Daily Log/Weekly View, Block practice Notes/history, Block-owned single image/PDF viewer, JSON backup **without media**, and permanent-key APK publishing. It also includes the compact Focused Practice transport. Preserve these and extend only the gaps below.

---

## 3. P1 — Recording review, explicit accept/re-record/discard

**Problem:** The existing recording flows commit a take too early for an effortless listen-and-decide workflow. The intended experience is to record, audition, and keep only useful material.

### Requirements

- After stopping a completed **free recording**, enter a temporary **Review Take** state. Provide **Play/Stop Preview**, **Save**, **Re-record**, and **Discard**. Do not add to permanent take listings until Save completes successfully.
- Re-record discards the current temporary take deliberately, then starts another capture through existing mic permission and foreground rules. Discard deletes only the temporary file. Existing saved takes remain intact.
- Use the same interaction in Focused Practice, saved-Block recording screens and standalone Free Recording. Avoid three separate implementations.
- A fully completed foreground phrase loop automatically previews its temporary take before the explicit Keep/Re-record/Discard choice (owner follow-up 2026-10-31). Playback never saves, and never starts from incomplete/cancelled/background capture or restarts on return/recomposition. Free review remains manual; **an incomplete phrase remains invalid** and must be rejected/discarded with understandable feedback, never silently padded or saved as complete.
- Temporary recording can be previewed without creating a fake session, Block, or Goal entry. Keep active Stop/Cancel controls and status reachable when references or notes are open.
- Preserve existing recovery/orphan handling, foreground interruption safety, media focus and playback ownership. A background/mic/storage/audio error may invalidate a pending file but must never corrupt a saved file or pretend a failed save succeeded.
- Keep V1 free audio **AAC/M4A** and phrase-loop audio **PCM16 WAV**. No MP3 encoder, multitrack, overdub, effects, trimming or simultaneous capture/mixing.

**Acceptance:** Record → stop → preview → save; stop → re-record → save; stop → discard; repeated attempts; failed file save; background/interruption; playback stops correctly; saved history unaffected. Check the four entry contexts (Focused Practice, Block recording, standalone Free Recording, completed loop). Deliver versioned signed APK and scoped tests.

---

## 4. P2 — Custom loop length and unrestricted-choice count-in

### Required behavior

- BPM remains **30–300**.
- Loop duration accepts **any whole number of beats from 1 through 64**. The former preset buttons **4 / 8 / 16 / 32** can remain as shortcuts, but the custom numeric field is authoritative. A 64-beat loop equals 16 bars of 4/4. **128 is not the V1 target.**
- Count-in accepts **any positive whole number of beats** (not a restricted list of options). Default **4**, globally configurable in Settings and overridable for a specific loop. Bound actual execution safely for overflow, unreasonable time/memory and resource availability; display a meaningful error if a requested value is infeasible. Do not present a fixed selection limit as a musical restriction. Prefer streaming/generated count-in ticks to allocating an arbitrarily long sample array.
- Count-in is **not recorded into the WAV**. Phrase capture begins after the selected count-in on the existing clock. Display **count-in beat N of total** rather than "of 4".
- Update all dependent validators and paths together: `PhraseSpec`, loop UI, count-in generator, timing/deadline, WAV reader/file-size safeguards, replay/loop points, completion/cancellation, and unit/instrumentation tests.
- At the extreme 30 BPM × 64 beats, the loop lasts **128 seconds**. Verify PCM buffers, memory, file length, static AudioTrack capability, and playback on target devices; avoid redundant whole-buffer copies. If a proven device limitation affects 64-beat playback, fix the implementation rather than silently reducing the advertised maximum.
- Time signatures and number-of-bars entry **do not** apply to the phrase-loop recorder in V1; they are a possible future change. Loop length remains counted in beats.

**Acceptance:** Non-preset 3, 5, 7, 11 and 64 beats; user-selected count-ins 1, 3, 7 and 12 beats; 30/120/300 BPM; long-loop playback, incomplete loop cancellation, lack of free storage, safe background behavior and correct loop duration. Signed APK and targeted tests.

---

## 5. P3 — Expandable metronome with time signatures, accent and flash

### Transport layout (Focused Practice)

- Current collapsed metronome stays **compact**: BPM readout, existing **−1 / +1** tempo adjustments, Start/Stop, and expand/collapse chevron. **Show time signature only while the metronome is running**. Display a small beat-flash indicator even while collapsed. Do not replace the compact row with a large card.
- Expansion follows the established transport pattern: opening Metronome makes it the exclusive expanded transport context, temporarily suppresses Description/Reference content and preserves their prior selection, PDF page and zoom; closing restores them. Do not restart timer, metronome, recording or media viewer because of recomposition.
- Expanded panel exposes BPM (30–300), selectable meter, **accent on/off**, audible/visual output mode, and optionally a small numbered beat display.
- Standalone Tools metronome uses the **same clock and control logic**. It need not use the identical transport shell, but must expose the same meter/sound/mode capabilities. No duplicate metronome engines.

### Musical functionality

- Initial meter choices: **2/4, 3/4, 4/4, 5/4, 6/8 and 7/8**. Implement a correct count of clicks per notated measure. Document what BPM means for `/8` meters (for V1, one selected subdivision/count unit per displayed beat unless a properly tested musical-beat mode is implemented); do **not** pretend that 6/8 grouping/dotted-quarter pulse has been implemented if it has not. Complex groupings and subdivision controls remain outside V1.
- **Default accent: Off**. With accent off, all clicks identical. Accent on changes the first beat's audible character (ictus/downbeat) and optionally its flash styling. Counted measures must remain phase-correct even when accent is off.
- Output choices: **Sound + Flash**, **Sound only**, **Flash only / silent**. Visual pulse follows the audio engine's beat events or one shared sample-derived clock; do not create a second UI delay-loop metronome with independent drift. Visual refresh must be lightweight, respond while collapsed and stop when the engine stops. Be honest about unavoidable Android display/audio-output latency.
- BPM changes must preserve a predictable click phase; meter or sound changes apply at a defined beat/bar boundary without restarting the practice timer. Maintain current foreground audio-focus/stop behavior. Visual-only mode must not output a hidden click.
- **Metronome click timbre** is selected in global Settings in P4. Accent sound should be distinguishable for each supported timbre. Completion alarm is a separate sound preference.
- Do not retrofit metronome meter into loop capture/count-in in this phase.

**Acceptance:** 3/4 and 5/4 bar cycles; 6/8 and 7/8 count definition tested; accented and straight clicks; all three output modes; visibly pulsing collapsed row; silent no-audio case; expansion/restoration with notes/PDF; BPM adjustment while running; standalone Tools parity; no timing/capture regression. Signed APK and focused audio/UI tests.

---

## 6. P4 — Settings and bounded visual/UI polish

### Minimal persistent Settings

Expose a discoverable Settings destination (Home or Tools, whichever integrates with existing navigation). Persist across process restart, no cloud account.

| Setting | V1 behavior |
|---|---|
| **Metronome sound** | Small built-in set: classic click, woodblock-like, soft tick, electronic pulse (or equivalent distinct tested timbres); preview audio. Shared between standalone and in-practice metronome. |
| **Completion alarm** | Current two-tone, bell-like, rising chime, short beep, **Off**; preview audio. Used for Block timeouts **and the foreground standalone countdown timer**. Avoid collisions with currently captured audio; completion must never change elapsed-time accounting. |
| **Default count-in** | Any validated positive whole beat count, default 4; each loop can override. |
| **Metronome output mode** | Sound + Flash / Sound Only / Flash Only; persist last selected default. |
| **Recording click** | Always enabled for loop recording (owner refinement, 2026-10-31); no setup toggle. Free recording/global output settings remain separate. |
| **Storage destination** | **Later in S1:** select/inspect/replace export root, with clear permission status. Do not implement cloud services in P4. |
| **Appearance** | Keep Lato, cream/green identity. See conditional dark mode below. |

### Specific UI polish acceptance

- Make Focused Practice transport compact when no panel is open. Eliminate needless bubble/pill containers around short descriptions when those conflict with the established flat design; reduce one-line description/header height without hiding essential content.
- Expanded Free/Loop/Metronome/Notes panels suppress upper Description/Reference as intended; collapsing restores the prior upper selection and **PDF page/zoom**, without disposing the recording sheet, stopping a running timer or losing note drafts.
- Notes: user can add, save, see previous notes **immediately**, expand/edit existing ones and return to practice. Preserve save failure/draft guards.
- Tall panels and media viewers reveal scrollability through a visible scroll indicator when overflow occurs; pinned primary record/stop/practice actions remain reachable; keyboard and safe insets work at larger text sizes.
- Review cream/green contrast and spacing, retain familiar navigation and **Lato across app-controlled UI**. Darkening cream slightly is acceptable within a coherent palette; verify text contrast. Do not take UI changes as permission for a total redesign.
- **Dark theme is conditional, not a release blocker:** Light/Dark/System can be implemented only if current hard-coded colors can be centralized within this focused phase. Otherwise explicitly defer and do not spend a long window refactoring unrelated screens. No silent partial dark theme.
- Do not add MP3, WAV conversion of free captures, custom uploaded alarm audio, destructive in-app Clear All button, or a new analytics page in P4.

**Acceptance:** Settings persist; selected sounds work in both tool contexts; timer alarms fire once, respect Off/foreground and don't alter clock; compact/expanded UI survives recreation and note editing; large text/scroll/landscape safe; signed APK and targeted Settings/UI tests.

---

## 7. P5 — First full regression and pre-Library release gate

Freeze P1–P4 features. Run and record:

- Full JVM/unit tests, lint, debug and release compilation; existing API **26 and API 35** instrumentation workflows; appropriate screenshot, accessibility, 200%-text, keyboard, portrait/landscape and state-restoration checks.
- Session lifecycle (build/continue/pause/complete/skip/extend, checkpoints), Block deletion blockers, Goal/Routine navigation, Date Log/Weekly View, old Notes and new review flow, recording/loop cancellation, playback/export, metronome/silent flash, alarms, media viewer, JSON backup compatibility and install-over/migration.
- Diagnose real failures in isolation. Don't declare a failure historical/flaky without evidence; don't repeatedly rerun all suites after speculative UI changes. Record exact checks run, exceptions, unresolved issues and owner-only checks.
- Permanent-key signed **pre-Library candidate** APK/ZIP; verify signature, app ID, versionCode, integrity, source SHA and retained old packages. Provide a short **real-device checklist** (speaker/mic, audio timing/loop edges, flash latency, navigation, updates, app foregrounding). Real-device completion cannot be fabricated from emulator success.

**Exit:** Owner accepts the pre-Library candidate (with explicitly documented phone-only limitations if any). **Do not begin L1 until this gate is recorded.**

---

## 8. L1–L3 — Media Library (separate from the existing Block list)

### Product goal

A **top-level Media Library** on Home: a reusable, persistent repository for scanned tunes, PDFs, snippets, photographs of written music, chord diagrams and reference images. The user may populate it **before** creating Blocks. Media Library is not the Block catalog; name navigation clearly so the two are not confused.

### L1 — Canonical local storage and metadata (High)

- Import **PDF, JPEG, PNG and WebP** through Android's Storage Access Framework picker. The picker may include **Google Drive** where installed/available, but the selected document is copied into the app's local Library; the app remains offline-first.
- One canonical private original per Library item, stable **media ID**, human-readable editable title, MIME/type, original filename/provenance where useful, size and import date. No external-provider URI dependency after successful import. Validate size, image/PDF structure, permissions and free storage using existing safe loaders where applicable.
- Implement rename, search, sort (name/recent), import, confirm deletion, and empty/missing-file handling. **Library item identity is independent of Blocks.** No collection/folder/tag hierarchy required for V1.
- **The owner's existing test-only Block image/PDF attachments are disposable.** No legacy reference data migration is required. Still write a safe database migration that explicitly accounts for the old attachment table/files and leaves unrelated Block, recording, session and note data untouched. Do not wipe whole app data. Confirm if an actual non-test attachment appears before discarding it.
- Keep reference media and recording data separate, with bounded file operations and cleanup on failed imports.

### L2 — Full-screen Library preview (Medium)

- Tapping a Library item opens a **full-screen reading view**: document title, close/back, image pinch-to-zoom/pan/reset; PDF page navigation + current/total page and zoom/pan. Preserve image orientation. Reading must work **without** starting a practice timer/session or any recorder.
- Reuse existing Android PDF/image rendering code where safe. Render only bounded PDF pages instead of decoding an entire document at once; close native descriptors on leaving. Support portrait/landscape, safe insets and larger text.

### L3 — Reusable Block references (Medium–High)

- Block create/edit offers **Choose from Library** and **Import New** (the latter imports to Library first, then links the media item).
- A Block links to **at most one** Library item in V1, preserving the current simple practice reference panel. **One Library item may be referenced by any number of Blocks**, with no byte duplication. Changing/removing a Block reference unlinks; it does not delete the Library item.
- During Focused Practice and existing recording sheet, reference expands/collapses within the same workspace; page/zoom/navigation do not stop timer or capture. Archived Blocks may keep links and saved session instruction snapshots remain stable.
- Deletion safety must account for shared ownership: deleting a Block must not remove a shared media original; deleting a Library item still referenced by Blocks must either be blocked with a clear list/count or require an explicit deliberate unlink confirmation. Prefer **block deletion of referenced media** for V1. Update old safe Block-delete checks to avoid treating a shared Library item as an owned file.
- Maintain existing one-reference Block interaction, while enabling reusable media elsewhere. No annotations, OCR, scanner/camera capture pipeline, multiple references per Block or PDF editing.

**L3 exit:** signed APK + tested import/search/rename/full-screen preview/link/unlink/shared reuse; clear old-test attachment policy; no file leaks or unrelated data loss; owner can import a PDF from Drive/phone, view it full screen, link it into two Blocks, practice and delete one Block without losing the media.

---

## 9. S0–S2 — Storage, organized Drive export and complete backup/recovery

### Approved V1 architecture and distinction

**Local app data is authoritative; Google Drive is an optional destination for copies.** Android's document-provider interface (SAF) is preferred; Google Drive visibility and folder operations depend on its actual Android provider behavior. No Google OAuth, app-specific Drive API/backend, automatic cloud synchronization, conflict-resolution service or background upload engine in V1. Do not promise automatic Drive support that the installed provider cannot offer.

### S0 — Storage implementation decision gate (High/Medium, bounded)

Before implementation, inspect current recording file ownership, export UI, Room relations, backup codec, SAF capabilities and test devices. Test selecting a Drive folder, persistable permission, file creation and nested folder creation where supported. Record results (supported/unsupported/untested) and choose the **smallest safe** path below. This is a feasibility **gate**, not permission to indefinitely postpone saving or recovery. If nested-folder creation fails with Drive, preserve functional **manual Save As** and implement organized folder export on supported local providers; explain the provider limitation to the owner.

### S1 — Organized audio exports (Medium)

- Settings → Storage lets the user choose an **export root** from SAF, display its name/access status, and reselect/revoke when missing. Never request whole-device storage permissions to work around provider failures.
- Preferred destination hierarchy (names sanitized, stable IDs or collision suffixes where necessary):

```text
practice.logger/
  Recordings/
    <Block Name>/
      YYYY-MM-DD/
        HH-mm-ss - <Take Name>.m4a   (free)
        HH-mm-ss - <BeatCount>beats <BPM>bpm.wav   (loop)
    Free Recordings/
      YYYY-MM-DD/
        HH-mm-ss - <Take Name>.m4a
  Backups/
    practice.logger-YYYY-MM-DD_HH-mm-ss.zip
```

- For a selected `practice.logger` root, don't create a second redundant nesting level. Create subfolders **where provider supports them**; otherwise offer manual per-take export to an explicit destination with sensible filenames.
- Exports copy bytes; **do not relocate/delete private originals**. Existing per-take export still works. Do not overwrite an existing file without deliberate confirmation; handle similarly named Blocks, duplicate takes, renamed Blocks and destination folder permission failures.
- For failed/interrupted export, do not claim success or delete local audio; avoid leaving misleading partial files where safe cleanup is supported. Report the exact destination and user-friendly failure reason. Organizing exports by Block/date does **not** replace the app's local recording library.

### S2 — Complete portable ZIP backup and verified restore (High; V1 release-critical)

- A true **complete** backup contains *all* durable V1 user data: Blocks, Goal/Block links/Goal history and snapshot attribution, Routines, sessions/placements, practice time, Daily Logs, practice and session Notes, Media Library metadata and original PDFs/images, recording/take metadata/labels and **actual M4A/WAV bytes**, settings where appropriate and stable IDs/relationships. Exclude temporary in-progress audio, caches and private secrets.
- ZIP contains a **versioned manifest**, explicit item IDs/paths, counts, types/sizes, cryptographic hashes (e.g. SHA-256), schema/format version and app version. Sanitize archive paths against traversal/duplicate entries and reject or safely handle malformed, oversized or inconsistent archives. Bound memory/storage; stream large file bytes rather than building one giant array.
- Build a complete archive to a temporary destination, verify integrity, then publish/rename the final copy where supported. Handle out-of-space, missing file, interruption and provider errors without pretending backup succeeded. The target folder may be SAF/Google Drive where supported; provide an explicit alternate Save As flow.
- Restore must validate the archive, media hashes, IDs, references, bounds and available disk space **before destructive operations**. Follow the existing conservative **empty-installation restore** contract (or a separately approved safe staging/rollback approach); **never overwrite or merge a populated installation without an explicit new owner decision**. Prevent partial database/media restoration or supply a verified rollback mechanism; report failures accurately.
- Continue supporting existing JSON backup **readers 1–7** as appropriate, clearly labeling JSON as **data-only legacy backup**. Update writing format/schema/migrations for the new data while preserving compatibility; never represent JSON as media-inclusive backup.
- Test complete round-trip restore on a clean target, plus recording playback, PDF viewing, Goal references, same stable IDs and actual practice time after restore. Test corruption, missing entries, duplicates, malicious paths, empty/partial archives, low storage, interruption and legacy JSON restore.

**S2 exit is a hard V1 gate:** demonstrate a complete backup/restore round trip including at least one PDF, one image, one Block-linked media item used by two Blocks, one free M4A take, one WAV loop, a Daily Log, a Goal, and nonzero committed practice time. A successful ZIP export alone is insufficient.

---

## 10. G1–G3 — V1 Goal colors, Block filtering and truthful statistics

### Current foundation; migrate conservatively

Up to five active Goals, Goal ordering/completion, manual percentages, existing `goal_blocks` many-to-many links and explicit `session_goals` are implemented. Current Goal time counts sessions explicitly started through Goal practice, not all practice. `placements.activeMs` is actual committed time; Home's `Data usage` is placeholder. **Block tags are not implemented at current baseline**—do not assert otherwise or create a separate tag system without explicit approval.

### G1 — Goal identity, primary link and colored Block cap (High for migration, Medium for UI)

- Each of up to **five active Goals** has a **distinct consistent color**. Persist color **by Goal identity**, not by priority order; reorder/complete/reopen do not unpredictably recolor an existing Goal. Define a deterministic policy for a new active Goal entering an available color slot. Ensure legibility and accessibility; show Goal name, not only color.
- Each Block has **zero or one primary Goal** (optional). Its own objective/instructions are separate from the overarching Goal. Display a **narrow colored end cap** on Block rows/cards across Manage Blocks, session builder, routine builder and relevant Goal lists. An unassigned Block uses neutral charcoal/black (theme-adjusted).
- Reuse existing Goal/Block relationships where possible. Legacy `goal_blocks` currently permits multiple Goals per Block. **Do not arbitrarily select a primary Goal or delete old links.** For preexisting multi-Goal Blocks, leave primary unassigned until deliberately chosen, or provide a specific owner-approved reconciliation. Do not break existing Goal-initiated practice. Preserve archived/previous Goal identities for history.
- Define how Blocks primarily assigned to a **completed** Goal are shown (retain their actual Goal identity/color in history and editing; don't mislabel them as `No Goal`). Let owner reassign voluntarily. No automatic reassignment when active five change.

### G2 — Shared Goal filtering (Medium)

- Implement **one shared filter** applied in (1) Practice Now/session Block picker, (2) Manage Blocks, and (3) saved Routine editor/builder.
- Choices **All Goals**, each of the current five active Goals, and **No Goal**. Match the **primary** association; `No Goal` means actual null, not an inactive or completed Goal. Show completed-Goal Blocks under All; allow their existing Goal identity to be visible even if completed Goals are not active filter choices.
- Combine with existing name/action/notes text search and active/archived eligibility. **If actual tag data/features exist by implementation time**, compose tag filtering in the same predicate. If absent, do not invent a tag filter or add tag infrastructure to V1 as a side effect.
- Retain caps in results, clear filter easily, show meaningful empty states and preserve session-builder ordering/history.

### G3 — Accurate time attribution and concise statistics (High for correctness)

- Replace the Home **`Data usage`** placeholder with compact **total actual practice time**; tap opens one simple statistics view (no large dashboard). Include cumulative total, recent/weekly time where valid from stored timestamps, per-Block practice time and optionally top-practiced Blocks. Provide Goal detail with Goal total and relevant practice history.
- Use **distinct committed placement `activeMs`** for totals, including recorded active time of unfinished sessions; exclude configured-but-not-worked duration, pauses, backgrounds, pure metronome use and playback. A Block used multiple times counts its distinct placements, never duplicates a placement through joins.
- New practice, whether ordinary, routine or Goal-launched, attributes a placement's recorded time to **its primary Goal at the time that placement first accrues actual time** (or equivalently stable snapshot). Null means unattributed. Store this historical snapshot or equivalent durable attribution; subsequent Block reassignment or Goal reorder/complete **must not rewrite historical attribution**.
- Existing `session_goals` relates entire Goal-launched sessions to a Goal. Define a migration/precedence rule to coexist with per-placement primary Goal attribution. Do not double-count the same minutes in global totals or accidentally credit an entire mixed session to every Goal. For old sessions without a defensible primary Goal snapshot, preserve legacy explicit Goal-session attribution **separately and clearly labeled** if reported, and identify unknown/unattributed time rather than inferring it from today's Block relationship.
- **Time measures effort, not mastery.** Do not auto-compute a completion percentage from minutes. The current Goal editor requires manual 0–100%; adapt the presentation so percentage is **optional or only displayed when a meaningful measurable target exists**, retaining legacy entered values without erasing them. Status, notes, accumulated time and practice activity can be shown without a fabricated percent.
- Reuse existing Goal/Block/session screens. Do not invent granular per-day split of a session across midnight where data does not support it; clearly label session-start-date-derived weekly totals if that is the available representation. No streak/gamification/social, broad AI analytics or separate reporting backend.

**G3 exit:** signed candidate with tests for zero/one/multiple legacy links, colors stable after Goal reorder/complete/reopen, filter + search across all three entry points, repeated Block placement, different session start types, unfinished/paused/overtime/skip, Block reassignment after past practice, backup/restore of attribution and no duplicate total time. Goal time must never silently be displayed as skill mastery.

---

## 11. F1–F2 — V1 Finalization, full release and real-life acceptance

### F1 — Feature freeze, cleanup and comprehensive automated verification

- No new V1 features after G3. Remove confirmed dead UI/placeholder labels, verify navigation, spacing, Lato, palette, clear error messages and accessibility. Cleanup is **not** an excuse to redesign already accepted screens.
- Run full unit suite + lint + debug/release compilation + complete available **API26/API35** instrumentation matrix, including updated media, SAF, backup and Goal attribution tests. Compare screenshots and verify 200% font, portrait/landscape, overflow/scroll indicators, accessible colors, TalkBack/semantic labels, keyboard transitions and process recreation where feasible.
- Verify old-to-new Room migrations and **supported backup format compatibility**. Test install-over from current permanent-key build, data/media persistence, deletion guards, self-contained backup/restore, file-provider failure, audio-focus/microphone, recording review, 64-beat looping, custom count-in, meter/flash, alarms, Logs/Notes, Goals filters/totals and independent session accounting.
- Keep a tracked list of failures with reproducible evidence; isolate failed tests and retest after targeted fixes. Run broad suites again at the release gate, not after each cosmetic change. No ungrounded blanket "all tests passed" assertion.

### F2 — Physical phone signoff and permanent-key release

Owner completes and records actual phone checks for: install-over and saved data; foreground/background safety; free and loop recording with the phone's microphone or connected interface; actual speaker/headphone click/alarm, silent flash and visual timing; long loop memory/playback; Library large PDFs/rotated photos; provider import/export; complete ZIP backup and **clean restore**; Goal time/color/filter correctness; navigation, large text and common practice tasks.

**V1 release artifact:** permanent-key signed installable Android APK and ZIP, `versionName`/increased `versionCode`, matching source SHA/commit, certificate/hash/ZIP validation, explicit CI matrix results, current delivery link/phone checklist and a concise CHANGELOG/readme. Preserve older APKs and signing key. The phone's real tests must be marked completed by the owner—not invented by Work. If critical tests are blocked, ship only an accurately labeled candidate, not a verified V1 release.

**Practical-use condition:** Conduct and document at least a few genuine practice sessions (ideally the already-planned 10-session evaluation) to spot significant friction. Do not automatically open a new feature phase just because evaluation finds an idea; fix actual blockers first. If owner defers the remaining sessions, record that plainly.

---

## 12. Explicit V1 non-goals and boundaries

These are **not** part of V1 unless the owner separately changes this document: in-app scanning/OCR/markup, multiple Library references per Block, Library folder/tag taxonomy, an entirely new tagging system, importing online scores, notation editing, 128-beat loops, meter-aware loop bar selection, grouped 6/8/subdivision tempo maps, MIDI sync, MP3 encoding, converting every free capture to WAV, multitrack/overdub/mixer/effects, always-on recording, background countdown notifications, user-defined imported sound packs, automated music-learning prescriptions, advanced analytics, streaks, cloud accounts, Google Drive OAuth/sync/background upload, teacher/student/publisher products, band collaboration, desktop companion, marketplace or subscription services. These remain future ideas, not V1 acceptance blockers.

**Visual theme:** Full dark mode is explicitly **optional** at V1; deliberate palette/contrast polish is required. Do not misrepresent dark mode as already delivered. The app remains useful offline; no backend is needed.

**Storage release boundary:** Local recording files plus legacy JSON data-only export are **not** enough to call V1 completely backed up. The complete ZIP round-trip is required, even if Drive's nested-folder support proves unavailable and the user must choose an explicit destination manually.

---

## 13. Acceptance checklist for the entire project

Mark these complete only on concrete evidence in `CURRENT_STATE.md` and the Work log:

- [ ] This master plan is committed in the existing repo and referenced from BUILD_PLAN/README; baseline/actual next phase correctly recorded.
- [ ] P1 recording review accepted; temporary takes handled safely.
- [ ] P2 arbitrary 1–64 loop beats and user-entered positive count-in; WAV/replay validated.
- [ ] P3 expandable metronome with meter, optional accent, silent visual flash; Tools/Focused Practice parity.
- [ ] P4 persistent global audio Settings, Block/Tools alarm, compact transport/Notes/UI polish.
- [ ] P5 complete **pre-Library regression** and signed owner-reviewed candidate.
- [ ] L1 local Media Library import/search/rename/delete, safe storage and disposal of only old test attachments.
- [ ] L2 full-screen image/PDF viewer, zoom/page controls and no accidental session side effects.
- [ ] L3 shared media picker/linking in Blocks, including reference viewer during practice/recording.
- [ ] S0 documented SAF/Google Drive provider capabilities and fallback decision.
- [ ] S1 organized non-destructive audio export and user-selectable destination.
- [ ] S2 verified **full media-inclusive ZIP backup and clean restore** with integrity/failure tests.
- [ ] G1 stable five Goal colors, neutral caps, optional primary Goal and preserved legacy associations.
- [ ] G2 Goal filter combined with available search across all three Block selection contexts.
- [ ] G3 actual time statistics and historically accurate, nonduplicated Goal attribution; no fake progress.
- [ ] F1 full final regression, migrations/backup compatibility, accessibility and failure triage.
- [ ] F2 permanent-key V1 release candidate, real-device acceptance and truthful final documentation.

### Minimal prompt for each new Work instance

> Read `PRACTICE_LOGGER_V1_MASTER_PLAN.md` and the current repository development records. Inspect the live branch and identify the next **owner-assigned** uncompleted phase; do not assume the checked-box state is correct without evidence. Implement **only [PHASE ID]** using the prescribed smallest safe change. Run targeted checks, build a signed installable APK for implementation phases, update `CURRENT_STATE.md`, `WORK_LOG.md` and `BUILD_PLAN.md`, commit and push intended changes, verify remote SHA and report exactly what passed/failed and the next recommended task. Stop; do not start the following phase automatically. For P0 documentation only, do not build an APK.

**End state:** Goals explain *why* practice matters; Blocks define *what* is practiced; sessions and recordings capture *what happened*; the Library provides reusable materials; statistics show *how much actual work* was done; and portable backup means those materials can be recovered. That is practice.logger V1.
