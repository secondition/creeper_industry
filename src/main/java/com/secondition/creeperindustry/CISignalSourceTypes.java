package com.secondition.creeperindustry;

import com.secondition.creeperindustry.content.energy.signal.ExplosionSignalSource;
import com.secondition.creeperindustry.content.energy.signal.InMemorySignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.SignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.SignalSourceType;

public class CISignalSourceTypes {
    private static final SignalSourceRepository REPOSITORY = new InMemorySignalSourceRepository();

    public static final SignalSourceType<ExplosionSignalSource> EXPLOSION = register("explosion", ExplosionSignalSource.class);

    private static <T extends com.secondition.creeperindustry.content.energy.signal.SignalSource> SignalSourceType<T> register(String path, Class<T> sourceClass) {
        return REPOSITORY.registerType(new SignalSourceType<>(CreeperIndustry.asResource(path), sourceClass));
    }

    public static SignalSourceRepository repository() {
        return REPOSITORY;
    }
}
