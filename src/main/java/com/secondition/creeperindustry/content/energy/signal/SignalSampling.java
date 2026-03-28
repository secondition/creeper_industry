package com.secondition.creeperindustry.content.energy.signal;

final class SignalSampling {
    private SignalSampling() {
    }

    static int sampleAtGameTime(SignalDefinition signal, int effectiveAmplitude, long gameTime) {
        int period = signal.periodTicks();
        int phase = Math.floorMod((int) Math.floorMod(gameTime + signal.phaseTicks(), period), period);
        return switch (signal.waveform()) {
            case SQUARE -> sampleSquare(period, phase, effectiveAmplitude);
        };
    }

    private static int sampleSquare(int period, int phase, int effectiveAmplitude) {
        if (period == 1) {
            return effectiveAmplitude;
        }
        int half = period / 2;
        return phase < half ? effectiveAmplitude : -effectiveAmplitude;
    }
}
