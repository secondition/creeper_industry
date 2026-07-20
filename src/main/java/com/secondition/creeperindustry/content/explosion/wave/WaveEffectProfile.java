package com.secondition.creeperindustry.content.explosion.wave;

public record WaveEffectProfile(
        double impulseThreshold,
        double impulseScale,
        double damageThreshold,
        double damageScale
) {
    public static final WaveEffectProfile DEFAULT = new WaveEffectProfile(0.0, 0.08, 2.0, 0.5);

    public WaveEffectProfile {
        if (impulseThreshold < 0) {
            throw new IllegalArgumentException("Impulse threshold cannot be negative");
        }
        if (Double.isNaN(impulseScale)) {
            throw new IllegalArgumentException("Impulse scale must be a number");
        }
        if (damageThreshold < 0) {
            throw new IllegalArgumentException("Damage threshold cannot be negative");
        }
        if (Double.isNaN(damageScale)) {
            throw new IllegalArgumentException("Damage scale must be a number");
        }
    }
}
