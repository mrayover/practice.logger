# Current state — owner-test preview 0.16.0/build35

Updated2026-10-30 (America/Los_Angeles). **Signed, installable owner-test preview; full regression verification pending.** The owner explicitly limited this layout-only preview to compilation, a basic launch check and package verification. Normal release requirements remain unchanged. No further automated work is scheduled.

## Delivery and provenance

| Item | Verified value |
|---|---|
| Version | 0.16.0 / code35 |
| Signed app source | `a5f12134764028b93a5f7782eef8870c06e0ca35` |
| Build and smoke launch | [Single owner-preview run41079261689](https://github.com/mrayover/practice.logger/actions/runs/41079261689) |
| Package | `com.mrayover.practicelogger.app`; minAPI26 / target36 |
| Permanent certificate SHA256 | `f56fcf9e872c9a52327bfb13be060eaecd008dccb3698213aa10da966eab2830` |
| APK SHA256 | `f1b4bf447b32ffe54f20db95e101f41b93a18a3ed3b0ee12c82621d6d5cae499` |
| ZIP SHA256 | `573be4cf134e2cab2bc384bf56272963889ed2047e20230faf03bd47e6f36466` |

[Installable APK](dist/practice.logger-0.16.0-b35-a5f1213/practice.logger-0.16.0-b35-a5f1213.apk) · [ZIP](dist/practice.logger-0.16.0-b35-a5f1213.zip) · [build info](dist/practice.logger-0.16.0-b35-a5f1213/BUILD_INFO.json) · [preview status](dist/practice.logger-0.16.0-b35-a5f1213/VERIFICATION_STATUS.txt) · [phone checklist](dist/practice.logger-0.16.0-b35-a5f1213/PHONE_CHECKLIST.txt) · [latest pointer](dist/LATEST.txt).

Install **over the existing permanent-key app without uninstalling or clearing data**. The publisher used the existing key and verified the pinned certificate, cryptographic signature, package ID and version. ZIP members round-trip byte-for-byte, including the APK. APK/ZIP are local ignored artifacts. Documentation commits follow the signed app-source commit; no later app changes are implied.

## Exactly what changed

Only Focused Practice layout and the build number changed in the app. Practice Description body is32–80dp instead of80–152dp. In ordinary portrait (workspace at least420dp, font scale below1.5, Notes closed), Reference Material fills available space, placing existing metronome/free/loop/Notes controls directly above the pinned primary actions. Short screens, enlarged text and open Notes retain bounded references and outer scrolling. Reference image/PDF viewport remains220dp with existing controls. Active recording Stop/Save and primary actions retain their existing placement outside workspace scrolling. No data, timer, audio, navigation or reference-loading behavior changed.

A separate manual owner-preview workflow compiles debug/release and performs one clean API35 debug launch smoke check. Existing android.yml, its full regression gates and permanent development rules are unchanged. Source changes were committed with automatic full CI skipped under the owner's explicit preview exception.

## What was tested

- Debug and unsigned release compilation pass in the single run above. No full unit/lint tasks requested.
- One clean API35 emulator: debug APK install, successful Activity start, live process after launch, visible app UI and no app crash in the smoke window pass. This is a launch smoke check; it does not validate the revised Focused Practice layout on a phone or launch the permanent-key release APK.
- Publisher verifies signed release identity/version/signature/pinned certificate and APK hash; final archive audit verifies ZIP integrity and every member against the delivered folder.
- All130previous APK/ZIP files retain their recorded SHA256. Static source comparison confirms only PracticeScreens.kt and versionCode changed in app/, with no data/schema/migration/audio/backup/manifest/dependency edits. Git whitespace checks pass.

## What remains unverified

**Full regression verification pending.** No full units/lint, complete API26/API35 suites, enlarged-text/media/audio/session regressions or screenshot automation were run for build35. Existing full-regression failures and cancelled routine diagnostic from build34 are retained in WORK_LOG; they were not investigated or claimed fixed. Prior build34 evidence does not establish build35 regression acceptance. Physical signed-APK launch/install-over/data retention and actual portrait/landscape/large-text layout, reference/Notes interactions, recordings, acoustic output and TalkBack remain owner checks.

Room10/schemas/migrations1–10, JSON6/readers1–6 and media exclusions remain. The original temporary-key0.1.0 installation is separate. Stage3 phone acceptance remains pending; Stage4 stays0/10real sessions. Build34 and all earlier deliveries remain available; its previous current-state record is in Git at02bda5c.

## One next task

Owner: install build35 in place, inspect the marked-up Focused Practice layout with existing data and follow its short PHONE_CHECKLIST.txt. Supply layout feedback before deciding on further development or full regression verification.
