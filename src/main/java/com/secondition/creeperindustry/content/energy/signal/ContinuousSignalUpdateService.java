package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ContinuousSignalUpdateService {
    private final ContinuousSignalSourceRepository repository;
    private final SignalReceiverIndex receiverIndex;
    private final SignalReceiverSelector receiverSelector;
    private final UnifiedSignalRefreshService refreshService;
    private final DeferredSignalRefreshQueue deferredRefreshQueue;
    private final Set<BlockPos> continuouslyDrivenTargets = ConcurrentHashMap.newKeySet();

    public ContinuousSignalUpdateService(
            ContinuousSignalSourceRepository repository,
            SignalReceiverIndex receiverIndex,
            SignalReceiverSelector receiverSelector,
            UnifiedSignalRefreshService refreshService,
            DeferredSignalRefreshQueue deferredRefreshQueue
    ) {
        this.repository = repository;
        this.receiverIndex = receiverIndex;
        this.receiverSelector = receiverSelector;
        this.refreshService = refreshService;
        this.deferredRefreshQueue = deferredRefreshQueue;
    }

    public void upsert(Level level, ContinuousSignalSource source) {
        Collection<BlockPos> receiverPositions = new LinkedHashSet<>();
        repository.get(source.id())
                .ifPresent(previousSource -> receiverPositions.addAll(receiverSelector.getPotentialTargets(level, previousSource)));

        repository.put(source);
        receiverPositions.addAll(receiverSelector.getPotentialTargets(level, source));
        rebuildContinuouslyDrivenTargets(level);
        scheduleReceivers(receiverPositions);
    }

    public void remove(Level level, UUID sourceId) {
        repository.remove(sourceId).ifPresent(source -> {
            Collection<BlockPos> affectedReceivers = receiverSelector.getPotentialTargets(level, source);
            rebuildContinuouslyDrivenTargets(level);
            scheduleReceivers(affectedReceivers);
        });
    }

    public void registerReceiver(Level level, BlockPos receiverPos) {
        receiverIndex.register(receiverPos);
        rebuildContinuouslyDrivenTargets(level);
        if (continuouslyDrivenTargets.contains(receiverPos)) {
            deferredRefreshQueue.enqueue(java.util.List.of(receiverPos));
        }
    }

    public void unregisterReceiver(BlockPos receiverPos) {
        receiverIndex.unregister(receiverPos);
        continuouslyDrivenTargets.remove(receiverPos);
    }

    public void rebuildContinuouslyDrivenTargets(Level level) {
        LinkedHashSet<BlockPos> rebuiltTargets = new LinkedHashSet<>();
        for (ContinuousSignalSource source : repository.getActiveSources()) {
            rebuiltTargets.addAll(receiverSelector.getPotentialTargets(level, source));
        }
        continuouslyDrivenTargets.clear();
        continuouslyDrivenTargets.addAll(rebuiltTargets);
    }

    public Collection<BlockPos> continuouslyDrivenTargets() {
        return java.util.List.copyOf(continuouslyDrivenTargets);
    }

    private void scheduleReceivers(Collection<BlockPos> receiverPositions) {
        deferredRefreshQueue.enqueue(receiverPositions);
    }

    public void refreshScheduledReceivers(ServerLevel level, Collection<BlockPos> receiverPositions) {
        for (BlockPos receiverPos : receiverPositions) {
            if (!level.hasChunkAt(receiverPos)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(receiverPos);
            if (!(blockEntity instanceof SignalReceiver)) {
                unregisterReceiver(receiverPos);
                continue;
            }
            refreshService.refreshTarget(level, receiverPos);
        }
    }

    public void clear() {
        continuouslyDrivenTargets.clear();
    }
}
