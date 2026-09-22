package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CIMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public class CreativeSignalSourceMenu extends AbstractContainerMenu {
    private final CreativeSignalSourceBlockEntity source;
    private final ContainerData data;

    public CreativeSignalSourceMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, read(inventory, buf));
    }

    private CreativeSignalSourceMenu(int id, Inventory inventory, Context context) {
        this(id, inventory, context.source(), context.data());
    }

    public CreativeSignalSourceMenu(
            int id,
            Inventory inventory,
            @Nullable CreativeSignalSourceBlockEntity source,
            ContainerData data) {
        super(CIMenuTypes.CREATIVE_SIGNAL_SOURCE.get(), id);
        checkContainerDataCount(data, 4);
        this.source = source;
        this.data = data;
        addDataSlots(data);
    }

    public boolean clickMenuButton(Player player, int id) {
        if (source == null || !stillValid(player) || player.level().isClientSide()) return false;
        if (id == 0) source.setSignalType(CreativeSignalSourceSignalType.PULSE);
        else if (id == 1) source.setSignalType(CreativeSignalSourceSignalType.CONTINUOUS);
        else if (id == 2) source.emitPulse();
        else if (id == 3 || id == 4) source.stepPhase(id == 3 ? -1 : 1);
        else if (id >= 100 && id <= 228) source.setAmplitude(id - 164);
        else if (id >= 300 && id < 360) source.setPeriodIndex(id - 300);
        else return false;
        return true;
    }

    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    public boolean stillValid(Player player) {
        return source != null
                && !source.isRemoved()
                && Container.stillValidBlockEntity(source, player);
    }

    public CreativeSignalSourceSignalType getSignalType() {
        return CreativeSignalSourceSignalType.bySerializedId(data.get(0));
    }

    public int getAmplitude() {
        return data.get(1);
    }

    public int getPeriodIndex() {
        return data.get(2);
    }

    public double getPeriodTicks() {
        return SignalTime.periodAt(Math.clamp(data.get(2), 0, 59));
    }

    public double getPhaseTicks() {
        return data.get(3) / 20.0;
    }

    public static int amplitudeButtonId(int amplitude) {
        return 164 + amplitude;
    }

    public static int periodButtonId(int index) {
        return 300 + index;
    }

    public static int setPulseButtonId() {
        return 0;
    }

    public static int setContinuousButtonId() {
        return 1;
    }

    public static int emitPulseButtonId() {
        return 2;
    }

    private static Context read(Inventory inventory, RegistryFriendlyByteBuf buf) {
        SimpleContainerData data = new SimpleContainerData(4);
        BlockPos pos = buf.readBlockPos();
        for (int i = 0; i < 4; i++) data.set(i, buf.readVarInt());
        return new Context(
                inventory.player.level().getBlockEntity(pos)
                                instanceof CreativeSignalSourceBlockEntity be
                        ? be
                        : null,
                data);
    }

    private record Context(CreativeSignalSourceBlockEntity source, ContainerData data) {}
}
