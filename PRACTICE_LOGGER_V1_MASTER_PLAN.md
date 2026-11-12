## Owner policy — development verification and F1 regression (2026-11-12)

During ordinary development, compile the changed application and run only focused checks for the assigned feature, critical data integrity, and signed delivery. Comprehensive cross-feature regression, device matrices, historical migration/backup compatibility campaigns, broad UI/accessibility suites and baseline lint investigations are deferred to F1. Retain all tests and verification infrastructure. Investigate failures directly affecting the assigned change; document unrelated failures for F1.

Comprehensive workflows are manual and require explicit owner authorization for each campaign. G3 completion does not authorize F1 or feature freeze. The owner decides when remaining features, including future check-ins/revisit suggestions, Playground and other approved or pending work, justify F1. This directive supersedes older per-stage broad-regression requirements, including the full-suite wording in the efficiency procedure; its other rules remain applicable.

# practice.logger — V1 Master Build & Release Plan

**Status:** Owner-approved authoritative forward roadmap. P5 is owner accepted. T3A–T3D, S1, O0, S2, O1, G1, W1 and G2 are delivered. P1 Intentional Practice Lifecycle and P2 Block and Session Reflection are implemented ahead of G3. G3 awaits a separate owner assignment. [Current state and verification](CURRENT_STATE.md).
**Prepared:** 2026-10-30 (America/Los_Angeles).  
**Repository:** https://github.com/mrayover/practice.logger  
**Starting reference:** `main` commit `5ebbd58b8705b0fc80f95b2443b9db464db07a75`; signed application source `cafa1bfad09cc4bcf3539902f2ddb1c8d49153b5`, **0.16.0/build 42**. Reinspect the live branch before each task; this reference is not a permanent assertion of current state.  
**Current delivery:** See [CURRENT_STATE](CURRENT_STATE.md) for signed releases, artifacts and verification.

> **Purpose.** Finish practice.logger as a dependable, offline-first Android application for daily music practice. V1 integrates reusable Blocks, sessions, recordings, reference materials, Goals and measured practice time, with export and full recovery. This document *supersedes the prior uncommitted separate pre-Library and Goal/Statistics proposals as the unified forward roadmap*. It does not erase verified implementation history in Git.

---

## 1. Rules for every Work instance

This document is meant to be committed to the **root of the existing repository**, at this exact path: `PRACTICE_LOGGER_V1_MASTER_PLAN.md`. The owner has not yet added earlier proposal documents to Git. **Do not require any of those drafts. This roadmap governs phase order; the linked integration specification details the approved L/K/T scope.**

1. **Inspect first:** read `README.md`, `AGENTS.md`, `CURRENT_STATE.md`, `BUILD_PLAN.md`, the mandatory efficiency section in `WORK_LOG.md`, the source affected by the assigned task, current local changes, branch and latest CI. The live repository wins over the starting reference above. Do not restart already completed features.
2. **Integrate this plan into the repo:** link it from `BUILD_PLAN.md` and `README.md` as the V1 forward roadmap, and reconcile obsolete future-scope exclusions in `BUILD_PLAN.md`/`AGENTS.md` without rewriting historical facts. Keep `CURRENT_STATE.md` as the authority for **what is actually implemented, packaged and verified**; use `WORK_LOG.md` for factual session notes. Update those after each increment. Never replace them with optimistic plan checkboxes.
3. **One bounded increment at a time:** a handoff explicitly assigns a phase or subphase. Read the phase dependencies; implement only its described deliverable. **Do not automatically move to the next phase.** Early focused tests and a build are preferable to spending an entire usage window debugging unrelated suites.
4. **Use existing architecture:** Kotlin, Jetpack Compose, Room, Lifecycle, existing PCM/metronome, recording repositories, session accounting and SAF document pickers. Use bundled **Lato**. No duplicate recording repository, independently owned waveform/annotation system, database, account system, network backend, or gratuitous UI rewrite. L2 starts with platform playback parameters; an alternative engine requires the feasibility evidence and owner approval in the integration specification.
5. **Data safety:** preserve app ID `com.mrayover.practicelogger.app`, permanent release signing certificate, existing stable IDs, existing recordings, sessions, Goals, Routines, Logs, Notes and old migration paths. Use explicit non-destructive Room/backup migrations. Never uninstall or clear app data as part of an update. **L1 handoff supersession:** preserve every existing visual attachment and imported take; the former test-only disposal exception is not used. Safe adoption waits for L3 ownership/resolution/deletion/recovery changes. No silent disposal is authorized.
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
| **P5** | **Full pre-Library regression** | Complete: build67 full gate and owner acceptance November 1, 2026 | **Medium** |
| **L1** | Canonical visual/audio Library storage; safe legacy coexistence/adoption audit | After accepted P5 and outstanding build69 phone checks; targeted persistence/migration/deletion tests | **High** |
| **L2** | Visual preview, shared audio waveform/player and master annotations | After L1; platform speed/seek/A–B feasibility and reusable functional tests | **High** for audio; **Medium** for UI |
| **L3** | Independent visual/audio Block links and legacy picker transition | After L2; two-Block reuse/restart without copied bytes | **Medium–High** |
| **K1** | Reference Piano Tool and reusable polyphonic keyboard | After L3; range/pitch/touch/lifecycle and audio coexistence checks | **High** for audio; **Medium** for UI |
| **T1** | Record Loop / Transcribe modes in existing transport | After K1; ordinary Block phrase practice without Loop Recorder regression | **Medium–High** |
| **T2** | Block-local annotations, overrides, promotion and playback state | After T1; inheritance/deletion/multi-Block/restart tests | **High** |
| **T3-0** | Roadmap lock and handoff | After delivered T2. Documentation checkpoint only | **Medium** |
| **T3A** | Standalone Transcribe Workspace | After T3-0. Scoped navigation, lifecycle and signed install-over | **Medium–High** |
| **T3B** | Standalone Timer and Goal Attribution | After T3A. Durable clock, aggregation, migration and restore checks | **High** |
| **T3C** | Record Reference Directly in Transcribe | After T3B. Shared recorder, canonical save and safe Block replacement | **High** |
| **T3D** | Concurrent Transcribe Playback and Free Record | Implemented. Physical Android microphone and playback verification pending | **High** |
| **S0** | Storage feasibility and decisions (complete) | Recorded provider evidence and fallback decision | **Medium** |
| **S1** | Organized Audio Export (complete) | Delivered with focused local SAF verification | **Medium** |
| **O0** | Practice Overlay Architecture and Roadmap Integration (complete) | Commit and publish the architecture and recovery contract before S2 | **Medium** |
| **S2** | Complete Portable ZIP Backup and Verified Restore (complete) | Verified clean-install recovery and local SAF publication. Owner cloud-provider checks are pending | **High** |
| **O1** | Block-Local Practice Overlay Implementation (complete) | Build95 passed focused viewer and separate-install ZIP v2 recovery, restart and edit checks on API 26 and 35. Owner phone checks are pending. | **High** |
| **G1** | Goal priority colors, primary Block association and first-accrual snapshots (complete) | Build96 with focused migration, UI and recovery checks. Owner phone checks are pending. | **High** for attribution; **Medium** for UI |
| **W1** | This Week practice workspace | Owner-prioritized after G1 and before G2. Focused Room, backup and Android workflow checks. | **Medium** |
| **G2** | Shared Goal Filtering | Delivered after W1. Shared filter with search and archived eligibility across four Block-selection contexts. | **Medium** |
| **P1 Intentional Practice Lifecycle** | Ready, Prep and deliberate Block transitions | After G2 and before G3. Signed candidate and focused lifecycle checks. | **High** |
| **P2 Block and Session Reflection** | Optional check-ins and immutable completion Goal snapshots | After P1 and before G3. Signed candidate and focused lifecycle, storage and ZIP checks. | **High** |
| **G3** | Accurate time attribution and concise statistics | After P2 Block and Session Reflection. Historical snapshots, legacy Goal sessions and distinct committed totals. | **High** |
| **F1–F2** | **V1 Finalization**: cleanup, full regression, physical-device tests, final signed APK | Required release gate | **Medium**, High only when justified |

**Gating rule:** P5 → L1 → L2 → L3 → K1 → T1 → T2 → T3-0 → T3A → T3B → T3C → T3D → S0 → S1 → O0 → S2 → O1 → G1 → W1 → G2 → P1 Intentional Practice Lifecycle → P2 Block and Session Reflection → G3 → F1 → F2. Each phase after T3-0 requires a separate explicit owner assignment. S2 must preserve T3 activity and media data before O1 begins. O1 must deliver overlay persistence and its ZIP extension together before G1. G1–G3 retain their approved scope, including preservation of historical Goal attribution.

### What is already implemented — do not rebuild it

Current Git baseline includes Block creation/editing/archive/safe delete, independent sessions and builder, foreground timers, metronome, tuner, stopwatch/countdown, free M4A recording, WAV phrase loops with custom 1–64 beat counts, take management/export, saved Routines, up to five Goals with existing links and manual progress, Daily Log/Weekly View, Block practice Notes/history, Block-owned single image/PDF viewer, JSON backup **without media**, and permanent-key APK publishing. It also includes temporary take review, MP3/M4A/PCM16 WAV imported backing takes using MediaPlayer, camera/gallery references, centralized themes and the compact Focused Practice transport. L1 now adds canonical independent media storage/management with Room12; JSON7/readers1–7 remain media-excluding. Legacy media stays under its existing ownership until safe L3 adoption. Preserve these and extend only separately assigned gaps below.

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
- Keep V1 free audio **AAC/M4A** and phrase-loop audio **PCM16 WAV**. No MP3 encoder, multitrack, overdub, effects, trimming or source-audio mixing. T3D permits canonical reference playback during ordinary Free Record microphone capture. Beat-based loop capture follows its existing workflow.

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

**Recorded status — November 1, 2026: P5 complete.** Final full CI41329443572 passed all required jobs; permanent-key 0.17.1/build67 was delivered. Owner confirmed: “Confirmed complete and running correctly”. See [CURRENT_STATE](CURRENT_STATE.md) for exact provenance and acceptance limits. L1 has since been explicitly assigned; see CURRENT_STATE for actual scope/verification and the L1 storage document for safe legacy coexistence.

---

## 8. L1–L3 — Canonical Media Library and Block references

The Library is the global, offline-first media preparation workspace, separate from the Block catalog and practice sessions. The [approved integration specification](docs/MEDIA_LIBRARY_TRANSCRIBE_INTEGRATION_PLAN.md) defines detailed requirements, exclusions and acceptance for L1–L3, K1 and T1–T2. CURRENT_STATE records current implementation, actual verification and owner acceptance; this section defines the approved phase scope.

### L1 — Canonical Media Library and storage (High)

- Extend existing SAF validation/importers for PDF, JPEG/PNG/WebP and MP3/M4A/supported WAV. Store one managed canonical original per asset, stable media ID, title, metadata and validated storage reference; ownership is independent of Blocks.
- Deliver bounded import/failure recovery, search, rename, sorting, type filtering and safe deletion. No session or recording subsystem belongs in the Library.
- The L1 handoff supersedes the former test-only disposal exception: preserve all existing visual attachments and imported takes. The ownership audit establishes safe coexistence; adoption requires L3 to update durable links, resolution, deletion and recovery together. See docs/MEDIA_LIBRARY_STORAGE.md. Preserve all take IDs/history, Block associations, playback and byte-preserving export; no blind deletion or duplication.

**Exit:** targeted persistence, migration, deletion and failure tests validate canonical storage and legacy ownership.

### L2 — Media preview and master annotation workspace (High/Medium)

- Reuse full-screen bounded PDF/image renderers, orientation, zoom/pan/reset and page navigation without practice side effects.
- Add one reusable audio waveform/player: play/pause/seek, zoom/scroll, A–B repeat, pitch-preserving speed and global named point/range markers with optional notes and add/rename/move/resize/delete controls.
- Generate bounded cached waveforms without whole-recording PCM allocation. Preview/annotation does not start Free Record, Goals or session-time tracking.
- Gate engine selection on platform playback-parameter, seek and A–B tests. Document unacceptable device gaps/short-loop behavior and seek owner approval for the smallest justified alternative. No sample-perfect compressed-loop promise or metronome alignment.

**Exit:** shared player/waveform and master markers are functionally verified and reusable by Blocks.

### L3 — Block Media Library associations (Medium–High)

- Extend existing Block editing with Choose from Library / Import into Library and link, separately for **one visual** and **one active audio** reference. Store stable media IDs, not per-Block original copies.
- Preserve compact visual panels and retained practice/recording viewers; score and audio coexist. Unlink retains originals; block accidental deletion of referenced Library assets and retain current Block/history deletion protections until safely migrated.
- Establish canonical audio selection in the Block editor while preserving existing Loop Setup imports and saved take ownership during L3. Converge those controls in T1 after K1; do not break legacy content or duplicate bytes for adoption. This bounded coexistence follows the explicit L3 implementation handoff.

**Exit:** two Blocks reuse the same audio bytes with independent visual links, survive restart and unlink safely.

### K1 — Reference Keyboard (High/Medium)

Add **Reference Piano** as the fourth reusable Tool: approximately 12–18 visible semitones, adjustable register, correct audible frequencies, simultaneous notes, phone-usable touch layout/expanded fallback and lifecycle/output cleanup. Reuse the component as a collapsible Transcribe keyboard without interrupting reference playback. No MIDI, recording, synth editing, detection, accompaniment or notation generation.

**Exit:** standalone range/pitch/polyphony works and the keyboard is reusable in active practice.

### T1 — Block Transcribe mode (Medium–High)

Extend the existing compact Loop transport with selectable **Record Loop** and **Transcribe** modes. Record Loop retains BPM, beats, count-in and review without inherited speed/marker controls. Transcribe uses the Block's canonical audio, Library picker and shared waveform/seek/zoom/speed/pitch/A–B controls, marker jumps, local phrase refinement and keyboard. No special Block type. Expansion temporarily collapses Description/Reference and restores their prior state on close without changing session ownership.

**Exit:** an ordinary Block selects, isolates and slows a phrase for practice without changing the original Loop Recorder.

### T2 — Local annotation and persistence model (High)

Use stable master marker identity with Block-local overrides/hiding, independent local markers and persisted selected media/speed/position/A–B state. Unmodified markers follow master revisions; local edits never silently change other Blocks. Explicit per-marker **Add to Master**, **Update Master** and **Reset to Master** preserve other Blocks' overrides. Master deletion preserves valid independent local work and safely removes invalid inherited display state. Exact Room entities remain implementation decisions; no automatic bidirectional sync or whole-Block bulk publishing.

**Exit:** inheritance, promotion, edits/deletion, multiple Blocks and process restart satisfy the detailed specification's rules.

### T3-0 — Roadmap lock

Documentation checkpoint after T2. The [Standalone Transcribe plan](docs/T3_STANDALONE_TRANSCRIBE_PLAN.md) defines the scope, acceptance and exclusions for the four separately assigned milestones.

### T3A–T3D — Standalone Transcribe and recording integration

- **T3A** adds direct Practice Now workflows and standalone Transcribe with shared Library audio, master markers and piano. Existing Block Transcribe stays available.
- **T3B** adds the foreground stopwatch, one durable activity per Stop and optional Goal attribution through shared time totals.
- **T3C** reuses microphone capture and review to save one canonical reference from either context. Reference playback pauses during capture, and Block replacement follows T2 protections.
- **T3D** allows canonical reference playback during ordinary Free Record capture with independent controls, existing take ownership and real-device audio verification.

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

### O0 — Practice Overlay Architecture and Roadmap Integration

The [Practice Overlay specification](docs/PRACTICE_OVERLAY_SPEC.md) defines the Block-local interaction, storage and recovery contract. O0 is documentation-only. S2 delivers verified recovery of existing durable data and leaves a versioned path for O1 records.

### S2 — Complete portable ZIP backup and verified restore (High; V1 release-critical)

S2 passed clean-install round trips and interruption recovery on API 26 and API 35. See the [implemented format](BACKUP_FORMAT.md#complete-portable-zip-v1) and [verification record](CURRENT_STATE.md).

- A true **complete** backup contains *all* durable V1 user data: Blocks, Goal/Block links/Goal history and snapshot attribution, Routines, sessions/placements, practice time including standalone activity records and their optional Goal/audio associations and historical attribution, Daily Logs, practice and session Notes, Media Library metadata and canonical PDFs/images/audio including microphone references (each original once), global markers/notes, Block-local markers/overrides, A–B selections, speed/position and stable media links, persisted keyboard settings if any, imported backing-track histories, recording/take metadata/labels and **actual MP3/M4A/WAV bytes**, settings where appropriate and stable IDs/relationships. Exclude temporary in-progress audio, caches and private secrets.
- ZIP contains a **versioned manifest**, explicit item IDs/paths, counts, types/sizes, cryptographic hashes (e.g. SHA-256), schema/format version and app version. Sanitize archive paths against traversal/duplicate entries and reject or safely handle malformed, oversized or inconsistent archives. Bound memory/storage; stream large file bytes rather than building one giant array.
- Build a complete archive to a temporary destination, verify integrity, then publish/rename the final copy where supported. Handle out-of-space, missing file, interruption and provider errors without pretending backup succeeded. The target folder may be SAF/Google Drive where supported; provide an explicit alternate Save As flow.
- Restore must validate the archive, media hashes, IDs, references, bounds and available disk space **before destructive operations**. Follow the existing conservative **empty-installation restore** contract (or a separately approved safe staging/rollback approach); **never overwrite or merge a populated installation without an explicit new owner decision**. Prevent partial database/media restoration or supply a verified rollback mechanism; report failures accurately.
- Continue supporting existing JSON backup **readers 1–7** as appropriate, clearly labeling JSON as **data-only legacy backup**. Update writing format/schema/migrations for the new data while preserving compatibility; never represent JSON as media-inclusive backup.
- Test complete round-trip restore on a clean target, reconstructing shared references and master/local inheritance without per-Block byte duplication, plus recording playback, PDF viewing, Goal references, same stable IDs and actual practice time after restore. Test corruption, missing entries, duplicates, malicious paths, empty/partial archives, low storage, interruption and legacy JSON restore.

**S2 exit is a hard V1 gate:** demonstrate a complete backup/restore round trip including at least one PDF, one image, one audio asset used by two Blocks with independent local markers/overrides and playback settings, preserved legacy imported history, one free M4A take, one WAV loop, a Daily Log, a Goal, and nonzero committed practice time. A successful ZIP export alone is insufficient.

---

### O1 — Block-Local Practice Overlay Implementation

Build95 delivers Highlight, Cover with Peek, and Focus over the existing single image or PDF per Block. Room 18 and ZIP v2 preserve the editable local markings. Focused delivery and verification are recorded in [CURRENT_STATE](CURRENT_STATE.md). The [Practice Overlay specification](docs/PRACTICE_OVERLAY_SPEC.md) owns the detailed scope and acceptance checks.

## 10. G1–G3 — V1 Goal colors, Block filtering and truthful statistics

### Current foundation; migrate conservatively

Up to five active Goals, Goal ordering/completion, manual percentages, existing `goal_blocks` many-to-many links and explicit `session_goals` are implemented. At the T2 baseline, Goal time counts committed placements from explicitly Goal-associated sessions. T3B adds standalone activities to the same totals. G1–G3 retain broader Goal identity, filtering and historical reporting, counting each T3B activity once. `placements.activeMs` is actual committed time; Home displays the existing Total practice duration. **Block tags are not implemented at current baseline**—do not assert otherwise or create a separate tag system without explicit approval.

### G1 — Priority colors, primary Goal and historical attribution (High for migration, Medium for UI)

- Active Goal positions 1 through 5 use red, orange, yellow, green and blue across all themes. Goal indicators and trailing Block caps follow the current order immediately after creation, reorder, completion or reopening. Include readable names and priority labels.
- Each Block can have one primary Goal, selected in the Block editor, or No Goal with a black cap. Completed Goals retain their assignments and have a hollow cap and a completed label. The Block objective remains independent.
- Preserve the existing many-to-many Goal links and Goal-launched sessions. An upgrade adopts a sole valid Goal link and leaves zero-link and multi-link Blocks unassigned. Setting a primary creates any missing link. Clearing the primary retains links. Removing the primary link through Goal management also clears the assignment.
- First positive committed placement time records its primary Goal ID or explicit absence and its active rank from 1 to 5 when applicable. Later practice, reorder and reassignment preserve that snapshot. Pre-G1 positive time remains without a placement snapshot. The complete ZIP preserves both assignments and snapshots.

### W1 — This Week

The owner explicitly assigned W1 ahead of G2. Practice Now offers This Week and an In Progress list for unfinished sessions. Weekly plans use Sunday through Saturday in the device's local calendar. Plan membership stores ordered permanent Block IDs. Prepare Week adds active Blocks, creates and adds a Block in one transaction, carries forward active Blocks from a chosen earlier week, and removes membership without deleting the Block. Plans do not roll over automatically.

Expand a Block and choose Practice Block to open independent practice at Ready. Multi-selection adds Blocks in saved order to the existing session builder. The Block Library and editor can add saved active Blocks directly to the current week. Room 20 and complete ZIP v4 introduced preservation of membership and order, including archived assignments. Later ZIP versions retain these records and older ZIP and JSON readers. Journal Weekly View retains its Monday–Sunday calendar.

### Block UX normalization

- Stage 1 implements shared compact pills in the Block Library, This Week and Prepare Week. See [current behavior and verification](CURRENT_STATE.md#stage-1-source-and-verification).
- Stage 1.5 is implemented in signed build108 with compact placement snapshots, an on-demand picker, persistent independent durations and ordering, and an accessible pinned Start Session action. [Scoped verification and phone limitations](CURRENT_STATE.md#stage-15-session-builder-delivery-and-verification)
- Stage 2 reorganizes the editor and supports staging references before the first Save with the current single-visual and single-audio model. Assess Goal Detail and Routine selection for safe reuse of the presentation.
- Stage 3 depends on the stabilized editor and adds ordered multiple visual and audio references, with populated migration fixtures, preservation of reference-local state and complete ZIP round-trip coverage.
- Stage 4 verifies the combined workflows from creation through practice, completion, reuse, history and backup/restore. Playground rewards retain their separate assignment.

### G2 — Shared Goal filtering (Medium)

- Build99 applies one shared filter in Manage Blocks, Practice Now → Add Blocks, the saved Routine editor, and This Week → Prepare Week → Add existing Blocks.
- Choices are **All Goals** (default), the current active Goals in priority order, and **No Goal**. No Goal matches a null primary association. Completed-Goal Blocks remain under All Goals and retain their indicators. Reordering keeps the selected Goal by ID. Completing it returns the picker to All Goals.
- The shared matcher combines Goal, name/action/notes text, and active/archived eligibility. Prepare Week offers only eligible Blocks that are available to add to the displayed week.
- Existing selections and ordering are independent of the displayed results. See [delivery and verification](CURRENT_STATE.md).

Transcribe remains ordinary Block practice: no separate transcription statistics or practice-time counter.

### G3 — Accurate time attribution and concise statistics (High for correctness)

- Make the existing Home **Total practice** duration a link to the concise statistics view. Include cumulative total, recent/weekly time where valid from stored timestamps, per-Block practice time and optionally top-practiced Blocks. Provide Goal detail with Goal total and relevant practice history.
- Use **distinct committed placement `activeMs`** for totals, including recorded active time of unfinished sessions; exclude configured-but-not-worked duration, pauses, backgrounds, pure metronome use and playback. A Block used multiple times counts its distinct placements, never duplicates a placement through joins.
- New practice, whether ordinary, routine or Goal-launched, attributes a placement's recorded time to **its primary Goal at the time that placement first accrues actual time** (or equivalently stable snapshot). Read G1 snapshots and distinguish explicit absence from missing legacy attribution. Subsequent Block reassignment or Goal reorder/complete **must not rewrite historical attribution**.
- Existing `session_goals` relates entire Goal-launched sessions to a Goal. Define a migration/precedence rule to coexist with per-placement primary Goal attribution. Do not double-count the same minutes in global totals or accidentally credit an entire mixed session to every Goal. For old sessions without a defensible primary Goal snapshot, preserve legacy explicit Goal-session attribution **separately and clearly labeled** if reported, and identify unknown/unattributed time rather than inferring it from today's Block relationship.
- **Time measures effort, not mastery.** Do not auto-compute a completion percentage from minutes. The current Goal editor requires manual 0–100%; adapt the presentation so percentage is **optional or only displayed when a meaningful measurable target exists**, retaining legacy entered values without erasing them. Status, notes, accumulated time and practice activity can be shown without a fabricated percent.
- Reuse existing Goal/Block/session screens. Do not invent granular per-day split of a session across midnight where data does not support it; clearly label session-start-date-derived weekly totals if that is the available representation. No streak/gamification/social, broad AI analytics or separate reporting backend.

**G3 exit:** signed candidate with tests for zero/one/multiple legacy links, current colors change with priority after Goal reorder/complete/reopen while earlier snapshots retain their original ranks, filter and search across the four G2 Block-selection contexts, repeated Block placement, different session start types, unfinished/paused/overtime/skip, Block reassignment after past practice, backup/restore of attribution and no duplicate total time. Goal time must never silently be displayed as skill mastery.

---

## 11. F1–F2 — V1 Finalization, full release and real-life acceptance

### F1 — Feature freeze, cleanup and comprehensive automated verification

- No new V1 features after G3. Remove confirmed dead UI/placeholder labels, verify navigation, spacing, Lato, palette, clear error messages and accessibility. Cleanup is **not** an excuse to redesign already accepted screens.
- Run full unit suite + lint + debug/release compilation + complete available **API26/API35** instrumentation matrix, including updated media, SAF, backup and Goal attribution tests. Compare screenshots and verify 200% font, portrait/landscape, overflow/scroll indicators, accessible colors, TalkBack/semantic labels, keyboard transitions and process recreation where feasible.
- Verify old-to-new Room migrations and **supported backup format compatibility**. Test install-over from current permanent-key build, data/media persistence, deletion guards, self-contained backup/restore, file-provider failure, audio-focus/microphone, recording review, 64-beat looping, custom count-in, meter/flash, alarms, Logs/Notes, Goals filters/totals and independent session accounting.
- Extend acceptance to original Loop Recorder/Free Record regression, 100% and slower pitch-accurate playback, short A–B repeat, long waveform navigation, master/local inheritance/promotion, shared storage/deletion, concurrent imported playback/Free Record, keyboard during playback and complete media/legacy restore. Keep phase-scoped checks during development; full regression belongs here after integration.
- Keep a tracked list of failures with reproducible evidence; isolate failed tests and retest after targeted fixes. Run broad suites again at the release gate, not after each cosmetic change. No ungrounded blanket "all tests passed" assertion.

### F2 — Physical phone signoff and permanent-key release

Owner completes and records actual phone checks for: Transcribe speed/pitch/short-loop quality, long waveform navigation, marker inheritance/promotion, keyboard during imported playback, concurrent microphone Free Record with speaker/headphone behavior and acoustic bleed, audio focus/lifecycle without unexpected resume; install-over and saved data; foreground/background safety; free and loop recording with the phone's microphone or connected interface; actual speaker/headphone click/alarm, silent flash and visual timing; long loop memory/playback; Library large PDFs/rotated photos; provider import/export; complete ZIP backup and **clean restore**; Goal time/color/filter correctness; navigation, large text and common practice tasks.

**V1 release artifact:** permanent-key signed installable Android APK and ZIP, `versionName`/increased `versionCode`, matching source SHA/commit, certificate/hash/ZIP validation, explicit CI matrix results, current delivery link/phone checklist and a concise CHANGELOG/readme. Preserve older APKs and signing key. The phone's real tests must be marked completed by the owner—not invented by Work. If critical tests are blocked, ship only an accurately labeled candidate, not a verified V1 release.

**Practical-use condition:** Conduct and document at least a few genuine practice sessions (ideally the already-planned 10-session evaluation) to spot significant friction. Do not automatically open a new feature phase just because evaluation finds an idea; fix actual blockers first. If owner defers the remaining sessions, record that plainly.

---

## 12. Explicit V1 non-goals and boundaries

These are **not** part of V1 unless the owner separately changes this document: in-app scanning/OCR and general document editing, unlimited references beyond one visual plus one active audio per Block, Library folder/tag taxonomy, an entirely new tagging system, importing online scores, notation editing, 128-beat loops, meter-aware loop bar selection, grouped 6/8/subdivision tempo maps, MIDI sync, MP3 encoding, converting every free capture to WAV, multitrack/overdub/mixer/effects, always-on recording, background countdown notifications, user-defined imported sound packs, automated music-learning prescriptions, advanced analytics, streaks, cloud accounts, Google Drive OAuth/sync/background upload, teacher/student/publisher products, band collaboration, desktop companion, marketplace or subscription services. These remain future ideas, not V1 acceptance blockers.

**Transcribe boundary:** Manual listening/phrase practice and audio markers are approved; automatic audio-to-MIDI/transcription, AI note/solo identification, BPM/time-signature analysis, pitch shifting/transposition, video/streaming/YouTube, general PDF/image editing, notation rewriting/generation and freehand PDF annotations remain excluded. O1 adds the bounded Block-local practice layer after S2.

**Visual theme:** Centralized themes are delivered; exact verification remains in CURRENT_STATE. Deliberate palette/contrast polish remains required. The app remains useful offline; no backend is needed.

**Storage release boundary:** Local recording files plus legacy JSON data-only export are **not** enough to call V1 completely backed up. The complete ZIP round-trip is required, even if Drive's nested-folder support proves unavailable and the user must choose an explicit destination manually.

---

## 13. Acceptance checklist for the entire project

Mark these complete only on concrete evidence in `CURRENT_STATE.md` and the Work log:

- [x] This master plan is committed in the existing repo and referenced from BUILD_PLAN/README; baseline/actual next phase correctly recorded.
- [x] P1 recording review delivered and generally owner-accepted; P5 verifies temporary-take safeguards.
- [x] P2 custom 1–64 beats/count-in delivered and generally owner-accepted; WAV/replay covered by P5.
- [x] P3 meter/accent/flash and Tools/practice parity delivered and generally owner-accepted; P5 passed.
- [x] P4 Settings/alarm/transport/Notes delivered and included in accepted P5; no separate itemized phone results inferred.
- [x] P5 complete **pre-Library regression** and signed owner-accepted build67 (November 1, 2026); build69 phone checks remain separate.
- [ ] L1 canonical visual/audio Library, validated legacy imported ownership and audited visual attachment policy.
- [ ] L2 full-screen visual preview, shared audio/waveform/master markers and platform playback feasibility.
- [ ] L3 independent visual/audio links, shared originals and safe legacy picker transition.
- [x] K1 reusable Reference Piano with scoped pitch/polyphony/playback-coexistence verification and signed delivery; physical acoustic checks remain pending in CURRENT_STATE.
- [ ] T1 compact Record Loop / Transcribe modes and phrase practice without recorder regression.
- [x] T2 local overrides/markers, explicit promotion/reset, safe master deletion and persisted audio state. See [CURRENT_STATE](CURRENT_STATE.md) for signed delivery and scoped verification.
- [ ] T3D physical-device concurrent reference playback/Free Record, keyboard and session/lifecycle safety.
- [x] S0 documented [SAF evidence, provider limitations and fallback decision](docs/S0_STORAGE_DECISION.md). Device qualification of Drive, OneDrive and folder-tree operations remains open.
- [x] S1 organized non-destructive audio export and user-selectable destination. See [CURRENT_STATE](CURRENT_STATE.md) for local SAF verification and pending phone checks.
- [x] O0 architecture and roadmap integration with a [single overlay specification](docs/PRACTICE_OVERLAY_SPEC.md).
- [x] S2 verified **full media-inclusive ZIP backup and clean restore** with integrity/failure tests.
- [ ] O1 Block-local overlays, including editable recovery, pass the [overlay acceptance gate](docs/PRACTICE_OVERLAY_SPEC.md#o1-acceptance).
- [ ] G1 stable five Goal colors, neutral caps, optional primary Goal and preserved legacy associations.
- [x] W1 This Week workspace, preparation, session entry and complete ZIP support. See [CURRENT_STATE](CURRENT_STATE.md) for verification and delivery.
- [x] G2 Goal filter and text search in Manage Blocks, Session Builder, saved Routine editor, and Prepare Week.
- [x] P1 Intentional Practice Lifecycle delivered with scoped verification. See [current verification and delivery](CURRENT_STATE.md).
- [x] P2 Block and Session Reflection implemented. See [current delivery and verification](CURRENT_STATE.md).
- [ ] G3 actual time statistics and historically accurate, nonduplicated Goal attribution; no fake progress.
- [ ] F1 full final regression, migrations/backup compatibility, accessibility and failure triage.
- [ ] F2 permanent-key V1 release candidate, real-device acceptance and truthful final documentation.

## P1 Intentional Practice Lifecycle

Every Block opens a Ready gate with its name, session position and planned duration. Start Block begins the shared measured timer. Prep Block opens the existing workspace with tools, playback, references and recording. Start Practice switches from prep to measured time. Reopening a partly practiced placement offers Resume Block. A recovered placement at zero still requires an explicit Continue for overtime.

P1 introduced Up Next with Start Next Block and Prep Next Block, and opens Session Summary after the final placement. Foreground monotonic prep checkpoints accrue separately from practice and Goal totals. Room 21 introduced per-placement `prepMs`, preserved since complete ZIP v5.

## P2 Block and Session Reflection

Complete saves the placement outcome and a placement-keyed completion snapshot before offering an optional security rating of 1–5 and four additional prompts. Skip on that screen advances without a review. Skipping practice itself keeps the skipped outcome and bypasses Block reflection. Up Next still handles deliberate advancement. The final placement commits the session and offers optional session prompts and the existing session note before Session Summary.

Room 22 stores completion identity, timestamp, the Block's primary Goal at completion, and the active priority and color when applicable. First-positive-time Goal attribution stays separate. Completion facts and optional reviews use separate records. Complete ZIP v6 preserves both. Older archives restore without invented completions or answers. This phase supplies historical facts for future earned Blocks.

## Deferred Practice Insights

Log includes the Playground placeholder. Future rewards and collectible layout follow the [Playground specification](docs/PLAYGROUND_SPEC.md) after separate owner assignment. Schedule revisits, suggestions and statistics also require separate assignments.
- **Flags and revisits.** A Block can be flagged Return Soon from review or the Block interface. The flag persists until explicitly cleared. Log flag changes as historical events. An optional Revisit in selector offers intervals such as 1, 2, 3, 4, 5, 7, 14 and 30 days. A deterministic rule can recommend an interval from security ratings and later revisit history. Users can override it. Store the due date, recommendation and chosen interval. Treat scheduled revisits as suggestions.
- **Suggested Blocks.** In Block selection, prioritize manual Return Soon flags, then consider due revisits, security trends, time since last practice and underattended active Goals. Show an understandable reason for each suggestion. Rules stay local, transparent, deterministic and configurable where appropriate. AI coaching and LLM integration are outside scope.
- **Statistics and insights.** Derive duration, frequency, Goal time, practice types, tags where available, per-Block activity, intentional prep, meaningful tool use, confidence trends, revisit effectiveness and weekly or longer patterns from shared persisted activity records. Preserve historical Goal attribution across reassignment and count each practice or preparation interval once in its own category. A dismissible weekly invitation can offer factual summaries and a writing prompt entirely offline.
- **Teacher integration.** A later, non-V1 extension could let teachers assign Blocks and work, set their own revisit thresholds and rules, and review appropriate history and reflections. Multi-user infrastructure, sync and permissions are outside this handoff.

### Minimal prompt for each new Work instance

> Read `PRACTICE_LOGGER_V1_MASTER_PLAN.md` and the current repository development records. Inspect the live branch and identify the next **owner-assigned** uncompleted phase; do not assume the checked-box state is correct without evidence. Implement **only [PHASE ID]** using the prescribed smallest safe change. Run targeted checks, build a signed installable APK for implementation phases, update `CURRENT_STATE.md`, `WORK_LOG.md` and `BUILD_PLAN.md`, commit and push intended changes, verify remote SHA and report exactly what passed/failed and the next recommended task. Stop; do not start the following phase automatically. For P0 documentation only, do not build an APK.

**End state:** Goals explain *why* practice matters; Blocks define *what* is practiced; sessions and recordings capture *what happened*; the Library provides reusable materials; statistics show *how much actual work* was done; and portable backup means those materials can be recovered. That is practice.logger V1.
