package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.core.BlockPos;

public record SignalReach(
        BlockPos targetPos,
        int propagationCost,
        int effectiveAmplitude
) {
    public SignalReach {
        if (propagationCost < 0) {
            throw new IllegalArgumentException("Propagation cost cannot be negative");
        }
        if (effectiveAmplitude <= 0) {
            throw new IllegalArgumentException("Effective amplitude must be positive");
        }
    }
}
