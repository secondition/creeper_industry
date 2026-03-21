package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface SignalSourceRepository {
    <T extends SignalSource> SignalSourceType<T> registerType(SignalSourceType<T> type);

    void put(SignalSource source);

    Collection<SignalSourceType<?>> registeredTypes();

    Collection<SignalSource> getActiveSources(ResourceKey<Level> level, long gameTime);

    Collection<SignalSource> removeActiveSources(ResourceKey<Level> level, long gameTime);

    void clearLevel(ResourceKey<Level> level);
}
