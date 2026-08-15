package com.andrewbristowx.emiprogresion.story;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import com.andrewbristowx.emiprogresion.network.StoryNetworking;
import com.andrewbristowx.emiprogresion.region.AdventureRegionService;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private static boolean automaticSetupPending;
    private static long nextAutomaticSetupTick;

    private StoryService() {}

    public static void initializeEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !(world instanceof ServerLevel level)) {
                return InteractionResult.PASS;
            }
            return handleInteraction(serverPlayer, level, entity) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SESSIONS.remove(handler.player.getUUID());
            PROMPT_COOLDOWN.remove(handler.player.getUUID());
        });
    }

    public static void onServerStarted(MinecraftServer server) {
        progress = new StoryProgressStore(server);
        StoryPlacementService.onServerStarted(server);
        ticks = 0L;
        automaticSetupPending = EmiProgresionConfig.get().storyEnabled
                && !EmiProgresionConfig.get().storyNpcSetupComplete;
        nextAutomaticSetupTick = 20L;
    }

    public static void onServerStopping() {
        if (progress != null) progress.save();
        StoryPlacementService.onServerStopping();
        SESSIONS.clear();
        PROMPT_COOLDOWN.clear();
        automaticSetupPending = false;
    }

    public static void tick(MinecraftServer server) {
        if (progress == null || !EmiProgresionConfig.get().storyEnabled) return;
        ticks++;
        if (automaticSetupPending && ticks >= nextAutomaticSetupTick) tickAutomaticSetup(server);
        if (ticks % 5L == 0L) tickTrainerSight(server);
        if (ticks % 20L == 0L) {
            tickBrockCompletion(server);
            tickPlacedBattleCompletion(server);
            tickGates(server);
            stabilizePlacements(server);
        }
        if (ticks % 40L == 0L) tickGuides(server);
    }

    private static void tickAutomaticSetup(MinecraftServer server) {
        nextAutomaticSetupTick = ticks + 20L;
        if (!rctTrainerAvailable("kanto_brock")) return;
        ServerLevel kanto = AdventureRegionService.getLevel(server, EmiProgresionConfig.get().kantoWorld);
        if (kanto == null || !AdventureRegionService.hasWildKantoSignature(kanto)) return;

        automaticSetupPending = false;
        cleanupLegacyStoryEntities(kanto);
        StoryPlacementService.SetupResult result = StoryPlacementService.setupPlacedEntities(server);
        EmiProgresionConfig.get().storyNpcSetupComplete = result.prepared() == result.expected();
        EmiProgresionConfig.save();
        EmiProgresion.LOGGER.info("Delayed automatic Kanto placement setup: {}/{}; failures={}",
                result.prepared(), result.expected(), result.failures());
    }

    private static void cleanupLegacyStoryEntities(ServerLevel level) {
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withSuppressedOutput().withPermission(4);
        for (String tag : List.of(OAK_TAG, GUIDE_TAG, COURIER_TAG, BROCK_TAG, ROUTE_TAG)) {
            executeStoryCommand(level, source, "kill @e[tag=" + tag + "]");
        }
        removeLegacyRctTrainersAt(level, 0, 64, 1250);
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
        if (handlePlacedAction(player, dialogueId, action, state)) return;
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

    private static boolean handlePlacedAction(ServerPlayer player, String dialogueId, String action,
                                               PlayerStoryProgress state) {
        if (dialogueId.startsWith("placed:") && action.startsWith("battle:")) {
            String placementId = action.substring("battle:".length());
            StoryPlacementService.find(placementId).ifPresent(placement -> startPlacementBattle(player, placement));
            SESSIONS.remove(player.getUUID());
            return true;
        }
        if (dialogueId.startsWith("npc:") && action.startsWith("npc_action:")) {
            String npcId = dialogueId.substring("npc:".length());
            String npcAction = action.substring("npc_action:".length());
            performNpcAction(player, npcId, npcAction, state);
            return true;
        }
        if (dialogueId.startsWith("terminal:") && action.startsWith("travel:")) {
            String targetId = action.substring("travel:".length());
            StoryPlacementService.find(targetId).ifPresent(target -> {
                ServerLevel destination = AdventureRegionService.getLevel(player.server, target.world);
                if (destination != null) {
                    player.teleportTo(destination, target.x + 0.5D, target.y + 1.0D, target.z + 0.5D,
                            target.yaw, 0.0F);
                }
            });
            SESSIONS.remove(player.getUUID());
            return true;
        }
        return false;
    }

    private static void openPlacement(ServerPlayer player, Entity entity, StoryPlacementStore.Placement placement) {
        switch (placement.kind) {
            case "npc" -> openPlacedNpc(player, placement);
            case "trainer" -> openPlacedTrainer(player, entity, placement);
            case "leader" -> openPlacedLeader(player, entity, placement);
            case "boss" -> openPlacedBoss(player, entity, placement);
            case "league", "champion" -> openPlacedLeague(player, entity, placement);
            default -> { }
        }
    }

    private static void openPlacedNpc(ServerPlayer player, StoryPlacementStore.Placement placement) {
        KantoStoryCatalog.NpcDefinition npc = KantoStoryCatalog.npc(placement.catalogId).orElse(null);
        if (npc == null) return;
        if (npc.id().equals("oak")) {
            openOak(player);
            return;
        }
        PlayerStoryProgress state = progress(player);
        String text = npc.repeat();
        List<DialogueState.Choice> choices = new ArrayList<>();
        switch (npc.action()) {
            case "parcel" -> {
                if (state.stage == StoryStage.STARTER_CHOSEN) {
                    text = npc.intro();
                    choices.add(choice("npc_action:parcel", "Recibir paquete", "Llévalo al Profesor Oak"));
                }
            }
            case "heal" -> {
                text = npc.intro();
                choices.add(choice("npc_action:heal", "Curar equipo", "Servicio gratuito"));
            }
            case "fossil" -> {
                text = npc.intro();
                if (state.stage == StoryStage.BROCK_DEFEATED && !state.flags.contains("fossil_chosen")) {
                    choices.add(choice("npc_action:fossil_helix", "Fósil Helix", "Elegir solo uno"));
                    choices.add(choice("npc_action:fossil_dome", "Fósil Domo", "Elegir solo uno"));
                }
            }
            case "bill" -> {
                text = npc.intro();
                if (state.stage == StoryStage.MOUNT_MOON_CLEARED) {
                    choices.add(choice("npc_action:bill", "Ayudar a Bill", "Recibir el pase del S.S. Anne"));
                }
            }
            case "ss_anne" -> {
                text = npc.intro();
                if (state.stage == StoryStage.MISTY_DEFEATED) {
                    choices.add(choice("npc_action:ss_anne", "Hablar con el capitán", "Completar la visita"));
                }
            }
            case "rocket_hideout" -> {
                text = npc.intro();
                choices.add(choice("npc_action:rocket_hideout", "Investigar", "Marcar la entrada secreta"));
            }
            case "pokemon_tower" -> {
                text = npc.intro();
                if (state.stage == StoryStage.ERIKA_DEFEATED) {
                    choices.add(choice("npc_action:pokemon_tower", "Recibir Poké Flauta", "Calmar la torre"));
                }
            }
            case "silph" -> {
                text = npc.intro();
                if (state.stage == StoryStage.SILPH_CO_CLEARED && !state.flags.contains("silph_master_ball")) {
                    choices.add(choice("npc_action:silph", "Aceptar agradecimiento", "Recompensa única"));
                }
            }
            case "cinnabar_key" -> {
                text = npc.intro();
                if (state.stage == StoryStage.SABRINA_DEFEATED) {
                    choices.add(choice("npc_action:cinnabar_key", "Recoger llave", "Abrir el gimnasio"));
                }
            }
            case "victory_road" -> {
                text = state.badges() >= 8 ? npc.intro() : "Necesitas las ocho medallas oficiales para entrar a Calle Victoria.";
                if (state.stage == StoryStage.GIOVANNI_DEFEATED) {
                    choices.add(choice("npc_action:victory_road", "Verificar medallas", "Abrir la Liga"));
                }
            }
            case "giovanni_gate" -> text = state.badges() < 7
                    ? "Giovanni no está. Regresa cuando tengas las otras siete medallas. Ahora tienes " + state.badges() + "."
                    : "Giovanni ha regresado. Ya puedes entrar al último gimnasio.";
            default -> {
                if (npc.action().startsWith("gift:") && !state.flags.contains(npc.action())) {
                    text = npc.intro();
                    choices.add(choice("npc_action:" + npc.action(), "Aceptar objeto", "Regalo único"));
                } else if (npc.action().isBlank()) {
                    text = npc.intro();
                }
            }
        }
        open(player, new DialogueState("npc:" + npc.id(), npc.name(), portrait(npc.trainerId()),
                List.of(text), choices, false, true));
    }

    private static void performNpcAction(ServerPlayer player, String npcId, String action, PlayerStoryProgress state) {
        String message;
        switch (action) {
            case "parcel" -> {
                if (state.stage != StoryStage.STARTER_CHOSEN) return;
                state.stage = StoryStage.PARCEL_RECEIVED;
                message = "Paquete recibido. Regresa al laboratorio de Pueblo Paleta.";
            }
            case "heal" -> {
                runAs(player, "pokeheal @s");
                message = "Tu equipo ha recuperado toda su energía.";
            }
            case "fossil_helix", "fossil_dome" -> {
                if (state.stage != StoryStage.BROCK_DEFEATED || !state.markOnce("fossil_chosen")) return;
                runAs(player, "give @s cobblemon:" + (action.equals("fossil_helix") ? "helix_fossil" : "dome_fossil") + " 1");
                state.stage = StoryStage.MOUNT_MOON_CLEARED;
                message = "Fósil elegido. Continúa hacia Ciudad Celeste y busca a Bill al norte.";
            }
            case "bill" -> {
                if (state.stage != StoryStage.MOUNT_MOON_CLEARED) return;
                state.stage = StoryStage.BILL_HELPED;
                state.markOnce("ss_anne_ticket");
                message = "Has ayudado a Bill y recibido el pase del S.S. Anne. Ahora desafía a Misty.";
            }
            case "ss_anne" -> {
                if (state.stage != StoryStage.MISTY_DEFEATED) return;
                state.stage = StoryStage.SS_ANNE_CLEARED;
                message = "Visita del S.S. Anne completada. El Teniente Surge ya acepta tu desafío.";
            }
            case "rocket_hideout" -> {
                state.markOnce("rocket_hideout_found");
                message = "Has localizado la guarida. Derrota a Giovanni bajo el casino.";
            }
            case "pokemon_tower" -> {
                if (state.stage != StoryStage.ERIKA_DEFEATED) return;
                state.stage = StoryStage.POKEMON_TOWER_CLEARED;
                if (state.markOnce("poke_flute")) runAs(player, "give @s minecraft:goat_horn 1");
                message = "La Torre Pokémon está en calma. Koga te espera en Ciudad Fucsia.";
            }
            case "silph" -> {
                if (state.stage != StoryStage.SILPH_CO_CLEARED || !state.markOnce("silph_master_ball")) return;
                runAs(player, "give @s cobblemon:master_ball 1");
                message = "Silph S.A. te entrega una Master Ball. Sabrina ha reabierto su gimnasio.";
            }
            case "cinnabar_key" -> {
                if (state.stage != StoryStage.SABRINA_DEFEATED) return;
                state.stage = StoryStage.CINNABAR_KEY_FOUND;
                state.markOnce("cinnabar_key");
                message = "Llave encontrada. Ya puedes desafiar a Blaine.";
            }
            case "victory_road" -> {
                if (state.stage != StoryStage.GIOVANNI_DEFEATED || state.badges() < 8) return;
                state.stage = StoryStage.VICTORY_ROAD_CLEARED;
                message = "Ocho medallas verificadas. El Alto Mando te espera.";
            }
            default -> {
                if (!action.startsWith("gift:") || !state.markOnce(action)) return;
                String item = switch (action) {
                    case "gift:pallet_potion" -> "cobblemon:potion 1";
                    case "gift:fanclub" -> "minecraft:lead 1";
                    case "gift:tea" -> "minecraft:honey_bottle 1";
                    case "gift:safari" -> "cobblemon:safari_ball 5";
                    default -> "cobblemon:poke_ball 3";
                };
                runAs(player, "give @s " + item);
                message = "Objeto recibido. Este regalo solo puede reclamarse una vez.";
            }
        }
        progress.save();
        KantoStoryCatalog.NpcDefinition npc = KantoStoryCatalog.npc(npcId).orElse(null);
        open(player, simple("npc_result:" + npcId, npc == null ? "Historia de Kanto" : npc.name(),
                npc == null ? "" : portrait(npc.trainerId()), message));
    }

    private static void openPlacedTrainer(ServerPlayer player, Entity entity, StoryPlacementStore.Placement placement) {
        KantoStoryCatalog.TrainerDefinition definition = findTrainer(placement.catalogId);
        String intro = definition == null ? "¡Te he visto! Prepárate para combatir." : definition.intro();
        String defeated = definition == null ? "Buen combate. Sigue adelante." : definition.defeated();
        openBattleDialogue(player, entity, placement, wasDefeatedBy(entity, player.getUUID()) ? defeated : intro, true);
    }

    private static void openPlacedLeader(ServerPlayer player, Entity entity, StoryPlacementStore.Placement placement) {
        int number;
        try { number = Integer.parseInt(placement.catalogId); } catch (NumberFormatException ignored) { return; }
        KantoStoryCatalog.GymDefinition gym = KantoStoryCatalog.gym(number).orElse(null);
        if (gym == null) return;
        PlayerStoryProgress state = progress(player);
        if (state.stage.atLeast(gym.resultStage())) {
            open(player, simple("leader_done:" + number, gym.leaderName(), portrait(gym.leaderTrainerId()),
                    "Ya has ganado mi medalla. Continúa con tu aventura."));
        } else if (!state.stage.atLeast(gym.requiredStage())) {
            open(player, simple("leader_locked:" + number, gym.leaderName(), portrait(gym.leaderTrainerId()),
                    lockText(gym, state)));
        } else {
            openBattleDialogue(player, entity, placement,
                    "Soy " + gym.leaderName() + ", líder de " + gym.city() + ". ¡Acepto tu desafío!", true);
        }
    }

    private static void openPlacedBoss(ServerPlayer player, Entity entity, StoryPlacementStore.Placement placement) {
        KantoStoryCatalog.BossDefinition boss = KantoStoryCatalog.boss(placement.catalogId).orElse(null);
        if (boss == null) return;
        PlayerStoryProgress state = progress(player);
        if (!state.stage.atLeast(boss.requiredStage())) {
            open(player, simple("boss_locked:" + boss.id(), boss.name(), portrait(boss.trainerId()), objectiveText(state.stage)));
        } else if (wasDefeatedBy(entity, player.getUUID())) {
            open(player, simple("boss_done:" + boss.id(), boss.name(), portrait(boss.trainerId()), "Nuestro combate ya terminó. Sigue adelante."));
        } else {
            openBattleDialogue(player, entity, placement, boss.intro(), true);
        }
    }

    private static void openPlacedLeague(ServerPlayer player, Entity entity, StoryPlacementStore.Placement placement) {
        int number;
        try { number = Integer.parseInt(placement.catalogId); } catch (NumberFormatException ignored) { return; }
        KantoStoryCatalog.LeagueDefinition member = KantoStoryCatalog.league(number).orElse(null);
        if (member == null) return;
        PlayerStoryProgress state = progress(player);
        if (state.stage.atLeast(member.resultStage())) {
            open(player, simple("league_done:" + number, member.name(), portrait(member.trainerId()),
                    member.champion() ? "Eres el nuevo Campeón de Kanto." : "Ya me has vencido. El siguiente miembro te espera."));
        } else if (!state.stage.atLeast(member.requiredStage())) {
            open(player, simple("league_locked:" + number, member.name(), portrait(member.trainerId()),
                    "Debes vencer al miembro anterior antes de entrar a esta sala."));
        } else {
            openBattleDialogue(player, entity, placement,
                    member.champion() ? "Nuestro viaje empezó juntos. Ahora decidiremos quién será el Campeón de Kanto."
                            : "Soy " + member.name() + ", miembro del Alto Mando. Demuestra que mereces continuar.", true);
        }
    }

    private static void openBattleDialogue(ServerPlayer player, Entity entity,
                                             StoryPlacementStore.Placement placement, String text, boolean battle) {
        List<DialogueState.Choice> choices = battle && !wasDefeatedBy(entity, player.getUUID())
                ? List.of(choice("battle:" + placement.id, "Combatir", "Equipo original de RCT")) : List.of();
        open(player, new DialogueState("placed:" + placement.id, placement.displayName, portrait(placement.trainerId),
                List.of(text), choices, false, true));
    }

    private static DialogueState.Choice choice(String id, String label, String hint) {
        return new DialogueState.Choice(id, label, hint);
    }

    private static KantoStoryCatalog.TrainerDefinition findTrainer(String id) {
        for (String route : KantoStoryCatalog.routeNames()) {
            for (KantoStoryCatalog.TrainerDefinition trainer : KantoStoryCatalog.route(route)) {
                if (trainer.id().equals(id)) return trainer;
            }
        }
        for (KantoStoryCatalog.GymDefinition gym : KantoStoryCatalog.gyms()) {
            for (KantoStoryCatalog.TrainerDefinition trainer : gym.trainers()) {
                if (trainer.id().equals(id)) return trainer;
            }
        }
        return null;
    }

    private static String lockText(KantoStoryCatalog.GymDefinition gym, PlayerStoryProgress state) {
        if (gym.number() == 8) return "Giovanni no está. Necesitas las otras siete medallas; ahora tienes " + state.badges() + ".";
        return "Aún no has completado la parte anterior de la historia. " + objectiveText(state.stage);
    }

    public static SetupResult setupNpcs(MinecraftServer server) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        ServerLevel level = AdventureRegionService.getLevel(server, config.kantoWorld);
        if (level == null) return new SetupResult(0, 0, "No existe la dimensión Kanto.");
        if (!rctTrainerAvailable("kanto_brock")) {
            return new SetupResult(0, 8, "RCT todavía está cargando sus entrenadores. Espera unos segundos y repite.");
        }
        CommandSourceStack cleanup = server.createCommandSourceStack().withLevel(level).withSuppressedOutput().withPermission(4);
        for (String tag : List.of(OAK_TAG, GUIDE_TAG, COURIER_TAG, BROCK_TAG, ROUTE_TAG)) {
            server.getCommands().performPrefixedCommand(cleanup, "kill @e[tag=" + tag + "]");
        }
        removeOrphanAt(level, "professor_oak_00c8", config.oakX, config.oakY, config.oakZ);
        removeOrphanAt(level, "youngster_ben_0065", config.palletGuideX, config.palletGuideY, config.palletGuideZ);
        removeOrphanAt(level, "youngster_calvin_005a", config.viridianCourierX, config.viridianCourierY, config.viridianCourierZ);
        removeOrphanAt(level, "youngster_ben_0065", 78, 76, 12);
        removeOrphanAt(level, "bug_catcher_rick_0066", -61, 91, -704);
        removeOrphanAt(level, "bug_catcher_doug_0067", -14, 102, -889);
        removeOrphanAt(level, "bug_catcher_sammy_0068", 37, 112, -1080);
        // Alpha.5/5.1 left persistent Brock entities at this obsolete prototype
        // anchor. They load before RCT's datapack registry and lose their trainer
        // ID, so matching by kanto_brock cannot find them later. This anchor was
        // never part of Wild Kanto; remove only RCT trainer entities found there.
        removeLegacyRctTrainersAt(level, 0, 64, 1250);
        removeOrphanAt(level, "kanto_brock", config.brockX, config.brockY, config.brockZ);
        int expected = 8;
        int spawned = 0;
        List<String> failed = new ArrayList<>();
        spawned += spawnAndRecord(level, "professor_oak_00c8", config.oakX, config.oakY, config.oakZ, OAK_TAG, 180f, failed);
        spawned += spawnAndRecord(level, "youngster_ben_0065", config.palletGuideX, config.palletGuideY, config.palletGuideZ, GUIDE_TAG, 0f, failed);
        spawned += spawnAndRecord(level, "youngster_calvin_005a", config.viridianCourierX, config.viridianCourierY, config.viridianCourierZ, COURIER_TAG, 180f, failed);
        spawned += spawnAndRecord(level, "youngster_ben_0065", 78, 76, 12, ROUTE_TAG, 0f, failed);
        spawned += spawnAndRecord(level, "bug_catcher_rick_0066", -61, 91, -704, ROUTE_TAG, 0f, failed);
        spawned += spawnAndRecord(level, "bug_catcher_doug_0067", -14, 102, -889, ROUTE_TAG, 180f, failed);
        spawned += spawnAndRecord(level, "bug_catcher_sammy_0068", 37, 112, -1080, ROUTE_TAG, 0f, failed);
        spawned += spawnStagedBrockAndRecord(level, config.brockX, config.brockY, config.brockZ, BROCK_TAG, 180f, failed);
        config.storyNpcSetupComplete = spawned == expected;
        if (config.storyNpcSetupComplete) automaticSetupPending = false;
        EmiProgresionConfig.save();
        String message = failed.isEmpty() ? "NPCs preparados."
                : "Fallaron: " + String.join(", ", failed) + ". Revisa latest.log.";
        return new SetupResult(spawned, expected, message);
    }

    public static void reset(ServerPlayer player) {
        progress.reset(player.getUUID());
        SESSIONS.remove(player.getUUID());
    }

    private static boolean handleInteraction(ServerPlayer player, ServerLevel level, Entity entity) {
        if (!AdventureRegionService.isKanto(level) || !EmiProgresionConfig.get().storyEnabled) return false;
        java.util.Optional<StoryPlacementStore.Placement> placed = StoryPlacementService.findByEntity(entity.getUUID());
        if (placed.isPresent()) {
            openPlacement(player, entity, placed.get());
            return true;
        }
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
                        "Has vencido a Brock y conseguido la primera medalla de la historia de Kanto.",
                        "La Medalla Roca, la caja de medallas y la MT son las recompensas del propio combate de Cobbleverse/RCT.",
                        "Continúa por la Ruta 3 y atraviesa Monte Moon para llegar a Ciudad Celeste."
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
                    trainer -> (trainer.getTags().contains(ROUTE_TAG) || StoryPlacementService.findByEntity(trainer.getUUID())
                            .map(placement -> placement.kind.equals("trainer")).orElse(false))
                            && !isInBattle(trainer) && !wasDefeatedBy(trainer, player.getUUID()));
            for (Entity trainer : trainers) {
                Vec3 towardPlayer = player.getEyePosition().subtract(trainer.getEyePosition()).normalize();
                if (trainer.getLookAngle().dot(towardPlayer) < 0.72D || !player.hasLineOfSight(trainer)) continue;
                PROMPT_COOLDOWN.put(player.getUUID(), ticks + 200L);
                StoryPlacementService.findByEntity(trainer.getUUID())
                        .ifPresentOrElse(placement -> openPlacedTrainer(player, trainer, placement),
                                () -> openRouteTrainer(player, trainer));
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
        for (StoryPlacementStore.Placement gate : StoryPlacementService.all().stream().filter(p -> p.kind.equals("gate")).toList()) {
            ServerLevel level = AdventureRegionService.getLevel(server, gate.world);
            if (level == null) continue;
            int gymNumber;
            try { gymNumber = Integer.parseInt(gate.catalogId.substring(gate.catalogId.lastIndexOf(':') + 1)); }
            catch (RuntimeException ignored) { continue; }
            KantoStoryCatalog.GymDefinition gym = KantoStoryCatalog.gym(gymNumber).orElse(null);
            if (gym == null) continue;
            for (ServerPlayer player : level.players()) {
                if (StoryPlacementService.isAdminMode(player) || !near(player, gate.x, gate.y, gate.z, 3.0D)) continue;
                PlayerStoryProgress state = progress(player);
                if (state.stage.atLeast(gym.requiredStage())) continue;
                double radians = Math.toRadians(gate.yaw);
                double x = gate.x + Math.sin(radians) * 3.5D;
                double z = gate.z - Math.cos(radians) * 3.5D;
                player.teleportTo(level, x + 0.5D, player.getY(), z + 0.5D, gate.yaw, player.getXRot());
                maybeOpenGate(player, "gate:" + gymNumber, "Encargado del gimnasio", "", lockText(gym, state));
            }
        }
    }

    private static void tickGuides(MinecraftServer server) {
        if (!EmiProgresionConfig.get().storyGuideEnabled) return;
        ServerLevel level = AdventureRegionService.getLevel(server, EmiProgresionConfig.get().kantoWorld);
        if (level == null) return;
        for (ServerPlayer player : level.players()) {
            PlayerStoryProgress state = progress(player);
            StoryPlacementStore.Placement objective = StoryPlacementService.find(objectivePlacementId(state.stage)).orElse(null);
            if (objective == null || !objective.world.equals(level.dimension().location().toString())) continue;
            BlockPos target = new BlockPos(objective.x, objective.y, objective.z);
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

    private static void startPlacementBattle(ServerPlayer player, StoryPlacementStore.Placement placement) {
        Entity trainer = placement.entityUuid == null ? null : player.serverLevel().getEntity(placement.entityUuid);
        if (trainer == null || player.distanceToSqr(trainer) > 225.0D) {
            player.sendSystemMessage(Component.literal("El entrenador ya no está cerca. Vuelve a hablar con él.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        try {
            trainer.getClass().getMethod("startBattleWith", net.minecraft.world.entity.player.Player.class)
                    .invoke(trainer, player);
        } catch (ReflectiveOperationException exception) {
            player.sendSystemMessage(Component.literal("RCT no pudo iniciar el combate.").withStyle(ChatFormatting.RED));
            EmiProgresion.LOGGER.error("Could not start placed RCT battle for {}", placement.id, exception);
        }
    }

    private static void tickPlacedBattleCompletion(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!AdventureRegionService.isKanto(player.serverLevel())) continue;
            PlayerStoryProgress state = progress(player);
            for (StoryPlacementStore.Placement placement : StoryPlacementService.all()) {
                if (!placement.world.equals(player.serverLevel().dimension().location().toString()) || placement.entityUuid == null) continue;
                Entity entity = player.serverLevel().getEntity(placement.entityUuid);
                if (entity == null || !wasDefeatedBy(entity, player.getUUID())) continue;
                String completed = "completed:" + placement.id;
                if (state.flags.contains(completed)) continue;
                StoryStage next = resultStage(placement);
                if (next != null && next.ordinal() > state.stage.ordinal() && requiredStage(placement, state)) {
                    state.markOnce(completed);
                    state.stage = next;
                    progress.save();
                    String text = next == StoryStage.CHAMPION_DEFEATED
                            ? "¡Has completado la historia de Kanto y te has convertido en Campeón!"
                            : "Victoria registrada. " + objectiveText(next);
                    open(player, simple("victory:" + placement.id, placement.displayName,
                            portrait(placement.trainerId), text));
                } else if (next == null) {
                    state.markOnce(completed);
                    progress.save();
                }
            }
        }
    }

    private static StoryStage resultStage(StoryPlacementStore.Placement placement) {
        if (placement.kind.equals("leader")) {
            try { return KantoStoryCatalog.gym(Integer.parseInt(placement.catalogId)).map(KantoStoryCatalog.GymDefinition::resultStage).orElse(null); }
            catch (NumberFormatException ignored) { return null; }
        }
        if (placement.kind.equals("boss")) {
            return KantoStoryCatalog.boss(placement.catalogId).map(KantoStoryCatalog.BossDefinition::resultStage).orElse(null);
        }
        if (placement.kind.equals("league") || placement.kind.equals("champion")) {
            try { return KantoStoryCatalog.league(Integer.parseInt(placement.catalogId)).map(KantoStoryCatalog.LeagueDefinition::resultStage).orElse(null); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private static boolean requiredStage(StoryPlacementStore.Placement placement, PlayerStoryProgress state) {
        if (placement.kind.equals("leader")) {
            try { return KantoStoryCatalog.gym(Integer.parseInt(placement.catalogId)).map(gym -> state.stage.atLeast(gym.requiredStage())).orElse(false); }
            catch (NumberFormatException ignored) { return false; }
        }
        if (placement.kind.equals("boss")) {
            return KantoStoryCatalog.boss(placement.catalogId).map(boss -> state.stage.atLeast(boss.requiredStage())).orElse(false);
        }
        if (placement.kind.equals("league") || placement.kind.equals("champion")) {
            try { return KantoStoryCatalog.league(Integer.parseInt(placement.catalogId)).map(member -> state.stage.atLeast(member.requiredStage())).orElse(false); }
            catch (NumberFormatException ignored) { return false; }
        }
        return true;
    }

    private static void stabilizePlacements(MinecraftServer server) {
        for (StoryPlacementStore.Placement placement : StoryPlacementService.all()) {
            if (placement.entityUuid == null) continue;
            ServerLevel level = AdventureRegionService.getLevel(server, placement.world);
            if (level == null) continue;
            Entity entity = level.getEntity(placement.entityUuid);
            if (entity == null || isInBattle(entity)) continue;
            double x = placement.x + 0.5D;
            double y = placement.y;
            double z = placement.z + 0.5D;
            if (entity.distanceToSqr(x, y, z) > 0.04D) entity.teleportTo(x, y, z);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.setNoGravity(true);
            entity.setYRot(placement.yaw);
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
                mob.setYBodyRot(placement.yaw);
                mob.setYHeadRot(placement.yaw);
            }
        }
    }

    public static boolean spawnPlacementEntity(ServerLevel level, StoryPlacementStore.Placement placement) {
        removePlacementEntity(level, placement);
        String tag = placementTag(placement.id);
        if (!spawn(level, placement.trainerId, placement.x, placement.y, placement.z, tag, placement.yaw)
                && !spawnStagedPlacement(level, placement, tag)) return false;
        AABB area = new AABB(placement.x - 3, placement.y - 3, placement.z - 3,
                placement.x + 4, placement.y + 5, placement.z + 4);
        Entity entity = level.getEntities((Entity) null, area,
                        candidate -> candidate.getTags().contains(tag))
                .stream().min(java.util.Comparator.comparingDouble(candidate -> candidate.distanceToSqr(
                        placement.x + 0.5D, placement.y, placement.z + 0.5D))).orElse(null);
        if (entity == null) return false;
        entity.setCustomName(Component.literal(placement.displayName));
        entity.setCustomNameVisible(false);
        entity.setNoGravity(true);
        StoryPlacementService.store().updateEntityUuid(placement.id, entity.getUUID());
        placement.entityUuid = entity.getUUID();
        return true;
    }

    private static boolean spawnStagedPlacement(ServerLevel level, StoryPlacementStore.Placement placement, String tag) {
        int stagingX = 0;
        int stagingY = 64;
        int stagingZ = 1250;
        level.getChunk(stagingX >> 4, stagingZ >> 4);
        level.getChunk(placement.x >> 4, placement.z >> 4);
        AABB stagingArea = new AABB(stagingX - 3, stagingY - 3, stagingZ - 3,
                stagingX + 4, stagingY + 5, stagingZ + 4);
        Set<UUID> before = new HashSet<>();
        for (Entity entity : level.getEntities((Entity) null, stagingArea, candidate -> true)) before.add(entity.getUUID());
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withPosition(new Vec3(stagingX + 0.5D, stagingY, stagingZ + 0.5D))
                .withSuppressedOutput().withPermission(4);
        executeStoryCommand(level, source, storyTrainerTransientCommand(placement.trainerId, stagingX, stagingY, stagingZ));
        Entity created = findNewTrainer(level, stagingArea, before, placement.trainerId, stagingX, stagingY, stagingZ);
        if (created == null) return false;
        created.setPos(placement.x + 0.5D, placement.y, placement.z + 0.5D);
        try {
            created.getClass().getMethod("setPersistent", boolean.class).invoke(created, true);
        } catch (ReflectiveOperationException exception) {
            EmiProgresion.LOGGER.warn("RCT trainer {} does not expose setPersistent; using vanilla persistence",
                    placement.trainerId);
        }
        return prepareCreatedTrainer(created, placement.trainerId, placement.x, placement.y, placement.z, tag, placement.yaw);
    }

    public static void removePlacementEntity(ServerLevel level, StoryPlacementStore.Placement placement) {
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withSuppressedOutput().withPermission(4);
        if (placement.entityUuid != null) {
            Entity entity = level.getEntity(placement.entityUuid);
            if (entity != null) {
                executeStoryCommand(level, source, storyTrainerUnregisterCommand(entity.getUUID()));
                entity.discard();
            }
        }
        String tag = placementTag(placement.id);
        AABB area = new AABB(placement.x - 5, placement.y - 5, placement.z - 5,
                placement.x + 6, placement.y + 7, placement.z + 6);
        for (Entity entity : level.getEntities((Entity) null, area, candidate -> candidate.getTags().contains(tag))) {
            executeStoryCommand(level, source, storyTrainerUnregisterCommand(entity.getUUID()));
            entity.discard();
        }
    }

    public static void placeTravelBlock(ServerLevel level, StoryPlacementStore.Placement placement, boolean terminal) {
        Block block = terminal ? TravelStopBlocks.TERMINAL : TravelStopBlocks.POKESTOP;
        level.setBlockAndUpdate(new BlockPos(placement.x, placement.y, placement.z), block.defaultBlockState());
    }

    public static void removeTravelBlock(ServerLevel level, StoryPlacementStore.Placement placement) {
        BlockPos pos = new BlockPos(placement.x, placement.y, placement.z);
        if (level.getBlockState(pos).is(TravelStopBlocks.POKESTOP) || level.getBlockState(pos).is(TravelStopBlocks.TERMINAL)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
    }

    public static void interactTravelStop(ServerPlayer player, BlockPos pos, boolean terminal) {
        StoryPlacementStore.Placement placement = StoryPlacementService.findAt(player.serverLevel(), pos).orElse(null);
        if (placement == null) {
            player.sendSystemMessage(Component.literal("Esta parada no está registrada. Un administrador debe volver a colocarla.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        PlayerStoryProgress state = progress(player);
        if (!terminal) {
            long now = System.currentTimeMillis();
            String key = "pokestop:" + placement.id;
            long remaining = state.cooldownRemaining(key, now);
            if (remaining > 0L) {
                long minutes = Math.max(1L, (remaining + 59_999L) / 60_000L);
                open(player, simple("pokestop_wait", placement.displayName, "", "Podrás volver a recoger objetos en " + minutes + " minutos."));
                return;
            }
            runAs(player, "give @s cobblemon:poke_ball " + (3 + Math.min(5, state.badges())));
            runAs(player, "give @s cobblemon:potion 1");
            if (state.badges() >= 3) runAs(player, "give @s cobblemon:great_ball 1");
            if (state.badges() >= 6) runAs(player, "give @s cobblemon:ultra_ball 1");
            state.setCooldown(key, now + EmiProgresionConfig.get().pokestopCooldownMinutes * 60_000L);
            progress.save();
            open(player, simple("pokestop_reward", placement.displayName, "", "Has recibido suministros. La parada se recargará con el tiempo."));
            return;
        }
        if (isPlayerInRctBattle(player)) {
            player.sendSystemMessage(Component.literal("No puedes usar una terminal durante un combate.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        state.markOnce("terminal_active:" + placement.id);
        progress.save();
        List<DialogueState.Choice> choices = new ArrayList<>();
        for (StoryPlacementStore.Placement target : StoryPlacementService.travelTerminals()) {
            if (target.id.equals(placement.id) || !state.flags.contains("terminal_active:" + target.id)) continue;
            choices.add(choice("travel:" + target.id, target.displayName, "Viaje rápido"));
        }
        String text = choices.isEmpty() ? "Terminal activada. Descubre otra terminal para poder viajar entre ambas."
                : "Elige una terminal que ya hayas descubierto.";
        open(player, new DialogueState("terminal:" + placement.id, placement.displayName, "",
                List.of(text), choices, false, true));
    }

    public static boolean rctTrainerAvailable(String trainerId) {
        return rctTrainerAvailableInternal(trainerId);
    }

    private static boolean isPlayerInRctBattle(ServerPlayer player) {
        try {
            Class<?> rctMod = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            Object instance = rctMod.getMethod("getInstance").invoke(null);
            Object value = instance.getClass().getMethod("isInBattle", net.minecraft.world.entity.player.Player.class)
                    .invoke(instance, player);
            return value instanceof Boolean battling && battling;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static String placementTag(String id) {
        return "emiprogresion_placed_" + id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private static int spawnAndRecord(ServerLevel level, String trainerId, int x, int y, int z,
                                      String tag, float yaw, List<String> failed) {
        if (spawn(level, trainerId, x, y, z, tag, yaw)) return 1;
        failed.add(trainerId + "@" + x + "," + y + "," + z);
        return 0;
    }

    private static int spawnStagedBrockAndRecord(ServerLevel level, int x, int y, int z,
                                                  String tag, float yaw, List<String> failed) {
        if (spawnStagedBrock(level, x, y, z, tag, yaw)) return 1;
        failed.add("kanto_brock@" + x + "," + y + "," + z);
        return 0;
    }

    private static boolean spawnStagedBrock(ServerLevel level, int x, int y, int z, String tag, float yaw) {
        int stagingX = 0;
        int stagingY = 64;
        int stagingZ = 1250;
        level.getChunk(stagingX >> 4, stagingZ >> 4);
        level.getChunk(x >> 4, z >> 4);
        AABB stagingArea = new AABB(stagingX - 3, stagingY - 3, stagingZ - 3,
                stagingX + 4, stagingY + 5, stagingZ + 4);
        Set<UUID> before = new HashSet<>();
        for (Entity entity : level.getEntities((Entity) null, stagingArea, candidate -> true)) {
            before.add(entity.getUUID());
        }
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withPosition(new Vec3(stagingX + 0.5D, stagingY, stagingZ + 0.5D))
                .withSuppressedOutput().withPermission(4);
        executeStoryCommand(level, source,
                storyTrainerTransientCommand("kanto_brock", stagingX, stagingY, stagingZ));
        Entity created = findNewTrainer(level, stagingArea, before, "kanto_brock", stagingX, stagingY, stagingZ);
        if (created == null) {
            EmiProgresion.LOGGER.warn("RCT could not create staged kanto_brock at the proven legacy anchor {} {} {}",
                    stagingX, stagingY, stagingZ);
            return false;
        }
        created.setPos(x + 0.5D, y, z + 0.5D);
        try {
            // Persist only after moving so RCT records the real Pewter Gym chunk,
            // not the temporary staging chunk where this trainer is known to load.
            created.getClass().getMethod("setPersistent", boolean.class).invoke(created, true);
        } catch (ReflectiveOperationException exception) {
            EmiProgresion.LOGGER.error("Could not make staged kanto_brock persistent at {} {} {}", x, y, z, exception);
            created.discard();
            return false;
        }
        return prepareCreatedTrainer(created, "kanto_brock", x, y, z, tag, yaw);
    }

    private static boolean spawn(ServerLevel level, String trainerId, int x, int y, int z, String tag, float yaw) {
        level.getChunk(x >> 4, z >> 4);
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withPosition(new Vec3(x + 0.5D, y, z + 0.5D)).withSuppressedOutput().withPermission(4);
        AABB search = new AABB(x - 3, y - 3, z - 3, x + 4, y + 5, z + 4);
        HashSet<UUID> before = new HashSet<>();
        for (Entity entity : level.getEntities((Entity) null, search, candidate -> true)) {
            before.add(entity.getUUID());
        }
        String command = storyTrainerCommand(trainerId, x, y, z);
        executeStoryCommand(level, source, command);
        Entity created = findNewTrainer(level, search, before, trainerId, x, y, z);
        if (created == null) {
            EmiProgresion.LOGGER.warn("RCT persistent command created no story trainer {} at {} {} {}; trying direct RCT entity summon",
                    trainerId, x, y, z);
            String fallback = storyTrainerFallbackCommand(trainerId, x, y, z);
            executeStoryCommand(level, source, fallback);
            created = findNewTrainer(level, search, before, trainerId, x, y, z);
            if (created == null) {
                List<String> nearbyNew = level.getEntities((Entity) null, search,
                                candidate -> !before.contains(candidate.getUUID()))
                        .stream().map(candidate -> candidate.getClass().getName() + ":" + trainerId(candidate))
                        .toList();
                EmiProgresion.LOGGER.warn("Neither RCT summon path created story trainer {} at {} {} {}. New nearby entities: {}",
                        trainerId, x, y, z, nearbyNew);
                return false;
            }
        }

        return prepareCreatedTrainer(created, trainerId, x, y, z, tag, yaw);
    }

    private static boolean prepareCreatedTrainer(Entity created, String trainerId, int x, int y, int z,
                                                  String tag, float yaw) {
        created.addTag(tag);
        created.setInvulnerable(true);
        created.setSilent(true);
        created.setPos(x + 0.5D, y, z + 0.5D);
        created.setYRot(yaw);
        created.setXRot(0f);
        if (created instanceof Mob mob) {
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            mob.setYBodyRot(yaw);
            mob.setYHeadRot(yaw);
        }
        EmiProgresion.LOGGER.info("Prepared story trainer {} ({}) at {} {} {}", trainerId, created.getUUID(), x, y, z);
        return created.getTags().contains(tag);
    }

    static String storyTrainerCommand(String trainerId, int x, int y, int z) {
        return "rctmod trainer summon_persistent " + trainerId + " " + x + " " + y + " " + z;
    }

    static String storyTrainerTransientCommand(String trainerId, int x, int y, int z) {
        return "rctmod trainer summon " + trainerId + " " + x + " " + y + " " + z;
    }

    static String storyTrainerUnregisterCommand(UUID trainerUuid) {
        return "rctmod trainer unregister_persistent " + trainerUuid;
    }

    static String storyTrainerFallbackCommand(String trainerId, int x, int y, int z) {
        return "summon rctmod:trainer " + x + " " + y + " " + z
                + " {TrainerId:\"" + trainerId + "\",Persistent:1b}";
    }

    private static void executeStoryCommand(ServerLevel level, CommandSourceStack source, String command) {
        try {
            level.getServer().getCommands().getDispatcher().execute(command, source);
        } catch (Exception exception) {
            EmiProgresion.LOGGER.error("Story trainer command failed '{}'", command, exception);
        }
    }

    private static Entity findNewTrainer(ServerLevel level, AABB search, Set<UUID> before,
                                         String expectedTrainerId, int x, int y, int z) {
        return level.getEntities((Entity) null, search,
                        candidate -> expectedTrainerId.equals(trainerId(candidate))
                                && !before.contains(candidate.getUUID()))
                .stream().min(java.util.Comparator.comparingDouble(candidate -> candidate.distanceToSqr(x, y, z)))
                .orElse(null);
    }

    private static boolean rctTrainerAvailableInternal(String trainerId) {
        try {
            Class<?> rctMod = Class.forName("com.gitlab.srcmc.rctmod.api.RCTMod");
            Object instance = rctMod.getMethod("getInstance").invoke(null);
            Object manager = instance.getClass().getMethod("getTrainerManager").invoke(instance);
            Object available = manager.getClass().getMethod("isValidId", String.class).invoke(manager, trainerId);
            return available instanceof Boolean valid && valid;
        } catch (ReflectiveOperationException exception) {
            if (ticks % 200L == 0L) {
                EmiProgresion.LOGGER.warn("Waiting for RCT trainer data before automatic story setup", exception);
            }
            return false;
        }
    }

    private static void removeOrphanAt(ServerLevel level, String expectedTrainerId, int x, int y, int z) {
        level.getChunk(x >> 4, z >> 4);
        AABB area = new AABB(x - 3, y - 3, z - 3, x + 4, y + 5, z + 4);
        for (Entity entity : level.getEntities((Entity) null, area,
                candidate -> expectedTrainerId.equals(trainerId(candidate)))) {
            EmiProgresion.LOGGER.info("Removing previous untagged/duplicate story trainer {} ({}) at anchor {} {} {}",
                    expectedTrainerId, entity.getUUID(), x, y, z);
            entity.discard();
        }
    }

    private static void removeLegacyRctTrainersAt(ServerLevel level, int x, int y, int z) {
        level.getChunk(x >> 4, z >> 4);
        AABB area = new AABB(x - 3, y - 3, z - 3, x + 4, y + 5, z + 4);
        CommandSourceStack source = level.getServer().createCommandSourceStack().withLevel(level)
                .withSuppressedOutput().withPermission(4);
        for (Entity entity : level.getEntities((Entity) null, area,
                candidate -> isRctTrainerType(BuiltInRegistries.ENTITY_TYPE.getKey(candidate.getType()).toString()))) {
            EmiProgresion.LOGGER.info("Removing legacy RCT trainer {} ({}) at obsolete Brock anchor {} {} {}",
                    trainerId(entity), entity.getUUID(), x, y, z);
            executeStoryCommand(level, source, storyTrainerUnregisterCommand(entity.getUUID()));
            entity.discard();
        }
    }

    static boolean isRctTrainerType(String entityTypeId) {
        return "rctmod:trainer".equals(entityTypeId);
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
            case BROCK_DEFEATED -> "Cruza la Ruta 3 y Monte Moon. Habla con el investigador y elige un fósil.";
            case MOUNT_MOON_CLEARED -> "Viaja al Cabo Celeste y ayuda a Bill.";
            case BILL_HELPED -> "Regresa a Ciudad Celeste y derrota a Misty.";
            case MISTY_DEFEATED -> "Sube al S.S. Anne en Ciudad Carmín y habla con su capitán.";
            case SS_ANNE_CLEARED -> "Derrota al Teniente Surge en el gimnasio de Ciudad Carmín.";
            case SURGE_DEFEATED -> "Investiga el casino de Ciudad Azulona y derrota a Giovanni en la guarida de Team Rocket.";
            case ROCKET_HIDEOUT_CLEARED -> "Desafía a Erika en Ciudad Azulona.";
            case ERIKA_DEFEATED -> "Libera la Torre Pokémon y habla con el señor Fuji en Pueblo Lavanda.";
            case POKEMON_TOWER_CLEARED -> "Viaja a Ciudad Fucsia y derrota a Koga.";
            case KOGA_DEFEATED -> "Libera Silph S.A. y derrota a Giovanni en Ciudad Azafrán.";
            case SILPH_CO_CLEARED -> "Habla con el presidente de Silph y desafía a Sabrina.";
            case SABRINA_DEFEATED -> "Explora la Mansión Pokémon de Isla Canela y consigue la llave del gimnasio.";
            case CINNABAR_KEY_FOUND -> "Derrota a Blaine en el gimnasio de Isla Canela.";
            case BLAINE_DEFEATED -> "Giovanni ha regresado a Ciudad Verde. Consigue la octava medalla.";
            case GIOVANNI_DEFEATED -> "Presenta tus ocho medallas en la recepción de la Liga.";
            case VICTORY_ROAD_CLEARED -> "Supera a Lorelei, primera integrante del Alto Mando.";
            case LORELEI_DEFEATED -> "Desafía a Bruno, segundo integrante del Alto Mando.";
            case BRUNO_DEFEATED -> "Desafía a Agatha, tercera integrante del Alto Mando.";
            case AGATHA_DEFEATED -> "Desafía a Lance, último integrante del Alto Mando.";
            case LANCE_DEFEATED -> "Derrota al Campeón Blue y ocupa tu lugar en el Salón de la Fama.";
            case CHAMPION_DEFEATED -> "Historia de Kanto completada. Eres el Campeón de la región.";
        };
    }

    private static String shortObjective(StoryStage stage) {
        return switch (stage) {
            case NEW -> "Habla con Oak";
            case STARTER_CHOSEN -> "Paquete en Ciudad Verde";
            case PARCEL_RECEIVED -> "Vuelve con Oak";
            case PARCEL_RETURNED -> "Desafía a Brock";
            case BROCK_DEFEATED -> "Elige un fósil";
            case MOUNT_MOON_CLEARED -> "Ayuda a Bill";
            case BILL_HELPED -> "Desafía a Misty";
            case MISTY_DEFEATED -> "Visita el S.S. Anne";
            case SS_ANNE_CLEARED -> "Desafía a Surge";
            case SURGE_DEFEATED -> "Guarida Rocket";
            case ROCKET_HIDEOUT_CLEARED -> "Desafía a Erika";
            case ERIKA_DEFEATED -> "Libera Torre Pokémon";
            case POKEMON_TOWER_CLEARED -> "Desafía a Koga";
            case KOGA_DEFEATED -> "Libera Silph S.A.";
            case SILPH_CO_CLEARED -> "Desafía a Sabrina";
            case SABRINA_DEFEATED -> "Llave de Isla Canela";
            case CINNABAR_KEY_FOUND -> "Desafía a Blaine";
            case BLAINE_DEFEATED -> "Desafía a Giovanni";
            case GIOVANNI_DEFEATED -> "Entrada a la Liga";
            case VICTORY_ROAD_CLEARED -> "Desafía a Lorelei";
            case LORELEI_DEFEATED -> "Desafía a Bruno";
            case BRUNO_DEFEATED -> "Desafía a Agatha";
            case AGATHA_DEFEATED -> "Desafía a Lance";
            case LANCE_DEFEATED -> "Combate de Campeón";
            case CHAMPION_DEFEATED -> "Campeón de Kanto";
        };
    }

    private static String objectivePlacementId(StoryStage stage) {
        return switch (stage) {
            case NEW, PARCEL_RECEIVED -> "npc:oak";
            case STARTER_CHOSEN -> "npc:dependiente_verde";
            case PARCEL_RETURNED -> "leader:1";
            case BROCK_DEFEATED -> "npc:cientifico_monte_luna";
            case MOUNT_MOON_CLEARED -> "npc:bill";
            case BILL_HELPED -> "leader:2";
            case MISTY_DEFEATED -> "npc:capitan_anne";
            case SS_ANNE_CLEARED -> "leader:3";
            case SURGE_DEFEATED -> "boss:giovanni_azulona";
            case ROCKET_HIDEOUT_CLEARED -> "leader:4";
            case ERIKA_DEFEATED -> "npc:senor_fuji";
            case POKEMON_TOWER_CLEARED -> "leader:5";
            case KOGA_DEFEATED -> "boss:giovanni_silph";
            case SILPH_CO_CLEARED -> "leader:6";
            case SABRINA_DEFEATED -> "npc:investigador_mansion";
            case CINNABAR_KEY_FOUND -> "leader:7";
            case BLAINE_DEFEATED -> "leader:8";
            case GIOVANNI_DEFEATED -> "npc:recepcion_liga";
            case VICTORY_ROAD_CLEARED -> "league:1";
            case LORELEI_DEFEATED -> "league:2";
            case BRUNO_DEFEATED -> "league:3";
            case AGATHA_DEFEATED -> "league:4";
            case LANCE_DEFEATED, CHAMPION_DEFEATED -> "league:5";
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
