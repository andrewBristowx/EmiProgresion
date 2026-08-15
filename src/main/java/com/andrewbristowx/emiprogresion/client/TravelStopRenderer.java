package com.andrewbristowx.emiprogresion.client;

import com.andrewbristowx.emiprogresion.story.TravelStopBlock;
import com.andrewbristowx.emiprogresion.story.TravelStopBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public final class TravelStopRenderer implements BlockEntityRenderer<TravelStopBlockEntity> {
    public TravelStopRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(TravelStopBlockEntity entity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (entity.getLevel() == null || !(entity.getBlockState().getBlock() instanceof TravelStopBlock stop)) return;
        boolean terminal = stop.terminal();
        float time = entity.getLevel().getGameTime() + partialTick;
        float bob = (float) Math.sin(time * 0.08F) * 0.08F;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.45D + bob, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * (terminal ? 1.8F : 1.2F)));
        float scale = terminal ? 0.72F : 0.58F;
        poseStack.scale(scale, scale, scale);
        ItemStack shell = new ItemStack(terminal ? Blocks.PURPLE_STAINED_GLASS : Blocks.LIGHT_BLUE_STAINED_GLASS);
        Minecraft.getInstance().getItemRenderer().renderStatic(shell, ItemDisplayContext.NONE, packedLight,
                packedOverlay, poseStack, buffers, entity.getLevel(), entity.getBlockPos().hashCode());
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.45D + bob, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-time * 2.2F));
        float iconScale = terminal ? 0.42F : 0.34F;
        poseStack.scale(iconScale, iconScale, iconScale);
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("cobblemon",
                terminal ? "ancient_poke_ball" : "poke_ball"));
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(item), ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, buffers, entity.getLevel(), entity.getBlockPos().hashCode() + 1);
        poseStack.popPose();
    }
}
