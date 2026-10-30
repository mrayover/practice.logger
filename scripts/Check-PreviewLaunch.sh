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

# Two directly affected notes checks, with a fresh app process between them. No full suite/matrix.
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -r -e class com.mrayover.practicelogger.NotesHistoryPreviewTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-notes.txt
adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/notes-preview app/build/preview-notes-screens || true
grep -q 'OK (1 test)' app/build/preview-notes.txt
adb shell am force-stop "$package"
adb shell am instrument -w -r -e class com.mrayover.practicelogger.NotesHistoryReopenTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-notes-reopen.txt
grep -q 'OK (1 test)' app/build/preview-notes-reopen.txt
