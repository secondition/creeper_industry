package com.secondition.creeperindustry.client.logistics.dropper;

import com.secondition.creeperindustry.content.logistics.dropper.PrecisionDropperMenu;
import com.secondition.creeperindustry.content.logistics.dropper.PrecisionDropperPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

@OnlyIn(Dist.CLIENT)
public class PrecisionDropperScreen extends AbstractContainerScreen<PrecisionDropperMenu> {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 154;
    private static final int CONTROL_LEFT_OFFSET = 14;
    private static final int CONTROL_WIDTH = 192;
    private static final int CONTROL_HEIGHT = 20;

    private EditBox targetXInput;
    private EditBox targetZInput;
    private Button saveButton;
    private Button launchButton;

    public PrecisionDropperScreen(PrecisionDropperMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int left = leftPos + CONTROL_LEFT_OFFSET;
        int firstRowTop = topPos + 48;
        int inputWidth = (CONTROL_WIDTH - 8) / 2;

        targetXInput = createCoordinateInput(
                left,
                firstRowTop,
                inputWidth,
                Component.translatable("gui.creeper_industry.precision_dropper.target_x"),
                menu.targetX()
        );
        targetZInput = createCoordinateInput(
                left + inputWidth + 8,
                firstRowTop,
                inputWidth,
                Component.translatable("gui.creeper_industry.precision_dropper.target_z"),
                menu.targetZ()
        );

        int buttonTop = firstRowTop + 40;
        int buttonWidth = (CONTROL_WIDTH - 8) / 2;
        saveButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.creeper_industry.precision_dropper.save"),
                button -> submit(false)
        ).bounds(left, buttonTop, buttonWidth, CONTROL_HEIGHT).build());
        launchButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.creeper_industry.precision_dropper.launch"),
                button -> submit(true)
        ).bounds(left + buttonWidth + 8, buttonTop, buttonWidth, CONTROL_HEIGHT).build());
        updateButtonState();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int right = leftPos + imageWidth;
        int bottom = topPos + imageHeight;
        guiGraphics.fill(leftPos, topPos, right, bottom, 0xFF10161B);
        guiGraphics.fill(leftPos + 1, topPos + 1, right - 1, bottom - 1, 0xFF29353D);
        guiGraphics.fill(leftPos + 4, topPos + 4, right - 4, topPos + 22, 0xFF6A3C25);
        guiGraphics.fill(leftPos + 4, topPos + 25, right - 4, bottom - 4, 0xFF182127);
        guiGraphics.fill(leftPos + 10, topPos + 31, right - 10, bottom - 10, 0xFF10171C);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 7, 0xF4F1E8, false);
        guiGraphics.drawString(
                font,
                Component.translatable("gui.creeper_industry.precision_dropper.target"),
                CONTROL_LEFT_OFFSET,
                27,
                0xE8C49C,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("gui.creeper_industry.precision_dropper.target_x"),
                CONTROL_LEFT_OFFSET,
                38,
                0xC8D2D7,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("gui.creeper_industry.precision_dropper.target_z"),
                CONTROL_LEFT_OFFSET + 100,
                38,
                0xC8D2D7,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("gui.creeper_industry.precision_dropper.hint"),
                CONTROL_LEFT_OFFSET,
                112,
                0xAFC8A5,
                false
        );
    }

    private EditBox createCoordinateInput(int x, int y, int width, Component hint, int value) {
        EditBox input = addRenderableWidget(new EditBox(font, x, y, width, CONTROL_HEIGHT, hint));
        input.setMaxLength(9);
        input.setFilter(text -> text.matches("-?\\d*"));
        input.setHint(hint);
        input.setValue(Integer.toString(value));
        input.setResponder(ignored -> updateButtonState());
        return input;
    }

    private void updateButtonState() {
        if (saveButton == null || launchButton == null) {
            return;
        }
        boolean valid = parseCoordinate(targetXInput) != null && parseCoordinate(targetZInput) != null;
        saveButton.active = valid;
        launchButton.active = valid;
    }

    private void submit(boolean launch) {
        Integer targetX = parseCoordinate(targetXInput);
        Integer targetZ = parseCoordinate(targetZInput);
        if (targetX == null || targetZ == null) {
            return;
        }
        PacketDistributor.sendToServer(new PrecisionDropperPayload(
                menu.dropperPos(),
                menu.containerId,
                targetX,
                targetZ,
                launch
        ));
    }

    private static Integer parseCoordinate(EditBox input) {
        try {
            return Integer.valueOf(input.getValue());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
