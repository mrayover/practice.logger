# Current state — P5 pre-Library regression in progress

Updated 2026-11-01 (America/Los_Angeles). P5 is explicitly assigned; no Library work has started. The existing app files are authoritative. Owner explicitly authorized Git command commit/push for this increment. [Detailed regression report](docs/P5_REGRESSION_REPORT.md) records every attempt and its actual scope.

## Working candidate and changes

Working version: **0.17.1/build67**. Current app source15c1a31 builds on5d57e68: it fixes six lint errors by placing native navigation-icon/force-dark attributes in API27/API29 resources, and restores full-width practice references by removing their reserved scrollbar inset. Screenshot review also identified200%-text Accent/checkbox overlap and clipped recording action text; enlarged controls now size to their text, with added assertions. No recording/data engine or Room/JSON migration change. Test-only sourcec173da0 adds valid overflow fixtures for transport scrolling. Legacy native scenarios now follow explicit independent Notes, workspace headers, inline recording/review and saved-Block management without dropping safety assertions.

Room remains11, JSON writer7/readers1–7. JSON excludes all take/audio data and image/PDF metadata/bytes. Preserve originals and export audio separately. All11 compiler schema exports equal the original snapshots; all278 pre-P5 APK/ZIP hashes remain unchanged. Permanent app ID `com.mrayover.practicelogger.app` and signing identity remain.

## Actual verification

- Initial CI41286865140:111/111 unit tests passed; lint failed6 errors/18 warnings. The six API-qualified resource errors were corrected.
- [Candidate CI41287370953](https://github.com/mrayover/practice.logger/actions/runs/41287370953), source328025e:111/111 units, zero failures/errors/skips; lint0 errors/18 advisory warnings; debug/release/instrumentation compilation passed.
- [Focused API35 CI41287372990](https://github.com/mrayover/practice.logger/actions/runs/41287372990), source328025e:2/2 native scenarios passed; screenshots reviewed.
- [Sessions API35 CI41287742759](https://github.com/mrayover/practice.logger/actions/runs/41287742759), source328025e:7/7 session/recovery/history/multiple-session/routine scenarios passed.
- [PDF/image API26 CI41288227315](https://github.com/mrayover/practice.logger/actions/runs/41288227315), source5d57e68:4/4 workflow/cold-reopen scenarios passed, including unchanged full-width geometry assertions.
- [Recording/phrase API26 CI41288229156](https://github.com/mrayover/practice.logger/actions/runs/41288229156), source5d57e68:4/4 recording/phrase/reopen scenarios passed.
- [200% text API35 CI41288422927](https://github.com/mrayover/practice.logger/actions/runs/41288422927), source5d57e68:1/1 broad accessibility/keyboard/navigation/layout scenario passed.
- [Appearance API26 CI41288420584](https://github.com/mrayover/practice.logger/actions/runs/41288420584), source5d57e68:6/6 scenarios passed. [Transport/nested API35 CI41288671701](https://github.com/mrayover/practice.logger/actions/runs/41288671701), sourcec173da0:2/2 scenarios passed. Static wiring includes all57 native classes/77 declared scenarios; this does not substitute for full execution.

Earlier instrumentation compilation errors, cancelled superseded build and targeted fixture failures are recorded in the detailed report. **No complete P5 full-matrix pass or owner-phone acceptance is claimed.** A diagnostic workflow's skipped jobs do not count as full gates. Physical acoustic/display/provider/TalkBack checks remain; Stage4 remains0/10 documented real sessions.

## Preserved delivery and candidate

Previous owner delivery remains **0.17.0/build65**, app source `a6c095c02fc2363bcdf04cd2c4b168ce7f55569f`: [APK](dist/practice.logger-0.17.0-b65-a6c095c/practice.logger-0.17.0-b65-a6c095c.apk), [ZIP](dist/practice.logger-0.17.0-b65-a6c095c.zip).

Initial P5 **build66** candidate, source328025e, is preserved as [APK](dist/practice.logger-0.17.1-b66-328025e/practice.logger-0.17.1-b66-328025e.apk), [ZIP](dist/practice.logger-0.17.1-b66-328025e.zip). It predates the full-width reference fix; use the eventual build67 replacement for phone acceptance. Package/version/signature/pinned-certificate,140 unsigned/signed payload entries and5 exact ZIP members verified. APK SHA256 `bc6f1901dd6d4cbfd87a3eb0d69357a632639a4d0106d5839eadb59fcae643fd`; ZIP SHA256 `e4e5aa24d9904f3a1e9a0f11a6cf14a2754a1ed08a02f767126717cc8ea5742d`.

Install only over the existing permanent-key app, without uninstalling/clearing data. Phone acceptance has not occurred. Earlier Orchid/import/provider/physical limitations carry into P5.

## Exact next task

Confirm the legacy install-over probe41288895114 and strengthened200%-text check41289104452, then dispatch one coherent complete build/unit/lint/API26/API35 gate for final build67 source, package/sign it with the permanent key and record actual outcomes. Owner must then accept that candidate using the short phone checklist before P5 exits. **Stop at P5; L1 needs a separate assignment.**
