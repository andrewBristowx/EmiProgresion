# EmiProgresion

Server-side Fabric progression and region-management mod for the Emipokemon server.

Target stack:
- Minecraft 1.21.1
- Fabric
- Java 21
- Cobblemon 1.7.3

## 0.1.0-alpha.2 — Kanto prototype through Brock

The main survival/community world and the Pokémon adventure are intentionally separated.

### Main world
- Custom spawn core: 300-block protected planning radius by default.
- Housing/community ring: up to 1000 blocks by default.
- Intended to remain free of Cobbleverse random gyms once the exact structure-set integration is finalized.

### Kanto world
- Bundled custom dimension id: `emiprogresion:kanto` using Overworld-style terrain generation.
- 8000-block world border by default.
- Prototype route: initial town → Route 1 → Brock.
- Brock placement is configurable and deliberately waits for the exact structure id from the server's `COBBLEVERSE-DP-v31.zip` instead of guessing.

### Commands
```text
/emiprogresion status
/emiprogresion layout
/emiprogresion kanto enter
/emiprogresion kanto leave
/emiprogresion kanto brock tp
/emiprogresion kanto brock place
/emiprogresion setspawn
/emiprogresion validate
/emiprogresion reload
```

### Important prototype limitation

Alpha.2 creates and manages the separate Kanto dimension and route coordinates, but it does **not** yet suppress Cobbleverse gym structures at worldgen level. To do that safely we need the exact Kanto gym structure/structure-set identifiers from the server's `COBBLEVERSE-DP-v31.zip`. Once those are confirmed, the next revision will override/exclude them from the normal world and keep only the deliberately placed campaign gyms in Kanto.

Development stays on isolated `agent/*` branches. `main` remains the stable baseline.
