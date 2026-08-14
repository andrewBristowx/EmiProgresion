package com.andrewbristowx.emiprogresion.story;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class StoryProgressStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<UUID, PlayerStoryProgress>>() {}.getType();
    private final Path path;
    private final Map<UUID, PlayerStoryProgress> players = new HashMap<>();

    public StoryProgressStore(MinecraftServer server) {
        this.path = server.getWorldPath(LevelResource.ROOT)
                .resolve("data").resolve("emiprogresion").resolve("story_progress.json");
        load();
    }

    public PlayerStoryProgress get(UUID playerId) {
        PlayerStoryProgress progress = players.computeIfAbsent(playerId, ignored -> new PlayerStoryProgress());
        progress.normalize();
        return progress;
    }

    public void save() {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(players, MAP_TYPE, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            EmiProgresion.LOGGER.error("Could not save Kanto story progress to {}", path, exception);
        }
    }

    public void reset(UUID playerId) {
        players.remove(playerId);
        save();
    }

    private void load() {
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            Map<UUID, PlayerStoryProgress> loaded = GSON.fromJson(reader, MAP_TYPE);
            if (loaded != null) {
                loaded.values().forEach(PlayerStoryProgress::normalize);
                players.putAll(loaded);
            }
        } catch (Exception exception) {
            EmiProgresion.LOGGER.error("Could not load Kanto story progress from {}", path, exception);
        }
    }
}
