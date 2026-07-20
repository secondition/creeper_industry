package com.secondition.creeperindustry.content.explosion.wave;

import java.util.UUID;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public record PulseWaveEmission(
        UUID id,
        Vec3 origin,
        long emissionGameTime,
        int sourceAmplitude,
        WavePropagationProfile profile
) {
    public PulseWaveEmission {
        if (id == null) {
            throw new IllegalArgumentException("Emission id cannot be null");
        }
        if (origin == null) {
            throw new IllegalArgumentException("Origin cannot be null");
        }
        if (sourceAmplitude <= 0) {
            throw new IllegalArgumentException("Source amplitude must be positive");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Propagation profile cannot be null");
        }
    }

    public double maxEffectiveRadius() {
        return WavePropagationMath.maxEffectiveRadius(sourceAmplitude, profile.attenuationPerBlock());
    }

    public static int amplitudeFromExplosionPower(float explosionPower) {
        return Mth.ceil(explosionPower * 4.0F);
    }
}
