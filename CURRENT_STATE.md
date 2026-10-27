# Current state

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

Local source and GitHub main matched faca5e5 and the checkout was clean before these documentation edits. GitHub Desktop automation failed with "Computer Use native pipe is unavailable" after retry and reset. Therefore release records are saved locally but NOT committed or pushed. Git mutations remain Desktop-only; no CLI/API commit or push was used. The root records now supersede the stale stage6b-edit release templates and older handoff.

Exact next task: in GitHub Desktop, review and commit the six changed release documents as "Document signed 0.6.0 build 11 phrase-loop delivery", then Push origin and verify the remote SHA. After publication, install build 11 over the existing app; record 8 beats at 120 BPM after four clicks, listen across ten repeats, stop/restart/reopen, retain a free take and second loop, export/play WAV, and check Done/Back/scrolling at large text. Report phone/Android/output route and onset/boundary findings, then discuss save/storage before implementation. Physical loop acceptance remains pending.
