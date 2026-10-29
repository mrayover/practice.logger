#!/usr/bin/env bash
set -euo pipefail
# One shell owns these variables. The emulator runner executes separate YAML lines
# in separate shells. This identity is only the disposable CI debug identity.
APKSIGNER=$(find "$ANDROID_HOME/build-tools" -name apksigner | sort -V | tail -n 1)
DEBUG_STORE=$(find "$HOME" -name debug.keystore -type f -print -quit)
if [ -z "$DEBUG_STORE" ]; then
  bash ./gradlew validateSigningDebug --rerun-tasks
  DEBUG_STORE=$(find "$HOME" -name debug.keystore -type f -print -quit)
fi
test -n "$APKSIGNER" && test -n "$DEBUG_STORE"
"$APKSIGNER" sign --ks "$DEBUG_STORE" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --out baseline-apk/upgrade-baseline.apk baseline-apk/app-debug.apk
"$APKSIGNER" sign --ks "$DEBUG_STORE" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android app/build/outputs/apk/debug/app-debug.apk
"$APKSIGNER" sign --ks "$DEBUG_STORE" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
