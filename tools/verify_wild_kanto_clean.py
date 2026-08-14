#!/usr/bin/env python3
"""Verify a cleaned Wild Kanto archive without launching Minecraft."""

from __future__ import annotations

import argparse
import collections
import gzip
import json
import sys
import zipfile
from pathlib import Path

import wild_kanto_cleaner as cleaner


FORBIDDEN_PATH_PARTS = {
    "advancements",
    "cobblemonplayerdata",
    "pokedex",
    "playerdata",
    "pokemon",
    "stats",
}


def block_name_at(chunks: dict[int, bytes], x: int, y: int, z: int) -> str:
    chunk_index = (x // 16 & 31) + 32 * (z // 16 & 31)
    document = cleaner.parse_nbt(chunks[chunk_index])
    section_y = y // 16
    for section in document.get("sections", document.get("Sections", [])):
        if int(section["Y"]) != section_y:
            continue
        block_states = section["block_states"]
        palette = block_states["palette"]
        index = (y & 15) * 256 + (z & 15) * 16 + (x & 15)
        indices, _ = cleaner.decode_palette_indices(block_states.get("data"), len(palette))
        return str(palette[indices[index]]["Name"])
    raise KeyError(f"Missing section for {x} {y} {z}")


def verify(path: Path) -> tuple[dict, list[str]]:
    errors: list[str] = []
    counts = collections.Counter()

    with zipfile.ZipFile(path) as archive:
        bad_crc = archive.testzip()
        if bad_crc:
            errors.append(f"CRC failed: {bad_crc}")

        names = [info.filename for info in archive.infolist() if not info.is_dir()]
        roots = {name.split("/", 1)[0] for name in names}
        if len(roots) != 1:
            errors.append(f"Expected one archive root, found: {sorted(roots)}")
            return dict(counts), errors
        root = next(iter(roots))

        for name in names:
            relative = name[len(root) + 1 :] if name.startswith(root + "/") else name
            parts = {part.lower() for part in relative.split("/")}
            if parts & FORBIDDEN_PATH_PARTS:
                errors.append(f"Forbidden player-data path: {relative}")
            if "distanthorizons" in relative.lower():
                errors.append(f"Distant Horizons file remains: {relative}")

        report_name = f"{root}/EMIPOKEMON-CLEANUP-REPORT.json"
        if report_name not in names:
            errors.append("Cleanup report is missing")
        else:
            report = json.loads(archive.read(report_name))
            counts["reported_files_written"] = int(report["stats"].get("files_written", 0))

        attribution_name = f"{root}/EMIPOKEMON-ATTRIBUTION.txt"
        if attribution_name not in names:
            errors.append("Attribution/license file is missing")
        else:
            attribution = archive.read(attribution_name).decode("utf-8")
            for required in ("RobotJoel", "CC BY-NC-SA 4.0", "planetminecraft.com/project/wild-kanto"):
                if required not in attribution:
                    errors.append(f"Attribution file is missing: {required}")

        level_name = f"{root}/level.dat"
        if level_name not in names:
            errors.append("level.dat is missing")
        else:
            level = cleaner.parse_nbt(gzip.decompress(archive.read(level_name)))
            data = level.get("Data", level)
            spawn = [int(data[key]) for key in ("SpawnX", "SpawnY", "SpawnZ")]
            if spawn != [87, 74, 130]:
                errors.append(f"Unexpected standalone spawn: {spawn}")
            if "Player" in data:
                errors.append("Embedded level.dat Player remains")

        spawn_region_name = f"{root}/region/r.0.0.mca"
        spawn_chunks: dict[int, bytes] = {}

        for info in archive.infolist():
            relative = info.filename[len(root) + 1 :] if info.filename.startswith(root + "/") else info.filename
            match = cleaner.MCA_RE.search("/" + relative)
            if not match:
                continue
            kind = match.group(1)
            region = archive.read(info)
            if len(region) < 8192:
                errors.append(f"Invalid region header: {relative}")
                continue

            for index, raw, _ in cleaner.unpack_region_chunks(region):
                document = cleaner.parse_nbt(raw)
                counts[f"{kind}_chunks"] += 1
                if info.filename == spawn_region_name:
                    spawn_chunks[index] = raw

                if kind == "region":
                    for section in document.get("sections", document.get("Sections", [])):
                        block_states = section.get("block_states")
                        if not block_states:
                            continue
                        names_in_palette = {str(entry.get("Name", "")) for entry in block_states.get("palette", [])}
                        for block_name in names_in_palette:
                            namespace = block_name.split(":", 1)[0] if ":" in block_name else "minecraft"
                            counts[f"palette_namespace:{namespace}"] += 1
                        forbidden = names_in_palette & cleaner.REMOVE_BLOCKS
                        if forbidden:
                            errors.append(f"Forbidden palette entry in {relative} chunk {index}: {sorted(forbidden)}")

                    block_entities = document.get("block_entities", document.get("TileEntities", []))
                    for block_entity in block_entities:
                        block_entity_id = str(block_entity.get("id", "[unknown]"))
                        if block_entity_id in cleaner.REMOVE_BLOCK_ENTITY_IDS:
                            errors.append(f"Forbidden block entity in {relative}: {block_entity_id}")
                else:
                    for entity in document.get("Entities", document.get("entities", [])):
                        entity_id = str(entity.get("id", "[unknown]"))
                        counts[f"entities:{entity_id}"] += 1
                        if entity_id not in cleaner.KEEP_ENTITY_IDS:
                            errors.append(f"Living/transient entity remains in {relative}: {entity_id}")

        if not spawn_chunks:
            errors.append("Spawn region/chunks are missing")
        else:
            expected = {
                (87, 73, 130): "minecraft:stone_bricks",
                (80, 73, 133): "minecraft:mossy_cobblestone",
                (91, 73, 133): "minecraft:grass_block",
            }
            for position, expected_name in expected.items():
                actual = block_name_at(spawn_chunks, *position)
                if actual != expected_name:
                    errors.append(f"Map signature mismatch at {position}: {actual} != {expected_name}")

    return dict(sorted(counts.items())), errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("archive", type=Path)
    args = parser.parse_args()
    if not args.archive.is_file():
        parser.error(f"Archive does not exist: {args.archive}")

    counts, errors = verify(args.archive)
    print(json.dumps({"archive": str(args.archive), "counts": counts, "errors": errors}, indent=2))
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
