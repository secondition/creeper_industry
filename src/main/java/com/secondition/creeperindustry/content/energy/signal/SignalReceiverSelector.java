package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class SignalReceiverSelector {
    private final SignalReceiverIndex receiverIndex;

    public SignalReceiverSelector(SignalReceiverIndex receiverIndex) {
        this.receiverIndex = receiverIndex;
    }

    public Collection<BlockPos> getPotentialTargets(ResourceKey<Level> level, SignalSource source) {
        int maxDistance = source.signal().amplitude() - 1;
        if (maxDistance < 0) {
            return java.util.List.of();
        }

        return receiverIndex.getWithinManhattanDistance(
                level,
                BlockPos.containing(source.position()),
                maxDistance
        );
    }
}
