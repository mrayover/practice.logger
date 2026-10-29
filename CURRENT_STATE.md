# Current state — practice.logger 0.13.0, build 25

Updated 2026-10-29 (America/Los_Angeles). This is the authoritative delivery and verification record. Release history and the mandatory efficiency procedure remain in [WORK_LOG.md](WORK_LOG.md).

## Delivery

| Item | Verified value |
|---|---|
| Release | **0.13.0 / versionCode 25** (build 24 retained as an earlier candidate) |
| APK source | `d29e4bf7c7287467a724b8405ea24a68a462ed8d`; later closeout commits update records only |
| Package ID | `com.mrayover.practicelogger.app` |
| Android | 8.0+; minimum API26, target/compile API36 |
| Database | Room **9**, explicit migrations 1→9 and compiler schemas retained |
| JSON | Writer **6**, readers **1–6**; independent Logs included, all recordings/take metadata/labels excluded |
| APK SHA-256 | `aad00c5f137f2c6ed9b27435cdfc8dfba8f67cdd523805b24fd7f5b5b3d3796d` |
| Permanent certificate SHA-256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |

- [APK](dist/practice.logger-0.13.0-b25-d29e4bf/practice.logger-0.13.0-b25-d29e4bf.apk)
- [ZIP](dist/practice.logger-0.13.0-b25-d29e4bf.zip)
- [Build provenance](dist/practice.logger-0.13.0-b25-d29e4bf/BUILD_INFO.json)
- [Verification status](dist/practice.logger-0.13.0-b25-d29e4bf/VERIFICATION_STATUS.txt)
- [Latest pointer](dist/LATEST.txt)

Existing Windows checkout: `C:\Users\mrove\Projects\practice.logger`. GitHub main contains source, tests, schemas and documentation; local distribution files/signing material are intentionally ignored. No synced ChatGPT sources were changed. Earlier APK/ZIP files are retained and checked byte-identical. Publisher verifies signature, pinned certificate, package/version, checksum and ZIP extraction. Install over the permanent-key app **without uninstalling**. The old temporary-key0.1.0 installation remains separate.

## Implemented app and increment

Home → **Log** opens today's local calendar date. A large multiline plain-text editor automatically saves one independent entry per date, preserving whitespace. **Choose date** revisits editable past entries. Returning on a new day opens that day without changing earlier entries. **Weekly View** groups Monday–Sunday, shows full text or No entry, navigates weeks and opens any dated entry for editing. No weekly records, practice relationships, tags, prompts, AI, rich text or attachments.

Every queued edit carries its original date. An IO writer coalesces only edits for the same date and flushes before date changes, navigation and Activity pause. Failed drafts remain in the current editor for Retry; leaving fails safely if persistence fails. Weekly entries read the daily table. The new migration only adds `daily_logs`; existing data/audio are preserved. JSON 6 exports all saved Logs and validates canonical real YYYY-MM-DD dates, uniqueness and text types. Restore remains atomic, refuses a Log-only/nonempty target and reads older formats1–5 with no Logs. Oversize exports explicitly fail at the existing8 MiB limit.

Existing Blocks/safe dependency-protected permanent deletion, independent sessions/history, integrated click, Block/free recordings/phrase loops, Tools, saved Routines and Goals remain implemented. Lato, cream#F1E6D3/green#315C47, banner/navigation retained. Image/PDF attachments remain a separate future increment.

## Actual verification

| Check | Result |
|---|---|
| Final build | [Release CI](https://github.com/mrayover/practice.logger/actions/runs/40897188240): **72 unit/Robolectric tests**, 0 failures/errors/skips; debug/release compilation; lint 0 errors / 10 existing warnings. Includes 7 new focused Log tests plus all 65 existing tests. |
| Persistence/migration/backup | Rapid date-bound edits, in-flight flush, retained failed draft/retry, file reopen, week/year/leap/local-date boundaries, schema 8→9 preservation of every old table/take row, JSON 6 round trip/legacy1–5, invalid/duplicate dates/types/size, Log-only restore refusal and late-write rollback pass. Compiler schema 9 agrees with committed export; schemas 1–8 unchanged. |
| API26 final release workflow | Log daily/weekly workflow, cold reopen and install-over retention passed. Catalog, practice/recovery/history, history upgrade, backup/picker restore/reopen, multiple sessions and metronome passed. Full job 123532311920 stopped at RecordingWorkflowTest.kt:101 waiting for Delete this take? after rename; subsequent phases were not reached. The unchanged recording class passed alone in [diagnostic CI 40899553803](https://github.com/mrayover/practice.logger/actions/runs/40899553803) (job 123537782782). Cause remains unestablished; no full API26 pass is claimed. |
| API35 final release workflow | Complete final suite passed in job 123532313006: original catalog plus all 31 direct phases, including Log daily/weekly, cold reopen, install-over retention, backup/pickers, practice/history, audio/loops, Tools/Routines/Goals/deletion and enlarged-text checks. |
| Enlarged text | Final LogWorkflowTest and LogReopenTest passed at 200% on API26 (CI 40897266669/job 123529683140) and API35 (CI 40897260725/job 123529666316). Tests cover today/yesterday edits, immediate navigation, week selection/navigation, recreation, simulated local-day change and process reopen. |
| Visual review | Final daily and weekly views reviewed at normal text on API35 and enlarged text on API26/35. The clipped build 24 weekly header was replaced by scrolling content; build 25 controls remain readable and the editor scrolls into view. Physical-phone landscape/TalkBack/provider acceptance remains pending. |
| Package | Signature/certificate/identity/version/APK hash/ZIP integrity verified; earlier packages preserved. |

Native Log checks cover today/yesterday editing, immediate Home/date navigation, weekly selection/navigation, Activity recreation, simulated new day, cold process reopen and backup encoding. Install-over probes use increasing CI versionCodes 26/27 (test probes, not delivered releases). Exact observed outcomes above take precedence over earlier candidate results. Build24 full CI was superseded/canceled for the screenshot-driven layout adjustment; its separate enlarged-text Log tests passed both APIs. Initial CI 40895602401 passed 7 Log tests but failed 1/72 broad tests because an old future-format fixture used version 6; the fixture now uses 7 with its rejection assertion retained.

## Limits and pending acceptance

- Physical-phone installation over the owner's permanent-key app has **not** been performed here. Matching permanent certificate/package and migration tests establish update compatibility; native install-over proofs use CI debug signing. Confirm real-phone data/audio preservation with [MANUAL_TESTS.md](MANUAL_TESTS.md), without uninstalling.
- Detailed provider/TalkBack/landscape/phone keyboard and recording/acoustic checks remain pending. Automated emulator success does not establish physical-phone acceptance. Stage 3 detailed phone backup/recovery acceptance remains pending; Stage 4 stays **0/10 documented real sessions**.
- Log writes are queued promptly and normal pause/navigation drains them. Abrupt kill/power loss before a database commit or storage failure cannot be guaranteed lossless; wait for Saved. Other practice-note/timer save rules remain unchanged. Storage failure keeps the current Log draft in memory for retry, not as an independently durable backup.
- JSON excludes audio/take labels/metadata; use per-take export. No media-inclusive bulk restore, cloud sync or image/PDF increment is implemented.

## Exact next task

Install signed **0.13.0/build 25** over the existing permanent-key app and run the six Daily Log/Weekly View phone checks in [MANUAL_TESTS.md](MANUAL_TESTS.md); report phone, Android, timezone/build and pass/fail observations. No additional feature is selected.
