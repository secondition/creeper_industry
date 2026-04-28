package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreativeSignalSourceScreen extends AbstractContainerScreen<CreativeSignalSourceMenu> {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 158;
    private static final int CONTROL_LEFT_OFFSET = 14;
    private static final int CONTROL_WIDTH = 192;
    private static final int TYPE_BUTTON_WIDTH = 92;
    private static final int TYPE_BUTTON_GAP = 8;
    private static final float HINT_SCALE = 0.8F;

    private Button pulseTypeButton;
    private Button continuousTypeButton;
    private Button emitPulseButton;
    private SignalValueSlider amplitudeSlider;
    private SignalValueSlider periodSlider;

    public CreativeSignalSourceScreen(CreativeSignalSourceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int controlsLeft = leftPos + CONTROL_LEFT_OFFSET;
        int typeTop = topPos + 32;

        pulseTypeButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.creeper_industry.creative_signal_source.pulse"),
                button -> sendMenuButton(CreativeSignalSourceMenu.setPulseButtonId())
        ).bounds(controlsLeft, typeTop, TYPE_BUTTON_WIDTH, 20).build());
        continuousTypeButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.creeper_industry.creative_signal_source.continuous"),
                button -> sendMenuButton(CreativeSignalSourceMenu.setContinuousButtonId())
        ).bounds(controlsLeft + TYPE_BUTTON_WIDTH + TYPE_BUTTON_GAP, typeTop, TYPE_BUTTON_WIDTH, 20).build());

        amplitudeSlider = addRenderableWidget(new SignalValueSlider(
                controlsLeft,
                typeTop + 30,
                CONTROL_WIDTH,
                Component.translatable("gui.creeper_industry.creative_signal_source.amplitude", menu.getAmplitude()),
                true
        ));
        periodSlider = addRenderableWidget(new SignalValueSlider(
                controlsLeft,
                typeTop + 58,
                CONTROL_WIDTH,
                Component.translatable("gui.creeper_industry.creative_signal_source.period", menu.getPeriodTicks()),
                false
        ));
        emitPulseButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.creeper_industry.creative_signal_source.emit_pulse"),
                button -> sendMenuButton(CreativeSignalSourceMenu.emitPulseButtonId())
        ).bounds(controlsLeft, typeTop + 88, CONTROL_WIDTH, 20).build());

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
        guiGraphics.drawString(font, Component.translatable("gui.creeper_industry.creative_signal_source.type"), CONTROL_LEFT_OFFSET, 22, 0xD5D0C6, false);

        CreativeSignalSourceSignalType type = menu.getSignalType();
        Component hint = type == CreativeSignalSourceSignalType.PULSE
                ? Component.translatable("gui.creeper_industry.creative_signal_source.pulse_hint")
                : Component.translatable("gui.creeper_industry.creative_signal_source.continuous_hint");
        drawScaledHint(guiGraphics, hint, CONTROL_LEFT_OFFSET, 145);
    }

    private void drawScaledHint(GuiGraphics guiGraphics, Component hint, int x, int y) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(HINT_SCALE, HINT_SCALE, 1.0F);
        guiGraphics.drawString(font, hint, Math.round(x / HINT_SCALE), Math.round(y / HINT_SCALE), 0xAFC8A5, false);
        guiGraphics.pose().popPose();
    }

    private void syncWidgets() {
        if (pulseTypeButton == null) {
            return;
        }

        CreativeSignalSourceSignalType type = menu.getSignalType();
        pulseTypeButton.active = type != CreativeSignalSourceSignalType.PULSE;
        continuousTypeButton.active = type != CreativeSignalSourceSignalType.CONTINUOUS;
        emitPulseButton.active = type == CreativeSignalSourceSignalType.PULSE && menu.getAmplitude() > 0;
        periodSlider.active = type == CreativeSignalSourceSignalType.CONTINUOUS;

        amplitudeSlider.syncTo(menu.getAmplitude());
        periodSlider.syncTo(menu.getPeriodTicks());
    }

    private void sendMenuButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private final class SignalValueSlider extends AbstractSliderButton {
        private final boolean amplitude;
        private boolean syncing;

        private SignalValueSlider(int x, int y, int width, Component message, boolean amplitude) {
            super(x, y, width, 20, message, sliderValue(amplitude, amplitude ? menu.getAmplitude() : menu.getPeriodTicks()));
            this.amplitude = amplitude;
            updateMessage();
        }

        private void syncTo(int currentValue) {
            syncing = true;
            value = sliderValue(amplitude, currentValue);
            updateMessage();
            syncing = false;
        }

        @Override
        protected void updateMessage() {
            int currentValue = valueFromSlider();
            setMessage(Component.translatable(
                    amplitude
                            ? "gui.creeper_industry.creative_signal_source.amplitude"
                            : "gui.creeper_industry.creative_signal_source.period",
                    currentValue
            ));
        }

        @Override
        protected void applyValue() {
            if (syncing) {
                return;
            }

            int currentValue = valueFromSlider();
            sendMenuButton(amplitude
                    ? CreativeSignalSourceMenu.amplitudeButtonId(currentValue)
                    : CreativeSignalSourceMenu.periodButtonId(currentValue));
        }

        private int valueFromSlider() {
            if (amplitude) {
                return (int) Math.round(value * CreativeSignalSourceBlockEntity.MAX_AMPLITUDE);
            }
            int periodIndex = (int) Math.round(value * (CreativeSignalSourceBlockEntity.MAX_PERIOD_TICKS / (double) CreativeSignalSourceBlockEntity.PERIOD_STEP));
            return periodIndex * CreativeSignalSourceBlockEntity.PERIOD_STEP;
        }
    }

    private static double sliderValue(boolean amplitude, int currentValue) {
        if (amplitude) {
            return currentValue / (double) CreativeSignalSourceBlockEntity.MAX_AMPLITUDE;
        }
        return currentValue / (double) CreativeSignalSourceBlockEntity.MAX_PERIOD_TICKS;
    }
}
