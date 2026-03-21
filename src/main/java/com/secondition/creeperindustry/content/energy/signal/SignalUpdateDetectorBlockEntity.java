package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CIBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignalUpdateDetectorBlockEntity extends BlockEntity implements SignalReceiver {
    private int lastAmplitude;
    private int lastPropagationCost;
    private long lastGameTime = -1L;
    private ResourceLocation lastSourceTypeId;

    public SignalUpdateDetectorBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.SIGNAL_UPDATE_DETECTOR.get(), pos, blockState);
    }

    @Override
    public void receiveSignal(DeliveredSignal signal) {
        lastAmplitude = signal.effectiveAmplitude();
        lastPropagationCost = signal.propagationCost();
        lastGameTime = signal.source().gameTime();
        lastSourceTypeId = signal.source().type().id();

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

    public int getLastPropagationCost() {
        return lastPropagationCost;
    }

    public long getLastGameTime() {
        return lastGameTime;
    }

    public ResourceLocation getLastSourceTypeId() {
        return lastSourceTypeId;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("last_amplitude", lastAmplitude);
        tag.putInt("last_propagation_cost", lastPropagationCost);
        tag.putLong("last_game_time", lastGameTime);
        if (lastSourceTypeId != null) {
            tag.putString("last_source_type_id", lastSourceTypeId.toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lastAmplitude = tag.getInt("last_amplitude");
        lastPropagationCost = tag.getInt("last_propagation_cost");
        lastGameTime = tag.getLong("last_game_time");
        if (tag.contains("last_source_type_id")) {
            lastSourceTypeId = ResourceLocation.tryParse(tag.getString("last_source_type_id"));
        } else {
            lastSourceTypeId = null;
        }
    }
}
