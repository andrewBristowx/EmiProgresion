# Wild Kanto cleaner

This tool creates a new, reversible test archive from the original Wild Kanto
`1-00-02` ZIP. It never edits the source archive.

Verified source SHA-256:
`A155879D262A5EE887F7C99F0895740E6F38A2BF11818903551CD40BB33395AE`.

It removes:

- redstone/command-block Pokemon nest mechanisms;
- pre-existing living and transient entities;
- creator player, Cobblemon, Pokedex, advancement, and stats data;
- local Distant Horizons databases;
- the optional `puddleflood:puddle` block dependency.

It preserves terrain, containers, berry blocks, static item-frame/armor-stand
decoration, map data, and points the standalone test world's spawn at
`87 74 130` near Pallet Town.

It also embeds `EMIPOKEMON-ATTRIBUTION.txt`. Wild Kanto is by RobotJoel and is
licensed under CC BY-NC-SA 4.0; the cleaned derivative stays under that same
license and may not be sold.

## Run

```text
python -m pip install -r tools/requirements-map-cleaner.txt
python tools/wild_kanto_cleaner.py "WildKanto 1-00-02.zip" "WildKanto-EMIPOKEMON-clean-alpha1.zip"
```

The output contains `EMIPOKEMON-CLEANUP-REPORT.json` with exact removal counts.
The cleaned archive is a test artifact and should not be committed to Git.

Verify the completed archive (CRC, map signature, palettes, block entities,
retained static entities, and removed player data):

```text
python tools/verify_wild_kanto_clean.py "WildKanto-EMIPOKEMON-clean-alpha1.zip"
```

The published source ZIP includes nine zero-byte entity-region placeholders.
They contain no chunks, so the cleaner records and omits them. Any non-empty
truncated region still stops the process instead of being silently accepted.
