# C5C — Edit Pattern and Make Unique

Arrange Actions enables Edit Pattern and Make Unique when a clip is selected. Edit Pattern resolves stable track/pattern IDs and opens the existing Loop piano roll. A transient per-track target selects the referenced pattern without changing project identities, saved mixer choices or Undo history. Explicit Loop pattern choices resume the existing selection behavior. The mounted document/audio owner remains; no branded reentry or separate editor.

Make Unique reuses ComposerManagement.addPattern duplication: fresh pattern and note UUIDs, identical musical data/length and a bounded nonconflicting Variation name. One validated immutable snapshot adds the pattern and changes only the selected clip's pattern reference. Clip identity, track, start and all mixer settings remain. One Undo restores the original collection/reference. Capacity or validation failure commits nothing, including at the global note budget. A singleton clip can also be made unique.

Arrange zoom, both scroll axes, bar, Hand mode and selection are retained by the existing saveable fields in a visit-scoped state holder when switching modes. Returning displays current titles/durations. Shared note edits affect every reference; variations are independent. Existing collision refusal, legacy document reads, atomic checkpoints/saves, v1–v4 and Save As remain unchanged.

Structural Make Unique stops song output before committing. Next Play rebuilds its snapshot with the new reference. Loop Repeat's worker-owned live updates and measured one-bar fit are unchanged. Composer projects/custom presets remain outside practice backup until C7.

Focused verification and signed delivery are recorded in [CURRENT_STATE](../CURRENT_STATE.md). No exports, deferred C4E campaign, comprehensive regression or F1 are part of C5C. Stop after this increment; C6A needs a separate assignment.

Signed0.57.0/build152 is delivered. Eight focused host methods, release/vital lint and Android-test assembly pass. API35 workflow44017475358 passes24.868s; exact force-stop/cold reopen passes8.807s. Rendered view pixels match before/after direct editing when transport is unchanged. Initial44016454380's transport-dependent pixel comparison was corrected by test ordering only; application sources are identical. Original/unique/Loop/returned/cold captures inspected. See CURRENT_STATE for package hashes, source/test provenance, actual limitations and phone acceptance.
