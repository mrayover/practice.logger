#!/usr/bin/env bash
set -euo pipefail
package=com.mrayover.practicelogger.app
out=app/build/audio-import
mkdir -p "$out"
collect() {
  timeout 15s adb logcat -d > "$out/logcat.txt" || true
  timeout 15s adb shell uiautomator dump /sdcard/import-window.xml || true
  timeout 15s adb pull /sdcard/import-window.xml "$out/window.xml" || true
  timeout 20s adb pull "/sdcard/Android/data/$package/files/audio-import" "$out/screens" || true
  timeout 20s adb pull "/sdcard/Android/data/$package/files/loop-refinement" "$out/loops" || true
}
trap collect EXIT
timeout 30s adb install -r app/build/outputs/apk/debug/app-debug.apk
timeout 30s adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb logcat -c || true
for method in importSelectRepeatInterruptAndKeepExistingTakes reopenAndManageImportedAndEarlierRecordings; do
  timeout 180s adb shell am instrument -w -r -e class "com.mrayover.practicelogger.ImportedAudioWorkflowTest#$method" "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee "$out/$method.txt"
  grep -q 'OK (1 test)' "$out/$method.txt"
  timeout 10s adb shell am force-stop "$package"
done
if [ "${1:-all}" = all ]; then
  timeout 240s adb shell am instrument -w -r -e class com.mrayover.practicelogger.LoopRecorderPreviewTest "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee "$out/existing-loops.txt"
  grep -q 'OK (2 tests)' "$out/existing-loops.txt"
fi
