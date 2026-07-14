package com.secondition.creeperindustry.content.energy.signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class InMemorySignalAggregationService implements SignalAggregationService {
    private final Map<AggregationKey, List<DeliveredSignal>> contributions = new ConcurrentHashMap<>();

    @Override
    public void submitContribution(Level level, DeliveredSignal contribution) {
        long gameTime = contribution.source().gameTime();
        AggregationKey key = new AggregationKey(contribution.targetPos(), gameTime);
        List<DeliveredSignal> bucket = contributions.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>());
        bucket.add(contribution);
    }

    @Override
    public List<DeliveredSignal> getSubmittedContributions(BlockPos targetPos, long gameTime) {
        AggregationKey key = new AggregationKey(targetPos, gameTime);
        List<DeliveredSignal> bucket = contributions.get(key);
        if (bucket == null) {
            return List.of();
        }
        return List.copyOf(bucket);
    }

    @Override
    public void clearThroughTick(long gameTime) {
        List<AggregationKey> staleKeys = new ArrayList<>();
        for (AggregationKey key : contributions.keySet()) {
            if (key.gameTime() <= gameTime) {
                staleKeys.add(key);
            }
        }
        for (AggregationKey staleKey : staleKeys) {
            contributions.remove(staleKey);
        }
    }

    @Override
    public void clear() {
        contributions.clear();
    }

    private record AggregationKey(BlockPos targetPos, long gameTime) {
    }
}
