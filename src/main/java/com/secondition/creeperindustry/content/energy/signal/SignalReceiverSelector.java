package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class SignalReceiverSelector {
    private final SignalReceiverIndex receiverIndex;

    public SignalReceiverSelector(SignalReceiverIndex receiverIndex) {
        this.receiverIndex = receiverIndex;
    }

    public Collection<BlockPos> getPotentialTargets(Level level, SignalSource source) {
        int maxDistance = source.signal().amplitude() - 1;
        if (maxDistance < 0) {
            return java.util.List.of();
        }

        if (DuctPropagationCostResolver.hasNearbyDuctEntrance(level, source.position(), maxDistance)) {
            return receiverIndex.getAll(level.dimension());
        }

        return receiverIndex.getWithinManhattanDistance(
                level.dimension(),
                BlockPos.containing(source.position()),
                maxDistance
        );
    }
}
