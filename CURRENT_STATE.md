# Current state — Assignment 1 owner preview 0.16.0/build38

Updated 2026-10-30 (America/Los_Angeles). **Signed, installable limited owner preview. Full regression verification pending.** Assignment 1 only; later recording acceptance and multiple-note history are not implemented. No further automated work is scheduled.

## Delivery and provenance

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code38 |
| Signed app source | `a9e438603811e4137016af438fa15eae4b6540a2` |
| Limited preview run | [41110598208](https://github.com/mrayover/practice.logger/actions/runs/41110598208) |
| App identity | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `88994562827508cd1fc8ec4b733b05002693f468dbfd1204a5b8085ab04162cd` |
| ZIP SHA256 | `f92bdb40d67fa4b18711291ba55a33c12ffc2078c3018d44fbc72bfb81501636` |

[Installable APK](dist/practice.logger-0.16.0-b38-a9e4386/practice.logger-0.16.0-b38-a9e4386.apk) · [ZIP](dist/practice.logger-0.16.0-b38-a9e4386.zip) · [build info](dist/practice.logger-0.16.0-b38-a9e4386/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b38-a9e4386/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b38-a9e4386/PHONE_CHECKLIST.txt) · [latest pointer](dist/LATEST.txt).

Install over the existing permanent-key app without uninstalling or clearing data. Existing publisher verifies the pinned certificate, cryptographic signature, stable app ID and version38. Final ZIP CRC/member round trip and all 142 prior APK/ZIP hashes pass. Packages are ignored local artifacts. A documentation-only commit follows the signed source; it does not change the app.

## Assignment 1 changes

Focused Practice retains banner, Block identity/timer and primary actions. The metronome and compact free/loop rows now occupy a pinned transport outside reference scrolling. Each row has a dropdown of this Block's saved recordings, readable name/date (loop BPM/beats), Record and selected playback/Stop. Selection starts at latest, survives panel/reference toggles during this visit and is never persisted. Earlier takes are retained. Existing automatic loop playback can be stopped from the loop row even if an older loop remains selected.

Notes and loop setup expand inline above the controls; one local panel flag makes them mutually exclusive. The weighted workspace contracts/expands as panel height changes. Panel/transport scrolling and wrapping controls provide reachability on shorter displays and enlarged text; primary and active recording actions stay outside that scroll. Reference collapse/image/PDF viewer reuse is retained.

Loop setup reuses the management dialog's form, BPM30–300, 4/8/16/32 beats, optional recording click, validation and four-click count-in. Record collapses the panel and calls the existing capture/automatic-save/autoplay path; count-in/capture status remains in the transport. Notes reuses the single Block-session placement note; Save and return closes after a successful saved value arrives. No new note history, recording lifecycle, storage/schema, media engine or navigation architecture. Daily Log is unchanged. Full Recordings navigation is removed only from Focused Practice; management and main Recording remain.

RecordingViewModel enterBlock/leaveBlock remain owned by the original focus DisposableEffect keyed to placement/VM, outside panel composition. Panel/reference state cannot restart that effect. Starting capture still stops playback; simultaneous loop playback/free capture is excluded. The independent click retains its established start/playback behavior.

## Verification actually performed

- Debug/release APK and instrumentation APK compilation pass in the limited preview run above.
- One clean API35 debug emulator install/start, live process, visible app UI and empty app crash window pass.
- Exactly one TransportPreviewTest scenario passes: PDF opens, transport remains reachable, Notes/loop panel exclusivity and workspace contraction, timer/click continue across toggles, note saves while free capture continues, two real free takes and two real loops save, old free/loop selection/playback works, loop playback survives Notes toggles, placement identity and all four saved takes remain.
- Existing permanent-key publisher and archive/hash audit pass. Git whitespace/source scope checks pass. Previous distribution files are preserved.

No full unit/lint/regression/instrumentation suite/API matrix, historical failure investigation, dedicated enlarged-text/landscape/TalkBack or permission/failure injection was run. The smoke/transport tests use debug signing. Physical-phone signed APK install-over, retained-data acceptance, layout/keyboard accessibility and acoustic recording/click/loop quality remain owner checks. Full release rules remain intact; prior incomplete build34 regression records remain in WORK_LOG. Room10/migrations1–10 and JSON6/readers1–6 are unchanged; JSON still excludes reference/take media and metadata. Stage3 phone acceptance pending, Stage4 remains0/10 real sessions.

## One next task

Owner: install build38 in place and run the short Assignment 1 checklist in MANUAL_TESTS.md or the package PHONE_CHECKLIST.txt. Report transport/reference sizing, selection, recording and note behavior. Stop at Assignment 1; no later assignment begins automatically.
