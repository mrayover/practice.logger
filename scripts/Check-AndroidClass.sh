#!/usr/bin/env bash
set -euo pipefail
[[ "$TARGET_TEST_CLASS" =~ ^com\.mrayover\.practicelogger\.[A-Za-z0-9_,.]+$ ]] || exit 2
adb shell locksettings set-disabled true
adb shell wm dismiss-keyguard
bash ./gradlew assembleDebug assembleDebugAndroidTest --stacktrace
if [[ "$TARGET_TEST_CLASS" == *LegacyReferenceSeedTest* ]]; then
  APKSIGNER=$(find "$ANDROID_HOME/build-tools" -name apksigner | sort -V | tail -n 1)
  "$APKSIGNER" sign --ks "$HOME/.android/debug.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out baseline-apk/upgrade-baseline.apk baseline-apk/app-debug.apk
  adb install -r baseline-apk/upgrade-baseline.apk
else
  adb install -r app/build/outputs/apk/debug/app-debug.apk
fi
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.mrayover.practicelogger.app
adb shell settings put system font_scale "${TARGET_FONT_SCALE:-1.0}"
for test_class in ${TARGET_TEST_CLASS//,/ }; do
  if [[ "$test_class" == *LegacyReferenceUpgradeTest ]]; then adb install -r app/build/outputs/apk/debug/app-debug.apk; fi
  adb shell am force-stop com.mrayover.practicelogger.app
  adb shell am instrument -w -r -e timeout_msec 120000 -e class "$test_class" com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "app/build/targeted-${test_class##*.}.txt"
  adb logcat -d > app/build/targeted-log.txt
  adb shell screencap -p /sdcard/targeted-screen.png
  adb pull /sdcard/targeted-screen.png app/build/targeted-screen.png
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/screenshots app/build/targeted-screenshots || true
  grep -Eq '^OK \([1-9][0-9]* tests?\)' "app/build/targeted-${test_class##*.}.txt"
done
