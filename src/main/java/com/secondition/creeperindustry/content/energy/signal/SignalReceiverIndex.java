package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface SignalReceiverIndex {
    void register(ResourceKey<Level> level, BlockPos pos);

    void unregister(ResourceKey<Level> level, BlockPos pos);

    Collection<BlockPos> getAll(ResourceKey<Level> level);

    Collection<BlockPos> getWithinManhattanDistance(ResourceKey<Level> level, BlockPos center, int maxDistance);

    void clearLevel(ResourceKey<Level> level);
}
