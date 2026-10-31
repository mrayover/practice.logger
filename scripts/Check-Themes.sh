#!/usr/bin/env bash
set -euo pipefail
api=${1:?API required}
package=com.mrayover.practicelogger.app
out="app/build/theme-$api"
mkdir -p "$out"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
classes=com.mrayover.practicelogger.ThemeWorkflowTest
expected=3
if [ "$api" = 35 ]; then
  classes="$classes,com.mrayover.practicelogger.MetronomeButtonPulseTest,com.mrayover.practicelogger.AudioSettingsWorkflowTest,com.mrayover.practicelogger.LoopRecorderPreviewTest"
  expected=9
fi
adb logcat -c
adb shell am instrument -w -r -e class "$classes" "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee "$out/results.txt"
adb logcat -d > "$out/logcat.txt"
for folder in theme metronome-pulse p4-settings loop-refinement; do
  adb pull "/sdcard/Android/data/$package/files/$folder" "$out/$folder" || true
done
grep -q "OK ($expected tests)" "$out/results.txt"
