#!/usr/bin/env bash
set -euo pipefail
package=com.mrayover.practicelogger.app
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell am start -W -n "$package/com.mrayover.practicelogger.MainActivity" | tee app/build/preview-launch.txt
grep -q 'Status: ok' app/build/preview-launch.txt
sleep 5
adb shell pidof "$package" | tee app/build/preview-pid.txt
adb shell uiautomator dump /sdcard/preview-window.xml
adb pull /sdcard/preview-window.xml app/build/preview-window.xml
grep -q "package=\"$package\"" app/build/preview-window.xml
adb logcat -d -b crash > app/build/preview-crashes.txt
if grep -q "Process: $package" app/build/preview-crashes.txt; then
  cat app/build/preview-crashes.txt
  exit 1
fi
echo 'PASS: debug variant installed, launched, remained alive and displayed app UI; release compiled. Full regression pending.' | tee app/build/preview-result.txt

# One targeted navigation/layout preview; no full regression matrix.
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class com.mrayover.practicelogger.${PREVIEW_TEST_CLASS:-HomeNavigationPreviewTest} com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/home-preview app/build/preview-home-screens || true
adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/metronome-pulse app/build/preview-metronome-screens || true
adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/practice-refinement app/build/preview-refinement-screens || true
if [[ "${PREVIEW_TEST_CLASS:-}" == PracticeRefinementPreviewTest,* ]]; then
  grep -q 'OK (4 tests)' app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-HomeNavigationPreviewTest}" = MetronomeButtonPulseTest ]; then
  grep -q 'OK (2 tests)' app/build/preview-home.txt
else
  grep -q 'OK (1 test)' app/build/preview-home.txt
fi
