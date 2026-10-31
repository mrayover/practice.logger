# Current state — P4 loop recorder build51 in progress

Compact loop setup/permanent click/inline review/aligned controls implemented. Scoped compilation/unit/native checks and APK delivery pending; latest verified signed package is build50 below. No full regression/P5. Exact next task: finish loop-refinement verification and package build51.

## Previous delivery

# Current state — P4 practice interface refinement, 0.16.0/build50

Updated 2026-10-31 (America/Los_Angeles). **Signed scoped owner preview.** Compact practice metronome and workspace-to-transport switching implemented and focused automated checks passed. Physical phone acceptance remains pending; this task's final on-phone criterion is not yet established. P1–P3 generally accepted; P5 and Media Library have not started.

## Delivery

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code50 |
| Signed source | `b94c5ea6ea6fdec7e9a325d340b44792a3d03f3f` |
| Scoped preview CI | [41226905512](https://github.com/mrayover/practice.logger/actions/runs/41226905512) — completed/success |
| App | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `f023dbfb146d004b457c7169741c73607eef07b01ca626c76da3c80bbc13268a` |
| ZIP SHA256 | `27dd525f8f7ecf2c247a51115b55bf5a5d82ac243ba41e6f91a040431598e8de` |

[APK](dist/practice.logger-0.16.0-b50-b94c5ea/practice.logger-0.16.0-b50-b94c5ea.apk) · [ZIP](dist/practice.logger-0.16.0-b50-b94c5ea.zip) · [build info](dist/practice.logger-0.16.0-b50-b94c5ea/BUILD_INFO.json) · [verification status](dist/practice.logger-0.16.0-b50-b94c5ea/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b50-b94c5ea/PHONE_CHECKLIST.txt) · [latest](dist/LATEST.txt).

Publisher verified permanent signature/certificate/app/version/hash; ZIP CRC/member bytes and all 190 previous APK/ZIP files unchanged. Install over the permanent-key app **without uninstalling or clearing data**. Packages/signing material remain local and ignored. Later documentation commits follow the signed source.

## Behavior and safeguards

Practice-specific compact options combine BPM/Apply, tighten meter/spacing, and keep all three output modes on one horizontal row with Flash Only label and touch targets of at least48dp. Practice omits meter/BPM-unit and next-beat/latency explanations. Tools retains its existing presentation. Whole Start/Stop click button flashing still uses the existing consumed-audio beat and90ms decay.

Tapping either workspace header while a transport is expanded closes that panel and explicitly opens the selected workspace. Existing explicit panel-close behavior restores the earlier choice. Reference composition is retained for PDF page/zoom. Header taps change presentation state only: no metronome, BPM, timer, capture, playback or temporary-file lifecycle calls. Active capture retains pinned Stop; collapsed temporary review retains pinned Save and a button to reopen full review. Unsaved take ownership and explicit save/discard guards remain.

Cream/green/Lato, P4 audio Settings, P1 review/P2 loops/count-in, selected takes/Notes drafts and existing UI architecture remain. Audio engines/VMs, Room11/JSON7 readers1–7, schemas/migrations/IDs/history/media unchanged. JSON excludes reference/take metadata/audio. Dark mode deferred; Stage3 detailed phone acceptance pending; Stage4 remains0/10 documented sessions.

## Exact verification

- Debug/release/test APK compilation and API35 launch passed through existing owner-preview workflow. Final run passed the two PracticeRefinementPreviewTest scenarios. Existing two Tools/practice pulse scenarios passed in [prior CI41225852928](https://github.com/mrayover/practice.logger/actions/runs/41225852928), whose overall result failed at the new normal mode-text diagnostic. App source/configuration are byte-identical between those runs; only tests/workflow/records changed. Four affected scenarios passed across the two runs; no four-test final-run claim.
- Normal portrait compact panel: mode geometry on one row, all controls displayed and no internal metronome/transport scroll overflow. Workspace switching preserves running BPM90, increasing beat sequence/nonzero output samples and practice elapsed time; PDF page2/zoom survive; loop setup collapses; explicit close restores choice. Active free capture continues and pinned Stop is available. Temporary take survives collapse/reopen, remains unsaved until explicit pinned Save, then becomes a saved take.
- Narrow320dp/200% text: one-row mode bounds, text-overflow assertions and selection, reachable meter/accent/BPM controls via overflow, plus full surrounding practice start-stop/workspace/footer access. BPM/Apply stack at enlarged text. Existing Tools/practice whole-button rendered-surface/live-beat/BPM/start-stop checks pass. Screenshots reviewed; this is not whole-app accessibility coverage.
- No full unit suite, lint, API26/35 matrix, full regression, whole-app TalkBack or physical-phone install-over/acoustic/display synchronization result. Historical build47/49 results remain historical. P5/final full gates unrun and separate.

## Baseline and publication

Clean existing main6508cef matched live remote; latest scoped button CI41222926473 successful. Required records/master-plan P4 scope and mandatory efficiency procedure reviewed. Owner explicitly authorized Git commit/push in this chat. Expanded only the existing preview selector for the four affected tests; automatic broad CI skipped with `[skip ci]`. No later phase started. Actual remote SHA/clean status checked at handoff.

## One next task

Owner install-over build50 and run its PHONE_CHECKLIST on the target phone, confirming compact fit and uninterrupted audio/capture during workspace switching. Record actual phone results before calling this refinement complete. Do not start P5 or Media Library automatically.
