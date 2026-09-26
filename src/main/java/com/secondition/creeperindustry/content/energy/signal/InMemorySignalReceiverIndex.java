package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Loaded receiver positions bucketed by chunk. No world block scans. */
public class InMemorySignalReceiverIndex implements SignalReceiverIndex {
    private final Map<Long, Set<BlockPos>> buckets = new HashMap<>();

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    public void register(BlockPos pos) {
        buckets.computeIfAbsent(key(pos.getX() >> 4, pos.getZ() >> 4), k -> new HashSet<>())
                .add(pos.immutable());
    }

    public void unregister(BlockPos pos) {
        long key = key(pos.getX() >> 4, pos.getZ() >> 4);
        Set<BlockPos> set = buckets.get(key);
        if (set != null) {
            set.remove(pos);
            if (set.isEmpty()) buckets.remove(key);
        }
    }

    public Collection<BlockPos> getAll() {
        return buckets.values().stream().flatMap(Collection::stream).toList();
    }

    public Collection<BlockPos> getWithinManhattanDistance(BlockPos center, int radius) {
        return within(Vec3.atCenterOf(center), radius);
    }

    public Collection<BlockPos> within(Vec3 center, double radius) {
        List<BlockPos> result = new ArrayList<>();
        int minX = ((int) Math.floor(center.x - radius)) >> 4,
                maxX = ((int) Math.floor(center.x + radius)) >> 4;
        int minZ = ((int) Math.floor(center.z - radius)) >> 4,
                maxZ = ((int) Math.floor(center.z + radius)) >> 4;
        for (var entry : buckets.entrySet()) {
            int x = (int) (entry.getKey() >> 32), z = (int) (long) entry.getKey();
            if (x < minX || x > maxX || z < minZ || z > maxZ) continue;
            for (BlockPos pos : entry.getValue())
                if (Math.abs(pos.getX() + 0.5D - center.x)
                                + Math.abs(pos.getY() + 0.5D - center.y)
                                + Math.abs(pos.getZ() + 0.5D - center.z)
                        <= radius) result.add(pos);
        }
        return result;
    }

    public void clear() {
        buckets.clear();
    }
}
