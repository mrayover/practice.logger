# L3 — shared Library references in Blocks

L3 adds independent **one visual (IMAGE/PDF)** and **one audio (AUDIO)** associations in the saved Block editor. Exact signed delivery, test outcomes and phone acceptance are recorded in [CURRENT_STATE](../CURRENT_STATE.md). L1/L2 owner-phone acceptance remains separately pending. K1 Reference Piano is the next milestone; T1 follows K1.

## Durable associations

Room14 adds `block_media_links(blockId, role, mediaId)` through explicit13→14. The composite primary key enforces one association per Block/role; an indexed mediaId supports shared ownership and restrictive foreign keys to both Blocks and assets prevent implicit detachment. Migration and fresh-install callbacks install insert/update role/type triggers and protect linked asset category changes. Repository validation also rejects missing Blocks/assets, incompatible categories and missing/changed originals before committing. Renaming titles never changes media IDs.

`BlockMediaRepository` manages lightweight associations using the existing canonical MediaLibraryRepository and legacy ImageReferenceRepository. It stores no originals or waveforms and creates no playback engine. Many Blocks resolve the same canonical file in files/media-library. Unlinking retains the original and master annotations, including after the final link is removed. Archived Blocks retain links.

Block deletion dependency counts include canonical links alongside legacy images, Goals, Routines, takes and all historical session placements. The final check/deletion remains transactional. Canonical asset deletion continues to remove master markers then the asset in one transaction; an existing restrictive Block link rejects the asset deletion and rolls marker deletion back. Original-file cleanup happens only after a successful database deletion.

## Editor, imports and legacy transition

The existing saved editor offers Choose from Library / Import New into Library for each role. Selection reuses Library search, metadata, stable sorting and compatible filters, shows the current selection and changes nothing on cancel. Canonical replacement/unlinking operate independently by role. Audio displays title/metadata; playback inside Blocks remains T1 scope.

Files, gallery photos and staged camera photos import through the existing canonical importer and original validation. Camera/gallery contracts, private provider and staging are reused. A successful import commits its canonical original before attachment; failed attachment retains that asset and the prior reference and reports the incomplete attachment. Choose from Library can retry. No persistent Block-specific copy is created.

Historical block_images/files/block-images retain their ownership. Resolution uses a canonical VISUAL link when present, otherwise the legacy Block attachment. Missing/corrupt canonical originals surface the shared viewer error/retry path; they are not silently replaced by a different legacy source. There is no automatic startup copying/adoption.

Selecting a canonical replacement for a legacy visual requires a dedicated confirmation that the old private original will be removed. Cancel preserves the legacy attachment (and any newly imported Library asset). The existing image mutex serializes replacement with legacy import/removal/recovery. The new link and removal of legacy metadata commit in one transaction; failure preserves the old row/file. Only after successful commit does conservative legacy recovery remove the retired original. A crash after commit can leave a recognized unowned legacy file for cleanup on the next startup; it cannot remove the canonical original. Uncertain cleanup outcomes retain bytes for recovery.

Historical IMPORTED_LOOP takes retain their IDs, labels, Block links, playback, format-correct export and exclusive ownership in files/recordings. TakeRepository.delete/recover/audioFile still reason only about take-owned files; no canonical asset points into this directory. Recorded WAV loops and free M4A takes remain unchanged. Automatic imported-take adoption is deferred because safe coexistence preserves every owner without copying bytes or weakening cleanup.

The old Loop Setup Add Backing Track importer remains available for saved imported takes. New canonical audio selection is established in the Block editor. Their convergence into Record Loop / Transcribe belongs to T1 after K1, as expressly allowed by the L3 implementation handoff. L3 adds no local markers, A–B, speed persistence, waveform, keyboard or simultaneous playback/capture mode.

## Shared visual workspace and recovery boundary

`ImageReferenceViewModel.visuals` resolves stable Block/sourceBlockId ownership across both stores. `CollapsibleImageReference` remains the single Block viewer, delegating page reads to the appropriate existing repository. FocusedWorkspace and RecordingSheet use this same resolution without changing their composition/lifecycle ownership. Page/zoom/pan/toggle state never calls recording or practice timing APIs. Description/Reference arrangement, workspace suppression/restoration and pinned active recording/review controls remain.

JSON writer7/readers1–7 are data-only and exclude canonical originals/metadata, Block media links, master annotations and legacy media. Existing media-only nonempty-target protection remains inside the restore transaction. Restoring old JSON creates Blocks without media links and cannot create dangling canonical references. Library/editor/Backup disclosures identify these limits. Future S2 must archive each original once and restore stable media IDs, Block links and master/local annotations together; derived waveform caches remain expendable.

## Verification

BlockMediaTest targets13→14 preservation, repository/raw-write role constraints, atomic replacement, failure rollback, archived links/reopen, shared ownership, marker rollback, deletion protection and JSON exclusions. Existing migration/image/import/recording/backup tests remain part of the scoped unit gate. Native checks use exact build76 install-over, seeded legacy metadata/hashes and playback/export, shared PDF/audio files and markers, editor selection/import/cancel/replace/remove, error/retry, orientation/enlarged text, canonical visual references during practice and recording, and cold reopen. Coverage descriptions do not assert a result: actual runs, failures and verification limits belong in CURRENT_STATE/WORK_LOG.
