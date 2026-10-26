# practice.logger — First Usable Android Build Plan

**Authority:** Current first-release build scope and stages  
**Status:** Stages 0–2 verified in CI on 2026-10-26; owner confirms 0.1.0 works, Stage 2 phone checks pending; Stages 3–4 remain unimplemented  
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

Goals; saved reusable Routines; multi-face cubes; metronome; tuner; recording and audio overdubs; backing tracks; PDF/YouTube embedding; randomization; notifications; algorithmic spaced repetition; deep analytics; accounts; cloud sync; backend; subscriptions; sharing; desktop app; integrations with other `.logger` apps; advanced animations.

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
- Default duration; optional numeric tempo (reference, not an actual metronome) and note/source description.
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

Stages 0–2 have passed automated build/tests and emulator workflows. Next: complete the Stage 2 phone checklist in `MANUAL_TESTS.md`, record results, resolve practice-loop issues, then begin Stage 3 versioned backup/export and safe restore. Versioned ZIPs and extracted APKs are retained in ignored `dist/`; permanent local signing supports future in-place updates to the new app identity while preserving the original 0.1.0 catalog separately. `CURRENT_STATE.md` and `WORK_LOG.md` contain verified run, APK and source-commit evidence. `FIRST_WORK_HANDOFF.md` is historical.

**Guiding rule: Build enough to practice. Practice enough to know what to build next.**
