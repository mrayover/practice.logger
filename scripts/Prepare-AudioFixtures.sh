#!/usr/bin/env bash
set -euo pipefail
# Generated test assets only. Keep codec fixtures consistent across full and scoped CI.
ffmpeg_bin=ffmpeg
if ! command -v ffmpeg >/dev/null 2>&1; then
  # Same pinned Linux fixture encoder used by the focused Transcribe workflow.
  work=$(mktemp -d)
  trap 'rm -rf "$work"' EXIT
  wheel="$work/imageio-ffmpeg.whl"
  curl -fsSL --retry 2 --max-time 60 'https://files.pythonhosted.org/packages/a0/2d/43c8522a2038e9d0e7dbdf3a61195ecc31ca576fb1527a528c877e87d973/imageio_ffmpeg-0.6.0-py3-none-manylinux2014_x86_64.whl' -o "$wheel"
  echo "c7e46fcec401dd990405049d2e2f475e2b397779df2519b544b8aab515195282  $wheel" | sha256sum --check
  unzip -p "$wheel" 'imageio_ffmpeg/binaries/ffmpeg-linux-x86_64-*' > "$work/ffmpeg"
  chmod +x "$work/ffmpeg"
  ffmpeg_bin="$work/ffmpeg"
fi
out=app/src/androidTest/assets/audio
mkdir -p "$out"
duration=${AUDIO_FIXTURE_DURATION:-0.45}
[[ "$duration" =~ ^[0-9]+([.][0-9]+)?$ ]]
timeout 15s "$ffmpeg_bin" -y -v error -f lavfi -i "sine=frequency=440:duration=$duration" -ar 44100 -ac 1 -c:a libmp3lame "$out/backing.mp3"
timeout 15s "$ffmpeg_bin" -y -v error -f lavfi -i "sine=frequency=330:duration=$duration" -ar 44100 -ac 1 -c:a aac "$out/backing.m4a"
test -s "$out/backing.mp3"
test -s "$out/backing.m4a"
if [ "${AUDIO_FIXTURE_WAV:-false}" = true ]; then
  timeout 15s "$ffmpeg_bin" -y -v error -f lavfi -i "sine=frequency=440:duration=$duration" -ar 44100 -ac 1 -c:a pcm_s16le "$out/backing.wav"
  test -s "$out/backing.wav"
fi
