package com.secondition.creeperindustry.content.energy.signal;

final class SignalSampling {
    private SignalSampling() {
    }

    static int sampleAtGameTime(SignalDefinition signal, int effectiveAmplitude, long gameTime) {
        int period = signal.periodTicks();
        int phase = Math.floorMod((int) Math.floorMod(gameTime + signal.phaseTicks(), period), period);
        return switch (signal.waveform()) {
            case SQUARE -> phase * 2 < period ? effectiveAmplitude : -effectiveAmplitude;
        };
    }
}
