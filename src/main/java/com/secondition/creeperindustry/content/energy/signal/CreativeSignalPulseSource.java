package com.secondition.creeperindustry.content.energy.signal;

import java.util.UUID;

import com.secondition.creeperindustry.CISignalSourceTypes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record CreativeSignalPulseSource(
        UUID id,
        ResourceKey<Level> level,
        Vec3 position,
        long gameTime,
        SignalDefinition signal
) implements SignalSource {
    public static final int PULSE_PERIOD_TICKS = 1;
    public static final int DEFAULT_PHASE_TICKS = 0;

    public CreativeSignalPulseSource {
        if (id == null) {
            throw new IllegalArgumentException("Signal source id cannot be null");
        }
        if (gameTime < 0) {
            throw new IllegalArgumentException("Game time cannot be negative");
        }
    }

    @Override
    public SignalSourceType<CreativeSignalPulseSource> type() {
        return CISignalSourceTypes.CREATIVE_PULSE;
    }
}
