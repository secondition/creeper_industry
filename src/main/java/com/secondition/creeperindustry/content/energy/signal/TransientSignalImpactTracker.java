package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.core.BlockPos;
public interface TransientSignalImpactTracker {
    void record(long gameTime, Collection<BlockPos> affectedTargets);

    Collection<BlockPos> popAffectedTargetsThroughTick(long gameTime);

    void clear();
}
