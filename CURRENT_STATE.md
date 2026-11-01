# Current state — P4 targeted UI and historical Notes / build57

## Delivery

Signed release **0.16.0/build57**, app source `e5010fec8795b22e37d8fabc9f8344aebe219536`. Later commits change only the targeted diagnostic/records. [APK](dist/practice.logger-0.16.0-b57-e5010fe/practice.logger-0.16.0-b57-e5010fe.apk) · [ZIP](dist/practice.logger-0.16.0-b57-e5010fe.zip) · [Build info](dist/practice.logger-0.16.0-b57-e5010fe/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.16.0-b57-e5010fe/PHONE_CHECKLIST.txt). Install over the permanent-key app without uninstalling or clearing data. Previous distributions remain preserved and ignored.

APK SHA256 `f4d383a7d9ebaaff0727f758c8562724f110e74f756aba6a2c770dc9afc0ddd0`. ZIP SHA256 `93cf9c51166cd8e18c584b9b2ab96b50e58009f883d0e0a56154caf0fc7fcc3d`. Existing publisher verified application ID, version57 and the pinned permanent certificate/signature; ZIP CRC and every packaged file match the distribution folder.

## Assigned scope

Selected chips retain visible outlines in Blocks/Goals/Tools/Settings; compact practice selection styling remains. Recent Practice and Block Library use shared pills with a separate body suitable for future end caps, without Goal caps. Home Library opens only Coming soon. The shared far-right Settings gear uses the existing settings screen and save/discard/exit guards. Settings remains available from Tools.

Focused Practice fills Notes/Skip, shows BPM/meter, inline Accent checkbox, aligned chevrons, −1/+1 and Start/Stop click. Existing metronome configuration/audio/pulse APIs, recording action columns and timer behavior remain. Compact reference reset overlay is removed; pinch/pan and an accessible Reset zoom action remain. Larger reference viewers retain their prior reset controls.

Session Summary previously read only the legacy placement note, although Save Note persists new timestamped entries in practice_notes. It now reads the existing history by exact session/placement/Block and reuses the same expand/edit/save/cancel controls. No schema, migration, backup format or audio engine change.

## Targeted evidence and limits

[Build56 CI41255636933](https://github.com/mrayover/practice.logger/actions/runs/41255636933) passes debug/release/instrumentation compilation and **14 scoped unit tests**: Notes4, BeatPcm2, metronome persistence2 and meter PCM6. Notes tests complete a session, close/reopen the database, exclude another session on the same Block, edit saved text and reopen again; legacy/new entries, timestamps and timing remain intact.

Three build56 API35 native scenarios pass: Library/header Settings/navigation/selection, historical Notes display/edit/recreation and Light/Dark practice whole-button pulse with actual nonzero audio writes and Stop. The added transport geometry check found a2.5physical-pixel text-bound difference, under1dp; the initial1px tolerance failed. [Isolated final build57 CI41256264255](https://github.com/mrayover/practice.logger/actions/runs/41256264255) passes both variants/test compilation and the affected native transport scenario with a1dp geometry allowance, including gear→Continue→ordinary Back→Back to Main→Home. Build57 adds only cancellation callbacks to existing practice/summary/editor guards, clearing the pending header request; Notes persistence/audio engines are unchanged. Superseded diagnostic41256055736 was cancelled without claiming a pass. The earlier source passed all four native scenarios before the final alignment change. Do not report the overall-failed app-source run as an all-four pass.

No comprehensive regression, lint, API26 matrix or enlarged-text/landscape run for this increment. Physical-phone appearance/audio and app restart acceptance remain pending. Existing theme Light/Dark owner acceptance is retained; System/physical/TalkBack and P5/final gates are not inferred. No P5, P6/P7 or roadmap changes.

## Exact next task

Install build57 over the existing app and perform the included short phone checklist, especially reference pinch/pan, click sound/flash and saved notes after app restart.

Full changed-file list, checkpoint commits and detailed verification: [P4 delivery record](docs/P4_TARGETED_UI_DELIVERY.md).
