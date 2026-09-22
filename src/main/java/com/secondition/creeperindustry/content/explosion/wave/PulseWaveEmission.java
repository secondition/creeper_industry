package com.secondition.creeperindustry.content.explosion.wave;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

import javax.annotation.Nullable;

public record PulseWaveEmission(
        UUID id,
        Vec3 origin,
        long emissionGameTime,
        double sourceAmplitude,
        WavePropagationProfile profile,
        @Nullable Entity directSource,
        @Nullable Entity causingEntity,
        @Nullable Explosion originalExplosion) {
    public PulseWaveEmission {
        if (id == null) {
            throw new IllegalArgumentException("Emission id cannot be null");
        }
        if (origin == null) {
            throw new IllegalArgumentException("Origin cannot be null");
        }
        if (sourceAmplitude == 0 || !Double.isFinite(sourceAmplitude)) {
            throw new IllegalArgumentException("Source amplitude must be positive and finite");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Propagation profile cannot be null");
        }
    }

    public double maxEffectiveRadius() {
        return WavePropagationMath.maxEffectiveRadius(
                Math.abs(sourceAmplitude), profile.attenuationPerBlock());
    }

    public static double amplitudeFromExplosionPower(float explosionPower) {
        return Math.ceil(explosionPower * 4.0F);
    }
}
