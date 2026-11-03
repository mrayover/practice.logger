# Current state — Reference camera/gallery actions delivered; P5 remains complete

Updated November 3, 2026 (America/Los_Angeles).

## Current delivery

**0.17.3/build69**, application/test source `9c723442d8f3eb1dd8c2d23cc76dd3af4de7e1d3`; later closeout commits change records only. [APK](dist/practice.logger-0.17.3-b69-9c72344/practice.logger-0.17.3-b69-9c72344.apk) · [ZIP](dist/practice.logger-0.17.3-b69-9c72344.zip) · [Build info](dist/practice.logger-0.17.3-b69-9c72344/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.17.3-b69-9c72344/PHONE_CHECKLIST.txt).

Install over the current permanent-key app without uninstalling/clearing data. Existing publisher verifies app ID, version, signature and pinned certificate. APK SHA256 `5eb33857e214490dfc196482c85c49e587df86ecfa21d59505fc4fbcafb31385`; ZIP SHA256 `1ec2120c694d0baae4d8077c57bb5145543ef5caf1fdabd74e7d00f076cd5b0d`. Signed non-signature payload matches the CI release; ZIP integrity/member bytes and all 294 prior APK/ZIP hashes pass. dist/LATEST points here. Previous build68/Submarine packages remain preserved; Submarine is included unchanged.

## Scope and focused verification

Saved Block reference controls now show files on the left, Or, and Take or Add a photo on the right. Matching outlined buttons have centered text, equal height and minimum48dp targets; enlarged text/narrow windows stack the actions. The photo chooser offers Take photo / Choose from gallery / Cancel. Full-resolution capture uses a private FileProvider; selected-photo access uses Android PickVisualMedia with older-device fallback. Delegating capture to the installed camera app requires no new broad app camera/gallery permission. Missing camera or launch failures show a recoverable message.

All sources reuse bounded image/PDF validation and safe replacement. Cancelling keeps the existing attachment. Capture filename survives activity recreation; consumed/cancelled capture staging is removed, while abandoned process-loss staging is OS-managed cache. Photos captured here are private app copies, not automatically saved to the shared gallery. Original image orientation and one-reference ownership remain; Room11, JSON7/media exclusions, practice/audio and themes remain unchanged. Media Library has not started.

[Targeted CI41363227308](https://github.com/mrayover/practice.logger/actions/runs/41363227308) passes release compilation and **11/11** tests across ReferencePhotoCaptureTest, ImageReferenceTest and PdfReferenceTest. Coverage includes provider scope/full-byte output/cancel contract/path rejection plus existing validation/rollback/orientation/PDF safety. No full P5 regression, native workflow, lint or physical camera/gallery/phone visual acceptance rerun. Accepted P5 evidence below applies to build67, not newly certified build69 behavior.

**Build69 owner acceptance:** On November 3, 2026, the owner confirmed that camera/gallery functionality works correctly on their phone. This closes the outstanding owner-phone action; no device details or individual edge-case observations were supplied.

**Exact next action: implement and verify the explicitly assigned L1 canonical Media Library/storage, then deliver its signed update.**

## Documentation amendment — November 3, 2026

Integrated the approved [Media Library / Reference Piano / Transcribe specification](docs/MEDIA_LIBRARY_TRANSCRIBE_INTEGRATION_PLAN.md) into the authoritative [V1 roadmap](PRACTICE_LOGGER_V1_MASTER_PLAN.md): L1–L3 → K1 → T1–T3 → S0–S2 → G1–G3 → F1–F2 after accepted P5. Canonical audio ownership, two independent reference roles, master/local markers and complete recovery are planned only. Current release, Room11, JSON7/readers1–7 and media exclusions remain unchanged. No app implementation, new APK, build or regression run. Next implementation phase is L1, separately assigned after the outstanding build69 owner-phone checklist; the exact next action above is unchanged.

## Accepted P5 baseline

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

No additional P5 work is pending. The current reference-photo next action is listed above. Media Library L1 has not started and is not authorized by acceptance alone. Imported repeat retains its documented lack of a certified seamless-join guarantee; JSON media exclusions remain. Stage4 remains 0/10 documented real sessions.
