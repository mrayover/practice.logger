#!/usr/bin/env bash
set -euo pipefail
out=app/build/stage3b
mkdir -p "$out"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
collect() {
  adb logcat -d > "$out/logcat.txt" || true
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/stage3b-fixtures "$out/fixtures" || true
}
trap collect EXIT
for class in ${MULTI_REFERENCE_CLASSES//,/ }; do
  [[ "$class" =~ ^[A-Za-z0-9_.#]+$ ]]
  [[ "$class" == *.* ]] || class="com.mrayover.practicelogger.$class"
  adb shell am force-stop com.mrayover.practicelogger.app
  timeout --kill-after=10s 1200s adb shell am instrument -w -r -e timeout_msec 480000 -e class "$class" com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/${class##*.}.txt"
  grep -Eq '^OK \([1-9][0-9]* tests?\)' "$out/${class##*.}.txt"
done
