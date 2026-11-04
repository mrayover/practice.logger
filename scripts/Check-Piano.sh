#!/usr/bin/env bash
set -euo pipefail
out=app/build/k1-native
mkdir -p "$out"
adb shell locksettings set-disabled true
adb shell wm dismiss-keyguard
adb shell settings put system font_scale "${1:-1.0}"
collect() {
  adb logcat -d > "$out/logcat.txt" || true
  adb shell uiautomator dump /sdcard/k1-window.xml || true
  adb pull /sdcard/k1-window.xml "$out/window.xml" || true
  adb shell screencap -p /sdcard/k1-screen.png || true
  adb pull /sdcard/k1-screen.png "$out/screen.png" || true
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/k1-native "$out/screenshots" || true
}
trap collect EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
tests='PianoNativeTest PianoWorkflowTest'
if [ "${2:-native}" = ui ]; then tests=PianoWorkflowTest; fi
for test in $tests; do
  adb shell am force-stop com.mrayover.practicelogger.app
  timeout --kill-after=10s 300s adb shell am instrument -w -r -e timeout_msec 120000 -e class "com.mrayover.practicelogger.$test" com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/$test.txt"
  grep -Eq '^OK \([1-9][0-9]* tests?\)' "$out/$test.txt"
done
