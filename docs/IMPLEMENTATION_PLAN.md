# Needs, Not Necessities implementation plan

## Repository inspection

The repository initially contained Git metadata only. There was no package structure, mod ID, NeoForge version, config or networking layer, player attachment pattern, or GUI code to retain. The project now uses the official NeoForge 1.21.1 ModDevGradle template with:

- mod ID: `needs_not_necessities` (NeoForge IDs cannot contain hyphens)
- display name: `Needs, Not Necessities`
- package root: `com.cappleapple.needsnotnecessities`
- Minecraft: 1.21.1
- NeoForge: 21.1.244
- Java: 21

## Architectural rules

- Each module has an independent server configuration flag. Disabled modules are excluded from state ticking, lifecycle resets, and modifier gathering.
- Biological time is derived only from active server/player ticks and the configured logical day length. It never reads celestial/world day time.
- Hunger, thirst, and rest are instances of one reloadable state-timeline engine. State counts and boundaries are not compiled into Java.
- Persistent state is stored in one versioned NeoForge player attachment. Clone/death behavior is handled explicitly because each module has its own reset policy.
- Attribute mutation has one owner. Modifier IDs are stable resource locations, stale modifiers are removed centrally, and health is clamped after max-health changes.
- Client behavior is isolated under `client` and invoked only by registered client-bound payload handlers and physical-client events.
- Server-authoritative data is synchronized through explicit versioned payloads.

## Phase status

### Phase 1 — foundation

- [x] Official project structure and metadata
- [x] Server and client configs
- [x] Independent module enable flags
- [x] Shared floating-point biological time conversion service
- [x] Versioned player survival attachment and NBT persistence
- [x] First-spawn, clone, dimension-return, logout, and configurable death-reset lifecycle
- [x] Generic reloadable state definitions and state timeline engine
- [x] Generic attribute/passive-regeneration modifier model
- [x] Central modifier recomputation with stable IDs and stale-modifier removal
- [x] Built-in hunger, thirst, and rest timeline data
- [x] Unit tests for time conversion and timeline boundaries

### Phase 2 — health and hunger

- [x] Base max-health ADD/MULTIPLY service
- [x] Passive health regeneration and combat cooldown
- [x] Complete vanilla hunger tick and HUD replacement without starvation damage
- [x] Food nutrition and saturation conversion to hunger-hours
- [x] Hunger ticking, state events, and modifier recomputation triggers
- [x] Vanilla Hunger status effect doubles custom hunger-timer drain by default
- [x] Datapack-stage-percentage eating gate, defaulting to the lowest 90% of Hunger stages
- [x] Max-health-proportional Instant Health and Regeneration with a configurable vanilla-health reference

### Phase 3 — thirst and rest

- [x] Food-driven thirst pressure
- [x] Drink and alcoholic-drink item tags
- [x] No natural thirst decay; configurable food-hours-to-thirst-hours ratio
- [x] Thirst-state participation in the shared modifier pipeline
- [x] Rest ticking and continuous partial bed recovery
- [x] Datapack-stage-percentage drinking gate, defaulting to the lowest 90% of Thirst stages
- [x] Configurable tiredness-gated daytime sleep and `playersSleepingPercentage` day/night skipping
- [x] Completed day/night skips maximize Rest for participating sleepers and immediately resynchronize modifiers

### Phase 4 — comfort

- [x] Reloadable comfort definitions for block IDs and tags
- [x] Periodic cached scans and diminishing returns by type
- [x] Retention behavior, threshold effects, and diagnostics

### Phase 5 — active meals

- [x] Recipe graph analyzer and cache invalidation
- [x] Item/item-tag datapack base effects with built-in common-tag defaults
- [x] Datapack hunger-hour tooltip groupings and synchronized meal-effect previews
- [x] Trait voting, score, duration, replacement rules, and persistent active-meal modifiers
- [x] Recursive prepared-food ingredient inheritance with cycle protection, nested alternative averaging, numeric cross-group stacking, and configurable geometric same-group returns shared by server activation and client tooltip prediction

### Phase 6 — optional integrations

- [x] Isolated Quality Food adapter verified against its 1.21.1 data-component source
- [x] Provider registries for other food and survival mods
- [x] Soft Farmer's Delight Nourishment integration that pauses the hunger countdown

### Phase 7 — client experience

- [x] Server-to-client snapshots
- [x] Panels Not Screens titleless collapsible inventory panel with a draggable, configurable-sprite opener/handle and client-side state storage
- [x] Current-tier modifier hover details, normal/advanced food tooltips, and per-state datapack notifications/cooldowns

### Phase 8 — operations and API

- [x] Admin/debug commands
- [x] Public mutation API, events, and provider interfaces
- [x] Conditional configurable post-death weakness/hunger/thirst message
- [x] Automated tests, dedicated-server smoke-test support, and final manual test matrix

## Data format direction

Simple scalar settings remain in NeoForge TOML configs. Arbitrary lists and structured state/modifier definitions use reloadable datapack JSON, avoiding fixed-length TOML tier arrays. Reloads build a complete validated snapshot before replacing the live registry; malformed data therefore fails the reload instead of partially mutating state.
