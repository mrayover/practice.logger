#!/usr/bin/env bash
set -euo pipefail
suite=${TARGET_FEATURE_SUITE:-}
if [ -n "$suite" ]; then
  case "$suite" in review|transport|appearance-import) ;; *) exit 2 ;; esac
  [ -z "${TARGET_TEST_CLASS:-}" ] || exit 2
else
  [[ "$TARGET_TEST_CLASS" =~ ^com\.mrayover\.practicelogger\.[A-Za-z0-9_,.#]+$ ]] || exit 2
fi
adb shell locksettings set-disabled true
adb shell wm dismiss-keyguard
if [[ "${TARGET_TEST_CLASS:-}" == *ImportedAudioWorkflowTest* || "${TARGET_TEST_CLASS:-}" == *PortableZipRoundTripTest* || "$suite" == appearance-import ]]; then
  if [[ "${TARGET_TEST_CLASS:-}" == *PortableZipRoundTripTest* ]]; then export AUDIO_FIXTURE_DURATION=4.0; fi
  bash scripts/Prepare-AudioFixtures.sh
fi
bash ./gradlew assembleDebug assembleDebugAndroidTest --stacktrace
if [[ "${TARGET_TEST_CLASS:-}" == *PortableRestoreTest* ]]; then
  cp app/schemas/com.mrayover.practicelogger.BlockDatabase/17.json app/build/targeted-schema17.json
fi
if [ -n "$suite" ]; then
  bash scripts/Check-CurrentFeatures.sh "$suite" "$TARGET_API"
  exit 0
fi
if [[ "$TARGET_TEST_CLASS" == *LegacyReferenceSeedTest* ]]; then
  bash scripts/Prepare-ReferenceUpgrade.sh
  adb install -r baseline-apk/upgrade-baseline.apk
else
  adb install -r app/build/outputs/apk/debug/app-debug.apk
fi
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.mrayover.practicelogger.app
adb shell settings put system font_scale "${TARGET_FONT_SCALE:-1.0}"
collect() {
  timeout 20s adb logcat -d > app/build/targeted-log.txt || true
  timeout 15s adb shell uiautomator dump /sdcard/targeted-window.xml || true
  timeout 15s adb pull /sdcard/targeted-window.xml app/build/targeted-window.xml || true
  timeout 15s adb shell screencap -p /sdcard/targeted-screen.png || true
  timeout 15s adb pull /sdcard/targeted-screen.png app/build/targeted-screen.png || true
  timeout 30s adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files app/build/targeted-files || true
}
trap collect EXIT
for test_class in ${TARGET_TEST_CLASS//,/ }; do
  if [[ "$test_class" == com.mrayover.practicelogger.PortableZipRoundTripTest ]]; then
    bash scripts/Check-PortableRoundTrip.sh
    continue
  fi
  if [[ "$test_class" == *LegacyReferenceUpgradeTest ]]; then adb install -r app/build/outputs/apk/debug/app-debug.apk; fi
  adb shell am force-stop com.mrayover.practicelogger.app
  per_test_ms=120000
  wall_seconds=480
  if [[ "$test_class" == *'#'* ]]; then wall_seconds=180; fi
  if [[ "$test_class" == *LongPhrasePlaybackTest* ]]; then per_test_ms=360000; wall_seconds=420; fi
  timeout --kill-after=10s "${wall_seconds}s" adb shell am instrument -w -r -e timeout_msec "$per_test_ms" -e class "$test_class" com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "app/build/targeted-${test_class##*.}.txt"
  adb logcat -d > app/build/targeted-log.txt
  adb shell screencap -p /sdcard/targeted-screen.png
  adb pull /sdcard/targeted-screen.png app/build/targeted-screen.png
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/screenshots app/build/targeted-screenshots || true
  grep -Eq '^OK \([1-9][0-9]* tests?\)' "app/build/targeted-${test_class##*.}.txt"
done
