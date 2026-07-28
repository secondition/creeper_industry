package com.secondition.creeperindustry.client.logistics.launcher;

import com.secondition.creeperindustry.content.logistics.launcher.RocketLauncherMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RocketLauncherScreen extends AbstractContainerScreen<RocketLauncherMenu> {
    public RocketLauncherScreen(RocketLauncherMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); imageHeight = 166; inventoryLabelY = 72; }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.creeper_industry.rocket_launcher.launch"), button -> {
            if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, RocketLauncherMenu.BUTTON_LAUNCH);
        }).bounds(leftPos + 58, topPos + 58, 60, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { renderBackground(graphics, mouseX, mouseY, partialTick); super.render(graphics, mouseX, mouseY, partialTick); renderTooltip(graphics, mouseX, mouseY); }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF20262B);
        g.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 21, 0xFF3C535E);
        for (int x : new int[]{43,79,115}) { g.fill(leftPos + x, topPos + 34, leftPos + x + 18, topPos + 52, 0xFF101417); g.fill(leftPos + x + 2, topPos + 36, leftPos + x + 16, topPos + 50, 0xFF343D42); }
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 7, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("gui.creeper_industry.rocket_launcher.cooldown", menu.cooldown()), 8, 60, 0xC7D6DC, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xC7D6DC, false);
    }
}
