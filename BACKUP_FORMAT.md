# Backup format and safety

Format identifier: `practice.logger.backup`. Format version: integer `1`. UTF-8 JSON. Independent of Room schema version (currently 2). The app uses Android's CreateDocument/OpenDocument picker; it requests no broad storage permission. Android owns the picker typography.

Top-level fields: `format`, `version`, `blocks`, `sessions`, `placements`. All record fields are required, including explicit JSON null for nullable values. Unknown extra fields are ignored; unknown format versions are rejected. IDs are preserved.

- Blocks: id, name, action, durationMinutes, tempo (nullable), notes, archived, createdAt, updatedAt.
- Sessions: id, startedAt, endedAt (nullable), status, currentIndex, note.
- Placements: id, sessionId, sourceBlockId, name, action, tempo (nullable), referenceNotes, position, configuredMs, activeMs, outcome, overtimeAccepted, note.

Export reads a consistent Room transaction. It includes archived Blocks, completed history snapshots, notes and the unfinished draft or active session. No monotonic clock anchor/running flag is exported; restored active sessions resume paused. Quick notes not yet saved with Complete/Skip are not part of the backup. Session notes require Save note before export.

Limits: 8 MiB UTF-8 file, 20,000 records per type, nesting at most 32 levels. Validation rejects missing/wrong field types, out-of-range integers, duplicate/blank IDs, broken links, nonpositive duration/tempo, negative elapsed time, noncontiguous placement order, unsupported statuses/outcomes, inconsistent current position/outcomes, multiple unfinished sessions and overflowing active-time totals. Malformed UTF-8 and trailing content are rejected. This is structural validation, not encryption or cryptographic authentication: keep backups private and restore only trusted files.

Restore first parses/validates the complete file and shows counts for confirmation. The repository validates again, checks that both Blocks and sessions are empty, then inserts everything in one database transaction. Any error rolls back all inserts. Restore never clears, replaces or merges data. It supports a fresh app/new phone; it intentionally offers no reset button. **Do not clear/uninstall the current app just to test restore.** Use a spare device or disposable test installation.

Cancelling the picker or confirmation does not change the database. Rotation retains an in-memory restore preview and operation state. Process death loses an unconfirmed preview; select the file again. A failed export may leave a partial document, which must not be treated as a completed backup; choose a new export and wait for the success message. Export does not modify database contents. Android CreateDocument preserves an existing same-name document by choosing a distinct name. Provider-specific availability, cancellation, disk-full behavior and large-text accessibility still require owner phone testing.

Tests cover a real JSON file round trip into an empty database and reopening, archived Blocks/Unicode/notes/historical snapshots/drafts/overrides, unfinished overtime recovery, malformed/version/type/size/depth/trailing/relationship/order/state rejection, protected nonempty target, and rollback after an injected mid-restore write failure. Device tests additionally exercise the backup screen, recreation and retained confirmation, real file export and restored database reopen, confirmation cancellation, rejected overwrite and malformed file. The device harness supplies local file URIs directly to the same ViewModel operations; it does not automate Android's system document picker.

This is a bounded Stage 3 increment. Dependable daily-use acceptance still requires phone document-provider export, spare-device restore, interrupted export and accessibility checks. Merge/replace restore, cloud sync and in-app update controls are outside this increment.
