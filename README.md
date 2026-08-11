# Needs, Not Necessities

`needs_not_necessities` is a NeoForge 1.21.1 survival framework focused on preparation, readable consequences, and low-maintenance play. Its Java package root is `com.cappleapple.needsnotnecessities`.

Hunger fully replaces the vanilla hunger tick/HUD when enabled; thirst, rest, comfort, Active Meals, passive regeneration, base health, notifications, Quality Food compatibility, the inventory panel, commands, and integration hooks remain independently toggleable. Farmer's Delight Nourishment pauses the custom hunger countdown by default, vanilla Instant Health and Regeneration healing scale against player max health, and recipe ingredient bonuses combine numerically into the resulting Active Meal. Prepared-food ingredients recursively inherit their own recipe contributions at any depth with cycle protection. Different datapack food groups stack fully, while repeated ingredients from one group use a configurable geometric diminishing factor (50% by default).

The client requires [Panels Not Screens](https://github.com/CappleApple/panelsnotscreens) 0.1.0 or newer. Its draggable carrot handle opens, collapses, docks, and moves the titleless inventory status panel; both handle position and panel state persist locally. Hovering a Hunger, Thirst, or Rest row shows every modifier supplied by that row's current datapack state. The handle accepts either a GUI sprite or an item/block texture resource location through `inventory_overlay.panel_icon_sprite`; it defaults to `minecraft:item/carrot`. The exact 0.1.0 companion JAR used by this checkout is kept in `libs/`.

Comfort classifications are generated from registered block IDs at datapack reload time and cached for runtime scans. A separately generated JSON server config supplies tunable regex groups for beds, chairs, benches, sofas, tables, lighting, and hearths; `{}` disables automatic matching entirely. Explicit datapack block/tag sources always suppress regex matches for the same block, so pack authors retain final control. Active Meal defaults rely on common food tags rather than ingredient-specific Baked Potato or Cooked Rice overrides.

## Development

Requirements:

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.244 or newer in the 21.1 line

Build with:

```powershell
.\gradlew.bat build
```

The versioned mod JAR is written to `build/libs/needsnotnecessities-<version>.jar`.

Useful operator commands are rooted at `/needs_not_necessities` with `/nnn` as a short alias. See [docs/TESTING.md](docs/TESTING.md) for the multiplayer and compatibility test pass.

## Data-driven state definitions

State timelines live under `data/<namespace>/survival_states/*.json`. Per-state notification outputs, food tooltip groupings, comfort, and item/tag meal effects are also reloadable datapack JSON. Built-in definitions are examples and can be replaced by a datapack. See [docs/DATAPACK_FORMATS.md](docs/DATAPACK_FORMATS.md), including the configurable `#c:food/meat` example.

## License

Needs, Not Necessities is available under the MIT License. NeoForge template and bundled companion-library notices are retained in `TEMPLATE_LICENSE.txt` and `THIRD_PARTY_LICENSES/`.
