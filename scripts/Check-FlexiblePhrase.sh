#!/usr/bin/env bash
set -euo pipefail
api=${1:?API required}
package=com.mrayover.practicelogger.app
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c
trap 'adb logcat -d > app/build/p2-logcat-'"$api"'.txt; adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/p2-phrase app/build/p2-screens-'"$api"' || true; adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/take-review app/build/p2-review-screens-'"$api"' || true; adb shell uiautomator dump /sdcard/p2-window.xml; adb pull /sdcard/p2-window.xml app/build/p2-window-'"$api"'.xml || true' EXIT
classes=com.mrayover.practicelogger.LongPhrasePlaybackTest
expected='OK (1 test)'
if [ "$api" = 35 ]; then
  classes=com.mrayover.practicelogger.FlexiblePhraseWorkflowTest,com.mrayover.practicelogger.LongPhrasePlaybackTest,com.mrayover.practicelogger.TakeReviewWorkflowTest
  expected='OK (7 tests)'
fi
adb shell am instrument -w -r -e class "$classes" "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee app/build/p2-native-"$api".txt
grep -q "$expected" app/build/p2-native-"$api".txt
