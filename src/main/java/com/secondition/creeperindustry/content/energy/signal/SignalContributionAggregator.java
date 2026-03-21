package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface SignalContributionAggregator {
    AggregatedSignal aggregate(ResourceKey<Level> level, BlockPos targetPos, long gameTime, Collection<DeliveredSignal> contributions);
}
