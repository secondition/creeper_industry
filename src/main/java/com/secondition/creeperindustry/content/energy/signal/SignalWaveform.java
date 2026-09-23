package com.secondition.creeperindustry.content.energy.signal;

public enum SignalWaveform {
    TRIANGLE,
    STATIC;

    public long sample(long peak, int stages, int stageUnits, int phaseUnits, long timeUnits) {
        if (this == STATIC) return peak;
        int step = (int) (Math.floorMod(timeUnits + phaseUnits, stages * stageUnits) / stageUnits);
        return peak * (stages - 4L * Math.min(step, stages - step)) / stages;
    }
}
