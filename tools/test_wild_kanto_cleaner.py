import collections
import unittest

import nbtlib

import wild_kanto_cleaner as cleaner


class PaletteCodecTest(unittest.TestCase):
    def test_padded_round_trip(self):
        for palette_size in (2, 16, 17, 33, 257):
            values = [index % palette_size for index in range(4096)]
            encoded = cleaner.encode_palette_indices(values, palette_size, "padded")
            decoded, mode = cleaner.decode_palette_indices(encoded, palette_size)
            self.assertEqual("padded", mode)
            self.assertEqual(values, decoded)

    def test_continuous_round_trip(self):
        palette_size = 17
        values = [(index * 7) % palette_size for index in range(4096)]
        encoded = cleaner.encode_palette_indices(values, palette_size, "continuous")
        decoded, mode = cleaner.decode_palette_indices(encoded, palette_size)
        self.assertEqual("continuous", mode)
        self.assertEqual(values, decoded)

    def test_empty_entity_region_is_omitted_and_reported(self):
        stats = collections.Counter()
        self.assertIsNone(cleaner.rebuild_region(b"", "entities", stats))
        self.assertEqual(1, stats["empty_region_files_skipped:entities"])

    def test_removed_block_is_absent_from_compacted_palette(self):
        palette = nbtlib.List[nbtlib.Compound]([
            nbtlib.Compound({"Name": nbtlib.String("minecraft:stone")}),
            nbtlib.Compound({"Name": nbtlib.String("puddleflood:puddle")}),
        ])
        indices = [0] * 4096
        indices[123] = 1
        section = nbtlib.Compound({
            "block_states": nbtlib.Compound({
                "palette": palette,
                "data": cleaner.encode_palette_indices(indices, 2, "padded"),
            })
        })
        stats = collections.Counter()
        cleaner.clean_section(section, stats)

        names = [str(entry["Name"]) for entry in section["block_states"]["palette"]]
        self.assertNotIn("puddleflood:puddle", names)
        self.assertEqual(1, stats["blocks:puddleflood:puddle"])
        decoded, _ = cleaner.decode_palette_indices(
            section["block_states"].get("data"), len(section["block_states"]["palette"])
        )
        self.assertEqual("minecraft:air", names[decoded[123]])


if __name__ == "__main__":
    unittest.main()
