# Manual phone checks

Owner phone execution is pending. Android emulator results are recorded separately in CURRENT_STATE.md and WORK_LOG.md.

## Stage 0

1. Download the successful CI APK artifact, extract and install on Android 8.0+.
2. Launch practice.logger. Confirm readable Lato text, no crash or unfinished controls.
3. Rotate, close/reopen and launch in airplane mode.

## Stage 1

1. Open Manage Blocks and create a Block with Name, Action, 7 minutes, optional BPM and notes. Try saving with blank Name/Action, zero/decimal duration and invalid BPM; each should show a clear error and retain the draft.
2. Save, reopen and edit the Block. Search using part of its name, action and notes; try a term with no matches.
3. Change a field, rotate and confirm the draft remains. Cancel → Keep editing should retain it; Cancel → Discard should leave the stored Block unchanged. Test Android Back the same way.
4. Archive → Cancel should keep the Block active. Confirm Archive; it should move to Archived. Open and Restore it; it should return to Active with its fields intact.
5. Force-stop the app in Android settings, then reopen it in airplane mode. Confirm saved Blocks and edits remain. An unsaved draft is not promised after force-stop.
6. Inspect Home, lists, fields, errors and confirmation dialogs for Lato, large text/display scaling, keyboard scrolling and comfortable touch targets. Android keyboard/system prompts use system typography.

Record phone model, Android version and failures. Avoid irreplaceable data until stable signing and backup exist. Do not uninstall to resolve an update signature mismatch if it contains needed data.
