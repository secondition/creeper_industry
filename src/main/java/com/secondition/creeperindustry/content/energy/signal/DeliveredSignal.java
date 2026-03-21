package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.core.BlockPos;

public record DeliveredSignal(
        SignalSource source,
        BlockPos targetPos,
        int effectiveAmplitude,
        int propagationCost
) {
    public DeliveredSignal {
        if (effectiveAmplitude <= 0) {
            throw new IllegalArgumentException("Delivered signal amplitude must be positive");
        }
        if (propagationCost < 0) {
            throw new IllegalArgumentException("Propagation cost cannot be negative");
        }
    }
}
