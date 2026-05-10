# SopPillars Specification

## Overview

`SopPillars` is a Paper/Spigot minigame plugin for a "Lucky Pillars" PvP mode.

Core gameplay:
- Players join an arena built by an admin.
- Before match start, players wait in a pre-game lobby area.
- At game start, players are teleported into personal cages placed above the arena.
- After a countdown, cages disappear and players fall onto pillars/platforms.
- The plugin gives random items directly into player inventories.
- Players kill each other until one player or one team remains alive.
- During the match, the playable zone shrinks with a world border.
- Optionally lava rises from below.
- At match end, arena state is restored to a saved baseline.

The plugin follows the `Sop*` style:
- Paper-first
- configurable through YAML
- messages configurable manually
- no unnecessary legacy `A*` compatibility
- future-friendly integration with `SopParty`, `SopLocales`, `SopLib`

## Scope

This document describes `SopPillars` only.

Global party behavior across multiple modes/servers is intentionally split into a separate future plugin: `SopParty`.

`SopPillars` must still support party-aware joining through an abstraction or direct integration when `SopParty` exists.

## Target Versions

Priority versions:
- `1.16.5`
- `1.21.1`
- `1.21.11`

Compatibility goal:
- one codebase
- Paper/Spigot compatible
- avoid brittle version-specific NMS unless strictly needed

## Arena Model

Each arena is permanent and built manually by an admin in the world where it will always exist.

Arena properties:
- `name`
- `mode`
- `teams`
- `players-per-team`
- `world`
- `state`
- `editable`
- `snapshot-ready`

Arena constraints:
- `mode` is a free-form matchmaking tag, not a preset
- examples: `solo`, `duo`, `1x1`, `4x4`, `draka`
- minimum team count is `2`
- `1 team` arenas are not allowed
- `1 player per team` is allowed

Arena states:
- `DISABLED`
- `EDITING`
- `WAITING`
- `STARTING`
- `RUNNING`
- `ENDING`
- `RESTORING`

## Arena Creation Flow

Command:

```text
/pillars create <name> <mode> <teams> <players per team>
```

Behavior:
- creates arena metadata
- binds arena to the admin's current world
- puts arena into `EDITING` mode immediately
- editing mode becomes the only state in which map construction is allowed

## Arena Editing

While arena is in `EDITING` mode:
- admins can build
- arena is not joinable
- random matchmaking ignores it

Outside `EDITING` mode:
- admins cannot build inside arena-controlled areas
- baseline map integrity must be preserved

Recommended commands:

```text
/pillars edit <arena>
/pillars save <arena>
/pillars cancel <arena>
```

### Save Behavior

`/pillars save <arena>`:
- exits `EDITING`
- validates required arena points/settings
- creates or refreshes the baseline snapshot once
- marks arena ready for matchmaking

Snapshot must be taken on save, not every match start.

## Required Arena Points and Regions

### 1. Gameplay Region

Set by:

```text
/pillars pos1
/pillars pos2
```

This region defines:
- horizontal limits for initial world border
- max build height as top
- death height as bottom
- rollback area
- block/build restrictions area

Region stores full 3D bounds.

### 2. Pre-Game Lobby Region

Required separate region for waiting players before start.

Requirements:
- players stay here while match is waiting or counting down
- region disappears or is invalidated at match start from gameplay perspective
- blocks may remain physically present or be managed separately; behavior is design-configurable

Commands should support:

```text
/pillars lobbypos1
/pillars lobbypos2
```

### 3. Player Spawn Points

Command:

```text
/pillars setspawn <team number> <player number>
```

Captured from executing player:
- position
- yaw
- pitch

Each slot stores:
- team index
- slot index
- location
- yaw/pitch
- derived cage rotation

### 4. Spectator Spawn

Required point where dead players are teleported.

### 5. Post-Game Spawn

Required point where:
- all players are teleported after match end
- players who leave before game start may also be sent if configured

Spawn source rules:
- default post-game spawn comes from main config
- arena may override it with a specific per-arena end spawn

## Cage System

Players start inside cages loaded from `.schem`.

Requirements:
- each player may choose a personal cage cosmetic
- cage rotation uses 4-direction snapping from stored spawn yaw
- cages are spawned at match start
- cages remain for configured countdown duration
- cages are removed when game starts

Optional gameplay toggle:
- smooth fall after cage removal

This likely means a configurable effect such as:
- slow falling potion
- custom motion handling

### Cage Cosmetics

Selection via GUI, not commands.

Base command:

```text
/pillars cosmetics
```

GUI structure:
- root cosmetics menu
- `Cages`
- reserved future submenus:
  - `Kill Effects`
  - `Death Effects`
  - `Victory Effects`

Access model:
- permissions define available cosmetics
- default cosmetic fallback exists

## Pre-Join Kit Selection

Kit selection happens before joining a match, not inside arena lobby.

Expected flow:
- player opens kit menu outside the match
- chosen kit becomes active preference
- if player has permission for that kit, it is used when the match starts

This is separate from team selection.

## Matchmaking and Joining

### Join Commands

```text
/pillars join <arena>
/pillars random
/pillars random <mode>[,<mode>[,<mode>]...]
```

### `/pillars join <arena>`

Behavior:
- joins exact arena if arena is in a joinable waiting state
- fails if arena is full or unavailable

### `/pillars random`

Behavior:
- first choose a random waiting arena that already has players and available slots
- if none exist, choose a random empty available arena of any mode

### `/pillars random <modes>`

Behavior:
- parse comma-separated mode list
- first choose a random waiting arena among those modes that already has players and available slots
- if none exist, choose a random empty available arena among those modes

### Team Assignment

Players do not choose teams with commands.
Team selection must be done through a GUI item while waiting in the lobby.

Requirements:
- upon joining a waiting arena, player receives a team-selector item
- clicking it opens team selection GUI
- GUI shows teams, occupancy, free slots, and lock/full state
- selector item is removed before players are distributed into cages

Team selection happens only inside the waiting lobby.

### Auto-Assignment Rules

Non-party player:
- may be auto-assigned to a valid team with free space
- may later manually switch through GUI while arena is still waiting

Party-aware behavior:
- if a party joins and there is a team that can fit all members, put whole party into that team
- if no single team can fit all members, split them intelligently across teams if total arena slots are sufficient
- if total free arena slots are insufficient for the whole party, reject join

Constraints:
- do not reject party just because one team cannot fit everyone
- reject only when the arena itself lacks enough total free slots
- assignment priority:
  - first try to keep whole party in one team
  - if impossible, split across as few teams as possible

This split behavior is intentional and must stay enabled.

This behavior should be implemented carefully and may later delegate to `SopParty`.

## Match Start Logic

### Auto-Start

Arena config must support both:
- `min-players`
- `min-filled-teams`

When minimum conditions are met:
- start countdown begins
- countdown duration defaults from main config
- can be overridden per arena config

If conditions become invalid before countdown ends:
- countdown is cancelled

### Start Sequence

1. Lock arena from new incompatible joins
2. Freeze waiting composition as needed
3. Teleport players to cage positions
4. Spawn cages
5. Give pre-start invulnerability/freeze as needed
6. Wait configured cage/countdown time
7. Remove cages
8. Optionally apply smooth-fall behavior
9. Start match timers and hazard systems

Important rule:
- once players are teleported out of the pre-game lobby and into cages, the game is considered started
- if a player leaves after this point, it counts as a loss

## Gameplay Settings

Admin GUI:

```text
/pillars settings
```

Settings to support:
- `allow-place-blocks` default `true`
- `allow-break-original-blocks` default `false`
- `allow-break-player-blocks` default `true`
- loot mode toggle `whitelist/blacklist`
- item collection GUI for list editing
- custom item list support through drag/drop
- toggle `smooth-fall-after-cage`
- `cage-time`
- `pre-border-delay`
- `border-shrink-duration`
- toggle `lava-rise-enabled`
- `lava-start-delay`
- `lava-rise-interval-per-level`
- `post-full-shrink-end-delay`
- death-to-spectator toggle
- spectator no-leave-bounds toggle
- allowed commands whitelist during match

Arena config should inherit unspecified values from main config.

## Loot System

Random items go directly into player inventory.

### Modes

#### Whitelist
- only configured allowed items can be generated

#### Blacklist
- any vanilla item can be generated except blocked items

Blacklist mode must support additional toggles for broad families:
- enchanted books enabled/disabled
- potions enabled/disabled
- tipped arrows enabled/disabled
- spawn eggs enabled/disabled

### Custom Items

Custom items must support arbitrary `ItemStack` data:
- name
- lore
- enchantments
- custom model data
- potion meta
- NBT/component-backed metadata as preserved by Bukkit/Paper serialization

Addition/removal flow:
- admin drags item into GUI to add
- remove through GUI interaction

Future integration point:
- `SopItemsCreator`

## Starting Kits

Players must be able to choose a starting kit before gameplay begins.

Requirements:
- kit selection through GUI
- kit presets configured per arena or inherited from main config
- permissions decide which kits are available
- selected kit is granted at or immediately before match start
- default kit fallback exists

## Border and Hazards

### World Border

The gameplay region horizontal bounds define initial border.

Behavior:
- border starts shrinking after configurable delay
- shrink duration configurable
- shrink target configurable or derived from arena center/final bounds

Need to define:
- start border = full arena region horizontal size
- final border = configured final size or region center collapse area

### Lava Rise

Optional feature.

Behavior:
- starts after configurable delay
- raises by one level on configured interval
- acts from arena bottom upward

Implementation must use real lava blocks.

Lava rules:
- rises as a flat layer
- one Y level at a time
- fills only the original gameplay region
- ignores current shrinking world border
- must not spread beyond configured arena bounds

## Death, Spectators, and Win Logic

On death:
- player becomes spectator
- teleported to spectator spawn
- cannot leave allowed spectator bounds

Spectator restrictions:
- no interaction with gameplay
- cannot be seen as alive participant
- may view alive players and spectator chat rules apply

### Death Cause Messaging

Need configurable message hooks for:
- self death
- killed by player
- killed by mob
- knocked into void by player
- knocked into void by mob or player's mob
- void self death
- fall self death
- fall due to player
- lava self death
- lava due to player
- projectile by player
- projectile by mob

Support should be event-context-driven and extensible.

### Team Elimination

When entire team is out:
- broadcast configurable team-death message if set

### Victory

Win condition:
- one player alive in solo-like setup
- one team alive in team mode

Friendly fire:
- always disabled
- not a per-arena setting

After victory:
- celebration period, default around `10s`
- all participants including spectators can see each other's chat during celebration
- victory commands run
- then all players are teleported to post-game/global spawn

Default win command example:

```text
/eco give {player} 1000
```

## Chat Rules

During waiting/running:
- match players do not see global server chat
- outside players do not see match chat
- alive players do not see spectator chat
- spectators can see alive players

During celebration:
- all players tied to this match, alive and spectator, see each other

Per-arena chat format should be configurable.

Arena config values should default from main config when not explicitly overridden.

## Leave/Disconnect Rules

Messages configurable for:
- joined before start
- left before start
- left during match
- left while spectator

Recommended runtime rules:
- leaving before start removes player from roster
- leaving during match counts as elimination
- reconnect during running match does not restore active participation
- player remains out of the match

## Team Names

If arena has `N` teams, its config should contain generated defaults:

```yaml
teams:
  1:
    name: team1
  2:
    name: team2
```

Admin may later rename them manually.

## Rollback

Rollback uses a baseline snapshot created once on `/pillars save`.

Rules:
- outside `EDITING`, building in protected arena space is blocked
- baseline remains valid until arena is edited and saved again
- after match, restore:
  - blocks
  - fluids
  - entities spawned during match
  - dropped items
  - temporary structures/effects from gameplay

Natural mob spawns inside match arena:
- disabled

But if mobs are introduced through:
- spawn eggs
- items
- mechanics

then kill/death attribution must still be handled correctly.

## Pre-Game Lobby Rules

The pre-game lobby is part of the overall arena-controlled region.

Rules:
- players stay there while waiting/countdown runs
- players have pre-start protection/invulnerability
- if a player leaves the lobby area before game start, they are teleported back
- the lobby stops being used once players are teleported into cages

## Statistics

The plugin should track at least:
- games played
- games won
- games lost
- kills
- deaths
- optional later: void kills, void deaths, streaks

## Dependencies and Integration

Hard/soft design expectations:
- `SopLocales` for message localization later
- `SopLib` where sharable utilities make sense
- future integration with `SopParty`

Not required for MVP:
- GUI join menus
- proxy-wide routing
- shared global party implementation

## Suggested Commands

Core admin:
- `/pillars create <name> <mode> <teams> <players per team>`
- `/pillars edit <arena>`
- `/pillars save <arena>`
- `/pillars delete <arena>`
- `/pillars pos1`
- `/pillars pos2`
- `/pillars lobbypos1`
- `/pillars lobbypos2`
- `/pillars setspawn <team> <slot>`
- `/pillars setspectator`
- `/pillars setendspawn`
- `/pillars settings`
- `/pillars start <arena>`
- `/pillars stop <arena>`

Player:
- `/pillars join <arena>`
- `/pillars random`
- `/pillars random <mode>[,<mode>...]`
- `/pillars leave`
- `/pillars cosmetics`

## MVP Boundary

Must-have for first playable version:
- arena create/edit/save
- waiting lobby region
- player spawns
- spectator/end spawns
- cages from schematics with 4-way rotation
- team GUI selector
- random/join matchmaking
- party-aware joining logic
- auto-start conditions and countdown
- starting kits
- direct inventory loot
- whitelist/blacklist + custom items
- border shrink
- optional lava rise
- spectator mode on death
- isolated chat
- rollback to saved baseline
- configurable messages and commands
- basic player statistics

Can be deferred:
- advanced cosmetics beyond cages
- shared cross-mode/global party plugin implementation
- ranked modes
- statistics/leaderboards
- proxy-wide routing
