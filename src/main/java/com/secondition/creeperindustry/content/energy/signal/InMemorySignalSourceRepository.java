package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class InMemorySignalSourceRepository implements SignalSourceRepository {
    private final Map<SignalSourceType<?>, SignalSourceType<?>> registeredTypes = new ConcurrentHashMap<>();
    private final Map<SignalCollectionKey, List<SignalSource>> activeSources = new ConcurrentHashMap<>();

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

        SignalCollectionKey key = new SignalCollectionKey(source.level(), source.gameTime());
        activeSources.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(source);
    }

    @Override
    public Collection<SignalSourceType<?>> registeredTypes() {
        return List.copyOf(registeredTypes.values());
    }

    @Override
    public Collection<SignalSource> getActiveSources(ResourceKey<Level> level, long gameTime) {
        SignalCollectionKey key = new SignalCollectionKey(level, gameTime);
        List<SignalSource> sources = activeSources.get(key);
        if (sources == null) {
            return List.of();
        }
        return List.copyOf(sources);
    }

    @Override
    public Collection<SignalSource> removeActiveSources(ResourceKey<Level> level, long gameTime) {
        SignalCollectionKey key = new SignalCollectionKey(level, gameTime);
        List<SignalSource> removed = activeSources.remove(key);
        if (removed == null) {
            return List.of();
        }
        return List.copyOf(removed);
    }

    @Override
    public void clearLevel(ResourceKey<Level> level) {
        activeSources.keySet().removeIf(key -> key.level().equals(level));
    }

    private record SignalCollectionKey(ResourceKey<Level> level, long gameTime) {
    }
}
