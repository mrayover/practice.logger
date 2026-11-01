# Current state — P4 targeted UI and historical Notes / build60

## Delivery

Signed release **0.16.0/build60**, app source `969855f69a4ca85272071b25b0bb36e553853869`. Later commits change only the targeted diagnostic/records. [APK](dist/practice.logger-0.16.0-b60-969855f/practice.logger-0.16.0-b60-969855f.apk) · [ZIP](dist/practice.logger-0.16.0-b60-969855f.zip) · [Build info](dist/practice.logger-0.16.0-b60-969855f/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.16.0-b60-969855f/PHONE_CHECKLIST.txt). Install over the permanent-key app without uninstalling or clearing data. Previous distributions remain preserved and ignored.

APK SHA256 `b3c86bec7ef192bbad93ebc77bcb40a2d86ea685ee74f7d02769c32451efd21a`. ZIP SHA256 `23159647fd3fda9f09da0360995384ebb32db56d812c687e8a1d22ec39ad6a0f`. Existing publisher verified application ID, version60 and the pinned permanent certificate/signature; ZIP CRC and every packaged file match the distribution folder.

## Final click/Accent follow-up

Owner accepts the remaining build59 layout. Build60 only centers Start/Stop click text with symmetric padding, offsets its bubble12dp into the existing scroll gutter so its left edge meets Record/Loop Setup's right edge, and makes Accent/checked/unchecked box use primary green. Tempo, chevron, timing/audio/Notes and recording rows are unchanged.

[CI41265135190](https://github.com/mrayover/practice.logger/actions/runs/41265135190) passes debug/release/test compilation and the same single130%-text transport case, including text-center/both-boundary checks and existing meter/chevron/tempo safeguards. Captured Light screenshots inspected; the nominal dark capture still shows Light, so no Dark screenshot verification is claimed. Superseded pre-color run41265093635 was cancelled, no pass claimed. No full regression or repeated units. Signed APK/certificate/version and ZIP CRC/member bytes verified; phone confirmation pending.

## Build59 accepted layout baseline

Build59 changes only PracticeScreens.kt plus versioning: remove rounded clipping around the BPM/name area, give it8dp more room, fit single-line labels/signatures when needed, and shift the tempo pair32dp to center over Record. The click action/text moves right with its final letter aligned to Play. The chevron's24dp width and184dp right-hand region remain unchanged; recording rows/audio/state/Notes are unchanged.

[Focused CI41258437458](https://github.com/mrayover/practice.logger/actions/runs/41258437458) passes debug/release/test compilation and the single API35 transport scenario at130% text size. All six signatures/name remain single-line and inside their bounds, the fixed chevron and tempo/Record/text-right-edge assertions pass, and existing accent/tempo/configuration/exit controls remain reachable. Screenshot inspected. Signed APK was compiled in41258024388; final passing test-only source has identical app/version files. No unit/full regression/lint/API matrix repeated; same-phone visual acceptance remains pending.

## Assigned scope

Selected chips retain visible outlines in Blocks/Goals/Tools/Settings; compact practice selection styling remains. Recent Practice and Block Library use shared pills with a separate body suitable for future end caps, without Goal caps. Home Library opens only Coming soon. The shared far-right Settings gear uses the existing settings screen and save/discard/exit guards. Settings remains available from Tools.

Focused Practice fills Notes/Skip, shows BPM/meter, inline Accent checkbox, aligned chevrons, −1/+1 and Start/Stop click. Existing metronome configuration/audio/pulse APIs, recording action columns and timer behavior remain. Compact reference reset overlay is removed; pinch/pan and an accessible Reset zoom action remain. Larger reference viewers retain their prior reset controls.

Session Summary previously read only the legacy placement note, although Save Note persists new timestamped entries in practice_notes. It now reads the existing history by exact session/placement/Block and reuses the same expand/edit/save/cancel controls. No schema, migration, backup format or audio engine change.

## Targeted evidence and limits

[Build56 CI41255636933](https://github.com/mrayover/practice.logger/actions/runs/41255636933) passes debug/release/instrumentation compilation and **14 scoped unit tests**: Notes4, BeatPcm2, metronome persistence2 and meter PCM6. Notes tests complete a session, close/reopen the database, exclude another session on the same Block, edit saved text and reopen again; legacy/new entries, timestamps and timing remain intact.

Three build56 API35 native scenarios pass: Library/header Settings/navigation/selection, historical Notes display/edit/recreation and Light/Dark practice whole-button pulse with actual nonzero audio writes and Stop. The added transport geometry check found a2.5physical-pixel text-bound difference, under1dp; the initial1px tolerance failed. [Isolated final build57 CI41256264255](https://github.com/mrayover/practice.logger/actions/runs/41256264255) passes both variants/test compilation and the affected native transport scenario with a1dp geometry allowance, including gear→Continue→ordinary Back→Back to Main→Home. Build57 adds only cancellation callbacks to existing practice/summary/editor guards, clearing the pending header request; Notes persistence/audio engines are unchanged. Superseded diagnostic41256055736 was cancelled without claiming a pass. The earlier source passed all four native scenarios before the final alignment change. Do not report the overall-failed app-source run as an all-four pass.

No comprehensive regression, lint, API26 matrix or enlarged-text/landscape run for this increment. Physical-phone appearance/audio and app restart acceptance remain pending. Existing theme Light/Dark owner acceptance is retained; System/physical/TalkBack and P5/final gates are not inferred. No P5, P6/P7 or roadmap changes.

## Exact next task

Install build60 over the existing app and confirm centered click text/bubble boundary and green Accent against the owner's phone screenshot.

Full changed-file list, checkpoint commits and detailed verification: [P4 delivery record](docs/P4_TARGETED_UI_DELIVERY.md).
