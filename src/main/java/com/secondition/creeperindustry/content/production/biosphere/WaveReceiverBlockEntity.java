package com.secondition.creeperindustry.content.production.biosphere;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.content.energy.signal.*;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WaveReceiverBlockEntity extends BlockEntity implements SignalReceiver {
    private BlockPos controller;

    public WaveReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(CIBlockEntityTypes.WAVE_RECEIVER.get(), pos, state);
    }

    public boolean bind(BlockPos owner) {
        if (controller != null && !controller.equals(owner)) return false;
        controller = owner.immutable();
        return true;
    }

    public void unbind(BlockPos owner) {
        if (owner.equals(controller)) controller = null;
    }

    @Override
    public boolean slowOnly() {
        return true;
    }

    public void receiveSignal(AggregatedSignal signal) {
        if (level != null
                && controller != null
                && level.hasChunkAt(controller)
                && level.getBlockEntity(controller) instanceof BiosphereBlockEntity machine)
            machine.receiveSignal(signal);
    }

    public void clearSignal() {}

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide())
            SignalRuntimeAccess.get(level).registerReceiver(worldPosition);
    }

    private void detach() {
        if (level != null && !level.isClientSide())
            SignalRuntimeAccess.getExisting(level)
                    .ifPresent(
                            r -> {
                                r.unregisterReceiver(worldPosition);
                                r.structures().changed(worldPosition);
                            });
    }

    @Override
    public void setRemoved() {
        detach();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        detach();
        super.onChunkUnloaded();
    }
}
