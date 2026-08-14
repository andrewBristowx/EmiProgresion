package com.andrewbristowx.emiprogresion.story;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import com.andrewbristowx.emiprogresion.network.StoryNetworking;
import com.andrewbristowx.emiprogresion.region.AdventureRegionService;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StoryService {
    private static final String OAK_TAG = "emiprogresion_story_oak";
    private static final String GUIDE_TAG = "emiprogresion_story_guide";
    private static final String COURIER_TAG = "emiprogresion_story_courier";
    private static final String BROCK_TAG = "emiprogresion_story_brock";
    private static final String ROUTE_TAG = "emiprogresion_story_route";
    private static final String OAK_PORTRAIT = "rctmod:textures/trainers/single/professor_oak_00c8.png";
    private static final String BROCK_PORTRAIT = "rctmod:textures/trainers/single/kanto_brock.png";
    private static final Map<UUID, String> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> PROMPT_COOLDOWN = new HashMap<>();
    private static StoryProgressStore progress;
    private static long ticks;

    private StoryService() {}

    public static void initializeEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
                return InteractionResult.PASS;
            }
            return handleInteraction(serverPlayer, level, entity) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
    }

    public static void onServerStarted(MinecraftServer server) {
        progress = new StoryProgressStore(server);
    }

    public static void onServerStopping() {
        if (progress != null) progress.save();
        SESSIONS.clear();
        PROMPT_COOLDOWN.clear();
    }

    public static void tick(MinecraftServer server) {
        if (progress == null || !EmiProgresionConfig.get().storyEnabled) return;
        ticks++;
        if (ticks % 5L == 0L) tickTrainerSight(server);
        if (ticks % 20L == 0L) {
            tickBrockCompletion(server);
            tickGates(server);
        }
        if (ticks % 40L == 0L) tickGuides(server);
    }

    public static PlayerStoryProgress progress(ServerPlayer player) {
        return progress.get(player.getUUID());
    }

    public static void openOak(ServerPlayer player) {
        PlayerStoryProgress state = progress(player);
        if (state.stage == StoryStage.NEW) {
            open(player, new DialogueState("oak_start", "Profesor Oak", OAK_PORTRAIT,
                    List.of(
                            "¡Hola! Bienvenido a Kanto. Este mundo está lleno de Pokémon y entrenadores que esperan conocerte.",
                            "Antes de salir de Pueblo Paleta necesito confiarte un compañero. Aunque ya elegiste uno en Cobblemon, este será el inicio oficial de la historia.",
                            "Elige con cuidado. Solo podrás recibir uno de estos tres Pokémon una vez."
                    ), List.of(
                            new DialogueState.Choice("starter:bulbasaur", "Bulbasaur", "Tipo Planta / Veneno"),
                            new DialogueState.Choice("starter:charmander", "Charmander", "Tipo Fuego"),
                            new DialogueState.Choice("starter:squirtle", "Squirtle", "Tipo Agua")
                    ), true, true));
        } else if (state.stage == StoryStage.PARCEL_RECEIVED) {
            open(player, new DialogueState("oak_parcel", "Profesor Oak", OAK_PORTRAIT,
                    List.of(
                            "¡Ese es el paquete que estaba esperando de Ciudad Verde! Gracias por traerlo.",
                            "Ya puedes continuar hacia el norte, atravesar el Bosque Verde y desafiar a Brock en Ciudad Plateada."
                    ), List.of(new DialogueState.Choice("return_parcel", "Entregar paquete", "Continuar la aventura")),
                    true, true));
        } else {
            open(player, new DialogueState("oak_repeat", "Profesor Oak", OAK_PORTRAIT,
                    List.of(objectiveText(state.stage)), List.of(), false, true));
        }
    }

    public static void openObjective(ServerPlayer player) {
        PlayerStoryProgress state = progress(player);
        open(player, new DialogueState("objective", "Guía de Kanto", "",
                List.of(objectiveText(state.stage)), List.of(), false, true));
    }

    public static void handleAction(ServerPlayer player, String dialogueId, String action) {
        String active = SESSIONS.get(player.getUUID());
        if (active == null || !active.equals(dialogueId) || !AdventureRegionService.isKanto(player.serverLevel())) return;
        if (action.equals("close")) {
            SESSIONS.remove(player.getUUID());
            return;
        }
        PlayerStoryProgress state = progress(player);
        if (dialogueId.equals("oak_start") && action.startsWith("starter:") && state.stage == StoryStage.NEW) {
            String species = action.substring("starter:".length()).toLowerCase(Locale.ROOT);
            if (!Set.of("bulbasaur", "charmander", "squirtle").contains(species)) return;
            runAs(player, "pokegive @s " + species);
            state.starter = species;
            state.stage = StoryStage.STARTER_CHOSEN;
            progress.save();
            SESSIONS.remove(player.getUUID());
            open(player, new DialogueState("starter_received", "Profesor Oak", OAK_PORTRAIT,
                    List.of("¡Excelente elección! " + displaySpecies(species) + " será tu compañero oficial.",
                            "Ve a Ciudad Verde por la Ruta 1. En la tienda tienen un paquete para mí. Si tu equipo estaba lleno, revisa también tu PC."),
                    List.of(), true, true));
            return;
        }
        if (dialogueId.equals("pallet_guide") && action.equals("gift:potion") && state.markOnce("pallet_potion")) {
            runAs(player, "give @s cobblemon:potion 1");
            progress.save();
            open(player, simple("pallet_gift_done", "Vecino de Pueblo Paleta", "",
                    "Toma esta Poción. Cada regalo de la historia se puede reclamar una sola vez."));
            return;
        }
        if (dialogueId.equals("viridian_courier") && action.equals("take_parcel")
                && state.stage == StoryStage.STARTER_CHOSEN) {
            state.stage = StoryStage.PARCEL_RECEIVED;
            progress.save();
            open(player, simple("parcel_received", "Empleado de la tienda", "",
                    "Paquete de Oak recibido. Regresa al laboratorio de Pueblo Paleta."));
            return;
        }
        if (dialogueId.equals("oak_parcel") && action.equals("return_parcel")
                && state.stage == StoryStage.PARCEL_RECEIVED) {
            state.stage = StoryStage.PARCEL_RETURNED;
            if (state.markOnce("oak_pokeballs")) runAs(player, "give @s cobblemon:poke_ball 5");
            progress.save();
            open(player, simple("parcel_returned", "Profesor Oak", OAK_PORTRAIT,
                    "Te he dado 5 Poké Balls. Tu siguiente objetivo oficial es Brock, en Ciudad Plateada."));
            return;
        }
        if (dialogueId.startsWith("trainer:") && action.equals("battle")) {
            startNearestTaggedBattle(player, ROUTE_TAG, null);
            SESSIONS.remove(player.getUUID());
            return;
        }
        if (dialogueId.equals("brock") && action.equals("battle")) {
            if (!state.stage.atLeast(StoryStage.PARCEL_RETURNED)) {
                open(player, simple("brock_locked", "Brock", BROCK_PORTRAIT,
                        "Primero completa el encargo del Profesor Oak. Después aceptaré tu desafío."));
                return;
            }
            startNearestTaggedBattle(player, BROCK_TAG, "kanto_brock");
            SESSIONS.remove(player.getUUID());
        }
    }

    public static SetupResult setupNpcs(MinecraftServer server) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        ServerLevel level = AdventureRegionService.getLevel(server, config.kantoWorld);
        if (level == null) return new SetupResult(0, 0, "No existe la dimensión Kanto.");
        CommandSourceStack cleanup = server.createCommandSourceStack().withLevel(level).withSuppressedOutput().withPermission(4);
        for (String tag : List.of(OAK_TAG, GUIDE_TAG, COURIER_TAG, BROCK_TAG, ROUTE_TAG)) {
            server.getCommands().performPrefixedCommand(cleanup, "kill @e[tag=" + tag + "]");
        }
        int expected = 8;
        int spawned = 0;
        spawned += spawn(level, "professor_oak_00c8", config.oakX, config.oakY, config.oakZ, OAK_TAG, 180f) ? 1 : 0;
        spawned += spawn(level, "youngster_ben_0065", config.palletGuideX, config.palletGuideY, config.palletGuideZ, GUIDE_TAG, 0f) ? 1 : 0;
        spawned += spawn(level, "youngster_calvin_005a", config.viridianCourierX, config.viridianCourierY, config.viridianCourierZ, COURIER_TAG, 180f) ? 1 : 0;
        spawned += spawn(level, "youngster_ben_0065", 78, 76, 12, ROUTE_TAG, 0f) ? 1 : 0;
        spawned += spawn(level, "bug_catcher_rick_0066", -61, 91, -704, ROUTE_TAG, 0f) ? 1 : 0;
        spawned += spawn(level, "bug_catcher_doug_0067", -14, 102, -889, ROUTE_TAG, 180f) ? 1 : 0;
        spawned += spawn(level, "bug_catcher_sammy_0068", 37, 112, -1080, ROUTE_TAG, 0f) ? 1 : 0;
        spawned += spawn(level, "kanto_brock", config.brockX, config.brockY, config.brockZ, BROCK_TAG, 180f) ? 1 : 0;
        config.storyNpcSetupComplete = spawned == expected;
        EmiProgresionConfig.save();
        return new SetupResult(spawned, expected, spawned == expected ? "NPCs preparados." : "Revisa los anclajes y los datapacks de RCT.");
    }

    public static void reset(ServerPlayer player) {
        progress.reset(player.getUUID());
        SESSIONS.remove(player.getUUID());
    }

    private static boolean handleInteraction(ServerPlayer player, ServerLevel level, Entity entity) {
        if (!AdventureRegionService.isKanto(level) || !EmiProgresionConfig.get().storyEnabled) return false;
        if (entity.getTags().contains(OAK_TAG)) {
            openOak(player);
            return true;
        }
        if (entity.getTags().contains(GUIDE_TAG)) {
            PlayerStoryProgress state = progress(player);
            List<DialogueState.Choice> choices = state.flags.contains("pallet_potion") ? List.of()
                    : List.of(new DialogueState.Choice("gift:potion", "Aceptar Poción", "Regalo único"));
            open(player, new DialogueState("pallet_guide", "Vecino de Pueblo Paleta", "",
                    List.of("La Ruta 1 está al norte. Habla con la gente: algunos tenemos consejos u objetos para ayudarte."),
                    choices, false, true));
            return true;
        }
        if (entity.getTags().contains(COURIER_TAG)) {
            PlayerStoryProgress state = progress(player);
            if (state.stage == StoryStage.STARTER_CHOSEN) {
                open(player, new DialogueState("viridian_courier", "Empleado de la tienda", "",
                        List.of("¡Llegas desde Pueblo Paleta! Tengo un paquete para el Profesor Oak."),
                        List.of(new DialogueState.Choice("take_parcel", "Recibir paquete", "Llévalo al laboratorio")),
                        false, true));
            } else {
                open(player, simple("courier_repeat", "Empleado de la tienda", "", objectiveText(state.stage)));
            }
            return true;
        }
        if (entity.getTags().contains(BROCK_TAG)) {
            openBrock(player);
            return true;
        }
        if (entity.getTags().contains(ROUTE_TAG)) {
            openRouteTrainer(player, entity);
            return true;
        }
        return false;
    }

    private static void openRouteTrainer(ServerPlayer player, Entity trainer) {
        if (wasDefeatedBy(trainer, player.getUUID())) {
            open(player, simple("trainer_defeated", trainer.getName().getString(), portrait(trainerId(trainer)),
                    "¡Buen combate! Sigue entrenando para llegar preparado a Ciudad Plateada."));
            return;
        }
        String id = "trainer:" + trainer.getUUID();
        open(player, new DialogueState(id, trainer.getName().getString(), portrait(trainerId(trainer)),
                List.of("¡Te he visto! Si quieres continuar por esta ruta, primero demuéstrame cómo combate tu equipo."),
                List.of(new DialogueState.Choice("battle", "Aceptar combate", "Equipo original de Cobbleverse/RCT")),
                false, true));
    }

    private static void openBrock(ServerPlayer player) {
        PlayerStoryProgress state = progress(player);
        if (state.stage == StoryStage.BROCK_DEFEATED) {
            open(player, simple("brock_repeat", "Brock", BROCK_PORTRAIT,
                    "Ya conseguiste la Medalla Roca. La siguiente parte de la historia llegará en otra versión."));
            return;
        }
        String text = state.stage.atLeast(StoryStage.PARCEL_RETURNED)
                ? "Soy Brock, líder del Gimnasio de Ciudad Plateada. Mi defensa es sólida como una roca. ¡Acepto tu desafío!"
                : "Todavía no estás listo. Ayuda al Profesor Oak y completa el encargo de Ciudad Verde.";
        List<DialogueState.Choice> choices = state.stage.atLeast(StoryStage.PARCEL_RETURNED)
                ? List.of(new DialogueState.Choice("battle", "Desafiar a Brock", "Equipo de Cobbleverse/RCT")) : List.of();
        open(player, new DialogueState("brock", "Brock", BROCK_PORTRAIT, List.of(text), choices, false, true));
    }

    private static void completeBrock(ServerPlayer player) {
        PlayerStoryProgress state = progress(player);
        if (state.stage == StoryStage.BROCK_DEFEATED || !state.stage.atLeast(StoryStage.PARCEL_RETURNED)) return;
        state.stage = StoryStage.BROCK_DEFEATED;
        state.markOnce("brock_story_reward");
        progress.save();
        open(player, new DialogueState("brock_victory", "Brock", BROCK_PORTRAIT,
                List.of(
                        "Has vencido a Brock y completado la primera versión de la historia de Kanto.",
                        "La Medalla Roca, la caja de medallas y la MT son las recompensas del propio combate de Cobbleverse/RCT.",
                        "La salida hacia la Ruta 3 permanece cerrada hasta la siguiente versión."
                ), List.of(), true, true));
    }

    private static void tickTrainerSight(MinecraftServer server) {
        ServerLevel level = AdventureRegionService.getLevel(server, EmiProgresionConfig.get().kantoWorld);
        if (level == null) return;
        for (ServerPlayer player : level.players()) {
            if (progress(player).stage == StoryStage.NEW || SESSIONS.containsKey(player.getUUID())) continue;
            Long until = PROMPT_COOLDOWN.get(player.getUUID());
            if (until != null && until > ticks) continue;
            List<Entity> trainers = level.getEntities((Entity) null, player.getBoundingBox().inflate(16.0D),
                    trainer -> trainer.getTags().contains(ROUTE_TAG)
                            && !isInBattle(trainer) && !wasDefeatedBy(trainer, player.getUUID()));
            for (Entity trainer : trainers) {
                Vec3 towardPlayer = player.getEyePosition().subtract(trainer.getEyePosition()).normalize();
                if (trainer.getLookAngle().dot(towardPlayer) < 0.72D || !player.hasLineOfSight(trainer)) continue;
                PROMPT_COOLDOWN.put(player.getUUID(), ticks + 200L);
                openRouteTrainer(player, trainer);
                break;
            }
        }
    }

    private static void tickBrockCompletion(MinecraftServer server) {
        ServerLevel level = AdventureRegionService.getLevel(server, EmiProgresionConfig.get().kantoWorld);
        if (level == null) return;
        for (ServerPlayer player : level.players()) {
            PlayerStoryProgress state = progress(player);
            if (!state.stage.atLeast(StoryStage.PARCEL_RETURNED) || state.stage == StoryStage.BROCK_DEFEATED) continue;
            List<Entity> brocks = level.getEntities((Entity) null, player.getBoundingBox().inflate(256.0D),
                    entity -> entity.getTags().contains(BROCK_TAG) && "kanto_brock".equals(trainerId(entity)));
            if (brocks.stream().anyMatch(entity -> wasDefeatedBy(entity, player.getUUID()))) completeBrock(player);
        }
    }

    private static void tickGates(MinecraftServer server) {
        ServerLevel level = AdventureRegionService.getLevel(server, EmiProgresionConfig.get().kantoWorld);
        if (level == null) return;
        EmiProgresionConfig config = EmiProgresionConfig.get();
        for (ServerPlayer player : level.players()) {
            if (near(player, config.giovanniGateX, config.giovanniGateY, config.giovanniGateZ, 4.5D)) {
                pushBack(player, config.giovanniGateX, config.giovanniGateZ);
                maybeOpenGate(player, "giovanni_closed", "Recepcionista del gimnasio", "",
                        "Giovanni no está. El gimnasio de Ciudad Verde permanecerá cerrado hasta que consigas las medallas necesarias.");
            }
            if (near(player, config.routeThreeGateX, config.routeThreeGateY, config.routeThreeGateZ, 4.5D)) {
                pushBack(player, config.routeThreeGateX, config.routeThreeGateZ);
                String text = progress(player).stage == StoryStage.BROCK_DEFEATED
                        ? "Has completado esta primera versión. La Ruta 3 se abrirá en la próxima actualización."
                        : "Primero debes vencer a Brock en el Gimnasio de Ciudad Plateada.";
                maybeOpenGate(player, "route3_closed", "Guía de Kanto", "", text);
            }
        }
    }

    private static void tickGuides(MinecraftServer server) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        if (!config.storyGuideEnabled) return;
        ServerLevel level = AdventureRegionService.getLevel(server, config.kantoWorld);
        if (level == null) return;
        for (ServerPlayer player : level.players()) {
            PlayerStoryProgress state = progress(player);
            BlockPos target = switch (state.stage) {
                case NEW, PARCEL_RECEIVED -> new BlockPos(config.oakX, config.oakY, config.oakZ);
                case STARTER_CHOSEN -> new BlockPos(config.viridianCourierX, config.viridianCourierY, config.viridianCourierZ);
                case PARCEL_RETURNED -> new BlockPos(config.brockX, config.brockY, config.brockZ);
                case BROCK_DEFEATED -> new BlockPos(config.routeThreeGateX, config.routeThreeGateY, config.routeThreeGateZ);
            };
            int distance = (int) Math.sqrt(player.blockPosition().distSqr(target));
            player.displayClientMessage(Component.literal("✦ " + shortObjective(state.stage) + " • "
                    + direction(player.blockPosition(), target) + " • " + distance + " bloques")
                    .withStyle(ChatFormatting.AQUA), true);
        }
    }

    private static void maybeOpenGate(ServerPlayer player, String id, String speaker, String portrait, String text) {
        Long until = PROMPT_COOLDOWN.get(player.getUUID());
        if (until != null && until > ticks) return;
        PROMPT_COOLDOWN.put(player.getUUID(), ticks + 100L);
        open(player, simple(id, speaker, portrait, text));
    }

    private static void pushBack(ServerPlayer player, int gateX, int gateZ) {
        double dx = player.getX() - gateX;
        double dz = player.getZ() - gateZ;
        double length = Math.max(0.001D, Math.sqrt(dx * dx + dz * dz));
        player.teleportTo(player.serverLevel(), gateX + dx / length * 6.0D, player.getY(),
                gateZ + dz / length * 6.0D, player.getYRot(), player.getXRot());
    }

    private static boolean near(ServerPlayer player, int x, int y, int z, double radius) {
        return player.distanceToSqr(x + 0.5D, y, z + 0.5D) <= radius * radius;
    }

    private static void startNearestTaggedBattle(ServerPlayer player, String tag, String requiredTrainerId) {
        List<Entity> candidates = player.serverLevel().getEntities((Entity) null,
                player.getBoundingBox().inflate(14.0D), trainer -> trainer.getTags().contains(tag)
                        && (requiredTrainerId == null || requiredTrainerId.equals(trainerId(trainer))));
        Entity nearest = candidates.stream().min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
        if (nearest == null) {
            player.sendSystemMessage(Component.literal("El entrenador ya no está cerca. Vuelve a hablar con él.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        try {
            nearest.getClass().getMethod("startBattleWith", net.minecraft.world.entity.player.Player.class)
                    .invoke(nearest, player);
        } catch (ReflectiveOperationException exception) {
            player.sendSystemMessage(Component.literal("RCT no pudo iniciar el combate. Revisa que sea la versión 0.18.1-beta.")
                    .withStyle(ChatFormatting.RED));
            EmiProgresion.LOGGER.error("Could not invoke RCT battle for {}", trainerId(nearest), exception);
        }
    }

    private static boolean spawn(ServerLevel level, String trainerId, int x, int y, int z, String tag, float yaw) {
        level.getChunk(x >> 4, z >> 4);
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withPosition(new Vec3(x + 0.5D, y, z + 0.5D)).withSuppressedOutput().withPermission(4);
        String nbt = "{NoAI:1b,Invulnerable:1b,Silent:1b,PersistenceRequired:1b,Rotation:[" + yaw
                + "f,0f],Tags:[\"" + tag + "\"]}";
        level.getServer().getCommands().performPrefixedCommand(source,
                "rctmod trainer summon_persistent " + trainerId + " ~ ~ ~ " + nbt);
        boolean found = !level.getEntities((Entity) null, new AABB(x - 2, y - 2, z - 2, x + 3, y + 4, z + 3),
                entity -> entity.getTags().contains(tag)).isEmpty();
        if (!found) EmiProgresion.LOGGER.warn("Could not summon story trainer {} at {} {} {}", trainerId, x, y, z);
        return found;
    }

    private static void runAs(ServerPlayer player, String command) {
        CommandSourceStack source = player.createCommandSourceStack().withSuppressedOutput().withPermission(4);
        player.server.getCommands().performPrefixedCommand(source, "execute as " + player.getScoreboardName()
                + " at @s run " + command);
    }

    private static void open(ServerPlayer player, DialogueState state) {
        SESSIONS.put(player.getUUID(), state.id());
        StoryNetworking.open(player, state);
    }

    private static DialogueState simple(String id, String speaker, String portrait, String text) {
        return new DialogueState(id, speaker, portrait, List.of(text), List.of(), false, true);
    }

    private static String objectiveText(StoryStage stage) {
        return switch (stage) {
            case NEW -> "Busca al Profesor Oak en su laboratorio de Pueblo Paleta y elige tu inicial oficial.";
            case STARTER_CHOSEN -> "Viaja al norte por la Ruta 1 y recoge el paquete de Oak en Ciudad Verde.";
            case PARCEL_RECEIVED -> "Regresa al laboratorio de Pueblo Paleta y entrega el paquete al Profesor Oak.";
            case PARCEL_RETURNED -> "Atraviesa la Ruta 2 y el Bosque Verde. Tu objetivo es derrotar a Brock en Ciudad Plateada.";
            case BROCK_DEFEATED -> "Primera versión completada. La historia continuará desde la Ruta 3.";
        };
    }

    private static String shortObjective(StoryStage stage) {
        return switch (stage) {
            case NEW -> "Habla con Oak";
            case STARTER_CHOSEN -> "Paquete en Ciudad Verde";
            case PARCEL_RECEIVED -> "Vuelve con Oak";
            case PARCEL_RETURNED -> "Desafía a Brock";
            case BROCK_DEFEATED -> "Historia alpha.4 completada";
        };
    }

    private static String direction(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        String vertical = dz < -4 ? "N" : dz > 4 ? "S" : "";
        String horizontal = dx > 4 ? "E" : dx < -4 ? "O" : "";
        String result = vertical + horizontal;
        return result.isBlank() ? "AQUÍ" : result;
    }

    private static String displaySpecies(String species) {
        return Character.toUpperCase(species.charAt(0)) + species.substring(1);
    }

    private static String portrait(String trainerId) {
        return "rctmod:textures/trainers/single/" + trainerId + ".png";
    }

    private static String trainerId(Entity entity) {
        try {
            Object value = entity.getClass().getMethod("getTrainerId").invoke(entity);
            return value instanceof String id ? id : "default";
        } catch (ReflectiveOperationException ignored) {
            return "default";
        }
    }

    private static boolean wasDefeatedBy(Entity entity, UUID playerId) {
        try {
            Object value = entity.getClass().getMethod("wasDefeatedBy", UUID.class).invoke(entity, playerId);
            return value instanceof Boolean defeated && defeated;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static boolean isInBattle(Entity entity) {
        try {
            Object value = entity.getClass().getMethod("isInBattle").invoke(entity);
            return value instanceof Boolean battling && battling;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public record SetupResult(int spawned, int expected, String message) {}
}
