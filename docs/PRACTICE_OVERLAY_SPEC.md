# Practice Overlay

O0 defines the contract. O1 delivers it after S2 has verified portable recovery and before G1. The [V1 master plan](../PRACTICE_LOGGER_V1_MASTER_PLAN.md) owns sequencing.

## Interaction

Open **Markings → Edit markings**, choose a tool, then drag a rectangle on the page. Tap a shape to select it. Drag inside it to move it, or drag its upper-left or lower-right handle to resize it. Use the Markings menu to change a highlight to yellow, blue or pink, delete the selected shape, undo the last edit, hide or show all shapes, and copy saved markings. Tap **Done** to resume ordinary zoom and pan. The page edges allow scrolling during editing. The menu and viewer provide named actions for selecting and placing shapes, moving, resizing and ending editing with accessibility services.

Hold **Peek** to reveal covers on the current page. The accessibility action reveals covers for three seconds. Multiple Focus rectangles keep all of their openings clear. The surrounding dim layer sits over the source and below highlights and covers. Highlights and covers draw in saved order.

| Tool | Behavior |
|---|---|
| Highlight | Add a translucent rectangle. Select it to move, resize, edit its supported attributes, or delete it. |
| Cover | Mask a rectangle for recall practice. Peek temporarily reveals the original region. Ending Peek restores the mask at the saved geometry. |
| Focus | Keep the selected rectangle visible and dim the surrounding page. Reuse Cover’s rectangle geometry. |

Provide explicit editing controls and a visibility toggle. Users can select, move, resize and delete shapes and undo their last committed shape edits within the workspace. Undo writes the inverse edit through the same persistence path. Hide and Peek affect presentation only. Clear Peek on release, leaving the page, or exiting the viewer. Saved markings reload after restart. Default navigation keeps normal page selection, pinch zoom and pan. In edit mode, reserve shape drags and handles for editing and keep a clear way back to normal navigation. Expose tool state and actions to accessibility services.

O1 scope is limited to rectangles and one existing visual reference per Block. Freehand, handwriting recognition, text or score editing, PDF rewriting, flattening and export are out of scope. Multiple separate PDFs per Block and Bluetooth page-turn controls are deferred.

## Viewer integration

Extend the existing Block viewer and reference resolution.

- [ImageReferenceView.kt](../app/src/main/java/com/mrayover/practicelogger/ImageReferenceView.kt) owns page display and zoom/pan. Draw over the fitted page content and use the same transform for rendering and hit testing. Map edits through its inverse. Clip to the page content and use the orientation-corrected bitmap axes for photos.
- [ImageReferenceViewModel.kt](../app/src/main/java/com/mrayover/practicelogger/ImageReferenceViewModel.kt) resolves canonical and legacy visual attachments. [ImageReferenceData.kt](../app/src/main/java/com/mrayover/practicelogger/ImageReferenceData.kt) and [LibraryVisualPreview.kt](../app/src/main/java/com/mrayover/practicelogger/LibraryVisualPreview.kt) provide the existing bounded image/PDF rendering and preview path. Leave the standalone Library preview free of Block overlays.
- Follow the ownership and deletion paths in [BlockMediaLinks.kt](../app/src/main/java/com/mrayover/practicelogger/BlockMediaLinks.kt), [BlockDatabase.kt](../app/src/main/java/com/mrayover/practicelogger/BlockDatabase.kt) and [Block media associations](BLOCK_MEDIA_ASSOCIATIONS.md). Integrate with the existing practice workspace and its [panels](../app/src/main/java/com/mrayover/practicelogger/PracticePanels.kt). Overlay actions must keep the viewer mounted, preserve its page/zoom/pan and stay independent of recording, the practice timer, metronome and Transcribe. Keep active capture controls reachable.

## Ownership and lifecycle

Store one editable record per shape with a stable ID, owning Block ID, exact visual reference identity, zero-based page index (0 for images), type, rectangle, supported appearance attributes and deterministic draw order. Store left, top, right and bottom as finite fractions of the unzoomed page content. Require `0 ≤ left < right ≤ 1` and `0 ≤ top < bottom ≤ 1`. Geometry must render consistently across zoom, pan, layout, density and device orientation. Save complete edits atomically. A failed edit retains the prior saved shape and reports the failure.

Canonical Media Library originals stay immutable. Use the existing canonical media ID for identity. For legacy references, preserve an attachment-specific identity across ZIP restore, using the existing generated filename identity when preserved or a durable attachment ID mapped during restore. Never key by mutable title, absolute path, page bitmap or Block ID alone. Older Blocks and legacy attachments start with zero shapes and retain existing rows and files.

Multiple Blocks can use the same source and keep independent overlays. Copy only by explicit action to a Block with the same verified source identity and compatible pages. Append independent records with new IDs in one transaction. Verify the current links again when committing. A name or matching page count alone cannot establish compatibility.

Use a confirmed discard policy for unlink or replacement of an annotated reference. Warn that the affected Block’s saved overlays will be removed. Cancel preserves the original link and markings. Confirmed unlink/replacement removes only that Block’s overlays for the exact reference in the same database transaction as the link change. Recheck the active identity and affected markings before commit. Retain the canonical original and all other Blocks’ data. Follow existing legacy file retirement and recovery rules after the transaction commits. Archive keeps the link and overlays. Existing Block and canonical asset deletion guards still apply. A missing or unreadable file keeps its overlay records and uses the existing viewer error/retry behavior.

## Persistence and recovery

O1 adds an explicit non-destructive Room migration from the then-current version, with constraints and repository validation binding records to the owning Block and active visual reference. Retain every earlier migration, row and media original. The migration adds empty overlay storage for existing installs.

In the same milestone, extend the S2 versioned ZIP manifest with overlay records and their relationship IDs. Restore the original bytes once per canonical asset and restore overlay geometry as editable records. Validate manifest and record versions, unique IDs, exact Block/reference relationships, supported type and appearance attributes, page indexes against the verified original, finite rectangle bounds and bounded per-page, per-Block and total counts. O1 must define finite limits shared by interactive writes and restore before shipping. Restore older supported ZIPs and legacy JSON through their existing compatibility paths. Older formats without overlays restore zero overlays. Reject unsupported or malformed overlay data before mutation and retain S2’s empty-target and all-or-nothing recovery protections.

Room 18 adds `practice_overlays` through migration 17→18. A restrictive Block foreign key and database triggers tie each shape to its active source. The reference key is `library:<mediaId>` or `legacy:<generated filename>`. The same keys survive ZIP restore. The repository compares the current saved shape before applying an edit or its undo. Copy appends all saved pages to a Block using the same verified canonical source. A legacy attachment’s private filename identifies one Block’s attachment.

The app and ZIP v2 share limits of 64 shapes per page, 256 per Block and 10,000 per installation. IDs are nonblank and at most 64 characters. Drawing order is unique within each page and ranges from 0 to 1,000,000,000. Highlights store one of three colors. Cover and Focus use their fixed appearances. ZIP v1 and legacy JSON restore without markings. [ZIP v2 layout and recovery](../BACKUP_FORMAT.md#practice-overlay-zip-v2)

## O1 acceptance

- Create and edit all three shapes, including move, resize, delete, undo, visibility and temporary Peek. Verify behavior across pages, zoom, pan, rotation, restart and enlarged text. Confirm source bytes and ordinary viewer navigation remain intact.
- Exercise canonical PDF, canonical image and legacy references. Verify isolation across Blocks sharing one source, explicit copy with new IDs, copy rejection for incompatible sources, and cancellation/confirmation and rollback for unlink or replacement.
- Check practice clock, metronome, recording, pinned capture controls and Transcribe while interacting with the viewer. Overlay and page actions must preserve their lifecycle and timing.
- Verify upgrade with the new migration and preservation of old rows and media. Test malformed, mismatched, oversized and unsupported overlay data against the transactional restore path. Verify earlier supported backups still restore.
- Back up a fixture with an image, multiple PDF pages, independent overlays on Blocks sharing a source, and a legacy attachment. Restore on a separate empty installation. Verify stable identities, correct pages, original bytes, editable geometry and post-restore edits. The release gate requires both the persistent records and verified ZIP recovery.
