package com.andrewbristowx.emiprogresion.command;

import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import com.andrewbristowx.emiprogresion.region.AdventureRegionService;
import com.andrewbristowx.emiprogresion.region.KantoMapInstaller;
import com.andrewbristowx.emiprogresion.story.StoryService;
import com.andrewbristowx.emiprogresion.story.StoryPlacementService;
import com.andrewbristowx.emiprogresion.story.KantoStoryCatalog;
import net.fabricmc.loader.api.FabricLoader;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class EmiProgresionCommand {
    private EmiProgresionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("emiprogresion")
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("layout").executes(ctx -> layout(ctx.getSource())))
                .then(Commands.literal("kanto")
                        .then(Commands.literal("enter").executes(ctx -> enterKanto(ctx.getSource())))
                        .then(Commands.literal("leave").executes(ctx -> leaveKanto(ctx.getSource())))
                        .then(Commands.literal("setup")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> setupKanto(ctx.getSource())))
                        .then(Commands.literal("mapcheck")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> validate(ctx.getSource())))
                        .then(Commands.literal("installstatus")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> installerStatus(ctx.getSource()))))
                .then(Commands.literal("story")
                        .then(Commands.literal("status").executes(ctx -> storyStatus(ctx.getSource())))
                        .then(Commands.literal("objective").executes(ctx -> storyObjective(ctx.getSource())))
                        .then(Commands.literal("setup")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> storySetup(ctx.getSource())))
                        .then(Commands.literal("reset")
                                .requires(s -> s.hasPermission(2))
                                .executes(ctx -> storyReset(ctx.getSource())))
                        .then(Commands.literal("setanchor")
                                .requires(s -> s.hasPermission(2))
                                .then(Commands.literal("oak").executes(ctx -> setAnchor(ctx.getSource(), "oak")))
                                .then(Commands.literal("pallet_guide").executes(ctx -> setAnchor(ctx.getSource(), "pallet_guide")))
                                .then(Commands.literal("viridian_courier").executes(ctx -> setAnchor(ctx.getSource(), "viridian_courier")))
                                .then(Commands.literal("giovanni_gate").executes(ctx -> setAnchor(ctx.getSource(), "giovanni_gate")))
                                .then(Commands.literal("brock").executes(ctx -> setAnchor(ctx.getSource(), "brock")))
                                .then(Commands.literal("route3_gate").executes(ctx -> setAnchor(ctx.getSource(), "route3_gate")))))
                .then(Commands.literal("admin").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("on").executes(ctx -> adminMode(ctx.getSource(), true)))
                        .then(Commands.literal("off").executes(ctx -> adminMode(ctx.getSource(), false)))
                        .then(Commands.literal("status").executes(ctx -> adminStatus(ctx.getSource()))))
                .then(Commands.literal("colocar").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("npc")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> placement(ctx.getSource(), StoryPlacementService.placeNpc(
                                                ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"))))))
                        .then(Commands.literal("entrenador")
                                .then(Commands.argument("zona", StringArgumentType.word())
                                        .executes(ctx -> placement(ctx.getSource(), StoryPlacementService.placeNextRouteTrainer(
                                                ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "zona"))))))
                        .then(Commands.literal("gimnasio")
                                .then(Commands.argument("numero", IntegerArgumentType.integer(1, 8))
                                        .then(Commands.literal("entrenador").executes(ctx -> placement(ctx.getSource(),
                                                StoryPlacementService.placeNextGymTrainer(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "numero")))))
                                        .then(Commands.literal("lider").executes(ctx -> placement(ctx.getSource(),
                                                StoryPlacementService.placeGymLeader(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "numero")))))
                                        .then(Commands.literal("entrada").executes(ctx -> placement(ctx.getSource(),
                                                StoryPlacementService.placeGymGate(ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "numero")))))))
                        .then(Commands.literal("jefe")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> placement(ctx.getSource(), StoryPlacementService.placeBoss(
                                                ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"))))))
                        .then(Commands.literal("alto_mando")
                                .then(Commands.argument("numero", IntegerArgumentType.integer(1, 4))
                                        .executes(ctx -> placement(ctx.getSource(), StoryPlacementService.placeLeague(
                                                ctx.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(ctx, "numero"))))))
                        .then(Commands.literal("campeon").executes(ctx -> placement(ctx.getSource(),
                                StoryPlacementService.placeLeague(ctx.getSource().getPlayerOrException(), 5))))
                        .then(Commands.literal("pokeparada").executes(ctx -> placement(ctx.getSource(),
                                StoryPlacementService.placePokestop(ctx.getSource().getPlayerOrException()))))
                        .then(Commands.literal("terminal")
                                .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                        .executes(ctx -> placement(ctx.getSource(), StoryPlacementService.placeTerminal(
                                                ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "nombre")))))))
                .then(Commands.literal("montaje").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("lista").executes(ctx -> placementList(ctx.getSource())))
                        .then(Commands.literal("catalogo").executes(ctx -> catalogue(ctx.getSource())))
                        .then(Commands.literal("validar").executes(ctx -> placementValidate(ctx.getSource())))
                        .then(Commands.literal("reconstruir").executes(ctx -> placementSetup(ctx.getSource())))
                        .then(Commands.literal("deshacer").executes(ctx -> placement(ctx.getSource(),
                                StoryPlacementService.undo(ctx.getSource().getPlayerOrException()))))
                        .then(Commands.literal("mover_cercano").executes(ctx -> placement(ctx.getSource(),
                                StoryPlacementService.moveNearest(ctx.getSource().getPlayerOrException()))))
                        .then(Commands.literal("eliminar_cercano").executes(ctx -> placement(ctx.getSource(),
                                StoryPlacementService.removeNearest(ctx.getSource().getPlayerOrException())))))
                .then(Commands.literal("lootcheck")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> lootCheck(ctx.getSource())))
                .then(Commands.literal("setspawn")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> setSpawn(ctx.getSource())))
                .then(Commands.literal("validate")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> validate(ctx.getSource())))
                .then(Commands.literal("reload")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> reload(ctx.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        source.sendSuccess(() -> Component.literal("✦ EmiProgresion • Wild Kanto map test ✦")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("Mundo principal: " + config.mainWorld
                        + " • Spawn protegido: " + config.protectedSpawnRadius + "b • Casas: hasta " + config.housingRadius + "b")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Mapa aventura: " + config.kantoWorld
                        + " • Spawn: " + config.kantoSpawnX + " " + config.kantoSpawnY + " " + config.kantoSpawnZ)
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("Gimnasios naturales: permitidos en el mundo normal, fuera de la campaña oficial.")
                .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Historia oficial completa: Pueblo Paleta → 8 gimnasios → Alto Mando → Campeón.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }

    private static int layout(CommandSourceStack source) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        source.sendSuccess(() -> Component.literal("Mundo principal: núcleo personalizado 0-" + config.protectedSpawnRadius
                        + "b; viviendas/comunidad hasta " + config.housingRadius + "b.")
                .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Wild Kanto completamente generado: X "
                        + config.kantoGeneratedMinX + ".." + config.kantoGeneratedMaxX + " • Z "
                        + config.kantoGeneratedMinZ + ".." + config.kantoGeneratedMaxZ + ".")
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("Border provisional: centro " + config.kantoBorderCenterX + ","
                        + config.kantoBorderCenterZ + " • diámetro " + config.kantoWorldBorderDiameter + "b.")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int enterKanto(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return AdventureRegionService.enterKanto(player) ? 1 : 0;
    }

    private static int leaveKanto(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return AdventureRegionService.leaveKanto(player) ? 1 : 0;
    }

    private static int setupKanto(CommandSourceStack source) {
        AdventureRegionService.applyKantoBorder(source.getServer());
        source.sendSuccess(() -> Component.literal("Border provisional aplicado. No se colocó ni reemplazó ninguna estructura del mapa.")
                .withStyle(ChatFormatting.GREEN), true);
        return validate(source);
    }

    private static int setSpawn(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmiProgresionConfig config = EmiProgresionConfig.get();
        config.mainWorld = player.serverLevel().dimension().location().toString();
        config.mainSpawnX = player.getBlockX();
        config.mainSpawnY = player.getBlockY();
        config.mainSpawnZ = player.getBlockZ();
        EmiProgresionConfig.save();
        source.sendSuccess(() -> Component.literal("✓ Spawn principal guardado en " + config.mainWorld + " "
                        + config.mainSpawnX + ", " + config.mainSpawnY + ", " + config.mainSpawnZ)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int validate(CommandSourceStack source) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        var kanto = AdventureRegionService.getLevel(source.getServer(), config.kantoWorld);
        boolean dimensionExists = kanto != null;
        boolean signatureFound = dimensionExists && AdventureRegionService.hasWildKantoSignature(kanto);
        boolean safeSpawn = dimensionExists && AdventureRegionService.getSafeKantoSpawn(kanto).isPresent();
        boolean spawnInsideGeneratedMap = AdventureRegionService.isInsideGeneratedRectangle(
                new net.minecraft.core.BlockPos(config.kantoSpawnX, config.kantoSpawnY, config.kantoSpawnZ)
        );

        source.sendSuccess(() -> Component.literal("Validación Wild Kanto alpha.5:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        validationLine(source, KantoMapInstaller.state() != KantoMapInstaller.InstallState.FAILED,
                "Instalador automático: " + KantoMapInstaller.state());
        validationLine(source, dimensionExists, "Dimensión Kanto: " + config.kantoWorld);
        validationLine(source, signatureFound, "Firma del mapa Wild Kanto 1-00-02");
        validationLine(source, safeSpawn, "Spawn seguro cerca de " + config.kantoSpawnX + " "
                + config.kantoSpawnY + " " + config.kantoSpawnZ);
        validationLine(source, spawnInsideGeneratedMap, "Spawn dentro del rectángulo completamente generado");
        source.sendSuccess(() -> Component.literal("✓ No se bloquean los gimnasios naturales del mundo normal.")
                .withStyle(ChatFormatting.GREEN), false);
        validationLine(source, FabricLoader.getInstance().isModLoaded("rctmod"), "Radical Cobblemon Trainers (RCT)");
        validationLine(source, config.storyNpcSetupComplete, "NPCs de historia preparados con /emiprogresion story setup");
        source.sendSuccess(() -> Component.literal("ℹ Lootr no se modifica si el mod no está instalado; usa /emiprogresion lootcheck.")
                .withStyle(ChatFormatting.YELLOW), false);

        return dimensionExists && signatureFound && safeSpawn && spawnInsideGeneratedMap ? 1 : 0;
    }

    private static int installerStatus(CommandSourceStack source) {
        KantoMapInstaller.InstallState state = KantoMapInstaller.state();
        boolean ok = state == KantoMapInstaller.InstallState.INSTALLED
                || state == KantoMapInstaller.InstallState.ALREADY_INSTALLED;
        ChatFormatting color = ok ? ChatFormatting.GREEN
                : state == KantoMapInstaller.InstallState.FAILED ? ChatFormatting.RED : ChatFormatting.YELLOW;
        source.sendSuccess(() -> Component.literal("Instalador Wild Kanto: " + state)
                .withStyle(color, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal(KantoMapInstaller.detail())
                .withStyle(ChatFormatting.GRAY), false);
        if (KantoMapInstaller.lastBackup() != null) {
            source.sendSuccess(() -> Component.literal("Respaldo anterior: " + KantoMapInstaller.lastBackup())
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        if (state == KantoMapInstaller.InstallState.FAILED) {
            source.sendSuccess(() -> Component.literal("El servidor continuará, pero Kanto seguirá bloqueado. Corrige la conexión o configuración y reinicia para reintentar.")
                    .withStyle(ChatFormatting.RED), false);
        }
        return ok ? 1 : 0;
    }

    private static void validationLine(CommandSourceStack source, boolean ok, String text) {
        source.sendSuccess(() -> Component.literal((ok ? "✓ " : "✕ ") + text)
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), false);
    }

    private static int reload(CommandSourceStack source) {
        EmiProgresionConfig.reload();
        AdventureRegionService.applyKantoBorder(source.getServer());
        source.sendSuccess(() -> Component.literal("EmiProgresion recargado.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int storyStatus(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        var progress = StoryService.progress(player);
        source.sendSuccess(() -> Component.literal("Historia Kanto: " + progress.stage
                        + (progress.starter.isBlank() ? "" : " • Inicial: " + progress.starter))
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int storyObjective(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        StoryService.openObjective(source.getPlayerOrException());
        return 1;
    }

    private static int storySetup(CommandSourceStack source) {
        return placementSetup(source);
    }

    private static int storyReset(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StoryService.reset(player);
        source.sendSuccess(() -> Component.literal("Progreso de historia reiniciado para " + player.getScoreboardName() + ".")
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int setAnchor(CommandSourceStack source, String anchor) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!AdventureRegionService.isKanto(player.serverLevel())) {
            source.sendFailure(Component.literal("Debes estar dentro de emiprogresion:kanto."));
            return 0;
        }
        EmiProgresionConfig config = EmiProgresionConfig.get();
        int x = player.getBlockX();
        int y = player.getBlockY();
        int z = player.getBlockZ();
        switch (anchor) {
            case "oak" -> { config.oakX = x; config.oakY = y; config.oakZ = z; }
            case "pallet_guide" -> { config.palletGuideX = x; config.palletGuideY = y; config.palletGuideZ = z; }
            case "viridian_courier" -> { config.viridianCourierX = x; config.viridianCourierY = y; config.viridianCourierZ = z; }
            case "giovanni_gate" -> { config.giovanniGateX = x; config.giovanniGateY = y; config.giovanniGateZ = z; }
            case "brock" -> { config.brockX = x; config.brockY = y; config.brockZ = z; }
            case "route3_gate" -> { config.routeThreeGateX = x; config.routeThreeGateY = y; config.routeThreeGateZ = z; }
            default -> { return 0; }
        }
        config.storyNpcSetupComplete = false;
        EmiProgresionConfig.save();
        source.sendSuccess(() -> Component.literal("Anclaje " + anchor + " guardado en " + x + " " + y + " " + z
                        + ". Ejecuta /emiprogresion story setup.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int adminMode(CommandSourceStack source, boolean enabled) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StoryPlacementService.setAdminMode(player, enabled);
        source.sendSuccess(() -> Component.literal("Modo de montaje " + (enabled ? "ACTIVADO" : "DESACTIVADO")
                        + (enabled ? ". Puedes atravesar entradas bloqueadas y colocar elementos." : "."))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int adminStatus(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        boolean enabled = StoryPlacementService.isAdminMode(source.getPlayerOrException());
        source.sendSuccess(() -> Component.literal("Modo de montaje: " + (enabled ? "ACTIVADO" : "DESACTIVADO"))
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        return enabled ? 1 : 0;
    }

    private static int placement(CommandSourceStack source, StoryPlacementService.PlacementResult result) {
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("✓ " + result.message()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int placementSetup(CommandSourceStack source) {
        StoryPlacementService.SetupResult result = StoryPlacementService.setupPlacedEntities(source.getServer());
        boolean ok = result.prepared() == result.expected();
        EmiProgresionConfig.get().storyNpcSetupComplete = ok;
        EmiProgresionConfig.save();
        source.sendSuccess(() -> Component.literal("Elementos reconstruidos: " + result.prepared() + "/" + result.expected()
                        + (result.failures().isEmpty() ? "" : " • Fallaron: " + String.join(", ", result.failures())))
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return ok ? 1 : 0;
    }

    private static int placementValidate(CommandSourceStack source) {
        StoryPlacementService.Validation result = StoryPlacementService.validate(source.getServer());
        source.sendSuccess(() -> Component.literal("Montaje de Kanto: " + result.placed() + " elementos colocados.")
                .withStyle(ChatFormatting.AQUA), false);
        validationLine(source, result.invalidTrainers().isEmpty(), result.invalidTrainers().isEmpty()
                ? "Todos los modelos/equipos colocados existen en RCT"
                : "IDs de RCT inválidos: " + String.join(", ", result.invalidTrainers()));
        validationLine(source, result.missing().isEmpty(), result.missing().isEmpty()
                ? "Catálogo principal completamente colocado"
                : "Pendientes: " + result.missing().size());
        if (!result.missing().isEmpty()) {
            source.sendSuccess(() -> Component.literal("Primeros pendientes: "
                            + String.join(", ", result.missing().stream().limit(15).toList()))
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        return result.invalidTrainers().isEmpty() ? 1 : 0;
    }

    private static int placementList(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("Colocados: " + StoryPlacementService.all().size())
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        StoryPlacementService.all().stream().limit(40).forEach(entry -> source.sendSuccess(() ->
                Component.literal("• " + entry.id + " — " + entry.displayName + " @ " + entry.x + " " + entry.y + " " + entry.z)
                        .withStyle(ChatFormatting.GRAY), false));
        if (StoryPlacementService.all().size() > 40) source.sendSuccess(() ->
                Component.literal("… y " + (StoryPlacementService.all().size() - 40) + " más.").withStyle(ChatFormatting.GRAY), false);
        return StoryPlacementService.all().size();
    }

    private static int catalogue(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("NPCs: " + KantoStoryCatalog.npcs().stream().map(KantoStoryCatalog.NpcDefinition::id).toList())
                .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Zonas de entrenadores: " + KantoStoryCatalog.routeNames())
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("Jefes: " + KantoStoryCatalog.bosses().stream().map(KantoStoryCatalog.BossDefinition::id).toList())
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        source.sendSuccess(() -> Component.literal("Usa: colocar gimnasio <1-8> entrenador|lider|entrada; alto_mando <1-4>; campeon.")
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int lootCheck(CommandSourceStack source) {
        boolean lootr = FabricLoader.getInstance().isModLoaded("lootr");
        if (!lootr) {
            source.sendFailure(Component.literal("Lootr no está instalado en este perfil. No se convirtió ningún cofre para evitar perder sus objetos."));
            source.sendSuccess(() -> Component.literal("Pasture Loot no es Lootr: son mods distintos. Instala una versión Fabric 1.21.1 compatible antes de preparar cofres personales.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Lootr detectado. La conversión seguirá desactivada hasta validar en una copia que conserva exactamente el contenido del mapa.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
