package com.secondition.creeperindustry.content.energy.signal;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import com.secondition.creeperindustry.CISignalSourceTypes;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record ExplosionSignalSource(
        UUID id,
        ResourceKey<Level> level,
        Vec3 position,
        long gameTime,
        SignalDefinition signal,
        float explosionPower,
        ResourceLocation sourceId
) implements SignalSource {
    public static final int PULSE_PERIOD_TICKS = 1;
    public static final int DEFAULT_PHASE_TICKS = 0;

    public ExplosionSignalSource {
        if (id == null) {
            throw new IllegalArgumentException("Signal source id cannot be null");
        }
    }

    public static Optional<ExplosionSignalSource> fromContext(ExplosionSignalContext context, ExplosionSignalAmplitudeResolver amplitudeResolver) {
        OptionalInt amplitude = amplitudeResolver.resolveAmplitude(context);
        if (amplitude.isEmpty()) {
            return Optional.empty();
        }

        SignalDefinition signal = new SignalDefinition(amplitude.getAsInt(), PULSE_PERIOD_TICKS, DEFAULT_PHASE_TICKS, SignalWaveform.SQUARE);
        return Optional.of(new ExplosionSignalSource(
                UUID.randomUUID(),
                context.level(),
                context.position(),
                context.gameTime(),
                signal,
                context.explosionPower(),
                context.sourceId()
        ));
    }

    @Override
    public SignalSourceType<ExplosionSignalSource> type() {
        return CISignalSourceTypes.EXPLOSION;
    }
}
