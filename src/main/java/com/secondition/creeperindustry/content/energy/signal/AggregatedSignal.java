package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record AggregatedSignal(
        ResourceKey<Level> level,
        BlockPos targetPos,
        long gameTime,
        double amplitude,
        double instantaneousValue,
        int contributionCount,
        int strongestPropagationCost,
        boolean fast,
        double compositePeriodTicks,
        int completedCycles,
        boolean changed) {
    public int processingUnits(double requirement) {
        return amplitude > requirement ? (fast ? completedCycles : changed ? 1 : 0) : 0;
    }
}
