package com.andrewbristowx.emiprogresion.story;

import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import com.andrewbristowx.emiprogresion.region.AdventureRegionService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class StoryPlacementService {
    private static final Set<UUID> ADMIN_MODE = new HashSet<>();
    private static StoryPlacementStore store;

    private StoryPlacementService() {}

    public static void onServerStarted(MinecraftServer server) {
        store = new StoryPlacementStore(server);
        migrateLegacyAnchors();
    }

    public static void onServerStopping() {
        if (store != null) store.save();
        ADMIN_MODE.clear();
        store = null;
    }

    public static StoryPlacementStore store() {
        if (store == null) throw new IllegalStateException("Kanto placement store is not loaded");
        return store;
    }

    public static boolean isAdminMode(ServerPlayer player) {
        return ADMIN_MODE.contains(player.getUUID());
    }

    public static boolean setAdminMode(ServerPlayer player, boolean enabled) {
        if (enabled) ADMIN_MODE.add(player.getUUID());
        else ADMIN_MODE.remove(player.getUUID());
        return enabled;
    }

    public static PlacementResult placeNpc(ServerPlayer player, String requestedId) {
        String id = KantoStoryCatalog.normalize(requestedId);
        Optional<KantoStoryCatalog.NpcDefinition> definition = KantoStoryCatalog.npc(id);
        if (definition.isEmpty()) return PlacementResult.failure("NPC desconocido: " + requestedId);
        KantoStoryCatalog.NpcDefinition npc = definition.get();
        return placeEntity(player, "npc:" + npc.id(), "npc", npc.id(), npc.trainerId(), npc.name());
    }

    public static PlacementResult placeNextRouteTrainer(ServerPlayer player, String requestedZone) {
        String zone = KantoStoryCatalog.normalize(requestedZone);
        List<KantoStoryCatalog.TrainerDefinition> definitions = KantoStoryCatalog.route(zone);
        if (definitions.isEmpty()) return PlacementResult.failure("Ruta desconocida: " + requestedZone);
        for (KantoStoryCatalog.TrainerDefinition definition : definitions) {
            String placementId = "trainer:" + definition.id();
            if (store().get(placementId).isEmpty()) {
                return placeEntity(player, placementId, "trainer", definition.id(), definition.trainerId(), "Entrenador de " + readable(zone));
            }
        }
        return PlacementResult.failure("Ya colocaste los " + definitions.size() + " entrenadores preparados para " + readable(zone) + ".");
    }

    public static PlacementResult placeNextGymTrainer(ServerPlayer player, int gymNumber) {
        Optional<KantoStoryCatalog.GymDefinition> found = KantoStoryCatalog.gym(gymNumber);
        if (found.isEmpty()) return PlacementResult.failure("El gimnasio debe estar entre 1 y 8.");
        for (KantoStoryCatalog.TrainerDefinition trainer : found.get().trainers()) {
            String placementId = "trainer:" + trainer.id();
            if (store().get(placementId).isEmpty()) {
                return placeEntity(player, placementId, "trainer", trainer.id(), trainer.trainerId(),
                        "Entrenador del gimnasio de " + found.get().leaderName());
            }
        }
        return PlacementResult.failure("Todos los entrenadores del gimnasio " + gymNumber + " ya están colocados.");
    }

    public static PlacementResult placeGymLeader(ServerPlayer player, int gymNumber) {
        Optional<KantoStoryCatalog.GymDefinition> found = KantoStoryCatalog.gym(gymNumber);
        if (found.isEmpty()) return PlacementResult.failure("El gimnasio debe estar entre 1 y 8.");
        KantoStoryCatalog.GymDefinition gym = found.get();
        return placeEntity(player, "leader:" + gymNumber, "leader", Integer.toString(gymNumber),
                gym.leaderTrainerId(), gym.leaderName());
    }

    public static PlacementResult placeGymGate(ServerPlayer player, int gymNumber) {
        Optional<KantoStoryCatalog.GymDefinition> found = KantoStoryCatalog.gym(gymNumber);
        if (found.isEmpty()) return PlacementResult.failure("El gimnasio debe estar entre 1 y 8.");
        return placeMarker(player, "gate:gym:" + gymNumber, "gate", "gym:" + gymNumber,
                "Entrada del gimnasio de " + found.get().leaderName());
    }

    public static PlacementResult placeBoss(ServerPlayer player, String requestedId) {
        String id = KantoStoryCatalog.normalize(requestedId);
        Optional<KantoStoryCatalog.BossDefinition> found = KantoStoryCatalog.boss(id);
        if (found.isEmpty()) return PlacementResult.failure("Jefe desconocido: " + requestedId);
        KantoStoryCatalog.BossDefinition boss = found.get();
        return placeEntity(player, "boss:" + boss.id(), "boss", boss.id(), boss.trainerId(), boss.name());
    }

    public static PlacementResult placeLeague(ServerPlayer player, int number) {
        Optional<KantoStoryCatalog.LeagueDefinition> found = KantoStoryCatalog.league(number);
        if (found.isEmpty()) return PlacementResult.failure("Usa Alto Mando 1-4 o Campeón 5.");
        KantoStoryCatalog.LeagueDefinition league = found.get();
        String kind = league.champion() ? "champion" : "league";
        return placeEntity(player, "league:" + number, kind, Integer.toString(number), league.trainerId(), league.name());
    }

    public static PlacementResult placePokestop(ServerPlayer player) {
        int number = store().nextNumber("pokestop");
        String id = "pokestop:" + number;
        PlacementResult result = placeMarker(player, id, "pokestop", Integer.toString(number), "Poképarada " + number);
        if (result.success()) StoryService.placeTravelBlock(player.serverLevel(), result.placement(), false);
        return result;
    }

    public static PlacementResult placeTerminal(ServerPlayer player, String requestedName) {
        String displayName = requestedName == null || requestedName.isBlank() ? "Terminal sin nombre" : requestedName.trim();
        String slug = KantoStoryCatalog.normalize(displayName).replaceAll("[^a-z0-9_]", "");
        if (slug.isBlank()) slug = "terminal_" + store().nextNumber("terminal");
        PlacementResult result = placeMarker(player, "terminal:" + slug, "terminal", slug, displayName);
        if (result.success()) StoryService.placeTravelBlock(player.serverLevel(), result.placement(), true);
        return result;
    }

    public static PlacementResult moveNearest(ServerPlayer player) {
        Optional<StoryPlacementStore.Placement> nearest = nearest(player, 8.0D);
        if (nearest.isEmpty()) return PlacementResult.failure("No hay ningún elemento de historia a menos de 8 bloques.");
        StoryPlacementStore.Placement old = nearest.get();
        removeWorldObject(player.serverLevel(), old);
        StoryPlacementStore.Placement moved = old.copyAt(player.serverLevel().dimension().location().toString(),
                player.getBlockX(), player.getBlockY(), player.getBlockZ(), player.getYRot());
        moved.entityUuid = null;
        store().put(moved);
        if (isEntityKind(moved.kind)) StoryService.spawnPlacementEntity(player.serverLevel(), moved);
        else if (moved.kind.equals("pokestop") || moved.kind.equals("terminal")) {
            StoryService.placeTravelBlock(player.serverLevel(), moved, moved.kind.equals("terminal"));
        }
        return PlacementResult.success("Movido: " + moved.displayName, moved);
    }

    public static PlacementResult removeNearest(ServerPlayer player) {
        Optional<StoryPlacementStore.Placement> nearest = nearest(player, 8.0D);
        if (nearest.isEmpty()) return PlacementResult.failure("No hay ningún elemento de historia a menos de 8 bloques.");
        StoryPlacementStore.Placement placement = nearest.get();
        removeWorldObject(player.serverLevel(), placement);
        store().remove(placement.id);
        return PlacementResult.success("Eliminado: " + placement.displayName, placement);
    }

    public static PlacementResult undo(ServerPlayer player) {
        Optional<StoryPlacementStore.Placement> last = store().last();
        if (last.isEmpty()) return PlacementResult.failure("Todavía no hay colocaciones para deshacer.");
        StoryPlacementStore.Placement placement = last.get();
        ServerLevel level = AdventureRegionService.getLevel(player.server, placement.world);
        if (level != null) removeWorldObject(level, placement);
        store().remove(placement.id);
        return PlacementResult.success("Deshecho: " + placement.displayName, placement);
    }

    public static SetupResult setupPlacedEntities(MinecraftServer server) {
        int expected = 0;
        int prepared = 0;
        List<String> failures = new ArrayList<>();
        for (StoryPlacementStore.Placement placement : store().all()) {
            ServerLevel level = AdventureRegionService.getLevel(server, placement.world);
            if (isEntityKind(placement.kind)) {
                expected++;
                if (level == null || !StoryService.spawnPlacementEntity(level, placement)) failures.add(placement.id);
                else prepared++;
            } else if (placement.kind.equals("pokestop") || placement.kind.equals("terminal")) {
                expected++;
                if (level == null) failures.add(placement.id);
                else {
                    StoryService.placeTravelBlock(level, placement, placement.kind.equals("terminal"));
                    prepared++;
                }
            }
        }
        return new SetupResult(prepared, expected, failures);
    }

    public static Validation validate(MinecraftServer server) {
        List<String> missing = new ArrayList<>();
        List<String> invalidTrainers = new ArrayList<>();
        for (KantoStoryCatalog.NpcDefinition npc : KantoStoryCatalog.npcs()) {
            if (store().get("npc:" + npc.id()).isEmpty()) missing.add("npc " + npc.id());
            if (!StoryService.rctTrainerAvailable(npc.trainerId())) invalidTrainers.add(npc.trainerId());
        }
        for (KantoStoryCatalog.GymDefinition gym : KantoStoryCatalog.gyms()) {
            if (store().get("leader:" + gym.number()).isEmpty()) missing.add("líder gimnasio " + gym.number());
            if (store().get("gate:gym:" + gym.number()).isEmpty()) missing.add("entrada gimnasio " + gym.number());
            if (!StoryService.rctTrainerAvailable(gym.leaderTrainerId())) invalidTrainers.add(gym.leaderTrainerId());
            for (KantoStoryCatalog.TrainerDefinition trainer : gym.trainers()) {
                if (store().get("trainer:" + trainer.id()).isEmpty()) missing.add("entrenador " + trainer.id());
                if (!StoryService.rctTrainerAvailable(trainer.trainerId())) invalidTrainers.add(trainer.trainerId());
            }
        }
        for (String route : KantoStoryCatalog.routeNames()) {
            for (KantoStoryCatalog.TrainerDefinition trainer : KantoStoryCatalog.route(route)) {
                if (store().get("trainer:" + trainer.id()).isEmpty()) missing.add("entrenador " + trainer.id());
                if (!StoryService.rctTrainerAvailable(trainer.trainerId())) invalidTrainers.add(trainer.trainerId());
            }
        }
        for (KantoStoryCatalog.BossDefinition boss : KantoStoryCatalog.bosses()) {
            if (store().get("boss:" + boss.id()).isEmpty()) missing.add("jefe " + boss.id());
            if (!StoryService.rctTrainerAvailable(boss.trainerId())) invalidTrainers.add(boss.trainerId());
        }
        for (KantoStoryCatalog.LeagueDefinition member : KantoStoryCatalog.league()) {
            if (store().get("league:" + member.number()).isEmpty()) missing.add(member.champion() ? "campeón" : "alto mando " + member.number());
            if (!StoryService.rctTrainerAvailable(member.trainerId())) invalidTrainers.add(member.trainerId());
        }
        for (StoryPlacementStore.Placement placement : store().all()) {
            if (!placement.trainerId.isBlank() && !StoryService.rctTrainerAvailable(placement.trainerId)) {
                invalidTrainers.add(placement.trainerId);
            }
        }
        return new Validation(store().all().size(), List.copyOf(missing), invalidTrainers.stream().distinct().toList());
    }

    public static List<StoryPlacementStore.Placement> all() {
        return store().all();
    }

    public static Optional<StoryPlacementStore.Placement> findByEntity(UUID uuid) {
        if (uuid == null || store == null) return Optional.empty();
        return store().all().stream().filter(entry -> uuid.equals(entry.entityUuid)).findFirst();
    }

    public static Optional<StoryPlacementStore.Placement> find(String id) {
        return store == null ? Optional.empty() : store.get(id);
    }

    public static List<StoryPlacementStore.Placement> travelTerminals() {
        return store().byKind("terminal");
    }

    public static Optional<StoryPlacementStore.Placement> findAt(ServerLevel level, net.minecraft.core.BlockPos pos) {
        String world = level.dimension().location().toString();
        return store().all().stream().filter(entry -> entry.world.equals(world)
                        && entry.x == pos.getX() && entry.y == pos.getY() && entry.z == pos.getZ())
                .findFirst();
    }

    private static PlacementResult placeEntity(ServerPlayer player, String placementId, String kind,
                                               String catalogId, String trainerId, String displayName) {
        if (!inKanto(player)) return PlacementResult.failure("Debes estar dentro del mundo de Kanto.");
        store().get(placementId).ifPresent(old -> removeWorldObject(player.serverLevel(), old));
        StoryPlacementStore.Placement placement = atPlayer(player, placementId, kind, catalogId, trainerId, displayName);
        store().put(placement);
        if (!StoryService.spawnPlacementEntity(player.serverLevel(), placement)) {
            return PlacementResult.failure("La posición fue guardada, pero RCT no pudo crear " + displayName + ". Revisa /emiprogresion montaje validar.");
        }
        return PlacementResult.success("Colocado: " + displayName, placement);
    }

    private static PlacementResult placeMarker(ServerPlayer player, String placementId, String kind,
                                               String catalogId, String displayName) {
        if (!inKanto(player)) return PlacementResult.failure("Debes estar dentro del mundo de Kanto.");
        store().get(placementId).ifPresent(old -> removeWorldObject(player.serverLevel(), old));
        StoryPlacementStore.Placement placement = atPlayer(player, placementId, kind, catalogId, "", displayName);
        store().put(placement);
        return PlacementResult.success("Guardado: " + displayName, placement);
    }

    private static StoryPlacementStore.Placement atPlayer(ServerPlayer player, String id, String kind,
                                                           String catalogId, String trainerId, String name) {
        return new StoryPlacementStore.Placement(id, kind, catalogId, trainerId, name,
                player.serverLevel().dimension().location().toString(), player.getBlockX(), player.getBlockY(),
                player.getBlockZ(), player.getYRot());
    }

    private static Optional<StoryPlacementStore.Placement> nearest(ServerPlayer player, double maxDistance) {
        double maxDistanceSquared = maxDistance * maxDistance;
        return store().all().stream()
                .filter(entry -> entry.world.equals(player.serverLevel().dimension().location().toString()))
                .filter(entry -> distanceSquared(player, entry) <= maxDistanceSquared)
                .min(Comparator.comparingDouble(entry -> distanceSquared(player, entry)));
    }

    private static double distanceSquared(ServerPlayer player, StoryPlacementStore.Placement placement) {
        double dx = player.getX() - (placement.x + 0.5D);
        double dy = player.getY() - placement.y;
        double dz = player.getZ() - (placement.z + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private static void removeWorldObject(ServerLevel level, StoryPlacementStore.Placement placement) {
        if (isEntityKind(placement.kind)) StoryService.removePlacementEntity(level, placement);
        else if (placement.kind.equals("pokestop") || placement.kind.equals("terminal")) {
            StoryService.removeTravelBlock(level, placement);
        }
    }

    private static boolean isEntityKind(String kind) {
        return switch (kind) {
            case "npc", "trainer", "leader", "boss", "league", "champion" -> true;
            default -> false;
        };
    }

    private static boolean inKanto(ServerPlayer player) {
        return AdventureRegionService.isKanto(player.serverLevel());
    }

    private static String readable(String value) {
        String text = value.replace('_', ' ');
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    private static void migrateLegacyAnchors() {
        if (!store().all().isEmpty()) return;
        EmiProgresionConfig config = EmiProgresionConfig.get();
        String world = config.kantoWorld;
        store().put(new StoryPlacementStore.Placement("npc:oak", "npc", "oak", "professor_oak_00c8",
                "Profesor Oak", world, config.oakX, config.oakY, config.oakZ, 180f));
        store().put(new StoryPlacementStore.Placement("npc:vecino_paleta", "npc", "vecino_paleta", "youngster_lancere_0025",
                "Vecino de Pueblo Paleta", world, config.palletGuideX, config.palletGuideY, config.palletGuideZ, 0f));
        store().put(new StoryPlacementStore.Placement("npc:dependiente_verde", "npc", "dependiente_verde", "gentleman_arthur_01a6",
                "Dependiente del Poké Mart", world, config.viridianCourierX, config.viridianCourierY, config.viridianCourierZ, 180f));
        store().put(new StoryPlacementStore.Placement("gate:gym:8", "gate", "gym:8", "",
                "Entrada del gimnasio de Giovanni", world, config.giovanniGateX, config.giovanniGateY, config.giovanniGateZ, 0f));
        store().put(new StoryPlacementStore.Placement("trainer:bosque_verde_1", "trainer", "bosque_verde_1", "bug_catcher_01ec",
                "Entrenador de Bosque Verde", world, 78, 76, 12, 0f));
        store().put(new StoryPlacementStore.Placement("trainer:bosque_verde_2", "trainer", "bosque_verde_2", "bug_catcher_anthony_0213",
                "Entrenador de Bosque Verde", world, -61, 91, -704, 0f));
        store().put(new StoryPlacementStore.Placement("trainer:bosque_verde_3", "trainer", "bosque_verde_3", "bug_catcher_rick_0066",
                "Entrenador de Bosque Verde", world, -14, 102, -889, 180f));
        store().put(new StoryPlacementStore.Placement("trainer:bosque_verde_4", "trainer", "bosque_verde_4", "bug_catcher_doug_0067",
                "Entrenador de Bosque Verde", world, 37, 112, -1080, 0f));
        store().put(new StoryPlacementStore.Placement("leader:1", "leader", "1", "kanto_brock",
                "Brock", world, config.brockX, config.brockY, config.brockZ, 180f));
    }

    public record PlacementResult(boolean success, String message, StoryPlacementStore.Placement placement) {
        static PlacementResult success(String message, StoryPlacementStore.Placement placement) {
            return new PlacementResult(true, message, placement);
        }

        static PlacementResult failure(String message) {
            return new PlacementResult(false, message, null);
        }
    }

    public record SetupResult(int prepared, int expected, List<String> failures) {}
    public record Validation(int placed, List<String> missing, List<String> invalidTrainers) {}
}
