# practice.logger — Current approved build plan

Updated 2026-11-10 (America/Los_Angeles). This records implemented scope and historical deliveries; the [V1 master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md) is the authoritative forward roadmap. Planned phases are not implemented features and require an explicit assignment. **Increment 1 (0.13.0/build 25 Daily Log) is complete. Increment 2 (0.14.0/build 26 images) is complete and delivered, with actual verification/limits in CURRENT_STATE.md.** Exact artifacts, source/CI evidence and limitations are maintained in [CURRENT_STATE.md](CURRENT_STATE.md), not repeated here. Increment 3 is delivered in 0.15.0/build 30 with PDF and recording-sheet integration; full API26/API35 and enlarged-text gates pass. The original Stage 0 first-build task is complete.

## Block UX normalization

Stage 1 implements the expandable Block pills and context actions in the Library, This Week and Prepare Week. Weekly markers read committed completion snapshots and leave Room and backup formats unchanged. See [implementation and verification](CURRENT_STATE.md#stage-1-source-and-verification) and the [forward dependency order](PRACTICE_LOGGER_V1_MASTER_PLAN.md#block-ux-normalization).

Stage 1.5 normalizes the Session Builder with snapshot-based placement pills, an on-demand active Block picker, independent repeated placements and a pinned Start Session action. Implementation and focused verification are in progress. Stage 2 remains the compact Block editor and pre-save reference attachment workflow. Stage 3 remains multiple visual and audio references with migration and backup compatibility.

## Inspirations and Playground navigation

Library offers Media and a personal Inspirations collection. Inspirations supports writing, UTF-8 TXT/Markdown import, camera capture, PDF and image viewing, and protected edit and delete flows. Room 23 stores stable records and complete ZIP v7 preserves originals and metadata. I2 expands the offline included collection to five read-only works using the same packaged TXT reader. Log offers Journal and the Playground placeholder. See [storage and bundled sources](docs/INSPIRATIONS.md), [future Playground specification](docs/PLAYGROUND_SPEC.md), and [verification](CURRENT_STATE.md).

## G2 — Shared Goal Filtering

Build99 delivers the same primary Goal filter alongside text search in Manage Blocks, the Session Builder, the saved Routine editor, and This Week → Prepare Week → Add existing Blocks. Results retain the existing active/archived rules and Goal indicators. [Scope](PRACTICE_LOGGER_V1_MASTER_PLAN.md#g2--shared-goal-filtering-medium) · [Delivery and checks](CURRENT_STATE.md)

## G1 — Goal priority colors and primary Block associations

Room 19 adds a primary link and a first-accrual placement snapshot. Goal indicators and Block caps follow the current active priority. Complete ZIP v3 carries both new record types. Existing Goal session totals still use their established attribution. [Scope](PRACTICE_LOGGER_V1_MASTER_PLAN.md#10-g1g3--v1-goal-colors-block-filtering-and-truthful-statistics) · [Verification and delivery](CURRENT_STATE.md)

## O1 — Block-Local Practice Overlay Implementation

Build95 delivers Highlight, Cover with Peek and Focus in the existing Block reference viewer. Room 18 stores editable Block-owned shapes and complete ZIP v2 preserves them with the original media. [Overlay scope](docs/PRACTICE_OVERLAY_SPEC.md) · [Verification and delivery](CURRENT_STATE.md)

## S2 — Complete Portable ZIP Backup and Verified Restore

S2 delivers ZIP v1 with original media, committed practice data, editable audio annotations and portable preferences. Restore validates and previews the archive, enforces an empty target and recovers across interrupted file promotion and database commit. [Format and recovery](BACKUP_FORMAT.md#complete-portable-zip-v1) · [Delivery and focused verification](CURRENT_STATE.md)

## T3D — Concurrent Transcribe Playback and Free Record

Block Transcribe adds **Record Take** using the existing Free Record capture and review workflow. The reference transport stays mounted and independently controllable. Preview pauses the source. Save keeps one Block-owned take and practice time stays with the active Block placement. [Implementation details](docs/MEDIA_AUDIO_WORKSPACE.md#record-take-in-block-transcribe) · [Delivery and focused verification](CURRENT_STATE.md) · [Owner-phone checklist](MANUAL_TESTS.md#t3d--concurrent-transcribe-playback-and-free-record).

## T3C — Record Reference Directly in Transcribe

Record and review AAC/M4A directly in standalone or Block Transcribe with playback paused. Keep saves one canonical Library original and immediately selects it or attaches it to the Block. Protected replacement uses the existing confirmation. T3C shipped with Room 16 and JSON 7. See [current state](CURRENT_STATE.md) for delivery and verification.

## T3B — Standalone Timer and Goal Attribution

Standalone Transcribe records open-ended foreground activity with an optional Goal. The timer shares exclusive clock ownership with Block practice. Completed activities contribute to Home, Weekly View and Goal totals. Room 16 introduced activity history. JSON 7 excludes it. The S2 ZIP includes it. See [current state](CURRENT_STATE.md) for verification and delivery.

## T3A — Practice Now and standalone Transcribe

Practice Now offers saved unfinished sessions, the existing Add Blocks builder, direct Routine start, and standalone Transcribe. Standalone uses canonical Library audio, the shared waveform and transport, global master annotations, and the embedded Reference Piano. T3A shipped with Room 15 and JSON 7. See [CURRENT_STATE](CURRENT_STATE.md) for delivery and verification.

## T3-0 — Standalone Transcribe roadmap lock


The November 3 Media Library roadmap amendment established the dependency chain through T2. The November 4 owner handoff splits the remaining T3 work.

Sequence: **P5 → L1 → L2 → L3 → K1 → T1 → T2 → T3-0 → T3A → T3B → T3C → T3D → S0 → S1 → O0 → S2 → O1 → G1 → W1 → G2 → P1 Intentional Practice Lifecycle → P2 Block and Session Reflection → G3 → F1 → F2**.

T2 is delivered in signed build86. T3-0 is a documentation checkpoint. The owner assigned **T3A — Standalone Transcribe Workspace** on November 4, 2026. The [Standalone Transcribe plan](docs/T3_STANDALONE_TRANSCRIBE_PLAN.md) defines T3A–T3D, their checks and their dependencies. S2 includes standalone activity history, Goal associations, canonical microphone references, and global and local annotations alongside its approved recovery scope.

## T2 - Block-local audio and marker publication

T2 extends Transcribe with Room 15 playback checkpoints and Block-local annotations tied to the active canonical AUDIO link. Blocks inherit Library markers, can customize or hide them, and can publish individual changes with explicit confirmation. See [audio workspace architecture](docs/MEDIA_AUDIO_WORKSPACE.md#block-local-audio-in-t2) and [CURRENT_STATE](CURRENT_STATE.md) for actual verification and delivery.

## T1 — Record Loop / Transcribe

Implemented in the existing Focused Practice transport. T1 uses the Block's canonical audio association, shared Library playback and waveform, read-only master annotations, and the K1 keyboard. The Library player owns shared audio focus during paused audition and play-along. Room 14 and JSON 7 remain in use. See [CURRENT_STATE](CURRENT_STATE.md) for delivery and verification, and [audio workspace architecture](docs/MEDIA_AUDIO_WORKSPACE.md) for ownership. T2 extends this baseline above.

## K1 — Reference Piano Tool

Delivered permanent-key0.21.0/build81: independent16-voice PCM engine, reusable native-multitouch Compose keyboard and small Tools wrapper; C1–C7 registers, compact/wider layouts and safe foreground/focus cleanup. Build 81 embedded mode borrowed the playing reference controller's focus; note release/close leaves the reference intact. Room14/JSON7/media ownership unchanged. Scoped unit/native results, setup diagnostics, signed artifacts and owner confirmation are in CURRENT_STATE; [reuse contract](docs/REFERENCE_PIANO.md). T1 is documented above.

## L3 — Shared Block media associations

Delivered permanent-key0.20.0/build80: independent canonical visual/audio links, explicit13→14 restrictive role relationships, shared Library picker/imports and existing practice/recording visual viewer. Legacy visual replacement is deliberate and atomic; imported takes retain exclusive ownership. Referenced Library deletion is refused, including rollback of master markers. JSON7 remains media/association-excluding. Scoped results and package/phone status are in CURRENT_STATE; [ownership details](docs/BLOCK_MEDIA_ASSOCIATIONS.md). K1 and T1 are documented above.

## L2 — Media preview and master audio annotation workspace

Explicitly assigned November3,2026. Implements dedicated Library image/PDF previews through shared bounded renderers/gestures; one reusable canonical-audio controller/waveform with source-time seeking, pitch-preserving 50–100% speed, A–B ranges/repeat and master point/section CRUD. Room13 adds restrictive master-marker ownership via12→13. Streamed signal-derived waveform cache is expendable; JSON7 still excludes media/annotations. Delivered as permanent-key0.19.0/build76 with focused automated verification; phone acceptance remains pending. Actual results and signed-delivery status are in [CURRENT_STATE](CURRENT_STATE.md). [Technical details](docs/MEDIA_AUDIO_WORKSPACE.md). L3 now adds Block links above. K1 and T1–T2 are delivered above. S2 delivers complete ZIP recovery.

## L1 — Canonical Media Library/storage

Owner explicitly assigned L1 and confirmed build69 camera/gallery works on their phone on November3,2026. Room12 adds indexed media_assets through11→12, with independent originals in files/media-library and shared existing validators. Home Library now imports/manages/searches/filters/sorts/renames/deletes supported visual/audio media. Legacy references, takes, playback/export and deletion restrictions remain unchanged; L3 retains safe legacy take coexistence and only retires legacy visuals after confirmed canonical replacement; no automatic adoption or byte copying. JSON7/readers1–7 stay media-excluding; Library-only installs now block restore. See [storage implementation](docs/MEDIA_LIBRARY_STORAGE.md) and CURRENT_STATE for actual focused verification/delivery. L2 now delivers previews/master annotations; Block links are delivered in L3; S2 later delivered complete ZIP backup.

## Owner-assigned reference photo refinement

Delivered0.17.3/build69 before Media Library: split files / Or / camera-gallery actions in the saved Block editor, private full-resolution camera output and selected gallery image through existing validation. Targeted release build and 11 reference/capture tests pass; full P5 is not rerun. Signed delivery, evidence and phone checklist: [CURRENT_STATE](CURRENT_STATE.md).

## Owner-assigned Submarine refinement

One dark red palette through existing semantic/native roles and stable persisted ID; Submarine immediately before System Default at the bottom. Existing palettes, Light default and saved choices remain. Owner clarified delivery requires a new signed install-over release: 0.17.2/build68, using the permanent key. Seven theme tests and release compilation pass; no P5 rerun. Verification: CURRENT_STATE.

## P5 — complete and owner accepted

Full pre-Library automated regression and normalization are complete: all nine required CI41329443572 jobs pass, with 111 unit tests and 168 native executions on API26/API35. Signed build67 fixes native-theme API qualification, full-width references and enlarged-text controls; tests follow current Notes/transport/management behavior. Exact source, package, attempt history, 18 lint advisories and physical limitations are in [CURRENT_STATE](CURRENT_STATE.md) and [P5 report](docs/P5_REGRESSION_REPORT.md). Owner accepted build67 on November 1, 2026: “Confirmed complete and running correctly”. P5 is closed. L1 is now explicitly assigned; its scoped implementation/status is recorded above and in CURRENT_STATE.

The bounded-delivery verification statements below describe each earlier increment at its delivery time. The current P5 evidence supersedes those earlier scoped-test limits; it does not imply phone acceptance.

## Owner-assigned Orchid theme

Build65 adds dark plum surfaces and light purple accents through existing theme roles, Settings and matching native window/accent styles. Layout/Lato/control state behavior remains shared. Verification and signed-delivery status: CURRENT_STATE. Phone acceptance pending; no later phase/full regression.

## Owner-assigned Sunny theme

Build64 adds a persisted yellow/orange Sunny palette through existing semantic roles, Settings and matching native window/accent styles. Layout/Lato/control state behavior remains shared. Verification and signed-delivery status: CURRENT_STATE. Phone acceptance pending; no later phase/full regression.

## Owner-assigned Smooth theme

0.17.0/build63 adds a persisted Smooth teal palette through existing semantic roles and Settings, with matching native window/accent defaults. Same Lato, geometry and control states; no data/audio-engine changes. Scoped contrast/persistence/native selector/recreation/practice-flash checks and signed delivery are recorded in CURRENT_STATE. Phone acceptance pending; no later phase/full regression.

## Owner-assigned imported backing audio

0.17.0/build62 adds per-Block imported MP3/M4A/PCM16 WAV as explicit file-repeat loops in the existing collection, through Focused Practice Loop Setup. Supported format/resource limits, private staged Save/selection, shared management, unchanged recorded sample-loop path and JSON exclusion are documented in docs/IMPORTED_AUDIO.md. This direct owner assignment narrowly supersedes the old P4 no-MP3 exclusion; it does not start Media Library, backup, P5 stabilization or other roadmap work. Exact scoped evidence/delivery/phone next task: CURRENT_STATE.

## P4 targeted visual/navigation/transport and historical Notes refinement

Build60 retains selection outlines outside practice, shares Recent Practice/Block Library pills, replaces Home Settings with a Library placeholder and restores the shared-header Settings gear through existing leave/save guards. Filled Notes/Skip and compact BPM/meter/Accent/chevrons reuse existing transport/audio behavior. Historical summaries now read existing timestamped/legacy notes by exact session placement and reuse editing; Room11/JSON7 remain. Scoped evidence, delivery and phone acceptance limits are in CURRENT_STATE; no P5/P6/P7 or roadmap change.

Build59's phone follow-up adjusts only metronome name/signature fit and action alignment, preserving the chevron and existing behavior. Focused130%-text geometry/control check and signed delivery are recorded in CURRENT_STATE.

Build60 only centers/nudges the click bubble at the recording boundary and matches Accent/checkbox to primary green. Existing owner-approved row layout remains; one scoped check and signed delivery in CURRENT_STATE.

## Generalized themes — complete; owner accepted

Build55 originally placed Settings below Log on Home; the current owner refinement moves it to the shared-header gear. Light/Dark/System choices, central Material/app semantic roles and platform chrome now share one root resolver; build55 is signed and the owner accepted Light/Dark as complete. Recheck themes at the next assigned P5/F1 regression. Existing cream/green/Lato/transport ownership and original media remain; no G1 identity migration or broader phase starts. Actual results and delivery are maintained in CURRENT_STATE.

## Home gear removal — owner-requested bounded follow-up

Build48 removes only the duplicate Goal gear; existing Edit opens Manage Goals. This historical increment introduced no global Settings screen; the current shared-header Settings gear was added later. Existing preview workflow checks compile/launch/Home navigation and Log saving; exact delivery and pending P4/P5 gates in CURRENT_STATE.

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

Build47 persists four click timbres, shared output default and completion alarms (two-tone/bell/rising chime/short beep/Off), with previews in Tools Audio Settings. Block and foreground countdown alarms share one-shot/capture/Off safeguards without clock changes. Existing count-in/recording-click and compact retained Notes/reference/transport behaviors remain; Settings adds overflow/large-text/landscape checks. At that historical checkpoint cream/green/Lato remained; centralized themes subsequently shipped in build55 and expanded through build65. No storage/Library/analytics features. P1–P3 generally accepted; P4 phone checklist pending. Exact scoped results/delivery in CURRENT_STATE. P5 full regression is now assigned and active; CURRENT_STATE records its execution.

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

A small offline Android practice tool for one musician: reusable concrete Blocks, ordered practice, accurate foreground time, notes and truthful history. Keep Kotlin/Compose/Room/Lifecycle, bundled Lato Regular/Bold, central Light/Dark/System/Smooth/Sunny/Orchid styling and the current navigation. Every visible action must work; Home Goals are functional, not placeholders.

| Area | Approved, implemented behavior |
|---|---|
| Blocks | Required Name/Action, validated positive duration/optional BPM, notes, literal search, Active/Archived, confirmed reversible Archive/Restore. |
| Permanent deletion | Saved editor action below Archive/Restore, both active/archived; inspect persistent Goal links, distinct saved Routines, takes and every session sourceBlockId placement. Explicit confirmation; atomic fresh recheck/delete; real blocker counts; protected completed history; navigation only on actual success. An image or PDF attachment also blocks deletion until confirmed removal. No automatic detachment, media removal or history rewriting. |
| Practice | One builder draft plus multiple independent unfinished sessions under Practice Now; order/duration overrides; Start/Pause/Resume, Complete/Skip, explicit overtime, saved notes and paused recovery; completed summaries/Recent Practice under Routines. |
| Click and audio | Shared practice/Tools metronome with six meters, optional accent and sound/flash modes, per-Block free takes and 1–64-beat fixed-tempo loops, configurable positive whole-beat count-in and always-enabled loop recording click, playback/rename/delete/export. Standalone free recordings reuse recording infrastructure without synthetic Blocks. Foreground lifecycle cleanup and no automatic audio restart. |
| Tools | Standalone metronome, actual chromatic tuner (single notes, A4=440), stopwatch and rolled whole-minute countdown; no background alarm or persistent tool log. |
| Saved Routines | Stable ordered Block references, independent duration overrides, management/confirmed deletion, builder save and fresh independent practice. Archived/unavailable links prevent the entire start; no silent skipping. |
| Goals | Five active Goals maximum, manual 0–100% progress/outcome/notes, ordering/top-two Home bars, completed/reopen, shared existing Blocks, fresh Goal practice and explicit session attribution of actual committed time. |
| Daily Log / Weekly View | Independent editable local-date plain text, autosave, date picker, Monday–Sunday weekly aggregation/navigation; no practice links or separate weekly records. |
| Reference image/PDF and audio | One canonical Library visual plus one independent audio association per saved Block, with legacy visual fallback and deliberate replacement. Existing legacy references retain their private originals; system picker, bounded validation, safe replace/confirmed remove; inline Show/Hide/zoom/pan, PDF page controls in Focused Practice and the existing recording sheet during capture. Retain archived sourceBlockId ownership, image orientation/layout and audio lifecycle. Increment3 verification status is in CURRENT_STATE.md. |
| Backup | JSON writer 7/readers 1–7, protected empty-target transactional restore, complete relationship/order/state/overflow validation. All image/PDF bytes and attachment metadata and audio/take metadata/labels excluded; per-take export remains available. |

## Data and release acceptance rules

- Preserve app ID, permanent certificate, existing data/audio/IDs, all prior migrations and distribution packages. Current Room schema is14; explicit13→14 adds restrictive Block media links after12→13 master markers; explicit11→12 adds canonical media_assets, after10→11 practice_notes and9→10 block_images. Migrations/schemas1–11 retained. Never use destructive fallback.
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

The [V1 master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md) supersedes older future exclusions only for its explicitly described phases: P1 temporary take review; P2 custom 1–64-beat loops and positive whole-beat count-ins; P3 metronome meter/accent/flash; P4 persistent audio Settings and bounded UI polish; P5 full pre-Library regression; L1–L3 canonical visual/audio Media Library; K1 Reference Piano; T1–T2 Block Transcribe and local annotations; T3A–T3D standalone Transcribe, shared time attribution and recording integration; S0–S2 provider feasibility, organized copies and complete ZIP backup/restore; G1–G3 Goal identity/filtering/historical time attribution; F1–F2 final verification and phone signoff. Centralized themes are delivered; verification is in CURRENT_STATE. P0 implemented none of these extensions; P1 temporary take review, P2 flexible loops/count-ins and P3 metronome have since been delivered, with evidence in CURRENT_STATE.md. P4 and P5 are also delivered. L1 canonical storage, L2 previews/master annotations and L3 independent Block associations are implemented; L3 verification/delivery is tracked in CURRENT_STATE. K1 and T1–T2 are delivered. T3A–T3D are implemented. S0 is documented and S1 implements organized recording export. O0 and S2 are complete. O1, G1–G3 and F1–F2 remain planned.

L1 adds canonical independent Media Library imports/management with Room12; legacy single Block-owned image/PDF references, P1 review/P2 custom loops/count-ins, existing Goal links/manual progress and JSON7 data-only backup remain intact. S2 provides verified media-inclusive recovery. Legacy JSON excludes media bytes and metadata. Per-take export remains available.

Preserve existing data, media, stable IDs, migrations and distributions. The L1 handoff supersedes the old test-only disposal exception: all legacy visual references/takes are preserved; safe in-place adoption requires L3 ownership/linking changes documented in docs/MEDIA_LIBRARY_STORAGE.md. P0 performs no disposal or migration. Restore remains empty-target and atomic; no populated-installation overwrite/merge is approved.

Master-plan §12 defines V1 non-goals. Long-press/historical-reference redesign, multitrack/overdub/mixing/effects/trimming, grouped subdivisions/tempo maps, automatic transcription/auto tempo, YouTube, notifications/background recording, algorithmic learning, advanced analytics, accounts/backend/sync, subscriptions/sharing, desktop/integrations and automatic updates remain outside this assignment and the described V1 scope. Meter/accent, concise actual-time statistics, SAF copies and complete recovery are planned exceptions to the older blanket exclusions, not approval for those broader systems. Tags, OCR, general document editing and unrestricted multiple references remain outside V1. The master plan includes O1 Block-local Practice Overlay, approved audio annotations and one visual plus one active audio link; imported backing takes are already implemented. Manual Transcribe is planned, not automatic note transcription.

Stage 3 phone acceptance and Stage 4's 0/10 real sessions remain pending. Evaluation feedback informs later owner decisions; it does not limit the already-approved V1 roadmap to one further feature. Follow the roadmap dependencies and stop after each assigned phase.

## Owner-requested image refinement

The owner’s 2026-10-29 phone screenshot authorizes compact Focused Practice top matter, upright camera-photo orientation and full safe-width image display. Signed0.14.1/build27 delivers these refinements; Room10/JSON6, private originals and timer/recording architecture stay unchanged. Delivery/testing status is in CURRENT_STATE.md.

## P1 Intentional Practice Lifecycle — delivered in build100

The shared Block practice screen gates entry and advancement with Start or Prep. Existing placements reopen Ready and active practice resumes only by explicit action. Prep retains the existing workspace, audio ownership and note/recording safeguards. Room 21 adds placement preparation time. Complete ZIP v5 preserves it and reads v1–v4. Session and Goal totals use active practice only. [Current verification and delivery](CURRENT_STATE.md)

Build103 gives the Focused Practice metronome row more room and uses a measured compact Start/Stop click button when it allows one row at enlarged text. Controls wrap with narrow widths or enlarged text. [Delivery and verification](CURRENT_STATE.md)

## P2 Block and Session Reflection — build104

Completion commits the outcome and its immutable Goal snapshot before a skippable Block review. The final placement commits the session before optional prompts and the existing session note. Session Summary displays saved reflections. Room 22 adds the completion and reflection tables. Complete ZIP v6 preserves those records and reads v1–v5. See the [authoritative phase](PRACTICE_LOGGER_V1_MASTER_PLAN.md#p2-block-and-session-reflection) and [delivery evidence](CURRENT_STATE.md).

## Next action

The next Block UX assignment is Stage 2, the compact editor and pre-save reference workflow. [Current verification and pending phone checks](CURRENT_STATE.md#stage-1-source-and-verification)

## Increment 3 — delivered 2026-10-29

Build26 single-image references and build27 portrait/layout refinement are both complete. The authorized scope is one image OR PDF per saved Block, bounded local PdfRenderer page viewing and inline recording-sheet reference viewing during capture/count-in. Preserve original bytes, ownership/deletion, recording lifecycle and JSON6 exclusions. Delivered 0.15.0/build 30; permanent-key packaging, full API26/API35 release and 200% PDF/image checks pass. Physical-phone acceptance was pending at this historical increment. Its original exclusion of multiple attachments/annotations is superseded only by the approved independent visual/audio links and audio marker scope; PDF/image markup, OCR and background recording remain excluded. Media-inclusive backup was deferred at this historical increment. S2 later delivered it through the complete ZIP. Legacy JSON remains data-only.


## Increment 4 — focused reliability/UI refinement, authorized 2026-10-29

Scope is the missing short Block-duration audio cue, compact one-screen Focused Practice matching the owner's hand-drawn sketch and safe interactive header/Home navigation. Reuse the sample-clock metronome, recorder/phrase setup, bounded reference viewer and saved-session workflow. Preserve exact BPM/history, foreground-only timing, explicit overtime and Complete/Skip, snapshot instructions/reference notes, expandable Quick notes and full take management. Exit confirmation saves/checkpoints before Home and keeps failed drafts recoverable; other editors retain discard guards. No schema/backup/audio-engine redesign. Implemented as signed0.16.0/build34 candidate; focused checks pass as recorded in CURRENT_STATE.md, with final image checks passed and complete release regression incomplete. The owner stopped further development/testing for that increment. The later V1 master plan now governs assigned forward work; it does not retroactively complete these verification gaps. Candidates31–33 are retained. Physical speaker/mixing and owner-phone usability remain separate acceptance requirements.


## Build35 — owner-authorized layout preview, 2026-10-30

The owner confirmed shorter Practice Description, expanded Reference Material and secondary controls moved toward pinned primary actions. Scope is layout only plus versionCode35 and existing permanent-key packaging. Explicit preview-only exception: compile debug/release, one API35 launch smoke check and verify signed package/identity/version/integrity; skip full units/lint/API26/35/regression/screenshot automation and unrelated legacy investigations. Added a separate manual owner-preview workflow; the normal android.yml and release verification rules are unchanged. Delivered as an owner-test preview with full regression and phone layout/data acceptance pending; CURRENT_STATE.md records actual evidence.


## Build36 — owner-requested compact reference, 2026-10-30

Owner accepted build35's expanded size and requested Reference Material small until clicked. Build36 starts with an Expand header, opens the approved area/media in one tap, and offers Collapse while retaining lower controls and existing scrolling. Only presentation/versionCode changed. Reused one manual preview compilation/API35 debug launch smoke run and permanent-key publisher. Full regression and owner compact/expand/collapse/data acceptance remain pending; normal release rules unchanged. Exact package/check evidence is in CURRENT_STATE.md.
