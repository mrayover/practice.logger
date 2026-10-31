# Current state — P4 audio Settings, 0.16.0/build47

Updated 2026-10-31 (America/Los_Angeles). **Signed installable scoped candidate; P4 engineering checks pass, physical acceptance and full regression pending.** Owner generally accepts P1/P2/P3 (“works”); individual phone checklist results are not inferred. P5 has not started; no automatic follow-up scheduled.

## Delivery

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code47 |
| Signed source | `4341af132882e26e24cc3fc85be8d289977b0e6e` |
| Scoped CI | [41178720243](https://github.com/mrayover/practice.logger/actions/runs/41178720243) — completed/success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `651dbaf29e5dac33c1730fbe849427068968ff5c02d2fa0ccbbca8860dcb4370` |
| ZIP SHA256 | `46ccabd1d697aaa83514a5c015f18b239b421f52bbf8cb7918aab313643ad4c8` |

[APK](dist/practice.logger-0.16.0-b47-4341af1/practice.logger-0.16.0-b47-4341af1.apk) · [ZIP](dist/practice.logger-0.16.0-b47-4341af1.zip) · [build info](dist/practice.logger-0.16.0-b47-4341af1/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b47-4341af1/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b47-4341af1/PHONE_CHECKLIST.txt) · [latest](dist/LATEST.txt).

Existing publisher verifies signature, pinned certificate, stable app/version and APK hash. ZIP CRC/all-member bytes verified; all 178 pre-P4 APK/ZIP files retained byte-identical. Install over permanent-key app **without uninstalling or clearing data**. Key/packages stay local and Git-ignored. Later documentation follows signed source; app/test equivalence checked at closeout.

## Implemented P4 behavior

- Tools → Audio Settings persists Classic/Woodblock/Soft tick/Electronic pulse and Sound + Flash/Sound only/Flash only defaults. Both metronomes observe changes; meter/accent/timbre/output apply at next beat without fractional-phase or practice-clock reset. Meter/accent remain runtime choices. /8 BPM still counts each eighth; flash uses consumed sample boundaries, with Android output/display latency uncalibrated.
- Shared completion choice: two-tone, bell, rising chime, short beep, Off. Block alarm retains committed/armed/foreground crossing and independent timer stop; standalone countdown now alarms once on its running foreground zero crossing. Off and active capture suppress cue output; no auto-advance/replay. Cue mixing ducks clicks without changing beat phase. Settings previews stop on leave/background and refuse active capture/take playback.
- Existing positive count-in default4/per-loop override and explicit recording click On/Off retained. Settings has visible overflow thumb, safe/keyboard insets and vertical choices. Existing compact transport, exclusive panels, retained PDF page/zoom/Notes drafts, immediate note history/edit and pinned controls reused. No visual redesign; cream/green/Lato retained. Optional dark mode explicitly deferred because hard-coded screen colors require broader centralization.
- Room11/JSON7 readers1–7, schema/migrations, IDs/media/history and app identity/key retained. JSON still excludes reference/take metadata/audio. No Library/storage/analytics work. Stage3 detailed phone acceptance remains pending; Stage4 remains0/10 documented real sessions.

## Exact scoped verification

- 22 JVM/Robolectric tests, zero failures/errors/skips: AudioSettingsPcmTest2, MeterPcmTest6, BeatPcmTest2, CompletionPcmTest1, MetronomePersistenceTest2, PhraseAudioTest5, TakeReviewTest4. Distinct timbres/accents/alarms, zero silent PCM/Off, bounds, phase/chunk continuity, BPM history/backup/migration and P1/P2 compatibility checked.
- Debug/release/test APK compilation passes. API35: ten native tests (AudioSettingsWorkflowTest2, NotesHistoryPreviewTest1, MeterWorkflowTest3, MetronomeOutputTest1, TakeReviewWorkflowTest3) pass. Preference new readers/activity recreation/shared defaults, preview controls, Settings200%-text/landscape/overflow, deterministic foreground countdown one-shot/Off/background and actual alarm PCM covered. Existing note append/edit/history/draft guards, retained PDF/Notes/timer, free capture/phrase count-in and review/failure/re-record/discard checks retained.
- Settings screenshots inspected; signature/certificate/app/version/hash, ZIP CRC/member bytes and prior package hashes verified. P3’s previous20 scoped tests,7 API35 and1 API26 probe remain historical evidence in WORK_LOG; this phase does not rerun full API26 matrix.

**Not run:** full unit suite/lint/API26–35 regression matrix, whole-app200%-text/landscape/TalkBack, physical-phone permanent-key install-over, speaker/headphone/mic/flash acoustic acceptance. Settings activity recreation/new readers plus synchronous preference commits checked; a forced-process-kill phone checklist remains. P5 pre-Library and final V1 full gates are mandatory. P4 narrow UI checks do not establish all-screen accessibility acceptance.

## Baseline and publication

P4 began on clean existing local/remote main `c702a6c1667239d8bd404db9b2538ecb06f57905`; latest P3 CI41176982100 completed/success. Mandatory efficiency procedure reviewed; [V1 master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md) unchanged. Owner authorized bounded progress with usage monitoring. Automatic broad CI skipped with `[skip ci]`; only manual phase-scoped workflow dispatched. No unrelated historical failure investigation, uninstall/clear or owner-data edits. Final remote main SHA is confirmed in chat after publication.

## One next task

Owner install-over and **P4 PHONE_CHECKLIST.txt on build47**. P5 full pre-Library regression is the next recommended engineering assignment after acceptance; do not start it automatically. L1 remains behind P5.
