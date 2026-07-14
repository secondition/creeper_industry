package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

public interface SignalSourceRepository {
    <T extends SignalSource> SignalSourceType<T> registerType(SignalSourceType<T> type);

    void put(SignalSource source);

    Collection<SignalSourceType<?>> registeredTypes();

    Collection<SignalSource> getActiveSources(long gameTime);

    Collection<SignalSource> removeActiveSources(long gameTime);

    void clear();
}
