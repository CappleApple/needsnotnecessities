# Needs, Not Necessities

Needs, Not Necessities is a configurable survival framework for NeoForge 1.21.1.

It adds survival systems that matter enough to prepare for, but are not meant to turn the game into constant meter babysitting. Hunger, thirst, rest, comfort, meals, regeneration, health tuning, notifications, and the inventory status panel can all be enabled or disabled separately.

The mod is designed mainly for modpacks, so most of the important behavior is data-driven rather than hard-coded around one balance style.

## Main systems

- Custom hunger progression
- Optional thirst
- Rest / sleep pressure
- Comfort from nearby blocks and furniture
- Active Meals and food-group bonuses
- Configurable passive regeneration
- Configurable base health
- HUD/inventory status display
- Datapack-defined states and effects
- Operator commands and integration hooks

You can use only the parts you want. Disabling one system does not require disabling the rest of the mod.

## Hunger and food

When the custom hunger system is enabled, it replaces vanilla hunger ticking and its HUD behavior.

Food effects are driven by datapack definitions. Meals can inherit contributions from their ingredients, including prepared ingredients that themselves come from recipes. Repeated ingredients within the same configured group can use diminishing returns, while separate groups can stack normally.

Placed foods count too. Eating cake or a Farmer's Delight pie applies the same hunger, thirst, and meal effects as held food. Slices and feast servings inherit the full food's recipe, including its prepared ingredients. Collecting or cutting a portion does not apply food effects until it is eaten.

Tooltips on placed foods show nutrition and meal effects per serving.

Farmer's Delight Nourishment is supported and, by default, pauses the custom hunger countdown while active.

Quality Food compatibility is also included.

## Thirst and rest

Thirst can be enabled independently from hunger.

Rest tracks how well-rested the player is over time. At low enough configured rest states, vanilla phantom spawning can resume around survival/adventure players. The normal phantom rules still apply, including darkness, sky access, local difficulty, `doInsomnia`, and `doMobSpawning`.

If the Rest system is disabled, vanilla insomnia behavior is left alone.

## Comfort

Comfort is based on nearby blocks such as beds, seating, tables, lights, and hearth-like blocks.

Pack authors can define comfort explicitly with datapacks. There is also an optional generated regex configuration for automatically classifying common furniture blocks by registry name.

Explicit datapack definitions always take priority over automatic matching.

Comfort scans can also see supported moving Sable sub-levels, which allows the same rules to work on vehicles without making Sable a required dependency.

## Inventory panel

The inventory status panel uses [Panels Not Screens](https://github.com/CappleApple/panelsnotscreens).

A draggable handle opens and moves the panel, and its position/state are saved locally. Hovering a Hunger, Thirst, or Rest row shows the modifiers currently affecting that state.

The panel icon can use either a GUI sprite or an item/block texture. The default is the vanilla carrot texture.

## Data-driven configuration

Survival state timelines live under:

```text
data/<namespace>/survival_states/*.json
```

Food groups, notifications, comfort definitions, meal effects, and related rules can also be supplied through datapacks.

Built-in definitions are intended as defaults and examples rather than something packs are forced to keep.

See [docs/DATAPACK_FORMATS.md](docs/DATAPACK_FORMATS.md) for the supported formats.

## Commands

Operator commands are available under:

```text
/needs_not_necessities
```

with the shorter alias:

```text
/nnn
```

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.244 or a compatible 21.1 build
- Java 21
- Panels Not Screens 0.1.0 or newer on the client

## Building from source

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

The built jar is written to `build/libs/`.

Release changes are tracked in [CHANGELOG.md](CHANGELOG.md), and multiplayer/compatibility testing notes are in [docs/TESTING.md](docs/TESTING.md).

## License

Needs, Not Necessities is available under the MIT License. NeoForge template and bundled companion-library notices are retained in `TEMPLATE_LICENSE.txt` and `THIRD_PARTY_LICENSES/`.
