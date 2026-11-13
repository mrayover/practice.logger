# Recording waveforms — RW1 and RW2

RW2 extends the existing compact RW1 transport to saved recording collections and completed temporary reviews. Canonical Media Library and Transcribe retain their separate complete Library transport. No capture waveform, audio engine, waveform files, database/archive fields or persistent review position is added. Room27, complete ZIP12 and legacy JSON7 remain unchanged.

## Coverage and reuse

| Environment | Shared component and ownership |
|---|---|
| Focused Practice saved Free/Loop | Existing RecordingScrubber and Free MediaPlayer / recorded-loop AudioTrack / imported-loop MediaPlayer |
| Block recordings and standalone Free recordings | Same BlockRecordings; deliberate Inspect take selection, one shared selected waveform and Play/Stop; row Rename/Export/Delete retained |
| Temporary Free review, including Block Transcribe Record Take | TakeReviewControls with ReviewRecordingScrubber |
| Temporary Loop review | Inline Loop Review with the same ReviewRecordingScrubber; existing completion audition retained |
| Block and standalone Record Reference | Same RecordReferenceDialog with ReviewRecordingScrubber; existing append-and-select / canonical Library Keep retained |
| Media Library, Block and standalone Transcribe | Existing complete transport/controller unchanged |

RecordingScrubber reuses CompactRecordingWaveform: signal envelope, visible cursor, elapsed/total timestamps, tap, horizontal preview and one seek on release, vertical-scroll coexistence and accessible progress/one-second actions. Existing theme/Lato roles apply. A failed decoder or missing/corrupt file shows Waveform unavailable; other takes remain usable. Collection waveforms scroll, while selection ownership stays mounted outside the lazy list; scrolling the waveform offscreen does not stop playback. The collection review waveform scrolls separately from the existing pinned review actions.

## Temporary identity and lifecycle

Only an inspected reviewTake backed by its exact active reviewPending object/file can seek or preview. seekSaved still refuses all review states. seekReview checks exact validated object identity, pending ID/path/Block context, open recording owner, foreground state, no active microphone owner, no capture and no controller work before submitting to the existing MediaPlayer/AudioTrack seek implementation. File existence is rechecked even for stopped seeks. Stopped seeking cues the next deliberate Play; it never starts output.

Review decoding runs in the existing bounded, cancellable decoder and process-only four-entry cache. Discard, Re-record, successful Save/Keep, foreground loss and leaving cancel pending review decoding. Removed/superseded review callbacks cannot seek a new source. Output requests retain the generation guard before engine creation and during MediaPlayer preparation. Free validation now also checks its original generation before publishing review, so leaving/reentering during validation cannot reopen it. Leaving the owner clears temporary review; background/rotation retains valid in-process review paused under existing screen lifecycle rules. Composition never starts capture or playback.

No saved row or rename is needed for waveform analysis. Save still atomically promotes pending audio and inserts its existing take row; failed promotion rolls back to reviewable bytes. Reference Keep retains canonical Library ownership and the existing atomic append/order check. Failed Save/Keep retains the same pending identity and audio. Existing focus loss, noisy-route, capture/count-in, preview audition, export and backup rules remain.

## Focused verification

RecordingWaveformTest covers timeline/frame clamping, actual PCM peaks, cache identity, bounded long envelopes and missing/corrupt files. TakeReviewTest covers temporary inspection/discard, failed commit rollback/retry and complete/incomplete Loop ownership. The existing RecordingWaveformWorkflowTest checks actual M4A/recorded WAV/imported MP3/M4A/WAV transport, repeat, playing/stopped seeks, drag release, switching, fallback and compact practice controls. RecordingCoverageWorkflowTest exercises both real collection sheets at 200% text, real mic Free/Loop/Reference reviews, guarded seeking, failed Save/Keep, byte-preserving promotion, Re-record/Discard, foreground/leave cleanup and pending decoder cancellation. Actual outcomes and package provenance are recorded in [CURRENT_STATE](../CURRENT_STATE.md). Physical audio, TalkBack and owner-phone acceptance remain separate; comprehensive F1 is deferred.
