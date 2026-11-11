# Multiple references: Stage 3A contract

Prepared 2026-11-10 against clean local and remote `block-ux-stage1` at
`cb7564e5992e7d3425d8b42b2d2153eeec951844`. **Architecture and fixture preparation
only. Nothing below is shipped behavior.** Latest signed application remains
0.37.0/build110 (application source `70778db`); see [delivery evidence](../CURRENT_STATE.md).
Stage 3B implements persistence; 3C implements controls; 3D verifies integration/delivery.

## Verified foundation

Room **23**, Room library **2.8.5**, exported schemas **1–23**, explicit migrations
**1→23**, no destructive fallback. Complete ZIP writes **7**, reads **1–7**;
JSON writes **7**, reads **1–7**. Stage 2 is committed, including pre-save staging,
shared file locking and promotion recovery. No undocumented working changes were present.
Baseline head has no combined-status entries in GitHub; this is not a passing CI claim.
Prior scoped workflow evidence and physical acceptance gaps remain in CURRENT_STATE.

Read with [association ownership](BLOCK_MEDIA_ASSOCIATIONS.md),
[canonical storage](MEDIA_LIBRARY_STORAGE.md), [audio state](MEDIA_AUDIO_WORKSPACE.md),
[overlays](PRACTICE_OVERLAY_SPEC.md), [editor staging](BLOCK_EDITOR_STAGING.md), and
[portable recovery](../BACKUP_FORMAT.md). Those describe the shipped single-reference app.

## Dependency inventory

Paths below are relative to `app/src/main/java/com/mrayover/practicelogger/` unless
otherwise stated. Required changes are bounded by the described assumptions; inclusion
in this audit does not require rewriting a whole file.

| Files | Current assumption / dependency | Required later change and risk |
|---|---|---|
| `BlockMediaLinks.kt` | PK `(blockId,role)`; `find` returns one; `remove` deletes a role; `@Upsert` replaces its asset | Triple PK, ordered list/exact membership, append/reorder/remove-one; never use role-wide removal for one attachment. Retain role/category triggers and restrictive FKs. |
| `BlockDatabase.kt`, `app/schemas/.../23.json` | Room23; media-link count already counts every association | Register 23→24 and export24 only in 3B. Count logic largely reusable; retain transactional deletion and explicit historical `sourceBlockId` checks. |
| `BlockAudioData.kt` | Triple FKs to unique parent index; `linked` compares singular `find`; unlink cascades local data | Rebuild children safely; membership uses exact triple. State/marker IDs and inheritance/promotion algorithm stay. Removing one audio cleans only its own local rows after confirmation. |
| `BlockEditorMedia.kt` | One `visual`, `audio`, original IDs, role-wide flags, scalar manifest and commit witness | Ordered draft collections and full baseline comparison; set-difference Save, preserve unchanged triples; versioned manifest and recovery validate every intended promotion/link, not a nonempty subset. |
| `BlockMediaEditor.kt`, `MainActivity.kt`, `CatalogViewModel.kt` | Compact scalar reference controls, dirty/save flow | 3C lists, per-item preview/remove, reorder, selection and staged-only archive warning. Keep existing form/builder/Goal/week atomic Save. |
| `ImageReferenceData.kt` | `block_images` PK Block ID; canonical visual excludes legacy import; exclusive original cleanup | Keep legacy ownership/file paths, add position, allow legacy alongside canonical links. Compatibility import remains explicit replace of the legacy row, with its own marking confirmation. |
| `ImageReferenceViewModel.kt`, `BlockMediaLinks.kt` resolver | One current visual identity; canonical hides legacy; picker operations replace | Return ordered union of all canonical visuals and optional legacy, including missing-source placeholders. Mutations and overlay copy targets identify an exact reference. |
| `ImageReferenceView.kt`, `PracticeOverlayView.kt` | Viewer finds first Block match; page/zoom state is viewer/source keyed | Select reference by stable identity; retain page/zoom independently per reference; reuse page rendering and orientation. No duplicate renderer. |
| `PracticeOverlayData.kt` | Data already `(blockId,reference,page,drawOrder)`; repository checks single `visualIdentity`; triggers exclude legacy when canonical exists | Membership check for exact source, retain all marking IDs/geometry/order; permit legacy+canonical. Remove obsolete legacy-replacement trigger; keep exact-source deletion guards. Aggregate Block capacity must count all references. |
| `PracticeScreens.kt`, `PracticePanels.kt`, `RecordingSheet.kt` | Shared reference workspace implicitly chooses first; recording embeds it | 3C chooser in existing workspace, retaining timer/Ready/Prep/recording owner. `FocusedWorkspace` lives in `PracticePanels.kt`. |
| `TranscribePanel.kt`, `LibraryAudioViewModel.kt`, `LibraryAudioScreen.kt` | First AUDIO link; VM already keys local state by Block/media; asynchronous checkpoint | Exact selected audio, await successful outgoing checkpoint before switching; fresh incoming source paused. Keep one controller, waveform and marker UI; guard stale callbacks. |
| `RecordReferenceDialog.kt`, `MediaLibraryData.kt` recorded-reference import, `BlockMediaLinks.kt` attach helper | Keep replaces previous AUDIO with loss confirmation | Keep appends a new exact association atomically with the new original; optimistic collection comparison; select new source after success. Keep standalone import behavior. |
| `MediaLibraryData.kt`, `MediaLibraryScreen.kt`, `InspirationData.kt` | Asset restricted by links/Inspiration; one canonical original and master markers | Ownership/deletion transaction and rollback remain. Optional UI link-count improvements only; verify any link across all Blocks protects the asset. |
| `BlockRepository.kt`, `BlockDatabase.kt` dependency query | Permanent delete counts links, images, takes, Goals, Routines, placements, weeks | Existing counts/recheck protections remain; test multiple links and historical placements. No automatic detach. |
| `PortableRecords.kt` | Frozen columns, old per-role PK validated in scratch DB, rejects canonical+legacy visual coexistence | Separate frozen v1–v7 validation from v8; version-specific order/duplicates/union ownership validation, transform after historical validation. |
| `PortableZip.kt`, `PortableMedia.kt`, `PortableRestore.kt` | v7/Room23 manifest, original inventory, schema-derived relationships, witness/journal | v8/Room24, ordered records and compatibility normalization; preserve bounds/hash checks/locks/recovery. Original inventory remains per owner, never per link. |
| `BackupData.kt`, `BackupScreen.kt` | JSON7 data-only, transactional nonempty guard | Format remains unchanged; verify new schema/any new durable rows cannot bypass empty-target protection. Update disclosure only if needed. |
| `app/src/test/...` (`BlockMediaTest`, `MediaMigrationTest`, `ImageMigrationTest`, `BlockAudioTest`, `PracticeOverlayRepositoryTest`, `BlockEditorSaveTest`, deletion/backup tests) | Scalar fixture constructors and some hard-coded schema18 expectations | Adapt callers when entities change; keep historical schema fixtures frozen. Do not repair unrelated assertions in 3A. New fixture described below. |
| `app/src/androidTest/...` (`BlockMediaUpgradeTest`, `MediaLegacyUpgradeTest`, `BlockAudioReopenTest`, editor/media/overlay/Transcribe workflows, `Portable*Test`, `ReferenceUpgradeTest`) | Install-over, scalar controls, native validation and separate-install ZIP phases | Extend in 3B/3C/3D as dependencies become available; no emulator run required for 3A. |

Reusable without redesign: canonical media metadata/IDs/generated filenames, importer
validation, waveform caches, player engine, piano, recorder/takes, master marker IDs,
practice timer/session snapshots, Goal/Routine/week/history ownership and restore journal.
No playlist/sequence engine, second repository, automatic file adoption or new recording system.

## Data contract: proposed Room24

### Identity, order and selection

Canonical association identity is **`(blockId, role, mediaId)`**. No association UUID.
Role is VISUAL for IMAGE/PDF and AUDIO for AUDIO. Duplicate attachment of the same
triple is an idempotent no-op: retain its position and local work; importing identical
bytes follows the existing Library duplicate policy, not a new content identity rule.
Different assets with identical bytes may still have different canonical IDs.

Add `position: Int = 0` to each canonical link and the legacy `block_images` row.
Visual ordering is one union of images, PDFs and any legacy visual. Audio ordering is
independent. Positions are dense zero-based within each Block/role at repository and
archive boundaries. The legacy item uses identity `legacy:<fileName>`; canonical uses
`library:<mediaId>`. A legacy item can coexist with and be reordered among canonical
visuals, retaining its exclusive file ownership. The existing legacy table still has
at most one row per Block; all new imports use canonical storage.

Reorder accepts the complete permutation of reviewed stable identities, rechecks exact
membership and original order inside one transaction, then assigns `0..n-1`. Reject
duplicates, missing IDs and stale lists. Appending uses the next dense position;
removal compacts only that role. Raw SQL may temporarily create tied positions during
the transaction, so do not add a unique position index. Reads explicitly use
`ORDER BY position, identity` (binary identity tie-break); never cursor/insertion order.
Validate integer/nonnegative position on insert/update with triggers; validators reject
non-dense archive lists. Tie-breaking also makes malformed raw input deterministic.

**Default is the first ordered reference**, with no persisted explicit default table.
Reordering can change that default. The optional selected source is visit-local saved
UI state keyed by Block/placement and source identity. A valid restored selection wins;
otherwise choose first, or none for an empty collection. This keeps selection distinct
from persisted per-file audio settings. Switching does not reorder or write a default.
Removing the selected source selects the first surviving item, paused for audio; removing
another source keeps selection. Missing/corrupt bytes remain a selected error/retry item;
never silently switch to a different asset. A user may explicitly choose another.

### Schema before / after

| Table | Room23 | Proposed Room24 |
|---|---|---|
| `block_media_links` | `blockId TEXT NOT NULL, role TEXT NOT NULL, mediaId TEXT NOT NULL`; PK `(blockId,role)` | Same fields plus `position INTEGER NOT NULL DEFAULT 0`; PK `(blockId,role,mediaId)` |
| Its indexes | `index_block_media_links_mediaId`; unique `index_block_media_links_blockId_role_mediaId` | Retain both (triple unique index redundant but intentional for explicit Room contract); add nonunique `index_block_media_links_blockId_role_position_mediaId` |
| Its FKs | Block and asset, `ON DELETE RESTRICT`, update NO ACTION | Unchanged |
| `block_audio_state` | PK/FK `(blockId,role,mediaId)`, child CASCADE | Identical columns, keys and FK actions; physically rebuild to final parent name |
| `block_audio_markers` | PK `(blockId,role,mediaId,id)`, same triple FK CASCADE | Identical columns, keys and FK actions; physically rebuild |
| `block_images` | PK `blockId`, restrictive Block FK, exclusive filename | Same plus `position INTEGER NOT NULL DEFAULT 0`; original row values and file path unchanged |
| `practice_overlays` | PK `id`; unique `(blockId,reference,page,drawOrder)`; restrictive Block FK | No table/index/key change; source-membership logic changes |
| All other tables/indexes | Exported Room23 contract | No schema or row change |

No persistent selection/default table is necessary. Local audio and master annotations
retain every existing key. Overlay `library:`/`legacy:` identities remain byte-for-byte
valid, including page1 of a multipage PDF. No file I/O belongs in Room migration.

### Mutation and deletion boundaries

Association operations are append, reorder and remove-one by exact identity; replacement
is explicit remove+append in one transaction after review. Unchanged associations are
never deleted/reinserted as a shortcut for saving/reordering: CASCADE would destroy local
audio rows. A confirmed audio removal cascades only its triple's state/markers. A visual
removal explicitly discards only its own markings after confirmation; marking guards
continue to reject raw removal. Unlink never deletes canonical original/master markers.
Legacy removal deletes its row after marking confirmation; exclusive orphan cleanup
runs only after commit under the existing lock.

Any surviving association restricts Library deletion, including archived Blocks and
Inspiration references. Permanent Block deletion still counts every association, legacy
row, recording, Goal, Routine, week and all placement statuses; recheck transactionally.
Placements have no sourceBlockId FK, so explicit history validation cannot be dropped.

## Exact 23→24 migration plan (3B only)

Implement one explicit Room migration under Room's existing upgrade transaction; do not
start/commit a nested transaction or toggle `PRAGMA foreign_keys` inside it. No schema24
entity, migration or registration is added in 3A.

1. Preflight `PRAGMA foreign_key_check`; abort on any row. Validate existing role/category
   pairs and overlay ownership/pages. Snapshot/count all three association/audio tables,
   legacy rows and overlays. Invalid input fails closed with the old database retained;
   no repair, discard or fallback. Room schema23 itself permits an unannotated canonical
   and legacy row together despite repository/ZIP policy: preserve both if present.
2. Make transaction-local TEMP copies of `block_media_links`, `block_audio_state`, and
   `block_audio_markers`, with explicit Room23 columns. Verify copy row counts before drops.
   Do not use `ALTER TABLE parent RENAME` with live children: SQLite can retarget their FKs.
3. Drop the eleven application triggers by exact names: `block_media_type_insert`,
   `block_media_type_update`, `linked_media_type_update`, `retire_block_master`,
   `practice_overlay_insert`, `practice_overlay_update`, `practice_overlay_library_delete`,
   `practice_overlay_library_update`, `practice_overlay_legacy_delete`,
   `practice_overlay_legacy_update`, `practice_overlay_legacy_replacement`.
   They include cross-table trigger SQL that can become invalid during rebuild. Keep their
   revised final definitions in shared fresh-install/migration installers. Preflight should
   fail on unexpected triggers referencing rebuilt tables rather than silently lose them.
4. Drop **both audio child tables first**, then old `block_media_links`. Never drop/delete
   the parent while children exist: CASCADE can erase saved local work even with deferred
   FK checks. No other Room23 table has a foreign key to this parent (verified schema audit).
5. `ALTER TABLE block_images ADD COLUMN position INTEGER NOT NULL DEFAULT 0`.
   Create final-named `block_media_links` with triple PK, position default0, unchanged Block/
   asset RESTRICT FKs, and all three named indexes in the schema comparison above.
6. Copy triples from the TEMP parent with `position = CASE WHEN role='VISUAL' AND EXISTS
   (SELECT 1 FROM block_images WHERE blockId=old.blockId) THEN 1 ELSE 0 END`.
   Every ordinary migrated role has its single item at0. A coexisting legacy visual is at0
   and canonical at1. This deterministic exceptional mapping loses no asset or original.
7. Recreate final-named `block_audio_state` and `block_audio_markers` using their exported23
   CREATE definitions and unchanged triple FKs to the **new final parent**. Restore every
   explicit old column, including role, marker IDs, originalMaster, hidden and selectedMarkerId.
   They have no explicit secondary indexes in23. No overlay rebuild or rewrite is needed.
8. Install revised role/category and integer-position insert/update validation, asset-category
   protection and `retire_block_master`. Reinstall overlay insert/update and source-specific
   delete/update guards. Overlay legacy membership now accepts its row alongside canonical
   links; omit obsolete `practice_overlay_legacy_replacement`. Position-only updates must
   never invoke marking-loss checks. Add position validation for legacy rows too. Check the
   full trigger inventory/behavior after open, not just Room's table identity hash.
9. Compare every old parent triple and child column with TEMP snapshots in both directions
   (`EXCEPT`, counts); verify legacy and overlay counts/IDs/values, expected positions,
   all unchanged table snapshots in tests, role compatibility and overlay ownership. Run
   `PRAGMA foreign_key_check` and `PRAGMA integrity_check`; abort unless clean. Inspect
   `PRAGMA foreign_key_list` and index metadata for both child tables and final parent.
   Drop TEMP copies only after checks. Room validates schema24 before committing the upgrade.

Failure/low storage/constraint error throws and rolls back the entire upgrade; files never
change. Process interruption must reopen as complete23 or complete24, with original hashes
and local rows intact. Verify this on Android in3D; a Robolectric reopen is not kill testing.
Rollback means SQLite transaction rollback, **not** installing build110 over a committed24
database. No downgrade path is proposed. Preserve backups before device install-over.

## Populated fixture and verification contract

`app/src/test/java/com/mrayover/practicelogger/MultiReferenceLegacyFixture.kt` constructs
a physical database from frozen exported23 SQL, setup identity hash and frozen triggers in
`app/src/test/resources/stage3a/room23-triggers.sql`. Named raw seed rows live in
`room23-rows.sql`; construction uses no evolving production entities or migration24.
The helper includes valid-format tiny PNG, two-page PDF, four-second PCM16 WAV, a legacy
image and a saved loop original, with stable generated ownership paths. Decoder/device
acceptance and complete ZIP round trips remain later gates; current tests do not claim them.

All27 durable tables are populated, including restore witness, Inspirations sharing the
image, saved notes, completion/reflections, primary Goal and historical attribution.
Four Blocks cover image+audio, multipage PDF, annotated legacy image and archived shared
image/audio. Audio includes distinct per-Block cursor/speed/A–B/repeat/selection, inherited,
local, overridden and hidden master markers. Image Block is in a Routine, Goal, week and
completed historical placement; snapshot name/action/tempo differ deliberately.

`assertPreserved` compares every frozen23 column in every table with SQL NULL intact,
plus SHA256 by relative original ownership path, FK check and integrity check. It can run
against the future writable database before/after migration and after cold reopen because
it projects old columns and ignores newly added positions. Additional3B assertions must
test positions/new capabilities, child FK definitions and indexes/triggers.

`MultiReferenceLegacyFixtureTest` currently verifies23 construction→Room23 open→cold
reopen, marker ownership, dependency counts and raw deletion/type/annotation guards.
These two checks use a test-only TRUNCATE Room builder, retaining the frozen trigger
definitions, after Windows native WAL journal transitions failed with SQLITE_CANTOPEN.
Production WAL configuration is unchanged; this does not establish production WAL or
Android interruption results. Fixture PNG chunk CRCs are checked during construction.
**It does not run23→24.** In3B reuse the helper, open through the registered migration,
call preservation assertions and add actual multiple attachments/removal/reorder/rollback
tests. Add a separate abnormal unannotated legacy+canonical input, empty23, earlier-version
upgrade chain, missing/corrupt-original preservation and injected mid-rebuild failure cases.

Existing coverage inspected: `BlockMediaTest` seeds13/14 associations; `MediaMigrationTest`
has11→current,15→current,16→current,17→current and full-chain inputs; `ImageMigrationTest`
seeds9/10; `InspirationMigrationTest` seeds22; native media/reference upgrade and portable
restore suites cover installed upgrades and file recovery. Several older migration tests
still assert schema18, so their existence is not current-version passing evidence.3A runs
only focused baseline checks and the new fixture; exact outcomes are in WORK_LOG.

## Portable ZIP8 and unchanged JSON7

**ZIP8 is required**: v7's frozen links columns and one-per-role uniqueness cannot represent
the new contract. Set producer Room24/version8 only in3B. Preserve historical table/version
descriptors and manifest bounds. v8 adds position to `block_media_links` and `block_images`;
no new original store or identity. Write link rows sorted by Block ID, role, position and
mediaId, legacy rows by Block ID; read order comes from positions, never JSON array order.
Originals remain one inventory entry per canonical asset, plus exclusive legacy/take owners.

For v1–v7, first validate using the historical frozen columns, keys, exclusions and relationship
rules, then normalize missing position to0 and insert into24 in dependency order. Missing
tables keep their historical empty defaults. In particular, a scratch Room24 database alone
cannot enforce v7's old `(blockId,role)` uniqueness or conflicting visual prohibition: add
explicit historical validation before normalization so malformed old archives stay rejected.
Versions1–7 bytes/formats are never rewritten by the reader or redefined by the v8 writer.

v8 validation rejects duplicate triples, invalid role/type, missing Block/asset, non-integer,
negative, duplicate or non-dense positions in each visual union/audio list, duplicate legacy
ownership filenames, wrong media-category local state, invalid overlay source/page, and
out-of-bounds audio settings/markers. Resolve saved marker selection against the same asset's
effective surviving markers; preserve stored IDs, allowing a stale selection to present as
unselected at runtime (master deletion can already leave a stale selectedMarkerId).
Preserve local state by exact triple and overlays by exact Block/source/page, not list index.

Missing/corrupt required originals fail complete archive creation or validation before restore
confirmation; never silently omit a link/file or partially restore. Missing bytes in an existing
database do not prevent schema migration, which changes no files; the viewer keeps error/retry.
Shared originals are hashed/validated once per owner, regardless of references.

Keep private staging, all resource limits, empty-target validation, synchronized promotion,
synced journal, atomic database witness and startup recovery **before orphan cleanup**.
Restore inserts all associations before local audio/overlays. Test crash before/after file
promotion and database commit with multiple shared sources: rollback uncommitted owned paths
only; committed witness preserves all originals and reapplies preferences. Backup/restore file
locks remain legacy image → Library → takes → database transaction; editor uses the same
prefix, never transaction → file-lock inversion. Reordering needs only a DB transaction.

JSON7/readers1–7 stay data-only, with the same exclusions, limits, strict historical ownership
and completely empty-target transactional guard. No position, link, local audio, overlay,
media metadata, original bytes or unsaved collection draft is added to legacy JSON.

## Editor and practice contract (3C, no controls in3A)

Editor shows compact ordered Visual and Audio lists. Visual combines images/PDFs/legacy.
Each item can preview, remove or move; Add offers Library/import. Explain that first is the
default. Preview selection never mutates saved links. New Blocks stage collections before
first Save using the existing UUID directory and canonical validator.

Manifest v2 stores full ordered baseline and desired collections, per-source reviewed loss
confirmations and staged asset metadata, retaining creation UUID and saved receipt. Read old
Stage2 manifests by mapping scalar selections to singleton lists and preserving intent/legacy
removal confirmations; don't abandon pending originals or fabricate a saved Block. Cancel
deletes only that draft's files. Save validates/rechecks the baseline, acquires existing locks,
promotes each selected new original once and transactionally applies collection differences,
Block/Goal/week/builder writes. Keep all failed staging retryable; on cancellation/interruption
the all-items receipt/witness and existing orphan recovery decide ownership. No change in3A;
3B must keep shipped scalar editor callers working through explicit compatibility adapters
that affect only reviewed selected items, preserving additional associations.
Those adapters read the first ordered item; replacement preserves that reviewed item's
position and unlink removes only that item, compacting order. Never infer remove-all from
a scalar null selection. Recheck the reviewed full baseline before either operation.

Practice selects a visual without leaving the Block. Save UI state per `(placementId,blockId,
referenceIdentity)` including PDF page, zoom/pan and expanded state; each PDF retains its own
page on return. Overlay edit/undo/copy uses exact source and revalidates membership; selecting
another source neither discards markings nor opens a replacement confirmation. Copy targets
must have that same canonical media, even when it is not their selected visual.

Audio chooser keeps exactly one active player. Before switch, pause outgoing source, stop
held piano notes, await a successful final Block/media checkpoint, cancel stale preparation/
waveform jobs, then open incoming local state paused. On save failure keep the current source
and offer retry; don't lose its state. Use a generation token plus exact pair for late callbacks.
Switching back restores cursor/speed/A–B/repeat/marker. No local rows are removed by selection.
Source switching/removal waits while capture, temporary review or save is unresolved, reusing
the existing T3D guard. Record Reference appends and selects only after Keep commits; Record
Take remains an ordinary take. Standalone Transcribe keeps its existing global context.

Visual/audio selection and page changes never call enterBlock/leaveBlock, recreate the
recording sheet, reset practice timer/Prep, restart metronome or modify historical snapshots.
Ready/Prep/Start/Up Next and pinned capture Stop/review controls stay with their current owner.

## Risk register

| Priority / risk | Prevention | Required verification |
|---|---|---|
| P1 lost/orphaned attachments | No migration file I/O; exact set-difference saves, full promotion witness | All old triples/paths/hash equality; multi-file failed Save/retry/interruption |
| P1 local markers/overlays lost | Child snapshots before parent drop; stable triple/source; no delete/reinsert reorder | All27 table snapshots; inherited/override/hide/local selection and PDF page1 annotations after reopen |
| P1 broken Room FKs | Drop children first; recreate final parent/children and indexes | Generated schema24 validation, FK metadata/checks, raw deletion and role constraints |
| P1 ZIP incompatibility | Version8 with frozen old validators then normalization | Separate-empty-target v1–v8 fixtures; malformed old duplicate roles and new duplicate/order rejection |
| P1 lock-order deadlock | Existing image→Library→takes→DB order; no file work in migration | Concurrent draft promotion/export/restore/recovery bounded completion and rollback tests |
| P1 shared-original deletion | RESTRICT any link/Inspiration; cleanup after successful asset delete | Shared active/archived links, failed deletion retains master markers/bytes |
| P1 interrupted migration | One upgrade transaction, no file mutation/destructive fallback | Inject failure after each rebuild boundary; Android process interruption/reopen23-or24 |
| P2 playback regression | Await checkpoint; generation guards; paused source switch; capture guard | Rapid A→B→A/reopen, save failure, background/rotation and concurrent capture workflows |

## Dependency-driven acceptance and stop points

**3B — Persistence:** implement/export Room24 and explicit23→24; adapt multi-association
repositories, ordered legacy union and all exact membership/overlay/audio helpers; keep
scalar callers compatible without role-wide data loss. Implement ZIP8 and frozen1–7
normalization/recovery while keeping JSON7. Run populated fixture migration and preservation,
empty/earlier chain, raw constraints, multiple mixed visuals/audio, no-op duplicate attach,
reorder, remove-one, shared deletion, stale-list conflict, rollback and portable tests.
No UI collection controls required to verify these APIs. Gate next stage on all preservation
and version-compatibility tests; investigate hard-coded historical assertions only as required
for these relevant gates. Do not call migration safe solely from compilation.

**3C — UI:** versioned collection staging/recreation/all-item receipts; compact lists with
individual preview/removal/reorder, visual and audio selection; exact state and checkpoint
contracts above. Native tests cover new first-save imports/Library collections, Cancel/retry,
legacy coexist/reorder, two PDFs with independent pages, overlays, A→B→A audio, 320dp/200%
text/themes, Ready/Prep and pinned capture/review controls.

**3D — Integration:** cross-feature Routine/Goal/week/history/archived regression; complete
ZIP separate-install/restart/crash tests; install over populated build110 on API26/35 and
physical device without uninstall; interrupt upgrades and verify original hashes/local work;
record actual acoustic/phone acceptance, update docs and only then perform authorized signed
delivery/version increment. Preserve build110 artifacts throughout.

Open verification dependencies: SQLite upgrade/process interruption on actual Android;
native image/PDF/audio decoding and complete ZIP fixtures across all historical versions;
bounded lock-concurrency tests; async checkpoint failure behavior. These are explicit3B–3D
gates, not passed3A results. No unresolved identity/ownership decision blocks starting3B.
