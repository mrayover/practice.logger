# Current state — P4 metronome button feedback, 0.16.0/build49

Updated 2026-10-31 (America/Los_Angeles). **Signed scoped owner preview.** Both Stop click buttons now pulse across their whole surface on flash-enabled beats; circular indicators removed. Existing sample clock, audio and 90ms decay reused. P1–P3 generally owner-accepted; P4 physical acceptance pending. P5 has not started; no separate milestone or regression gate added.

## Delivery

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code49 |
| Signed source | `858dabf0f3a855177274ce3d87b3741b77c8ae04` |
| Scoped CI | [41222926473](https://github.com/mrayover/practice.logger/actions/runs/41222926473) — completed/success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `446503d54fca92aec856177762a662c00a75852f26496d81ec540d24bca11937` |
| ZIP SHA256 | `eebb9f55c8d1e299a887f10907c937ee089c1efa36bb1451e92cccce83571288` |

[APK](dist/practice.logger-0.16.0-b49-858dabf/practice.logger-0.16.0-b49-858dabf.apk) · [ZIP](dist/practice.logger-0.16.0-b49-858dabf.zip) · [build info](dist/practice.logger-0.16.0-b49-858dabf/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b49-858dabf/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b49-858dabf/PHONE_CHECKLIST.txt) · [latest](dist/LATEST.txt).

Publisher verifies permanent signature/certificate/app/version/hash; ZIP CRC/member bytes and all 186 prior APK/ZIP files unchanged. Install over the permanent-key app **without uninstalling or clearing data**. Key/packages remain local and ignored by Git. Later verification documentation follows the signed source; actual remote SHA verified at session close.

## Current behavior and safeguards

Standalone Stop click alternates green/white and cream/green; outlined practice Stop click fills green/white then returns transparent over the existing tinted transport. Shared button colors use `running && pulse.lit`; explicit stop immediately removes the highlight. Existing output-mode behavior remains: Sound only suppresses flash; Flash only pulses silently. No audio/VM, independent timer, button geometry, navigation, BPM, meter, accent or transport redesign. HomeButton adds a styling overload; other callers keep their existing defaults.

P4 preferences/timbres/completion safeguards, P1 review/P2 loops/count-in, compact panels/Notes/reference retention and cream/green/Lato remain. Room11/JSON7 readers1–7/schema/migrations/IDs/media/history unchanged; JSON excludes reference/take metadata/audio. Dark mode remains deferred. Stage3 detailed phone acceptance pending; Stage4 remains0/10 documented sessions.

## Exact verification

- Build49 debug/release/test APK compilation, one API35 install/launch and two focused native button scenarios pass. Tests inspect rendered surface pixels over successive live beats and between beats, readable Stop click text, immediate stopped appearance plus a later static sample, repeated start/stop and BPM +1 for both interfaces. Native nonzero click PCM confirmed. Button screenshots reviewed.
- Relevant initial verification failures/repairs, if any, are recorded in WORK_LOG. Historical build47 scoped22 JVM/10 API35 Settings/transport checks and build48 Home preview remain historical, not rerun here.
- Scope/diff/document links/source equivalence and signed-package integrity checked. No full unit suite/lint/API26–35 matrix, broad regression/release gate, whole-app accessibility or physical-phone install-over/acoustic/display-latency acceptance. P5/final gates retained, not started.

## Baseline and publication

Existing clean main36682c3 matched live remote; latest scoped CI41180090915 successful. Mandatory efficiency procedure and P4 master-plan scope reviewed. Owner explicitly approved commit/push, focused CI and signed delivery. Existing preview workflow now accepts a metronome test selector; default Home check preserved. Automatic broad CI skipped with `[skip ci]`. No owner-data edits or later phase.

## One next task

Owner install-over and **build49 PHONE_CHECKLIST.txt**, checking both pulses/audio synchronization and remaining P4 acceptance. P5 is a separate recommended assignment after acceptance; do not start automatically. L1 remains behind P5.
