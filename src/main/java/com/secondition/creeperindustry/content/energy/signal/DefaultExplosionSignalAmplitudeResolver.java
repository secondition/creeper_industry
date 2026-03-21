package com.secondition.creeperindustry.content.energy.signal;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalInt;

import net.minecraft.util.Mth;

public class DefaultExplosionSignalAmplitudeResolver implements ExplosionSignalAmplitudeResolver {
    private final Map<ExplosionSignalKind, Integer> baseAmplitudes;

    public DefaultExplosionSignalAmplitudeResolver() {
        this(baseAmplitudeMap());
    }

    public DefaultExplosionSignalAmplitudeResolver(Map<ExplosionSignalKind, Integer> baseAmplitudes) {
        this.baseAmplitudes = Map.copyOf(baseAmplitudes);
    }

    @Override
    public OptionalInt resolveAmplitude(ExplosionSignalContext context) {
        Integer baseAmplitude = baseAmplitudes.get(context.kind());
        if (baseAmplitude == null) {
            return OptionalInt.empty();
        }

        int powerFloor = Mth.ceil(context.explosionPower() * 2.0F);
        return OptionalInt.of(Math.max(baseAmplitude, powerFloor));
    }

    private static Map<ExplosionSignalKind, Integer> baseAmplitudeMap() {
        EnumMap<ExplosionSignalKind, Integer> amplitudes = new EnumMap<>(ExplosionSignalKind.class);
        amplitudes.put(ExplosionSignalKind.CREEPER, 12);
        amplitudes.put(ExplosionSignalKind.TNT, 10);
        amplitudes.put(ExplosionSignalKind.BED, 14);
        amplitudes.put(ExplosionSignalKind.END_CRYSTAL, 16);
        return amplitudes;
    }
}
