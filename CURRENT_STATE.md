# Current state — P3 metronome, 0.16.0/build46

Updated 2026-10-30 (America/Los_Angeles). **Signed installable scoped candidate; P3 engineering checks pass, P3 phone acceptance and full regression pending.** Owner generally accepts P1/P2 (“works”); individual phone checklist results are not inferred. P4 has not started; no automatic follow-up scheduled.

## Delivery

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code46 |
| Signed source | `09e16179750a79705a92e1b1bca18096567e1a98` |
| Scoped CI | [41176982100](https://github.com/mrayover/practice.logger/actions/runs/41176982100) — completed/success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `9984fcb6a499e1e94188030fa13fccc5c094f97a8a18a7a3b4a9499f4bc7b234` |
| ZIP SHA256 | `76e0a5d502956a20f804237c8bfcd1e888bd26347c44e303b3cd9db7c92449a0` |

[APK](dist/practice.logger-0.16.0-b46-09e1617/practice.logger-0.16.0-b46-09e1617.apk) · [ZIP](dist/practice.logger-0.16.0-b46-09e1617.zip) · [build info](dist/practice.logger-0.16.0-b46-09e1617/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b46-09e1617/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b46-09e1617/PHONE_CHECKLIST.txt) · [latest](dist/LATEST.txt).

Existing publisher verifies signature, pinned certificate, stable app/version and APK hash. ZIP CRC/all-member bytes verified; all 174 pre-P3 APK/ZIP files retained byte-identical. Install over the permanent-key app **without uninstalling or clearing data**. Packages/key remain local and Git-ignored. Later documentation follows signed source; product/test source equivalence is checked at closeout.

## Implemented P3 behavior

- Compact practice BPM/−1/+1/Start–Stop gains a chevron, small beat pulse and running-only meter. Expansion uses the existing exclusive transport context; collapsing restores Description/Reference, PDF page/zoom and Notes drafts without changing practice timing or recorder ownership.
- Shared practice/Tools controls expose 2/4, 3/4, 4/4, 5/4, 6/8, 7/8, Accent first beat (Off default), Sound + Flash, Sound only and Flash only / silent. /8 BPM counts each eighth (6 or7 clicks per bar), not dotted-quarter/grouped pulse. BPM stays30–300.
- Existing BeatPcm fractional sample phase drives sound and bar counts even when accent is Off or sound is silent. Meter/accent/output changes apply at the next beat; meter changes start a new bar there. Tempo changes preserve phase. Beat/flash callbacks are delivered after AudioTrack consumes each beat boundary;90ms decay is a single-pulse timer, not a second clock. Android display/speaker latency is not calibrated.
- Flash-only uses zero click PCM on the same foreground audio clock. Completion cue stays separate, retains its one-shot/ducking rules, and does not reset click phase or timing. Stop/background/focus loss clears audio/pulse and never auto-restarts.
- Current runtime controls are implemented; P4 persistent global mode/timbre/alarm settings are not. P2 phrase/count-in, P1 review and saved data remain. Practice click cannot start during active phrase capture; metronome meter never changes loop beats/count-in.

Room11/JSON7 readers1–7, schemas/migrations, stable IDs/media/history, app ID/key and prior distributions retained. JSON still excludes references/take metadata/audio; no Library/full-media backup claim. P1/P2 generally owner-accepted; Stage3 detailed phone acceptance and Stage4 0/10 documented sessions remain unchanged.

## Exact scoped verification

- 20 JVM/Robolectric tests, zero failures/errors/skips: MeterPcmTest6, BeatPcmTest2, CompletionPcmTest1, MetronomePersistenceTest2, PhraseAudioTest5, TakeReviewTest4. All six bar cycles, straight/accented PCM, zero silent PCM, mode parity, next-beat config boundary, fractional tempo continuity, chunk invariance, completion mixing, BPM history/migration/backup and P1/P2 audio/storage compatibility checked.
- Debug/release/test APK compilation passes. API35: seven native tests (MeterWorkflowTest3, MetronomeOutputTest1, TakeReviewWorkflowTest3) pass. Compact/expanded pulse,3/4,5/4,6/8,7/8 controls, silent native output, sound/mode transitions, Tools parity/background, PDF page/zoom/Notes draft/timer retention, active free capture and unchanged P2 phrase count-in, P1 review/failure/re-record/discard checked.
- API26: one native MetronomeOutputTest passes; consumed-clock pulse/bar events, actual zero click PCM then audible output on the same engine, tempo/meter change without restart, and stop cleanup verified. Scoped audio probe only, not full regression.
- Final screenshots inspected; permanent signature/certificate/app/version/hash, ZIP CRC/member bytes and prior package hashes verified. Initial run41176691058/source044ce6b stopped at Kotlin compilation because explicit Tools setBpm collided with its generated property setter. Renamed method/call site to applyMetronomeBpm; no signed artifact or native result from that failed attempt is claimed.

**Not run:** full unit suite/lint/API26–35 regression matrix, dedicated200%-text/landscape/TalkBack, permanent-key physical-phone install-over, speaker/headphone/mic/flash/acoustic latency acceptance. P5 pre-Library and final V1 full gates remain mandatory. Runtime P3 choices are not claimed persisted across process restart; P4 owns those defaults.

## Baseline and publication

P3 began on clean existing local/remote main `76be3429d9600a3a78791f364aa1dd7e70828b93`. Owner generally accepted P2 and assigned P3 only. Mandatory efficiency procedure reviewed; [V1 master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md) remains the forward roadmap. Only scoped manual P3 workflows dispatched; source/records use `[skip ci]`. P2 recording/count-in sources and full-regression workflow unchanged; synced sources read-only. Final remote main SHA is confirmed in the closing handoff, distinct from the signed source.

## One next task

Owner: install build46 in place and complete its P3 PHONE_CHECKLIST.txt for real click/accent/flash and retained data. P4 remains unassigned; do not start automatically.
