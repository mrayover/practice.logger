# Current state — P1 temporary take review, 0.16.0/build44

Updated 2026-10-30 (America/Los_Angeles). **Signed, installable scoped candidate; P1 engineering checks pass, owner reports P1 works; detailed phone checks and full regression pending.** P1 implements explicit temporary take review only. P2 is now owner-assigned and in implementation; candidate code45/checks/signing pending. Delivery table still identifies build44. No P3 work started.

## Delivery

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code44 |
| Signed app source | `6ebf1924e36423877f4ef77dffee93d03f8c98ab` |
| Scoped CI | [41172401080](https://github.com/mrayover/practice.logger/actions/runs/41172401080) — completed/success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `e31b68aa0cd8b82af7600901b59c2f1ee422555ca4c066d6a7e88f53dc5e7ee2` |
| ZIP SHA256 | `8d543ebcd24f1ea9ad9b580a276e992025ccb60d9e73d6394e627323a9624cd9` |

[APK](dist/practice.logger-0.16.0-b44-6ebf192/practice.logger-0.16.0-b44-6ebf192.apk) · [ZIP](dist/practice.logger-0.16.0-b44-6ebf192.zip) · [build info](dist/practice.logger-0.16.0-b44-6ebf192/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b44-6ebf192/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b44-6ebf192/PHONE_CHECKLIST.txt) · [latest](dist/LATEST.txt).

Existing permanent publisher verified signature, pinned certificate, stable package/version and APK hash. Final ZIP CRC/all-member byte checks pass. All 162 pre-P1 APK/ZIP files remain byte-identical; early signed build43 is retained. Install over the permanent-key app **without uninstalling or clearing data**. Signed distributions/signing material stay local and Git-ignored. Later test/documentation commits may follow the signed source; app-source equivalence is checked at closeout.

## P1 behavior and safeguards

- Free recording Stop opens temporary Review Take with Play/Stop Preview, Save, Re-record and Discard. Same controls in Focused Practice, saved-Block recording management and standalone Free Recording; no synthetic Block/session/Goal is created.
- Only successful Save renames/commits permanent take metadata. Before that the pending file is protected from live orphan cleanup and absent from saved listings. Failed database commit rolls bytes back to the pending name for retry; existing saved recordings remain intact. Re-record replaces only temporary audio through existing mic/foreground rules; Discard removes only the temporary file.
- Completed PCM16 WAV phrases also await review; no automatic saving or playback. Incomplete phrases remain rejected/discarded. Preset beats/four-click count-in remain; P2 is not implemented. Free audio remains AAC/M4A.
- Background stops capture/playback; returning never auto-starts/resumes or auto-saves. Valid free audio may await review within the process; incomplete loops are discarded. Review is temporary: process loss removes unsaved audio on cold recovery. Explicitly Save valuable takes.
- Active Stop/Cancel and sheet review controls remain outside scrolling references. Focus review temporarily uses the existing transport area and restores Notes draft/PDF page afterward; practice timer is independent. Session completion/exit waits for recording/review resolution. Management closing offers Return to review or deliberate discard.

Existing build42 Home-button removal/banner navigation, build41 workspace/footer and timestamped Notes remain. Room11/JSON7 readers1–7, schemas/migrations1–11, existing IDs/media/session history, permanent app ID/key and prior distributions are preserved. JSON still excludes image/PDF references and all take metadata/labels/audio; per-take export remains available. No Library, full media backup, custom loop length/count-in, meter, Settings or Goal/statistics extension was added.

## Exact verification and limits

- **12 scoped JVM/Robolectric tests, 0 failures/errors/skips:** TakeReviewTest4, RecordingRepositoryTest4, StandaloneRecordingTest2 and PhraseRepositoryTest2. Covers temporary visibility/discard/live-orphan protection, retry after injected commit failure, incomplete-loop rejection, byte preservation, existing repository/export/migration safety and no synthetic sessions.
- Debug/release/test APK compilation passed. Three API35 TakeReviewWorkflowTest scenarios passed: Focused Practice free review/preview/re-record/save/discard with running timer, Notes draft and PDF page2 restoration; saved Block free and completed-loop review/save/native playback/cancel; standalone save failure/retry, background playback/capture stop, discard guard and unchanged existing saved rows/bytes/Blocks/sessions.
- Screenshots of focus review/restored content, loop preview and failed-save retry inspected. Permanent signing/identity/version/hash, ZIP CRC/member round trip and prior distribution hashes verified.
- First run41171943218/sourceb310619 passed12 storage tests, compilation and2/3 native scenarios. The remaining scenario launched a second activity on return (logs show nonmatching launcher intent), so it could not see the original review. Corrected test task launch and UI wait; screenshot review separately exposed a Windows-default encoding mistake in RecordingSheet. UTF-8 corrected and version advanced44; build43 retained. This final scoped run verifies the corrections. Directly affected older native tests now make explicit review acceptance and compile, but their broader historical UI workflows were not run or investigated.

**Not run:** full unit suite, lint, API26/full API26–35 regression matrix, dedicated200%-text/landscape/TalkBack, permanent-key phone install-over or physical mic/speaker/acoustic timing tests. P5 pre-Library and final F1–F2 full-regression/phone gates remain mandatory. Stage3 detailed phone acceptance remains pending; Stage4 remains0/10 documented real sessions. Emulator capture/playback is not physical-phone acceptance.

## Baseline and publication

P1 began on clean existing Windows main `b2a97211f629cbc98cee1d738f81ae4dd7020747`, confirmed against remote; build42 source was cafa1bf and latest preview41145916544 was successful. P0 master-plan integration remains complete. The [V1 master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md) governs forward work. P1 source, tests and records use `[skip ci]` with a dedicated manually dispatched scoped workflow; no automatic full build or regression was dispatched. Sources references remained read-only. The closing handoff reports final confirmed remote main SHA, distinct from the signed source above.

## P2 implementation status

Owner generally accepted P1 and assigned P2. Source now supports custom1–64 loop beats, per-loop positive count-in and persistent default in minimal Audio Settings. Count-in/guide stream in bounded chunks on the existing output sample clock; WAV IO is chunked and validation avoids full PCM allocation. Code45 verification/signing pending; no new delivered package is claimed yet. P1 detailed physical checks and full regression remain separate.

## One next task

Complete P2 scoped verification and signed delivery; do not start P3 automatically. Full regression remains at P5 and final V1.
