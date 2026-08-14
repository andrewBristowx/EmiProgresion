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
    private static final int CURRENT_CONFIG_VERSION = 3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("emiprogresion.json");
    private static EmiProgresionConfig INSTANCE = new EmiProgresionConfig();

    public int configVersion = 0;

    public String mainWorld = "minecraft:overworld";
    public String kantoWorld = "emiprogresion:kanto";

    public int mainSpawnX = 0;
    public int mainSpawnY = 80;
    public int mainSpawnZ = 0;
    public int protectedSpawnRadius = 300;
    public int housingRadius = 1000;

    /** Wild Kanto 1-00-02 cleaned map defaults. */
    public boolean adventurePrototypeEnabled = true;
    public boolean requireWildKantoMap = true;
    public int kantoSpawnX = 87;
    public int kantoSpawnY = 74;
    public int kantoSpawnZ = 130;
    public int kantoBorderCenterX = 1288;
    public int kantoBorderCenterZ = -248;
    public int kantoWorldBorderDiameter = 5600;

    /** Rectangle containing only chunks with full terrain status in the source map. */
    public int kantoGeneratedMinX = -1520;
    public int kantoGeneratedMaxX = 4095;
    public int kantoGeneratedMinZ = -2416;
    public int kantoGeneratedMaxZ = 1919;

    public boolean autoSetKantoSeriesOnEnter = false;
    public boolean preventBuildingInKanto = false;
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

        // Alpha.2 generated coordinates for an empty noise world. Migrate old configs
        // to the imported Wild Kanto map instead of keeping the unsafe Y=-64 fallback.
        if (configVersion < CURRENT_CONFIG_VERSION) {
            requireWildKantoMap = true;
            kantoSpawnX = 87;
            kantoSpawnY = 74;
            kantoSpawnZ = 130;
            kantoBorderCenterX = 1288;
            kantoBorderCenterZ = -248;
            kantoWorldBorderDiameter = 5600;
            kantoGeneratedMinX = -1520;
            kantoGeneratedMaxX = 4095;
            kantoGeneratedMinZ = -2416;
            kantoGeneratedMaxZ = 1919;
            autoSetKantoSeriesOnEnter = false;
            configVersion = CURRENT_CONFIG_VERSION;
        }

        protectedSpawnRadius = clamp(protectedSpawnRadius, 32, 1000);
        housingRadius = clamp(housingRadius, protectedSpawnRadius, 4000);
        kantoSpawnY = clamp(kantoSpawnY, -62, 317);
        kantoWorldBorderDiameter = clamp(kantoWorldBorderDiameter, 1000, 30000);

        if (kantoGeneratedMinX > kantoGeneratedMaxX) {
            int value = kantoGeneratedMinX;
            kantoGeneratedMinX = kantoGeneratedMaxX;
            kantoGeneratedMaxX = value;
        }
        if (kantoGeneratedMinZ > kantoGeneratedMaxZ) {
            int value = kantoGeneratedMinZ;
            kantoGeneratedMinZ = kantoGeneratedMaxZ;
            kantoGeneratedMaxZ = value;
        }
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
