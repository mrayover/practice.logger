# practice.logger — Current approved build plan

Updated 2026-10-28 (America/Los_Angeles). This is the approved scope for the app as it exists in Git. **0.12.0/build 23 is delivered.** Exact artifacts, source/CI evidence and limitations are maintained in [CURRENT_STATE.md](CURRENT_STATE.md), not repeated here. The original Stage 0 first-build task is complete.

## Product and implemented scope

A small offline Android practice tool for one musician: reusable concrete Blocks, ordered practice, accurate foreground time, notes and truthful history. Keep Kotlin/Compose/Room/Lifecycle, bundled Lato Regular/Bold, cream/green styling and the current navigation. Every visible action must work; Home Goals are functional, not placeholders.

| Area | Approved, implemented behavior |
|---|---|
| Blocks | Required Name/Action, validated positive duration/optional BPM, notes, literal search, Active/Archived, confirmed reversible Archive/Restore. |
| Permanent deletion | Saved editor action below Archive/Restore, both active/archived; inspect persistent Goal links, distinct saved Routines, takes and every session sourceBlockId placement. Explicit confirmation; atomic fresh recheck/delete; real blocker counts; protected completed history; navigation only on actual success. No automatic detachment, media removal or history rewriting. |
| Practice | One builder draft plus multiple independent unfinished sessions under Practice Now; order/duration overrides; Start/Pause/Resume, Complete/Skip, explicit overtime, saved notes and paused recovery; completed summaries/Recent Practice under Routines. |
| Click and audio | Independent integrated metronome, per-Block free takes and fixed phrase loops, four-click count-in and default-on optional capture guide, playback/rename/delete/export. Standalone free recordings reuse recording infrastructure without synthetic Blocks. Foreground lifecycle cleanup and no automatic audio restart. |
| Tools | Standalone metronome, actual chromatic tuner (single notes, A4=440), stopwatch and rolled whole-minute countdown; no background alarm or persistent tool log. |
| Saved Routines | Stable ordered Block references, independent duration overrides, management/confirmed deletion, builder save and fresh independent practice. Archived/unavailable links prevent the entire start; no silent skipping. |
| Goals | Five active Goals maximum, manual 0–100% progress/outcome/notes, ordering/top-two Home bars, completed/reopen, shared existing Blocks, fresh Goal practice and explicit session attribution of actual committed time. |
| Backup | JSON writer 5/readers 1–5, protected empty-target transactional restore, complete relationship/order/state/overflow validation. All audio/take metadata/labels excluded; per-take export remains available. |

## Data and release acceptance rules

- Preserve app ID, permanent certificate, existing data/audio/IDs, all prior migrations and distribution packages. Current Room schema is 8. Never use destructive fallback.
- Session placements deliberately lack a Block foreign key; safe deletion must check their saved sourceBlockId explicitly. Completed history remains protected even after Goal/Routine links are removed. Keep original FK restrictions and strict JSON references.
- Never report a failed database operation as success. Cancellation changes no persistent data. Restore never clears, merges or overwrites a nonempty installation, including an app containing only Goals or standalone takes.
- Keep the practice timer independent of audio; count only committed foreground active time. Saved snapshots/notes/history must survive Block edits/archive and app updates. Do not promise lossless abrupt-kill recovery or seamless/latency-free acoustic loops without evidence.
- For an authorized app increment: focused tests/compilation first, early clearly labeled signed candidate, evidence-driven isolated diagnostics, then required full release checks. Package with the existing publisher, validate signature/identity/version/integrity and preserve older files. Include actual limitations and a phone checklist. See the mandatory efficiency procedure in WORK_LOG.md.
- The safe-deletion increment required no schema migration or backup semantic change. Focused acceptance covers unused active/archived removal, all four blocker categories, exact counts, cancellation/recreation/recheck, database errors, restart persistence, remaining-data integrity and JSON export/restore.

## Milestone status

| Milestone | Current status |
|---|---|
| Stages 0–2: foundation/catalog/practice | Implemented and automated engineering checks completed in earlier releases; general owner acceptance recorded. |
| Stage 3: backup/recovery/hardening | Implemented; prior automated engineering verification retained. Detailed owner phone/provider/TalkBack acceptance remains pending. Current release verification has the explicit API 35 limit in CURRENT_STATE.md. |
| Stage 4: real-world evaluation | Prepared and deferred; **0/10 documented real sessions**. Only real owner sessions count. |
| Stage 5: integrated metronome | Implemented; owner generally accepted. Detailed acoustic results not supplied. |
| Stage 6A/6B: free takes/phrase loops | Implemented; owner generally accepted. Detailed physical timing/quality/lifecycle checks remain unverified individually. |
| Tools, standalone recording, saved Routines, Goals, visual polish | Owner-approved extensions, implemented. Earlier stage deferrals do not prohibit these shipped features. |
| Safe permanent deletion | Delivered in 0.12.0/build 23. Complete final API 26 and focused both-API proofs pass; final API 35 full-run limitation remains explicit. Phone acceptance pending. |

The owner explicitly approved later features while Stage 3 phone acceptance and Stage 4 evaluation remained deferred. Do not mark those gates completed or turn their deferral into a blanket prohibition on an explicit new owner request.

## Retained future boundaries

No additional implementation is selected. Broader save/storage and audio-inclusive bulk export/restore require an owner discussion. A future media portability design must include audio bytes, a versioned manifest, integrity/reference validation and a safe restore policy; JSON must not be presented as media backup.

Long-press interactions, historical-reference redesign, overdubs/layers/multitrack/mixer/effects/waveforms/trimming, tempo maps/time signatures/subdivisions/accent/tap tempo, transcription/auto tempo, backing tracks, PDF/YouTube embedding, multi-face cubes, randomization, notifications, algorithmic spaced repetition, deep analytics, accounts/cloud/backend/subscriptions/sharing, automatic updates, desktop apps, integrations, remote/background recording/looping and advanced animations remain outside current approved scope. These are retained boundaries/ideas, not implementation tasks or claims of infeasibility. No unresolved future decision is inferred as approved.

Stage 4 may eventually support choosing at most one further feature from real observations, or none. Use observed problem → simplest change → cost/tradeoff → owner decision. Reliability feedback takes priority over speculative expansion.

## Next action

Use the current signed release and report the deletion phone checks in MANUAL_TESTS.md. Resume deferred acceptance/evaluation only when the owner is ready. New development follows actual feedback or a new explicit owner request; do not restart completed stages.
