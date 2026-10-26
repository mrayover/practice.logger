# Work log

Append-only session record. Times/dates use America/Los_Angeles.

## 2026-10-26 — Initial development session, Stage 0 checkpoint

- Inspected actual Windows repository and GitHub contents, Desktop clean status and main/origin refs. Baseline cbf1ac527e9aa6e114cb1375389da22b6d8d14fc. Existing prefixed build plan and handoff read completely and preserved. README/AGENTS/CURRENT_STATE/WORK_LOG/BUILD_PLAN did not exist; established operational records and canonical copies of existing plan/handoff.
- Authoritative scope: Stage 0 first; Stage 1 only after successful foundation build. Deferred functionality remains excluded.
- Added Android Kotlin/Compose foundation, bundled offline Lato Regular/Bold with license, Room version-1 Block schema infrastructure, Gradle wrapper, ignores, targeted foundation tests and CI build/lint/test/APK workflow. Home truthfully identifies the foundation shell without unfinished controls.
- No local JDK/Android SDK detected, so build and automated launch verification must run in CI. At this checkpoint build/tests/APK/push are pending; no functionality or stage completion claimed. Phone tests pending. Debug signing continuity and absence of backup are documented limitations.
- Stage 0 manual checklist: install APK on Android 8+; open Home; confirm title/body use Lato; rotate and reopen; enable airplane mode and reopen; confirm no crash or misleading unfinished navigation. Owner execution pending.
- Next: publish via GitHub Desktop, verify remote SHA/CI/artifact and test results, then decide Stage 1 gate from evidence.

### Stage 0 verification refinement
- Published foundation main 2e31ef6885d26a8e39070c93ff97cdb256fdc592 via GitHub Desktop; remote verified. CI run 40366969344 started. During review, made the coroutine database test explicitly return Unit for JUnit compatibility and exposed generated schema in CI logs for retaining its exact migration baseline. Validation pending next run.
- Initial CI run failed before compilation: setup-android default requested retired SDK package tools. Explicitly configured platform-tools, SDK 36 and build-tools 35.0.0; subsequent CI must verify the correction.

### 2026-10-26 — Foundation gate passed; Stage 1 implementation checkpoint
- Stage 0 main c3c8e8f5b71a34ef3e68cebd6ce16be59f062979 pushed with GitHub Desktop and verified in GitHub Actions run https://github.com/mrayover/practice.logger/actions/runs/40367248107 . Build/test/lint passed; API 35 emulator installed/launched Home and recreated Activity successfully. APK: https://github.com/mrayover/practice.logger/actions/runs/40367248107/artifacts/12671091904 . Owner phone checks pending.
- Began Stage 1 only after foundation build/test/lint succeeded. Implemented Home, Block Library, Block Editor, transactional Room create/edit, literal case-insensitive search, archive/restore, validation, save/cancel/back and discard confirmation, draft restoration on Activity recreation and stored error handling. Stable IDs and original creation timestamps are retained. Archive-only policy protects future historical references; no permanent delete API.
- Retained compiler-generated Room schema version 1 from successful foundation CI logs, identity 740ceb46ea18aabd35c3fd36b704888e. No destructive migrations.
- Added repository/validation tests and emulator workflow for create/edit/search/archive/restore/discard/draft recreation, followed by a separate force-stop/reopen instrumentation run. Stage 1 build/tests are pending; do not claim its exit gate yet. Added MANUAL_TESTS.md and usage notes.
- Design is restrained/provisional; no prior approved sketches were present. Practice Now/history controls omitted until Stage 2 because they cannot yet perform a real workflow. No deferred functionality added.
- CI ignores documentation-only changes; source/config changes still build/test/lint and publish APKs. Two emulator phase reports retained separately.

### 2026-10-26 — Stage 1 lifecycle review
- Published catalog source cd6ef72b84c7f7a4c335b8d6e5d30a8350ac6beb via Desktop; CI run 40368836246 started. Found that save/archive callbacks captured the previous Activity composition during recreation. Moved pending navigation outcome into the retained ViewModel so the current screen consumes successful saves. Extended emulator workflow to recreate immediately after every save/archive/restore/discard transition. Verification pending.
