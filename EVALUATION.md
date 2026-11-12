# Stage 4 — Ten real practice sessions

Updated November 12, 2026. Stage4 is resolved for sequencing by owner-approved session deferral; the practical-use review was not performed. Signed0.39.0/build112 remains the current release identified in CURRENT_STATE.md. **0 of 10 real sessions documented**; no new owner observations or phone model/Android details have been supplied. Stage3/G3 physical acceptance remains pending. Automated/emulator sessions never count. Existing shipped features and the approved roadmap are preserved.

Sequence: **G3 → Stage 4 practical-use review → LGE-1 → LGE-2 → remaining owner-approved V1 features → F1 → F2**. Stage4 is real-world usability review, not authorization for broad regression, a device matrix or certification. Comprehensive regression remains deferred until the owner explicitly authorizes F1. LGE-1 and LGE-2 need separate implementation assignments after the review; no engine is implemented here.

## Before session 1

Record the phone model, Android version and exact build here. Use the focused G3 and outstanding Stage3 phone checklist in MANUAL_TESTS.md while practicing: install-over data/media, paused recovery, references, notes/history, actual-time totals and understandable Goal attribution. Retain a complete portable ZIP backup outside the app; JSON7 excludes media and newer history and is not a complete recovery copy. Complete ZIP recovery is implemented. Record actual provider/backup usability when encountered; do not launch a historical restore campaign for this review or clear the main installation. Any deliberately assigned restore check uses a separate empty installation.

Phone / Android / build: **not yet supplied**.

Stage 3 phone acceptance and date: **pending**.

## What to record

Use the existing optional Session note to jot down friction immediately after practicing, then Save note. No new in-app workflow or extra mandatory data entry is needed. Copy concise observations into the table when reporting results:

- Approximate taps/time from Home to the first running Block, including choosing or creating Blocks.
- Whether instructions and timer were readable, including while holding the instrument.
- Any tedious editing/reordering, unexpected pauses, lost notes, timing/recovery error or unclear outcome.
- Whether notes/history helped at the next practice; whether session flexibility was actually missing.
- Whether Ready/Prep/Start, source switching, completion reflections and Statistics felt clear during real practice; note a concrete example rather than inferring mastery from minutes or confidence.
- One concrete improvement tied to an observed event. "None" is useful evidence too.

If data loss, crash, inflated timing, failed recovery or unusable backup occurs, pause the evaluation, retain the backup and exact build, record reproduction steps and fix the reliability problem before counting further sessions. Do not uninstall as a fix.

## Real-session record

| # | Date / build / saved session | Taps or time to start | Observed friction and useful behavior | Priority / follow-up |
|---|---|---|---|---|
| 1 | Pending | | | |
| 2 | Pending | | | |
| 3 | Pending | | | |
| 4 | Pending | | | |
| 5 | Pending | | | |
| 6 | Pending | | | |
| 7 | Pending | | | |
| 8 | Pending | | | |
| 9 | Pending | | | |
| 10 | Pending | | | |

## Review after ten

Group repeated observations, distinguish bugs from convenience requests, and fix the most frequent concrete friction first. Record each decision as **observed problem → simplest change → cost/tradeoff → decision**. Update WORK_LOG.md and CURRENT_STATE.md for each development session; deliver a tested, versioned APK for fixes.

Record a concise usability conclusion, actual blockers and the next recommended bounded assignment. The old one-feature-or-none limit is superseded by the approved V1 roadmap: LGE-1 and LGE-2 are already approved future scope, and Stage4 does not revoke other approved features or authorize implementing them automatically. See [LGE architecture](docs/LEARNING_GUIDANCE_EXTENSION.md). The ten-session target remains; mark Stage4 complete only with actual ten-session evidence and a recorded review, or an explicit owner decision documenting a shorter review/remaining-session deferral. Do not manufacture observations or infer acceptance from the shipped APK. After review, recommend separately assigning LGE-1; the owner retains the decision. Feature freeze/F1 remain separately authorized after LGE and remaining owner-approved features.

Decision: **all ten undocumented sessions deferred by owner direction on November 12, 2026; no real-use acceptance claimed**.

On November 12, 2026, the owner directed: “Proceed as if they're reviewed, I don't know what I need to do.” This is recorded as owner direction to defer all ten undocumented evaluation sessions and proceed past the Stage4 prerequisite. No sessions or practical-use findings were supplied. The ten-session target and blank session table remain available for later voluntary observations; 0/10 sessions are documented. Stage3/G3 physical acceptance remains pending. Tradeoff: the next development phase can proceed without practical-use evidence to guide usability corrections. Recommend a separate LGE-1 assignment under docs/LEARNING_GUIDANCE_EXTENSION.md. This decision does not start LGE-1, authorize LGE-2 or authorize F1.
