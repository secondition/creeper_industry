package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.List;

/** Sum of the discrete values present at one receiver. */
public final class CompositeWaveform {
    public record Term(long amplitude, SignalDefinition signal, double delay) {
        public long sample(double time) {
            return signal.sample(amplitude, time - delay);
        }

        public double nextChange(double time) {
            return signal.waveform() == SignalWaveform.TRIANGLE
                    ? signal.nextBend(time - delay) + delay : Double.POSITIVE_INFINITY;
        }
    }

    private final List<Term> terms;

    public CompositeWaveform(Collection<Term> input) {
        terms = List.copyOf(input);
    }

    public boolean isZero() {
        return terms.isEmpty();
    }

    public CompositeWaveform slow() {
        return new CompositeWaveform(terms.stream().filter(term ->
                term.signal().waveform() == SignalWaveform.PULSE
                        || term.signal().frequency() <= 1).toList());
    }

    public long valueAt(double time) {
        long sum = 0;
        for (Term term : terms) sum += term.sample(time);
        return sum;
    }

    public double nextChange(double time) {
        double next = Double.POSITIVE_INFINITY;
        for (Term term : terms) next = Math.min(next, term.nextChange(time));
        return next;
    }
}
