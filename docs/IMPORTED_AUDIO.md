# Imported backing audio — bounded addition

## Contract

Focused Practice → Loop Setup → Add Backing Track switches creation mode only. Existing musical values stay retained but disabled. Choose file validates a private temporary copy; filename and duration appear; explicit Save alone inserts a take and selects it after the takes flow includes that ID. Import never starts playback. Cancel/failure preserves saved recordings and the prior active selection; failed Save retains the staged candidate for retry. Unchecking restores Record Loop. Temporary imports are removed when leaving that Block or after process loss; saved ones survive restart.

## Supported files and limits

- MP3; AAC inside M4A; ordinary RIFF PCM16 WAV, mono/stereo at8–96kHz. No Ogg, FLAC, standalone AAC, ALAC, float/24-bit/RF64 WAV, protected or multi-track audio.
- Filenames must end in .mp3, .m4a or .wav (case-insensitive); contents are validated independently of the suffix.
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

## Verification and delivery

Signed0.17.0/build62/source10c3a41; artifact paths/hashes and exact phone next task are authoritative in [CURRENT_STATE](../CURRENT_STATE.md).17 scoped unit tests passed on5a39c64; validation/storage logic remains unchanged. Two imported native scenarios and existing cancellation/background loop case passed on945f699; the final remaining existing compact/tall/mic-loop/free-review case passed isolated on10c3a41 after moving the measurement marker to its shared row, keeping every assertion/tolerance. No single full final-suite pass is claimed. Screenshots inspected; no full regression/lint/API matrix. Physical joins/speakers/vendor codecs/long files/phone acceptance remain pending.

Earlier failures were bounded and recorded: missing test fixture tool before build,450ms position observation despite repeated output, an uncomposed lazy row lookup, and text-versus-row spacing measurement. One known stale-selector run was cancelled as superseded. See WORK_LOG for exact runs/results. Source histories after the unit run add only display/controller/selection handling to AudioImport's core file validation/storage paths.

## Commits

|Commit|Change|
|---|---|
|c058949|Private import/validation, explicit identity, UI/controller and scoped tests|
|5a39c64|Bounded fixture setup and preserved review geometry;17 unit pass source|
|4a140ab|Duration display, save/playback handoff and support documentation|
|74704bb|Realistic repeat observation and native-only scope|
|13f3a4d|Compact import form and explicit lazy management scrolling|
|945f699|Filename button semantics; all imported scenarios pass source|
|10c3a41|Duration group measurement/version62; signed source and remaining native pass|

## Changed files

- [.github/workflows/audio-import.yml](../.github/workflows/audio-import.yml)
- [AGENTS.md](../AGENTS.md)
- [BUILD_PLAN.md](../BUILD_PLAN.md)
- [CURRENT_STATE.md](../CURRENT_STATE.md)
- [FIRST_WORK_HANDOFF.md](../FIRST_WORK_HANDOFF.md)
- [README.md](../README.md)
- [WORK_LOG.md](../WORK_LOG.md)
- [app/build.gradle.kts](../app/build.gradle.kts)
- [app/src/androidTest/java/com/mrayover/practicelogger/ImportedAudioWorkflowTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/ImportedAudioWorkflowTest.kt)
- [app/src/main/java/com/mrayover/practicelogger/AudioImport.kt](../app/src/main/java/com/mrayover/practicelogger/AudioImport.kt)
- [app/src/main/java/com/mrayover/practicelogger/PracticePanels.kt](../app/src/main/java/com/mrayover/practicelogger/PracticePanels.kt)
- [app/src/main/java/com/mrayover/practicelogger/PracticeScreens.kt](../app/src/main/java/com/mrayover/practicelogger/PracticeScreens.kt)
- [app/src/main/java/com/mrayover/practicelogger/RecordingData.kt](../app/src/main/java/com/mrayover/practicelogger/RecordingData.kt)
- [app/src/main/java/com/mrayover/practicelogger/RecordingSheet.kt](../app/src/main/java/com/mrayover/practicelogger/RecordingSheet.kt)
- [app/src/main/java/com/mrayover/practicelogger/RecordingViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/RecordingViewModel.kt)
- [app/src/test/java/com/mrayover/practicelogger/AudioImportTest.kt](../app/src/test/java/com/mrayover/practicelogger/AudioImportTest.kt)
- [docs/IMPORTED_AUDIO.md](../docs/IMPORTED_AUDIO.md)
- [docs/P4_TARGETED_UI_DELIVERY.md](../docs/P4_TARGETED_UI_DELIVERY.md)
- [scripts/Check-AudioImport.sh](../scripts/Check-AudioImport.sh)
