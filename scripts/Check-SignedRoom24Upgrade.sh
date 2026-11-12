#!/usr/bin/env bash
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true && "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]]
out=app/build/stage3-signed-upgrade
mkdir -p "$out"
app=com.mrayover.practicelogger.app
runner="$app.test/androidx.test.runner.AndroidJUnitRunner"
trap 'adb logcat -d > "$out/logcat.txt" || true' EXIT
run() {
  timeout --kill-after=10s 180s adb shell am instrument -w -r -e stage3d_emulator_only true -e expected_build "$EXPECTED_BUILD" \
    -e class "com.mrayover.practicelogger.SignedRoom24UpgradeTest#$1" "$runner" | tee "$out/$2.txt"
  grep -Eq '^OK \(1 test\)' "$out/$2.txt"
}
adb install signed-packages/baseline.apk
adb install signed-packages/tests.apk
run seedPopulatedSignedBuild110 seed-room23
adb shell am start -W -n "$app/com.mrayover.practicelogger.MainActivity"
adb shell am force-stop "$app"
run verifyPopulatedSignedBuild110BeforeUpgrade verify-signed110
adb shell am force-stop "$app"
# True in-place permanent-key install: no uninstall, clear, or data copy between versions.
adb install -r signed-packages/update.apk
adb shell am start -W -n "$app/com.mrayover.practicelogger.MainActivity"
adb shell am force-stop "$app"
run verifySignedRoom24AfterInstallOverAndColdReopen verify-signed-upgrade
adb shell am force-stop "$app"
run verifySignedRoom24AfterInstallOverAndColdReopen verify-cold-reopen
cp signed-packages/SHA256SUMS.txt "$out/"
