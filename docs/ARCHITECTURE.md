# Architecture notes

This document describes the main design choices in Needs, Not Necessities. It is intended for maintainers and integrations; datapack authors should use [DATAPACK_FORMATS.md](DATAPACK_FORMATS.md) instead.

## Project structure

- Mod ID: `needs_not_necessities`
- Java package: `com.cappleapple.needsnotnecessities`
- Minecraft: 1.21.1
- NeoForge: 21.1.x
- Java: 21

The mod is split into independent survival modules rather than one hard-wired survival loop. Hunger, Thirst, Rest, Comfort, Active Meals, passive regeneration, base health, notifications, and optional integrations can be enabled or disabled separately.

## Survival state timelines

Hunger, Thirst, and Rest use the same reloadable timeline system. The number of states, their boundaries, names, modifiers, and notifications come from datapacks instead of being compiled into Java.

Time advances from server/player ticks and the configured logical day length. It does not depend on the world's sun/moon time, which keeps the mechanics stable in dimensions with unusual or fixed celestial time.

State changes feed one central modifier recomputation path. Modifier IDs are stable resource locations, and stale modifiers are removed when a definition or module changes.

## Player data

Persistent survival state lives in a versioned NeoForge player attachment.

Death/clone, dimension return, first spawn, logout, and reset behavior are handled deliberately because different modules can have different reset policies. Max-health changes are followed by a health clamp so removing a health bonus cannot leave the entity above its new maximum.

## Hunger

When the Hunger module is enabled, it owns the hunger timer and HUD instead of running a second system beside vanilla hunger. Food nutrition/saturation is converted into configured hunger time, while normal food use rules can still be overridden by `can_always_eat` items and the stage-percentage eating gate.

Held-food finishes and placed-food bites feed the same consumption service. Server-side block interactions track actual nutrition calls, so a rejected bite, candle placement, or collected serving does not count as eating. Nutrition and saturation use the values supplied by the food rather than the change in vanilla hunger, which may already be full.

Vanilla Hunger status can accelerate the custom timer. Farmer's Delight Nourishment can pause it through the optional compatibility hook.

## Thirst

Thirst is intentionally driven by food/drink choices rather than passive time decay. Foods can add thirst pressure based on their hunger value, while drink/alcohol tags apply their configured adjustments.

Because Thirst uses the shared timeline engine, its state modifiers and notifications work the same way as Hunger and Rest.

## Rest and sleep

Rest recovers while a player sleeps and can optionally control whether sufficiently rested players may sleep during the day. Completed sleep skips restore participating sleepers to the best configured Rest state.

Phantom integration uses the configured Rest stages without rewriting the vanilla insomnia statistic. When Rest is disabled, normal vanilla insomnia behavior is left alone.

## Comfort

Comfort sources can come from explicit datapack block IDs/tags or from the generated auto-classification config. Runtime scans use cached classifications and apply diminishing returns by comfort type.

Explicit datapack classification wins over regex auto-classification for the same block.

When Sable/Create Aeronautics support is available, the scan can include comfort blocks on moving/rotated sub-levels without making Sable a hard dependency.

## Active Meals

Active Meals analyze recipe ingredients recursively and turn them into temporary traits/modifiers.

The analyzer caches recipe results and protects against recipe cycles. Recursion includes cake, pie, and feast items even when they cannot be eaten while held. Different food groups can stack numerically; repeated ingredients from one group use the configured diminishing factor.

Farmer's Delight serving items without a native recipe receive an analysis-only link back to the food block that supplies them. The block's serving methods provide that relationship, including state-dependent servings and subclasses. Existing recipes take priority, so a food that can also be arranged into a platter keeps its normal ingredients.

Server activation and client tooltip prediction share the recipe index and combination rules. Recipe reloads invalidate the caches, and previews for placed foods use the matching bite or serving. Additional non-edible recipe intermediates can be marked with the `needs_not_necessities:placed_foods` item tag.

## Health scaling

Base-health changes, passive regeneration, and the optional max-health scaling for Instant Health / Regeneration / Absorption are owned by dedicated services instead of being scattered through each module.

This also keeps direct attribute changes and post-death health restoration from compounding across reloads or player clones.

## Client/server split

Gameplay state is server-owned. Clients receive explicit snapshots for the inventory panel, tooltips, and notifications.

Client-only UI code is isolated under the client package. The inventory status panel uses Panels Not Screens, while panel position/state stays client-local.

## Datapacks versus config

Simple scalar/toggle settings live in NeoForge TOML config.

Structured or arbitrarily sized definitions—state timelines, comfort data, meal effects, tooltip groupings, notifications—use reloadable datapack JSON. Reloads build and validate a complete new snapshot before replacing the live data, avoiding partially-applied definitions when one file is invalid.

## Optional integrations

Optional mods are kept behind isolated adapters/provider registries so they do not become hard runtime dependencies. Current integrations include Quality Food, Farmer's Delight, Panels Not Screens, and the optional Sable/Create Aeronautics comfort path.
