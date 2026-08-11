# Datapack configuration

Structured survival definitions are datapack data so `/reload` can replace them safely. Global scaling, limits, module toggles, and death policies remain in `needs_not_necessities-server.toml`.

## Item and item-tag meal effects

Place definitions in `data/<namespace>/meal_effects/*.json`. A definition must select exactly one `item` or one non-empty `tags` array. Tags in the array are alternatives; the rule matches if any tag matches.

```json
{
  "tags": ["c:foods/meat", "c:food/meat"],
  "traits": {
    "power": 2.0,
    "recovery": 1.0
  },
  "bonuses": [
    {
      "id": "power",
      "trait": "power",
      "target": "minecraft:generic.attack_damage",
      "amount": 0.05,
      "operation": "MULTIPLY_TOTAL"
    },
    {
      "id": "recovery",
      "trait": "recovery",
      "target": "minecraft:generic.max_health",
      "amount": 2.0,
      "operation": "ADD"
    }
  ],
  "score_bonus": 2.0,
  "duration_bonus_hours": 2.0
}
```

An exact-item rule uses `"item": "minecraft:beef"` instead of `tags`. Supported operations are `ADD`, `MULTIPLY_BASE`, and `MULTIPLY_TOTAL`. Targets may be registered attributes or the custom scalar `needs_not_necessities:passive_regeneration`.

The result item and every recipe ingredient contribute their matching effects. When an ingredient is itself an edible prepared food with a recipe, its ingredients are resolved recursively through every further prepared-food layer. This makes an intermediate food's displayed buffs carry into meals that use it, such as Raw Pasta carrying its dough-derived bonuses into Pasta with Meatballs. A repeated item on the active recipe path ends that branch, preventing reversible or cyclic recipes from recursing forever without imposing an arbitrary depth cutoff.

Each `meal_effects` definition ID is one food group. Distinct groups contribute at full strength and amounts with the same modifier target and operation add into one combined modifier, so two groups that each grant `+1 armor` produce one `+2 armor` meal effect.

Repeated ingredients from the same group use geometric diminishing returns across the entire resolved recipe tree. The server setting `meal.same_group_diminishing_factor` defaults to `0.5`, producing ingredient weights of 100%, 50%, 25%, 12.5%, and so on for that group. Set it to `1.0` for full stacking or `0.0` to count only the first matching ingredient. The eaten result item's own definition remains a separate full-strength base contribution. Recipe ingredient alternatives are proportionally averaged at every depth before group occurrences are counted, so a tag ingredient does not falsely count every possible alternative as another copy of the group.

Traits, score bonuses, duration bonuses, and numeric modifier amounts all use the same per-group ingredient weight. Trait votes determine priority only when the number of distinct combined modifiers exceeds the server-configured `maximum_bonuses` cap, which defaults to five. The effect multiplier, score-per-complexity, durations, caps, and equal-score replacement policy are also server-configurable. Built-in defaults cover meat, fish, vegetables, fruit, grain, soups/stews, and golden food using common tags, including the requested `#c:food/meat` alias.

The bundled meal definitions rely on common food tags rather than maintaining ingredient-specific Baked Potato or Cooked Rice exceptions. Mod and modpack datapacks should add missing ingredients to the appropriate common tag or provide a purpose-built meal-effect group.

## State timelines

Place arbitrary-length state timelines in `data/<namespace>/survival_states/*.json`. Each timeline declares `neutral_state` and ordered `states`. Each state supports `id`, `name`, `order`, `duration_hours`, `description`, `passive_regeneration_multiplier`, generic `modifiers`, and an optional `notifications` array.

Notifications are declared at the exact state that should trigger them. Omit the array or use an empty array for no notification. Any combination of `SOUND`, `ACTION_BAR`, `TOAST`, and `CHAT` may be supplied:

```json
{
  "id": "starving",
  "name": "Starving",
  "order": 0,
  "duration_hours": 12.0,
  "notifications": [
    {
      "type": "SOUND",
      "sound": "example:notification.hunger_low",
      "volume": 0.65,
      "pitch": 1.0
    },
    {
      "type": "ACTION_BAR",
      "message": "{system}: {state}"
    },
    {
      "type": "TOAST",
      "title": "Survival warning",
      "message": "You are now {state}."
    },
    {
      "type": "CHAT",
      "message": "{system} reached {state}."
    }
  ]
}
```

`{system}` and `{state}` are expanded at runtime. The bundled hunger, thirst, and rest timelines only play their respective placeholder sound when their lowest state is reached. The server-config cooldown prevents repeated re-entry spam but no longer chooses notification types.

The bundled hunger, thirst, and rest files are complete examples. State IDs are formed as `<namespace>:<timeline>/<state>` unless the state supplies a full resource location.

## Food tooltip groupings

Place hunger-hour label ranges in `data/<namespace>/food_tooltip_groups/*.json`:

```json
{
  "name": "Light Snack",
  "minimum_hours": 0.0,
  "maximum_hours": 2.0,
  "color": "#A8D8A8",
  "description": "Light Snack"
}
```

Bounds are inclusive; when two definitions share a boundary, the lower range wins. Omit `maximum_hours` for the final open-ended group. The built-in progression is Light Snack, Snack, Small Meal, Meal, Filling Meal, Substantial Meal, and Hearty Meal. The bundled flavor labels contain at most two words, but datapacks are not subject to an artificial word limit.

Normal food tooltips only show the short flavor label. F3+H reveals exact hunger restoration and thirst cost. Holding Shift reveals predicted Active Meal modifiers and their duration; Shift and F3+H together also show the advanced recipe analysis.

## Comfort

Comfort sources live in `data/<namespace>/comfort_sources/*.json`:

```json
{
  "tag": "needs_not_necessities:comfort/hearths",
  "type": "hearths",
  "name": "Hearth",
  "comfort": 8.0
}
```

Use `block` instead of `tag` for one exact block. `type` is the stable diminishing-returns group and `name` is its user-facing tooltip label.

Automatic furniture matching is configured separately in `config/needs_not_necessities/comfort_auto_classification.json`. The file is created with the bundled defaults on first server load:

```json
{
  "groups": [
    {
      "group": "chairs",
      "name": "Chair",
      "namespace_regex": "^(?!minecraft$).+$",
      "regex": "(?:^|_)(?:chair|armchair|recliner|stool|seat)(?:_|$)",
      "comfort": 4.0
    },
    {
      "group": "tables",
      "name": "Table",
      "namespace_regex": "^(?!minecraft$).+$",
      "regex": "(?:^|_)(?:table|desk|nightstand)(?:_|$)",
      "exclude_regex": "(?:^|_)(?:crafting|workbench|saw|lamp|light)(?:_|$)",
      "comfort": 5.0
    }
  ]
}
```

The root `groups` object is recommended, although a bare array of entries is also accepted. Replace the entire file with `{}` to disable every automatic rule. Changes take effect on `/reload`; deleting the file recreates the defaults on the next load. These are Java regular expressions without slash delimiters. `namespace_regex` matches the complete namespace and defaults to `.+`; `regex` searches the block path; optional `exclude_regex` rejects false positives. `group`, `name`, `regex`, and positive `comfort` are required, and group names must be unique. Rules are evaluated from top to bottom and each otherwise-unclassified block receives only its first regex match; later matches are ignored. For example, the default order classifies `desk_chair` as a chair because `chairs` appears before `tables`.

Expressions compile once, then every registered block is classified once after datapack tags update. Runtime comfort scans use the resulting block cache and never execute regexes. Any matching explicit `block` or `tag` comfort source suppresses every regex classification for that block, regardless of group or comfort amount. Regexes therefore only fill blocks that datapacks have not classified explicitly, and configuration order resolves ambiguous names deterministically.

The built-in sources, extension tags, and generated standalone configuration cover `beds`, `chairs`, `benches`, `sofas`, `tables`, `lighting`, and `hearths`. Automatic defaults intentionally ignore the `minecraft` namespace, whose supported blocks are explicitly tagged. Block items use the same classification for their comfort tooltip; unrelated item-only registry entries are not scanned because they cannot contribute placed comfort.

Comfort effects live in `data/<namespace>/comfort_effects/*.json` and declare `threshold`, `repeat`, and one or more generic modifiers.

## Classification tags

- `#needs_not_necessities:drinks`
- `#needs_not_necessities:alcoholic_drinks`
- comfort block tags under `#needs_not_necessities:comfort/*`, including beds, chairs, benches, sofas, tables, lighting, and hearths

Other mods and datapacks may append values to all of these tags.

The default drinks tag includes vanilla potion, splash-potion, lingering-potion, honey-bottle, and milk-bucket items. It also consumes the common `#c:drinks`/legacy `#forge:drinks` tags and optional drink tags from Farmer's Delight, Brewin' and Chewin', Croptopia, Vinery, and Herbal Brews. Farmer's Delight's four standard drinks are listed explicitly as an additional fallback. Alcoholic common/mod tags are checked first.

Thirst has no natural timer decay. Eating reduces it by `food hunger-hours * thirst_hours_per_food_hour`; that TOML ratio is configurable. Drinks then apply their configured level adjustment, and every resulting thirst state participates in the same generic modifier pipeline as Hunger and Rest.

The Hunger `eat_below_stage_percentage` and Thirst `drink_below_stage_percentage` server rules both default to 90. Ordered datapack stages are counted from worst and whole stages are selected; with five stages, 90% permits consumption in the lowest four and blocks it in the best stage.
