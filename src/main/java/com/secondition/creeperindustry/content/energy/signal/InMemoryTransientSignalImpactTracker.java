package com.secondition.creeperindustry.content.energy.signal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
public class InMemoryTransientSignalImpactTracker implements TransientSignalImpactTracker {
    private final java.util.Map<Long, Set<BlockPos>> trackedImpacts = new ConcurrentHashMap<>();

    @Override
    public void record(long gameTime, Collection<BlockPos> affectedTargets) {
        if (affectedTargets.isEmpty()) {
            return;
        }

        Set<BlockPos> bucket = trackedImpacts.computeIfAbsent(gameTime, ignored -> ConcurrentHashMap.newKeySet());
        for (BlockPos pos : affectedTargets) {
            bucket.add(pos.immutable());
        }
    }

    @Override
    public Collection<BlockPos> popAffectedTargetsThroughTick(long gameTime) {
        List<Long> staleKeys = new ArrayList<>();
        for (long key : trackedImpacts.keySet()) {
            if (key <= gameTime) {
                staleKeys.add(key);
            }
        }

        Set<BlockPos> affectedTargets = ConcurrentHashMap.newKeySet();
        for (long staleKey : staleKeys) {
            Set<BlockPos> removed = trackedImpacts.remove(staleKey);
            if (removed != null) {
                affectedTargets.addAll(removed);
            }
        }

        return List.copyOf(affectedTargets);
    }

    @Override
    public void clear() {
        trackedImpacts.clear();
    }
}
