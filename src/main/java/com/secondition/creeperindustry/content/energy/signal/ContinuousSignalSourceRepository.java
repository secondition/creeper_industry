package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface ContinuousSignalSourceRepository {
    void put(ContinuousSignalSource source);

    Optional<ContinuousSignalSource> get(ResourceKey<Level> level, UUID sourceId);

    Collection<ContinuousSignalSource> getActiveSources(ResourceKey<Level> level);

    Optional<ContinuousSignalSource> remove(ResourceKey<Level> level, UUID sourceId);

    void clearLevel(ResourceKey<Level> level);
}
