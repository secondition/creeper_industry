package com.secondition.creeperindustry.content.energy.signal;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class SimpleTransientSignalDispatcher implements TransientSignalDispatcher {
    private final SignalSourceRepository sourceRepository;
    private final SignalReceiverSelector receiverSelector;
    private final SignalAggregationService aggregationService;
    private final TransientSignalImpactTracker impactTracker;
    private final DeferredSignalRefreshQueue deferredRefreshQueue;
    private final DuctNetworkManager ductNetworkManager;

    public SimpleTransientSignalDispatcher(
            SignalSourceRepository sourceRepository,
            SignalReceiverSelector receiverSelector,
            SignalAggregationService aggregationService,
            TransientSignalImpactTracker impactTracker,
            DeferredSignalRefreshQueue deferredRefreshQueue,
            DuctNetworkManager ductNetworkManager
    ) {
        this.sourceRepository = sourceRepository;
        this.receiverSelector = receiverSelector;
        this.aggregationService = aggregationService;
        this.impactTracker = impactTracker;
        this.deferredRefreshQueue = deferredRefreshQueue;
        this.ductNetworkManager = ductNetworkManager;
    }

    @Override
    public void dispatch(Level level, SignalSource source) {
        sourceRepository.put(source);
        List<SignalReach> reachableTargets = SignalReachEvaluator.findReachableTargets(
                ductNetworkManager,
                level,
                source,
                receiverSelector.getPotentialTargets(level, source)
        );
        for (SignalReach reach : reachableTargets) {
            aggregationService.submitContribution(
                    level,
                    new DeliveredSignal(source, reach.targetPos(), reach.effectiveAmplitude(), reach.propagationCost())
            );
        }

        List<BlockPos> affectedTargets = reachableTargets.stream().map(SignalReach::targetPos).toList();
        impactTracker.record(
                source.gameTime(),
                affectedTargets
        );
        deferredRefreshQueue.enqueue(affectedTargets);
    }
}
