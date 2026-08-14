package com.andrewbristowx.emiprogresion.client;

import com.andrewbristowx.emiprogresion.network.StoryNetworking;
import com.andrewbristowx.emiprogresion.story.DialogueState;
import com.google.gson.Gson;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

final class StoryDialogueScreen extends Screen {
    private static final Gson GSON = new Gson();
    private final Screen parent;
    private final DialogueState state;
    private int page;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    StoryDialogueScreen(Screen parent, String json) {
        super(Component.literal("Historia de Kanto"));
        this.parent = parent;
        this.state = GSON.fromJson(json, DialogueState.class);
    }

    @Override
    protected void init() {
        clearWidgets();
        panelWidth = Math.min(650, width - 24);
        panelHeight = Math.min(330, height - 24);
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;
        int bottom = panelY + panelHeight - 36;
        int right = panelX + panelWidth - 18;

        if (page < state.pages().size() - 1) {
            addRenderableWidget(Button.builder(Component.literal("Siguiente ▶"), button -> {
                page++;
                refreshPage();
            }).bounds(right - 112, bottom, 112, 22).build());
        } else {
            addChoiceButtons(bottom);
        }

        if (state.allowSkip() && page < state.pages().size() - 1) {
            addRenderableWidget(Button.builder(Component.literal("Saltar"), button -> {
                page = state.pages().size() - 1;
                refreshPage();
            }).bounds(panelX + 18, bottom, 74, 22).build());
        }
        if (state.allowExit()) {
            addRenderableWidget(Button.builder(Component.literal("Salir"), button -> onClose())
                    .bounds(panelX + 98, bottom, 70, 22).build());
        }
    }

    private void addChoiceButtons(int bottom) {
        List<DialogueState.Choice> choices = state.choices();
        if (choices == null || choices.isEmpty()) return;
        int available = panelWidth - 205;
        int gap = 7;
        int buttonWidth = Math.min(150, (available - gap * (choices.size() - 1)) / choices.size());
        int total = choices.size() * buttonWidth + (choices.size() - 1) * gap;
        int x = panelX + panelWidth - 18 - total;
        for (DialogueState.Choice choice : choices) {
            addRenderableWidget(Button.builder(Component.literal(choice.label()), button -> select(choice.id()))
                    .bounds(x, bottom, buttonWidth, 22).build());
            x += buttonWidth + gap;
        }
    }

    private void select(String action) {
        if (ClientPlayNetworking.canSend(StoryNetworking.DialogueActionPayload.TYPE)) {
            ClientPlayNetworking.send(new StoryNetworking.DialogueActionPayload(state.id(), action));
        }
        onClose();
    }

    private void refreshPage() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA3000000);
        drawPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelX + 7, panelY + 8, panelX + panelWidth + 7, panelY + panelHeight + 8, 0x85000000);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xF20E1725);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 6, 0xFFFF4F9A);
        graphics.fill(panelX, panelY + 6, panelX + panelWidth, panelY + 10, 0xFF42C7E8);
        graphics.fill(panelX + 15, panelY + 47, panelX + panelWidth - 15, panelY + panelHeight - 51, 0xBE18283B);

        int portraitSize = 82;
        int textX = panelX + 30;
        if (state.portrait() != null && !state.portrait().isBlank()) {
            ResourceLocation texture = ResourceLocation.tryParse(state.portrait());
            if (texture != null) {
                graphics.fill(panelX + 24, panelY + 62, panelX + 24 + portraitSize,
                        panelY + 62 + portraitSize, 0xFF334B64);
                PlayerFaceRenderer.draw(graphics, texture, panelX + 29, panelY + 67,
                        portraitSize - 10, true, false);
                textX = panelX + 126;
            }
        }

        graphics.drawString(font, Component.literal(state.speaker()), panelX + 22, panelY + 25, 0xFFFFD8ED, true);
        graphics.drawString(font, Component.literal((page + 1) + "/" + Math.max(1, state.pages().size())),
                panelX + panelWidth - 47, panelY + 25, 0xFF9FDCEC, false);

        String text = state.pages().isEmpty() ? "" : state.pages().get(Math.min(page, state.pages().size() - 1));
        int wrapWidth = panelX + panelWidth - 34 - textX;
        List<FormattedCharSequence> lines = font.split(Component.literal(text), wrapWidth);
        int y = panelY + 66;
        for (FormattedCharSequence line : lines) {
            if (y > panelY + panelHeight - 92) break;
            graphics.drawString(font, line, textX, y, 0xFFF4F8FF, false);
            y += 14;
        }

        if (page == state.pages().size() - 1 && state.choices() != null && !state.choices().isEmpty()) {
            graphics.drawString(font, Component.literal("Elige una opción:"), textX,
                    panelY + panelHeight - 74, 0xFFFFD86A, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (ClientPlayNetworking.canSend(StoryNetworking.DialogueActionPayload.TYPE)) {
            ClientPlayNetworking.send(new StoryNetworking.DialogueActionPayload(state.id(), "close"));
        }
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
