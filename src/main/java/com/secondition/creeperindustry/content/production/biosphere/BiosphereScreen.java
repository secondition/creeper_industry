package com.secondition.creeperindustry.content.production.biosphere;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BiosphereScreen extends AbstractContainerScreen<BiosphereMenu> {
    public BiosphereScreen(BiosphereMenu menu, Inventory playerInventory, Component title) {
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

        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF24211E);
        guiGraphics.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xFF3A342F);
        guiGraphics.fill(panelLeft + 4, panelTop + 4, panelRight - 4, panelTop + 20, 0xFF2B5D4C);
        guiGraphics.fill(panelLeft + 4, panelTop + 24, panelRight - 4, panelTop + 68, 0xFF2B2724);
        guiGraphics.fill(panelLeft + 4, panelTop + 78, panelRight - 4, panelBottom - 4, 0xFF2A2521);

        drawSlot(guiGraphics, leftPos + 43, topPos + 34);
        if (menu.getBiosphereType() != BiosphereType.MONSTER) {
            drawSlot(guiGraphics, leftPos + 79, topPos + 34);
        }
        drawSlot(guiGraphics, leftPos + 115, topPos + 34);

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
        switch (menu.getBiosphereType()) {
            case BOTANICAL -> {
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.sapling"), 33, 22, 0xD5D0C6, false);
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.bonemeal"), 67, 22, 0xD5D0C6, false);
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.output"), 117, 22, 0xD5D0C6, false);
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.signal_hint"), 8, 58, 0xB8D7C9, false);
            }
            case ZOOLOGICAL -> {
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.animal_template"), 25, 22, 0xD5D0C6, false);
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.feed"), 73, 22, 0xD5D0C6, false);
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.animal_product"), 110, 22, 0xD5D0C6, false);
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.zoological_hint"), 8, 48, 0xB8D7C9, false);
                guiGraphics.drawString(font, getSelectionLine(), 8, 58, 0xD5D0C6, false);
            }
            case MONSTER -> {
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.spawn_egg"), 29, 22, 0xD5D0C6, false);
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.drop"), 116, 22, 0xD5D0C6, false);
                guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.biosphere.monster_hint"), 8, 48, 0xB8D7C9, false);
                guiGraphics.drawString(font, getSelectionLine(), 8, 58, 0xD5D0C6, false);
            }
        }
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xD5D0C6, false);
    }

    private Component getSelectionLine() {
        BiosphereBlockEntity biosphere = menu.getBiosphereBlockEntity();
        if (biosphere == null) {
            return Component.translatable("gui.creeper_industry.biosphere.no_output");
        }

        ItemStack preview = biosphere.getSelectedOutputPreview();
        if (preview.isEmpty()) {
            return Component.translatable("gui.creeper_industry.biosphere.no_output");
        }

        return Component.translatable("gui.creeper_industry.biosphere.selected_output", preview.getHoverName());
    }

    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xFF171412);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF8B837A);
        guiGraphics.fill(x + 2, y + 2, x + 16, y + 16, 0xFF2A2521);
    }
}
