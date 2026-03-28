package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class ContinuousSignalUpdateService {
    private final ContinuousSignalSourceRepository repository;
    private final SignalReceiverIndex receiverIndex;
    private final SignalReceiverSelector receiverSelector;
    private final UnifiedSignalRefreshService refreshService;

    public ContinuousSignalUpdateService(
            ContinuousSignalSourceRepository repository,
            SignalReceiverIndex receiverIndex,
            SignalReceiverSelector receiverSelector,
            UnifiedSignalRefreshService refreshService
    ) {
        this.repository = repository;
        this.receiverIndex = receiverIndex;
        this.receiverSelector = receiverSelector;
        this.refreshService = refreshService;
    }

    public void upsert(Level level, ContinuousSignalSource source) {
        Collection<BlockPos> receiverPositions = new LinkedHashSet<>();
        repository.get(level.dimension(), source.id())
                .ifPresent(previousSource -> receiverPositions.addAll(receiverSelector.getPotentialTargets(level, previousSource)));

        repository.put(source);
        receiverPositions.addAll(receiverSelector.getPotentialTargets(level, source));
        refreshReceivers(level, receiverPositions);
    }

    public void remove(Level level, UUID sourceId) {
        repository.remove(level.dimension(), sourceId)
                .ifPresent(source -> refreshAffectedReceivers(level, source));
    }

    public void removeWithoutRefresh(Level level, UUID sourceId) {
        repository.remove(level.dimension(), sourceId);
    }

    public void refreshAllReceivers(Level level) {
        refreshReceivers(level, receiverIndex.getAll(level.dimension()));
    }

    public void refreshReceivers(Level level, Collection<BlockPos> receiverPositions) {
        refreshService.refreshTargets(level, receiverPositions);
    }

    private void refreshAffectedReceivers(Level level, ContinuousSignalSource source) {
        Collection<BlockPos> receiverPositions = receiverSelector.getPotentialTargets(level, source);
        refreshReceivers(level, receiverPositions);
    }
}
