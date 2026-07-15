package com.secondition.creeperindustry.content.energy.signal.runtime;

import java.util.Collection;
import java.util.LinkedHashSet;

import com.secondition.creeperindustry.CISignalSourceTypes;
import com.secondition.creeperindustry.content.energy.signal.ContinuousSignalPropagationService;
import com.secondition.creeperindustry.content.energy.signal.ContinuousSignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.ContinuousSignalUpdateService;
import com.secondition.creeperindustry.content.energy.signal.DefaultSignalContributionAggregator;
import com.secondition.creeperindustry.content.energy.signal.DeferredSignalRefreshQueue;
import com.secondition.creeperindustry.content.energy.signal.DuctNetworkManager;
import com.secondition.creeperindustry.content.energy.signal.InMemoryContinuousSignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.InMemorySignalAggregationService;
import com.secondition.creeperindustry.content.energy.signal.InMemorySignalReceiverIndex;
import com.secondition.creeperindustry.content.energy.signal.InMemorySignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.InMemoryTransientSignalImpactTracker;
import com.secondition.creeperindustry.content.energy.signal.SignalAggregationService;
import com.secondition.creeperindustry.content.energy.signal.SignalReceiverIndex;
import com.secondition.creeperindustry.content.energy.signal.SignalReceiverSelector;
import com.secondition.creeperindustry.content.energy.signal.SignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.SimpleTransientSignalDispatcher;
import com.secondition.creeperindustry.content.energy.signal.TransientSignalDispatcher;
import com.secondition.creeperindustry.content.energy.signal.TransientSignalImpactTracker;
import com.secondition.creeperindustry.content.energy.signal.UnifiedSignalRefreshService;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class SignalRuntime implements AutoCloseable {
    private final SignalSourceRepository sourceRepository = new InMemorySignalSourceRepository();
    private final ContinuousSignalSourceRepository continuousSourceRepository = new InMemoryContinuousSignalSourceRepository();
    private final SignalReceiverIndex receiverIndex = new InMemorySignalReceiverIndex();
    private final DuctNetworkManager ductNetworkManager = new DuctNetworkManager();
    private final DeferredSignalRefreshQueue deferredRefreshQueue = new DeferredSignalRefreshQueue();
    private final TransientSignalImpactTracker transientImpactTracker = new InMemoryTransientSignalImpactTracker();
    private final SignalAggregationService aggregationService = new InMemorySignalAggregationService();
    private final DefaultSignalContributionAggregator contributionAggregator = new DefaultSignalContributionAggregator();
    private final SignalReceiverSelector receiverSelector = new SignalReceiverSelector(receiverIndex, ductNetworkManager);
    private final ContinuousSignalPropagationService continuousPropagationService = new ContinuousSignalPropagationService(
            continuousSourceRepository,
            contributionAggregator,
            ductNetworkManager
    );
    private final UnifiedSignalRefreshService refreshService = new UnifiedSignalRefreshService(
            continuousPropagationService,
            aggregationService,
            contributionAggregator
    );
    private final ContinuousSignalUpdateService continuousUpdateService = new ContinuousSignalUpdateService(
            continuousSourceRepository,
            receiverIndex,
            receiverSelector,
            refreshService,
            deferredRefreshQueue
    );
    private final TransientSignalDispatcher transientDispatcher;

    public SignalRuntime() {
        CISignalSourceTypes.registeredTypes().forEach(sourceRepository::registerType);
        transientDispatcher = new SimpleTransientSignalDispatcher(
                sourceRepository,
                receiverSelector,
                aggregationService,
                transientImpactTracker,
                deferredRefreshQueue,
                ductNetworkManager
        );
    }

    public ContinuousSignalSourceRepository continuousSourceRepository() {
        return continuousSourceRepository;
    }

    public SignalReceiverIndex receiverIndex() {
        return receiverIndex;
    }

    public DuctNetworkManager ductNetworkManager() {
        return ductNetworkManager;
    }

    public ContinuousSignalUpdateService continuousUpdateService() {
        return continuousUpdateService;
    }

    public TransientSignalDispatcher transientDispatcher() {
        return transientDispatcher;
    }

    public UnifiedSignalRefreshService refreshService() {
        return refreshService;
    }

    public void registerReceiver(BlockPos receiverPos) {
        continuousUpdateService.registerReceiver(receiverPos);
    }

    public void unregisterReceiver(BlockPos receiverPos) {
        continuousUpdateService.unregisterReceiver(receiverPos);
    }

    public void scheduleTopologyRefresh(Level level, Collection<BlockPos> receiverPositions) {
        continuousUpdateService.rebuildContinuouslyDrivenTargets(level);
        deferredRefreshQueue.enqueue(receiverPositions);
    }

    public void tick(ServerLevel level) {
        long staleThroughTick = level.getGameTime() - 1;
        LinkedHashSet<BlockPos> refreshTargets = new LinkedHashSet<>();
        if (staleThroughTick >= 0) {
            sourceRepository.removeActiveSources(staleThroughTick);
            aggregationService.clearThroughTick(staleThroughTick);
            refreshTargets.addAll(transientImpactTracker.popAffectedTargetsThroughTick(staleThroughTick));
        }
        continuousUpdateService.rebuildContinuouslyDrivenTargetsIfNeeded(level);
        refreshTargets.addAll(deferredRefreshQueue.drain());
        refreshTargets.addAll(continuousUpdateService.continuouslyDrivenTargets());
        continuousUpdateService.refreshScheduledReceivers(level, refreshTargets);
    }

    @Override
    public void close() {
        sourceRepository.clear();
        continuousSourceRepository.clear();
        receiverIndex.clear();
        ductNetworkManager.clear();
        aggregationService.clear();
        transientImpactTracker.clear();
        deferredRefreshQueue.clear();
        continuousUpdateService.clear();
    }
}
