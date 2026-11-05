# Backup format and safety — current implementation

Complete portable ZIP v2 includes editable Block practice overlays, original media and standalone Transcribe activity. ZIP v1 remains readable. Legacy JSON 7 remains data-only and is independent of Room 18. Actual signed delivery and verification are recorded in [CURRENT_STATE.md](CURRENT_STATE.md).

## Format and records

Identifier: `practice.logger.backup`. Writer version **7**, readers **1–7**. UTF-8 JSON; independent of current Room schema **17**. Top-level fields are `format`, `version`, `blocks`, `sessions`, `placements`, `routines`, `routinePlacements`, `goals`, `goalBlocks`, `sessionGoals`, `logs`, `practiceNotes`. Required fields for the declared version include explicit JSON null for nullable values; unknown extra fields are ignored and unknown versions rejected. Stable IDs are preserved.

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
| practiceNotes | id, blockId, sessionId, placementId, createdAt, text |

| Version | Compatibility |
|---|---|
| 1 | Legacy session rules: at most one unfinished session; no lastUsedBpm/Routines/Goals. |
| 2 | Multiple IN_PROGRESS sessions plus at most one DRAFT. |
| 3 | Nullable placement lastUsedBpm added; old files import it as null. |
| 4 | Routines and routinePlacements required; older files import no Routines. |
| 5 | Goals, goalBlocks and sessionGoals required; older files import no Goals or attribution. Existing history is not retroactively attributed. |

Version **6** requires `logs`, an array of unique date/text pairs with no practice associations. Versions 1–5 import no Logs. Dates must be real Gregorian calendar dates in years 0001–9999, with exact four-digit year and two-digit month/day; invalid dates, duplicates and non-string text are rejected. Weekly collections are derived and never serialized separately.

Version **7** requires `practiceNotes`, independent nonblank timestamped entries with unique IDs and matching existing Block/session/placement ownership. Versions1–6 import no new entries; their original placement notes remain readable in combined Block history. New rows and legacy notes stay separate on export; no copied/backfilled duplicates. Editing changes text, retaining creation time and association fields. Older apps cannot read format7. Descriptions (`blocks.action`/textual references), placement practice notes, overall session notes and daily Logs stay distinct.

## Snapshot, validation and restore

Export takes a consistent Room transaction, including all saved daily Logs and saved archived Blocks, Routines, Goals/links, historical snapshots, notes and saved draft/unfinished recovery state. No monotonic clock anchor or running flag is exported. Restored practice resumes paused. Save new Block notes via Save Note and edits via Save Changes before export; overall session notes still use Save note. Unsaved new/edit drafts are not exported.

Limits: 8 MiB, 20,000 records per type, nesting 32. Oversize export fails explicitly before opening the destination stream; it never silently drops Log entries. Log navigation flushes autosave before returning Home, so a subsequent export includes those edits. Validation rejects malformed UTF-8/trailing data, unsupported versions, missing/wrong types, duplicate/blank IDs, broken references, invalid order/status/outcome/position, nonpositive durations/tempo, negative elapsed time and overflowing totals. Overtime requires reaching configured time; lastUsedBpm is null or 30–300. Every session placement's sourceBlockId must reference an existing Block even for completed history. Routine names are trimmed 1–120 characters, routines nonempty, entries contiguous with positive durations and valid Block references. Archived links are retained; the app refuses new Routine practice until repaired/restored.

Goals require a nonblank title, manual 0–100% progress and ordered nonnegative timestamps. At most five active Goals have contiguous zero-based priority; completed priority is -1. Goal/Block pairs are unique with contiguous link order and valid references. One Goal attribution is allowed per existing non-draft session; per-Goal time totals cannot overflow.

The UI parses/validates the entire file and shows counts before confirmation. The repository validates again and, in the same transaction, requires an empty target across **standalone Transcribe activities, canonical Media Library assets, practice notes, Logs, Goals, Blocks, sessions, Routines and all take metadata (including standalone takes)** before inserting everything. Failures roll back all inserts. Restore never clears, replaces or merges data. Use a spare empty installation; never clear/uninstall the main app to test it.

Canceling picker/confirmation changes nothing. Rotation retains an in-memory preview/operation; process death loses an unconfirmed preview, requiring reselection. Export does not mutate database contents. Android owns the system picker and chooses a distinct document name rather than replacing a same-name document. Failed exports may leave partial documents; retry to a new file and wait for success. Plain text provides no encryption/authentication: keep it private and use trusted files. Provider/full-disk/TalkBack behavior still needs phone checks.

## Image/PDF exclusion and private storage

**JSON versions 1–7 include no image/PDF bytes, Block reference metadata, private filenames or picker URIs.** Restoring JSON retains all structured data and Logs but creates no attachment rows or broken media references. Keep every original image and PDF separately, then attach it again to its restored Block. Use complete ZIP for media-inclusive recovery. Clearing/uninstalling/device loss destroys app-owned copies; Android cloud/device-transfer backup remains excluded.

One `block_images` row per stable Block ID points to a generated UUID `.img` (image) or `.pdf` file under `files/block-images`. Historical table/type names are retained; the existing mimeType and dimension fields support either format, so that PDF increment did not add a migration. The current10→11 notes migration leaves all reference rows and originals unchanged. For PDFs, width/height record the first page’s dimensions; live page count comes from PdfRenderer. The system picker’s source is read only and never deleted. Import copies at most 20 MiB to a generated pending file, checks decoded JPEG/PNG/WebP content, at most 40 million pixels/16384 per side, and verifies sampled decoding at about 2048 pixels per side. File copy/decode run on IO. The original bytes are retained. Display decoding reads rotation/flip metadata through AndroidX ExifInterface and orients the sampled bitmap; it does not rewrite originals, change attachment rows or require reattaching existing images. The file is synced/renamed before the transactional metadata upsert; old attachment/file remain until that commit succeeds. Failed/canceled imports preserve the old image. Confirmed removal deletes metadata, then the private file. A shared media mutex serializes replacement/removal/recovery; known unreferenced generated files (including interrupted pending files) are retried at startup only after successfully reading metadata. Unknown files and other media directories are untouched.

PDF imports copy at most20MiB, require1–100 pages and supported page dimensions (1–14400 points per side), and open/render every page through Android PdfRenderer using one temporary256px bitmap. Validation and page rendering run on IO. An IO scan conservatively rejects PDF encryption names (including hex-escaped names); this also excludes owner-encrypted PDFs that Android15 can open without an opening password. PDFs containing these reserved encryption markers are unsupported. Invalid, password-protected/unreadable or unsupported documents are rejected before replacement commits. Display opens only the requested page, renders its longest edge to2048px and closes page/renderer/descriptor. Only one page bitmap per visible viewer is retained; page changes replace UI references and stale results are discarded. Temporary transport/workspace hiding now retains the current viewer subtree/page/zoom rather than reloading it. No PDF dependency or extra permission is added.

Missing/corrupt references report an inline error with Retry reference; metadata remains until deliberate replacement/removal. Archive preserves image/PDF references. Practice resolves live ownership by sourceBlockId without copying images into historical text snapshots. Permanent deletion rechecks attachment dependencies with all earlier protections in the same transaction; the restrictive foreign key additionally prevents silent loss.

## Audio exclusion and storage

**No JSON version includes recordings, imported backing tracks, take metadata or labels.** JSON restore creates no missing-media take references. Export desired takes individually: FREE as AAC/M4A; LOOP as one normal WAV phrase; IMPORTED_LOOP in its original MP3, AAC/M4A or PCM16 WAV format, with unchanged bytes. Complete ZIP recovers saved recordings and backing tracks together with their metadata. Failed/canceled export preserves the source but can leave a partial destination. Clearing/uninstalling/device loss destroys unexported audio/labels; platform backup/device transfer is explicitly excluded.

Private audio uses generated UUID names under `files/recordings`, with validated duration/frame data, file sync and rename before metadata commit. Interrupted/unreferenced generated files are recovered/removed after metadata is successfully read, retaining saved/current capture files. Missing/corrupt files report errors rather than fabricate playback. A very short/failed unsaved capture may be discarded. Per-take deletion removes its metadata/file; interrupted cleanup is retried at startup. Archive/Restore retains takes by Block ID. Standalone FREE takes have null Block ownership; LOOP captures retain immutable recorded BPM/beats and PCM16 mono 48 kHz WAV frames rounded from 60×48000×beats/BPM.

## Database compatibility and safe Block deletion

All explicit Room migrations through 18 and compiler schemas are retained. Room 18 adds Block practice overlays. Room 17 added the internal portable restore witness. T3B adds standalone activity records through 15→16. T2 added Block-local audio state and markers through 14→15. L3 adds restrictive Block media links through13→14 after L2 master markers through12→13. L1 adds only indexed media_assets through11→12; all prior tables/rows/files remain. Earlier migrations: sessions(2), used BPM(3), takes(4), phrase metadata(5), nullable standalone take ownership(6), Routines(7), Goals(8), independent daily_logs(9), Block images(10), independent practice notes(11). The 10→11 migration only creates the new note table; legacy rows read through unchanged. The 9→10 migration only adds the restrictive image table. The 8→9 migration only adds the date-keyed table; all prior tables and recordings remain intact. Existing data/audio are preserved; no destructive fallback.

Safe permanent Block deletion originally shipped with Room 8/JSON 5 and preserves those protections with Room 11/JSON 7 and includes image/PDF references as one attachment blocker. It refuses attached images/PDFs, Goal links, saved Routine references, takes and **all** session placements in a transaction, preserving strict backup integrity. A successfully deleted eligible Block is absent from subsequent snapshots; remaining data restore normally. Completed-history references are not detached or weakened.

Automated unit coverage includes round trips/reopen, legacy compatibility, validation limits/references/states/overflow, nonempty guards and injected write-failure rollback. Native tests exercise actual document pickers, export/cancel/restore/reopen. See CURRENT_STATE.md for the exact current-release unit/native outcomes and limits; earlier release results are historical evidence in WORK_LOG.md. Phone acceptance remains pending in MANUAL_TESTS.md.


PDF support requires a complete standard startxref/EOF trailer and a cross-reference table or stream at its declared offset; truncated or repair-dependent documents are unsupported. This bounded preflight prevents the observed API26 renderer state failure for incomplete files. Deep native parsing errors may require reopening the app before retrying; retained original bytes/metadata are preserved.


## Room10→11 note-history migration

Only practice_notes is created: six non-null fields listed above, primary ID and indices on blockId/sessionId/placementId, restrictive foreign keys to blocks/sessions/placements. No legacy rows are copied or rewritten. Combined history reads existing nonblank placements.note once per placement, with an internal legacy key and session ended/start timestamp marked Earlier session; an exact old note-creation time is unavailable. New note rows have true save timestamps and independent IDs. Every new/edit operation updates the intended note text only; original descriptions, logs, session notes and active metrics stay separate. Compiler schema11 is retained beside1–10. Focused verification/limitations are authoritative in CURRENT_STATE.md.


## Complete portable ZIP v1

Tools → Backup and restore prepares a private complete archive and verifies it before enabling destination selection. The archive covers all 18 durable tables in Room 16/17, including practice history, interrupted and draft sessions, Logs, Goals, Routines, notes, Transcribe activity, canonical assets, legacy visuals, takes, Block links and master/local annotations and playback state. Each canonical original is stored once regardless of the number of links. Restored original names, IDs and ownership are preserved. The internal restore witness is excluded.

The archive also includes the saved theme, click sound, default output mode, completion alarm and count-in. Keyboard settings are runtime-only. Export-folder URIs and grants are specific to the device and must be selected again. Unsaved edits and captures, playback/clock runtime state, caches, secrets and signing material are excluded.

### Files and resource bounds

- `manifest.json` identifies `practice.logger.portable` v1, the producing app version and build, Room version, counts by durable record type, and an inventory of all other ZIP entries. Each inventory item has its logical path, type, owner ID, expanded byte count and SHA-256.
- `records.json` contains a `tables` object with the frozen v1 columns for each durable record type and an allowlisted `preferences` object. Records store stable IDs and relationship columns explicitly. A restore constructs the database through validated records.
- Original bytes use `originals/media-library/<name>`, `originals/block-images/<name>` or `originals/recordings/<name>`, with the existing generated storage filenames. Canonical hashes also match the stored asset metadata.
- The format uses a single-disk ZIP32 with no archive comment and caps both archive bytes and expanded bytes at 3 GiB, with up to 12,000 entries. Manifest and records each have a 16 MiB cap, a 500,000-value cap and maximum JSON depth 12. Each table has up to 20,000 rows. Existing 20 MiB visual and 50 MiB audio file limits also apply. Staging keeps an 8 MiB free-space reserve and needs space for the input plus expanded media during validation.

The writer reads committed rows in a Room transaction while the image, Library and recording file locks protect originals from replacement or cleanup. It validates referenced originals and streams them into the private ZIP. It then reopens the archive and verifies the directory, counts, logical relationships, byte lengths, CRC and SHA-256. Missing or corrupt required originals stop creation.

### Destination behavior

**Save ZIP as…** writes a new document through Android's picker. The app reports a verified destination only after closing and reading back bytes that match the private ZIP. A provider that accepts the write but denies reopening produces an explicit unverified-copy result and leaves the private ZIP available for another destination. The selected folder option writes under `Backups/` using a new pending document, verifies the bytes and renames it when supported. If verification or publication fails, it offers Save As and attempts to remove only the new incomplete document. Interrupted copies may still be present at the provider. Read-back verifies the provider-visible copy. Cloud synchronization depends on the provider and account.

### Empty-installation restore and interruption recovery

Restore stages the input privately, validates version and paths, rejects duplicates and undeclared entries, checks bounds and hashes, validates all record types and relationships, and verifies original media with the existing image, PDF and audio validators. The preview shows validated record counts and media size. Cancellation before confirmation leaves the target data and settings intact.

Confirmation rechecks all durable tables for emptiness in the same Room transaction that inserts restored records. Media locks exclude concurrent file mutations and orphan cleanup. A synced journal records the unique transaction token, previously absent destination paths, media hashes and desired settings. Synced staged files are promoted into the existing private stores before references are committed. The witness is inserted with the restored rows. Room 17 adds only this internal table through a non-destructive 16→17 migration.

Startup resolves a pending journal before opening the catalog or running orphan cleanup. An absent witness rolls back only the journal's newly promoted files. A committed witness keeps the restored rows and files, verifies the promoted bytes and finishes the saved settings. Settings are written after commit, so pre-commit rollback leaves the target settings alone. Finalization is retryable and the app gates normal navigation while recovery is required. Unfinished Block practice and Transcribe reopen paused.

## Practice Overlay ZIP v2

The v2 manifest identifies `practice.logger.portable` with `version: 2` and `roomSchema: 18`. The `records.json` root declares `version: 2` alongside the existing `tables` and `preferences`. The frozen v1 tables, paths, media inventory and hash checks carry forward. The new `practice_overlays` table and count are required in v2, including when empty.

Each overlay row contains, in order, `id`, `blockId`, `reference`, `page`, `type`, `left`, `top`, `right`, `bottom`, `appearance` and `drawOrder`. Source identities are `library:<mediaId>` or `legacy:<generated filename>`. Geometry uses finite JSON numbers representing normalized coordinates. Validation checks the original numbers and the app’s stored precision, accepted types and appearances, unique IDs and draw order, existing Block and exact visual relationships, and the page count from the staged original. Images use page 0. Each PDF keeps its own zero-based page numbers.

The archive and interactive limits are 64 markings per page, 256 per Block and 10,000 per installation. Draw order ranges from 0 to 1,000,000,000 and is unique within a Block, reference and page. Overlay IDs are nonblank and at most 64 characters. The existing ZIP size and metadata limits also apply.

ZIP v1 from Room 16 or 17 restores through its frozen format with zero markings. JSON writer v7 and readers v1–v7 remain data-only. ZIP v2 stages and validates the full archive before applying the existing empty-target check, synchronized file promotion, transaction witness and startup recovery. The same transaction restores the overlay records and their Block/media ownership. [Overlay behavior and limits](docs/PRACTICE_OVERLAY_SPEC.md#persistence-and-recovery)

## L1 canonical media exclusion and restore guard

JSON7 and readers1–7 exclude all media_assets metadata/UUIDs/titles and all canonical originals. files/media-library owns one generated original per asset, independently of legacy block-images/recordings. A Library-only installation is nonempty and rejects legacy JSON restore in the same transaction, with existing assets retained. Use the complete ZIP to recover Library originals and metadata. See [L1 storage/legacy ownership](docs/MEDIA_LIBRARY_STORAGE.md).

## L2 master audio annotations

Room13 master point/section markers are canonical Library metadata and remain excluded from unchanged JSON7 (readers1–7), together with canonical originals/metadata. The current JSON backup is data-only and cannot transport Library annotations. Derived waveform caches are expendable and are not backup content. The complete ZIP preserves stable media/marker identities and shared originals once. Current Library-only nonempty restore protection remains.

## L3 Block associations remain excluded

Room14 adds restrictive Block VISUAL/AUDIO media links; JSON writer7/readers1–7 do not serialize them or canonical assets. Restoring a legacy JSON backup into an empty installation creates no media links or dangling references. The existing transactional nonempty guard includes media_assets (and Blocks), so a linked or Library-only installation cannot be overwritten. Legacy JSON excludes originals, Block associations and master annotations. The complete ZIP preserves stable asset/link/annotation IDs and each original once. Legacy visual/take ownership and current per-take export remain intact. See [L3 ownership](docs/BLOCK_MEDIA_ASSOCIATIONS.md).


**T2 portability.** JSON 7 excludes Block-local playback settings, locally created markers, master-marker overrides and hides. Restore creates no Block/audio associations. Complete media-inclusive recovery belongs to S2.
