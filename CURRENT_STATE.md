# Current state — P2 flexible phrase loops, 0.16.0/build45

Updated 2026-10-30 (America/Los_Angeles). **Signed installable scoped candidate; P2 engineering checks pass, P2 phone acceptance/full regression pending.** Owner explicitly reports P1 works; record general P1 acceptance only. Owner generally accepts P2 and assigns P3. P3 source/code46 is in progress; verification/signing pending. Delivery table remains build45. No P4 started.

## Delivery

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code45 |
| Signed source | `feca6767c83c34580ccee7e7d07037839b535c9a` |
| Build/storage/API35 CI | [41174052523](https://github.com/mrayover/practice.logger/actions/runs/41174052523) —13 tests/build/API35 pass; overall failed before API26 instrumentation (optional log clearing) |
| Isolated API26 playback | [41175096904](https://github.com/mrayover/practice.logger/actions/runs/41175096904) — completed/success; app/test source unchanged |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `6e87ca8b854b34c42e36542bdeef9ee8cf9cbf81228ac85f69c695deb06b0099` |
| ZIP SHA256 | `db519cf0ea8328abf59c4ed6973c091098b8c6c257c2a548f1fd008565571cdb` |

[APK](dist/practice.logger-0.16.0-b45-feca676/practice.logger-0.16.0-b45-feca676.apk) · [ZIP](dist/practice.logger-0.16.0-b45-feca676.zip) · [build info](dist/practice.logger-0.16.0-b45-feca676/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b45-feca676/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b45-feca676/PHONE_CHECKLIST.txt) · [latest](dist/LATEST.txt).

Existing publisher verifies signature/pinned certificate/stable identity/version/hash. Final ZIP CRC/all-member bytes verified; all 170 pre-P2 APK/ZIP files retained byte-identical, including build43/44. Install OVER the permanent-key app without uninstalling/clearing data. Packages/key remain local and ignored. Later records follow signed source; product-source equivalence is verified at closeout.

## Implemented P2 behavior

- Loop setup accepts any whole 1–64 beats;4/8/16/32 remain shortcuts. BPM stays30–300.128 beats, meter-aware bar entry and P3 meter/flash are absent.
- Count-in accepts positive whole beats, default 4. Tools → Audio Settings saves a shared default across restarts; per-loop input overrides it without changing the default. Other P4 settings are not implemented. A 15-minute execution budget at chosen BPM rejects excessive/overflow requests with a meaningful resource message; count-in is not a preset list.
- Count-in/optional capture guide stream from bounded buffers using the existing output sample clock and absolute rounded beat boundaries. Mic starts after the selected count-in; WAV has only the configured loop frames. UI displays beat N of total. Countdown/guide/recording cancel and foreground guards remain.
- PCM16 WAV IO uses8KiB byte chunks and header-only validation, avoiding duplicate whole-file/guide allocations. Maximum 30BPMx64 is6,144,000 frames,128seconds,12,288,044 WAV bytes. Native static playback has been checked through the full repeated boundary on API26 and35.
- P1 Review Take remains shared: preview/Save/Re-record/Discard; no permanent take until Save succeeds, no automatic loop playback/save, safe retry on failed commit, no incomplete-loop acceptance and no background restart. Free captures remain AAC/M4A.

P1 is generally owner-accepted from explicit “works” feedback, not an invented checklist. Existing Notes/PDF/timer/session behavior, stable IDs/data/media, app ID/key and prior packages are retained. Room11/JSON7 readers1–7 and schema/migration history unchanged. JSON still excludes reference files/metadata and take labels/metadata/audio; no full-media recovery is claimed. Existing single references/Goals/Routines/Log/Tools and build42 navigation remain.

## Exact scoped verification

- 13 JVM/Robolectric tests: PhraseAudioTest (5), FlexiblePhraseStorageTest (2), PhraseRepositoryTest (2), TakeReviewTest (4); zero failures/errors/skips. All supported BPM/loop frame boundaries, custom count-in/guide ticks, chunk boundaries,15-minute/overflow guards, maximum WAV round-trip/corruption, low-space preflight, preference persistence and existing review/storage safety checked.
- Debug/release/test APK compilation passes.
- API35: 7 native tests pass (FlexiblePhraseWorkflowTest (3), LongPhrasePlaybackTest (1), TakeReviewWorkflowTest (3)). Custom3/5/7/11 loop lengths and count-ins, tempo edges, persisted default/recreation/per-loop UI validation, guide clock during capture, count-in excluded from WAV, cancellation/background, P1 failed-save/re-record/discard/old-take preservation and Notes/PDF/timer continuity checked. Real30BPMx64 capture produces the exact 128-second WAV; native playback head crosses the full loop boundary.
- API26: 1 scoped LongPhrasePlaybackTest passes; maximum 128-second WAV loads and actual AudioTrack playback crosses its full loop boundary. This is scoped audio verification, not a full API26/API35 regression matrix.
- First run41174052523 reached API26 installs, then optional `adb logcat -c` failed before instrumentation. No API26 audio failure was established. Corrected only log housekeeping and ran isolated API26 compilation/playback at 41175096904; existing signed app/test sources and passing unit/API35 results were retained. No full pipeline or API35 repeat.
- Screenshots inspected; permanent-key signature/certificate/app/version/APK hash, ZIP CRC/member integrity and all prior package hashes verified. Frame and byte arithmetic remains bounded and file validation precedes full sample allocation.

**Not run:** full unit suite/lint/regression matrix; dedicated200%-text/landscape/TalkBack; physical-phone permanent-key install-over, mic/speaker/interface timing/onset/loop-boundary acceptance.15-minute count-in generation/resource boundaries are tested in chunks; no claim of a 15-minute real-device count-in run. P5 pre-Library and final V1 full gates remain mandatory. Stage3 detailed phone acceptance and Stage4 0/10 documented real sessions remain unchanged.

## Baseline and publication

P2 began on clean existing local/remote main `9fe6fbc61b5709b601e5f2bffbeff64e684a6345`; latest scoped P1 run41172401080 successful. Owner generally accepted P1 and assigned only P2. Mandatory efficiency procedure reviewed; existing architecture reused. [V1 master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md) remains the roadmap. Source/records use `[skip ci]`; only the explicit P2 scoped and isolated API26 manual workflows were dispatched. Synced sources read-only; no owner app installed/cleared. Final remote main SHA is confirmed in the closing handoff, distinct from signed source.

## P3 implementation status

General P2 acceptance recorded from owner feedback. P3 source adds shared meter/accent/mode controls, a sample-consumption-derived flash, running compact meter and exclusive expanded practice panel. No new signed delivery yet; code46 scoped checks/signing pending. P2 recording/count-in sources, Room11 and JSON7 are unchanged.

## One next task

Complete P3 scoped verification and permanent-key signed delivery. Do not start P4 automatically; full regression remains at P5/final.
