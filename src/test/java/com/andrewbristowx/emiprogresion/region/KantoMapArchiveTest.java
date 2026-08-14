package com.andrewbristowx.emiprogresion.region;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KantoMapArchiveTest {
    @TempDir
    Path temporary;

    @Test
    void extractsOnlyDimensionFoldersAndRequiresPalletRegion() throws Exception {
        Path archive = temporary.resolve("map.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            add(zip, "WildKanto/region/r.0.0.mca", new byte[2048]);
            add(zip, "WildKanto/entities/r.0.0.mca", new byte[]{1});
            add(zip, "WildKanto/poi/r.0.0.mca", new byte[]{2});
            add(zip, "WildKanto/data/map_0.dat", new byte[]{3});
            add(zip, "WildKanto/level.dat", new byte[]{4});
        }

        Path destination = temporary.resolve("dimension");
        KantoMapArchive.ExtractionResult result = KantoMapArchive.extractDimension(archive, destination);

        assertEquals(3, result.entries());
        assertEquals(3, result.counts().size());
        assertTrue(Files.isRegularFile(destination.resolve("region/r.0.0.mca")));
        assertTrue(Files.isRegularFile(destination.resolve("entities/r.0.0.mca")));
        assertTrue(Files.isRegularFile(destination.resolve("poi/r.0.0.mca")));
        assertFalse(Files.exists(destination.resolve("data")));
        assertFalse(Files.exists(destination.resolve("level.dat")));
    }

    @Test
    void rejectsZipTraversalAndCleansPartialExtraction() throws Exception {
        Path archive = temporary.resolve("traversal.zip");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            add(zip, "WildKanto/region/r.0.0.mca", new byte[2048]);
            add(zip, "WildKanto/entities/r.0.0.mca", new byte[]{1});
            add(zip, "WildKanto/poi/../../escape.txt", new byte[]{2});
        }

        Path destination = temporary.resolve("unsafe-dimension");
        assertThrows(IOException.class, () -> KantoMapArchive.extractDimension(archive, destination));
        assertFalse(Files.exists(destination));
        assertFalse(Files.exists(temporary.resolve("escape.txt")));
    }

    @Test
    void calculatesStableUppercaseSha256() throws Exception {
        Path file = temporary.resolve("value.bin");
        Files.writeString(file, "EMIPOKEMON");
        assertEquals("78D714987D0C39F9AAB20B0355B774742587B4BAAF27680E18D03ED7252F8DE6",
                KantoMapArchive.sha256(file));
    }

    @Test
    void installsStagedDimensionAndPreservesThePreviousOne() throws Exception {
        Path world = temporary.resolve("world");
        Path destination = world.resolve("dimensions/emiprogresion/kanto");
        Files.createDirectories(destination.resolve("region"));
        Files.writeString(destination.resolve("region/old.mca"), "old");
        Path staged = temporary.resolve("staged");
        Files.createDirectories(staged.resolve("region"));
        Files.createDirectories(staged.resolve("entities"));
        Files.createDirectories(staged.resolve("poi"));
        Files.writeString(staged.resolve("region/new.mca"), "new");

        Path backup = KantoMapInstaller.installAtomically(world, staged, destination);

        assertTrue(Files.isRegularFile(destination.resolve("region/new.mca")));
        assertFalse(Files.exists(destination.resolve("region/old.mca")));
        assertFalse(Files.exists(staged));
        assertTrue(backup != null && Files.isRegularFile(backup.resolve("region/old.mca")));
    }

    @Test
    void extractsVerifiedProductionArchiveWhenAvailable() throws Exception {
        String configured = System.getenv("EMIPROGRESION_REAL_MAP_ARCHIVE");
        Assumptions.assumeTrue(configured != null && !configured.isBlank());
        Path archive = Path.of(configured);
        assertEquals(KantoMapInstaller.EXPECTED_SHA256, KantoMapArchive.sha256(archive));

        KantoMapArchive.ExtractionResult result = KantoMapArchive.extractDimension(
                archive, temporary.resolve("real-dimension"));
        assertEquals(337, result.entries());
        assertEquals(1_031_774_208L, result.bytes());
        assertEquals(143, result.counts().get("region"));
        assertEquals(62, result.counts().get("entities"));
        assertEquals(132, result.counts().get("poi"));
    }

    private static void add(ZipOutputStream zip, String name, byte[] value) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value);
        zip.closeEntry();
    }
}
