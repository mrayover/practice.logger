# compose.logger C4D — isolated channels, Pan and durable mixing

C4D is limited to closing C4C verification, independent channel processing, track Pan and persistent mixer settings. EQ, width, FX, Arrange and exports remain unimplemented; C4E is a separate assignment. Practice ZIP/JSON still exclude Composer projects and custom presets until C7. Install updates without uninstalling or clearing app data.

## Audio

The native wrapper traverses each bank's bounded voice pool once, invoking TinySoundFont's existing `tsf_voice_render` once per active layer for the requested frame count. Each voice writes to its tagged channel's bounded stereo buffer. No bank is rendered repeatedly for different channels. Vendored tsf.h, note scheduling, pitch, velocity, token release and C4A private regions are unchanged.

Sixteen preallocated 256-frame stereo buffers (32 KiB) retain sequence channels 0–7 and audition channels 8–15 separately. The two sets can have different UUID orders during musical edits. Mixer processing occurs before final summation and the existing PCM limiter. Channel gain retains the existing native TSF channel-volume path. One ComposerAudio worker and one AudioTrack remain; no render-loop allocation or filesystem operation is added.

Mixer Pan is independent of preset Pan. Preset shaping generates the stereo source first; Mixer Pan then moves that source. Center is an identity operation. For p from 0 to +1, theta=p*pi/2: L'=cos(theta)*L, R'=R+sin(theta)*L. For p from -1 to 0, theta=abs(p)*pi/2: L'=L+sin(theta)*R, R'=cos(theta)*R. Each moved source component uses equal-power coefficients; the stationary component is retained. At the extremes both components reach the requested side; centered stereo character is unchanged. Correlated stereo can rise by up to 6 dB when folded to one side; existing bank headroom and final limiting remain. No width parameter or normalization is added. Coefficients are calculated when commands change Pan, outside the per-frame loop.

Master retains the Composer-only AudioTrack volume path, reapplied on output reopening. Pan and Volume changes do not restart the sequencer; pattern choices retain C4C's deliberate restart from zero.

## Project data and history

Explicit project v3 adds a strict `mixer` object to each track (volume 0–1, pan -1–1, mute, solo, nullable selectedPatternId) and project masterVolume 0–1. Mixer ownership is embedded in the stable track UUID object, never a list-position association. Selected IDs must belong to that track. Null selects its first available pattern, or None for an empty track. Nonfinite/out-of-range values, incorrect types and unknown/missing fields are rejected without rewriting the document.

Valid v1/v2 files retain their exact old field sets and open with unity gain/master, centered Pan, unmuted/non-solo defaults and first-pattern fallback. A mixer edit upgrades only the active draft to v3. Custom instrument snapshots continue working in both v2 and v3. Older app versions cannot read v3; do not downgrade after saving.

Save As remaps project, track, pattern, note and clip UUIDs plus every selected-pattern reference. All mixer values are copied, while the original document remains unchanged. Deleting a track removes its own settings; deleting a selected pattern clears the reference to safe fallback. Duplicating a pattern preserves the current selection until explicitly changed. Undo restores the immutable document, including deleted-track settings.

Transient slider values provide live audio. Finishing a gesture commits one validated project edit, Undo entry and atomic draft checkpoint. Mixer commits transform the ViewModel's latest document, so retained strip callbacks cannot overwrite another channel's newer settings. Mute, Solo and pattern choices each commit one logical edit. Save, Return, recreation, cold reopening and Save As reuse the existing project store and atomic-save protections. There are no practice database changes.

## Interface and verification

Vertical strips retain identity, owned pattern, M/S, compact Pan with L/C/R numerical readout and vertical Volume. Combining M/S into one row makes room for Pan without reducing the fader height. Pan explicitly owns its horizontal gesture, with a small center detent; surrounding channel strips scroll independently. Only channel strips scroll horizontally; Master and the app header remain pinned. A quiet “EQ later” label reserves the details location without enabling a nonexistent control.

Focused checks cover strict v1/v2/v3 storage, Save As/deletion ownership, failed atomic saves, actual native PCM separation/once-only advancement, stereo placement, eight-track rendering cost and one bounded Android persistence/playback/recreation/cold-reopen workflow. Actual outcomes, failures, timing and signed delivery are recorded in [CURRENT_STATE](../CURRENT_STATE.md) and [WORK_LOG](../WORK_LOG.md). Physical sound quality, routing and touch acceptance require the owner's phone.
Delivered signed0.53.0/build145, sourcefa6b562. Focused coverage is completed across targeted host/native/UI/touch/tail/cold runs; exact failures, source identities and results are in CURRENT_STATE. Eight-track32-note native rendering mean0.050739ms/p990.091506ms per5ms block. Actual screenshots inspected. The owner explicitly skips personal checklists; no checklist is required. Physical phone observations remain unreported. No broad regression/F1.
