# EmiProgresion

Server-side Fabric progression and region-management mod for the Emipokemon server.

Target stack:
- Minecraft 1.21.1
- Fabric
- Java 21
- Cobblemon 1.7.3

## Alpha 1 — Kanto planning core

The first development version establishes the safe layout/configuration layer before any world is pregenerated.

Default Kanto plan:
- Kanto radius: **4000 blocks** (8000-block diameter).
- Custom spawn/town protected core: **300 blocks**.
- Nearby housing/expansion zone: from the edge of the protected core out to **1000 blocks**.
- Main Kanto routes are intended to begin around/outside the 1000-block housing radius.
- Johto, Hoenn and Sinnoh start disabled.

The 1000 blocks are **not** intended to be fully protected. Only the central spawn/town core is reserved; the outer ring exists so players can build homes near spawn without crowding the custom town.

This alpha deliberately does **not** generate terrain, place gyms, change the vanilla world border, or modify Cobbleverse structures. Those operations will only be enabled after Kanto's layout has been validated.

Commands:
```text
/emiprogresion status
/emiprogresion layout
/emiprogresion setspawn
/emiprogresion validate
/emiprogresion reload
```

`setspawn`, `validate`, and `reload` require permission level 2.

Configuration is generated at `config/emiprogresion.json`.

Development remains isolated on `agent/*` branches until the build is validated in-game.
