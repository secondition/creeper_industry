package com.secondition.creeperindustry.client.snapshot;

import com.secondition.creeperindustry.content.snapshot.SnapshotTableMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SnapshotTableScreen extends AbstractContainerScreen<SnapshotTableMenu> {
    public SnapshotTableScreen(SnapshotTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 140;
    }

    @Override protected void init() {
        super.init();
        int x = leftPos + 10;
        int y = topPos + 30;
        for (int i = 0; i < 4; i++) {
            int slowdown = 1 << i;
            int buttonId = i + 1;
            addRenderableWidget(Button.builder(Component.literal(slowdown + "×"),
                    button -> sendMenuButton(buttonId)).bounds(x + i * 50, y, 45, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.creeper_industry.snapshot.enter"),
                button -> sendMenuButton(10)).bounds(x, y + 30, 95, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.creeper_industry.snapshot.replay"),
                button -> sendMenuButton(11)).bounds(x + 100, y + 30, 95, 20).build());
    }

    private void sendMenuButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null)
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
    }

    @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xff182028);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 8, 0xfff4f1e8, false);
        graphics.drawString(font, Component.translatable("gui.creeper_industry.snapshot.slowdown"), 10, 20, 0xffd5d0c6, false);
    }
}
