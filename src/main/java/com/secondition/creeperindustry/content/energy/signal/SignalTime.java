package com.secondition.creeperindustry.content.energy.signal;

public final class SignalTime {
    public static final int UNITS_PER_TICK = 80;
    public static final int AMPLITUDE_SCALE = 1000;
    public static final int STAGE_LENGTH_COUNT = 14;
    public static final int MAX_PERIOD_UNITS = 16 * 10 * UNITS_PER_TICK;

    private SignalTime() {}

    public static int stageUnits(int index) {
        if (index < 0 || index >= STAGE_LENGTH_COUNT)
            throw new IllegalArgumentException("Stage length index");
        return index < 4 ? 5 << index : (index - 3) * UNITS_PER_TICK;
    }

    public static boolean isStageUnits(int units) {
        for (int index = 0; index < STAGE_LENGTH_COUNT; index++)
            if (stageUnits(index) == units) return true;
        return false;
    }

    public static long travelUnits(double distance, double speed) {
        return Math.max(0, (long) Math.ceil(distance / speed * UNITS_PER_TICK - 1e-9));
    }
}
