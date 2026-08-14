package com.andrewbristowx.emiprogresion.command;

import com.andrewbristowx.emiprogresion.config.ProgressionConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ProgressionCommand {
    private ProgressionCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("emiprogresion")
                        .then(Commands.literal("status")
                                .executes(context -> status(context.getSource())))
                        .then(Commands.literal("layout")
                                .executes(context -> layout(context.getSource())))
                        .then(Commands.literal("setspawn")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> setSpawn(context.getSource())))
                        .then(Commands.literal("validate")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> validate(context.getSource())))
                        .then(Commands.literal("reload")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> reload(context.getSource())))
        );
    }

    private static int status(CommandSourceStack source) {
        ProgressionConfig config = ProgressionConfig.get();
        source.sendSuccess(() -> Component.literal("✦ EmiProgresion ✦ ")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("Región activa: " + config.activeRegion.toUpperCase())
                        .withStyle(ChatFormatting.YELLOW)), false);
        source.sendSuccess(() -> Component.literal("Kanto: radio " + config.kanto.radius + " bloques (diámetro "
                + (config.kanto.radius * 2) + ")")
                .withStyle(ChatFormatting.GRAY), false);
        source.sendSuccess(() -> Component.literal("Spawn: " + (config.spawn.defined
                        ? config.spawn.x + ", " + config.spawn.y + ", " + config.spawn.z
                        : "AÚN NO DEFINIDO"))
                .withStyle(config.spawn.defined ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        return 1;
    }

    private static int layout(CommandSourceStack source) {
        ProgressionConfig.SpawnConfig spawn = ProgressionConfig.get().spawn;
        source.sendSuccess(() -> Component.literal("Diseño previsto del spawn de Kanto:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal("• 0–" + spawn.protectedCoreRadius
                + " bloques: spawn/pueblo personalizado protegido.").withStyle(ChatFormatting.YELLOW), false);
        source.sendSuccess(() -> Component.literal("• " + spawn.protectedCoreRadius + "–"
                + spawn.nearbyHousingRadius + " bloques: zona cercana para casas y expansión de jugadores.")
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> Component.literal("• Desde ~" + spawn.nearbyHousingRadius
                + " bloques: rutas y progresión principal de Kanto.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int setSpawn(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ProgressionConfig.setSpawn(player);
        source.sendSuccess(() -> Component.literal("✓ Spawn base de EmiProgresion definido en tu posición: "
                        + player.blockPosition().toShortString())
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), true);
        return 1;
    }

    private static int validate(CommandSourceStack source) {
        ProgressionConfig config = ProgressionConfig.get();
        boolean spawnOk = config.spawn.defined;
        boolean radiiOk = config.spawn.nearbyHousingRadius > config.spawn.protectedCoreRadius
                && config.kanto.radius > config.spawn.nearbyHousingRadius;

        source.sendSuccess(() -> Component.literal("Validación base de Kanto:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
        source.sendSuccess(() -> Component.literal((spawnOk ? "✓" : "✕") + " Spawn personalizado definido")
                .withStyle(spawnOk ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.literal((radiiOk ? "✓" : "✕")
                        + " Núcleo, viviendas y región tienen márgenes válidos")
                .withStyle(radiiOk ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        source.sendSuccess(() -> Component.literal("○ Gimnasios, protecciones, duplicados y Liga se añadirán en las siguientes alphas antes de pregenerar.")
                .withStyle(ChatFormatting.GRAY), false);
        return spawnOk && radiiOk ? 1 : 0;
    }

    private static int reload(CommandSourceStack source) {
        ProgressionConfig.reload();
        source.sendSuccess(() -> Component.literal("EmiProgresion: configuración recargada.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }
}
