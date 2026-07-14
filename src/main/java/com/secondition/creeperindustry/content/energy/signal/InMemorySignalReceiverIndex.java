package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
public class InMemorySignalReceiverIndex implements SignalReceiverIndex {
    private final Set<BlockPos> indexedReceivers = ConcurrentHashMap.newKeySet();

    @Override
    public void register(BlockPos pos) {
        indexedReceivers.add(pos.immutable());
    }

    @Override
    public void unregister(BlockPos pos) {
        indexedReceivers.remove(pos);
    }

    @Override
    public Collection<BlockPos> getAll() {
        return java.util.List.copyOf(indexedReceivers);
    }

    @Override
    public Collection<BlockPos> getWithinManhattanDistance(BlockPos center, int maxDistance) {
        if (maxDistance < 0) {
            return java.util.List.of();
        }

        return indexedReceivers.stream()
                .filter(pos -> computeManhattanDistance(center, pos) <= maxDistance)
                .map(BlockPos::immutable)
                .toList();
    }

    @Override
    public void clear() {
        indexedReceivers.clear();
    }

    private int computeManhattanDistance(BlockPos left, BlockPos right) {
        return Math.abs(left.getX() - right.getX())
                + Math.abs(left.getY() - right.getY())
                + Math.abs(left.getZ() - right.getZ());
    }
}
