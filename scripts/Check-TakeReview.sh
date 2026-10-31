#!/usr/bin/env bash
set -euo pipefail
package=com.mrayover.practicelogger.app
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
trap 'adb logcat -d > app/build/p1-logcat.txt; adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/take-review app/build/p1-screens || true; adb shell uiautomator dump /sdcard/p1-window.xml; adb pull /sdcard/p1-window.xml app/build/p1-window.xml || true' EXIT
adb shell am instrument -w -r -e class com.mrayover.practicelogger.TakeReviewWorkflowTest "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee app/build/p1-native.txt
grep -q 'OK (3 tests)' app/build/p1-native.txt
