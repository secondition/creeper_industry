package com.secondition.creeperindustry.content.automation.breaker;

public interface RangeBreakerStrengthRule {
    boolean canBreak(float explosionResistance, int signalAmplitude, int rangeVolume);

    double maximumBreakableResistance(int signalAmplitude, int rangeVolume);

    long minimumRequiredAmplitude(double explosionResistance, int rangeVolume);
}
