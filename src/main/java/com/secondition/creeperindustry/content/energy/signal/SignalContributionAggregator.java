package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface SignalContributionAggregator {
    @Nullable
    AggregatedSignal aggregate(ResourceKey<Level> level, BlockPos targetPos, long gameTime, Collection<DeliveredSignal> contributions);
}
