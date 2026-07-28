package com.secondition.creeperindustry.content.explosion.wave;

public record WaveEffectProfile(
        double impulseThreshold,
        double impulseScale,
        double damageThreshold,
        double damageScale
) {
    public static final WaveEffectProfile DEFAULT = new WaveEffectProfile(0.0, 0.08, 2.0, 0.5);

    public WaveEffectProfile {
        if (impulseThreshold < 0 || !Double.isFinite(impulseThreshold)) {
            throw new IllegalArgumentException("Impulse threshold must be non-negative and finite");
        }
        if (impulseScale < 0 || !Double.isFinite(impulseScale)) {
            throw new IllegalArgumentException("Impulse scale must be non-negative and finite");
        }
        if (damageThreshold < 0 || !Double.isFinite(damageThreshold)) {
            throw new IllegalArgumentException("Damage threshold must be non-negative and finite");
        }
        if (damageScale < 0 || !Double.isFinite(damageScale)) {
            throw new IllegalArgumentException("Damage scale must be non-negative and finite");
        }
    }
}
