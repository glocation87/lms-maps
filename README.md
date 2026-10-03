# LmsMaps: Last Man Standing hub + arena generator (Paper 26.2)

This Paper plugin builds two maps from code. Stand where you want a map, run the command, and the plugin places it a few thousand blocks per tick so the server doesn't freeze.

| Map | Size | What's in it |
|---|---|---|
| **Hub** (`hub.png`) | 53 × 53, floating island | A quartz fountain dais in the centre and 4 walkways to 4 gate alcoves: **PLAY** (north, gold pressure-plate join pad), **KITS** (east), **STATS** (west), **SPECTATE** (south, glass window). Also cherry trees in planters, lamp posts, and a walled rim with lantern pillars. |
| **Arena** (`arena.png`) | 113 × 113 colosseum | A grass battlefield (radius 44) with small hills, a pond and dirt paths. The centre is a raised ruin plateau with 4 staircases, broken columns and a loot chest. It has 2–24 spawn pads facing the centre and seeded cover (ruined walls, boulders, trees, crates), plus a 6-high wall with a railing, 10 rows of stands and a lantern rim. |

## Build & install
```
./gradlew build    # -> build/libs/lms-maps-1.0.0.jar
```
Put the jar in `plugins/` and restart. It needs Java 25 and Paper 26.2.

## Commands (op / `lmsmaps.admin`)
| Command | Does |
|---|---|
| `/lmsmap hub [seed]` | Builds the hub. The floor goes on the block under your feet. |
| `/lmsmap arena [players] [seed]` | Builds the arena with N spawn pads (default 12). A different seed moves the cover and pond. |
| `/lmsmap export <id> [players] [seed]` | Builds an arena in its own void world and exports it as a Nature7 map folder (see below). Works from the console. The seed is random if left out. |
| `/lmsmap tp <hub\|arena>` | Teleports you to the saved spawn. |
| `/lmsmap info` | Shows what's saved. |

**Where to build:** use a void/flat world, or stand high in the air (y≈150). Both maps are floating islands and **clear a cylinder around them** (hub ≈ 29 blocks radius, arena ≈ 58), so building inside normal terrain carves a hole. Keep the two maps at least 150 blocks apart.

## What gets saved (for your game code)
After each build, `config.yml` gets:
```yaml
hub:
  world: lms
  origin: "0,150,0"
  spawns: ["0.5,151.0,15.5,180.0"]          # x,y,z,yaw
  regions: { join_pad: "-1,151,-24,1,152,-22" }
arena:
  world: lms
  origin: "500,150,0"
  radius: "44"                               # use this for a shrinking WorldBorder
  players: "12"
  spawns: [ "...12 entries, each facing centre..." ]
  regions: { center_chest: "500,154,0,500,154,0" }
```
The **join pad** already works. Stepping on the gold PLAY pad runs `join-pad.command` as the player (e.g. `lms join`) and shows `join-pad.message`.

## Exporting maps for Nature7
`/lmsmap export colosseum 12` builds the arena at (0, 64, 0) in a temporary world, saves it, and writes:
```
plugins/Nature7/maps/last_standing/colosseum/
  map.yml        name, authors, spectator-spawn, bounds, game.spawns
  world/region/  the arena's chunks
```
The temporary world is deleted afterwards. Nature7 loads the folder like any other map and never depends on this plugin. Change `export-folder` in `config.yml` to export somewhere else.

## How the code is laid out
- `gen/BlockBuffer`: an in-memory schematic of plain block-state strings (`"quartz_stairs[facing=north]"`), plus spawns, regions and sign text. It has no Bukkit dependency, so the generators can be previewed or tested outside a server.
- `gen/HubGenerator`, `gen/ArenaGenerator`: the designs. Sizes are constants at the top (`R`, `WALL`, `ROWS`, `SPAWN_RING`).
- `gen/Geo`: noise, hashing, facings, and a `Frame` helper for building the same structure on all 4 sides.
- `BuildTask`: places blocks over several ticks in this order: clear → solid blocks bottom-up → fragile blocks (lanterns, plates, water) → sign text.
- `MapExporter`: creates the temporary void world, builds into it, then copies the saved chunks and writes `map.yml`.
- `LmsMapsPlugin`: commands, saving, and the join pad.

Fences, walls and iron bars are pre-connected by `BlockBuffer.connectAll()`, because blocks are placed with physics off.

## Not included (game logic, on purpose)
Queue/countdown, freezing players on pads, filling chest loot, the shrinking border, and elimination/void handling. The arena is a floating island, so falling off the edge means falling into the void. The saved spawns, centre and radius are what those systems need.
