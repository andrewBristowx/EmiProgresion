package com.andrewbristowx.emiprogresion.command;

import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import com.andrewbristowx.emiprogresion.region.AdventureRegionService;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
                        .then(Commands.literal("brock")
                                .then(Commands.literal("tp").requires(s -> s.hasPermission(2)).executes(ctx -> tpBrock(ctx.getSource())))
                                .then(Commands.literal("place").requires(s -> s.hasPermission(2)).executes(ctx -> placeBrock(ctx.getSource())))))
                .then(Commands.literal("setspawn").requires(s -> s.hasPermission(2)).executes(ctx -> setSpawn(ctx.getSource())))
                .then(Commands.literal("validate").requires(s -> s.hasPermission(2)).executes(ctx -> validate(ctx.getSource())))
                .then(Commands.literal("reload").requires(s -> s.hasPermission(2)).executes(ctx -> reload(ctx.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        source.sendSuccess(() -> Component.literal("EmiProgresion • Kanto prototipo hasta Brock")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("Mundo principal: " + c.mainWorld + " • Spawn protegido: " + c.protectedSpawnRadius + "b • Casas: hasta " + c.housingRadius + "b")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Kanto: " + c.kantoWorld + " • Border: " + c.kantoWorldBorderDiameter + "b • Brock: " + c.brockX + ", " + c.brockY + ", " + c.brockZ)
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int layout(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        source.sendSuccess(() -> Component.literal("Spawn principal → núcleo protegido 0-" + c.protectedSpawnRadius + "b → viviendas hasta " + c.housingRadius + "b.")
                .withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("Aventura separada: Pueblo inicial Kanto → Ruta 1 → Brock (prototipo alpha.2).")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int enterKanto(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel kanto = AdventureRegionService.getLevel(source.getServer(), c.kantoWorld);
        if (kanto == null) {
            source.sendFailure(Component.literal("El mundo/dimensión '" + c.kantoWorld + "' todavía no existe. Instala/crea la dimensión Kanto y reinicia."));
            return 0;
        }
        player.teleportTo(kanto, c.kantoSpawnX + 0.5, c.kantoSpawnY, c.kantoSpawnZ + 0.5, player.getYRot(), player.getXRot());
        return 1;
    }

    private static int leaveKanto(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel main = AdventureRegionService.getLevel(source.getServer(), c.mainWorld);
        if (main == null) {
            source.sendFailure(Component.literal("No se encontró el mundo principal '" + c.mainWorld + "'."));
            return 0;
        }
        player.teleportTo(main, c.mainSpawnX + 0.5, c.mainSpawnY, c.mainSpawnZ + 0.5, player.getYRot(), player.getXRot());
        return 1;
    }

    private static int tpBrock(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel kanto = AdventureRegionService.getLevel(source.getServer(), c.kantoWorld);
        if (kanto == null) {
            source.sendFailure(Component.literal("No existe el mundo Kanto."));
            return 0;
        }
        player.teleportTo(kanto, c.brockX + 0.5, c.brockY, c.brockZ + 0.5, player.getYRot(), player.getXRot());
        return 1;
    }

    private static int placeBrock(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        if (c.brockStructureId == null || c.brockStructureId.isBlank()) {
            source.sendFailure(Component.literal("brockStructureId está vacío. Primero necesitamos el ID exacto del gimnasio de Brock de COBBLEVERSE-DP-v31."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Ejecuta en Kanto: /place structure " + c.brockStructureId + " " + c.brockX + " " + c.brockY + " " + c.brockZ)
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int setSpawn(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EmiProgresionConfig c = EmiProgresionConfig.get();
        c.mainSpawnX = player.getBlockX();
        c.mainSpawnY = player.getBlockY();
        c.mainSpawnZ = player.getBlockZ();
        EmiProgresionConfig.save();
        source.sendSuccess(() -> Component.literal("Spawn principal guardado en " + c.mainSpawnX + ", " + c.mainSpawnY + ", " + c.mainSpawnZ)
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int validate(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        boolean kantoExists = AdventureRegionService.getLevel(source.getServer(), c.kantoWorld) != null;
        source.sendSuccess(() -> Component.literal("Validación Kanto alpha.2:").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal((kantoExists ? "✓ " : "✕ ") + "Mundo Kanto: " + c.kantoWorld)
                .withStyle(kantoExists ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.literal((c.brockStructureId != null && !c.brockStructureId.isBlank() ? "✓ " : "⚠ ") + "ID estructura Brock: " + (c.brockStructureId == null || c.brockStructureId.isBlank() ? "pendiente" : c.brockStructureId))
                .withStyle(c.brockStructureId != null && !c.brockStructureId.isBlank() ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("✓ Border Kanto configurado: " + c.kantoWorldBorderDiameter + " bloques")
                .withStyle(ChatFormatting.GREEN), false);
        return kantoExists ? 1 : 0;
    }

    private static int reload(CommandSourceStack source) {
        EmiProgresionConfig.reload();
        AdventureRegionService.applyKantoBorder(source.getServer());
        source.sendSuccess(() -> Component.literal("EmiProgresion recargado.").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
