# P5 regression report — PASS; owner accepted

Verified November 1, 2026 (America/Los_Angeles). **All nine required jobs passed on one final source: 111/111 unit tests, 168/168 native executions, zero lint errors, and debug/release/instrumentation compilation.** The owner accepted signed 0.17.1/build67 on November 1, 2026: “Confirmed complete and running correctly”. P5 is PASS and closed. No Library or later milestone started.

## Final source and release

- Application and test source: `bf999b9678039dbf669560cd10649b340c11e217`, branch `main`.
- [Final full workflow](https://github.com/mrayover/practice.logger/actions/runs/41329443572), dispatched with build_only=false and empty test_class/feature_suite.
- [Signed APK](C:/Users/mrove/Projects/practice.logger/dist/practice.logger-0.17.1-b67-bf999b9/practice.logger-0.17.1-b67-bf999b9.apk) and [five-member ZIP](C:/Users/mrove/Projects/practice.logger/dist/practice.logger-0.17.1-b67-bf999b9.zip). Both are local, ignored distribution artifacts.
- APK SHA256: `460a2e16f52e4aeec9988abe96bc8f82c247dffceccff8ba280406c548f9b496`.
- ZIP SHA256: `9b43b40f6612f7efdf3e12b8950a2fd6e956390dc28245af308cb0c495940c95`.
- Permanent certificate SHA256: `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`; package `com.mrayover.practicelogger.app`.

Later documentation commits do not change the application or test source above. The [machine-readable baseline](P5_REGRESSION_BASELINE.json) retains job IDs, unit names, native declarations/result checksums, schema hashes and package provenance.

## Required gates

| Native gate | API 26 | API 35 |
|---|---:|---:|
| Ordered device workflows, migration/install-over/reopen | 49/49 | 49/49 |
| Review, flexible phrases, maximum-loop playback | 7/7 | 7/7 |
| Transport, meter, Settings, alarms, Notes, references | 20/20 | 20/20 |
| Appearance and imported audio/reopen | 8/8 | 8/8 |
| **Total executions** | **84/84** | **84/84** |

The ninth required job passes all 111 JVM/Robolectric tests in 30 suites, with zero failures/errors/skips; lint has zero errors and 18 advisories. Debug, release and instrumentation compile successfully. All required jobs ran. Only the optional diagnostic job is intentionally skipped in a full dispatch. No required test is recorded as skipped or unresolved.

Native counts come from raw instrumentation output and the connected LaunchTest XML: 77 declared scenarios plus seven ordered upgrade/enlarged-text repeats per API. They are 168 executions, not 168 unique tests. All 57 native classes are included.

Coverage retains Blocks/archive/deletion safeguards; Goals/Routines; independent sessions/timing/checkpoints/history; new and legacy Notes/editing/failed-save barriers; dated Log/Weekly View; free/phrase/imported audio/review/Save/export; original audio bytes; flexible loops/count-in including 128-second capture/playback; metronome meter/accent/sound/flash and one-shot alarms; image/PDF originals/page/zoom; six themes; JSON7/readers1–7/atomic empty-target restore; migration/install-over, recreation/cold reopen, normal/200% text, keyboard and portrait/landscape. Emulated install-over uses disposable debug installations.

## Failures, changes and classification

- **A — Obsolete/mismatched tests:** old Notes writes/buttons, in-practice management, reference routes, meter representation, removed Loop Setup Cancel, and off-screen virtualized Block rows. Tests now use the current interface, scroll the real lists and retain underlying data/audio/safety assertions. Real overflowing history/take fixtures preserve scrollbar checks; four actual captures and 16 valid existing-audio fixtures remain asserted.
- **B — Harness/synchronization:** review Save readiness, IME dismissal, foreground relaunch and configuration cleanup. Logs proved old import flags created another Activity; standard launcher flags resume the task. ActivityScenario callbacks avoid stale Compose idling during font/orientation resets. Assertions, geometry tolerances and test budgets were retained.
- **C — Demonstrated product/build defects earlier in P5:** six lint errors from unsupported theme attributes moved to API27/API29 resources; a reserved 12dp gutter reduced reference width; enlarged Accent/checkbox and recording labels overlapped/clipped. Narrow fixes and strengthened assertions passed. No engine, schema or backup redesign occurred.
- **D — Inconclusive:** none remains unresolved. Earlier compilation mistakes, failures, unreached checks and the one superseded cancellation are retained in the attempt history.

This completion assignment began at local/remote `ad1f3ed`, version0.17.1/build67, preserving three existing record edits. Full run41327674374 passed all nine required jobs on that source. A diagnostic already active at session entry (41327577586) separately exposed AudioSettingsWorkflowTest cleanup timing out at line49 after all large-text/landscape assertions and screenshots. Logs and stack identify stale Compose idling after configuration reset. Test-only `bf999b9` changes cleanup to the live activity callback; isolated run41328228247 passes 2/2 and full transport35 diagnostic41328303806 passes 20/20. The final coherent full run41329443572 then passes all nine jobs on that corrected source. **No product code changed in the completion assignment.** No tests were removed, ignored or weakened.

Initial full run41289654339 failed six required jobs: off-screen recording selection, outdated meter/scroll assertions, import foreground relaunch and appearance teardown. Build and both review groups passed; later unreached gates were not counted. A late cancellation request returned409 because it had completed, so no cancellation was registered. Causes were isolated before subsequent complete gates. Diagnostic successes never counted as full release passes.

## Attempt history

| Run | Source | Scope | Actual outcome |
|---|---|---|---|
| [41286865140](https://github.com/mrayover/practice.logger/actions/runs/41286865140) | `e90e540` | Initial build/unit/lint | failure: 111 units passed;lint failed6 API-resource errors/18 warnings. |
| [41287071761](https://github.com/mrayover/practice.logger/actions/runs/41287071761) | `67cb91a` | Focused35 | failure: Test zoom-helper compilation failed;no native execution. |
| [41287161976](https://github.com/mrayover/practice.logger/actions/runs/41287161976) | `dae7ec1` | PDF/image26 | failure: Same zoom-helper compilation failure;no native execution. |
| [41287317517](https://github.com/mrayover/practice.logger/actions/runs/41287317517) | `ad8e7d0` | Superseded build | cancelled: Cancelled after known compilation cause;no pass claimed. |
| [41287370953](https://github.com/mrayover/practice.logger/actions/runs/41287370953) | `328025e` | Candidate build | success: 111 units,lint0 errors/18 warnings,debug/release/instrumentation passed. |
| [41287372990](https://github.com/mrayover/practice.logger/actions/runs/41287372990) | `328025e` | Focused35 | success: 2 cases passed. |
| [41287685179](https://github.com/mrayover/practice.logger/actions/runs/41287685179) | `328025e` | PDF/image26 | failure: PDF2 passed;image width failed12dp inset;later reopen not reached. |
| [41287687286](https://github.com/mrayover/practice.logger/actions/runs/41287687286) | `328025e` | Recording/phrase26 | failure: Recording2 passed;phrase Save transition failed;reopen not reached. |
| [41287742759](https://github.com/mrayover/practice.logger/actions/runs/41287742759) | `328025e` | Sessions/routines35 | success: 7 cases passed. |
| [41287896686](https://github.com/mrayover/practice.logger/actions/runs/41287896686) | `328025e` | Transport/nested35 | failure: Outer Notes scrollbar failed with nonoverflowing fixture;nested not reached. |
| [41288227315](https://github.com/mrayover/practice.logger/actions/runs/41288227315) | `5d57e68` | PDF/image26 | success: 4 cases passed after reference-width fix. |
| [41288229156](https://github.com/mrayover/practice.logger/actions/runs/41288229156) | `5d57e68` | Recording/phrase26 | success: 4 cases passed after enabled-Save synchronization. |
| [41288231437](https://github.com/mrayover/practice.logger/actions/runs/41288231437) | `5d57e68` | Transport/nested35 | failure: Notes check passed;short take-list scrollbar fixture failed. |
| [41288420584](https://github.com/mrayover/practice.logger/actions/runs/41288420584) | `5d57e68` | Appearance26 | success: 6 cases passed. |
| [41288422927](https://github.com/mrayover/practice.logger/actions/runs/41288422927) | `5d57e68` | Accessibility35 at200% | success: 1 case passed;visual review still exposed Accent overlap/Play clipping. |
| [41288671701](https://github.com/mrayover/practice.logger/actions/runs/41288671701) | `c173da0` | Transport/nested35 | success: 2 cases passed with real overflowing history/take fixtures. |
| [41288895114](https://github.com/mrayover/practice.logger/actions/runs/41288895114) | `c173da0` | Legacy migration26 | success: 2 seed/install-over cases passed against rebuilt immutable build27. |
| [41289104452](https://github.com/mrayover/practice.logger/actions/runs/41289104452) | `15c1a31` | Strengthened accessibility35 at200% | success: 1 case passed;corrected layout screenshots inspected. |
| [41289336396](https://github.com/mrayover/practice.logger/actions/runs/41289336396) | `1a71fc8` | Focused35 at200% | success: 2 active-capture/exit/layout cases passed. |
| [41289654339](https://github.com/mrayover/practice.logger/actions/runs/41289654339) | `d3d5c63` | First complete gate | failure: Failed:3 required jobs passed,6 failed;see diagnosis below. |
| [41290650042](https://github.com/mrayover/practice.logger/actions/runs/41290650042) | `83fbb69` | Meter35 capture/count-in method | success: 1 case passed. |
| [41290652434](https://github.com/mrayover/practice.logger/actions/runs/41290652434) | `83fbb69` | Appearance/import26 | failure: Import foreground relaunch failed again;wait-only fix insufficient. |
| [41290831506](https://github.com/mrayover/practice.logger/actions/runs/41290831506) | `bbdec37` | Recording/phrase26,populated catalog | success: 4 cases passed with virtualized-list selection. |
| [41290974650](https://github.com/mrayover/practice.logger/actions/runs/41290974650) | `bbdec37` | Transport26 | failure: First10 cases passed;Notes expected removed Cancel modal. |
| [41290976740](https://github.com/mrayover/practice.logger/actions/runs/41290976740) | `bbdec37` | Transport35 | failure: Meter visibility/keyboard transition failed;later cases not reached. |
| [41291081751](https://github.com/mrayover/practice.logger/actions/runs/41291081751) | `65f1c0d` | Appearance/import26 | success: 8 cases passed after standard launcher-task flags. |
| [41291266435](https://github.com/mrayover/practice.logger/actions/runs/41291266435) | `65f1c0d` | Theme35 final dark/landscape method | success: 1 case passed in isolation;did not by itself disprove teardown race. |
| [41291481321](https://github.com/mrayover/practice.logger/actions/runs/41291481321) | `b72b541` | Transport35 | failure: First10 cases passed;Notes expected removed Cancel modal. |
| [41291483192](https://github.com/mrayover/practice.logger/actions/runs/41291483192) | `b72b541` | Appearance/import35 | success: 8 cases passed after lifecycle-safe teardown. |
| [41291771091](https://github.com/mrayover/practice.logger/actions/runs/41291771091) | `ad1f3ed` | Notes/history reopen26 | success: 2 cases passed after inline Loop Setup toggle alignment. |
| [41327574923](https://github.com/mrayover/practice.logger/actions/runs/41327574923) | `ad1f3ed` | Transport26 | success: 20 cases passed. |
| [41327577586](https://github.com/mrayover/practice.logger/actions/runs/41327577586) | `ad1f3ed` | Transport35 | failure: AudioSettings cleanup stalled after font reset;later cases not reached. |
| [41327674374](https://github.com/mrayover/practice.logger/actions/runs/41327674374) | `ad1f3ed` | Second complete gate | success: All9 required jobs passed;111 units and168 native executions. |
| [41328228247](https://github.com/mrayover/practice.logger/actions/runs/41328228247) | `bf999b9` | AudioSettings35,successor harness | success: 2 cases passed using live ActivityScenario cleanup. |
| [41328303806](https://github.com/mrayover/practice.logger/actions/runs/41328303806) | `bf999b9` | Transport35,successor harness | success: All20 cases passed. |
| [41329443572](https://github.com/mrayover/practice.logger/actions/runs/41329443572) | `bf999b9` | Final complete corrected-source gate | success: All 9 required jobs passed; 111 units and 168 native executions. |

## Lint advisories

| Category | Count | Disposition |
|---|---:|---|
| Dependency/Gradle version suggestions | 7 | Frozen versions retained; no unrelated toolchain upgrade. |
| OldTargetApi | 1 | Existing target36 retained; newer Android versions are not certified by this matrix. |
| ApplySharedPref | 1 | Existing synchronous theme commit/rollback and failure handling retained. |
| ModifierParameter | 1 | Optional parameter ordering; no behavioral defect. |
| ObsoleteSdkInt | 1 | Redundant v26 launcher qualifier at min26. |
| UsableSpace | 2 | Existing bounded-copy/free-space safeguards retained. |
| UseKtx | 5 | Equivalent convenience API suggestions. |

All 18 remain visible in lint output; no new suppressions were added.

## Preservation and signing

Room remains11 with explicit migrations; all 11 compiler schemas match source and original snapshots. JSON writer7/readers1–7 remains; it excludes takes/audio and image/PDF metadata/bytes. Stable IDs, history, original media, audio engine, signing identity and app ID are preserved. Synced project sources were read-only; no owner phone was accessed, uninstalled or cleared. Mandatory efficiency procedure is preserved verbatim after newline normalization.

All 278 pre-P5 APK/ZIP hashes and all 282 packages present at completion-session entry remain unchanged, including build66. The intermediate ad1f3ed build67 folder/archive remains preserved; its original ZIP was not finalized to include later folder additions and is not the delivery. No owner distribution of that intermediate was recorded. Final build67 uses identical application code and the final verified test source; no version bump is needed for unchanged application code before owner distribution.

The established Publish-LocalApk.ps1 publisher used the existing permanent key and verified package, versionCode67, version0.17.1, signature and pinned certificate. All 140 original unsigned CI APK entries match the signed APK byte-for-byte. APK/ZIP CRC, unique members, five exact delivery members and archive-to-folder bytes pass. Same package/certificate as build65 and a higher code support in-place updating. The agent did not perform the owner's phone installation; the subsequent owner acceptance is recorded below.

## Owner acceptance — P5 closed

Owner confirmed on November 1, 2026: “Confirmed complete and running correctly”. This accepts the delivered 0.17.1/build67 in the context of the P5 handoff and closes P5. Phone model, Android version and itemized checklist observations were not supplied; no additional measurements or individual check results are inferred.

The checklist below is retained as the delivered acceptance reference, not an itemized record of independently observed results. The packaged checklist/verification text remains the original delivery snapshot; package bytes and hashes are unchanged.

### Delivered phone checklist

Install the final build67 APK over the permanent-key app without uninstalling/clearing. Complete PHONE_CHECKLIST.txt and record phone, Android version, installed build, results and any explicitly accepted limitation:

1. Confirm Blocks, Goals, Routines, Logs, new/legacy Notes, sessions/history, references and all recorded/imported takes persist.
2. Check microphone/speaker, free Stop→Review→Save, loop count-in/audition/Save, saved-take playback, metronome audio/flash, completion sound and timing/loop edges.
3. Check image/PDF colors/page/zoom, all six themes, keyboard/enlarged text/portrait/landscape and TalkBack.
4. Background/reopen and interrupt audio safely; no automatic recording/playback restart or save. Test real phone/Drive providers and exported originals.

Physical acoustics/display latency/providers/TalkBack cannot be certified by emulators. Imported repeat has no certified seamless-join guarantee. Keep originals separately because JSON excludes media. Stage4 remains0/10 documented real sessions. **Owner acceptance has closed P5. Next action: await a separate owner assignment; L1 remains unstarted.**
