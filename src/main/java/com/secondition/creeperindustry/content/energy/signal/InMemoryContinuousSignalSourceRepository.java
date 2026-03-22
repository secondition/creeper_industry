package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class InMemoryContinuousSignalSourceRepository implements ContinuousSignalSourceRepository {
    private final Map<ResourceKey<Level>, Map<UUID, ContinuousSignalSource>> activeSources = new ConcurrentHashMap<>();

    @Override
    public void put(ContinuousSignalSource source) {
        activeSources
                .computeIfAbsent(source.level(), ignored -> new ConcurrentHashMap<>())
                .put(source.id(), source);
    }

    @Override
    public Optional<ContinuousSignalSource> get(ResourceKey<Level> level, UUID sourceId) {
        Map<UUID, ContinuousSignalSource> levelSources = activeSources.get(level);
        if (levelSources == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(levelSources.get(sourceId));
    }

    @Override
    public Collection<ContinuousSignalSource> getActiveSources(ResourceKey<Level> level) {
        Map<UUID, ContinuousSignalSource> levelSources = activeSources.get(level);
        if (levelSources == null) {
            return List.of();
        }
        return List.copyOf(levelSources.values());
    }

    @Override
    public Optional<ContinuousSignalSource> remove(ResourceKey<Level> level, UUID sourceId) {
        Map<UUID, ContinuousSignalSource> levelSources = activeSources.get(level);
        if (levelSources == null) {
            return Optional.empty();
        }

        ContinuousSignalSource removed = levelSources.remove(sourceId);
        if (levelSources.isEmpty()) {
            activeSources.remove(level, levelSources);
        }
        return Optional.ofNullable(removed);
    }

    @Override
    public void clearLevel(ResourceKey<Level> level) {
        activeSources.remove(level);
    }
}
