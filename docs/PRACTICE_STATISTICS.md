# G3 — Recorded practice statistics

`PRACTICE_FACTS_SQL` in PracticeStatistics.kt is a derived, read-only union. Existing TranscribeActivityDao provides activity facts and shared totals; GoalDao aggregates the same rows. G3 originally introduced no statistics store, table, migration or backup extension (Room24/ZIP8); subsequent increments, including W2 below, add their own user-authored data. JSON7 still excludes standalone Transcribe history; use complete ZIP for that history.

Each non-draft placement contributes exactly its committed activeMs, irrespective of session completion, placement outcome or configured duration. Distinct placement IDs retain repeated Blocks and overtime. Completed standalone Transcribe activities contribute once; unfinished standalone activities retain the established completion-only reporting rule. Prep, pause, background and playback are not inputs. One-to-one primary-key joins to Goal snapshots, session associations, completion and reflection records cannot multiply time. Current Goal/Block links, Routines, plans and note collections never enter totals.

Attribution precedence:

1. A saved placement snapshot supplies its Goal; a snapshot row with NULL Goal is explicit No Goal and blocks all fallback.
2. Only an absent snapshot can fall back to the explicit saved session_goals association. Its sessionId primary key guarantees one Goal. These minutes are labeled legacy Goal-session attribution; current Block relationships are never used to infer history.
3. No snapshot and no explicit session association means unknown historical time.
4. Standalone Transcribe keeps its explicit saved Goal, or recorded No Goal.

Global total = Goal-attributed totals + recorded No Goal + unknown historical time. Block totals plus standalone Transcribe also reconcile to the global total. Historical Goal IDs are stable after reassignment, ordering, completion and reopening. Goal Detail shows weekly actual and planned minutes alongside its cumulative recorded practice.

Home **This Week** opens Statistics. It shows cumulative and current-week time, per-Block time, standalone time, per-Goal time, explicit legacy portions, recorded No Goal, unknown historical time, and the latest20 activity records. Goal Detail shows its total and latest12 positive-time related records, with access to existing session notes/reflections. Pending placements in those summaries are labeled Unfinished rather than Skipped. Practice history uses historical placement names; current names may differ.

Weekly statistics match Journal's Monday–Sunday local week. The separate Sunday–Saturday planning calendar is unchanged. A placement's time belongs to its session's start date; standalone time belongs to its activity start date. No midnight split is claimed. Skipped outcomes with zero time are visible but contribute zero; Prep-only pending placements do not appear as practice. Recent rows use recorded start-date ordering, not an invented last-active timestamp.

Future check-ins can consume PracticeFact's stable placement/session/Block IDs, start date, completedAt when present, actual time, outcome, attribution and confidence/security. Those IDs resolve the existing Block current primary Goal, full Block/session reflections and timestamped/legacy notes through the established DAOs. No scheduling, recommendations, tracking tables or background jobs are added.

Verification is focused on PracticeStatisticsTest and one StatisticsNavigationTest in the existing manual owner-preview workflow's statistics selector. All other test infrastructure is retained for separately authorized F1. Physical checks are in MANUAL_TESTS.md; exact executed outcomes and package provenance belong in CURRENT_STATE.md.
LGE-1/LGE-2 were later separately assigned and implemented in builds113/114 after the owner-deferred Stage4; see the [Learning Guidance Extension contract](LEARNING_GUIDANCE_EXTENSION.md). Their separate implementation preserves G3 as the shared time-accounting model. G3 remains the single time/Goal accounting model; future evidence must not multiply its rows or infer retention from minutes/confidence.


## Weekly planned and actual minutes

Home, Goal views and the This Week summary use the Sunday–Saturday planning calendar. Overall planned minutes sum the configured Block duration times planned repetitions once per weekly membership. Each Goal includes every weekly membership linked to it. One Block can be linked to several Goals and can therefore contribute to each of their plans.

Actual totals read the existing G3 practice facts and preserve milliseconds through calculation. All qualifying time counts overall. Goal totals follow the placement's saved attribution or explicit legacy session link, including attributed time with no current planned Block. Completed standalone Transcribe counts once. Local start dates determine the week, including across daylight-saving changes. Home displays whole minutes and caps only the bar. With zero planned minutes the Home track is empty and actual minutes stay visible.

Legacy manual Goal percentages and W2 target rows remain in their original storage and supported backup formats. Goal metadata edits preserve the old percentage. The app derives minute totals and bar fill from weekly memberships and recorded practice. The saved Goal percentage is used only to reserve the original subtitle height when it previously wrapped. The presence of a historical target preserves the original footer geometry. A previously trackless footer draws the bar in the existing space below its first row. [Backup contract](../BACKUP_FORMAT.md#complete-portable-zip-v11--weekly-practice-targets) · [Delivery and verification](../CURRENT_STATE.md).
