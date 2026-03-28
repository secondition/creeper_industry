package com.secondition.creeperindustry.content.energy.signal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ContinuousSignalPropagationService {
    private final ContinuousSignalSourceRepository repository;
    private final SignalContributionAggregator aggregator;

    public ContinuousSignalPropagationService(
            ContinuousSignalSourceRepository repository,
            SignalContributionAggregator aggregator
    ) {
        this.repository = repository;
        this.aggregator = aggregator;
    }

    public List<DeliveredSignal> collectContributions(Level level, BlockPos targetPos, long gameTime) {
        List<DeliveredSignal> contributions = new ArrayList<>();
        for (ContinuousSignalSource source : repository.getActiveSources(level.dimension())) {
            SignalReach reach = SignalReachEvaluator.evaluate(level, source, targetPos);
            if (reach != null) {
                contributions.add(new DeliveredSignal(
                        source,
                        reach.targetPos(),
                        reach.effectiveAmplitude(),
                        reach.propagationCost()
                ));
            }
        }
        return List.copyOf(contributions);
    }

    public void refreshTarget(Level level, BlockPos targetPos) {
        BlockEntity blockEntity = level.getBlockEntity(targetPos);
        if (!(blockEntity instanceof SignalReceiver receiver)) {
            return;
        }

        List<DeliveredSignal> contributions = collectContributions(level, targetPos, level.getGameTime());
        if (contributions.isEmpty()) {
            receiver.clearSignal();
            return;
        }

        AggregatedSignal aggregatedSignal = aggregator.aggregate(level.dimension(), targetPos, level.getGameTime(), contributions);
        receiver.receiveSignal(aggregatedSignal);
    }

    public void refreshTargets(Level level, Collection<BlockPos> targetPositions) {
        for (BlockPos targetPos : targetPositions) {
            refreshTarget(level, targetPos);
        }
    }
}
