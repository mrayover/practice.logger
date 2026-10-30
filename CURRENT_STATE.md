# Current state — Workspace/footer visual preview,0.16.0/build41

Updated 2026-10-30 (America/Los_Angeles). **Signed, installable limited owner preview; full regression pending.** This assignment is a limited visual preview: Description defaults open, one workspace stays expanded, Notes moves to the footer and Save for later is removed. Existing Room11/JSON7 and notes behavior remain. No later audio acceptance workflow or unrelated feature started. No further automated work scheduled.

## Delivery

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code41 |
| Signed app source | `6a49903adedd78cd21d09a9ec29cfca955de048e` |
| Limited preview | [41139176536](https://github.com/mrayover/practice.logger/actions/runs/41139176536) |
| Workflow status | completed / success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `d9165b2b5c17b155bd622a6141fb7ff015e4d3f8608adfdcdc093c84a237ece6` |
| ZIP SHA256 | `8523fffc721b7574318dcf313fbb12b1e363c96e9e90853e1f7773ae93e944e6` |

[APK](dist/practice.logger-0.16.0-b41-6a49903/practice.logger-0.16.0-b41-6a49903.apk) · [ZIP](dist/practice.logger-0.16.0-b41-6a49903.zip) · [build info](dist/practice.logger-0.16.0-b41-6a49903/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b41-6a49903/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b41-6a49903/PHONE_CHECKLIST.txt) · [latest](dist/LATEST.txt).

Install over the permanent-key app without uninstalling/clearing data. Existing publisher verifies signature, pinned certificate, identity/version and APK hash. Final ZIP CRC/member audit passes; all 154 prior APK/ZIP files match their pre-work hashes. Packages remain local/ignored. Application code remains identical to signed source; schema11 is unchanged.

## Behavior

Build41 changes only workspace/footer presentation and versionCode. Description opens by default, including Blocks with attachments. Tapping either header switches sections so one always remains expanded outside temporary panel suppression. Notes is an outlined footer pill beside Start/Pause/Resume, Complete and Skip. Removing the former Notes/Save for later row lowers the transport by that row. At font scale1.5 and above footer actions wrap into two rows. Banner/Android Back retain the existing checkpoint/save/unfinished-session behavior. PDF state retention, notes guards and audio ownership are retained.


Build39's single Quick note/Save and return is replaced by Add New Note and newest-first Block history across sessions. Save Note appends an independent timestamped entry, clears only after commit, updates history and keeps Notes open. Entries expand/collapse complete text. Return to Practice closes separately. Save/Return actions are pinned outside history scrolling; overflow retains a visible thumb.

Expanded saved entries offer Edit, Save Changes and Cancel Changes. Both new and legacy notes can be edited. Only text changes; new-note creation time and Block/session/placement links remain. Legacy notes have no original note-creation time, so their header is explicitly marked Earlier session and uses the session ended/start date. Edits do not change that session date or practice metrics. No note deletion. Unsaved new/edit drafts use SavedStateHandle across panel switches/recreation; leaving via Back/banner or Complete/Skip offers Keep editing or explicit discard. Abrupt process/task removal can still lose unsaved text; committed notes remain durable.

Opening Notes, free/loop selection or loop setup temporarily suppresses both upper contents while keeping both headers visible. Direct panel switches keep suppression; final close restores the prior Description/Reference choice. BPM dialog also suppresses upper expansion. The reference subtree is kept measured but unplaced/hidden from accessibility rather than disposed, preserving PDF page/zoom/scroll state across panels; after first opening it is also retained across manual workspace switching. Expanded transport uses the freed space. Controls can scroll within their compact area on constrained displays; active capture status and Stop remain in the recording row while a panel is open, with the original pinned capture strip retained otherwise.

## Persistence and compatibility

Room11 adds only `practice_notes`: id, blockId, sessionId, placementId, createdAt and text, restrictive foreign keys and indices. Migration10→11 creates an empty table; all schema10 tables/rows stay intact. Read-through history combines original nonblank placement notes with new rows using separate internal keys. No backfill/copy or duplicate legacy entries. New/edited notes do not overwrite Block instructions/reference notes, overall session notes or daily Log.

JSON writer7/readers1–7 adds practiceNotes with strict ownership/nonblank/timestamp/ID validation and atomic empty-target restore. Versions1–6 restore legacy placement notes and no new rows. Legacy notes remain in placements for export, new entries in their own array; no duplication. Older app versions cannot import format7. Images/PDFs and all take media/metadata/labels remain excluded. Schemas/migrations1–10 retained; compiler schema11 is checked against all previous table definitions. Recording engine, timing/transition algorithms, image/PDF loader, branding and unrelated navigation remain unchanged. Focused Practice now uses an opt-in legacy-note preservation flag for Complete/Skip, preventing a stale screen copy from overwriting an edit.

## Exact verification

- Debug/release/test APK compilation and **3 PracticeNoteRepositoryTest tests** pass: complete seeded schema10 table preservation, 10→11 validation, legacy history, independent notes, text edits with unchanged timestamp, close/reopen, ownership and injected-write failure safety, JSON7 round-trip, readers1–6, nonempty guard and atomic restore failure rollback.
- One clean API35 debug install/Activity launch/live process/visible UI/no-crash smoke check passes.
- **NotesHistoryPreviewTest and NotesHistoryReopenTest**, with app force-stop between them, pass: append twice during one session, immediate history/editor clearing/panel staying open, edit/save/cancel and timestamp retention, earlier-session legacy entry, save during real free capture with running timer/click, scroll thumb, unsaved draft retention/exit guard, Description default/footer placement/removal of Save for later and Description/reference restoration across Notes/selection/setup and direct panel switch, PDF page/zoom retention, disk-backed fresh-process reopening and another saved entry.
- Direct screenshots reviewed before signing; schema11 and focused XML results retained from this run. Permanent-key signature/version/hash/archive, old-package and Git scope/whitespace audits pass.

Not run: full units/lint/regression/instrumentation suite/API matrix, dedicated enlarged-text/landscape/TalkBack, real-provider JSON pickers, acoustic loop/click quality or permanent-key owner-phone install-over. Storage failure rollback is tested; native save-error injection is not. Legacy editing is covered in storage tests and shares the UI edit form; the native edit path exercises a new note. Very short keyboard layouts may require scrolling the compact controls/history; phone keyboard/layout acceptance remains pending. Existing full-release gates remain unchanged. Prior incomplete regression evidence stays in WORK_LOG. Stage3 phone acceptance pending; Stage4 remains0/10 real sessions.

## One next task

Owner: install build41 in place and check Description default, section switching, footer Notes, lower transport and existing data on the phone. Check enlarged text/keyboard and Back-to-Main/resume as practical. No subsequent work starts automatically.
