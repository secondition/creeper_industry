package com.secondition.creeperindustry.content.energy.signal;

import java.util.UUID;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.CISignalSourceTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ContinuousSignalEmitterBlockEntity extends BlockEntity {
    private static final int[] AMPLITUDE_OPTIONS = {4, 8, 12, 16};
    private static final int[] PERIOD_OPTIONS = {1, 2, 3, 4, 5};

    private UUID sourceId = UUID.randomUUID();
    private int amplitude = AMPLITUDE_OPTIONS[0];
    private int periodTicks = PERIOD_OPTIONS[0];
    private boolean sourceRegistered;

    public ContinuousSignalEmitterBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.CONTINUOUS_SIGNAL_EMITTER.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        pushSignalUpdate();
    }

    @Override
    public void setRemoved() {
        removeSignalSource(true);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        removeSignalSource(false);
        super.onChunkUnloaded();
    }

    public int getAmplitude() {
        return amplitude;
    }

    public int getPeriodTicks() {
        return periodTicks;
    }

    public void cycleAmplitude() {
        amplitude = nextOption(AMPLITUDE_OPTIONS, amplitude);
        pushSignalUpdate();
        setChanged();
    }

    public void cyclePeriod() {
        periodTicks = nextOption(PERIOD_OPTIONS, periodTicks);
        pushSignalUpdate();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("source_id", sourceId);
        tag.putInt("amplitude", amplitude);
        tag.putInt("period_ticks", periodTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("source_id")) {
            sourceId = tag.getUUID("source_id");
        }
        amplitude = readOption(tag, "amplitude", AMPLITUDE_OPTIONS, AMPLITUDE_OPTIONS[0]);
        periodTicks = readOption(tag, "period_ticks", PERIOD_OPTIONS, PERIOD_OPTIONS[0]);
    }

    private void pushSignalUpdate() {
        Level currentLevel = level;
        if (currentLevel == null || currentLevel.isClientSide()) {
            return;
        }

        MachineSignalSource source = new MachineSignalSource(
                sourceId,
                currentLevel.dimension(),
                Vec3.atCenterOf(worldPosition),
                currentLevel.getGameTime(),
                new SignalDefinition(amplitude, periodTicks, 0, SignalWaveform.SQUARE)
        );
        CISignalSourceTypes.continuousSignalUpdateService().upsert(currentLevel, source);
        sourceRegistered = true;
    }

    private void removeSignalSource(boolean refreshAffectedReceivers) {
        Level currentLevel = level;
        if (currentLevel == null || currentLevel.isClientSide() || !sourceRegistered) {
            return;
        }

        if (refreshAffectedReceivers) {
            CISignalSourceTypes.continuousSignalUpdateService().remove(currentLevel, sourceId);
        } else {
            CISignalSourceTypes.continuousSignalUpdateService().removeWithoutRefresh(currentLevel, sourceId);
        }
        sourceRegistered = false;
    }

    private static int nextOption(int[] options, int currentValue) {
        for (int index = 0; index < options.length; index++) {
            if (options[index] == currentValue) {
                return options[(index + 1) % options.length];
            }
        }
        return options[0];
    }

    private static int readOption(CompoundTag tag, String key, int[] options, int fallback) {
        int value = tag.getInt(key);
        for (int option : options) {
            if (option == value) {
                return option;
            }
        }
        return fallback;
    }
}
