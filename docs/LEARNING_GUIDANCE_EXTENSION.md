# Learning Guidance Extension — approved future architecture

Approved for the V1 roadmap on November 12, 2026. **LGE-1 separately assigned November12: implementation and focused delivery in progress. LGE-2 remains future scope and is not implemented or assigned.** This is the bounded approved addition to the earlier Deferred Practice Insights proposal. The [master plan](../PRACTICE_LOGGER_V1_MASTER_PLAN.md) owns phase order; [CURRENT_STATE](../CURRENT_STATE.md) owns shipped facts.

## LGE-1 implementation contract

Two additive stores implement the proposed design: learning_observations (placement primary key, restrictive placement and prior-observation anchor FKs) and guidance_events (stable operation ID, restrictive Block FK, unique Block/revision). Observations preserve timestamp, criterion text/token and explicit comparison anchor; corrections change only outcome/context and record correction time. No derived comparison cache exists; future comparisons must read corrected source evidence. Manual scheduling records resulting independent flag/date state, selected local base date/interval and origin; declining preserves existing state. offeredSuggestion is a nullable provenance slot reserved for LGE-2 and must be null in LGE-1. No suggestion or algorithm exists.

Room25 adds both tables without backfill. Complete ZIP9 adds their fixed typed columns, validates comparability/ownership, ordered event transitions and anchor cycles, restores anchors before dependants, and includes both stores in empty-target protection. ZIP1–8 retain frozen readers. JSON7 remains unchanged and excludes both stores. Clearing a flag/date never removes history; archive retains it, and restrictive deletion guards protect it.

Learning & revisits is an optional dialog in the practice description, saved Block editor and placement summary. Closing without saving records no observation; Not assessed is explicit and distinct. New criteria prefill from placement instructions; reusing an earlier target requires confirming unchanged conditions. Old criteria cannot be edited through a correction. Saving invokes no timing or audio API. Dates are manual opportunities, not reminders.

## Baseline and sequence

Live baseline inspected: block-ux-stage1 at14749db; Stage3C.2 and scoped Stage3D complete in signed build111; G3 complete in signed0.39.0/build112, application source58a13bd. Room24, complete ZIP8/readers1–8, legacy JSON7/readers1–7. G3/Stage3 phone acceptance is pending; Stage4 remains0/10 documented real sessions; the owner deferred all ten on November12, resolving the prerequisite without claiming a usability review or phone acceptance (see EVALUATION.md). No active local changes were present at reconciliation.

**G3 → Stage 4 practical-use review → LGE-1 → LGE-2 → remaining owner-approved V1 features → F1 → F2.** Each implementation increment requires a separate explicit assignment. Stage4 gathers real-world usability evidence using [EVALUATION](../EVALUATION.md); it is not a regression campaign or permission to start LGE. The owner decides when review is sufficient to assign LGE-1 and records any deferral of the ten-session target. Remaining approved work, including separately scoped Playground work, is considered before the owner explicitly authorizes F1 and feature freeze. LGE-2 completion does not authorize F1.

## Preserved architecture and interpretation

Five musician-defined ordered Goals, IDs, colors, relationships, primary Block associations, manual progress and historical snapshots remain authoritative. Preserve Block objectives/content, This Week, Session Builder, Routines, P1 Ready/Prep/Start and timing, P2 security/reflections/prompts, Journal, notes, recordings, multiple references, source-local annotations and backups. No duplicate Blocks, new Goal hierarchy, compulsory classification or separate learning-management system.

Immediate performance is not durable learning. Subjective confidence is not demonstrated retention. Comparisons require task-specific performance criteria; exploration needs no measurable output. Inactivity is not failure. Minutes and attendance never imply mastery. Suggestions are local, deterministic, explainable, optional and ignorable without penalty. No AI coaching or generative evaluation. These are design constraints, not a claim that the proposed heuristic is scientifically validated.

## Existing integration points inspected

| Existing record/surface | Reuse and boundary |
|---|---|
| `Placement` / `SessionData.kt` | Exact placement ID distinguishes repeated Blocks. Contains sourceBlockId, instruction snapshots, activeMs, prepMs and outcome. Session start is not an exact first-attempt timestamp. |
| `BlockCompletion` / `BlockReflection` in `ReflectionData.kt` | Completion timestamp/Goal snapshot and optional end-of-practice1–5 security already exist. Reflection is completion-owned; a first-attempt observation must not require a fabricated completion or overload security. `revisit` is free text, not a structured due date. |
| `SessionReflection`, `PracticeNote`, Journal | Keep their existing ownership and purposes. Stable placement/session IDs resolve context; do not copy notes or parse prose into performance outcomes. |
| `Block.action`, tempo, reference notes and placement snapshots | Prefill an optional concise performance criterion; they do not fully identify passage, target tempo, difficulty, aids or success condition. Editing a Block must not rewrite older criteria. |
| `PracticeFact` / `PRACTICE_FACTS_SQL`, Goal snapshots | Consume G3 committed history and trustworthy attribution; retain completion/observation timestamps when available. Never join multiple evidence/events into time totals. |
| Practice Now picker / This Week preparation | Reuse optional suggestion sections and existing explicit selection actions. Manual search, No Goal, exploration, Goal filters, builder ordering and Routines stay available. |
| Complete ZIP restore pipeline / deletion guards | Extend validation, nonempty checks, transaction witness/recovery and restrictive ownership together. Archive preserves evidence; history is never silently detached. |

## LGE-1 — learning evidence and optional revisits

Deliver a compact optional first-attempt observation within the existing Block practice workspace/history. Choices: **Held up / Met intended target; Partially held up; Didn't hold up yet; Not assessed.** This is separate from end-of-practice security and manual Goal progress. Saving or skipping it never pauses/restarts timers, recording, playback or metronome, and adds no compulsory Ready gate. Ordinary unassessed and exploratory practice remain complete workflows.

Bind each observation to the exact placement, not the Block alone or its position. Record the actual observation time and whether the musician confirms this was the first attempt before rehearsal/warm-up of that task. A later recollection or unknown context must remain labeled as such; the app cannot detect a first attempt from media playback, elapsed minutes or a button press. Missing observation and explicitly chosen Not assessed are distinguishable, neither is failure. Skipping a check-in writes no success/failure. Skipped practice cannot generate a retention success; if an observation was deliberately saved before a later Skip, preserve that fact with its skipped placement status and exclude it from interval reinforcement. An unfinished placement can retain its saved observation and resume under existing P1 rules; it is not implicitly completed or scheduled.

Use an optional concise criterion snapshot plus stable comparability token, reusing the Block objective as a starting point. The musician can confirm reuse of an earlier criterion or explicitly change it. Material/passage, target tempo, success threshold, relevant assistance and difficulty changes require a new token. Identical Block ID or text alone does not prove comparability. Preserve old snapshot text/token; absent criteria remain unclassified and cannot support adaptive comparison. No compulsory taxonomy or detailed scoring rubric.

Offer optional **Return Soon**, a manually selected revisit date/interval, and explicit **Clear flag**, **Clear date** and **No scheduling** actions. Flag and date are independent; clearing either must be unambiguous. Declining scheduling must never create an implicit due date or silently replace a prior one. A completed practice session does not clear Return Soon automatically. Use local calendar dates for revisit opportunities; record the selected interval/base date and origin. Preserve manual versus accepted suggested scheduling, the offered suggestion and the final choice. LGE-1 exposes manual scheduling only; provenance supports LGE-2 later. No notification or background scheduler is introduced.

### Smallest proposed persistence contract

Two additive logical record types are sufficient for the initial design; table/field names are proposals to confirm during the separately assigned implementation:

1. **Placement learning observation:** one optional placement-keyed row, restrictive ownership through the existing placement. Store outcome, observation timestamp, first-attempt context, nullable criterion token/text snapshot and optional prior evidence identity used as a comparison anchor. Existing sourceBlockId/session/outcome/Goal data are resolved rather than duplicated. Saved criterion identity/text is immutable for the observation. An explicit outcome/context correction preserves identity and a correction timestamp, invalidates affected derived comparisons, and cannot silently revise criteria on other placements.
2. **Block guidance event:** small append-only Block-keyed events for explicit flag/date changes, including clearing/declining and accepted suggested versus manual choices. Each event records the resulting flag/date state, recorded time, an ordered per-Block revision, chosen interval/base date, and suggestion provenance when applicable (policy, reason and the evidence identities/outcomes/delay used when accepted). Later observation corrections do not rewrite an accepted scheduling decision. Latest revision derives current state; no duplicate mutable scheduling store or recommendation cache. Criteria reuse is resolved from saved observation snapshots, without a separate criteria catalog. If implementation finds an unavoidable extra persisted concept, justify it before widening scope.

History of flag changes remains available, as required by the earlier insights proposal. Use transactional writes and idempotent actions to avoid duplicate observations/events on rapid taps, retries or recreation. Block archive leaves all records intact. Restrictive event references must participate in dependency counts and atomic safe-delete checks; guidance history cannot be silently cascade-deleted merely because a flag is cleared. Use Archive when history prevents deletion. Session/placement deletion and empty-target restore checks must account for the new ownership before delivery.

Expect one additive Room migration and one complete ZIP writer/records update in LGE-1, including the two record types, counts, bounded values, valid ownership, criterion/anchor references, schedule origin and event revision validation. **If baseline remains unchanged, the expected versions are Room25/ZIP9; these are proposals, not reserved or shipped versions.** Choose actual next versions from live state at implementation. Retain every existing schema/migration and ZIP1–8/JSON1–7 restore reader. Old data restores with no fabricated evidence, criteria, flags or dates. Keep JSON7 unchanged and document its exclusion of LGE data; complete ZIP is the recovery path. Preserve media inventory/bytes, lock order, atomic database witness, rollback and startup recovery. No media-format or ownership redesign.

## LGE-2 — explainable adaptive suggestions

Depends on delivered LGE-1 and a separate explicit assignment. Consume its evidence and existing Block, Goal, weekly and G3 history; do not create new time accounting. An eligible delayed-return comparison needs the same confirmed criterion token, a known earlier comparable practice endpoint, a later first-attempt observation before rehearsing that task, and a known elapsed delay meeting the policy's documented minimum. Unknown timestamps, intervening unlocated practice, changed difficulty or ambiguous first-attempt context mean insufficient evidence. A repeated placement in one session or an immediate/early successful attempt is not proof of durable retention. External practice cannot be inferred; explanations must acknowledge that observations are musician-reported.

Use a small versioned provisional policy with explicit minimum delay, bounded interval range, deterministic tie-breaking and conservative missing-data handling. Confirm its numeric constants in the LGE-2 assignment; they are heuristic product choices, not validated forgetting-curve parameters. Comparable successful delayed returns may lengthen the proposed interval; comparable partial/unsuccessful returns may shorten it. Confidence alone never changes intervals. Not assessed, skipped/unfinished practice, early success and incomparable observations cannot advance the interval; insufficient evidence leaves a conservative optional/manual choice. Do not fit personalized forgetting curves. Existing manual dates remain authoritative until the musician explicitly accepts a replacement.

Suggestion precedence is fixed:

1. Explicit user-pinned Return Soon selections.
2. Due revisit opportunities.
3. Recent comparable delayed-retention difficulties.
4. Underattended active Goals, derived from trustworthy committed G3 activity.

Deduplicate by Block ID without multiplying minutes; combine reasons and use stable ordering within a tier. Define the recency window and Goal-attention rule explicitly during LGE-2, disclose unknown/unattributed time, and avoid performance rankings. Archived Blocks are not actionable suggestions; completed Goals do not count as active. No Goal Blocks remain selectable and eligible for the first three tiers. Each suggestion states its concrete reason and, for an interval, the relevant observation/delay/criterion, policy version, uncertainty and proposed date. Explain absence of comparable evidence rather than inventing a score.

Surface suggestions in existing Practice Now selection and This Week preparation, with optional explicit Add/select actions. Never change Goals or priorities, add Blocks to This Week, start sessions, schedule by default or create overdue obligations. No penalties, streaks or mastery percentage. Recompute locally on demand from persisted facts; no scheduler, cloud or generative engine. Prefer no new schema/ZIP change for LGE-2 if LGE-1 provenance suffices; any unavoidable change requires an explicit rationale and its focused preservation checks.

## Future acceptance and verification boundaries

These are requirements for later assignments, **not checks run by this planning task**:

| Increment | Focused acceptance |
|---|---|
| LGE-1 | Missing/Not assessed/exploration; independent repeated placements; criteria reuse/change without rewriting history; security separate from observation; skipped check-in/practice and unfinished recovery; explicit scheduling/clear/decline and provenance; process death, reopen and archive; focused additive migration and complete ZIP round-trip/legacy default preservation; new ownership/deletion guards; unchanged G3 totals/Goals and transport/media/source-local state. |
| LGE-2 | Deterministic precedence/ties/deduplication; early versus delayed returns; comparable versus changed criteria; ambiguous/intervening practice; success/partial/failure bounds; confidence-only changes have no interval effect; manual date overrides; explainable missing-data behavior; No Goal/exploration/manual choice; no implicit plan/session/Goal mutations. |

Use tiny deterministic fixtures and only the specific new migration/restore/UI checks needed to establish these behaviors. No full historical compatibility campaign or device matrix per increment. Ordinary implementation delivery follows existing signed-development rules. Stage4 and this architecture task run no automated suites; comprehensive cross-feature, historical migration/backup and accessibility regression remains deferred until the owner explicitly authorizes F1 under the [verification policy](VERIFICATION_POLICY.md).

G3 remains complete and owns time/Goal attribution. A later separately scoped UI change may display recorded return attempts/outcomes/confidence factually; this plan neither reopens G3 nor promises a new dashboard. Preserve manual Goal progress. Playground is separate. Advanced predictive models, season-management entities, prerequisite graphs, automated skill diagnosis, teacher/student infrastructure and AI coaching remain deferred/out of scope.
