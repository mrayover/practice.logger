# P4 — Generalized Theme Architecture Readiness Audit

Date: 2026-10-31 (America/Los_Angeles). Audited checkout: `C:\Users\mrove\Projects\practice.logger`, clean `main` at `6f82088392f36a8bbf93a8c2c1e9982e84c05596`, matching GitHub main before this document. App: 0.16.0/build53; APK source is the earlier `15af3ea7f49bb0ab565b3ccd1addd3e16c99d125`. Existing [focused CI](https://github.com/mrayover/practice.logger/actions/runs/41235843607) is completed/success; it is baseline evidence, not theme verification.

## Decision and scope

**Ready for a small, incremental centralization; not ready for a complete palette switch today.** Compose Material 3 already supplies semantic color roles and the app has one root theme. Most standard controls inherit it. Direct palette references, platform styling and a few locally derived colors bypass that root. Adding only a dark `ColorScheme` would produce a partial theme.

The smallest practical architecture is the existing `PracticeTheme`, a complete Material `ColorScheme` per palette, one small immutable set of app-specific colors, and one root preference resolver. After a one-time migration of the identified call sites to semantic roles, another palette should require central definitions and registration, with no screen edits.

V1 theme scope for a later assigned implementation: preserve the existing Light appearance, add a deliberate Dark palette, and preferably offer System Default. Future palettes use the same contract; no palette editor, plugin registry, dependency upgrade, generalized design framework or screen redesign is needed. This audit implements none of those features.

The [V1 plan](../PRACTICE_LOGGER_V1_MASTER_PLAN.md), section 6, currently makes dark mode conditional; [CURRENT_STATE](../CURRENT_STATE.md) records it as deferred. The present assignment establishes the architecture recommendation and intended theme scope without claiming a shipped theme or changing release acceptance gates. P5, Library and G1 remain separate assignments.

## Current architecture and evidence

Kotlin paths below are relative to `app/src/main/java/com/mrayover/practicelogger/`. Line numbers refer to the audited revision.

| Location | Finding and implication |
|---|---|
| [Theme.kt](../app/src/main/java/com/mrayover/practicelogger/Theme.kt), lines 8–27 | Central Lato Regular/Bold typography and `PracticeTheme` exist. `PracticeCream = #F1E6D3`, `PracticeGreen = #315C47`; only `lightColorScheme` is installed. All surface/container variants are explicitly flattened to cream; primary/secondary/tertiary container content is green. Other roles, including secondary/tertiary base colors, error, outline, inverse and some foregrounds, inherit Material defaults. Semantic tokens therefore **already exist**, but are incomplete and inconsistently consumed. |
| [MainActivity.kt](../app/src/main/java/com/mrayover/practicelogger/MainActivity.kt), lines 40–42, 82–87 | One root `PracticeTheme { CatalogApp() }` is the correct injection point. Edge-to-edge setup hard-codes green status and cream navigation bars. A later `SideEffect` forces light status icons and green status color below API35. Both paths must follow the resolved palette. |
| [HomeScreens.kt](../app/src/main/java/com/mrayover/practicelogger/HomeScreens.kt), lines 25–77, 120–127 | Home background, headings, Goal labels/progress, borders, helper text and `HomeButton` directly reference palette constants. Banner repeats green/white plus literal `#244A37`. The disabled Edit text is intentionally green. These bypass semantic replacement and need role-specific treatment. |
| [PracticeScreens.kt](../app/src/main/java/com/mrayover/practicelogger/PracticeScreens.kt), lines 185, 200–201, 249 | Mostly Material roles, but transport background is `lerp(PracticeCream, PracticeGreen, .085f)` and divider uses primary at .22 alpha. Error text already uses `error`. The transport tint belongs in a central role. |
| [PracticePanels.kt](../app/src/main/java/com/mrayover/practicelogger/PracticePanels.kt), lines 25–38, 74, 121, 153 | Shared scroll panel derives track/thumb from primary at .16/.8 alpha; dividers repeat .12/.16. These are repeated styling recipes, not independent semantic colors. Centralize recipes/roles only where shared or contrast-sensitive; preserve geometry. |
| [ToolScreens.kt](../app/src/main/java/com/mrayover/practicelogger/ToolScreens.kt), lines 56, 96, 153–159 | BPM, tuner note and minute wheel use fixed green; unselected minutes use .5 alpha. Tuner state is also expressed in words, which should remain. |
| [MetronomeControls.kt](../app/src/main/java/com/mrayover/practicelogger/MetronomeControls.kt), lines 16–24, 57–58, 80–81 | Existing shared `metronomeButtonColors` already uses primary/container pairs for pulse. Compact selected meter/mode buttons use `primaryContainer`, which equals the ordinary cream surface in Light: the selection has no distinct fill there. Semantic selected state exists, but visual distinction needs review. |
| [RecordingSheet.kt](../app/src/main/java/com/mrayover/practicelogger/RecordingSheet.kt), lines 74–84, 240, 289–367 | Bottom sheet/dialogs inherit Material. Active recording/count-in text uses `error`; compact capture/playback reuse a local `green = colorScheme.primary`, with parallel filled/transparent state recipes. Review controls use default buttons. Palette-ready in part, but capture is not an error and should have a separately named role that can initially retain the same Light color. |
| [GoalScreens.kt](../app/src/main/java/com/mrayover/practicelogger/GoalScreens.kt), [GoalData.kt](../app/src/main/java/com/mrayover/practicelogger/GoalData.kt), lines 9–12, [HomeScreens.kt](../app/src/main/java/com/mrayover/practicelogger/HomeScreens.kt), lines 43–46 | Goals currently have ID, priority, completion and manual progress, **no persisted color identity**. Home bars are all green. G1's five identity colors and primary Goal caps are planned, not implemented. Do not confuse current list index/priority with identity. |
| [LogScreen.kt](../app/src/main/java/com/mrayover/practicelogger/LogScreen.kt), [RoutineScreens.kt](../app/src/main/java/com/mrayover/practicelogger/RoutineScreens.kt), [BackupScreen.kt](../app/src/main/java/com/mrayover/practicelogger/BackupScreen.kt), catalog/editors in MainActivity | Mostly standard Material text/buttons/fields, dialogs and date picker. Save/loading/failure messages often inherit ordinary text; several already have live-region semantics. They should inherit central schemes without new per-screen theme branches. Status wording and retry behavior must survive. |
| [ImageReferenceView.kt](../app/src/main/java/com/mrayover/practicelogger/ImageReferenceView.kt), lines 133–150; [ImageReferenceData.kt](../app/src/main/java/com/mrayover/practicelogger/ImageReferenceData.kt), line 150 | Viewer surround uses `surfaceVariant`; Reset uses an opaque Material `Surface`. PDF rasterization deliberately clears the bitmap to white. White paper is content, not an accidental light-theme surface. Images have no color filter. |
| [styles.xml](../app/src/main/res/values/styles.xml), lines 2–6; [AndroidManifest.xml](../app/src/main/AndroidManifest.xml), line 4 | Native window theme is `Theme.Material.Light.NoActionBar`, with duplicate cream background, green accent and light-status-bar flag. No `values-night` counterpart or theme preference exists. Startup/native fallback and Compose can disagree. Launcher artwork also has fixed brand colors; it need not follow the in-app palette. |
| [MetronomeButtonPulseTest.kt](../app/src/androidTest/java/com/mrayover/practicelogger/MetronomeButtonPulseTest.kt), lines 52, 67 | Existing pixel tests assert literal cream/green and the transport blend. Preserve their beat/stop assertions; later parameterize expected colors by resolved palette. No dedicated Light/Dark/System verification was found. |

There is no `isSystemInDarkTheme` resolver, dynamic-color integration, theme preference, custom semantic token provider or theme-specific screen branching in main source. Repeated button layouts and spacing are established UI conventions, not justification for a new component framework. `Color.Transparent`, white PDF paper and launcher colors must not be blindly replaced during a color search.

## Recommended semantic contract

Use Material's existing role names for ordinary UI, following [Android's Material 3 guidance](https://developer.android.com/develop/ui/compose/designsystems/material3). Keep raw hex colors private to palette definitions. Do not expose `cream`, `green`, `darkText` or screen-specific color names as the contract.

| Semantic role | Central definition and consumers |
|---|---|
| Page and content | `background/onBackground`, `surface/onSurface`, `surfaceVariant/onSurfaceVariant`, surface container levels. Home, tools, editors, lists and ordinary secondary text. Preserve Light's flattened cream surfaces initially; Dark may distinguish elevation where needed. |
| Actions | `primary/onPrimary`, `primaryContainer/onPrimaryContainer`, corresponding secondary/tertiary pairs, `outline/outlineVariant`. Standard buttons, fields, progress and borders. Explicitly define remaining used/default-component roles rather than accepting unrelated default accents. |
| Failures and inverse UI | `error/onError`, `errorContainer/onErrorContainer`, `inverseSurface/inverseOnSurface/inversePrimary`, `scrim`, `surfaceTint`. Validation, destructive emphasis where already appropriate, dialogs/sheets/date picker and overlay composition. A scrim is not a text background. |
| Banner | App extras: `banner`, `onBanner`, `bannerEdge`. Necessary because banner green/white and action primary need not be identical in Dark or another palette. |
| Practice transport | App extra: `transportSurface`; use `onSurface` for ordinary contents and `primary` for action labels, validating both on this background. Light initially matches the .085 blend. |
| Selection | App extras: `selectionContainer/onSelectionContainer` for custom compact meter/mode selections. Include a visible border/check or existing state label if fill alone is insufficient; keep Material `selected` semantics. Standard Material chips retain their normal scheme. |
| Recording | App extras: `recording/onRecording` for active capture text or fill; Light may alias current `error/onError` values to preserve appearance. Ordinary playback/start actions can keep primary pairs; recording failures keep error roles. Count-in/Recording/Playing/Stopped/Not saved remain explicit words. |
| Scroll affordance | App extras: `scrollThumb/scrollTrack`, used by the existing shared scroll panel. Derive once centrally if suitable, but check against each actual panel surface. Existing decorative dividers can use a shared `outlineVariant` recipe rather than gaining many new tokens. |
| Goal identity, reserved for G1 | Palette-owned five-entry `GoalColors` table, each with `accent`, `container`, `onContainer`, plus `unassignedGoalAccent`. Distinct from action primary, recording and error colors. No Goal migration or five-color UI is part of the theme-only implementation. |

One immutable `PracticeColors` value supplied through one `CompositionLocal` is sufficient for these extras. `PracticeTheme` provides it alongside `MaterialTheme`; consumers use one accessor such as `PracticeTheme.colors`. Keep palettes as static definitions in Theme.kt initially; split a palette file only if size warrants it. Retain `LatoTypography`, current shapes, spacing, touch sizes and component ownership.

Avoid independent status enums or a broad success/warning/info palette until an actual existing state requires those colors. Current saved/loading/completed/tuner labels can remain `onSurface` or `onSurfaceVariant`; failures use `error`. Never infer status from message strings merely to select a color. No new red/green-only encoding is needed.

### Root selection and persistence

- Define stable IDs `light`, `dark`, and `system`. Resolve `system` to Light or Dark from the current OS configuration; it is a preference, not a third color palette. Explicit Light/Dark override OS changes.
- Keep the resolved scheme/extras together in a small palette value. A single resolver maps the stored ID plus system configuration to that value. A later additional palette is another static value and resolver/selector entry; screens contain no palette IDs or `if (dark)` checks. Dynamic wallpaper colors are outside this scope.
- Reuse the small SharedPreferences approach demonstrated by [AudioPreferences.kt](../app/src/main/java/com/mrayover/practicelogger/AudioPreferences.kt), with a separate UI preference and root observer. Do not involve Room, change JSON7 or add a settings framework. On write failure retain the last committed selection and show a retryable error. Unknown IDs safely fall back to Light. Preserve Light for existing installs with no preference; selecting System is explicit.
- Expose Light/Dark/System through one small Appearance group in the existing Tools settings route. Its current Audio Settings label can become Settings with distinct Appearance and Audio groups; retain every existing audio option. No new Home gear or editor is required.
- Observe configuration changes at the root without keying/rebuilding `CatalogApp`, view models, recording sheet, viewer or audio effects by palette. A palette preference change should only recompose colors. System configuration may recreate the Activity under existing Android behavior; preserve its established foreground safety and saved state, and never auto-resume audio.
- Derive system-bar backgrounds and icon brightness from actual banner/navigation surfaces, not simply `dark = true`. Consolidate MainActivity's two competing paths. Audit API26 and API35 edge-to-edge behavior, keyboard/navigation insets and initial window background. Add aligned native day/night fallback resources during implementation; honor explicit override early enough to avoid a bright startup flash. Keep native fallback values documented against Compose definitions rather than constructing a cross-language token generator.

### Goal identity boundary

G1 should persist a stable color key associated with Goal ID, never an index in the current priority-sorted list or a raw palette RGB value. Each palette resolves that key to accessible hues with consistent identity. Reorder, completion and reopening retain the key; completed/historical Goals retain their identity. A later G1 assignment must define deterministic allocation/reuse among five active slots and preserve legacy relationships/backup compatibility. A hash alone does not guarantee five distinct active colors.

Neutral/unassigned caps need a contrasting charcoal-like Light value and lighter Dark value. Completed Goal does not mean unassigned. Always show Goal name; do not make colored text or a narrow cap the sole identifier. Do not add primary Goal selection, migrations, caps or historical attribution as a prerequisite for centralizing themes.

## Contrast, controls, overlays and media

Use [WCAG text contrast](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html) as the engineering target: at least 4.5:1 for normal text and 3:1 for qualifying large text. Use [non-text contrast](https://www.w3.org/WAI/WCAG22/Understanding/non-text-contrast.html) of at least 3:1 for visual information needed to identify active controls/states against adjacent colors. These are pair-specific checks, not a claim that every decorative divider or disabled control must pass the same threshold.

Static sRGB calculations from current constants, compositing alpha over plain cream before calculating luminance:

| Current pair | Approximate ratio | Finding |
|---|---:|---|
| Green on cream | 6.18:1 | Good ordinary-text baseline. |
| White on green | 7.63:1 | Good filled-button/banner baseline. |
| Green at .5 alpha over cream | 2.21:1 | Minute-wheel unselected values fall below even the large-text target; use a readable secondary foreground. |
| Green at .18 alpha over cream | 1.30:1 | Home progress track is faint; evaluate filled progress versus track and surrounding surface, and retain the numeric percentage. |
| Green at .16 alpha over cream | 1.26:1 | Scroll track/dividers are faint. Essential scroll affordances need a visible thumb; decorative separators may remain subtle. |

These calculations do not measure screenshots, Material defaults, all rendered combinations or dark-theme accessibility. Validate actual foreground/background pairs, including alpha composition, transport tint, disabled/pressed/focused/selected states and sheet surfaces. Keep disabled states discernible even where contrast exceptions apply. Do not assume a lighter primary automatically works on both page and filled-button backgrounds.

For metronome pulse, retain the existing sample-consumed beat event and 90ms decay; only centralize colors. Check lit/unlit text contrast in both filled Tools and outlined Focused Practice controls. Preserve the full-button pulse, visible Stop, Sound/Flash modes and state descriptions. Do not add an animation timer or change flash timing to implement theming.

For recordings, preserve pinned active Stop/Cancel and explicit Keep/Save, Preview, Re-record and Discard. Palette changes must not call enter/leave, stop/restart capture, initiate playback, save/discard a temporary take, or change the accepted build52 layout. Retain large-text wrapping, aligned actions, safe/keyboard insets and exclusive panels. Error/recording colors are distinct semantic roles even if Light aliases their RGB values.

Dialogs, bottom sheets, menus and date picker should inherit the same resolved Material scheme; verify foreground pairs, scrim, drag handle and tonal surfaces rather than assigning each overlay a separate theme. Reset controls over references should retain an opaque themed surface, so readability does not depend on arbitrary imported pixels. Android permission dialogs, document pickers and share sheets are platform-controlled; the app cannot promise to recolor all external UI.

Imported image pixels, transparent-image content, saved orientation, PDF white paper/ink and source files must remain faithful: no automatic inversion, tint, bitmap rewriting or palette-triggered re-import. Theme only the viewer surround and app-owned controls. A white page in Dark is expected; transparent images need review on the chosen surround. Preserve viewer instance/page/zoom/pan and existing file/cache ownership across palette recomposition; do not rerender PDF pages solely because app chrome changes.

## Minimal implementation sequence for a later assignment

1. **Capture Light baseline and define roles.** Inventory the above call sites and current default Material roles, including disabled controls and overlays. Record accepted Home/Focused Practice/recording appearance. Resolve the compact selection and minute-wheel contrast issues deliberately. Do not redesign layout.
2. **Centralize while still Light-only.** Introduce complete Light scheme plus the small extras value/provider. Replace direct palette use in Home, banner, Tools, transport and system bars; centralize scroll/state recipes in existing helpers. Preserve behavior and compare Light against the baseline. Leave media/launcher constants intact.
3. **Define Dark using the identical contract.** Specify all used Material roles and extras, with paired foregrounds. Keep Goal slots separate and unused until G1. Validate contrast and all control states; do not ship a partially themed app.
4. **Add root preference and System Default.** Persist one stable selection, resolve OS configuration centrally, connect the small settings group and system bars/native fallbacks. Keep existing-install Light default and save-failure recovery. Ensure no palette-keyed lifecycle effects or navigation changes.
5. **Run bounded theme acceptance.** Add token-pair contrast checks and root preference tests (explicit override, OS-following, unknown ID, restart/save failure). Adapt existing pulse pixel expectations without weakening beat/stop assertions. Run a focused UI set covering Home/Goals, editor/form validation, Focused Practice/Notes, free/loop capture and review, Tools tuner/timer/metronome, Log/date picker, destructive/exit dialogs and reference Reset. Cover Light/Dark, System changes, API26/API35 system chrome, 200% text and landscape. Exercise a preference change during capture/preview and confirm no theme-induced operation, navigation or state loss; ordinary background safety still applies. No need to rerun unrelated storage/audio matrices for each color edit.

Completion criterion: the same screens and controls consume semantic roles in every supported palette; a sample future palette can be defined centrally without screen modifications. New palettes must still pass contrast and focused visual checks. Release builds and broader P5/final gates belong to their separately authorized implementation/release phases.

## Audit verification and limits

This assignment performed source/resource/test inspection, repository/remote/CI provenance checks, static contrast calculations and document link/whitespace checks. No screens, themes, resources, dependencies, data models or audio behavior were changed. No Gradle tests, full regression, emulator run, APK build or theme editor was introduced. Existing owner phone acceptance remains pending as recorded in CURRENT_STATE. The only accompanying maintenance changes record this audit in CURRENT_STATE and WORK_LOG.
