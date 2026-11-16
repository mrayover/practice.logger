**Historical C5A foundation. C5B supersedes lane visibility,labels,placement and transport; see [current Arrange behavior](COMPOSER_C5B.md).**

# compose.logger C5A — Arrange timeline foundation

Arrange is a dedicated bounded canvas within the existing mounted Composer workspace. Loop, Arrange and Mixer share the document and one audio owner. Entering Arrange stops Loop; arrangement playback belongs to C5B. The application header, Save, Save As, Settings and Return retain existing paths. Loop editor viewport and selected patterns remain mounted across mode switches.

Select a bar on the ruler or empty timeline, then Add Pattern. Choose an instrument and one of its populated patterns. Placement references existing track/pattern UUIDs with a fresh clip UUID. Repeated placement shares notes. Lanes appear only for tracks with clips, follow project track order and disappear after the last instance is deleted. Labels use C4B instrument identity and descriptive track name. Clips show the live pattern title and its lengthBars duration; there is no separate clip length.

Select mode owns clip dragging; Hand mode owns horizontal/vertical navigation. Ruler, grid and clips use one viewport transform; instrument headers stay fixed. A drag previews the whole-bar destination (red for a collision) and commits once on release. Cancellation discards the preview. Movement keeps clip identity, owner, pattern and length. New placements/moves reject same-track overlaps, negative starts and positions beyond the existing codec document bound (3,840,000,000 ticks). Different tracks may overlap. Timeline drawing is bounded by the visible viewport, with no low song-length cap.

Delete Clip removes only the selected instance. Add, completed Move and Delete each use the existing immutable project edit/Undo/checkpoint path. Existing atomic storage and Save As remapping persist clips without a new format. Supported v1–v4 readers retain legacy behavior, including existing overlaps. Pattern-length edits (including note-driven automatic growth) refuse newly introduced arrangement collisions; legacy overlaps remain readable. Renaming a pattern updates every instance automatically.

No song playback, repeat/copy commands, Make Unique, clip resizing, cross-track dragging, underlying-pattern editor, export, new audio processing or practice data changes. Composer projects/custom presets remain outside practice ZIP/JSON recovery until C7.

Verification and signed delivery: [CURRENT_STATE](../CURRENT_STATE.md). Physical phone acceptance remains separate from automated checks.

Signed build147 and focused verification are complete. Five host checks and one bounded API35 editing/save/recreation workflow plus separate exact cold reopen pass in [run43795631203](https://github.com/mrayover/practice.logger/actions/runs/43795631203). Actual normal/narrow screenshots inspected. Save As remapping is host-tested. Physical phone acceptance remains pending. Arrange viewport/selection are visit-local; long lane names clip in their fixed header. See CURRENT_STATE for exact provenance, hashes and delivery. Stop before C5B.
