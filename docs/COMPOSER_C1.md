# compose.logger C1 — project foundation

The owner-assigned [compact landscape UI and Save As](COMPOSER_COMPACT_UI.md) supersede earlier scrolling toolbar/header descriptions below. C4, Arrange editing, velocity editing and exports remain deferred.

This records the C1 foundation. [C2](COMPOSER_C2.md) supersedes the inactive editor/Undo/audition and settings-only draft descriptions below while retaining the project codec and storage protections.

[Approved V1 contract and future phases](COMPOSE_LOGGER_V1_SPEC.md) · [Signed delivery, actual verification and next task](../CURRENT_STATE.md)

C1 adds Tools → compose.logger and a portrait composition library. Create an empty document with title, tempo (whole30–300 BPM, default120) and meter (default4/4); open, rename and confirm deletion. Titles have1–120 non-control characters. Stable UUID filenames are independent of titles. A black branded surface precedes a dedicated dark immersive editor. MainActivity's orientation and existing practice/audio owners are unchanged. Composer requests sensor-landscape; Android may override orientation on larger screens, so controls wrap/scroll and a portrait fallback explains rotation/widening. [Android orientation rules](https://developer.android.com/about/versions/16/behavior-changes-16#large-screens).

The editor provides Save, inactive Undo, Back, Project settings and visible Loop/Arrange selectors. Settings change title/tempo/meter and are real editable draft state; Apply does not save. Save persists them. Dirty Back offers Save and return, Discard changes or Keep editing. Mode and settings drafts survive ordinary recreation. No piano roll, audition, instruments UI, music playback, arrangement editing or export is implemented. The workspace and transport text explicitly identify that boundary.

## Storage and format

`com.mrayover.practicelogger.composer` owns the model, codec, store and Activities. Model/store have no practice-table, Goal, Block, session, media or timer references. Storage is private `files/composer/projects/<UUID>.json`, with no Room schema or practice-backup changes.

The strict v1 JSON root has `formatVersion`, `id`, `title`, `createdAt`, `modifiedAt`, `revision`, `tempoBpm`, `numerator`, `denominator`, `ticksPerQuarter`, `tracks` and `clips`. Canonical PPQ is960. Each track has a stable ID/title, versioned stable instrument/preset reference and independent patterns. Patterns have IDs/titles/bar lengths and notes with ID, pitch, start/duration ticks and velocity. Arrangement clips have ID/trackID/patternID/startTick; only clips imply future lanes, so an instrument need not have a lane. New documents contain no tracks, patterns, notes or clips.

Validation rejects future versions, unexpected/missing fields, wrong types, noncanonical/duplicate IDs, bad meter/tempo, invalid note ranges and dangling/cross-track pattern references. Bounds:8 tracks,256 patterns per track,100,000 total notes,10,000 clips and8MiB per file. Future implementation can deliberately version these development bounds. Unknown/corrupt documents are preserved and counted in the library rather than overwritten or silently hidden. No import/export API is exposed in C1.

Writes validate/encode before staging, sync `<UUID>.json.new` bytes, then use same-directory `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)` and read back the result. Failure before commit preserves the previous base; there is no non-atomic delete/move fallback. A shared process lock serializes stores; revisions reject stale writers/deletion. Startup/list/read clean recognized uncommitted staging files and can restore a `.bak`. Cleanup/delete operate only on the selected UUID's recognized files. The application declares no separate Composer process; this is not a cross-process filesystem locking framework. Sudden physical power loss and every manufacturer's storage implementation are not certified by the simulated interruption checks.

Creation saves a stable pending document in saved state and retries idempotently by ID. Once Activity launch is handed off, its request is consumed so restoring the underlying library does not open another editor. Editor identity comes from its Intent; settings-only draft JSON is saved through SavedStateHandle and checked against the committed revision on recovery. Larger musical draft-state handling belongs with future editing rather than storing future full songs in Activity bundles.

## Development recovery limitation

**Existing complete practice ZIP and legacy JSON backups do not contain Composer files.** No portable project export exists in C1. Projects survive ordinary close/reopen and app updates, but remain on this device; uninstall, clear-data or device loss can lose them. The launcher discloses this. C7 must add complete Composer portable recovery before V1 acceptance. Preserve existing production installation/data; never ask the owner to uninstall or clear it for testing.

## Focused verification boundary

Retained Composer storage checks cover operations, stable identity/serialization, sparse arrangement references, invalid/future input, optimistic conflicts, interrupted writes, recovery and unrelated-file preservation. Saved-state checks cover idempotent creation, consumed launch and dirty settings recovery/save. One API35 workflow exercises actual Tools/library/editor Activities, landscape/portrait return, recreation/mode/draft retention, Save/Discard, rename/confirmed deletion and a second instrumentation process after force-stop. C2 replaces the synthetic MainActivity transition rendering with captures of the actual Composer Activity, requiring a black background and centered restrained white branding plus at least three seconds from branded draw to workspace draw. Its focused C1 workflow/cold reopen remain, with separate C2 touch and exact-note cold-reopen checks. Actual outcomes and failed attempts are in CURRENT_STATE/WORK_LOG; this document does not assert phone acceptance or F1 certification.
