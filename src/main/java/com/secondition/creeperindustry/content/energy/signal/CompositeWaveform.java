package com.secondition.creeperindustry.content.energy.signal;

import java.util.*;

/** Pure fixed point compiler. Slow combinations never expand their common period. */
public final class CompositeWaveform {
    public record Term(long amplitude, int period, int stages, int phase, boolean transientSignal) {
        public Term {
            if (period == 0) {
                if (stages != 0 || phase != 0)
                    throw new IllegalArgumentException("Wave term");
            } else if (period < 0
                    || period > SignalTime.MAX_PERIOD_UNITS
                    || stages < 2
                    || stages > 16
                    || stages % 2 != 0
                    || !SignalTime.isStageUnits(period / stages)
                    || period % stages != 0
                    || phase < 0
                    || phase >= period
                    || transientSignal) {
                throw new IllegalArgumentException("Wave term");
            }
        }

        public long sample(long time) {
            return period == 0 ? amplitude
                    : SignalWaveform.TRIANGLE.sample(amplitude, stages, period / stages, phase, time);
        }
    }

    private final List<Term> terms;
    private final boolean fast;
    private final boolean periodic;
    private final long[] fastSamples;
    private final int period;
    private final long peak;
    private long windowStart = Long.MIN_VALUE;
    private final long[] slowWindow = new long[32];

    public CompositeWaveform(Collection<Term> input) {
        boolean fastInput = input.stream().anyMatch(t -> t.period() > 0)
                && input.stream().allMatch(t -> !t.transientSignal() && t.period() <= SignalTime.UNITS_PER_TICK);
        periodic = input.stream().noneMatch(Term::transientSignal);
        // Classification precedes cancellation: optimization cannot upgrade mixed inputs.
        record Key(int period, int stages, int phase, boolean transientSignal) {}
        Map<Key, Long> groups = new HashMap<>();
        for (Term term : input) {
            // Opposite half-cycle phases are the same basis with an inverted amplitude.
            boolean inverted = term.period() > 0 && term.phase() >= term.period() / 2;
            int phase = inverted ? term.phase() - term.period() / 2 : term.phase();
            groups.merge(
                    new Key(term.period(), term.stages(), phase, term.transientSignal()),
                    inverted ? -term.amplitude() : term.amplitude(),
                    Math::addExact);
        }
        terms = groups.entrySet().stream()
                .filter(e -> e.getValue() != 0)
                .map(e -> new Term(e.getValue(), e.getKey().period(), e.getKey().stages(),
                        e.getKey().phase(), e.getKey().transientSignal()))
                .toList();
        fast = fastInput && terms.stream().anyMatch(t -> t.period() > 0);
        if (!fast) {
            fastSamples = null;
            period = 0;
            peak = 0;
            return;
        }
        int common = 1;
        for (Term term : terms)
            if (term.period() > 0) common = common / gcd(common, term.period()) * term.period();
        if (common > 8400) throw new IllegalStateException("Fast period exceeds finite tier bound");
        long[] values = new long[common];
        for (Term term : terms) for (int i = 0; i < common; i++) values[i] += term.sample(i);
        int[] prefix = new int[common];
        long maximum = 0;
        for (int i = 0; i < common; i++) {
            maximum = Math.max(maximum, Math.abs(values[i]));
            if (i == 0) continue;
            int j = prefix[i - 1];
            while (j > 0 && values[i] != values[j]) j = prefix[j - 1];
            if (values[i] == values[j]) j++;
            prefix[i] = j;
        }
        int candidate = common - prefix[common - 1];
        period = common % candidate == 0 ? candidate : common;
        peak = maximum;
        fastSamples = Arrays.copyOf(values, period);
    }

    public boolean isZero() {
        return terms.isEmpty() || (fast && peak == 0);
    }

    public boolean fast() {
        return fast;
    }

    public boolean periodic() {
        return periodic;
    }

    public int periodUnits() {
        return period;
    }

    public long peak() {
        return peak;
    }

    public long valueAt(long units) {
        if (fast) return fastSamples[(int) Math.floorMod(units, period)];
        if (!periodic || Math.floorMod(units, SignalTime.UNITS_PER_TICK) != 0) return direct(units);
        long tick = Math.floorDiv(units, SignalTime.UNITS_PER_TICK);
        if (windowStart == Long.MIN_VALUE
                || tick < windowStart
                || tick >= windowStart + slowWindow.length) {
            windowStart = tick;
            for (int i = 0; i < slowWindow.length; i++)
                slowWindow[i] = direct((tick + i) * SignalTime.UNITS_PER_TICK);
        }
        return slowWindow[(int) (tick - windowStart)];
    }

    private long direct(long time) {
        long sum = 0;
        for (Term term : terms) sum += term.sample(time);
        return sum;
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }
}
