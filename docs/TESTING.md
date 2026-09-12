# Testing

Use Java 21 for development and release checks.

## Automated tests

```powershell
.\gradlew.bat clean test build
.\gradlew.bat runGameTestServer
```

The automated suite covers the shared timeline math, command handling, player clone/respawn lifecycle, death settings, phantom spawning rules, state initialization, health synchronization, and several datapack/config edge cases.

GameTest classes are development-only and are excluded from the release jar.

## Dedicated server check

```powershell
.\gradlew.bat runServer
```

Before a release, confirm the server reaches `Done`, saves normally, and does not require client UI classes.

## Core manual pass

The sections below are the scenarios worth checking in a disposable world when the related system changes. They are grouped by feature instead of as one release-signoff checklist.

### HUD and client state

- Open the inventory with Hunger, Thirst, and Rest enabled and verify the Panels Not Screens status panel appears without replacing the normal inventory.
- Expand/collapse the panel, move/dock its handle on each side, relog, and confirm the client-local position/state persists.
- Hover Hunger, Thirst, and Rest rows and confirm the current state's modifier summary matches the loaded datapack definition.
- Change `inventory_overlay.panel_icon_sprite` and verify the handle uses the new item/block/GUI sprite without affecting movement.
- Temporarily make the client config unwritable and confirm a failed save logs an error without crashing.

### Hunger

- Verify the vanilla hunger bar/tick is replaced only while the Hunger module is enabled.
- Check the configured stage-percentage eating threshold, including custom state counts and the 0%/100% boundaries.
- Confirm `can_always_eat` foods remain usable when ordinary food is blocked.
- Apply vanilla Hunger and verify it changes custom drain by the configured multiplier.
- With Farmer's Delight installed, verify Nourishment pauses the custom timer when that compatibility option is enabled.

### Thirst

- Check normal drinks, potion forms, tagged drinks, and tagged alcohol.
- Verify Thirst does not passively move while the player does nothing.
- Eat foods with different hunger-hour values and confirm the configured food-to-thirst pressure is applied.
- Confirm disabling the Thirst module leaves unrelated systems running.

### Rest and sleep

- Sleep without completing a time skip and verify partial Rest recovery.
- Check the configured “tired enough to sleep” threshold during daytime.
- Test `playersSleepingPercentage` with both daytime and nighttime skips and confirm only participating sleepers receive the completed-sleep Rest refill.
- In the lowest configured Rest state, verify natural phantom spawning still follows darkness/sky/altitude/difficulty/gamerule checks.
- Move out of the lowest Rest stage and verify Rest-based phantom attempts stop.
- Disable Rest and confirm vanilla insomnia behavior returns.

### Health

- Test the Base Health module in both additive and percentage-modifier combinations and reload/restart to make sure the base value does not compound.
- Remove a max-health bonus while current health is above the new maximum and verify the clamp does not look like incoming damage.
- Compare Instant Health, Regeneration, and Absorption on players with different maximum health values when proportional scaling is enabled.
- Test death respawn-health percentage with max-health modifiers and with the death module disabled.
- Return from the End without dying and verify death-only health/reset logic is not applied.

### Comfort

- Place repeated and mixed comfort source types and compare the panel with `/nnn comfort scan <player>`.
- Edit `comfort_auto_classification.json`, reload, and confirm regex group order/values change classification as expected.
- Set the auto-classification file to `{}` and confirm explicit datapack block/tag comfort still works.
- Give a block both an explicit datapack definition and a regex match and confirm the explicit definition wins.
- If testing Sable/Create Aeronautics support, move and rotate a sub-level containing comfort sources and verify the scan follows the transformed world positions.

### Active Meals

- Eat foods with basic and multi-ingredient recipes and compare the Shift tooltip preview with `/nnn meal analyze`.
- Check that a weaker meal does not replace a stronger active meal unless the configured replacement rule allows it.
- Combine different food groups and verify their numeric bonuses stack.
- Repeat ingredients from the same group and verify the configured diminishing factor.
- Check a recipe chain several levels deep and confirm prepared ingredients inherit their own recipe contributions.
- Add an intentional recipe cycle in a test datapack and confirm analysis terminates cleanly.
- Change `meal.maximum_bonuses` and verify the combined result is capped deterministically.

### Datapack reloads and notifications

- Override at least one state and meal definition, run `/nnn reload` or `/reload`, and confirm the live data changes without a restart.
- Test state-entry notifications with sound/action-bar combinations and with no notifications.
- Confirm notifications fire on entering a state rather than every tick spent inside it.

### Death/reset commands

- Check the configured post-death Hunger/Thirst levels and optional message.
- Run `/nnn reset` as a player, `/nnn reset <player>` as an operator, and the long command alias.
- Test death, relog, dimension changes, and restart for persistent state and non-duplicating modifiers.

### Optional integrations

When touching an adapter, test both with the optional mod installed and with it completely absent:

- Farmer's Delight / Nourishment
- Quality Food
- Panels Not Screens
- Sable / Create Aeronautics comfort scanning

For multiplayer changes, connect at least two clients and verify snapshots, sleep recovery, and panel state do not leak between players.

## Useful commands

```text
/nnn status
/nnn hunger set <player> <value>
/nnn thirst set <player> <value>
/nnn rest set <player> <value>
/nnn comfort scan <player>
/nnn meal inspect
/nnn meal analyze
/nnn reset [player]
/nnn reload
```

The exact command tree may grow over time; `/help nnn` is the best source for the current operator surface.
