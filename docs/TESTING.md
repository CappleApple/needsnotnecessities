# Overall testing pass

Build and automated tests:

```powershell
.\gradlew.bat clean test build
```

Run the dedicated-server smoke test with `run/eula.txt` accepted:

```powershell
.\gradlew.bat runServer
```

The manual pass should cover:

- Install both mod JARs, join with every module enabled, open inventory, and verify there is no title above the status rows. Hover each Hunger, Thirst, and Rest row and confirm its tooltip lists the current datapack tier's generic modifiers and passive-regeneration change, or `No effects` for a modifier-free tier. Beneficial/nonnegative amounts should be green and negative amounts red in both these hover details and Shift food previews. Confirm the draggable handle uses the complete, uncropped carrot item texture at the normal 16x16 GUI-icon scale, then expand/collapse it, drag it around every screen edge, and verify its position, docking side, and collapsed state persist after relog/restart. Set `inventory_overlay.panel_icon_sprite` to another valid GUI sprite or item/block texture ID and verify the complete icon changes without affecting panel movement. If the client config file is temporarily locked by another process, moving the panel may fail to persist and log an error, but it must not crash the client.
- Verify the vanilla hunger bar is absent, vanilla starvation/natural hunger regeneration does not run, food remains edible at full vanilla food level, and named hunger state advances using nutrition plus saturation.
- With the default `hunger.eat_below_stage_percentage = 90`, verify food can be eaten in each of the lowest four default Hunger stages but not in the best stage. Repeat around custom datapack stage counts and with 0%/100% settings.
- Disable Hunger only, restart/reload the server config as required, and verify vanilla hunger behavior/UI returns while thirst, rest, meals, comfort, and regeneration remain functional.
- Sleep without completing a time skip and confirm continuous partial Rest recovery. With the default 50% Rest threshold, verify Exhausted and Tired players may sleep while Neutral, Rested, and Well Rested players receive the not-tired message.
- Set `playersSleepingPercentage` to several values. Verify enough daytime sleepers skip to tick 13000 (night) and enough nighttime sleepers skip to the next day. Every player who was actually sleeping for the completed skip should immediately reach the best configured Rest state, receive its modifiers, and see the updated panel; awake players must remain unchanged. Then test `rest.allow_daytime_sleep`, `rest.require_tired_to_sleep`, `rest.sleep_below_stage_percentage`, and `rest.daytime_sleep_skips_to_night`; the final option should make daytime sleepers skip to the next day when false.
- Drink water, regular/splash/lingering potions, Farmer's Delight drinks, common-tag drinks, and tagged alcohol; confirm normal drinks improve thirst and alcohol uses its separate adjustment.
- Verify drinks work in every default Thirst stage, including the best stage. Confirm non-drink use items remain unaffected.
- Stand idle without eating and verify Thirst never moves. Eat foods worth several different hunger-hour amounts and confirm Thirst drops only by the configured `thirst_hours_per_food_hour` ratio and immediately updates state modifiers.
- Apply vanilla Hunger and verify the custom hunger timer drains at exactly twice its normal rate while the effect lasts.
- With Farmer's Delight installed, apply Nourishment and verify the custom hunger timer stops. Apply Hunger and Nourishment together and verify Nourishment still pauses the countdown; remove Nourishment and confirm Hunger acceleration resumes. Disable `compatibility.farmers_delight_nourishment_pauses_hunger` and verify the pause no longer occurs.
- Compare Instant Health and Regeneration on players with 20 and 40 maximum health. With the default 20-health reference, verify every healing amount doubles at 40 max health and halves at 10 max health. Disable `health_effects.scale_with_max_health` and verify vanilla fixed amounts return.
- Inspect foods normally and verify only the short datapack flavor label appears. Enable F3+H and verify hunger restoration/thirst cost appear; hold Shift and verify Active Meal effects/durations appear. Test Shift+F3+H together, then compare the prediction with `/nnn meal analyze` after eating.
- Place repeated same-type and mixed-type comfort sources; compare `/nnn comfort scan <player>` with the panel, retention timer, and expected geometric diminishing returns.
- Confirm first server load creates `config/needs_not_necessities/comfort_auto_classification.json`. With at least one furniture mod installed, place blocks whose registry paths contain chair, bench, sofa/couch, table/desk, lamp, fireplace/stove, and bed/futon tokens. Run `/nnn reload`, verify each item tooltip reports its automatically classified comfort type, and verify `/nnn comfort scan` includes the placed block. Check that a crafting table, table lamp, industrial oven, and garden bed do not inherit the excluded categories. Test a name such as `desk_chair` and verify it receives only the first matching group in JSON order; reorder chairs and tables, reload, and verify the selected group changes. Tune a rule's regex/name/comfort and verify `/reload` applies it. Replace the file with `{}`, reload, and verify all automatic matches disappear while explicit comfort tags still work. Finally, explicitly tag a block that also matches a higher-valued regex in a different group and verify only the explicit definition applies.
- Eat basic, cooked, and multi-ingredient foods; verify only one Active Meal applies, weaker food cannot replace it, and equal-score policy follows server config.
- Define two different food-group definitions that each grant `+5% armor`, use one ingredient from each in a recipe, and verify both the Shift tooltip and active meal show a combined `+10% armor` modifier.
- Use two ingredients matched by the same vegetable definition and verify a `+5% armor` definition totals `+7.5% armor` with the default `meal.same_group_diminishing_factor = 0.5`. Add a third and verify `+8.75% armor`; test `1.0`, `0.25`, and `0.0` factors as well. Confirm each recipe tag slot is counted once regardless of how many item alternatives it contains.
- With Farmer's Delight installed, inspect Steak and Potatoes while holding Shift and verify every ingredient supplied through current common food tags contributes at every recursive recipe depth. The default datapack must not rely on ingredient-specific Baked Potato or Cooked Rice overrides.
- Still with Farmer's Delight, inspect Raw Pasta and record its dough-derived Active Meal modifiers. Then inspect Pasta with Meatballs and verify those Raw Pasta modifiers are included alongside its meat and sauce contributions. Repeat with another food chain at least three recipes deep and confirm each inherited group follows the same configured diminishing sequence.
- Add a test datapack with two edible foods whose selected recipes reference one another. Verify Shift preview and server analysis terminate normally and count the repeated item only as the cycle-ending ingredient rather than recursing indefinitely.
- Confirm `meal.maximum_bonuses` defaults to 5 and lowering it deterministically caps distinct combined modifier lines after numeric stacking.
- Add a datapack meal rule for an item or `#c:food/meat`, run `/nnn reload`, and verify the new trait/bonus appears without a restart.
- Die once while both Hunger and Thirst are above their configured post-death levels and verify `You awaken weak, hungry, and parched.` appears. Die when either need is already at/below its post-death value and verify it does not. Change/blank the server-config message and repeat.
- Test death, non-death End return, dimension changes, relog, and a full server restart for persistence and non-duplicating attribute modifiers.
- Override a state with every datapack notification type, combinations, and no `notifications` array. Verify outputs occur only on entering that state. The bundled defaults should play the three lowest-state warning sounds; entering each highest Hunger, Thirst, or Rest state should show its full-state action-bar message and play its configured sound.
- Install Quality Food 1.21.1, consume none/iron/gold/diamond-quality copies of the same result item, and confirm quality primarily extends duration with only the configured capped strength increase. Remove Quality Food and confirm startup still succeeds.
- Remove Farmer's Delight and confirm startup still succeeds without a hard dependency.
- Connect two clients and verify each player receives only their own snapshot, sleep recovery works independently, and no panel state leaks between clients.
- Use `/nnn status`, state `set`/`add`, meal `inspect`/`analyze`, `reset`, and `reload` as an operator.
