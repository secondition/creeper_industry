package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DeferredSignalRefreshQueue {
    private final Map<ResourceKey<Level>, Set<BlockPos>> queuedTargets = new ConcurrentHashMap<>();

    public void enqueue(ResourceKey<Level> level, Collection<BlockPos> targets) {
        if (targets.isEmpty()) {
            return;
        }

        queuedTargets
                .computeIfAbsent(level, ignored -> ConcurrentHashMap.newKeySet())
                .addAll(targets.stream().map(BlockPos::immutable).toList());
    }

    public Collection<BlockPos> drain(ResourceKey<Level> level) {
        Set<BlockPos> targets = queuedTargets.remove(level);
        if (targets == null || targets.isEmpty()) {
            return java.util.List.of();
        }
        return java.util.List.copyOf(new LinkedHashSet<>(targets));
    }
}
