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
    private boolean notifyClose = true;

    StoryDialogueScreen(Screen parent, String json) {
        super(Component.literal("Historia de Kanto"));
        this.parent = parent;
        this.state = GSON.fromJson(json, DialogueState.class);
    }

    @Override
    protected void init() {
        clearWidgets();
        panelWidth = Math.min(920, width - 36);
        panelHeight = state.choices() != null && state.choices().size() > 4 ? 190 : 158;
        panelX = (width - panelWidth) / 2;
        panelY = height - panelHeight - 18;
        int bottom = panelY + panelHeight - 29;
        int right = panelX + panelWidth - 18;

        if (page < state.pages().size() - 1) {
            addRenderableWidget(Button.builder(Component.literal("Siguiente ▶"), button -> {
                page++;
                refreshPage();
            }).bounds(right - 104, bottom, 104, 20).build());
        } else {
            addChoiceButtons(bottom);
        }

        if (state.allowSkip() && page < state.pages().size() - 1) {
            addRenderableWidget(Button.builder(Component.literal("Saltar"), button -> {
                page = state.pages().size() - 1;
                refreshPage();
            }).bounds(panelX + 132, bottom, 62, 20).build());
        }
        if (state.allowExit()) {
            addRenderableWidget(Button.builder(Component.literal("Salir"), button -> onClose())
                    .bounds(panelX + 202, bottom, 62, 20).build());
        }
    }

    private void addChoiceButtons(int bottom) {
        List<DialogueState.Choice> choices = state.choices();
        if (choices == null || choices.isEmpty()) return;
        int columns = Math.min(4, choices.size());
        int gap = 6;
        int available = panelWidth - 294;
        int buttonWidth = Math.max(80, Math.min(150, (available - gap * (columns - 1)) / columns));
        int total = columns * buttonWidth + (columns - 1) * gap;
        int startX = panelX + panelWidth - 18 - total;
        for (int i = 0; i < choices.size(); i++) {
            DialogueState.Choice choice = choices.get(i);
            int x = startX + (i % columns) * (buttonWidth + gap);
            int y = bottom - (i / columns) * 23;
            addRenderableWidget(Button.builder(Component.literal(choice.label()), button -> select(choice.id()))
                    .bounds(x, y, buttonWidth, 20).build());
        }
    }

    private void select(String action) {
        if (ClientPlayNetworking.canSend(StoryNetworking.DialogueActionPayload.TYPE)) {
            ClientPlayNetworking.send(new StoryNetworking.DialogueActionPayload(state.id(), action));
        }
        notifyClose = false;
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private void refreshPage() {
        clearWidgets();
        init();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void drawPanel(GuiGraphics graphics) {
        graphics.fill(panelX + 5, panelY + 5, panelX + panelWidth + 5, panelY + panelHeight + 5, 0x78000000);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xEA101927);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 4, 0xFFFF5AA5);
        graphics.fill(panelX, panelY + 4, panelX + panelWidth, panelY + 7, 0xFF49D5ED);
        graphics.fill(panelX + 12, panelY + 34, panelX + panelWidth - 12, panelY + panelHeight - 31, 0xB81A293B);

        int portraitSize = 74;
        int textX = panelX + 24;
        if (state.portrait() != null && !state.portrait().isBlank()) {
            ResourceLocation texture = ResourceLocation.tryParse(state.portrait());
            if (texture != null) {
                graphics.fill(panelX + 20, panelY + 43, panelX + 20 + portraitSize,
                        panelY + 43 + portraitSize, 0xFF334B64);
                PlayerFaceRenderer.draw(graphics, texture, panelX + 24, panelY + 47,
                        portraitSize - 8, true, false);
                textX = panelX + 112;
            }
        }

        graphics.drawString(font, Component.literal(state.speaker()), panelX + 18, panelY + 17, 0xFFFFD8ED, true);
        graphics.drawString(font, Component.literal((page + 1) + "/" + Math.max(1, state.pages().size())),
                panelX + panelWidth - 43, panelY + 17, 0xFF9FDCEC, false);

        String text = state.pages().isEmpty() ? "" : state.pages().get(Math.min(page, state.pages().size() - 1));
        int wrapWidth = panelX + panelWidth - 34 - textX;
        List<FormattedCharSequence> lines = font.split(Component.literal(text), wrapWidth);
        int y = panelY + 46;
        for (FormattedCharSequence line : lines) {
            if (y > panelY + panelHeight - 58) break;
            graphics.drawString(font, line, textX, y, 0xFFF4F8FF, false);
            y += 14;
        }

        if (page == state.pages().size() - 1 && state.choices() != null && !state.choices().isEmpty()) {
            graphics.drawString(font, Component.literal("Elige una opción:"), textX,
                    panelY + panelHeight - 46, 0xFFFFD86A, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (notifyClose && ClientPlayNetworking.canSend(StoryNetworking.DialogueActionPayload.TYPE)) {
            ClientPlayNetworking.send(new StoryNetworking.DialogueActionPayload(state.id(), "close"));
        }
        notifyClose = false;
        if (minecraft != null) minecraft.setScreen(parent);
    }
}
