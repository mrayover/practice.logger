# Current state — Focused Practice UI refinement, 0.16.0/build39

Updated 2026-10-30 (America/Los_Angeles). **Signed, installable limited owner preview. Full regression verification pending.** UI refinement only; no later recording acceptance workflow or multiple-note history. No further automated work scheduled.

## Delivery and provenance

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code39 |
| Signed app source | `b0e7d40df6646595b2d7436a4d342becb3f96fad` |
| Limited preview run | [41115775640](https://github.com/mrayover/practice.logger/actions/runs/41115775640) |
| Final workflow status | completed / success |
| App identity | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `383d2a8b6f63f7edfddbd21717e6964cdd0509efbbda60527a4c9f0e66de3391` |
| ZIP SHA256 | `41024d34e161b08c407547dd057a7eaa2a2297811511f243ef1ac4076a875a71` |

[Installable APK](dist/practice.logger-0.16.0-b39-b0e7d40/practice.logger-0.16.0-b39-b0e7d40.apk) · [ZIP](dist/practice.logger-0.16.0-b39-b0e7d40.zip) · [build info](dist/practice.logger-0.16.0-b39-b0e7d40/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b39-b0e7d40/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b39-b0e7d40/PHONE_CHECKLIST.txt) · [latest pointer](dist/LATEST.txt).

Install over the existing permanent-key app without uninstalling or clearing data. Existing publisher verifies the pinned permanent certificate, cryptographic signature, stable app ID and version39. ZIP CRC/member round trip and all 146 prior APK/ZIP hashes pass. Local packages remain ignored by Git. A documentation-only commit follows the signed source.

## Before and after

Build38 always displayed a bounded description alongside a separately collapsible reference, and used multiple recording metadata lines on the cream background. Build39 shares the flexible workspace between Description and Reference, keeps both compact single-line headers visible and permits one expansion or both collapsed. An attached image/PDF is favored after attachment data loads; otherwise Description opens. Empty reference content does not create a large reference card. The existing viewer now has a default-off option that sizes its viewport from available height after measuring its page/guide controls; aspect fit, pages, zoom/reset and resource loading remain.

The pinned transport has a subtle cream/green blended surface and a divider. Plain clickable recording labels have no rounded wrapper; action buttons retain their established rounded style and 48dp targets. A selected recording uses one ellipsized line: user name or concise date/time including seconds. No raw IDs or duplicate dates. Green action/playback states replace the redundant Looping line; recording/count-in/errors remain. Normal rows have fewer gaps/lines. Ready/practicing/paused feedback is represented by the timer action's accessible state; time-reached guidance stays visible.

Free/loop selection lists now expand inline into the same exclusive Notes/setup/history panel state. Lists contain detailed existing metadata. Standard Compose verticalScroll handles contents; a persistent right-side track/thumb is drawn when there is overflow in Notes, selection lists, loop setup, workspace and short-display fallback controls. Panel height is bounded; main/active practice actions remain outside panel scrolling. Notes still saves the same single Block-session note. No database, schema, recording engine, audio ownership, timer, daily Log or unrelated navigation change. FocusedPractice's original placement-keyed DisposableEffect remains the sole session/audio owner.

## Verification performed

- Debug/release and instrumentation APK compilation pass.
- One clean API35 debug emulator install/launch: successful start, live process, visible app UI and no app crash in smoke window.
- One targeted TransportPreviewTest scenario passes: reference default/display, Description/Reference exclusivity and both-collapsed switching, no-reference Description default, workspace contraction, setup/Notes/free-list/loop-list overflow indicators, note save during capture, timer/click continuity, two real free takes and two real loops, older selections/playback, placement identity and retained take count. Directly affected screenshots reviewed before signing.
- Permanent-key publisher, final APK/ZIP/member/hash audit, prior-package preservation and source/whitespace checks pass.

Not run: full units/lint/regression/instrumentation suites, API matrix, dedicated 200%/landscape/TalkBack, permission-denial/failure injection or historical diagnostics. Debug emulator checks do not establish permanent-key phone install-over/data retention or physical audio quality/timing. Phone keyboard/enlarged-text/landscape reachability and scrollbar discoverability remain owner checks. Full release requirements remain intact; prior incomplete full-regression evidence stays in WORK_LOG. Room10/migrations1–10, JSON6/readers1–6 and media exclusions are unchanged. Stage3 phone acceptance pending; Stage4 remains0/10 real sessions.

## One next task

Owner: install build39 in place and follow the six-step refinement phone checklist in MANUAL_TESTS.md or the package PHONE_CHECKLIST.txt. Report workspace sizing, compact transport, scrollbar/keyboard and recording/note behavior. No subsequent assignment starts automatically.
