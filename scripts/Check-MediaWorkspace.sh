#!/usr/bin/env bash
set -euo pipefail
out=app/build/l2-native
mkdir -p "$out"
adb shell locksettings set-disabled true
adb shell wm dismiss-keyguard
adb shell settings put system font_scale "${2:-1.0}"
collect() {
  adb logcat -d > "$out/logcat.txt" || true
  adb shell screencap -p /sdcard/l2-screen.png || true
  adb pull /sdcard/l2-screen.png "$out/screen.png" || true
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/l2-native "$out/screenshots" || true
}
trap collect EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
classes=LibraryPlatformGateTest
if [ "$1" = native ]; then classes=LibraryWorkspaceTest; fi
if [ "$1" = ui ]; then classes='LibraryWorkspaceTest#libraryVisualAudioMarkersRotationAndListState'; fi
timeout --kill-after=10s 300s adb shell am instrument -w -r -e timeout_msec 240000 -e class "com.mrayover.practicelogger.$classes" com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/results.txt"
grep -Eq '^OK \([1-9][0-9]* tests?\)' "$out/results.txt"
if [ "$1" = native ] || [ "$1" = ui ]; then
  adb shell am force-stop com.mrayover.practicelogger.app
  timeout --kill-after=10s 60s adb shell am instrument -w -r -e class com.mrayover.practicelogger.LibraryWorkspaceReopenTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/reopen.txt"
  grep -Eq '^OK \([1-9][0-9]* tests?\)' "$out/reopen.txt"
fi
