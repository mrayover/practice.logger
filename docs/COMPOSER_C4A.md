# compose.logger C4A — instrument editor

C4A adds one full-screen landscape overlay inside the existing Composer Activity. The piano roll stays mounted, retaining its selected track/pattern, scroll, zoom and single ComposerAudio owner. Opening the editor deliberately stops Loop playback. Back and preset changes protect unsaved sound changes. Use on Track applies only a saved factory/custom selection to the current draft; composition Save remains the durability boundary. There is no second entrance or practice navigation destination.

## Sounds and controls

Nine untouched factory sounds are grouped separately from My Presets. Factory sounds bypass custom overrides and retain their original per-zone envelopes/filtering/pan. Their dials show explicitly labeled editor starting defaults, rather than claiming that a multi-zone SF2 has one envelope. Turning a dial enables the complete explicit custom sound. Save New Preset can also create a custom sound from those defaults. Each save creates an immutable UUID; duplicate display names are allowed. Existing custom selections restore exact numeric values. There is no shared-preset overwrite, rename or deletion.

- Attack: 0–3000 ms, linear amplitude rise, applied on the next note.
- Release: 10–5000 ms, TinySoundFont amplitude release after note-off; samples can end sooner.
- Cutoff: 40–18000 Hz, logarithmic knob, actual per-voice two-pole low-pass filter.
- Resonance: Q 0.5–4.0, bounded filter resonance.
- Sustain: 0–100% held amplitude after attack/decay, never MIDI pedal sustain. The fixed SF2 decay parameter is 300 ms toward near-zero; TSF enters sustain sooner when it reaches the target. Sample natural decay/length still applies.
- Pan: -1 (left) to +1 (right), equal-power stereo placement; center is 0.

Vertical knob dragging spans 160dp for the full range. Tapping its numeric value opens bounded precise entry and Default; accessibility supports set-progress, precise entry and restore-default actions. Parameters affect the preview immediately, except attack which starts with the next note and release which determines the next release transition. No Apply is needed to hear the keyboard. Track Volume remains in the existing mixer and scales both preview and Loop channels. Custom filtering replaces factory filter modulation; the original sample, tuning and other bank behavior remain. No extra effects or sample editor are provided. Extreme resonance can increase level; existing final PCM limiting remains.

The reused Reference Piano component (with an optional compact layout; other callers keep their existing defaults) supplies MIDI labels, chords, glissando, cancellation, octave buttons, semitone range drag and accessibility. Its callbacks use the selected Composer SF2 bank and edited settings, never the Reference Piano tone. Range is the existing C1–C7 bounded component behavior. Drums retain the C3 mapped19-key subset: shift down to C2 for kick/snare; one-shot samples naturally end even with a long envelope. Focus loss/background clears audio and resets held-key input; closing, preset/range switching and recreation release audition voices. Keyboard range/drafts survive ordinary Activity recreation. A new explicit editor visit starts from the track's applied sound; closing without Use on Track leaves the track unchanged even if a new preset was saved.

## Native ownership

Pinned MIT TinySoundFont v0.9 is unchanged. Its public API supplies volume/pan but not the requested full envelope/filter controls. The bounded extension lives in composer_soundfont.cpp: 16 channel sound snapshots, 9 x 128 private voice-region copies (matching the existing fixed maximum voices), direct envelope setup and the existing TSF biquad. There is no render-time allocation. At note-on a voice copies its source region into its private bank/voice slot; shared preset regions and asset bytes are never changed. Active custom voices receive filter/Q/pan/sustain/next-release updates from the one audio worker. Factory-to-custom switching clears previous preview voices before the next note. Factory selection bypasses overrides. Existing token-based independent release and channel-range clear remain.

LoopPlan embeds each track's sound snapshot; Play configures channels 0–7 before dispatch. Preview uses channels 8–15 in the same synth/output/focus owner. Musical edits and preset application take effect in the sequencer on next Play, preserving 960 PPQ, notes/velocities/identities, pattern selection, mixer, mute/solo and transport behavior. Same-bank tracks have independent parameters.

## Persistence and compatibility

files/composer/presets/<UUID>.json is independent version-1 private preset storage: exact fields version/id/title/instrumentId/sound, bounded UTF-8 document, UUID/name/base-bank validation, numeric finite bounds, synced temporary write and same-directory atomic move. Failed precommit writes remove only their temporary file. Invalid committed files are counted and preserved. Library reads and writes run on IO with a process-wide lock.

Composer project v1 continues to read/write unchanged, including reference-piano/default and all C3 references. Explicit v2 adds mandatory sound and presetTitle fields (nullable for factory references) to every instrument reference. A custom reference records the stable base instrument ID, UUID, readable saved title and a versioned complete parameter snapshot. Applying a custom sound advances only the active draft to v2. It never rewrites other projects. Unknown fields/versions and invalid parameters remain rejected. Playback/recovery/Save As do not require a separate preset file to reconstruct the sound. Older app builds cannot open v2; install updates without downgrading.

Practice ZIP/JSON backups still exclude Composer projects **and presets**. Complete recovery remains later C7/export work; this increment does not change that backup system.

## Layout and verification boundary

Normal short landscape uses40dp header controls and retains all six values above the keyboard. The full-screen Dialog follows Composer immersive system-bar behavior. At enlarged text or orientation restrictions, the control body scrolls while80dp keys stay pinned; numeric/naming content scrolls and respects IME insets. Native screenshot tests verify original/unclipped value bounds and physically tap Save while the software keyboard is visible.

Use the focused C4A host/native workflow and actual screenshots recorded in CURRENT_STATE.md. PCM differences prove signal processing, not physical speaker/headphone/Bluetooth sound quality or latency. Owner Galaxy S26 Ultra acceptance remains separate. C4B patterns, C5 Arrange, C6 exports, C7 Library/recovery and F1 remain outside this increment.

C4B adds instrument-type/preset identification in the compact selector and pattern/track management while retaining this editor and immutable custom snapshots unchanged. [C4B](COMPOSER_C4B.md).
