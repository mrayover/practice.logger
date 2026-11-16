# compose.logger C5B — Song playback and compact Arrange

Arrange shows every composition track in project order, including empty lanes. It uses the assigned instrument identity and meaningful descriptive names, suppressing legacy Piano N / Track N display hints without rewriting saved titles. Lanes are 52dp tall. Hand mode scrolls horizontally and vertically with fixed instrument headers.

In Select mode, complete a tap on empty lane space to choose one of that instrument's populated patterns at the tapped whole bar. Ruler taps select a bar; clip taps select; clip drags preview and commit a snapped same-lane move. Cancel makes no edit. The existing placement/collision validation, immutable Undo and atomic persistence remain authoritative.

Play starts an immutable song snapshot from Bar1. Notes are scheduled at clip.startTick + note.startTick at960PPQ, preserving duration, pitch and velocity. The latest clip end defines the song endpoint. Empty time remains silent, and instruments never independently loop during song playback. The existing one-worker ComposerAudio / AudioTrack and native synth/mixer own output. Playhead position is polled from consumed audio frames, using the same bar/zoom/scroll transform as clips. Stop clears voices; Beginning restarts safely if playing. The Mixer Repeat control reflects its current owner and repeats the whole song when song owns playback.

Switching Arrange↔Mixer preserves playback and the immutable snapshot. Volume, Pan, Mute, Solo, EQ, Width and Master use the existing mixer path. Mixer pattern choices remain Loop selections and cannot replace song clips. Switching to Loop stops song; Loop Play retains independent pattern-repeat behavior. Structural musical edits stop song; subsequent Play builds a new snapshot. Song expansion is bounded to200,000 note instances with explicit feedback, before allocation.

Actions offers Duplicate Clip (one fresh instance immediately after source), Repeat Clip (2–8 additional consecutive instances) and Beginning. Every copy keeps underlying track/pattern references with a fresh clip UUID. The complete batch is validated before one commit/Undo; any invalid destination rejects all copies.

No new project format: v1–v4, shared pattern edits, draft recovery, Save and Save As retain existing semantics. Composer projects/presets remain outside practice ZIP/JSON until C7. C5C Make Unique and Edit Pattern, exports and unrelated work remain unassigned. Deferred C4E verification is not resumed.

Verification and signed delivery: [CURRENT_STATE](../CURRENT_STATE.md). Physical-device acceptance remains separate.
