# Current state — Playground control refinement

November 18, 2026 (America/Los_Angeles). **0.60.1 / build 168** gives Playground a connected dark-purple D-pad with four separate direction targets and an inert center. Circular B sits lower-left of A. The controls occupy the same 96 dp row as build 166. Direction holding, release, cancellation, A input and reserved B behavior use the existing logic. [Playground behavior](docs/PLAYGROUND_SPEC.md).

**Source and package.** The release was assembled in the canonical Windows checkout from `6a9e0169c210a97abed32d534e270d99e5b7957a` on `block-ux-stage1`. [APK](dist/practice.logger-0.60.1-b168-6a9e016/practice.logger-0.60.1-b168-6a9e016.apk) · [ZIP](dist/practice.logger-0.60.1-b168-6a9e016.zip) · [Phone checklist](dist/practice.logger-0.60.1-b168-6a9e016/PHONE_CHECKLIST.txt) · [Build metadata](dist/practice.logger-0.60.1-b168-6a9e016/BUILD_INFO.json). APK SHA-256 `65cefccd02476f3600639156e19e1b52650baeb3a09f80010443374337673605`. APK v2/v3 and the v4 sidecar verify against the pinned permanent certificate.

**Focused verification.** Release assembly, vital lint, Android-test compilation and two host movement tests passed. The Android workflow passed at 1440 × 3120 (Galaxy S26 Ultra profile, API 36) and 720 × 1440 (320 × 640 dp, API 35). It verified the connected cross, separate arm targets, inert center, sustained movement and stopping, A and B behavior, the diagonal button spacing and the fixed no-scroll page. Both profiles match build 166's page, viewport and control-row bounds exactly. The Light-theme viewport pixels also match build 166 on both profiles. [Galaxy workflow](https://github.com/mrayover/practice.logger/actions/runs/44421571503) · [Compact workflow](https://github.com/mrayover/practice.logger/actions/runs/44421579384) · [Bounds comparison](dist/practice.logger-0.60.1-b168-6a9e016/evidence/gameboy-galaxy/PLAYGROUND_BOUNDS_COMPARISON.txt) · [Cosmic screenshot](dist/practice.logger-0.60.1-b168-6a9e016/evidence/gameboy-compact/playground-controller-cosmic.png).

**ZIP and local delivery.** ZIP SHA-256 `d669a5fe8d4ab4a1b4f9f44f21d63e8d81e5347d5023c17abf53e8db2d30bbf0`. All 16 archive members and the local OneDrive delivery copies at `C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Playground Home delivery/build168` match by checksum.

**Exact next task.** Install build 168 over the permanent-key app on the owner's Galaxy S26 Ultra and complete the linked phone checklist. Physical acceptance remains pending.

---

## Previous delivery — Playground Home Foundation

November 18, 2026 (America/Los_Angeles). **0.60.0 / build 166** opens a fixed portrait Playground from Log. It includes a tiled room, four-direction movement with wall collision, A interaction input and a read-only inventory overlay. The inventory observes saved completion colors and counts all earned Blocks, including Neutral. Existing completion and arrangement rows, Room 29, Complete ZIP 14 and JSON 7 are preserved. [Behavior](docs/PLAYGROUND_SPEC.md).

**Source and package.** The release was assembled in the canonical Windows checkout from `5044fab5308c791ac4abca10768d23953a28c4ba` on `block-ux-stage1`. [APK](dist/practice.logger-0.60.0-b166-5044fab/practice.logger-0.60.0-b166-5044fab.apk) · [ZIP](dist/practice.logger-0.60.0-b166-5044fab.zip) · [Install checklist](dist/practice.logger-0.60.0-b166-5044fab/PHONE_CHECKLIST.txt) · [Build metadata](dist/practice.logger-0.60.0-b166-5044fab/BUILD_INFO.json). App ID `com.mrayover.practicelogger.app`. APK SHA-256 `eb7f232514531502948fae1d211b0d99d92ce77f421ad58b28b8b43a700dd6ac`. APK v2/v3 and the v4 sidecar verify against the pinned permanent certificate. All 180 APK payload entries match the unsigned release.

**ZIP and local delivery.** ZIP SHA-256 `7b6f8c73225495c2bbc7da7432c4bb51e1cc120348e797276a31a50eb8373514`. All 13 archived files match their extracted copies by checksum. The local OneDrive delivery copies at `C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Playground Home delivery/build166` match the canonical files. The 757 pre-existing APK/ZIP files retain their sizes and timestamps. Build 162's APK and ZIP still match their recorded hashes.

**Focused verification.** Release assembly, vital lint, Android-test compilation and 18 focused host tests passed. The host checks cover wall and corner clamps, elapsed-time movement, saved colors, exact completion and layout backup round trips, ZIP 11 fallback and malformed layout rejection.

**Portrait verification.** The same Android workflow passed on the signed application source at 1440 × 3120 px and 560 dpi (Galaxy S26 Ultra resolution emulated on API 36) and at 720 × 1440 px and 360 dpi (320 × 640 dp on API 35). The measured viewport took 68% and 52% of the Playground page respectively. Both runs checked full destination labels, visible controls, swipes that leave the page fixed, sustained movement in four directions, release and gesture cancellation, wall collision, A input, inventory opening and dismissal, live Neutral and total counts, and unchanged arrangement rows through recreation. A compact API 36 attempt was blocked by an emulator System UI dialog. [Galaxy-sized workflow](https://github.com/mrayover/practice.logger/actions/runs/44410132830) · [Compact workflow](https://github.com/mrayover/practice.logger/actions/runs/44411140394) · [Galaxy-sized screenshot](dist/practice.logger-0.60.0-b166-5044fab/evidence/galaxy-s26-ultra-final/playground-home.png) · [Compact screenshot](dist/practice.logger-0.60.0-b166-5044fab/evidence/compact-phone-api35/playground-home.png) · [Inventory screenshot](dist/practice.logger-0.60.0-b166-5044fab/evidence/compact-phone-api35/playground-inventory.png).

**Build 166 phone status.** Physical acceptance was pending at delivery.

---

## Previous delivery — Weekly planned and actual practice minutes

November 18, 2026 (America/Los_Angeles). **0.59.1 / build 162** derives the Home footer and Goal weekly summaries from the existing This Week plan and G3 practice facts. Edit weekly plan opens Prepare Week. Existing percentages and weekly targets remain in Room 29 and supported backups. Complete ZIP14 and JSON7 retain their existing behavior. [Accounting and historical attribution](docs/PRACTICE_STATISTICS.md#weekly-planned-and-actual-minutes).

**Source and package.** Build 162 was assembled locally in the canonical Windows checkout from `436e356f95874baffbe9d526c3b35e2af28e3c79` on `block-ux-stage1`. [APK](dist/practice.logger-0.59.1-b162-436e356/practice.logger-0.59.1-b162-436e356.apk) · [ZIP](dist/practice.logger-0.59.1-b162-436e356.zip) · [Install checklist](dist/practice.logger-0.59.1-b162-436e356/PHONE_CHECKLIST.txt) · [Build metadata](dist/practice.logger-0.59.1-b162-436e356/BUILD_INFO.json). App ID `com.mrayover.practicelogger.app`. APK SHA-256 `3e16abc6a5c89dc22877879c5e0c3363c6ce579b2167fb1581d644cabcb85e38`. APK v2/v3 and its v4 sidecar verify against the pinned certificate. Signed candidates 160 and 161 are retained locally.

**ZIP and local delivery.** ZIP SHA-256 `ab2abeec6f8ba8c681258944619be015321bad8d37a7187d4952600e8b5f35a3`. The extracted APK, signature sidecar and checked evidence match the original files by checksum. Local OneDrive APK and ZIP copies match the canonical distribution at `C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Weekly Plan delivery/build162`.

**Focused verification.** Release assembly, vital lint, Android test compilation and 25 host checks passed. Two API 35 workflow tests passed on the signed application source, covering live repetition edits, duration and Goal-link changes, unassigned practice, historical attribution, recreation, removal and legacy target restore protection. A host test verifies the local Sunday boundary across daylight-saving time. [API 35 workflow](https://github.com/mrayover/practice.logger/actions/runs/44392266094).

**Visual verification.** Matched build 159 and build 162 captures at 360 × 800 and 411 × 886 in Cosmic and at 320 × 640 with 200% text preserved the measured Home bounds, scroll range, navigation, Goal text baselines and track geometry. The long-number capture shows both Goal values fully on one line. At 200% the previous wrapped subtitle height remains reserved. The formerly trackless 411 × 886 Light case preserved all prior measured bounds and scroll range. Its empty track uses space within the original footer. [Before/after images](dist/practice.logger-0.59.1-b162-436e356/evidence/HOME_BEFORE_AFTER.png) · [Bounds comparison](dist/practice.logger-0.59.1-b162-436e356/evidence/HOME_BOUNDS_COMPARISON.txt) · [Before run](https://github.com/mrayover/practice.logger/actions/runs/44392110450) · [After run](https://github.com/mrayover/practice.logger/actions/runs/44392261674).

**Build 162 phone status.** Physical acceptance was pending at delivery.

---

## Previous delivery — C6B MIDI export

November 17, 2026 (America/Los_Angeles). Build **0.59.0 / 159** adds editable **Standard MIDI File Type 1** from the live Current Loop or Full Arrangement through Composer's Export dialog. It preserves the 960 PPQ timing, tempo, meter and separate populated instrument tracks, including General MIDI drums on channel 10. WAV and M4A remain available in the same dialog. [MIDI behavior and limits](docs/COMPOSER_C6B.md).

**Source.** The canonical Windows checkout and GitHub development branch are `block-ux-stage1`. Build 158's archived patch was published unchanged at `5bd677c94b98773d2414c2fdada01688862058e9` before C6B. Reapplying the patch to the recorded release base independently reproduced the original build 158 tree `612d2129824a35eb076304f4fad83b37b5ae71da`. Build 158 was preserved without a rebuild. Build 159's signed application source is `b3944e133ecceeebe1c185186c330d5e1362f1bb`.

**Signed package.** [APK](dist/practice.logger-0.59.0-b159-b3944e1/practice.logger-0.59.0-b159-b3944e1.apk) · [ZIP](dist/practice.logger-0.59.0-b159-b3944e1.zip) · [Install and Galaxy checklist](dist/practice.logger-0.59.0-b159-b3944e1/INSTALL.txt) · [Build metadata](dist/practice.logger-0.59.0-b159-b3944e1/BUILD_INFO.json). Package `com.mrayover.practicelogger.app`, version 0.59.0, code 159. APK SHA-256 `fe71bdba94f5296d30c393cd7e4bb17a2b6fa0d86de7bff1e39d7a7b4edfefb3`. APK v2/v3 and its v4 sidecar verify against the pinned permanent certificate `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. Install as an update to the existing permanent-key app.

ZIP SHA-256 `8903e1022b49924c60076ecc59521ad429e172d708a15dc84925787bebad87b1`. The ZIP round trip and local OneDrive copies matched the canonical files by checksum. OneDrive delivery is `C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Compose C6B delivery/build159`. [Delivery report](dist/practice.logger-0.59.0-b159-b3944e1/DELIVERY_REPORT.md).

**Focused verification.** Local release assembly, vital lint, four MIDI host tests and five existing audio-export host tests passed on the signed source. The API 35 failed-destination method passed in the initial run. The final picker method passed genuine Downloads exports for both scopes, playback continuation, cancellation and draft, saved project and Undo preservation. OpenJDK Java Sound read both files as Type 1, 960 PPQ, with the unsaved note and three named instruments, and verified 15,360 Loop ticks, 26,880 Arrangement ticks, channel 10 percussion and the Make Unique variation at tick 23,040. The final picker test commit `3847937cf8b255b029e6a84dfbef0da6bebf173e` adds only a test change relative to the signed source. The earlier picker attempts encountered the Android full-screen tutorial, a provider-added `.rtx` suffix fixed in the signed source, and a stale test node. [Destination failure check](https://github.com/mrayover/practice.logger/actions/runs/44242399358) · [Final picker check](https://github.com/mrayover/practice.logger/actions/runs/44244721891).

**Build 159 phone status.** Owner Galaxy and Dorico acceptance remain unverified. C7, MusicXML and F1 remain deferred. Composer projects and custom presets remain outside practice backup.

---

## Build 157 reconciliation and build 158 provenance

November 17, 2026 (America/Los_Angeles). The published application source on `block-ux-stage1` includes This Week repetition targets and Planned Minutes, M4A import compatibility, and Block/Source organization. Build 157 release source is `e2eb8bd6db987a612a65865f194f0dda270eba62`. Its full tree `9832c7f115f3a1ce46c38a00191cda9703ac65a0` matches both build metadata and OneDrive integration commit `8bc3084d38080d22f2c5059c816d669d321bdc65`. The published MIME and weekly commits are `90ff01d` and `b069789`. See [weekly repetitions](docs/WEEK_REPETITIONS.md) and [Block organization](docs/BLOCK_ORGANIZATION.md).

**Windows checkout history.** The prior reconciliation could not write `.git/FETCH_HEAD` from the older checkout at `bc7b602ff4991341b825e262629c64f201a94db8`. The C6B session opened the synchronized canonical branch at `d0fe942ea062799238ff0ebc125556dd42e42262` with the dialog patch preserved in the build 158 distribution. The older OneDrive main checkout and archived commits were left in place.

**Signed build 157.** The canonical [APK](dist/practice.logger-0.58.2-b157-e2eb8bd/practice.logger-0.58.2-b157-e2eb8bd.apk), [ZIP](dist/practice.logger-0.58.2-b157-e2eb8bd.zip), signature sidecar, [metadata](dist/practice.logger-0.58.2-b157-e2eb8bd/BUILD_INFO.json), and [install instructions](dist/practice.logger-0.58.2-b157-e2eb8bd/INSTALL.txt) match the OneDrive delivery byte for byte. The original ZIP contains the APK, metadata and install instructions. The verified v4 sidecar is alongside the loose APK. The APK reports `com.mrayover.practicelogger.app`, version `0.58.2`, build `157`. Its v2/v3 signatures and v4 sidecar verify with the pinned permanent certificate shared with build 156. APK SHA-256 is `b99f746ba5124a6ced4f765d684a5bedf101eecabfa43f663d3b2e45b78a822d`. ZIP SHA-256 is `d65a4b051e94ec6f4ce3a10eeb8f7bb26d90350ca72f6696d4b1b2d06aba5431`.

**Focused verification.** [Build 157 workflow](https://github.com/mrayover/practice.logger/actions/runs/44223220074) passed its diagnostic job. The normal build, current-features and device jobs were skipped. [M4A workflow](https://github.com/mrayover/practice.logger/actions/runs/44224720276) checked out the exact release commit and passed one API 35 Android test covering a valid AAC file with an alternate provider MIME and rejection of malformed bytes. The verification branch adds only its workflow. Physical Galaxy acceptance of build 157 is pending.

**Build 158.** Signed `0.58.3 / 158` and its [source patch](dist/practice.logger-0.58.3-b158-e2eb8bd/SOURCE.patch) were preserved. The published patch is based on build 157. Galaxy landscape and Samsung Keyboard acceptance for the inherited dialogs remains pending. [Build 158 install and phone checklist](dist/practice.logger-0.58.3-b158-e2eb8bd/INSTALL.txt).

---

## Historical delivery records

The dated records below describe earlier checkout, delivery and verification states.

# Build156 delivery/publication correction

The signed APK/ZIP already exist in `C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/This-Week-delivery/build156`; File Explorer now visibly confirms them. OneDrive currently reports Sync pending. No download is required. Application source is committed locally at **3f619b5efa5a8256efdfd7b2fee1a1d6f074ccc1**; the earlier uncommitted status below is superseded.

The owner explicitly authorizes Git commit/push. Actual publication remains blocked: CLI networking fails, while the connected GitHub API reads the live branch at **bc7b602ff4991341b825e262629c64f201a94db8** but denies blob writes with **403 Resource not accessible by integration**. No branch ref was changed. The canonical Projects checkout rejects writes; all verified source and distributions remain preserved in the writable checkout and OneDrive delivery folder. Exact next task: publish the already-committed source when a writable Git connection is available. No rebuild or repeated testing is needed.

---
# This Week repetitions - implemented, signed local build156

November17,2026. Signed local **0.58.1/build156** adds weekly repetition targets (new membership3, minimum1, editable in expanded This Week/Prepare Week rows), collapsed dots/squares with bounded numeric condensation, completed/target counts, target-gated completion slit, and configured Planned Minutes below the heading. The unchanged placement completion/Playground pipeline remains uncapped. Room28 explicitly migrates existing memberships to target1 without changing history/rewards. Complete ZIP13 preserves targets and reads frozen ZIP1-12; JSON7 is unchanged. [Behavior/files](docs/WEEK_REPETITIONS.md).

**Verification:** release compilation, generated Room28 schema and vital lint pass. Three focused methods pass through direct JUnit/Robolectric API35: repetition/reward/target/duration/removal/backup/reopen8.794s, explicit27→28 history/layout preservation0.290s, local-week completion boundaries/counts0.023s. No broader suite, instrumentation, screenshots or phone acceptance is claimed. Standard Gradle test workers cannot connect to localhost in this environment; direct execution uses the existing cached Android jar and a short writable temporary directory. A migration-fixture missing optional indices was corrected; only test sources were recompiled afterward, with application compilation up-to-date.

**Delivery:** `dist/practice.logger-0.58.1-b156-e5e4373/` contains the permanent-key APK, BUILD_INFO, SOURCE.patch, report and short phone check; matching ZIP is alongside it. APK SHA256 **e162e86f71fd849a64d8b19e788e32b22e3248e7d3a68e2074a027bd9d18821d**. Package/version, signature and pinned permanent certificate pass. The existing publisher was reused through a temporary adaptation that reads the canonical signing/tools paths and records base-plus-patch provenance; no key copy/change or canonical distribution write. No production data was touched.

Work is local in the existing writable `m4a-import-fix` checkout, based on **e5e4373** (the prior M4A fix). SOURCE.patch captures the exact application/test/schema changes and behavior document; BUILD_INFO records its hash. Changes are **not committed or pushed**; canonical `C:/Users/mrove/Projects/practice.logger` remains unchanged. Git remote verification failed with `getaddrinfo() thread failed to start`. The prior M4A source now compiles with this candidate, but its Android import method remains unrun. No Composer implementation was changed.

**Exact next task:** synchronize the verified weekly-repetition source from the writable checkout into canonical block-ux-stage1 using the owner's GitHub Desktop workflow. Physical-phone visual/install acceptance remains unobserved. Stop before unrelated phases.

---

# Retained M4A work record before the weekly-target build

November17,2026. Candidate 0.58.1/build156 changes only the M4A provider-MIME gate: audio/*, video/mp4 and application/mp4 hints can reach the unchanged inspectImportedAudio validator. MP3/WAV and visual/PDF MIME rules are unchanged. Actual codec, single audio track, decoded duration, malformed-content and size validation remain in force. Samsung's actual declared MIME is unknown.

One focused Android method is added, using the unchanged C6A build155 loop-native.m4a fixture: accept valid AAC M4A declared video/mp4, then reject malformed bytes under the same filename/MIME hint after opening the stream. The test has NOT run. Compilation is NOT verified: wrapper cache lock access was denied; a writable-cache attempt failed connecting to Gradle's local daemon. ADB also could not start. Shell Git networking fails with getaddrinfo() thread failed to start. No signed156 APK/ZIP exists; the last delivered build remains155 below. No regression, screenshots, production-data or signing changes.

Work is isolated in C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/m4a-import-fix from canonical branch baseline bc7b602. The canonical Projects checkout and older OneDrive main checkout were not edited. Exact next task: run M4aImportCompatibilityTest on one Android device, compile, sign and deliver156 using the permanent publisher, then synchronize the canonical checkout. Stop before MIDI/Library/backup/Composer phases.

---
# Current state — C6A delivered as signed build155

November16,2026 (America/Los_Angeles). Signed **0.58.0/build155** adds **Export Audio** for Current Loop and Full Arrangement, using default AAC-LC M4A (stereo48kHz/192kbps) or uncompressed PCM16 RIFF WAV (stereo48kHz). The current unsaved draft, actual per-track Loop selections and mixer/master are frozen without saving or changing revision/Undo. Rendering runs offline in bounded chunks using the existing scheduler, SoundFonts and channel DSP, with natural release tails, standard Android document destination, progress/cancellation and cleanup. [Behavior and limits](docs/COMPOSER_C6A.md).

Canonical C:/Users/mrove/Projects/practice.logger, **block-ux-stage1**. Initial clean local/live remote f511afa4763d1b3350b1b1463a223be18fa7cc65. Application/APK and final focused-workflow source **0696ebbb391e32cfd5d3997a516a3cc61eef413c**. Closing publication changes records only; the live remote is verified at handoff. Older OneDrive source checkout untouched; no main merge.

[APK](dist/practice.logger-0.58.0-b155-0696ebb/practice.logger-0.58.0-b155-0696ebb.apk) · [ZIP](dist/practice.logger-0.58.0-b155-0696ebb.zip) · [Actual results/report](dist/practice.logger-0.58.0-b155-0696ebb/DELIVERY_REPORT.md) · [Export dialog](dist/practice.logger-0.58.0-b155-0696ebb/EXPORT_DIALOG.png) · [Phone checklist](dist/practice.logger-0.58.0-b155-0696ebb/PHONE_CHECKLIST.txt).

Delivery: **C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Compose C6A delivery/build155**. Install over the existing permanent-key application; do not uninstall or clear data. dist/LATEST.txt points here. Complete ZIP roundtrip (37 files), all37 ZIP-entry CRC/size checks and all local delivery-copy hashes pass; cloud synchronization is unobserved. Candidates153/154 and all previous distributions retained.

- APK SHA256 **4eb49d18e35ba79533d9ba727dcf778fb8e1a35fb9c8db3c4452c0e2958a455f**; ZIP SHA256 **8cf10116ba4845603dce7746be44817424d90dccd4d2461f28e6a61b03c91d50**. Stable package/version/signature/pinned permanent certificate verified. All180 unsigned payload entries and nine original SF2 hashes match. **719** earlier APK/ZIP hashes unchanged.
- **Five focused host methods pass** (final5.446s): stereo PCM16 WAV bytes/header/length, independent4/2/1-bar Loop repetitions and finite endpoint, frozen unsaved pattern/mixer/source safety, arrangement/variation positions and distant-clip refusal/filename/storage arithmetic. Final release/vital lint and Android-test assembly pass.
- **Three isolated API35 methods pass in [final155 workflow 44027324342](https://github.com/mrayover/practice.logger/actions/runs/44027324342)**: native WAV/AAC files with real MediaExtractor/decoder, source/timing/gap/tail/mixer representatives, cancellation/retry; destination-write and encoder setup failure/resource recovery; actual Loop M4A and Arrange WAV document-picker export with unsaved-draft/Undo preservation and subsequent normal playback. Actual dialog/completion/returned-editor captures inspected. Loop WAV220,928frames/883,756bytes; M4A114,849bytes decodes223,232frames (full source plus48ms codec padding); Arrange336,000frames with exact gap silence; custom500ms release72,064frames. Native5.672s, safety3.751s, picker32.321s.
- Initial44023327415 failed on a valid percussion bank's long release declaration and a test-only nested main-thread helper.154 corrects release handling, descriptor/cancellation/startup-recovery cleanup and adds safety checks.44024234965 ended when the system MediaProvider FUSE service aborted/reset shared storage; the test's shell-copy/external staging was replaced with read-only transfer/internal staging.44025282885 stopped before tests on an omitted debug-only workflow condition; corrected. Run44025462238 passes native/safety before its picker helper hits a stale Downloads node. Picker-only44026552153 passes32.703s after a test-only navigation correction. Final155 adds ≤64ms AAC block/flush padding after packet-table inspection exposed omitted partial endings; final155 repeats the scoped three methods with a stricter complete-frame assertion. Failed evidence is retained; no full-workflow pass is inferred from earlier failed runs.

No project codec/schema/practice data/signing-key change, Library import, new effects, MIDI or portable recovery. Long external exports can exceed Library50MiB/15-minute import limits. Composer projects/custom presets remain outside practice ZIP/JSON backup until C7. Physical-phone acoustics, nonlocal provider behavior and cloud synchronization remain unobserved. No deferred C4E campaign, broad regression/device matrix or F1 was run.

Current owner scope clarification: **MusicXML is deferred from embedded compose.logger V1 planning**; historical approval wording is retained with the current decision explicit. **Exact next task: owner-phone C6A acceptance of build155.** Stop before C6B Standard MIDI File Type1 export, the next separately authorized phase.

---

# Retained C5C baseline — signed build152

November16,2026 (America/Los_Angeles). Signed **0.57.0/build152** adds selected-clip **Edit Pattern** and **Make Unique** to Arrange Actions. Edit Pattern directly targets the referenced instrument/pattern in the existing Loop editor without changing the document or adding Undo. Make Unique uses existing deep-copy semantics and a single validated edit; one Undo removes the variation and restores the reference. Arrange zoom, horizontal/vertical position and clip selection survive mode navigation. [Behavior](docs/COMPOSER_C5C.md).

Canonical C:/Users/mrove/Projects/practice.logger, **block-ux-stage1**. Initial clean local/live remote b83ebdae6bce139a544398fce76270a133c164cc. Application/APK source **f7a8f67d0cae4215bc33910c03ac13449d765418**; final focused workflow source **19fcdfd73d5342d426e0a2895f5d24acec2046b3**. Closing publication changes records only; actual live remote SHA is verified at handoff. Older OneDrive source untouched; no main merge.

[APK](dist/practice.logger-0.57.0-b152-f7a8f67/practice.logger-0.57.0-b152-f7a8f67.apk) · [ZIP](dist/practice.logger-0.57.0-b152-f7a8f67.zip) · [Report](dist/practice.logger-0.57.0-b152-f7a8f67/DELIVERY_REPORT.md) · [Original/unique screenshot](dist/practice.logger-0.57.0-b152-f7a8f67/MAKE_UNIQUE.png) · [Phone check](dist/practice.logger-0.57.0-b152-f7a8f67/PHONE_CHECKLIST.txt).

Delivery: **C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Compose C5C delivery/build152**. Install over the existing permanent-key app without uninstalling or clearing data. dist/LATEST.txt is current. Complete ZIP roundtrip and all local delivery-copy hashes pass; cloud-sync completion unobserved. All previous distributions retained.

- APK SHA256 **ee6d57a988dab8991a51ac9e8ed1deeb6f8f4e184ac0f9440656a5e7cc853e14**; ZIP SHA256 **96a137b3ca68aa60d6bbafcc8147ec2aef00c7f9ee71b645e66839b10aadb5b5**. Stable package/version/signature and pinned permanent certificate verified. All180 unsigned payload entries and nine original SF2 hashes match. **715** prior APK/ZIP hashes unchanged.
- **Eight focused host methods pass**: ArrangementPatternTest3 and ComposerManagementTest5. Coverage: singleton/multiple references, independent note/pattern IDs and musical data, mixer preservation, source/variation edits, correct song expansion, collision refusal,256-pattern/global-note budget refusal without mutation, supported v1–v4 exact save/reopen and Save As reference/ownership mapping. Release/vital lint and Android-test assembly pass.
- [Bounded API35 workflow 44017475358](https://github.com/mrayover/practice.logger/actions/runs/44017475358) passes selected-clip Make Unique during song playback, safe Stop, real single Undo, direct track/pattern navigation without project/history changes or branded reentry, normal note editing, independent source/variation edits, one-bar initial fit, rendered zoom/both-axis/selection retention, Loop/song playback, Save/reopen and Save As. Separate force-stop/cold-process reopen passes exact equality. Actual original/unique/Loop/returned/cold screenshots inspected. See report for timing and any test-only corrections.

No codec/schema/storage/native DSP or worker pending-update changes. Build151 Loop live notes/one-bar fit and C5B playback/mixer remain. Existing pattern-length collision refusal applies. Composer projects/presets remain outside practice ZIP/JSON recovery until C7. Physical phone touch/acoustic acceptance and cloud sync remain unobserved. No deferred C4E campaign, broad regression, device matrix, F1 or exports.

**Exact next task: owner-phone C5C acceptance of build152.** Stop here. C6A offline WAV and M4A/AAC backing-track export requires a separate assignment.

---

# Current state — Loop live notes and one-bar fit delivered

November16,2026 (America/Los_Angeles). Signed **0.56.1/build151** delivers only the bounded Loop UX increment. With Loop Repeat enabled, musical note edits and Undo activate at each affected track's own next pattern cycle without restarting the shared transport/output or changing its mixer. Empty patterns and independent lengths are supported. The measured meter-aware piano roll initially fits one complete bar; relative zoom/tick scroll are retained per pattern through note edits and Loop/Mixer navigation. [Implementation and limits](docs/COMPOSER_LOOP_LIVE.md).

Canonical C:/Users/mrove/Projects/practice.logger, **block-ux-stage1**. Initial clean local/live remote29486421b96da4ca1ffafa6340b0b8f2d56ea034. Signed application source **f35f7e40ef2ed85247c568578ce4e3209b6f0085**; final corrected viewport test/workflow source **dc73178fc78d5c1a0186333429f94ee58fdfa620**. Application sources match151; closing publication changes documentation only and actual remote is verified at handoff. Older OneDrive source untouched; no main merge.

[APK](dist/practice.logger-0.56.1-b151-f35f7e4/practice.logger-0.56.1-b151-f35f7e4.apk) · [ZIP](dist/practice.logger-0.56.1-b151-f35f7e4.zip) · [Actual results/report](dist/practice.logger-0.56.1-b151-f35f7e4/DELIVERY_REPORT.md) · [One-bar screenshot](dist/practice.logger-0.56.1-b151-f35f7e4/ONE_BAR_FIT.png).

Delivery: **C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Compose Loop UX delivery/build151**. Install over the existing permanent-key app without uninstalling/clearing data. dist/LATEST.txt points here. ZIP full-file roundtrip and all local delivery-copy hashes pass. Cloud-sync completion is unobserved. Candidate150 and all earlier distributions retained.

- APK SHA256 **62fe8ead3e3f251882688953be07738e6337da5bf2f98afcbd526cc9fd56112c**; ZIP SHA256 **bc909ac6f5d6bd247512b0e5aad9488070650ef679a97d1755e0a6f2294da16f**. Stable package/version/permanent certificate/signature,180 payload entries and nine original SF2 hashes verified. **711** prior APK/ZIP hashes unchanged, including150.
- **14 focused host methods pass across initial and corrected-class runs**: LoopLiveUpdates6, LoopPlan5, SongPlan3. Initial fixture reused a note ID (13/14); corrected new class passes6/6. Independent cycles, ordering, coalescing/Undo, silent/empty updates, stale/structural rejection, validation and meter/geometry coverage pass. Final151 release/vital lint and Android-test assembly pass.
- [Native44009748701](https://github.com/mrayover/practice.logger/actions/runs/44009748701) passes actual scheduled PCM: changed pitch/velocity, added notes, empty-cycle zero samples, restore and Stop silence (4on/4off). Its later width assertion excluded resize-handle pixels; corrected only extent measurement. [UI44010614061](https://github.com/mrayover/practice.logger/actions/runs/44010614061) passes4/4 fit/capture, draw/move/pitch/resize, real worker add/Undo/empty activation with the same plan and advancing AudioTrack position, Stop cleanup, Zoom+, Mixer retention and Hand scroll, before its faulty pinch assertion. Those passed portions are reused for unchanged151 paths; no full-workflow pass claimed.
- Final [isolated151 viewport44013141015](https://github.com/mrayover/practice.logger/actions/runs/44013141015) **passes15.271s**: correctly paired pinch, scrolling, controls, Loop/Mixer view retention, fresh pattern fit,7/8 sixteen-bar pattern and narrower landscape. Actual3120x1440 and2400x1440 captures inspected; one bar fills the editing width. Earlier4/4 capture shares unchanged initial-fit code. Intermediate44011707148 repeated a crossed-finger fixture;44012692351 stopped before emulator on ABI-induced APK naming. Corrected named gesture arguments/restored standard debug naming. Failed-run evidence retained; no broad reruns.

No project codec/schema/storage/native DSP change. Mixer/Arrange snapshot behavior and instrument mapping remain. **Repeat off: edits may wait until the next Play.** Structural changes retain safe snapshots; explicit pattern selection may restart. Already-buffered Android audio cannot be rewritten. Physical phone acoustics/touch/routing acceptance remains pending. Composer projects/custom presets remain outside practice ZIP/JSON recovery until C7. No deferred C4E campaign, comprehensive regression or F1 performed.

**Exact next task: owner acceptance of build151 Loop live notes and one-bar fit.** Hard stop here. Do not begin C5C, exports or unrelated work; C5C remains the next separately scoped assignment.

---

# Current state — C5B delivered as signed build149

November 15,2026 (America/Los_Angeles). **0.56.0/build149** makes Arrange a compact song workspace: every composition track has a 52dp lane, including empty tracks; legacy automatic subtitles are suppressed; empty-lane taps choose that instrument's populated patterns. Play/Stop use explicit clips, with audio-clock playhead and the existing instrument mixer. Whole-bar dragging, Undo and saving remain; Actions adds atomic Duplicate / 2–8 additional Repeat. [Behavior](docs/COMPOSER_C5B.md).

Canonical C:/Users/mrove/Projects/practice.logger, branch **block-ux-stage1**. Initial clean local/live remote 39384635652d18a3c53620001561e94d16fda59a. Application/APK source **47602d3a6e1d9a42d91904419e75741b42027760**; full-workflow corrected test source **fef6b3bdf735f49ea6fc4c99bf5d8d2e6d94ca7b**; isolated transport test source **47602d3a6e1d9a42d91904419e75741b42027760**. All final application/build sources match the signed 149 source. Build 148 workflow coverage is reused for unchanged arrangement/audio/native/editing/persistence; final 149 isolated transport also checks independent Loop Repeat after song. Closing publication adds documentation only; actual remote SHA is verified at session close. Older OneDrive source checkout untouched; no main merge.

[APK](dist/practice.logger-0.56.0-b149-47602d3/practice.logger-0.56.0-b149-47602d3.apk) · [ZIP](dist/practice.logger-0.56.0-b149-47602d3.zip) · [Delivery report](dist/practice.logger-0.56.0-b149-47602d3/DELIVERY_REPORT.md) · [Phone checklist](dist/practice.logger-0.56.0-b149-47602d3/PHONE_CHECKLIST.txt).

Delivery: **C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Compose C5B delivery/build149**. Install over the existing permanent-key app; do not uninstall or clear data. dist/LATEST.txt points here. Local delivery-copy hashes match; OneDrive cloud-sync completion unobserved. Prior distributions retained.

- APK SHA256 **64ba0d99bdd4bdeb214864d2ded0a1a742ff2370d9fd898e9b54bbc91ae5f8fd**; ZIP SHA256 **234b41178c3c4b826b940cb131921911df3b443b05059aaf04c6df037d9e3f46**. Package/version/signature and pinned permanent certificate f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830 verified. All 180 unsigned payload entries, nine original SF2 hashes,703 previous APK/ZIP hashes and ZIP exact-byte round trip pass.
- 13 focused host methods pass locally and in [initial run 43800804324](https://github.com/mrayover/practice.logger/actions/runs/43800804324): SongPlanTest (3), LoopPlanTest (5), ArrangementEditsTest (5). Release/vital lint and Android-test assembly pass. [Build 148 device follow-up 43801815937](https://github.com/mrayover/practice.logger/actions/runs/43801815937) covers packaged native song PCM and the bounded API35 editing/playback/Mixer/save/recreation workflow plus separate exact cold reopen. Actual two-track, eight-track/scrolled and narrow-landscape screenshots inspected. Physical-phone acceptance is separate.
- Native energy by Bar 1–4=[30019340,60726808,0,55121384], late-Bar 3 gap 0. Exactly 4 on/4 off events/velocities, simultaneous instruments, repeated references, finite endpoint, independent same-pitch release and immediate Stop silence pass. Full UI method: 28.318s; cold reopen: 6.487s. Live Volume/Pan/Master keep the same song snapshot. Mute/Solo/EQ/Width remain on the unchanged shared mixer path; no new full FX campaign claimed.
- [Isolated transport 43803202774](https://github.com/mrayover/practice.logger/actions/runs/43803202774) verifies two rendered green cursor positions against consumed audio frames, meaningful renamed lane, disabled Play while running, natural non-repeating endpoint and Stop, plus independent Loop Repeat after song (17.606s; cursor x551→737; endpoint384000 audio frames). The initial workflow screenshot did not show transport state; isolated test-clock advancement verifies the rendered requirement without repeating passed host/native/full UI checks.
- Initial native run failed before UI because its silence window accidentally included Bar 4's required bass entry. Corrected only the test window; the factory sound/release and all application sources were unchanged by that fixture correction. Intermediate run 43801715816 was cancelled during compilation before executing the proposed release-envelope probe. The device-only follow-up skips already-passed host checks. No C4E deferred campaign, broad regression or F1 performed.

No new project format: v1–v4 and existing atomic save/draft/Undo/Save As reference remapping remain. Song snapshots allow at most 200,000 expanded note instances with clear feedback. Long labels clip in the fixed header; viewport/selection remain visit-local. **Composer projects/custom presets remain outside practice ZIP/JSON recovery until C7.** Physical phone touch/acoustics/routing acceptance remains pending.

**Exact next task: owner-phone C5B acceptance.** Stop here. C5C Make Unique and Edit Pattern from Arrangement Clip is the next separately authorized phase; no exports or unrelated work.

---

# Retained C5A delivery — build147

November15,2026 (America/Los_Angeles). **0.55.0/build147** delivers a horizontal musical timeline with numbered bars, populated-pattern placement, dynamic instrument lanes, whole-bar clip drag/preview, deletion, Undo and exact existing save/reopen. Shared patterns remain references; same-track overlap and newly colliding pattern-length edits are refused. Loop/Mixer share the mounted document/audio owner. [Behavior](docs/COMPOSER_C5A.md).

Canonical C:/Users/mrove/Projects/practice.logger, branch **block-ux-stage1**. Initial clean local/live remote **2248c358c5c9c90a4ead649cfd56719436472bbb**. Application/APK and focused-test source **f290561f1a2b843b0e30910b880b516ade03bbf6**, pushed and verified against the live remote before documentation closeout. Final publication adds records only; its remote SHA is verified at session close. Older OneDrive source checkout untouched; no main merge.

[APK](dist/practice.logger-0.55.0-b147-f290561/practice.logger-0.55.0-b147-f290561.apk) · [ZIP](dist/practice.logger-0.55.0-b147-f290561.zip) · [Delivery report](dist/practice.logger-0.55.0-b147-f290561/DELIVERY_REPORT.md) · [Phone checklist](dist/practice.logger-0.55.0-b147-f290561/PHONE_CHECKLIST.txt).

Delivery: **C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Compose C5A delivery/build147**. Install over the existing permanent-key app without uninstalling or clearing data. dist/LATEST.txt points here. Local delivery copies match; OneDrive cloud-sync completion unobserved. All prior distributions retained.

- APK SHA256 **23feea33fde43638efd876e100d748fa516edff91967693744f4369fd430b991**; ZIP SHA256 **0024e34d60b76497449c9215ada8819e43b6762ad42765f414d77f17b37daf03**.
- Package/version/signature and pinned permanent certificate f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830 verified. All106 unsigned payload entries, nine original SF2 hashes,692 previous APK/ZIP hashes and ZIP exact-byte round trip pass.
- Five focused host methods pass locally and in [API35 run43795631203](https://github.com/mrayover/practice.logger/actions/runs/43795631203): fresh UUID/shared references, snap/bounds, same/different-track placement, move/delete snapshots, shared names, pattern-length collisions, legacy overlaps, Save As remapping and exact atomic save/reopen for supported v1–v4,7/8 meter,16-bar patterns and far placement. Release/vital lint and Android-test compilation pass locally; remote debug/release/test assembly and vital lint also pass.
- One bounded Android workflow passes placement at Bars1/5, actual drag preview with no premature edit and one Undo on release, move Undo, Delete/Undo, last-clip lane removal, fixed-label Hand scrolling, Loop/Mixer switching with the same audio owner, Save→Return→reopen and Activity recreation. Separate force-stop/cold-process reopen passes exact project equality. Main method19.138s; cold phase7.464s. Actual empty/two-lane/preview/deleted/restored/scrolled/narrow/cold landscape screenshots inspected. Save As reference remapping is host-tested; no new Save As UI workflow is claimed.

No new project format, practice schema/data changes, synthesis/mixer changes or arrangement playback. No broad regression/device matrix/F1, and no C4E deferred tests resumed. Physical phone acceptance remains pending. Arrange viewport/selection are visit-local; Loop editor state stays mounted across modes. Long lane names clip within the fixed label area. **Composer projects/custom presets remain outside practice ZIP/JSON recovery until C7.**

**Exact next task: await owner feedback on C5A build147.** Stop here; C5B song playback, duplication/repetition, Make Unique and clip-to-pattern editing require a separate assignment.

---

# Retained C4E build146 — testing stopped at owner request

November15,2026 (America/Los_Angeles). Signed **0.54.0/build146** implements four-band per-channel EQ, bypass,0–200% stereo width, compact channel editor, strict project v4 persistence and completed-gesture Undo on the existing C4D foundation. Owner says “it works” and asks to stop, with further testing later. No build-specific installation/acoustic observations are inferred. [Behavior](docs/COMPOSER_C4E.md).

Canonical C:/Users/mrove/Projects/practice.logger, branch **block-ux-stage1**. Initial clean local/live remote ffa7703b36264ba82bf6ced0164effa4103d2c87. Application/APK source **70a6ab3d9332eef65571d71312d356982c3bbf4d**. Corrected UI test source **c2d2a33bf6ced8ceed48c9f3a1d5a5b7d6a03ab8**; only EOF whitespace changed in application source after the APK commit. Older OneDrive source checkout untouched; no main merge.

[APK](dist/practice.logger-0.54.0-b146-70a6ab3/practice.logger-0.54.0-b146-70a6ab3.apk) · [ZIP](dist/practice.logger-0.54.0-b146-70a6ab3.zip) · [Delivery report](dist/practice.logger-0.54.0-b146-70a6ab3/DELIVERY_REPORT.md).

Delivery: **C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Compose C4E delivery/build146**. Install over the existing app without uninstalling or clearing data. dist/LATEST.txt updated. Previous distributions preserved. OneDrive cloud-sync completion unobserved.

- APK SHA256 **88042cf4393f188d92389ad1c1fd89afac2d3ad5130cd655297cd48f9e4ee9e4**; ZIP SHA256 **ee0f4dd5cbbf0b71c3afe4fb30380d8bc2125965e0a5fd828dc41042f26a83be**.
- Permanent certificate/package/version/signature,106 unsigned payload entries,nine original SoundFont hashes and690 earlier APK/ZIP hashes verified before the owner stop. Signing publisher verified the initial ZIP/APK round trip. Final packaging adds evidence and status; no further tests run after the stop.
- Six local C4D/C4E persistence methods pass: v1–v3 defaults/no read rewrite, strict v4 validation, exact save/reopen, Save As ownership, deletion/pattern duplication and unchanged musical timing. Retained future-version method initially treats supported v4 as future; changed both fixtures to v5 and reran only that method: pass. Release/device-test compilation and vital lint pass.
- [Initial focused run43793227786](https://github.com/mrayover/practice.logger/actions/runs/43793227786) passes C++ band response, exact neutral/bypass,width0/100/200,mono,state isolation,reset,nonfinite/stability,Pan ordering and performance. Eight-channel DSP mean **0.085512ms per5ms block**. Packaged native renderer checks complete before UI reaches Play. Then the test times out waiting for playback at line45, before EQ UI assertions. Unlike established C4D tests, its helper clicks without waiting for enabled UI after worker readiness. Corrected to reuse the enabled-state barrier; diagnostic evidence retained.
- [UI-only follow-up43793976290](https://github.com/mrayover/practice.logger/actions/runs/43793976290) skips passed audio checks and was **cancelled during compilation at the owner's stop request**. New EQ rendered UI/actual gesture Undo/Save As/cold-process workflow remains uncompleted; no rendered screenshots inspected or passing UI outcome claimed. Host persistence/model assertions are distinct from this open native UI coverage. No broad regression/F1.

v1–v3 retain neutral defaults and saved files are not rewritten on read; processing edits advance the draft to v4. Save As preserves processing and remaps identities. Avoid downgrading after saving v4. Processing does not restart Loop. Existing Pan/master/volume paths remain; no practice schema/data changes. **Composer projects/presets remain outside practice ZIP/JSON until C7.** No FX/Arrange playback/export.

**Exact next task: resume the deferred C4E UI/cold verification only when requested.** C5A Arrange timeline foundation remains the next separately scoped development direction; do not start automatically. No owner checklist required.

---

# Retained C4D delivery — build145

Updated November15,2026 (America/Los_Angeles). Signed **0.53.0/build145** delivers independent channel rendering, live Mixer Pan, durable Volume/Pan/Mute/Solo/active-pattern and Master settings, compatible project v3 and remapped Save As. The owner-requested C4C verification gap is closed. **16 scoped host checks, compilation/vital lint, native PCM/performance, API35 UI/save coverage, remaining tail/cold reopen and isolated Pan touch checks are completed across targeted runs.** Actual four/eight-channel, left/center/right, Master and narrow-landscape screenshots inspected. Physical phone acoustics/touch/routing acceptance remains pending. No comprehensive regression/device matrix/F1.

Canonical C:/Users/mrove/Projects/practice.logger, branch **block-ux-stage1**. Initial clean local/live remote6e7ee4ca993850b4a1fc5b23716b809c6ee5b365. Application/APK source **fa6b5625152f4c4c01c378370168493e68252496**; corrected UI/cold test source **d241e2118ccfb61c31ed0f103c8439f8ff1befc3**. Closing documentation publication is verified separately. Older OneDrive source checkout untouched; no main merge.

[APK](dist/practice.logger-0.53.0-b145-fa6b562/practice.logger-0.53.0-b145-fa6b562.apk) · [ZIP](dist/practice.logger-0.53.0-b145-fa6b562.zip) · [Delivery report](dist/practice.logger-0.53.0-b145-fa6b562/DELIVERY_REPORT.md) · [Behavior/format/pan law](docs/COMPOSER_C4D.md).

Delivery: **OneDrive / Practice Log development / Compose C4D delivery / build145**. Install over the existing app without uninstalling or clearing data. Local copies match; OneDrive cloud-sync completion is unobserved. dist/LATEST.txt points here. Earlier142/143/144 distributions remain preserved.

- APK SHA256 **0b696b74bb356f18b24ac24559eae27238ab0c25c77fe2aee5fe20b7211887a4**; ZIP SHA256 **eda93961359b48c89553965ddd464e8a079a3e70e5f0932ff0706a0f486dda7e**. Package/version, signature and pinned permanent certificate f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830 verified. All106 unsigned payload entries, nine original SF2 hashes, ZIP/APK exact-byte round trip, delivery copies and682 previous APK/ZIP hashes pass.
- C4C baseline [43786034370](https://github.com/mrayover/practice.logger/actions/runs/43786034370) at6e7ee4c passes36 host checks, full focused API35 sequence including corrected final cleanup/master reopening/recreation and exact cold reopening. Final four-strip/narrow screenshots inspected. Previously interrupted runs remain historical failures, not retroactive passes.
- C4D local16 scoped checks cover v1/v2 defaults, strict v3 fields, invalid selected ownership/numerics, custom snapshots, unchanged musical timing, deletion/duplication, Save As IDs and failed atomic saves. Initial15/16 passed; the retained future-version assertion expected3 to fail and was advanced to4. Only that failing method was rerun and passed. Native-test/release compilation and vital lint pass. Initial new test coordinate Int/Float compile error was corrected before execution.
- Native checks in [43787607551](https://github.com/mrayover/practice.logger/actions/runs/43787607551), source6374f7f, pass independent additive PCM/once-per-frame advancement, overlapping identical-pitch release, stereo left/center/right, preset-Pan combination, separation and Stop silence. Eight-track32-note rendering: mean **0.050739ms**, p99 **0.091506ms** per5ms block on emulator. That run then finds the actual UI defect at57: a later strip callback overwrites first Pan. Fixed with latest-document transforms. Native/audio/model code is byte-identical in final145, so these passed PCM checks were not repeated.
- [Remaining tail/cold workflow 43789554535](https://github.com/mrayover/practice.logger/actions/runs/43789554535), sourced241e2118ccfb61c31ed0f103c8439f8ff1befc3, passes the remaining visible eighth-strip narrow measurement, final exact Save, master output reopening, interruption cleanup and separate exact cold reopen with retained settings/patterns/output. Earlier corrected main UI run43788527019 atfa6b562 passes active Pan/Volume without restart, shared Loop/Mixer, pattern restart, gesture Undo, Mute/Solo, recreation, exact Save, Save As/original preservation and four/eight captures, then stops at an off-screen eighth-strip measurement after resize. Captured visible faders are52dp; corrected the helper to scroll into view before measuring. The tail checks only the remaining assertions. No complete green main UI workflow is claimed; required focused coverage is assembled from these distinct runs. Passed PCM/host/earlier UI portions were not repeated.
- [Isolated Pan touch 43788143148](https://github.com/mrayover/practice.logger/actions/runs/43788143148), source9b5950f, passes actual horizontal drag/one Undo/viewport ownership/independent channel scrolling. Final145 retains that exact Pan gesture implementation. Initial43787501517 was cancelled during compilation after test review corrected Save As mode expectations; no native pass inferred from it.

Settings belong to stable track UUIDs; deletion/Undo cannot transfer them to another instrument. v1/v2 remain readable; mixer edits advance the draft to strict v3. Do not downgrade after saving v3. Preset Pan remains independent. Each completed slider gesture is one Undo/checkpoint; changing patterns deliberately restarts playback. Master stays Composer-only; one audio worker/AudioTrack remains.

**Practice ZIP/JSON still exclude Composer projects and presets until C7.** EQ, width, FX, Arrange and exports are unimplemented. **Exact next task: await the owner’s separate C4E assignment. C4E — Four-Band EQ and Stereo Width — needs a separate assignment.**

---

# Retained C4B delivery — build140

Updated November15,2026 (America/Los_Angeles). Signed **0.51.0/build140** corrects assigned instrument identities and delivers expanded pattern/track management. **34 focused host checks, compilation/vital lint, one API35 C4B workflow and exact cold-process project reopening pass.** Actual rendered landscape selectors/menus/rename/deletion dialogs inspected. Physical phone/audio acceptance remains pending; no full regression/F1.

Canonical C:/Users/mrove/Projects/practice.logger on **block-ux-stage1**; older OneDrive source checkout untouched. Initial clean local/live remote **4f5d1236ed2fb0f640e58c1206e76d9df178604b**. Application/APK source **d3d0ef4af16d91088b136d13913121af1a1bc5a2**; focused test source **d3d0ef4af16d91088b136d13913121af1a1bc5a2**, byte-identical application/build sources. Closing documentation publication is verified separately at closeout; no main merge. Owner reports C4A working without individual physical-check results.

[APK](dist/practice.logger-0.51.0-b140-d3d0ef4/practice.logger-0.51.0-b140-d3d0ef4.apk) · [ZIP](dist/practice.logger-0.51.0-b140-d3d0ef4.zip) · [Delivery report](dist/practice.logger-0.51.0-b140-d3d0ef4/DELIVERY_REPORT.md) · [Rendered evidence](dist/practice.logger-0.51.0-b140-d3d0ef4/evidence/preview-composer-c4b-screens/01-correct-legacy-selector.png) · [Implementation](docs/COMPOSER_C4B.md) · [Phone checklist](MANUAL_TESTS.md#composelogger-c4b--owner-phone-check).

Delivery copies: **OneDrive / Practice Log development / Compose C4B delivery / build140**. Install over the existing permanent-key app without uninstalling/clearing data. Copies match locally; cloud-sync completion is unobserved. dist/LATEST.txt points to them.

- APK SHA256 **6971c47272b2ea4076ef5db28d9f4e2c56ee0d29d03e69fa72ddfe016586c6c7**; ZIP SHA256 **32cef0ec7143e7fb7c516a155997e8c30b17a916d994388c0b66963a9f98c30f**.
- Package com.mrayover.practicelogger.app, version/build, pinned permanent certificate f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830 and signature verified. **106** unsigned payload entries and all nine original SF2 hashes match. ZIP exact-byte round trip and delivery-copy hashes pass. **662** previous APK/ZIP hashes unchanged, including initial138/139; all earlier distributions retained.

## Implemented behavior

Instrument labels derive from the existing registry and show numbered actual type, descriptive track name when present, and custom preset identity. Legacy automatic Piano N titles do not mask sounds or get rewritten. New tracks use neutral Track N names. Sound changes immediately update identity while preserving names, UUIDs, notes and sound data. C4A editor/preset library and native synthesis remain unchanged; existing custom presets are selected through Edit Instrument.

The existing second-row Pattern dropdown supports ordinary vertical lists up to the existing256-per-track model limit, create with unique A–Z/AA onward names, independent duplication with fresh pattern/note UUIDs, validated rename and explicit confirmed deletion. Eight-track Add/Select/Rename/Delete remain compact. Deletion refuses arrangement dependencies, removes only owned objects and permits empty projects/empty tracks. Pattern lengths1/2/4/8/16 and refusal to truncate notes remain. All seven structural operations use one validated project edit/Undo/checkpoint each. No persisted field or project-format migration was added; existing v1/v2 and Save As stay compatible.

Selected patterns and session mute/solo/volume follow track UUIDs through changes/recreation. Deleted selections/mixer entries are pruned; restored musical tracks get default session mixer settings after Undo. Loop retains the current coherent musical snapshot, independent lengths and longest region until Stop/next Play. Deleted track sequence voices are cleared; surviving mixer controls map to frozen snapshot UUID order. Preview channels follow current track order. Next Play installs current mixer mapping before first onset. The960PPQ timing engine and four-row layout stay intact.

## Focused evidence and limitations

- [Final workflow 43767877375](https://github.com/mrayover/practice.logger/actions/runs/43767877375), source d3d0ef4af16d91088b136d13913121af1a1bc5a2:34 focused host checks with zero failures/errors/skips, debug/release/native-test compilation and vital lint pass. One API35 workflow checks actual legacy/factory/custom labels and sound/name changes, more-than-three patterns and retained per-track selections, create/duplicate/rename/delete/Cancel/Undo, active coherent snapshots, middle-track deletion/next Play indexing, save/recreation and exact saved project/custom identity after a separate cold process. Rendered landscape captures show names, dropdowns, custom presets, expanded pattern/menu and rename/deletion confirmations. Native output writes pass on cold reopening. No physical acoustic/touch acceptance is inferred.
- Local34 checks and compilation/vital lint pass. Host checks include256 capacity/A–Z/AA naming, duplication musical values/UUIDs/source independence, title rejection, clip reference integrity/refusals, eight-track/empty states, length validation, per-track Loop data and v1/v2 atomic persistence, plus existing Composer/PCM checks.
- Superseded43766183935 was cancelled during compilation after test selection assumptions were corrected. Run43766287034 passed34 host checks/compilation, then failed an Undo assertion helper that called runOnMainSync from the main thread. Exact stack retained; test lookup corrected without removing assertions. Superseded43766594159 was cancelled during compilation after that test correction. Final test source adds independent selection and rendered menu scrolling. Initial signed138 and verified139 remain preserved. Application140 puts type before descriptive names based on inspected139 captures, and uses current topology when asynchronous Play installs its mixer mapping. No comprehensive regression, historical/device matrix or F1 was run.

**Practice ZIP/JSON still exclude Composer projects AND presets.** C5 Arrange, C6 exports and C7 integration/recovery are unimplemented and require separate assignment.

**Exact next task: install build140 over the existing app and perform the C4B owner-phone checklist. Stop before C5–C7/F1.**

---

# Historical C4A delivery

# Current state — compose.logger C4A delivered

Updated November15,2026 (America/Los_Angeles). Signed **0.50.0/build137** delivers the dedicated instrument editor, real six-parameter sound shaping, immutable custom preset library and reused polyphonic preview keyboard. **29 focused host checks, compilation/vital lint, API35 C4A workflow and exact project/preset cold reopen pass.** Actual rendered primary/narrow/enlarged/IME captures inspected. Physical Galaxy S26 Ultra/audio/routing acceptance remains pending; no full regression/F1.

Canonical C:/Users/mrove/Projects/practice.logger on **block-ux-stage1**; older OneDrive source checkout untouched. Initial clean local/live remote **3c55c2986ce5ed8d4a7f090bb9f0addecff5413a**. Application/APK source **58439a13a10584ed880892439cc41d4300e23e01**; final focused test source **58439a13a10584ed880892439cc41d4300e23e01**. Closing documentation publication is verified separately at handoff; no main merge.

[APK](dist/practice.logger-0.50.0-b137-58439a1/practice.logger-0.50.0-b137-58439a1.apk) · [ZIP](dist/practice.logger-0.50.0-b137-58439a1.zip) · [Delivery report](dist/practice.logger-0.50.0-b137-58439a1/DELIVERY_REPORT.md) · [Technical ownership/storage/limits](docs/COMPOSER_C4A.md) · [Phone checklist](MANUAL_TESTS.md#composelogger-c4a--owner-phone-check).

Delivery copies: **OneDrive / Practice Log development / Compose C4A delivery / build137**. Install over the existing permanent-key app without uninstalling/clearing data. Local copies match; cloud-sync completion is unobserved. dist/LATEST.txt points to them.

- APK SHA256 **33b376971a8fddbb1856190cea6dc372e7f4eced79cc0f126f7dd29e6a2ff1d7**; ZIP SHA256 **f50a757408729fbe93c45901e726d2cd2756692b049b9fd0831449be03ad5f64**.
- Stable package com.mrayover.practicelogger.app, version/build, pinned certificate f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830 and signature verified. **106** unsigned payload entries and all nine original SF2 hashes match. ZIP exact-byte round trip and delivery-copy hashes pass. **648** previous APK/ZIP hashes unchanged, including initial signed134/135 candidates and verified136; all distributions retained.

## Implemented behavior

Edit Instrument opens a full-screen landscape overlay inside the existing Composer Activity. The four-row roll stays mounted with its audio owner, selected track/pattern and view position; no branded entrance repeats. Entry deliberately stops Loop. Back/preset changes protect unsaved sound changes. Save New Preset creates an immutable validated UUID/private file. Factory and My Presets are distinct groups; factory data is never overwritten. Use on Track applies a saved selection only to the current draft, preserving notes, velocities and identities; composition Save commits it.

Attack0–3000ms, Release10–5000ms, logarithmic Cutoff40–18000Hz, ResonanceQ0.5–4, Sustain0–100% held amplitude and equal-power PanL/Center/R affect actual SF2 output. Attack starts on next note; release determines the next release transition. Numeric entry/default/accessibility supplement160dp vertical knob travel. Factory bypass preserves original per-zone sound; its dials explicitly show editor starting defaults until a control is changed. Custom envelope has bounded fixed SF2 decay rate; sample natural decay/end still applies. Existing track Volume remains live. No effects chain or sample editing.

The existing Reference Piano component provides chords, note labels, octave/semitone movement, release/cancel and accessibility, routed exclusively to Composer SF2 audio. Focus loss/background/reset, range/preset switching, editor disposal and recreation release preview. One audio worker owns16 independent channel snapshots and private voice-region copies; shared SF2 regions/assets and vendored tsf.h are unchanged. Preview uses channels8–15; Loop uses0–7 and an immutable per-track sound snapshot on next Play. The existing960PPQ clock, mixer and playback ownership remain.

Normal landscape keeps all six numeric values and the keyboard visible. Short headers use40dp controls. Enlarged text/orientation restrictions scroll the control body with80dp keys pinned below it. Naming/numeric dialogs account for the software keyboard and scroll their content. Native bounds checks use original/unclipped values, and Save is physically tapped with IME visible. Physical safe-inset/touch/latency acceptance remains separate.

Private preset format1 records base instrument, UUID, readable title and versioned sound parameters via synced temporary files and atomic rename. Existing project v1 reads/writes unchanged, including reference-piano/default and nine C3 identities. Explicit project v2 embeds nullable factory fields or a complete immutable custom identity/title/settings snapshot. Unknown fields/versions remain rejected. Custom projects play without a library-file dependency and Save As preserves the sound. Other compositions never change when a preset is created. Room27/ZIP12/JSON7 and practice data are unchanged.

## Focused evidence and limitations

- [Final C4A workflow 43762786936](https://github.com/mrayover/practice.logger/actions/runs/43762786936), source58439a13a10584ed880892439cc41d4300e23e01:29 focused checks with zero failures/errors/skips, debug/release/instrumentation compilation/vital lint, one API35 workflow and cold-process exact project/preset reopening pass. Native PCM demonstrates attack/release/sustain/filter/Q/pan, same-bank channel isolation, polyphony and clear silence. UI exercises knob dragging/precise values, save/factory/custom reselection, actual chords/range, track/Loop snapshot preservation, dirty warnings/recreation, narrow/enlarged layout, keyboard-visible Save, focus/exit cleanup and persisted reopening.
- Initial43758391137 passed build/29 checks/launch then instrumentation did not return; cancelled after over10minutes and logs/artifact retained. Its exact cause was not established. Follow-up43759998847 passed native PCM and most UI, then a final enabled-key readiness assertion failed; the test now awaits actual enabled semantics. Screenshots from that run exposed genuine narrow-value and IME button clipping, corrected in136. No failed assertion was removed. Verified136 workflow43761365333 passed; final137 adds the saved custom preset name to the composition Sound display and checks it after application/cold reopening. The enlarged-controls capture is actual1.6x evidence; the early enlarged-header capture caught the preceding scale frame. Detailed evidence and compiler corrections are in WORK_LOG.
- PCM generation/emulator interaction does not establish physical speaker/headphone/Bluetooth quality, latency or owner acceptance. No API26 matrix, full regression or F1 certification.

**Practice ZIP/JSON still exclude Composer projects AND presets.** Complete recovery remains later C7/export work. Custom rename/delete/shared overwrite/version management, expanded C4B patterns, Arrange, exports, Library integration and effects remain unimplemented by design. Earlier SoundFont drum subset/sample limits remain.

**Exact next task: install build137 over the existing app and perform the C4A owner-phone checklist. Stop before C4B–C7/F1.**

---

# Historical build133 delivery

# Current state — compact compose.logger and Save As delivered

Updated November 15, 2026 (America/Los_Angeles). Signed **0.49.1/build133** consolidates Composer into four compact measured landscape rows, expands the piano roll, and implements independent live-draft Save As. **27 focused host checks, compilation/vital lint, and the API35 compact workflow plus exact original/copy cold reopening pass.** Actual rendered screenshots were inspected. Physical Galaxy S26 Ultra touch/fit/audio acceptance remains pending; no comprehensive regression/F1.

Canonical C:/Users/mrove/Projects/practice.logger on block-ux-stage1; older OneDrive source checkout untouched. Clean initial local/live remote da6289813f7138b759d9cfd41e1a87202b41c8f3. Application/APK source **f5ddeea2595392a296889e170c0a4ce80fa4765b**; final focused test source **127f98fcede20d4115983e69834fe9fa11895c69**. Application/build source is byte-identical between them; subsequent changes are tests/docs. Final documentation publication is verified at closeout; no main merge.

[APK](dist/practice.logger-0.49.1-b133-f5ddeea/practice.logger-0.49.1-b133-f5ddeea.apk) · [ZIP](dist/practice.logger-0.49.1-b133-f5ddeea.zip) · [Delivery report](dist/practice.logger-0.49.1-b133-f5ddeea/DELIVERY_REPORT.md) · [Behavior/ownership](docs/COMPOSER_COMPACT_UI.md) · [Phone checklist](MANUAL_TESTS.md#composelogger-compact-ui-and-save-as--owner-phone-check).

Delivery copies: **OneDrive / Practice Log development / Compose Compact UI delivery / build133**. Install over the existing permanent-key app without uninstalling/clearing data. File hashes match locally; cloud-sync completion is unobserved. dist/LATEST.txt points to the copies.

- APK SHA256 **ff3bd05cfd1eb30de7bf27fdf766c98922f5879e4a6089df63d1235c73418bf0**; ZIP SHA256 **ffc2b475f8615c30abe4dea23e811f2d75cb3cce64cbc0d900a76bb9bf5b8876**.
- Package com.mrayover.practicelogger.app, version/build and pinned permanent certificate f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830 verified. 106 unsigned payload entries and all nine unchanged SF2 hashes match. ZIP content/round-trip hashes and delivery copies pass. Build129/132 APK/ZIP hashes unchanged; earlier distributions retained.

## Implemented behavior

Application row: identity, Save/Save As, selected Loop/Arrange, Project Settings, Return. Context row: numbered Instrument selector (number/title entries), Pattern with Create Pattern, + Instrument/eight-track cap, Bars, nine-bank Sound and disabled Edit Instrument. Performance row: Play/Stop/Repeat/Mute/Solo/Volume, disabled Velocity and compact ↤ menu for Beginning. Editing row: Tool/Grid/Duration/Snap/Zoom/Actions/Undo and musical position/region. Actual text measurements at current font scale select explicit More overflow before controls exceed row width. No primary control row has horizontal scrolling. The unchanged musical canvas still pans/zooms and receives remaining height. One compact status line replaces static audio descriptions; contextual errors remain visible.

Four rows measure40dp each. Primary1040x480dp canvas304dp (**63.3%** of screen height); narrow640x360dp canvas184dp (**51.1%**); large1280x800dp canvas624dp (**78.0%**). At640x360dp/1.6x text, More provides the accessible fallback; some lower-priority/critical controls move there when all cannot fit. Menus/dialog/software keyboard and actual key/grid pixels were visually inspected. Android transient status/navigation bars appear in the primary handoff/keyboard captures; later normal captures are unobstructed. Existing immersive overlay behavior remains, with physical safe-inset acceptance pending. These are emulated viewports, not physical Galaxy measurements.

Save As validates the title and copies current unsaved notes/velocities, tempo/meter, sound assignments, patterns and arrangement content. Project/track/pattern/note/clip IDs are regenerated and clip references remapped; metadata/revision history starts fresh. Atomic create succeeds before ViewModel saved-state/Intent identity changes and source Undo/clipboard/checkpoint retirement. Original saved bytes remain unchanged. Failure retains recoverable source draft state. Save As is device-local composition creation; practice ZIP/JSON still exclude Composer until C7.

PianoRollWorkspace keeps one audio/editor-state owner while menus, mixer and layout change. Mode changes stop playback deliberately while preserving session editing state. Save As intentionally replaces the workspace/audio owner for the new document. Existing canvas geometry/gestures, sound banks/TinySoundFont, practice data, Room/ZIP/JSON and entrance behavior are unchanged. Edit Instrument/Velocity stay disabled; Arrange is a placeholder. No C4–C7/F1 scope is claimed.

## Focused evidence and limits

- [Final workflow 43754884820](https://github.com/mrayover/practice.logger/actions/runs/43754884820), source127f98fcede20d4115983e69834fe9fa11895c69, passes27 host checks with zero failures/errors/skips, debug/release/instrumentation compilation and vital lint. Native workflow checks four rows, menus, actual visible keyboard/keys, three normal landscape sizes and1.6x text/overflow Volume; selected patterns, Play/Stop/Repeat/Beginning/mixer and stable audio owner through menus; Actions Copy/Paste/Undo; tool/grid/snap/zoom and keyboard scrolling; unsaved-note/velocity Save As, independent edits/Save, new-document recreation, dirty Return/Keep editing/Undo, portrait library and both exact documents after force-stop.
- First workflow43754024479 passed27 checks and both native executions, but first primary capture caught branding and its keyboard-named capture had no software keyboard. Final capture test waits for committed frames, asserts actual keyboard pixels and requires IME visibility. No product change was needed for this evidence correction. Later test source explicitly includes unsaved-note assertions, new-project recreation and enlarged-text overflow.
- Local debug/release/vital/native compilation and27 focused host checks pass; strengthened3-check Save As class passes locally. Earlier C1–C3 workflows retain their tests/assertions with current labels, but were not broadly rerun. No full regression, historical matrix, physical speaker/headphone/Bluetooth or phone acceptance is claimed.

**Exact next task: install build133 over the existing app and perform the compact UI/Save As owner-phone checklist. Stop before C4–C7/F1.**

---
# Historical C3 delivery — build132

# Current state — compose.logger C2 corrections + C3 delivered

Updated November 15, 2026 (America/Los_Angeles). Signed **0.49.0/build132** delivers keyboard scrolling/Hand/Actions and nine SoundFont instruments with synchronized multitrack Loop playback. **24/24 focused host checks and the final API35 audio/editing/lifecycle workflow plus exact saved cold reopen pass.** Actual landscape captures reviewed. Physical touch/audio/routing acceptance remains pending; no F1/full regression.

Canonical C:/Users/mrove/Projects/practice.logger on block-ux-stage1; older OneDrive checkout untouched. Clean starting local/live remote f7c353bfb79d03d409f94a23da40308b9ca96f11. Application source **2d6a95e01587335b4594db16e182b42edb1e1246**; successful focused test source **72b8239c77efcecf824351a50d5d55806379a6b8**, verified live remotely before documentation closeout. Application/build configuration are byte-identical between those commits; only the draft-recovery fixture differs. Final documentation publication SHA is reported in the closing handoff/PUBLICATION.txt; no main merge.

[APK](dist/practice.logger-0.49.0-b132-2d6a95e/practice.logger-0.49.0-b132-2d6a95e.apk) · [ZIP](dist/practice.logger-0.49.0-b132-2d6a95e.zip) · [Nine-part delivery report](dist/practice.logger-0.49.0-b132-2d6a95e/DELIVERY_REPORT.md) · [Phone checklist](MANUAL_TESTS.md#composelogger-c3--owner-phone-check) · [C3 technical behavior and bank licenses](docs/COMPOSER_C3.md).

Delivery copies: **OneDrive / Practice Log development / Compose C3 delivery / build132**. Install over the permanent-key app without uninstalling or clearing data. Local copies verified; cloud-sync completion unobserved. dist/LATEST.txt points to these files.

- APK SHA256 **92749f4b34a12e0289d380fd36e15ebaa542ae45662cc6985a604f553641542c**; ZIP SHA256 **83872b23a937bc83c3fd89bea903abc8dc8861329baf973f147db3aa36f7a551**.
- Package com.mrayover.practicelogger.app and permanent certificate f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830 verified. All **106** unsigned payload entries and all nine unchanged SF2 hashes/notices verified. ZIP CRC/size/exact round-trip bytes pass for29 files; delivery hashes match. Build129 APK/ZIP hashes unchanged; intermediate130/131 and earlier packages retained.

## Implemented behavior

The actual defect was keyboard ACTION_MOVE routing to key audition instead of panY. Vertical movement beyond Android touch slop now releases held audition and owns scrolling until lift; keys, labels, note rectangles and hit testing share the clamped0–127 transform. Partial rows clip correctly. Hand pans the grid with one finger; Draw/Select and two-finger pan/zoom remain. Actions holds all five selection operations in order, with actual-note selection/clipboard/editability states, dismissal and unchanged musical/Undo algorithms.

Pinned MIT TinySoundFont v0.9 and nine explicitly CC0 FreePats banks provide acoustic piano, FM electric piano, organ, electric bass, synth bass/lead/pad/strings and electronic drums. Total **65,217,226bytes (62.20MiB)**; [exact sizes/sources/download identities/licenses/ranges](docs/COMPOSER_C3.md#bank-provenance-and-licenses). One worker preloads/owns native operations and stereo output; queued controls never race rendering. Keyboard/note-drag audition uses the selected sound, velocity, polyphony and independent release tokens. Focus/background/noisy/disposal clear output. Existing Reference Piano outside Composer is unchanged.

Loop uses one audio-frame clock derived from stored960PPQ, per-track selected-pattern streams, independent repeat lengths, explicit longest-pattern region, off-before-on boundaries, Play/Stop/Beginning/Repeat and consumed-audio green playhead/bar-beat. Mute/Solo/Volume apply live. Musical edits/instrument/pattern/tempo changes apply on next Play through a coherent snapshot. View/session pattern and mixer state survive recreation; durable instrument assignments use compatible existing v1 references with reference-piano alias, without changing notes/UUIDs. Room/practice audio/data/backup formats remain unchanged.

## Focused evidence and limits

- C2 was verified before C3: [workflow43741378088](https://github.com/mrayover/practice.logger/actions/runs/43741378088), source2adccce, passes native scroll release/alignment, Hand separation, existing edit/clipboard/Undo/recreation and exact cold reopening. Actual scrolled G5 key/note screenshot reviewed; partial-row drawing correction compiled before C3.
- [Final C3 workflow43745345404](https://github.com/mrayover/practice.logger/actions/runs/43745345404), source72b8239, passes24 host checks with zero failures/errors/skips, compilation/vital lint and two native executions on one API35 configuration. Actual packaged bank hashes/loading, all bank velocity/PCM, melodic MIDI extremes, independent overlapping release/clear silence, both viewport bounds, dropdown disabled states/quantize Undo, instrument save/retained per-track B selection, repeated multitrack output, mixer interactions, Stop/Beginning/one-shot/restart, focus loss, recreation and exact saved cold reopen pass. Final dropdown/playhead/recreated instrument captures and earlier highest/lowest/sound-picker captures were reviewed.
- Local debug/release/instrumentation/vital compilation and24 focused host checks pass; all four native ABIs compile. No physical speaker/headphone/Bluetooth acceptance or comprehensive/device-matrix regression.
- Earlier43743180310 was cancelled after source drum mapping inspection.43743548252 reached Stop after passing banks/bounds/loop/mixer, then exposed a stale cached voice count after output disposal; worker command completion now publishes the cleared native count.43744436123 compiled and passed23/24 host checks, then the recovery fixture read an intermediate matching file while writers drained. Test-only72b8239 waits for writer completion, retaining every exact-value assertion; isolated4-check class and final24-check gate pass. No unresolved failure remains in the completed focused gate.

Extreme melodic pitches transpose edge samples; electronic drums expose a documented19-hit subset with other keys silent. Mixer/pattern choices and clipboard/Undo are view/session state; instrument assignments and musical data persist. Explicit Save remains the durability boundary for recent asynchronously checkpointed edits. **Practice ZIP/JSON still exclude Composer until C7.** No Arrange playback, full C4 management, export, Library integration or pack installation.

**Exact next task: owner installs build132 over the existing app and follows the C3 phone checklist, especially touch scrolling and audible multitrack timing/Stop/output routing. Stop before C4–C7/F1.**

---
# Historical C2 delivery — build129
# compose.logger C2 delivered, build129

Updated November15,2026 (America/Los_Angeles). Signed **0.48.0/build129** delivers the C1 three-second branded entrance refinement and functional C2 landscape piano roll. **19/19 focused host checks and four native workflow executions on one API35 configuration pass.** Actual branded/landscape screenshots reviewed. Physical phone/acoustic acceptance remains pending; no F1/V1 certification or C3 sequencing claim.

Canonical C:/Users/mrove/Projects/practice.logger on **block-ux-stage1**; older OneDrive checkout untouched. Exact application/package/main-workflow source **359383ac27612ef7e26d3e36139c896b3fc506c9**, verified on the live remote before closeout. Final documentation publication/remote SHA is reported in the closing handoff; no main merge. Starting clean local/remote d80308c3741de06d8086521b4cd568952ee8fffc. Closing documentation does not change the APK.

[APK](dist/practice.logger-0.48.0-b129-359383a/practice.logger-0.48.0-b129-359383a.apk) · [ZIP](dist/practice.logger-0.48.0-b129-359383a.zip) · [Phone checklist](MANUAL_TESTS.md#composelogger-c2--owner-phone-check) · [Editing/format/lifecycle details](docs/COMPOSER_C2.md) · [Approved V1 contract](docs/COMPOSE_LOGGER_V1_SPEC.md).

Delivery: **OneDrive / Practice Log development / Compose C2 delivery / build129**. Install over the existing permanent-key app without uninstalling/clearing data. dist/LATEST.txt points to these copies; cloud-sync completion is unobserved.

- APK SHA256 **144f84a2886d66f409085c1d348f5641d276977fc9cdb0e2d4d47d4ce5cc6d32**; ZIP SHA256 **f0bfffc8c72dea5d14ad48d5e1c87ae067996ebb5ae14684e7ba24755430921f**.
- Stable app ID com.mrayover.practicelogger.app and pinned permanent certificate f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830 verified by publisher. All **74** unsigned APK payload entries match; ZIP entries and delivery-copy hashes match. Baseline build127 APK/ZIP retain their recorded hashes; intermediate candidate128 and all previous distributions are preserved.

## Implemented C1 refinement and C2

The black entrance shows centered white compose.logger for at least3,000ms of resumed/focused displayed time, with no UI-thread blocking, independent file readiness and saved remaining time. Ordinary recomposition/recreation does not restart it; background/lost-focus time does not advance it. Dedicated sensor-landscape immersive Activity and safe return remain.

Loop mode now has actual note rectangles, vertical touch piano keys, meter-aware beat/bar ruler, independent red insertion/paste cursor, horizontal zoom and two-finger pan/zoom. Draw/Select modes distinguish empty-grid insertion, existing-note audition/selection, body/group movement, expanded right-edge resizing, double-tap deletion and rectangle selection. Independent quarter/eighth/sixteenth/thirty-second and eighth/sixteenth/thirty-second-triplet grids/durations use960 PPQ. Snap off preserves single-tick timing; grid changes never rewrite notes. Explicit selected-start quantization, Select All/Copy/Paste/Delete and bounded project-wide Undo are functional.

Minimal instrument selection/creation supports up to8 Reference Piano tracks with A/B and missing A/B/C slot creation. Clipboard persists across pattern/instrument switches, preserves relative timing/pitch/duration/velocity and assigns fresh IDs without replacing coincident notes. Paste can grow to the next permitted pattern length, refusing beyond16 bars. Length controls1/2/4/8/16 refuse shortening past existing notes. Undo is one completed gesture per operation and spans pattern switches, including edits before Save with revision-safe restoration.

Reference Piano synthesis provides real polyphonic key audition/release and short note/pitch-change audition, stopping on lifecycle/focus loss/disposal. No sequencer, microphone or practice clock is started. Arrange remains its C1 placeholder. Strict v1 project storage/960 PPQ/IDs/validation/atomic writes remain compatible with empty C1 projects. Ordered full-draft file checkpoints replace potentially oversized musical Activity bundles; Save/reopen/recreation and dirty Back preserve coherent musical state. Room27/ZIP12/JSON7 and practice data are unchanged.

## Actual focused verification

- Local release/vital lint/native compilation and **19/19** host checks pass, zero failures/errors/skips:8 storage/codec safeguards,2 saved-state recovery,4 musical-grid/bounds/clipboard/project-undo/draft checks and5 existing PCM synthesis/geometry checks.
- [Final scoped CI43736793657](https://github.com/mrayover/practice.logger/actions/runs/43736793657), exact source above, passes compilation/vital lint and the same19 host checks. One API35 phone emulator passes the real C1 functional/landscape/recreation/return workflow, its force-stop/cold reopen, C2 native editing/selection/cross-pattern/cross-instrument/undo/free-tick/triplet/gesture-separation/audio-release/draft and saved recreation workflow, and a separate force-stop/cold reopen verifying complete musical values/identities against the saved document.
- Real entrance capture checks solid-black corners plus a restrained centered white branding region; trace: **5470 ms from branded draw to workspace draw**. Rendered transition, landscape notes/selection and recreated saved-note screenshots were inspected. Full-screen system education was marked confirmed in this emulator so its overlay cannot substitute for the app surface. A separate [C2-only capture follow-up](https://github.com/mrayover/practice.logger/actions/runs/43737994541), test-only source da85c735505f2a673320f5cb7a6e950d0fd7b642, confirms the rendered keyboard before/after recreation and repeats C2 exact-note cold reopening; its app source is identical to the signed package. Native-idle screenshot synchronization resolves the earlier incomplete immediate recreation capture.
- Earlier run43735971303/sourcebb5f704 passes its18 host checks, C1 functional/entrance corner check and C1 cold reopen; C2 gestures/clipboard/audio/save reach final recreation, where the test incorrectly expects Pattern A instead of retained B. Corrected that expectation without changing retention. Screenshot review caught Android's education overlay despite corner-black samples; strengthened white-brand/centering assertions. Also fixed Canvas drawing over the footer and refined snap bounds. Earlier synthetic C1 capture targeted MainActivity and returned its green background; the replacement captures the actual Composer Activity and verifies visible branding/duration. Historical failures remain in WORK_LOG.

## Limits and exact next task

No unresolved C2 gesture/audio/orientation/persistence defect was observed in the completed focused checks. Physical touch feel, acoustic quality/headphones/Bluetooth routing, manufacturer orientation behavior and enlarged display/text acceptance remain owner-phone checks, not certified results. The first-use Android full-screen tip may appear; dismiss Got it. Sudden process death before an asynchronous unsaved draft checkpoint completes can lose the most recent gesture; explicit Save is the durability boundary. Undo history and clipboard are session-local; musical project values/IDs persist. Full video/power-loss/device-matrix/broad regression/F1 testing was not run.

**Existing practice ZIP/JSON backups still exclude Composer.** Editable projects remain device-local until C7 recovery; do not uninstall or clear data. C3 SoundFont/sequenced playback, full C4 management, Arrange editing, exports and Library integration are unimplemented.

**Exact next task: owner tries the C2 phone checklist and reports any concrete gesture/audio/layout/persistence issue. Stop after C2; do not begin C3 without a separate assignment.**

---
# Previous delivery — Android typing assistance, build125

Updated November14,2026 (America/Los_Angeles). **Owner-reported phone confirmation received November14** for the current delivery; individual checklist observations and keyboard/device details were not supplied. Signed **0.46.4/build125** adds native keyboard sentence capitalization/autocorrect requests for prose. Canonical C:/Users/mrove/Projects/practice.logger on block-ux-stage1; existing Home correction and all prior working changes preserved. The owner explicitly authorized committing all pending changes on November14. Home refinement, typing assistance, focused interface checks and related records are recorded together on block-ux-stage1, based on 88ce207d594eb25090de6fcb23bdddf209df89a0. Publication SHA and remote verification are reported in the closing handoff. No CI dispatch or merge to main. Older OneDrive repository untouched.

[APK](dist/practice.logger-0.46.4-b125-88ce207/practice.logger-0.46.4-b125-88ce207.apk) · [ZIP](dist/practice.logger-0.46.4-b125-88ce207.zip) · [Phone checklist](MANUAL_TESTS.md#native-typing-assistance--build125). Copies: OneDrive / Practice Log development / Typing assistance delivery / build125. Install over the permanent-key app without uninstalling or clearing data. Cloud synchronization unobserved; dist/LATEST.txt points here.

One shared ProseKeyboardOptions requests KeyboardCapitalization.Sentences and autoCorrectEnabled=true. Applied to20 field/helper declarations covering Log, practice-note creation/editing, session notes/reflections, Block/Goal prose/name fields, Routine names, learning criteria, Inspiration text/note/title/author, marker notes/labels, take labels and media titles. Numeric/date/search options and the dedicated Inspiration Source field retain prior settings. No app-side case/spelling/punctuation transformations, dependencies or persistence changes; no existing text is rewritten. The installed Android keyboard, language and user settings decide actual corrections and punctuation shortcuts. [Android keyboard options](https://developer.android.com/develop/ui/compose/text/user-input). Home remains build124's composition; Room27/ZIP12/JSON7 unchanged.

Verification: local debug compilation, offline release assembly, release vital lint and instrumentation APK assembly pass. **2/2 focused host Android input-connection checks pass**, zero failures/errors/skips: actual prose EditorInfo inputType180225 contains TEXT/CAP_SENTENCES/AUTO_CORRECT/MULTI_LINE, raw inserted text stays unchanged; numeric inputType2 and Done action remain. Temporary fixture/configuration used existing cached Robolectric/Compose dependencies, were saved in the ZIP as reproducible evidence and removed from source; no build dependency additions. No broad tests, full regression or emulator run. November14 phone confirmation is owner-reported general acceptance; individual physical correction/punctuation results are not independently documented.

Publisher verified stable app ID/version, permanent certificate/signature. All74 unsigned non-META-INF payload entries match the signed APK; ZIP member/copy hashes pass. Prior distributions preserved. APK SHA256 824da36f6980e706ddf6c99e622880c68658ea550a2c79f2493a8f357409490b; ZIP SHA256 9a01682c69f66ccedb4853f84a4cdab15176c0cf11d5ed0809283c2ccd4c122d. Exact SOURCE.patch SHA256 2e178cc6154258f6f0fe49d54160f92d651910d5f3bf5528cec7d57daceac0a7 includes the new TextInputDefaults.kt and reproduces current app/test/configuration changes against base 88ce207d594eb25090de6fcb23bdddf209df89a0; reverse-apply check passed. BUILD_INFO sourceCommit=null preserves the original packaging-time working-snapshot provenance. The package is unchanged; its source patch reverse-check passes against the source now being committed. Subsequent documentation records phone confirmation and Git publication.

**Next: await the owner’s next bounded assignment.** Phone confirmation is recorded; no further implementation or F1 is authorized. The previously listed phone check is no longer the pending next task.

---

# Previous screenshot-directed Home correction, build124

Updated November13,2026 (America/Los_Angeles). Signed **0.46.3/build124** supersedes the scrolling build123 review candidate. The owner's screenshot and later instructions govern this correction: full-width buttons, percentage-only Goals and a minimal weekly footer visible on opening. Canonical C:/Users/mrove/Projects/practice.logger on block-ux-stage1; older OneDrive checkout untouched. Changes remain uncommitted/unpushed; base/live remote 88ce207d594eb25090de6fcb23bdddf209df89a0. No CI dispatch or merge.

## Signed review delivery

[APK](dist/practice.logger-0.46.3-b124-88ce207/practice.logger-0.46.3-b124-88ce207.apk) · [ZIP](dist/practice.logger-0.46.3-b124-88ce207.zip) · [Phone checklist](MANUAL_TESTS.md#home-screenshot-correction--build124). Copies: OneDrive / Practice Log development / Home visual refinement delivery / build124. Install over the permanent-key app without uninstalling or clearing data. Cloud synchronization unobserved. dist/LATEST.txt points here.

- APK SHA256 2c20288e94164e12ffbc2d23eed12c7ec7d80027ba15c2e56bf160457e554df4
- ZIP SHA256 5bb32ed353a9e99ba8bb8f5d8c51f5d9af9c036de634bc297e7c52ba59ad71fe
- Stable app ID/version/signature/permanent certificate verified by the existing publisher; all74 unsigned non-META-INF payload entries match, ZIP members and delivery copy hashes match. Prior packages, including build123, preserved. No historical hash campaign.
- Source is an **uncommitted review snapshot**, not the unchanged base commit. Included SOURCE.patch SHA256 1e6d367396ffac858ee9262f7a3bc43167700b199ae280b29d067ec3f69c93c4, applied to base 88ce207d594eb25090de6fcb23bdddf209df89a0, reproduces app/test/build configuration source. BUILD_INFO sets sourceCommit=null and separately records sourceBaseCommit/sourcePatchSha256.

## Final composition and verification

Single-line clickable Goals show separate manual percentages and existing right priority caps, without Goal progress bars. The seven ordered buttons stay full width, at least56dp high, with8–10dp gaps. Intrinsic minimum sizing preserves content/touch targets; existing Column weights give spare height to buttons instead of large empty section gaps. Section gaps are4dp, with no bottom content padding. Weekly footer contains only This Week, actual/target minutes, percentage, Edit weekly target and the weekly bar. This Week retains the Statistics callback; Edit retains the existing weekly-target destination. Cumulative Total practice is absent from Home. Footer has no rounded container/clip, and the weekly bar uses square ends without a stop decoration. System navigation insets remain outside the content. Cosmic softer-blue secondary Home actions/bright Practice Now and Lato remain; no data/audio/storage/navigation destinations change. Room27/ZIP12/JSON7 unchanged.

**3/3 focused host-rendered Android/Compose checks pass**, zero failures/errors/skips, using existing cached Robolectric/Compose tooling through a temporary ignored verification configuration, with no retained dependency/build-configuration additions. At360x800 with24dp top/24dp bottom reserved system space and411x886 with24dp top/48dp bottom, normal text has **exactly zero scroll range**, every button/footer control visible, at least56dp button heights, at least8dp button gaps, and all12 callbacks. At360x800/200% text all controls remain reachable by scrolling. Both normal renders and the enlarged-text render were visually inspected. Evidence, temporary check source/configuration and three PNGs are in the ZIP. These are host-rendered fixtures, not emulator/physical-phone results.

Local offline release assembly, release vital lint and instrumentation APK assembly pass. Updated existing HomeFitWorkflowTest mirrors normal zero-scroll/reference composition and large-text reachability; its Android-device execution remains unperformed. No connected device or emulator; no full regression, new test project, broad suite or unrelated investigation. An inset check initially found1px of scroll on the smaller layout; removing duplicate Goal-group bottom padding corrected it without shrinking buttons or weakening the zero-scroll assertion. Initial host screenshot capture timed out; direct native Canvas rendering produced the reviewed captures. A temporary DpRect helper compile error was corrected before executing checks; the enlarged-text fixture was corrected to scroll to the lower numeric row before asserting visibility. No physical acceptance inferred.

**Next: install build124 over the existing app and compare the opening view with the owner's screenshot, including the real phone's navigation bar and normal font setting.** Shorter or enlarged-text configurations may scroll to preserve full-size controls. Git publication remains outstanding through the preferred workflow. Stop at this Home-only correction; no other redesign/F1.

---

# Current state — compact Home delivered, build122

Updated November 12, 2026 (America/Los_Angeles). Owner-requested Home correction delivered as signed **0.46.1/build122** on **block-ux-stage1**. Exact application/package/focused-test source **a70d341901822a87ab3b5876541e35ab418af18f**. Later closeout changes documentation only; application/test source remains identical. Canonical checkout used; older OneDrive checkout untouched.

## Signed delivery

OneDrive → Practice Log development → **Home fit delivery → Home fit build122**.
[APK](dist/practice.logger-0.46.1-b122-a70d341-homefit-development/practice.logger-0.46.1-b122-a70d341.apk) · [ZIP](dist/practice.logger-0.46.1-b122-a70d341-homefit-development.zip) · [Phone checklist](MANUAL_TESTS.md#compact-home-follow-up). dist/LATEST.txt points here.

- APK SHA256 **0c302b6287b9129e1a1d59c8942f85679bd6056412ffe977c522e1b3539bef2e**.
- ZIP SHA256 **5af21f5930225e9c600dd22cb1fb77af9b04d6c04d8308a5af689654e898425c**.
- Stable ID com.mrayover.practicelogger.app, permanent certificate SHA256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Package/version/signature/pinned certificate, exact148 unsigned payload entries, APK/ZIP CRC/length, eight ZIP member hashes and OneDrive copied hashes verified. All **592** prior APK/ZIP files retain their hashes. Cloud-sync completion unobserved. Install over the existing permanent-key app without uninstalling/clearing data.

## Home correction and actual checks

Goals keep one line with ellipsis and full text semantics. Edit shares the centered Goals header. Home uses tighter4dp gaps, reduced padding and48dp buttons. At normal text below700dp available content height, full-width Practice Now is followed by paired Blocks/Routines, Tools/Recording and Log/Library rows; taller screens keep the vertical list. Statistics/weekly-target links share a row, above a compact one-line weekly summary/progress bar. This Week retains full target detail. Goal progress/clicks, all destinations, themes, other pages and audio/data/geometry remain. Room27/ZIP12/JSON7 unchanged. At150% and larger text, navigation and footer links stack at full width and can scroll to keep labels readable and controls reachable.

- Windows release/testAPK compilation and release vital lint pass; final retained-test expected-text change also compiles. No host test suite or broad regression was run for this visual correction.
- [Focused CI43375803182](https://github.com/mrayover/practice.logger/actions/runs/43375803182): debug/release/testAPK compilation, release vital lint, install/launch and **1/1 HomeFitWorkflowTest** pass on one API35 configuration. The initial CI43373972840 already passed normal fit; screenshot review prompted a focused enlarged-text refinement and this final-source rerun. At normal text, bounded320×640,411×731 and360×900 Compose viewports each have zero Home scroll range, single-line ellipsized long Goals, every control visible, and all12 Home callbacks invoked per layout (**36 callbacks**). At200% text, both Goal rows and all navigation labels remain one line without navigation overflow, and the weekly target remains reachable by scrolling. This is a focused layout fixture, not a device matrix or real-phone result.
- Reviewed five final captures: the three normal-text Home layouts plus200% Goal/header and scrolled navigation/footer. Short/ordinary layouts show all destinations and footer in one view; tall layout retains the vertical menu. Screenshots use the rendered Compose image to avoid the prior native-frame capture race.
- Intermediate CI43375007728 passed normal fit but failed a generic text-overflow flag at200%. The screenshot showed readable full-width labels. Final verification replaces that flag with rendered line bounds and text containment in each button, with at most one physical-pixel rounding allowance; one-line/no-ellipsis requirements remain. Those measured checks pass. Production layout is unchanged by this test correction; the precise generic-flag cause is not inferred.
- Retained WeeklyTargetWorkflowTest now expects the compact Home summary (This Week ·90/180min·50%); the detailed This Week assertion remains intact. That older workflow was not rerun.

**Next: install build122 and confirm Home fits on the owner's phone at normal text size; report model/font/orientation if anything clips.** Physical install-over, TalkBack, landscape and cloud sync remain pending. Stop after this Home-only correction; no F1 or merge to main.

---

# Previous UI-C1 delivery — build121

Updated November 12, 2026 (America/Los_Angeles). Signed **0.46.0/build121** delivered on **block-ux-stage1**. Exact application/test/package source **6e26000a66c3d5669ee330dafab0021d3f3c5d6a**; later closeout changes documentation only. Canonical C:/Users/mrove/Projects/practice.logger used; older OneDrive checkout untouched. Physical acceptance pending.

## UI-C1 delivery and integrity

OneDrive → Documents → ChatGPT → Practice Log development → **Cosmic UI-C1 delivery → Cosmic UI-C1 build121**.
[APK](dist/practice.logger-0.46.0-b121-6e26000-uic1-development/practice.logger-0.46.0-b121-6e26000.apk) · [ZIP](dist/practice.logger-0.46.0-b121-6e26000-uic1-development.zip) · [Phone checklist](MANUAL_TESTS.md#ui-c1--cosmic-and-centered-page-headings) · [Coverage/design](docs/COSMIC_UI.md). dist/LATEST.txt points to this delivery.

- APK SHA256: **34a7d860471a918048e64bfe4b65677656be4fb033494b9d6d41322f73f9e02b**.
- ZIP SHA256: **5437f36e248dbc4547c95ebc6b5fbdea2778ab103774d7c83a4b4c2c6ac59887**.
- Stable app ID com.mrayover.practicelogger.app; unchanged permanent certificate SHA256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Publisher verifies package/version/signature/pinned certificate. Final signature recheck, exact unsigned payload, APK/ZIP CRC/length, all eight ZIP member hashes and copied file/ZIP hashes pass. All **586** prior APK/ZIP hashes match. Cloud-sync completion unobserved.
- Install over the permanent-key app without uninstalling/clearing data. Room27/ZIP12/readers1–12 and JSON7/readers1–7 unchanged. No audio/timing/storage/backup/Playground geometry implementation changed.

## Centered headings and Cosmic

Shared PageHeading centers primary titles over the full content width with Lato and heading semantics. Coverage includes Home's existing centered Goals; Practice Now/unfinished/Ready/focused Block; Block Library/editor; Goals/detail/editor/associated Blocks; This Week/Prepare Week; session builder/picker and Routines/editor; Tools/tool pages/Piano; Recording/management; Transcribe; Media Library/pickers/audio/visual viewers; Inspirations/editor/detail; Log/Weekly View/Playground; Statistics; reflections/Session Summary; Backup and Settings. Audio Settings is a subsection of the existing Settings page, retaining its alignment. Banner branding, navigation controls and ordinary body/subsection/list text remain. Editor reserves equal measured side-control space; Settings balances its scrollbar gutter. Constrained dynamic titles wrap/ellipsize; compact practice timer retains its row, with a full-width single-line name and existing measured constraints/pinned actions.

Cosmic joins the existing selector/preferences/resolver before System Default. Light-blue actions, lavender selections, pale-violet accents and readable dark surfaces sit over a quiet navy/violet gradient with 96 static deterministic pale stars and subtle glow. One cached noninteractive root drawing layer; no animation, timer, external assets or media interaction. Prior palettes/resume-button appearance, five historical Goal colors and recording/error roles are preserved. Native Cosmic style/window/system-bar handling uses the existing in-place mechanism; startup/recovery uses its deep background. Original media and specialized opaque surfaces remain.

## Focused verification actually run

- Windows **8/8**, zero failures/errors/skips: ThemePaletteTest4, ThemePreferencesTest3, CosmicBackgroundTest1. Covers semantic contrast including resume actions, unchanged Goal fills, explicit/System resolution, selector IDs/order, every choice surviving a new reader, commit rollback/retry and static-star bounds. Debug/release/testAPK assembly and release vital lint pass. Final source's native fixture compilation passes.
- [Final focused CI43371397756](https://github.com/mrayover/practice.logger/actions/runs/43371397756) on the exact source above: the same **8/8 Linux checks**, debug/release/testAPK compilation, release vital lint, install/launch and **1/1 native workflow on one API35 configuration** pass.
- Actual selector choice, recreation, native background/dark system-bar icon modes, Light switch-back and retained recording ViewModel identity pass. At a 320dp Compose viewport/200% text, shared full-width alignment/text-layout assertions pass for representative navigation, long Goal/Block names, editor and focused timer visibility. Earned Playground red pixel/selection/pan and shared waveform/input/dialog surfaces are sampled. This is a bounded viewport fixture, not a physical 320dp device or a device matrix.
- Reviewed all 11 final captures plus the first-run failure: Settings/Home, narrow Practice Now/editor/dynamic Goal/Metronome/Media Library/Playground/focused Block and dialog. Stars are visible and restrained; sampled headings wrap/truncate without covering navigation. The dedicated waveform/input capture retained the preceding focused frame; the waveform visibility assertion passes and the subsequent dialog capture shows the synthetic waveform/input underneath its dim layer. No separate undimmed waveform screenshot or audio/listening claim.
- Initial CI43370361898 on d2f2aa5 passed host/build stages but failed the new Settings alignment assertion: center524px versus page540px. Screenshot/semantics confirmed the 12dp end scrollbar gutter. Balancing only the title start inset fixes it; the assertion remains unchanged. Final audit also adds Cosmic-specific semantic resume colors while preserving the old green in earlier themes. No unrelated failure investigation or broad rerun.

## Limitations and exact next task

Physical install-over, TalkBack, vendor bars/display, landscape, real-phone large text, recording/reference interaction, full Playground pinch/rotation and cloud sync remain pending. Existing theme/audio/data workflows were preserved in source; this increment does not certify all cross-feature behavior. No broad UI/accessibility/audio/backup suite, historical migration matrix, comprehensive lint investigation or F1 was run. All earlier distributions remain; no merge to main.

**Next: perform the UI-C1 build121 owner-phone checklist and report concrete issues. Stop after UI-C1; further features and F1 require separate assignment.**

---

# Previous RW2 delivery — build120

Updated November12,2026 (America/Los_Angeles). Signed **0.45.0/build120** delivered on **block-ux-stage1**. Exact application/test/package source **c43546e1072aeb54965b02c4e477d056eadd50f6**; later closeout changes documentation only. Canonical C:/Users/mrove/Projects/practice.logger used; older OneDrive checkout untouched. Physical acceptance pending.

## RW2 delivery and integrity

OneDrive → Documents → ChatGPT → Practice Log development → **RW2 delivery → RW2 build120**.
[APK](dist/practice.logger-0.45.0-b120-c43546e-rw2-development/practice.logger-0.45.0-b120-c43546e.apk) · [ZIP](dist/practice.logger-0.45.0-b120-c43546e-rw2-development.zip) · [Owner-phone checklist](MANUAL_TESTS.md#rw2--waveform-and-seekable-recording-review). dist/LATEST.txt points to the verified copy. Signed candidate retained separately; use the final rw2-development package.

- APK SHA256: **2f4e6a3cbb865d5af63190f3656ac054a2c8a038f5b88d0f8ae2f3396617e7f4**.
- ZIP SHA256: **b9a31b8d933bb3899b5c7f883e8bdcbacc326e6a769c4485440ed1017ddb9654**.
- Stable app ID com.mrayover.practicelogger.app; unchanged permanent certificate SHA256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Publisher verified package/version/pinned certificate/signature. Final signature recheck, exact148 unsigned payload entries, APK/ZIP CRC/length, all8 ZIP member hashes and copied file/ZIP hashes pass. All **580** pre-session APK/ZIP hashes match. Cloud-sync completion unobserved.
- Install over the permanent-key app without uninstalling or clearing data. Room27/ZIP12/readers1–12 and JSON7/readers1–7 unchanged. No schema, migration, repository/promotion, backup, stored media ownership or owner-phone data changed.

## Actual waveform and transport coverage

| Environment | RW2 result |
|---|---|
| Focused Practice saved Free and Loop | Existing RW1 waveform, cursor and deliberate seek retained |
| Full Block Recordings | Deliberate Inspect take selection, one shared selected waveform/transport, no selection autoplay; Rename/Export/Delete retained |
| Standalone Free Recordings | Same collection implementation and transport |
| Temporary Free review | Shared compact review waveform/cursor/timestamps and stopped/playing seeks in TakeReviewControls |
| Temporary Loop review | Same review waveform in inline Loop Review; existing complete-loop audition retained |
| Block and standalone Record Reference | Same RecordReferenceDialog review waveform; existing canonical Keep and Block append-and-select semantics retained |
| Media Library, Block and standalone Transcribe | Existing complete transport unchanged |

Reuses RecordingScrubber/CompactRecordingWaveform, bounded cancellable decoding and four-entry process cache. Free/imported audio keeps MediaPlayer; recorded loops keep AudioTrack/repeat. No renderer/player/clock, waveform files or database is added. Temporary seeking requires the exact validated active review object, matching pending identity/file/Block context, foreground/open owner and no capture or controller work. seekSaved still rejects review. Selection and preparation use source/generation guards; Free validation also rejects a callback from a departed owner. Discard/Re-record/Save/Keep/leave cancel pending review decoding. Failed Save/Keep retains reviewable temporary bytes; successful promotion remains atomic. Capture/count-in/timers and existing actions remain. [Full reuse/safety contract](docs/RECORDING_WAVEFORMS.md).

## Focused verification actually run

- Windows **6/6**, zero failures/errors/skips: RecordingWaveformTest2 (timeline/frame bounds, real PCM/cache, bounded long envelope, corrupt/missing fallback) and TakeReviewTest4 (temporary ownership, complete/incomplete loops, rollback/retry, discard). Debug/release/testAPK assembly and release vital lint pass; final native test compilation passes. Initial compile exposed a property/JVM setter naming collision; corrected before checks.
- [Focused CI43362804723](https://github.com/mrayover/practice.logger/actions/runs/43362804723) on the exact source above: the same **6/6 Linux host tests**, debug/release/testAPK assembly, release vital lint, install/launch and **2/2 native tests on one API35 configuration** pass. No rerun or native failure occurred.
- Existing RecordingWaveformWorkflowTest covers real saved Free M4A, recorded Loop WAV and imported MP3/M4A/WAV; stopped/playing seeks and repeat; one drag seek on release; switching during prepare and drag; background stop; fallback and320dp compact practice controls.
- RecordingCoverageWorkflowTest exercises both actual collection sheets at200% text, deliberate selection/cue/Play/switch and stale-seek rejection; real microphone Free/Loop and both Reference review paths; exact-object/saved-path guards, failed Save/Keep with same bytes, atomic promotion, Re-record/Discard, foreground and leave cleanup including pending decoder cancellation. The temporary Loop is exercised through the shared review surface; its inline host integration compiles but was not separately navigated on device. Reference Keep was checked in the dialog/recording controller; unchanged higher-level source-selection callbacks were not independently renavigated.
- Reviewed captured collection/action and Reference/shared Loop review screenshots. The collection screenshots show the scrolled take rows rather than the waveform; waveform visibility itself was asserted by the native test. Emulator mic captures are silent/near-silent, so review envelopes may be flat. No physical acoustic inference is made.

## Limitations and exact next task

Physical install-over, actual listening/loop joins, TalkBack, focus/route interruption, narrow-phone/landscape/full inline Loop layout and provider/cloud sync remain owner acceptance work. No broad media suite, Android matrix, full lint campaign, historical migration checks, comprehensive regression or F1 was run. Earlier PG1.1 and all distributions remain preserved; no merge to main.

**Next: perform the RW2 build120 owner-phone checklist and report concrete issues. Stop after RW2; F1 and further audio changes require separate assignment.**

---

# Previous PG1.1 delivery — build119

Updated November12,2026 (America/Los_Angeles). Signed **0.44.1/build119** delivered on **block-ux-stage1**. Exact application/test/package source **5ffdbaad83368ba6f4bb20125a9da1303c251415**; later closeout changes documentation only. Canonical checkout C:/Users/mrove/Projects/practice.logger used; older OneDrive checkout untouched. Physical acceptance pending.

## PG1.1 delivery and integrity

OneDrive → Documents → ChatGPT → Practice Log development → **PG1.1 delivery → PG1.1 build119**.
[APK](dist/practice.logger-0.44.1-b119-5ffdbaa-pg11-development/practice.logger-0.44.1-b119-5ffdbaa.apk) · [ZIP](dist/practice.logger-0.44.1-b119-5ffdbaa-pg11-development.zip) · [Phone checklist](MANUAL_TESTS.md#pg11--visual-and-interaction-refinements). dist/LATEST.txt points to the copied delivery.

- APK SHA256: **2ed2633fc2db1f5b0ce5a38af422e8ee461895bff1fc524a73c83f913fad3e83**.
- ZIP SHA256: **75c7bf07583d2d85ba5a10339b3abe20dd7577259458713fc4adf2e6f2b73c32**.
- Stable app ID com.mrayover.practicelogger.app; permanent certificate SHA256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Publisher package/version/signature/pinned-certificate verification passes. Exact CI unsigned payload, APK/ZIP CRC/length, all8 ZIP member hashes and copied file hashes pass. All **574** earlier distribution APK/ZIP hashes match, including build118 and preserved build117 artifacts. Cloud-sync completion unobserved.
- Install over the permanent-key app without uninstalling/clearing. Keep a complete ZIP backup. Room27/ZIP12/readers1–12 and JSON7/readers1–7 unchanged; no schema, repository, migration, archive writer or history change. No owner phone/data altered.

## Playground behavior

PG1 remains completion-backed: one plain solid historically colored square per authoritative completion, immutable identity/color/history, deterministic placement, centered proportional20/40/80/160 expansion and one-unit oriented geometry. ZIP12 preserves exact world coordinates/angles. Missing historical Goal titles remain unavailable; no current Goal fallback or fabricated rewards. Read-only historical details, expandable panel and Journal navigation remain.

PG1.1 adds faint neutral world-anchored dots behind squares, viewport/world-bounded rendering with power-of-two thinning only at extreme zoom-out, a20% closer initial camera and a stronger static external selection halo. Overview retains90%-fit full world. Pinch/free twist remain. Painted squares and collision bounds stay exactly one unit.

Tap unselected square selects; empty tap deselects after touch-slop handling. Drag selected square or empty space while selected moves only the selected square by relative deltas. Without selection, background drag pans. Background pinch beginning before a drag owns zoom/pan even with selection. Ownership stays fixed until all pointers lift; adding a second finger after a remote drag begins does not transfer ownership. Selected-origin two-finger twist remains available.

45°/Upright set exact absolute angles through the existing conservative shortest-arc validation and arrangement repository. Obstruction rejects the entire preset, preserving the previous angle and position with a more-space message. Existing swept translation stops at contact and world bounds, including fast background drags; other squares remain stationary. No snapping, pushing, physics or persistence changes.

## Focused verification actually run

- Windows **16/16**, zero failures/errors/skips:11 PlaygroundGeometryTest methods (including exact45°/0°, repeat preset, wrapped shortest arc, obstacle/boundary/intermediate-arc rejection) and5 existing PlaygroundRepositoryTest data checks. Debug/release/testAPK compilation and release vital lint pass; extended native test compilation passes.
- [Focused CI43359247460](https://github.com/mrayover/practice.logger/actions/runs/43359247460) on exact source above: **16/16 Linux checks**, debug/release/testAPK assembly and release vital lint; install/launch and **one API35 PlaygroundWorkflowTest pass**. Covers initial20% painted-width comparison, faint dot pixel checks and selection screenshots in Light/Dark/Smooth/Sunny/Orchid/Submarine, frozen colors, relative remote drag/no jump, stationary neighbors, fast remote collision, blocked preset message/unchanged angle, exact45°/0°, free twist, selected-background pinch, deselected pan, details/Journal and saved arrangement after recreation. Screenshots visually inspected.
- Initial local command used an absent SDK path and stopped before compilation; corrected to the existing SDK. No broad regression, full lint, unrelated suite, device matrix or F1 run. Existing schema/backup production files are unchanged.

## Limitations and exact next task

Physical permanent-key install-over and gesture feel, enlarged text/landscape, actual-provider backup and cloud sync remain unverified. Camera/selection remain visit-only. Existing bounded placement/rotation may conservatively stop; no overlap is forced. Prior baseline PG1 build118 remains preserved (sourcef55a0c764e5a92743f51e152497d53766fcaae15; prior evidence in Git history).

**Next: perform the PG1.1 build119 owner-phone checklist and report concrete issues. Stop after PG1.1; PG2, animations/templates and F1 require separate owner assignment.**

---

# Previous W2 development delivery — build116 (historical)

Updated November12,2026 (America/Los_Angeles). **Signed 0.43.0/build116 is delivered. W2 engineering and focused verification are complete; physical owner-phone acceptance remains pending.** Exact application/package/test source **f8ebd95a4d625944ec07f2eb285a253d45dd20ac**, branch **block-ux-stage1**. Later documentation commits do not change the signed application. Canonical checkout C:/Users/mrove/Projects/practice.logger was used; the older OneDrive checkout is untouched, with only a new delivery folder beside it.

## Delivery and integrity

OneDrive → Documents → ChatGPT → Practice Log development → **W2 delivery → W2 build116**.
[APK](dist/practice.logger-0.43.0-b116-f8ebd95-w2-development/practice.logger-0.43.0-b116-f8ebd95.apk) · [ZIP](dist/practice.logger-0.43.0-b116-f8ebd95-w2-development.zip) · [Phone checklist](MANUAL_TESTS.md#w2--weekly-practice-target).
Local final folder: dist/practice.logger-0.43.0-b116-f8ebd95-w2-development; dist/LATEST.txt points to the verified OneDrive copy. BUILD_INFO, VERIFICATION, INSTALL, PHONE_CHECKLIST, SIGNATURE and SHA256SUMS accompany the APK and incremental-signature companion.

- Stable app ID com.mrayover.practicelogger.app; unchanged permanent certificate SHA256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Package/version/pinned identity/signature verified by scripts/Publish-LocalApk.ps1.
- APK SHA256 **52544004b76dee45c22046538f56c171a282a52ffd0717d9d143669599fafb7d**.
- ZIP SHA256 **7b15900b8d0749c65168a5c8adccfd830ed86c0ea8b1f787b507ce15acd613b9**.
- Exact successful CI unsigned release payload matches the signed APK. APK/ZIP CRC and lengths, all 8 ZIP member hashes, local delivery-copy hashes and all 552 pre-session distribution APK/ZIP hashes pass. Cloud-sync completion is unobserved.
- Install over the permanent-key app **without uninstalling or clearing data**; keep a complete pre-update backup. Room25→26 is additive; complete ZIP11 needs the new reader and build115 cannot open Room26. APK downgrade is not rollback. No owner phone was installed/reset or owner data altered.

## Implemented W2 behavior

Home previously had the cumulative Total practice → Statistics control only. It retains that control and adds a compact current-week actual/target summary, progress bar, uncapped percentage, remaining/extra minutes and Set/Edit route to This Week. This Week previously contained calendar navigation and Block preparation/assignment; it now has an independent compact target editor above those controls, with no Block preparation required.

One optional target per canonical local Sunday week, positive whole 1–10,080 minutes. Explicit set/edit/clear for current and upcoming weeks; previous weeks are read-only. Historical targets survive reopening/rollover and are never inherited automatically. No daily obligations, counter, notification, mastery or manual Goal-progress changes; G3/LGE/RW1 remain.

Actual practice reuses G3's existing persisted union: distinct placement activeMs, including partial/unfinished/overtime practice, plus completed standalone Transcribe once. No planned duration, Prep, pause, passive playback or Goal/Routine duplication. Placement time belongs to session start date, Transcribe to activity start date; no invented midnight split. W2 uses Sunday–Saturday local start-of-day boundaries with end exclusive and DST/time-zone handling. Journal/G3 remain Monday–Sunday. The existing lifecycle-aware 15-second poll now refreshes week and zone, checking immediately on foreground resume. Original milliseconds drive ratios; only visual fill caps 100%, while numeric progress and extra time remain uncapped. Display rounding never enters the calculation. [Accounting contract](docs/PRACTICE_STATISTICS.md#w2--weekly-practice-target).

Room **26** adds only weekly_practice_targets(weekStart,targetMinutes) through explicit 25→26. All29 old entity definitions and schemas 1–25 are preserved. Complete ZIP writer **11**, readers **1–11**, adds required target rows/counts with canonical Sunday/date, integer/bounds and duplicate-week validation. Frozen ZIP 1–10 restore without fabricated targets. Legacy JSON **7**, readers **1–7**, unchanged/excludes targets. Target-only installs reject both restore paths, including transactional recheck after preview. Media ownership, file locks, journal, witness and recovery remain. [Backup contract](BACKUP_FORMAT.md#complete-portable-zip-v11--weekly-practice-targets).

## Actual focused verification

- Windows: **7/7 WeeklyPracticeTargetTest methods pass**, zero failures/errors/skips. CRUD/reopen/history/current/future/rollover, malformed/overflow bounds, under/exact/over/unset/partial precision, Sunday versus Monday and DST/time-zone boundaries, G3 persisted unfinished/repeated/overtime/completed Transcribe without double-counting, Room25 upgrade/preservation, ZIP11 actual archive round-trip, ZIP10 actual envelope/default, duplicate/malformed target rejection and JSON target-only guard. Debug/release/testAPK assembly and release vital lint pass.
- [Manual W2 preview43326881686](https://github.com/mrayover/practice.logger/actions/runs/43326881686), exact sourcef8ebd95a4d625944ec07f2eb285a253d45dd20ac: **7/7 Linux host tests**, release vital lint and all three APK compilations pass. API35 install/launch smoke and **one WeeklyTargetWorkflowTest pass (11.386seconds)**: Home/This Week routes, cumulative Statistics, numeric invalid/set/edit/clear, live50%/150% progress, read-only historical/explicit upcoming target, recreation, real-filesystem PortableRestore precommit rollback/retry and target-only guards before stage/after preview. Home/This Week screenshots inspected.
- Initial debug compile exposed public/internal progress-type visibility; corrected. Expanded Windows8-test attempt had7 passes and one Robolectric ShadowLinux directory-open EIO at syncPrivateDirectory (PortableRestore.kt254) during recovery finalization. The filesystem scenario moved to the same Android workflow; production recovery was not changed. Its initial transferred fixture lacked Flow.first import; corrected before CI. First native attempt43325357425 passed host/build/launch and initial target/Home checks but timed out at fixture keyboard dismissal during edit. The helper now uses Android Back for the visible dialog IME; final workflow passes, with no production recovery or input behavior change. Retained LGE fixture version/table expectations were mechanically updated; LGE suites were not rerun.
- No full regression, historical compatibility/device matrix, broad lint investigation, media/LGE campaign, Playground, F1 or F2 was run. Narrow/enlarged-text/theme, actual Sunday transition, populated owner data and physical-phone acceptance remain on the phone checklist; deterministic calendar/recreation tests do not establish those physical results. Existing Stage3/G3/LGE/RW1 phone checks remain pending; Stage4 remains0/10 owner-deferred sessions.

## Exact next task

**Owner-phone W2 weekly-target checklist.** Stop W2 engineering here. Recommended next development milestone is a separately bounded owner assignment for Playground after reviewing remaining feature priorities; no Playground implementation, feature freeze or F1 is initiated.

---

# Previous RW1 delivery — build115 (historical record)

Updated November12,2026 (America/Los_Angeles). **Signed0.42.0/build115 is delivered. RW1 implementation and focused verification are complete; physical owner-phone acceptance remains pending.** Exact application/package/test source **145ff9d0a61dff7aa1f26cfe79e91c148e124f7a** on **block-ux-stage1**. Later documentation commits do not change the signed application. Work used C:/Users/mrove/Projects/practice.logger; the older OneDrive checkout is untouched, with only a new delivery folder added beside it.

## Delivery and integrity

OneDrive → Documents → ChatGPT → Practice Log development → **RW1 delivery → RW1 build115**.
[APK](dist/practice.logger-0.42.0-b115-145ff9d-rw1-development/practice.logger-0.42.0-b115-145ff9d.apk) · [ZIP](dist/practice.logger-0.42.0-b115-145ff9d-rw1-development.zip) · [Phone checklist](MANUAL_TESTS.md#rw1--saved-recording-waveform-scrubbing).
Local final folder: dist/practice.logger-0.42.0-b115-145ff9d-rw1-development; dist/LATEST.txt points to the copied OneDrive delivery. BUILD_INFO, VERIFICATION, INSTALL, PHONE_CHECKLIST, SIGNATURE and SHA256SUMS accompany the APK and incremental-signature companion.

- Stable app ID com.mrayover.practicelogger.app; unchanged permanent certificate SHA256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Package/version/pinned identity/signature verified by the existing permanent-key publisher.
- APK SHA256 **1f7bbbd590c163328ce1367a547edca07a029f77cbc03eafa56e66f3180ce613**.
- ZIP SHA256 **2d0133eb79ddc7c90ae1a566e8cf50fdc99f88485f82c4f2458941940e93ffd8**.
- Final APK uses the exact successful CI unsigned release. Every unsigned payload entry matches; APK/ZIP CRC and lengths, all8 ZIP member hashes, copied delivery files and all542 pre-session distribution APK/ZIP hashes pass. The earlier locally signed5e3dfc7 candidate is preserved separately. Cloud-sync completion is unobserved.
- Install over the existing permanent-key app **without uninstalling or clearing data**. Keep a complete pre-update backup. Room25, complete ZIP10/readers1–10 and JSON7/readers1–7 are unchanged. No owner data or physical-phone installation was altered.

## Implemented scope

Existing Focused Practice saved Free/Loop Recording rows now include a compact48dp amplitude waveform touch area, visible cursor, elapsed/total timestamps and accessible seek controls. Waveform changes with the selected take; selection stays silent. Tap seeks immediately on release. Horizontal dragging previews cursor/time under the finger without audio seeks, then commits one source-bound seek; cancelled/changed-source gestures do not commit. Vertical scrolling remains available. Existing Play/Stop, explicit recording selection and temporary Review Take remain.

Waveform extraction/rendering reuses the Library utilities without its player/state model. IO decoding is cancellable when selection/view changes; codec/extractor resources close on cancellation. A four-entry process-only LRU keys file path/length/mtime/duration. Adaptive bins retain at most180001 peaks per item, including long Free Record files; decoder work has the existing120second deadline. Missing/invalid/unreadable files show Waveform unavailable without affecting other takes or playback controls. No stored derivative/database entity is added.

MediaPlayer seeks use the existing instance with valid-duration clamping and actual-position reporting; pending preparation and initial file-read requests are invalidated by source changes. Stopped seeks only cue the next explicit Play. Static recorded-loop AudioTrack seeks pause before setting the bounded PCM frame, restore existing full-loop repeat points, and resume only prior playback. Cursor position derives from actual consumed-frame deltas modulo original loop length; it wraps without a new playback engine. Stop/background/leave clear temporary cues. No timer, practice accounting, metronome, reference checkpoint, capture/review, media ownership or persistence changes.

## Actual focused verification

- Windows: **2/2 focused RecordingWaveformTest methods pass**, zero failures/errors/skips. Coordinate/duration/PCM-frame bounds, actual WAV amplitude peaks, unchanged-file cache, bounded long-take bins and corrupt/missing WAV rejection. Debug/release/instrumentation compilation and release vital lint pass; final native fixture revision also compiles locally.
- [Manual focused preview43316281814](https://github.com/mrayover/practice.logger/actions/runs/43316281814), exact source145ff9d: **2/2 Linux host tests**, release vital lint and all three APK compilations pass. API35 install/launch smoke and **one RecordingWaveformWorkflowTest pass (5.955seconds)**. Verifies real waveform extraction and actual-engine seek for saved Free Record M4A, imported MP3/M4A/PCM16 WAV and recorded phrase WAV; repeat wrapping for imported/recorded loops; stopped cue then explicit Play; initial pending-play selection cancellation; drag finger-following/no seek until release/one commit; tap; stale gesture/source rejection; controller background stop/no resume; corrupt-file UI recovery; and actual compact-control visibility at320dp.
- This native fixture isolates the recording controller and compact component; it is not a complete practice-screen/physical acoustic campaign. The screenshot shows the preceding isolated waveform fixture and is not accepted as final two-row visual evidence. Full Focused Practice phone appearance/scrolling/listening checks remain in the owner checklist. Existing capture/focus/recreation protections were retained; no new comprehensive claims are made for those paths.
- Earlier focused runs43315410949 and43316140243 were cancelled before final native verification as the pending-play identity guard and fixture foreground setup were finalized. Their partial compilation/checks are not complete passes. Replacing Activity content disposes its normal foreground observer; the isolated native fixture explicitly restores foreground afterward. Production lifecycle ownership was not changed for the test.
- No android.yml, stage3-final.yml, multi-reference.yml, full historical migration campaign, device matrix, broad lint investigation, comprehensive regression, Playground, F1 or F2 was run. No outstanding failure remains in the final focused gate. Physical Galaxy S26 Ultra/acoustic acceptance and seamless imported codec joins are not claimed.

## Exact next task

**Owner-phone RW1 waveform/scrubbing checklist.** RW1 engineering/delivery stops here. Any next feature and comprehensive F1 require separate explicit owner assignment. Prior Stage3/G3/LGE phone checks remain pending; Stage4 remains0/10 documented sessions with all ten owner-deferred on November12.

---
# Previous LGE-2 signed delivery — build114 (historical record)

Updated November12,2026 (America/Los_Angeles). **Signed0.41.0/build114 is delivered. LGE-2 implementation and focused checks are complete; owner-phone acceptance remains pending.** Application/package/test source **192d52087490551a2b1dc61c7f403e2ce98dce6c** on **block-ux-stage1**. Later documentation commits do not alter the signed application. The designated C:/Users/mrove/Projects/practice.logger checkout was used; the older OneDrive checkout was not edited or synchronized. Only a new delivery folder was added there.

## Delivery and integrity

OneDrive → Documents → ChatGPT → Practice Log development → **LGE-2 delivery → LGE-2 build114**.
[APK](dist/practice.logger-0.41.0-b114-192d520-lge2-development/practice.logger-0.41.0-b114-192d520.apk) · [ZIP](dist/practice.logger-0.41.0-b114-192d520-lge2-development.zip) · [Phone checklist](MANUAL_TESTS.md#lge-2--optional-explainable-suggestions).
Local final folder: dist/practice.logger-0.41.0-b114-192d520-lge2-development; dist/LATEST.txt points to the copied OneDrive delivery. BUILD_INFO, VERIFICATION, INSTALL, PHONE_CHECKLIST, SIGNATURE and SHA256SUMS accompany the APK and incremental-signature companion.

- Stable app ID com.mrayover.practicelogger.app; unchanged permanent certificate SHA256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Package/version/signature/pinned identity verified with the existing permanent-key publisher.
- APK SHA256 **36db3da364c17abbabbf6a5db840bea26dae3c1f557a121037c124bf26c3eddb**.
- ZIP SHA256 **7ae876d79d455c8cab29555671e9eee6df6c1202ac3a942a3b5028dd211a0735**.
- Final APK uses the exact successful CI unsigned release; every unsigned payload entry matches. APK/ZIP entry CRC and lengths, all8 final ZIP member hashes and copied delivery files pass. All532 pre-session distribution APK/ZIP files retain their hashes. The early0bca182 locally signed candidate is retained separately. Cloud-sync completion is unobserved.
- Install over the existing permanent-key app **without uninstalling or clearing data**. Keep a complete pre-update backup. Room25 is unchanged; ZIP10 needs the new reader and an older APK is not a backup rollback method. No physical-phone installation or alteration of owner data was performed.

## Implemented scope

Pure versioned LGE2_INTERVAL_V1 uses the actual elapsed interval from the exact earlier completed Block placement to a later confirmed comparable first attempt. Same original Block/token/criterion/anchor, positive practice time, completed placements/sessions and consistent endpoint/session identities/timestamps are required. Minimum24hours; HELD uses ceil1.5, PARTIAL floor0.75, NOT_YET floor0.5, clamped1–30days. Insufficient evidence offers a clearly labeled3day starting suggestion. First attempts/conditions are musician-reported; external practice cannot be detected. Confidence and duration never establish retention. Corrections are read fresh. Exact endpoint calculations remain separate from G3 accounting.

Learning & revisits shows reason, policy, criterion/evidence, actual delay/outcome and proposed local date. Accept is explicit and transactionally rechecks facts/date/current revision. Manual dates are retained until acceptance. Append-only idempotent provenance preserves independent Return Soon/date, Clear flag, Clear date and No scheduling. Browsing/calculation never writes.

Optional Suggested views reuse Session Builder Add Blocks and Prepare Week Add existing Blocks. Reasons combine without duplicate stable Block IDs. Ordering: explicit flag, due date, recent14local-date comparable difficulty, then active Goal priority with no trustworthy attributed G3 practice in the previous7complete local dates. Existing search, Goal/No Goal filters, active eligibility, manual catalog ordering and explicit Add actions remain. Prepare Week excludes already assigned Blocks for the displayed week. No new navigation pill, automatic dates/plans/sessions/Goal changes, score, cache or background scheduler.

Room **25** remains unchanged; compiler schema25 and all prior schema files match. Complete ZIP writer **10**, readers **1–10**. ZIP10 is required because frozen ZIP9 forbids suggestions; ZIP9 still rejects nonmanual origins/non-null provenance. Accepted snapshots validate bounded fixed-key provenance and immutable observation/endpoint interval, yet survive outcome/context corrections. Existing recovery/media/empty-target/ownership safeguards are unchanged. Legacy JSON **7**, readers **1–7**, remains unchanged/excludes learning/guidance. [Policy/architecture](docs/LEARNING_GUIDANCE_EXTENSION.md) · [Backup contract](BACKUP_FORMAT.md#complete-portable-zip-v10--accepted-lge-2-suggestions).

## Actual focused verification

- Windows **32/32 tests, zero failures/errors/skips** (10policy,6adaptive scheduling/provenance,7existing learning,9unchanged G3). Debug/release/instrumentation APK assembly and release vital lint pass. The exact corrected CI command also passes locally at the final tested source.
- [Manual focused preview43279124139](https://github.com/mrayover/practice.logger/actions/runs/43279124139), exact source192d520: **32/32 Linux tests**, debug/release/testAPK compilation and release vital lint, install/launch smoke and **one API35 LearningWorkflowTest pass**. Covers original complete picker/unassessed exploration, optional Suggested, explicit Add/no implicit writes, optional observation/context/manual date/decline/accepted starting suggestion, independent flag, live timer/metronome preservation, recreation, ZIP10 original-PNG recovery and injected precommit rollback. Screenshot inspected.
- Earlier attempts: initial JRE lacked a compiler; selected the existing full JDK. New exact-endpoint model needed public visibility for generated Room queries; corrected. Superseded43274488213 was canceled during compilation after the additional archive guard. CI43275300807 compiled all APKs, then failed before tests on workflow task-option ordering; corrected without application changes. CI43276069097 passed32tests/lint/compilation/launch, then the preflagged picker fixture showed Clear flag where the test expected Return Soon; the test now explicitly clears/asserts/sets the flag. The next43277387611 native run timed out at manual-date persistence. Added explicit field/visible/enabled checks, awaited IME settlement and failure diagnostics; the final workflow is the successful combination, without an independent keyboard-cause claim. The corrected native testAPK also compiles on Windows. These earlier attempts are not passing complete native results. Details in WORK_LOG.md.
- No android.yml, stage3-final.yml, multi-reference.yml or comprehensive campaign; no historical device matrix, full lint, unrelated baseline investigation, F1/F2 or physical/acoustic/provider acceptance is claimed. The final focused gate has no outstanding failure. Actual permanent-key phone install-over and remaining Stage3/G3/LGE phone checks are pending.

## Exact next recommended task

**Owner decision on remaining V1 features, particularly Playground.** Implement any selected feature only after a separate bounded assignment. Comprehensive F1 requires separate explicit authorization after feature development is finished. Stage4 remains0/10 documented sessions with all ten owner-deferred on November12; no practical-use review or phone acceptance is inferred. Stop LGE-2 here.

---
# Previous LGE-1 delivery — build113 (historical record)

Updated November12,2026 (America/Los_Angeles). **Signed0.40.0/build113 is delivered. LGE-1 implementation and focused verification are complete; owner phone acceptance remains pending.** Application/package/test source **23e0b6660d2a5027421eff7a605869f119574c46**, branch **block-ux-stage1**. Later documentation commits do not change the packaged app. Existing checkout C:/Users/mrove/Projects/practice.logger was used; the older OneDrive reference checkout was not synchronized or edited. Only the new delivery folder was added there.

## Delivery and integrity

OneDrive → Documents → ChatGPT → Practice Log development → **LGE-1 delivery → LGE-1 build113**.
[APK](dist/practice.logger-0.40.0-b113-23e0b66-lge1-development/practice.logger-0.40.0-b113-23e0b66.apk) · [ZIP](dist/practice.logger-0.40.0-b113-23e0b66-lge1-development.zip) · [Phone checklist](MANUAL_TESTS.md#lge-1--optional-learning-evidence-and-manual-revisits).
Local final folder: dist/practice.logger-0.40.0-b113-23e0b66-lge1-development; dist/LATEST.txt points to this delivery. BUILD_INFO, VERIFICATION, INSTALL, PHONE_CHECKLIST, SIGNATURE and SHA256SUMS accompany the APK and incremental-signature companion.

- Stable app ID com.mrayover.practicelogger.app; permanent certificate SHA256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830. Package/version and signature verified with the existing permanent-key tools.
- APK SHA256 **fa170d641216cda19374b34c6b7802d359fc7e9fa9458ec26b4e6e2a35da3869**.
- ZIP SHA256 **698408b7a7fa6f8c9057a1d1c92d9e359218eb09d0b53f3e3ffba8c52d0b60f0**.
- Final signing uses the exact Linux CI release: all148 unsigned payload entries match the signed APK. All8 final ZIP members and OneDrive copies match. All538 prior snapshotted APK/ZIP paths retain their hashes. The earlier locally signed candidate is preserved separately; it is not the final installation target. Cloud-sync completion was not independently observed.
- Keep a complete pre-update backup; install over the permanent-key app **without uninstalling or clearing data**. Build112 cannot open Room25/read ZIP9; APK downgrade is not rollback. No owner phone installation or data alteration was performed.

## Implemented scope

Optional Learning & revisits controls in the practice description, saved Block editor and placement summary provide exact-placement first-attempt observations, explicit first-attempt/recollection/unknown context, immutable criterion snapshots and comparability tokens, and outcome/context corrections without changing original time/criteria. Missing observation differs from Not assessed. Repeated placements are independent. Confidence, manual Goal progress and G3 time remain separate.

Return Soon and manually chosen local revisit dates are independent. Clear flag, Clear date and No scheduling have explicit behavior; declining retains prior choices. Append-only revisioned events retain operation identity, manual date/base/interval/origin and flag history. Completion never automatically clears flags. No reminders, adaptive interval calculation or LGE-2 suggestions are implemented. [Learning contract](docs/LEARNING_GUIDANCE_EXTENSION.md).

Room25 adds only learning_observations and guidance_events with restrictive ownership; all27 existing table definitions and schemas1–24 remain unchanged. ZIP9 adds validated records, linear anchor ordering, revision/state checks and empty-target protection through the existing journal/witness/rollback pipeline. Frozen ZIP1–8 readers remain; no evidence is backfilled. JSON7/readers1–7 are unchanged and exclude LGE data. Use complete ZIP9 for recovery. Cleared scheduling history still prevents permanent Block deletion; Archive retains it. [Recovery format](BACKUP_FORMAT.md#complete-portable-zip-v9).

## Focused verification and limitations

- Windows:7 LearningRepositoryTest +9 unchanged PracticeStatisticsTest methods, **16 passed with zero failures/errors/skips**. Debug/release/instrumentation APK assembly and release vital lint pass. Explicit24→25 upgrade, independent placements, criteria/corrections, unassessed/skipped/unfinished practice, scheduling clear/decline/retries, archive/delete protections, reopen, ZIP9 record roundtrip, ZIP8 no-backfill and malformed evidence/events are covered.
- [Manual focused preview43268142574](https://github.com/mrayover/practice.logger/actions/runs/43268142574), exact source23e0b66: **16/16 Linux tests**, debug/release/instrumentation compilation, launch smoke and **one API35 LearningWorkflowTest pass (12.429seconds)**. It verifies optional skip/save/context, manual flag/date/decline, unchanged live practice/metronome ownership, recreation, complete ZIP9 restore of learning/history and original PNG bytes, and injected pre-commit failure rollback. Screenshot inspected; Close remains visible with the keyboard open.
- Initial test-only JSON-byte and unordered-relation assertions were corrected. Windows bitmap promotion and directory-fsync shadows prevented the newly attempted full journal/media JVM checks; those assertions run on actual Android instead. No media/storage engine was changed to satisfy that environment. These limitations and attempts are retained in WORK_LOG.

No broad regression, full lint, historical migration/compatibility campaign, device matrix or physical-phone acceptance was run. Focused automation does not certify all audio routes, providers, enlarged-text/device combinations or existing Stage3/G3 phone checks. Those remain pending under MANUAL_TESTS; comprehensive regression remains reserved for explicitly owner-authorized F1.

## Next bounded assignment

At the build113 closeout, the next recommended assignment was LGE-2. It has since been explicitly assigned and delivered in build114 above; Playground and F1 remain unstarted. Stage4 remains0/10 real sessions, with the owner's November12 deferral recorded in EVALUATION.md; no practical-use review or phone acceptance is inferred.

---

## Retained build112 delivery record

# Current state — G3 development release, build112

Updated November 12, 2026 (America/Los_Angeles). **Signed 0.39.0/build112 is delivered. G3 functionality and focused verification are complete; owner phone acceptance is pending.** Application/package source **58a13bda4fb5ac594fbfa70a1f237daaf918d01f**, branch **block-ux-stage1**, pushed after separate policy checkpoint **a8d4c9a1f60e3060ec143c08f2145a3dda6f0261**. Later documentation commits do not change the packaged app. Work is in C:\Users\mrove\Projects\practice.logger; the older OneDrive reference checkout was not synchronized or edited. Only the new delivery folder was added there.

## November12 Stage4 owner decision

Stage4 prerequisite is resolved by the November12 owner-approved deferral of all ten undocumented sessions (0/10; no practical-use review or phone acceptance claimed). See EVALUATION.md. Next recommended task: separately assign LGE-1 — Learning Evidence and Optional Revisits. Build112 and Stage3/G3 delivery evidence remain unchanged; phone acceptance is pending. [Decision and evidence limits](EVALUATION.md).

## November12 roadmap-only reconciliation

Live starting branch/remote14749db86f959201e4058b3e88a8a40bf5ecd710 was clean; G3 was already complete, so no active development needed stopping. Integrated the owner-approved LGE-1/LGE-2 architecture and revised sequence, replacing stale next-phase instructions. This assignment changed documentation only: no production/schema/version/workflow changes, tests, compilation, emulator activity, APK generation or LGE implementation. Existing build112, app source58a13bd and all prior delivery/verification facts remain unchanged. Historical CI43257870444 is completed/success; it was inspected, not rerun.

## G3 delivery and integrity

OneDrive → Documents → ChatGPT → Practice Log development → **G3 delivery → G3 build112**. APK **practice.logger-0.39.0-b112-58a13bd.apk**; ZIP **practice.logger-0.39.0-b112-58a13bd-g3-development.zip**. Local final package: dist/practice.logger-0.39.0-b112-58a13bd-g3-development. dist/LATEST.txt points to the same delivery. BUILD_INFO.json, VERIFICATION.json, INSTALL.txt, SIGNATURE.txt and SHA256SUMS.txt accompany it.

- Stable app ID **com.mrayover.practicelogger.app**, permanent certificate SHA256 **f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830**; signature v2/v3, package and version verified with the existing permanent-key publisher.
- APK SHA256 **eaa26e96ee1087c985bee71b338d1af9f51b49e594cdad0bda0f0ed11fa2abdc**.
- ZIP SHA256 **92bba44c3b6c7d7cc4e3ac929de5daa366f22d66fa90d7cf446e598d3b603bee**.
- All148 unsigned CI payload entries match the signed APK; all7 ZIP members match local files. OneDrive copies match. All520 earlier snapshotted APK/ZIP files, including5 build111 paths, retain their hashes; the prior OneDrive build111 APK also matches its recorded hash. Cloud-sync completion was not independently observed.
- Retain a complete backup and install over the permanent-key app **without uninstalling or clearing data**. No phone installation or owner-data modification was performed.

## Implemented G3 behavior

Implemented shared actual-time accounting and Home → Statistics, accurate snapshot-based Goal time/history and optional manual assessment. Explicit No Goal blocks fallback; absent legacy snapshots use only explicit saved Goal-session associations, labeled separately. Unknown time remains unattributed. Unfinished sessions and overtime count; Prep/pause/background/playback do not. Completed standalone Transcribe counts once. No statistics tables or schema changes: Room24/ZIP8/JSON7 retained. [Accounting and date contract](docs/PRACTICE_STATISTICS.md).

Statistics use the Journal's **Monday–Sunday local week**; the separate Sunday–Saturday planning calendar is unchanged. Placement time uses session start date and standalone time uses activity start date, without invented midnight splits. Recent ordering uses those starts, not a precise last-active timestamp; recent views show20 overall/12 Goal records. Stable IDs and confidence/security remain available for later features. JSON7 still excludes standalone activity history; complete ZIP8 preserves it.

## Actual focused verification and limitations

Windows: **9/9 PracticeStatisticsTest checks pass**, zero failures/errors/skips; debug/release/test APK assembly and release vital lint pass. [Manual preview43257870444](https://github.com/mrayover/practice.logger/actions/runs/43257870444), exact source58a13bd: **9/9 Linux scoped units**, debug/release/test compilation, API35 launch smoke and **one StatisticsNavigationTest pass**. Home link, empty state, weekly labels, scrolling, Android Back and banner return checked; screenshot inspected. Initial KSP failure and interim week-boundary unit failure are recorded in WORK_LOG; the final sequential run passes.

No comprehensive regression, full lint, historical migration/backup suite, device matrix, broad accessibility or physical-phone test was run. G3 is a development release with focused verification, not V1 certification. Populated Statistics/Goal UI, enlarged-text acceptance and install-over on the owner's phone remain pending checks in MANUAL_TESTS.md.

## Permanent testing policy and next assignment

Regression workflows android.yml/stage3-final.yml/multi-reference.yml now have no automatic push/PR triggers and require manual owner authorization. Existing tests/scripts remain. Branch API reports protected=false; no safeguards were changed. Comprehensive regression is deferred to separately authorized F1. G3 completion is not feature freeze. Owner must consider remaining check-ins/revisit suggestions, Playground and other work before F1. Stage3 scoped completion and pending physical acceptance remain as recorded below.

**Exact next task:** separately assign LGE-1 — Learning Evidence and Optional Revisits under docs/LEARNING_GUIDANCE_EXTENSION.md. Stage4 is resolved by the recorded owner deferral (0/10 documented sessions); physical acceptance is pending. No LGE implementation begins in this documentation closeout. Approved sequence: **G3 → Stage 4 practical-use review → LGE-1 → LGE-2 → remaining owner-approved V1 features → F1 → F2**. LGE-1 and LGE-2 are approved future scope, not implemented. [Architecture and future focused acceptance](docs/LEARNING_GUIDANCE_EXTENSION.md). Each needs a separate implementation assignment; only the owner authorizes F1 after determining feature completion. [Permanent verification policy](docs/VERIFICATION_POLICY.md).

---

## Prior build111 delivery record (retained)

# Current state — Stage 3 scoped final delivery, build111

Updated November 11, 2026 (America/Los_Angeles). **Signed0.38.0/build111** is delivered under the owner's revised scoped assignment: stop broad regression and finish using passing focused and signed-upgrade checks. **Stage3C.2 is complete. Stage3D scoped delivery is complete; the original broad regression and physical acceptance gates are NOT complete.** No later roadmap work started.

Application/package source **4d1002cd4a358dfdf00ff69e5268e178e0fc247d**, branch block-ux-stage1; b61493e changes only workflow registration. Stage3C.2 implementation/test source8b9c78e, checkpoint record69085cc. Room24, complete ZIP8/readers1–8 and data-only JSON7/readers1–7. No schema/backup-format change was made in3C.2/3D.

## Delivery and provenance

OneDrive → Documents → ChatGPT → Practice Log development → Block UX delivery → **Stage 3 final build111**. APK **practice.logger-0.38.0-b111-4d1002c.apk**; final ZIP **practice.logger-0.38.0-b111-4d1002c-stage3-final.zip**. Local final package is dist/practice.logger-0.38.0-b111-4d1002c-stage3-final; dist/LATEST.txt points to this delivery. Original candidate package is retained separately with its signing-time verification state.

- Package com.mrayover.practicelogger.app; permanent certificate SHA256 f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830.
- APK SHA256 **0bc9ce55a7c49f2444dbd9c626e3ebe7750b905a972220afc023dd6908edd956**.
- Final ZIP SHA256 **f688406d138281325bb0d4a225f61c4be55134cf19735745977f1dc20fe974bd**.
- Existing publisher and final verification confirm identity/version/certificate/signature, exact tested APK hash and ZIP content integrity. OneDrive file/ZIP copies match their local originals. All12 retained APK/ZIP paths for builds107–110 match the preservation snapshot. Remote OneDrive cloud-sync completion was not independently observed.
- BUILD_INFO.json, VERIFICATION.json, INSTALL.txt, SIGNATURE.txt and SHA256SUMS.txt accompany the APK/ZIP. Retain a verified complete pre-upgrade backup and install over build110 **without uninstalling**. Room24 cannot be opened by build110; an APK downgrade is not rollback.

## Implemented practice integration

Compact ordered selectors reuse the existing visual viewer, overlay repository and single audio controller. Stable library:/legacy: identities retain source-local PDF page, zoom/pan and expansion in occurrence-scoped saveable state. Selection does not recreate a Block, placement, timer, Prep, metronome or recording owner. Audio switching pauses output/piano, awaits serialized final checkpoint success, restores incoming Block/media settings paused and cancels/invalidates old preparation/waveform work. Failed persistence keeps the current source/settings for retry. Capture, unresolved review/Save and marker edits guard switching. Link rows and local/master/hidden/overridden markers are retained. Library audio additions and successful Record Reference Keep append and select; failed/cancelled Keep does not change existing links. Ordinary takes and standalone master context retain their existing behavior. Editor staging/recovery contracts are unchanged.

## Actual verification

| Gate | Actual result |
|---|---|
| Checkpoint A | [43163685034](https://github.com/mrayover/practice.logger/actions/runs/43163685034): **9 native checks per API26 andAPI35**, jobs131000486868/131000486614. Two PDFs/image/legacy, independent pages/zoom, exact-source overlays, A→B→A, delayed/failed checkpoint/retry, rapid selection, paused restore, recreation/Prep, failed/cancelled Keep and exactly-one append/select, ordinary Transcribe/capture/focus/background,320dp/200% Light/Dark/Orchid. Screenshots inspected. |
| Preservation units/build | Initial43162713405 and local focused logs pass63 units and debug/release/test assembly with release vital lint. Final Linux [43165127681](https://github.com/mrayover/practice.logger/actions/runs/43165127681) full unit/assembly step **passes** (BUILD SUCCESSFUL); owner cancelled its overall run during full lint. No complete final CI-run pass is claimed. |
| Actual signed110→111 install-over | [43165156145](https://github.com/mrayover/practice.logger/actions/runs/43165156145), API26/job131005223832 and API35/job131005223484: **4 phases per API pass**. Permanent-key package/certificate/version checked on device; all27 populated frozen Room23 tables/old values/IDs and5 original hashes survive baseline launch, actual in-place signed update, Room24 launch and cold reopen. No uninstall/clear/data copy between versions. Temporary private APK staging was deleted; no signing keys were uploaded. |
| Migration/ZIP data preservation | [43164969656](https://github.com/mrayover/practice.logger/actions/runs/43164969656): both APIs pass populated migration/rollback/missing-original preservation, WAL persistence, frozen ZIP1–7, repository/editor/storage/archive validation, injected migration process death/recovery, actual ZIP8 export and separate-empty-install restore. Cold-reopen record/inventory/hash, marker/state and actual playback checks pass before the later UI failure described below. |
| Windows host/lint | Full Windows180 tests/17 failures match prior baseline methods (16 exact failure messages; provider temporary paths differ). Full local lint has the same5 historical error locations and38 warnings; release vital lint passes. Neither a full Windows nor full-lint pass is claimed. |

## Explicitly incomplete and failed checks

The owner explicitly requested **“Stop broad regression; finish scoped delivery”** after the initial handoff. All still-running broad jobs were cancelled; no further regression run or unrelated repair was started. Partial native outcomes are retained rather than described as a full pass.

- Current ZIP UI workflow43164969656 fails on both APIs at PortableZipRoundTripTest.kt:149: it clicks the collection title (now plain text) instead of the Stage3C.1 Preview action, then waits for overlay-layer. All preceding cold-reopen record/original hash assertions and playback checks passed. Later overlay-edit/install-over/interrupted-restore phases of this particular current run were not reached. Their earlier Stage3B passing evidence below remains historical evidence, not a new full-workflow pass.
- Practice group43165087822 fails LaunchTest at its obsolete Save control. Other partial/cancelled runs43165083466,43165132383 and43165137142 report SessionBuilder/GoalPriority timeouts, Recording/Phrase/TakeReview assertions and the LibraryWorkspace old Show reference selector. The Phrase test assumes prior free takes that the isolated fresh setup removed. Other causes remain unestablished; these results are not certified baseline defects or resolved product regressions. Logs are retained under .tools/stage3-final-job-*.log. No broad regression pass is claimed.
- No owner/physical-phone acceptance, TalkBack, cloud/provider, camera/gallery or acoustic/route acceptance has occurred. The Galaxy S26 Ultra and second-phone checklist remains in MANUAL_TESTS.md and delivered INSTALL.txt. Owner's actual populated installation/pre-upgrade backup was not accessed.

**Exact next task: owner phone acceptance of build111 using the retained complete pre-upgrade backup and checklist.** Broad regression remains deferred until explicitly reassigned. Do not begin Stage4, statistics or other roadmap work automatically.

The Stage3C.1 and earlier sections below are historical source/verification records, superseded only where stated above.

# Current state — Stage 3C.1 editor complete

Updated November 11, 2026 (America/Los_Angeles). Canonical checkout: C:/Users/mrove/Projects/practice.logger, branch block-ux-stage1. Baseline bb86f6db5886947ae3e83638f705e20254bb4711; final verified implementation/test source **49aae982420d4f4f59b2386511606d747465bb2b**. Later closeout changes are documentation only.

## Multiple-reference Block Editor

**Stage3C.1 is complete with focused engineering verification.** New and existing Blocks have independent ordered Visual and Audio collections, including mixed images/PDFs and an existing legacy visual. Compact ellipsized rows expose exact Preview/Remove/Up/Down and a subtle first-item Default label. Add offers Library/import and Visual Photo/Gallery. Multiple files can be staged, reordered, previewed and individually removed before one Save. Duplicate canonical/pending imports reuse verified originals, keep position/local work and report duplicate selection.

Manifest v2 retains stable draft/creation identity, upgrades Stage2 scalar intent, records complete ordered baselines/intent and all-item receipts, and preserves unavailable pending items for removal/retry. Save validates intended originals, checks BOTH saved collections and reviewed exact-source loss snapshots, preserves unchanged links and commits Block/Goal/week/Builder/media differences together. Confirmed removal affects only that attachment, keeps shared originals, and rejects later local work changes. Saved visual previews retain existing PDF/overlay tools; pending previews use actual staged bytes. Isolated audio preview offers Play/Stop with focus, route-loss, background and disposal cleanup, without persistent associations or state writes. [Staging/recovery contract](docs/BLOCK_EDITOR_STAGING.md)

## Verified Stage3C.1 gates

- **63 focused host tests pass**, including six new collection checks, draft Save, migration/persistence, audio/overlays, deletion, backup, sessions, Routines, Goals and weeks. Debug/release/Android test assembly and release vital lint pass. Full lintDebug retains the **five unchanged baseline errors and30 warnings** recorded before this increment; no full-lint or full-host-suite pass is claimed.
- [43135686543](https://github.com/mrayover/practice.logger/actions/runs/43135686543), source a3c7839:63 units/builds pass per job. API26 passes23 native checks; API35 passes all storage/recovery/week/migration/frozen archive/restore checks, with one new UI visibility failure. This includes a real process kill after first-file promotion, complete draft recovery and exactly-once Builder append; the editor-created four-reference collection also exports/restores through ZIP8 into an independent empty database.
- [43136482999](https://github.com/mrayover/practice.logger/actions/runs/43136482999), reviewed product source317868f: both APIs pass6 shared preservation checks,5 native staging/ZIP checks, interrupted promotion recovery and2 established editor/Builder workflows. Each job has one collection-UI interaction failure. Captured evidence led to test keyboard-transition synchronization rather than product layout changes.
- [43157782919](https://github.com/mrayover/practice.logger/actions/runs/43157782919), source a316d60: **all3 collection UI methods pass onAPI26/job130981640153**. API35 passes compact320dp/200% text in Light/Dark/Orchid and Prepare Week/default-practice; the extended saved-PDF assertion runs before asynchronous page loading finishes. The final test waits for the exact tagged Page2of2 result while retaining visibility/text assertions.
- [43158444988](https://github.com/mrayover/practice.logger/actions/runs/43158444988), final source49aae98: **that exact UI method passes onAPI26/job130983766482 andAPI35/job130983766259**. All **24 distinct scoped native checks** therefore have passing evidence on EACH API across these recorded runs. Earlier failures remain part of the record; this is not a claim that every grouped attempt passed.

Coverage includes multiple mixed visuals/audio, independent order/defaults, duplicates, pending/saved exact removal and confirmation, legacy coexistence, retained overlays/audio markers/master relationships, stale lists and late local-work rejection, all-item first Save/rollback/retry, scalar upgrade, recreation, missing pending originals, cancellation/shared-original protection, interrupted promotion and receipt replay. Native UI covers Library and pending selections, preview/background release, saved multipage PDF, save/reopen, Builder preservation, selected prior-week creation and deterministic first visual/audio in practice. Light/Dark/Orchid320dp/200% screenshots were visually inspected: titles ellipsize, actions wrap and Save stays pinned. Phone/provider/TalkBack/acoustic acceptance is not inferred.

## Limits, owner baseline and exact next assignment

Development remains **Room24, ZIP8/readers1–8, JSON7/readers1–7**. Schemas/migration, backup implementation, application version and practice/recording engines are unchanged by3C.1. **Signed0.37.0/build110 remains the known working Room23/ZIP7 owner baseline. No signed APK, owner distribution or version increment was produced.** A build110 downgrade cannot open committed Room24.

Audio preview is a small audition dialog; waveform/speed/marker editing remain in existing workspaces. Android saved navigation/form state restores fields while the manifest restores media/order; force-stop without task restoration abandons a temporary draft to the established safe expiration policy. Physical collection checks remain in [MANUAL_TESTS](MANUAL_TESTS.md).

**Exact next assignment: Stage3C.2 practice-screen reference selection/switching**, only after explicit owner assignment. Implement per-source visual/page/zoom state, awaited outgoing audio checkpoint and paused incoming state, stale callback/capture guards and Record Reference append-and-select. None is implemented here. Practice still opens the first ordered reference deterministically. **Stage3D** remains final integration, owner install-over/device acceptance and signed delivery. Stop after3C.1.

---

# Stage 3B — verified persistence baseline (historical)

Updated November11,2026 (America/Los_Angeles).

## Stage 3B multiple-reference persistence

Development source implements **Room24**, explicit23→24, ordered multi-association repositories and **complete ZIP8/readers1–8**. **JSON7/readers1–7 is unchanged and data-only.** Signed **0.37.0/build110 remains Room23/ZIP7**; Stage3B delivers no new signed owner package. Actual build110 APK/ZIP hashes were rechecked and match the delivery record below.

- Migration preflights foreign keys, roles, overlay membership and unexpected triggers; snapshots parent/audio children; drops children before their CASCADE parent; rebuilds final tables/indexes; restores every old column; reinstalls constraints and checks integrity. No application file operations, resets or destructive fallback. Coexisting legacy/canonical visuals become positions0/1.
- Stable (blockId,role,mediaId) identity, independent dense role order and a canonical/legacy visual union support idempotent append, exact removal/membership and reviewed permutations. Reorder updates positions without deleting links. Audio/overlay checks use exact source membership; shared originals and archived links remain protected.
- Scalar editor/confirmation adapters recheck reviewed orders and affect only the reviewed source; null never means remove all. Existing staging, receipts, cancellation and retry remain scalar. Record Reference retains confirmed scalar replacement; appendRecordedAudio is ready for Stage3C's append-and-select flow. **Collection interface/staging/switching is not implemented.**
- ZIP8 saves explicit positions, validates dense union/audio order, preserves originals once per owner and restores links before dependent state. Historical1–7 rules are validated before normalization. Immutable fixtures/provenance/hashes are in androidTest/assets/stage3b. Restore preserves overlay numeric values without Float rounding; rendering/geometry algorithms are unchanged.

### Verified gates

Application source **98c20f866586514e88f76a0fcc2963ced679a7e7**: [43128696524](https://github.com/mrayover/practice.logger/actions/runs/43128696524) passes on **Android8/API26/job130887195252** and **Android15/API35/job130887195980**. Each build passes **57 focused units**, debug/release/test assembly and release vital lint. Each device passes **32 native checks**, plus two expected process crashes for migration/restore recovery.

Coverage: all27 populated Room23 tables/old columns and original bytes after migration/cold reopen; empty schemas1–23 through24; ten injected rollback boundaries; production WAL; actual kill during parent rebuild with intact23 recovery before successful24 upgrade; incorrect roles/relationships/triggers; missing/corrupt originals retained; mixed ordered references, stale changes, duplicates, exact confirmed removal, independent audio/markers/overlays, shared originals and scalar editor/recorded-reference behavior. ZIP checks include frozen1–7, malformed historical/new records, physical file comparisons, empty-target guards, preview and failed/interrupted restore. The real picker workflow exports8 originals, restores into a separate disposable installation, cold reopens, retains overlay edits after install-over and recovers a killed restore commit.

Regression: [43127710836](https://github.com/mrayover/practice.logger/actions/runs/43127710836) passed API26 editor2/builder3/week/Goal/Routine before an obsolete overlay selector. [43128700401](https://github.com/mrayover/practice.logger/actions/runs/43128700401) passed builder3/week/Goal/Routine/protected deletion/initial practice on BOTH APIs; missing WAV and wrong reopen filters were corrected. [43129177920](https://github.com/mrayover/practice.logger/actions/runs/43129177920) passes isolated editor and updated full overlay workflows on BOTH APIs. The unchanged baseline API35 editor method also passes in [43128635405](https://github.com/mrayover/practice.logger/actions/runs/43128635405); the grouped clipping cause remains unestablished. Test navigation/synchronization was corrected without product UI changes.

[43130057741](https://github.com/mrayover/practice.logger/actions/runs/43130057741) passes Transcribe transport, concurrent playback/capture ownership, ordinary recording/export/reopen, deletion/reopen and all practice/recovery/history phases on BOTH APIs. One interruption assertion ran before asynchronous reference restoration was ready (75% expected, initial100% observed). Its exact method now waits for media identity/preparation readiness and also checks persisted75%; [43131094962](https://github.com/mrayover/practice.logger/actions/runs/43131094962) passes on API26/job130895136655 and API35/job130895137100. All19 relevant cross-feature native checks have passing results on EACH API across these recorded runs. **Stage3B acceptance is complete.**

### Limits and exact next task

Full Windows host run:173 tests/19 failures before projection-expectation correction. Untouched ba267be reproduced17 matching media-promotion/provider/journal failures and two old schema18 assertions. Relevant migration tests now assert24 and compare every frozen old column. One newly exposed AudioImport journal-open failure occurs before schema creation while its exact baseline method passes; cause unestablished. No full-host-suite pass is claimed. Full lintDebug has the five documented error locations in unchanged files (30 warnings); release vital lint passes. Physical phone/provider/TalkBack/acoustic and signed owner install-over acceptance remain Stage3D work.

The subsequent assignment split3C: editor collections/staging are complete in3C.1 above; practice selection/checkpoints/append-and-select remain3C.2. Stage3D retains final integration, owner install-over and signed delivery. Build110 cannot open committed Room24; APK downgrade is not rollback.

Historical stage records below retain their original signed/preparation baselines; the current status and next assignment above supersede their former next-task statements.

# Stage 3A architecture and verification preparation (preceding baseline)

Updated November 10, 2026 (America/Los_Angeles).

## Stage 3A multiple-reference preparation

Stage3A completes the [multiple-reference architecture](docs/MULTI_REFERENCE_ARCHITECTURE.md) and populated frozen Room23 fixture. **Multiple references are not implemented.** Production entities, migrations/registration, attachment behavior, archive writer/reader, version and signed delivery remain unchanged: **0.37.0/build110**, Room23, ZIP7/readers1–7, JSON7/readers1–7. Stage2 delivery and physical acceptance limits remain below.

The proposed3B contract uses `(blockId,role,mediaId)` associations with independent ordering, an ordered legacy/canonical visual union, unchanged source-local annotation identities, safe parent/child rebuild and versioned ZIP8 compatibility. The fixture populates all27 durable tables and records original-file hashes. All13 focused checks pass (11 existing plus two fixture checks). These exercise current-schema preservation and constraints, not a future migration. The new fixture uses test-only TRUNCATE journaling after Windows native WAL transitions failed; production journal configuration is untouched. [Actual results and limitations](WORK_LOG.md#2026-11-10--stage-3a-multiple-reference-architecture-and-fixtures)

**Exact next assignment: Stage3B — implement the planned Room23→24 persistence migration, ordered multi-association repositories and legacy compatibility, preserve existing local media state, add ZIP8 with frozen v1–v7 restore compatibility, and execute populated migration/rollback/backup tests.** Keep existing editor callers safe; collection controls/staging belong to3C and integration/install-over/device delivery to3D. This session stops after3A; no APK or version increment is produced.

## Stage 2 compact Block editor delivery and verification

**Signed Stage2 delivery is0.37.0/build110**, application source `70778db74deb8202ead9ca1dc3c36113f843903c` on `block-ux-stage1`. Canonical checkout: `C:/Users/mrove/Projects/practice.logger`. The older OneDrive source checkout is unchanged. APK and ZIP are in **OneDrive → Documents → ChatGPT → Practice Log development → Block UX delivery → Stage 2 build110**, named `practice.logger-0.37.0-b110-70778db`. Install over permanent-key build108 without uninstalling. Build109 is a preserved intermediate candidate; builds107/108 remain unchanged.

- Package `com.mrayover.practicelogger.app`; permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- APK SHA256 `a7afd0db21f3530362b34590aee7ef42a9510cc9c123699c82e5b638ab98880d`. ZIP SHA256 `b1e76505303a9ba9130acb0aaf737e7f0d07daddbb3a91038454dc9a8aa3decf`.
- Publisher verified package/version, signature and pinned certificate. All148 unsigned APK payload entries and all five ZIP members match; OneDrive copies match their originals. Build107/108 APK hashes still match the historical records. Final release assembly and vital lint passed at70778db (`.tools/editor-b110-release.log`). BUILD_INFO preserves the verification status at signing time.

The old editor placed large media-management controls before the fields and required saving/reopening a new Block before choosing references. The new form puts Name, Action, adaptive Duration/BPM and primary Goal first. Compact filename/type references and expandable Notes follow. Save Block/Cancel stay pinned above keyboard/system insets. More contains Archive/Restore, protected Delete and Recordings; expanded Library details also expose Recordings. Existing weekly actions remain on Block details outside the form.

New and existing reference selections wait for Save. Existing Library IDs are selected without attaching; new imports/camera/gallery originals are validated in private staging. Cancel preserves the saved Block and shared originals. Block fields, Goal, correct selected week, media links and new builder placement commit together; failed saves retain recoverable selections. Synced manifests and completion receipts handle recreation/interruption. Saved canonical and legacy previews retain the existing annotated viewer; unchanged references retain local audio/marking state. [Staging and recovery contract](docs/BLOCK_EDITOR_STAGING.md).

**The limit remains one image OR PDF and one audio.** Room23, complete ZIP7 and JSON7/readers1–7 are unchanged; no Stage3 migration or multiple-reference work was implemented.

### Focused Stage2 checks

- All21 focused units passed across Block/editor, session, audio, Goal, week and JSON backup persistence (`.tools/editor-final-checks.log`, `.tools/editor-b110-checks.log`). Later promotion-recovery changes also passed the rollback unit plus updated native tests. Release and Android-test compilation and UTF-8 gate passed (`.tools/editor-b110-final.log`); final source70778db changes only the staged-only Archive warning after those native runs.
- [API26 complete editor/storage run](https://github.com/mrayover/practice.logger/actions/runs/42919325875), source0f795cc:4 storage+2 UI tests passed. Covers first-save Library selection, validation, themes, long fields, recreation, cancellation, and builder save/cancel/one append with preserved placements.
- [API35 320dp/200%/gesture editor](https://github.com/mrayover/practice.logger/actions/runs/42920156997), sourcefd27e61: complete Library-reference/theme/recreation/cancellation workflow passed with real touches and geometry capture. Light/Dark/Orchid and keyboard screenshots inspected. The inherited banner wraps at enlarged text, reducing the scroll viewport; Save and reference controls remained reachable. Earlier narrow visibility failures and the capture/idle boundary are documented without claiming an independently proven cause.
- [API35 ZIP/install-over and editor storage](https://github.com/mrayover/practice.logger/actions/runs/42920160542), sourcefd27e61:5 portable phases passed (export, separate-empty-install restore, cold restart, overlay edit retained after install-over, interrupted-restore recovery), with the expected deliberate restore-process crash. All4 updated editor storage tests pass: both/individual media roles, staged photo/PDF, Goal/week/builder, failed commit rollback/retry, uncommitted-promotion recovery, manifest recreation, expiry, shared originals, saved overlays/local audio markers and unchanged placement snapshots.
- [Weekly/deletion/builder group](https://github.com/mrayover/practice.logger/actions/runs/42920164204), sourcefd27e61: selected-week creation, Ready/Prep integration and the complete protected deletion workflow pass. Its existing builder filter method timed out before Create/editor entry in that grouped run; the [isolated exact method](https://github.com/mrayover/practice.logger/actions/runs/42920936120), same sourcefd27e61, passed including Create/save/return and confirmed discard. The dedicated new builder editor workflow is green on API26 and on API35/200% in42919322147.

The initial13-test local baseline had five reproduced failures before edits: two outdated schema18 assertions and three Windows file-promotion failures. Full-lint baseline errors remain as documented under Stage1.5; no unrelated repair or full-regression success is claimed. Physical build108→110 phone install-over, TalkBack, camera/gallery/system-provider behavior, acoustic checks and owner ZIP acceptance remain unverified. Emulator install-over verifies a debug package over restored data; permanent-key continuity is independently verified by signing. [Physical checklist](MANUAL_TESTS.md#stage-2-compact-block-editor-phone-checks).

**Stage2 implementation, signed delivery and scoped automated verification are complete; physical acceptance remains pending.** Stage3A preparation is complete as recorded above. Stage3B is the next implementation milestone; multiple references remain unimplemented.

## Stage 1.5 Session Builder delivery and verification

**Signed delivery is 0.36.3/build108**, application source `bd80507ad834bb28a1648f385d24fbc8002cbf8f` on `block-ux-stage1`. Work used the canonical checkout `C:/Users/mrove/Projects/practice.logger`. The separate older OneDrive checkout was not synchronized. The APK and ZIP are in **OneDrive → Documents → ChatGPT → Practice Log development → Block UX delivery → Stage 1.5 build108**, named `practice.logger-0.36.3-b108-bd80507`. Install over permanent-key build107.

- Package `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- APK SHA-256 `a6be26668eaefeb5aedf1125bf015a3f0f6743495834ad132d27a80b2c90e190`. ZIP SHA-256 `50e2598327a08519659377de0ca41a6dde5d50a1e96a22f0452ebbaeb11d5052`. Verified package/version, signature, all 148 APK payload entries, all five ZIP members and matching OneDrive copies. Earlier packages are retained.
- BUILD_INFO records native checks as pending at signing time. Later commits `89dd83b` and `e7805ad` correct tests only. Application source matches the signed package.

The catalog-sized form is replaced by a compact Build Session heading, placement count and total configured duration, then the selected lineup. Pills show saved position/title/Action, configured duration and snapshot BPM. Expand for saved details, independent duration editing, Up, Down and Remove from Session. Stable placement IDs preserve repeated Blocks and expansion state through reorder. The current primary Goal supplies the left square.

**+ Add Blocks** opens a dedicated active Block picker with the existing text matcher and Goal filter, explicit Add and confirmed added counts. Done and Android Back preserve lineup order and overrides. Create returns through the existing editor and appends the saved Block. Start Session stays pinned, handles keyboard and system insets, and is disabled for an empty draft. Routine save and confirmed discard remain secondary. Back and discard use the recorded origin. Existing single-draft persistence and Ready/Start/Prep operations are reused. Room23, complete ZIPv7 and JSONv7 remain in use.

### Focused Stage 1.5 checks

- Baseline debug assembly and 14 unit tests passed. Final release assembly, source encoding, Android test assembly and all 25 focused unit tests passed across sessions, Routines, Routine backup, Goal matching and weekly order/completion. Release `lintVitalRelease` passed. Full `lintRelease` failed with five errors and 39 warnings. An isolated archive of original head `2725d24` reproduced exactly the five errors in AudioManager constants, Activity casts and a StateFlow composition read.
- Android15 passed all three builder workflows at standard size, including empty state, repeats, totals, duration validation, reorder/removal, expansion, snapshots, Routine save, recovery, filtering, Create/discard, Library origin and Ready/Prep/Start. Marker pixels and text ellipsis were checked in Light and Dark. [Standard builder run](https://github.com/mrayover/practice.logger/actions/runs/42912651766)
- Android8 passed the complete builder-through-Ready/Prep workflow and shared Goal filtering. [Builder lifecycle](https://github.com/mrayover/practice.logger/actions/runs/42911531652) · [Goal filtering](https://github.com/mrayover/practice.logger/actions/runs/42912659861)
- Android15 passed weekly ordered entry and existing Routine management. Their grouped run later failed its filter fixture and did not reach practice. Goal filtering and practice completion/skip/overtime/background recovery passed isolated reruns. [Weekly and Routine results](https://github.com/mrayover/practice.logger/actions/runs/42911535260) · [Practice regression](https://github.com/mrayover/practice.logger/actions/runs/42912663865)
- At 320dp/200% text, search/filter/Create/discard passed, followed by both remaining builder methods after correcting an offscreen-header assertion on a restored lazy list. All three workflows pass across these narrow runs. The scoped native checks total 11 successful test executions. [Search and editor-return result](https://github.com/mrayover/practice.logger/actions/runs/42912655611) · [Final narrow lifecycle and theme checks](https://github.com/mrayover/practice.logger/actions/runs/42913973296)

Initial native failures and test-only fixture/visibility corrections are retained in WORK_LOG. Screenshots were inspected. The existing shared practice.logger banner wraps at 320dp/200%, reducing content height. Start and picker Done remain pinned. Physical Galaxy S26 Ultra and smaller-phone install-over, TalkBack and provider/backup acceptance remain pending. The full regression matrix was skipped for this bounded stage. [Phone checklist](MANUAL_TESTS.md#stage-15-session-builder-phone-checks)

**Stage1.5 implementation and scoped verification are complete. Its Stage2 successor is now delivered above.** The Stage1.5 handoff assigned the compact Block editor and pre-save references. Put Name, Action, Duration/BPM and Goal first, collapse References and Notes, move oversized media management out of the main form, and stage one visual and one audio before initial Save with safe cancellation, retry and cleanup. Stage3 remains Multiple Visual and Audio References with migration, switching, annotation preservation and backup compatibility.

## Stage 1 source and verification

**Signed Stage 1 delivery is 0.36.2/build107**, app source `e3e884c3896619d3cb3e6002caa12fed897e3f15` on `block-ux-stage1`. The APK and ZIP are in **OneDrive → Documents → ChatGPT → Practice Log development → Block UX delivery → Stage 1 build107**, named `practice.logger-0.36.2-b107-e3e884c`. Install the APK over permanent-key build106.

- Package `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- APK SHA-256 `fb3e51cfa18c60cd0776443152e0a8bc5160e75130fc56713a2771b317319c5b`. ZIP SHA-256 `f690f7873405ffefb40cf61af9ca9e7ed121cfc45a69f936359a4c0c8ffe546f`. Verified all 148 APK payload entries, all five ZIP members and matching OneDrive copies.
- Release assembly and release lint passed on build107. The focused unit and Android results below are from the equivalent Stage 1 implementation at `f20a06e837e6c7074beae01aec14ffe159cf70d0`, before the version bump.

The Block Library, This Week and Prepare Week share a compact, expandable Block pill. The collapsed header shows a two-line title, the first Action line, duration and optional reference BPM. Expand for the full saved text, primary Goal, attached reference summary and actions for that screen. Library actions include Edit, weekly membership, Add to Practice through the session draft, and confirmed Restore in Archived. Weekly actions include Edit, adding or removing membership, and Practice Block through Ready/Prep. Weekly multi-selection keeps its own checkbox and preserves plan order in the builder.

An active primary Goal supplies the left square for reusable or unfinished Blocks. A timestamped completion in the displayed Sunday–Saturday week supplies the right slit in its recorded color. When a Block has several completions that week, the pill uses the latest. The Library uses the current reusable state. Session Summary retains the placement title and shows the recorded slit when a color snapshot exists. Legacy completions keep their outcome and saved placement text in history. Without a per-placement completion timestamp or color snapshot, the weekly UI cannot reliably date or color those older completions. No Goal retains its neutral treatment.

Stage 1 uses the existing Room 23 schema, complete ZIP v7 and legacy JSON v7. See the [Stage 1 phone checks](MANUAL_TESTS.md#stage-1-block-pill-phone-checks).

Local verification passed for source encoding, debug and release APKs, the release lint gate and the Android test APK. All 11 focused unit tests passed across weekly planning and completion, Goal filtering, reflection and backup compatibility.

- Android 15 passed both pill workflows at standard text and width and at 320 dp with 200% text. Coverage includes ellipsis, expanded details, Edit, Add to Practice, Restore, marker geometry and color, and week isolation. [Standard](https://github.com/mrayover/practice.logger/actions/runs/42906397369) · [320 dp and 200% text](https://github.com/mrayover/practice.logger/actions/runs/42906401543)
- Android 15 passed weekly preparation, practice and ordered builder placement, plus Library search, archive, restore and draft recovery. Android 8 passed shared Goal filtering. [Weekly workflow](https://github.com/mrayover/practice.logger/actions/runs/42906405240) · [Library smoke test](https://github.com/mrayover/practice.logger/actions/runs/42906413419) · [Goal filtering](https://github.com/mrayover/practice.logger/actions/runs/42906409047)
- Android 15 passed Goal assignment, reordering, completion, reopening and marker colors across the available themes. [Goal priority workflow](https://github.com/mrayover/practice.logger/actions/runs/42907223878)

Physical checks on the Galaxy S26 Ultra and a smaller Android phone, including TalkBack, remain pending.

**Next implementation assignment** is Stage 2. Put Name, Action, duration/BPM and primary Goal first in the Block editor. Add a compact References section that stages one visual and one audio attachment before initial Save, keeps staged selections retryable after failure, and cleans them up on Cancel. Preserve existing recording and archive access and assess remaining Block lists for reuse.

## Signed I2 delivery

**0.36.1/build106**, app source `146596d04884982d410b10474bac24d8585e59f5`. The APK and ZIP are in **OneDrive → Documents → ChatGPT → Practice Log development → Inspirations delivery → I2 build106**, named `practice.logger-0.36.1-b106-146596d`. Install over permanent-key build105.

- Package `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- APK SHA-256 `a97cd0bd1e9cf89eae14b2365bb3ad77e9778b1a603864d1251f07e3e20f5b2b`. ZIP SHA-256 `9314d56eacbf19a44d2f4609a79bcd1169147aeb2cb62733ed25bbfef19e1c01`. Verified 148 APK payload entries, all five ZIP members and matching OneDrive copies.
- Library → Inspirations offers five offline, read-only works. I2 adds Taylor's Orphic Hymn to the Muses, Dryden's 1687 song and Brady's 1692 ode using the existing bundled TXT reader. The two build105 assets match byte for byte. Room 23 and complete ZIP v7 remain in use. [Text editions and sources](docs/INSPIRATIONS.md)
- Local release assembly, source encoding, release lint gate and instrumentation compilation passed.
- Android 15 passed the focused Included Inspirations test. It opened all five works, compared the complete reader text with each bundled asset, switched between Media and Inspirations, recreated the Activity, then reopened and compared all five again. No full regression was run. [Focused Android run](https://github.com/mrayover/practice.logger/actions/runs/42548771199)

Physical install-over and the [I2 included Inspirations phone checks](MANUAL_TESTS.md#i2-included-inspirations-phone-checks) remain pending.

## Signed build105 Inspirations and Playground delivery

**0.36.0/build105**, app source `0b6af9c5b60a527a30c018f9cf8e2a7eb4a62ba5`. The APK and ZIP are in **OneDrive → Documents → ChatGPT → Practice Log development → Inspirations delivery**, named `practice.logger-0.36.0-b105-0b6af9c`. Install over permanent-key build104.

- Package `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- APK SHA-256 `43929ed8e188ae9397a79a794a1e5e9678e0c34fc70364689c15de0c3815cf3c`. ZIP SHA-256 `86f4147800b2aabc6d440603b283d939e4dea509ec1ddff1ea0d728c6d923674`. Verified 145 APK payload entries, all five ZIP members and the OneDrive copies.

Library offers Media and Inspirations. Personal entries can be written or pasted, captured with the installed camera, or imported from PDF, JPEG, PNG, WebP, UTF-8 TXT or MD. Read, edit and delete entries and deliberately remove visual attachments. Shared Library originals are protected while linked. Two verified 1914 Evelyn-White selections are packaged read-only. Room 22→23 adds the personal collection. Complete ZIP v7 preserves it and reads v1–v6. Journal and Playground are under Log. Playground shows Coming Soon. See [Inspirations](docs/INSPIRATIONS.md) and [Playground specification](docs/PLAYGROUND_SPEC.md).

- Release build, source encoding, release lint gate and instrumentation compilation passed. All nine selected local tests passed across InspirationMigrationTest, BackupRepositoryTest and WeekPlanTest. The migration fixture reconstructs the published build104 schema and checks Block, Journal and weekly data plus reopening a new Inspiration. Initial test failures involved missing index arrays in the migration fixture, a Windows SQLite open failure resolved with a shorter fixture filename, and a legacy test archive that still included the new table. The corrected runs passed.
- Android 15 at 320 dp and 200% text passed both workflows on test commit `afc1b33`. Coverage includes Journal text across Playground, returning to Media, bundled reading, long-text creation/edit/reopen/delete and draft protection, unsupported file rejection, TXT/MD editing, PDF paging, image rendering, EXIF orientation, simulated camera cancellation and capture, and deliberate attachment removal. The first narrow run passed imports and timed out looking for bundled text before scrolling. The final test explicitly scrolls to the text. [Passing run](https://github.com/mrayover/practice.logger/actions/runs/42542531232) · [Initial run](https://github.com/mrayover/practice.logger/actions/runs/42541989021)
- Android 15 passed all three archive checks on `afc1b33`, including v7 recovery of metadata, long text and original images, shared-original deletion protection, empty-target restore, ZIP v1–v7 and JSON v1–v7 compatibility, malformed archives and provider-write failure cases. [Archive run](https://github.com/mrayover/practice.logger/actions/runs/42542531310)
- External Files and Camera UI, camera permission denial, a real capture, and physical install-over remain phone checks. Native tests inject file and camera results. Saved data was verified through database reopen and Activity recreation.

[Build105 phone checks](MANUAL_TESTS.md#inspirations-and-playground-phone-checks) also cover the original personal collection and provider flows.

## Signed P2 delivery

**0.35.0/build104**, app source `e12b6aed9811a03fbbb465ad73c3fdea68d1dc03`. The APK and ZIP are in **OneDrive → Documents → ChatGPT → Practice Log development → P2 delivery** under `practice.logger-0.35.0-b104-e12b6ae`. Install the APK over permanent-key build103.

- Package `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- APK SHA-256 `c2ff0b90e8bb18419bb890b3c3e2207676a5193b920b8e65341cb3fed48eca17`. ZIP SHA-256 `2f21eac8ae00844dd28e1d66dec7f82f83f410e6bdf2ecf4918c1402769ed85c`. Verified 142 APK payload entries, all four ZIP members and the OneDrive copies.

### P2 behavior and verification

Complete saves the outcome and a placement-keyed snapshot of the Block's primary Goal, including its active priority and color when applicable. The optional 1–5 security rating and four additional prompts advance to Up Next or the final session flow. Skipping practice bypasses Block reflection. The last placement commits the session before optional session prompts and the existing session note. Session Summary displays saved reflections. Reopening from a fresh app launch retains committed practice and saved answers. Unsubmitted reflection is optional and can be left behind.

Room 22 adds separate completion and reflection tables. Complete ZIP v6 preserves them and reads v1–v5. Legacy JSON v1–v7 remains readable. Earlier histories retain their original records without invented completion data. G1 first-positive-time attribution remains independent.

- Release and Android instrumentation test compilation passed. All 28 focused local tests passed, including Room migration, committed outcomes, rating bounds, repeat placement identity, Goal changes, persistence after reopen, unchanged time attribution, v6 ZIP round trip, legacy JSON and prior v3 ZIP records, practice notes, clock and metronome persistence.
- Android 8/API 26 passed Prep tools, recording-review protection, note-draft safeguards and the two optional reflection skips. [API 26 check](https://github.com/mrayover/practice.logger/actions/runs/42530860130)
- Android 15/API 35 at 320 dp and 200% text passed both P2 review workflows, including the committed-outcome check before reflection, rating persistence through recreation, Up Next, skipped practice, session answers and summary. The Prep note case at this size timed out waiting for Complete to become visible after dismissing the keyboard. The job stopped before the separate prepared-Block cold-reopen case. The two reflection screenshots were reviewed. Some actions require scrolling at this text size. [Initial run](https://github.com/mrayover/practice.logger/actions/runs/42530860533) · [Keyboard dismissal](https://github.com/mrayover/practice.logger/actions/runs/42531330644) · [Visibility follow-up](https://github.com/mrayover/practice.logger/actions/runs/42531735898)
- Android 15 passed both portable archive cases, including verified restore of ZIP v1–v6 and rejection of malformed completion/ratings. ZIP v6 retained the completion facts and reflection records. The same run passed the three-outcome practice and overtime workflow. [Archive and practice checks](https://github.com/mrayover/practice.logger/actions/runs/42531330846)
- Android 15 at standard text passed the three-stage practice workflow, paused recovery and saved summary/history after a process restart. [Practice and cold-reopen checks](https://github.com/mrayover/practice.logger/actions/runs/42531735972)

The [P2 reflection phone checks](MANUAL_TESTS.md#p2-reflection-phone-checks) remain available for the retained release.

## Signed metronome scaling fix

**0.34.3/build103**, app source `a22a52a34b445996031002685e5acfbea7e68b96`. Install over build101 or build102. The APK and ZIP are in **OneDrive → Documents → ChatGPT → Practice Log development → P1 delivery → Compact metronome → Build 103**, named `practice.logger-0.34.3-b103-a22a52a`.

The owner's S26 Ultra showed +1 alone on a second line in build101. The intermediate build102 reclaimed 28dp from gutters and the scrollbar, but still wrapped at 130% on Android 15. Build103 measures a more compact Start/Stop click button when that lets all six controls fit, retaining the 48dp touch targets and wrapping at narrower layouts. Local release and instrumentation compilation, permanent-key signature, ZIP integrity and OneDrive copy hashes verified. [Build101 reproduction](https://github.com/mrayover/practice.logger/actions/runs/42526714122) · [Intermediate build102 check](https://github.com/mrayover/practice.logger/actions/runs/42526719175)

- APK SHA-256 `f566125c52feb074407bed7dcc757a495ad5187e28a5768d803d88952fa5a3e7`.
- ZIP SHA-256 `eab97e5fb393f2cd94ed25a1044a25cab14f1ab0950807ebd245a45d8692f169`.
- Android 15 at 130% text passed with all six controls on one row in order, without overlap and with 48dp touch targets. Physical S26 Ultra confirmation is pending. [Build103 check](https://github.com/mrayover/practice.logger/actions/runs/42527201294)

## Retained compact metronome update

**0.34.1/build101**, app source `b7b1417a328ce22851f28d18efda0efd8a298a47`. Install over build100. The APK and ZIP are in **OneDrive → Documents → ChatGPT → Practice Log development → P1 delivery → Compact metronome** under the name `practice.logger-0.34.1-b101-b7b1417`.

- Focused Practice lays out Metronome/BPM, Accent, expand, Start/Stop click, −1 and +1 on the same row when there is room. The controls wrap at narrow widths or enlarged text and retain 48dp touch targets.
- Local Gradle release and instrumentation compilation passed. The permanent certificate matches build100. The delivery copies and all four ZIP members passed hash verification.
- APK SHA-256 `360b0a9a9833a8f2c0f70e43b927a710fafe9b0afac8f740d7fe3cb58b1a6fad`. ZIP SHA-256 `6bf9661e331e59ae308ca92cdf9c1c4100500f5917792b298130bdc9492436f2`.
- Android 15 passed both focused layout checks. At about 411dp and standard text, all six controls shared one row in the requested order with no overlap. At 320dp and 200% text, controls wrapped and stayed reachable, and tempo, click and expanded settings worked. Both screenshots were reviewed. S26 Ultra verification is pending. [Standard layout](https://github.com/mrayover/practice.logger/actions/runs/42525561538) · [Narrow and enlarged layout](https://github.com/mrayover/practice.logger/actions/runs/42525563517)

## Retained signed P1 delivery

**0.34.0/build100**, app source `49178a24e60a47c0a6ded64454ed3131efa6af75`. The signed APK and ZIP are in **OneDrive → Documents → ChatGPT → Practice Log development → P1 delivery**. Install `practice.logger-0.34.0-b100-49178a2.apk` over the permanent-key build99.

- Package `com.mrayover.practicelogger.app`, permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- APK SHA-256 `9ad3083f68f8bea970a9d8db2f6978f2545a06b426e95d3f6d7eedd7c33fe934`. ZIP SHA-256 `83c35cec375726f2b6c17c40c2132991ddedd8097ea78e57dceac9f332844a7b`. All 142 APK payload entries matched the unsigned build. All four ZIP members and the OneDrive copies matched.
- Local Gradle release and Android test compilation passed. All 17 selected tests in SessionRepositoryTest, GoalAttributionTest, WeekPlanTest and BackupRepositoryTest passed. The checks include migration, separate prep time, the ZIP round trip, older records, and first-accrual Goal attribution.

## P1 behavior and native verification

Version **0.34.0/build100** adds Ready, Prep and Up Next to the shared Block practice workflow. Room 21 stores foreground preparation checkpoints separately and complete ZIP v5 preserves them. Previous ZIP v1–v4 and JSON v1–v7 remain readable. [Phone checks](MANUAL_TESTS.md#p1-intentional-practice-lifecycle-phone-checks)

- Android 15/API 35 at 200% text and 720 × 1440 at 360 dpi passed Ready, Up Next, Start/Pause/Resume, Prep background and Activity recreation, Skip and force-stop/reopen. Ready, Up Next and Prep screenshots were reviewed. [Narrow Android checks](https://github.com/mrayover/practice.logger/actions/runs/42522300462)
- Android 15 passed the three-outcome builder flow, countdown, overtime, paused recovery, saved notes and history reopen. [Practice and recovery checks](https://github.com/mrayover/practice.logger/actions/runs/42522302128)
- Android 8/API 26 passed Prep reference playback and capture, recording/exit protection, note draft protection, and Prep to Start. The first run reached playback and failed on an incorrect Close button selector, which was corrected in test source `0d79f41`. [Corrected Prep tools check](https://github.com/mrayover/practice.logger/actions/runs/42522616688)
- Android 15 passed This Week single and multi-Block Ready entry and weekly process reopen. The same run passed the saved Routine launch and Block transitions, then stopped when the recovery test tried to open Notes from Ready. After the test chose Prep, the Routine workflow and cold reopen both passed. [This Week and first Routine run](https://github.com/mrayover/practice.logger/actions/runs/42522304073) · [Corrected Routine checks](https://github.com/mrayover/practice.logger/actions/runs/42522765613)
- Android 15 passed complete ZIP v1–v5 restore, JSON v1–v7 compatibility and malformed archive checks. ZIP v5 preserved prep time through restore. Earlier ZIP and JSON versions restored with zero prep. [Archive checks](https://github.com/mrayover/practice.logger/actions/runs/42522305848)

- Android 15 passed Goal-launched Ready and Prep, independent sessions, resume, and preservation of first-accrual attribution after reassignment. [Goal workflow check](https://github.com/mrayover/practice.logger/actions/runs/42522767506)

Verification was scoped to P1. Native test follow-ups through `ab06778` leave the signed app source unchanged. Physical-phone install-over and acceptance remain pending.

The retained build103 follow-up is the [compact metronome phone check](MANUAL_TESTS.md#compact-metronome-phone-check).

## Retained signed G2 delivery

**0.33.0/build99**, app source `e4b805961c9fb4fb8efd1e61535498b5be87fc1f`. Install over permanent-key build98 without uninstalling. The verified APK and ZIP are in the owner's **OneDrive → Documents → ChatGPT → Practice Log development → G2 delivery** folder. Open `practice.logger-0.33.0-b99-e4b8059.apk` to install. The ZIP has the same name with a `.zip` extension and includes the build manifest and installation notes. [Phone checks](MANUAL_TESTS.md#g2-shared-goal-filtering-phone-checks)

- Package `com.mrayover.practicelogger.app`. The signature matches build98 and pinned certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- APK SHA-256 `c9560d26dcc7048865b7c7db8bbda2d10f2464504f5e2ba540b9e60fe6aeb536`. ZIP SHA-256 `93bf699e117ca1d1f736ae4c019de5d5b0c2fb8fef26d394c5af04e287c50587`. All 142 APK payload entries matched the unsigned build. All four ZIP members and the OneDrive folder copies matched their source files.

### G2 behavior and verification

The same Goal filter is beside text search in Manage Blocks, Practice Now → Add Blocks, the saved Routine editor, and This Week → Prepare Week → Add existing Blocks. All Goals is the default for each visit. Active Goals are listed in priority order. No Goal matches a null primary association. Completed Goals remain identifiable on their Blocks under All Goals. Reordering follows the Goal ID, and completing a selected Goal returns the picker to All Goals. Results respect the current active/archived view and Prepare Week's existing membership rules.

- Local Windows Gradle passed source encoding and debug, release and Android test APK compilation. All 10 focused unit tests passed across BlockGoalFilterTest, BlockRepositoryTest, GoalAttributionTest and WeekPlanTest. Coverage includes all five Goal ranks, legacy links versus primary IDs, completed Goals, text search, archive eligibility, changing Goal choices and weekly persistence.
- The API 35 W1 workflow and separate process reopen passed, covering add/create/remove, carry-forward, builder order and paused single-Block practice. [W1 Android run](https://github.com/mrayover/practice.logger/actions/runs/42519588530)
- The four-picker G2 workflow passed on API 26 at 100% text and on API 35 at 200% text with a 720 × 1440, 360 dpi display override. It verified Library archive switching, Goal reorder and completion, session draft order, saving a Routine after filtering, and adding an existing Block in Prepare Week. The native test follow-up `77399ce4ee2f3a7323133ecb20425214a635c500` changed only the test's menu selectors. [API 26 run](https://github.com/mrayover/practice.logger/actions/runs/42519694262) · [API 35 run](https://github.com/mrayover/practice.logger/actions/runs/42519696489)
- G2 used Room 20 and complete ZIP v4. Physical phone verification for build99 remains pending.

## Retained signed W1 candidate

**0.32.0/build98**, app source `6c93d474f5db653eeec0ecd8bad77093ed34ad95`. Install over permanent-key build97. [APK](dist/practice.logger-0.32.0-b98-6c93d47/practice.logger-0.32.0-b98-6c93d47.apk) · [ZIP](dist/practice.logger-0.32.0-b98-6c93d47.zip) · [Build info](dist/practice.logger-0.32.0-b98-6c93d47/BUILD_INFO.json) · [Phone checks](MANUAL_TESTS.md#w1-this-week-phone-checks)

The publisher verified package `com.mrayover.practicelogger.app`, version, permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`, and the distribution ZIP round trip. All 142 unsigned payload entries matched the signed APK. APK SHA-256 `e7dfe021793304cdde098ee29cebc99e0a035fee498fe920cfa1d60824fe4ac0`. ZIP SHA-256 `dbebee6cb72faee8dfa27e2ed4686c5698552654154a96d275099e1a545cd8c7`.

- Practice Now opens This Week or the In Progress session list. Prepare Week adds, creates, carries forward, and removes membership immediately. Plans are Sunday–Saturday with no automatic rollover. Journal Weekly View remains Monday–Sunday.
- Single-Block practice opens an independent paused session. Multi-selection appends to the existing draft in weekly order, retaining any placements already there. Archived Blocks stay visible in their assigned weeks and become eligible again when restored. Carry-forward skips archived Blocks. Permanent deletion requires removing all weekly memberships first.
- Room 19→20 adds only ordered Block membership. Schemas 1–19 are unchanged. Complete ZIP v4 preserves the new rows. ZIP v1–v3 and legacy JSON v1–v7 remain readable and restore without weekly plans. [Backup format](BACKUP_FORMAT.md#weekly-plans-in-zip-v4)

### W1 verification

- Local Windows Gradle compiled debug, release and instrumentation APKs and passed source encoding verification. The final targeted run passed all 12 unit tests across WeekPlanTest, BlockRepositoryTest, BackupRepositoryTest and GoalAttributionTest. These cover Room 19→20, Sunday boundaries, database reopen, ordered membership, archiving and deletion, session draft safety, and ZIP record validation. The fixture-only follow-up `faed19ab0f8d270bd03475a32cc2f2d6ad070b02` left the signed app source unchanged. Earlier unit attempts hit an unsupported Room cleanup call, a missing optional schema field in the fixture, and a Robolectric directory-sync error. Full restore was verified on Android.
- API 35 passed all four focused instrumentation tests on the signed candidate's source. WeekWorkflowTest exercised Library add, editor membership status, earlier-week preparation, Block creation, carry-forward, removal, multi-Block builder order, paused single-Block practice and In Progress. WeekReopenTest checked persistence after a separate process launch. PortableArchiveValidationTest verified complete ZIP v1–v4 restore, archived membership, invalid record rejection and destination error handling. [Android diagnostic run](https://github.com/mrayover/practice.logger/actions/runs/42516430245)
- Full regression was outside this increment. Physical phone install-over and owner acceptance are pending.

## Retained build97 Android compatibility candidate

**0.31.1/build97**, app source `16a496fd58f740388e02f9782a1c73772c52cbcf`. Install over permanent-key build96. [APK](dist/practice.logger-0.31.1-b97-16a496f/practice.logger-0.31.1-b97-16a496f.apk) · [ZIP](dist/practice.logger-0.31.1-b97-16a496f.zip) · [Build info](dist/practice.logger-0.31.1-b97-16a496f/BUILD_INFO.json) · [Phone checks](MANUAL_TESTS.md#android-display-compatibility-phone-checks)

Package `com.mrayover.practicelogger.app` and certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` match build96. APK SHA-256 `769224ec5269cab8149dd4815c0425bf1803ed5703e79ed12fcc94959333cf0f`. ZIP SHA-256 `67dd0e04109020b2c5714fca78deeddd3a2f19f1a25b20c1cb7bbbf178282c3a`. Verified all 142 unsigned APK payload entries, the distribution ZIP and all 454 earlier APK and ZIP hashes.

- Loop setup now puts compact labels above centered numeric inputs and fits the column count to the available width and font. The same form serves Focused Practice and the saved Block recorder. Narrow and short viewports can scroll to the record action. The fields retain full accessibility names.
- The metronome uses measured control widths, a Material chevron and separate touch targets. Start/Stop continues to use the existing audio-driven flash. Repaired minus, separators, an ellipsis and the BPM range throughout Practice.
- Short keyboard viewports give the active Practice panel room to scroll. Tools respect keyboard insets. Short Library audio viewports scroll the transport and marker controls. The compatibility changes retain Room 19, legacy JSON 7, portable ZIP v3, themes, and the existing session and audio owners.

## Android compatibility verification

The app still supports API 26 through target API 36. Local Windows Gradle compiled the debug, release and instrumentation APKs. The following command passed 15 focused tests with zero failures, errors or skips.

```powershell
.\gradlew.bat --offline :app:verifySourceEncoding :app:assembleDebug :app:assembleDebugAndroidTest :app:assembleRelease :app:testDebugUnitTest --tests '*Phrase*Test' --tests '*Metronome*Test' --tests '*RecordingRepositoryTest'
```

The UTF-8 check passed on app source and text resources. Separate probes verified that it accepts accented and Japanese text, musical punctuation and symbols, and rejects both common mojibake and invalid UTF-8.

GitHub diagnostics built the app and test APKs with `assembleDebug assembleDebugAndroidTest`, then ran selected classes with `adb shell am instrument -w -r -e class … com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner`. Tests used emulator fixtures and disposable installs.

| Configuration | Result |
| --- | --- |
| API 26, default phone and text | Build97 source passed compact and taller loop setup, capture and review, the transport workflow and both practice and Tools button-pulse tests. An earlier compatibility commit also passed loop cancellation/background, Free Recording, Tools and the narrow piano. [Build97](https://github.com/mrayover/practice.logger/actions/runs/42164265662) · [Cancellation](https://github.com/mrayover/practice.logger/actions/runs/42163619893) · [Tools and Free Recording](https://github.com/mrayover/practice.logger/actions/runs/42162715129) |
| API 35, default text and 115% | Focused Practice passed with a long Goal, PDF page navigation and a recorded loop. The transport passed bounds and touch-target checks. Separate 115% checks passed dark forms, dialogs, date picker, Settings, Goal priority/theme indicators and the journal. [Focused layout result](https://github.com/mrayover/practice.logger/actions/runs/42163616853) · [Transport rerun](https://github.com/mrayover/practice.logger/actions/runs/42163760167) · [115%](https://github.com/mrayover/practice.logger/actions/runs/42162714593) |
| API 35, 200%, gesture navigation | Build97 source passed Home, catalog, routine and session builders, Focused Practice, loop setup, Block editor, session summary, Tools and backup control reachability. [Accessibility workflow](https://github.com/mrayover/practice.logger/actions/runs/42164265666) |
| API 35, media fixtures | Standalone Transcribe passed practice navigation, routine launch, stopwatch, the native Media Library picker, waveform and playback, markers, Reference Piano and rotation. Library platform speed, seek, loop and waveform checks passed. These jobs used the same media code as build97. [Transcribe](https://github.com/mrayover/practice.logger/actions/runs/42163760644) · [Library audio](https://github.com/mrayover/practice.logger/actions/runs/42163760870) |
| API 36, 720×1440 at density 360, 130%, three-button navigation | Build97 source passed the metronome and loop layout checks. The tests also override the Compose viewport and text scale to exercise a 320dp practice layout and a 280dp loop form at 200%. [Narrow phone](https://github.com/mrayover/practice.logger/actions/runs/42164265843) |
| API 36, 1600×2400 at density 320, 150%, gesture navigation | Build97 source passed the full-size Focused Practice workflow with a long Goal, PDF navigation, loop capture and saved-take playback. [Tablet profile](https://github.com/mrayover/practice.logger/actions/runs/42164484825) |
| API 36, narrow portrait and verified landscape, 130% and 200% system text | The final test-only follow-up passed label bounds, value retention, and visibility of the entire enabled Record Loop action with the native keyboard open. The landscape run explicitly requested and asserted the activity orientation. [Narrow three-button](https://github.com/mrayover/practice.logger/actions/runs/42165056596) · [Landscape gesture](https://github.com/mrayover/practice.logger/actions/runs/42165056464) |

Earlier trials exposed a one-column intrinsic-width error, text bounds at 200%, and insufficient height in the expanded practice panel. Corrected runs are linked above. The prior API 36 routine-navigation diagnostic encountered a System UI ANR. The generic API 35 Transcribe diagnostic lacked its audio fixture and the fixture-backed API 35 rerun passed. The early landscape setting did not rotate the app. The first stricter instrumentation build failed to compile because its bounds accessor was incompatible. The corrected test compiled locally and passed in narrow portrait and verified landscape. The two follow-up commits changed tests and their runner only.

No physical phone or tablet was attached. Samsung One UI, TalkBack, physical install-over and audio/display timing still need owner-device checks.

## Signed G1 delivery

**0.31.0/build96**, app source `a5b2a4740af4a45a220200ac52bdbbd25e1b017f`. Install over permanent-key build95. Use a separate empty installation for restore checks. [APK](dist/practice.logger-0.31.0-b96-a5b2a47/practice.logger-0.31.0-b96-a5b2a47.apk) · [ZIP](dist/practice.logger-0.31.0-b96-a5b2a47.zip) · [Build info](dist/practice.logger-0.31.0-b96-a5b2a47/BUILD_INFO.json) · [Owner-phone checks](MANUAL_TESTS.md#g1-goal-priority-and-primary-association-phone-checks)

The APK was built locally from the same app source verified by the emulator jobs. Package `com.mrayover.practicelogger.app` and certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` match build95. APK SHA-256 `31cb4f4d4a8ff41b7d924f4406c834087acbed81bc2bd1fd8b093aebd3376397`. ZIP SHA-256 `90faa5d6dfc99ea07adb09028f2912d531a76d5398e561eae09972c5293ac90e`. All 67 unsigned payload entries matched, the ZIP passed integrity and round-trip checks, and all 450 earlier APK/ZIP hashes matched.

## Implementation and checks

- Room 18→19 adds a Block-keyed primary association backed by the existing Goal/Block junction and a placement-keyed snapshot. Migration adopts a sole valid link and retains all existing links. Legacy positive practice keeps its existing attribution. First committed positive time records the current Goal and optional one-based rank atomically. Earlier ranks and explicit absence survive changes to the live association. Home and Goal totals retain the existing queries.
- Current Goal colors follow priority across themes. The Session and Routine builders and Goal Block lists retain the G1 cap. The Stage 1 screens use the Block marker rules above. The Block editor offers No Goal and preserves the current completed assignment for review or reassignment. [G1 scope](PRACTICE_LOGGER_V1_MASTER_PLAN.md#g1--priority-colors-primary-goal-and-historical-attribution-high-for-migration-medium-for-ui)
- Debug, release and instrumentation compiled. All 44 focused unit tests passed, including the Room 18 upgrade, all entry points, first accrual, continued practice, completion/reopening, assignment/clearing and unchanged totals. Schema 19 was compiler-generated. Schemas 1–18 are byte-identical. Full lint reported 5 errors and 27 warnings locally. The errors match build94 and build95 by rule, message and source file, with line shifts where G1 added UI.
- Goal cap and editor tests passed on [API 26](https://github.com/mrayover/practice.logger/actions/runs/42123935150) and [API 35 at 200% text](https://github.com/mrayover/practice.logger/actions/runs/42123938833). The test exercised all appearance choices, cap pixels, reorder, completion/reopening and clearing.
- Goal-launched timer tests passed on [API 26](https://github.com/mrayover/practice.logger/actions/runs/42124929480) and [API 35](https://github.com/mrayover/practice.logger/actions/runs/42124933111). First accrual captured rank 2, pause and resume retained that snapshot after reassignment, zero-time completion had no snapshot, and cold reopen retained the established Goal session history.
- Complete ZIP v3 passed export, restore to a separate empty install, restart, process-death recovery and another restart after editing a restored overlay. Archived records, original media hashes, live Goal assignment, historical rank and explicit absence survived. Prior ZIP v1/v2 restore, JSON v1–v7 readers, malformed attribution rejection and transactional recovery passed. Each API passed 10 checks plus deliberate process termination. [API 26](https://github.com/mrayover/practice.logger/actions/runs/42123942086) · [API 35](https://github.com/mrayover/practice.logger/actions/runs/42123945300) · [Format](BACKUP_FORMAT.md#goal-attribution-zip-v3)

G1 phone checks are pending for install-over, TalkBack, enlarged-text use in the builders and restore on a separate device or profile.

## Signed O1 delivery

**0.30.0/build95**, app source `ededd3f9e1e72744bad0e0de9ba53b583720f702`. Install over permanent-key build94. Run restore checks on a separate empty installation. [APK](dist/practice.logger-0.30.0-b95-ededd3f/practice.logger-0.30.0-b95-ededd3f.apk) · [ZIP](dist/practice.logger-0.30.0-b95-ededd3f.zip) · [Build info](dist/practice.logger-0.30.0-b95-ededd3f/BUILD_INFO.json) · [Owner-phone checks](MANUAL_TESTS.md#o1-practice-overlay-phone-checks)

Package `com.mrayover.practicelogger.app` and permanent certificate `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` match build94. APK SHA-256 `5d45c8324cac84598f1f03482f1abbb210d56e1db1fca922113c603d14f6c72c`. ZIP SHA-256 `123f5fd6cac3c87c322ffd38e6388f16ca15fadd42b75a3c0ea9b7099df85d97`. The signed APK matches all 67 unsigned payload entries. The distribution ZIP passed integrity and round-trip checks. All 444 prior APK and ZIP hashes match the session baseline.

## Implemented overlay and recovery

Highlight, Cover with Peek, and Focus use the existing viewer’s orientation-corrected fitted page and zoom and pan transform. Shapes support selection, movement, resizing, highlight colors, deletion, undo and visibility. Room 18 adds an explicit 17→18 migration and keeps markings bound to the exact Block and source. Copy gives shapes fresh IDs. Confirmed visual changes discard only the affected markings in the same transaction. [Practice Overlay scope and limits](docs/PRACTICE_OVERLAY_SPEC.md)

Complete ZIP v2 preserves editable shapes, all S2 durable data and original media. ZIP v1 and JSON v1–v7 remain readable. JSON continues to write data-only v7. [ZIP v2 format and recovery](BACKUP_FORMAT.md#practice-overlay-zip-v2)

## Verified O1 gates

- [Build95 CI](https://github.com/mrayover/practice.logger/actions/runs/42115513257) compiled debug, release and instrumentation APKs. It passed 62 scoped unit tests. Full lint reported 5 errors and 36 warnings. The five errors match the build94 baseline by rule, message and source location. Compiler schemas 16–18 match Git. The Room 17 upgrade fixture retained its archived Block, legacy visual and restore witness.
- [API 26 viewer](https://github.com/mrayover/practice.logger/actions/runs/42093603142) and [API 35 viewer at 200% text](https://github.com/mrayover/practice.logger/actions/runs/42093607030) passed three executions each. They covered shape edits, copy, Peek, paging, zoom, rotation, legacy replacement, cold reopen and editing while recording and the practice clock were active.
- [API 26 round trip](https://github.com/mrayover/practice.logger/actions/runs/42114761036) and [API 35 round trip](https://github.com/mrayover/practice.logger/actions/runs/42114764615) passed six executions each plus deliberate process termination after commit. Separate empty installations restored 19 durable tables, five markings and seven original files. The fixture used a multipage PDF shared by two Blocks, a canonical image and a legacy PDF. A Cover moved in the restored viewer survived another cold start. Every original hash matched. The S2 history, settings and audio state also passed.
- The first [API 26](https://github.com/mrayover/practice.logger/actions/runs/42113977058) and [API 35](https://github.com/mrayover/practice.logger/actions/runs/42113981493) recovery jobs passed six checks each, including corruption, a ZIP v1 fixture, ZIP creation, rollback and recovery. The legacy JSON workflow then stopped on its completed-session fixture prerequisite. The reordered jobs passed it after seeding that data. JSON v1–v7 compatibility passed.
- [Transcribe on API 26](https://github.com/mrayover/practice.logger/actions/runs/42114065270), [Transcribe on API 35](https://github.com/mrayover/practice.logger/actions/runs/42114071403) and [three metronome tests on API 35](https://github.com/mrayover/practice.logger/actions/runs/42114080004) passed. The full F1 regression was outside this assignment.

O1 physical phone install-over, touch, TalkBack and provider checks remain pending.

## Retained signed S2 delivery

**0.29.0/build94**, app source `9b2fb69a910dde4295e8b2062fb32284a6b759b8`. Install over permanent-key build93 to retain data. Restore tests belong on a separate empty installation. [APK](dist/practice.logger-0.29.0-b94-9b2fb69/practice.logger-0.29.0-b94-9b2fb69.apk) · [ZIP](dist/practice.logger-0.29.0-b94-9b2fb69.zip) · [Build info](dist/practice.logger-0.29.0-b94-9b2fb69/BUILD_INFO.json) · [Phone checklist](MANUAL_TESTS.md#s2-complete-portable-backup-phone-checks)

Package `com.mrayover.practicelogger.app` and permanent certificate `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` verified against build93. APK SHA-256 `4c54b5b0bba79e3cd973798e46050637e6e00dc14daefa880d9081679731a19f`. ZIP SHA-256 `48954060259b8f6de53648a57c1b7e7bc85649ecc80005ae31ef0ea21c0f3905`. All 67 unsigned payload entries match the signed APK. The distribution ZIP passed round-trip verification and 438 prior APK/ZIP files retained their hashes.

## S2 backup and recovery baseline

Complete ZIP v1 contains durable practice records, original media, audio annotations, portable preferences and standalone activity. Save As reports whether the destination passed byte read-back. A supported selected folder publishes a verified copy in Backups. Restore previews the validated archive and only applies to an empty installation. Room 17 adds one internal commit witness with explicit 16→17 migration. Legacy JSON still writes v7 and reads v1–v7. [Format, limits and recovery](BACKUP_FORMAT.md#complete-portable-zip-v1)

## Verified S2 gates

- [Build94 CI](https://github.com/mrayover/practice.logger/actions/runs/42085700439) compiled debug, release and instrumentation APKs and passed 57 scoped unit tests with zero failures or skips. Room 16 and 17 exports match the tracked files. The 16→17 upgrade retained existing rows. The ZIP inventory matched every column in all 18 durable tables.
- [API 26](https://github.com/mrayover/practice.logger/actions/runs/42084771822) and [API 35](https://github.com/mrayover/practice.logger/actions/runs/42083876310) each passed 11 instrumentation checks plus an intentional post-commit process kill. One installation saved through the system picker, a fresh installation restored through the picker, and a cold process restart verified all logical records and SHA-256 for all seven originals. The fixture included a two-page canonical PDF, canonical image, two-page legacy PDF, canonical microphone reference shared by two Blocks, M4A free take, WAV loop and imported MP3. Native rendering and playback, independent markers, A–B and playback state, paused unfinished practice, portable settings, edits and time totals passed. The restored total was 72,666 ms before the post-restore practice checkpoint.
- Failure checks exercised commit rollback, interrupted promotion, committed preference recovery, restart with a pending journal, cancellation and populated-target protection. Archive checks covered malformed paths, duplicate or missing entries, bad hashes, versions, invalid relationships, expanded-size limits and injected low storage. Destination checks covered unavailable access and simulated write/close failures. Legacy JSON v1–v7 decoding and the v7 export and restore workflow passed.
- [Local selected-folder test](https://github.com/mrayover/practice.logger/actions/runs/42084890480) verified a ZIP under Backups alongside S1 audio export. The first round-trip jobs timed out while installing the audio fixture encoder. The pinned encoder resolved setup. The first API 26 retry passed the normal round trip and stopped on the journal-check shell command. The corrected runner completed recovery.

Owner-phone install-over, Drive/OneDrive behavior and actual cloud synchronization remain open. The emulator verifies native playback start and progress. Physical audio and provider checks are in the phone checklist.

## Retained O0 and build93 history

O0 completed the [roadmap integration](PRACTICE_LOGGER_V1_MASTER_PLAN.md) and [overlay architecture](docs/PRACTICE_OVERLAY_SPEC.md). The [deferred lifecycle and insights register](PRACTICE_LOGGER_V1_MASTER_PLAN.md#deferred-practice-lifecycle-and-insights) records future design decisions.

## Signed Home footer correction

**0.28.1/build93**, app source `6e2c29d42254f69ce793be287645054e5bfdcf75`. Install over build92 to retain existing data. [APK](dist/practice.logger-0.28.1-b93-6e2c29d/practice.logger-0.28.1-b93-6e2c29d.apk) · [ZIP](dist/practice.logger-0.28.1-b93-6e2c29d.zip) · [Build info](dist/practice.logger-0.28.1-b93-6e2c29d/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.28.1-b93-6e2c29d/PHONE_CHECKLIST.txt)

Home removes the static Data usage row. Total practice remains a compact, non-clickable duration from the existing practice-time flow. The seven navigation buttons, Goals, Settings, theme and system insets use their existing paths. Short and enlarged-text layouts retain vertical scrolling. G3 will link Total practice to its statistics screen.

[Build and scoped tests](https://github.com/mrayover/practice.logger/actions/runs/42068841202) compiled debug, release and instrumentation APKs, passed 50 unit tests and reproduced the Room 16 schema byte for byte. The Home test checked all seven destinations and the duration across navigation. [API 35 at 100%](https://github.com/mrayover/practice.logger/actions/runs/42068845784) verified the footer and buttons fit without scrolling in an approximate 400 × 890 dp portrait display, with clear space above Android navigation. [API 26 at 200%](https://github.com/mrayover/practice.logger/actions/runs/42069832923) verified the shorter 400 × 690 dp layout scrolls to the footer and keeps the same navigation usable. The first API 26 dispatch returned HTTP 500 and created no test job. Physical phone acceptance and comparison with the owner’s actual display and saved total remain pending.

Package `com.mrayover.practicelogger.app` and permanent certificate `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` verified against build92. APK SHA-256 `f87378c971f409e0dd01794c709f1f47a273c81e8317a6c1835197169e152ebe`. ZIP SHA-256 `2d6c266e4cc99425d53083a5f6f61c5b66c796b80e49064a1d0ee09cdc98cf15`. Verified all 67 unsigned payload entries, ZIP integrity and hashes for 432 earlier APK/ZIP files.

## Signed S1 delivery

**0.28.0/build92**, app source `9c29261bc8fee96b11e3cf693f56eb668017bf7a`. Install over build91 without clearing app data. [APK](dist/practice.logger-0.28.0-b92-9c29261/practice.logger-0.28.0-b92-9c29261.apk) · [ZIP](dist/practice.logger-0.28.0-b92-9c29261.zip) · [Build info](dist/practice.logger-0.28.0-b92-9c29261/BUILD_INFO.json) · [Phone checklist](MANUAL_TESTS.md#s1--organized-audio-export-phone-checks)

Package `com.mrayover.practicelogger.app` and pinned certificate `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` verified. APK SHA-256 `82ab5803eca73a9a61e611f7f0b651422d3de7eb741e0e0e586e3f193e360d79`. ZIP SHA-256 `d27a5b56ec71d26a561a5e220e8c4a0f6f81d16a4bcb5add233776d5832aa939`. All 67 unsigned APK payload entries match the signed APK. ZIP round-trip passed and all 426 existing APK/ZIP files retained their hashes.

## S1 implementation

Settings → Storage chooses, changes or forgets a folder and checks current access. Saved-take export uses Android SAF, retains the returned read/write permission and copies original bytes into Block/date or Free Recordings/date folders. Same-name Blocks use stable IDs and repeated exports create independent documents. Save As stays available, including when a provider cannot create or write the organized path. Copy failures preserve originals and report whether the new partial document was removed or may remain. [Usage and naming](README.md#exporting-audio)

Debug, release and instrumentation compiled, all 50 scoped unit tests passed and the generated Room 16 schema matched the baseline byte for byte. [Build and unit tests](https://github.com/mrayover/practice.logger/actions/runs/41938953027)

[API 26](https://github.com/mrayover/practice.logger/actions/runs/41939860147) and [API 35](https://github.com/mrayover/practice.logger/actions/runs/41939862743/job/126958161118) passed the organized-export test on the local SAF provider. It covers picker selection, retained access after Activity recreation, Block/standalone paths, same-name Blocks, Block rename, repeated exports, original MP3/M4A/WAV fixture bytes, a rejected read-only grant, missing source, partial-copy cleanup, retry and forgetting access. API 26 also passed the existing Block and standalone recording workflows, including per-take Save As. API 35's generic diagnostic later stopped on a missing generated fixture for Record Reference. The app source in the signed build is identical to the app source tested by these runs.

The dedicated API 35 workflows generated the fixtures and passed both Record Reference tests and both T3D concurrent capture tests. [Record Reference](https://github.com/mrayover/practice.logger/actions/runs/41940462250) · [T3D](https://github.com/mrayover/practice.logger/actions/runs/41940465228)

Drive and OneDrive are untested. The local and cloud owner-phone checklist includes folder persistence after app restart, stale grants, interrupted copies and provider availability. [Phone checklist](MANUAL_TESTS.md#s1--organized-audio-export-phone-checks)


## Retained build91 — T3D signed delivery

**0.27.0/build91**, app source `1b842504cb04b8b19f44eeb40867332a7e54f9f6`. Install over build90 without uninstalling or clearing data. [APK](dist/practice.logger-0.27.0-b91-1b84250/practice.logger-0.27.0-b91-1b84250.apk) · [ZIP](dist/practice.logger-0.27.0-b91-1b84250.zip) · [Build info](dist/practice.logger-0.27.0-b91-1b84250/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.27.0-b91-1b84250/PHONE_CHECKLIST.txt)

App ID `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. APK SHA-256 `b358be1e7b9f9f7ff25dec9b832dea8c51e0f1fbf7309c036a62d53b7a24aff6`. ZIP SHA-256 `fbd9ca56bcf0cb03c07a28b08d294cc5af8421b3f2150ac3e31a62de73d3600b`. Verified the build90 signing identity, package and version, ZIP round trip, all 67 unsigned APK payload entries, and all 1,689 prior distribution file hashes.

## Implemented

**Record Take** uses the existing Free Record capture and review in Block Transcribe. The reference player stays mounted and retains output focus while the reference or piano plays. The mic records a separate AAC/M4A take. Reference transport stays available during capture, with the mic status and Stop pinned. Play Preview pauses reference and piano output before the take player requests focus. The user can resume the reference explicitly after review.

Saving retains normal Block take ownership in the active practice session. The existing Block placement remains the only practice clock and Goal attribution source in this flow. Room 16 and JSON 7 keep their formats. See [audio ownership and lifecycle](docs/MEDIA_AUDIO_WORKSPACE.md#record-take-in-block-transcribe).

## Focused verification

- Debug, release and instrumentation APKs compiled, all 50 scoped unit tests passed, and the generated Room 16 export matched byte for byte. The tests include existing standalone activity and Goal accounting, recording file recovery, and migration and backup safeguards. [Build](https://github.com/mrayover/practice.logger/actions/runs/41934161097)
- Two T3D tests passed on each API. They exercise capture alongside 75% reference playback and A–B repeat, independent pause/seek/stop and speed, preview, Re-record, Save and Discard, original bytes and annotations, single-counted Block time, backgrounding, Activity recreation, and competing output focus. [API 26 at 100%](https://github.com/mrayover/practice.logger/actions/runs/41934795571) · [API 35 at 200%](https://github.com/mrayover/practice.logger/actions/runs/41934795818)
- Adjacent checks passed for platform pitch/speed, seek and waveform integrity on API 26. API 35 checks passed for existing Block Transcribe and piano, T3C reference capture and protected replacement, and ordinary Free Record and phrase review. [Library](https://github.com/mrayover/practice.logger/actions/runs/41934795580) · [Block Transcribe and piano](https://github.com/mrayover/practice.logger/actions/runs/41934795568) · [T3C](https://github.com/mrayover/practice.logger/actions/runs/41934795557) · [Free Record and phrase review](https://github.com/mrayover/practice.logger/actions/runs/41934413158)
- No completed scoped test run failed. The initial build run was cancelled after a review found a Notes switch that could unmount Transcribe during capture. The replacement build includes the guard. [Superseded run](https://github.com/mrayover/practice.logger/actions/runs/41934038169)

Physical headphones, speaker, Bluetooth routing, microphone denial and acoustic quality remain pending. Speaker playback can be picked up by the mic. Android selects the physical routes. Valid review audio can survive foreground loss or rotation within the app process. A cold restart cleans up unsaved interrupted takes.

**T3D and T3C owner-phone acceptance remains pending.** See the [T3D checklist](MANUAL_TESTS.md#t3d--concurrent-transcribe-playback-and-free-record) and [T3C checklist](MANUAL_TESTS.md#t3c--record-reference-directly-in-transcribe).

## Retained build90 — Metronome BPM fix

**0.26.1/build90** applies a valid typed BPM when the Android keyboard's Done action is pressed. The shared field covers the standalone and Focused Practice metronomes.

[Signed APK](dist/practice.logger-0.26.1-b90-7f9124a/practice.logger-0.26.1-b90-7f9124a.apk) · [ZIP](dist/practice.logger-0.26.1-b90-7f9124a.zip) · [Build info](dist/practice.logger-0.26.1-b90-7f9124a/BUILD_INFO.json). Install over build89 without uninstalling or clearing data. Source 7f9124a09a8890da488838ac5653559eb2eee061. APK SHA-256 1f6660d8a0678de2cd4f325982f3eaa4c68d5d82ed9ba6ce781afd851d1d8a08. ZIP SHA-256 81037b63679445d144a1fa55bf187856ec55d7ceaa434f4cdd49cb644c7cc600.

[Package-only CI](https://github.com/mrayover/practice.logger/actions/runs/41901998805) passed. Verified permanent signing identity, package/version, APK payload and ZIP integrity. The owner confirmed that the metronome BPM keyboard Done fix works on their phone.

## Retained T3C signed delivery

Updated November 4, 2026 (America/Los_Angeles).

## Signed delivery

**0.26.0/build89**, source `1c8724b293d90489ec984803595037a0c0e84245`. [APK](dist/practice.logger-0.26.0-b89-1c8724b/practice.logger-0.26.0-b89-1c8724b.apk) · [ZIP](dist/practice.logger-0.26.0-b89-1c8724b.zip) · [Build info](dist/practice.logger-0.26.0-b89-1c8724b/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.26.0-b89-1c8724b/PHONE_CHECKLIST.txt). Install over build88 without uninstalling or clearing data.

App ID `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. APK SHA-256 `c1f264e7c05ff57aff9e349b3048d7a0993de9e57e592a77bf885ff66ed319a4`. ZIP SHA-256 `9218fd2e95239dfc365685092b4ad66b3ee9147dddf76de5cc93f3c95ded33e7`. Verified APK signature, package/version, ZIP integrity, all 67 unsigned payload entries and 1,622 prior distribution file hashes.

## Implemented

Record Reference opens in standalone and Block Transcribe and pauses reference playback. Capture uses the existing microphone recorder and AAC/M4A review. Keep selects one canonical Library reference in standalone or attaches it in Block Transcribe. A protected Block replacement requires confirmation. The old link and local edits survive failed or cancelled replacement. The previous Library original and master markers survive successful replacement. No ordinary take is created. Room 16 and JSON 7 retain their formats and JSON excludes canonical audio.

## Focused verification

- Debug, release and device-test APKs compiled. All 50 scoped unit tests passed and the generated Room 16 export matched the committed file. [Build](https://github.com/mrayover/practice.logger/actions/runs/41897981515)
- Both new tests passed on API 26 at 100% text and API 35 at 200% text. They cover standalone microphone capture, canonical Keep and selection without a take, plus transactional rollback and confirmed Block replacement. [API 26](https://github.com/mrayover/practice.logger/actions/runs/41898002203) · [API 35](https://github.com/mrayover/practice.logger/actions/runs/41898002856)
- The existing Block Transcribe and standalone regression checks passed. [Block Transcribe](https://github.com/mrayover/practice.logger/actions/runs/41898736659) · [Standalone Transcribe](https://github.com/mrayover/practice.logger/actions/runs/41898736586)

The [T3C owner-phone checklist](MANUAL_TESTS.md#t3c--record-reference-directly-in-transcribe), including permission and interrupted capture, remains pending.

## Retained T3B signed delivery

**0.25.0/build88**, source `947f636f450e944a8b511cc2efc98cdd09459a9f`. [APK](dist/practice.logger-0.25.0-b88-947f636/practice.logger-0.25.0-b88-947f636.apk) · [ZIP](dist/practice.logger-0.25.0-b88-947f636.zip) · [Build info](dist/practice.logger-0.25.0-b88-947f636/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.25.0-b88-947f636/PHONE_CHECKLIST.txt). Install over build87 without uninstalling or clearing data.

App ID `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. APK SHA-256 `80ce147cdd7e2c156b70e367adbd0b23ffffb98bf3d9c76b186dab95ef19964c`. ZIP SHA-256 `de220d0084f71f8f51698cfd5d02982951ef5ddebba2e42088392f74410d00d3`. Verified signing identity, app ID, version, ZIP integrity, all 67 unsigned payload entries and 1,584 prior distribution file hashes.

## Implemented

T3B implements an open-ended stopwatch with optional Goal attribution, an exclusive clock shared with Block practice, and Home/Weekly View totals. Room 16 adds an activity table with explicit migration from Room 15. JSON 7 readers and writer are retained. Standalone Transcribe history is excluded until S2, and the empty-target restore guard refuses targets with standalone activity.

The timer checkpoints every five seconds while active and when paused or its context changes. Leaving, backgrounding and reopening restore a paused timer. A cold restart retains the last durable checkpoint. Active time since that checkpoint may be lost. A save failure pauses accumulation and can be retried against the same ID. Weekly time uses the local Monday–Sunday week of the start timestamp.

## Focused verification

- Debug, release and device-test compilation passed. All 50 scoped unit tests passed. They cover the explicit Room 15→16 migration, the migration chain from schema 1, Goal and overall aggregation, local week boundaries, duplicate Stop protection, media deletion, and the JSON 7 activity-only restore safeguard. The generated Room 16 export matches the committed file byte for byte. [Build 41891794705](https://github.com/mrayover/practice.logger/actions/runs/41891794705)
- Three standalone methods passed on each API, covering builder/Routine navigation, timed Goal attribution, pause on leaving, recreation, failed Stop and successful retry, audio playback with an idle timer, and the existing Library, waveform, master-marker and piano flows. The landscape screenshots show the stopwatch and upper playback controls at both text scales. Lower workspace content scrolls. [API 26 at 100%](https://github.com/mrayover/practice.logger/actions/runs/41891820516) · [API 35 at 200%](https://github.com/mrayover/practice.logger/actions/runs/41891820771)
- The API 35 force-stop and reopen check passed both phases at 200% text using the signed build's verified source. [Cold reopen](https://github.com/mrayover/practice.logger/actions/runs/41892827746)

The owner reported that T3B works. Individual phone checklist observations were not supplied. Earlier focused-run failures and their fixes are recorded in [WORK_LOG](WORK_LOG.md).

## Retained T3A signed delivery

**0.24.0/build87**, source `bb582ba8eb8195bcbe68888c9f42df582ddcb617`. [APK](dist/practice.logger-0.24.0-b87-bb582ba/practice.logger-0.24.0-b87-bb582ba.apk) · [ZIP](dist/practice.logger-0.24.0-b87-bb582ba.zip) · [Build info](dist/practice.logger-0.24.0-b87-bb582ba/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.24.0-b87-bb582ba/PHONE_CHECKLIST.txt). Install over build86 without uninstalling or clearing data.

App ID `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. APK SHA-256 `055fe3a2d3108c86344b60839c723924896bdfd4ac581d87fcb6c00c1a360698`. ZIP SHA-256 `f55ff71546b31efae9d5d059cf7b9cb65325e2f3083b35884fe8d6c9b06dc6b5`. The permanent-key publisher verified package, version and signature. The ZIP passed member and CRC checks. All 67 unsigned APK payload entries matched the signed APK, and all 398 prior distribution hashes matched.

## Implemented

Practice Now lists every unfinished session with its current Block. Add Blocks opens the existing builder and its saved draft. Routines starts the selected valid Routine directly, and Home → Routines still manages templates and Recent Practice.

Standalone Transcribe chooses or imports canonical AUDIO through the Library picker. It shares the Library transport, waveform and master point/section editor, and reuses Block Transcribe's embedded Reference Piano and focus coordination. Global edits flow to inheriting Blocks. Customized and local-only Block markers retain their own data.

Changing the recording, leaving or backgrounding releases playback and piano output. Rotation keeps the selected recording and opens it paused. The selection is saved for the current visit. Room 15 and JSON 7 remain in use. Standalone playback carries no practice-time attribution.

## Focused verification

- Debug, release and device-test APK compilation passed. All 30 scoped unit tests passed and generated Room 15 matched the committed export byte for byte. [Build 41884754998](https://github.com/mrayover/practice.logger/actions/runs/41884754998)
- Both standalone tests passed on API 26 at 100% and API 35 at 200%. They cover builder draft retention, direct Routine start, session continuation, empty Library, real document import and failed import, selection cancellation and change, 75% and 50% speed settings, zoom, A–B playback, master point create/edit/delete, piano with the reference running and paused, rotation without autoplay, and exit without session writes. [API 26](https://github.com/mrayover/practice.logger/actions/runs/41885549924) · [API 35](https://github.com/mrayover/practice.logger/actions/runs/41885553890)
- The final source passed the existing enlarged-text Block Transcribe workflow. The first T3A source passed five navigation and cold-reopen checks covering multiple sessions, Routine management and Goal practice, and three Library platform checks covering MP3/M4A/WAV playback, looping, seeking, interruption and waveform integrity. The final source retains the navigation and controller paths. [Block Transcribe](https://github.com/mrayover/practice.logger/actions/runs/41885557891) · [Navigation](https://github.com/mrayover/practice.logger/actions/runs/41883772709) · [Library platform](https://github.com/mrayover/practice.logger/actions/runs/41883777746)
- Reviewed portrait piano and landscape transport screenshots at both text scales. Landscape at 200% requires vertical scrolling for the full waveform and lower controls.

See [WORK_LOG](WORK_LOG.md) for the initial standalone device failure and its follow-up. The owner reports having installed and tested T3A and says it works. Specific checklist observations were not supplied. The existing platform decoder may leave a small gap at short compressed loop joins.

## Retained T2 release — Block-local Transcribe

### Signed delivery

**0.23.0/build86**, source `c4d48edcc27ea2060c6684b8a2f30a8b100d08d0`. [APK](dist/practice.logger-0.23.0-b86-c4d48ed/practice.logger-0.23.0-b86-c4d48ed.apk) · [ZIP](dist/practice.logger-0.23.0-b86-c4d48ed.zip) · [Build info](dist/practice.logger-0.23.0-b86-c4d48ed/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.23.0-b86-c4d48ed/PHONE_CHECKLIST.txt). Install over build84 without uninstalling or clearing data.

App ID `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. APK SHA-256 `3e42bd73e147a16b7ea1fb16bda7feff063a2e760466a73085176c273c3bdb62`. ZIP SHA-256 `81166c850120a4fd4a09b5ecb95292d0e1bb2149b61b0b7c9bb0e766908e8dab`. Verified package/version, signature, 67 matching unsigned payload entries, ZIP members and CRC, and 392 historical APK/ZIP hashes.

### Implemented

Transcribe checkpoints Block-local source position, speed, A–B, repeat and marker selection. It restores the active recording paused. Two Blocks can share one canonical original and retain independent settings.

The shared waveform and list render the same inherited, customized and local markers. Blocks can add points/sections, edit, hide and reset. Add to Master and Update Master require confirmation. Update compares the current master against the original snapshot and checks the reviewed version again after a conflict. Master deletion preserves customized markers as local-only and removes pure hides.

Room 14→15 retains schemas 1–14 and adds two tables scoped to the exact active AUDIO link. Replacement or unlink warns before removing local work and cascades in the association transaction. JSON 7 remains data-only and excludes Block audio state and local markers. Media-inclusive backup is assigned to S2.

### Focused verification

- Build86 compiled debug, release and test APKs, passed 30 scoped units and generated schema15 matching the committed export. Coverage includes 14 to 15 and earlier migration paths, shared ownership, promotion rollback and conflicts, independent overrides and deletion cleanup. [Build 41776290501](https://github.com/mrayover/practice.logger/actions/runs/41776290501)
- Build86 passed the Transcribe UI workflow on API26 at normal text and API35 at 200% text. The tests cover portrait and landscape, piano drag and playing, paused restoration, local marker creation, canceled and confirmed publication, and canceled and confirmed audio replacement. Reviewed portrait piano and enlarged landscape screenshots. [API26](https://github.com/mrayover/practice.logger/actions/runs/41776777624) · [API35](https://github.com/mrayover/practice.logger/actions/runs/41776779735)
- Build85 passed both checkpoint and force-stop reopen phases on API26 and API35. It also passed three Library platform/playback/waveform checks and two Record Loop, Free Record and legacy take compatibility workflows. Build86 changes the marker dialog host and version while retaining those persistence and audio paths. [Reopen API26](https://github.com/mrayover/practice.logger/actions/runs/41775914113) · [Reopen API35](https://github.com/mrayover/practice.logger/actions/runs/41775915966) · [Library API26](https://github.com/mrayover/practice.logger/actions/runs/41775920659) · [Transport API35](https://github.com/mrayover/practice.logger/actions/runs/41775918461)

See [WORK_LOG](WORK_LOG.md) for the initial migration fixture failure and UI reruns. Verification was scoped to T2 and adjacent audio workflows.

The T3-0 handoff reports general working status. The owner also confirmed the build84 piano-range refinement works correctly. Individual build86 install-over, device and listening checks remain unreported. The [T2 owner-phone checklist](MANUAL_TESTS.md#t2

---owner-phone-checklist) remains open.

## T3-0 roadmap lock

The [Standalone Transcribe plan](docs/T3_STANDALONE_TRANSCRIBE_PLAN.md) records the owner’s Practice Now navigation, standalone stopwatch and optional Goal attribution, and separate Record Reference and concurrent Free Record workflows. T3-0 was the documentation checkpoint for the subsequently delivered T3A–T3D increments.

**Next executable task:** **T3A — Standalone Transcribe Workspace**, after T3-0 is committed and the owner explicitly assigns T3A.

---

## Retained build84 — Reference Piano range refinement

Updated November 3, 2026 (America/Los_Angeles).

### Signed delivery

**0.22.1/build84**, source `7091421748baa2be79535976acc6af1b1bcc46ee`. [APK](dist/practice.logger-0.22.1-b84-7091421/practice.logger-0.22.1-b84-7091421.apk) · [ZIP](dist/practice.logger-0.22.1-b84-7091421.zip) · [Build info](dist/practice.logger-0.22.1-b84-7091421/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.22.1-b84-7091421/PHONE_CHECKLIST.txt). Install over build83 without uninstalling or clearing data.

App ID `com.mrayover.practicelogger.app`. Permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. APK SHA-256 `cac99ddd839ef56ccccc91a4dfe094d18b0461ce8346f80596533ec9b2112137`. ZIP SHA-256 `8020eb2591bccfb2780770593098ffedf8240674dfe72e11a96a74b5251830a6`. Verified APK signature, app ID and version, 67 matching unsigned payload entries, ZIP contents and CRC, and 380 historical package hashes.

The shared Tools and Block Transcribe keyboard shows 17 pitches in typical portrait layouts, 13 below 300 dp and 18 from 600 dp. The register handle shifts by semitone within MIDI 24–96. Both octave buttons retain the current pitch class and require room for a full 12-semitone move. Sharp-starting and sharp-ending windows display complete black keys. Range changes release existing voices and clear their key feedback. Both screens still use the existing audio engine and focus ownership. Room14 and JSON7 remain in use.

### Focused verification

- Debug, release and Android test APK compilation and all five piano geometry and PCM units passed. [Build 41770969141](https://github.com/mrayover/practice.logger/actions/runs/41770969141)
- The Transcribe workflow passed on API26 at normal text and API35 at 200% text, including a semitone drag, sharp-key input during reference playback and portrait/landscape navigation. The register and keyboard are reachable within the scrollable panel. [API26](https://github.com/mrayover/practice.logger/actions/runs/41771459836) · [API35](https://github.com/mrayover/practice.logger/actions/runs/41771461747)
- Two native audio tests per API passed, covering piano output with the reference playing and paused, interruption, release and standalone output. The overall runs stopped in the separate Tools test when the test driver tried to start a second touch using an active pointer ID. [API26](https://github.com/mrayover/practice.logger/actions/runs/41771455590) · [API35](https://github.com/mrayover/practice.logger/actions/runs/41771457973)
- The Tools-only reruns passed both tests per API after invoking the octave button's accessibility action while the original pointer was held. They verified chord input, glissando, range movement, release and continued-touch suppression, exact octave shifts, both range limits, rotation and both black edges at 260 dp. Portrait, landscape and narrow screenshots were reviewed. At 260 dp with 200% text, the range label can wrap while the handle and both octave buttons remain usable. App source and build configuration match the signed APK source. [API26](https://github.com/mrayover/practice.logger/actions/runs/41771762628) · [API35](https://github.com/mrayover/practice.logger/actions/runs/41771759735)

Verification was limited to the scoped piano and Transcribe checks. Physical install-over and listening checks are pending.

The owner confirms the latest piano-range refinement works correctly. Individual install-over, device and acoustic checklist results remain unreported.

---

## Retained T1 release — Record Loop / Transcribe

Updated November 3, 2026 (America/Los_Angeles). T1 is implemented and its scoped Android checks passed. T1 owner-phone acceptance is pending. The owner confirms the K1 Reference Piano works on their phone. This accepts the reported keyboard functionality. L1–L3 retain their separately documented acceptance status.

### Build 83 signed delivery

**0.22.0/build83**, app source `b7e422ab92d019c33ae5fed1b5f71ab31890f31e`. [APK](dist/practice.logger-0.22.0-b83-b7e422a/practice.logger-0.22.0-b83-b7e422a.apk) · [ZIP](dist/practice.logger-0.22.0-b83-b7e422a.zip) · [Build info](dist/practice.logger-0.22.0-b83-b7e422a/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.22.0-b83-b7e422a/PHONE_CHECKLIST.txt). Install over build81 without uninstalling or clearing data.

App ID `com.mrayover.practicelogger.app`, permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. APK SHA-256 `25440f884c24cfe94ec49b183618767c16fbd07f4afec4371e49e118949d7837`. ZIP SHA-256 `893a0b357d657ff521b7107924a00d3628c3e8bc24d45d645a1d4350ff7d3464`. Signature, package/version, 67 matching unsigned payload entries, ZIP contents/CRC, 368 prior package hashes and both build82 candidate hashes were verified.

### Implemented scope

Focused Practice switches between Record Loop and Transcribe. Transcribe uses the source Block's canonical AUDIO link, the existing Library picker/import and role checks, and the shared source-time player, waveform and zoom. The panel supports pitch-preserving 50–100% speed, seek, A–B repeat, one-shot phrase playback and timestamp edits. Master points and sections are read-only. The existing expandable piano can sound while reference audio is playing or paused, using the controller's single focus request. Keyboard collapse releases its output. Leaving the panel, changing the reference or backgrounding releases active Transcribe output.

Record Loop and Free Record use the existing capture and review path. The practice timer remains authoritative. Saved legacy imports retain their original take ownership and playback. Room14 and JSON7 are in use. T1 playback state is transient. Durable local state and annotations belong to T2, and simultaneous Transcribe playback and Free Record belong to T3D.

### Actual verification

- [Build83 compilation and unit tests](https://github.com/mrayover/practice.logger/actions/runs/41767496452) passed debug, release and instrumentation APK compilation and all **24 scoped units**.
- [API26 at normal text](https://github.com/mrayover/practice.logger/actions/runs/41767948973) and [API35 at 200% text](https://github.com/mrayover/practice.logger/actions/runs/41767951438) passed the Transcribe workflow in portrait and landscape. Tests cover mode changes, retained Loop setup, canonical association isolation, picker cancellation and replacement, 75% playback with section selection and repeat, piano collapse during playback, rotation and pinned actions. Compact portrait keyboard and enlarged landscape transport screenshots were reviewed. Secondary controls scroll in constrained layouts.
- [API35 recording compatibility](https://github.com/mrayover/practice.logger/actions/runs/41767954387) passed **2 native workflows** for Record Loop, Free Record, Notes during capture, saved take selection/playback and legacy imported audio.
- The unchanged audio engine passed **3 native piano and Tools checks** on [API35 build82](https://github.com/mrayover/practice.logger/actions/runs/41766318314) and **3 Library playback and waveform checks** on [API26 build82](https://github.com/mrayover/practice.logger/actions/runs/41766321106). These covered actual piano output during paused and playing reference audio, interruption, standalone operation, MP3/M4A/WAV speed and pitch parameters, repeat/seek, missing original handling and long waveform cache recovery. Build83 changes presentation and version after those audio checks.

See [WORK_LOG](WORK_LOG.md) for the early dependency failure and layout retries. Physical install-over, listening and owner phone verification remain pending. No full regression gate was rerun.

The original [T1 phone checklist](MANUAL_TESTS.md#t1--block-transcribe-owner-phone-checklist) remains available. T2, Block-local Audio State, Marker Overrides and Master Promotion, remains a separately assigned milestone.

---

## Retained K1 release — Reference Piano

Updated November 3, 2026 (America/Los_Angeles). K1 implementation, targeted engineering verification and permanent-key signed delivery are complete. **The owner confirms the K1 Reference Piano works on their phone.** This accepts the reported keyboard functionality. Individual acoustic measurements and checklist results were not supplied. The owner reports the latest pre-K1 functionality works; no individual L1/L2/L3 phone-check results are invented.

## Build 81 signed delivery

**0.21.0/build81**, application/release source `a3fd4090dd34a95c0bea266b83a9b04e0fc14638`. [APK](dist/practice.logger-0.21.0-b81-a3fd409/practice.logger-0.21.0-b81-a3fd409.apk) · [ZIP](dist/practice.logger-0.21.0-b81-a3fd409.zip) · [Build info](dist/practice.logger-0.21.0-b81-a3fd409/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.21.0-b81-a3fd409/PHONE_CHECKLIST.txt). Install over build80 without uninstalling or clearing data. AppID `com.mrayover.practicelogger.app`; unchanged permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.

APK SHA256 `4c295089bffab0ccf9a789ead6973bc01c4b3977901bb5fbc84ea95de2e47b75`; ZIP SHA256 `7b01029604c6e593052026dc28533a3877b83a3649d779f72c45c0ed1a9bb028`. Publisher signature/package/version/pinned-certificate checks pass. All67 unsigned application payload entries match the signed APK. ZIP CRC/exact members/bytes and all362 session-entry APK/ZIP hashes pass; earlier releases are retained. Exact final Git/remote SHA is verified at handoff.

## Implemented scope and reuse

Home → Tools → Reference Piano adds the compact instrument without a new Home control or microphone request. Compact C4–C5 inclusive, large-width C4–F5, octave shifts covering C1–C7, white/black geometry, pressed-key/note feedback and genuine native multitouch support chords, unisons, glissando, repetition and cancellation. Scrolling keeps keyboard/Back reachable in landscape/enlarged text. Register is saveable UI state only.

Independent PianoPcm provides12-tone A4=440Hz synthesis,16 token-owned voices including release tails,6ms attack/45ms release, light decaying harmonics and bounded smooth mixing. PianoAudioEngine owns one48kHz mono PCM16 stream and foreground/output cleanup. PianoKeyboard is a reusable Compose callback component; PianoScreen is the small standalone Tools owner. [Architecture and T1 reuse contract](docs/REFERENCE_PIANO.md). Embedded construction borrows an existing playing LibraryAudioController's focus, with no competing request/abandon. Owner pause/close/source change/background/errors/focus loss stop companion notes; keyboard release/close never closes the reference controller. No global mixer or player rewrite.

Room14, schemas/migrations1–14, JSON7/readers1–7, media originals/links/markers, take ownership and practice accounting are unchanged. No T1/Block Transcribe, MIDI, recording/export or new persisted piano data exists.

## Actual scoped verification

- [Build41745864648](https://github.com/mrayover/practice.logger/actions/runs/41745864648), sourcea3fd409: debug/release/instrumentation compilation and **4/4 PianoPcm unit tests**, zero failures/errors/skips. Generated PCM fundamental checks at MIDI24/40/60/69/72/84/96, A4/semitone/octave formulas, lower/upper pitches,16-voice bounds, unison/rapid retrigger/glissando/release and black/white/outside hit geometry pass.
- [Final UI API26/100% 41747320122](https://github.com/mrayover/practice.logger/actions/runs/41747320122) and [final UI API35/200% 41747324096](https://github.com/mrayover/practice.logger/actions/runs/41747324096) pass the UI class each. [Audio API26/100%41746593019](https://github.com/mrayover/practice.logger/actions/runs/41746593019) and [audio API35/200%41746596773](https://github.com/mrayover/practice.logger/actions/runs/41746596773) pass2/2 audio tests each before their later UI assertion failure. **Three distinct native tests pass per API across scoped runs,6 successful selected executions total; no single combined green-run claim.** Actual nonzero AudioTrack writes while Library source position advances; triad output, release/close retaining reference playback, post-close seek/speed, borrowed focus interruption/background/no automatic restart, standalone repetition/background/close are checked. UI native three-finger chord, selected-key feedback, white-to-white slide, black note, ACTION_CANCEL, endpoint registers, recreation/background, portrait/landscape and Back pass. Existing Metronome PCM, Tuner microphone processing, Timer and Audio Settings navigation pass. Portrait chord/landscape screenshots on both APIs were inspected. Signed application sourcea3fd409 is unchanged; audio test/workflow source1b11505 and final UI sourcee957f7d differ only in workflow/records and new-test synchronization.

Initial build41745587125 failed a new local theme lookup and was corrected. Native setup41746405634/41746409574 failed an artifact reuse folder before test execution; corrected workflow only. Combined audio/UI runs41746593019/41746596773 passed all piano stages then failed the new immediate tools.clicking assertion after Back at line66. The existing Tools test waits for Compose disposal here; adding the same synchronization preserves the stop assertion and final UI-only checks pass. No product change was made for that retry. Exact diagnostics remain in WORK_LOG. No full P5/F1/F2/broad lint rerun or physical acoustic listening occurred.

## Limits and exact next action

Simple synthesized piano-like timbre; no acoustic samples. Sixteen voices include45ms tails; extra presses are ignored until capacity frees. In build 81, embedded notes required a playing reference controller. Native active outputs/source progression do not certify phone speaker/headphone audibility, tuning/timbre, latency, route/vendor interruptions or TalkBack usability. Those remain physical checks. Existing media-inclusive backup limits remain; keep originals separately until S2.

P5 acceptance remains closed; Stage4 remains0/10 documented real sessions.

---

## Retained L3 release — historical delivery and evidence

# L3 Block media associations delivered (historical)

Updated November 3, 2026 (America/Los_Angeles). L3 implementation and signed delivery are complete; actual scoped verification and limits are recorded below. **Owner-phone acceptance of L3 is pending; L1/L2 phone acceptance remains separately pending.** P5/build69 acceptance is preserved below. Stop after L3.

## Current signed delivery

**0.20.0/build80**, application/release source `34edea07be82a03c84087cb5540b0ce27fa07b7c`. [APK](dist/practice.logger-0.20.0-b80-34edea0/practice.logger-0.20.0-b80-34edea0.apk) · [ZIP](dist/practice.logger-0.20.0-b80-34edea0.zip) · [Build info](dist/practice.logger-0.20.0-b80-34edea0/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.20.0-b80-34edea0/PHONE_CHECKLIST.txt). Install over build76 without uninstalling or clearing data. Internal77/78/79 and all earlier packages remain. App ID `com.mrayover.practicelogger.app`; unchanged permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.

APK SHA256 `4d113ce795b9f14fb3c1c656259034d80e95448936eb47f6d5188577ce34f633`; ZIP SHA256 `83c440b99f0e94c70b65b997f0839074126bcca2d394de62b98f4dfd00214203`. Publisher signature/package/version/pinned certificate checks pass. All67 unsigned application payload entries match the signed APK; ZIP CRC/exact members/bytes and338 session-entry APK/ZIP hashes pass. Production source is unchanged after the signed source above; later commits change tests/workflow/records only. Exact final Git/remote SHA is reported at handoff.

## L3 scope and ownership

Saved Blocks independently choose/import/replace/unlink one canonical visual (IMAGE/PDF) and one canonical audio asset using stable Library IDs. Shared Library search, compatible filters, metadata, stable sorting and current-selection display are reused. New file/camera/gallery imports commit one canonical original before linking; failed attachment retains it and the previous reference. A second Block link stores only a lightweight row. Unlinking preserves the original, master annotations and other Blocks.

Room **14**, explicit **13→14**, adds block_media_links with composite Block/role identity, mediaId index and restrictive Block/asset foreign keys. Repository validation and fresh/migrated database triggers enforce role/category compatibility. Block deletion dependencies include canonical links without weakening Goals/Routines/takes/legacy images/all-session-history protections. Referenced asset deletion rolls master-marker removal back before any original-file deletion. Schema14 matches KSP; schemas/migrations1–13 retained.

Legacy visuals stay readable through explicit fallback. Confirmed replacement atomically commits the canonical link and retires legacy metadata, then serialized conservative cleanup removes the old original; failure preserves the legacy row/file. Existing IMPORTED_LOOP takes keep their IDs, labels, Block links, exclusive files/recordings ownership, playback/export/deletion/recovery; recorded WAV and free M4A takes remain. No automatic adoption, startup copying or canonical pointers into legacy directories. Loop Setup's legacy importer remains until T1 convergence after K1, per the explicit L3 handoff.

Canonical and legacy visuals share the existing Block viewer in Focused Practice and the recording sheet. sourceBlockId ownership, Description/Reference arrangement, collapse/restore, PDF pages/zoom/pan and pinned capture/review controls remain. Audio is an association/metadata capability in L3; Block waveforms, A–B/speed/local annotations, Transcribe, keyboard and concurrent playback/capture modes remain later scope. [Architecture and legacy decisions](docs/BLOCK_MEDIA_ASSOCIATIONS.md).

JSON **7** / readers **1–7** stay data-only: canonical originals/metadata, Block media links and master annotations remain excluded. Empty-target restore creates no dangling media references; Library-only/linked targets remain protected transactionally. Editor/Library/Backup UI disclose limits. Keep originals separately until S2 complete recovery.

## Actual scoped verification and failures

- [Final build41739881932](https://github.com/mrayover/practice.logger/actions/runs/41739881932), source34edea0: debug/release/instrumentation compilation. [Unit gate41738532205](https://github.com/mrayover/practice.logger/actions/runs/41738532205), sourcedf17016: **51/51 units**, zero failures/errors/skips on unchanged data/domain code. Final80 changes only shared Block viewer sizing, measured transport height at enlarged text and version; its presentation is verified natively. Unit coverage includes13→14 and representative1→14/11→14/12→14 preservation, role/raw-write constraints, shared identity, failed replacement rollback, legacy retention, archive/reopen, master-marker deletion rollback, Block dependencies, JSON exclusions/restore and affected image/audio/recording/backup safeguards.
- [API26/100% install-over41739387573](https://github.com/mrayover/practice.logger/actions/runs/41739387573) and [API35/200% ownership41736583181](https://github.com/mrayover/practice.logger/actions/runs/41736583181), API26 ownership sourceee4c679 executes79; API35 ownership source2aca284 executes78; final80 retains their identical database/storage core: **seed, upgrade and shared-storage classes pass on each API**. Exact immutable76 code/payload is re-signed only with one disposable emulator test key, preserving every non-signature payload byte, then upgraded without clearing to79 on26 and78 on35. Final80 changes only shared visual sizing, measured enlarged-text transport height and version after that ownership core; no database/file/playback logic changes. Legacy row/byte hashes, imported MP3/M4A/WAV playback/rename/export, recorded/free takes, old visuals, canonical two-Block PDF/audio sharing/no copied bytes, rename/unlink/deletion refusal and retained master markers pass. The earlier API35 ownership run later fails its new-test sort-control lookup; the API26 ownership run is fully green.
- Final build80 editor/viewer/reopen [API35/200% 41739887086](https://github.com/mrayover/practice.logger/actions/runs/41739887086) and build79 [API26/100% 41739387573](https://github.com/mrayover/practice.logger/actions/runs/41739387573) pass **one UI workflow plus one cold-reopen each**. Both-role search/filter/sort/selection/current-selection/cancel, actual visual/audio picker imports, independent replace/remove, deliberate legacy transition, missing-original retry, portrait/landscape, canonical PDF pages during active practice/free capture, retained timer/capture, recording-sheet PDF zoom/pages/pinned Stop/review and durable links/originals are exercised. Screenshots reviewed. API26 build79 is a fully green five-class workflow; final80 changes presentation sizing at enlarged text and version, retaining the identical storage/domain/capture core. Five distinct native classes pass per API across scoped runs (**10 successful selected native executions**); no single full-matrix run claim.

Development failures/cancellations remain in WORK_LOG: one runtime-download class-setup failure (four classes,34 executed checks passed), baseline/current debug signature mismatch before seed, missing runner debug key before emulator, enlarged-text Name-chip visibility query, modal/background duplicate Show reference query and an immediate asynchronous Page2 label assertion. Live failure capture exposed an actual one-pixel PDF viewport at200% text; final80 retains a minimum180dp bitmap inside the existing scroll panel, with an explicit height check. A later active-capture screenshot/tree exposed zero Reference area at200%; final80 measures heading/capture/footer heights to cap the same scrollable transport and reserve Reference space. New bitmap/active-area assertions pass with unchanged capture/timing ownership. Corrected the runtime endpoint and explicit emulator-only signing/payload checks, then targeted lazy descendant scrolling/modal scoping/render completion waits without weakening assertions. Superseded setup/selector and interim viewport-scope runs were canceled; exact outcomes remain in WORK_LOG. The first build41734468616 passed50 units;51 adds JSON link-exclusion coverage and passes both41735971595 and41738532205. Cancellation of the interim build returned HTTP500; it completed successfully and is not classified canceled. No assertions were weakened and no unproven flake/pre-existing-defect claim is made. No full P5/F1/F2/broad lint rerun, physical-phone/vendor/provider/TalkBack or acoustic acceptance is inferred. Stage4 remains0/10 documented real sessions.

On the owner’s delivery-priority instruction, no further testing is started. Additional build80 API26 populated-link reinstall probe41740431256 was requested canceled while running; no passing result is inferred. The earlier fully green API26 build79 and final build80 API35 results remain the scoped evidence. No full regression was run.

**Exact next task: owner L3 seven-item phone verification using the linked checklist.** L1/L2 phone acceptance remains separately pending. **Next separately assigned milestone: K1 — Reference Piano Tool; T1 follows K1.** No K1/T1–T3/S0–S2 work begins.

---

## Retained L2 release — build76 historical verification

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

**Retained L2 phone action:** its separate seven-item phone verification remains pending. Current L3 delivery/next action is above. L1 phone acceptance remains separately pending; do not reopen L1 engineering or P5 without defect evidence.

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
