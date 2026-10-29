# Current state — Increment 2 candidate, 0.14.0/build 26

Updated 2026-10-29 (America/Los_Angeles). Increment1 Daily Log/Weekly View was delivered in0.13.0/build25. Increment2 image implementation is present but native release acceptance remains in progress. This file is the authoritative verification/delivery record; historical release evidence remains in WORK_LOG.md.

## Candidate delivery

- Signed candidate0.14.0/versionCode26; package com.mrayover.practicelogger.app, Android8+/API26 minimum, compile/target36.
- APK source: `3106e7e1ce95f4ef25e9ee0c11a182ae1d1a8c46`; subsequent schema/test/documentation changes do not change product source.
- [APK](dist/practice.logger-0.14.0-b26-3106e7e/practice.logger-0.14.0-b26-3106e7e.apk), [ZIP](dist/practice.logger-0.14.0-b26-3106e7e.zip), [provenance](dist/practice.logger-0.14.0-b26-3106e7e/BUILD_INFO.json), [verification status](dist/practice.logger-0.14.0-b26-3106e7e/VERIFICATION_STATUS.txt).
- APK SHA256: `7b465a51f89211b720bfd981b7a6b7857dfdf3633589be037d20872cf048a03f`.
- Permanent certificate SHA256: `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- Publisher verified signature/pinned certificate/package/version/hash/ZIP round trip. All92pre-session APK/ZIP packages preserved byte-identical. Install over the permanent-key app without uninstalling/clearing data; physical-phone installation is pending. The old temporary-key0.1.0 app is separate.

## Implemented capabilities and data

Saved Block editor: one private JPEG/PNG/WebP image, system file/photo selection, Show/Hide, safe replacement and confirmed removal. New Blocks require Save/reopen first. Generated files under files/block-images; bounded20MiB/40MP/16384px validation, actual sampled decoding up to2048px, file sync and atomic metadata replacement on IO. Canceled/failed imports retain old attachments. Serialized file cleanup retries known orphans at startup. Archive retains images. Missing/corrupt image reports inline Retry and editor repair/removal.

Focused Practice: sourceBlockId resolves the live Block image without modifying textual historical snapshots. Viewer starts collapsed per placement, fitted aspect ratio, bounded240dp height, pinch1–6×, pan while enlarged, Reset image zoom. Display state does not call timer/click/recording lifecycle. PracticeViewModel, session model, recording engine and modal sheet unchanged; no simultaneous image viewing inside recording is claimed.

Room10: explicit9→10 only adds block_images with restrictive Block ownership; schemas/migrations1–9 retained. Image dependency joins all original Goal/Routine/take/every-session deletion blockers in the confirmation-time transaction. JSON writer6/readers1–6 unchanged; all Logs/structured records remain included. Image bytes/metadata/paths/URIs and recordings are excluded. Retain originals separately and reattach after JSON restore. No missing-media metadata is recreated; existing empty-target protections remain.

## Actual verification

[CI40904738845](https://github.com/mrayover/practice.logger/actions/runs/40904738845), source3106e7e:79unit/Robolectric tests,0failures/errors/skips; debug/release compilation passed; lint0errors/12warnings (10existing and2UsableSpace recommendations). Seven new tests cover formats, failure/size/low-space/rollback preservation, replacement/reopen/archive/removal, dependency recheck/restrictive FK, backup exclusion/restore and conservative orphan cleanup; schema9fixture preserves every prior table including Logs/takes. Compiler schema10 retained,1–9 unchanged.

Both initial Android jobs stopped at ImageReferenceWorkflowTest:46: new fixture JPEG was absent from picker Downloads before import. Setup corrected to external-file/direct-copy/size-check/media-scan; isolated rerun pending. No full native pass claimed. Native workflow, enlarged-text visual review and final release verification remain pending. Prior build25 fullAPI35 passed; fullAPI26 stopped at a recording-dialog timeout while the unchanged recording class passed alone; no cause for that historical timeout was established.

## Remaining limits and exact next task

Run isolated corrected image workflow/reopen checks on API26/35, then200% text and required full regression checks; finalize signed delivery only after actual evidence. Physical phone install-over/data/audio/provider/landscape/TalkBack/acoustic acceptance remains pending; use MANUAL_TESTS.md. Stage3detailed phone pending; Stage4stays0/10. No PDFs, multi-image gallery, camera, annotation, image export, media-inclusive backup or recording-sheet image integration.

Existing Windows checkout C:\Users\mrove\Projects\practice.logger is authoritative Git. Source/tests/docs/schema are committed/pushed through the explicitly authorized workflow; exact final remote SHA is verified at handoff. Dist/signing are local and intentionally ignored; synced sources are untouched.
