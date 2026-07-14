package com.secondition.creeperindustry;

import com.secondition.creeperindustry.content.energy.signal.CreativeSignalPulseSource;
import com.secondition.creeperindustry.content.energy.signal.ExplosionSignalSource;
import com.secondition.creeperindustry.content.energy.signal.MachineSignalSource;
import com.secondition.creeperindustry.content.energy.signal.SignalSourceType;

import java.util.Collection;
import java.util.List;

public final class CISignalSourceTypes {
    public static final SignalSourceType<ExplosionSignalSource> EXPLOSION = new SignalSourceType<>(
            CreeperIndustry.asResource("explosion"),
            ExplosionSignalSource.class
    );
    public static final SignalSourceType<MachineSignalSource> MACHINE = new SignalSourceType<>(
            CreeperIndustry.asResource("machine"),
            MachineSignalSource.class
    );
    public static final SignalSourceType<CreativeSignalPulseSource> CREATIVE_PULSE = new SignalSourceType<>(
            CreeperIndustry.asResource("creative_pulse"),
            CreativeSignalPulseSource.class
    );

    private CISignalSourceTypes() {
    }

    public static Collection<SignalSourceType<?>> registeredTypes() {
        return List.of(EXPLOSION, MACHINE, CREATIVE_PULSE);
    }
}
