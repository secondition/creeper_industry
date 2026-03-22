package com.secondition.creeperindustry.content.energy.signal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class InMemoryTransientSignalImpactTracker implements TransientSignalImpactTracker {
    private final Map<ImpactKey, Set<BlockPos>> trackedImpacts = new ConcurrentHashMap<>();

    @Override
    public void record(ResourceKey<Level> level, long gameTime, Collection<BlockPos> affectedTargets) {
        if (affectedTargets.isEmpty()) {
            return;
        }

        ImpactKey key = new ImpactKey(level, gameTime);
        Set<BlockPos> bucket = trackedImpacts.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet());
        for (BlockPos pos : affectedTargets) {
            bucket.add(pos.immutable());
        }
    }

    @Override
    public Collection<BlockPos> popAffectedTargetsThroughTick(ResourceKey<Level> level, long gameTime) {
        List<ImpactKey> staleKeys = new ArrayList<>();
        for (ImpactKey key : trackedImpacts.keySet()) {
            if (key.level().equals(level) && key.gameTime() <= gameTime) {
                staleKeys.add(key);
            }
        }

        Set<BlockPos> affectedTargets = ConcurrentHashMap.newKeySet();
        for (ImpactKey staleKey : staleKeys) {
            Set<BlockPos> removed = trackedImpacts.remove(staleKey);
            if (removed != null) {
                affectedTargets.addAll(removed);
            }
        }

        return List.copyOf(affectedTargets);
    }

    @Override
    public void clearLevel(ResourceKey<Level> level) {
        trackedImpacts.keySet().removeIf(key -> key.level().equals(level));
    }

    private record ImpactKey(ResourceKey<Level> level, long gameTime) {
    }
}
