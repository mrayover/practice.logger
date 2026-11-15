# compose.logger Phase 1 — approved V1 contract

Owner-approved November 14, 2026. This is the authoritative forward specification for an offline MIDI composition workstation inside practice.logger, with eventual extraction into an independent Android application named **compose.logger**. Approval of this contract does not authorize every implementation phase. C1/C2 are delivered. The November15 owner handoff separately assigns C2 scrolling/Hand/Actions corrections plus C3 instruments and Loop playback. The November15 C4A handoff separately assigns the dedicated instrument editor, sound controls, custom presets and reused keyboard. C4B–C7 require separate assignments. [Current implementation and evidence](../CURRENT_STATE.md).

## Identity, navigation and ownership

Enter through practice.logger → Tools → compose.logger. A portrait composition library offers Create New Composition above saved compositions. Creating/opening shows a black surface with centered compose.logger branding before a separate immersive dark landscape editor. Save, Export, Undo and Back belong inside Composer as their implementing phases arrive; do not imply unavailable functionality. Back returns to the library with unsaved-change protection. Orientation requests must not change MainActivity; provide a usable fallback where Android overrides orientation restrictions.

Keep composition documents, musical events, arrangement state, playback and rendering independently owned and extractable. Do not couple them to Blocks, Goals, sessions, statistics, learning guidance, recording transport or practice timers. Initially retain the existing APK/application ID/permanent certificate. No second production application, elaborate plugin framework or premature module restructuring.

## Loop mode

Up to eight instrument tracks, selected through a horizontally scrolling instrument selector. Each instrument owns an independent piano roll and multiple named patterns (A, B, C, etc.), with Add Pattern and lengths of 1, 2, 4, 8 or 16 bars.

Touch inserts a note with the selected default duration. Tap selects and auditions; dragging the body changes pitch/timing, dragging its right edge changes duration, vertical dragging auditions new pitches, and double-tap deletes. Draw and Select tools, rectangle selection, Select All, Copy, Paste and Undo are required. Clipboard works across patterns and instruments, preserving relative pitches, timing, velocity and duration. The grid pans horizontally/vertically and zooms. A touch keyboard supports polyphonic audition; drums use an appropriate percussion-note layout.

Grid divisions include quarters, eighths, sixteenths and thirty-seconds with triplets. Snap is independently toggleable; disabling it permits free timing/duration. Use **960 ticks per quarter note** as canonical internal resolution, independent of the visible grid. The ruler/transport provides a movable playhead and loop-length controls.

## Arrange mode

Loop and Arrange share instruments, patterns and notes. Arrange is a horizontal multitrack song timeline. Only instruments with placed pattern clips have visible lanes. Add Pattern offers instruments with populated patterns; first placement reveals its lane and removing its last clip hides the lane without deleting the instrument/patterns. Clips support place, move, copy, repeat and delete. Selecting a clip allows editing its underlying pattern; shared edits propagate to all instances. Make Unique creates an independent variation. Song playback follows the arrangement; Loop playback follows the selected loop.

## Instruments and audio

Initial targets: acoustic piano, FM electric piano, organ, electric bass, synth bass, synth lead, synth pad, synth strings and electronic drums. Acoustic upright bass is optional if a satisfactory redistributable source exists; it must not delay the core feature.

TinySoundFont with lightweight SF2 banks is a **candidate**, requiring engine validation and redistribution-license review before assets ship. Eventually support interactive audition, simultaneous tracks, accurate loop/song scheduling and offline WAV rendering. Use a versioned instrument registry with stable instrument/preset references suitable for future optional expansion packs. No pack downloads, marketplace or VST hosting in V1.

## Documents, exports and integration

The editable native project is the source of truth: stable project, track, pattern, note and clip identities; instruments/presets; tempo/meter/timing resolution; pitch/start/duration/velocity; independent pattern lengths/collections; and arrangement clips referencing patterns. Titles are display data, never filesystem identities. C1 uses valid empty projects, versioned app-private files and atomic saving, with validation, safe future-version refusal, interrupted-write recovery and lifecycle restoration. Do not add Composer tables to the practice database or a generalized media repository.

Required V1 exports: stereo WAV for loop/selected range/arrangement; multitrack Standard MIDI File Type 1; MusicXML with pitched parts, rhythmic values, rests, chords, required polyphony and percussion handling; and native editable projects. MIDI/MusicXML must support continued editing in Dorico/DAWs; they do not embed instrument samples.

The sole required initial integration is explicit **Save Rendered WAV to Library** using the existing canonical media system. No automatic practice/Goal/Block/session integration. Complete portable recovery of editable projects is required before V1 acceptance. C7 owns that integration; until implemented, existing practice ZIP/JSON backups must not be described as including Composer projects. Development builds must clearly disclose local-only Composer recovery limitations.

## Bounded implementation sequence

| Phase | Assigned scope |
| --- | --- |
| C1 | Composition library/launcher; black branded transition; separate landscape Activity and Loop/Arrange shell; versioned project model; create/open/rename/confirmed delete/save/reopen and lifecycle foundation. |
| C2 | Piano-roll editing, note gestures, grids/snap, selection, cross-pattern clipboard and undo. |
| C3 | SoundFont engine, instrument selection, keyboard audition and loop playback. |
| C4A | Dedicated instrument editor, actual sound shaping, immutable custom presets and preview keyboard. |
| C4B | Expanded pattern slots, pattern lengths, duplication and remaining instrument management. |
| C5 | Arrange timeline, clip placement/duplication, Make Unique and song playback. |
| C6 | Offline WAV rendering; MIDI, MusicXML and native project export. |
| C7 | Explicit rendered WAV import into Media Library and complete Composer portable recovery. |

Proven technical dependencies may justify sequencing changes, never silent assignment expansion. C1 deliberately excludes piano-roll editing, synthesis, playback, arrangement editing, exports and Library/recording interactions. Undo remains inactive until real editing exists. Reserve workspace/transport space without presenting inactive musical features as operational.

## Verification and release policy

Use focused assigned-feature and critical-data-safety checks, compile changed applications and use existing signed-delivery procedures. Preserve production data, all existing tests/infrastructure, recordings/media, identity and signing key. No comprehensive regression, historical restore campaign, device matrix or V1 certification during ordinary development; those await an explicitly authorized F1 gate.

C1 specifically checks document operations, serialization/IDs, atomic/rejected saves, reopen/settings, Activity recreation/process recovery, enter/exit and one relevant landscape emulator configuration. Review rendered library, branded-transition and editor screenshots. Deliver the signed development APK/ZIP with exact source/remote provenance, actual results/limitations and owner-phone instructions. Never require uninstalling/clearing the production installation.

## November15 C2 assignment clarification

The owner explicitly includes the three-second displayed entrance, Reference Piano keyboard/note audition, minimal cross-instrument A/B/C selection/creation,1/2/4/8/16-bar lengths and cursor controls in C2 so cross-pattern editing is usable. These bounded prerequisites do not authorize the C3 SoundFont/sequencer or full C4 management. [C2 behavior and ownership](COMPOSER_C2.md).

## November15 C3 assignment

The owner explicitly assigns TinySoundFont/curated redistributable instruments, real selected-track audition, synchronized multitrack Loop playback, basic mixer/picker and preceding C2 keyboard scrolling/Hand/dropdown repairs. [C3 behavior/provenance](COMPOSER_C3.md). No full C4 management, Arrange playback, exports, pack installation or Library integration.

## November15 compact UI and Save As assignment

The owner separately assigns measured four-row landscape consolidation, responsive explicit overflow, compact status and independent live-draft Save As after C3. Disabled Edit Instrument/Velocity reserve positions; C4–C7/F1 are not authorized. [Behavior and ownership](COMPOSER_COMPACT_UI.md).

## C4A assignment — November15

The owner assigns a dedicated landscape Edit Instrument workspace with factory/My Presets selector, saved custom UUIDs, numeric rotary Attack/Release/Cutoff/Resonance/Sustain/Pan controls and the existing Reference Piano input routed through Composer SoundFont audio. Preserve four-row piano roll, independent tracks, strict compatible project reading, atomic private presets and the existing audio owner. Deliver focused PCM/UI/persistence checks and a permanent-key APK/ZIP. [C4A behavior and limits](COMPOSER_C4A.md). Stop before C4B–C7/F1. Composer projects and presets remain outside practice ZIP/JSON recovery.
