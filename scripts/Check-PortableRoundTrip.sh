#!/usr/bin/env bash
set -euo pipefail

# The clean-install and kill checks are restricted to the CI emulator.
[[ "${GITHUB_ACTIONS:-}" == true && "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]]
app=com.mrayover.practicelogger.app
runner="$app.test/androidx.test.runner.AndroidJUnitRunner"
package=com.mrayover.practicelogger
zip=/sdcard/Download/practice-s2-roundtrip.zip
archive=app/build/targeted-s2-roundtrip.zip
temp=/data/local/tmp/practice-s2-roundtrip.zip
debug=app/build/outputs/apk/debug/app-debug.apk
test_apk=app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

run() {
  local method="$1" output="$2"
  timeout --kill-after=10s 180s adb shell am instrument -w -r -e timeout_msec 120000 -e class "$package.$method" "$runner" | tee "$output"
  grep -Eq '^OK \(1 test\)' "$output"
}

adb shell rm -f "$zip" "$temp"
adb shell pm clear "$app"
run 'PortableZipRoundTripTest#exportFromSourceThroughLocalSaveAs' app/build/targeted-s2-export.txt
adb pull "$zip" "$archive"
test -s "$archive"

# The exported document is outside both installations. Keep a private read-only verification input too.
adb uninstall "$app"
adb install -r "$debug"
adb install -r "$test_apk"
adb push "$archive" "$temp"
supply_expected() {
  adb shell "run-as $app mkdir -p files && cat $temp | run-as $app tee files/s2-expected.zip >/dev/null"
}
supply_expected
run 'PortableZipRoundTripTest#restoreIntoSeparateEmptyInstallation' app/build/targeted-s2-restore.txt
adb shell am force-stop "$app"
run 'PortableZipRoundTripTest#verifyAfterProcessRestart' app/build/targeted-s2-restart.txt

adb shell pm clear "$app"
supply_expected
timeout --kill-after=10s 180s adb shell am instrument -w -r -e timeout_msec 120000 \
  -e class "$package.PortableRestoreProcessDeathTest#dieAfterDatabaseCommit" "$runner" \
  | tee app/build/targeted-s2-process-death.txt || true
grep -F 'Process crashed' app/build/targeted-s2-process-death.txt
adb shell "run-as $app sh -c 'test -s files/portable-restore/journal.json'"
run 'PortableZipRoundTripTest#verifyAfterProcessRestart' app/build/targeted-s2-recover.txt
adb shell "run-as $app sh -c 'test ! -e files/portable-restore/journal.json'"
adb shell rm -f "$temp"
