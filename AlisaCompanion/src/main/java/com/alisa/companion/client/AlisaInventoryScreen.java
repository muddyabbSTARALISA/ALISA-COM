package com.alisa.companion.client;

import com.alisa.companion.menu.AlisaInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

public class AlisaInventoryScreen extends AbstractContainerScreen<AlisaInventoryMenu> {
    public AlisaInventoryScreen(AlisaInventoryMenu menu, net.minecraft.world.entity.player.Inventory inventory,
                                Component title) {
        super(menu, inventory, title);
        this.imageWidth = 270;
        this.imageHeight = 178;
        this.inventoryLabelY = 148;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xEE151519);
        graphics.fill(left + 5, top + 5, left + imageWidth - 5, top + imageHeight - 5, 0xFF25252B);
        graphics.fill(left + 8, top + 8, left + 82, top + 142, 0xFF17171C);
        graphics.fill(left + 87, top + 8, left + imageWidth - 8, top + 139, 0xFF1C1C22);

        if (menu.getAlisa() != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics, left + 18, top + 126, 48, left + 48 - mouseX, top + 70 - mouseY, menu.getAlisa());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, "АЛИСА", 20, 12, 0xFFFFFF, false);
        graphics.drawString(this.font, "Личные вещи", 91, 8, 0xFFFFFF, false);
        graphics.drawString(this.font, "Инвентарь игрока", 91, 146, 0xBDBDC7, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
