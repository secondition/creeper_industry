package com.secondition.creeperindustry.content.energy.signal;

public record SignalDefinition(int amplitude, int periodTicks, int phaseTicks, SignalWaveform waveform) {
    public SignalDefinition {
        if (amplitude <= 0) {
            throw new IllegalArgumentException("Signal amplitude must be positive");
        }
        if (periodTicks <= 0) {
            throw new IllegalArgumentException("Signal period must be positive");
        }
        if (phaseTicks < 0) {
            throw new IllegalArgumentException("Signal phase cannot be negative");
        }
    }
}
