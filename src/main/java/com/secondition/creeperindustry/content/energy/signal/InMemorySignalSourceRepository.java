package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemorySignalSourceRepository implements SignalSourceRepository {
    private final Map<SignalSourceType<?>, SignalSourceType<?>> registeredTypes = new ConcurrentHashMap<>();
    private final Map<Long, List<SignalSource>> activeSources = new ConcurrentHashMap<>();

    @Override
    public <T extends SignalSource> SignalSourceType<T> registerType(SignalSourceType<T> type) {
        SignalSourceType<?> existing = registeredTypes.putIfAbsent(type, type);
        if (existing != null) {
            @SuppressWarnings("unchecked")
            SignalSourceType<T> cast = (SignalSourceType<T>) existing;
            return cast;
        }
        return type;
    }

    @Override
    public void put(SignalSource source) {
        if (!registeredTypes.containsKey(source.type())) {
            throw new IllegalArgumentException("Signal source type is not registered: " + source.type().id());
        }

        activeSources.computeIfAbsent(source.gameTime(), ignored -> new CopyOnWriteArrayList<>()).add(source);
    }

    @Override
    public Collection<SignalSourceType<?>> registeredTypes() {
        return List.copyOf(registeredTypes.values());
    }

    @Override
    public Collection<SignalSource> getActiveSources(long gameTime) {
        List<SignalSource> sources = activeSources.get(gameTime);
        if (sources == null) {
            return List.of();
        }
        return List.copyOf(sources);
    }

    @Override
    public Collection<SignalSource> removeActiveSources(long gameTime) {
        List<SignalSource> removed = activeSources.remove(gameTime);
        if (removed == null) {
            return List.of();
        }
        return List.copyOf(removed);
    }

    @Override
    public void clear() {
        activeSources.clear();
    }
}
