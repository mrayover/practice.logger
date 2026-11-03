#!/usr/bin/env bash
set -euo pipefail
out=app/build/l3-native
mkdir -p "$out"
adb shell locksettings set-disabled true
adb shell wm dismiss-keyguard
adb shell settings put system font_scale "${2:-1.0}"
collect() {
  adb logcat -d > "$out/logcat.txt" || true
  adb shell uiautomator dump /sdcard/l3-window.xml || true
  adb pull /sdcard/l3-window.xml "$out/window.xml" || true
  adb shell screencap -p /sdcard/l3-screen.png || true
  adb pull /sdcard/l3-screen.png "$out/screen.png" || true
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/l3-native "$out/screenshots" || true
}
trap collect EXIT
run_test() {
  adb shell am force-stop com.mrayover.practicelogger.app
  timeout --kill-after=10s 300s adb shell am instrument -w -r -e timeout_msec 240000 -e class "com.mrayover.practicelogger.$1" com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/$1.txt"
  grep -Eq '^OK \([1-9][0-9]* tests?\)' "$out/$1.txt"
}
if [ "$1" = native ]; then
  adb install -r baseline76/build/outputs/apk/debug/app-debug.apk
  adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
  run_test BlockMediaSeedTest
fi
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if [ "$1" = native ]; then run_test BlockMediaUpgradeTest; run_test BlockMediaNativeTest; fi
run_test BlockMediaWorkflowTest
run_test BlockMediaReopenTest
