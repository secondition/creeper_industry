package com.secondition.creeperindustry.content.energy.signal;

import javax.annotation.Nullable;

import com.secondition.creeperindustry.CIMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class CreativeSignalSourceMenu extends AbstractContainerMenu {
    private static final int BUTTON_SET_PULSE = 0;
    private static final int BUTTON_SET_CONTINUOUS = 1;
    private static final int BUTTON_EMIT_PULSE = 2;
    private static final int BUTTON_AMPLITUDE_BASE = 100;
    private static final int BUTTON_PERIOD_BASE = 200;

    private final CreativeSignalSourceBlockEntity source;
    private final ContainerData data;

    public CreativeSignalSourceMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, createClientContext(playerInventory, extraData));
    }

    private CreativeSignalSourceMenu(int containerId, Inventory playerInventory, ClientContext context) {
        this(containerId, playerInventory, context.source(), context.data());
    }

    public CreativeSignalSourceMenu(int containerId, Inventory playerInventory, @Nullable CreativeSignalSourceBlockEntity source, ContainerData data) {
        super(CIMenuTypes.CREATIVE_SIGNAL_SOURCE.get(), containerId);
        checkContainerDataCount(data, 3);
        this.source = source;
        this.data = data;
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (source == null) {
            return false;
        }

        if (id == BUTTON_SET_PULSE) {
            source.setSignalType(CreativeSignalSourceSignalType.PULSE);
            return true;
        }
        if (id == BUTTON_SET_CONTINUOUS) {
            source.setSignalType(CreativeSignalSourceSignalType.CONTINUOUS);
            return true;
        }
        if (id == BUTTON_EMIT_PULSE) {
            source.emitPulse();
            return true;
        }
        if (id >= BUTTON_AMPLITUDE_BASE && id <= BUTTON_AMPLITUDE_BASE + CreativeSignalSourceBlockEntity.MAX_AMPLITUDE) {
            source.setAmplitude(id - BUTTON_AMPLITUDE_BASE);
            return true;
        }
        int periodIndex = id - BUTTON_PERIOD_BASE;
        int maxPeriodIndex = CreativeSignalSourceBlockEntity.MAX_PERIOD_TICKS / CreativeSignalSourceBlockEntity.PERIOD_STEP;
        if (periodIndex >= 0 && periodIndex <= maxPeriodIndex) {
            source.setPeriodTicks(periodIndex * CreativeSignalSourceBlockEntity.PERIOD_STEP);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return source == null || source.getLevel() == null || source.getLevel().getBlockEntity(source.getBlockPos()) == source;
    }

    public CreativeSignalSourceSignalType getSignalType() {
        return CreativeSignalSourceSignalType.bySerializedId(data.get(0));
    }

    public int getAmplitude() {
        return data.get(1);
    }

    public int getPeriodTicks() {
        return data.get(2);
    }

    public static int amplitudeButtonId(int amplitude) {
        return BUTTON_AMPLITUDE_BASE + amplitude;
    }

    public static int periodButtonId(int periodTicks) {
        return BUTTON_PERIOD_BASE + periodTicks / CreativeSignalSourceBlockEntity.PERIOD_STEP;
    }

    public static int setPulseButtonId() {
        return BUTTON_SET_PULSE;
    }

    public static int setContinuousButtonId() {
        return BUTTON_SET_CONTINUOUS;
    }

    public static int emitPulseButtonId() {
        return BUTTON_EMIT_PULSE;
    }

    private static ClientContext createClientContext(Inventory playerInventory, @Nullable RegistryFriendlyByteBuf extraData) {
        SimpleContainerData data = new SimpleContainerData(3);
        if (extraData == null) {
            return new ClientContext(null, data);
        }

        BlockPos pos = extraData.readBlockPos();
        data.set(0, extraData.readVarInt());
        data.set(1, extraData.readVarInt());
        data.set(2, extraData.readVarInt());
        if (playerInventory.player.level().getBlockEntity(pos) instanceof CreativeSignalSourceBlockEntity source) {
            return new ClientContext(source, data);
        }
        return new ClientContext(null, data);
    }

    private record ClientContext(@Nullable CreativeSignalSourceBlockEntity source, ContainerData data) {
    }
}
