# Reference Piano — K1 architecture

Home → Tools → Reference Piano uses the existing navigation and Lato/theme roles. It requires no microphone access. Compact portrait shows C4–C5 inclusive (eight white keys); widths of at least600dp extend through F5. Octave buttons cover C1 through C7 (MIDI24–96), clamping the upper wide view. Keyboard height is184dp; screen content scrolls at enlarged text and in landscape. C labels include octave numbers; transient note names and selected key semantics reflect held fingers.

## Reuse contract

- `PianoPcm.kt`: independent equal-temperament synthesis and drawing/hit geometry. Frequency is440 ×2^((note−69)/12). Sixteen token-owned voices include release tails. Multiple fingers may sound the same pitch. A finger moving between keys releases its previous voice before starting the next. At the voice limit, new presses are ignored until a voice finishes; sounding voices are never abruptly stolen.
- `PianoAudioEngine.kt`: one48kHz mono PCM16 AudioTrack, one audio worker, bounded240-frame chunks, native low-latency request and platform minimum buffer. Six-millisecond attack,45ms release, decaying second/third harmonics and sustained fundamental give a simple piano-like reference tone. A smooth tanh bound keeps mixed output below28000/32768. This is synthesized reference sound, not acoustic piano sampling.
- `PianoKeyboard.kt`: reusable Compose component accepting note-on(token,note), note-off(token), stop-all callbacks plus enabled/reset signals. Native MotionEvent pointer IDs, black-key-first hit testing, glissando, multitouch and ACTION_CANCEL all use the same geometry as rendering. It disallows parent scroll interception while touching keys. Register changes, re-layout, disable/reset and disposal release notes. TalkBack key actions audition250ms through the same callbacks.
- `PianoScreen.kt`: small standalone wrapper owns engine creation/disposal, foreground lifecycle, interruption reset and Back. ToolScreens and ToolsViewModel retain their existing metronome/tuner/timer implementations.

T1 should place `PianoKeyboard` directly in its existing expanded transport and create `PianoAudioEngine(context, referenceController)` for the same existing LibraryAudioController. Keep the component/engine only while its panel is expanded. Forward foreground changes; pass an incremented reset token from the interruption callback to clear touch feedback. Do not navigate to the Tools wrapper or create a second instrument. No Transcribe presentation is added in K1.

## Focus ownership and interruptions

With no reference controller, the engine owns a normal media focus request. With a reference controller, the engine never requests or abandons focus and only starts a new note while that reference is playing. This explicit borrowed-focus mode avoids stealing the reference player's focus. For future T1, paused-reference solo audition needs an explicit owner focus grant before starting embedded notes; this is a documented contract limit, not a claim of paused-player support.

The reference controller registers companion interruption callbacks. Pause, stop, close, source replacement, background, decoder errors, permanent completion and platform focus loss stop its companions. Continuing/repeating reference playback retains companions. Removing/closing a companion touches only its own PCM/AudioTrack and registration; it never pauses/disposes/seeks the reference controller. Route-change broadcasts also stop note output. Reopening or gaining focus never resumes old voices. Standalone output is released after one second of silence; screen/background cleanup releases immediately. Renderer synchronization serializes note commands and buffer generation; output errors return a retry message on the main thread.

## Persistence and verification

Register state is UI-saveable through activity recreation only; no new preference, Room migration, media file or session exists. Room14/JSON7, canonical originals, Block links, markers and take ownership are unchanged.

Scoped unit/native results, exact build source and remaining physical acoustic/latency/route/vendor acceptance are recorded in [CURRENT_STATE](../CURRENT_STATE.md). Generated PCM/pitch, real AudioTrack writes and concurrent reference progression are engineering evidence; they do not certify acoustic audibility on the owner's phone. No P5/F1/F2 matrix runs for K1.
