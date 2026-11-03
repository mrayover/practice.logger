#!/usr/bin/env bash
set -euo pipefail
out=app/build/l1-native
mkdir -p "$out"
adb shell locksettings set-disabled true
adb shell wm dismiss-keyguard
adb shell settings put system font_scale "${1:-1.0}"
collect() {
  adb logcat -d > "$out/logcat.txt" || true
  adb shell uiautomator dump /sdcard/l1-window.xml || true
  adb pull /sdcard/l1-window.xml "$out/window.xml" || true
  adb shell screencap -p /sdcard/l1-screen.png || true
  adb pull /sdcard/l1-screen.png "$out/screen.png" || true
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/l1-native "$out/screenshots" || true
}
trap collect EXIT
run_test() {
  adb shell am force-stop com.mrayover.practicelogger.app
  timeout --kill-after=10s 240s adb shell am instrument -w -r -e timeout_msec 150000 -e class "com.mrayover.practicelogger.$1" com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/$1.txt"
  grep -Eq '^OK \([1-9][0-9]* tests?\)' "$out/$1.txt"
}
# A disposable emulator only; install69, seed actual legacy ownership, install70 without clearing.
adb install -r baseline-apk/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
run_test MediaLegacySeedTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
run_test MediaLegacyUpgradeTest
run_test MediaLibraryStorageTest
run_test MediaLibraryWorkflowTest
run_test MediaLibraryReopenTest
