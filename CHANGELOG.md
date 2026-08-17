# Changelog

## 1.1.2 - 2026-08-16

### Added

- The default Hunger, Thirst, and Rest datapack timelines now show a message and play a sound when their respective highest state is reached.
- The mod now has a custom icon in the in-game mods list.

### Changed

- Drinks can now be consumed at every Thirst stage; the `thirst.drink_below_stage_percentage` server config option has been removed.

## 1.1.1 - 2026-08-13

### Fixed

- Moving or toggling the Needs, Not Necessities panel no longer crashes the client when another process temporarily locks `needs_not_necessities-client.toml` and NeoForge cannot atomically replace it. The failure is logged, the panel remains usable for the current session, and a later successful save can persist its state.
- Direct item and block textures on the draggable handle now render as the complete, uncropped 16x16 icon. The previous draw path sampled only a 14x14 region of a 16x16 texture, which made the icon appear zoomed in.

## 1.1 - 2026-08-11

### Added

- Independently configurable Hunger, Thirst, Rest, Comfort, Active Meal, passive-regeneration, base-health, notification, and compatibility modules.
- Data-driven survival state timelines, modifiers, comfort sources/effects, food tooltip groups, meal effects, and notification outputs.
- Configurable biological time, death penalties, health scaling, sleep rules, comfort retention, meal scoring, and same-group diminishing returns.
- A draggable Panels Not Screens inventory overlay with persistent position, docking side, expanded state, and configurable handle icon.
- Automatic comfort classification with JSON-ordered first-match rules while preserving explicit datapack block/tag precedence.
- Farmer's Delight and Quality Food compatibility, operator commands, networking, persistence, and public integration hooks.
