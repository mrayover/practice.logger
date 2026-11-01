#!/usr/bin/env bash
set -euo pipefail
# The workflow checks out the immutable build27 source at baseline-source.
# Rebuild instead of depending on a CI artifact's retention period.
test -f baseline-source/gradlew
mkdir -p baseline-apk
bash baseline-source/gradlew -p baseline-source assembleDebug --stacktrace
cp baseline-source/app/build/outputs/apk/debug/app-debug.apk baseline-apk/app-debug.apk
