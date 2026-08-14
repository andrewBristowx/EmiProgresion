package com.andrewbristowx.emiprogresion.config;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EmiProgresionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("emiprogresion.json");
    private static EmiProgresionConfig INSTANCE = new EmiProgresionConfig();

    public String mainWorld = "minecraft:overworld";
    public String kantoWorld = "emiprogresion:kanto";

    public int mainSpawnX = 0;
    public int mainSpawnY = 80;
    public int mainSpawnZ = 0;
    public int protectedSpawnRadius = 300;
    public int housingRadius = 1000;

    public boolean adventurePrototypeEnabled = true;
    public int kantoWorldBorderDiameter = 8000;
    public int kantoSpawnX = 0;
    public int kantoSpawnZ = 0;

    public int route1EndX = 0;
    public int route1EndZ = 900;

    public int ashX = 0;
    public int ashY = 64;
    public int ashZ = 0;
    public String ashStructureId = "cobbleverse:ash";
    public String ashTrainerId = "pallet_ash";

    public int brockX = 0;
    public int brockY = 64;
    public int brockZ = 1250;
    public int brockProtectionRadius = 64;
    public String brockStructureId = "cobbleverse:brock";
    public String brockTrainerId = "kanto_brock";

    public boolean autoSetKantoSeriesOnEnter = true;
    public boolean preventBuildingInKanto = false;
    public boolean preventRandomKantoStoryStructures = true;
    public boolean requireBrockBeforeLeavingPrototype = false;

    public static EmiProgresionConfig get() {
        return INSTANCE;
    }

    public static void load() {
        EmiProgresionConfig loaded = new EmiProgresionConfig();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                EmiProgresionConfig parsed = GSON.fromJson(reader, EmiProgresionConfig.class);
                if (parsed != null) loaded = parsed;
            } catch (Exception exception) {
                EmiProgresion.LOGGER.error("Could not read {}. Using defaults.", CONFIG_PATH, exception);
            }
        }
        loaded.normalize();
        INSTANCE = loaded;
        save();
    }

    public static void reload() {
        load();
    }

    private void normalize() {
        if (mainWorld == null || mainWorld.isBlank()) mainWorld = "minecraft:overworld";
        if (kantoWorld == null || kantoWorld.isBlank()) kantoWorld = "emiprogresion:kanto";
        if (ashStructureId == null || ashStructureId.isBlank()) ashStructureId = "cobbleverse:ash";
        if (brockStructureId == null || brockStructureId.isBlank()) brockStructureId = "cobbleverse:brock";
        if (ashTrainerId == null || ashTrainerId.isBlank()) ashTrainerId = "pallet_ash";
        if (brockTrainerId == null || brockTrainerId.isBlank()) brockTrainerId = "kanto_brock";

        protectedSpawnRadius = clamp(protectedSpawnRadius, 32, 1000);
        housingRadius = clamp(housingRadius, protectedSpawnRadius, 4000);
        kantoWorldBorderDiameter = clamp(kantoWorldBorderDiameter, 1000, 30000);
        brockProtectionRadius = clamp(brockProtectionRadius, 24, 256);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException exception) {
            EmiProgresion.LOGGER.error("Could not write {}", CONFIG_PATH, exception);
        }
    }
}
