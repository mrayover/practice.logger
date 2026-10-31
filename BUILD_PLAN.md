# practice.logger — Current approved build plan

Updated 2026-10-31 (America/Los_Angeles). This records implemented scope and historical deliveries; the [V1 master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md) is the authoritative forward roadmap. Planned phases are not implemented features and require an explicit assignment. **Increment 1 (0.13.0/build 25 Daily Log) is complete. Increment 2 (0.14.0/build 26 images) is complete and delivered, with actual verification/limits in CURRENT_STATE.md.** Exact artifacts, source/CI evidence and limitations are maintained in [CURRENT_STATE.md](CURRENT_STATE.md), not repeated here. Increment 3 is delivered in 0.15.0/build 30 with PDF and recording-sheet integration; full API26/API35 and enlarged-text gates pass. The original Stage 0 first-build task is complete.

## Home gear removal — owner-requested bounded follow-up

Build48 removes only the duplicate Goal gear; existing Edit opens Manage Goals. No global Settings screen is introduced; Tools Audio Settings remains. Existing preview workflow checks compile/launch/Home navigation and Log saving; exact delivery and pending P4/P5 gates in CURRENT_STATE.

## P4 automatic completed-loop audition

Build53 reuses existing preview playback once after successful foreground loop capture cleanup, before explicit Keep. Incomplete/cancelled/stale/background capture never auditions; background/recomposition never restarts it. Stop/manual Preview and Keep/Re-record/Discard retain temporary ownership. Free preview remains manual and the owner-accepted build52 layout is unchanged. Scoped compilation/seven phrase unit checks/two API35 playback/lifecycle/layout scenarios pass. Signed debug delivery/phone audible confirmation in CURRENT_STATE; no new milestone/full regression.

## P4 loop spacing follow-up

Build52 fixes the owner phone screenshot’s clipped digits and unused lower setup area: intrinsic/minimum field height, proportional vertical distribution and recording/review footer at the transport boundary. Existing loop behavior/engine remains. Scoped compilation/seven phrase unit checks and an expanded compact/tall API35 path pass. Permanent-key debug delivery and phone confirmation pending in CURRENT_STATE. No new milestone/full regression.

## P4 loop recorder screenshot refinement

Build51 compacts three fields/eight shortcuts/duration/green Record Loop and execution budget, always enables the existing phrase click, and replaces Record Loop with inline Keep/Re-record plus small Preview/Discard. Free/Loop action columns align; selectors/footer retained. Scoped compilation, seven phrase audio/storage unit checks and one API35 native layout/capture/review path pass. Permanent-key debug APK and pending phone acceptance in CURRENT_STATE. No new milestone/P5/full regression.

## P4 targeted practice interface refinement

Build50 adds practice-specific compact metronome options and one-row output modes, removes explanatory text there, and allows workspace header taps to collapse transport and select that section while preserving live audio/capture and retained viewers. Collapsed temporary review retains Save/full-review access. Four focused preview UI tests and compilation pass; physical phone fit/switching acceptance pending in CURRENT_STATE. No new milestone, P5, Media Library or full regression cycle.

## P4 metronome button feedback refinement

Build49 replaces both metronome dots with full Stop click button background pulses, using the existing consumed-audio beat/90ms decay and cream/green palette. Button geometry, audio, modes and controls are retained. Compilation and two focused API35 surface-pixel/start-stop/BPM checks pass; no separate milestone or regression gate. Actual results/delivery in CURRENT_STATE.

## P4 audio Settings and bounded UI polish — scoped candidate delivered

Build47 persists four click timbres, shared output default and completion alarms (two-tone/bell/rising chime/short beep/Off), with previews in Tools Audio Settings. Block and foreground countdown alarms share one-shot/capture/Off safeguards without clock changes. Existing count-in/recording-click and compact retained Notes/reference/transport behaviors remain; Settings adds overflow/large-text/landscape checks. Cream/green/Lato retained; conditional dark theme explicitly deferred to avoid a broad hard-coded-color refactor. No storage/Library/analytics features. P1–P3 generally accepted; P4 phone checklist pending. Exact scoped results/delivery in CURRENT_STATE. Next engineering gate is P5 full regression; it requires the next assignment and has not started.

## P3 metronome — scoped candidate delivered

Build46 extends the existing sample-clock engine and shared practice/Tools controls: six meters, default-Off first-beat accent, Sound + Flash / Sound only / Flash only, compact pulse/running meter and exclusive expanded transport panel. /8 BPM counts each eighth note without grouped/dotted-quarter mode. Config changes apply at the next beat; tempo phase and practice timing remain independent. Native playback progress drives flash events, with zero click PCM in silent mode. Exact scoped results, signed delivery and physical acceptance limits are in CURRENT_STATE.md. P2 is generally owner-accepted; P3 detailed phone checks remain pending. P4 subsequently delivers global sound/output/alarm defaults; see above.

## P2 flexible phrase loops/count-ins — scoped candidate delivered

Build45 adds any whole loop length 1–64, positive user-entered count-in, persistent default 4 in minimal Tools → Audio Settings and per-loop override. BPM 30–300 remains. A 15-minute count-in execution budget guards resources/overflow with a reason; no fixed musical count-in menu. Streaming guide output and chunked WAV IO retain exact sample boundaries without giant guide/file buffers. Current P1 review/explicit Save remains; Room11/JSON7 unchanged. 13 scoped audio/storage tests, 7 API35 native checks and 1 API26 max-playback probe pass; exact signed delivery and phone limits are in CURRENT_STATE.md. P3 metronome is now delivered separately; P4 subsequently delivers broader sound/settings features; see above.

## P1 temporary audio take review — scoped candidate delivered

Build44 adds shared temporary Review Take to Focused Practice, saved-Block and standalone free recordings, and complete phrase loops. Preview, explicit Save, Re-record and Discard reuse the existing engines/private-file ownership. Saved rows appear only after a successful commit; failed commits retain temporary bytes for retry. Background stops audio, no automatic saving/playback/resume; incomplete phrases remain invalid. Review is in-process temporary state, not durable saved audio. Room11/JSON7 and existing saved data are unchanged. Exact tests, signing and phone limitations are in [CURRENT_STATE.md](CURRENT_STATE.md). Owner generally accepted P1 (“works”); individual phone results are not inferred. P2 is delivered with scoped checks; P3 and later phases do not start automatically.

## Delivered Home button removal build42

Remove standalone Back to Home/Home buttons throughout the app and their rows; move following content up. Retain banner/Android Back with current save/discard barriers and parent-specific navigation. Compile/one launch/one targeted navigation and Log-save scenario only; full release gates unchanged. Delivery in CURRENT_STATE.

## Existing workspace/footer behavior

Description defaults expanded; one section remains open outside transport panels. Notes moves to the pinned footer; remove Save for later and its row. Keep existing notes/PDF/audio/session behavior. Scoped existing storage/native checks and compilation/one launch; full release gates unchanged. Delivery evidence is in CURRENT_STATE.

## Existing Block note history and workspace behavior

Build40 adds multiple timestamped Block/session practice notes, immediate collapsible history, explicit new/legacy note editing and draft safeguards. Room11 adds only practice_notes; legacy placement notes read through without copying. JSON7/readers1–7 preserve both forms. Descriptions/reference notes, overall session notes and daily Log remain distinct. Transport panels temporarily suppress and restore the saved upper workspace while retaining PDF page/zoom and the existing audio/session owner. This scope supersedes the earlier multi-note/migration exclusions; the owner explicitly added Edit during implementation. Limited compile/one launch/focused storage/UI checks only. Actual signed delivery is in CURRENT_STATE. Temporary Accept/Re-record/Discard take review was absent from build40/build42 and is now delivered as the P1 build44 scoped candidate. Concurrent loop playback/free capture, mixing, YouTube and unrelated work remain excluded; no subsequent assignment begins automatically.

## Product and implemented scope

A small offline Android practice tool for one musician: reusable concrete Blocks, ordered practice, accurate foreground time, notes and truthful history. Keep Kotlin/Compose/Room/Lifecycle, bundled Lato Regular/Bold, cream/green styling and the current navigation. Every visible action must work; Home Goals are functional, not placeholders.

| Area | Approved, implemented behavior |
|---|---|
| Blocks | Required Name/Action, validated positive duration/optional BPM, notes, literal search, Active/Archived, confirmed reversible Archive/Restore. |
| Permanent deletion | Saved editor action below Archive/Restore, both active/archived; inspect persistent Goal links, distinct saved Routines, takes and every session sourceBlockId placement. Explicit confirmation; atomic fresh recheck/delete; real blocker counts; protected completed history; navigation only on actual success. An image or PDF attachment also blocks deletion until confirmed removal. No automatic detachment, media removal or history rewriting. |
| Practice | One builder draft plus multiple independent unfinished sessions under Practice Now; order/duration overrides; Start/Pause/Resume, Complete/Skip, explicit overtime, saved notes and paused recovery; completed summaries/Recent Practice under Routines. |
| Click and audio | Shared practice/Tools metronome with six meters, optional accent and sound/flash modes, per-Block free takes and 1–64-beat fixed-tempo loops, configurable positive whole-beat count-in and default-on optional capture guide, playback/rename/delete/export. Standalone free recordings reuse recording infrastructure without synthetic Blocks. Foreground lifecycle cleanup and no automatic audio restart. |
| Tools | Standalone metronome, actual chromatic tuner (single notes, A4=440), stopwatch and rolled whole-minute countdown; no background alarm or persistent tool log. |
| Saved Routines | Stable ordered Block references, independent duration overrides, management/confirmed deletion, builder save and fresh independent practice. Archived/unavailable links prevent the entire start; no silent skipping. |
| Goals | Five active Goals maximum, manual 0–100% progress/outcome/notes, ordering/top-two Home bars, completed/reopen, shared existing Blocks, fresh Goal practice and explicit session attribution of actual committed time. |
| Daily Log / Weekly View | Independent editable local-date plain text, autosave, date picker, Monday–Sunday weekly aggregation/navigation; no practice links or separate weekly records. |
| Reference image/PDF | One private JPEG/PNG/WebP or PDF per saved Block, system picker, bounded validation, safe replace/confirmed remove; inline Show/Hide/zoom/pan, PDF page controls in Focused Practice and the existing recording sheet during capture. Retain archived sourceBlockId ownership, image orientation/layout and audio lifecycle. Increment3 verification status is in CURRENT_STATE.md. |
| Backup | JSON writer 7/readers 1–7, protected empty-target transactional restore, complete relationship/order/state/overflow validation. All image/PDF bytes and attachment metadata and audio/take metadata/labels excluded; per-take export remains available. |

## Data and release acceptance rules

- Preserve app ID, permanent certificate, existing data/audio/IDs, all prior migrations and distribution packages. Current Room schema is 11; explicit 10→11 adds only practice_notes, after 9→10 block_images with restrictive Block ownership. Migrations/schemas 1–9 retained. Never use destructive fallback.
- Session placements deliberately lack a Block foreign key; safe deletion must check their saved sourceBlockId explicitly. Completed history remains protected even after Goal/Routine links are removed. Keep original FK restrictions and strict JSON references.
- Never report a failed database operation as success. Cancellation changes no persistent data. Restore never clears, merges or overwrites a nonempty installation, including an app containing only Logs, Goals or standalone takes.
- Keep the practice timer independent of audio; count only committed foreground active time. Saved snapshots/notes/history must survive Block edits/archive and app updates. Do not promise lossless abrupt-kill recovery or seamless/latency-free acoustic loops without evidence.
- For an authorized app increment: focused tests/compilation first, early clearly labeled signed candidate, evidence-driven isolated diagnostics, then the checks required by the assigned master-plan phase. P1–P4 use scoped checks and signed delivery, not a full regression per increment; P5 and F1–F2 retain full regression and real-device gates. P0 uses documentation checks only, with automatic build CI skipped and no compilation, signing, Gradle tests/lint or emulator runs. Package with the existing publisher, validate signature/identity/version/integrity and preserve older files. Include actual limitations and a phone checklist. See the mandatory efficiency procedure in WORK_LOG.md.
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


## Forward V1 scope and retained boundaries

The [V1 master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md) supersedes older future exclusions only for its explicitly described phases: P1 temporary take review; P2 custom 1–64-beat loops and positive whole-beat count-ins; P3 metronome meter/accent/flash; P4 persistent audio Settings and bounded UI polish; P5 full pre-Library regression; L1–L3 reusable Media Library; S0–S2 provider feasibility, organized copies and complete ZIP backup/restore; G1–G3 Goal identity/filtering/historical time attribution; F1–F2 final verification and phone signoff. Full dark mode remains conditional. P0 implemented none of these extensions; P1 temporary take review, P2 flexible loops/count-ins and P3 metronome have since been delivered, with evidence in CURRENT_STATE.md. The other roadmap extensions remain planned.

Current single Block-owned image/PDF references, P1 review/P2 custom loops/count-ins, existing Goal links/manual progress and JSON7 data-only backup remain the implemented baseline. Media-inclusive backup is required for completed V1 but does not exist yet. Current JSON excludes media bytes and metadata; keep originals separately and retain per-take export until the later phases deliver verified recovery.

Preserve existing data, media, stable IDs, migrations and distributions. Only the owner's existing test-only Block references have the narrow later L1 disposal exception: a safe migration must account for their table/files, preserve unrelated data and confirm any actual non-test attachment before discarding it. P0 performs no disposal or migration. Restore remains empty-target and atomic; no populated-installation overwrite/merge is approved.

Master-plan §12 defines V1 non-goals. Long-press/historical-reference redesign, multitrack/overdub/mixing/effects/trimming, grouped subdivisions/tempo maps, transcription/auto tempo, backing tracks/YouTube, notifications/background recording, algorithmic learning, advanced analytics, accounts/backend/sync, subscriptions/sharing, desktop/integrations and automatic updates remain outside this assignment and the described V1 scope. Meter/accent, concise actual-time statistics, SAF copies and complete recovery are planned exceptions to the older blanket exclusions, not approval for those broader systems. Tags, OCR/annotations and multiple references per Block remain outside V1.

Stage 3 phone acceptance and Stage 4's 0/10 real sessions remain pending. Evaluation feedback informs later owner decisions; it does not limit the already-approved V1 roadmap to one further feature. Follow the roadmap dependencies and stop after each assigned phase.

## Owner-requested image refinement

The owner’s 2026-10-29 phone screenshot authorizes compact Focused Practice top matter, upright camera-photo orientation and full safe-width image display. Signed0.14.1/build27 delivers these refinements; Room10/JSON6, private originals and timer/recording architecture stay unchanged. Delivery/testing status is in CURRENT_STATE.md.

## Next action

Owner-phone-check the signed build46 P3 candidate using its PHONE_CHECKLIST.txt. P1/P2 are generally owner-accepted; P3 scoped engineering checks pass, while detailed phone acceptance and full regression remain pending. P4 is the next roadmap phase but is unassigned and must not begin automatically. P1–P4 use scoped delivery, with full gates at P5 and final V1. See CURRENT_STATE.md for exact source/packages/checks.

## Increment 3 — delivered 2026-10-29

Build26 single-image references and build27 portrait/layout refinement are both complete. The authorized scope is one image OR PDF per saved Block, bounded local PdfRenderer page viewing and inline recording-sheet reference viewing during capture/count-in. Preserve original bytes, ownership/deletion, recording lifecycle and JSON6 exclusions. Delivered 0.15.0/build 30; permanent-key packaging, full API26/API35 release and 200% PDF/image checks pass. Physical-phone acceptance remains pending. Multiple attachments, annotations/OCR and background recording remain outside V1. Media-inclusive backup was deferred at this historical increment and is now planned as the required S2 gate; current JSON remains data-only.


## Increment 4 — focused reliability/UI refinement, authorized 2026-10-29

Scope is the missing short Block-duration audio cue, compact one-screen Focused Practice matching the owner's hand-drawn sketch and safe interactive header/Home navigation. Reuse the sample-clock metronome, recorder/phrase setup, bounded reference viewer and saved-session workflow. Preserve exact BPM/history, foreground-only timing, explicit overtime and Complete/Skip, snapshot instructions/reference notes, expandable Quick notes and full take management. Exit confirmation saves/checkpoints before Home and keeps failed drafts recoverable; other editors retain discard guards. No schema/backup/audio-engine redesign. Implemented as signed0.16.0/build34 candidate; focused checks pass as recorded in CURRENT_STATE.md, with final image checks passed and complete release regression incomplete. The owner stopped further development/testing for that increment. The later V1 master plan now governs assigned forward work; it does not retroactively complete these verification gaps. Candidates31–33 are retained. Physical speaker/mixing and owner-phone usability remain separate acceptance requirements.


## Build35 — owner-authorized layout preview, 2026-10-30

The owner confirmed shorter Practice Description, expanded Reference Material and secondary controls moved toward pinned primary actions. Scope is layout only plus versionCode35 and existing permanent-key packaging. Explicit preview-only exception: compile debug/release, one API35 launch smoke check and verify signed package/identity/version/integrity; skip full units/lint/API26/35/regression/screenshot automation and unrelated legacy investigations. Added a separate manual owner-preview workflow; the normal android.yml and release verification rules are unchanged. Delivered as an owner-test preview with full regression and phone layout/data acceptance pending; CURRENT_STATE.md records actual evidence.


## Build36 — owner-requested compact reference, 2026-10-30

Owner accepted build35's expanded size and requested Reference Material small until clicked. Build36 starts with an Expand header, opens the approved area/media in one tap, and offers Collapse while retaining lower controls and existing scrolling. Only presentation/versionCode changed. Reused one manual preview compilation/API35 debug launch smoke run and permanent-key publisher. Full regression and owner compact/expand/collapse/data acceptance remain pending; normal release rules unchanged. Exact package/check evidence is in CURRENT_STATE.md.
