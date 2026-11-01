# Current state — Submarine refinement; P5 remains complete

## Submarine increment — November 1, 2026

Submarine adds red night lighting through existing Compose semantic roles and native dark window/dialog styling. Settings order: Light, Dark, Smooth, Sunny, Orchid, Submarine, System Default. Existing palettes, stable saved IDs and Light fallback/default remain. No layout, database or audio changes.

[Targeted CI41333454953](https://github.com/mrayover/practice.logger/actions/runs/41333454953) passed on source `a039df0814a1adc6cdbcfb87f2112e566be51a0f`: 7/7 tests (4 palette/contrast/resolver, 3 preference/persistence/order) and one debug APK compilation. No P5 regression, native scenarios, lint, release compilation or signing ran. Existing palette definitions are unchanged; diff checks pass.

[Inspection debug APK](.tools/submarine-checks-41333454953/outputs/apk/debug/app-debug.apk), SHA256 `4121748443f446c08b5ea9fa14e7835ce70ea265fb1a5e79b3b2afa2bb2b4733`; downloaded artifact/APK ZIP integrity verified. It retains version0.17.1/code67 and uses the CI debug key; it cannot update the permanent-key installation. Do not uninstall the accepted app to inspect it. No version bump or new signed release, per this assignment. Visual/device acceptance remains pending. The permanent-key release below remains the accepted installable update.

**Current next action: inspect Submarine visually on an isolated debug installation, including Settings order, dialogs, practice, metronome, recording, selected/disabled controls and warnings.** The accepted P5 baseline below remains closed.


Updated November 1, 2026 (America/Los_Angeles). **P5 outcome: PASS — automated verification, signed delivery and owner acceptance complete.** [Full CI41329443572](https://github.com/mrayover/practice.logger/actions/runs/41329443572) passed all nine required jobs on `bf999b9678039dbf669560cd10649b340c11e217`, branch `main`: 111/111 units, 168/168 native executions, lint zero errors/18 advisories, debug/release/instrumentation compilation. Only the optional diagnostic job was skipped.

## Accepted pre-Library release

**0.17.1/build67:** [APK](C:/Users/mrove/Projects/practice.logger/dist/practice.logger-0.17.1-b67-bf999b9/practice.logger-0.17.1-b67-bf999b9.apk), [ZIP](C:/Users/mrove/Projects/practice.logger/dist/practice.logger-0.17.1-b67-bf999b9.zip). Application and test source: `bf999b9678039dbf669560cd10649b340c11e217`. Later record-only commits are separate.

- APK SHA256: `460a2e16f52e4aeec9988abe96bc8f82c247dffceccff8ba280406c548f9b496`.
- ZIP SHA256: `9b43b40f6612f7efdf3e12b8950a2fd6e956390dc28245af308cb0c495940c95`.
- Permanent certificate SHA256: `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.

Stable app ID `com.mrayover.practicelogger.app`. Publisher package/version/signature/pinned-certificate checks pass; all 140 unsigned CI payload entries match; APK/ZIP integrity and five exact ZIP members verified. Install over the permanent-key app without uninstalling/clearing. Existing distributions, build66 and intermediate ad1-source67 remain preserved; use the final bf999b9 package linked above.

## Evidence and preserved scope

Per API: 49 ordered device executions, 7 review/max-128-second-loop, 20 transport/Settings/Notes/reference and 8 appearance/import, 84 total. All 57 classes/77 native scenarios plus 7 ordered upgrade/large-text repeats execute on both APIs. Migration/install-over, recovery/reopen, backup, original media/data, normal/200% text, keyboard/configuration and landscape checks pass. See [P5 report](docs/P5_REGRESSION_REPORT.md) for all 36 attempts, failure classifications and 18 lint advisories; [baseline](docs/P5_REGRESSION_BASELINE.json) retains exact case/result/schema/job metadata.

Earlier P5 product fixes are limited to qualified theme attributes, full-width references and enlarged Accent/recording-control sizing. Current tests follow explicit independent Notes, inline review and saved-Block management. This completion assignment changes only AudioSettings test cleanup after evidenced stale Compose idling; targeted checks and the final full gate pass. No audio/data engine or approved UI redesign.

Room11, explicit migrations, JSON7/readers1–7, stable IDs, original references/audio and permanent signing remain. JSON excludes all takes/audio and image/PDF metadata/bytes; retain originals/export audio separately. All 11 compiler schemas and all 282 session-entry APK/ZIP hashes match. Synced sources and owner phone/data were untouched. Owner authorized Git command publication; mandatory efficiency procedure reviewed/preserved.

## Owner acceptance and next action

Owner confirmed on November 1, 2026: “Confirmed complete and running correctly”. This accepts the delivered 0.17.1/build67 in the context of the P5 handoff and closes P5. Phone model, Android version and itemized checklist observations were not supplied; no additional measurements or individual check results are inferred.

No additional P5 work is pending. The current Submarine next action is listed above. Media Library L1 has not started and is not authorized by acceptance alone. Imported repeat retains its documented lack of a certified seamless-join guarantee; JSON media exclusions remain. Stage4 remains 0/10 documented real sessions.
