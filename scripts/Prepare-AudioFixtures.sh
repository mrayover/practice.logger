#!/usr/bin/env bash
set -euo pipefail
# Generated test assets only. Keep codec fixtures consistent across full and scoped CI.
if ! command -v ffmpeg >/dev/null 2>&1; then
  timeout 90s sudo apt-get update -qq
  timeout 120s sudo apt-get install -y --no-install-recommends ffmpeg
fi
out=app/src/androidTest/assets/audio
mkdir -p "$out"
duration=${AUDIO_FIXTURE_DURATION:-0.45}
[[ "$duration" =~ ^[0-9]+([.][0-9]+)?$ ]]
timeout 15s ffmpeg -y -v error -f lavfi -i "sine=frequency=440:duration=$duration" -ar 44100 -ac 1 -c:a libmp3lame "$out/backing.mp3"
timeout 15s ffmpeg -y -v error -f lavfi -i "sine=frequency=330:duration=$duration" -ar 44100 -ac 1 -c:a aac "$out/backing.m4a"
test -s "$out/backing.mp3"
test -s "$out/backing.m4a"
