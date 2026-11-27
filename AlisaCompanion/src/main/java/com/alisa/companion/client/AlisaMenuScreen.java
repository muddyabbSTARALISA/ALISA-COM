package com.alisa.companion.client;

import com.alisa.companion.entity.AlisaEntity;
import com.alisa.companion.network.AlisaNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AlisaMenuScreen extends Screen {
    private final int entityId;
    private final String relationship;
    private final double points;
    private final boolean staying;

    public AlisaMenuScreen(AlisaEntity alisa) {
        super(Component.literal("Алиса"));
        this.entityId = alisa.getId();
        this.relationship = alisa.getRelationshipStatus();
        this.points = alisa.getRelationshipPoints();
        this.staying = alisa.isOrderedToSit();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        int w = 150;
        int h = 20;
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal(staying ? "Открепить" : "Закрепить"),
            b -> AlisaNetwork.sendMenuAction(entityId, 4))
            .bounds(cx - 165, cy - 72, w, h).build());
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal("Диалог"), b -> AlisaNetwork.sendMenuAction(entityId, 3))
            .bounds(cx + 15, cy - 72, w, h).build());
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal("Инвентарь"), b -> AlisaNetwork.sendMenuAction(entityId, 5)).bounds(cx - 245, cy - 10, w, h).build());
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal("Спец. взаимодействия"), b -> {
                if (minecraft.player != null) minecraft.player.displayClientMessage(
                    Component.literal("Особые взаимодействия пока разрабатываются."), true);
            }).bounds(cx + 95, cy - 10, w, h).build());
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal("Задания"), b -> AlisaNetwork.sendMenuAction(entityId, 2))
            .bounds(cx - 165, cy + 62, w, h).build());
        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
            Component.literal("Настройки"), b -> {
                if (minecraft.player != null) minecraft.player.displayClientMessage(
                    Component.literal("Настройки поведения появятся в следующей части обновления."), true);
            }).bounds(cx + 15, cy + 62, w, h).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        int cx = this.width / 2;
        int cy = this.height / 2;
        graphics.drawCenteredString(this.font, "АЛИСА", cx, cy - 100, 0xFFFFFF);
        graphics.drawCenteredString(this.font, "♥", cx, cy - 78, 0xF06A9B);
        graphics.drawCenteredString(this.font, points + " / 99  ·  " + relationship, cx, cy - 64, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
