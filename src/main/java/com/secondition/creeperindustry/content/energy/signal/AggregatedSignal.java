package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record AggregatedSignal(
        ResourceKey<Level> level,
        BlockPos targetPos,
        long gameTime,
        SignalDefinition signal,
        int contributionCount,
        int strongestPropagationCost
) {
    public AggregatedSignal {
        if (contributionCount <= 0) {
            throw new IllegalArgumentException("Contribution count must be positive");
        }
        if (strongestPropagationCost < 0) {
            throw new IllegalArgumentException("Propagation cost cannot be negative");
        }
    }
}
