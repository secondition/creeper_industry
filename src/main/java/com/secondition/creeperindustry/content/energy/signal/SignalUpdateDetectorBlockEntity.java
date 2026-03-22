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
    private int lastAmplitude;

    public SignalUpdateDetectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.SIGNAL_UPDATE_DETECTOR.get(), pos, blockState);
    }

    @Override
    public void receiveSignal(AggregatedSignal signal) {
        lastAmplitude = signal.signal().amplitude();

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
        lastAmplitude = 0;

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

    public int getLastAmplitude() {
        return lastAmplitude;
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
        tag.putInt("last_amplitude", lastAmplitude);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastAmplitude = tag.getInt("last_amplitude");
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
            CISignalSourceTypes.continuousSignalPropagationService().refreshTarget(currentLevel, worldPosition);
        }
    }
}
