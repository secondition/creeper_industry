package com.secondition.creeperindustry.content.logistics.storage;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DiscBurnerScreen extends AbstractContainerScreen<DiscBurnerMenu> {
    public DiscBurnerScreen(DiscBurnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = 73;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int panelLeft = leftPos;
        int panelTop = topPos;
        int panelRight = panelLeft + imageWidth;
        int panelBottom = panelTop + imageHeight;

        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF251D17);
        guiGraphics.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xFF3B2F26);
        guiGraphics.fill(panelLeft + 4, panelTop + 4, panelRight - 4, panelTop + 20, 0xFF6A3E22);
        guiGraphics.fill(panelLeft + 4, panelTop + 24, panelRight - 4, panelTop + 68, 0xFF2A221C);
        guiGraphics.fill(panelLeft + 4, panelTop + 78, panelRight - 4, panelBottom - 4, 0xFF26201A);

        drawSlot(guiGraphics, leftPos + 43, topPos + 34);
        drawSlot(guiGraphics, leftPos + 115, topPos + 34);

        guiGraphics.fill(leftPos + 67, topPos + 39, leftPos + 109, topPos + 47, 0xFF171412);
        int progress = menu.getScaledProgress(40);
        if (progress > 0) {
            guiGraphics.fill(leftPos + 68, topPos + 40, leftPos + 68 + progress, topPos + 46, 0xFFE1711D);
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(guiGraphics, leftPos + 7 + column * 18, topPos + 83 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(guiGraphics, leftPos + 7 + column * 18, topPos + 141);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 7, 0xF4F1E8, false);
        guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.disc_burner.input"), 32, 22, 0xD5D0C6, false);
        guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.disc_burner.disc"), 111, 22, 0xD5D0C6, false);
        guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.disc_burner.status"), 67, 29, 0xD59C67, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xD5D0C6, false);
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF171412);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B837A);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF2A2521);
    }
}
