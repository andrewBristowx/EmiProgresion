package com.andrewbristowx.emiprogresion.story;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class TravelStopBlocks {
    public static final TravelStopBlock POKESTOP = new TravelStopBlock(false,
            Block.Properties.of().strength(3.0F).noOcclusion().lightLevel(state -> 8));
    public static final TravelStopBlock TERMINAL = new TravelStopBlock(true,
            Block.Properties.of().strength(4.0F).noOcclusion().lightLevel(state -> 12));
    public static BlockEntityType<TravelStopBlockEntity> BLOCK_ENTITY;

    private TravelStopBlocks() {}

    public static void register() {
        registerBlock("pokestop", POKESTOP);
        registerBlock("poketerminal", TERMINAL);
        BLOCK_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id("travel_stop"),
                BlockEntityType.Builder.of(TravelStopBlockEntity::new, POKESTOP, TERMINAL).build(null));
    }

    private static void registerBlock(String name, Block block) {
        ResourceLocation id = id(name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(EmiProgresion.MOD_ID, path);
    }
}
