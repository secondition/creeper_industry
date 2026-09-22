package com.secondition.creeperindustry.content.energy.signal;

import java.util.*;

/** Pure fixed point compiler. Slow combinations never expand their common period. */
public final class CompositeWaveform {
    public record Term(long amplitude, int period, int phase) {
        public Term {
            if (period < 0
                    || period > 2000
                    || period % 2 != 0
                    || (period > 0 && (phase < 0 || phase >= period)))
                throw new IllegalArgumentException("Wave term");
        }

        public long sample(long time) {
            return period == 0 || Math.floorMod(time + phase, period) < period / 2
                    ? amplitude
                    : -amplitude;
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
        fast = !input.isEmpty() && input.stream().allMatch(t -> t.period() > 0 && t.period() <= 20);
        periodic = input.stream().allMatch(t -> t.period() > 0);
        // Classification precedes cancellation: optimization cannot upgrade mixed inputs.
        record Key(int period, int phase) {}
        Map<Key, Long> groups = new HashMap<>();
        for (Term term : input) {
            // Opposite half-cycle phases are the same basis with an inverted amplitude.
            boolean inverted = term.period() > 0 && term.phase() >= term.period() / 2;
            int phase = inverted ? term.phase() - term.period() / 2 : term.phase();
            groups.merge(
                    new Key(term.period(), phase),
                    inverted ? -term.amplitude() : term.amplitude(),
                    Math::addExact);
        }
        terms =
                groups.entrySet().stream()
                        .filter(e -> e.getValue() != 0)
                        .map(e -> new Term(e.getValue(), e.getKey().period(), e.getKey().phase()))
                        .toList();
        if (!fast) {
            fastSamples = null;
            period = 0;
            peak = 0;
            return;
        }
        int common = 1;
        for (Term term : terms) common = common / gcd(common, term.period()) * term.period();
        if (common > 5040) throw new IllegalStateException("Fast period exceeds finite tier bound");
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
        if (!periodic || Math.floorMod(units, 20) != 0) return direct(units);
        long tick = Math.floorDiv(units, 20);
        if (windowStart == Long.MIN_VALUE
                || tick < windowStart
                || tick >= windowStart + slowWindow.length) {
            windowStart = tick;
            for (int i = 0; i < slowWindow.length; i++) slowWindow[i] = direct((tick + i) * 20);
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
