package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignalUpdateDetectorBlockEntity extends BlockEntity implements SignalReceiver {
    private double currentAmplitude;
    private double currentInstantaneousValue;
    private double lastNonZeroAmplitude;
    private double nextArrival = Double.POSITIVE_INFINITY;

    public SignalUpdateDetectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.SIGNAL_UPDATE_DETECTOR.get(), pos, blockState);
    }

    @Override
    public void receiveSignal(AggregatedSignal signal) {
        double last = lastNonZeroAmplitude;
        for (var step : signal.steps())
            if (step.after() != 0) lastNonZeroAmplitude = Math.abs(step.after());
        if (signal.amplitude() <= 0) {
            clearSignal();
            if (last != lastNonZeroAmplitude) setChanged();
            return;
        }
        boolean changed = last != lastNonZeroAmplitude
                || currentAmplitude != signal.amplitude()
                || currentInstantaneousValue != signal.instantaneousValue();
        currentAmplitude = signal.amplitude();
        currentInstantaneousValue = signal.instantaneousValue();
        if (currentAmplitude > 0) {
            lastNonZeroAmplitude = currentAmplitude;
        }

        if (level != null) {
            BlockState state = getBlockState();
            if (!state.getValue(SignalUpdateDetectorBlock.TRIGGERED)) {
                level.setBlock(
                        worldPosition,
                        state.setValue(SignalUpdateDetectorBlock.TRIGGERED, true),
                        3);
            }
        }

        if (changed) {
            setChanged();
        }
    }

    @Override
    public void clearSignal() {
        boolean changed = currentAmplitude != 0 || currentInstantaneousValue != 0;
        currentAmplitude = 0;
        currentInstantaneousValue = 0;

        if (level != null) {
            BlockState state = getBlockState();
            if (state.getValue(SignalUpdateDetectorBlock.TRIGGERED)) {
                level.setBlock(
                        worldPosition,
                        state.setValue(SignalUpdateDetectorBlock.TRIGGERED, false),
                        3);
            }
        }

        if (changed) {
            setChanged();
        }
    }

    public double getCurrentAmplitude() {
        return currentAmplitude;
    }

    public double getLastNonZeroAmplitude() {
        return lastNonZeroAmplitude;
    }

    public double getCurrentInstantaneousValue() {
        return currentInstantaneousValue;
    }

    @Override
    public void scheduleSignalChange(double arrivalTick) {
        nextArrival = arrivalTick;
    }

    public double pendingDelay() {
        return !Double.isFinite(nextArrival) || level == null
                ? -1 : Math.max(0, nextArrival - level.getGameTime());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerReceiver();
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
        tag.putDouble("current_amplitude", currentAmplitude);
        tag.putDouble("current_instantaneous_value", currentInstantaneousValue);
        tag.putDouble("last_non_zero_amplitude", lastNonZeroAmplitude);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        currentAmplitude = tag.getDouble("current_amplitude");
        currentInstantaneousValue = tag.getDouble("current_instantaneous_value");
        lastNonZeroAmplitude = tag.getDouble("last_non_zero_amplitude");
    }

    private void registerReceiver() {
        if (level == null || level.isClientSide()) {
            return;
        }
        SignalRuntimeAccess.get(level).registerReceiver(worldPosition);
    }

    private void unregisterReceiver() {
        if (level == null || level.isClientSide()) {
            return;
        }
        SignalRuntimeAccess.getExisting(level)
                .ifPresent(runtime -> runtime.unregisterReceiver(worldPosition));
    }
}
