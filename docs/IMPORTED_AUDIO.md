# Imported backing audio — bounded addition

## Contract

Focused Practice → Loop Setup → Add Backing Track switches creation mode only. Existing musical values stay retained but disabled. Choose file validates a private temporary copy; filename and duration appear; explicit Save alone inserts a take and selects it after the takes flow includes that ID. Import never starts playback. Cancel/failure preserves saved recordings and the prior active selection; failed Save retains the staged candidate for retry. Unchecking restores Record Loop. Temporary imports are removed when leaving that Block or after process loss; saved ones survive restart.

## Supported files and limits

- MP3; AAC inside M4A; ordinary RIFF PCM16 WAV, mono/stereo at8–96kHz. No Ogg, FLAC, standalone AAC, ALAC, float/24-bit/RF64 WAV, protected or multi-track audio.
- Maximum50MiB and15minutes; at least51MiB free for staging. Original filename1–255 characters without control characters; saved as the initial label. Existing explicit rename retains its120-character limit. Compact labels ellipsize; management shows the complete saved label.
-32KiB copy buffer, bounded private UUID filename, WAV chunk/length/alignment validation or full compressed decoder drain to EOS. Native decoder buffers are drained/released rather than collecting a decoded track. Compressed validation has a90-second budget, with cancellation and duration checks between codec calls. A document provider's own blocking operation is platform-dependent; no instant cancellation of a wedged provider is claimed.
- Save reuses file sync/rename and Room transaction rollback, active temporary-file recovery protection and existing deletion/export ownership. There is no new database table/schema migration. File bytes are kept in their original format; no conversion of recorded/free takes.

## Identity and playback

| Existing mode string | Identity | Playback |
|---|---|---|
|FREE|Free recording|Existing MediaPlayer once|
|LOOP|Microphone-recorded phrase|Existing PhraseSpec/canonical48kHz PCM16 WAV/AudioTrack sample loop|
|IMPORTED_LOOP|Imported backing file|Streaming MediaPlayer file repeat; duration/EOS from audio, no BPM/beat/count-in dependency|

TakeKind centralizes category handling. Imported loops have null musical fields; changing creation mode or renaming never changes their stored identity. MP3 exports audio/mpeg/.mp3, M4A audio/mp4/.m4a, WAV audio/wav/.wav. Playback reuses the existing focus request, preparation guards, Stop, noisy-route and foreground cleanup. Saving a new active item stops prior take playback after commit and never starts the import. No automatic restart after returning from background.

Ordinary file repeat is provided, **not certified seamless looping**. Codec delay/padding, file edits and device/framework behavior may produce gaps or clicks at the join. Physical listening is required. Android API references: [MediaPlayer](https://developer.android.com/reference/android/media/MediaPlayer), [MediaExtractor](https://developer.android.com/reference/android/media/MediaExtractor), [MediaCodec](https://developer.android.com/reference/android/media/MediaCodec).

## Boundaries

Room11/JSON7 and old audio remain. JSON backup still excludes every recording, label and audio file; per-take export remains the way to retain external audio copies. This is not the future Media Library or media-inclusive backup. No practice timing, Notes, reference ownership or metronome engine changes.

## Verification

Compilation, scoped repository/limit/failure tests, native three-format/picker/selection/repeat/foreground/focus/cold-reopen/management checks and existing loop/free workflows are pending. Record actual source/run outcomes and signed delivery in CURRENT_STATE.md; do not infer phone acceptance.
