# practice.logger — First Usable Android Build Plan

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

Implement and verify owner-approved Stage 5 integrated metronome per sections 12–16. Stage 5 engineering is verified in 0.4.0/build 8: build, 24 unit tests, lint and full API26/35 suites passed, with permanent-key ZIP/APK delivered. Owner audible/timing and in-place phone acceptance remain pending; perform MANUAL_TESTS.md Stage 5 before declaring its complete exit gate. Stage 6A/6B are authorized later increments and must not begin in this increment. Preserve prior signed releases, permanent app/signing identity, old database data and legacy backup readers. Stage 3 phone acceptance remains pending and Stage 4 remains 0/10; the owner approved this explicit scope exception without claiming those gates completed. See CURRENT_STATE.md for exact current source/run/delivery and one next task.
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

**Current status (2026-10-26):** Stage 5 engineering verified in 0.4.0/build 8, source 4e08da13555696965094d74d5730ba73094329b6, CI https://github.com/mrayover/practice.logger/actions/runs/40439799999 . Signed dist delivery recorded in CURRENT_STATE. Phone acceptance pending; Stage 6 remains later work.

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

The existing version-2 JSON backup protects **database data, not audio files**. Do not silently redefine it as a media backup, and do not expose a misleading restore action. Each take must be exportable as a real file before Stage 6A is called complete. Before describing recording data as fully portable/protected, implement and verify a separate **audio-inclusive bulk export/restore path** (e.g. a versioned ZIP containing JSON manifest + audio) compatible with empty-target, transactional restore policy, or obtain an explicit owner decision to defer bulk restoration with prominently documented risk. That packaging is separate from the core metronome/loop playback feature and must not consume the initial implementation cycle merely because it is feasible. Confirm old backups and old app installations are never broken by format changes.

## 15. Mandatory documentation and delivery protocol for these stages

- **Do not reset or rewrite history.** Stages 0–3 remain as achieved; Stage 3 owner phone acceptance remains pending; Stage 4 real-session evaluation remains **0/10** until the owner performs actual sessions. The owner has now explicitly authorized Stages 5 and 6 before that evaluation is complete. Record this as a scope exception, not fabricated completion.
- Edit `BUILD_PLAN.md` **and** the conflicting scope sentences in `AGENTS.md` (approved metronome + basic Block recording only). Update `README.md` and `CURRENT_STATE.md` to reflect *planned* status first, then actual verified implementation as it occurs; append only to `WORK_LOG.md`. Do not delete historical entries, remove old source files or overwrite older ZIP/APK distributions.
- On every Work session, first read all five root operational records completely, inspect GitHub Desktop branch/uncommitted files/origin, review recent CI and code, then make the narrowest correct change. Git operations—including commit/push/fetch/pull—go through **GitHub Desktop**, not raw CLI or GitHub write API. Verify the actual remote `main` SHA, not merely local changes. If a cloud checkout/PR is used, clearly explain how the owner pulls/merges it rather than claiming it is in their Windows repo.
- Test with the existing unit, Robolectric, instrumentation, Android 8/15 CI, lint, backup/recovery and upgrade regression suites. Identify precisely what is untested on the owner's physical phone. Keep offline-first/no backend, the stable app ID, permanent signing key, schema migrations and Lato everywhere.
- Each deliverable gets a tested **installable signed APK** and versioned ZIP + extracted folder in ignored local `dist/` using versionName/versionCode/source SHA and existing signing workflow; keep prior releases, user data and key. If Work cannot access the owner's local machine/signing key, report what is blocked; deliver actual CI unsigned/debug artifacts without pretending they are replace-in-place signed releases.
- For every session—including planning-only or incomplete sessions—append `WORK_LOG.md`, refresh `CURRENT_STATE.md` with a verified current status and **one exact next task**, record errors/test outputs/CI links/source SHA/build identity, commit and push intended changes through GitHub Desktop if possible, and give a small phone checklist. Never treat an approved specification as an already implemented feature.

## 16. Immediate action

**Begin Stage 5, not Stage 6.** First integrate the approved change into the authoritative plan and agent rules; inspect current audio, BPM, lifecycle, backup and persistence code; implement a minimal metronome inside Focused Practice without interfering with the timer. Produce tests and the next reproducible APK. Only after this is verified, proceed to free Block recording, then the one-phrase looper. Do not expand into a tuner, overdubbing, Goals or Routines.

**Guiding criterion:** practice.logger should provide the click and the take where the Block is being practiced, not force the musician out into another app.
