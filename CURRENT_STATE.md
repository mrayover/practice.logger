# Current state

Updated 2026-10-27 America/Los_Angeles. **0.7.0/build 13** implements the owner's supplied home sketch. Engineering checks and signed local packaging are complete; owner phone/visual acceptance remains pending. The owner's explicit visual scope allows inactive Goals settings/Edit and a Data usage label. Functional Goals/reusable routines, future tools and broader save/storage remain deferred. Stage 3 phone backup acceptance remains pending; Stage 4 stays0/10.

## Home layout and destinations

- Separated green header (#315C47) with centered white practice.logger in bundled Lato Bold; status icons contrast with the header.
- White body and app background/surfaces; blank two-row Goals panel with inactive settings/Edit controls.
- Five green rectangular rounded buttons in exact order: Practice Now, Blocks, Routines, Tools, Recording.
- Plain Data usage footer; no fabricated measurements. Home scrolls when screen height/text scaling needs it.
- Practice Now → existing session builder/selection. Blocks → existing Block Library/editor.
- Routines → existing practice overview, Continue Session, individually selectable unfinished sessions and recent history. It is not a reusable-routine engine.
- Tools → tools landing with existing Backup and restore access; no standalone tuner/metronome was invented.
- Recording → select an active/archived Block and use the existing per-Block recording sheet. Back/Close retains navigation and stops recording/audio through existing lifecycle rules.

Existing catalog/search/archive, draft builder, foreground monotonic timer/zero/overtime, independent unfinished sessions, saved notes/history, versioned empty-target JSON restore, click and multiple free/loop recordings are preserved. Default-enabled Click while recording from0.6.1 remains; four count-in clicks continue through one chosen phrase at its BPM when enabled, off retains count-in only. No audio engine, database/schema5/migration, backup-format3, app identity or key change. All app text retains Lato. Goals/settings/Edit are intentionally disabled visual placeholders as requested, not missing implemented features.

## Verification

Tested app source main: 61b76d50e1ce0dc995cf4775d6d1840a5d10f007
Successful CI: https://github.com/mrayover/practice.logger/actions/runs/40620587011
build 122593365745 SUCCESS; device (35) 122594517794 SUCCESS; device (26) 122594517934 SUCCESS.
All 33 unit/Robolectric tests pass,0 failures/errors/skips. Lint:0 errors/10 warnings. Android 8/API 26 and Android 15/API 35 pass initial expanded catalog/home workflow plus17 direct native phases. Home tests exercise all five menu routes, blank/inactive Goals and empty/populated Recording selection. Existing recovery/history/backup/system-picker/install-over/metronome/free-recording/phrase/timer/export checks follow explicit new Routines/Tools paths and retain native/data assertions. Upgrade test uses code 14.

Downloaded normal and200% home screenshots were visually reviewed against the sketch. Normal API 26/35 screenshots show centered bold Lato wordmark, white body, two blank numbered Goal rows, five green buttons in the sketch's order and fully visible Data usage footer. At200% text the header remains centered and the body scrolls to expose all labels/footer without clipping. Automatic text-layout checks cover header, Goals, all five buttons and footer at200%; no physical phone visual or audio test is claimed. Device phrase/free-recording crash buffers remain empty. Downloaded reports and unsigned APK match official SHA256 metadata. No local Android SDK/compiler/emulator was installed or used.

Initial source0af24df/CI40619032902 failed Kotlin compilation on the Edit styling reference TextButtonDefaults. Corrected to Material3 ButtonDefaults.textButtonColors before the successful source; no failed-run APK distributed. The next sourced2b8126/CI40619311941 passed build/tests/lint and native workflows, but the new200% Home label-bounds check exposed intrinsic Text width versus centered paragraph allocation. Button text now fills its content width. Normal screenshot review also prompted tighter blank Goal rows and spacing to keep the Data usage footer visible. WORK_LOG retains both corrections and earlier verification history.

## Signed distribution

ZIP: dist/practice.logger-0.7.0-b13-61b76d5.zip
APK: dist/practice.logger-0.7.0-b13-61b76d5/practice.logger-0.7.0-b13-61b76d5.apk
BUILD_INFO.json and INSTALL.txt accompany the APK; dist/LATEST.txt points here.
APK SHA256: 2e66741ae596ed76c0cf15c9755847f28d8c6f327392c8716b5e36c640cbd144
Permanent certificate SHA256: f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830

Existing Publish-LocalApk.ps1 verified signature, pinned permanent certificate, stable package com.mrayover.practicelogger.app, version 0.7.0/code 13 and ZIP-extracted APK checksum. Android 8+; install over the existing permanent-key app without uninstalling. All 30 earlier release APK/ZIP files remain byte-identical. Old temporary-key0.1.0 stays separate. Private key unchanged/not uploaded. Temporary-key CI debug APK is unsuitable for updating this installation.

CI artifacts (30-day retention), under the successful run's /artifacts/ path:
- device-test-reports-api-35: 12787766147; sha256:07e86e01141f357c7cb8ddd7a8d608309678c4cfea6fe3372ccb6b805917295d
- device-test-reports-api-26: 12787278000; sha256:3749f9f08f0929a80b5dc363a7647ad72e7f83a5364c5f10d03b02406ed42ef5
- practice-logger-debug: 12786859071; sha256:4e13db529aa58092f22002fd644b3d8405140bc257f50e2b7cdc2a8c742f96a6
- verification-reports: 12786709425; sha256:e79b0262a749e68196665802d833092bd7e1380872aae797fcffe52c07b2b271
- practice-logger-unsigned-release: 12786669607; sha256:4d560a34b554a71d270eac8cdd136bd7db0f9bc5de137ab331d70dde1dcefad8

## Limits and exact next task

Data usage is only the requested footer label. Goals/routine editing and future tools are not implemented; enabled home buttons open the meaningful existing destinations above. JSON3/legacy readers protect practice database data only, excluding all take metadata/labels/M4A/WAV; export wanted takes separately. No audio import/bulk restore; uninstall/clear/device loss destroys unexported audio. Failed export may leave a partial destination while keeping the original. Saved notes require a save/leave/complete action. Abrupt process death can lose the latest uncommitted timer/BPM interval (250 ms plus write latency) or an unsaved capture. Physical audio onset/boundaries and keyboard Done/Back/lower-control access remain phone checks from the preceding release.

Owner selected option1 explicitly authorizing Git commands for this redesign and release closeout because Desktop controls were unavailable. Existing Windows checkout used; app source is committed/pushed. Closing release records follow as a separate commit/push and final local/remote SHA/clean status is verified in the handoff. Default Desktop preference remains for future work. No cloud checkout/PR/alternate repository.

Exact next task: owner installs signed0.7.0/build 13 over the current app, compares Home to the sketch at normal/large text, checks all five routes and Back, continues an existing session under Routines, reopens history/recordings and confirms data remains. Goals/Edit/settings and Data usage are intentionally nonfunctional. Collect actual phone/Android and any visual adjustments; then discuss save/storage if desired before implementing it.
