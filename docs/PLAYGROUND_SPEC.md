# Playground — Home Foundation

Open **Log → Playground** to enter the portrait Home. The fixed page keeps the practice.logger banner, Journal return, Block inventory, Home navigation, room, Explore / Battle and controls together inside the window's safe area. Stats, Workshop, Kitchen, Explore / Battle and B are inactive.

Hold an arm of the connected D-pad to walk around the tiled room. Hold between adjacent arms to move diagonally, or slide the same thumb to change direction. The center has an 8 dp neutral radius, and a 5° margin keeps the current direction steady near sector boundaries. Multiple touches combine on each axis, and opposing directions cancel on that axis. Release, gesture cancellation, window focus loss or leaving the foreground clears held input. Movement uses elapsed frame time and matches speed in all eight directions. The character slides along either wall and stops when both axes are blocked.

Batel stands in the `idle` pose and loops the two `walk` poses at 100 ms each while changing position. Releasing the D-pad or reaching a fully blocked position returns Batel to idle. All eight directions share the original orientation. The Canvas uses nearest-neighbor scaling and aligns the lowest opaque row of each pose to the bottom of the 12 × 16 logical collision rectangle. To replace the art, export a transparent PNG and untrimmed, unrotated Aseprite JSON Hash with forward `idle` and `walk` tags to `app/src/main/assets/playground/`.

The circular A button sits above and to the right of B. A sends an interaction input, and the room currently has no interactions. The controls use the original row height. The renderer fits the 224 × 256 logical room proportionally inside the existing viewport with a 1 dp minimum inset and clearance at rounded corners. Extra space from the aspect ratio is centered. The player position is visit-only.

The original `FloorTile1.png` (30 × 30) and `Wall1.png` (45 × 45) repeat in their respective 16-unit grid cells. `Door1.png` (45 × 45) fits as a 24-unit square centered inside the existing 24 × 28 top-wall doorway. The door is decorative and the top wall remains impassable. The Canvas uses nearest-neighbor sampling and snaps shared tile edges to the same screen pixels. The three square PNGs in `app/src/main/assets/playground/` can be replaced individually. Movement and collision use the logical room geometry.

The inventory groups authoritative `block_completions` rows by their saved color. Every row contributes one available Block. Null colors count as Neutral. Any saved colors outside the five original palette entries are included in the total and appear under Other saved colors. Counts update as completion rows change. The dialog overlays the fixed Home bounds and closes with Close, Android Back or a tap outside.

Room 29, Complete ZIP 14 and JSON 7 retain their existing storage and recovery contracts. The Home screen reads earned counts and leaves prior arrangement coordinates untouched. It does not generate layouts for new completions. The legacy arrangement and backup details below describe the preserved data and previous presentation. [Build and verification](../CURRENT_STATE.md).

---

## Historical PG1 / PG1.1 contract

Owner-approved November 12, 2026. This supersedes the earlier slit presentation and milestone thresholds. PG1 is a freely arranged celebration of completed practice, reached through Log → Playground, with Journal preserved.

### Earned history

One persisted block_completions snapshot earns one solid-color square, keyed by placementId. Repeated reusable Blocks in different completed placements earn separate squares. Skipped, prepared and unfinished placements earn none. Retry, reopen and restart never issue duplicates. Existing historical completion snapshots appear automatically; older placements without authoritative completion evidence do not invent rewards. No separate reward ledger or counter exists.

The entire visible square uses completion.colorArgb. Missing trustworthy color and explicit No Goal use the theme's neutral outline color as a solid fill (Light surfaceVariant equals the canvas and is unsuitable). Never derive historical color/priority from a current Goal, association or first-accrual snapshot. Goal ID/priority, original placement name/action, committed duration, date, outcome, session ID, saved notes and reflection answers are read from existing history; unavailable details are not fabricated. Goal titles were not captured, so the panel identifies the saved Goal ID without claiming a current title is historical.

### World and arrangement

Every square is exactly one world unit on each side, with arbitrary angle and no printing, title, number, icon, duration, Goal label or badge. Selection adds only a stronger, static four-layer external glow, retaining the historical fill and actual geometric bounds. A faint neutral dotted background provides world-space reference, with viewport/world-bounded drawing and power-of-two dot thinning only at extreme zoom-out. No connecting lines, labels, snapping or packing. Empty space is intentional.

The centered rectangular world has fixed 3:2 aspect ratio and area capacity/0.15 square units: capacity20 through20 completions,40 through40,80 through80,160 through160,320 through320, continuing powers of two within archive record limits. Width and height each grow by sqrt(capacity multiplier). Origin, existing x/y/angle and active camera remain unchanged on expansion. Rotated boundary clearance is enforced.

Missing layouts are assigned in completedAt/placementId order using deterministic base2/base3 low-discrepancy candidates, at most8192 per square, with0.25 world-unit separation. Existing rows are never rearranged. A placement failure is recoverable with Retry; no overlap or extra unsupported capacity is forced. Candidate order is stable for the current world; already saved positions remain authoritative as counts grow.

### Interaction and collision

Tap selects and opens compact details; tapping empty canvas clears selection. Unselected squares never move, rotate, push, stack or animate in response to another square.

One-finger drag initiated on the selected square or empty canvas while selected moves only that square by relative viewport-transformed deltas, without jumping to the touch origin. Empty taps wait for touch slop and still deselect. A two-finger gesture whose first touch starts on the selected square owns rotation around that square's center; subsequent pinch changes never resize it. Two fingers initiated on empty canvas own camera zoom and pan. One-finger empty-space drag pans when nothing is selected. Touching an unselected square selects on tap; dragging it does not move it. Gesture ownership resolves when drag slop is crossed or a second pointer begins, and stays fixed until all pointers lift. A background pinch beginning before a drag owns camera navigation even while selected; adding a second finger after a background drag starts does not transfer ownership or rotate the square. Interrupted gestures roll back the preview; a valid completed gesture saves through a transaction. Failed saves retain the prior arrangement and show a retry message.

Oriented-square separating axes provide actual geometry, hit testing and boundary clearance. Swept SAT limits translation at the earliest contact even for a large drag. Rotation uses conservative advancement bounded by the maximum vertex travel along the requested arc, with a bounded2048 iterations and numerical-scale directional probes at contact; an update cannot rotate through an obstacle to a clear endpoint. Numerical penetration tolerance is1e-9 world unit; contact is allowed. Collision does not depend on zoom. No physics, velocity, friction, bouncing, inertia or spatial index.

Pinch preserves the world point at its previous centroid and applies centroid pan. Minimum zoom is a90%-fit overview; maximum is at least12 times overview or160 pixels/world unit. Initial camera scale is 1.2 times the full-world overview, making normal squares20% larger without altering one-unit geometry. Overview resets to the90%-fit whole-world camera. The camera and selection are visit-only. Panel expansion changes available viewport size without changing world geometry or camera transform.

45° and Upright controls set absolute45°/0° through existing conservative rotation validation along the shortest arc. A blocked arc or boundary leaves the prior orientation and position intact and reports that more space is needed. Clear presets save exact angles through the existing repository; free twist remains. No persistence changes.

### Information panel

Compact read-only panel shows original Block name, completion date/time, actual duration and saved Goal identity/priority. More reveals original action/reference notes, completion/session identity, existing placement and timestamped notes, security/confidence and saved focus/difficulty/progress/revisit answers. No editing, Block management, reclassification or collectible deletion. The panel scrolls and uses existing theme/Lato typography; expansion is explicit. No forced expansion.

### Persistence and portable recovery

Room27 adds only playground_layouts(placementId TEXT primary key, x REAL, y REAL, rotationDegrees REAL), with foreign key to block_completions and ON DELETE CASCADE. This follows the existing completion/placement ownership chain, adds no new deletion action and does not weaken protected Block/session deletion. No earned facts are duplicated. Schemas1–26 and explicit migrations are retained.

Repository operations are idempotent and transactional. Unique identity, finite numbers, bounded world coordinates, bounded angle−360≤angle≤360 (interaction saves normalize to0≤angle<360), boundary clearance and pairwise nonintersection are checked. Malformed data is rejected safely, preserving saved records rather than silently moving them. New completions create layouts on first Playground load. Valid committed edits survive recreation/reopen/process death.

Complete ZIP12 adds the same four layout fields with finite double wire values. Frozen ZIP1–11 readers remain. ZIP12 validates unique keys, completion references, world bounds and collision-free arrangements before transactional restore; exact values are retained. Older archives restore their own completion facts normally and derive defaults when Playground opens. Legacy JSON7/readers1–7 remain unchanged and exclude completion snapshots/layouts. Existing empty-target, file ownership, private staging/journal, atomic witness, rollback and startup recovery safeguards remain. No media files or ownership change.

### Verification and boundary

PG1 needs small deterministic geometry/data/migration/archive checks and one focused Android workflow for essential interactions and recreation. Actual results and signed provenance are in [CURRENT_STATE](../CURRENT_STATE.md). Physical acceptance is separate. Comprehensive regression and historical device matrices wait for explicit F1 assignment.

PG1 excludes earning/expansion/collision animation, physics/pushing, snapping/line grids, artistic auto-arrangement, templates, paint-by-numbers, shared artwork, artwork backgrounds, milestone celebrations, special shapes/sizes, scores/mastery, AI guidance, reward accounts, extra databases/services or template infrastructure. Stop after PG1.1; PG2 and F1 require separate assignments.
