#!/usr/bin/env bash
set -euo pipefail
out=app/build/stage3b
mkdir -p "$out"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
collect() {
  adb logcat -d > "$out/logcat.txt" || true
  adb shell screencap -p /sdcard/stage3b-screen.png || true
  adb pull /sdcard/stage3b-screen.png "$out/screen.png" || true
  adb shell uiautomator dump /sdcard/stage3b-window.xml || true
  adb pull /sdcard/stage3b-window.xml "$out/window.xml" || true
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/stage3b-fixtures "$out/fixtures" || true
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/screenshots "$out/screenshots" || true
}
trap collect EXIT
failed=0
group_failed=false
for class in ${MULTI_REFERENCE_CLASSES//,/ }; do
  if [ "$class" = fresh ]; then
    [[ "${GITHUB_ACTIONS:-}" == true && "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]]
    adb shell pm clear com.mrayover.practicelogger.app
    group_failed=false
    continue
  fi
  if [ "$group_failed" = true ]; then
    echo "Skipped dependent phase $class after failed setup"
    continue
  fi
  if [ "$class" = process-death ]; then
    [[ "${GITHUB_ACTIONS:-}" == true && "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]]
    runner=com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner
    timeout --kill-after=10s 120s adb shell am instrument -w -r -e stage3b_emulator_only true -e class com.mrayover.practicelogger.MultiReferenceProcessDeathTest#dieWhileParentAndChildrenAreRebuilt "$runner" | tee "$out/migration-process-death.txt" || true
    grep -F 'Process crashed' "$out/migration-process-death.txt"
    timeout --kill-after=10s 180s adb shell am instrument -w -r -e class com.mrayover.practicelogger.MultiReferenceProcessDeathTest#recoverRoom23ThenMigrateAndColdReopenWithoutLoss "$runner" | tee "$out/migration-process-recovery.txt"
    grep -Eq '^OK \(1 test\)' "$out/migration-process-recovery.txt"
    continue
  fi
  if [ "$class" = portable-install ]; then
    bash scripts/Check-PortableRoundTrip.sh
    cp app/build/targeted-s2-*.txt "$out/"
    continue
  fi
  [[ "$class" =~ ^[A-Za-z0-9_.#]+$ ]]
  [[ "$class" == *.* ]] || class="com.mrayover.practicelogger.$class"
  adb shell am force-stop com.mrayover.practicelogger.app
  if ! timeout --kill-after=10s 1200s adb shell am instrument -w -r -e timeout_msec 480000 -e class "$class" com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/${class##*.}.txt"; then
    failed=1
  fi
  if ! grep -Eq '^OK \([1-9][0-9]* tests?\)' "$out/${class##*.}.txt"; then
    [ "${MULTI_REFERENCE_CONTINUE:-false}" = true ] || exit 1
    failed=1
    group_failed=true
  fi
done
exit "$failed"
