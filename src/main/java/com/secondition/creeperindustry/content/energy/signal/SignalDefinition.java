package com.secondition.creeperindustry.content.energy.signal;

/** Parameters of a traveling wave. Phase is measured in cycles at the anchor tick. */
public record SignalDefinition(
        int amplitude,
        int wavelength,
        int frequencyNumerator,
        int frequencyDenominator,
        long anchorTick,
        double phase,
        SignalWaveform waveform) {
    public static final int AMPLITUDE_SCALE = 1000;

    public SignalDefinition {
        if (amplitude == 0 || Math.abs((long) amplitude) > 4096)
            throw new IllegalArgumentException("Amplitude outside signed source range");
        if (waveform == SignalWaveform.PULSE) {
            if (wavelength != 0 || frequencyNumerator != 0 || frequencyDenominator != 0)
                throw new IllegalArgumentException("Pulse has no cycle");
        } else if (waveform != SignalWaveform.TRIANGLE
                || !validFrequency(frequencyNumerator, frequencyDenominator)
                || wavelength != (frequencyNumerator == 1 ? frequencyDenominator : 0)
                || !Double.isFinite(phase) || phase < 0 || phase >= 1) {
            throw new IllegalArgumentException("Invalid wave parameters");
        }
    }

    public static SignalDefinition pulse(int amplitude) {
        return new SignalDefinition(amplitude, 0, 0, 0, 0, 0, SignalWaveform.PULSE);
    }

    public static boolean validFrequency(int numerator, int denominator) {
        return numerator >= 1 && numerator <= 8 && denominator >= 1 && denominator <= 2560;
    }

    public static boolean validSourceFrequency(int numerator, int denominator) {
        return numerator == 1 && denominator >= 1 && denominator <= 320
                || denominator == 1 && numerator >= 2 && numerator <= 8;
    }

    public double frequency() {
        return frequencyNumerator / (double) frequencyDenominator;
    }

    public SignalDefinition atSnapshot(long snapshotTick, int slowdown) {
        if (waveform == SignalWaveform.PULSE) return this;
        int numerator = frequencyNumerator;
        int denominator = frequencyDenominator * slowdown;
        int divisor = java.math.BigInteger.valueOf(numerator)
                .gcd(java.math.BigInteger.valueOf(denominator)).intValue();
        numerator /= divisor;
        denominator /= divisor;
        double snapshotPhase = cycle(snapshotTick);
        snapshotPhase -= Math.floor(snapshotPhase);
        return new SignalDefinition(amplitude, numerator == 1 ? denominator : 0,
                numerator, denominator, snapshotTick, snapshotPhase, waveform);
    }

    public double speed() {
        return 1;
    }

    public double cycle(double emissionTime) {
        return phase + (emissionTime - anchorTick) * frequency();
    }

    public long sample(long peak, double emissionTime) {
        if (waveform == SignalWaveform.PULSE) return peak;
        return Math.round(peak * SignalWaveform.value(cycle(emissionTime)));
    }

    public double nextBend(double emissionTime) {
        double quarter = Math.floor(cycle(emissionTime) * 4 + 1e-9) + 1;
        return anchorTick + (quarter / 4 - phase) / frequency();
    }
}
