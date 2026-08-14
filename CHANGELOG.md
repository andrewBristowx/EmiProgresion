# Changelog

## 0.1.0-alpha.1
- Initializes EmiProgresion as a server-side Fabric 1.21.1 / Cobblemon 1.7.3 mod.
- Separates the normal community/survival world from the `emiprogresion:kanto` adventure dimension.
- Keeps the main spawn plan at a 300-block protected core and a 1000-block nearby housing/community zone.
- Gives Kanto an 8000-block border.
- Adds travel commands between the main world and Kanto.
- Confirms and uses the supplied Cobbleverse ids `cobbleverse:ash`, `cobbleverse:brock`, `pallet_ash`, and `kanto_brock`.
- Adds `/emiprogresion kanto setup` to place the Ash and Brock structures for the prototype.
- Attempts to activate RCT series `kanto` when a player enters the adventure dimension.
- Bundles zero-frequency structure-set overrides for Ash, all eight Kanto gyms, and the Kanto League so story structures can be manually controlled.
- Keeps natural-generation suppression explicitly pending in-game pack-priority validation before any final pregeneration.
