package com.andrewbristowx.emiprogresion.region;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AdventureRegionService {
    private static final Set<UUID> INTRO_SHOWN = new HashSet<>();
    private static long ticks;

    private AdventureRegionService() {}

    public static void onServerStarted(MinecraftServer server) {
        applyKantoBorder(server);
        EmiProgresion.LOGGER.info("Kanto prototype configured. World '{}' must exist as a registered dimension/world.", EmiProgresionConfig.get().kantoWorld);
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks % 20L != 0L) return;

        EmiProgresionConfig config = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(server, config.kantoWorld);
        if (kanto == null) return;

        for (ServerPlayer player : kanto.players()) {
            if (INTRO_SHOWN.add(player.getUUID())) {
                player.sendSystemMessage(Component.literal("✦ AVENTURA DE KANTO ✦")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("Comienza en el pueblo inicial y sigue la ruta hasta Brock.")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static ServerLevel getLevel(MinecraftServer server, String dimensionId) {
        ResourceLocation location = ResourceLocation.tryParse(dimensionId);
        if (location == null) return null;
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
        return server.getLevel(key);
    }

    public static void applyKantoBorder(MinecraftServer server) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(server, config.kantoWorld);
        if (kanto == null) return;

        WorldBorder border = kanto.getWorldBorder();
        border.setCenter(config.kantoSpawnX, config.kantoSpawnZ);
        border.setSize(config.kantoWorldBorderDiameter);
    }

    public static boolean isKanto(ServerLevel level) {
        ResourceLocation id = level.dimension().location();
        return id.toString().equals(EmiProgresionConfig.get().kantoWorld);
    }

    public static boolean isInsideBrockProtection(BlockPos pos) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        long dx = pos.getX() - c.brockX;
        long dz = pos.getZ() - c.brockZ;
        return dx * dx + dz * dz <= (long) c.brockProtectionRadius * c.brockProtectionRadius;
    }
}
