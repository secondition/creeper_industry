package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryContinuousSignalSourceRepository implements ContinuousSignalSourceRepository {
    private final Map<UUID, ContinuousSignalSource> activeSources = new ConcurrentHashMap<>();

    @Override
    public void put(ContinuousSignalSource source) {
        activeSources.put(source.id(), source);
    }

    @Override
    public Optional<ContinuousSignalSource> get(UUID sourceId) {
        return Optional.ofNullable(activeSources.get(sourceId));
    }

    @Override
    public Collection<ContinuousSignalSource> getActiveSources() {
        return java.util.List.copyOf(activeSources.values());
    }

    @Override
    public Optional<ContinuousSignalSource> remove(UUID sourceId) {
        return Optional.ofNullable(activeSources.remove(sourceId));
    }

    @Override
    public void clear() {
        activeSources.clear();
    }
}
