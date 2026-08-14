package com.andrewbristowx.emiprogresion.region;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.andrewbristowx.emiprogresion.config.EmiProgresionConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
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

        BlockPos safe = findSafeSurface(kanto, c.kantoSpawnX, c.kantoSpawnZ);
        player.teleportTo(kanto, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                player.getYRot(), player.getXRot());

        if (c.autoSetKantoSeriesOnEnter) {
            activateKantoSeries(player);
        }

        player.sendSystemMessage(Component.literal("Has entrado a Kanto en superficie segura Y=" + safe.getY()
                        + ". Brock está aproximadamente en Z +" + c.brockZ + ".")
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

        BlockPos preferred = new BlockPos(c.mainSpawnX, c.mainSpawnY, c.mainSpawnZ);
        BlockPos destination = isSafeStandPosition(main, preferred)
                ? preferred
                : findSafeSurface(main, c.mainSpawnX, c.mainSpawnZ);
        player.teleportTo(main, destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        return true;
    }

    public static boolean teleportToKantoPoint(ServerPlayer player, int x, int z) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(player.server, c.kantoWorld);
        if (kanto == null) return false;

        BlockPos safe = findSafeSurface(kanto, x, z);
        player.teleportTo(kanto, safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("TP seguro: " + safe.getX() + " " + safe.getY() + " " + safe.getZ())
                .withStyle(ChatFormatting.GRAY));
        return true;
    }

    public static int setupPrototype(CommandSourceStack source) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(source.getServer(), c.kantoWorld);
        if (kanto == null) {
            source.sendFailure(Component.literal("No existe la dimensión Kanto: " + c.kantoWorld));
            return 0;
        }

        int ashY = structureSurfaceY(kanto, c.ashX, c.ashZ);
        int brockY = structureSurfaceY(kanto, c.brockX, c.brockZ);

        int ash = placeStructure(source, c.ashStructureId, c.ashX, ashY, c.ashZ);
        int brock = placeStructure(source, c.brockStructureId, c.brockX, brockY, c.brockZ);
        applyKantoBorder(source.getServer());

        if (ash > 0) {
            source.sendSuccess(() -> Component.literal("Ash colocado en superficie: " + c.ashX + " " + ashY + " " + c.ashZ)
                    .withStyle(ChatFormatting.GREEN), false);
        }
        if (brock > 0) {
            source.sendSuccess(() -> Component.literal("Brock colocado en superficie: " + c.brockX + " " + brockY + " " + c.brockZ)
                    .withStyle(ChatFormatting.GREEN), false);
        }
        return (ash > 0 ? 1 : 0) + (brock > 0 ? 1 : 0);
    }

    public static int placeStructureAtSurface(CommandSourceStack source, String structureId, int x, int z) {
        EmiProgresionConfig c = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(source.getServer(), c.kantoWorld);
        if (kanto == null) return 0;
        int y = structureSurfaceY(kanto, x, z);
        return placeStructure(source, structureId, x, y, z);
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

    public static int getSurfaceY(ServerLevel level, int x, int z) {
        return structureSurfaceY(level, x, z);
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

    private static int structureSurfaceY(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return Math.max(level.getMinBuildHeight() + 2, Math.min(y, level.getMaxBuildHeight() - 3));
    }

    private static BlockPos findSafeSurface(ServerLevel level, int x, int z) {
        int baseY = structureSurfaceY(level, x, z);
        BlockPos candidate = new BlockPos(x, baseY, z);
        if (isSafeStandPosition(level, candidate)) return candidate;

        int maxY = Math.min(level.getMaxBuildHeight() - 3, baseY + 12);
        for (int y = baseY + 1; y <= maxY; y++) {
            candidate = new BlockPos(x, y, z);
            if (isSafeStandPosition(level, candidate)) return candidate;
        }

        int minY = Math.max(level.getMinBuildHeight() + 2, baseY - 24);
        for (int y = baseY - 1; y >= minY; y--) {
            candidate = new BlockPos(x, y, z);
            if (isSafeStandPosition(level, candidate)) return candidate;
        }

        return new BlockPos(x, baseY, z);
    }

    private static boolean isSafeStandPosition(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockPos head = feet.above();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }

    private static void activateKantoSeries(ServerPlayer player) {
        String command = "execute as " + player.getScoreboardName() + " run rctmod player set series kanto @s";
        player.server.getCommands().performPrefixedCommand(
                player.server.createCommandSourceStack().withSuppressedOutput(),
                command
        );
    }
}
