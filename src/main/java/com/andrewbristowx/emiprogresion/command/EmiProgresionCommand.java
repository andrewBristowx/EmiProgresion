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
                        .then(Commands.literal("ash")
                                .then(Commands.literal("tp")
                                        .requires(s -> s.hasPermission(2))
                                        .executes(ctx -> tpAsh(ctx.getSource())))
                                .then(Commands.literal("place")
                                        .requires(s -> s.hasPermission(2))
                                        .executes(ctx -> placeAsh(ctx.getSource()))))
                        .then(Commands.literal("brock")
                                .then(Commands.literal("tp")
                                        .requires(s -> s.hasPermission(2))
                                        .executes(ctx -> tpBrock(ctx.getSource())))
                                .then(Commands.literal("place")
                                        .requires(s -> s.hasPermission(2))
                                        .executes(ctx -> placeBrock(ctx.getSource())))))
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
        EmiProgresionConfig c = EmiProgresionConfig.get();
        source.sendSuccess(() -> Component.literal("✦ EmiProgresion • Kanto hasta Brock ✦")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("Mundo principal: " + c.mainWorld
                        + " • Spawn protegido: " + c.protectedSpawnRadius + "b • Casas: hasta " + c.housingRadius + "b")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Aventura: " + c.kantoWorld
                        + " • Border: " + c.kantoWorldBorderDiameter + "b")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Ash: " + c.ashStructureId + " @ " + c.ashX + "," + c.ashZ
                        + " • Brock: " + c.brockStructureId + " @ " + c.brockX + "," + c.brockZ)
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int layout(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        source.sendSuccess(() -> Component.literal("Mundo principal: núcleo personalizado 0-" + c.protectedSpawnRadius
                        + "b; viviendas/comunidad hasta " + c.housingRadius + "b.")
                .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Mundo aventura: Ash/Pueblo inicial → Ruta 1 (hasta Z "
                        + c.route1EndZ + ") → Brock (Z " + c.brockZ + ").")
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("Los gimnasios de Kanto se reservan para colocación manual; no deben generarse aleatoriamente.")
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
        int placed = AdventureRegionService.setupPrototype(source);
        if (placed == 2) {
            source.sendSuccess(() -> Component.literal("✓ Prototipo Kanto colocado: Ash + Brock.")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
            return 1;
        }
        source.sendFailure(Component.literal("No se pudieron colocar las dos estructuras. Revisa la consola y /emiprogresion validate."));
        return 0;
    }

    private static int tpAsh(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        return AdventureRegionService.teleportToKantoPoint(source.getPlayerOrException(), c.ashX, c.ashZ) ? 1 : 0;
    }

    private static int placeAsh(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        int result = AdventureRegionService.placeStructure(source, c.ashStructureId, c.ashX, c.ashY, c.ashZ);
        if (result > 0) source.sendSuccess(() -> Component.literal("✓ Estructura de Ash colocada.").withStyle(ChatFormatting.GREEN), true);
        else source.sendFailure(Component.literal("No se pudo colocar " + c.ashStructureId));
        return result > 0 ? 1 : 0;
    }

    private static int tpBrock(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        return AdventureRegionService.teleportToKantoPoint(source.getPlayerOrException(), c.brockX, c.brockZ) ? 1 : 0;
    }

    private static int placeBrock(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        int result = AdventureRegionService.placeStructure(source, c.brockStructureId, c.brockX, c.brockY, c.brockZ);
        if (result > 0) source.sendSuccess(() -> Component.literal("✓ Gimnasio de Brock colocado con trainer id " + c.brockTrainerId + ".")
                .withStyle(ChatFormatting.GREEN), true);
        else source.sendFailure(Component.literal("No se pudo colocar " + c.brockStructureId));
        return result > 0 ? 1 : 0;
    }

    private static int setSpawn(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmiProgresionConfig c = EmiProgresionConfig.get();
        c.mainWorld = player.serverLevel().dimension().location().toString();
        c.mainSpawnX = player.getBlockX();
        c.mainSpawnY = player.getBlockY();
        c.mainSpawnZ = player.getBlockZ();
        EmiProgresionConfig.save();
        source.sendSuccess(() -> Component.literal("✓ Spawn principal guardado en " + c.mainWorld + " "
                        + c.mainSpawnX + ", " + c.mainSpawnY + ", " + c.mainSpawnZ)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int validate(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        boolean kantoExists = AdventureRegionService.getLevel(source.getServer(), c.kantoWorld) != null;
        boolean idsOk = "cobbleverse:ash".equals(c.ashStructureId)
                && "cobbleverse:brock".equals(c.brockStructureId)
                && "kanto_brock".equals(c.brockTrainerId);

        source.sendSuccess(() -> Component.literal("Validación Kanto alpha.1:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal((kantoExists ? "✓ " : "✕ ") + "Dimensión Kanto: " + c.kantoWorld)
                .withStyle(kantoExists ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.literal((idsOk ? "✓ " : "✕ ")
                        + "IDs Cobbleverse confirmados: Ash/Brock/kanto_brock")
                .withStyle(idsOk ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.literal("✓ Border previsto: " + c.kantoWorldBorderDiameter + " bloques")
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("⚠ Comprueba en chunks nuevos del mundo normal que /locate structure cobbleverse:brock no encuentre generación natural.")
                .withStyle(ChatFormatting.YELLOW), false);
        return kantoExists && idsOk ? 1 : 0;
    }

    private static int reload(CommandSourceStack source) {
        EmiProgresionConfig.reload();
        AdventureRegionService.applyKantoBorder(source.getServer());
        source.sendSuccess(() -> Component.literal("EmiProgresion recargado.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
