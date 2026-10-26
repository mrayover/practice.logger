# practice.logger

Small offline Android practice tool. The authoritative scope is [BUILD_PLAN.md](BUILD_PLAN.md). Read [AGENTS.md](AGENTS.md), [CURRENT_STATE.md](CURRENT_STATE.md) and [WORK_LOG.md](WORK_LOG.md) before every development session.

Stage 0 foundation is under verification. This shell is not yet a functional practice logger. No Goals, Routines or audio tools are included.

## Build and APK

GitHub Actions → Android APK → successful run → Artifacts → practice-logger-debug. Download the ZIP, extract app-debug.apk and transfer it to an Android 8.0+ phone. Open it and approve Android's install-from-this-source prompt if you trust this build; disable that source afterward if desired. A debug build is for testing, not Play Store distribution. Artifacts expire after 30 days; rerun the workflow to rebuild.

Developers need JDK 17 and Android SDK 36. Run ./gradlew testDebugUnitTest lintDebug assembleDebug (Windows: .\gradlew.bat). Configure a local SDK through ANDROID_HOME or ignored local.properties. No developer installation is needed on the owner's PC to obtain the CI APK.

Debug APK signing uses the CI runner's temporary debug key. Builds from different runs may not install as an update; do not store irreplaceable data in these early test APKs. Stable signing and backup must precede dependable daily use. Do not uninstall an existing app containing needed data to resolve a signing conflict.

## Design and dependencies

Kotlin 2.2.21, Compose, Room 2.8.5, AGP 8.13.2, Gradle 8.14.3, minSdk 26 / targetSdk 36. No Internet permission. Lato Regular/Bold are bundled with their SIL Open Font License in app assets and THIRD_PARTY_NOTICES.md. Android system UI and keyboards control their own font.

Git operations use GitHub Desktop. This session edits the actual Windows checkout, not a cloud copy.
