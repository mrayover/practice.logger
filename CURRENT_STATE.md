# Current state — practice.logger 0.16.0, build32 signed candidate; build33 in validation

Updated October29,2026 (America/Los_Angeles). The owner-approved Focused Practice refinement is implemented and a permanent-key candidate is available. **Full native release gates and physical-phone acceptance remain pending. Final build33 adds a wrapping secondary Notes/Recordings/Save toolbar after200% screenshot review; build32 and build31 remain preserved candidates. The current build33 product is92bcc89; focused nested-reference diagnostics are in progress.** Previous verified release0.15.0/build30 remains preserved; its complete PDF/recording release record is in WORK_LOG.md and Git history at6969abb.

## Delivery and provenance

- Signed APK source: `14ae372d605b36e897d5051331782b151cebe02c`; build32 checks are CI41001594197 (82units, lint0/13, compilation/schema checks). Build33 production source follows; do not install a candidate as a fully verified release.
- [Signed APK](dist/practice.logger-0.16.0-b32-14ae372/practice.logger-0.16.0-b32-14ae372.apk), [ZIP](dist/practice.logger-0.16.0-b32-14ae372.zip), [build info](dist/practice.logger-0.16.0-b32-14ae372/BUILD_INFO.json), [verification status](dist/practice.logger-0.16.0-b32-14ae372/VERIFICATION_STATUS.txt), [phone checklist](dist/practice.logger-0.16.0-b32-14ae372/PHONE_CHECKLIST.txt), [latest pointer](dist/LATEST.txt).
- Package `com.mrayover.practicelogger.app`; version0.16.0/code32; minAPI26/target36. Permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.
- APK SHA256 `bb33ae1e3fa9e0d7131bb225f4639df6f7504da84e70ea3442c09c360d784ba4`; ZIP SHA256 `dcfb84cee66cdecea09e0f495e87f4e315866f9806563e51a03e4440d19e6175`. Publisher checks signature, pinned certificate, identity/version and ZIP round trip. Earlier packages retained.
- Install **over the permanent-key app without uninstalling or clearing data**. Local dist/signing remain Git-ignored; source lives in the original Windows checkout/GitHub. Synced project sources are untouched.

## Implemented behavior

Root cause: the old PracticeViewModel checkpoint only stopped the countdown and had no audible completion invocation. The new run is armed only when foreground timing starts below the configured duration. Its successfully committed crossing consumes the arm, stops at zero and asks the existing AudioTrack path for a short two-note cue. A running click is briefly ducked in the same PCM sample clock without altering BPM or beat phase; cue-only output makes no competing audio-focus request. Composition, database emissions, reference/sheet controls and already-expired recovery never produce alerts. Audio failure cannot undo the saved zero state. Explicit Continue overtime, Complete and Skip remain unchanged; no automatic navigation/outcome.

The supplied sketch governs the compact interface: Block count/Goal/name beside a small timer, bounded Practice Description and Reference Material, compact BPM/exact-entry/±1/click row and direct free/loop recording rows. Quick notes expand without losing drafts. Full saved takes retain rename/export/delete. Bottom Block actions and active recording Stop/Save are pinned outside scrolling content; larger text and expanded media permit controlled scrolling. References preserve EXIF orientation/zoom/pan/PDF pages, with a full safe-width fitted220dp practice viewer inside a bounded300dp area; editor and recording sheet retain their existing viewer bounds. Focus owns recording lifecycle across management-sheet open/close; viewing never restarts audio.

Banner and Android Back share the practice confirmation. Continue keeps timer/click/note intact. Back to Main stops click, pauses/checkpoints/saves the note and navigates only on success. Busy guards reject duplicate operations, failed saves retain the screen/draft, and active capture must Stop/Save or cancel before exit. Other editors retain discard protection; Log uses its save barrier, and summary notes have an explicit discard guard.

## Data preservation

Room10/schemas and migrations1–10 unchanged; JSON writer6/readers1–6 and empty-target atomic restore unchanged. Snapshot instructions/reference notes, sourceBlockId ownership, archived references, BPM history, independent sessions, recordings, Logs, Goals/Routines and restrictive deletion dependencies remain intact. No database migration, permission or runtime dependency added. JSON excludes all image/PDF bytes/metadata and take/audio metadata; keep originals separately and export wanted takes individually. Stable app ID/permanent key retained.

## Actual verification

- [Build40999961809](https://github.com/mrayover/practice.logger/actions/runs/40999961809): **82 unit/Robolectric tests passed**, 0 failures/errors/skips; debug/release compilation; lint0errors/13warnings. Includes non-silent bounded cue, click phase continuity and all previous units. Compiler schemas1–10 match committed bytes.
- Focused API35 [40999959170](https://github.com/mrayover/practice.logger/actions/runs/40999959170) passed normal one-screen visibility and actual AudioTrack writes for cue with click off/on, one-shot/recreation/overtime/BPM retention, then stopped at the new header test's merged-title lookup. Test lookup corrected; no complete native pass claimed yet.
- In-progress focused reruns: normal API35 [41000428347](https://github.com/mrayover/practice.logger/actions/runs/41000428347),200% API26 [41000431859](https://github.com/mrayover/practice.logger/actions/runs/41000431859). They include real note-save failure/exit refusal, recovery, direct free/loop, long/Goal/PDF and saved-expired cold-open checks. All three phases passed on each API; normal API35 job123883312139 and200% API26 job123883320861. Full release and enlarged-text reference/recording checks are next.
- Actual normal/zero screenshots inspected against the owner's sketch. Full API26/API35 release and final200% PDF/image/recording regression checks are pending.
- Initial unsupported Compose None semantics compile error and first BPM test-tag lookup failure are recorded in WORK_LOG. Neither failed run is counted as passing. Publisher's legacy-shell attempt failed before distribution; PowerShell7 retry verified the signed candidate.

## Remaining acceptance and limitations

Actual phone speaker audibility/simultaneous click balance, media volume/routing, permanent-key install-over, original owner photo, vendor pickers, landscape and TalkBack remain physical-phone checks. PCM/event/output-write assertions do not prove acoustic output. No owner phone result is invented. Existing PDF validation/encryption/bounded-resolution limits, foreground-only audio/time and conservative committed-checkpoint recovery remain. Abrupt death can lose unsaved text/latest uncommitted interval. No background alarm/recording, new audio engine or media-inclusive backup. Stage3 detailed phone acceptance is pending; Stage4 remains0/10 real sessions.

## One next task

Finish focused native acceptance, then run required complete API26/API35 release gates and enlarged-text reference/recording checks; reconcile exact test source/results and candidate status before final closeout.
