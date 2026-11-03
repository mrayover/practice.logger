# Current state — L1 signed candidate; Library interaction verification in progress

Updated November 3, 2026 (America/Los_Angeles).

## Current L1 candidate

**0.18.0/build71**, application/release source `5da4f17b3bf382b49c227accc47bc5e219673c89`. [APK](dist/practice.logger-0.18.0-b71-5da4f17/practice.logger-0.18.0-b71-5da4f17.apk) · [ZIP](dist/practice.logger-0.18.0-b71-5da4f17.zip) · [Build info](dist/practice.logger-0.18.0-b71-5da4f17/BUILD_INFO.json) · [L1 phone checklist](dist/practice.logger-0.18.0-b71-5da4f17/PHONE_CHECKLIST.txt). Install over accepted build69 without uninstalling/clearing data. This is a signed candidate with remaining UI/reopen checks, not owner-accepted L1.

Permanent publisher verifies app ID, version, signature and pinned certificate `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. APK SHA256 `e796cd2bac780d495733744bc32423af3dfbf34d62da8e3897967f0a963b83e4`; ZIP SHA256 `57b014c427cc2b8079f2dde13f014b6492946043f2c1bd4cc28054a5317dc3bf`. All 67 non-signature CI payload entries, ZIP CRC/exact member bytes and 298 prior distribution hashes pass. Previous packages and signing material remain. Internal70 was prepared before integrity hardening and is not the current installation target. dist/LATEST remains on accepted69 until closeout.

## Implemented ownership and boundaries

Room12 adds only indexed media_assets through explicit11→12; KSP export12 matches the implementation and schemas/migrations1–11 remain unchanged. One canonical original per UUID in files/media-library, streaming SHA256 integrity checks, shared existing image/PDF/audio validators, bounded IO import/recovery, searchable/filterable/stably sorted Home Library, title-only rename and confirmed deletion. [Storage and legacy audit](docs/MEDIA_LIBRARY_STORAGE.md).

Legacy block_images/files/block-images and takes/files/recordings remain under original ownership, with IDs, labels, Blocks and original bytes preserved. No legacy adoption, disposal or duplicated migration bytes. Safe adoption requires L3 to update durable links, resolution, deletion and recovery together; old media stays in existing Blocks/Recording. JSON7/readers1–7 still exclude canonical/legacy media metadata and bytes; Library-only targets now reject restore transactionally. Library/Backup screens disclose the exclusion. L2 previews/annotations, L3 linking and S2 complete ZIP recovery are not implemented.

## Actual scoped verification so far

- CI41705910512 / `5da4f17b3bf382b49c227accc47bc5e219673c89`: debug/release/instrumentation compilation and **34/34** units pass, including1→12 and11→12, all legacy rows/FKs, import failures/cancellation/commit recovery, original integrity/rename/delete and Library-only restore across readers1–7. Compiler schema12 retrieved and committed; exports1–11 match prior originals.
- API35/200% CI41705910512 and API26/100% CI41706664910: build69 seed, actual install-over, raw legacy metadata/byte hashes, imported playback/rename/export, Block protection and native canonical format/failure/dependency checks pass. Both runs later stop at new-test offscreen-row lookups in the Library UI. No product workaround or weakened assertion was introduced.
- UI diagnostics now explicitly compose lazy rows and wait for the list across recreation; API26/35 Library/reopen checks are active. Full P5/F1/F2, broad lint and owner-phone/vendor/acoustic acceptance are not rerun or claimed. P5 remains complete. Earlier failed/canceled attempts and evidence are in WORK_LOG.

**Exact next action: finish the isolated Library/reopen checks and close out L1 for owner verification.** The next separately assigned implementation phase is **L2 — Media Preview and Master Audio Annotation Workspace**.

## Build69 camera/gallery owner acceptance

On November3,2026 the owner confirmed build69 camera/gallery functionality works correctly on their phone. This closes its outstanding owner-phone action. Device specifications and individual edge-case observations were not supplied or inferred. P5 remains completed and owner accepted.

Prior signed **0.17.3/build69**, application/test source `9c723442d8f3eb1dd8c2d23cc76dd3af4de7e1d3`: [APK](dist/practice.logger-0.17.3-b69-9c72344/practice.logger-0.17.3-b69-9c72344.apk) · [ZIP](dist/practice.logger-0.17.3-b69-9c72344.zip). APK SHA256 `5eb33857e214490dfc196482c85c49e587df86ecfa21d59505fc4fbcafb31385`; ZIP SHA256 `1ec2120c694d0baae4d8077c57bb5145543ef5caf1fdabd74e7d00f076cd5b0d`. Targeted CI41363227308 passed release compilation and11/11 reference-photo/image/PDF checks. Package identity/signature/pinned certificate/CI payload/ZIP/prior-package checks were verified at delivery. Earlier source/records remain in Git history.

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

No additional P5 work is pending. Build69 camera/gallery is now owner accepted; L1 is explicitly assigned. Its current status and exact next action are above. Imported repeat retains its documented lack of a certified seamless-join guarantee; JSON media exclusions remain. Stage4 remains 0/10 documented real sessions.
