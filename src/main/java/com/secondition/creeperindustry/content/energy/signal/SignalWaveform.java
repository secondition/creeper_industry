package com.secondition.creeperindustry.content.energy.signal;

public enum SignalWaveform {
    TRIANGLE,
    PULSE;

    /** Mean of the ideal triangle over one of the equally spaced emission windows. */
    public static double average(int wavelength, int window) {
        double from = window / (double) wavelength;
        double to = (window + 1.0) / wavelength;
        return (integral(to) - integral(from)) * wavelength;
    }

    private static double integral(double phase) {
        return phase <= 0.5 ? phase - 2 * phase * phase
                : 2 * phase * phase - 3 * phase + 1;
    }
}
