# SopPillars

`SopPillars` is a Paper plugin for a Lucky Pillars-style PvP mini-game:
players queue into an arena, start in cages, receive random loot over time,
fight in teams, and the last alive team wins.

It is built for real server usage with admin-friendly tooling (wizard + menus),
party integration, cosmetics, PlaceholderAPI support, and arena rollback flow.

## Key Features

- Full arena lifecycle: waiting lobby -> countdown -> cages -> combat -> winner phase.
- Interactive setup wizard with hotbar controls and action bar progress.
- Party-aware queueing via `SopParty`, with proper solo fallback.
- Per-arena settings menu (`/pillars settings`) with immediate save.
- Improved loot UX:
  - explicit `WHITELIST/BLACKLIST` mode switch,
  - inventory editor for arena whitelist items,
  - inventory editors for arena/global blacklist lists.
- Kit system with player selection and `No kit` option.
- Cosmetics system for cages, victory effects, kill effects, and death effects.
- WorldEdit cage paste with rotation from configured spawn yaw.
- PlaceholderAPI placeholders for tab, scoreboards, and admin widgets.
- Arena snapshots/backups for clean post-match restoration.

## Requirements

- Java 8+ (the project compiles with `target 8`).
- Paper/Spigot-compatible server (`api-version` in `plugin.yml`: `1.16`).
- Optional soft dependencies:
  - `SopParty` (party queue integration),
  - `PlaceholderAPI` (placeholders),
  - `SopLib` (if used in your stack).
- WorldEdit plugin is required on the server for `.schem` cage support.

## Build

From repository root:

```bash
mvn -DskipTests package
```

Output JAR:

- `target/SopPillars.jar`

## Installation

1. Copy `SopPillars.jar` into `plugins/`.
2. Start the server once to generate config folders/files.
3. (Optional) Install `SopParty`, `PlaceholderAPI`, and WorldEdit.
4. Restart the server.

## Quick Start

1. Set global fallback spawn:
   - `/pillars setglobalspawn`
2. Create an arena:
   - `/pillars create <name> <mode> <teams> <playersPerTeam>`
3. Complete the setup wizard (see below).
4. Save arena:
   - `/pillars save`
5. Test join:
   - `/pillars join <name>`

## Arena Setup Wizard

Wizard starts automatically after `/pillars create` or `/pillars edit`.

Hotbar controls:

- Slot 1: set current point (`Set`)
- Slot 2: go back (`Back`)
- Slot 3: skip (`Skip`) - only for optional `setendspawn`

Step order:

1. `pos1` (gameplay area)
2. `pos2` (gameplay area)
3. `lobbypos1`
4. `lobbypos2`
5. `setspectator`
6. `setlobbyspawn`
7. `setendspawn` (optional)
8. `setspawn` for each team/player slot in order

Progress is shown in action bar as `Step X/8`, and during `setspawn` it also shows spawn-slot progress.

## Commands

Player commands:

- `/pillars list`
- `/pillars join <arena>`
- `/pillars random [mode,mode,...]`
- `/pillars leave`
- `/pillars kits`
- `/pillars cosmetics`
- `/pillars stats`

Admin commands:

- `/pillars create <name> <mode> <teams> <playersPerTeam>`
- `/pillars edit <arena>`
- `/pillars save`
- `/pillars cancel <arena>`
- `/pillars delete <arena>`
- `/pillars settings` (currently edited arena)
- `/pillars settings global` (global loot defaults)
- `/pillars setglobalspawn`
- `/pillars tp <arena>`
- `/pillars kitadd <kit>`
- `/pillars kitremove <kit> <slot|all>`
- `/pillars reload`

## Permissions

- `soppillars.play` - default `true`
- `soppillars.stats` - default `true`
- `soppillars.admin` - default `op`
- `soppillars.cage.<id>` - optional per-cage permission
- `soppillars.victory-effect.<id>` - optional per-victory-effect permission
- `soppillars.kill-effect.<id>` - optional per-kill-effect permission
- `soppillars.death-effect.<id>` - optional per-death-effect permission

## Kits

- Players choose kits via `/pillars kits`.
- `No kit` allows playing without any starter kit.
- Admin kit management without manual YAML edits:
  - hold item in main hand -> `/pillars kitadd <kit>`
  - remove by index -> `/pillars kitremove <kit> <slot>`
  - clear entire kit -> `/pillars kitremove <kit> all`

Kit files are stored in `plugins/SopPillars/kits/`.

## Loot System

Arena settings menu (`/pillars settings`) includes:

- loot interval,
- explicit loot mode (`WHITELIST` or `BLACKLIST`),
- arena list editors:
  - `Edit arena whitelist`
  - `Edit arena blacklist`

Global settings menu (`/pillars settings global`) includes:

- default loot mode,
 - global blacklist editor.

List editor workflow:

- open editor inventory,
- add/remove items,
- close inventory to save list.

### Whitelist Mode

Whitelist mode is now item-based.

Per-arena file format:

```yml
loot:
  blacklist-mode: false
  whitelist-items:
    - ==: org.bukkit.inventory.ItemStack
      v: 3955
      type: PLAYER_HEAD
      amount: 1
```

This allows whitelist entries such as:

- normal items
- custom heads
- items with custom meta
- other saved `ItemStack` data

The arena whitelist editor now saves full `ItemStack` entries, not only material names.

## Cosmetics

Players choose cosmetics with:

- `/pillars cosmetics`

Current categories:

- `Cages`
- `Victory Effects`
- `Kill Effects`
- `Death Effects`

### Cages

- Put cage `.schem` files into `plugins/SopPillars/cages/`.
- `default.schem` is auto-restored from plugin resources if deleted.
- At match start, plugin pastes the selected schematic through WorldEdit.
- Rotation is based on `setspawn` yaw (nearest cardinal direction).

Menu metadata for cages lives in:

- `plugins/SopPillars/cosmetics/cages.yml`

Example:

```yml
cages:
  default:
    display-name: "&bDefault Cage"
    permission: ""
    icon: LIGHT_BLUE_STAINED_GLASS
    lore:
      - "&7Classic glass cage."
```

### Victory Effects

Configured in:

- `plugins/SopPillars/cosmetics/victory-effects.yml`

Supported types:

- `fireworks`
- `entity_rain`
- `block_rain`

Arena settings only control celebration geometry and timing:

- `shape`
- `radius`
- `interval-ticks`
- `spawn-height`
- `amount-per-wave`

The selected victory cosmetic controls what actually spawns, such as:

- fireworks
- pig/cow/chicken rain
- anvil rain
- block rain with custom block material

### Kill and Death Effects

Configured in:

- `plugins/SopPillars/cosmetics/kill-effects.yml`
- `plugins/SopPillars/cosmetics/death-effects.yml`

Supported burst types:

- `particle_burst`
- `totem`
- `lightning_fake`

Example:

```yml
effects:
  default_kill:
    display-name: "&cDefault Kill Burst"
    permission: ""
    icon: BLAZE_POWDER
    lore:
      - "&7Simple flame burst on kill."
    type: particle_burst
    particle: FLAME
    particle-count: 18
    sound: ENTITY_BLAZE_SHOOT
    sound-volume: 0.9
    sound-pitch: 1.2
```

### PlaceholderAPI In Cosmetic Text

If `PlaceholderAPI` is installed, cosmetic menu item names and lore support placeholders.

This works in:

- `cages.yml`
- `victory-effects.yml`
- `kill-effects.yml`
- `death-effects.yml`

Cosmetic menu items are created through `SopLib`.

## PlaceholderAPI

Prefix: `%soppillars_<key>%`

Available keys:

- `in_game`
- `game_status`
- `arena`
- `mode`
- `team`
- `alive`
- `countdown`
- `alive_players`
- `filled_teams`
- `players_total`
- `loot_in` (alias: `next_loot_in`)
- `time_to_end` (alias: `game_end_in`)
- `min_players`
- `max_players`
- `min_filled_teams`
- `arena_world`
- `arena_teams`
- `arena_players_per_team`
- `arena_max_players`
- `arena_min_players`
- `arena_min_filled_teams`
- `arena_countdown_seconds`
- `arena_cage_seconds`
- `arena_pre_border_delay_seconds`
- `arena_border_shrink_seconds`
- `arena_end_border_diameter`
- `arena_lava_enabled`
- `arena_lava_start_delay_seconds`
- `arena_lava_rise_interval_seconds`
- `arena_post_shrink_end_delay_seconds`
- `arena_friendly_fire`
- `arena_allow_place_blocks`
- `arena_allow_break_original_blocks`
- `arena_allow_break_player_blocks`
- `arena_allow_smooth_fall`
- `arena_smooth_fall_seconds`
- `arena_loot_enabled`
- `arena_loot_interval_seconds`
- `arena_celebration_seconds`
- `arena_victory_effect_shape`
- `arena_victory_effect_radius`
- `arena_victory_effect_interval_ticks`
- `arena_victory_effect_spawn_height`
- `arena_victory_effect_amount_per_wave`
- `gameplay_min_x`
- `gameplay_min_y`
- `gameplay_min_z`
- `gameplay_max_x`
- `gameplay_max_y`
- `gameplay_max_z`
- `gameplay_size_x`
- `gameplay_size_y`
- `gameplay_size_z`
- `stats_games`
- `stats_wins`
- `stats_kills`
- `stats_deaths`
- `stats_winstreak`

Examples:

- `%soppillars_game_status%`
- `%soppillars_min_players%`
- `%soppillars_stats_wins%`
- `%soppillars_stats_winstreak%`
- `%soppillars_filled_teams%`
- `%soppillars_loot_in%`
- `%soppillars_time_to_end%`
- `%soppillars_arena_world%`
- `%soppillars_arena_border_shrink_seconds%`
- `%soppillars_gameplay_size_x%`

Arena-specific lookup by name is also supported:

- `%soppillars_arena111_min_players%`
- `%soppillars_arena111_max_players%`
- `%soppillars_arena111_world%`
- `%soppillars_arena111_gameplay_size_x%`

Format:

- `%soppillars_<arena_name>_<property>%`

Example:

- arena name `arena111`
- property `min_players`
- final placeholder `%soppillars_arena111_min_players%`

The long property form also works, for example:

- `%soppillars_arena111_arena_min_players%`
- `%soppillars_arena111_filled_teams%`

## Configuration

Main file: `plugins/SopPillars/config.yml`

Important options:

- `settings.global-spawn` - fallback teleport location.
- `settings.default-min-players` and `settings.default-min-filled-teams`.
- `settings.default-loot-blacklist-mode`.
- `settings.default-loot-blacklist`.

Arena files are stored in `plugins/SopPillars/arenas/`.

## Troubleshooting

- "No kits are available":
  - check kit permissions and kit YAML files in `kits/`.
- Custom cage is not spawning:
  - verify `.schem` exists in `cages/`,
  - ensure WorldEdit is installed and loaded.
- Placeholder returns empty:
  - ensure PlaceholderAPI is installed and expansion is registered at startup.
- Build outputs classes but no jar:
  - use Maven `package`, not only `compile`.

## Development Notes

- Current module version: `1.0.0`.
- `MVP.md` and `SPEC.md` are intentionally kept local and ignored in Git.
