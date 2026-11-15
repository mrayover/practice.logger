# compose.logger C3 — instruments and Loop playback

[Current delivery/evidence](../CURRENT_STATE.md) · [C2 editor](COMPOSER_C2.md) · [V1 contract](COMPOSE_LOGGER_V1_SPEC.md)

## Audio ownership

ComposerAudio owns one foreground-only AudioTrack/focus request and one native/render worker. Reference Piano elsewhere, Library, sessions, recordings, metronome and database are unchanged. Leaving Loop, backgrounding, focus loss, noisy-output broadcast and disposal invalidate queued touch events, clear voices and stop output without automatic restart.

TinySoundFont v0.9 is vendored unmodified at commit 853a0a171759f1ddba0de1442133a75912bbeffa ([upstream](https://github.com/schellingb/TinySoundFont/tree/853a0a171759f1ddba0de1442133a75912bbeffa)), MIT. Its small implementation compiles for arm64-v8a, armeabi-v7a, x86 and x86_64 with NDK 27.2.12479018/CMake 3.22.1. JNI and pinned play-index access live in one bounded Composer wrapper. Binary redistribution includes the complete MIT notice in composer/licenses/TinySoundFont-MIT.txt. No synth service or plugin host.

All nine SF2 banks load and channel/voice capacity is allocated on the worker before output starts. No asset/file loading, snapshot construction or growing voice arrays occur in steady rendering. Queued commands serialize changes with rendering. Output is 48 kHz stereo PCM16 in 240-frame blocks, split at exact musical event boundaries. Each bank has a bounded 128-voice/layer pool with TSF voice stealing; layers consume voices, reducing effective polyphony in dense scores. Audition has 64 bounded tokens. Banks mix into one output with headroom and final limiting. TSF omits reverb/chorus/modulators; physical latency/quality need owner acceptance.

Channels 0–7 are sequence tracks and 8–15 audition. Pinned TSF play indices release every layer of exactly the requested note, including overlapping identical pitches. Keyboard scroll/pattern changes clear audition without stopping the sequencer; Stop/interruption clears both. Melodic edge key zones extend in memory to MIDI 0–127 without changing SF2 bytes or stored pitches. Extreme transposition can sound unnatural. Touch velocity defaults to 100; playback respects stored 1–127 velocity.

## Bank provenance and licenses

All nine banks explicitly use [CC0-1.0](https://creativecommons.org/publicdomain/zero/1.0/), without attribution/share-alike conditions. Upstream author/readme files and available full dedication texts are retained in the APK. Organ's upstream readme expressly declares CC0; full CC0 text is also bundled with the other banks. [Original bytes, SHA256, filenames and download identities](../app/src/main/assets/composer/sounds/manifest.json). Files are unchanged from their archives. Total 65,217,226bytes (62.20MiB) uncompressed, bundled once as assets, never copied into projects. No optional upright bass or extra packs.

| Instrument / stable identity | Original file / version identity | Bytes | Original range / quality limits |
| --- | --- | ---: | --- |
| Acoustic piano / acoustic-piano | [UprightPianoKW-small-20190703.sf2](https://freepats.zenvoid.org/Piano/UprightPianoKW/UprightPianoKW-small-SF2-20190703.7z) | 9,456,310 | 21–108; compact upright, fewer layers than full bank |
| FM electric piano / fm-electric-piano | [FM-Piano2 20161112.sf2](https://github.com/freepats/fm-piano2/releases/download/2016-11-12/FM-Piano2-SF2-20161112.7z) | 15,878,656 | 26–103; bright FM variant |
| Organ / organ | [DrawbarOrganEmulation-20190712.sf2](https://freepats.zenvoid.org/Organ/DrawbarOrganEmulation/DrawbarOrganEmulation-SF2-20190712.tar.xz) | 6,505,320 | 33–98; sampled drawbar, no rotary effect |
| Electric bass / electric-bass | [FingerBassYR 20190930.sf2](https://github.com/freepats/electric-bass-YR/releases/download/2019-09-30/FingerBassYR-SF2-20190930.7z) | 4,220,300 | 26–45; finger bass, heavy transposition above bass range |
| Synth bass / synth-bass | [SynthBass2 20210405.sf2](https://github.com/freepats/synth-bass-2/releases/download/2021-04-05/SynthBass2-SF2-20210405.7z) | 4,635,192 | 28–88; FM Bass 2 |
| Synth strings / synth-strings | [SynthStrings1 20200528.sf2](https://github.com/freepats/synth-strings-1/releases/download/2020-05-28/SynthStrings1-SF2-20200528.7z) | 5,910,658 | 24–100; synthesized ensemble |
| Synth lead / synth-lead | [SynthBassLead 20200522.sf2](https://github.com/freepats/synth-bass-lead/releases/download/2020-05-22/SynthBassLead-SF2-20200522.7z) | 8,542,812 | 21–108; Bass & Lead patch |
| Synth pad / synth-pad | [SweepPad 20190813.sf2](https://github.com/freepats/sweep-pad/releases/download/2019-08-13/SweepPad-SF2-20190813.7z) | 8,414,208 | 33–96; Sweep Pad |
| Electronic drums / electronic-drums | [SynthesizerPercussion-20220718.sf2](https://freepats.zenvoid.org/Percussion/SynthesizerPercussion/SynthesizerPercussion-SF2-20220718.7z) | 1,653,770 | 19 hits at source 48–66, remapped; other keys silent |

Upright KW recordings: Gonzalo/Roberto with permission from Inma Martínez de Miguel. Finger Bass YR: Andrea Biasior. Synth Bass & Lead: Elf of Happy and Love. Other listed sounds: Roberto/FreePats; retained readmes give complete provenance.

Every bank resolves preset bank 0/program 0 through registryVersion 1, stable instrumentId and presetId=default. The existing strict v1 project codec already supports these references: no migration or musical rewrite. C1/C2 reference-piano/default stays an acoustic-piano alias without silently rewriting it. Unknown references remain intact; Play explains unavailable sounds and the picker permits an explicit replacement. Changing sounds is undoable and preserves all note/pattern identities and values.

Drum mapping (project MIDI → source key):35→49,36→48,38→50,40→51,42→52,44→53,46→54,49→55,51→56,41→57,43→58,45→59,47→60,48→61,50→62,39→63,70→64,82→65,75→66. Labels identify hits; other keys show a dash and are silent. These are electronic approximations, not a complete GM drum kit.

## Loop behavior

Play compiles an immutable snapshot off UI/audio threads and begins at shared bar1/beat1. Each of eight tracks retains its independently selected A/B/C pattern while Loop is open. Patterns repeat at their own lengths. The explicit region is the longest selected pattern, including muted tracks so mixer changes cannot shift the clock/region. Supported 1/2/4/8/16-bar lengths divide each other; the longest is also their least common multiple. No truncation/shift of musical notes.

Tick t maps to round(t×48000×60/(BPM×960)). Per-track streams retain absolute tick cycles rather than adding rounded loop durations, preventing accumulated rounding drift. At equal frames note-offs precede note-ons, including boundaries. Quarter/eighth/sixteenth/thirty-second, triplet and free-tick notes use the same mapping. Green playhead and bar/beat come from AudioTrack's consumed playback head. The red insertion/paste cursor is independent. UI refresh never schedules sound. Playback labels use the snapshot meter/tempo while editing settings.

Repeat Off plays one full region; switching it off during playback stops at the current region's end. Stop clears both sequence/audition and retains consumed position. Play always begins a fresh snapshot from zero. Beginning resets position and, while playing, restarts the current snapshot. Repeated Play is disabled while running. Musical edits, instruments/patterns/tempo and saves remain safe; musical changes apply next Play, as the UI states. No Arrange playback.

Mute/Solo/Volume apply live. Muting clears that track's sequence voices; unmuting joins future onsets. Any Solo enables only soloed tracks; Mute takes priority. Volume affects audition and sequence channels. Mixer controls and selected patterns are Loop-session/view state retained over ordinary recreation; instrument assignments and musical project data are durable. No advanced effects/mixer or pack installation.

## Focused checks and limits

Actual results/failures/signed provenance belong in CURRENT_STATE/WORK_LOG. Host checks cover musical timing/repeats/boundaries, coherent snapshots, compatibility/IDs, clipboard/Undo, atomic storage and existing PCM. One API35 workflow covers packaged bank hashes/loading/velocity/MIDI edges/independent release/clear silence, actual output writes, retained patterns, sounds/mixer, Stop/repeat/restart/focus/recreation and saved cold reopen. C2 scrolling/Hand/dropdown/clipboard/Undo was verified before C3; final C3 captures cover pitch boundaries and toolbar. No comprehensive regression/device matrix or physical audio acceptance.

[Phone checklist](../MANUAL_TESTS.md#composelogger-c3--owner-phone-check). Practice ZIP/JSON still exclude Composer. Projects remain device-local until C7; do not uninstall/clear data. Stop before C4–C7/F1.
