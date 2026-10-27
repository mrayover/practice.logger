# Current state

## Current task — optional click during loop capture, 0.6.1/build 12 pending

2026-10-27 America/Los_Angeles. Owner generally accepts 0.6.0/build 11 ("it's working great!") and requests an audible click while recording a loop, followed by the save/storage discussion. No phone model/OS or individual timing results inferred. Stage 3 manual backup acceptance stays pending; Stage 4 stays 0/10.

Implementation is prepared locally: Record phrase has a full-row accessible "Click while recording" checkbox, enabled by default. Four clicks always count in. If enabled, the same native output track continues clicks at the chosen phrase BPM through microphone capture; if disabled, only the four count-in clicks sound. The ordinary metronome is stopped before phrase startup to avoid overlapping/focus-competing click streams. The guide stops before saved-loop playback and on cancellation/background/error. No digital click is mixed into the saved WAV, but the microphone can hear the speaker. The timer and saved phrase duration/tempo stay independent. Existing FREE/LOOP files, schema 5, JSON exclusions, identity and permanent key remain unchanged. Save/storage redesign is deferred until the owner discussion.

Validation completed: three PhraseAudioTest JUnit tests pass locally using cached Kotlin 2.2.20/JRE 17 on the unmodified pure PCM/WAV declarations extracted from production PhraseAudio.kt and the actual test file. This checks optional guide samples, unchanged count-in, exact output length and beat positions at 30/60/73/120/180/299/300 BPM across all supported lengths, existing frame validation and WAV roundtrip/corruption. This isolated check does NOT compile Android/Compose/native code or establish audio on a phone. First CI40599974541 passed full build/33 unit tests/lint and API26; API35 exposed a live-capture test timing race at 180 BPM. The assertion now samples active mode and native guide counter in the same poll; production audio is unchanged. New full CI is pending. No new installable APK is claimed. Last successful released-source CI remains https://github.com/mrayover/practice.logger/actions/runs/40566516233 for build 11, not this fix.

New instrumentation checks toggle the option, verify the real guide playback counter advances during microphone capture, exercise count-in-only mode, retain existing exact WAV/native repeat/export/timer/lifecycle assertions and check 200% text accessibility. Target release is 0.6.1/code 12; upgrade regression uses 13. Dist/LATEST.txt still points to the unchanged signed build 11 until the new source passes CI and is permanently signed.

Publication: existing Windows checkout was clean at local/remote main f6e38e0657c98747117a78d6588850406da3dac9. Fourteen intended files (eight source/config/test plus six operational documents) are prepared locally but uncommitted/unpushed. Computer-use tools are unavailable in this session. The owner explicitly authorized Git commands for this update (selected option 1), overriding Desktop-only for the 0.6.1 fix and release closeout. Source commit/push and CI verification now proceed in the existing checkout; future work retains the Desktop preference.

Exact next task: commit and push the 14 reviewed files using the owner-authorized Git-command exception as "Keep optional click audible during phrase recording". Verify the new source SHA and complete full CI (expected 33 unit tests, lint, Android 8/15 native/large-text suites); fix any failure, inspect screenshots, and sign 0.6.1/build 12 with the existing publisher/key, retaining all prior releases. After installation, hear four clicks followed by the phrase-tempo click throughout an 8-beat recording, try the option off, cancel/background, stop/reopen the saved loop, then discuss save/storage. Do not treat this prepared fix as a delivered build.

## Previous delivered build — provenance and retained limitations
Updated 2026-10-27 (America/Los_Angeles). Stage 6B phrase looping is implemented, tested and locally packaged as 0.6.0/build 11. The owner generally accepted the 0.5.0 free recorder ("It all works"); physical loop acceptance remains pending. Stage 3 phone backup acceptance remains pending and Stage 4 stays 0/10. Save/storage changes await the owner discussion after looping.

## Implemented behavior

Focused Practice or a saved/archived Block editor → Recordings → Record phrase. Choose 30–300 whole BPM and 4/8/16/32 beats (default 8). Four output-clock clicks precede one fixed-length microphone phrase; a new LOOP take is saved and immediately repeats until stopped. The practice timer remains independent. Multiple loops and free M4A takes coexist per stable Block ID, with playback, optional labels, confirmed deletion and per-take document-picker export. Saved BPM/beats are immutable; ordinary working BPM changes never change an existing loop's speed or pitch.

Loops use PCM16 mono 48 kHz WAV with round(60 × 48000 × beats / BPM) frames and static AudioTrack loop points. Export writes one phrase, not infinite repeat. Input/output latency is not calibrated: count-in alignment, route behavior, perceived boundary gaps/clicks and microphone quality require the owner's phone. No seamless or latency-free claim. No overdubs, trimming, layers or time stretching.

Room schema 5 and explicit 4→5 migration add nullable recordedBpm/beatCount while retaining existing free takes, labels, files and practice data. Schemas 1–5 and prior migrations remain; compiler identity is d5085287269b321d2d337709c8a8f308. Incomplete cancelled/backgrounded phrases are discarded with feedback; previously saved takes stay intact. Validated exact-frame pending files are synced/renamed before metadata commit. Recovery retains saved/active files and cleans generated orphans. Audio stops on leave/background/recreation/focus loss/noisy route and never resumes automatically. Free capture behavior and existing practice/history/backup workflows remain.

## Verification

Tested app source: faca5e5c01e0dfed9136dd58cf4a771b1362b6c8 on main.
CI: https://github.com/mrayover/practice.logger/actions/runs/40566516233
All three jobs succeeded: build 122409112869, API 26 122410616196, API 35 122410616399. Downloaded verification XML confirms 32 unit/Robolectric tests, zero failures/errors/skips. Lint: 0 errors, 10 existing warnings. Both device reports pass initial catalog plus 17 direct phases: sessions/timer/notes/history/recovery, upgrade to test code 12, backup/system pickers, independent sessions, metronome, free recordings/reopen, phrase loops/reopen and 200% text.

Native phrase checks verify exact WAV frames at 60/120/180 BPM, native repeat counters past two phrase boundaries, timer independence, frozen recorded tempo, cancellation/background stop, real Downloads export with matching full-file hash, failed-export source retention, labels/deletion and archived Block/process reopen. Both downloaded phrase and free-recording crash buffers are empty. Unit tests also cover all whole BPM 30–300 at all supported lengths, four-click PCM, malformed WAV and schema 4 migration with FREE/LOOP coexistence.

Downloaded API 26/35 reports and normal/200% screenshots were reviewed. The shortened Phrase BPM floating label and separate 30–300 range are readable; API 35 enlarged setup shows all beat choices and actions. The normal setup captures still show the keyboard immediately after the test's Done action; API 26 enlarged captures also retain it. These images do not establish reliable Done dismissal. Include keyboard hiding and lower-control access in phone acceptance; do not claim that specific behavior passed visual verification. No app source change was made in this delivery session. No local Android build/emulator or physical phone test is claimed.

Earlier build 10/source 502fb55 is retained as a candidate. Its long floating label clipped at 200% text; build 11/source faca5e5 shortens the label and adds supporting range text and Done handling. The earlier static AudioTrack startup failure at source 2492291 was fixed in 502fb55. WORK_LOG preserves prior failures and acceptance history.

## Signed distribution

ZIP: dist/practice.logger-0.6.0-b11-faca5e5.zip
APK: dist/practice.logger-0.6.0-b11-faca5e5/practice.logger-0.6.0-b11-faca5e5.apk
BUILD_INFO.json and INSTALL.txt accompany the APK; dist/LATEST.txt points to build 11.
APK SHA-256: cad7e7b4a85f1542388f4e4c3c309e0a86892ec325fe713f75a26b6b99d9aeea
Permanent certificate SHA-256: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830

Existing scripts/Publish-LocalApk.ps1 signed and verified signature, pinned certificate, stable package com.mrayover.practicelogger.app, version 0.6.0/code 11 and ZIP-extracted APK checksum. Android 8+; install over the permanently signed 0.5.0 or build 10 without uninstalling. The old 0.1.0 app remains separate. All 24 pre-loop baseline APK/ZIP hashes and the retained build 10 files were rechecked unchanged. Private signing material was neither changed nor uploaded.

CI artifacts (30-day retention) under the successful run's /artifacts/: unsigned release 12764300250, verification 12764190331, debug 12764040362, API26 12763841228, API35 12764750247. Downloaded unsigned/verification/API26/API35 bytes match GitHub's SHA-256 metadata. Temporary-key CI debug builds cannot update this signed installation.

## Data limits and publication

JSON writer 3/readers 1–3 still protect practice database data only: no take metadata, labels, M4A or WAV. Export wanted takes separately. No audio import/bulk restore; uninstall/clear/device loss destroys unexported audio. Failed export may leave a partial destination while retaining the source. Existing unsaved-note/checkpoint/process-death limits remain.

Build 11 release records were subsequently committed/pushed by the owner: local and remote main now match f6e38e0657c98747117a78d6588850406da3dac9. The prior checkout was clean before the current click fix. Git mutations remain Desktop-only. The current session has no callable Windows computer-use tools; publication of the new changes needs the owner in Desktop.

Previous build 11 handoff (superseded by the current task above): in GitHub Desktop, review and commit the six changed release documents as "Document signed 0.6.0 build 11 phrase-loop delivery", then Push origin and verify the remote SHA. After publication, install build 11 over the existing app; record 8 beats at 120 BPM after four clicks, listen across ten repeats, stop/restart/reopen, retain a free take and second loop, export/play WAV, and check Done/Back/scrolling at large text. Report phone/Android/output route and onset/boundary findings, then discuss save/storage before implementation. Physical loop acceptance remains pending.

Verification follow-up: run40601576976 passed build/33tests/lint, but API35 fixture setup raced asynchronous Room publication before recording. Test click helpers now wait for exact enabled controls; new fullCI pending. Production click implementation unchanged since5e86e31. Owner Git-command authorization remains active for this update.

Verification follow-up: API26 of run40602791872 exposed timer UI actions outlasting the short phrase; screenshot confirms saved looping audio and timer running. Timer-independence interactions now run during a32beat capture, while original8beat exact-frame/repeat/export assertions remain. New fullCI pending; production audio unchanged.
