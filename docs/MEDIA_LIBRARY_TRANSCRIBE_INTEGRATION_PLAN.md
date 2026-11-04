# practice.logger — Media Library, Reference Keyboard & Transcribe Integration

**Status:** Approved design; L1–L3 are implemented under explicit owner handoffs. Actual scoped verification, signed delivery and separately pending phone acceptance are in CURRENT_STATE. K1 and T1–T3 remain planned. See [L1 ownership decision](MEDIA_LIBRARY_STORAGE.md) and CURRENT_STATE for delivered evidence.
**Amended:** November 3, 2026 (America/Los_Angeles).
**Verified repository baseline:** `main`, `aa3e755638bfbb14e039cf8764d40a5ad76df042`.

This is the detailed specification supporting the [authoritative V1 master roadmap](../PRACTICE_LOGGER_V1_MASTER_PLAN.md), not a second master plan. [CURRENT_STATE](../CURRENT_STATE.md) governs actual delivery and verification. The original amendment authorized documentation only; the later explicit owner handoff authorized L1 implementation, now delivered with scoped verification and signed packaging. Reinspect live code before each separately assigned phase.

**Order:** accepted P5 → L1 → L2 → L3 → K1 → T1 → T2 → T3 → S0–S2 → G1–G3 → F1–F2. Build69 camera/gallery owner acceptance is recorded; L1–L3 have explicit implementation handoffs. Phone acceptance remains distinct from engineering verification. After the assigned L3 delivery, K1 Reference Piano is next by separate assignment; T1 follows K1. L1/L2 phone acceptance remains separately pending.

## 1. Verified existing implementation

The repository already contains:

- P5 pre-Library regression: passed and owner accepted November 1, 2026.
- Current release 0.17.3/build69, source `9c723442`, providing camera/gallery reference actions with targeted verification.
- Single Block-owned image/PDF references, rendering, zoom, page navigation, file validation and retained practice workspace.
- Beat-based WAV Loop Recorder, count-in, temporary take review and saved takes.
- Free AAC/M4A recording and playback.
- Imported backing-track support for MP3, M4A and PCM16 WAV, stored as `IMPORTED_LOOP` takes.
- Existing Android `MediaPlayer` playback for imported audio, separate from recorded PCM loop playback.
- Room schema 11, JSON backup format 7, and existing explicit migrations.
- Existing metronome, tuner and timer Tools.

The list above records the pre-L1 baseline. L1 now adds independent canonical media storage/management with Room12; preview/linking remains L2/L3. Legacy adoption is deferred until L3 can change resolution, deletion and recovery safely together; existing bytes/IDs/associations stay intact. See [storage implementation](MEDIA_LIBRARY_STORAGE.md).

Relevant source locations:

- `docs/IMPORTED_AUDIO.md`
- `app/src/main/java/com/mrayover/practicelogger/AudioImport.kt`
- `RecordingData.kt`
- `RecordingViewModel.kt`
- `RecordingSheet.kt`
- `PracticeScreens.kt`
- `PracticePanels.kt`
- `ImageReferenceData.kt`
- `ImageReferenceView.kt`
- `ToolScreens.kt`
- `BlockDatabase.kt`
- `BackupData.kt`

### Important conflicts to resolve

1. The pre-amendment V1 master plan allowed only one Library reference per Block. The new design requires one visual reference and one independently selectable audio reference.
2. Imported backing tracks currently belong to a Block's take collection. New imported references must instead support shared canonical ownership.
3. The existing `Add Backing Track` control appears within Loop Setup. The future interface should use separate Record Loop and Transcribe modes rather than maintain two competing imported-audio workflows.
4. `RecordingViewModel.startCapture()` currently calls `stopPlayback()`. Concurrent Transcribe playback and Free Record therefore require a deliberate audio-ownership change.
5. The pre-amendment roadmap excluded transcription and markup. Only the manual audio Transcribe scope below is approved; unrelated document editing remains excluded.
6. Existing JSON backup excludes media. New shared assets, local annotations and audio associations must be included in the later complete-backup phase.

Do not erase historical decisions or claim that new capabilities already exist.

---

## 2. Architectural principles

### Global Library is authoritative

The Media Library owns one canonical managed original for each imported asset.

Supported types:

- PDF documents.
- JPEG, PNG and WebP images.
- MP3, M4A and supported WAV audio.

Every asset receives a stable media ID, title, metadata and a validated storage reference.

The Library is where media is aggregated, previewed and prepared. It is not a practice-session environment.

### Blocks are the working environment

Blocks link to Library assets by stable media ID.

For the initial scope, a Block may attach:

- One visual reference (PDF or image).
- One active audio reference for Transcribe.

This is intentionally **not** an unrestricted multi-attachment system.

Different Blocks may link to the same audio or visual original. No duplicate media bytes are created per Block.

### Global versus Local

Global annotations belong to the Media Library.

Local annotations and playback configurations belong to individual Blocks.

Local changes never silently alter global information or other Blocks.

An explicit per-marker **Add to Master** or **Update Master** action may promote local work into the global annotation set. A **Reset to Master** action removes a local override.

### Storage principle

One canonical audio asset is stored once, regardless of the number of referencing Blocks.

Markers, notes, playback speed, selected sections and positions are lightweight metadata, not audio copies.

Do not introduce a second recording repository or a second independently owned waveform/annotation system.

---

## 3. Revised milestone sequence

### L1 — Canonical Media Library and storage

Extend the existing L1 scope to include audio as a first-class Library asset.

Deliver:

- Stable media identity and a single local canonical original.
- Bounded importing of visual and audio files through existing SAF mechanisms.
- File validation, metadata, import failure recovery and safe deletion.
- Library list with search, rename, sorting and type filtering.
- A storage ownership model independent of Blocks.
- A safe transition strategy for existing `IMPORTED_LOOP` takes.

**Critical migration requirement:** Existing imported backing tracks must remain playable, exportable and correctly associated with their Blocks.

Before coding the transition, inspect whether existing owned audio files can be adopted into the canonical media model without physically copying their bytes. Prefer an in-place ownership/reference migration where safe.

Do not blindly delete existing takes, rewrite their histories, or duplicate imported files during migration.

The L1 owner handoff supersedes the previous test-only disposal exception: preserve all existing visual attachments and imported takes. L1 uses safe coexistence; adoption waits for L3 ownership/resolution/deletion/recovery changes documented in MEDIA_LIBRARY_STORAGE.md.

**Exit:** Canonical storage and legacy ownership are validated with targeted persistence, migration, deletion and failure tests.

### L2 — Media preview and master annotation workspace

Retain planned full-screen PDF/image viewing using existing renderers.

Add an audio workspace to the same Media Library.

Audio capabilities:

- Play, pause and seek.
- Visible waveform with playback position.
- Zoomable/scrollable timeline for precise positioning.
- A–B section selection.
- Continuous selected-section repeat.
- Playback speed adjustment without changing pitch.
- Global named point markers and optional ranged sections.
- Optional notes attached to markers or sections.
- Add, rename, move, resize and delete master markers.

Audio references and backing tracks are both ordinary audio assets. Do not create separate storage subsystems for those uses.

Implement waveform generation as a bounded cached visualization. It must not require loading an entire large recording into memory as decoded PCM.

The Library may preview and annotate audio, but practice sessions, Free Record, Goals and session-time tracking remain exclusive to practice workflows.

**Technical feasibility gate:** Before committing to a playback engine, verify pitch-preserving time stretching, seeking and A–B repeat using the existing Android playback infrastructure. Start with platform playback parameters. If real devices exhibit unacceptable gaps or poor short-loop behavior, document the limitation and propose the smallest justified alternative for owner approval.

Do not silently add a new media framework, promise sample-perfect compressed-audio looping, or impose metronome alignment.

**Exit:** Shared audio player/waveform and master marker features are functionally tested and reusable by the future Block interface.

### L3 — Block Media Library associations

Extend existing Block references rather than rebuilding the Block editor.

Deliver:

- Choose visual reference from Library.
- Import a visual reference directly into Library and link it.
- Choose audio reference from Library.
- Import audio into Library and link it.
- Preserve the compact existing visual reference panel.
- Store only stable reference IDs inside Blocks.
- Retain original files when a Block unlinks them.
- Prevent accidental deletion of referenced Library assets.

The audio association is separate from the visual reference association.

A Block should be able to display a score and use an audio reference during the same practice session.

L3 establishes canonical selection in the Block editor and preserves the existing Loop Setup importer and take-owned history. Convergence belongs to T1 after K1, per the explicit L3 handoff. The eventual Transcribe picker reuses Library imports without creating new Block-owned audio duplicates.

**Exit:** Two Blocks can reference the same audio file, use independent visual references, and survive restart without multiplying the stored audio bytes.

### K1 — Reference Keyboard

Add a fourth reusable Tool: **Reference Piano**.

Requirements:

- Compact on-screen piano with approximately 12–18 semitones visible.
- Adjustable keyboard register; users can shift the visible range.
- Audible reference pitches with correct note frequencies.
- Support simultaneous notes for checking intervals and chords.
- Reuse the same keyboard component in standalone Tools and Transcribe.
- Collapsible, space-efficient placement in Transcribe.
- Keyboard interaction must not interrupt audio reference playback.
- Touch layout must be usable on typical phone screens, with an expanded presentation if compact keys become too small.
- Appropriate lifecycle and audio-output cleanup.

Avoid MIDI, recording, synthesizer editing, chord detection, accompaniment or notation generation.

The keyboard is a pitch reference, not another recording subsystem.

**Exit:** Standalone keyboard works across its supported range and is reusable from an active practice session.

### T1 — Block Transcribe mode

The Focused Practice transport now consumes the canonical AUDIO link and reuses the Library player, waveform and master markers with an expandable K1 keyboard. See the [implemented architecture](MEDIA_AUDIO_WORKSPACE.md#t1-in-focused-practice) and [current delivery status](../CURRENT_STATE.md).

Modify the existing Loop transport presentation to support two selectable modes:

**Record Loop**
- Retains the existing beat-based microphone recording workflow.
- Retains BPM, beat count, count-in and existing take review.
- Does not inherit the Transcribe player's speed controls or audio markers.

**Transcribe**
- Uses the Block's selected canonical audio reference.
- Allows selecting/changing audio from the Media Library.
- Presents the shared waveform and transport controls.
- Supports seeking, zoom, playback speed, pitch preservation and A–B repeat.
- Supports tapping a marker to jump to its location.
- Allows refining practice ranges without editing the global master.
- Provides access to the reusable reference keyboard.

Do not introduce special transcription Block types.

The existing transport remains compact when collapsed. Opening Transcribe changes the expanded panel, not the underlying Block or practice-session model.

Use established layout behavior: opening a transport panel temporarily collapses upper Description/Reference regions, then restores their prior state when the panel closes.

**Exit:** A normal Block can enter Transcribe, select a recording, isolate a short phrase, slow it down and practice without changing the original Loop Recorder.

### T2 — Local annotation and persistence model

Implement Block-local customization over shared master annotations.

Suggested logical records:

- `MediaAsset`: canonical global file metadata.
- `MediaMarker`: stable global marker identity, timestamp/range, label and optional note.
- `BlockMediaLink`: Block-to-asset associations, distinguishing visual and audio usage.
- `BlockMarkerOverride`: local modifications/hiding of inherited markers.
- `BlockLocalMarker`: new markers belonging only to one Block.
- `BlockAudioState`: selected media, speed, playback position and active A–B loop.

Exact Room entities are implementation decisions; minimize tables where safe, while preserving stable identity and migration clarity.

Rules:

1. Unmodified inherited markers follow master changes.
2. Local changes affect only the current Block.
3. Adding local markers never modifies master data.
4. Locally hiding an inherited marker does not delete its master.
5. Individual local markers may be deliberately promoted to master.
6. Modified inherited markers may deliberately update master.
7. Other Blocks' explicit overrides remain unchanged by master promotion.
8. Resetting an override resumes inheritance from the master.
9. Deleting a master marker must preserve valid independent local work and remove invalid inherited display state safely.
10. A Block's selected phrase, speed and position survive session exit and reopening.

Promotion must be explicit, understandable and tested. Avoid automatic bidirectional synchronization and whole-Block bulk publishing.

**Exit:** Master/local behavior remains correct across edits, multiple Blocks, master revisions, deletion and process restart.

### T3 — Audio and practice integration

Ensure the completed feature works within the actual practice environment.

Requirements:

- Free Record remains independently accessible.
- Transcribe audio can continue playing while Free Record captures microphone audio.
- Do not force existing beat-based Loop Recorder and Transcribe player to run together.
- No metronome synchronization, tempo detection or beat-grid alignment for imported audio.
- No automatic reference-audio mixing or multitrack capture.
- A reference keyboard can be used while hearing imported audio.
- Notes and visual references remain available without unnecessary playback interruption.
- Playback and recording retain appropriate foreground/background, interruption and audio-focus handling.
- Recordings remain associated with the correct Block/session and retain existing review/save behaviors.
- Practice time is counted once by the existing session system, not separately for Transcribe playback.
- Backgrounding and reopening must not silently resume audio unexpectedly.

**Existing code conflict:** Free Record currently stops active playback. Refactor audio ownership only as much as necessary to allow the approved coexistence. Verify real-device microphone recording and speaker/headphone behavior, including possible acoustic bleed.

Do not reinterpret simultaneous capture as approval for overdubbing, synchronization or source-audio mixing.

**Exit:** A complete transcription practice session works on a physical Android device with stable audio, persistent state and no damage to existing recording workflows.

---

## 4. Resume the existing pipeline

After T3, resume the V1 master plan's existing sequence.

### S0–S2 — Storage and recovery

Preserve the existing provider feasibility, organized recording exports and media-inclusive ZIP backup objectives.

Extend complete backup/restore coverage to include:

- All canonical PDFs, images and audio assets, each stored once.
- Global audio markers and annotation notes.
- Block-local markers, overrides, A–B selections and speed settings.
- Stable Block/media relationships.
- Existing imported backing-track histories and recorded takes.
- Reference keyboard settings if persisted.
- All prior Blocks, Goals, sessions, Routines, practice notes and daily Logs.

Do not duplicate a shared audio file for every referencing Block in the ZIP.

Preserve legacy JSON readers 1–7 and apply explicit safe schema migrations.

A complete restore must reconstruct the same shared references and master/local inheritance behavior.

### G1–G3 — Goals and Statistics

Keep the already-approved Goal colors, Block filtering and actual-time attribution.

Do not add separate transcription statistics or a new practice-time counter.

Transcribe activity is part of ordinary Block practice.

### F1–F2 — Finalization and verification

Run the full regression gate only after feature integration is complete.

Extend acceptance to cover:

- Original Loop Recorder and Free Record regression.
- Audio playback at 100% and slower speeds.
- Pitch accuracy and short A–B repeated playback.
- Waveform navigation of longer recordings.
- Correct marker inheritance, overrides and promotion.
- Correct shared storage ownership and deletion safety.
- Concurrent imported playback and Free Record.
- Keyboard during audio playback.
- Media-inclusive export/restore and legacy migration.
- Physical phone navigation, audio focus and lifecycle.
- Install-over without uninstalling or clearing existing user data.

Keep targeted tests per increment. Do not rerun full P5 or the entire final regression matrix for documentation changes or routine visual refinements.

---

## 5. Explicit scope exclusions

These are not approved by this amendment:

- Audio-to-MIDI or automatic transcription.
- AI note identification or automatic solo extraction.
- Automatic BPM or time-signature analysis.
- Metronome-to-recording synchronization.
- Pitch shifting or transposition of the recording itself.
- Multitrack editing, overdubbing or automatic reference mixing.
- Video imports or streaming media.
- YouTube integration.
- A general PDF/image editor.
- OCR, notation rewriting or score generation.
- Global/local PDF pencil/highlight annotation in this increment.
- Unlimited simultaneous visual/audio references per Block.
- Cloud synchronization or an account/backend service.

Potential later enhancement: non-destructive image/PDF highlights, pencil marks and comments, following the same Global/Local principles. Defer until the core audio workflow and complete backup are reliable.

## 6. Implementation decisions and compatibility gates

The exact Room entities and schema versions remain implementation decisions. L1 must audit real attachments and legacy imported-file ownership before choosing adoption/migration and deletion rules. Imported histories, IDs, playback and byte-preserving export must survive. L2 must establish real-device platform speed/seek/A–B quality before selecting an engine; any justified alternative needs owner approval. T3 must resolve playback/capture focus and lifecycle ownership, verify microphone/speaker/headphone behavior and document acoustic bleed. S2 must version and validate complete recovery without changing the current JSON7 contract prematurely.

Reuse [existing audio import](IMPORTED_AUDIO.md), visual renderers, recording repository, practice accounting and [current backup safeguards](../BACKUP_FORMAT.md). No new feature in this specification is delivered by this documentation amendment. Keep targeted checks per assigned increment and the full regression at the final F1/F2 gate.

## K1 implementation checkpoint — November 3, 2026

K1 is delivered as build81 with one reusable PCM engine and Compose keyboard, standalone Tools wrapper and borrowed-focus active-reference coexistence. T1 remains unimplemented and must embed the same instrument. See [architecture and current focus contract](REFERENCE_PIANO.md) and [delivery/actual scoped evidence](../CURRENT_STATE.md). This checkpoint records implementation without expanding later milestone scope.
