# G3 — Recorded practice statistics

`PRACTICE_FACTS_SQL` in PracticeStatistics.kt is a derived, read-only union. Existing TranscribeActivityDao provides activity facts and shared totals; GoalDao aggregates the same rows. G3 originally introduced no statistics store, table, migration or backup extension (Room24/ZIP8); subsequent increments, including W2 below, add their own user-authored data. JSON7 still excludes standalone Transcribe history; use complete ZIP for that history.

Each non-draft placement contributes exactly its committed activeMs, irrespective of session completion, placement outcome or configured duration. Distinct placement IDs retain repeated Blocks and overtime. Completed standalone Transcribe activities contribute once; unfinished standalone activities retain the established completion-only reporting rule. Prep, pause, background and playback are not inputs. One-to-one primary-key joins to Goal snapshots, session associations, completion and reflection records cannot multiply time. Current Goal/Block links, Routines, plans and note collections never enter totals.

Attribution precedence:

1. A saved placement snapshot supplies its Goal; a snapshot row with NULL Goal is explicit No Goal and blocks all fallback.
2. Only an absent snapshot can fall back to the explicit saved session_goals association. Its sessionId primary key guarantees one Goal. These minutes are labeled legacy Goal-session attribution; current Block relationships are never used to infer history.
3. No snapshot and no explicit session association means unknown historical time.
4. Standalone Transcribe keeps its explicit saved Goal, or recorded No Goal.

Global total = Goal-attributed totals + recorded No Goal + unknown historical time. Block totals plus standalone Transcribe also reconcile to the global total. Historical Goal IDs are stable after reassignment, ordering, completion and reopening. Existing priority colors and manual progress remain independent; Goal Detail offers an optional manual-assessment panel rather than calculating mastery from time.

Home Total practice opens Statistics. It shows cumulative and current-week time, per-Block time, standalone time, per-Goal time, explicit legacy portions, recorded No Goal, unknown historical time, and the latest20 activity records. Goal Detail shows its total and latest12 positive-time related records, with access to existing session notes/reflections. Pending placements in those summaries are labeled Unfinished rather than Skipped. Practice history uses historical placement names; current names may differ.

Weekly statistics match Journal's Monday–Sunday local week. The separate Sunday–Saturday planning calendar is unchanged. A placement's time belongs to its session's start date; standalone time belongs to its activity start date. No midnight split is claimed. Skipped outcomes with zero time are visible but contribute zero; Prep-only pending placements do not appear as practice. Recent rows use recorded start-date ordering, not an invented last-active timestamp.

Future check-ins can consume PracticeFact's stable placement/session/Block IDs, start date, completedAt when present, actual time, outcome, attribution and confidence/security. Those IDs resolve the existing Block current primary Goal, full Block/session reflections and timestamped/legacy notes through the established DAOs. No scheduling, recommendations, tracking tables or background jobs are added.

Verification is focused on PracticeStatisticsTest and one StatisticsNavigationTest in the existing manual owner-preview workflow's statistics selector. All other test infrastructure is retained for separately authorized F1. Physical checks are in MANUAL_TESTS.md; exact executed outcomes and package provenance belong in CURRENT_STATE.md.
LGE-1/LGE-2 are now explicitly approved future increments after Stage4 practical-use review; see the [Learning Guidance Extension contract](LEARNING_GUIDANCE_EXTENSION.md). Neither is implemented by G3 or by that planning assignment. G3 remains the single time/Goal accounting model; future evidence must not multiply its rows or infer retention from minutes/confidence.


## W2 — weekly practice target

One optional target stores only canonical Sunday weekStart and positive whole targetMinutes (1–10,080) in weekly_practice_targets. This Week may explicitly set/edit/clear the current or upcoming week without any assigned Blocks. Previous weeks are read-only; rollover neither deletes history nor copies a target. Home keeps cumulative Statistics and displays the current week's actual time even when unset, with an explicit This Week editing route.

W2 reuses TranscribeActivityDao.observePracticeTime, the existing G3 PRACTICE_FACTS_SQL union. Distinct placements contribute committed activeMs, including unfinished sessions, partial practice and overtime; completed standalone Transcribe contributes once. No planned durations, Prep, pause, background time, passive playback, current Goal/Routine links or LGE evidence enter the calculation. Placements belong to session start dates; standalone activity belongs to activity start dates, with no invented midnight splits.

W2 uses This Week's Sunday–Saturday calendar (practiceWeek), while Statistics and Journal retain Monday–Sunday. Boundaries are local start-of-Sunday through the next local start-of-Sunday, end exclusive, using ZoneId rather than fixed168-hour arithmetic. The existing lifecycle-aware15-second poll now refreshes both week and zone; foreground resume checks immediately.

Ratio uses actual committed milliseconds divided by targetMinutes×60,000. Only visual fill caps at100%; actual totals and numeric percentages continue above it. Display minutes retain up to two decimal places and percentage one; tiny positive values show <0.01minutes/<0.1% rather than zero. Remaining/extra derives from original milliseconds. Targets measure practice commitment, never mastery or manual Goal completion.

Room26 and complete ZIP11 preserve all target rows; JSON7 excludes them. No duplicate counter or statistics cache is persisted. See [backup contract](../BACKUP_FORMAT.md#complete-portable-zip-v11--weekly-practice-targets) and [actual verification](../CURRENT_STATE.md).
