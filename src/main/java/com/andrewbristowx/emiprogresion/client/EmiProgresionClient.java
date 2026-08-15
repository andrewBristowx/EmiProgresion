package com.andrewbristowx.emiprogresion.client;

import com.andrewbristowx.emiprogresion.network.StoryNetworking;
import com.andrewbristowx.emiprogresion.story.TravelStopBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;

public final class EmiProgresionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BlockEntityRenderers.register(TravelStopBlocks.BLOCK_ENTITY, TravelStopRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(StoryNetworking.OpenDialoguePayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().setScreen(new StoryDialogueScreen(context.client().screen, payload.json()))));
    }
}
