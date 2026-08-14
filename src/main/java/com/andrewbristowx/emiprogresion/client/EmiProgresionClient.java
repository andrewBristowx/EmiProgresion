package com.andrewbristowx.emiprogresion.client;

import com.andrewbristowx.emiprogresion.network.StoryNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class EmiProgresionClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(StoryNetworking.OpenDialoguePayload.TYPE,
                (payload, context) -> context.client().execute(() ->
                        context.client().setScreen(new StoryDialogueScreen(context.client().screen, payload.json()))));
    }
}
