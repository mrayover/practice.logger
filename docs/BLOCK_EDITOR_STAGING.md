# Block editor collection staging — Stage 3C.1

The compact editor holds independent ordered Visual and Audio collections before creating a Block. Visuals mix images, PDFs and an existing exclusively owned legacy visual. First in each list is the deterministic default. Adding, reordering, previewing or removing pending selections never writes a Block association. One Save commits the entire form and both collections.

## Manifest and compatibility

BlockEditorMedia reuses files/block-editor-staging/<editor UUID>, MediaLibraryRepository.stage, the canonical bounded import/decoder/hash validator and existing capture/picker infrastructure. A synced temporary manifest atomically replaces draft.json. Manifest version2 stores the reserved creation UUID, saved Block ID receipt, full baseline and desired ordered identity arrays, staged metadata for every original, legacy source metadata, per-source reviewed loss snapshots, and the prepared transaction timestamp/Block witness. Unknown versions fail closed. Form fields and navigation/selected week remain in the existing saved editor state; Activity recreation restores them while the manifest restores selections/order. Drafts remain excluded from backups.

Unversioned Stage2 manifests are read as version1. Scalar changed selections replace/remove only their reviewed first source, preserving any additional baseline members. Scalar originals become singleton collections when there are no other members. Pending bytes and reserved creation identity survive upgrade. Existing successful savedId receipts and creation UUID witnesses are retained. An interrupted version1 existing-Block commit is recognized only when every intended role order and every selected staged asset match the committed database, never from a staged subset.

Existing Library assets are selected by stable ID. An import matching a verified canonical hash/type/size reuses that original; a matching pending original is also reused. Duplicate association additions leave position and local work untouched and report that the reference is already attached. No second media ownership system is introduced.

## Atomic Save and preservation

Validate fields, all intended canonical/staged originals and role compatibility. Under the existing legacy-image then Library locks, promote selected temporary originals once and sync each file. The private staging original remains available until the database commit succeeds. The full collection intent is synced before committing. In one Room transaction, recheck both full original saved orders, insert promoted assets, save Block fields/primary Goal/selected week, remove only exact omitted sources after fresh local-work checks, append genuinely new sources, write both dense orders and append a newly created Block to Builder exactly once. Unchanged association rows are retained, preserving exact media IDs, visual/PDF annotations and audio states/local/inherited/hidden/master marker relationships. Historical placements and Routines are untouched.

Removing a saved reference requires explicit review/confirmation. The draft records that exact source's annotation or audio state/marker snapshot; Save rejects newly changed work. Legacy removal keeps its file until successful metadata commit and existing orphan recovery. Canonical unlink never deletes the Library original. Stale collection errors retain the draft and explicitly direct Cancel/reopen to review current saved references; they never overwrite another operation silently.

The prepared all-item receipt includes ordered identities, staged metadata and exact saved Block/timestamp witness, synced while the same database transaction is still pending. On success savedId is persisted; temporary files alone are deleted. A reserved creation UUID witnesses the atomic first-save transaction even if receipt/navigation cleanup is interrupted, preventing another Block or Builder append. For existing-Block commits, recovery requires the exact Block witness and BOTH complete intended collections. A subset of promoted/linked media never proves success.

## Failure, cancellation and recovery

Validation, missing bytes, stale references and failed persistence retain selections/order for retry or individual removal. An unavailable pending original stays represented/removable on reopen. Failed database transactions roll back every field, association, Goal/week and Builder write. Cleanup deletes promoted files only after a successful database read proves no asset owns them; uncertain outcomes retain bytes for established locked orphan recovery. A process interrupted after some promotions retains the manifest and temporary originals; retry removes proven uncommitted promotion paths and promotes the complete set again.

Cancel/discard deletes only this UUID directory. Shared Library originals stay untouched. Other abandoned draft directories, receipts and capture files retain the established seven-day expiration behavior; updating a manifest refreshes draft activity time. Incomplete files not named by the synced manifest are cleaned on reopen. Startup portable recovery still precedes canonical orphan cleanup. There is no APK downgrade path from Room24 to signed build110's Room23.

## Previews and scope

Saved visual items use the existing exact-source image/PDF viewer and overlays; pending items render actual temporary bytes, including multipage PDFs, without association writes. The small audio dialog auditions actual pending or canonical bytes with a single MediaPlayer, no local-state writes, waveform duplication or persistent association; Close, background and disposal release it. It has Play/Stop only; waveform, speed and marker editing stay in existing workspaces.

Stage3C.2 practice source switching, outgoing audio checkpoints, append-and-select recording integration and per-practice-source viewer state are explicitly outside3C.1. Practice continues to resolve the first ordered reference. Actual tests/results and remaining limitations are maintained in [CURRENT_STATE](../CURRENT_STATE.md).
