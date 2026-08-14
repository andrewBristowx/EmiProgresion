# EmiProgresion

Server-side Fabric progression and region-management mod for the Emipokemon server.

Target stack:
- Minecraft 1.21.1
- Fabric
- Java 21
- Cobblemon 1.7.3
- Cobbleverse datapacks v31 / RCT v20

## 0.1.0-alpha.1 — Kanto prototype through Brock

The main community/survival world and the Pokémon adventure are separated.

### Main world
- Custom spawn core: 300 blocks by default.
- Housing/community zone: up to 1000 blocks by default.
- Kanto story structures are reserved for manual campaign placement rather than random worldgen.

### Kanto adventure world
- Bundled dimension: `emiprogresion:kanto`.
- Overworld-style terrain and 8000-block world border.
- First test route: Ash / initial area → Route 1 → Brock.
- Entering Kanto attempts to set the RCT player series to `kanto` so route trainers can participate in the Kanto series.

Confirmed from the supplied Cobbleverse files:
- Ash structure: `cobbleverse:ash` (43×22×44), trainer `pallet_ash`.
- Brock structure: `cobbleverse:brock` (27×17×24), trainer `kanto_brock`.
- Brock defeat advancement listens for RCT trainer id `kanto_brock`.

### Commands

```text
/emiprogresion status
/emiprogresion layout
/emiprogresion kanto enter
/emiprogresion kanto leave
/emiprogresion kanto setup
/emiprogresion kanto ash tp
/emiprogresion kanto ash place
/emiprogresion kanto brock tp
/emiprogresion kanto brock place
/emiprogresion setspawn
/emiprogresion validate
/emiprogresion reload
```

`setup`, placement/teleport admin helpers, `setspawn`, `validate`, and `reload` require permission level 2.

`/emiprogresion kanto setup` attempts to place the real Cobbleverse Ash and Brock structures into the Kanto dimension at the configured coordinates.

### Natural Kanto gym suppression

The mod bundles structure-set overrides for Ash, all eight Kanto gyms, and the Kanto League with worldgen frequency set to zero. The underlying structure definitions and NBT templates remain untouched, so they can still be deliberately placed in the adventure world.

Because Cobbleverse is loaded as an external datapack on the server, pack-priority behavior must be validated in-game before pregenerating the final world. Test in fresh normal-world chunks with `/locate structure cobbleverse:brock`.

Configuration is generated at `config/emiprogresion.json`.

Development remains isolated on `agent/*` branches until the build is validated in-game.
