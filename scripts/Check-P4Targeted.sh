#!/usr/bin/env bash
set -euo pipefail
package=com.mrayover.practicelogger.app
out=app/build/p4-targeted
mkdir -p "$out"
collect() {
  timeout 15s adb logcat -d > "$out/logcat.txt" || true
  timeout 15s adb shell uiautomator dump /sdcard/p4-window.xml || true
  timeout 15s adb pull /sdcard/p4-window.xml "$out/window.xml" || true
  timeout 20s adb pull "/sdcard/Android/data/$package/files/p4-targeted" "$out/screens" || true
  timeout 20s adb pull "/sdcard/Android/data/$package/files/metronome-pulse" "$out/pulse" || true
}
trap collect EXIT
timeout 30s adb install -r app/build/outputs/apk/debug/app-debug.apk
timeout 30s adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if [ "${1:-all}" = transport ]; then
  timeout 10s adb shell settings put system font_scale 1.3
fi
adb logcat -c || true
classes='com.mrayover.practicelogger.TargetedP4PreviewTest,com.mrayover.practicelogger.MetronomeButtonPulseTest#practiceWholeButtonFollowsLiveBeatsAndStops'
expected='OK (4 tests)'
if [ "${1:-all}" = transport ]; then
  classes='com.mrayover.practicelogger.TargetedP4PreviewTest#compactTransportAccentTempoAndFooter'
  expected='OK (1 test)'
fi
timeout 180s adb shell am instrument -w -r -e class "$classes" "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee "$out/results.txt"
grep -q "$expected" "$out/results.txt"
