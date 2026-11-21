# practice.logger · Earned Blocks

Open **Log → Earned Blocks** for lifetime counts and the practice history behind each reward. **Playground → Blocks** shows lifetime earned, used, and available counts from the same records. Use a complete ZIP backup to carry the history and inventory to a new install. [Behavior](docs/PLAYGROUND_SPEC.md) · [Delivery and verification](CURRENT_STATE.md).

# practice.logger · Up Next

Up Next shows the upcoming Block's latest saved reflection and Security rating below **Prep Next Block**. It also plays the latest available standalone Free Recording. Scroll to read longer answers. [Delivery and verification](CURRENT_STATE.md).

# practice.logger · Playground Home

Open **Log → Playground** to walk Batel around the tiled Home in eight directions. Hold between adjacent D-pad arms for a diagonal, or slide your thumb to change direction. Open **Blocks** to see what you've earned through completed practice. [Controls, inventory and saved arrangements](docs/PLAYGROUND_SPEC.md) · [Delivery and verification](CURRENT_STATE.md).

# practice.logger · Weekly planned and actual minutes

Home and the top two Goals show weekly actual / planned minutes. **Edit weekly plan** opens Prepare Week, where you can change Blocks and repetitions. [Current delivery and verification](CURRENT_STATE.md) · [Accounting](docs/PRACTICE_STATISTICS.md#weekly-planned-and-actual-minutes).

# practice.logger · Composer MIDI export

Composer's **Export** dialog saves the current Loop or Full Arrangement as an editable Standard MIDI File Type 1. It uses the current draft and keeps the instruments on separate tracks. WAV and M4A backing-track export are available in the same dialog. [MIDI behavior and limitations](docs/COMPOSER_C6B.md) · [Current delivery and verification](CURRENT_STATE.md).

## Block and Source organization

Browse Blocks by Category or Source, or search the full catalog. Sources can group PDFs, images, and audio across the Library. Existing weekly plans, Goals, and saved routines keep their current order and progress. [Behavior](docs/BLOCK_ORGANIZATION.md) · [Delivery and verification](CURRENT_STATE.md).

# This Week repetition targets and Planned Minutes

Weekly entries default to three repetitions, editable with minus/plus inside their expanded This Week/Prepare Week row. Collapsed dots/squares and completed/target counts track progress; every additional completion still earns its own Playground reward. Planned Minutes uses configured durations and weekly targets. Existing memberships retain one-practice targets during migration. [Behavior/persistence](docs/WEEK_REPETITIONS.md) · [Actual verification and delivery](CURRENT_STATE.md).

# M4A compatibility fix

Build 156 added an M4A provider-MIME correction. The focused API 35 import check passed on the published build 157 source. [Release history and verification](CURRENT_STATE.md).

---

# C6A backing-track export

0.58.0/build155 introduced Current Loop and Full Arrangement backing-track export as compact M4A / AAC or uncompressed WAV. It captures the current draft and mix, renders without speaker playback, and saves through Android's document picker. [Delivery and actual checks](CURRENT_STATE.md) · [Guide and limits](docs/COMPOSER_C6A.md).

MusicXML is deferred from embedded Composer V1. Library integration and portable recovery belong to C7, which requires a separate assignment.

---

# C5C — Make Unique and Edit Pattern

Signed **0.57.0/build152** adds selected-clip pattern editing and independent variations while retaining Arrange view/selection, Loop live editing and existing mixer/persistence. [Delivery, source and actual checks](CURRENT_STATE.md) · [Behavior](docs/COMPOSER_C5C.md). Eight focused host checks and bounded API35/cold reopening pass; phone acceptance remains pending. Stop before separately authorized C6A.

---

# Current increment — Loop live notes and one-bar fit

Signed0.56.1/build151 applies note edits on each track's next cycle during Loop Repeat and fits one complete bar by default, preserving zoom/scroll. [Delivery and actual checks](CURRENT_STATE.md) · [Behavior and Repeat-off limits](docs/COMPOSER_LOOP_LIVE.md). Stop before C5C.

---

# Current increment — compose.logger C5B Arrange

Signed 0.56.0/build149 adds song playback, audio-clock playhead, compact lanes for all composition tracks, direct lane-specific pattern placement and atomic Duplicate/Repeat. [Delivery and verification](CURRENT_STATE.md) · [Arrange guide](docs/COMPOSER_C5B.md). Stop before C5C.

# Retained C5A increment

0.55.0/build147 adds a musical timeline with shared pattern placement, bar-snapped clip movement, deletion and Undo. Existing Save/Save As persist the arrangement. No song playback until C5B. [Current delivery and verification](CURRENT_STATE.md) · [Arrange guide](docs/COMPOSER_C5A.md). Earlier notices below are retained history.

# Current delivery — compose.logger C4D build145

Signed0.53.0/build145 adds independent channel Pan and saved mixer settings, preserving compatible older projects and Save As. Focused audio/persistence/UI checks pass; ready for ordinary owner use. [Download/evidence](CURRENT_STATE.md) · [Behavior](docs/COMPOSER_C4D.md). Earlier release notices below are historical.

# C4C — signed build142 delivered

Owner requested immediate delivery at2% remaining usage. Signed0.52.0/build142 implements velocity and shared Loop/Arrange/Mixer, owned-pattern selectors controlling real playback with restart, live Mute/Solo/Volume and Composer-only AudioTrack Master Volume. Source 1ab3b10a94a8bf375e5b4e5b49190012c78860dd, branch block-ux-stage1.

Delivery: C:/Users/mrove/OneDrive/Documents/ChatGPT/Practice Log development/Compose C4C delivery/build142. Install over the existing app without uninstalling/clearing data. Permanent certificate/package/signature verified. [APK](dist/practice.logger-0.52.0-b142-1ab3b10/practice.logger-0.52.0-b142-1ab3b10.apk) · [ZIP](dist/practice.logger-0.52.0-b142-1ab3b10.zip) · [Behavior](docs/COMPOSER_C4C.md).

C4C candidate0.52.0/build141 adds velocity and a shared multitrack Mixer with functional pattern selection. [Current verification/delivery](CURRENT_STATE.md) · [Behavior](docs/COMPOSER_C4C.md).

# practice.logger

Signed **0.51.0/build140** shows actual instrument types/custom presets and adds expanded patterns with create/duplicate/rename/confirmed deletion, plus track naming/deletion.34 focused checks, compilation/vital lint, API35 workflow and exact cold reopening pass; landscape captures inspected. [Delivery and next phone check](CURRENT_STATE.md) · [C4B behavior](docs/COMPOSER_C4B.md). Composer projects/presets remain outside practice backups. C5–C7/F1 require separate assignment.

Previous delivery: signed0.46.4/build125 requests Android sentence capitalization and autocorrect for prose entry throughout the app. Your keyboard/language settings control corrections and punctuation shortcuts; existing text and numeric/date/search inputs retain their behavior. Home keeps build124’s composition. Compilation/signing and2 focused input-connection checks pass; owner phone confirmation received November14; source recorded on block-ux-stage1. [Current delivery](CURRENT_STATE.md).

Screenshot-directed Home correction is delivered as signed0.46.3/build124: full-size separated buttons, percentage-only Goal summaries and a minimal weekly footer. Focused host-rendered checks confirm zero opening scroll at360x800 and411x886 with reserved system-bar space;200% text remains reachable. Local release/vital lint/instrumentation assembly pass. Phone acceptance and Git publication remain pending. [Delivery/evidence](CURRENT_STATE.md).

G3, LGE-1/LGE-2 and RW1 remain implemented. W2 does not change manual Goal progress or learning recommendations.

An offline Android practice app using Kotlin, Jetpack Compose, Room and Lato. See [CURRENT_STATE.md](CURRENT_STATE.md) for the latest permanent-key build, focused verification and phone acceptance.

## Forward V1 roadmap

P0–P5, L1–L3, K1, T1–T2 and T3A–T3D are implemented. P5 closed with full automated regression and owner acceptance on build67. The [Media Library integration specification](docs/MEDIA_LIBRARY_TRANSCRIBE_INTEGRATION_PLAN.md) covers shared originals, Reference Piano, Block Transcribe and local annotations.

The [V1 master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md) defines **G3 → Stage 4 practical-use review → LGE-1 → LGE-2 → remaining owner-approved V1 features → F1 → F2**. G3 is complete; the owner deferred all ten undocumented Stage4 sessions on November12, resolving that prerequisite without practical-use or phone acceptance. See [the decision](EVALUATION.md). [LGE-1/LGE-2](docs/LEARNING_GUIDANCE_EXTENSION.md) are implemented; PG1 is now implemented; next is its physical-phone acceptance and the owner decision on subsequent V1 scope. Comprehensive regression waits for explicit owner F1 authorization. The [Practice Overlay specification](docs/PRACTICE_OVERLAY_SPEC.md) defines O1, which follows S2. The [Standalone Transcribe plan](docs/T3_STANDALONE_TRANSCRIBE_PLAN.md) records the approved T3 milestones.

## Appearance and Settings

Home → **Library** offers **Media** and **Inspirations**. Media opens the canonical Media Library. Import JPEG/PNG/WebP, supported unencrypted PDFs, MP3/AAC-in-M4A/PCM16 WAV; search title/original filename, filter type, sort by Name/Most recent, rename or confirm deletion. Library media is local. Use complete ZIP backup for its originals and metadata. Open images/PDFs in dedicated zoomable/pageable previews. Audio opens a reusable real-signal waveform/player with source-time seek/zoom/pan, pitch-preserving50–100% speed, precise A/B repeat and persistent labeled master points/sections with notes. Numeric source-second and10ms controls supplement dragging. Library audio does not create practice sessions. The development Block Editor stages independent ordered visual and audio collections, sharing Library originals without per-Block copies; JSON excludes media, Block associations and master annotations. See [technical details](docs/MEDIA_AUDIO_WORKSPACE.md). Legacy attachments/backing takes remain in existing Blocks/Recording and are not automatically adopted. See [storage/legacy decision](docs/MEDIA_LIBRARY_STORAGE.md). Block Library, This Week and Prepare Week share the expandable Block pill. Selected chips retain visible outlines.

The far-right shared-header **Settings** gear offers Light, Dark, **Smooth** (teal), **Sunny** (yellow/orange), **Orchid** (dark purple), **Submarine** (red night lighting), **Cosmic** (light-blue/lavender stars), then **System Default**, with the same control states. Existing installs stay Light until changed. Choices persist locally; System Default follows the device. Lato, screen layout, recording ownership and original image/PDF pixels remain. The existing Tools → Audio Settings shortcut reaches the same settings page. Central palette definitions and semantic app roles live in Theme.kt. Owner confirmed the downloaded new app works in both Light and Dark and explicitly authorized certification as complete on2026-10-31. Exact installed build was not independently captured. Theme implementation is complete; P5 automated appearance checks pass on API26/API35. Detailed physical/System/audio/TalkBack acceptance remains pending. Exact evidence and limits are in CURRENT_STATE.

Inspirations keeps a private collection of writings, PDFs and images, with five read-only included works. Add a piece by writing or pasting, taking a photo, or choosing a file. Complete ZIP backup preserves saved personal entries. Home → **Log** opens **Journal** and offers **Playground** with a read-only earned Block inventory. [Inspirations details](docs/INSPIRATIONS.md)

## Reference Piano

Home → Tools → Reference Piano offers a multitouch reference instrument: compact C4–C5, wider landscape keys, C1–C7 octave shifts, sustained chords and clean release/cancellation. No microphone permission. Block Transcribe and standalone Transcribe embed the same keyboard and engine and borrow focus from the reference player. [Architecture and limits](docs/REFERENCE_PIANO.md); exact automated and acoustic status is in CURRENT_STATE.

## Metronome

Use the compact practice row for BPM, −1/+1 and Start/Stop; the chevron opens shared controls for 2/4, 3/4, 4/4, 5/4, 6/8 or 7/8, Accent first beat (Off by default), and Sound + Flash / Sound only / Flash only. The running meter remains available while collapsed; the entire Stop click button pulses with each flash-enabled beat in practice and Tools. Opening/closing restores the prior Description/Reference, PDF page/zoom and Notes draft without restarting practice or recording. Tools → Metronome provides the same controls and clock.

For 6/8 and 7/8, BPM counts each eighth note: six or seven clicks per bar, without grouped/dotted-quarter mode. Meter/accent/output changes take effect on the next beat; BPM retains phase. Flash follows the audio playback clock; actual display/speaker latency can differ. Silent mode silences click PCM. Tools → Audio Settings saves four click timbres, output default and a shared completion alarm (four sounds or Off) for Block and foreground countdown timers; previews stop on leave/background. Active capture suppresses completion audio.

## Focused Practice transport and Notes

Each Block opens Ready. Choose **Start Block** to begin measured practice or **Prep Block** to use references, playback, click and recording before starting. **Start Practice** switches from prep to measured time. After completing or skipping a placement, **Up Next** waits for a choice. Reopen an unfinished session from **In Progress**.

Description and Reference retain their shared workspace; Description starts open and switching sections always leaves one open. Notes is a footer pill beside the practice actions; Save for later and its row are removed. Opening a transport panel temporarily hides both contents, retains both headers and gives the panel the space. Closing restores the prior workspace choice, including PDF page/zoom. Tapping either workspace header while a panel is open collapses it and opens that section; active capture continues with pinned Stop, and collapsed temporary review keeps Save and a full-review button. Only one transport panel opens. Compact tinted recording controls/selection, timer/metronome and primary actions remain.

Notes opens Add New Note with the Block's newest-first collapsible history across sessions. Save Note adds an independent timestamped entry, clears the editor after commit and keeps Notes open; Return to Practice closes separately. Expand a saved new/legacy note for Edit, Save Changes or Cancel Changes; original creation/session date and timing stay unchanged. Drafts survive panel switches and leaving unsaved text asks before discarding. Legacy notes are marked Earlier session because their actual creation timestamp was not stored. Room11 and JSON7/readers1–7 preserve old/new notes independently; descriptions, overall session notes and daily Log remain separate.

A running Block crossing its configured duration plays a short two-note cue through the existing media AudioTrack path, briefly ducking a running click while retaining BPM/beat phase. It stops at zero and still requires explicit Continue for overtime or Complete. It never marks a Block finished or replays from reopening/recomposition. Device media volume/routing determine physical audibility.

The banner returns Home through the current save/discard barriers. Unsaved new or edited practice Notes first require returning to save or explicitly discarding the draft. Once resolved, banner and Android Back open **Leave current session?**: Continue stays put; Back to Main pauses/checkpoints before navigation and preserves saved/legacy Notes. Failed saves/checkpoints remain recoverable on the same screen. Stop/Save or cancel an active recording before confirming exit. Unsaved Block/Goal/Routine editors reuse their discard safeguards; Log retains its save barrier. Current candidate/release verification is in CURRENT_STATE.md.

## Install and verify

Find the current APK and ZIP in the owner's OneDrive delivery folder recorded in [CURRENT_STATE.md](CURRENT_STATE.md). That file also records the source SHA, signature, hashes and verification results. The checkout's local distribution pointer is `dist/LATEST.txt`.

Android 8.0+; install over the existing permanent-key app **without uninstalling** to retain data/audio. Package ID: `com.mrayover.practicelogger.app`. Original temporary-key 0.1.0 is a separate app with no automatic data transfer. CI debug APKs use a temporary key and cannot update the permanent-key distribution.

## Exporting audio

Open the Settings gear and find **Storage → Export destination** to choose a writable folder. From a saved take, tap **Export audio**, then **Export to Selected Folder** or **Save As…**. With no folder selected, Export audio opens Save As directly.

Organized exports copy original audio into `Recordings/<Block name> - <Block ID>/<saved date>/` or `Recordings/Free Recordings/<saved date>/`. Filenames include the local recording time, a take label or recorded loop beat count and BPM, and a short take ID. Repeated copies use distinct filenames. If a provider cannot create the folders or write the file, choose another folder or use Save As. Drive and OneDrive availability depends on what their Android providers expose. The original remains in practice.logger.

## Using the app

The Home footer and top two Goals show actual / planned practice minutes for the local Sunday–Saturday week. **Edit weekly plan** opens Prepare Week. Tap **This Week** in the footer for cumulative Statistics, recent activity and Block/Goal breakdowns. The Statistics weekly view uses Monday–Sunday local time, matching Journal. Goal time follows saved historical attribution. [Accounting details](docs/PRACTICE_STATISTICS.md). The menu scrolls when its content exceeds the available height.

- **Blocks:** tap a Block to expand it and choose Edit, Add to This Week or Add to Practice. Search name, action or notes, filter by Goal, and switch between Active and Archived. Restore from an expanded archived Block. The editor requires Name and Action, and supports duration, optional BPM and notes, archiving and protected permanent deletion.
- **References (development source):** in a new or saved Block, use Add Visual Reference for Library, image/PDF import, Photo or Gallery; use Add Audio Reference for Library or supported audio import. Attach several before first Save, mix images/PDFs, preview any item, move Up/Down or remove one item. First in each independent list is the default; duplicates keep their position. Save applies the entire collection together. Saved removals require confirmation and preserve shared Library originals. Existing image orientation, zoom/page controls and saved Block annotations remain. Visual imports retain the20MiB/40MP/image-dimension and1–100-page unencrypted-PDF limits. Audio preview has Play/Stop and releases on close/background. Practice offers compact source selectors; PDFs/images keep independent viewing state, audio settings/markers remain Block/source-local, and new reference recordings append. Build111 delivers these collection and practice controls.
- **Safe deletion:** unused active or archived Blocks can be deleted after confirmation. An attached image or PDF, Goal links, saved Routines, recordings and all draft/unfinished/completed session entries protect the Block. Remove the reference deliberately in the editor before permanent deletion. Refusal shows persistent counts and explains completed history. Associations/audio/history are never removed automatically; use Archive when dependencies remain.
- **Practice Now:** open This Week for your Sunday–Saturday plan. Expand a Block and choose Practice Block to open Ready with Start and Prep. Use Prepare Week to add, create, carry forward, or remove Blocks. Use In Progress to resume an unfinished session, Add Blocks to build or edit the current draft, Routines to start a saved Routine, or Transcribe. The builder supports reordering, duration overrides and saving the draft as a Routine. Focus starts paused; Start/Resume, Pause, Complete and Skip record foreground active time. At zero a short two-note cue sounds (media output permitting), then choose Continue for overtime or Complete. Header/Back opens a confirmation; Back to Main saves and pauses before Home, and restart resumes paused.
- **Block Transcribe:** **Record Take** captures your performance while the canonical reference plays. Use the reference controls during capture, then tap **Stop and review take**. **Play Preview** pauses the reference. **Save** keeps a regular Block Free Record take. Headphones reduce the reference sound picked up by the mic.
- **Standalone Transcribe:** choose an existing Library recording or import an MP3, M4A or WAV. Slow the reference to 50–100%, seek and zoom the waveform, repeat an A–B range or play it once, edit shared master markers, and play the Reference Piano over the recording. Playback carries no practice-time attribution.
- **Notes and history:** Timestamped Block notes require Save Note; edits require Save Changes. Leaving unsaved new/edit text offers a discard guard. Completed session summaries display timestamped and legacy Block notes with expand/edit/save controls, filtered to that exact session placement. Session notes require Save note; leaving an edited summary asks before discarding. Routines retains Recent Practice and reopenable summaries with historical instruction snapshots.
- **Saved Routines:** reusable ordered Block references with independent durations, create/edit/reorder/delete, builder Save as Routine, and independent fresh starts. Archived or unavailable references prevent starting until repaired/restored.
- **Goals:** up to five active Goals, notes/outcome, priority, top-two Home weekly minutes, completion/reopen, shared Block links and Goal practice. Choose a **Primary Goal** in a Block editor. Active Goal priority colors run from red through orange, yellow, green and blue. On expandable Block pills, the left square shows the current Goal color for work to do. The right slit shows the color captured at completion when the displayed week has a recorded outcome. No Goal uses the existing neutral cap. Displayed Goal time retains the existing attribution. New practice stores its first committed placement priority for future reporting.
- **Recordings:** Show reference within a Block’s recording sheet opens the same image/PDF inline, including during free capture and phrase count-in/capture. Hide, zoom, pan and page changes keep the sheet and recording active. Active status and Stop and review take/Cancel phrase stay above scrolling references. Stop opens temporary **Review Take** with Play/Stop Preview, Save, Re-record and Discard in Focused Practice, saved-Block recording and standalone Free Recording. Only successful Save adds to permanent saved takes. A complete foreground phrase automatically loops its temporary take for review after capture cleanup; Keep/Save is still explicit; incomplete phrases are discarded. Close recordings asks before discarding review audio. Background stops capture/playback without auto-saving or restarting; valid free audio can remain for review within the process. Process loss discards unsaved temporary audio on recovery. Existing saved takes remain intact; recording never runs in the background. Standalone Free recording has no Block reference. Each Block retains multiple free M4A takes and fixed-phrase WAV loops; Full recording management is available in the saved Block editor, including archived Blocks, and main Recording; Focused Practice uses compact selectors. Record phrase supports 30–300 BPM, any whole loop length of 1–64 beats (4/8/16/32 and 6/12/24/48 shortcuts), a positive user-entered count-in (default 4), and an always-enabled loop recording click. Takes support playback, rename, confirmed deletion and per-take export. Recording also provides standalone Free recording and Saved free recordings without creating a synthetic Block. Audio stops on leave/background and never restarts automatically.
- **Log:** Home → Log opens today in your local calendar. Write freely; text saves automatically. Wait for Saved. Choose date to revisit and edit any day. Weekly View displays Monday–Sunday from the same entries; Previous/Next week navigate, and tapping a day opens its editor. Back returns from the week to the daily editor; The banner saves before leaving. If saving fails, the text stays in the editor for Retry. Returning on another day opens that day without changing earlier text.
- **Tools:** standalone metronome, chromatic microphone tuner, stopwatch/countdown and Backup and restore. The practice click and Block timer are independent. Tools → Audio Settings persists the default loop count-in; each loop can override it. Count-ins must fit the 15-minute execution budget at selected BPM. Shared metronome sound/output and completion alarm choices also persist; appearance choices are available from the header Settings gear. Lato, shared banner navigation and the selected central appearance palette apply throughout app screens.

## Imported backing audio

Focused Practice → Loop Setup → **Add Backing Track** disables musical creation fields and replaces Record Loop with Choose file/Save. Supported: MP3, M4A(AAC), ordinary PCM16 WAV mono/stereo8–96kHz, up to50MiB/15minutes. Validated filename/duration appear; Save adds/selects the existing loop collection item without autoplay. Saved imports retain their type when the checkbox changes, survive restart and share rename/delete/export. Playback repeats the actual file with the existing Stop/focus/background safeguards, independent of BPM/count-in. Ordinary repeat may have audible codec/file/device joins; seamless playback is not certified. JSON still excludes audio. [Support and scoped evidence](docs/IMPORTED_AUDIO.md).

## Practice Overlay

Open a Block’s image or PDF and tap **Markings → Edit markings**. Choose Highlight, Cover or Focus and drag a rectangle. Select a marking to move, resize, recolor a highlight, or delete it. Use the menu to undo the last edit or hide markings. Hold **Peek** to see through covers and tap **Done** to return to zoom and pan. **Copy overlays to Block** copies all saved pages to a Block using the same Library original.

## Backup and restore

Open **Tools → Backup and restore → Prepare complete ZIP** to save your practice data, original media and recordings, editable practice markings, audio annotations, and portable settings. When the private archive is verified, use **Save ZIP as…** to choose a document or save to **Backups/** under a supported folder selected in Settings. A read-back-verified message confirms the provider-visible copy. Check separately that a cloud provider has finished syncing. Save any open edits before preparing the archive.

On a separate empty installation, choose **Choose complete ZIP to restore**, review the preview, and confirm. The app rejects populated targets and opens restored unfinished practice paused. Keep the ZIP private because it includes your recordings and notes. Export-folder access must be selected again on the new device. Unsaved work and temporary audio are outside the archive.

**Legacy JSON** still writes v7 and reads v1–v7, up to 8 MiB. It carries data such as Logs, Blocks, Routines, Goals and session history. Use the complete ZIP v6 for completion snapshots, reflection prompts, preparation time, weekly plans, originals, recordings, media links, editable practice markings, audio annotations, standalone Transcribe activity, portable settings and primary Goal assignments and snapshots. The app also restores ZIP v1–v5. See [backup format, limits and recovery](BACKUP_FORMAT.md).

## Historical P5 verification

P5 full CI41329443572 passes all nine required jobs: 111 unit tests, 168 native executions on API26/API35, lint zero errors/18 documented advisories and debug/release/instrumentation compilation. Signed build67, source provenance, all attempt outcomes and the exact phone-acceptance next action are in [CURRENT_STATE.md](CURRENT_STATE.md), [the P5 report](docs/P5_REGRESSION_REPORT.md) and [machine-readable baseline](docs/P5_REGRESSION_BASELINE.json). The owner confirmed build67 complete and running correctly on November 1, 2026; P5 is closed. Stage 4 remains at 0/10 documented real sessions; the owner deferred the remaining sessions on November12 to permit the next separately assigned phase.

## Development

Read [AGENTS.md](AGENTS.md), [CURRENT_STATE.md](CURRENT_STATE.md), [BUILD_PLAN.md](BUILD_PLAN.md) and [WORK_LOG.md](WORK_LOG.md), including the mandatory efficiency procedure. Use [FIRST_WORK_HANDOFF.md](FIRST_WORK_HANDOFF.md) to start a new work session.

Build/check command: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest` (Windows: `.\gradlew.bat` with the same tasks). Generate the MP3/M4A instrumentation fixtures with `scripts/Prepare-AudioFixtures.sh` before compiling native tests. The repository uses JDK 17, SDK 36, Gradle wrapper, Kotlin/Compose and Room 2.8.5; exact dependency versions are in Gradle files. Room schemas 1–16 and explicit migrations are retained. GitHub Actions supports full release checks and isolated class/API/text-scale diagnostics; documentation-only changes skip CI.

Use `scripts/Publish-LocalApk.ps1` for the permanent-key distribution. Keep private `.signing`, tools, build outputs and APKs out of Git; retain earlier releases and verify signatures, metadata and hashes. License attribution: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
