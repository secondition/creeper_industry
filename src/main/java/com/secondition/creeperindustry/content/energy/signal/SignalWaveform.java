package com.secondition.creeperindustry.content.energy.signal;

public enum SignalWaveform {
    TRIANGLE,
    PULSE;

    public static double value(double cycles) {
        double phase = cycles - Math.floor(cycles);
        return phase < 0.25 ? 4 * phase
                : phase < 0.75 ? 2 - 4 * phase : 4 * phase - 4;
    }
}
