# L2 media preview and master audio workspace

L2 adds dedicated Library image/PDF previews and a reusable canonical-audio preparation workspace. Actual verification, signed delivery and phone acceptance are recorded in [CURRENT_STATE](../CURRENT_STATE.md). L1 phone acceptance remains separately pending. L3 now adds independent Block associations using the same canonical originals; see [Block media ownership](BLOCK_MEDIA_ASSOCIATIONS.md). Transcribe UI remains outside L3.

## Shared visual rendering

`MediaLibraryRepository.visualPage` resolves a canonical ID with existing integrity checks on IO. `ImageReferenceRepository.loadOriginalPage` shares the existing sampled EXIF-oriented decoder and bounded 2048px PdfRenderer page rendering. `ZoomableReference` shares fit, pinch/pan and reset behavior with existing Block references. Each page/document/descriptor closes on IO; only the displayed bitmap is retained. Originals, import validation and existing Block ownership remain unchanged. Dedicated previews hide the shared banner and retain their own Back below system bars. Enlarged text uses compact source readouts with full numeric precision in edit dialogs, preserving independent waveform/marker scrolling. Library list query/filter/sort/scroll survive closing a preview. Missing originals show retry.

## Engine and source-time controls

`LibraryAudioController` uses the existing Android MediaPlayer infrastructure, without a Compose, Block, take or practice-session dependency. Its public inputs are validated canonical IDs and source milliseconds. A repository resolves the private original; no arbitrary external paths enter UI models. `LibraryAudioState` publishes transport state. `AudioWaveformView` and `AudioTimeline` are reused by the T1 Block panel with the same controller API.

Speed is 50–100% in 5% steps. PlaybackParams sets pitch independently to 1.0. Speed changes while playing update the same player; paused changes store the speed without starting playback. Display/ranges/markers use original source milliseconds, independent of speed. Seek requests serialize and coalesce until platform seek completion. A 15ms foreground cursor check requests a closest seek at B for continuous A–B repeat. Play selection turns off repeat and plays once to B. Full-file and A–B repeat are mutually exclusive; loading a named section does not enable repeat.

This is asynchronous platform playback, not a sample-clock loop engine. Codec seek granularity, seek completion, scheduling and output buffering can produce audible gaps/overshoot, especially for very short compressed phrases. No sample-accurate or perfectly seamless join is promised. Numeric A/B seconds with three decimals and ±10ms adjustments are accessible alternatives to separate 48dp drag handles. Actual practical short-loop results belong in the verification record; audible pitch and vendor/headphone behavior still need owner-phone checks.

Backgrounding or leaving releases the player and abandons focus. Interruption/ducking and disconnected-output broadcasts stop playback; it never auto-resumes. Configuration recreation retains the ViewModel, cursor, range, speed and viewport; process recreation begins paused. The Library player owns foreground audio output. Block Transcribe coordinates it with the existing microphone recorder. Audio playback has no practice timer or statistics side effect.

## Derived waveform cache

WAV analysis streams PCM16 chunks; MP3/AAC analysis drains MediaExtractor/MediaCodec into bounded output buffers. It never accumulates full decoded audio. The envelope stores actual absolute signal peaks in fixed 5ms source-time bins: at most 180001 floats (~704KiB) for fifteen minutes. Rendering aggregates visible bins per pixel; viewport math maps source time to pixels independently of speed. Channel peaks preserve stereo events. Codec output presentation timestamps locate decoded buffers on the source timeline.

Cache files live in `cache/audio-waveforms/<mediaId>-<originalSHA256>.wave`, with a versioned header, duration/count validation and finite bounded peaks. A corrupt/missing cache regenerates; writes use unique pending files and close resources even on cancellation. Original identity/integrity is checked before cache reuse. At most twelve completed recording caches are retained (under 9MiB); UI-confirmed media deletion also removes that recording's cache. These are expendable derived data, excluded from backup. Originals are never rewritten, trimmed, converted or copied by playback/analysis.

## Master annotations and Room13

Explicit migration12→13 adds only `media_markers`, retaining all prior exported schemas/migrations. A stable UUID identifies each master point or section; fields are mediaId, type, source start/end milliseconds, label and optional note. Composite mediaId/startMs indexing supports ordered observation. Repository transactions validate audio ownership, source duration, label and note bounds; editing retains marker identity. Sections must be nonempty and inside duration. Marker positions are independent of current A–B selection.

The parent foreign key is RESTRICT. Confirmed canonical deletion explicitly removes owned markers and then the asset within one transaction. A restrictive L3 Block asset link rejecting deletion rolls back marker removal too; existing original-file cleanup/recovery remains unchanged. Direct asset deletion cannot orphan annotations. There is no Block-local override/inheritance state in L2.

JSON7/readers1–7 stay unchanged: canonical metadata, originals and master annotations are not portable through this data-only backup. Retain originals separately. S2 must include canonical originals once and master/local annotation identity in its later complete ZIP recovery. Legacy `block_images`, `takes`, `IMPORTED_LOOP`, their files and export/recovery ownership remain untouched.

## T1 in Focused Practice

The Record Loop selector opens Transcribe in the bounded practice transport. It resolves the canonical AUDIO link by the source Block ID, then uses the same controller, waveform cache, source-time controls and marker repository as the Library. Block markers and sections are read-only. Choosing or importing audio uses the existing role validation and replaces only the Block link after validation succeeds. The Library keeps the original.

The expanded keyboard uses the reference controller's focus while playing or paused. Pause leaves held notes sounding. Closing the keyboard releases its own output and leaves the reference transport available. Source changes, panel exit, backgrounding and output interruption release audio. Playback resumes only on explicit Play. Practice timing remains with the session. Record Loop and Free Record use their existing capture path. Saved legacy backing takes stay in the take system.

T1 introduced the shared workspace. The [Standalone Transcribe plan](T3_STANDALONE_TRANSCRIBE_PLAN.md) defines T3A reuse of this audio workspace, T3C Record Reference with playback paused, and T3D reference playback with ordinary Free Record capture. See [Reference Piano](REFERENCE_PIANO.md) and [verification](../CURRENT_STATE.md).


## Record Reference in T3C

Standalone and Block Transcribe reuse the foreground AAC/M4A recorder, temporary review and cleanup. Opening the recording dialog pauses the reference. The existing Library size and duration limits govern capture and Keep. Review offers preview, Rerecord, Discard and Keep. Only Keep creates a canonical Library asset.

Stage3C.2 Block Transcribe uses compact ordered audio selection with one controller. Switching awaits the outgoing final checkpoint, retains the old source/settings on failure and restores the incoming Block/media settings paused. Capture/review/save and marker edits guard switching. Record Reference commits the new canonical asset and appends its link together after rechecking the reviewed full audio order, then selects the new source. Failure or cancellation keeps previous references/local work and retryable review. Standalone Keep selects its new master asset in place. Reference capture creates no ordinary take and needs no Room or JSON change.

## Block-local audio in T2

The Room 15 migration adds state and marker tables scoped to the active Block/audio link. The three-column foreign key identifies the exact association and cascades on unlink. Replacing an audio link removes its old local rows in the same transaction. The editor asks before removing local work or non-default saved playback state. The Library still owns the canonical original and its master markers.

Transcribe restores source-time cursor, speed, A-B range, repeat and surviving marker selection with playback paused. It checkpoints playback and rapid seeks at bounded intervals and saves control changes and the final foreground state. Writes validate the active association. Library playback keeps its own state.

One repository calculation feeds both waveform and list. A Block can create points and sections, hide inherited items, or store a whole-marker override. Reset removes the local record and resumes inheritance. Deleting a master removes hide records and leaves customized markers as local-only, retaining their IDs.

Add to Master inserts a master and removes the local record in one transaction. Update Master retains the original ID and compares the current master with the override's original content snapshot. If it changed, Transcribe shows the current and proposed versions. A second confirmation compares against the reviewed version again. Other Blocks keep their overrides.

JSON 7 excludes Block audio settings and local annotations along with existing media exclusions. S2 owns their portable backup and restore.

## Standalone Transcribe in T3A

Practice Now opens a full-screen activity with a scrollable waveform, transport, piano and master annotations. The AUDIO-only picker lists canonical Library recordings and imports supported files through Android's document picker and existing validation. Selection is a saveable media ID for the current visit. Library and standalone share transport, waveform and master annotation controls. Block and standalone share the embedded piano, which borrows the reference controller's focus.

Changing source or leaving releases playback and piano output. Backgrounding releases output and returning prepares the selected source paused. Rotation restores the selected ID and opens it paused. Master edits apply to the original recording, so inheriting Blocks follow the shared markers and customized markers keep their local values. Standalone uses the global audio context and carries no practice-time attribution.


## Record Take in Block Transcribe

**Record Take** uses the Focused Practice Free Record owner and microphone AAC/M4A recorder. The Transcribe panel and its Library player remain mounted through capture, temporary review, and Save/Discard. The reference retains its source cursor, pitch-preserving speed, A–B, repeat, and local marker state. Play/Pause, Stop, seek and range controls still operate during capture. Source replacement and workspace switching wait while the take is unresolved. The recording status and mic Stop are pinned outside the scrolling reference.

The Library controller retains output focus while the reference or piano plays. Free Record requests no focus for microphone input and stops only its own take output when it starts. A loss of the reference controller's focus stops concurrent capture and makes valid audio available for review. **Play Preview** pauses the reference and releases held piano notes before the take player requests output focus. Dismissing or stopping preview leaves the reference paused. Wired-output disconnection uses Android's noisy-route broadcast. The recorder also checks for connected audio device changes during T3D capture. Android selects the actual input and output routes.

Backgrounding releases reference playback and finalizes capture under the existing rules. Rotation uses the same foreground interruption path and keeps a valid review in the retained ViewModel. Process-loss cleanup removes unfinished temporary recordings. Save writes one existing Block-owned take and leaves the Library original and annotations intact. The existing session placement owns practice time and Goal attribution. Room 16 and JSON 7 keep their formats. Headphones reduce acoustic bleed. The phone speaker can be picked up by the microphone.
