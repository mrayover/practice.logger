#!/usr/bin/env bash
set -euo pipefail
suite=${1:?suite required}
api=${2:?API required}
case "$suite" in review|transport|appearance-import) ;; *) exit 2 ;; esac
case "$api" in 26|35|36) ;; *) exit 2 ;; esac
package=com.mrayover.practicelogger.app
out="app/build/current-$suite-$api"
mkdir -p "$out"
if [ "$suite" = appearance-import ]; then
  # Fail before launching a device scenario if the shared test build missed preparation.
  unzip -Z1 app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk > "$out/apk-assets.txt"
  grep -Fxq assets/audio/backing.mp3 "$out/apk-assets.txt"
  grep -Fxq assets/audio/backing.m4a "$out/apk-assets.txt"
fi
collect() {
  timeout 20s adb logcat -d > "$out/logcat.txt" || true
  timeout 15s adb shell uiautomator dump /sdcard/current-window.xml || true
  timeout 15s adb pull /sdcard/current-window.xml "$out/window.xml" || true
  timeout 30s adb pull "/sdcard/Android/data/$package/files" "$out/files" || true
}
trap collect EXIT
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
fresh() {
  adb shell am force-stop "$package"
  adb shell pm clear "$package"
  adb shell settings put system font_scale 1.0
  adb shell cmd uimode night no
}
run_class() {
  local class=$1 count=$2
  local per_test_ms=120000 wall_seconds=$((count * 120 + 60))
  # This case captures 128 seconds and then plays beyond a full 128-second boundary.
  if [ "$class" = LongPhrasePlaybackTest ]; then
    per_test_ms=360000
    wall_seconds=420
  fi
  local result="$out/${class//\#/-}.txt"
  timeout "${wall_seconds}s" adb shell am instrument -w -r \
    -e timeout_msec "$per_test_ms" -e class "com.mrayover.practicelogger.$class" \
    "$package.test/androidx.test.runner.AndroidJUnitRunner" | tee "$result"
  grep -Eq "^OK \\($count tests?\\)" "$result"
}
case "$suite" in
  review)
    fresh; run_class TakeReviewWorkflowTest 3
    fresh; run_class FlexiblePhraseWorkflowTest 3
    fresh; run_class LongPhrasePlaybackTest 1
    ;;
  transport)
    fresh; run_class MeterWorkflowTest 3
    fresh; run_class MetronomeOutputTest 1
    fresh; run_class AudioSettingsWorkflowTest 2
    fresh; run_class MetronomeButtonPulseTest 2
    fresh; run_class LoopRecorderPreviewTest 3
    fresh; run_class NotesHistoryPreviewTest 1
    # Keep the seed data, but force a fresh application process before checking it.
    adb shell am force-stop "$package"
    run_class NotesHistoryReopenTest 1
    fresh; run_class HomeNavigationPreviewTest 1
    fresh; run_class TargetedP4PreviewTest 3
    fresh; run_class PracticeRefinementPreviewTest 2
    fresh; run_class TransportPreviewTest 1
    fresh; run_class NestedReferenceDiagnosticTest 1
    ;;
  appearance-import)
    fresh; run_class ThemeWorkflowTest 6
    fresh; run_class ImportedAudioWorkflowTest#importSelectRepeatInterruptAndKeepExistingTakes 1
    adb shell am force-stop "$package"
    run_class ImportedAudioWorkflowTest#reopenAndManageImportedAndEarlierRecordings 1
    ;;
esac
