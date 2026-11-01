#!/usr/bin/env bash
set -euo pipefail
api=${1:?API required}
package=com.mrayover.practicelogger.app
out="app/build/theme-$api"
mkdir -p "$out"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
classes=com.mrayover.practicelogger.ThemeWorkflowTest
expected=6
if [ "$api" = 35 ] && [ "${2:-all}" = all ]; then
  classes="$classes,com.mrayover.practicelogger.MetronomeButtonPulseTest,com.mrayover.practicelogger.AudioSettingsWorkflowTest,com.mrayover.practicelogger.LoopRecorderPreviewTest"
  expected=12
fi
collect() {
  timeout 20s adb logcat -d > "$out/logcat.txt" || true
  timeout 15s adb shell uiautomator dump /sdcard/theme-window.xml || true
  timeout 15s adb pull /sdcard/theme-window.xml "$out/window.xml" || true
  for folder in theme metronome-pulse p4-settings loop-refinement; do
    timeout 20s adb pull "/sdcard/Android/data/$package/files/$folder" "$out/$folder" || true
  done
}
trap collect EXIT
adb logcat -c || true
timeout 420s adb shell am instrument -w -r -e timeout_msec 120000 -e class "$classes" "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee "$out/results.txt"
grep -q "OK ($expected tests)" "$out/results.txt"
