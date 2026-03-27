package com.secondition.creeperindustry.content.energy.signal;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface SignalAggregationService {
    void submitContribution(Level level, DeliveredSignal contribution);

    List<DeliveredSignal> getSubmittedContributions(ResourceKey<Level> level, BlockPos targetPos, long gameTime);

    void clearThroughTick(ResourceKey<Level> level, long gameTime);
}
