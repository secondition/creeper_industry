package com.secondition.creeperindustry.content.energy.signal;

import java.util.UUID;

import com.secondition.creeperindustry.CISignalSourceTypes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record MachineSignalSource(
        UUID id,
        ResourceKey<Level> level,
        Vec3 position,
        long gameTime,
        SignalDefinition signal
) implements ContinuousSignalSource {
    public MachineSignalSource {
        if (id == null) {
            throw new IllegalArgumentException("Signal source id cannot be null");
        }
        if (gameTime < 0) {
            throw new IllegalArgumentException("Game time cannot be negative");
        }
    }

    @Override
    public SignalSourceType<MachineSignalSource> type() {
        return CISignalSourceTypes.MACHINE;
    }
}
