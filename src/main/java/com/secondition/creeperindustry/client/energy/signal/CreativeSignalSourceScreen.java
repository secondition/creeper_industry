package com.secondition.creeperindustry.client.energy.signal;

import com.secondition.creeperindustry.content.energy.signal.*;

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
    private static final int PANEL_HEIGHT = 225;
    private static final int CONTROL_LEFT_OFFSET = 14;
    private static final int CONTROL_WIDTH = 192;
    private static final int TYPE_BUTTON_WIDTH = 60;
    private static final int TYPE_BUTTON_GAP = 6;
    private static final float HINT_SCALE = 0.8F;

    private Button pulseTypeButton;
    private Button continuousTypeButton;
    private Button staticTypeButton;
    private Button emitPulseButton;
    private SignalValueSlider amplitudeSlider;
    private SignalValueSlider stagesSlider;
    private SignalValueSlider stageLengthSlider;
    private Button previousPhaseButton;
    private Button nextPhaseButton;

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

        staticTypeButton =
                addRenderableWidget(
                        Button.builder(
                                        Component.translatable(
                                                "gui.creeper_industry.creative_signal_source.static"),
                                        button ->
                                                sendMenuButton(
                                                        CreativeSignalSourceMenu.setStaticButtonId()))
                                .bounds(controlsLeft + 2 * (TYPE_BUTTON_WIDTH + TYPE_BUTTON_GAP),
                                        typeTop, TYPE_BUTTON_WIDTH, 20)
                                .build());
        amplitudeSlider =
                addRenderableWidget(
                        new SignalValueSlider(controlsLeft, typeTop + 30, CONTROL_WIDTH, 0));
        stagesSlider =
                addRenderableWidget(
                        new SignalValueSlider(controlsLeft, typeTop + 58, CONTROL_WIDTH, 1));
        stageLengthSlider =
                addRenderableWidget(
                        new SignalValueSlider(controlsLeft, typeTop + 86, CONTROL_WIDTH, 2));
        previousPhaseButton =
                addRenderableWidget(
                        Button.builder(Component.literal("−"), b -> sendMenuButton(3))
                                .bounds(controlsLeft, typeTop + 113, 24, 20)
                                .build());
        nextPhaseButton =
                addRenderableWidget(
                        Button.builder(Component.literal("+"), b -> sendMenuButton(4))
                                .bounds(controlsLeft + 168, typeTop + 113, 24, 20)
                                .build());
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
        if (type == CreativeSignalSourceSignalType.CONTINUOUS)
            guiGraphics.drawString(
                    font,
                    Component.translatable(
                            "gui.creeper_industry.creative_signal_source.phase",
                            menu.getPhaseSteps(),
                            String.format(java.util.Locale.ROOT, "%.3f", menu.getPeriodTicks())),
                    42,
                    151,
                    0xD5D0C6,
                    false);
        Component hint =
                Component.translatable(
                        "gui.creeper_industry.creative_signal_source."
                                + (type == CreativeSignalSourceSignalType.PULSE ? "pulse_hint"
                                        : type == CreativeSignalSourceSignalType.STATIC ? "static_hint"
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
        staticTypeButton.active = type != CreativeSignalSourceSignalType.STATIC;
        emitPulseButton.visible = type == CreativeSignalSourceSignalType.PULSE;
        emitPulseButton.active = emitPulseButton.visible && menu.getAmplitude() != 0;
        stagesSlider.visible = type == CreativeSignalSourceSignalType.CONTINUOUS;
        stageLengthSlider.visible = stagesSlider.visible;
        previousPhaseButton.visible = stagesSlider.visible;
        nextPhaseButton.visible = stagesSlider.visible;
        stagesSlider.active = stagesSlider.visible;
        stageLengthSlider.active = stagesSlider.visible;
        previousPhaseButton.active = stagesSlider.visible;
        nextPhaseButton.active = stagesSlider.visible;

        amplitudeSlider.syncTo(menu.getAmplitude());
        stagesSlider.syncTo(menu.getStages());
        stageLengthSlider.syncTo(menu.getStageLengthIndex());
    }

    private void sendMenuButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private final class SignalValueSlider extends AbstractSliderButton {
        private final int kind;
        private boolean syncing;

        private SignalValueSlider(int x, int y, int width, int kind) {
            super(x, y, width, 20, Component.empty(), sliderValue(kind,
                    kind == 0 ? menu.getAmplitude()
                            : kind == 1 ? menu.getStages() : menu.getStageLengthIndex()));
            this.kind = kind;
            updateMessage();
        }

        private void syncTo(int currentValue) {
            syncing = true;
            value = sliderValue(kind, currentValue);
            updateMessage();
            syncing = false;
        }

        @Override
        protected void updateMessage() {
            int currentValue = valueFromSlider();
            String key = switch (kind) {
                case 0 -> "gui.creeper_industry.creative_signal_source.amplitude";
                case 1 -> "gui.creeper_industry.creative_signal_source.stages";
                default -> "gui.creeper_industry.creative_signal_source.stage_length";
            };
            String label = kind == 2 && currentValue < 4
                    ? "1/" + (16 >> currentValue)
                    : Integer.toString(kind == 2 ? currentValue - 3 : currentValue);
            setMessage(Component.translatable(key, label));
        }

        @Override
        protected void applyValue() {
            if (syncing) return;
            int currentValue = valueFromSlider();
            sendMenuButton(switch (kind) {
                case 0 -> CreativeSignalSourceMenu.amplitudeButtonId(currentValue);
                case 1 -> CreativeSignalSourceMenu.stagesButtonId(currentValue);
                default -> CreativeSignalSourceMenu.stageLengthButtonId(currentValue);
            });
        }

        private int valueFromSlider() {
            return switch (kind) {
                case 0 -> (int) Math.round(value * 128) - 64;
                case 1 -> 2 + (int) Math.round(value * 7) * 2;
                default -> (int) Math.round(value * (SignalTime.STAGE_LENGTH_COUNT - 1));
            };
        }
    }

    private static double sliderValue(int kind, int currentValue) {
        return switch (kind) {
            case 0 -> (currentValue + 64) / 128.0;
            case 1 -> (currentValue - 2) / 14.0;
            default -> currentValue / (double) (SignalTime.STAGE_LENGTH_COUNT - 1);
        };
    }
}
