#!/usr/bin/env python3
"""Build a reversible, server-safe Wild Kanto test world from the original ZIP.

The source archive is never modified. The cleaner removes the redstone-based
Pokemon nests, transient/living entities, creator player data, and local
Distant Horizons databases while preserving terrain and static decoration.
"""

from __future__ import annotations

import argparse
import collections
import gzip
import io
import json
import math
import re
import struct
import time
import zipfile
import zlib
from pathlib import Path

import nbtlib


MCA_RE = re.compile(r"/(region|entities)/r\.-?\d+\.-?\d+\.mca$")

REMOVE_BLOCKS = {
    "minecraft:command_block",
    "minecraft:chain_command_block",
    "minecraft:repeating_command_block",
    "minecraft:sculk_sensor",
    "minecraft:calibrated_sculk_sensor",
    "minecraft:sculk_catalyst",
    "minecraft:sculk_shrieker",
    "minecraft:comparator",
    "minecraft:hopper",
    "minecraft:mob_spawner",
    "minecraft:spawner",
    "minecraft:redstone_wire",
    "minecraft:repeater",
    "puddleflood:puddle",
}

REMOVE_BLOCK_ENTITY_IDS = {
    "DUMMY",
    "minecraft:command_block",
    "minecraft:sculk_sensor",
    "minecraft:calibrated_sculk_sensor",
    "minecraft:sculk_catalyst",
    "minecraft:sculk_shrieker",
    "minecraft:comparator",
    "minecraft:hopper",
    "minecraft:mob_spawner",
}

KEEP_ENTITY_IDS = {
    "minecraft:armor_stand",
    "minecraft:glow_item_frame",
    "minecraft:item_frame",
    "minecraft:painting",
}

ATTRIBUTION_TEXT = """Wild Kanto 1-00-02 — attribution and license

Original map: Wild Kanto - FREE - Pokemon Region Map for Cobblemon
Creator: RobotJoel
Source: https://www.planetminecraft.com/project/wild-kanto-a-cobblemon-region-map/

The original work is licensed under Creative Commons
Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0):
https://creativecommons.org/licenses/by-nc-sa/4.0/

EMIPOKEMON clean alpha1 is a derivative test version. It removes the custom
redstone/command-block Pokemon nests, pre-existing living entities, creator
player data, Distant Horizons data, and the optional Puddles & Floods block.
Terrain, buildings, containers, map art, and static decoration are preserved.

This derivative is distributed under the same CC BY-NC-SA 4.0 license. Access
to this map may not be sold. RobotJoel is not affiliated with EMIPOKEMON.
"""

SKIP_TOP_LEVEL = {
    "advancements",
    "cobblemonplayerdata",
    "pokedex",
    "playerdata",
    "pokemon",
    "stats",
}

SKIP_EXACT = {
    "level.dat_old",
    "session.lock",
    "data/DistantHorizons.sqlite",
    "data/raids.dat",
    "data/random_sequences.dat",
    "DIM-1/data/DistantHorizons.sqlite",
    "DIM-1/data/raids.dat",
    "DIM1/data/DistantHorizons.sqlite",
    "DIM1/data/raids_end.dat",
}


def parse_nbt(raw: bytes) -> nbtlib.File:
    return nbtlib.File.parse(io.BytesIO(raw))


def write_nbt(document: nbtlib.File) -> bytes:
    stream = io.BytesIO()
    document.write(stream)
    return stream.getvalue()


def signed_long(value: int) -> int:
    value &= (1 << 64) - 1
    return value - (1 << 64) if value >= (1 << 63) else value


def decode_palette_indices(data_tag, palette_size: int) -> tuple[list[int], str]:
    if data_tag is None:
        return [0] * 4096, "single"

    bits = max(4, (palette_size - 1).bit_length())
    mask = (1 << bits) - 1
    longs = [int(value) & ((1 << 64) - 1) for value in data_tag]
    values_per_long = 64 // bits
    padded_size = math.ceil(4096 / values_per_long)

    if len(longs) == padded_size:
        result = []
        for index in range(4096):
            word = longs[index // values_per_long]
            shift = (index % values_per_long) * bits
            result.append((word >> shift) & mask)
        return result, "padded"

    continuous_size = math.ceil(4096 * bits / 64)
    if len(longs) != continuous_size:
        raise ValueError(
            f"Unexpected block-state array length {len(longs)} for palette {palette_size}"
        )

    result = []
    for index in range(4096):
        bit_index = index * bits
        word_index = bit_index // 64
        shift = bit_index % 64
        value = longs[word_index] >> shift
        if shift + bits > 64:
            value |= longs[word_index + 1] << (64 - shift)
        result.append(value & mask)
    return result, "continuous"


def encode_palette_indices(indices: list[int], palette_size: int, mode: str):
    if palette_size == 1:
        return None

    bits = max(4, (palette_size - 1).bit_length())
    mask = (1 << bits) - 1

    if mode in {"single", "padded"}:
        values_per_long = 64 // bits
        longs = [0] * math.ceil(4096 / values_per_long)
        for index, value in enumerate(indices):
            word_index = index // values_per_long
            shift = (index % values_per_long) * bits
            longs[word_index] |= (value & mask) << shift
    else:
        longs = [0] * math.ceil(4096 * bits / 64)
        for index, value in enumerate(indices):
            bit_index = index * bits
            word_index = bit_index // 64
            shift = bit_index % 64
            longs[word_index] |= (value & mask) << shift
            if shift + bits > 64:
                longs[word_index + 1] |= (value & mask) >> (64 - shift)

    return nbtlib.LongArray([signed_long(value) for value in longs])


def clean_section(section, stats: collections.Counter) -> None:
    block_states = section.get("block_states")
    if not block_states:
        return

    palette = block_states.get("palette")
    if not palette:
        return

    target_indices = {
        index
        for index, entry in enumerate(palette)
        if str(entry.get("Name", "")) in REMOVE_BLOCKS
    }
    if not target_indices:
        return

    if len(palette) == 1:
        removed_name = str(palette[0].get("Name", ""))
        palette[0] = nbtlib.Compound({"Name": nbtlib.String("minecraft:air")})
        block_states.pop("data", None)
        stats[f"blocks:{removed_name}"] += 4096
        return

    original_palette_size = len(palette)
    air_index = next(
        (index for index, entry in enumerate(palette) if str(entry.get("Name", "")) == "minecraft:air"),
        None,
    )
    if air_index is None:
        palette.append(nbtlib.Compound({"Name": nbtlib.String("minecraft:air")}))
        air_index = len(palette) - 1

    indices, mode = decode_palette_indices(block_states.get("data"), original_palette_size)
    names = [str(entry.get("Name", "")) for entry in palette]
    for position, palette_index in enumerate(indices):
        if palette_index in target_indices:
            if palette_index < len(names):
                stats[f"blocks:{names[palette_index]}"] += 1
            indices[position] = air_index

    # Remove every now-unused palette entry as well. Minecraft resolves all
    # palette names while loading a section, even when no block index refers to
    # them, so leaving an unused `puddleflood:puddle` entry would retain a hidden
    # mod dependency.
    used_indices = sorted(set(indices))
    compact_index = {old: new for new, old in enumerate(used_indices)}
    compact_palette = nbtlib.List[nbtlib.Compound]([palette[index] for index in used_indices])
    indices = [compact_index[index] for index in indices]
    block_states["palette"] = compact_palette

    encoded = encode_palette_indices(indices, len(compact_palette), mode)
    if encoded is None:
        block_states.pop("data", None)
    else:
        block_states["data"] = encoded


def filter_entity_list(container, key: str, stats: collections.Counter) -> None:
    entities = container.get(key)
    if entities is None:
        return
    kept = []
    for entity in entities:
        entity_id = str(entity.get("id", "[unknown]"))
        if entity_id in KEEP_ENTITY_IDS:
            kept.append(entity)
            stats[f"entities_kept:{entity_id}"] += 1
        else:
            stats[f"entities_removed:{entity_id}"] += 1
    container[key] = nbtlib.List[nbtlib.Compound](kept)


def clean_terrain_chunk(raw: bytes, stats: collections.Counter) -> bytes:
    document = parse_nbt(raw)
    for section in document.get("sections", document.get("Sections", [])):
        clean_section(section, stats)

    block_entities_key = "block_entities" if "block_entities" in document else "TileEntities"
    block_entities = document.get(block_entities_key)
    if block_entities is not None:
        kept = []
        for block_entity in block_entities:
            block_entity_id = str(block_entity.get("id", "[unknown]"))
            if block_entity_id in REMOVE_BLOCK_ENTITY_IDS:
                stats[f"block_entities_removed:{block_entity_id}"] += 1
            else:
                kept.append(block_entity)
        document[block_entities_key] = nbtlib.List[nbtlib.Compound](kept)

    filter_entity_list(document, "Entities", stats)
    filter_entity_list(document, "entities", stats)
    stats["terrain_chunks"] += 1
    return write_nbt(document)


def clean_entity_chunk(raw: bytes, stats: collections.Counter) -> bytes:
    document = parse_nbt(raw)
    filter_entity_list(document, "Entities", stats)
    filter_entity_list(document, "entities", stats)
    stats["entity_chunks"] += 1
    return write_nbt(document)


def unpack_region_chunks(region: bytes):
    if len(region) < 8192:
        raise ValueError("Region file is shorter than its header")
    for index in range(1024):
        location = int.from_bytes(region[index * 4 : index * 4 + 4], "big")
        sector = location >> 8
        sector_count = location & 0xFF
        if not sector or not sector_count:
            continue
        start = sector * 4096
        length = int.from_bytes(region[start : start + 4], "big")
        compression = region[start + 4]
        external = bool(compression & 0x80)
        compression &= 0x7F
        if external:
            raise ValueError("External .mcc chunks are not supported")
        body = region[start + 5 : start + 4 + length]
        if compression == 1:
            raw = gzip.decompress(body)
        elif compression == 2:
            raw = zlib.decompress(body)
        elif compression == 3:
            raw = body
        else:
            raise ValueError(f"Unsupported chunk compression type {compression}")
        timestamp = region[4096 + index * 4 : 4096 + index * 4 + 4]
        yield index, raw, timestamp


def rebuild_region(region: bytes, kind: str, stats: collections.Counter) -> bytes | None:
    # The published ZIP contains nine zero-byte entity region placeholders.
    # They contain no chunks and are safer to omit than to copy as invalid MCA
    # files. Non-empty truncated regions still fail loudly in the parser.
    if not region:
        stats[f"empty_region_files_skipped:{kind}"] += 1
        return None

    locations = bytearray(4096)
    timestamps = bytearray(4096)
    payload = bytearray(8192)
    next_sector = 2

    for index, raw, timestamp in unpack_region_chunks(region):
        if kind == "region":
            cleaned = clean_terrain_chunk(raw, stats)
        else:
            cleaned = clean_entity_chunk(raw, stats)
        compressed = zlib.compress(cleaned, level=6)
        chunk_payload = bytes([2]) + compressed
        length = len(chunk_payload)
        record = length.to_bytes(4, "big") + chunk_payload
        sectors = math.ceil(len(record) / 4096)
        if sectors > 255:
            raise ValueError(f"Chunk {index} needs {sectors} sectors")

        locations[index * 4 : index * 4 + 4] = ((next_sector << 8) | sectors).to_bytes(4, "big")
        timestamps[index * 4 : index * 4 + 4] = timestamp
        required = (next_sector + sectors) * 4096
        if len(payload) < required:
            payload.extend(b"\x00" * (required - len(payload)))
        start = next_sector * 4096
        payload[start : start + len(record)] = record
        next_sector += sectors

    payload[0:4096] = locations
    payload[4096:8192] = timestamps
    return bytes(payload)


def clean_level_dat(raw: bytes) -> bytes:
    document = parse_nbt(gzip.decompress(raw))
    data = document.get("Data", document)
    data.pop("Player", None)
    data["LevelName"] = nbtlib.String("WildKanto EMIPOKEMON clean alpha1")
    data["SpawnX"] = nbtlib.Int(87)
    data["SpawnY"] = nbtlib.Int(74)
    data["SpawnZ"] = nbtlib.Int(130)
    data_packs = data.get("DataPacks")
    if data_packs is not None:
        data_packs["Enabled"] = nbtlib.List[nbtlib.String]([
            nbtlib.String("vanilla"),
            nbtlib.String("fabric"),
            nbtlib.String("cobblemon"),
        ])
        data_packs["Disabled"] = nbtlib.List[nbtlib.String]([])
    return gzip.compress(write_nbt(document), compresslevel=6)


def should_skip(relative: str) -> bool:
    if relative in SKIP_EXACT:
        return True
    top = relative.split("/", 1)[0]
    return top in SKIP_TOP_LEVEL


def build_clean_archive(source: Path, output: Path) -> dict:
    stats = collections.Counter()
    started = time.time()
    output.parent.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(source, "r") as source_zip:
        bad_entry = source_zip.testzip()
        if bad_entry:
            raise ValueError(f"Source ZIP failed CRC validation at {bad_entry}")

        source_root = source_zip.infolist()[0].filename.split("/", 1)[0]
        output_root = "WildKanto-EMIPOKEMON-clean-alpha1"

        with zipfile.ZipFile(
            output,
            "w",
            compression=zipfile.ZIP_DEFLATED,
            compresslevel=6,
            allowZip64=True,
        ) as output_zip:
            for position, info in enumerate(source_zip.infolist(), 1):
                relative = info.filename[len(source_root) + 1 :] if info.filename.startswith(source_root + "/") else info.filename
                if not relative:
                    continue
                if should_skip(relative):
                    stats["files_skipped"] += 1
                    stats["bytes_skipped"] += info.file_size
                    continue

                output_name = f"{output_root}/{relative}"
                if info.is_dir():
                    output_zip.writestr(output_name, b"")
                    continue

                raw = source_zip.read(info)
                match = MCA_RE.search("/" + relative)
                if match:
                    raw = rebuild_region(raw, match.group(1), stats)
                    if raw is None:
                        stats["files_skipped"] += 1
                        continue
                    stats[f"region_files:{match.group(1)}"] += 1
                elif relative == "level.dat":
                    raw = clean_level_dat(raw)
                output_zip.writestr(output_name, raw)
                stats["files_written"] += 1

                if position % 20 == 0:
                    print(
                        f"[{position}/{len(source_zip.infolist())}] {relative} | "
                        f"chunks={stats['terrain_chunks'] + stats['entity_chunks']}",
                        flush=True,
                    )

            report = {
                "source": str(source),
                "output": str(output),
                "elapsed_seconds": round(time.time() - started, 2),
                "stats": dict(sorted(stats.items())),
                "removed_blocks": sorted(REMOVE_BLOCKS),
                "kept_entity_ids": sorted(KEEP_ENTITY_IDS),
                "spawn": [87, 74, 130],
                "fully_generated_block_bounds": [-1520, 4095, -2416, 1919],
                "recommended_kanto_center": [1288, -248],
            }
            output_zip.writestr(
                f"{output_root}/EMIPOKEMON-CLEANUP-REPORT.json",
                json.dumps(report, ensure_ascii=False, indent=2).encode("utf-8"),
            )
            output_zip.writestr(
                f"{output_root}/EMIPOKEMON-ATTRIBUTION.txt",
                ATTRIBUTION_TEXT.encode("utf-8"),
            )

    report["output_size_bytes"] = output.stat().st_size
    report["elapsed_seconds"] = round(time.time() - started, 2)
    return report


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path, help="Original Wild Kanto ZIP")
    parser.add_argument("output", type=Path, help="Destination cleaned ZIP")
    args = parser.parse_args()

    if not args.source.is_file():
        parser.error(f"Source does not exist: {args.source}")
    if args.source.resolve() == args.output.resolve():
        parser.error("Source and output must be different files")
    if args.output.exists():
        parser.error(f"Output already exists: {args.output}")

    report = build_clean_archive(args.source, args.output)
    print(json.dumps(report, ensure_ascii=False, indent=2), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
