# This Week repetition targets

Weekly membership stores its own `plannedRepetitions`, defaulting to 3 for every new addition, including carry-forward. Re-adding an existing membership does not change its target. Expand an assigned This Week/Prepare Week Block to use the minus/plus control (minimum 1).

Collapsed rows show completed planned repetitions as small neutral dots and remaining repetitions as Goal-colored squares, with a completion slit only at or above target. Targets above six use condensed dot/square counts. The numerical completed/target count includes completions beyond the target.

Actual progress is derived from existing placement-keyed completion snapshots within local Sunday-Saturday boundaries. SessionRepository and Playground retain their existing reward pipeline: each successful completion earns an independent reward, without a weekly cap. Editing/removing weekly membership never edits completions or layouts.

Planned Minutes appears immediately below the heading and sums current configured Block duration times each weekly target. It is independent of actual practice time and the weekly minute goal.

Room 27→28 adds one column with SQL default 1, retaining the previous one-practice meaning of all existing weekly memberships and their progress. New application-created memberships explicitly use 3. Complete ZIP13/Room28 persists the new field; frozen ZIP1-12 restore existing memberships with target 1. JSON7 remains unchanged and excludes weekly membership.

Focused checks extend WeekPlanTest (target/default/duration/removal, repeated completion/rewards, backup records, reopening, and 27→28 preservation) and WeekCompletionTest (local week boundaries/counts). Actual execution and delivery status are recorded in [CURRENT_STATE](../CURRENT_STATE.md).
