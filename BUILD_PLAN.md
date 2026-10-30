# practice.logger — Current approved build plan

Updated 2026-10-29 (America/Los_Angeles). This is the approved scope for the app as it exists in Git. **Increment 1 (0.13.0/build 25 Daily Log) is complete. Increment 2 (0.14.0/build 26 images) is complete and delivered, with actual verification/limits in CURRENT_STATE.md.** Exact artifacts, source/CI evidence and limitations are maintained in [CURRENT_STATE.md](CURRENT_STATE.md), not repeated here. Increment 3 is delivered in 0.15.0/build 30 with PDF and recording-sheet integration; full API26/API35 and enlarged-text gates pass. The original Stage 0 first-build task is complete.

## Product and implemented scope

A small offline Android practice tool for one musician: reusable concrete Blocks, ordered practice, accurate foreground time, notes and truthful history. Keep Kotlin/Compose/Room/Lifecycle, bundled Lato Regular/Bold, cream/green styling and the current navigation. Every visible action must work; Home Goals are functional, not placeholders.

| Area | Approved, implemented behavior |
|---|---|
| Blocks | Required Name/Action, validated positive duration/optional BPM, notes, literal search, Active/Archived, confirmed reversible Archive/Restore. |
| Permanent deletion | Saved editor action below Archive/Restore, both active/archived; inspect persistent Goal links, distinct saved Routines, takes and every session sourceBlockId placement. Explicit confirmation; atomic fresh recheck/delete; real blocker counts; protected completed history; navigation only on actual success. An image or PDF attachment also blocks deletion until confirmed removal. No automatic detachment, media removal or history rewriting. |
| Practice | One builder draft plus multiple independent unfinished sessions under Practice Now; order/duration overrides; Start/Pause/Resume, Complete/Skip, explicit overtime, saved notes and paused recovery; completed summaries/Recent Practice under Routines. |
| Click and audio | Independent integrated metronome, per-Block free takes and fixed phrase loops, four-click count-in and default-on optional capture guide, playback/rename/delete/export. Standalone free recordings reuse recording infrastructure without synthetic Blocks. Foreground lifecycle cleanup and no automatic audio restart. |
| Tools | Standalone metronome, actual chromatic tuner (single notes, A4=440), stopwatch and rolled whole-minute countdown; no background alarm or persistent tool log. |
| Saved Routines | Stable ordered Block references, independent duration overrides, management/confirmed deletion, builder save and fresh independent practice. Archived/unavailable links prevent the entire start; no silent skipping. |
| Goals | Five active Goals maximum, manual 0–100% progress/outcome/notes, ordering/top-two Home bars, completed/reopen, shared existing Blocks, fresh Goal practice and explicit session attribution of actual committed time. |
| Daily Log / Weekly View | Independent editable local-date plain text, autosave, date picker, Monday–Sunday weekly aggregation/navigation; no practice links or separate weekly records. |
| Reference image/PDF | One private JPEG/PNG/WebP or PDF per saved Block, system picker, bounded validation, safe replace/confirmed remove; inline Show/Hide/zoom/pan, PDF page controls in Focused Practice and the existing recording sheet during capture. Retain archived sourceBlockId ownership, image orientation/layout and audio lifecycle. Increment3 verification status is in CURRENT_STATE.md. |
| Backup | JSON writer 6/readers 1–6, protected empty-target transactional restore, complete relationship/order/state/overflow validation. All image/PDF bytes and attachment metadata and audio/take metadata/labels excluded; per-take export remains available. |

## Data and release acceptance rules

- Preserve app ID, permanent certificate, existing data/audio/IDs, all prior migrations and distribution packages. Current Room schema is 10; explicit 9→10 adds only block_images with restrictive Block ownership. Migrations/schemas 1–9 retained. Never use destructive fallback.
- Session placements deliberately lack a Block foreign key; safe deletion must check their saved sourceBlockId explicitly. Completed history remains protected even after Goal/Routine links are removed. Keep original FK restrictions and strict JSON references.
- Never report a failed database operation as success. Cancellation changes no persistent data. Restore never clears, merges or overwrites a nonempty installation, including an app containing only Logs, Goals or standalone takes.
- Keep the practice timer independent of audio; count only committed foreground active time. Saved snapshots/notes/history must survive Block edits/archive and app updates. Do not promise lossless abrupt-kill recovery or seamless/latency-free acoustic loops without evidence.
- For an authorized app increment: focused tests/compilation first, early clearly labeled signed candidate, evidence-driven isolated diagnostics, then required full release checks. Package with the existing publisher, validate signature/identity/version/integrity and preserve older files. Include actual limitations and a phone checklist. See the mandatory efficiency procedure in WORK_LOG.md.
- The safe-deletion increment required no schema migration or backup semantic change. Focused acceptance covers unused active/archived removal, all four blocker categories, exact counts, cancellation/recreation/recheck, database errors, restart persistence, remaining-data integrity and JSON export/restore.

## Milestone status

| Milestone | Current status |
|---|---|
| Stages 0–2: foundation/catalog/practice | Implemented and automated engineering checks completed in earlier releases; general owner acceptance recorded. |
| Stage 3: backup/recovery/hardening | Implemented; prior automated engineering verification retained. Detailed owner phone/provider/TalkBack acceptance remains pending. Current release verification and any limits are in CURRENT_STATE.md. |
| Stage 4: real-world evaluation | Prepared and deferred; **0/10 documented real sessions**. Only real owner sessions count. |
| Stage 5: integrated metronome | Implemented; owner generally accepted. Detailed acoustic results not supplied. |
| Stage 6A/6B: free takes/phrase loops | Implemented; owner generally accepted. Detailed physical timing/quality/lifecycle checks remain unverified individually. |
| Tools, standalone recording, saved Routines, Goals, visual polish | Owner-approved extensions, implemented. Earlier stage deferrals do not prohibit these shipped features. |
| Safe permanent deletion | Delivered in 0.12.0/build 23 and retained. Historical evidence is in WORK_LOG.md; current verification is in CURRENT_STATE.md. Phone acceptance pending. |
| Increment 2: collapsible Block image reference | Complete: signed 0.14.0/build 26 delivered; exact native results and remaining phone acceptance in CURRENT_STATE.md. Build27 delivered the portrait/layout refinement; Increment 3 delivers PDF and recording integration; current verification is in CURRENT_STATE.md. |
| Daily Log and Weekly View (Increment 1, complete) | Delivered in 0.13.0/build25; exact tests/package status in CURRENT_STATE.md. |

The owner explicitly approved later features while Stage 3 phone acceptance and Stage 4 evaluation remained deferred. Do not mark those gates completed or turn their deferral into a blanket prohibition on an explicit new owner request.


## Retained future boundaries

Increment 1 (Daily Log/Weekly View) is complete. Increment 2 delivered a single-image Block attachment and collapsible inline practice viewer. Increment 3 delivers PDF support and reference viewing inside the recording sheet; implementation and delivery evidence are maintained in CURRENT_STATE.md. No tagging, AI analysis, rich text, search or practice correlations are included. Broader save/storage and audio-inclusive bulk export/restore require an owner discussion. A future media portability design must include audio bytes, a versioned manifest, integrity/reference validation and a safe restore policy; JSON must not be presented as media backup.

Long-press interactions, historical-reference redesign, overdubs/layers/multitrack/mixer/effects/waveforms/trimming, tempo maps/time signatures/subdivisions/accent/tap tempo, transcription/auto tempo, backing tracks, YouTube embedding, multi-face cubes, randomization, notifications, algorithmic spaced repetition, deep analytics, accounts/cloud/backend/subscriptions/sharing, automatic updates, desktop apps, integrations, remote/background recording/looping and advanced animations remain outside current approved scope. These are retained boundaries/ideas, not implementation tasks or claims of infeasibility. No unresolved future decision is inferred as approved.

Stage 4 may eventually support choosing at most one further feature from real observations, or none. Use observed problem → simplest change → cost/tradeoff → owner decision. Reliability feedback takes priority over speculative expansion.

## Owner-requested image refinement

The owner’s 2026-10-29 phone screenshot authorizes compact Focused Practice top matter, upright camera-photo orientation and full safe-width image display. Signed0.14.1/build27 delivers these refinements; Room10/JSON6, private originals and timer/recording architecture stay unchanged. Delivery/testing status is in CURRENT_STATE.md.

## Next action

Install signed 0.15.0/build 30 in place and perform the PDF/recording/update-retention phone checks in MANUAL_TESTS.md. See CURRENT_STATE.md for precise pending acceptance and verification limits. Resume deferred acceptance/evaluation only when the owner is ready. New development follows actual feedback or a new explicit owner request; do not restart completed stages.

## Increment 3 — delivered 2026-10-29

Build26 single-image references and build27 portrait/layout refinement are both complete. The authorized scope is one image OR PDF per saved Block, bounded local PdfRenderer page viewing and inline recording-sheet reference viewing during capture/count-in. Preserve original bytes, ownership/deletion, recording lifecycle and JSON6 exclusions. Delivered 0.15.0/build 30; permanent-key packaging, full API26/API35 release and 200% PDF/image checks pass. Physical-phone acceptance remains pending. Multiple attachments, annotations/OCR, media-inclusive backup and background recording remain deferred.


## Increment 4 — focused reliability/UI refinement, authorized 2026-10-29

Scope is the missing short Block-duration audio cue, compact one-screen Focused Practice matching the owner's hand-drawn sketch and safe interactive header/Home navigation. Reuse the sample-clock metronome, recorder/phrase setup, bounded reference viewer and saved-session workflow. Preserve exact BPM/history, foreground-only timing, explicit overtime and Complete/Skip, snapshot instructions/reference notes, expandable Quick notes and full take management. Exit confirmation saves/checkpoints before Home and keeps failed drafts recoverable; other editors retain discard guards. No schema/backup/audio-engine redesign. Candidate0.16.0/build31 implemented; checks/signing pending in CURRENT_STATE.md. Physical speaker/mixing and owner-phone usability remain separate acceptance requirements.
