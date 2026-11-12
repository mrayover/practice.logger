# practice.logger — RW1 delivered

RW1 recording waveform and scrubbing is implemented and delivered in **0.42.0/build115** on **block-ux-stage1**. [CURRENT_STATE](CURRENT_STATE.md) owns exact application/test source, APK/ZIP paths/hashes, signed-delivery integrity and focused evidence. App/package/test source 145ff9d0a61dff7aa1f26cfe79e91c148e124f7a; later record commits do not alter the package. Room25/ZIP10/JSON7 unchanged.

**Exact next task: owner-phone [RW1 waveform/scrubbing checklist](MANUAL_TESTS.md#rw1--saved-recording-waveform-scrubbing).** Engineering stops here. Do not begin adjacent audio features, Playground or F1. Further implementation or comprehensive regression requires separate owner authorization.

The existing Focused Practice selected free/loop rows use shared waveform extraction/drawing and their existing MediaPlayer/static AudioTrack. Preserve release-only source-bound scrubbing, silent selection, stopped cue/explicit Play, static-loop pause/reposition/repeat/resume, actual consumed-frame cursor wrap, capture/review ownership and foreground safeguards. No persistent playback-position feature or reference-source checkpoint change was introduced.

Two host checks pass on Windows/Linux; app/release/device-test compilation and release vital lint pass; one isolated API35 recording engine/gesture workflow verifies the five supported saved recording types. Physical/layout/acoustic acceptance remains pending. Read actual limitations/attempts in CURRENT_STATE and WORK_LOG; no broad regression was run. Earlier phone checks remain pending and Stage4 remains owner-deferred0/10.

Use C:/Users/mrove/Projects/practice.logger; the older OneDrive checkout is untouched. Read [AGENTS](AGENTS.md), [BUILD_PLAN](BUILD_PLAN.md), [master plan](PRACTICE_LOGGER_V1_MASTER_PLAN.md), mandatory efficiency procedure in [WORK_LOG](WORK_LOG.md), and [verification policy](docs/VERIFICATION_POLICY.md). Preserve unrelated work and previous distributions.
