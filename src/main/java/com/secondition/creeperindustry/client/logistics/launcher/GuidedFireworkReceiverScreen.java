package com.secondition.creeperindustry.client.logistics.launcher;

import com.secondition.creeperindustry.content.logistics.launcher.GuidedFireworkReceiverMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GuidedFireworkReceiverScreen extends AbstractContainerScreen<GuidedFireworkReceiverMenu> {
    public GuidedFireworkReceiverScreen(GuidedFireworkReceiverMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageHeight = 174; inventoryLabelY = 80; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.creeper_industry.receiver.toggle"), b -> click(GuidedFireworkReceiverMenu.BUTTON_TOGGLE_MODE)).bounds(leftPos + 8, topPos + 68, 76, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.creeper_industry.receiver.print"), b -> click(GuidedFireworkReceiverMenu.BUTTON_PRINT_ADDRESS)).bounds(leftPos + 92, topPos + 68, 76, 20).build());
    }
    private void click(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override public void render(GuiGraphics g, int x, int y, float tick) { renderBackground(g, x, y, tick); super.render(g, x, y, tick); renderTooltip(g, x, y); }
    @Override protected void renderBg(GuiGraphics g, float tick, int x, int y) { g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20262B); g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 21, 0xFF3C535E); }
    @Override protected void renderLabels(GuiGraphics g, int x, int y) { g.drawString(font, title, 8, 7, 0xFFFFFF, false); g.drawString(font, Component.translatable("gui.creeper_industry.receiver.mode", menu.mode().name()), 8, 22, 0xC7D6DC, false); g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC7D6DC, false); }
}
