# practice.logger

Small offline Android practice tool. The authoritative scope is [BUILD_PLAN.md](BUILD_PLAN.md). Read [AGENTS.md](AGENTS.md), [CURRENT_STATE.md](CURRENT_STATE.md) and [WORK_LOG.md](WORK_LOG.md) before every development session.

Stage 0 foundation and Stage 1 Block catalog passed CI build, unit tests, lint, Android emulator catalog workflows and a force-stop/reopen persistence test. Owner phone checks are pending. No Goals, Routines or audio tools are included.

## Build and APK

GitHub Actions → Android APK → successful run → Artifacts → practice-logger-debug. Download the ZIP, extract app-debug.apk and transfer it to an Android 8.0+ phone. Open it and approve Android's install-from-this-source prompt if you trust this build; disable that source afterward if desired. A debug build is for testing, not Play Store distribution. Artifacts expire after 30 days; rerun the workflow to rebuild.

Developers need JDK 17 and Android SDK 36. Run ./gradlew testDebugUnitTest lintDebug assembleDebug (Windows: .\gradlew.bat). Configure a local SDK through ANDROID_HOME or ignored local.properties. No developer installation is needed on the owner's PC to obtain the CI APK.

Debug APK signing uses the CI runner's temporary debug key. Builds from different runs may not install as an update; do not store irreplaceable data in these early test APKs. Stable signing and backup must precede dependable daily use. Do not uninstall an existing app containing needed data to resolve a signing conflict.

## Design and dependencies

Kotlin 2.2.21, Compose, Room 2.8.5, AGP 8.13.2, Gradle 8.14.3, minSdk 26 / targetSdk 36. No Internet permission. Lato Regular/Bold are bundled with their SIL Open Font License in app assets and THIRD_PARTY_NOTICES.md. Android system UI and keyboards control their own font.

Git operations use GitHub Desktop. This session edits the actual Windows checkout, not a cloud copy.

## Block catalog (0.1.0)

Home → Manage Blocks → New Block. Name and Action are required; default duration starts at five minutes. Duration and optional BPM accept positive whole numbers representable as 32-bit integers. Search matches name, action and notes literally, ignoring case. Save writes locally; Cancel/Back asks before discarding edits. An editor draft survives Activity recreation and saved-state restoration. An unsaved draft is not a saved Block.

Archive is reversible and requires confirmation. Switch between Active and Archived, open a Block and Restore it. No hard deletion is exposed: stable IDs and Block records are retained for future history. Archiving with unsaved edits discards those edits after the explicit confirmation.

Practice Now, sessions, timers, history and backup belong to later stages and have no dead controls in this build. See MANUAL_TESTS.md for the phone checklist. CI runs only for source/configuration changes; documentation-only commits retain the prior tested APK.

Latest verified catalog APK: [Download 0.1.0 debug artifact](https://github.com/mrayover/practice.logger/actions/runs/40370047278/artifacts/12671548458). [Successful CI run](https://github.com/mrayover/practice.logger/actions/runs/40370047278), source d9b83548b2fa0e17afb68aea73a211266b1dd955. See CURRENT_STATE.md for delivery evidence and limitations.
