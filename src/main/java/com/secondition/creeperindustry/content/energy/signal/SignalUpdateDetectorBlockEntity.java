package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.CISignalSourceTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignalUpdateDetectorBlockEntity extends BlockEntity implements SignalReceiver {
    private int currentAmplitude;
    private int currentInstantaneousValue;
    private int lastNonZeroAmplitude;

    public SignalUpdateDetectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.SIGNAL_UPDATE_DETECTOR.get(), pos, blockState);
    }

    @Override
    public void receiveSignal(AggregatedSignal signal) {
        currentAmplitude = signal.signal().amplitude();
        currentInstantaneousValue = signal.instantaneousValue();
        if (currentAmplitude > 0) {
            lastNonZeroAmplitude = currentAmplitude;
        }

        if (level != null) {
            BlockState state = getBlockState();
            if (!state.getValue(SignalUpdateDetectorBlock.TRIGGERED)) {
                level.setBlock(worldPosition, state.setValue(SignalUpdateDetectorBlock.TRIGGERED, true), 3);
            } else {
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }

        setChanged();
    }

    @Override
    public void clearSignal() {
        currentAmplitude = 0;
        currentInstantaneousValue = 0;

        if (level != null) {
            BlockState state = getBlockState();
            if (state.getValue(SignalUpdateDetectorBlock.TRIGGERED)) {
                level.setBlock(worldPosition, state.setValue(SignalUpdateDetectorBlock.TRIGGERED, false), 3);
            } else {
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }

        setChanged();
    }

    public int getCurrentAmplitude() {
        return currentAmplitude;
    }

    public int getLastNonZeroAmplitude() {
        return lastNonZeroAmplitude;
    }

    public int getCurrentInstantaneousValue() {
        return currentInstantaneousValue;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerReceiver();
        refreshOnFirstLoad();
    }

    @Override
    public void setRemoved() {
        unregisterReceiver();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unregisterReceiver();
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("current_amplitude", currentAmplitude);
        tag.putInt("current_instantaneous_value", currentInstantaneousValue);
        tag.putInt("last_non_zero_amplitude", lastNonZeroAmplitude);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        currentAmplitude = tag.getInt("current_amplitude");
        currentInstantaneousValue = tag.getInt("current_instantaneous_value");
        lastNonZeroAmplitude = tag.getInt("last_non_zero_amplitude");
    }

    private void registerReceiver() {
        if (level == null || level.isClientSide()) {
            return;
        }
        CISignalSourceTypes.signalReceiverIndex().register(level.dimension(), worldPosition);
    }

    private void unregisterReceiver() {
        if (level == null || level.isClientSide()) {
            return;
        }
        CISignalSourceTypes.signalReceiverIndex().unregister(level.dimension(), worldPosition);
    }

    private void refreshOnFirstLoad() {
        Level currentLevel = level;
        if (currentLevel == null || currentLevel.isClientSide()) {
            return;
        }

        if (!CISignalSourceTypes.continuousSignalSourceRepository().getActiveSources(currentLevel.dimension()).isEmpty()) {
            CISignalSourceTypes.unifiedSignalRefreshService().refreshTarget(currentLevel, worldPosition);
        }
    }
}
