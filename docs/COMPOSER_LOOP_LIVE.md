# Loop live note updates and one-bar fit

This bounded increment follows C5B and stops before C5C. Project v1–v4, storage, instruments, mixer and Arrange remain unchanged.

## Live notes

Committed musical document edits (including Undo, velocity and empty patterns) produce validated immutable event lists away from rendering. A generation-checked command supplies them to the existing audio worker. Each track owns one pending replacement, coalesced to its latest notes. Track ID, selected pattern ID, length, instrument, meter and tempo must match the running snapshot. Structural incompatibility clears pending replacements and retains the existing safe snapshot; explicit pattern selection retains its restart behavior. Stop, interruption and switching playback discard the old clock and pending state.

The shared clock includes pending pattern boundaries in its next wake-up, including silent patterns. Each track activates its replacement at its own next cycle, independently of the global region. At a frame shared by events and updates, all old note-offs run before replacements and new note-ons. Replacement releases obsolete tokens and installs preallocated voice slots without changing the clock, output, unaffected channels or mixer. No event generation, sorting, allocation or locking is introduced inside dispatch/rendering.

Live updates require Loop Repeat. With Repeat disabled, edits may wait until the next Play. Arrange keeps its coherent song snapshot; live arrangement replacement is excluded. Audio buffers already submitted to Android cannot be rewritten; a change arriving after a rendered boundary takes the following cycle.

## Piano roll

Measured canvas width minus the fixed 62dp keyboard and a two-pixel boundary margin spans exactly project.barTicks() at relative zoom 1.0. This follows the actual meter, including 7/8 and unusual valid meters. Every horizontal note/grid/ruler/cursor/playhead/hit/drag/resize calculation shares that scale. Vertical note rows remain 26dp.

Relative zoom and horizontal scroll in musical ticks are saved per pattern for the editor visit and Activity recreation. New patterns start at one-bar fit. Note edits and Loop/Mixer navigation preserve intentional adjustments. Resizing recomputes the scale while retaining relative zoom and clamping visible scroll. Existing buttons and pinch remain; bounds are relative 1/16×–32×. Pinch reads current relative zoom for each batched touch move, keeping its musical anchor in tick coordinates. Exact note rectangles own hit testing, removing the old extra 12dp overlap into adjacent short notes; the existing internal right-edge handle stays available.

## Verification

Focused host tests cover independent cycle lengths, boundary ordering, coalescing/Undo, empty/silent patterns, stale identities, structural rejection, finite-snapshot cancellation, validation, measured meters and note geometry. Packaged native PCM and one bounded API35 editing/transport/viewport workflow provide actual scheduling and visual evidence. Actual results and delivery provenance are recorded in CURRENT_STATE.md.

Physical phone acoustics/touch and OneDrive cloud synchronization require separate observation. No deferred C4E campaign, comprehensive regression or F1 is authorized here.
