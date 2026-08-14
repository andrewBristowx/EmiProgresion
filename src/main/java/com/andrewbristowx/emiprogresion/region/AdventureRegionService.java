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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AdventureRegionService {
    private static final Set<UUID> INTRO_SHOWN = new HashSet<>();
    private static long ticks;

    private AdventureRegionService() {}

    public static void onServerStarted(MinecraftServer server) {
        applyKantoBorder(server);
        EmiProgresionConfig config = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(server, config.kantoWorld);
        if (kanto != null && config.requireWildKantoMap && !hasWildKantoSignature(kanto)) {
            EmiProgresion.LOGGER.warn("Wild Kanto 1-00-02 map signature was not found in {}. Kanto entry is locked.", config.kantoWorld);
        }
        EmiProgresion.LOGGER.info("Kanto map-test dimension: {}", config.kantoWorld);
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks % 20L != 0L) return;

        EmiProgresionConfig config = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(server, config.kantoWorld);
        if (kanto == null) return;

        for (ServerPlayer player : kanto.players()) {
            if (INTRO_SHOWN.add(player.getUUID())) {
                player.sendSystemMessage(Component.literal("✦ PRUEBA DEL MAPA DE KANTO ✦")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                player.sendSystemMessage(Component.literal("Explora y reporta cortes de terreno, edificios incompletos o zonas inaccesibles.")
                        .withStyle(ChatFormatting.GRAY));
                player.sendSystemMessage(Component.literal("Los gimnasios del mundo normal son opcionales; la campaña oficial se validará únicamente aquí.")
                        .withStyle(ChatFormatting.YELLOW));
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
        border.setCenter(config.kantoBorderCenterX, config.kantoBorderCenterZ);
        border.setSize(config.kantoWorldBorderDiameter);
    }

    public static boolean enterKanto(ServerPlayer player) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        ServerLevel kanto = getLevel(player.server, config.kantoWorld);
        if (kanto == null) {
            player.sendSystemMessage(Component.literal("No se encontró la dimensión Kanto: " + config.kantoWorld)
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if (config.requireWildKantoMap && !hasWildKantoSignature(kanto)) {
            player.sendSystemMessage(Component.literal("Kanto bloqueado: no se detectó el mapa Wild Kanto limpio en la dimensión.")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("Importa region, entities y poi, reinicia el servidor y ejecuta /emiprogresion validate.")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }

        Optional<BlockPos> safe = findSafeNearExpectedY(
                kanto, config.kantoSpawnX, config.kantoSpawnY, config.kantoSpawnZ, 12
        );
        if (safe.isEmpty()) {
            player.sendSystemMessage(Component.literal("Kanto bloqueado: el spawn esperado del mapa no es seguro cerca de Y=" + config.kantoSpawnY + ".")
                    .withStyle(ChatFormatting.RED));
            player.sendSystemMessage(Component.literal("No se usará la bedrock como alternativa. Ejecuta /emiprogresion validate.")
                    .withStyle(ChatFormatting.YELLOW));
            return false;
        }

        BlockPos destination = safe.get();
        player.teleportTo(kanto, destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D,
                player.getYRot(), player.getXRot());

        if (config.autoSetKantoSeriesOnEnter) {
            activateKantoSeries(player);
        }

        player.sendSystemMessage(Component.literal("Has entrado al mapa de Kanto en "
                        + destination.getX() + " " + destination.getY() + " " + destination.getZ() + ".")
                .withStyle(ChatFormatting.GREEN));
        return true;
    }

    public static boolean leaveKanto(ServerPlayer player) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        ServerLevel main = getLevel(player.server, config.mainWorld);
        if (main == null) {
            player.sendSystemMessage(Component.literal("No se encontró el mundo principal: " + config.mainWorld)
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        BlockPos preferred = new BlockPos(config.mainSpawnX, config.mainSpawnY, config.mainSpawnZ);
        BlockPos destination = isSafeStandPosition(main, preferred)
                ? preferred
                : findSafeSurface(main, config.mainSpawnX, config.mainSpawnZ);
        player.teleportTo(main, destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D,
                player.getYRot(), player.getXRot());
        return true;
    }

    /**
     * Signature for the unmodified terrain around the Wild Kanto 1-00-02 spawn.
     * These blocks are deliberately not touched by the cleaner.
     */
    public static boolean hasWildKantoSignature(ServerLevel level) {
        return level.getBlockState(new BlockPos(87, 73, 130)).is(Blocks.STONE_BRICKS)
                && level.getBlockState(new BlockPos(80, 73, 133)).is(Blocks.MOSSY_COBBLESTONE)
                && level.getBlockState(new BlockPos(91, 73, 133)).is(Blocks.GRASS_BLOCK);
    }

    public static Optional<BlockPos> getSafeKantoSpawn(ServerLevel level) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        return findSafeNearExpectedY(level, config.kantoSpawnX, config.kantoSpawnY, config.kantoSpawnZ, 12);
    }

    public static boolean isInsideGeneratedRectangle(BlockPos pos) {
        EmiProgresionConfig config = EmiProgresionConfig.get();
        return pos.getX() >= config.kantoGeneratedMinX && pos.getX() <= config.kantoGeneratedMaxX
                && pos.getZ() >= config.kantoGeneratedMinZ && pos.getZ() <= config.kantoGeneratedMaxZ;
    }

    public static boolean isKanto(ServerLevel level) {
        return level.dimension().location().toString().equals(EmiProgresionConfig.get().kantoWorld);
    }

    private static Optional<BlockPos> findSafeNearExpectedY(ServerLevel level, int x, int expectedY, int z, int radius) {
        int minY = Math.max(level.getMinBuildHeight() + 1, expectedY - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 2, expectedY + radius);

        BlockPos expected = new BlockPos(x, expectedY, z);
        if (isSafeStandPosition(level, expected)) return Optional.of(expected);

        for (int offset = 1; offset <= radius; offset++) {
            int above = expectedY + offset;
            if (above <= maxY) {
                BlockPos candidate = new BlockPos(x, above, z);
                if (isSafeStandPosition(level, candidate)) return Optional.of(candidate);
            }
            int below = expectedY - offset;
            if (below >= minY) {
                BlockPos candidate = new BlockPos(x, below, z);
                if (isSafeStandPosition(level, candidate)) return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private static BlockPos findSafeSurface(ServerLevel level, int x, int z) {
        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos candidate = new BlockPos(x, baseY, z);
        if (isSafeStandPosition(level, candidate)) return candidate;

        int maxY = Math.min(level.getMaxBuildHeight() - 2, baseY + 12);
        for (int y = baseY + 1; y <= maxY; y++) {
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
