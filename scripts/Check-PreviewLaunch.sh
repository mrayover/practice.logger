#!/usr/bin/env bash
set -euo pipefail
package=com.mrayover.practicelogger.app
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
# Suppress Android first-use education so captures measure the app surface, not its system overlay.
adb shell settings put secure immersive_mode_confirmations confirmed
adb shell am start -W -n "$package/com.mrayover.practicelogger.MainActivity" | tee app/build/preview-launch.txt
grep -q 'Status: ok' app/build/preview-launch.txt
sleep 5
adb shell pidof "$package" | tee app/build/preview-pid.txt
adb shell uiautomator dump /sdcard/preview-window.xml
adb pull /sdcard/preview-window.xml app/build/preview-window.xml
grep -q "package=\"$package\"" app/build/preview-window.xml
adb logcat -d -b crash > app/build/preview-crashes.txt
if grep -q "Process: $package" app/build/preview-crashes.txt; then
  cat app/build/preview-crashes.txt
  exit 1
fi
echo 'PASS: debug variant installed, launched, remained alive and displayed app UI; release compiled. Full regression pending.' | tee app/build/preview-result.txt

# One targeted navigation/layout preview; no full regression matrix.
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
if [ "${PREVIEW_TEST_CLASS:-}" = ComposerExportWorkflowTest ]; then
  trap 'adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-export app/build/preview-composer-export || true; adb logcat -d > app/build/preview-export-logcat.txt || true' EXIT
  timeout 240s adb shell am instrument -w -r -e class com.mrayover.practicelogger.ComposerExportWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerC5CWorkflowTest ]; then
  trap 'adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c5c app/build/preview-composer-c5c || true; adb logcat -d > app/build/preview-c5c-logcat.txt || true' EXIT
  timeout 180s adb shell am instrument -w -r -e class com.mrayover.practicelogger.ComposerC5CWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerLoopLiveTest ]; then
  trap 'adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-loop-live app/build/preview-composer-loop-live || true; adb logcat -d > app/build/preview-loop-live-logcat.txt || true' EXIT
  timeout 180s adb shell am instrument -w -r -e loopLiveTail "${LOOP_LIVE_TAIL:-false}" -e loopLiveSkipNative "${LOOP_LIVE_SKIP_NATIVE:-false}" -e class com.mrayover.practicelogger.ComposerLoopLiveTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerC5BTransportTest ]; then
  trap 'adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c5b-transport app/build/preview-composer-c5b-transport || true; adb logcat -d > app/build/preview-c5b-transport-logcat.txt || true' EXIT
  timeout 90s adb shell am instrument -w -r -e class com.mrayover.practicelogger.ComposerC5BTransportTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerC5BWorkflowTest ]; then
  trap 'adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c5b app/build/preview-composer-c5b-screens || true; adb logcat -d > app/build/preview-c5b-logcat.txt || true' EXIT
  timeout 240s adb shell am instrument -w -r -e class com.mrayover.practicelogger.ComposerC5BWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerC5AWorkflowTest ]; then
  trap 'adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c5a app/build/preview-composer-c5a-screens || true; adb logcat -d > app/build/preview-c5a-logcat.txt || true' EXIT
  timeout 240s adb shell am instrument -w -r -e class com.mrayover.practicelogger.ComposerC5AWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4EWorkflowTest ]; then
  trap 'adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4e app/build/preview-composer-c4e-screens || true; adb logcat -d > app/build/preview-c4e-logcat.txt || true' EXIT
  adb shell am instrument -w -r -e c4eSkipNative "${C4E_SKIP_NATIVE:-false}" -e class com.mrayover.practicelogger.ComposerC4EWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4AWorkflowTest ]; then
  function c4a_diagnostics() {
    timeout 15s adb exec-out screencap -p > app/build/preview-c4a-final.png || true
    timeout 15s adb shell uiautomator dump /sdcard/c4a-final.xml || true
    timeout 15s adb pull /sdcard/c4a-final.xml app/build/preview-c4a-final.xml || true
    timeout 15s adb shell kill -3 "$(adb shell pidof "$package")" || true
    timeout 15s adb logcat -d > app/build/preview-c4a-logcat.txt || true
    timeout 20s adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4a app/build/preview-composer-c4a-screens || true
  }
  trap c4a_diagnostics EXIT
  timeout 360s adb shell am instrument -w -r -e class com.mrayover.practicelogger.${PREVIEW_TEST_CLASS:-HomeNavigationPreviewTest} com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4DWorkflowTest ]; then
  function c4d_diagnostics() {
    local result=$?
    if [ "$result" -ne 0 ]; then
      timeout 15s adb exec-out screencap -p > app/build/preview-c4d-final.png || true
      timeout 15s adb logcat -d > app/build/preview-c4d-logcat.txt || true
      timeout 20s adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4d app/build/preview-composer-c4d-screens || true
    fi
  }
  trap c4d_diagnostics EXIT
  timeout 360s adb shell am instrument -w -r -e c4dSkipNative "${C4D_SKIP_NATIVE:-false}" -e class com.mrayover.practicelogger.ComposerC4DWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4CWorkflowTest ]; then
  function c4c_diagnostics() {
    local result=$?
    if [ "$result" -ne 0 ]; then
      timeout 15s adb exec-out screencap -p > app/build/preview-c4c-final.png || true
      timeout 15s adb logcat -d > app/build/preview-c4c-logcat.txt || true
      timeout 20s adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4c app/build/preview-composer-c4c-screens || true
    fi
  }
  trap c4c_diagnostics EXIT
  timeout 360s adb shell am instrument -w -r -e class com.mrayover.practicelogger.ComposerC4CWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4BWorkflowTest ]; then
  function c4b_diagnostics() {
    local result=$?
    if [ "$result" -ne 0 ]; then
      timeout 15s adb exec-out screencap -p > app/build/preview-c4b-final.png || true
      timeout 15s adb logcat -d > app/build/preview-c4b-logcat.txt || true
      timeout 20s adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4b app/build/preview-composer-c4b-screens || true
    fi
  }
  trap c4b_diagnostics EXIT
  timeout 360s adb shell am instrument -w -r -e class com.mrayover.practicelogger.ComposerC4BWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
else
  adb shell am instrument -w -r -e class com.mrayover.practicelogger.${PREVIEW_TEST_CLASS:-HomeNavigationPreviewTest} com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-home.txt
fi
adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/home-preview app/build/preview-home-screens || true
adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/metronome-pulse app/build/preview-metronome-screens || true
adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/practice-refinement app/build/preview-refinement-screens || true
adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/loop-refinement app/build/preview-loop-screens || true
if [ "${PREVIEW_TEST_CLASS:-}" = ComposerFoundationWorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c1 app/build/preview-composer-screens
  grep -q 'OK (1 test)' app/build/preview-home.txt
  adb shell am force-stop "$package"
  adb shell am instrument -w -r -e class com.mrayover.practicelogger.ComposerColdReopenTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-cold.txt
  grep -q 'OK (1 test)' app/build/preview-composer-cold.txt
  if [ "${COMPOSER_C2:-false}" = true ]; then
    adb shell am instrument -w -r -e class com.mrayover.practicelogger.ComposerPianoRollWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c2.txt
    adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c2 app/build/preview-composer-c2-screens || true
    grep -q 'OK (1 test)' app/build/preview-composer-c2.txt
    adb shell am force-stop "$package"
    adb shell am instrument -w -r -e composerC2 true -e class com.mrayover.practicelogger.ComposerColdReopenTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c2-cold.txt
    grep -q 'OK (1 test)' app/build/preview-composer-c2-cold.txt
  fi
fi
if [ "${PREVIEW_TEST_CLASS:-}" = ComposerPianoRollWorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c2 app/build/preview-composer-c2-screens || true
  grep -q 'OK (1 test)' app/build/preview-home.txt
  adb shell am force-stop "$package"
  adb shell am instrument -w -r -e composerC2 true -e class com.mrayover.practicelogger.ComposerColdReopenTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c2-cold.txt
  grep -q 'OK (1 test)' app/build/preview-composer-c2-cold.txt
fi
if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC3WorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c3 app/build/preview-composer-c3-screens || true
fi
if [[ "${PREVIEW_TEST_CLASS:-}" == RecordingWaveformWorkflowTest,* ]]; then
  grep -q 'OK (2 tests)' app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-}" = LoopRecorderPreviewTest ]; then
  grep -q 'OK (3 tests)' app/build/preview-home.txt
elif [[ "${PREVIEW_TEST_CLASS:-}" == PracticeRefinementPreviewTest,* ]]; then
  grep -q 'OK (4 tests)' app/build/preview-home.txt
elif [ "${PREVIEW_TEST_CLASS:-HomeNavigationPreviewTest}" = MetronomeButtonPulseTest ] || [ "${PREVIEW_TEST_CLASS:-}" = PracticeRefinementPreviewTest ]; then
  grep -q 'OK (2 tests)' app/build/preview-home.txt
else
  grep -q 'OK (1 test)' app/build/preview-home.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC3WorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c3 app/build/preview-composer-c3-screens || true
  grep -q 'OK (1 test)' app/build/preview-home.txt
  adb shell am force-stop "$package"
  adb shell am instrument -w -r -e composerC3 true -e class com.mrayover.practicelogger.ComposerColdReopenTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c3-cold.txt
  grep -q 'OK (1 test)' app/build/preview-composer-c3-cold.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerCompactWorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-compact app/build/preview-composer-compact-screens || true
  grep -q 'OK (1 test)' app/build/preview-home.txt
  adb shell am force-stop "$package"
  adb shell am instrument -w -r -e compactCold true -e class com.mrayover.practicelogger.ComposerCompactWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-compact-cold.txt
  grep -q 'OK (1 test)' app/build/preview-composer-compact-cold.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4AWorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4a app/build/preview-composer-c4a-screens || true
  grep -q 'OK (1 test)' app/build/preview-home.txt
  adb shell am force-stop "$package"
  timeout 120s adb shell am instrument -w -r -e c4aCold true -e class com.mrayover.practicelogger.ComposerC4AWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c4a-cold.txt
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4a app/build/preview-composer-c4a-screens || true
  grep -q 'OK (1 test)' app/build/preview-composer-c4a-cold.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4BWorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4b app/build/preview-composer-c4b-screens || true
  grep -q 'OK (1 test)' app/build/preview-home.txt
  adb shell am force-stop "$package"
  timeout 120s adb shell am instrument -w -r -e c4bCold true -e class com.mrayover.practicelogger.ComposerC4BWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c4b-cold.txt
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4b app/build/preview-composer-c4b-screens || true
  grep -q 'OK (1 test)' app/build/preview-composer-c4b-cold.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4CWorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4c app/build/preview-composer-c4c-screens || true
  grep -q 'OK (1 test)' app/build/preview-home.txt
  adb shell am force-stop "$package"
  timeout 120s adb shell am instrument -w -r -e c4cCold true -e class com.mrayover.practicelogger.ComposerC4CWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c4c-cold.txt
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4c app/build/preview-composer-c4c-screens || true
  grep -q 'OK (1 test)' app/build/preview-composer-c4c-cold.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4DWorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4d app/build/preview-composer-c4d-screens || true
  grep -q 'OK (1 test)' app/build/preview-home.txt
  adb shell am force-stop "$package"
  timeout 120s adb shell am instrument -w -r -e c4dCold true -e class com.mrayover.practicelogger.ComposerC4DWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c4d-cold.txt
  grep -q 'OK (1 test)' app/build/preview-composer-c4d-cold.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4DTouchTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4d-touch app/build/preview-composer-c4d-touch-screens
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4DTailTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4d app/build/preview-composer-c4d-screens
  adb shell am force-stop "$package"
  timeout 120s adb shell am instrument -w -r -e c4dCold true -e class com.mrayover.practicelogger.ComposerC4DWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c4d-cold.txt
  grep -q 'OK (1 test)' app/build/preview-composer-c4d-cold.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC4EWorkflowTest ]; then
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4e app/build/preview-composer-c4e-screens
  adb shell am force-stop "$package"
  timeout 120s adb shell am instrument -w -r -e c4eCold true -e class com.mrayover.practicelogger.ComposerC4EWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c4e-cold.txt
  grep -q 'OK (1 test)' app/build/preview-composer-c4e-cold.txt
  adb pull /sdcard/Android/data/com.mrayover.practicelogger.app/files/composer-c4e app/build/preview-composer-c4e-screens
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC5AWorkflowTest ]; then
  adb shell am force-stop "$package"
  timeout 120s adb shell am instrument -w -r -e c5aCold true -e class com.mrayover.practicelogger.ComposerC5AWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c5a-cold.txt
  grep -q "OK (1 test)" app/build/preview-composer-c5a-cold.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC5BWorkflowTest ]; then
  adb shell am force-stop "$package"
  timeout 120s adb shell am instrument -w -r -e c5bCold true -e class com.mrayover.practicelogger.ComposerC5BWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c5b-cold.txt
  grep -q "OK (1 test)" app/build/preview-composer-c5b-cold.txt
fi

if [ "${PREVIEW_TEST_CLASS:-}" = ComposerC5CWorkflowTest ]; then
  adb shell am force-stop "$package"
  timeout 120s adb shell am instrument -w -r -e c5cCold true -e class com.mrayover.practicelogger.ComposerC5CWorkflowTest com.mrayover.practicelogger.app.test/androidx.test.runner.AndroidJUnitRunner | tee app/build/preview-composer-c5c-cold.txt
  grep -q 'OK (1 test)' app/build/preview-composer-c5c-cold.txt
fi
