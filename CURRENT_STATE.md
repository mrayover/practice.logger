# Current state — T1 Record Loop / Transcribe delivered

Updated November 3, 2026 (America/Los_Angeles). T1 is implemented and its scoped Android checks passed. T1 owner-phone acceptance is pending. The owner confirms the K1 Reference Piano works on their phone. This accepts the reported keyboard functionality. L1–L3 retain their separately documented acceptance status.

## Current signed delivery

**0.22.0/build83**, app source `b7e422ab92d019c33ae5fed1b5f71ab31890f31e`. [APK](dist/practice.logger-0.22.0-b83-b7e422a/practice.logger-0.22.0-b83-b7e422a.apk) · [ZIP](dist/practice.logger-0.22.0-b83-b7e422a.zip) · [Build info](dist/practice.logger-0.22.0-b83-b7e422a/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.22.0-b83-b7e422a/PHONE_CHECKLIST.txt). Install over build81 without uninstalling or clearing data.

App ID `com.mrayover.practicelogger.app`, permanent certificate SHA-256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. APK SHA-256 `25440f884c24cfe94ec49b183618767c16fbd07f4afec4371e49e118949d7837`. ZIP SHA-256 `893a0b357d657ff521b7107924a00d3628c3e8bc24d45d645a1d4350ff7d3464`. Signature, package/version, 67 matching unsigned payload entries, ZIP contents/CRC, 368 prior package hashes and both build82 candidate hashes were verified.

## Implemented scope

Focused Practice switches between Record Loop and Transcribe. Transcribe uses the source Block's canonical AUDIO link, the existing Library picker/import and role checks, and the shared source-time player, waveform and zoom. The panel supports pitch-preserving 50–100% speed, seek, A–B repeat, one-shot phrase playback and timestamp edits. Master points and sections are read-only. The existing expandable piano can sound while reference audio is playing or paused, using the controller's single focus request. Keyboard collapse releases its output. Leaving the panel, changing the reference or backgrounding releases active Transcribe output.

Record Loop and Free Record use the existing capture and review path. The practice timer remains authoritative. Saved legacy imports retain their original take ownership and playback. Room14 and JSON7 are in use. T1 playback state is transient. Durable local state and annotations belong to T2, and simultaneous Transcribe playback and Free Record belong to T3.

## Actual verification

- [Build83 compilation and unit tests](https://github.com/mrayover/practice.logger/actions/runs/41767496452) passed debug, release and instrumentation APK compilation and all **24 scoped units**.
- [API26 at normal text](https://github.com/mrayover/practice.logger/actions/runs/41767948973) and [API35 at 200% text](https://github.com/mrayover/practice.logger/actions/runs/41767951438) passed the Transcribe workflow in portrait and landscape. Tests cover mode changes, retained Loop setup, canonical association isolation, picker cancellation and replacement, 75% playback with section selection and repeat, piano collapse during playback, rotation and pinned actions. Compact portrait keyboard and enlarged landscape transport screenshots were reviewed. Secondary controls scroll in constrained layouts.
- [API35 recording compatibility](https://github.com/mrayover/practice.logger/actions/runs/41767954387) passed **2 native workflows** for Record Loop, Free Record, Notes during capture, saved take selection/playback and legacy imported audio.
- The unchanged audio engine passed **3 native piano and Tools checks** on [API35 build82](https://github.com/mrayover/practice.logger/actions/runs/41766318314) and **3 Library playback and waveform checks** on [API26 build82](https://github.com/mrayover/practice.logger/actions/runs/41766321106). These covered actual piano output during paused and playing reference audio, interruption, standalone operation, MP3/M4A/WAV speed and pitch parameters, repeat/seek, missing original handling and long waveform cache recovery. Build83 changes presentation and version after those audio checks.

See [WORK_LOG](WORK_LOG.md) for the early dependency failure and layout retries. Physical install-over, listening and owner phone verification remain pending. No full regression gate was rerun.

**Exact next task:** run the [T1 owner-phone checklist](MANUAL_TESTS.md#t1--block-transcribe-owner-phone-checklist) on signed build83. **Next separately assigned milestone:** T2, Block-local Audio State, Marker Overrides and Master Promotion.

---

## Retained K1 release — Reference Piano

Updated November 3, 2026 (America/Los_Angeles). K1 implementation, targeted engineering verification and permanent-key signed delivery are complete. **The owner confirms the K1 Reference Piano works on their phone.** This accepts the reported keyboard functionality. Individual acoustic measurements and checklist results were not supplied. The owner reports the latest pre-K1 functionality works; no individual L1/L2/L3 phone-check results are invented.

## Build 81 signed delivery

**0.21.0/build81**, application/release source `a3fd4090dd34a95c0bea266b83a9b04e0fc14638`. [APK](dist/practice.logger-0.21.0-b81-a3fd409/practice.logger-0.21.0-b81-a3fd409.apk) · [ZIP](dist/practice.logger-0.21.0-b81-a3fd409.zip) · [Build info](dist/practice.logger-0.21.0-b81-a3fd409/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.21.0-b81-a3fd409/PHONE_CHECKLIST.txt). Install over build80 without uninstalling or clearing data. AppID `com.mrayover.practicelogger.app`; unchanged permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.

APK SHA256 `4c295089bffab0ccf9a789ead6973bc01c4b3977901bb5fbc84ea95de2e47b75`; ZIP SHA256 `7b01029604c6e593052026dc28533a3877b83a3649d779f72c45c0ed1a9bb028`. Publisher signature/package/version/pinned-certificate checks pass. All67 unsigned application payload entries match the signed APK. ZIP CRC/exact members/bytes and all362 session-entry APK/ZIP hashes pass; earlier releases are retained. Exact final Git/remote SHA is verified at handoff.

## Implemented scope and reuse

Home → Tools → Reference Piano adds the compact instrument without a new Home control or microphone request. Compact C4–C5 inclusive, large-width C4–F5, octave shifts covering C1–C7, white/black geometry, pressed-key/note feedback and genuine native multitouch support chords, unisons, glissando, repetition and cancellation. Scrolling keeps keyboard/Back reachable in landscape/enlarged text. Register is saveable UI state only.

Independent PianoPcm provides12-tone A4=440Hz synthesis,16 token-owned voices including release tails,6ms attack/45ms release, light decaying harmonics and bounded smooth mixing. PianoAudioEngine owns one48kHz mono PCM16 stream and foreground/output cleanup. PianoKeyboard is a reusable Compose callback component; PianoScreen is the small standalone Tools owner. [Architecture and T1 reuse contract](docs/REFERENCE_PIANO.md). Embedded construction borrows an existing playing LibraryAudioController's focus, with no competing request/abandon. Owner pause/close/source change/background/errors/focus loss stop companion notes; keyboard release/close never closes the reference controller. No global mixer or player rewrite.

Room14, schemas/migrations1–14, JSON7/readers1–7, media originals/links/markers, take ownership and practice accounting are unchanged. No T1/Block Transcribe, MIDI, recording/export or new persisted piano data exists.

## Actual scoped verification

- [Build41745864648](https://github.com/mrayover/practice.logger/actions/runs/41745864648), sourcea3fd409: debug/release/instrumentation compilation and **4/4 PianoPcm unit tests**, zero failures/errors/skips. Generated PCM fundamental checks at MIDI24/40/60/69/72/84/96, A4/semitone/octave formulas, lower/upper pitches,16-voice bounds, unison/rapid retrigger/glissando/release and black/white/outside hit geometry pass.
- [Final UI API26/100% 41747320122](https://github.com/mrayover/practice.logger/actions/runs/41747320122) and [final UI API35/200% 41747324096](https://github.com/mrayover/practice.logger/actions/runs/41747324096) pass the UI class each. [Audio API26/100%41746593019](https://github.com/mrayover/practice.logger/actions/runs/41746593019) and [audio API35/200%41746596773](https://github.com/mrayover/practice.logger/actions/runs/41746596773) pass2/2 audio tests each before their later UI assertion failure. **Three distinct native tests pass per API across scoped runs,6 successful selected executions total; no single combined green-run claim.** Actual nonzero AudioTrack writes while Library source position advances; triad output, release/close retaining reference playback, post-close seek/speed, borrowed focus interruption/background/no automatic restart, standalone repetition/background/close are checked. UI native three-finger chord, selected-key feedback, white-to-white slide, black note, ACTION_CANCEL, endpoint registers, recreation/background, portrait/landscape and Back pass. Existing Metronome PCM, Tuner microphone processing, Timer and Audio Settings navigation pass. Portrait chord/landscape screenshots on both APIs were inspected. Signed application sourcea3fd409 is unchanged; audio test/workflow source1b11505 and final UI sourcee957f7d differ only in workflow/records and new-test synchronization.

Initial build41745587125 failed a new local theme lookup and was corrected. Native setup41746405634/41746409574 failed an artifact reuse folder before test execution; corrected workflow only. Combined audio/UI runs41746593019/41746596773 passed all piano stages then failed the new immediate tools.clicking assertion after Back at line66. The existing Tools test waits for Compose disposal here; adding the same synchronization preserves the stop assertion and final UI-only checks pass. No product change was made for that retry. Exact diagnostics remain in WORK_LOG. No full P5/F1/F2/broad lint rerun or physical acoustic listening occurred.

## Limits and exact next action

Simple synthesized piano-like timbre; no acoustic samples. Sixteen voices include45ms tails; extra presses are ignored until capacity frees. In build 81, embedded notes required a playing reference controller. Native active outputs/source progression do not certify phone speaker/headphone audibility, tuning/timbre, latency, route/vendor interruptions or TalkBack usability. Those remain physical checks. Existing media-inclusive backup limits remain; keep originals separately until S2.

P5 acceptance remains closed; Stage4 remains0/10 documented real sessions.

---

## Retained L3 release — historical delivery and evidence

# L3 Block media associations delivered (historical)

Updated November 3, 2026 (America/Los_Angeles). L3 implementation and signed delivery are complete; actual scoped verification and limits are recorded below. **Owner-phone acceptance of L3 is pending; L1/L2 phone acceptance remains separately pending.** P5/build69 acceptance is preserved below. Stop after L3.

## Current signed delivery

**0.20.0/build80**, application/release source `34edea07be82a03c84087cb5540b0ce27fa07b7c`. [APK](dist/practice.logger-0.20.0-b80-34edea0/practice.logger-0.20.0-b80-34edea0.apk) · [ZIP](dist/practice.logger-0.20.0-b80-34edea0.zip) · [Build info](dist/practice.logger-0.20.0-b80-34edea0/BUILD_INFO.json) · [Phone checklist](dist/practice.logger-0.20.0-b80-34edea0/PHONE_CHECKLIST.txt). Install over build76 without uninstalling or clearing data. Internal77/78/79 and all earlier packages remain. App ID `com.mrayover.practicelogger.app`; unchanged permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.

APK SHA256 `4d113ce795b9f14fb3c1c656259034d80e95448936eb47f6d5188577ce34f633`; ZIP SHA256 `83c440b99f0e94c70b65b997f0839074126bcca2d394de62b98f4dfd00214203`. Publisher signature/package/version/pinned certificate checks pass. All67 unsigned application payload entries match the signed APK; ZIP CRC/exact members/bytes and338 session-entry APK/ZIP hashes pass. Production source is unchanged after the signed source above; later commits change tests/workflow/records only. Exact final Git/remote SHA is reported at handoff.

## L3 scope and ownership

Saved Blocks independently choose/import/replace/unlink one canonical visual (IMAGE/PDF) and one canonical audio asset using stable Library IDs. Shared Library search, compatible filters, metadata, stable sorting and current-selection display are reused. New file/camera/gallery imports commit one canonical original before linking; failed attachment retains it and the previous reference. A second Block link stores only a lightweight row. Unlinking preserves the original, master annotations and other Blocks.

Room **14**, explicit **13→14**, adds block_media_links with composite Block/role identity, mediaId index and restrictive Block/asset foreign keys. Repository validation and fresh/migrated database triggers enforce role/category compatibility. Block deletion dependencies include canonical links without weakening Goals/Routines/takes/legacy images/all-session-history protections. Referenced asset deletion rolls master-marker removal back before any original-file deletion. Schema14 matches KSP; schemas/migrations1–13 retained.

Legacy visuals stay readable through explicit fallback. Confirmed replacement atomically commits the canonical link and retires legacy metadata, then serialized conservative cleanup removes the old original; failure preserves the legacy row/file. Existing IMPORTED_LOOP takes keep their IDs, labels, Block links, exclusive files/recordings ownership, playback/export/deletion/recovery; recorded WAV and free M4A takes remain. No automatic adoption, startup copying or canonical pointers into legacy directories. Loop Setup's legacy importer remains until T1 convergence after K1, per the explicit L3 handoff.

Canonical and legacy visuals share the existing Block viewer in Focused Practice and the recording sheet. sourceBlockId ownership, Description/Reference arrangement, collapse/restore, PDF pages/zoom/pan and pinned capture/review controls remain. Audio is an association/metadata capability in L3; Block waveforms, A–B/speed/local annotations, Transcribe, keyboard and concurrent playback/capture modes remain later scope. [Architecture and legacy decisions](docs/BLOCK_MEDIA_ASSOCIATIONS.md).

JSON **7** / readers **1–7** stay data-only: canonical originals/metadata, Block media links and master annotations remain excluded. Empty-target restore creates no dangling media references; Library-only/linked targets remain protected transactionally. Editor/Library/Backup UI disclose limits. Keep originals separately until S2 complete recovery.

## Actual scoped verification and failures

- [Final build41739881932](https://github.com/mrayover/practice.logger/actions/runs/41739881932), source34edea0: debug/release/instrumentation compilation. [Unit gate41738532205](https://github.com/mrayover/practice.logger/actions/runs/41738532205), sourcedf17016: **51/51 units**, zero failures/errors/skips on unchanged data/domain code. Final80 changes only shared Block viewer sizing, measured transport height at enlarged text and version; its presentation is verified natively. Unit coverage includes13→14 and representative1→14/11→14/12→14 preservation, role/raw-write constraints, shared identity, failed replacement rollback, legacy retention, archive/reopen, master-marker deletion rollback, Block dependencies, JSON exclusions/restore and affected image/audio/recording/backup safeguards.
- [API26/100% install-over41739387573](https://github.com/mrayover/practice.logger/actions/runs/41739387573) and [API35/200% ownership41736583181](https://github.com/mrayover/practice.logger/actions/runs/41736583181), API26 ownership sourceee4c679 executes79; API35 ownership source2aca284 executes78; final80 retains their identical database/storage core: **seed, upgrade and shared-storage classes pass on each API**. Exact immutable76 code/payload is re-signed only with one disposable emulator test key, preserving every non-signature payload byte, then upgraded without clearing to79 on26 and78 on35. Final80 changes only shared visual sizing, measured enlarged-text transport height and version after that ownership core; no database/file/playback logic changes. Legacy row/byte hashes, imported MP3/M4A/WAV playback/rename/export, recorded/free takes, old visuals, canonical two-Block PDF/audio sharing/no copied bytes, rename/unlink/deletion refusal and retained master markers pass. The earlier API35 ownership run later fails its new-test sort-control lookup; the API26 ownership run is fully green.
- Final build80 editor/viewer/reopen [API35/200% 41739887086](https://github.com/mrayover/practice.logger/actions/runs/41739887086) and build79 [API26/100% 41739387573](https://github.com/mrayover/practice.logger/actions/runs/41739387573) pass **one UI workflow plus one cold-reopen each**. Both-role search/filter/sort/selection/current-selection/cancel, actual visual/audio picker imports, independent replace/remove, deliberate legacy transition, missing-original retry, portrait/landscape, canonical PDF pages during active practice/free capture, retained timer/capture, recording-sheet PDF zoom/pages/pinned Stop/review and durable links/originals are exercised. Screenshots reviewed. API26 build79 is a fully green five-class workflow; final80 changes presentation sizing at enlarged text and version, retaining the identical storage/domain/capture core. Five distinct native classes pass per API across scoped runs (**10 successful selected native executions**); no single full-matrix run claim.

Development failures/cancellations remain in WORK_LOG: one runtime-download class-setup failure (four classes,34 executed checks passed), baseline/current debug signature mismatch before seed, missing runner debug key before emulator, enlarged-text Name-chip visibility query, modal/background duplicate Show reference query and an immediate asynchronous Page2 label assertion. Live failure capture exposed an actual one-pixel PDF viewport at200% text; final80 retains a minimum180dp bitmap inside the existing scroll panel, with an explicit height check. A later active-capture screenshot/tree exposed zero Reference area at200%; final80 measures heading/capture/footer heights to cap the same scrollable transport and reserve Reference space. New bitmap/active-area assertions pass with unchanged capture/timing ownership. Corrected the runtime endpoint and explicit emulator-only signing/payload checks, then targeted lazy descendant scrolling/modal scoping/render completion waits without weakening assertions. Superseded setup/selector and interim viewport-scope runs were canceled; exact outcomes remain in WORK_LOG. The first build41734468616 passed50 units;51 adds JSON link-exclusion coverage and passes both41735971595 and41738532205. Cancellation of the interim build returned HTTP500; it completed successfully and is not classified canceled. No assertions were weakened and no unproven flake/pre-existing-defect claim is made. No full P5/F1/F2/broad lint rerun, physical-phone/vendor/provider/TalkBack or acoustic acceptance is inferred. Stage4 remains0/10 documented real sessions.

On the owner’s delivery-priority instruction, no further testing is started. Additional build80 API26 populated-link reinstall probe41740431256 was requested canceled while running; no passing result is inferred. The earlier fully green API26 build79 and final build80 API35 results remain the scoped evidence. No full regression was run.

**Exact next task: owner L3 seven-item phone verification using the linked checklist.** L1/L2 phone acceptance remains separately pending. **Next separately assigned milestone: K1 — Reference Piano Tool; T1 follows K1.** No K1/T1–T3/S0–S2 work begins.

---

## Retained L2 release — build76 historical verification

Updated November 3, 2026 (America/Los_Angeles).

## Current L2 delivery

**0.19.0/build76**, application/release source `e12b53f16e42fad98e6aa1a878fd05f70f9e5e75`. [APK](dist/practice.logger-0.19.0-b76-e12b53f/practice.logger-0.19.0-b76-e12b53f.apk) · [ZIP](dist/practice.logger-0.19.0-b76-e12b53f.zip) · [Build info](dist/practice.logger-0.19.0-b76-e12b53f/BUILD_INFO.json) · [L2 phone checklist](dist/practice.logger-0.19.0-b76-e12b53f/PHONE_CHECKLIST.txt). Install over build72 without uninstalling or clearing data. Permanent-key candidates73/74/75 and every earlier distribution are preserved. **L2 owner-phone acceptance is pending; L1 phone acceptance remains separately pending.** P5 remains complete and owner accepted.

App ID `com.mrayover.practicelogger.app`; unchanged permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. Publisher verifies package/version/signature/pinned identity. APK SHA256 `73fd502be5fd2b7d515a4391fdb02bc16f2aa7b5c4d192e1504e2e4bbaea243f`; ZIP SHA256 `3472bf45b917e5a4ace436a9d2c29d056a7c543024fe990de84eb3e372a08f97`. All67 non-signature CI payload entries match; APK/ZIP CRC and exact ZIP member bytes pass. All314 session-entry APK/ZIP hashes remain unchanged; candidates73/74/75 are also retained. Higher versionCode, stable package/key and explicit12→13 migration support install-over; physical permanent-key phone installation is not inferred from automated checks.

## Features and ownership

Library images/PDFs open in dedicated previews, sharing existing EXIF-oriented bounded decoding, PdfRenderer pages, zoom/pan/reset and retry. Query/filter/sort/list scroll are retained on return; original bytes and Block reference behavior remain.

The reusable Android MediaPlayer controller provides play/pause/stop, source-time seeking/cursor/duration, independent pitch1.0 with50–100% speed in5% steps, whole-file repeat, one-shot selected phrases and A–B repeat. Shared waveform/viewports support real-signal peaks, zoom/pan, separate draggable A/B handles, numeric source-second edits and10ms adjustments. Transport/range readouts stay independent of the scrollable marker list. Foreground/focus/headphone-error handling releases resources without automatic background/process resume; configuration recreation retains active playback. No session, metronome, microphone, take selection or statistics ownership is introduced.

Room **13** adds only indexed `media_markers` through explicit **12→13**; schemas/migrations1–12 are retained. Stable master point/section IDs, labels, optional notes and source-time bounds support add/edit/move/resize/confirmed-delete, seek and section selection. Loading a section does not enable repeat. Restrictive FKs plus transactional owned-marker removal preserve deletion safety and roll back annotations when another asset link blocks deletion.

Waveform analysis streams PCM chunks/codec buffers into5ms peak bins (at most180001floats, ~704KiB/15minutes). Expendable `cache/audio-waveforms` files use mediaID/originalSHA256, validate headers/count/peaks, regenerate on corruption and cap completed caches at12. Originals, L1 formats/limits/integrity, legacy references/takes and byte-preserving export remain under existing ownership. [Architecture and limitations](docs/MEDIA_AUDIO_WORKSPACE.md).

JSON **7** / readers **1–7** remain unchanged and exclude canonical originals/metadata/master annotations and legacy media. UI discloses this boundary. Keep originals separately until S2 complete ZIP recovery; waveform caches are not backup content. No L3 links/adoption, K1, T1–T3 or S2 backup is implemented.

## Actual focused verification

- Platform gate, source76f0d69: [API26 CI41713276728 attempt2](https://github.com/mrayover/practice.logger/actions/runs/41713276728) and [API35 CI41713272359](https://github.com/mrayover/practice.logger/actions/runs/41713272359), one native scenario each, pass MP3/M4A/PCM16 WAV speed100/75/50/95%, independent pitch parameter1.0, source-position advances, repeated closest seeks and pause. First API26 attempt timed out downloading fixture dependencies before compilation.
- Final build [CI41721617835](https://github.com/mrayover/practice.logger/actions/runs/41721617835), source`e12b53f16e42fad98e6aa1a878fd05f70f9e5e75`: **37/37 scoped units**, zero failures/errors/skips; debug/release/instrumentation compilation pass. Includes explicit1→13/11→13/12→13 preservation, master CRUD/reopen/FK/deletion rollback,15minute coordinate math/range validation and existing Library/import/image/recording/phrase/backup safeguards. KSP13 matches the retained exported schema. No broad lint/P5/F1/F2 rerun.
- Source37a9234: [API26/100% CI41716550157](https://github.com/mrayover/practice.logger/actions/runs/41716550157) and [API35/200% CI41716555437](https://github.com/mrayover/practice.logger/actions/runs/41716555437), **four workspace tests plus one cold-reopen per API** all pass. Actual three-format transport/50–75% phrase-repeat/one-shot/full-repeat/EOF/focus/background/error, faithful alternating-signal fifteen-minute waveform/cache regeneration/original hashes, existing Block PDF viewer/no practice-row change, Library image/PDF error/retry/list state and master creation/many dense markers/cold persistence are covered.
- Final UI/inset checks, source`e12b53f16e42fad98e6aa1a878fd05f70f9e5e75`: isolated UI/reopen [API26/100% CI41723099027](https://github.com/mrayover/practice.logger/actions/runs/41723099027) and [API35/200% CI41721633323](https://github.com/mrayover/practice.logger/actions/runs/41721633323) pass. They explicitly assert settled rotation during active A–B playback, retained speed/range, fully bounded narrow(~305dp) transport controls, status-bar-safe Back, at least48dp of marker list and reachable waveform/marker navigation. Screenshots reviewed. Dedicated previews hide the banner and use compact readouts for enlarged text. API26 workflow source d08d721 only adds exact successful-build fixture reuse; its app/test assertions match signed source e12b53f. Earlier controller/waveform/storage/Block code is unchanged. Evidence is scoped across these runs, not a new full regression claim.

Development outcomes retained in WORK_LOG: one fixture JSON error, two runs with the same Kotlin callback-inference compile error, superseded canceled build, one canceled unintended broad push run, search/IME row-lookup failure, and two final API26 fixture-install timeouts before compilation. Keyboard settlement corrected the lookup failure. Settled screenshot review exposed combined large-text/narrow panel starvation and preview top-inset overlap; build76 corrects both and adds marker-area/status-bar-clearance assertions. Final API26 reuses the identical synthetic audio fixtures from successful build41721617835. No assertions were weakened. The independent platform and integrated targeted runs pass; acoustic pitch, very short compressed-loop gap/overshoot, vendor headphones/providers/TalkBack and permanent-key owner-phone install-over remain physical checks. No sample-accurate or perfectly seamless compressed playback is claimed. Stage4 remains0/10 documented sessions.

**Retained L2 phone action:** its separate seven-item phone verification remains pending. Current L3 delivery/next action is above. L1 phone acceptance remains separately pending; do not reopen L1 engineering or P5 without defect evidence.

## Previous L1 delivery — retained build72

**0.18.0/build72**, application/release source `ef83bd8ae3e34804dd436cbbdcdf3ac023530e19`. [APK](dist/practice.logger-0.18.0-b72-ef83bd8/practice.logger-0.18.0-b72-ef83bd8.apk) · [ZIP](dist/practice.logger-0.18.0-b72-ef83bd8.zip) · [Build info](dist/practice.logger-0.18.0-b72-ef83bd8/BUILD_INFO.json) · [L1 phone checklist](dist/practice.logger-0.18.0-b72-ef83bd8/PHONE_CHECKLIST.txt). Install over accepted build69 or candidate71 without uninstalling or clearing data. L1 owner-phone verification remains pending; P5 stays complete and accepted.

App ID `com.mrayover.practicelogger.app`; permanent certificate SHA256 `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`. Existing publisher verifies package/version/signature/pinned identity. APK SHA256 `7ee01ca7bca9e96608e019f27ac991e5cb249f07ad1454a7fa024340dbf3db78`; ZIP SHA256 `a93aca60b1ad338441ae93dcc10160be6b454cf019b41fec484ebf9d1cae4edd`. All67 non-signature CI payload entries, APK/ZIP CRC/exact member bytes and298 prior APK/ZIP hashes pass. Existing packages/signing material are preserved; dist/LATEST now points to the L2 delivery above. Internal70 predates integrity hardening and is not the current installation target.

## L1 implementation and ownership

Room **12** adds only indexed media_assets through explicit **11→12**, retaining schemas/migrations1–11 and all existing rows/FKs. KSP export12 identity `219b9e3550bfaafd1b07fb3551935484` matches implementation. Each stable UUID owns one original in files/media-library, independent of Blocks. Streaming SHA256 supports original integrity checking without content-addressed storage/deduplication. Imports reuse existing image/PDF and MP3/AAC-in-M4A/PCM16 WAV validators and resource limits, bounded IO/sync/staging/transactional commit, cancellation and conservative serialized recovery.

Home → Library now provides imports, search over title/original filename, All/Images/PDFs/Audio, stable Name/Most recent ordering, title-only rename, metadata/availability errors and confirmed deletion. Future restrictive durable links block deletion; no premature L3 association tables exist. New files/recovery cannot enter legacy storage. [Implementation and legacy audit](docs/MEDIA_LIBRARY_STORAGE.md).

All legacy block_images/files/block-images and takes/files/recordings retain existing ownership, IDs, labels, Block associations and original bytes. Imported tracks remain playable/renameable/exportable through existing controls; recorded WAV/free M4A are not reclassified. Nothing is disposed or physically copied for migration. In-place adoption requires L3 to update ownership/links, resolution, deletion and recovery together; legacy media is not automatically listed in the new Library.

JSON writer **7** / readers **1–7** remain unchanged and exclude canonical/legacy media metadata and bytes. Library-only installs now block JSON restore transactionally. Library/Backup UI disclose the exclusion; keep originals separately until S2. At L1 delivery, no L2 viewer/player/annotation was implemented; L2 is now delivered above. Complete ZIP backup, L3 Block linking, K1 and Transcribe remain unimplemented.

## Actual focused verification

- [CI41705910512](https://github.com/mrayover/practice.logger/actions/runs/41705910512), storage source `5da4f17`: debug/release/instrumentation compile and **34/34** scoped units pass. Coverage includes explicit1→12/11→12, all old rows/FKs, import limits/provider/content/MIME/space/cancel/finalization/commit recovery, active-import protection, same-size corruption detection, unique ownership/rename/delete, reopen, stable filtering/sorting and Library-only restore across readers1–7. This run later failed a new-test lazy-row lookup; its passing build/unit/storage stages are reported separately. Final build72 only changes Library selections to existing OutlinedSelectionChip and version; storage/validation/restore are unchanged. Final release compilation passes [CI41708998178](https://github.com/mrayover/practice.logger/actions/runs/41708998178).
- API35/200% [CI41705910512](https://github.com/mrayover/practice.logger/actions/runs/41705910512) and API26/100% [CI41706664910](https://github.com/mrayover/practice.logger/actions/runs/41706664910): actual immutable build69 seed → install71 without clearing, raw legacy metadata/byte hashes, imported playback/rename/export, original Block protections and canonical native format/failure/dependency checks pass. Both stop later at test-only offscreen row lookups.
- Isolated Library/reopen checks pass on API26/100% [CI41709010676](https://github.com/mrayover/practice.logger/actions/runs/41709010676) and API35/200% [CI41709004489](https://github.com/mrayover/practice.logger/actions/runs/41709004489): real picker cancel/success, imports/failure, search/filter/sort/rename, canceled/confirmed deletion, recreated query, portrait/landscape controls and cold process restart with original canonical/legacy hashes.
- All **five distinct native classes pass on each API**, across targeted runs per API (**12 selected successful executions**, including repeated seeds). There is no single all-green final full matrix claim. Final Library/reopen tests compile and execute build72; earlier ownership/storage checks execute the identical storage core. Production code is unchanged since `ef83bd8ae3e34804dd436cbbdcdf3ac023530e19`; later commits update records. Final native test/source SHAs are recorded in BUILD_INFO/WORK_LOG. Screenshots were inspected. Earlier failures/cancellation and fixes are retained in WORK_LOG.

No P5/F1/F2 full regression, broad lint rerun or owner-phone/provider/vendor/acoustic acceptance is claimed. Stage4 remains0/10 documented real sessions. Physical phone checks are separate from automated evidence.

L1 owner verification using its six-item phone checklist remains pending. L2 has since been delivered; its actual verification and exact next phone task are above. P5 remains complete and accepted.

## Build69 camera/gallery owner acceptance

On November3,2026 the owner confirmed build69 camera/gallery functionality works correctly on their phone. This closes its outstanding owner-phone action. Device specifications and individual edge-case observations were not supplied or inferred. P5 remains completed and owner accepted.

Prior signed **0.17.3/build69**, application/test source `9c723442d8f3eb1dd8c2d23cc76dd3af4de7e1d3`: [APK](dist/practice.logger-0.17.3-b69-9c72344/practice.logger-0.17.3-b69-9c72344.apk) · [ZIP](dist/practice.logger-0.17.3-b69-9c72344.zip). APK SHA256 `5eb33857e214490dfc196482c85c49e587df86ecfa21d59505fc4fbcafb31385`; ZIP SHA256 `1ec2120c694d0baae4d8077c57bb5145543ef5caf1fdabd74e7d00f076cd5b0d`. Targeted CI41363227308 passed release compilation and11/11 reference-photo/image/PDF checks. Package identity/signature/pinned certificate/CI payload/ZIP/prior-package checks were verified at delivery. Earlier source/records remain in Git history.

## Accepted P5 baseline

Updated November 1, 2026 (America/Los_Angeles). **P5 outcome: PASS — automated verification, signed delivery and owner acceptance complete.** [Full CI41329443572](https://github.com/mrayover/practice.logger/actions/runs/41329443572) passed all nine required jobs on `bf999b9678039dbf669560cd10649b340c11e217`, branch `main`: 111/111 units, 168/168 native executions, lint zero errors/18 advisories, debug/release/instrumentation compilation. Only the optional diagnostic job was skipped.

## Accepted pre-Library release

**0.17.1/build67:** [APK](C:/Users/mrove/Projects/practice.logger/dist/practice.logger-0.17.1-b67-bf999b9/practice.logger-0.17.1-b67-bf999b9.apk), [ZIP](C:/Users/mrove/Projects/practice.logger/dist/practice.logger-0.17.1-b67-bf999b9.zip). Application and test source: `bf999b9678039dbf669560cd10649b340c11e217`. Later record-only commits are separate.

- APK SHA256: `460a2e16f52e4aeec9988abe96bc8f82c247dffceccff8ba280406c548f9b496`.
- ZIP SHA256: `9b43b40f6612f7efdf3e12b8950a2fd6e956390dc28245af308cb0c495940c95`.
- Permanent certificate SHA256: `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830`.

Stable app ID `com.mrayover.practicelogger.app`. Publisher package/version/signature/pinned-certificate checks pass; all 140 unsigned CI payload entries match; APK/ZIP integrity and five exact ZIP members verified. Install over the permanent-key app without uninstalling/clearing. Existing distributions, build66 and intermediate ad1-source67 remain preserved; use the final bf999b9 package linked above.

## Evidence and preserved scope

Per API: 49 ordered device executions, 7 review/max-128-second-loop, 20 transport/Settings/Notes/reference and 8 appearance/import, 84 total. All 57 classes/77 native scenarios plus 7 ordered upgrade/large-text repeats execute on both APIs. Migration/install-over, recovery/reopen, backup, original media/data, normal/200% text, keyboard/configuration and landscape checks pass. See [P5 report](docs/P5_REGRESSION_REPORT.md) for all 36 attempts, failure classifications and 18 lint advisories; [baseline](docs/P5_REGRESSION_BASELINE.json) retains exact case/result/schema/job metadata.

Earlier P5 product fixes are limited to qualified theme attributes, full-width references and enlarged Accent/recording-control sizing. Current tests follow explicit independent Notes, inline review and saved-Block management. This completion assignment changes only AudioSettings test cleanup after evidenced stale Compose idling; targeted checks and the final full gate pass. No audio/data engine or approved UI redesign.

Room11, explicit migrations, JSON7/readers1–7, stable IDs, original references/audio and permanent signing remain. JSON excludes all takes/audio and image/PDF metadata/bytes; retain originals/export audio separately. All 11 compiler schemas and all 282 session-entry APK/ZIP hashes match. Synced sources and owner phone/data were untouched. Owner authorized Git command publication; mandatory efficiency procedure reviewed/preserved.

## Owner acceptance and next action

Owner confirmed on November 1, 2026: “Confirmed complete and running correctly”. This accepts the delivered 0.17.1/build67 in the context of the P5 handoff and closes P5. Phone model, Android version and itemized checklist observations were not supplied; no additional measurements or individual check results are inferred.

No additional P5 work is pending. Build69 camera/gallery is now owner accepted; L1 is explicitly assigned. Its current status and exact next action are above. Imported repeat retains its documented lack of a certified seamless-join guarantee; JSON media exclusions remain. Stage4 remains 0/10 documented real sessions.
