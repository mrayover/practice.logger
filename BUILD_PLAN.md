# practice.logger — First Usable Android Build Plan

Current status (2026-10-28): Goals0.11.0/build20 finalized at owner request. Signed APK/ZIP available;57unit tests/build/lint pass. Owner reports app works and stops final testing; final matrix not claimed passed. See CURRENT_STATE.md. Stage3 detailed phone pending; Stage4 stays0/10.

Owner-approved scope 2026-10-26: unfinished practice must not block starting new sessions. Implementing always-available Practice Now and individually selectable unfinished sessions on Home; one foreground timer and one builder draft, no new screen/schema/dependency. Version 0.3.3/build 7 verification and signed delivery complete. Backup format 2 supports multiple unfinished sessions and reads legacy version 1; older apps reject new version 2 backups. Reserve remaining capacity for stable build/logs/push. Manual acceptance/evaluation remain deferred.

Owner instruction 2026-10-26: defer manual backup/recovery acceptance and the ten-real-session evaluation until this build cycle finishes. They remain unverified and do not block this authorized 0.3.2 note-reliability increment. General owner report: 0.3.0 works; no model/OS or individual checklist evidence supplied. Feature work stops by 10% usage remaining; prioritize stable signed APK and closing records. This cycle delivered verified 0.3.2; next test its unfinished-note save/leave/restart flow during normal practice, then resume owner-directed acceptance/evaluation later.

**Authority:** Current first-release build scope and stages  
**Status:** Stages 0–2 verified in CI on 2026-10-26; owner generally confirms 0.3.0 works; Stage 3 engineering verified on API 26/35, owner phone acceptance pending; Stage 4 evaluation prepared (0/10 real sessions)
**Repository:** https://github.com/mrayover/practice.logger  
**Owner workflow:** GitHub Desktop  
**Required application font:** **Lato throughout**  
**Updated:** 2026-10-26

## 1. What we are actually building

A small, reliable Android application that replaces the combination of a paper practice list and a phone timer, while retaining useful session history. It is primarily for one musician on one phone, not for public release, music education, a band, or a multi-user service.

**Success:** Open app → choose or create a few **Blocks** → arrange them → practice one at a time → save accurate results → inspect past practice. The loop should feel easier than using a notebook.

A **Block** is one repeatable, concrete instruction (for example, “Play the bridge of Along Came Betty from memory”). It is not an entire repertoire project or practice course. The normal intended duration is around five to ten minutes, without a hard limit.

The earlier full V1 document is **a future destination**, not the checklist for this release. The project must not build all 34 full-V1 items just because they are documented elsewhere.

## 2. Scope boundary

### Included

- Create, edit, find and reuse Blocks.
- Each Block has required **name**, **action**, default duration, and optional tempo/reference notes.
- Build a one-off session by selecting/reordering Blocks, changing the duration of a placement without changing its saved Block, and removing a placement.
- Practice one Block at a time with countdown, pause/resume, skip, early completion and post-zero count-up continuation.
- Save actual active practice time, Block outcome and optional short notes; preserve session position during interruption.
- Show a basic recent-session list and reopen session summaries; keep historical snapshots when a Block later changes.
- Fully offline local storage; safe export and restore of versioned data.
- Reproducible build, GitHub Actions APK artifact, tests, and an installable application on the owner's phone.

### Explicitly excluded for this release

Goals; saved reusable Routines; multi-face cubes; tuner; audio overdubs; backing tracks; PDF/YouTube embedding; randomization; notifications; algorithmic spaced repetition; deep analytics; accounts; cloud sync; backend; subscriptions; sharing; desktop app; integrations with other `.logger` apps; advanced animations.

**No menu item, card, or button may lead to unfinished future functionality.** The first release does not have to visually advertise the entire future roadmap.

## 3. Screens and their responsibilities

There are **six UI surfaces**; a Recent Practice list can live on Home.

### 3.1 Home

- Title: `practice.logger`.
- Primary `Practice Now` button.
- `Continue Session` shown **only when a recoverable unfinished session exists**.
- `Manage Blocks` button.
- A short recent-practice list with access to existing session summaries; optional small weekly total computed from real log data.
- Backup/restore may be tucked in a simple menu after Stage 3, not placed as a dead button during earlier stages.

### 3.2 Block Library

- List and simple search of existing Blocks.
- `New Block`; select a Block to view/edit.
- Support choosing Blocks from the Session Builder.
- Archive or delete safely without erasing historic practice sessions. Confirm destructive actions.

### 3.3 Block Editor

- **Name** and **Action** are mandatory.
- Default duration; optional numeric tempo (saved reference/default for the Stage 5 metronome) and note/source description.
- Clear Save / Cancel / Back behavior, obvious validation errors, reasonable defaults.
- Do not add elaborate categories, tags, attachments or nested editors now.

### 3.4 Session Builder / Practice Now

- Select existing Blocks; create a new Block using the same editor when necessary.
- Show current Block placements in order; rearrange, remove, and edit a placement's duration.
- Clearly separate changes to the current session from editing the reusable Block itself.
- Start enabled only with at least one Block. No named Routine saving.

### 3.5 Focused Practice

- One dominant Block at a time: **name, action, readable countdown, optional tempo reference**, current Block number.
- Large controls for **Pause/Resume**, **Complete**, **Skip**.
- When countdown reaches 00:00, it **does not auto-advance**. Offer `Continue` (track overtime as count-up) or `Complete`.
- Pause a session and later resume the correct Block, ordering, accumulated time and remaining timer state.
- On backgrounding, auto-pause to avoid counting time when interrupted; persist recovery state. Keep screen awake while actively practicing in foreground if appropriate. Log actual foreground active time, not nominal duration.

### 3.6 Summary + History Detail

- Completing a session automatically stores a summary.
- Show Blocks practiced/skipped, real active time, and optional quick session note.
- Previous session summaries can be reopened via Home's recent list.
- History stays truthful if a Block is edited/archived later.

## 4. Minimum durable data model

Use a conventional local database such as Room for a new Kotlin project. Keep it small and migration-aware.

- **Block:** stable ID, name, action, default duration, optional tempo/note, archived flag, created/updated timestamps.
- **Session:** stable ID, start/end timestamps, status (in progress/completed), optional session note, active elapsed time.
- **Session placement/log entry:** stable ID, session ID, source Block ID, fixed Block name/action snapshot, order, configured duration, measured active elapsed time, outcome (pending/completed/skipped), optional note and required recovery state.

Be explicit about timekeeping. Do not add elapsed wall-clock time while the app is paused or backgrounded. Preserve correct time when an Activity is recreated or Android terminates the process. Completed-session records must be immutable enough to reconstruct what was actually practiced at that time.

**Backup:** Use Android system document-picker APIs to export a clearly versioned JSON backup to a location chosen by the user. Restore must validate version/data before committing changes and protect against partial writes/accidental overwrite. Test export → clear test database → import → compare representative records. System file-picker appearance is controlled by Android, not by app typography.

## 5. Visual rules

- **Lato** is mandatory for all app-controlled text, from title to timer to dialogs, buttons, inputs, labels and lists. Bundle authorized Lato font files with the Android project and retain the applicable font license. Use Regular 400 / Bold 700 as the baseline. App must not require network font loading. Do not distribute font binaries as part of these starter documents.
- Respect earlier user-approved screen sketches when the actual images or approved visual descriptions are available. The previous Home sketch showed Goals and Tools, but those are *deferred*; their visual spaces should not become nonfunctional UI.
- Where visual details are undefined, use a restrained, accessible provisional design. Do not invent a large card system, graphic identity, gestures, animation style or decorative illustrations.
- Large, clear, reachable touch targets matter because the app is used while holding/playing an instrument. Avoid forced data entry after each Block; capture basic log entries automatically.
- Treat user testing as the design feedback loop: define → build → practice → revise → document.

## 6. Architecture and engineering guardrails

- Start from the user-created GitHub repository. If it is empty, initialize it in place, not by replacing it or switching to a new repo.
- Prefer Kotlin + Jetpack Compose + Room, AndroidX Lifecycle and the simplest workable architecture. Use stable toolchain versions available at implementation time. Do not overbuild a plugin system, internal event bus, dependency framework, background service or generalized `.logger` framework.
- Build without a developer environment on the owner's computer: GitHub Actions must run automated checks and publish an installable APK artifact at each completed build stage. Document download/install steps and expected Android sideload warning.
- Do not commit `.gradle/`, `build/`, `local.properties`, SDKs, keys, debug APKs or machine-dependent file paths as active config. Use GitHub Actions artifacts for APKs.
- Write targeted tests for Block persistence, session order/overrides, timer calculation/recovery, history snapshot behavior and backup round trips as these are implemented.
- An APK that compiles is **not** equivalent to functionality tested on a real phone. Mark device tests as pending until the owner confirms.
- Before marking a feature complete, demonstrate its real workflow; no stub controls, mock storage or data loss on restart.

## 7. Staged execution and exit gates

### Stage 0 — Foundation

**Build:** Initialize the Android project in `mrayover/practice.logger`; add/retain the root project docs; configure Lato across app UI; establish minimal navigation shell with no false destination promises; set up Gradle wrapper, `.gitignore`, unit-test runner and GitHub Actions debug-APK workflow.

**Exit:** CI green, APK artifact download available and app launch verified by the best available environment. A shell app is *not* a functional practice logger. Record exact CI run/limitations.

### Stage 1 — Functional Block catalog

**Build:** Home, Block Library, Block Editor, persistent database, create/edit/search/archive and safe history-conscious delete policy. All Stage 1 controls perform real operations.

**Exit:** APK usable for managing Blocks offline; tests cover create/edit/validation/reopen/persistence; works after process/app restart. User receives a short phone-test checklist.

### Stage 2 — Working practice loop

**Build:** Session Builder; selectable Blocks, placement order/duration; Focused Practice timer and controls; early complete, skip, overtime, interruption/resume; Summary, recent History.

**Exit:** Select three Blocks, complete one, skip one, pause/resume one, and review a saved accurate session after restart. End-to-end workflow actually works offline. **First practically usable test release.**

### Stage 3 — Trustworthy daily-use release

**Build:** Versioned backup/export and safe restore; data-integrity verification; migration/recovery testing; accessibility fixes; bug fixes from device feedback; reproducible release labeling.

**Exit:** Export/restore round trip passes; CI green; manual owner device checks confirm persistence/recovery; current open risks documented. **Only now call it ready for dependable daily practice.**

### Stage 4 — Real-world evaluation

**Do:** Use it for **at least ten real practice sessions**. Log pain points rather than guessing: how many taps to start, whether editing Blocks is tedious, whether timers help, whether notes and history are useful, whether sessions need flexibility.

**Exit:** Fix friction and decide what *one* future feature earns a place, if any. Possibilities include saved Routines, richer Block history, a metronome or a recording tool—**not all simultaneously**.

**Stage sizing:** Stage boundaries are delivery gates, not a demand to do an entire stage in one enormous Work session. Split a stage when necessary, but leave code building and history documented each time.

## 8. Functional acceptance checklist

- [ ] Installs/opens on the user's Android phone with no account or required internet.
- [ ] Lato appears in every app-controlled text surface; remains available offline.
- [ ] Can quickly create a Block with name, action and duration; invalid name/action cannot be saved.
- [ ] Can edit/search/archive saved Blocks; changes survive restart.
- [ ] Can choose three Blocks, reorder them, and override one duration only for that session.
- [ ] Timer correctly counts down, pauses/resumes, permits early completion, skip and post-zero count-up.
- [ ] Backgrounding does not inflate active practice time; interrupted sessions resume accurately.
- [ ] Completed sessions store accurate actual time and outcomes without forcing notes.
- [ ] Recent History opens past summaries, including accurate snapshots of edited/archived Blocks.
- [ ] Backup JSON exports and restores with validation and without silent data loss.
- [ ] No fake routes or dead controls; no app crash in common flows.
- [ ] Automated tests / Gradle build succeed; each stage has a downloadable CI APK artifact.
- [ ] `WORK_LOG.md` has an appended truthful account of each session; `CURRENT_STATE.md` reflects latest verified state; intended changes are committed and pushed or explicitly marked not pushed.

## 9. Documentation / continuity protocol

The repository root must retain **four operational files** plus this plan: `README.md`, `AGENTS.md`, `CURRENT_STATE.md`, `WORK_LOG.md` and `BUILD_PLAN.md`.

**At start of EVERY session:** inspect Git/CI; read `AGENTS.md` → `CURRENT_STATE.md` → latest `WORK_LOG.md` → `BUILD_PLAN.md`; inspect related code. **At close:** append a dated factual log entry, refresh current state, record tests/results and outstanding defects, commit and push if authorized, confirm SHA/remote and provide local GitHub Desktop sync instructions. Keep the repo buildable and do not erase earlier log entries. This requirement covers bug fixes and tiny changes as well as new stages.

The worker cannot make a new cloud-side GitHub commit automatically appear on `C:\Users\mrove\Projects\practice.logger`. GitHub Desktop must fetch and pull remote updates. Conversely, work left only in a local clone or an unmerged PR is *not* yet reflected on default branch. Never obscure this distinction.

## 10. Scope change policy

Do not add a new screen, feature, dependency or architectural layer simply because it seems useful or because it appears in the full V1. For proposals, record **problem observed → simplest solution → complexity/cost → decision**. Only approved changes to the first-release scope should modify this plan.

## 11. Next action

Owner generally accepts Stage 5 (2026-10-26: "it all works") and requests recording. Finish and verify Stage 6A free Block takes, following section 14/17, including saved takes, playback, confirmed delete and per-take export. Stage 6B count-in/phrase looping is a separate later increment. Preserve signing identity, data, prior releases and JSON compatibility; prominently state JSON excludes recordings. Stage 3 phone backup acceptance and 0/10 Stage 4 evaluation remain unchanged.
**Guiding rule: Build enough to practice. Practice enough to know what to build next.**

Approved 2026-10-26: observed unfinished-session lockout → always-available Practice Now plus individual unfinished-session selection on Home → one retained selection, same Room schema, format-2 backup semantics and focused tests → implemented in 0.3.3. No reusable Routines/new screen. That increment is delivered; current development task: Stage 5 metronome verification and signed delivery; manual backup/evaluation remain deferred.


## 12. Owner-approved scope change — direct practice tools

**Observed problem:** A numeric BPM reference in a Block forces the musician to leave the app to get an audible click; without recording and repeatable backing, the existing workflow is little more than a paper practice list plus timer. The application must help perform the musical work inside Focused Practice, not only log that work.

**Approved smallest solution:**

1. An adjustable, audible quarter-note metronome built directly into Focused Practice, functioning alongside—but independently from—the Block timer.
2. A simple microphone recorder **attached to each reusable Block**: free recording or one fixed-length phrase that starts after a four-click count-in and loops continuously for play-along.
3. Multiple individually retained takes per Block, with playback, safe delete and file export. No overdubbing.

**Cost/tradeoff:** Metronome timing must be audio-driven rather than UI-timer-driven. Phrase looping requires precise boundaries, microphone permissions, storage integrity and phone testing; it is materially harder than free recording. Phased delivery is mandatory. The owner is not concerned about audible metronome bleed, so do **not** add noise cancellation, automatic bleed removal, headphones detection or sound processing solely for that purpose.

**Product rule:** The current Block remains the practice workspace. The musician must not have to leave it to turn on a click, adjust BPM, capture a take, audition an earlier take or play an existing loop.

### Out of scope for these stages

No overdubs, synchronized layers, multi-track timeline, mixer, effects, waveforms, punch-in, trimming UI, tempo maps, time-signature setup, subdivisions, accent patterns, tap tempo, audio transcription, auto tempo detection, cloud storage, remote recording, desktop app, standalone DAW, background recording or background looping. No unrelated feature development while implementing these stages.

## 13. Stage 5 — Integrated metronome (FIRST IMPLEMENTATION TASK)

**Current status (2026-10-26):** Stage 5 engineering verified in 0.4.0/build 8, source 4e08da13555696965094d74d5730ba73094329b6, CI https://github.com/mrayover/practice.logger/actions/runs/40439799999 . Signed dist delivery recorded in CURRENT_STATE. Owner reports "it all works" and authorizes recording on 2026-10-26; Stage 5 accepted generally, without inventing phone model/OS or individual checklist results. Stage 6A begins now; 6B remains later.

### User-visible behavior

- In the existing Focused Practice screen, add a compact metronome area with **BPM readout**, **Start/Stop click**, easy `-1/+1` and/or `-5/+5` controls, and an accessible way to enter a precise BPM. Keep the Block instruction and practice timer visually dominant. Do not launch a separate app or navigate away.
- A simple unaccented click on each beat. **No meter selection.** Suggested validated tempo range: **30–300 BPM**, with clear bounds; document exact chosen limits if hardware implementation requires a narrower reliable range.
- Initial click tempo uses the current Block's saved optional BPM when valid; otherwise use a sensible documented default (e.g. 120). Changing working BPM must not silently overwrite the Block's default/goal BPM. Label and maintain this distinction.
- Start/stop and tempo changes work **while the Block timer is running or paused**; manipulating the metronome never silently starts/stops or resets that practice timer.
- Changing BPM while clicking takes effect promptly without requiring a screen change. The click has a reliable audible output, no drifting UI coroutine as the sole scheduling mechanism, and does not depend on network access.
- Auto-stop audible click when Focused Practice leaves the foreground, the user leaves/changes Blocks or sessions, or the Activity is being torn down. Resume silently/paused; **never unexpectedly play audio on cold launch/recovery**. Avoid leaking audio resources or running a background service.
- Persist the **last BPM actually used for the current practice placement** for historical reference (a single last-used value suffices; no tempo-change timeline). Prefer a snapshot in the placement/session outcome while keeping the reusable Block's goal/default untouched; implement a safe explicit Room migration and update the versioned JSON backup reader/writer if the persistent schema changes. Preserve existing 0.3.3 data and legacy format readers. An in-memory-only initial iteration must not be presented as fulfilling BPM-history persistence.
- Make all app-owned text Lato. Keep controls usable with large text and while holding an instrument; labels and controls accessible by screen readers. Avoid fake controls.

### Engineering and regression criteria

- Review the current Kotlin/Compose/Room app, `PracticeScreens.kt`, `PracticeViewModel.kt`, `SessionData.kt`, `BackupData.kt`, navigation and audio APIs before coding. Prefer Android platform APIs / minimal dependencies; avoid introducing a general-purpose audio engine abstraction for one click.
- Audio click timing should be driven by a stable audio clock/buffer where possible. Visual BPM displays can update independently. Handle rapid tempo changes and repeated Start/Stop without overlapping streams, hanging sound or crash.
- If a schema change is needed, preserve all Blocks, active sessions, drafts, history and notes via explicit migration and backward-compatible restore/version handling. Do not alter the permanently signed application ID or key.
- Automated checks: BPM validation/clamping, start-stop/tempo-change state, background/leave/recovery stop behavior, independent practice timer timing, migration/backup compatibility, and Compose UI access/navigation. Add device-level checks on existing CI API 26/35 where meaningful. Real click timing/audibility on the owner's phone remains a manual acceptance item and must be identified honestly.
- Deliver the next **higher versionCode**, passing builds/tests/lint/CI, and a consistently signed, versioned local ZIP plus extracted APK under ignored `dist/`; retain all old distributions. Use the existing `scripts/Publish-LocalApk.ps1` and permanent signing certificate. No automatic updater.

**Stage 5 exit gate:** On the phone, open an existing Block's Focused Practice screen, start the practice timer, start the click at the saved BPM, adjust BPM during playback, stop/restart click, continue practicing without navigating away, leave/reopen and verify silence until explicitly started; existing practice/history/data and upgrades remain intact. Automated checks must be green. Explicitly record owner phone checks as pending until actually performed.

## 14. Stage 6 — Minimal Block recorder (AFTER Stage 5 verification)

Split this stage into **6A free takes** and **6B single-phrase looping**, each ending with a tested runnable APK; never deliver unfinished on-screen actions.

### 6A — Free recording, playback and saved takes

- Recording is discoverable within the current Focused Practice Block, and the **same reusable Block's** saved takes remain accessible when editing/revisiting that Block (including archived Blocks where applicable). Use its stable Block ID, not mutable name or one particular session placement ID, as the owning association. Keep the UI compact; use a short inline area or simple Block recordings view/sheet only if needed, not a full studio.
- Request Android microphone permission only when the user tries to record. Handle denial, revocation, microphone in use, failure and low storage gracefully; show a real error instead of generating an empty/fake take.
- **Free Record:** tap Record, capture from the phone mic until Stop. A four-click count-in can be optional for free takes; recording itself must not require BPM. Provide an unmistakable recording state and elapsed time. The user is unconcerned about click bleed, so a running metronome may be heard and captured; do not attempt to remove it.
- Saving creates a new independently identifiable take with immutable stable ID, Block ID, created timestamp, mode (`FREE`), duration, locally referenced audio file and optional user-edited label. Never overwrite a prior take. Allow one-at-a-time playback, stop, rename, **confirmed deletion**, and export/share to a normal audio file via Android's file picker. Export failure does not destroy the source take.
- Use private app storage with safe file naming, atomic or carefully recovered writes and metadata consistent with actual files. Do not put audio bytes in Room rows or JSON text backups. Avoid orphaned half-written recordings or references to missing files; account for interrupted capture/process death.
- Keep the existing Block timer usable independently. Stop or safely finalize recording when leaving/backgrounding; **no background recording** or simultaneous independent capture streams. Audio focus/lifecycle rules should be documented, tested and kept simple.
- A Block may contain multiple free takes without a small artificial cap. Archive/restoration of a Block must not delete its audio. Existing historical Block snapshots and session recovery must still work.
- At a minimum, document that the existing **JSON backup does not include audio**, and implement clear per-take export. Do not claim full media restore yet. Avoid degrading JSON restore for existing metadata; if recording metadata is exported while audio isn't, never silently create playable-looking missing media.

**Stage 6A exit gate:** Record and save 2+ takes against one Block, restart app, play each and export one; existing Blocks/history survive upgrade; denial/interruption/restart paths handled. Android 8/15 CI runs and a signed local distribution delivered. Owner phone microphone/playback tests pending until actually reported.

### 6B — One fixed-length phrase, then loop playback

- **Loop Record** is a second option within the Block recorder. Set/confirm BPM and choose **4, 8, 16 or 32 beats** (simple beat counts, not a meter editor); show resulting expected recording duration. Defaults can be 8 beats and the Block/working BPM. Clamp/validate selection.
- Tap Record → audible **4 evenly spaced clicks** as count-in → start microphone capture on the next beat → capture **exactly one configured phrase** → automatically end capture and begin looping **only that captured phrase**. No manual endpoint trimming UI and no automatic recording of successive loops.
- Playback repeats the one phrase continuously until explicitly stopped or the user leaves/backgrounds. Provide Play/Stop for any saved loop; show its fixed BPM, beat count and recorded date. Preserve as a normal separate take attached to the Block, with mode (`LOOP`), recorded BPM/beat count and duration; multiple loops and free takes can coexist.
- Aim for seamless/gapless repeat. Treat input/output latency, device audio clocks and any residual boundary gaps/clicks as engineering constraints to measure on the owner's actual phone. Avoid MP3-style encoder-padding loops if they introduce gaps; choose a suitable playback format/buffer approach after inspecting platform support. A metronome can sound during recording/playback and may bleed; this is acceptable to the owner. Click/loop timing should be coherent. **Do not promise latency-free, quantized or perfectly seamless playback without real device validation.**
- Freeze the loop's BPM during capture/playback: changing the regular metronome must **not** silently change a recorded loop's speed or pitch. If the user wants another tempo, create another loop. No time stretching or overdub.
- Stop capture/playback safely on background, Block change, audio interruptions, microphone failures and route changes where needed. Leave previously saved takes untouched when a new recording fails.
- Add device/manual tests for count-in boundary, exact sample/beat duration, loop repeat continuity at e.g. 60/120/180 BPM, playback start/stop, app lifecycle, storage and prior free-take playback. Automated tests can verify frame-count/timing logic and navigation; they cannot prove perceived loop quality on the user's phone.

**Stage 6B exit gate:** On the phone, create an 8-beat loop at 120 BPM after four clicks, hear it repeat while playing along, stop/restart playback, leave/reopen the Block and play it again; a free take and second loop remain independent. No overdub layer or extra timeline. CI green, next higher versionCode and signed ZIP/APK saved under `dist/`.

### Audio portability and safe backup rule

The current format-3 JSON backup protects **database data, not audio files**. Do not silently redefine it as a media backup, and do not expose a misleading restore action. Each take must be exportable as a real file before Stage 6A is called complete. Before describing recording data as fully portable/protected, implement and verify a separate **audio-inclusive bulk export/restore path** (e.g. a versioned ZIP containing JSON manifest + audio) compatible with empty-target, transactional restore policy, or obtain an explicit owner decision to defer bulk restoration with prominently documented risk. That packaging is separate from the core metronome/loop playback feature and must not consume the initial implementation cycle merely because it is feasible. Confirm old backups and old app installations are never broken by format changes.

## 15. Mandatory documentation and delivery protocol for these stages

- **Do not reset or rewrite history.** Stages 0–3 remain as achieved; Stage 3 owner phone acceptance remains pending; Stage 4 real-session evaluation remains **0/10** until the owner performs actual sessions. The owner has now explicitly authorized Stages 5 and 6 before that evaluation is complete. Record this as a scope exception, not fabricated completion.
- Edit `BUILD_PLAN.md` **and** the conflicting scope sentences in `AGENTS.md` (approved metronome + basic Block recording only). Update `README.md` and `CURRENT_STATE.md` to reflect *planned* status first, then actual verified implementation as it occurs; append only to `WORK_LOG.md`. Do not delete historical entries, remove old source files or overwrite older ZIP/APK distributions.
- On every Work session, first read all five root operational records completely, inspect GitHub Desktop branch/uncommitted files/origin, review recent CI and code, then make the narrowest correct change. Git operations—including commit/push/fetch/pull—go through **GitHub Desktop**, not raw CLI or GitHub write API. Verify the actual remote `main` SHA, not merely local changes. If a cloud checkout/PR is used, clearly explain how the owner pulls/merges it rather than claiming it is in their Windows repo.
- Test with the existing unit, Robolectric, instrumentation, Android 8/15 CI, lint, backup/recovery and upgrade regression suites. Identify precisely what is untested on the owner's physical phone. Keep offline-first/no backend, the stable app ID, permanent signing key, schema migrations and Lato everywhere.
- Each deliverable gets a tested **installable signed APK** and versioned ZIP + extracted folder in ignored local `dist/` using versionName/versionCode/source SHA and existing signing workflow; keep prior releases, user data and key. If Work cannot access the owner's local machine/signing key, report what is blocked; deliver actual CI unsigned/debug artifacts without pretending they are replace-in-place signed releases.
- For every session—including planning-only or incomplete sessions—append `WORK_LOG.md`, refresh `CURRENT_STATE.md` with a verified current status and **one exact next task**, record errors/test outputs/CI links/source SHA/build identity, commit and push intended changes through GitHub Desktop if possible, and give a small phone checklist. Never treat an approved specification as an already implemented feature.

## 16. Immediate action

**Current action (2026-10-26):** Stage 5 is verified and generally accepted by the owner. Implement/verify Stage 6A free Block recording now as its own increment, including private file integrity, saved takes, playback/export, explicit migration and phone checklist. Stage 6B remains separate. Do not expand into a tuner, overdubbing, Goals or Routines.

**Guiding criterion:** practice.logger should provide the click and the take where the Block is being practiced, not force the musician out into another app.


## 17. Stage 6A active increment — 2026-10-26
Owner reports the metronome works and requests recording. Implement free microphone takes, retained per stable Block ID, with playback, rename, confirmed delete and per-take export in Focused Practice and saved Block editor (including archived Blocks). Keep timer independent; preserve click during capture. Stop/finalize on background/leave. Private M4A audio and Room metadata; explicit schema migration, recover incomplete writes. JSON backups continue protecting practice data only and exclude takes/audio; make this limitation prominent and do not claim bulk audio restore. Stage 6B is a later bounded increment. Build/test/lint/API26/35 and permanent-key signed versioned delivery required. Stage 3 acceptance/evaluation unchanged.

### Stage 6A engineering delivery — 2026-10-26
Verified 0.5.0/build 9 source dfdf83b194c95fb0847ae5a1705b35708be0ead4, CI https://github.com/mrayover/practice.logger/actions/runs/40447257853. All 28 unit/Robolectric tests, lint (0 errors/10 existing warnings), full API26/35 recording/reopen/backup/upgrade/accessibility suites pass. Native M4A capture and actual per-take document-picker export are exercised. Signed permanent-key ZIP and extracted APK retained in dist/practice.logger-0.5.0-b9-dfdf83b; previous releases preserved. JSON format remains 3 and excludes takes/labels/audio; Room schema is 4 with explicit migration. Next task is owner phone acceptance: install over 0.4.0, record two takes, restart, play both and export/play one outside the app. Physical microphone/output quality is unverified. Stage 6B implementation remains a separate increment after this handoff; no loops delivered here.
## 18. Stage 6B active increment — 2026-10-27
Owner generally accepts 0.5.0 free recording ("It all works") and explicitly requests phrase looping, followed by a save/storage discussion. No individual device results inferred. Implement section 14 6B now: 30–300 BPM, 4/8/16/32 beats, four audio-clock clicks, one exact sample-count PCM/WAV phrase, automatic fixed-tempo repeat, multiple independently saved takes and per-take WAV export. Preserve free M4A takes, prior data/migrations, identity/signing and all old releases. Interrupted incomplete phrases are discarded explicitly; full loops stop on background/leave/focus/route changes, never resume automatically. Existing practice timer stays independent. JSON exclusions and per-take export remain; no bulk media storage/restore redesign before the owner discussion. Updated 2026-10-27: 0.6.0/build 11 is implemented, CI-verified and locally signed; physical phone acceptance and Desktop publication remain pending. See the delivery record below. Stage3 phone acceptance and Stage4 0/10 unchanged.
Exact next action: implement and verify native count-in, exact phrase capture, repeated PCM playback, free-take coexistence, migration4→5, lifecycle/export/accessibility on API26/35, then publish signed versioned APK/ZIP and phone checklist.
### Stage 6B engineering delivery — 2026-10-27

Source faca5e5c01e0dfed9136dd58cf4a771b1362b6c8, successful CI https://github.com/mrayover/practice.logger/actions/runs/40566516233. All 32 unit/Robolectric tests, lint (0 errors/10 existing warnings), and full API26/35 suites pass, including exact native WAV frames, repeated playback, lifecycle cancellation, actual export/hash, migration and previous free takes. Normal/200% screenshots reviewed: short Phrase BPM label and range are readable; keyboard Done dismissal is not confirmed by captured images and remains a phone check. No source behavior changed during this delivery session.

Permanent-key 0.6.0/build 11 ZIP/APK: dist/practice.logger-0.6.0-b11-faca5e5. Publisher verified signature/certificate/package/version and ZIP extraction. Previous distributions including build 10 retained unchanged. APK SHA-256 cad7e7b4a85f1542388f4e4c3c309e0a86892ec325fe713f75a26b6b99d9aeea. CURRENT_STATE.md contains provenance and remaining limits. Root release records are local and uncommitted/unpushed because Desktop automation is unavailable.

Current next action supersedes earlier dated next-action paragraphs: review/commit/push the six changed documents in GitHub Desktop, then owner installs over the existing signed app and tests 8 beats at 120 BPM, ten repeats, independent retained takes, stop/reopen/export, keyboard dismissal and enlarged controls. Audible latency/boundaries remain unverified. Discuss save/storage afterward; implement no storage redesign yet. Stage 3 phone backup acceptance and Stage 4 0/10 remain unchanged.

## 19. Owner-reported loop recording friction — 2026-10-27

Observed problem: count-in stops before loop capture, making it hard to play in time. Owner explicitly asks for the click during loop recording, then the save/storage discussion. Smallest fix: a default-enabled "Click while recording" option in phrase setup; extend the same native output clock from four count-in beats through the selected phrase at its frozen BPM. Option off retains count-in-only behavior. Stop the guide before saved playback and on interrupted capture; preserve timer independence, exact WAV length, saved takes/data/signing and JSON exclusions. No separate free-running metronome/audio-focus client, no overdub, digital click mix or storage redesign.

Prepared 0.6.1/build 12 (upgrade test 13). Added optional-guide PCM tests, native output-counter checks during capture for enabled/disabled modes, checkbox interaction/200% accessibility, and retained cancellation/timer/export/reopen assertions. Three isolated PCM/WAV JUnit tests pass with cached Kotlin; full Android/Compose/native CI and signed delivery are pending. Local/remote baseline f6e38e0 was clean. Changes need owner Desktop commit/push because this session has no computer-use tools. Exact next task is that push, followed by CI/screenshot verification and permanent-key signed APK/ZIP. Owner generally accepts build 11; detailed new phone checks follow the update. Stage 3 backup acceptance and Stage 4 0/10 remain unchanged.

### Recording-click fix verified and delivered — 2026-10-27

Source f4f827bde0921b805b4f3126555344507bb0e3fd / https://github.com/mrayover/practice.logger/actions/runs/40603962994 passes build, all33 unit/Robolectric tests, lint0errors/10existing warnings and full Android8/15 device suites. Default-enabled optional click continues from count-in through capture on the same output clock and ends before saved playback; disabled capture has count-in only. Native counters, on/off, cancellation, exact frames, timer, retained free/loop takes, export/reopen and200% checkbox checks pass; screenshots reviewed. Earlier failures were test synchronization issues retained in WORK_LOG and corrected without changing production audio after5e86e31.

Permanent-key0.6.1/build12 ZIP/APK retained at dist/practice.logger-0.6.1-b12-f4f827b with BUILD_INFO.json/INSTALL.txt; LATEST updated. APK SHA256 31d38af9325d6fda5252fa2fd69b927cfc4b352111b769548bd9ccf4984a6445. Signature/package/version/ZIP extraction verified, all28 prior release files unchanged. Owner-authorized Git-command exception used for this update/closeout. Exact next task: phone check default-on click during8beats120BPM, optionoff, cancel/background and retained takes, then discuss save/storage before implementation. Stage3 phone backup acceptance pending; Stage4 remains0/10.

## 20. Owner-approved home sketch — 2026-10-27

Observed request: home must visually match the supplied drawing now, without requiring future Goals/tools to function. Approved solution: centered Lato Bold practice.logger on separated green header, white body, blank Goals panel plus inactive settings/Edit, five stacked green buttons Practice Now/Blocks/Routines/Tools/Recording, and plain Data usage footer. Explicit owner instruction overrides the earlier no-placeholder control restriction for this visual scope only. Do not add fake Goal data, storage numbers or functional deferred features.

Use existing destinations: session builder, Block Library, unfinished-session/history overview under Routines, backup under Tools, and per-Block recorder selection under Recording. Preserve access to all previous practice/history/backup/audio features. No schema, migration, audio-engine change or added dependency. Version0.7.0/build13; CI upgrade14. Adapt actual navigation tests and retain all native assertions; collect normal/200% screenshots for real visual review. Full CI and signed delivery pending, no phone completion claimed. Default Desktop workflow still applies unless owner extends the prior limited Git-command exception to this concrete redesign. Save/storage discussion remains deferred until after the requested visual update.

### Home sketch engineering delivery — 2026-10-27

Source 61b76d50e1ce0dc995cf4775d6d1840a5d10f007 / https://github.com/mrayover/practice.logger/actions/runs/40620587011 passed build,33unit tests, lint0errors/10warnings and full Android8/15 home/catalog/practice/recovery/history/backup/recording/loop/upgrade/200% suites. Normal/large home screenshots reviewed against owner's image. Green centered bold Lato header, white body, blank Goals/inactive Edit/settings, green five-button stack and Data usage footer implemented. Existing routes/data/audio preserved under the new menu; no functional deferred Goal/routine/storage feature added.

Signed permanent-key0.7.0/build13 ZIP and extracted APK at dist/practice.logger-0.7.0-b13-61b76d5; publisher verified signature/certificate/package/version/ZIP extraction. APK SHA256 2e66741ae596ed76c0cf15c9755847f28d8c6f327392c8716b5e36c640cbd144; all30 prior release files unchanged. Owner-authorized Git-command exception used for this redesign and closeout. Next: owner checks visual/layout/navigation on phone, then desired save/storage discussion. Stage3 manual backup acceptance and Stage4 0/10 unchanged.

## 21. Owner-approved persistent banner and standalone Tools — 2026-10-27

Current action supersedes older next-task paragraphs: implement0.8.0/build14 after general owner acceptance of0.7.0. Root green centered Lato Bold banner remains above every app route; system pickers/dialogs are Android overlays. Move Continue Session and individually selectable unfinished sessions from Routines to Practice Now, preserving new-draft creation and independent paused recovery. Routines retains existing recent history.

Tools gets matching green Metronome/Tuner/Timer buttons plus retained Backup and restore. Metronome reuses sample-clock audio30–300BPM, centered single number, ±1 row then ±5 row, Start/Stop. Chromatic tuner analyzes live mono PCM in memory using YIN, A4=440Hz,50–1600Hz, RMS/confidence gating, note/octave/frequency/cents and flat/sharp/in-tune labels; no fake pitch or saved capture. Permission only after Start tuner, then explicit second Start; stops on background/leave/recreation. Timer has independent stopwatch Start/Stop/Reset and snap wheel1–180 whole minutes with Start/Stop/Reset countdown. Monotonic timing; tool timers pause on exit/background/recreation, no background alarm or persistence after process death. Tool values survive ordinary Activity recreation paused.

Verify pitch over sample rates/range/harmonics/detuning/silence/noise, monotonic paused intervals, native mic sample reads, live click/tempo controls, rolling selection, clock progression/cleanup, all old recovery/history/backup/native audio suites and200% text on API26/35. Review native screenshots, permanently sign/version package and preserve prior distributions. Acoustic accuracy and physical audibility remain phone checks. No schema/dependency/key/storage/backup-format change. Stage3 phone acceptance and Stage4 0/10 remain unchanged. Monitor account usage; stop feature expansion by10% remaining.
### Tools engineering delivery —2026-10-27

Sourcec96b903c6617f0351baf99202e262cadc0a0696c / https://github.com/mrayover/practice.logger/actions/runs/40629201227:38unit tests, lint0errors10warnings, fullAPI26/35initial+18phases/200% tools pass. Normal API26/35 and 200% tool screenshots were visually reviewed: persistent centered banner, green menu/tempo buttons, microphone Listening state, rolled minute selector and zero/Time’s up. Large text scrolls to its controls; API35 status backing stays green after background/resume (sampled #315C47). Permanent-key0.8.0/build14 under dist/practice.logger-0.8.0-b14-c96b903, verified certificate/package/version/ZIP hash; all31 baseline files unchanged. No schema/storage change. Owner approved command exception for publication/closeout; phone tuner accuracy, click audibility, foreground minute completion and navigation acceptance remain pending. Next task is exact phone checklist in CURRENT_STATE, then save/storage discussion. Stage3 pending/Stage4 0/10 unchanged.

## 2026-10-27 — Cream background and standalone recording increment

Owner explicitly requests light cream throughout app backgrounds and a Free recording block in Recording that starts a free take immediately and keeps all standalone takes together using existing Block recorder infrastructure. Implemented cream#FFF8EB across Material surfaces/dialogs/fields, Home, navigation and launch background; green banner/buttons/Lato retained. Recording has Free recording(one tap starts capture or requests mic then starts after grant) and Saved free recordings(browse without capture). Shared recorder retains Stop/save, playback, rename, confirmed delete/export and lifecycle cleanup. Phrase loop controls remain in Blocks only. No synthetic Block is created.

Room6 explicit5→6 rebuild makes take.blockId nullable, preserves every old take field/Block foreign key/index and all session data. Null owner denotes freestanding takes; active-screen guard distinguishes standalone recording from a closed screen. JSON3 still excludes all recordings/labels/metadata; per-take export retained. No bulk storage/backup redesign or new dependency/key/appID. Goals/routines remain deferred; Stage3 phone pending, Stage4 0/10. Target0.9.0/build15, upgrade16.

Prepared two meaningful Robolectric tests for frozen schema5 upgrade (free/loop takes, notes, FK enforcement) and standalone file save/reopen/rename/export/delete isolation. Two native phases test one-tap actual M4A, two independent takes, recreation save/no restart, playback/rename/browse and cold reopen without creating/changing Blocks or linked takes. 200% free-list controls/screenshots added; all prior suites retained. Full compilation/lint/unit/API26/35 pending CI; compiler-generated schema6 export must be committed before final verification. No new APK yet.
## 0.9.0/build 15 — verified signed delivery, 2026-10-28 America/Los_Angeles

Light cream #FFF8EB covers app backgrounds, Material surfaces/dialogs/fields, launch and navigation; green banner/buttons and Lato retained. Recording → Free recording starts native M4A capture immediately when permission/storage/foreground are ready, or after microphone permission is allowed. Saved free recordings opens the same collection without capture. Shared Block recorder provides Stop/save, playback, rename, confirmed delete and per-take export. Rotation/background/exit stops and saves usable capture without starting another; free takes remain together, separate from Block takes. No synthetic Block is created; phrase controls remain in Blocks.

Room schema6 explicit5→6 migration preserves old take IDs/files/labels/tempo/beats, foreign keys/indexes and all practice data; nullable blockId denotes standalone ownership. Compiler-generated6.json committed. JSON3/legacy readers unchanged and still exclude ALL takes/labels/audio, including standalone: export wanted audio separately. No new dependency, appID or permanent-key change; broader storage/backup, Goals/reusable routines remain deferred. Stage3 phone acceptance pending; Stage4 stays0/10.

Verified app/test source: e1c884c91faa26576de8765296e6f203399df8c5
Successful CI: https://github.com/mrayover/practice.logger/actions/runs/40634982208
40 unit/Robolectric tests pass, no failures/errors/skips; lint0 errors/10 warnings. Both Android8/API26 and Android15/API35 pass initial workflow plus20 direct phases, including new standalone native capture/recreation/rename/playback/cold reopen/isolation checks, frozen schema5 upgrade/file safety, all prior practice/backup/recording/phrase/tools/upgrade and200% checks. Upgrade code16. Normal Android8/15 free-take lists and cream Home plus 200% free-recording controls visually reviewed; cream surfaces, retained green branding, two saved takes and shared playback/rename/export/delete controls are visible. The first menu-named screenshot captured the preceding Home frame; automated button navigation/capture assertions and actual saved-list screenshots verify the recording destination. Physical phone/audio quality acceptance remains pending. No local Android SDK/emulator installed.

ZIP: dist/practice.logger-0.9.0-b15-e1c884c.zip
APK: dist/practice.logger-0.9.0-b15-e1c884c/practice.logger-0.9.0-b15-e1c884c.apk
APK SHA256: 1a6a9ffcda4cc80938d4b43491040f88b72dc28758eb5445dd2fd47e1810f42f
Permanent certificate: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830
Existing publisher verified signature/pinned certificate/stable com.mrayover.practicelogger.app/version0.9.0/code15/ZIP extraction. All33 prior baseline APK/ZIP files unchanged; LATEST updated after packaging. Private key unchanged/not uploaded. Install over current permanent-key app without uninstalling.

CI artifacts (30-day retention):
- practice-logger-debug: 12793865029; sha256:27a782946535999d6d8376d31fe58db74b4e4e3fae317b74d490710e1630acd5
- practice-logger-unsigned-release: 12793620700; sha256:2dfa49348b3802926c38920c3803fbf984360b8b8a8987f8d51357e9e588e7d8
- device-test-reports-api-35: 12793436676; sha256:f4e1bb520720baefb91b8ed31b2f79bb12af2a1151bf85d2ef1903eca600b358
- verification-reports: 12793062805; sha256:be6b4322679508c7ebf28fa5fd9b6c2c076160e91a316c02a5b6fc92e4efbe60
- device-test-reports-api-26: 12792904844; sha256:95863457a8735344b510036bcfc466dbe7b86ac8d0244af08648910fbb0cd517

Owner authorized Git commands for this update/schema/closeout because Desktop controls unavailable; default Desktop preference retained. Existing Windows checkout, synced sources untouched; app source/schema pushed, closing records publish separately with local/remote clean status verified in handoff.

Exact next task: owner installs signed0.9.0/build15, checks cream screens/dialogs, taps Free recording and saves two takes, listens/renames/exports, browses Saved free recordings without mic, restarts/rotates/backgrounds and confirms old Blocks/session notes/free/loop takes untouched. Report phone/Android/mic route/results; discuss broader save/storage afterward. JSON still excludes audio.
## 2026-10-28 — Saved Routines approved; preparation and design

- Owner supplied complete Implement Saved Routines request: reusable named ordered stable Block references with per-placement durations; Save as Routine from builder; actual management CRUD/reorder/duration/start; preserve Recent Practice/history and existing engine/data/audio/design; Room migration and backward-compatible JSON routines; full tests/API26/35/signed APK and ongoing records. Routines only; no Goals/recording/storage redesign. This explicitly supersedes reusable-routine deferral. Desktop remains default; no blanket command mutation authorization inferred from earlier releases.
- Read AGENTS/CURRENT_STATE/README/BUILD_PLAN/WORK_LOG completely in bounded sections, plus BACKUP_FORMAT/MANUAL_TESTS and session/practice/navigation/backup/schema/migration implementation. Existing checkout clean at local/remote main9578b46a0317a21295b3fcc41d3a0ed3e6df61e2; previous app sourcee1c884c/CI40634982208 fully successful, release0.9.0/build15. Existing Windows checkout retained; synced sources read-only and untouched. No new repository/worktree. No computer-use/Desktop tool is callable; publication choice will be resolved after concrete implementation/review, before any Git mutation.
- Design: Room7 adds routines and routine_placements with stable Block FK/RESTRICT and routine FK/CASCADE. Store only Block IDs/order/durations, not copied library data. Save builder selection transactionally without changing draft/Blocks. Routine management retains Recent Practice below saved routines. Direct Practice creates a fresh paused IN_PROGRESS session with current Block snapshots/saved durations via existing SessionRepository/PracticeViewModel; preserves builder draft and other unfinished sessions. Session notes/time/BPM/history stay independent; deleting routine only cascades its own entries. Duplicate Block placements supported. Archived/unavailable references are explicit and block starting the entire routine; editing/removal/restoring Block repairs it, no silent omission.
- Planned JSON4 includes routine/placement arrays; readers1–3 create no routines. Validate IDs/references/order/positive durations/nonempty names/routines and existing bounds before transactional empty-target restore. Empty target checks all persisted data, including standalone takes; JSON still excludes all recording metadata/labels/audio. Version target0.10.0/build16, upgrade17. No identity/key/dependency change. Stage3 phone acceptance pending/Stage4 0/10 unchanged. Implementation/tests/CI/new APK pending.
- Exact next task: implement routine repository/editor/builder-save/session-start/backup4 and focused tests, update records at milestones, publish using authorized workflow, retain compiler Room7 export, run full build/lint/unit/API26/35, review screenshots and deliver permanent-key versioned APK/ZIP preserving old releases.

## 2026-10-28 — Saved Routines implementation checkpoint (not yet CI-verified)

Implemented Room7 routine/entry entities, explicit additive6→7 migration, transactional CRUD and builder Save as Routine, saved-state editor, retained Recent Practice, confirmed deletion and explicit unavailable/archived start blocking. Practice Routine uses current Blocks/saved durations to create independent paused sessions through existing engine while preserving builder draft. JSON4 export/validation/atomic restore includes routines/entries and retains readers1–3/audio exclusions; nonempty guard includes standalone metadata. Target0.10.0/build16, upgrade17; identity/key/dependencies unchanged.

Prepared11 Robolectric tests for CRUD/file reopen/duplicates/draft isolation/current Block snapshots/repeated starts/archive-missing safety/invalid writes/frozen schema6 migration/audio-byte preservation/JSON4+legacy/invalid backup/rollback/nonempty guards. Added two native routine workflow/cold-reopen phases, real document-picker routine fixture, and200% editor/save dialogs. All existing regressions retained. Static diff review passed; Android compilation, full51-test suite, lint/API26/35, generated7.json, screenshots and new signed APK still pending CI. Physical phone acceptance pending. README/BACKUP_FORMAT/MANUAL_TESTS updated. No Git mutations performed; Desktop default retained and publication exception has not yet been requested.

Next: publish prepared change using owner-authorized workflow, commit compiler-generated schema7 and run full checks, fix any failures, inspect screenshots and sign/package preserving all35 prior APK/ZIP files.


## 0.10.0/build16 — Saved Routines verified delivery, 2026-10-28 America/Los_Angeles

Implemented reusable named ordered Routine templates with stable Block references and independent placement durations. Builder Save as Routine leaves draft/Blocks unchanged and starts no practice. Home → Routines provides create/edit/rename/add/remove/reorder/whole-minute duration/confirmed delete/Practice Routine above retained Recent Practice/history. Editor state survives recreation; Back confirms unsaved discard. Duplicate Block placements work. Missing/archived references are explicit and prevent the whole start; edit/remove/replace or restore the Block, never silently skip.

Changed components: new RoutineData.kt, RoutineViewModel.kt, RoutineScreens.kt, RoutineRepositoryTest.kt, RoutineBackupTest.kt and RoutineWorkflowTest.kt; integrated BlockDatabase.kt, SessionData.kt, PracticeViewModel.kt, PracticeScreens.kt, MainActivity.kt, BackupData.kt and BackupScreen.kt. Extended SystemPickerTest.kt and AccessibilityWorkflowTest.kt; synchronized existing LaunchTest.kt; updated app/build.gradle.kts, android.yml, generated7.json and all seven project documents. Publisher, signing material, audio implementation and original prefixed plans untouched.

Practice Routine creates a fresh paused IN_PROGRESS session using current Block name/action/tempo/reference notes and saved Routine durations through existing SessionRepository/PracticeViewModel. Each start gets independent session/placement IDs, time, notes, BPM/outcomes. Builder draft and other unfinished sessions remain; later Block/template changes and deleting a template cannot rewrite earlier sessions/history. Existing timers/overtime/Complete/Skip/click/Block free/loop recordings/notes/summary/recovery reused.

Room7 explicit6→7 adds routines/routine_placements and FK/indexes only; compiler-generated7.json identity752227f5c28e47ea770144988e8d8cab committed. Compared schema6→7: all four original entity definitions unchanged. Existing migrations retained, no destructive fallback. JSON4 includes routine/entry arrays and IDs; validates names/timestamps/order/references/durations and restores everything atomically into an empty target. Legacy1–3 readers retained; future versions rejected. Nonempty guard covers Blocks/sessions/routines/takes, including standalone-only metadata. All take audio/labels/metadata still excluded; export audio individually. Cream#FFF8EB, green#315C47, bundled Lato/persistent banner, appID/minSDK26/target36/permanent key unchanged; no new dependency/recording/Goals/cloud/storage redesign.

Verified source: aa78783e9b264dccec64018362b1a88cf0f5e255
Successful CI: https://github.com/mrayover/practice.logger/actions/runs/40733036079
51 unit/Robolectric tests pass,0 failures/errors/skips (11 new Routines tests); lint0errors/10existingwarnings; debug/release build pass. API26/Android8 and API35/Android15 each pass initial catalog workflow plus22 direct phases, including two new native Routine CRUD/builder-save/repeated-start/current-Block/archive/delete/recreation/cold-recovery checks, real Android picker JSON4 routine export/restore/reopen and200% routine editor/duration/save dialogs. All prior catalog/practice/timer/overtime/multiple-session/notes/history/upgrade/backup/click/native-recording/phrase/standalone-tools/free-recording suites retained and pass. Upgradecode17. Normal Routine list/editor/fresh practice/recovery/history and200% screenshots visually reviewed; cream/green/persistent banner retained and controls readable/scrollable. Automated emulators are not physical phone acceptance; real audio/phone/TalkBack/provider behavior remains owner checklist work. No local Android SDK/emulator installed.

ZIP: dist/practice.logger-0.10.0-b16-aa78783.zip
APK: dist/practice.logger-0.10.0-b16-aa78783/practice.logger-0.10.0-b16-aa78783.apk
APK SHA256: d5fc5f96ccb57ee88f3364fd8a74d2d15ee2e18f02b8622af27642a08c28a2c5
Permanent certificate: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830
Existing publisher verified signature/pinned certificate/stable com.mrayover.practicelogger.app/version0.10.0/code16/ZIP extraction; unchanged private key never uploaded. All35 prior APK/ZIP files rehashed unchanged; versioned ZIP/extracted folder retained and LATEST updated. Install over existing permanent-key app without uninstalling. Original0.1.0 remains separate.

CI artifacts (30-day retention):
- practice-logger-unsigned-release: 12836875419; sha256:baacda1d92da9f5bd8ef6d4e988e509e1acf6719674e9ea0e3f4ccc0deaa0495
- device-test-reports-api-26: 12836742349; sha256:99d13561fa7f85bf4807c319fecb7df4bd018aa62f905f09a5c8eeafc3957ae2
- practice-logger-debug: 12836417224; sha256:04a52d3a575fe057d879acf80572003fcd423e7ee3f163aa18f591bc110e7de1
- verification-reports: 12836332219; sha256:8f9267b1408ba97bbec57c2a20c55d9f0def3faf64a957f85d219b814953f803
- device-test-reports-api-35: 12836072808; sha256:52bfa41538c6d31d6428b0588329908aa8c50e59688a7087b495e3512e5e4040

Development history: f2b6d02 initial implementation/51-test build;8781678 compiler schema milestone. Initial device jobs were cancelled by newer schema-source verification, not counted as passing. Verification exposed catalog save/navigation/rotation and large-text target-readiness races. Retained every workflow assertion and both recreations; added exact-target/displayed-row waits and waited for committed navigation before rotation. Temporary forced-scroll/IME probes were removed. Bounded catalog timeout plus thread/screenshot diagnostics captured Espresso idle polling and undrawn row; final aa78783 clears editing focus/hides IME before catalog and Routine-editor save navigation. Final API26 catalog completes13.092s and API35 completes12.921s; both initial+22 phases pass. Earlier failed/cancelled runs are retained in WORK_LOG and never counted as passes. Owner's final steering prioritized immediate build: signed/package completed while final device results were arriving; all final gates are now independently confirmed successful.
All seven project documents updated (AGENTS,README,BUILD_PLAN,CURRENT_STATE,WORK_LOG,BACKUP_FORMAT,MANUAL_TESTS). WORK_LOG earlier entries preserved. Owner explicitly authorized Git commands for this update/schema/fixes/closeout; Desktop default remains for future work. Actual existing Windows checkout edited; synced sources untouched. Source/test/schema published to main; closing documentation published separately, final local/remote SHA and clean state verified in handoff. No owner phone results fabricated; Stage3 phone acceptance pending, Stage4 stays0/10.

Exact next task: owner installs signed0.10.0/build16 over current app and executes Saved Routines phone checklist in MANUAL_TESTS.md: builder save/no mutation; CRUD/reorder/durations/restart; two independent starts and existing draft; current Block changes/archive handling; save-for-later/cold recovery; confirm old notes/history/free/loop/standalone audio; JSON4 export/spare-empty restore/legacy import;200%/TalkBack. Report phone/Android/results. Address any feedback before choosing further feature scope.


## 2026-10-28 — Goals approved increment

Owner explicitly authorizes Goals as the next increment and all necessary commits/pushes using Git commands for this session. This supersedes earlier Goals deferrals only. Target 0.11.0/build17, upgrade18. Five active Goals, manual progress, priority/top-two Home, completed/reopen, shared existing Blocks, independent Goal practice through the existing engine, explicit session attribution and actual committed time. Room8 additive migration and JSON5 (read1–5), protected transactional empty restore. Preserve identity, audio, earlier distributions and all existing regression gates. No deadlines, automatic scoring, cloud or unrelated expansion. Stage3 owner phone acceptance remains pending; Stage4 remains0/10. Follow the mandatory efficiency procedure: reuse, early candidate, targeted evidence before full reruns, reserve verification/signing/documentation capacity.

Goals candidate0.11.0/build17/2980d8e was permanently signed early after56unit tests/build/lint passed; API26/35 verification pending. Preserve candidate as such. Final target advances to0.11.0/build18 (upgrade19) for reviewed editor-restoration/backup refinements and compiler schema8. No phone acceptance inferred.

2026-10-28 final owner instruction: "The app works. Stop testing and finalize this output." Goals0.11.0/build20 sourcec0b1dfe5c8d9f3f0f8ef8084a741963e18eb3bb4 finalized with permanent signing,57unit tests/build/lint and focused native Goal/200%checks passing; final full device matrix stopped at owner request, not claimed passed. All prior data/audio/key/releases retained. See CURRENT_STATE.md. No further development/testing requested; use the delivered build and respond only to observed feedback. Stage3 detailed phone acceptance pending; Stage4 stays0/10. Current-session Git commands/pushes explicitly authorized; future Desktop default retained.
