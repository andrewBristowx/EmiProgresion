package com.andrewbristowx.emiprogresion.network;

import com.andrewbristowx.emiprogresion.EmiProgresion;
import com.andrewbristowx.emiprogresion.story.DialogueState;
import com.andrewbristowx.emiprogresion.story.StoryService;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class StoryNetworking {
    private static final Gson GSON = new Gson();

    private StoryNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(OpenDialoguePayload.TYPE, OpenDialoguePayload.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(DialogueActionPayload.TYPE, DialogueActionPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(DialogueActionPayload.TYPE,
                (payload, context) -> StoryService.handleAction(context.player(), payload.dialogueId(), payload.action()));
    }

    public static void open(ServerPlayer player, DialogueState state) {
        if (!ServerPlayNetworking.canSend(player, OpenDialoguePayload.TYPE)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    state.speaker() + ": " + String.join(" ", state.pages())));
            return;
        }
        ServerPlayNetworking.send(player, new OpenDialoguePayload(GSON.toJson(state)));
    }

    public record OpenDialoguePayload(String json) implements CustomPacketPayload {
        public static final Type<OpenDialoguePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(EmiProgresion.MOD_ID, "open_dialogue"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialoguePayload> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, OpenDialoguePayload::json, OpenDialoguePayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DialogueActionPayload(String dialogueId, String action) implements CustomPacketPayload {
        public static final Type<DialogueActionPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(EmiProgresion.MOD_ID, "dialogue_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DialogueActionPayload> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, DialogueActionPayload::dialogueId,
                        ByteBufCodecs.STRING_UTF8, DialogueActionPayload::action,
                        DialogueActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
