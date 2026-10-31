#!/usr/bin/env bash
set -euo pipefail
api=${1:?API required}
package=com.mrayover.practicelogger.app
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c || echo 'Log clear unavailable; retaining existing diagnostic logs.'
trap 'adb logcat -d > app/build/p3-logcat-'"$api"'.txt; adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/p3-meter app/build/p3-screens-'"$api"' || true; adb shell uiautomator dump /sdcard/p3-window.xml; adb pull /sdcard/p3-window.xml app/build/p3-window-'"$api"'.xml || true' EXIT
classes=com.mrayover.practicelogger.MetronomeOutputTest
expected='OK (1 test)'
if [ "$api" = 35 ]; then
  classes=com.mrayover.practicelogger.MeterWorkflowTest,com.mrayover.practicelogger.MetronomeOutputTest,com.mrayover.practicelogger.TakeReviewWorkflowTest
  expected='OK (7 tests)'
fi
adb shell am instrument -w -r -e class "$classes" "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee app/build/p3-native-"$api".txt
grep -q "$expected" app/build/p3-native-"$api".txt
