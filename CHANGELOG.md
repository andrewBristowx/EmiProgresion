# Changelog

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
