# BacinaSkills

A skills plugin for Purpur / Paper (Minecraft 26.x, Java 25) with five levelable skills, per-skill perks, and a 10-tier prestige system.

| Skill | XP from |
|---|---|
| Mining | Stone, ores and other underground blocks (pickaxe) |
| Woodcutting | Logs (every log cut by tree feller counts) |
| Herbalism | Ripe crops, melons, pumpkins, cane, cactus, mushrooms, flowers, berries |
| Herdmaster | Breeding animals |
| Fishing | Fish, treasure and junk |

Levels go from 1 to 100. At level 100 a skill can be prestiged (up to Prestige 10): the level resets to 1 and the prestige perks are permanent.

---

## Installation

1. Build the plugin with Maven/Gradle (Java 25) and put the jar in `plugins/`.
2. Start the server once. This creates `config.yml`, `multipliers.yml` and a `data/` folder.
3. Adjust `config.yml` and `multipliers.yml` to taste and restart.

Player data is saved as `plugins/BacinaSkills/data/<uuid>.yml` (on quit, on shutdown and every few minutes).

---

## Commands

Main command: `/bskills` (alias `/bskill`)

| Command | Description | Permission |
|---|---|---|
| `/bskills` | Overview of all skill levels, XP and prestige | everyone |
| `/bskills info <skill>` | Your active perks and XP multiplier for a skill | everyone |
| `/bskills prestige <skill>` | Prestige a skill (requires level 100, max Prestige 10) | everyone |
| `/bskills autoreplant [on\|off]` | Toggle Herbalism auto replant (default on) | everyone |
| `/bskills autosapling [on\|off]` | Toggle Woodcutting tree-feller sapling planting (default off) | everyone |
| `/bskills multiplier` | List the XP multiplier of each skill | `skills.admin` |
| `/bskills multiplier <skill\|all> <value>` | Set an XP multiplier, e.g. `/bskills multiplier mining 2.00` | `skills.admin` |
| `/bskills admin <player> <skill> <level\|prestige\|xp> <value>` | Set a player's level/prestige or add XP (for testing) | `skills.admin` |

`<skill>` is one of `mining`, `woodcutting`, `herbalism`, `herdmaster`, `fishing`.

### Permissions

| Permission | Default | Description |
|---|---|---|
| `skills.admin` | op | `/bskills multiplier` and `/bskills admin` |

---

## XP multipliers

Every skill has its own XP multiplier, default `1.00`. Set it with `/bskills multiplier <skill|all> <value>` (allowed range 0 to 1000) or edit `plugins/BacinaSkills/multipliers.yml`. The multiplier is applied to every XP gain of that skill.

---

## Perks

Level perks **replace** each other: the highest tier you have reached is the one that applies. Prestige perks are permanent.

### Mining

| Level | Perk |
|---|---|
| 1 / 30 / 60 / 90 | 5% / 10% / 15% / 20% mining speed |
| 10 / 40 / 70 | 10% / 20% / 30% chance to double mined ore |
| 20 / 50 / 80 | 10% / 20% / 30% chance not to use durability |
| 100 | 10% chance to triple mined ore |
| Prestige 9 | 40% mining speed (permanent) |
| Prestige 10 | 15% triple and 40% double ore chance |

- Mining speed only works while holding a pickaxe. It is an attribute modifier, so it stacks with the Haste potion effect.
- Ore multipliers do **not** multiply Fortune. The no-Fortune amount is multiplied (x1, x2 or x3), then the extra that Fortune added is added on top. Triple is rolled first, then double. Silk Touch drops are not multiplied.

### Woodcutting

| Level | Perk |
|---|---|
| 1 / 10 / 40 | 5% / 10% / 15% chance for an extra log |
| 20 / 50 / 80 | 10% / 30% / 70% chance to fell a tree instantly |
| 30 / 60 | 10% / 30% chance not to use axe durability |
| 70 | 50% chance to drop 1-3 apples when tree feller triggers |
| 90 | Plants saplings on felled trees (needs `/bskills autosapling on` and saplings in your inventory) |
| 100 | Tree feller cooldown removed |

- Tree feller needs an axe. Block limit: 64 logs from level 20, 256 from level 50. Default cooldown is 30 seconds (`woodcutting.tree-feller-cooldown-seconds`).
- Only natural trees are felled: connected logs of one species, touching enough natural (non-persistent) leaves, with a log on dirt-like ground. Stripped logs and wood blocks are ignored, so player builds are safe. Claim/protection plugins can veto each block.
- Every felled log gives XP and can drop an extra log. Each log costs axe durability (Unbreaking and the no-durability perk apply).
- Saplings are planted where a log stood on dirt, one sapling taken from your inventory per spot.

### Herbalism

| Level | Perk |
|---|---|
| 1 / 30 / 60 / 90 | 10% / 15% / 30% / 50% chance for double crop drops |
| 10 / 40 | 10% / 20% chance for a planted crop to instantly grow one stage (1 minute cooldown) |
| 20 / 50 / 80 | 10% / 50% / 100% chance to auto replant (toggle with `/bskills autoreplant`) |
| 70 / 100 | 40% / 80% chance for a planted crop to get one bone meal |
| Prestige 3 / 6 | 10% / 20% chance to triple crops |
| Prestige 10 | 100% chance for a planted crop to get one bone meal |

- Crops only give XP when fully grown.
- Auto replant uses one seed from the drops when there is one (otherwise the replant is free).

### Herdmaster

| Level | Perk |
|---|---|
| 1 / 20 / 40 / 70 / 100 | 10% / 20% / 30% / 40% / 50% breeding cooldown reduction (5 min base) |
| 10 / 50 / 80 | Offspring permanently give +1 / +2 / +3 drops (meat and leather) |
| 30 / 60 / 90 | 10% / 20% / 30% chance for twins |

Prestige perks (horses, donkeys and mules):

| Prestige | Effect |
|---|---|
| 1 / 2 / 4 / 8 | 10% / 20% / 25% / 30% chance for each stat (health, speed, jump) to increase |
| 1 / 3 / 5 / 10 | Stat increase of 1% / 2% / 3% / 5% when it triggers |

- Stat gains are a percentage of the current value and never go above the vanilla limits (`herdmaster.horse-limits` in `config.yml`).
- Twins copy sheep colour and horse genetics from the first baby. Other variants use spawn defaults.

### Fishing

| Level | Perk |
|---|---|
| 1 / 30 / 60 / 90 | 10% / 20% / 30% / 50% faster fishing |
| 20 / 50 / 80 | +1 / +2 / +3 Luck of the Sea |
| 10 / 40 / 70 | 10% / 20% / 30% chance to roll the loot table twice |
| 100 | 10% chance to roll the loot table three times |

Prestige perks (stack with level perks):

| Prestige | Double loot | Triple loot |
|---|---|---|
| 1 | +5% | |
| 2 | +10% | |
| 3 | +10% | +5% |
| 4 | +15% | +5% |
| 5 | +15% | +10% |
| 6 | +20% | +10% |
| 7 | +20% | +15% |
| 8 | +25% | +15% |
| 9 | +25% | +20% |
| 10 | +35% | +30% |

- Faster fishing shortens the hook's wait time; the Lure enchantment stacks with it. Bonus Luck of the Sea stacks with the rod's own enchantment.
- On each catch, triple is checked first, then double, otherwise it is a single catch. Every pull is a full roll of the vanilla fishing loot table, so one catch can give e.g. a fish and a treasure.
- When a triple fires, one random pull of the three gets roughly triple treasure chance.
- Extra loot flies to the player like a normal catch. Only the normal catch gives XP.

---

## Configuration

`config.yml`:

```yaml
settings:
  action-bar-xp: true          # show "+X Mining XP" above the hotbar
  ignore-player-placed: true   # player-placed ores/logs/plants give no XP
  autosave-minutes: 5

xp-curve:                      # XP from level L to L+1 = round(base * L ^ exponent)
  mining:      { base: 20, exponent: 1.15 }
  woodcutting: { base: 20, exponent: 1.15 }
  herbalism:   { base: 20, exponent: 1.15 }
  herdmaster:  { base: 10, exponent: 1.15 }
  fishing:     { base: 10, exponent: 1.15 }

xp:
  mining:      { default-ore: 8, default-stone: 1, blocks: { ... } }
  woodcutting: { log: 6 }
  herbalism:   { default-flower: 2, blocks: { ... } }
  herdmaster:  { breed: 150 }
  fishing:     { fish: 10, treasure: 40, junk: 5 }

woodcutting:
  tree-feller-cooldown-seconds: 30
  min-natural-leaves: 4

herdmaster:
  horse-limits: { health: 30.0, speed: 0.3375, jump: 1.0 }
```

Values missing from your `config.yml` fall back to the defaults above, so updating the plugin never breaks an existing config. With the default curve, level 100 takes roughly 185k XP in the standard skills; raise `base` to make levelling slower, or use the XP multiplier.

`multipliers.yml` is managed by `/bskills multiplier`:

```yaml
mining: 1.0
woodcutting: 1.0
herbalism: 1.0
herdmaster: 1.0
fishing: 1.0
```

---

## Notes

- Blocks that a player placed (ores, logs, non-crop plants) give no XP or perks when broken. This is tracked in memory only and resets on restart. Disable with `settings.ignore-player-placed: false`.
- Creative mode players earn no XP.
- Perks that depend on a tool only work with the right tool: pickaxe for Mining, axe for tree feller and Woodcutting durability.
