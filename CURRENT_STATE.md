# Current state — L3 signed candidate78; native verification pending

November 3, 2026. L3 only is explicitly assigned, including signing/commit/push. **L1/L2 owner-phone acceptance remains separately pending.** P5 and build69 camera/gallery acceptance remain recorded below.

Permanent-key **0.20.0/build78**, app source `5c6b8135861b4adb8d9a3c3c2b4d40e0d8a54062`: [APK](dist/practice.logger-0.20.0-b78-5c6b813/practice.logger-0.20.0-b78-5c6b813.apk) · [ZIP](dist/practice.logger-0.20.0-b78-5c6b813.zip). Install over build76 without clearing data. Signature, stable appID/version, pinned certificate, all67 unsigned payload entries, ZIP CRC/exact members and338 pre-session package hashes pass. Internal77 and all earlier distributions remain. APK SHA256 `9fdf802cb54cceb986720b9bc5205f1ffd5e43a8932f5f2c4479b9af36722b65`; ZIP SHA256 `7238d4d9adc418f1533fc6826395a678346ab6ac5e745ea02489faf12449275c`.

CI41735971595 passes51/51 focused units and debug/release/instrumentation compilation. Room14/schema14 and explicit13→14 are present; schemas1–13 retained. JSON7/readers1–7 remain data-only. [L3 architecture and legacy transition](docs/BLOCK_MEDIA_ASSOCIATIONS.md). Canonical visual/audio associations share Library originals; legacy visual references and take-owned audio remain safe. No K1/Transcribe/S2 implementation begins.

Initial native setup was blocked before assertions by mismatched temporary debug signatures, then missing runner debug key. Test setup now explicitly signs all emulator packages with one disposable key and verifies exact non-signature payloads; production signing is separate. One superseded API26 run was canceled. One unit run failed class setup fetching the Android runtime from repo1.maven.org; the canonical Maven endpoint resolves it and final51 tests pass. Detailed attempts remain in WORK_LOG.

**Exact next task: complete targeted API26/API35 native ownership/UI/reopen checks and final L3 records/publication.** Signed78 is an installable candidate, not a completed native/phone acceptance claim. Stop after L3; K1 Reference Piano is the next milestone and T1 follows K1.

---

# Current state — L2 media preparation workspace delivered

Updated November 3, 2026 (America/Los_Angeles).

## Current L2 delivery

**0.19.0/build76**, application/release source `e12b53f16e42fad98e6aa1a878fd05f70f9e5e75`. [APK](dist/practice.logger-0.19.0-b76-e12b53f/practice.logger-0.19.0-b76-e12b53f.apk) · [ZIP](dist/practice.logger-0.19.0-b76-e12b53f.zip) · [Build info](dist/practice.logger-0.19.0-b76-e12b53f/BUILD_INFO.json) · [L2 phone checklist](dist/practice.logger-0.19.0-b76-e12b53f/PHONE_CHECKLIST.txt). Install over build72 without uninstalling or clearing data. Permanent-key candidates73/74/75 and every earlier distribution are preserved. **L2 owner-phone acceptance is pending; L1 phone acceptance remains separately pending.** P5 remains complete and owner accepted.

App ID `com.mrayover.practicelogger.app`; unchanged permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. Publisher verifies package/version/signature/pinned identity. APK SHA256 `73fd502be5fd2b7d515a4391fdb02bc16f2aa7b5c4d192e1504e2e4bbaea243f`; ZIP SHA256 `3472bf45b917e5a4ace436a9d2c29d056a7c543024fe990de84eb3e372a08f97`. All67 non-signature CI payload entries match; APK/ZIP CRC and exact ZIP member bytes pass. All314 session-entry APK/ZIP hashes remain unchanged; candidates73/74/75 are also retained. Higher versionCode, stable package/key and explicit12→13 migration support install-over; physical permanent-key phone installation is not inferred from automated checks.

## Features and ownership

Library images/PDFs open in dedicated previews, sharing existing EXIF-oriented bounded decoding, PdfRenderer pages, zoom/pan/reset and retry. Query/filter/sort/list scroll are retained on return; original bytes and Block reference behavior remain.

The reusable Android MediaPlayer controller provides play/pause/stop, source-time seeking/cursor/duration, independent pitch1.0 with50–100% speed in5% steps, whole-file repeat, one-shot selected phrases and A–B repeat. Shared waveform/viewports support real-signal peaks, zoom/pan, separate draggable A/B handles, numeric source-second edits and10ms adjustments. Transport/range readouts stay independent of the scrollable marker list. Foreground/focus/headphone-error handling releases resources without automatic background/process resume; configuration recreation retains active playback. No session, metronome, microphone, take selection or statistics ownership is introduced.

Room **13** adds only indexed `media_markers` through explicit **12→13**; schemas/migrations1–12 are retained. Stable master point/section IDs, labels, optional notes and source-time bounds support add/edit/move/resize/confirmed-delete, seek and section selection. Loading a section does not enable repeat. Restrictive FKs plus transactional owned-marker removal preserve deletion safety and roll back annotations when another asset link blocks deletion.

Waveform analysis streams PCM chunks/codec buffers into5ms peak bins (at most180001floats, ~704KiB/15minutes). Expendable `cache/audio-waveforms` files use mediaID/originalSHA256, validate headers/count/peaks, regenerate on corruption and cap completed caches at12. Originals, L1 formats/limits/integrity, legacy references/takes and byte-preserving export remain under existing ownership. [Architecture and limitations](docs/MEDIA_AUDIO_WORKSPACE.md).

JSON **7** / readers **1–7** remain unchanged and exclude canonical originals/metadata/master annotations and legacy media. UI discloses this boundary. Keep originals separately until S2 complete ZIP recovery; waveform caches are not backup content. No L3 links/adoption, K1, T1–T3 or S2 backup is implemented.

## Actual focused verification

- Platform gate, source76f0d69: [API26 CI41713276728 attempt2](https://github.com/mrayover/practice.logger/actions/runs/41713276728) and [API35 CI41713272359](https://github.com/mrayover/practice.logger/actions/runs/41713272359), one native scenario each, pass MP3/M4A/PCM16 WAV speed100/75/50/95%, independent pitch parameter1.0, source-position advances, repeated closest seeks and pause. First API26 attempt timed out downloading fixture dependencies before compilation.
- Final build [CI41721617835](https://github.com/mrayover/practice.logger/actions/runs/41721617835), source`e12b53f16e42fad98e6aa1a878fd05f70f9e5e75`: **37/37 scoped units**, zero failures/errors/skips; debug/release/instrumentation compilation pass. Includes explicit1→13/11→13/12→13 preservation, master CRUD/reopen/FK/deletion rollback,15minute coordinate math/range validation and existing Library/import/image/recording/phrase/backup safeguards. KSP13 matches the retained exported schema. No broad lint/P5/F1/F2 rerun.
- Source37a9234: [API26/100% CI41716550157](https://github.com/mrayover/practice.logger/actions/runs/41716550157) and [API35/200% CI41716555437](https://github.com/mrayover/practice.logger/actions/runs/41716555437), **four workspace tests plus one cold-reopen per API** all pass. Actual three-format transport/50–75% phrase-repeat/one-shot/full-repeat/EOF/focus/background/error, faithful alternating-signal fifteen-minute waveform/cache regeneration/original hashes, existing Block PDF viewer/no practice-row change, Library image/PDF error/retry/list state and master creation/many dense markers/cold persistence are covered.
- Final UI/inset checks, source`e12b53f16e42fad98e6aa1a878fd05f70f9e5e75`: isolated UI/reopen [API26/100% CI41723099027](https://github.com/mrayover/practice.logger/actions/runs/41723099027) and [API35/200% CI41721633323](https://github.com/mrayover/practice.logger/actions/runs/41721633323) pass. They explicitly assert settled rotation during active A–B playback, retained speed/range, fully bounded narrow(~305dp) transport controls, status-bar-safe Back, at least48dp of marker list and reachable waveform/marker navigation. Screenshots reviewed. Dedicated previews hide the banner and use compact readouts for enlarged text. API26 workflow source d08d721 only adds exact successful-build fixture reuse; its app/test assertions match signed source e12b53f. Earlier controller/waveform/storage/Block code is unchanged. Evidence is scoped across these runs, not a new full regression claim.

Development outcomes retained in WORK_LOG: one fixture JSON error, two runs with the same Kotlin callback-inference compile error, superseded canceled build, one canceled unintended broad push run, search/IME row-lookup failure, and two final API26 fixture-install timeouts before compilation. Keyboard settlement corrected the lookup failure. Settled screenshot review exposed combined large-text/narrow panel starvation and preview top-inset overlap; build76 corrects both and adds marker-area/status-bar-clearance assertions. Final API26 reuses the identical synthetic audio fixtures from successful build41721617835. No assertions were weakened. The independent platform and integrated targeted runs pass; acoustic pitch, very short compressed-loop gap/overshoot, vendor headphones/providers/TalkBack and permanent-key owner-phone install-over remain physical checks. No sample-accurate or perfectly seamless compressed playback is claimed. Stage4 remains0/10 documented sessions.

**Exact next action: owner verification of L2 using its seven-item phone checklist. Stop after L2. The next separately assigned phase is L3 — Block Media Library Associations.** L1 phone acceptance remains separately pending; do not reopen L1 engineering or P5 without defect evidence.

## Previous L1 delivery — retained build72

**0.18.0/build72**, application/release source `ef83bd8ae3e34804dd436cbbdcdf3ac023530e19`. [APK](dist/practice.logger-0.18.0-b72-ef83bd8/practice.logger-0.18.0-b72-ef83bd8.apk) · [ZIP](dist/practice.logger-0.18.0-b72-ef83bd8.zip) · [Build info](dist/practice.logger-0.18.0-b72-ef83bd8/BUILD_INFO.json) · [L1 phone checklist](dist/practice.logger-0.18.0-b72-ef83bd8/PHONE_CHECKLIST.txt). Install over accepted build69 or candidate71 without uninstalling or clearing data. L1 owner-phone verification remains pending; P5 stays complete and accepted.

App ID `com.mrayover.practicelogger.app`; permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. Existing publisher verifies package/version/signature/pinned identity. APK SHA256 `7ee01ca7bca9e96608e019f27ac991e5cb249f07ad1454a7fa024340dbf3db78`; ZIP SHA256 `a93aca60b1ad338441ae93dcc10160be6b454cf019b41fec484ebf9d1cae4edd`. All67 non-signature CI payload entries, APK/ZIP CRC/exact member bytes and298 prior APK/ZIP hashes pass. Existing packages/signing material are preserved; dist/LATEST now points to the L2 delivery above. Internal70 predates integrity hardening and is not the current installation target.

## L1 implementation and ownership

Room **12** adds only indexed media_assets through explicit **11→12**, retaining schemas/migrations1–11 and all existing rows/FKs. KSP export12 identity `219b9e3550bfaafd1b07fb3551935484` matches implementation. Each stable UUID owns one original in files/media-library, independent of Blocks. Streaming SHA256 supports original integrity checking without content-addressed storage/deduplication. Imports reuse existing image/PDF and MP3/AAC-in-M4A/PCM16 WAV validators and resource limits, bounded IO/sync/staging/transactional commit, cancellation and conservative serialized recovery.

Home → Library now provides imports, search over title/original filename, All/Images/PDFs/Audio, stable Name/Most recent ordering, title-only rename, metadata/availability errors and confirmed deletion. Future restrictive durable links block deletion; no premature L3 association tables exist. New files/recovery cannot enter legacy storage. [Implementation and legacy audit](docs/MEDIA_LIBRARY_STORAGE.md).

All legacy block_images/files/block-images and takes/files/recordings retain existing ownership, IDs, labels, Block associations and original bytes. Imported tracks remain playable/renameable/exportable through existing controls; recorded WAV/free M4A are not reclassified. Nothing is disposed or physically copied for migration. In-place adoption requires L3 to update ownership/links, resolution, deletion and recovery together; legacy media is not automatically listed in the new Library.

JSON writer **7** / readers **1–7** remain unchanged and exclude canonical/legacy media metadata and bytes. Library-only installs now block JSON restore transactionally. Library/Backup UI disclose the exclusion; keep originals separately until S2. At L1 delivery, no L2 viewer/player/annotation was implemented; L2 is now delivered above. Complete ZIP backup, L3 Block linking, K1 and Transcribe remain unimplemented.

## Actual focused verification

- [CI41705910512](https://github.com/mrayover/practice.logger/actions/runs/41705910512), storage source `5da4f17`: debug/release/instrumentation compile and **34/34** scoped units pass. Coverage includes explicit1→12/11→12, all old rows/FKs, import limits/provider/content/MIME/space/cancel/finalization/commit recovery, active-import protection, same-size corruption detection, unique ownership/rename/delete, reopen, stable filtering/sorting and Library-only restore across readers1–7. This run later failed a new-test lazy-row lookup; its passing build/unit/storage stages are reported separately. Final build72 only changes Library selections to existing OutlinedSelectionChip and version; storage/validation/restore are unchanged. Final release compilation passes [CI41708998178](https://github.com/mrayover/practice.logger/actions/runs/41708998178).
- API35/200% [CI41705910512](https://github.com/mrayover/practice.logger/actions/runs/41705910512) and API26/100% [CI41706664910](https://github.com/mrayover/practice.logger/actions/runs/41706664910): actual immutable build69 seed → install71 without clearing, raw legacy metadata/byte hashes, imported playback/rename/export, original Block protections and canonical native format/failure/dependency checks pass. Both stop later at test-only offscreen row lookups.
- Isolated Library/reopen checks pass on API26/100% [CI41709010676](https://github.com/mrayover/practice.logger/actions/runs/41709010676) and API35/200% [CI41709004489](https://github.com/mrayover/practice.logger/actions/runs/41709004489): real picker cancel/success, imports/failure, search/filter/sort/rename, canceled/confirmed deletion, recreated query, portrait/landscape controls and cold process restart with original canonical/legacy hashes.
- All **five distinct native classes pass on each API**, across targeted runs per API (**12 selected successful executions**, including repeated seeds). There is no single all-green final full matrix claim. Final Library/reopen tests compile and execute build72; earlier ownership/storage checks execute the identical storage core. Production code is unchanged since `ef83bd8ae3e34804dd436cbbdcdf3ac023530e19`; later commits update records. Final native test/source SHAs are recorded in BUILD_INFO/WORK_LOG. Screenshots were inspected. Earlier failures/cancellation and fixes are retained in WORK_LOG.

No P5/F1/F2 full regression, broad lint rerun or owner-phone/provider/vendor/acoustic acceptance is claimed. Stage4 remains0/10 documented real sessions. Physical phone checks are separate from automated evidence.

L1 owner verification using its six-item phone checklist remains pending. L2 has since been delivered; its actual verification and exact next phone task are above. P5 remains complete and accepted.

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
