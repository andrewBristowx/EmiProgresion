# EmiProgresion

Server-side Fabric progression and region-management mod for the EMIPOKEMON server.

Target stack:

- Minecraft 1.21.1
- Fabric / Java 21
- Cobblemon 1.7.3
- Cobbleverse datapacks v31 / RCT v20

## 0.1.0-alpha.3 — cleaned Wild Kanto map test

This alpha replaces the empty noise-world prototype with a map-first test based
on Wild Kanto 1-00-02. Its purpose is to verify terrain, buildings, routes, and
access before story rewards or final progression are implemented.

The source page identifies the map as built for Cobblemon 1.7.2. EMIPOKEMON uses
1.7.3, so the archive and code can be prepared safely, but compatibility is not
considered final until this visual/in-game test passes.

The normal world and the official campaign remain separate:

- Natural Cobbleverse gyms are allowed to generate in the normal world.
- Players may challenge those gyms as optional content.
- They do not represent official EMIPOKEMON story progress or rewards.
- Official campaign validation will only accept events inside
  `emiprogresion:kanto` when that system is added in a later alpha.

Alpha.3 does not yet grant campaign rewards.
It also leaves automatic RCT Kanto-series activation disabled during this map
inspection, preventing story state from leaking back into normal-world gym play.

### Cleaned map archive

`tools/wild_kanto_cleaner.py` creates a new archive without modifying the
creator's original ZIP. It removes command-block/redstone Pokémon nests,
spawners, transient/living entities, creator player data, Distant Horizons
databases, and the optional Puddles & Floods block. It preserves terrain,
containers, Cobblemon blocks, map art, and static decoration.

Known source-map geometry:

- Standalone spawn: `87 74 130`
- Fully generated terrain rectangle: X `-1520..4095`, Z `-2416..1919`
- Provisional center: `1288 -248`
- Provisional vanilla border: 5600 blocks

The square vanilla border is intentionally provisional: the generated map is
rectangular, so the visual test must identify the best final playable limits.

### Recommended first test: standalone world

Extract `WildKanto-EMIPOKEMON-clean-alpha1` into the local Minecraft `saves`
directory and open it with the same Minecraft 1.21.1 Cobbleverse profile used by
EMIPOKEMON. This preserves the map-art files and is the safest way to inspect the
entire build before touching the server world.

### Optional server-dimension preview

Stop the server and make a backup before replacing an earlier test dimension.
From the cleaned archive, copy only these three directories into:

```text
world/dimensions/emiprogresion/kanto/
├── region/
├── entities/
└── poi/
```

Do not copy the standalone world's `level.dat` or datapacks into the server
world. Start the server with alpha.3 and run:

```text
/emiprogresion validate
/emiprogresion kanto enter
```

The entry command verifies three known map blocks and a safe position within 12
blocks of Y=74. If the map is absent or unsafe, teleportation is cancelled. It
never falls back to bedrock.

Important: the standalone `data/map_*.dat` files are deliberately preserved,
but must not be copied blindly into an existing server because map IDs can
collide. Filled-map decorations may therefore look blank or incorrect in this
optional dimension preview. A controlled map-ID import/remap belongs to the next
integration phase after the standalone visual test passes.

### Commands

```text
/emiprogresion status
/emiprogresion layout
/emiprogresion kanto enter
/emiprogresion kanto leave
/emiprogresion kanto setup
/emiprogresion kanto mapcheck
/emiprogresion setspawn
/emiprogresion validate
/emiprogresion reload
```

`setup` now applies the provisional border and validates the imported map; it
does not place or replace any structure. Admin helpers require permission level
2. Configuration is generated at `config/emiprogresion.json`; alpha.2 configs
are migrated to the map coordinates automatically.

### First visual test

Check the spawn/Pallet area, the edge of all four generated bounds, routes,
caves, interiors, doors, containers, map-art frames, and transitions between
regions. Report any terrain seam, missing building, floating block, inaccessible
area, or decoration that depended on a removed entity.

Wild Kanto is by RobotJoel and is licensed under CC BY-NC-SA 4.0. The creator
explicitly permits server use and modified redistribution with credit when
access to the map is not sold. The cleaned ZIP embeds the source link,
attribution, change summary, and same-license notice. Keep that file with every
copy or later derivative.

Development remains isolated on `agent/*` branches until each build is validated
in game.
