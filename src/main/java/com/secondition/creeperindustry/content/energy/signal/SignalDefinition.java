package com.secondition.creeperindustry.content.energy.signal;

/** Parameters of a sampled traveling wave. Phase is measured in cycles at the anchor tick. */
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
                || wavelength < 1 || wavelength > 16
                || frequencyNumerator < 1 || frequencyNumerator > 32767
                || frequencyDenominator < 1 || frequencyDenominator > 32767
                || frequencyNumerator * 320L < frequencyDenominator
                || frequencyNumerator > 8L * frequencyDenominator
                || !Double.isFinite(phase) || phase < 0 || phase >= 1) {
            throw new IllegalArgumentException("Invalid wave parameters");
        }
    }

    public static SignalDefinition pulse(int amplitude) {
        return new SignalDefinition(amplitude, 0, 0, 0, 0, 0, SignalWaveform.PULSE);
    }

    public double frequency() {
        return frequencyNumerator / (double) frequencyDenominator;
    }

    public double speed() {
        return wavelength * frequency();
    }

    public double cycle(double emissionTime) {
        return phase + (emissionTime - anchorTick) * frequency();
    }

    public long sample(long peak, double emissionTime) {
        if (waveform == SignalWaveform.PULSE) return peak;
        long window = (long) Math.floor(cycle(emissionTime) * wavelength);
        return Math.round(peak * SignalWaveform.average(wavelength, (int) Math.floorMod(window, wavelength)));
    }

    public double nextWindow(double emissionTime) {
        long window = (long) Math.floor(cycle(emissionTime) * wavelength + 1e-9);
        return anchorTick + ((window + 1.0) / wavelength - phase) / frequency();
    }
}
