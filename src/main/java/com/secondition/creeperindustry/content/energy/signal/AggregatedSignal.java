package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.List;

public record AggregatedSignal(
        ResourceKey<Level> level,
        BlockPos targetPos,
        long gameTime,
        double amplitude,
        double instantaneousValue,
        int contributionCount,
        List<Step> steps) {
    public record Step(double before, double after, boolean continuous) {
        public boolean risesPast(int threshold) {
            return Math.abs(after) >= threshold
                    && (Math.abs(before) < threshold
                            || continuous && Math.signum(before) != Math.signum(after));
        }
    }

    public int processingUnits(int threshold) {
        return (int) steps.stream().filter(step -> step.risesPast(threshold)).count();
    }
}
