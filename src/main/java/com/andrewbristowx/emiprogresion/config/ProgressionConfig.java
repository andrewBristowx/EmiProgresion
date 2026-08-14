package com.andrewbristowx.emiprogresion.config;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ProgressionConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("emiprogresion.json");
    private static ProgressionConfig INSTANCE = new ProgressionConfig();

    public String activeRegion = "kanto";
    public RegionConfig kanto = RegionConfig.enabled(0, 0, 4000);
    public RegionConfig johto = RegionConfig.disabled();
    public RegionConfig hoenn = RegionConfig.disabled();
    public RegionConfig sinnoh = RegionConfig.disabled();
    public SpawnConfig spawn = new SpawnConfig();

    public static ProgressionConfig get() {
        return INSTANCE;
    }

    public static void load() {
        ProgressionConfig loaded = new ProgressionConfig();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ProgressionConfig parsed = GSON.fromJson(reader, ProgressionConfig.class);
                if (parsed != null) {
                    loaded = parsed;
                }
            } catch (Exception exception) {
                EmiProgresion.LOGGER.error("Could not read {}. Using safe defaults.", CONFIG_PATH, exception);
            }
        }
        loaded.normalize();
        INSTANCE = loaded;
        save();
    }

    public static void reload() {
        load();
    }

    public static void setSpawn(ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        SpawnConfig target = INSTANCE.spawn;
        target.defined = true;
        target.x = pos.getX();
        target.y = pos.getY();
        target.z = pos.getZ();
        target.dimension = player.serverLevel().dimension().location().toString();
        save();
    }

    private void normalize() {
        if (activeRegion == null || activeRegion.isBlank()) activeRegion = "kanto";
        if (kanto == null) kanto = RegionConfig.enabled(0, 0, 4000);
        if (johto == null) johto = RegionConfig.disabled();
        if (hoenn == null) hoenn = RegionConfig.disabled();
        if (sinnoh == null) sinnoh = RegionConfig.disabled();
        if (spawn == null) spawn = new SpawnConfig();

        kanto.normalize(true);
        johto.normalize(false);
        hoenn.normalize(false);
        sinnoh.normalize(false);
        spawn.normalize();
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException exception) {
            EmiProgresion.LOGGER.error("Could not write {}", CONFIG_PATH, exception);
        }
    }

    public static final class RegionConfig {
        public boolean enabled;
        public int centerX;
        public int centerZ;
        public int radius;

        public static RegionConfig enabled(int centerX, int centerZ, int radius) {
            RegionConfig value = new RegionConfig();
            value.enabled = true;
            value.centerX = centerX;
            value.centerZ = centerZ;
            value.radius = radius;
            return value;
        }

        public static RegionConfig disabled() {
            RegionConfig value = new RegionConfig();
            value.enabled = false;
            value.radius = 4000;
            return value;
        }

        private void normalize(boolean shouldDefaultEnabled) {
            radius = Math.max(1000, Math.min(radius <= 0 ? 4000 : radius, 30000));
            if (shouldDefaultEnabled) enabled = true;
        }
    }

    public static final class SpawnConfig {
        public boolean defined = false;
        public String dimension = "minecraft:overworld";
        public int x = 0;
        public int y = 80;
        public int z = 0;

        // Only the custom spawn/town core is intended to be protected.
        public int protectedCoreRadius = 300;

        // From the protected core outward, players can build homes near spawn.
        public int nearbyHousingRadius = 1000;

        private void normalize() {
            if (dimension == null || dimension.isBlank()) dimension = "minecraft:overworld";
            protectedCoreRadius = Math.max(64, Math.min(protectedCoreRadius, 1000));
            nearbyHousingRadius = Math.max(protectedCoreRadius + 64, Math.min(nearbyHousingRadius, 3000));
        }
    }
}
