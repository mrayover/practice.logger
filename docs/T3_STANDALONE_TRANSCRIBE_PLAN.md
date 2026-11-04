# practice.logger — Standalone Transcribe plan

T3-0 locks the owner’s November 4, 2026 roadmap for standalone Transcribe, activity tracking and recording integration. T2 is delivered in signed 0.23.0/build86 with Room 15 and JSON 7. T3A implements the standalone workspace. T3B–T3D each require a separate explicit owner assignment. [Current state](../CURRENT_STATE.md) · [V1 roadmap](../PRACTICE_LOGGER_V1_MASTER_PLAN.md) · [Owner checklists](../MANUAL_TESTS.md#t3-owner-phone-checklists)

## Owner decisions

The following decisions supersede the previous single-T3 plan and earlier informal T3A/T3B naming.

### Practice Now navigation

Home retains **Practice Now** as the entry point for musical activity.

Selecting Practice Now presents:

- **Continue Session**, when an unfinished session exists.
- **Add Blocks**, opening the existing session builder.
- **Routines**, directly selecting an existing Routine for practice.
- **Transcribe**, launching a standalone transcription workspace.

Do not require another intermediate choice after selecting a workflow.

Preserve current Routine management and existing shortcuts until an explicitly authorized navigation cleanup. Do not introduce duplicate Routine storage or session builders.

### Standalone Transcribe

Standalone Transcribe:

- Requires no Block, Routine or formal Block practice session.
- Operates on canonical Media Library audio.
- Reuses the existing waveform, playback engine, pitch-preserving slowdown, A–B looping, markers and Reference Piano.
- Allows audio selection/import using existing Library behavior.
- Edits **global master annotations** shared across Blocks using the recording.
- Allows the user to attach the recording to a Block later through existing functionality.
- Does not automatically create a Block.

The existing Block Transcribe mode remains available and unchanged in purpose.

Both contexts reuse common audio and UI components. They are entry points, not separate transcription engines.

### Standalone timer

Standalone Transcribe uses an **open-ended stopwatch**:

- Start begins counting active transcription time.
- Stop saves the completed activity.
- No prescribed duration, countdown or overtime behavior.
- Preparing audio or listening before Start does not automatically count time.
- A Goal association is optional and initially defaults to No Goal.
- The Goal can be selected before or during the activity.
- The completed activity belongs to the selected Goal if one is assigned.
- Background time does not silently accumulate.
- Playback and the timer remain independent controls.
- A Block session and standalone Transcribe cannot count the same simultaneous interval twice.

### Activity persistence

Do not store time by directly incrementing a Goal total.

Store one small durable activity record containing:

- Stable activity ID.
- Started date/time.
- Ended date/time.
- Committed active duration.
- Optional Goal ID.
- Optional canonical audio ID.

On canonical media deletion, clear the optional audio link while preserving the activity and its duration. Preserve completed or archived Goal attribution by stable ID, using the same historical protection as session attribution. Guard removal of a referenced Goal so it cannot silently erase attribution or recorded time.

Aggregate these records with the existing practice-session time.

The existing Goal time query currently counts session-associated practice. Extend the shared accumulation model to include standalone transcription activities.

No new statistics subsystem is authorized.

G1–G3 will still handle the broader previously approved Goal association, color, filtering and historical reporting work.

### Direct microphone capture

Add a future **Record Reference** action in both standalone and Block Transcribe.

The intended workflow:

**Record Reference → Stop → Review → Keep/Rerecord/Discard → Open in Transcribe**

When Keep succeeds:

- Save one canonical microphone recording in the Media Library.
- In standalone Transcribe, select the new recording automatically.
- In Block Transcribe, associate it with the current Block and open it.
- Preserve the previous reference until the new save/association succeeds.
- Protect existing Block-local annotations from accidental loss.

Use the existing microphone-recording infrastructure rather than building a second independent recorder.

Reference audio playback pauses during this capture workflow.

No source-stream capture, microphone overdubbing, trimming or automatic audio enhancement.

### Concurrent microphone recording

A separately scoped later increment allows reference audio playback while Free Record captures the musician's performance.

That workflow is distinct from Record Reference.

Do not accidentally implement concurrent recording as part of standalone navigation or simple microphone reference capture.

The captured microphone take retains its existing review/save ownership. There is no automatic digital mixing of source and recorded audio.

---

## Milestones

### T3A — Standalone Transcribe Workspace

**Goal:** Access transcription directly without creating a Block. See [current state](../CURRENT_STATE.md) for T3A verification and delivery.

#### Scope

- Implement the Practice Now decision screen.
- Preserve Continue Session.
- Add Blocks opens the existing builder.
- Routines opens a direct existing-Routine selection/start workflow.
- Transcribe opens a standalone workspace.
- Reuse the Media Library audio playback and master marker infrastructure.
- Include existing waveform, slowdown, A–B, master markers and piano.
- Support selecting or importing an audio file.
- Preserve playback lifecycle/background safety.
- Keep master annotations clearly identified as shared across Blocks.

The existing Library workspace remains a global media-management area. Standalone Transcribe has a dedicated activity interface.

#### Exclusions

- No standalone stopwatch or Goal attribution yet.
- No microphone capture.
- No new database table.
- No simultaneous microphone/reference recording.
- No transcription projects or transcription-specific Block types.

#### Exit criteria

A user can open Practice Now → Transcribe, choose a recording, slow it down, loop a phrase, use the piano, and edit its master markers without creating a Block.

Practice Now's existing Block and Routine workflows remain functional.

**Testing:** Targeted navigation, reuse, lifecycle and API26/API35 UI checks. Signed install-over APK and owner checklist.

---

### T3B — Standalone Timer and Goal Attribution

**Goal:** Accurately log open-ended transcription activity alongside ordinary practice.

#### Scope

- Add Start/Stop stopwatch controls in standalone Transcribe.
- Optional Goal selector using existing Goals.
- Record active practice intervals durably.
- Create one completed activity record on Stop. Use its stable ID for retry-safe finalization so a repeated Stop or save retry cannot add time twice.
- Show elapsed time without crowding waveform/piano.
- Pause/checkpoint safely when leaving or backgrounding.
- Use a monotonic elapsed-time clock while active.
- Restore interrupted activity paused from its durable checkpoint. Resume only after user action and exclude any elapsed interval that cannot be verified.
- Prevent overlapping Block and standalone activity accounting.
- Include standalone activity time in existing overall/weekly practice totals.
- Include Goal-associated standalone activity time in the existing Goal's accumulated practice time.
- Preserve Goal progress as manually entered; elapsed time must not modify completion percentage.
- Handle zero-length activities and failed saves truthfully.

#### Persistence

Room 15→16 adds one activity table. A null completion timestamp denotes an unfinished row. The monotonic anchor exists only in memory. Checkpoints use absolute active duration every five seconds and at pause, stop, navigation or Goal/audio changes. Recovery restores the last committed duration paused. Unverified time since the last durable write may be lost. A failed write pauses the timer and retains its in-memory elapsed time for retry. Stop retries update the same ID and cannot reopen a completed record. A zero-duration save adds zero to totals.

Goal attribution is explicit and restrictive. Canonical audio is optional and clears on deletion. Goal time combines independent sums from linked placements and completed activities. Home total and Weekly View combine committed Block placement time with completed standalone time, including No Goal. Both sources use the local start date and Monday–Sunday weeks. A cross-week activity contributes its entire duration to the start week. JSON 7 keeps its existing reader and writer formats and blocks restore into a target with any standalone activity. S2 owns portable recovery.

Use a lightweight activity record within the existing practice accounting model.

Preserve historical activity time when an associated media asset becomes unavailable.

Do not create separate transcription analytics or a second Goal model.

#### Exit criteria

A user can spend an open-ended period transcribing, press Stop, and have the committed time appear exactly once in overall practice totals and, when selected, the appropriate Goal.

The activity survives restart without fabricated elapsed time.

**Testing:** Clock correctness, pause/background/process death, Goal association, aggregation, overlap prevention, migration and restore safety. Targeted signed delivery.

---

### T3C — Record Reference Directly in Transcribe

**Goal:** Capture a musician's phrase immediately and begin transcribing without menu diving.

#### Scope

- Add Record Reference to standalone and Block Transcribe.
- Reuse microphone permission, capture safety, temporary file handling and review conventions.
- Record AAC/M4A compatible with canonical Library import.
- Present a compact elapsed recording timer.
- Provide Stop and Review.
- Review offers playback, Keep, Rerecord and Discard.
- Keep saves one canonical Library original.
- Select the new recording immediately in standalone Transcribe.
- Attach/select the new audio in Block Transcribe.
- Respect T2's confirmation rules when replacing audio that contains local annotation work.
- Preserve the previous reference if recording/import/attachment fails.
- Clean abandoned temporary files conservatively.
- Pause reference playback before starting microphone capture.
- Never create an ordinary practice take as a side effect of Record Reference.

Use existing validated size/duration limits; do not invent an unlimited recording path.

#### Exclusions

- No trimming.
- No automatic transcription.
- No noise reduction.
- No audio effects.
- No recording from Spotify/YouTube or other apps' playback streams.
- No concurrent reference playback during Record Reference.
- No new independent microphone-recording subsystem.

#### Exit criteria

A user can ask someone to repeat a phrase, tap Record Reference, capture it, review it, Keep it, and immediately slow or loop it in Transcribe.

Existing Library assets and old Block annotations are never silently overwritten.

**Testing:** Mic permission, Stop/Review/Keep/Rerecord/Discard, interrupted capture, failed import, canonical single-copy storage, Block association and standalone selection. Signed delivery.

---

### T3D — Concurrent Transcribe Playback and Free Record

**Goal:** Record one's own playing while listening to a reference.

#### Scope

- Permit existing Free Record microphone capture while a canonical reference is playing.
- Resolve the existing recording/viewmodel audio-stop and focus ownership conflict.
- Preserve independent reference playback and microphone capture controls.
- Preserve ordinary temporary take review, Keep/Rerecord/Discard and take ownership.
- Keep recordings attached to their correct Block/session context.
- Maintain practice timer and Goal accounting without double counting.
- Preserve source playback speed, A–B and marker settings.
- Handle audio interruptions, backgrounding, rotation and microphone failure.
- Verify actual phone behavior with headphones and speaker output.
- Document acoustic bleed when recording with the phone's speaker.

#### Explicit limits

- No source-audio mixdown into the microphone file.
- No multitrack overdubbing.
- No sample-perfect synchronization.
- No BPM detection.
- No automatic transposition.
- No simultaneous beat-based Loop Recorder and reference playback requirement.
- Do not change Record Reference into a concurrent-recording feature.

#### Exit criteria

The reference can keep playing while Free Record captures a take, and both playback and recorded audio remain valid and independently controllable.

Existing normal Free Record, Record Loop and Transcribe operations continue working.

**Testing:** Focus ownership, lifecycle, data safety, API26/API35 targeted tests and real Android microphone/headphone verification. Signed delivery.

---

## Dependencies and roadmap reconciliation

Update the authoritative sequence to:

**P5 → L1 → L2 → L3 → K1 → T1 → T2 → T3-0 → T3A → T3B → T3C → T3D → S0 → S1 → S2 → G1 → G2 → G3 → F1 → F2**

Already delivered milestones must remain marked as delivered.

T3-0 is a documentation checkpoint.

Each subsequent phase requires a separate explicit owner assignment.

### Storage/backup impact

S2 complete backup must also preserve:

- Standalone transcription activity records.
- Optional activity Goal associations.
- Historical time attribution.
- Canonical microphone reference recordings.
- Existing global and Block-local markers.
- All already planned shared Library and recording data.

Maintain JSON7's current limitations until a separately authorized recovery phase replaces them safely.

### Goals/statistics impact

T3B implements only the necessary shared time attribution for standalone transcription.

G1–G3 retain the broader approved Goal and usage-statistics scope.

Do not implement that broader work early or count T3B activity time twice when G1–G3 arrive.

### Permanent exclusions

No transcription-project database, separate transcription-session hierarchy, automatic audio-to-MIDI, streaming service capture, new media backend, cloud synchronization, effects suite or automatic creation of Blocks from transcripts.

Standalone and Block Transcribe remain two contexts using the same underlying components.

---

## Implementation baseline

- Reuse the canonical Library import, shared waveform and Library audio controller. Block Transcribe retains T2’s master inheritance, local overrides, explicit publication and confirmed audio replacement. Standalone uses the same Reference Piano keyboard and embedded focus grant. [Audio workspace](MEDIA_AUDIO_WORKSPACE.md) · [Reference Piano](REFERENCE_PIANO.md)
- Existing Goal totals sum committed placement time joined through session Goal links. T3B adds committed standalone activity time to the same accumulation path. The standalone stopwatch and the existing practice clock must share an exclusive accounting boundary.
- Free Record already owns microphone capture, temporary files and take review. Its capture entry point stops its own playback and existing audio-focus handling must be reconciled with the reference controller for T3D. T3C reuses capture and review behavior with Library ownership on Keep. The recording and any required Block association must succeed before the previous Block link or local work is retired.
