package com.secondition.creeperindustry.content.energy.signal;

/** Source parameters; composite waveforms are stored separately. */
public record SignalDefinition(
        int amplitude, double periodTicks, double phaseTicks, SignalWaveform waveform) {
    public SignalDefinition {
        if (amplitude == 0 || Math.abs((long) amplitude) > 4096)
            throw new IllegalArgumentException("Amplitude outside signed source range");
        if (!SignalTime.isSourcePeriod(periodTicks))
            throw new IllegalArgumentException("Invalid source period: " + periodTicks);
        if (!Double.isFinite(phaseTicks) || phaseTicks < 0 || phaseTicks >= periodTicks)
            throw new IllegalArgumentException("Phase outside period");
        if (Math.abs(phaseTicks * 20 - Math.rint(phaseTicks * 20)) > 1e-7)
            throw new IllegalArgumentException("Phase must use 0.05 tick steps");
        if (waveform == null) throw new IllegalArgumentException("Missing waveform");
    }

    public int periodUnits() {
        return (int) Math.round(periodTicks * 20);
    }

    public int phaseUnits() {
        return (int) Math.round(phaseTicks * 20);
    }
}
