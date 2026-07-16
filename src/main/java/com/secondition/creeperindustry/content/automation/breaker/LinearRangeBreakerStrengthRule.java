package com.secondition.creeperindustry.content.automation.breaker;

public final class LinearRangeBreakerStrengthRule implements RangeBreakerStrengthRule {
    private static final double RESISTANCE_NUMERATOR = 6.0D;
    private static final double AMPLITUDE_DENOMINATOR = 13.0D;

    @Override
    public boolean canBreak(float explosionResistance, int signalAmplitude, int rangeVolume) {
        if (!Float.isFinite(explosionResistance) || explosionResistance < 0.0F || signalAmplitude <= 0 || rangeVolume <= 0) {
            return false;
        }
        return AMPLITUDE_DENOMINATOR * rangeVolume * explosionResistance
                <= RESISTANCE_NUMERATOR * signalAmplitude;
    }

    @Override
    public double maximumBreakableResistance(int signalAmplitude, int rangeVolume) {
        if (signalAmplitude <= 0 || rangeVolume <= 0) {
            return 0.0D;
        }
        return RESISTANCE_NUMERATOR * signalAmplitude / (AMPLITUDE_DENOMINATOR * rangeVolume);
    }

    @Override
    public long minimumRequiredAmplitude(double explosionResistance, int rangeVolume) {
        if (!Double.isFinite(explosionResistance) || explosionResistance < 0.0D || rangeVolume <= 0) {
            return -1L;
        }
        double requiredAmplitude = AMPLITUDE_DENOMINATOR * rangeVolume * explosionResistance
                / RESISTANCE_NUMERATOR;
        if (requiredAmplitude > Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        return (long) Math.ceil(requiredAmplitude);
    }
}
