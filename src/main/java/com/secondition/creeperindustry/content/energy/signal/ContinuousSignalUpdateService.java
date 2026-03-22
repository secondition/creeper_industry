package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class ContinuousSignalUpdateService {
    private final ContinuousSignalSourceRepository repository;
    private final SignalReceiverIndex receiverIndex;
    private final SignalReceiverSelector receiverSelector;
    private final ContinuousSignalPropagationService propagationService;

    public ContinuousSignalUpdateService(
            ContinuousSignalSourceRepository repository,
            SignalReceiverIndex receiverIndex,
            SignalReceiverSelector receiverSelector,
            ContinuousSignalPropagationService propagationService
    ) {
        this.repository = repository;
        this.receiverIndex = receiverIndex;
        this.receiverSelector = receiverSelector;
        this.propagationService = propagationService;
    }

    public void upsert(Level level, ContinuousSignalSource source) {
        repository.put(source);
        refreshAffectedReceivers(level, source);
    }

    public void remove(Level level, UUID sourceId) {
        repository.remove(level.dimension(), sourceId)
                .ifPresent(source -> refreshAffectedReceivers(level, source));
    }

    public void refreshAllReceivers(Level level) {
        refreshReceivers(level, receiverIndex.getAll(level.dimension()));
    }

    public void refreshReceivers(Level level, Collection<BlockPos> receiverPositions) {
        propagationService.refreshTargets(level, receiverPositions);
    }

    private void refreshAffectedReceivers(Level level, ContinuousSignalSource source) {
        Collection<BlockPos> receiverPositions = receiverSelector.getPotentialTargets(level.dimension(), source);
        refreshReceivers(level, receiverPositions);
    }
}
