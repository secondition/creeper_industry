package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ContinuousSignalSourceRepository {
    void put(ContinuousSignalSource source);

    Optional<ContinuousSignalSource> get(UUID sourceId);

    Collection<ContinuousSignalSource> getActiveSources();

    Optional<ContinuousSignalSource> remove(UUID sourceId);

    void clear();
}
