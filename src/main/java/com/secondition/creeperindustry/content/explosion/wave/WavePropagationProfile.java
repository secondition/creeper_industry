package com.secondition.creeperindustry.content.explosion.wave;

public record WavePropagationProfile(
        double propagationSpeedBlocksPerTick,
        double attenuationPerBlock
) {
    public static final WavePropagationProfile DEFAULT = new WavePropagationProfile(1.0, 1.0);

    public WavePropagationProfile {
        if (propagationSpeedBlocksPerTick <= 0) {
            throw new IllegalArgumentException("Propagation speed must be positive");
        }
        if (attenuationPerBlock < 0) {
            throw new IllegalArgumentException("Attenuation per block cannot be negative");
        }
    }
}
