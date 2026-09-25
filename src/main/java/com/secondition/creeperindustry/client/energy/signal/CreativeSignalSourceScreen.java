package com.secondition.creeperindustry.client.energy.signal;

import com.secondition.creeperindustry.content.energy.signal.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class CreativeSignalSourceScreen extends AbstractContainerScreen<CreativeSignalSourceMenu> {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 225;
    private static final int CONTROL_LEFT_OFFSET = 14;
    private static final int CONTROL_WIDTH = 192;
    private static final int TYPE_BUTTON_WIDTH = 93;
    private static final int TYPE_BUTTON_GAP = 6;
    private static final float HINT_SCALE = 0.8F;

    private Button pulseTypeButton;
    private Button continuousTypeButton;
    private Button emitPulseButton;
    private Button applyFrequencyButton;
    private SignalValueSlider amplitudeSlider;
    private EditBox frequencyBox;

    public CreativeSignalSourceScreen(
            CreativeSignalSourceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int controlsLeft = leftPos + CONTROL_LEFT_OFFSET;
        int typeTop = topPos + 32;

        pulseTypeButton =
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable(
                                                "gui.creeper_industry.creative_signal_source.pulse"),
                                        button ->
                                                sendMenuButton(
                                                        CreativeSignalSourceMenu
                                                                .setPulseButtonId()))
                                .bounds(controlsLeft, typeTop, TYPE_BUTTON_WIDTH, 20)
                                .build());
        continuousTypeButton =
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable(
                                                "gui.creeper_industry.creative_signal_source.continuous"),
                                        button ->
                                                sendMenuButton(
                                                        CreativeSignalSourceMenu
                                                                .setContinuousButtonId()))
                                .bounds(
                                        controlsLeft + TYPE_BUTTON_WIDTH + TYPE_BUTTON_GAP,
                                        typeTop,
                                        TYPE_BUTTON_WIDTH,
                                        20)
                                .build());

        amplitudeSlider =
                addRenderableWidget(
                        new SignalValueSlider(controlsLeft, typeTop + 30, CONTROL_WIDTH));
        frequencyBox = addRenderableWidget(new EditBox(font, controlsLeft + 75,
                typeTop + 113, 77, 20,
                Component.translatable("gui.creeper_industry.creative_signal_source.frequency")));
        frequencyBox.setMaxLength(24);
        applyFrequencyButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.creeper_industry.creative_signal_source.apply"),
                button -> applyFrequency())
                .bounds(controlsLeft + 156, typeTop + 113, 36, 20).build());
        emitPulseButton =
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable(
                                                "gui.creeper_industry.creative_signal_source.emit_pulse"),
                                        button ->
                                                sendMenuButton(
                                                        CreativeSignalSourceMenu.emitPulseButtonId()))
                                .bounds(controlsLeft, typeTop + 141, CONTROL_WIDTH, 20)
                                .build());
        syncWidgets();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        syncWidgets();
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

        guiGraphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0xFF121820);
        guiGraphics.fill(panelLeft + 1, panelTop + 1, panelRight - 1, panelBottom - 1, 0xFF24303A);
        guiGraphics.fill(panelLeft + 4, panelTop + 4, panelRight - 4, panelTop + 22, 0xFF4B5F35);
        guiGraphics.fill(panelLeft + 4, panelTop + 25, panelRight - 4, panelBottom - 4, 0xFF182028);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 7, 0xF4F1E8, false);
        guiGraphics.drawString(
                font,
                Component.translatable("gui.creeper_industry.creative_signal_source.type"),
                CONTROL_LEFT_OFFSET,
                22,
                0xD5D0C6,
                false);

        CreativeSignalSourceSignalType type = menu.getSignalType();
        if (type == CreativeSignalSourceSignalType.CONTINUOUS) {
            guiGraphics.drawString(font, Component.translatable(
                    "gui.creeper_industry.creative_signal_source.wavelength",
                    menu.getWavelength() == 0
                            ? "1/" + menu.getFrequencyNumerator() : menu.getWavelength()),
                    CONTROL_LEFT_OFFSET, 125, 0xD5D0C6, false);
            guiGraphics.drawString(font, Component.translatable(
                    "gui.creeper_industry.creative_signal_source.frequency"),
                    CONTROL_LEFT_OFFSET, 151, 0xD5D0C6, false);
        }
        Component hint =
                Component.translatable(
                        "gui.creeper_industry.creative_signal_source."
                                + (type == CreativeSignalSourceSignalType.PULSE ? "pulse_hint"
                                        : "continuous_hint"));
        drawScaledHint(guiGraphics, hint, CONTROL_LEFT_OFFSET, 211);
    }

    private void drawScaledHint(GuiGraphics guiGraphics, Component hint, int x, int y) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(HINT_SCALE, HINT_SCALE, 1.0F);
        guiGraphics.drawString(
                font,
                hint,
                Math.round(x / HINT_SCALE),
                Math.round(y / HINT_SCALE),
                0xAFC8A5,
                false);
        guiGraphics.pose().popPose();
    }

    private void syncWidgets() {
        if (pulseTypeButton == null) {
            return;
        }

        CreativeSignalSourceSignalType type = menu.getSignalType();
        pulseTypeButton.active = type != CreativeSignalSourceSignalType.PULSE;
        continuousTypeButton.active = type != CreativeSignalSourceSignalType.CONTINUOUS;
        emitPulseButton.visible = type == CreativeSignalSourceSignalType.PULSE;
        emitPulseButton.active = emitPulseButton.visible && menu.getAmplitude() != 0;
        frequencyBox.visible = type == CreativeSignalSourceSignalType.CONTINUOUS;
        applyFrequencyButton.visible = frequencyBox.visible;

        amplitudeSlider.syncTo(menu.getAmplitude());
        if (!frequencyBox.isFocused()) frequencyBox.setValue(
                menu.getFrequencyNumerator() + "/" + menu.getFrequencyDenominator());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (frequencyBox != null && frequencyBox.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            applyFrequency();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void applyFrequency() {
        try {
            String value = frequencyBox.getValue().trim();
            int n = value.startsWith("1/") ? 1 : Integer.parseInt(value);
            int d = value.startsWith("1/") ? Integer.parseInt(value.substring(2)) : 1;
            if (!SignalDefinition.validFrequency(n, d)) throw new NumberFormatException();
            frequencyBox.setTextColor(0xFFE0E0E0);
            sendMenuButton(CreativeSignalSourceMenu.frequencyButtonId(n, d));
            frequencyBox.setFocused(false);
        } catch (ArithmeticException | IllegalArgumentException ex) {
            frequencyBox.setTextColor(0xFFFF5555);
        }
    }

    private void sendMenuButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private final class SignalValueSlider extends AbstractSliderButton {
        private boolean syncing;

        private SignalValueSlider(int x, int y, int width) {
            super(x, y, width, 20, Component.empty(), sliderValue(menu.getAmplitude()));
            updateMessage();
        }

        private void syncTo(int currentValue) {
            syncing = true;
            value = sliderValue(currentValue);
            updateMessage();
            syncing = false;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(
                    "gui.creeper_industry.creative_signal_source.amplitude", valueFromSlider()));
        }

        @Override
        protected void applyValue() {
            if (syncing) return;
            sendMenuButton(CreativeSignalSourceMenu.amplitudeButtonId(valueFromSlider()));
        }

        private int valueFromSlider() {
            return (int) Math.round(value * 128) - 64;
        }
    }

    private static double sliderValue(int currentValue) {
        return (currentValue + 64) / 128.0;
    }
}
