package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.core.BlockPos;
public interface SignalReceiverIndex {
    void register(BlockPos pos);

    void unregister(BlockPos pos);

    Collection<BlockPos> getAll();

    Collection<BlockPos> getWithinManhattanDistance(BlockPos center, int maxDistance);

    void clear();
}
