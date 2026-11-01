# Smooth theme

Owner-assigned teal appearance test,0.17.0/build63. Shared-header Settings → Appearance → Smooth. Deep blue-green backgrounds and transport, aqua controls, muted teal selection/outline/scroll roles and matching native window/accent colors. Existing recording/error colors remain distinct; Goal slots retain their distinct category hues. Lato, geometry, selected/disabled/pressed states and sample-derived metronome flashes use the same shared components.

ThemeChoice persists stable `smooth` in existing appearance preferences. Explicit Smooth overrides device night mode; Light remains the fresh-install/unknown-ID fallback and System Default still selects Light/Dark. No database/backup migration, media transformation or audio-engine change.

Source changes: Theme.kt palette/resolver, ThemePreferences.kt selector entry, MainActivity.kt native style resolution, colors.xml/styles.xml mirrors and versionCode63. Appearance automatically enumerates the selector. Existing contrast/persistence tests include Smooth; a focused native test checks selection, recreation, rendered background and native accent. Existing practice pulse test includes Smooth. Dedicated manually dispatched smooth-theme.yml produces release/debug/test builds and focused evidence.

Delivery, exact verification and phone acceptance: [CURRENT_STATE](../CURRENT_STATE.md).
