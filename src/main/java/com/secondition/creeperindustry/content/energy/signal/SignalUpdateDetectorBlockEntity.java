package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CIBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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

    public int getLastAmplitude() {
        return lastAmplitude;
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
}
