package com.andrewbristowx.emiprogresion.command;

import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import com.andrewbristowx.emiprogresion.region.AdventureRegionService;
import com.mojang.brigadier.CommandDispatcher;
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
                                .executes(ctx -> validate(ctx.getSource()))))
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

        source.sendSuccess(() -> Component.literal("Validación Wild Kanto alpha.3:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        validationLine(source, dimensionExists, "Dimensión Kanto: " + config.kantoWorld);
        validationLine(source, signatureFound, "Firma del mapa Wild Kanto 1-00-02");
        validationLine(source, safeSpawn, "Spawn seguro cerca de " + config.kantoSpawnX + " "
                + config.kantoSpawnY + " " + config.kantoSpawnZ);
        validationLine(source, spawnInsideGeneratedMap, "Spawn dentro del rectángulo completamente generado");
        source.sendSuccess(() -> Component.literal("✓ No se bloquean los gimnasios naturales del mundo normal.")
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("ℹ Esta alpha prueba limpieza, terreno y acceso; las recompensas oficiales aún no están activadas.")
                .withStyle(ChatFormatting.YELLOW), false);

        return dimensionExists && signatureFound && safeSpawn && spawnInsideGeneratedMap ? 1 : 0;
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
}
