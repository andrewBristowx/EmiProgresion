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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.CommandSourceStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class AdventureRegionService {
    private static final Set<UUID> INTRO_SHOWN = new HashSet<>();
    private static long ticks;

    private AdventureRegionService() {}

    public static void onServerStarted(MinecraftServer server) {
        applyKantoBorder(server);
        EmiProgresion.LOGGER.info("Kanto adventure dimension: {}", EmiProgresionConfig.get().kantoWorld);
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
                player.sendSystemMessage(Component.literal("Pueblo inicial → Ruta 1 → Brock. Sigue hacia Z +" + config.brockZ + ".")
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

    public static boolean enterKanto(ServerPlayer player) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(player.server, c.kantoWorld);
        if (kanto == null) {
            player.sendSystemMessage(Component.literal("No se encontró la dimensión Kanto: " + c.kantoWorld)
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        int y = safeSurfaceY(kanto, c.kantoSpawnX, c.kantoSpawnZ);
        player.teleportTo(kanto, c.kantoSpawnX + 0.5D, y, c.kantoSpawnZ + 0.5D, player.getYRot(), player.getXRot());

        if (c.autoSetKantoSeriesOnEnter) {
            activateKantoSeries(player);
        }

        player.sendSystemMessage(Component.literal("Has entrado a Kanto. Brock está aproximadamente en Z +" + c.brockZ + ".")
                .withStyle(ChatFormatting.GREEN));
        return true;
    }

    public static boolean leaveKanto(ServerPlayer player) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel main = getLevel(player.server, c.mainWorld);
        if (main == null) {
            player.sendSystemMessage(Component.literal("No se encontró el mundo principal: " + c.mainWorld)
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        player.teleportTo(main, c.mainSpawnX + 0.5D, c.mainSpawnY, c.mainSpawnZ + 0.5D, player.getYRot(), player.getXRot());
        return true;
    }

    public static boolean teleportToKantoPoint(ServerPlayer player, int x, int z) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(player.server, c.kantoWorld);
        if (kanto == null) return false;
        int y = safeSurfaceY(kanto, x, z);
        player.teleportTo(kanto, x + 0.5D, y, z + 0.5D, player.getYRot(), player.getXRot());
        return true;
    }

    public static int setupPrototype(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        int ash = placeStructure(source, c.ashStructureId, c.ashX, c.ashY, c.ashZ);
        int brock = placeStructure(source, c.brockStructureId, c.brockX, c.brockY, c.brockZ);
        applyKantoBorder(source.getServer());
        return (ash > 0 ? 1 : 0) + (brock > 0 ? 1 : 0);
    }

    public static int placeStructure(CommandSourceStack source, String structureId, int x, int y, int z) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(source.getServer(), c.kantoWorld);
        if (kanto == null || structureId == null || structureId.isBlank()) return 0;

        CommandSourceStack target = source.withLevel(kanto).withPosition(new Vec3(x + 0.5D, y, z + 0.5D));
        String command = "place structure " + structureId + " " + x + " " + y + " " + z;
        source.getServer().getCommands().performPrefixedCommand(target, command);
        return 1;
    }

    public static boolean isKanto(ServerLevel level) {
        return level.dimension().location().toString().equals(EmiProgresionConfig.get().kantoWorld);
    }

    public static boolean isInsideBrockProtection(BlockPos pos) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        long dx = pos.getX() - c.brockX;
        long dz = pos.getZ() - c.brockZ;
        return dx * dx + dz * dz <= (long) c.brockProtectionRadius * c.brockProtectionRadius;
    }

    private static int safeSurfaceY(ServerLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1;
    }

    private static void activateKantoSeries(ServerPlayer player) {
        String command = "execute as " + player.getScoreboardName() + " run rctmod player set series kanto @s";
        player.server.getCommands().performPrefixedCommand(
                player.server.createCommandSourceStack().withSuppressedOutput(),
                command
        );
    }
}
