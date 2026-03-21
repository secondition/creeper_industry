package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface SignalSourceRepository {
    <T extends SignalSource> SignalSourceType<T> registerType(SignalSourceType<T> type);

    void submit(SignalSource source);

    Collection<SignalSourceType<?>> registeredTypes();

    Collection<SignalSource> getSources(ResourceKey<Level> level, long gameTime);

    Collection<SignalSource> drainSources(ResourceKey<Level> level, long gameTime);

    void clearLevel(ResourceKey<Level> level);
}
