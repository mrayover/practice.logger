#!/usr/bin/env bash
set -euo pipefail
out=app/build/t1-native
mkdir -p "$out"
adb shell locksettings set-disabled true
adb shell wm dismiss-keyguard
adb shell settings put system font_scale "${1:-1.0}"
collect() {
  adb logcat -d > "$out/logcat.txt" || true
  adb shell uiautomator dump /sdcard/t1-window.xml || true
  adb pull /sdcard/t1-window.xml "$out/window.xml" || true
  adb shell screencap -p /sdcard/t1-screen.png || true
  adb pull /sdcard/t1-screen.png "$out/screen.png" || true
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/t1-native "$out/screenshots" || true
}
trap collect EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
tests='PianoNativeTest PianoWorkflowTest'
case "${2:-native}" in
  ui) tests=TranscribeWorkflowTest ;;
  standalone) tests=StandaloneTranscribeTest ;;
  standalone-reopen) tests="StandaloneTimerReopenTest#startAndCheckpoint StandaloneTimerReopenTest#restorePausedAndSave" ;;
  practice-navigation) tests="MultipleSessionWorkflowTest MultipleSessionReopenTest RoutineWorkflowTest RoutineReopenTest GoalWorkflowTest" ;;
  reopen) tests="BlockAudioReopenTest#checkpointBeforeForceStop BlockAudioReopenTest#restoreAfterForceStop" ;;
  transport) tests="TransportPreviewTest ImportedAudioWorkflowTest#importSelectRepeatInterruptAndKeepExistingTakes" ;;
  library) tests="LibraryPlatformGateTest LibraryWorkspaceTest#realFormatsControllerLoopSpeedSeekEndAndForeground LibraryWorkspaceTest#waveformFaithfulnessLongDurationCacheRecoveryAndOriginalIntegrity" ;;
esac
for test in $tests; do
  if [[ "${2:-}" == practice-navigation && "$test" != *ReopenTest ]]; then
    adb shell pm clear com.mrayover.practicelogger.app
  fi
  adb shell am force-stop com.mrayover.practicelogger.app
  timeout --kill-after=10s 300s adb shell am instrument -w -r -e timeout_msec 120000 -e class "com.mrayover.practicelogger.$test" com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee "$out/$test.txt"
  grep -Eq '^OK \([1-9][0-9]* tests?\)' "$out/$test.txt"
done
