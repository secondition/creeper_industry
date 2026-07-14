package com.secondition.creeperindustry.content.energy.signal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class UnifiedSignalRefreshService {
    private final ContinuousSignalPropagationService continuousPropagationService;
    private final SignalAggregationService signalAggregationService;
    private final SignalContributionAggregator signalContributionAggregator;

    public UnifiedSignalRefreshService(
            ContinuousSignalPropagationService continuousPropagationService,
            SignalAggregationService signalAggregationService,
            SignalContributionAggregator signalContributionAggregator
    ) {
        this.continuousPropagationService = continuousPropagationService;
        this.signalAggregationService = signalAggregationService;
        this.signalContributionAggregator = signalContributionAggregator;
    }

    public void refreshTarget(Level level, BlockPos targetPos) {
        refreshTarget(level, targetPos, level.getGameTime());
    }

    public void refreshTarget(Level level, BlockPos targetPos, long gameTime) {
        BlockEntity blockEntity = level.getBlockEntity(targetPos);
        if (!(blockEntity instanceof SignalReceiver receiver)) {
            return;
        }

        List<DeliveredSignal> contributions = new ArrayList<>(
                continuousPropagationService.collectContributions(level, targetPos, gameTime)
        );
        contributions.addAll(signalAggregationService.getSubmittedContributions(targetPos, gameTime));

        if (contributions.isEmpty()) {
            receiver.clearSignal();
            return;
        }

        @Nullable AggregatedSignal aggregatedSignal = signalContributionAggregator.aggregate(
                level.dimension(),
                targetPos,
                gameTime,
                contributions
        );
        if (aggregatedSignal == null) {
            receiver.clearSignal();
            return;
        }
        receiver.receiveSignal(aggregatedSignal);
    }

    public void refreshTargets(Level level, Collection<BlockPos> targetPositions) {
        refreshTargets(level, targetPositions, level.getGameTime());
    }

    public void refreshTargets(Level level, Collection<BlockPos> targetPositions, long gameTime) {
        for (BlockPos targetPos : targetPositions) {
            refreshTarget(level, targetPos, gameTime);
        }
    }
}
