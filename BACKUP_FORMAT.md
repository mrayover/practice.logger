# Backup format and safety — current implementation

Updated for Increment 3 (0.15.0/build 29; delivery status in CURRENT_STATE.md) on 2026-10-29. Actual release verification is recorded in CURRENT_STATE.md.

## Format and records

Identifier: `practice.logger.backup`. Writer version **6**, readers **1–6**. UTF-8 JSON; independent of current Room schema **10**. Top-level fields are `format`, `version`, `blocks`, `sessions`, `placements`, `routines`, `routinePlacements`, `goals`, `goalBlocks`, `sessionGoals`, `logs`. Required fields for the declared version include explicit JSON null for nullable values; unknown extra fields are ignored and unknown versions rejected. Stable IDs are preserved.

| Array | Fields |
|---|---|
| blocks | id, name, action, durationMinutes, tempo(nullable), notes, archived, createdAt, updatedAt |
| sessions | id, startedAt, endedAt(nullable), status, currentIndex, note |
| placements | id, sessionId, sourceBlockId, name, action, tempo(nullable), referenceNotes, position, configuredMs, activeMs, outcome, overtimeAccepted, note, lastUsedBpm(nullable) |
| routines | id, name, createdAt, updatedAt |
| routinePlacements | id, routineId, sourceBlockId, position, configuredMs |
| goals | id, title, description, progress, notes, completed, priority, createdAt, updatedAt |
| goalBlocks | goalId, blockId, position |
| sessionGoals | sessionId, goalId |
| logs | date (canonical local YYYY-MM-DD), text (untrimmed plain text, including empty text) |

| Version | Compatibility |
|---|---|
| 1 | Legacy session rules: at most one unfinished session; no lastUsedBpm/Routines/Goals. |
| 2 | Multiple IN_PROGRESS sessions plus at most one DRAFT. |
| 3 | Nullable placement lastUsedBpm added; old files import it as null. |
| 4 | Routines and routinePlacements required; older files import no Routines. |
| 5 | Goals, goalBlocks and sessionGoals required; older files import no Goals or attribution. Existing history is not retroactively attributed. |

Version **6** requires `logs`, an array of unique date/text pairs with no practice associations. Versions 1–5 import no Logs. Dates must be real Gregorian calendar dates in years 0001–9999, with exact four-digit year and two-digit month/day; invalid dates, duplicates and non-string text are rejected. Weekly collections are derived and never serialized separately.

## Snapshot, validation and restore

Export takes a consistent Room transaction, including all saved daily Logs and saved archived Blocks, Routines, Goals/links, historical snapshots, notes and saved draft/unfinished recovery state. No monotonic clock anchor or running flag is exported. Restored practice resumes paused. Save quick notes via Save Block note/Save for later/Back/Complete/Skip and session notes via Save note before export.

Limits: 8 MiB, 20,000 records per type, nesting 32. Oversize export fails explicitly before opening the destination stream; it never silently drops Log entries. Log navigation flushes autosave before returning Home, so a subsequent export includes those edits. Validation rejects malformed UTF-8/trailing data, unsupported versions, missing/wrong types, duplicate/blank IDs, broken references, invalid order/status/outcome/position, nonpositive durations/tempo, negative elapsed time and overflowing totals. Overtime requires reaching configured time; lastUsedBpm is null or 30–300. Every session placement's sourceBlockId must reference an existing Block even for completed history. Routine names are trimmed 1–120 characters, routines nonempty, entries contiguous with positive durations and valid Block references. Archived links are retained; the app refuses new Routine practice until repaired/restored.

Goals require a nonblank title, manual 0–100% progress and ordered nonnegative timestamps. At most five active Goals have contiguous zero-based priority; completed priority is -1. Goal/Block pairs are unique with contiguous link order and valid references. One Goal attribution is allowed per existing non-draft session; per-Goal time totals cannot overflow.

The UI parses/validates the entire file and shows counts before confirmation. The repository validates again and, in the same transaction, requires an empty target across **Logs, Goals, Blocks, sessions, Routines and all take metadata (including standalone takes)** before inserting everything. Failures roll back all inserts. Restore never clears, replaces or merges data. Use a spare empty installation; never clear/uninstall the main app to test it.

Canceling picker/confirmation changes nothing. Rotation retains an in-memory preview/operation; process death loses an unconfirmed preview, requiring reselection. Export does not mutate database contents. Android owns the system picker and chooses a distinct document name rather than replacing a same-name document. Failed exports may leave partial documents; retry to a new file and wait for success. Plain text provides no encryption/authentication: keep it private and use trusted files. Provider/full-disk/TalkBack behavior still needs phone checks.

## Image/PDF exclusion and private storage

**JSON versions 1–6 include no image/PDF bytes, Block reference metadata, private filenames or picker URIs.** Restoring JSON retains all structured data and Logs but creates no attachment rows or broken media references. Keep every original image and PDF separately, then attach it again to its restored Block. There is no media-inclusive ZIP backup or per-reference export in this increment. Clearing/uninstalling/device loss destroys app-owned copies; Android cloud/device-transfer backup remains excluded.

One `block_images` row per stable Block ID points to a generated UUID `.img` (image) or `.pdf` file under `files/block-images`. Historical table/type names are retained; the existing mimeType and dimension fields support either format, so schema10, all existing rows and originals remain unchanged. There is no new Room migration. For PDFs, width/height record the first page’s dimensions; live page count comes from PdfRenderer. The system picker’s source is read only and never deleted. Import copies at most 20 MiB to a generated pending file, checks decoded JPEG/PNG/WebP content, at most 40 million pixels/16384 per side, and verifies sampled decoding at about 2048 pixels per side. File copy/decode run on IO. The original bytes are retained. Display decoding reads rotation/flip metadata through AndroidX ExifInterface and orients the sampled bitmap; it does not rewrite originals, change attachment rows or require reattaching existing images. The file is synced/renamed before the transactional metadata upsert; old attachment/file remain until that commit succeeds. Failed/canceled imports preserve the old image. Confirmed removal deletes metadata, then the private file. A shared media mutex serializes replacement/removal/recovery; known unreferenced generated files (including interrupted pending files) are retried at startup only after successfully reading metadata. Unknown files and other media directories are untouched.

PDF imports copy at most20MiB, require1–100 pages and supported page dimensions (1–14400 points per side), and open/render every page through Android PdfRenderer using one temporary256px bitmap. Validation and page rendering run on IO. An IO scan conservatively rejects PDF encryption names (including hex-escaped names); this also excludes owner-encrypted PDFs that Android15 can open without an opening password. PDFs containing these reserved encryption markers are unsupported. Invalid, password-protected/unreadable or unsupported documents are rejected before replacement commits. Display opens only the requested page, renders its longest edge to2048px and closes page/renderer/descriptor. Only one page bitmap per visible viewer is retained; page changes/hiding release UI references, and stale results are discarded. No PDF dependency or extra permission is added.

Missing/corrupt references report an inline error with Retry reference; metadata remains until deliberate replacement/removal. Archive preserves image/PDF references. Practice resolves live ownership by sourceBlockId without copying images into historical text snapshots. Permanent deletion rechecks attachment dependencies with all earlier protections in the same transaction; the restrictive foreign key additionally prevents silent loss.

## Audio exclusion and storage

**No JSON version includes recordings, take metadata or labels.** JSON restore creates no missing-media take references. Export desired takes individually: FREE as AAC/M4A; LOOP as one normal WAV phrase. Failed/canceled export preserves the source but can leave a partial destination. No audio-inclusive bulk restore/import is implemented. Clearing/uninstalling/device loss destroys unexported audio/labels; platform backup/device transfer is explicitly excluded.

Private audio uses generated UUID names under `files/recordings`, with validated duration/frame data, file sync and rename before metadata commit. Interrupted/unreferenced generated files are recovered/removed after metadata is successfully read, retaining saved/current capture files. Missing/corrupt files report errors rather than fabricate playback. A very short/failed unsaved capture may be discarded. Per-take deletion removes its metadata/file; interrupted cleanup is retried at startup. Archive/Restore retains takes by Block ID. Standalone FREE takes have null Block ownership; LOOP captures retain immutable recorded BPM/beats and PCM16 mono 48 kHz WAV frames rounded from 60×48000×beats/BPM.

## Database compatibility and safe Block deletion

All explicit Room migrations1→10 and compiler schemas are retained: sessions(2), used BPM(3), takes(4), phrase metadata(5), nullable standalone take ownership(6), Routines(7), Goals(8), independent daily_logs(9), Block images(10). The 9→10 migration only adds the restrictive image table. The 8→9 migration only adds the date-keyed table; all prior tables and recordings remain intact. Existing data/audio are preserved; no destructive fallback.

Safe permanent Block deletion originally shipped with Room 8/JSON 5 and preserves those protections with Room 10/JSON 6 and includes image/PDF references as one attachment blocker. It refuses attached images/PDFs, Goal links, saved Routine references, takes and **all** session placements in a transaction, preserving strict backup integrity. A successfully deleted eligible Block is absent from subsequent snapshots; remaining data restore normally. Completed-history references are not detached or weakened.

Automated unit coverage includes round trips/reopen, legacy compatibility, validation limits/references/states/overflow, nonempty guards and injected write-failure rollback. Native tests exercise actual document pickers, export/cancel/restore/reopen. See CURRENT_STATE.md for the exact current-release unit/native outcomes and limits; earlier release results are historical evidence in WORK_LOG.md. Phone acceptance remains pending in MANUAL_TESTS.md.
