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
    public int kantoSpawnY = 90;
    public int kantoSpawnZ = 0;

    public int route1EndX = 0;
    public int route1EndZ = 900;
    public int brockX = 0;
    public int brockY = 90;
    public int brockZ = 1250;
    public int brockProtectionRadius = 96;

    public boolean preventBuildingInKanto = true;
    public boolean preventRandomGymsInMainWorld = true;
    public boolean requireBrockBeforeLeavingPrototype = false;

    /** Fill this with the exact Cobbleverse structure id once confirmed from COBBLEVERSE-DP-v31. */
    public String brockStructureId = "";

    /** Optional comma-separated structure-id fragments that are considered gyms in the main world. */
    public String blockedMainWorldGymPatterns = "gym,brock,misty,surge,erika,koga,sabrina,blaine,giovanni";

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
        protectedSpawnRadius = clamp(protectedSpawnRadius, 32, 1000);
        housingRadius = clamp(housingRadius, protectedSpawnRadius, 4000);
        kantoWorldBorderDiameter = clamp(kantoWorldBorderDiameter, 1000, 30000);
        brockProtectionRadius = clamp(brockProtectionRadius, 16, 256);
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
