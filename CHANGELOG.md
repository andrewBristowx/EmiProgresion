# Changelog

## 0.1.0-alpha.5.1

- Fixes story setup reporting `0/8` when RCT did not preserve the NBT story tag.
- Summons persistent RCT trainers with the documented minimal command, then applies
  story tags, position, rotation, invulnerability and NoAI directly on the entity.
- Detects the new trainer by its RCT trainer ID instead of assuming its tag survived
  the summon command.
- Removes untagged trainers left at story anchors by the alpha.5 setup attempt.
- Reports the exact failed trainer IDs and coordinates in chat and adds detailed
  server-log diagnostics.

## 0.1.0-alpha.5

- Automatically downloads the versioned cleaned Wild Kanto archive on the first
  dedicated-server start; clients do not download the map.
- Verifies the archive with a pinned SHA-256 before extracting any file.
- Extracts only `region`, `entities` and `poi`, with entry-count, size and path-
  traversal protections.
- Runs before Minecraft loads any level, preventing live region-file replacement.
- Recognizes a complete manual import and leaves it untouched.
- Moves old or partial Kanto data into a timestamped, recoverable backup before
  installing the clean dimension.
- Keeps a local-archive fallback for hosts that block outbound GitHub downloads.
- Adds `/emiprogresion kanto installstatus` and installer state to validation.
- Automatically creates the alpha.4 story NPCs after a fresh map installation.
- Adds unit tests plus a full extraction test against the real 764 MB archive.
- Updates configuration to version 5 and enables automatic installation for
  upgraded dedicated servers.

## 0.1.0-alpha.4

- Adds the first per-player Kanto story from Professor Oak through Brock.
- Adds a client dialogue screen with RCT skin portraits, multiple pages, skip,
  choices, next and exit controls.
- Gives one official Kanto starter per player even when Cobblemon's normal starter
  was already selected; a full party is handled by Cobblemon's normal PC path.
- Adds one-time Pallet/Oak gifts and persistent duplicate protection.
- Adds Route 1, Route 2 and Viridian Forest trainer placements using existing RCT
  trainer IDs and unchanged Cobbleverse teams.
- Uses `kanto_brock`; his existing RCT loot table remains the source of the Boulder
  Badge, badge box and TM.
- Records Brock only when his tagged story entity reports that the player defeated
  it inside `emiprogresion:kanto`.
- Adds action-bar distance/direction guidance for the current objective.
- Keeps Giovanni unavailable and Route 3 closed with story dialogue.
- Adds editable map anchors and an idempotent story-NPC setup command.
- Changes the mod to client+server because the dialogue screen is client-rendered;
  progress and rewards remain server-authoritative.
- Detects the absence of Lootr and deliberately leaves all original containers
  untouched. Pasture Loot is not treated as Lootr.

## 0.1.0-alpha.3

- Switches the Kanto prototype to the cleaned Wild Kanto 1-00-02 map.
- Adds a reproducible, source-preserving map cleaner and region-codec tests.
- Embeds RobotJoel attribution and the required CC BY-NC-SA 4.0 same-license notice.
- Compacts chunk palettes so the removed Puddles & Floods block cannot leave a
  hidden mod dependency.
- Uses the real map spawn at `87 74 130` and verifies a three-block map signature.
- Cancels entry when the imported map or safe spawn is missing; no bedrock fallback.
- Changes `/emiprogresion kanto setup` into a non-destructive map/border check.
- Adds `/emiprogresion kanto mapcheck` and map-focused validation output.
- Removes structure-set overrides so Cobbleverse gyms can generate normally in
  the main world as optional, non-campaign content.
- Defines the fully generated source-map rectangle and provisional map center.
- Migrates alpha.2 configuration values to the imported-map coordinates.
- Leaves official campaign rewards explicitly disabled for this visual-test alpha.
- Disables automatic RCT Kanto-series activation during the map-only test.
- Keeps standalone map art intact; production-server map-ID remapping remains a
  later integration step.

## 0.1.0-alpha.2

- Tried heightmap-based safe-surface teleportation and Ash/Brock placement.
- In server testing, the empty prototype dimension still resolved to bedrock-level
  positions; alpha.3 replaces that approach rather than building over it.

## 0.1.0-alpha.1

- Initializes EmiProgresion as a server-side Fabric 1.21.1 / Cobblemon 1.7.3 mod.
- Separates the normal community world from `emiprogresion:kanto`.
- Adds travel, status, layout, configuration, and validation commands.
