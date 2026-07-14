package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;

public class DeferredSignalRefreshQueue {
    private final Set<BlockPos> queuedTargets = ConcurrentHashMap.newKeySet();

    public void enqueue(Collection<BlockPos> targets) {
        if (targets.isEmpty()) {
            return;
        }

        queuedTargets.addAll(targets.stream().map(BlockPos::immutable).toList());
    }

    public Collection<BlockPos> drain() {
        if (queuedTargets.isEmpty()) {
            return java.util.List.of();
        }
        LinkedHashSet<BlockPos> targets = new LinkedHashSet<>(queuedTargets);
        queuedTargets.removeAll(targets);
        return java.util.List.copyOf(targets);
    }

    public void clear() {
        queuedTargets.clear();
    }
}
