package com.secondition.creeperindustry.client.automation.breaker;

import com.secondition.creeperindustry.content.automation.breaker.LinearRangeBreakerStrengthRule;
import com.secondition.creeperindustry.content.automation.breaker.SignalRange;
import com.secondition.creeperindustry.content.automation.breaker.SignalRangeBreakerMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SignalRangeBreakerScreen extends AbstractContainerScreen<SignalRangeBreakerMenu> {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 194;
    private static final int CONTROL_LEFT_OFFSET = 14;
    private static final int CONTROL_WIDTH = 192;
    private static final int BUTTON_HEIGHT = 20;

    private final LinearRangeBreakerStrengthRule strengthRule = new LinearRangeBreakerStrengthRule();
    private Button widthButton;
    private Button heightButton;
    private Button depthButton;
    private Button amplitudeModeButton;
    private Button resistanceModeButton;
    private EditBox calculatorInput;
    private CalculatorMode calculatorMode = CalculatorMode.AMPLITUDE;
    private String amplitudeInput = "13";
    private String resistanceInput = "6";

    public SignalRangeBreakerScreen(SignalRangeBreakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = PANEL_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        int left = leftPos + CONTROL_LEFT_OFFSET;
        int top = topPos + 32;
        int buttonWidth = (CONTROL_WIDTH - 8) / 3;

        widthButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendMenuButton(
                SignalRangeBreakerMenu.toggleWidthButtonId()
        )).bounds(left, top, buttonWidth, BUTTON_HEIGHT).build());
        heightButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendMenuButton(
                SignalRangeBreakerMenu.toggleHeightButtonId()
        )).bounds(left + buttonWidth + 4, top, buttonWidth, BUTTON_HEIGHT).build());
        depthButton = addRenderableWidget(Button.builder(Component.empty(), button -> sendMenuButton(
                SignalRangeBreakerMenu.toggleDepthButtonId()
        )).bounds(left + (buttonWidth + 4) * 2, top, buttonWidth, BUTTON_HEIGHT).build());

        int calculatorTop = topPos + 105;
        int modeButtonWidth = (CONTROL_WIDTH - 4) / 2;
        amplitudeModeButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.creeper_industry.signal_range_breaker.mode_amplitude"),
                button -> setCalculatorMode(CalculatorMode.AMPLITUDE)
        ).bounds(left, calculatorTop, modeButtonWidth, BUTTON_HEIGHT).build());
        resistanceModeButton = addRenderableWidget(Button.builder(
                Component.translatable("gui.creeper_industry.signal_range_breaker.mode_resistance"),
                button -> setCalculatorMode(CalculatorMode.RESISTANCE)
        ).bounds(left + modeButtonWidth + 4, calculatorTop, modeButtonWidth, BUTTON_HEIGHT).build());

        calculatorInput = addRenderableWidget(new EditBox(
                font,
                left,
                calculatorTop + 26,
                CONTROL_WIDTH,
                BUTTON_HEIGHT,
                Component.translatable("gui.creeper_industry.signal_range_breaker.calculator_input")
        ));
        calculatorInput.setMaxLength(12);
        configureCalculatorInput();
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
        int right = leftPos + imageWidth;
        int bottom = topPos + imageHeight;
        guiGraphics.fill(leftPos, topPos, right, bottom, 0xFF10161B);
        guiGraphics.fill(leftPos + 1, topPos + 1, right - 1, bottom - 1, 0xFF29353D);
        guiGraphics.fill(leftPos + 4, topPos + 4, right - 4, topPos + 22, 0xFF6A3C25);
        guiGraphics.fill(leftPos + 4, topPos + 25, right - 4, bottom - 4, 0xFF182127);
        guiGraphics.fill(leftPos + 14, topPos + 66, right - 14, topPos + 86, 0xFF10171C);
        guiGraphics.fill(leftPos + 14, topPos + 90, right - 14, topPos + 178, 0xFF10171C);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        SignalRange range = menu.range();
        guiGraphics.drawString(font, title, 8, 7, 0xF4F1E8, false);
        guiGraphics.drawString(
                font,
                Component.translatable("gui.creeper_industry.signal_range_breaker.range", range.width(), range.height(), range.depth()),
                CONTROL_LEFT_OFFSET,
                58,
                0xE8C49C,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("gui.creeper_industry.signal_range_breaker.volume", range.volume()),
                CONTROL_LEFT_OFFSET,
                76,
                0xC8D2D7,
                false
        );
        guiGraphics.drawString(
                font,
                Component.translatable("gui.creeper_industry.signal_range_breaker.calculator"),
                CONTROL_LEFT_OFFSET,
                94,
                0xE8C49C,
                false
        );
        guiGraphics.drawString(
                font,
                calculatorResult(range),
                CONTROL_LEFT_OFFSET,
                158,
                0xAFC8A5,
                false
        );
    }

    private void syncWidgets() {
        if (widthButton == null) {
            return;
        }
        SignalRange range = menu.range();
        widthButton.setMessage(Component.translatable("gui.creeper_industry.signal_range_breaker.width", range.width()));
        heightButton.setMessage(Component.translatable("gui.creeper_industry.signal_range_breaker.height", range.height()));
        depthButton.setMessage(Component.translatable("gui.creeper_industry.signal_range_breaker.depth", range.depth()));
        amplitudeModeButton.active = calculatorMode != CalculatorMode.AMPLITUDE;
        resistanceModeButton.active = calculatorMode != CalculatorMode.RESISTANCE;
    }

    private void setCalculatorMode(CalculatorMode mode) {
        if (calculatorMode == mode) {
            return;
        }
        calculatorMode = mode;
        configureCalculatorInput();
        syncWidgets();
    }

    private void configureCalculatorInput() {
        if (calculatorInput == null) {
            return;
        }
        calculatorInput.setFilter(calculatorMode == CalculatorMode.AMPLITUDE
                ? value -> value.matches("\\d*")
                : value -> value.matches("\\d*(\\.\\d*)?"));
        calculatorInput.setHint(Component.translatable(calculatorMode == CalculatorMode.AMPLITUDE
                ? "gui.creeper_industry.signal_range_breaker.input_amplitude"
                : "gui.creeper_industry.signal_range_breaker.input_resistance"));
        calculatorInput.setResponder(value -> {
            if (calculatorMode == CalculatorMode.AMPLITUDE) {
                amplitudeInput = value;
            } else {
                resistanceInput = value;
            }
        });
        calculatorInput.setValue(calculatorMode == CalculatorMode.AMPLITUDE ? amplitudeInput : resistanceInput);
    }

    private Component calculatorResult(SignalRange range) {
        try {
            if (calculatorMode == CalculatorMode.AMPLITUDE) {
                int amplitude = Integer.parseInt(amplitudeInput);
                double resistance = strengthRule.maximumBreakableResistance(amplitude, range.volume());
                return Component.translatable(
                        "gui.creeper_industry.signal_range_breaker.result_resistance",
                        String.format(java.util.Locale.ROOT, "%.1f", resistance)
                );
            }

            double resistance = Double.parseDouble(resistanceInput);
            long amplitude = strengthRule.minimumRequiredAmplitude(resistance, range.volume());
            if (amplitude < 0L) {
                return invalidCalculatorValue();
            }
            if (amplitude > Integer.MAX_VALUE) {
                return Component.translatable("gui.creeper_industry.signal_range_breaker.result_too_large");
            }
            return Component.translatable("gui.creeper_industry.signal_range_breaker.result_amplitude", amplitude);
        } catch (NumberFormatException ignored) {
            return invalidCalculatorValue();
        }
    }

    private Component invalidCalculatorValue() {
        return Component.translatable("gui.creeper_industry.signal_range_breaker.result_invalid");
    }

    private void sendMenuButton(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    private enum CalculatorMode {
        AMPLITUDE,
        RESISTANCE
    }
}
