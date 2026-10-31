# Current state — P4 automatic loop audition, 0.16.0/build53

Updated 2026-10-31 (America/Los_Angeles). **Signed scoped debug preview.** Owner accepted build52 appearance as visually perfect and requested automatic playback after loop capture to judge Keep/Re-record. Build53 automatically auditions a successfully completed foreground loop while it is still temporary. Layout source is unchanged. Focused verification passes; owner audible-playback confirmation remains pending. P5 and Media Library have not started.

## Delivery

| Item | Verified value |
|---|---|
| Version / variant | 0.16.0 / code53 / debug |
| APK source | `15af3ea7f49bb0ab565b3ccd1addd3e16c99d125` |
| Focused CI | [41235843607](https://github.com/mrayover/practice.logger/actions/runs/41235843607) — completed/success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `d0b27896422d6265b115478a05610b3e97cf74c8dee2cf2edd7d06cc9e4a9dad` |
| ZIP SHA256 | `ee26bfd6db1bde2616399355b34b3b1da3f95620a15d4a371523c804c64f59e8` |

[Debug APK](dist/practice.logger-0.16.0-b53-15af3ea/practice.logger-0.16.0-b53-15af3ea.apk) · [ZIP](dist/practice.logger-0.16.0-b53-15af3ea.zip) · [Build info](dist/practice.logger-0.16.0-b53-15af3ea/BUILD_INFO.json) · [Verification](dist/practice.logger-0.16.0-b53-15af3ea/VERIFICATION_STATUS.txt) · [Phone checklist](dist/practice.logger-0.16.0-b53-15af3ea/PHONE_CHECKLIST.txt) · [Latest](dist/LATEST.txt).

The existing publisher re-signed the tested CI debug variant with the permanent key; non-signature app members match. App/version/debuggable flag, signature/certificate and hashes verified; ZIP CRC/member bytes checked; all 202 previous APK/ZIP hashes preserved. Install over the permanent-key app **without uninstalling or clearing data**. Packages/keys remain local and ignored. Later documentation commits follow APK source.

## Behavior and safeguards

The investigation found no automatic preview invocation: capture completion previously created only review state. One call to the existing previewReview/play path now follows successful capture cleanup and audio-focus release. It runs from that completion event, with matching generation, foreground, recording-screen and Block guards, so cancelled/incomplete/background/stale captures do not start playback. UI recomposition or returning from background never restarts it.

The temporary WAV loops for audition without creating a saved take. Stop/manual Preview remains available. Re-record stops/deletes the prior temporary take and auditions its successful replacement. Keep explicitly saves and stops; Discard stops and removes only temporary audio. Background stops preview and retains in-process review without saving/restarting. Free recording remains manual preview. Existing failure messages/manual retry and saved-file ownership safeguards remain.

Accepted build52 fields/proportional spacing/footer, permanent phrase click/count-in, transport alignment and Notes/PDF/workspace guards remain. RecordingSheet.kt and PracticeScreens.kt match accepted build52 source exactly. No sample engine, capture timing, Room11/JSON7/readers1–7, IDs/history/media or saved audio changes. JSON still excludes references/take metadata/audio; dark mode deferred.

## Exact verification and limits

- Final CI compiles debug/release/instrumentation APKs and passes seven existing phrase timing/storage unit tests plus two API35 native scenarios.
- Automatic preview is verified by the actual native loop playback counter crossing a complete phrase with no Preview tap and no saved row. Native tests exercise Stop/manual replay, stopped review collapse/reopen, Re-record/new automatic audition, Keep/Discard and saved-take preservation, and free review remaining manual.
- Count-in/partial capture cancellation produces no review/playback. Real Android Home/return during count-in cancels without restart; Home during audition stops it, retains temporary review, stays quiet on return and permits manual replay. Existing compact/tall numeric/spacing/footer/action geometry remains passing; screenshots inspected.
- Other affected legacy review expectations were updated/compiled, not executed as broader workflows. No full regression, lint/API matrix or owner physical acoustic/routing acceptance is claimed. P1–P4 historical evidence remains in WORK_LOG; P5/final gates remain unassigned/unrun. Stage3 detailed phone checks pending; Stage4 remains0/10 documented sessions.

## Exact next task

Owner install-over build53 and confirm immediate audible loop audition after successful capture, explicit Keep/Re-record/Discard behavior, and stopped playback/quiet return after backgrounding on the same phone.
