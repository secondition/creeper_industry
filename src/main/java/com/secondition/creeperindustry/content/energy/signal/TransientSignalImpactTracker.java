package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface TransientSignalImpactTracker {
    void record(ResourceKey<Level> level, long gameTime, Collection<BlockPos> affectedTargets);

    Collection<BlockPos> popAffectedTargetsThroughTick(ResourceKey<Level> level, long gameTime);

    void clearLevel(ResourceKey<Level> level);
}
