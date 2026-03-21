package com.secondition.creeperindustry;

import com.secondition.creeperindustry.content.energy.signal.ExplosionSignalSource;
import com.secondition.creeperindustry.content.energy.signal.InMemorySignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.NoOpTransientSignalDispatcher;
import com.secondition.creeperindustry.content.energy.signal.SignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.SignalSourceType;
import com.secondition.creeperindustry.content.energy.signal.TransientSignalDispatcher;

public class CISignalSourceTypes {
    private static final SignalSourceRepository REPOSITORY = new InMemorySignalSourceRepository();
    private static final TransientSignalDispatcher TRANSIENT_DISPATCHER = new NoOpTransientSignalDispatcher();

    public static final SignalSourceType<ExplosionSignalSource> EXPLOSION = register("explosion", ExplosionSignalSource.class);

    private static <T extends com.secondition.creeperindustry.content.energy.signal.SignalSource> SignalSourceType<T> register(String path, Class<T> sourceClass) {
        return REPOSITORY.registerType(new SignalSourceType<>(CreeperIndustry.asResource(path), sourceClass));
    }

    public static SignalSourceRepository repository() {
        return REPOSITORY;
    }

    public static TransientSignalDispatcher transientDispatcher() {
        return TRANSIENT_DISPATCHER;
    }
}
