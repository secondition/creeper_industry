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
        checkContainerDataCount(data, 5);
        this.source = source;
        this.data = data;
        addDataSlots(data);
    }

    public boolean clickMenuButton(Player player, int id) {
        if (source == null || !stillValid(player) || player.level().isClientSide()) return false;
        if (id == 0) source.setSignalType(CreativeSignalSourceSignalType.PULSE);
        else if (id == 1) source.setSignalType(CreativeSignalSourceSignalType.CONTINUOUS);
        else if (id == 2) source.emitPulse();
        else if (id == 5) source.setSignalType(CreativeSignalSourceSignalType.STATIC);
        else if (id == 3 || id == 4) source.stepPhase(id == 3 ? -1 : 1);
        else if (id >= 100 && id <= 228) source.setAmplitude(id - 164);
        else if (id >= 300 && id < 308) source.setStages(2 + (id - 300) * 2);
        else if (id >= 400 && id < 400 + SignalTime.STAGE_LENGTH_COUNT)
            source.setStageLengthIndex(id - 400);
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

    public int getStages() {
        return data.get(2);
    }

    public int getStageLengthIndex() {
        return data.get(3);
    }

    public int getPhaseSteps() {
        return data.get(4);
    }

    public double getPeriodTicks() {
        return getStages() * SignalTime.stageUnits(Math.clamp(getStageLengthIndex(), 0,
                SignalTime.STAGE_LENGTH_COUNT - 1)) / (double) SignalTime.UNITS_PER_TICK;
    }

    public static int amplitudeButtonId(int amplitude) {
        return 164 + amplitude;
    }

    public static int stagesButtonId(int stages) {
        return 300 + (stages - 2) / 2;
    }

    public static int stageLengthButtonId(int index) {
        return 400 + index;
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

    public static int setStaticButtonId() {
        return 5;
    }

    private static Context read(Inventory inventory, RegistryFriendlyByteBuf buf) {
        SimpleContainerData data = new SimpleContainerData(5);
        BlockPos pos = buf.readBlockPos();
        for (int i = 0; i < 5; i++) data.set(i, buf.readVarInt());
        return new Context(
                inventory.player.level().getBlockEntity(pos)
                                instanceof CreativeSignalSourceBlockEntity be
                        ? be
                        : null,
                data);
    }

    private record Context(CreativeSignalSourceBlockEntity source, ContainerData data) {}
}
