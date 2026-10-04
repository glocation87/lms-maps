# LmsMaps
Map generator for the Nature7 minigame engine. Builds Last Standing arenas from code and exports them straight into Nature7's map folder.

![arena](arena.png)

## Themes
Every theme uses the same layout: a radius 44 battlefield, a raised ruin in the middle with a loot chest, 2 to 24 spawn pads facing the centre, seeded cover (ruined walls, boulders, trees, crates), stands and a lantern rim. Only the blocks change.

| Theme | Look |
| --- | --- |
| `colosseum` | Grass battlefield, stone brick colosseum, water pond, oak and birch |
| `desert` | Sand and red sand, sandstone walls, oasis pond, acacia and jungle trees, cactus |
| `snow` | Snowfield with ice patches, deepslate walls, frozen pond, spruce trees |
| `nether` | Crimson and warped nylium, nether brick walls, lava pond, fungus trees, soul lanterns |

## Commands
Op or `lmsmaps.admin`. `seed` and `theme` both take `random`, and without a theme `default-theme` from `config.yml` is used.

| Command | Does |
| --- | --- |
| `/lmsmap export <id> [players] [seed] [theme]` | Builds an arena in a temporary void world and exports it as a Nature7 map. Works from the console |
| `/lmsmap arena [players] [seed] [theme]` | Builds an arena where you stand |
| `/lmsmap hub [seed]` | Builds the lobby hub where you stand |
| `/lmsmap tp <hub\|arena>` | Teleports to a built map |
| `/lmsmap info` | Shows what's been built |

## Exporting to Nature7
`/lmsmap export dunes 12 random desert` writes:
```
plugins/Nature7/maps/last_standing/dunes/
  map.yml        name, authors, spectator-spawn, bounds, game.spawns
  world/region/  the arena's chunks
```
Nature7 picks the folder up on its next start and adds it to the map vote. The folder format is the only link between the two plugins, so Nature7 never depends on this one. `export-folder` in `config.yml` changes where maps go.

## Building in a world
`hub` and `arena` build on the block under your feet and clear a cylinder around it (hub about 29 blocks, arena about 58), so use a void or flat world, or build high up. Spawns, regions and the arena radius are saved to `config.yml` for game code, and stepping on the hub's gold PLAY pad runs `join-pad.command`.

![hub](hub.png)

## Running it
```
./gradlew build      # build/libs/lms-maps-1.0.0.jar, needs Java 25 and Paper 26.2
./gradlew test
./gradlew runServer  # dev server on port 25568
```

## Code
- `gen/` has no Bukkit dependency: `BlockBuffer` is an in-memory schematic, `ArenaGenerator` and `HubGenerator` fill it, `Theme` is the block palette for each arena theme and `Geo` has the noise and facing helpers
- `BuildTask` places a buffer a few thousand blocks per tick: clear, solid blocks bottom up, then fragile ones (plants, lanterns, fluids), then sign text
- `MapExporter` builds into a void world, copies the chunks out and writes `map.yml`
- Tests pin the colosseum output with a fingerprint so old seeds keep giving the same map, and check every theme's blocks against the real block registry
