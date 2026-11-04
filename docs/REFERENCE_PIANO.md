# Reference Piano — K1 architecture

Home → Tools → Reference Piano and Block → Transcribe → Piano use the same keyboard. The initial range starts at C4 and shows 17 chromatic pitches at keyboard widths from 300 dp, 18 from 600 dp, and 13 below 300 dp. Drag the arrow above the keys left to raise the range or right to lower it, one semitone at a time. The handle also provides accessible semitone actions. The entire range stays within MIDI 24–96 (C1–C7). Each octave button moves the starting pitch by exactly 12 semitones and disables when a full octave would exceed the limit. Standalone keys are 184 dp tall, and the Transcribe panel supplies its own key height. Screen content scrolls at enlarged text and in landscape. C labels include octave numbers, and transient note names and selected key semantics reflect held fingers.

## Reuse contract

- `PianoPcm.kt`: independent equal-temperament synthesis and drawing/hit geometry. Frequency is440 ×2^((note−69)/12). Sixteen token-owned voices include release tails. Multiple fingers may sound the same pitch. A finger moving between keys releases its previous voice before starting the next. At the voice limit, new presses are ignored until a voice finishes; sounding voices are never abruptly stolen.
- `PianoAudioEngine.kt`: one48kHz mono PCM16 AudioTrack, one audio worker, bounded240-frame chunks, native low-latency request and platform minimum buffer. Six-millisecond attack,45ms release, decaying second/third harmonics and sustained fundamental give a simple piano-like reference tone. A smooth tanh bound keeps mixed output below28000/32768. This is synthesized reference sound, not acoustic piano sampling.
- `PianoKeyboard.kt`: reusable Compose component accepting note-on(token,note), note-off(token), stop-all callbacks plus enabled/reset signals. Native MotionEvent pointer IDs, black-key-first hit testing, glissando, multitouch and ACTION_CANCEL all use the same geometry as rendering. It disallows parent scroll interception while touching keys. Register changes, re-layout, disable/reset and disposal release notes. TalkBack key actions audition250ms through the same callbacks.
- `PianoScreen.kt`: small standalone wrapper owns engine creation/disposal, foreground lifecycle, interruption reset and Back. ToolScreens and ToolsViewModel retain their existing metronome/tuner/timer implementations.

T1 places the existing keyboard directly in the practice transport. Expanding Piano creates the embedded engine for the same LibraryAudioController and uses an incremented reset token to clear key feedback after interruptions. Collapsing Piano closes its output.

## Focus ownership and interruptions

The standalone engine owns its media focus request. The embedded engine acquires an output grant from the reference controller, which holds one media focus request while either output is active. Paused-reference audition uses the same grant, and playback can resume while notes are held. Closing the keyboard releases its grant. The controller releases focus when both outputs become idle. Focus loss, route changes, source changes and backgrounding stop both outputs. A subsequent gain never starts either output automatically.

## Persistence and verification

Register state is UI-saveable through activity recreation only; no new preference, Room migration, media file or session exists. Room14/JSON7, canonical originals, Block links, markers and take ownership are unchanged.

Scoped unit/native results, exact build source and remaining physical acoustic/latency/route/vendor acceptance are recorded in [CURRENT_STATE](../CURRENT_STATE.md). Generated PCM/pitch, real AudioTrack writes and concurrent reference progression are engineering evidence; they do not certify acoustic audibility on the owner's phone. No P5/F1/F2 matrix runs for K1.
