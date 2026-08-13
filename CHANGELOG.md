# Changelog

## 1.1.1 - 2026-08-13

This patch makes the inventory status panel safer to move and corrects its handle icon rendering.

### Fixed

- Moving or toggling the Needs, Not Necessities panel no longer crashes the client when another process temporarily locks `needs_not_necessities-client.toml` and NeoForge cannot atomically replace it. The failure is logged, the panel remains usable for the current session, and a later successful save can persist its state.
- Direct item and block textures on the draggable handle now render as the complete, uncropped 16x16 icon. The previous draw path sampled only a 14x14 region of a 16x16 texture, which made the icon appear zoomed in.
- Configurable GUI sprites and direct texture resource locations remain supported through `inventory_overlay.panel_icon_sprite`.

### Compatibility and upgrading

- Replace the 1.1 JAR with `needsnotnecessities-1.1.1.jar` on Minecraft 1.21.1 NeoForge installations.
- Panels Not Screens 0.1.0 or newer remains required on the client.
- Existing panel positions, client/server configuration, datapacks, and saved survival data remain compatible. This release does not change the public API or any data format.

### Verification

- Clean offline build completed successfully with 57 automated tests and no failures.
- The packaged mod metadata reports version 1.1.1.

## 1.1 - 2026-08-11

Initial public release of the configurable Needs, Not Necessities survival framework for Minecraft 1.21.1 NeoForge.

### Added

- Independently configurable Hunger, Thirst, Rest, Comfort, Active Meal, passive-regeneration, base-health, notification, and compatibility modules.
- Data-driven survival state timelines, modifiers, comfort sources/effects, food tooltip groups, meal effects, and notification outputs.
- Configurable biological time, death penalties, health scaling, sleep rules, comfort retention, meal scoring, and same-group diminishing returns.
- A draggable Panels Not Screens inventory overlay with persistent position, docking side, expanded state, and configurable handle icon.
- Automatic comfort classification with JSON-ordered first-match rules while preserving explicit datapack block/tag precedence.
- Farmer's Delight and Quality Food compatibility, operator commands, networking, persistence, and public integration hooks.
