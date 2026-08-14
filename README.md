# EmiProgresion

Fabric progression, dialogue and region-management mod for the EMIPOKEMON server.

Target stack:

- Minecraft 1.21.1
- Fabric / Java 21
- Cobblemon 1.7.3
- Cobbleverse datapacks v31 / RCT v20

## 0.1.0-alpha.5.4 — invalid legacy Brock cleanup

Alpha.5.4 removes only RCT trainer entities at the abandoned `0,64,1250`
prototype anchor, even when RCT has replaced their invalid early-load trainer ID
with `default`, and unregisters their saved RCT tickets. Brock is then staged at
that known-compatible anchor, moved immediately into the Wild Kanto Pewter Gym,
and made persistent only after reaching the correct gym chunk.

## 0.1.0-alpha.5.3 — legacy Brock cleanup

Alpha.5.3 removes persistent Brock entities left at the obsolete prototype
anchor before placing the gym leader. It also uses a direct RCT entity summon as
a fallback when RCT's `summon_persistent` command produces no entity.

## 0.1.0-alpha.5.2 — RCT startup and Brock anchor hotfix

Alpha.5.2 waits for RCT's trainer registry before creating the story NPCs. It
also migrates the obsolete Brock anchor `0,64,1250` to the Wild Kanto Pewter Gym
at `22,127,-1294` and automatically performs one clean rebuild after upgrading.

## 0.1.0-alpha.5.1 — RCT story NPC hotfix

Alpha.5.1 fixes `NPCs de historia: 0/8`. RCT trainers are now located by their
trainer ID after summoning and EmiProgresion applies its story tag and stationary
properties directly. Running `/emiprogresion story setup` also removes any
untagged duplicate left at the eight configured anchors by alpha.5.

## 0.1.0-alpha.5 — automatic Wild Kanto server installation

Alpha.5 removes the need to copy map folders by hand. On the first dedicated-
server start, before any Minecraft levels are loaded, the mod:

1. detects whether a complete Wild Kanto installation already exists;
2. downloads the versioned 764 MB cleaned archive over HTTPS only when needed;
3. verifies the exact SHA-256 before opening it;
4. extracts only the 337 files inside `region`, `entities` and `poi`;
5. moves an older or partial Kanto dimension into a recoverable timestamped backup;
6. installs the staged dimension and writes an attribution/version marker; and
7. automatically prepares the story NPCs after the server finishes loading.

The client never downloads the map. Players still need the alpha.5 JAR and the
RCT resource pack because the dialogue interface and portraits are client-side.

The first server start can take several minutes and needs roughly 2 GB of free
working space. Installation progress is printed to the server console. If the
download fails, the normal world continues to start and Kanto remains locked;
the next restart retries without teleporting anyone to an unsafe position.

Useful operator commands:

```text
/emiprogresion kanto installstatus
/emiprogresion validate
/emiprogresion kanto enter
```

No existing complete manual import is overwritten. Partial/old dimension data is
moved to `world/emiprogresion-backups/kanto-before-auto-<date>/`. Servers whose
host blocks GitHub downloads can place the verified archive at
`config/emiprogresion/WildKanto-EMIPOKEMON-clean-alpha1.zip` and restart; the mod
will verify and install that local file automatically.

The download URL, expected SHA-256 and automatic-install switch are stored in
`config/emiprogresion.json`. The default archive is the cleaned derivative of
[Wild Kanto by RobotJoel](https://www.planetminecraft.com/project/wild-kanto-a-cobblemon-region-map/),
distributed with attribution under CC BY-NC-SA 4.0 and never sold separately.

## 0.1.0-alpha.4 — first playable story through Brock

Alpha.4 turns the validated Wild Kanto copy into the first playable campaign
slice. Install the JAR on both the server and every client: the server remains
authoritative for progress and rewards, while the client renders the Pokémon-style
dialogue screen.

Required content stays external and is not redistributed by this project:

- Cobblemon 1.7.3
- Radical Cobblemon Trainers API 0.15.2-beta
- Radical Cobblemon Trainers 0.18.1-beta
- COBBLEVERSE-RCT-DP-v20
- `COBBLEVERSE RCTmod RP.zip` enabled on clients (NPC portraits)

The official sequence is stored independently for every player:

1. Talk to Professor Oak and choose Bulbasaur, Charmander or Squirtle once.
2. Receive an optional one-time Potion in Pueblo Paleta.
3. Follow Route 1 to collect Oak's parcel in Ciudad Verde.
4. Return the parcel to Oak and receive five Poké Balls once.
5. Cross Route 2 and Bosque Verde, including stationary RCT route trainers.
6. Challenge the existing Cobbleverse trainer `kanto_brock`.
7. After the RCT victory grants its normal Boulder Badge, badge box and TM,
   the story records completion and closes Route 3 until the next version.

The dialogue window includes the NPC face from the enabled RCT resource pack,
page navigation, a skip button that jumps only to the important choice, explicit
choices and an exit button. The server validates every selection, so reconnecting
or resending a packet cannot duplicate starters or gifts.

Giovanni's gym is present in the first city but remains closed for this story
stage. Natural Cobbleverse gyms in the normal world remain optional and never
advance this campaign.

### Story setup and test

With alpha.5, start the dedicated server and wait for the automatic map installer.
After it reports completion, run:

```text
/emiprogresion validate
/emiprogresion kanto enter
/emiprogresion story objective
```

Story NPCs are prepared automatically after a successful first installation.
`story setup` remains available for corrections and removes/recreates only entities
tagged as EmiProgresion story NPCs. It does not replace buildings, terrain or
normal Cobbleverse trainers.
The default anchors were read from Wild Kanto 1-00-02. If an interior needs a
small correction, stand on the desired block and use one of:

```text
/emiprogresion story setanchor oak
/emiprogresion story setanchor pallet_guide
/emiprogresion story setanchor viridian_courier
/emiprogresion story setanchor giovanni_gate
/emiprogresion story setanchor brock
/emiprogresion story setanchor route3_gate
```

Then run `story setup` again. For a clean per-player retest, an operator can use
`/emiprogresion story reset`.

### Lootr safety

The inspected Cobbleverse profile contains Pasture Loot, but no independent
Lootr mod. Those are different mods. Alpha.4 therefore does not transform any
container and `/emiprogresion lootcheck` reports the missing dependency. This is
intentional: replacing pre-filled map chests before proving that every exact item
is preserved could erase unique leader rewards. Lootr conversion remains a
separate migration on another map copy after a compatible Fabric 1.21.1 build is
installed and tested.

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
  `emiprogresion:kanto`.

Alpha.3 itself does not grant campaign rewards.
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
/emiprogresion story status
/emiprogresion story objective
/emiprogresion story setup
/emiprogresion story reset
/emiprogresion lootcheck
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
