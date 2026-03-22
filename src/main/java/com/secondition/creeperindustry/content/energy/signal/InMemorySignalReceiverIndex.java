package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class InMemorySignalReceiverIndex implements SignalReceiverIndex {
    private final Map<ResourceKey<Level>, Set<BlockPos>> indexedReceivers = new ConcurrentHashMap<>();

    @Override
    public void register(ResourceKey<Level> level, BlockPos pos) {
        indexedReceivers
                .computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet())
                .add(pos.immutable());
    }

    @Override
    public void unregister(ResourceKey<Level> level, BlockPos pos) {
        Set<BlockPos> positions = indexedReceivers.get(level);
        if (positions == null) {
            return;
        }

        positions.remove(pos);
        if (positions.isEmpty()) {
            indexedReceivers.remove(level, positions);
        }
    }

    @Override
    public Collection<BlockPos> getAll(ResourceKey<Level> level) {
        Set<BlockPos> positions = indexedReceivers.get(level);
        if (positions == null) {
            return List.of();
        }
        return List.copyOf(positions);
    }

    @Override
    public Collection<BlockPos> getWithinManhattanDistance(ResourceKey<Level> level, BlockPos center, int maxDistance) {
        if (maxDistance < 0) {
            return List.of();
        }

        Set<BlockPos> positions = indexedReceivers.get(level);
        if (positions == null) {
            return List.of();
        }

        return positions.stream()
                .filter(pos -> computeManhattanDistance(center, pos) <= maxDistance)
                .map(BlockPos::immutable)
                .toList();
    }

    @Override
    public void clearLevel(ResourceKey<Level> level) {
        indexedReceivers.remove(level);
    }

    private int computeManhattanDistance(BlockPos left, BlockPos right) {
        return Math.abs(left.getX() - right.getX())
                + Math.abs(left.getY() - right.getY())
                + Math.abs(left.getZ() - right.getZ());
    }
}
