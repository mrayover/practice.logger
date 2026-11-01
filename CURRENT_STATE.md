# Smooth theme — build63 verification in progress

Owner-assigned Smooth teal palette is implemented through the existing theme system. Focused build/tests and signed delivery are pending; previous verified delivery follows.

# Current state — imported backing audio /0.17.0/build62

## Delivery

Signed **0.17.0/build62**, source `10c3a41b0ef6f186b4bea49810c0498b8c125f12`. Later closeout commits change records only. [APK](dist/practice.logger-0.17.0-b62-10c3a41/practice.logger-0.17.0-b62-10c3a41.apk) · [ZIP](dist/practice.logger-0.17.0-b62-10c3a41.zip) · [Build info](dist/practice.logger-0.17.0-b62-10c3a41/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.17.0-b62-10c3a41/PHONE_CHECKLIST.txt). Install over the existing permanent-key app without uninstalling/clearing data; previous packages, including candidate61, are preserved and ignored.

APK SHA256 `4f7b8475ae885fbfcb420d351b5815c00bc96583b57ee5aba13eb6c6076c8d3f`; ZIP SHA256 `ac0c51ab7b9fbcf9e0cd32ae61e4b09a2fb7cbece1763067fca139b2b20e8984`. Publisher verifies stable app ID/version/signature/pinned certificate; signed non-signature entries equal the compiled CI release, and exact ZIP file set/CRC/member bytes match the distribution folder.

## Behavior and limits

Focused Practice → Loop Setup → Add Backing Track switches creation mode only. Unchecked retains Record Loop; checked disables musical fields/shortcuts and shows Choose file/validated filename-duration/explicit Save. Original label ellipsizes in compact controls but appears fully in management. Save adds to the existing collection/picker and explicitly selects that saved ID after the takes flow contains it; never autoplays. Cancellation/failure preserves saved takes and active selection; failed Save is retryable. Temporary import is discarded on Block leave/process loss, saved audio persists.

MP3, AAC-in-M4A and ordinary PCM16 RIFF WAV, mono/stereo8–96kHz;50MiB/15minutes,51MiB free staging space, filename1–255 characters. Chunked copy and bounded full codec validation/WAV structure checks; long imports never enter PhraseWave's in-memory PCM path. Explicit IMPORTED_LOOP mode has null BPM/beats and uses existing MediaPlayer file repeat/focus/foreground owner. Existing recorded loops retain their exact PhraseSpec/PhraseWave/AudioTrack path. Ordinary file repeat is **not certified seamless**; physical joins/codec padding/routing still need phone listening.

Room11/JSON7 unchanged. JSON still excludes all recordings/labels/audio; keep originals and per-take exports. No Media Library, media backup, practice timing/Notes/reference/metronome-engine changes. Earlier owner-accepted UI remains the baseline.

## Scoped verification

- [CI41267911943/source5a39c64](https://github.com/mrayover/practice.logger/actions/runs/41267911943): compilation and17 unit tests pass (AudioImport4, RecordingRepository4, PhraseRepository2, PhraseAudio5, StandaloneRecording2). Parser/repository persistence/limits/rollback/cancellation logic is unchanged in final source. Native run failed its450ms position-poll observation, though logs show repeated output; no overall pass claimed.
- [CI41269123574/source945f699](https://github.com/mrayover/practice.logger/actions/runs/41269123574): final import UI/controller/native decoder behavior passes both imported scenarios and the existing cancelled-loop/background scenario. Covers real picker/cancel, invalid audio/selection preservation, all three formats, explicit selection/no autoplay, two real WAV position wraps/Stop/focus loss/Home-return, force-stop/reopen, full filename, rename/export-byte-equality/delete and earlier takes. Overall run failed only the existing tall-spacing assertion because it measured text inside the new shared row.
- [Final CI41269534069/source10c3a41](https://github.com/mrayover/practice.logger/actions/runs/41269534069): debug/release/test compilation and the isolated remaining existing compact/tall loop scenario pass. All assertions/tolerances retained; marker now measures the duration row. Covers no-scroll/field/footer geometry, range/count-in/guide/exact WAV, auto/manual temporary preview, Keep/Re-record/Discard and free capture/review preservation. Since945f699 only this marker and version changed in app source; imported/native checks were not needlessly repeated.

Four native scenarios collectively pass across the stated sources, not a single comprehensive final run. Screenshots inspected. Prior missing-ffmpeg setup, position-observation, lazy-list selector and cancelled superseded run are recorded in WORK_LOG. No full regression/lint/API26/enlarged-text matrix for this increment. Physical audio/codec joins, long files/vendor providers and same-phone acceptance remain pending.

## Exact next task

Install build62 over the existing app and perform the included short imported-audio phone checklist. No automatic later phase or full regression.

Design/support/complete changed-file and commit list: [Imported audio](docs/IMPORTED_AUDIO.md).
