package com.andrewbristowx.emiprogresion.story;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class StoryPlacementStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path path;
    private final Map<String, Placement> placements = new LinkedHashMap<>();
    private long revision;

    public StoryPlacementStore(MinecraftServer server) {
        path = server.getWorldPath(LevelResource.ROOT)
                .resolve("data").resolve("emiprogresion").resolve("kanto_placements.json");
        load();
    }

    public synchronized Optional<Placement> get(String id) {
        return Optional.ofNullable(placements.get(id));
    }

    public synchronized List<Placement> all() {
        return placements.values().stream()
                .sorted(Comparator.comparingLong(entry -> entry.revision))
                .toList();
    }

    public synchronized List<Placement> byKind(String kind) {
        return placements.values().stream().filter(entry -> entry.kind.equals(kind))
                .sorted(Comparator.comparingLong(entry -> entry.revision)).toList();
    }

    public synchronized List<Placement> byCatalogPrefix(String prefix) {
        return placements.values().stream().filter(entry -> entry.catalogId.startsWith(prefix))
                .sorted(Comparator.comparingLong(entry -> entry.revision)).toList();
    }

    public synchronized Placement put(Placement placement) {
        revision++;
        placement.revision = revision;
        placements.put(placement.id, placement);
        save();
        return placement;
    }

    public synchronized void updateEntityUuid(String id, UUID entityUuid) {
        Placement placement = placements.get(id);
        if (placement == null) return;
        placement.entityUuid = entityUuid;
        save();
    }

    public synchronized Optional<Placement> remove(String id) {
        Placement removed = placements.remove(id);
        if (removed != null) save();
        return Optional.ofNullable(removed);
    }

    public synchronized Optional<Placement> last() {
        return placements.values().stream().max(Comparator.comparingLong(entry -> entry.revision));
    }

    public synchronized int nextNumber(String kind) {
        int maximum = 0;
        for (Placement placement : placements.values()) {
            if (!placement.kind.equals(kind)) continue;
            int split = placement.id.lastIndexOf(':');
            if (split < 0) continue;
            try {
                maximum = Math.max(maximum, Integer.parseInt(placement.id.substring(split + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return maximum + 1;
    }

    public synchronized void save() {
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Snapshot snapshot = new Snapshot(1, revision, new ArrayList<>(placements.values()));
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(snapshot, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception exception) {
            EmiProgresion.LOGGER.error("Could not save Kanto placements to {}", path, exception);
        }
    }

    private synchronized void load() {
        if (!Files.exists(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            Snapshot snapshot = GSON.fromJson(reader, Snapshot.class);
            if (snapshot == null || snapshot.placements == null) return;
            revision = Math.max(0L, snapshot.revision);
            for (Placement placement : snapshot.placements) {
                placement.normalize();
                if (!placement.id.isBlank()) placements.put(placement.id, placement);
                revision = Math.max(revision, placement.revision);
            }
        } catch (Exception exception) {
            EmiProgresion.LOGGER.error("Could not load Kanto placements from {}", path, exception);
        }
    }

    private record Snapshot(int version, long revision, List<Placement> placements) {}

    public static final class Placement {
        public String id = "";
        public String kind = "";
        public String catalogId = "";
        public String trainerId = "";
        public String displayName = "";
        public String world = "emiprogresion:kanto";
        public int x;
        public int y;
        public int z;
        public float yaw;
        public UUID entityUuid;
        public long revision;

        public Placement() {}

        public Placement(String id, String kind, String catalogId, String trainerId, String displayName,
                         String world, int x, int y, int z, float yaw) {
            this.id = id;
            this.kind = kind;
            this.catalogId = catalogId;
            this.trainerId = trainerId;
            this.displayName = displayName;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
        }

        public void normalize() {
            if (id == null) id = "";
            if (kind == null) kind = "";
            if (catalogId == null) catalogId = "";
            if (trainerId == null) trainerId = "";
            if (displayName == null) displayName = "";
            if (world == null || world.isBlank()) world = "emiprogresion:kanto";
        }

        public StoryPlacementStore.Placement copyAt(String newWorld, int newX, int newY, int newZ, float newYaw) {
            Placement copy = new Placement(id, kind, catalogId, trainerId, displayName,
                    newWorld, newX, newY, newZ, newYaw);
            copy.entityUuid = entityUuid;
            copy.revision = revision;
            return copy;
        }
    }
}
