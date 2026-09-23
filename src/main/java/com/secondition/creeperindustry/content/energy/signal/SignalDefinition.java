package com.secondition.creeperindustry.content.energy.signal;

/** Source parameters; composite waveforms are stored separately. */
public record SignalDefinition(
        int amplitude, int stages, int stageUnits, int phaseSteps, SignalWaveform waveform) {
    public SignalDefinition {
        if (amplitude == 0 || Math.abs((long) amplitude) > 4096)
            throw new IllegalArgumentException("Amplitude outside signed source range");
        if (waveform == null) throw new IllegalArgumentException("Missing waveform");
        if (waveform == SignalWaveform.STATIC) {
            if (stages != 0 || stageUnits != 0 || phaseSteps != 0)
                throw new IllegalArgumentException("Static field has no cycle");
        } else if (stages < 2
                || stages > 16
                || stages % 2 != 0
                || !SignalTime.isStageUnits(stageUnits)
                || phaseSteps < 0
                || phaseSteps >= stages) {
            throw new IllegalArgumentException("Invalid waveform stages");
        }
    }

    public int periodUnits() {
        return stages * stageUnits;
    }

    public int phaseUnits() {
        return phaseSteps * stageUnits;
    }

    public double periodTicks() {
        return (double) periodUnits() / SignalTime.UNITS_PER_TICK;
    }

    public long sample(long peak, long timeUnits) {
        return waveform.sample(peak, stages, stageUnits, phaseUnits(), timeUnits);
    }
}
