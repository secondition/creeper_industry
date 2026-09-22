package com.secondition.creeperindustry.content.energy.signal;

public final class SignalTime {
    public static final int UNITS_PER_TICK = 20;
    public static final int AMPLITUDE_SCALE = 1000;
    public static final int PERIOD_COUNT = 60;

    private SignalTime() {}

    public static boolean isSourcePeriod(double ticks) {
        if (!Double.isFinite(ticks) || ticks <= 0 || ticks > 100) return false;
        return ticks <= 1
                ? Math.abs(ticks * 10 - Math.rint(ticks * 10)) < 1e-7
                : Math.abs(ticks / 2 - Math.rint(ticks / 2)) < 1e-7;
    }

    public static double periodAt(int index) {
        if (index < 0 || index >= PERIOD_COUNT) throw new IllegalArgumentException("Period index");
        return index < 10 ? (index + 1) / 10.0 : (index - 9) * 2.0;
    }

    public static int indexOf(double ticks) {
        if (!isSourcePeriod(ticks)) throw new IllegalArgumentException("Period");
        return ticks <= 1 ? (int) Math.round(ticks * 10) - 1 : 9 + (int) Math.round(ticks / 2);
    }

    public static long travelUnits(double distance, double speed) {
        return Math.max(0, (long) Math.ceil(distance / speed * 20 - 1e-9));
    }
}
