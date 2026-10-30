# Current state — owner-test preview 0.16.0/build36

Updated2026-10-30 (America/Los_Angeles). **Signed, installable owner-test preview; full regression verification pending.** The owner explicitly limited this layout-only preview to compilation, a basic launch check and package verification. Normal release requirements remain unchanged. No further automated work is scheduled.

## Delivery and provenance

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code36 |
| Signed app source | `5c807084c68fecfc8e4c0ac3bbc768cd60025341` |
| Build and smoke launch | [Single owner-preview run41083507676](https://github.com/mrayover/practice.logger/actions/runs/41083507676) |
| Package | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `61e5df273a67e53d2d8fa42fde2152a8cb56d8e8893dd09f42922ea2568e9732` |
| ZIP SHA256 | `190db700afbcf1f4e3dd2e8c7cdd437384041fa8784752bdcc99cfdd343d8ed9` |

[Installable APK](dist/practice.logger-0.16.0-b36-5c80708/practice.logger-0.16.0-b36-5c80708.apk) · [ZIP](dist/practice.logger-0.16.0-b36-5c80708.zip) · [build info](dist/practice.logger-0.16.0-b36-5c80708/BUILD_INFO.json) · [preview status](dist/practice.logger-0.16.0-b36-5c80708/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b36-5c80708/PHONE_CHECKLIST.txt) · [latest pointer](dist/LATEST.txt).

Install **over the existing permanent-key app without uninstalling or clearing data**. The publisher used the existing key and verified the pinned certificate, cryptographic signature, package ID and version. ZIP members round-trip byte-for-byte, including the APK. APK/ZIP are local ignored artifacts. Documentation commits follow the signed app-source commit; no later app changes are implied.

## Exactly what changed

The owner approved build35's expanded reference size and requested a compact reference until clicked. Reference Material now starts as a48dp Expand header for each placement. Tap it to expand to the approved workspace allocation and display the attached image/PDF immediately; tap Collapse to return to the small header. The collapse header stays outside reference-content scrolling. A local saveable flag controls presentation. A spacer retains the lower secondary-control placement while collapsed in ordinary portrait. Description remains32–80dp; short-screen/enlarged-text/open-Notes scrolling, the220dp media viewport and pinned main/active-recording actions remain.

PracticeScreens.kt owns the outer toggle. ImageReferenceView.kt adds a default-off embedded-expanded option so the practice panel opens media in one tap; editor and recording-sheet toggles retain their defaults. VersionCode36; no data/timer/audio/navigation/reference-loader changes.

A separate manual owner-preview workflow compiles debug/release and performs one clean API35 debug launch smoke check. Existing android.yml, its full regression gates and permanent development rules are unchanged. Source changes were committed with automatic full CI skipped under the owner's explicit preview exception.

## What was tested

- Debug and unsigned release compilation pass in the single run above. No full unit/lint tasks requested.
- One clean API35 emulator: debug APK install, successful Activity start, live process after launch, visible app UI and no app crash in the smoke window pass. This is a launch smoke check; it does not validate the revised Focused Practice layout on a phone or launch the permanent-key release APK.
- Publisher verifies signed release identity/version/signature/pinned certificate and APK hash; final archive audit verifies ZIP integrity and every member against the delivered folder.
- All134previous APK/ZIP files retain their recorded SHA256. Static source comparison confirms only PracticeScreens.kt, ImageReferenceView.kt presentation and versionCode changed in app/, with no data/schema/migration/audio/backup/manifest/dependency edits. Git whitespace checks pass.

## What remains unverified

**Full regression verification pending.** No full units/lint, complete API26/API35 suites, enlarged-text/media/audio/session regressions or screenshot automation were run for build36. Existing full-regression failures and cancelled routine diagnostic from build34 are retained in WORK_LOG; they were not investigated or claimed fixed. Prior build34 evidence does not establish build36 regression acceptance. Physical signed-APK launch/install-over/data retention and actual portrait/landscape/large-text layout, reference/Notes interactions, recordings, acoustic output and TalkBack remain owner checks.

Room10/schemas/migrations1–10, JSON6/readers1–6 and media exclusions remain. The original temporary-key0.1.0 installation is separate. Stage3 phone acceptance remains pending; Stage4 stays0/10real sessions. Build35 and all earlier deliveries remain available; build35 delivery evidence is in Git at328df5b. The owner accepted its expanded reference size; this does not establish broader phone acceptance.

## One next task

Owner: install build36 in place, check that Reference Material starts compact, opens its approved size/media with one tap and collapses again with existing data and follow its short PHONE_CHECKLIST.txt. Supply layout feedback before deciding on further development or full regression verification.
