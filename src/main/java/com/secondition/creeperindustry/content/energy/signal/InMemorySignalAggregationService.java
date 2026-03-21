package com.secondition.creeperindustry.content.energy.signal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class InMemorySignalAggregationService implements SignalAggregationService {
    private final SignalContributionAggregator aggregator;
    private final Map<AggregationKey, List<DeliveredSignal>> contributions = new ConcurrentHashMap<>();

    public InMemorySignalAggregationService(SignalContributionAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @Override
    public void submitContribution(Level level, DeliveredSignal contribution) {
        long gameTime = contribution.source().gameTime();
        AggregationKey key = new AggregationKey(level.dimension(), contribution.targetPos(), gameTime);
        List<DeliveredSignal> bucket = contributions.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>());
        bucket.add(contribution);

        BlockEntity blockEntity = level.getBlockEntity(contribution.targetPos());
        if (!(blockEntity instanceof SignalReceiver receiver)) {
            return;
        }

        AggregatedSignal aggregatedSignal = aggregator.aggregate(level.dimension(), contribution.targetPos(), gameTime, List.copyOf(bucket));
        receiver.receiveSignal(aggregatedSignal);
    }

    @Override
    public void clearThroughTick(ResourceKey<Level> level, long gameTime) {
        List<AggregationKey> staleKeys = new ArrayList<>();
        for (AggregationKey key : contributions.keySet()) {
            if (key.level().equals(level) && key.gameTime() <= gameTime) {
                staleKeys.add(key);
            }
        }
        for (AggregationKey staleKey : staleKeys) {
            contributions.remove(staleKey);
        }
    }

    private record AggregationKey(ResourceKey<Level> level, BlockPos targetPos, long gameTime) {
    }
}
