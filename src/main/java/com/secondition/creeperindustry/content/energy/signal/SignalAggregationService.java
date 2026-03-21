package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface SignalAggregationService {
    void submitContribution(Level level, DeliveredSignal contribution);

    void clearThroughTick(ResourceKey<Level> level, long gameTime);
}
